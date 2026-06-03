import { Injectable, OnDestroy, signal } from '@angular/core';

/**
 * Service de détection de connectivité réseau.
 *
 * Source de vérité combinée :
 *  1. `navigator.onLine` (état initial),
 *  2. les événements `window` `online` / `offline`,
 *  3. un heartbeat optionnel (ping léger) qui détecte les "faux online"
 *     fréquents en zone à connectivité intermittente (CI / UEMOA) :
 *     l'OS croit être connecté (WiFi/captif) mais aucune route ne mène au backend.
 *
 * Expose un signal `isOnline`. Teardown propre via OnDestroy (retrait des
 * listeners + arrêt du heartbeat) pour éviter les fuites en environnement de test.
 */
@Injectable({ providedIn: 'root' })
export class ConnectivityService implements OnDestroy {

  /** Endpoint léger pour le heartbeat. Spring Actuator renvoie un petit JSON. */
  private static readonly HEARTBEAT_URL = '/actuator/health';
  private static readonly HEARTBEAT_INTERVAL_MS = 30_000;
  private static readonly HEARTBEAT_TIMEOUT_MS = 5_000;

  private readonly _isOnline = signal<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true,
  );
  /** Vrai si le navigateur ET (si activé) le backend sont joignables. */
  readonly isOnline = this._isOnline.asReadonly();

  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private readonly onOnline = (): void => this._isOnline.set(true);
  private readonly onOffline = (): void => this._isOnline.set(false);

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', this.onOnline);
      window.addEventListener('offline', this.onOffline);
    }
  }

  /**
   * Active le heartbeat périodique. Optionnel : ne l'appeler que lorsque
   * l'application est démarrée et authentifiée (sinon bruit réseau inutile).
   */
  startHeartbeat(): void {
    if (this.heartbeatTimer !== null || typeof window === 'undefined') {
      return;
    }
    void this.ping();
    this.heartbeatTimer = setInterval(() => void this.ping(), ConnectivityService.HEARTBEAT_INTERVAL_MS);
  }

  stopHeartbeat(): void {
    if (this.heartbeatTimer !== null) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  /**
   * Effectue un ping et met à jour le signal. N'écrase jamais un état "offline"
   * imposé par le navigateur : si `navigator.onLine` est faux, on reste offline.
   */
  private async ping(): Promise<void> {
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      this._isOnline.set(false);
      return;
    }
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), ConnectivityService.HEARTBEAT_TIMEOUT_MS);
    try {
      // `fetch` plutôt que HttpClient pour ne pas traverser les intercepteurs
      // (auth/tenant/offline) — un ping ne doit jamais être mis en file d'attente.
      await fetch(ConnectivityService.HEARTBEAT_URL, {
        method: 'GET',
        cache: 'no-store',
        signal: controller.signal,
      });
      this._isOnline.set(true);
    } catch {
      this._isOnline.set(false);
    } finally {
      clearTimeout(timeout);
    }
  }

  ngOnDestroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('online', this.onOnline);
      window.removeEventListener('offline', this.onOffline);
    }
    this.stopHeartbeat();
  }
}
