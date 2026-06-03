import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { IndexedDbService } from './indexed-db.service';
import { isDenied } from './offline-policy';

/** Statut d'une entrée de l'outbox. */
export type OutboxStatus = 'PENDING' | 'SYNCING' | 'FAILED' | 'CONFLICT';

/** Méthode HTTP mutante stockable dans l'outbox. */
export type OutboxMethod = 'POST' | 'PUT' | 'PATCH' | 'DELETE';

/**
 * Entrée de la file d'attente offline : une requête mutante émise hors ligne,
 * persistée dans IndexedDB et rejouée au retour de la connectivité.
 *
 * On ne stocke qu'un sous-ensemble sûr de headers — JAMAIS le header
 * `Authorization` (pas de JWT/token au repos, cf. consignes sécurité).
 */
export interface OutboxEntry {
  /** Identifiant primaire (clé IndexedDB). */
  id: string;
  method: OutboxMethod;
  url: string;
  /** Corps sérialisé (déjà JSON-compatible). `null` pour DELETE sans body. */
  body: unknown;
  /** Sous-ensemble whitelisté de headers (Content-Type, X-Tenant-ID, ...). */
  headers: Record<string, string>;
  /** Tenant propriétaire — permet l'isolation et clearForTenant() (A04). */
  tenantId: string;
  /** Clé d'idempotence client envoyée au backend pour dédupliquer (A08). */
  idempotencyKey: string;
  createdAt: number;
  status: OutboxStatus;
  /** Nombre de tentatives de replay déjà effectuées. */
  retries: number;
  /** Dernière erreur lisible (diagnostic UI). */
  lastError?: string;
}

/** Données nécessaires à la création d'une entrée (le reste est dérivé). */
export interface NewOutboxRequest {
  method: OutboxMethod;
  url: string;
  body: unknown;
  headers: Record<string, string>;
  tenantId: string;
}

/**
 * File d'attente d'écritures offline.
 *
 * - `enqueue()` persiste une requête mutante et génère sa clé d'idempotence.
 * - `sync()` / `drain()` rejouent les entrées dans l'ordre de création quand
 *   la connexion revient ; succès → suppression, 409 → CONFLICT (jamais perdu),
 *   autres erreurs → retry avec backoff plafonné.
 *
 * Signals exposés : `pendingCount`, `entries`, `conflicts`.
 */
@Injectable({ providedIn: 'root' })
export class OutboxService {

  private static readonly STORE = 'outbox' as const;
  /** Plafond de tentatives avant abandon (passage en FAILED définitif). */
  static readonly MAX_RETRIES = 5;
  /** Base du backoff exponentiel (ms) : 1s, 2s, 4s, 8s, 16s. */
  private static readonly BACKOFF_BASE_MS = 1_000;
  private static readonly BACKOFF_CAP_MS = 60_000;
  /** Header d'idempotence attendu par le backend (cf. nexus-payment). */
  static readonly IDEMPOTENCY_HEADER = 'Idempotency-Key';

  private readonly db = inject(IndexedDbService);
  private readonly http = inject(HttpClient);

  private readonly _entries = signal<OutboxEntry[]>([]);
  readonly entries = this._entries.asReadonly();

  /** Nombre d'opérations en attente de synchronisation (PENDING + FAILED + SYNCING). */
  readonly pendingCount = computed(
    () => this._entries().filter((e) => e.status !== 'CONFLICT').length,
  );

  /** Entrées en conflit (409) qui requièrent une intervention utilisateur. */
  readonly conflicts = computed(() => this._entries().filter((e) => e.status === 'CONFLICT'));

  private syncing = false;

  /** Recharge la liste des entrées depuis IndexedDB (au démarrage de l'app). */
  async hydrate(): Promise<void> {
    const all = await this.db.getAll<OutboxEntry>(OutboxService.STORE);
    all.sort((a, b) => a.createdAt - b.createdAt);
    this._entries.set(all);
  }

  /**
   * Met une requête mutante en file d'attente. Refuse silencieusement les
   * endpoints du denylist (sécurité) en levant une erreur explicite.
   * Retourne l'entrée créée (avec sa clé d'idempotence).
   */
  async enqueue(request: NewOutboxRequest): Promise<OutboxEntry> {
    if (isDenied(request.url)) {
      throw new Error(`Endpoint sensible non éligible à la file offline: ${request.url}`);
    }

    const entry: OutboxEntry = {
      id: crypto.randomUUID(),
      method: request.method,
      url: request.url,
      body: request.body ?? null,
      headers: this.sanitizeHeaders(request.headers),
      tenantId: request.tenantId,
      idempotencyKey: crypto.randomUUID(),
      createdAt: Date.now(),
      status: 'PENDING',
      retries: 0,
    };

    await this.db.put(OutboxService.STORE, entry);
    this._entries.update((list) => [...list, entry]);
    return entry;
  }

