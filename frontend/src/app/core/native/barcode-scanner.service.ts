import { Injectable, inject } from '@angular/core';
import { PlatformService } from './platform.service';
import { loadPlugin } from './capacitor-loader';

/** Sous-ensemble typé de `@capacitor-mlkit/barcode-scanning` que nous utilisons. */
interface ScannedBarcode {
  readonly rawValue?: string;
  readonly displayValue?: string;
}
interface ScanResult {
  readonly barcodes: ReadonlyArray<ScannedBarcode>;
}
interface SupportedResult {
  readonly supported: boolean;
}
interface PermissionResult {
  readonly camera: string;
}
interface BarcodeScannerPlugin {
  isSupported(): Promise<SupportedResult>;
  checkPermissions(): Promise<PermissionResult>;
  requestPermissions(): Promise<PermissionResult>;
  scan(): Promise<ScanResult>;
}
interface BarcodeScannerModule {
  BarcodeScanner: BarcodeScannerPlugin;
}

/**
 * Erreur métier levée lorsque le scan n'est pas réalisable (web, permission refusée,
 * module ML Kit indisponible). Permet à l'appelant de réagir sans dépendre de Capacitor.
 */
export class BarcodeUnavailableError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'BarcodeUnavailableError';
  }
}

/**
 * Scanner de codes-barres / QR via la caméra (ML Kit).
 *
 * Web-safe : sur le web, `scan()` renvoie `null` (aucune capacité caméra native).
 * Sur natif sans permission ou sans support, lève `BarcodeUnavailableError`.
 * Les paquets natifs sont chargés en `import()` dynamique : le bundle web n'en dépend pas.
 */
@Injectable({ providedIn: 'root' })
export class BarcodeScannerService {
  private static readonly MODULE = '@capacitor-mlkit/barcode-scanning';
  private readonly platform = inject(PlatformService);

  /** Vrai si le scan caméra est utilisable dans le contexte courant. */
  get isAvailable(): boolean {
    return this.platform.isNative();
  }

  /**
   * Lance un scan. Résout avec la valeur lue, ou `null` si l'utilisateur annule
   * ou si la plateforme ne supporte pas le scan (web).
   * Lève `BarcodeUnavailableError` si la permission caméra est refusée.
   */
  async scan(): Promise<string | null> {
    if (!this.platform.isNative()) {
      return null;
    }

    const mod = await loadPlugin<BarcodeScannerModule>(BarcodeScannerService.MODULE);
    if (mod === null) {
      throw new BarcodeUnavailableError('Module de scan indisponible sur cet appareil.');
    }
    const scanner = mod.BarcodeScanner;

    const support = await scanner.isSupported();
    if (!support.supported) {
      throw new BarcodeUnavailableError("Le scan de code-barres n'est pas supporté.");
    }

    const granted = await this.ensurePermission(scanner);
    if (!granted) {
      throw new BarcodeUnavailableError('Permission caméra refusée.');
    }

    const result = await scanner.scan();
    const first = result.barcodes[0];
    if (!first) {
      return null;
    }
    return first.rawValue ?? first.displayValue ?? null;
  }

  private async ensurePermission(scanner: BarcodeScannerPlugin): Promise<boolean> {
    const current = await scanner.checkPermissions();
    if (current.camera === 'granted') {
      return true;
    }
    const requested = await scanner.requestPermissions();
    return requested.camera === 'granted';
  }
}
