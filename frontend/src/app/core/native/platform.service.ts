import { Injectable, signal } from '@angular/core';
import { getCapacitor, isNativeRuntime } from './capacitor-loader';

/**
 * Expose la nature de la plateforme d'exécution sous forme de signaux.
 *
 * Web-safe : si Capacitor n'est pas présent (navigateur, Karma), `isNative`
 * vaut `false` et `platform` vaut `'web'`. Aucun paquet natif n'est requis.
 */
@Injectable({ providedIn: 'root' })
export class PlatformService {
  private readonly _isNative = signal<boolean>(isNativeRuntime());
  /** Vrai uniquement dans le conteneur natif (Android/iOS). */
  readonly isNative = this._isNative.asReadonly();

  private readonly _platform = signal<string>(getCapacitor()?.getPlatform() ?? 'web');
  /** `'android'`, `'ios'` ou `'web'`. */
  readonly platform = this._platform.asReadonly();

  /** Pratique pour masquer/afficher des fonctionnalités mobiles dans les templates. */
  get isAndroid(): boolean {
    return this._platform() === 'android';
  }
}
