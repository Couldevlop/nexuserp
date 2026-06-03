/**
 * Sous-système offline-first NexusERP.
 *
 * Point d'entrée unique : importer depuis `core/offline` plutôt que par fichier.
 *
 * Wiring requis (voir app.config.ts) :
 *   provideHttpClient(withInterceptors([..., offlineInterceptor]))
 * et appeler `SyncService.init()` au démarrage de l'application.
 */
export { ConnectivityService } from './connectivity.service';
export { IndexedDbService } from './indexed-db.service';
export type { ObjectStoreName } from './indexed-db.service';
export { OutboxService } from './outbox.service';
export type {
  OutboxEntry,
  OutboxStatus,
  OutboxMethod,
  NewOutboxRequest,
} from './outbox.service';
export { SyncService } from './sync.service';
export type { SyncStatus } from './sync.service';
export { offlineInterceptor, OFFLINE_QUEUED_STATUS } from './offline.interceptor';
export type { CachedResponse } from './offline.interceptor';
export {
  OFFLINE_DENYLIST,
  isDenied,
  isApiUrl,
  isMutating,
} from './offline-policy';
