import { Injectable } from '@angular/core';

/**
 * Noms des object stores utilisés par le sous-système offline.
 * - `outbox` : file d'attente des requêtes mutantes (POST/PUT/PATCH/DELETE) émises hors ligne.
 * - `cache`  : dernières réponses GET réussies, servies en lecture hors ligne.
 */
export type ObjectStoreName = 'outbox' | 'cache';

/**
 * Wrapper IndexedDB typé et minimaliste (Promise-based).
 *
 * Objectif : éviter toute dépendance lourde (pas de `idb`) tout en restant
 * entièrement typé (aucun `any`). Les deux object stores utilisent une clé
 * primaire explicite (`keyPath: 'id'` pour l'outbox, `keyPath: 'key'` pour le cache).
 *
 * Ce service est volontairement injectable afin que les services consommateurs
 * (OutboxService, OfflineInterceptor) puissent être testés en le stubbant.
 */
@Injectable({ providedIn: 'root' })
export class IndexedDbService {

  private static readonly DB_NAME = 'nexuserp-offline';
  private static readonly DB_VERSION = 1;

  /** Promesse mémoïsée d'ouverture de la base (singleton). */
  private dbPromise: Promise<IDBDatabase> | null = null;

  /**
   * Ouvre (et crée si besoin) la base IndexedDB.
   * L'appel est idempotent : la même promesse est réutilisée.
   */
  openDB(): Promise<IDBDatabase> {
    if (this.dbPromise) {
      return this.dbPromise;
    }

    this.dbPromise = new Promise<IDBDatabase>((resolve, reject) => {
      if (typeof indexedDB === 'undefined') {
        reject(new Error('IndexedDB indisponible dans cet environnement'));
        return;
      }

      const request = indexedDB.open(IndexedDbService.DB_NAME, IndexedDbService.DB_VERSION);

      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains('outbox')) {
          // L'outbox indexe aussi par tenant pour permettre clearForTenant().
          const outbox = db.createObjectStore('outbox', { keyPath: 'id' });
          outbox.createIndex('by_tenant', 'tenantId', { unique: false });
          outbox.createIndex('by_status', 'status', { unique: false });
        }
        if (!db.objectStoreNames.contains('cache')) {
          const cache = db.createObjectStore('cache', { keyPath: 'key' });
          cache.createIndex('by_tenant', 'tenantId', { unique: false });
        }
      };

      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error ?? new Error('Échec ouverture IndexedDB'));
    });

    return this.dbPromise;
  }

  /** Lit une valeur par clé. Retourne `undefined` si absente. */
  async get<T>(store: ObjectStoreName, key: IDBValidKey): Promise<T | undefined> {
    const db = await this.openDB();
    return this.runRequest<T | undefined>(db, store, 'readonly', (os) => os.get(key));
  }

  /** Récupère toutes les valeurs d'un store. */
  async getAll<T>(store: ObjectStoreName): Promise<T[]> {
    const db = await this.openDB();
    const result = await this.runRequest<T[]>(
      db,
      store,
      'readonly',
      (os) => os.getAll() as IDBRequest<T[]>,
    );
    return result ?? [];
  }

  /** Insère ou remplace une valeur (clé portée par l'objet via keyPath). */
  async put<T>(store: ObjectStoreName, value: T): Promise<void> {
    const db = await this.openDB();
    await this.runRequest<IDBValidKey>(db, store, 'readwrite', (os) => os.put(value));
  }

  /** Supprime une valeur par clé. */
  async delete(store: ObjectStoreName, key: IDBValidKey): Promise<void> {
    const db = await this.openDB();
    await this.runRequest<undefined>(db, store, 'readwrite', (os) => os.delete(key));
  }

  /** Vide entièrement un store. */
  async clear(store: ObjectStoreName): Promise<void> {
    const db = await this.openDB();
    await this.runRequest<undefined>(db, store, 'readwrite', (os) => os.clear());
  }

  /**
   * Supprime toutes les entrées d'un store appartenant à un tenant donné.
   * Sécurité A04 (isolation tenant) : utilisé au logout pour ne pas laisser
   * fuiter de données d'un tenant à l'autre sur un poste partagé.
   */
  async deleteByTenant(store: ObjectStoreName, tenantId: string): Promise<void> {
    const db = await this.openDB();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(store, 'readwrite');
      const os = tx.objectStore(store);
      const index = os.index('by_tenant');
      const cursorReq = index.openCursor(IDBKeyRange.only(tenantId));
      cursorReq.onsuccess = () => {
        const cursor = cursorReq.result;
        if (cursor) {
          cursor.delete();
          cursor.continue();
        }
      };
      cursorReq.onerror = () => reject(cursorReq.error ?? new Error('Échec deleteByTenant'));
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error ?? new Error('Échec transaction deleteByTenant'));
    });
  }

  /** Exécute une requête IndexedDB dans une transaction et renvoie son résultat typé. */
  private runRequest<R>(
    db: IDBDatabase,
    store: ObjectStoreName,
    mode: IDBTransactionMode,
    op: (os: IDBObjectStore) => IDBRequest<R>,
  ): Promise<R> {
    return new Promise<R>((resolve, reject) => {
      const tx = db.transaction(store, mode);
      const os = tx.objectStore(store);
      const request = op(os);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error ?? new Error(`Échec opération sur '${store}'`));
    });
  }
}
