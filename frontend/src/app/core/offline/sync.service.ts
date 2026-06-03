import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { ConnectivityService } from './connectivity.service';
import { OutboxService } from './outbox.service';
import { IndexedDbService } from './indexed-db.service';
import { NotificationService } from '../services/notification.service';

/** État global de synchronisation, consommable par l'UI. */
export type SyncStatus = 'idle' | 'syncing' | 'error';

/**
 * Orchestrateur de synchronisation offline → online.
 *
 * Responsabilités :
 *  - observer `ConnectivityService.isOnline` ;
 *  - au passage offline → online, déclencher `OutboxService.sync()` ;
 *  - exposer un signal `status` (idle / syncing / error) et `lastSyncedAt` ;
 *  - fournir le nettoyage au logout (`clearAll` / `clearForTenant`) — A04.
 *
 * `init()` doit être appelé une fois au démarrage de l'application (ex: depuis
 * le shell ou un APP_INITIALIZER) pour hydrater l'outbox et armer l'effet.
 */
@Injectable({ providedIn: 'root' })
export class SyncService {

  private readonly connectivity = inject(ConnectivityService);
  private readonly outbox = inject(OutboxService);
  private readonly db = inject(IndexedDbService);
  private readonly notif = inject(NotificationService);

  private readonly _status = signal<SyncStatus>('idle');
  readonly status = this._status.asReadonly();

  private readonly _lastSyncedAt = signal<number | null>(null);
  readonly lastSyncedAt = this._lastSyncedAt.asReadonly();

  /** Nombre d'opérations encore en attente (relayé depuis l'outbox). */
  readonly pendingCount = computed(() => this.outbox.pendingCount());
  readonly conflicts = computed(() => this.outbox.conflicts());

  private wasOnline = this.connectivity.isOnline();
  private initialized = false;

  constructor() {
    // Effet réactif : déclenche une sync à chaque transition offline -> online.
    effect(() => {
      const online = this.connectivity.isOnline();
      const transitionedToOnline = online && !this.wasOnline;
      this.wasOnline = online;
      if (this.initialized && transitionedToOnline && this.outbox.pendingCount() > 0) {
        void this.runSync();
      }
    });
  }

  /** Hydrate l'outbox, démarre le heartbeat, et tente une sync si déjà en ligne. */
  async init(): Promise<void> {
    this.initialized = true;
    await this.outbox.hydrate();
    this.connectivity.startHeartbeat();
    if (this.connectivity.isOnline() && this.outbox.pendingCount() > 0) {
      await this.runSync();
    }
  }

  /** Lance manuellement la synchronisation (ex: bouton "Réessayer"). */
  async syncNow(): Promise<void> {
    if (!this.connectivity.isOnline()) {
      this.notif.warning('Toujours hors ligne — synchronisation impossible');
      return;
    }
    await this.runSync();
  }

  private async runSync(): Promise<void> {
    if (this._status() === 'syncing') {
      return;
    }
    this._status.set('syncing');
    try {
      await this.outbox.sync();
      this._lastSyncedAt.set(Date.now());
      const conflicts = this.outbox.conflicts().length;
      if (conflicts > 0) {
        this._status.set('error');
        this.notif.warning(
          `${conflicts} opération(s) en conflit — vérification manuelle requise`,
        );
      } else if (this.outbox.pendingCount() > 0) {
        // Des entrées restent (FAILED/PENDING) : un retry ultérieur les reprendra.
        this._status.set('error');
      } else {
        this._status.set('idle');
        this.notif.success('Données synchronisées');
      }
    } catch {
      this._status.set('error');
      this.notif.error('Échec de la synchronisation');
    }
  }

  /** Purge complète outbox + cache (logout). Sécurité A04. */
  async clearAll(): Promise<void> {
    await this.outbox.clearAll();
    await this.db.clear('cache');
    this._status.set('idle');
    this._lastSyncedAt.set(null);
  }

  /** Purge des données offline d'un tenant (changement de tenant). A04. */
  async clearForTenant(tenantId: string): Promise<void> {
    await this.outbox.clearForTenant(tenantId);
    await this.db.deleteByTenant('cache', tenantId);
  }
}
