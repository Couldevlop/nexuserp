import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { PlatformService } from './platform.service';
import { loadPlugin } from './capacitor-loader';

/** Sous-ensemble typé de `@capacitor/network`. */
interface ConnectionStatus {
  readonly connected: boolean;
  readonly connectionType?: string;
}
interface PluginListenerHandle {
  remove(): Promise<void>;
}
interface NetworkPlugin {
  getStatus(): Promise<ConnectionStatus>;
  addListener(
    event: 'networkStatusChange',
    cb: (status: ConnectionStatus) => void,
  ): Promise<PluginListenerHandle>;
}
interface NetworkModule {
  Network: NetworkPlugin;
}

/**
 * Pont entre `@capacitor/network` et le pattern signal de `ConnectivityService`.
 *
 * Sur natif, l'API Network d'Android est plus fiable que `navigator.onLine`
 * (qui reste souvent `true` sur un WiFi captif sans route). On expose ici un
 * signal `isOnline` alimenté par les évènements natifs.
 *
 * Web-safe : si Capacitor/Network n'est pas présent, `init()` est un no-op et le
 * signal conserve sa valeur initiale. Cette classe est *optionnelle* : elle ne
 * remplace pas `ConnectivityService`, elle peut le renforcer (voir `bridgeTo`).
 */
@Injectable({ providedIn: 'root' })
export class NativeNetworkService implements OnDestroy {
  private static readonly MODULE = '@capacitor/network';
  private readonly platform = inject(PlatformService);

  private readonly _isOnline = signal<boolean>(true);
  readonly isOnline = this._isOnline.asReadonly();

  private listener: PluginListenerHandle | null = null;
  private bridge: ((online: boolean) => void) | null = null;

  /**
   * Initialise l'écoute native. No-op sur le web. Idempotent.
   * @param onChange callback optionnel appelé à chaque changement (ex. pour
   *        propager vers `ConnectivityService` ou déclencher une synchro).
   */
  async init(onChange?: (online: boolean) => void): Promise<void> {
    if (onChange) {
      this.bridge = onChange;
    }
    if (!this.platform.isNative() || this.listener !== null) {
      return;
    }

    const mod = await loadPlugin<NetworkModule>(NativeNetworkService.MODULE);
    if (mod === null) {
      return;
    }
    const net = mod.Network;

    const initial = await net.getStatus();
    this.apply(initial.connected);

    this.listener = await net.addListener('networkStatusChange', (status) => {
      this.apply(status.connected);
    });
  }

  private apply(online: boolean): void {
    this._isOnline.set(online);
    this.bridge?.(online);
  }

  ngOnDestroy(): void {
    void this.listener?.remove();
    this.listener = null;
    this.bridge = null;
  }
}
