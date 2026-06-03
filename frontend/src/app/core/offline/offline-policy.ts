/**
 * Politique de sécurité du sous-système offline (OWASP).
 *
 * A01/A02 — Contrôle d'accès & cryptographie :
 *   Les endpoints sensibles (authentification, 2FA, secrets de paiement, RGPD,
 *   exports) ne doivent JAMAIS être mis en cache ni mis en file d'attente offline.
 *   Mettre en cache une réponse d'auth = fuite de données sensibles sur le disque.
 *   Rejouer une opération de paiement sans le contexte sécurité live = risque.
 *
 * A08 — Intégrité :
 *   Les requêtes mutantes rejouées portent un `idempotencyKey` (header
 *   `Idempotency-Key`) pour permettre au backend de dédupliquer (cf. design
 *   nexus-payment). On ne met donc JAMAIS en file un endpoint non idempotent
 *   côté métier qui ne sait pas gérer cette clé : ici on s'appuie sur le denylist.
 */

/**
 * Motifs d'URL (sous-chaînes) jamais mis en cache ni en file d'attente offline.
 * Comparaison insensible à la casse sur le path de l'URL.
 */
export const OFFLINE_DENYLIST: readonly string[] = [
  '/auth',            // login, logout, refresh token
  '/oauth',           // flux OIDC / token endpoint
  '/token',           // endpoints token Keycloak
  '/2fa',             // setup / verify / recovery 2FA
  '/two-factor',
  '/gdpr',            // droit à l'oubli, export données personnelles (RGPD)
  '/payment',         // secrets / initialisation de paiement
  '/payments',
  '/secrets',
  '/realms',          // Keycloak realms
  '.well-known',      // discovery OIDC
];

/** Vrai si l'URL correspond à un endpoint interdit de cache/queue. */
export function isDenied(url: string): boolean {
  const lower = url.toLowerCase();
  return OFFLINE_DENYLIST.some((pattern) => lower.includes(pattern));
}

/**
 * Vrai si l'URL est une requête API métier candidate au cache/queue offline.
 * On ne traite que les endpoints applicatifs (`/api/`), pas les assets ni i18n.
 */
export function isApiUrl(url: string): boolean {
  return url.includes('/api/');
}

/** Vrai si la méthode HTTP est mutante (doit passer par l'outbox hors ligne). */
export function isMutating(method: string): boolean {
  const m = method.toUpperCase();
  return m === 'POST' || m === 'PUT' || m === 'PATCH' || m === 'DELETE';
}
