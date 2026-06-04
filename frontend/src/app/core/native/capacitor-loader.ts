/**
 * Chargeur Capacitor *web-safe*.
 *
 * Objectif : ne JAMAIS exiger les paquets Capacitor au build/test web.
 * Les paquets `@capacitor/*` sont des `optional` côté code : ils ne sont
 * présents que dans le bundle natif (Android). Sur le web (et en test Karma),
 * ces imports sont remplacés par des fallbacks gracieux.
 *
 * Stratégie :
 *  - on détecte la présence du runtime Capacitor via la globale `window.Capacitor`,
 *    injectée uniquement par le WebView natif ;
 *  - les plugins sont chargés via `import()` dynamique, encapsulé dans un try/catch :
 *    si le paquet n'est pas résolvable (web), on renvoie `null` proprement.
 *
 * Aucune dépendance de type sur les paquets natifs (zéro `import type` depuis
 * `@capacitor/*`) : on déclare ici des interfaces minimales et autonomes.
 */

/** Sous-ensemble de l'API runtime `@capacitor/core` que nous utilisons. */
export interface CapacitorRuntime {
  isNativePlatform(): boolean;
  getPlatform(): string;
}

declare global {
  interface Window {
    Capacitor?: CapacitorRuntime;
  }
}

/** Renvoie le runtime Capacitor s'il est présent (WebView natif), sinon `null`. */
export function getCapacitor(): CapacitorRuntime | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const cap = window.Capacitor;
  return cap && typeof cap.isNativePlatform === 'function' ? cap : null;
}

/** Vrai uniquement à l'intérieur d'un conteneur natif Capacitor. */
export function isNativeRuntime(): boolean {
  const cap = getCapacitor();
  return cap !== null && cap.isNativePlatform();
}

/**
 * Charge dynamiquement un module de plugin. Renvoie `null` si le paquet n'est
 * pas résolvable (cas du web/test où les `@capacitor/*` ne sont pas bundlés).
 *
 * Le `specifier` est passé via une variable pour empêcher le bundler Angular
 * d'essayer de résoudre statiquement ces paquets optionnels au build.
 */
export async function loadPlugin<T>(specifier: string): Promise<T | null> {
  try {
    const dynamicImport = new Function('s', 'return import(s);') as (s: string) => Promise<unknown>;
    const mod = (await dynamicImport(specifier)) as T;
    return mod ?? null;
  } catch {
    return null;
  }
}
