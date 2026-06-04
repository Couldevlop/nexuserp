/**
 * Wrappers natifs Capacitor — *web-safe*.
 *
 * Tous ces services dégradent gracieusement sur le web : aucun paquet `@capacitor/*`
 * n'est requis pour le build/test web (chargement par `import()` dynamique).
 *
 * Point d'entrée unique : importer depuis `core/native`.
 */
export { PlatformService } from './platform.service';
export { BarcodeScannerService, BarcodeUnavailableError } from './barcode-scanner.service';
export { NativeNetworkService } from './native-network.service';
export {
  getCapacitor,
  isNativeRuntime,
  loadPlugin,
} from './capacitor-loader';
export type { CapacitorRuntime } from './capacitor-loader';
