import {
  HttpEvent,
  HttpHeaders,
  HttpInterceptorFn,
  HttpResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, from, of, switchMap, tap } from 'rxjs';
import { ConnectivityService } from './connectivity.service';
import { IndexedDbService } from './indexed-db.service';
import { OutboxMethod, OutboxService } from './outbox.service';
import { AuthStateService } from '../services/auth-state.service';
import { isApiUrl, isDenied, isMutating } from './offline-policy';

/** Forme d'une réponse GET mise en cache dans IndexedDB. */
export interface CachedResponse<T = unknown> {
  /** Clé primaire = `${tenantId}::${url}`. */
  key: string;
  tenantId: string;
  url: string;
  status: number;
  body: T;
  cachedAt: number;
}

/** Statut HTTP synthétique renvoyé lorsqu'une écriture est mise en file offline. */
export const OFFLINE_QUEUED_STATUS = 202;

function cacheKey(tenantId: string, url: string): string {
  return `${tenantId}::${url}`;
}

/**
 * Intercepteur offline-first (style fonctionnel Angular 18).
 *
 * GET :
 *   - en ligne  → laisse passer, met en cache la réponse réussie (sauf denylist) ;
 *   - hors ligne→ sert la dernière réponse en cache si disponible, sinon erreur 504.
 *
 * Mutations (POST/PUT/PATCH/DELETE) :
 *   - en ligne  → laisse passer (l'écriture distante reste la source de vérité) ;
 *   - hors ligne→ met la requête en file (outbox) et renvoie une réponse 202
 *                 synthétique afin que l'UI poursuive en mode optimiste.
 *
 * Sécurité : les endpoints du denylist (auth, 2FA, paiement, RGPD...) ne sont
 * NI mis en cache NI mis en file ; ils passent toujours directement au réseau.
 * Aucun JWT n'est jamais persisté (seul un sous-ensemble de headers est stocké).
 */
export const offlineInterceptor: HttpInterceptorFn = (req, next) => {
  const connectivity = inject(ConnectivityService);
  const db = inject(IndexedDbService);
  const outbox = inject(OutboxService);
  const authState = inject(AuthStateService);

  // Hors périmètre API ou endpoint sensible → comportement réseau normal.
  if (!isApiUrl(req.url) || isDenied(req.url)) {
    return next(req);
  }

  const online = connectivity.isOnline();
  const tenantId = authState.tenantId() ?? 'anonymous';

  // ---- Lectures (GET) -------------------------------------------------------
  if (req.method === 'GET') {
    if (online) {
      return next(req).pipe(
        tap((event) => {
          if (event instanceof HttpResponse && event.status >= 200 && event.status < 300) {
            const cached: CachedResponse = {
              key: cacheKey(tenantId, req.urlWithParams),
              tenantId,
              url: req.urlWithParams,
              status: event.status,
              body: event.body,
              cachedAt: Date.now(),
            };
            // Persistance best-effort : ne jamais casser la réponse live.
            void db.put('cache', cached).catch(() => undefined);
          }
        }),
      );
    }

    // Hors ligne : servir le cache.
    return from(db.get<CachedResponse>('cache', cacheKey(tenantId, req.urlWithParams))).pipe(
      switchMap((cached): Observable<HttpEvent<unknown>> => {
        if (cached) {
          return of(
            new HttpResponse({
              status: cached.status,
              body: cached.body,
              url: req.urlWithParams,
              headers: new HttpHeaders({ 'X-Nexus-Offline-Cache': 'HIT' }),
            }),
          );
        }
        // Pas de cache → réponse réseau (échouera) pour signaler l'indisponibilité.
        return next(req);
      }),
    );
  }

  // ---- Écritures (POST/PUT/PATCH/DELETE) -----------------------------------
  if (isMutating(req.method) && !online) {
    const headers = collectHeaders(req.headers);
    const enqueue$ = from(
      outbox.enqueue({
        method: req.method as OutboxMethod,
        url: req.url,
        body: req.body,
        headers,
        tenantId,
      }),
    );

    return enqueue$.pipe(
      switchMap((entry): Observable<HttpEvent<unknown>> =>
        of(
          new HttpResponse({
            status: OFFLINE_QUEUED_STATUS,
            statusText: 'Accepted (queued offline)',
            url: req.url,
            headers: new HttpHeaders({
              'X-Nexus-Offline-Queued': 'true',
              'X-Nexus-Idempotency-Key': entry.idempotencyKey,
            }),
            body: {
              queued: true,
              idempotencyKey: entry.idempotencyKey,
              message: 'Opération enregistrée hors ligne — synchronisation au retour du réseau',
            },
          }),
        ),
      ),
    );
  }

  // En ligne (ou méthode non gérée) → passe au réseau.
  return next(req);
};

/** Extrait les headers de la requête sous forme d'objet plat (sans secret). */
function collectHeaders(headers: HttpHeaders): Record<string, string> {
  const result: Record<string, string> = {};
  for (const name of headers.keys()) {
    const value = headers.get(name);
    if (value !== null) {
      result[name] = value;
    }
  }
  return result;
}