  /** Alias sémantique de sync(). */
  drain(): Promise<void> {
    return this.sync();
  }

  /**
   * Rejoue les entrées rejouables (PENDING / FAILED sous le plafond) dans
   * l'ordre de création. Idempotent : un second appel concurrent est ignoré.
   */
  async sync(): Promise<void> {
    if (this.syncing) {
      return;
    }
    this.syncing = true;
    try {
      const ordered = [...this._entries()].sort((a, b) => a.createdAt - b.createdAt);
      for (const entry of ordered) {
        if (!this.isReplayable(entry)) {
          continue;
        }
        await this.replay(entry);
      }
    } finally {
      this.syncing = false;
    }
  }

  /** Une entrée est rejouable si non résolue et sous le plafond de tentatives. */
  private isReplayable(entry: OutboxEntry): boolean {
    if (entry.status === 'CONFLICT') {
      return false;
    }
    return entry.retries < OutboxService.MAX_RETRIES;
  }

  /** Rejoue une entrée et applique la logique de succès / conflit / retry. */
  private async replay(entry: OutboxEntry): Promise<void> {
    await this.patch(entry.id, { status: 'SYNCING' });

    const headers = new HttpHeaders({
      ...entry.headers,
      [OutboxService.IDEMPOTENCY_HEADER]: entry.idempotencyKey,
    });

    try {
      await firstValueFrom(
        this.http.request(entry.method, entry.url, {
          body: entry.body ?? undefined,
          headers,
        }),
      );
      // Succès → on retire l'entrée définitivement.
      await this.remove(entry.id);
    } catch (error) {
      const status = this.extractStatus(error);
      if (status === 409) {
        // Conflit : NE JAMAIS perdre — on remonte à l'utilisateur.
        await this.patch(entry.id, {
          status: 'CONFLICT',
          lastError: 'Conflit serveur (409) — résolution manuelle requise',
        });
        return;
      }
      const retries = entry.retries + 1;
      const status409Free = retries >= OutboxService.MAX_RETRIES ? 'FAILED' : 'PENDING';
      await this.patch(entry.id, {
        status: status409Free,
        retries,
        lastError: this.describeError(error, status),
      });
    }
  }

  /**
   * Délai de backoff exponentiel plafonné pour la n-ième tentative.
   * Exposé pour permettre à l'orchestrateur (SyncService) de planifier un retry.
   */
  static backoffDelayMs(retries: number): number {
    const delay = OutboxService.BACKOFF_BASE_MS * 2 ** retries;
    return Math.min(delay, OutboxService.BACKOFF_CAP_MS);
  }

  /** Supprime explicitement une entrée (ex: l'utilisateur abandonne un conflit). */
  async remove(id: string): Promise<void> {
    await this.db.delete(OutboxService.STORE, id);
    this._entries.update((list) => list.filter((e) => e.id !== id));
  }

  /**
   * Vide TOUTE la file (logout global). Sécurité A04.
   */
  async clearAll(): Promise<void> {
    await this.db.clear(OutboxService.STORE);
    this._entries.set([]);
  }

  /**
   * Vide la file pour un tenant donné (changement de tenant / logout). A04.
   */
  async clearForTenant(tenantId: string): Promise<void> {
    await this.db.deleteByTenant(OutboxService.STORE, tenantId);
    this._entries.update((list) => list.filter((e) => e.tenantId !== tenantId));
  }

  /** Met à jour partiellement une entrée en base et dans le signal. */
  private async patch(id: string, changes: Partial<OutboxEntry>): Promise<void> {
    const current = this._entries().find((e) => e.id === id);
    if (!current) {
      return;
    }
    const updated: OutboxEntry = { ...current, ...changes };
    await this.db.put(OutboxService.STORE, updated);
    this._entries.update((list) => list.map((e) => (e.id === id ? updated : e)));
  }

  /**
   * Ne conserve qu'un sous-ensemble sûr de headers. Exclut tout secret :
   * jamais `Authorization` (le JWT est ré-injecté live par authInterceptor au replay).
   */
  private sanitizeHeaders(headers: Record<string, string>): Record<string, string> {
    const allowed = ['content-type', 'x-tenant-id', 'accept', 'content-language'];
    const result: Record<string, string> = {};
    for (const [key, value] of Object.entries(headers)) {
      if (allowed.includes(key.toLowerCase())) {
        result[key] = value;
      }
    }
    return result;
  }

  private extractStatus(error: unknown): number | undefined {
    if (typeof error === 'object' && error !== null && 'status' in error) {
      const status = (error as { status: unknown }).status;
      return typeof status === 'number' ? status : undefined;
    }
    return undefined;
  }

  private describeError(error: unknown, status: number | undefined): string {
    if (status !== undefined) {
      return `Échec replay (HTTP ${status})`;
    }
    if (error instanceof Error) {
      return error.message;
    }
    return 'Échec replay (réseau)';
  }
}
