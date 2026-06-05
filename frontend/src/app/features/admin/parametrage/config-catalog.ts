import { ConfigCategory, ConfigValueType } from './config.model';

/**
 * Catalogue curaté des paramètres connus (providers, clés API, réglages).
 *
 * Objectif : présenter à l'administrateur des libellés conviviaux et le
 * drapeau `secret` correct, MÊME si le paramètre n'est pas encore défini
 * côté backend. Les entrées non encore renseignées sont affichées comme
 * « non défini » et peuvent être remplies depuis l'UI.
 *
 * Pour ÉTENDRE : il suffit d'ajouter une entrée à `CONFIG_CATALOG`.
 */
export interface CatalogEntry {
  /** Clé technique (telle que stockée par nexus-config). */
  readonly key: string;
  /** Libellé convivial affiché à l'admin. */
  readonly label: string;
  readonly category: ConfigCategory;
  readonly valueType: ConfigValueType;
  /** true = valeur sensible, jamais affichée en clair (write-only). */
  readonly secret: boolean;
  /** Aide contextuelle (placeholder, exemple…). */
  readonly description?: string;
  /** Regroupement visuel intra-catégorie (ex. "Orange Money"). */
  readonly group?: string;
}

export const CONFIG_CATALOG: readonly CatalogEntry[] = [
  // ─── PAIEMENT — Mobile Money / agrégateurs (UEMOA) ────────────────────────
  { key: 'payment.orange.client-id', label: 'Orange Money — Client ID', category: 'PAYMENT', valueType: 'STRING', secret: false, group: 'Orange Money', description: 'Identifiant client de l\'API Orange Money.' },
  { key: 'payment.orange.client-secret', label: 'Orange Money — Client Secret', category: 'PAYMENT', valueType: 'SECRET', secret: true, group: 'Orange Money' },
  { key: 'payment.orange.merchant-key', label: 'Orange Money — Merchant Key', category: 'PAYMENT', valueType: 'SECRET', secret: true, group: 'Orange Money' },

  { key: 'payment.wave.api-key', label: 'Wave — Clé API', category: 'PAYMENT', valueType: 'SECRET', secret: true, group: 'Wave' },

  { key: 'payment.mtn.api-user', label: 'MTN MoMo — API User', category: 'PAYMENT', valueType: 'STRING', secret: false, group: 'MTN MoMo' },
  { key: 'payment.mtn.api-key', label: 'MTN MoMo — Clé API', category: 'PAYMENT', valueType: 'SECRET', secret: true, group: 'MTN MoMo' },
  { key: 'payment.mtn.subscription-key', label: 'MTN MoMo — Subscription Key', category: 'PAYMENT', valueType: 'SECRET', secret: true, group: 'MTN MoMo' },

  // NB : les secrets webhook (payment.*.webhook-secret) restent gérés par variables
  // d'environnement — la vérification HMAC des callbacks s'exécute SANS TenantContext
  // (endpoint public serveur-à-serveur), donc hors de portée du store par tenant.

  { key: 'payment.moov.base-url', label: 'Moov Money — URL de base', category: 'PAYMENT', valueType: 'STRING', secret: false, group: 'Moov Money', description: 'Ex. https://api.moov-africa.ci' },
  { key: 'payment.moov.api-key', label: 'Moov Money — Clé API', category: 'PAYMENT', valueType: 'SECRET', secret: true, group: 'Moov Money' },

  // ─── NOTIFICATION — Email / SMS / WhatsApp ────────────────────────────────
  { key: 'notification.smtp.username', label: 'SMTP — Utilisateur', category: 'NOTIFICATION', valueType: 'STRING', secret: false, group: 'Email (SMTP Google)', description: 'Adresse Gmail expéditrice.' },
  { key: 'notification.smtp.app-password', label: 'SMTP — Mot de passe d\'application', category: 'NOTIFICATION', valueType: 'SECRET', secret: true, group: 'Email (SMTP Google)', description: 'Google App Password (jamais le mot de passe du compte).' },

  { key: 'notification.sms.api-key', label: 'SMS — Clé API', category: 'NOTIFICATION', valueType: 'SECRET', secret: true, group: 'SMS' },
  { key: 'notification.sms.sender', label: 'SMS — Expéditeur', category: 'NOTIFICATION', valueType: 'STRING', secret: false, group: 'SMS', description: 'Nom court affiché comme expéditeur.' },

  { key: 'notification.whatsapp.token', label: 'WhatsApp — Token', category: 'NOTIFICATION', valueType: 'SECRET', secret: true, group: 'WhatsApp' },
  { key: 'notification.whatsapp.phone-id', label: 'WhatsApp — Phone Number ID', category: 'NOTIFICATION', valueType: 'STRING', secret: false, group: 'WhatsApp' },

  // ─── FISCALITÉ ────────────────────────────────────────────────────────────
  { key: 'tax.vat.standard-rate', label: 'TVA — Taux standard (%)', category: 'TAX', valueType: 'NUMBER', secret: false, group: 'TVA', description: 'Ex. 18 (CI) ou 20 (FR).' },
  { key: 'tax.vat.reduced-rate', label: 'TVA — Taux réduit (%)', category: 'TAX', valueType: 'NUMBER', secret: false, group: 'TVA' },
  { key: 'tax.vat.on-payment', label: 'TVA sur encaissements', category: 'TAX', valueType: 'BOOLEAN', secret: false, group: 'TVA', description: 'Sinon TVA sur les débits.' },

  // ─── GÉNÉRAL ──────────────────────────────────────────────────────────────
  { key: 'general.company.name', label: 'Raison sociale', category: 'GENERAL', valueType: 'STRING', secret: false, group: 'Société' },
  { key: 'general.company.country', label: 'Pays', category: 'GENERAL', valueType: 'STRING', secret: false, group: 'Société', description: 'Code ISO (FR, CI, SN…).' },
  { key: 'general.default-currency', label: 'Devise par défaut', category: 'GENERAL', valueType: 'STRING', secret: false, group: 'Société', description: 'XOF, EUR, USD…' },
  { key: 'general.default-locale', label: 'Langue par défaut', category: 'GENERAL', valueType: 'STRING', secret: false, group: 'Société', description: 'fr-FR, fr-CI, en-US…' },

  // ─── IA ───────────────────────────────────────────────────────────────────
  { key: 'ai.enabled', label: 'IA activée', category: 'AI', valueType: 'BOOLEAN', secret: false, group: 'Général', description: 'Bascule globale des fonctionnalités IA.' },
  { key: 'ai.anthropic.api-key', label: 'Anthropic — Clé API', category: 'AI', valueType: 'SECRET', secret: true, group: 'Anthropic Claude' },

  // ─── SÉCURITÉ ─────────────────────────────────────────────────────────────
  { key: 'security.2fa.required', label: '2FA obligatoire (rôles sensibles)', category: 'SECURITY', valueType: 'BOOLEAN', secret: false, group: 'Authentification' },
  { key: 'security.session.access-token-ttl', label: 'TTL access token (heures)', category: 'SECURITY', valueType: 'NUMBER', secret: false, group: 'Sessions' },
];

export function catalogForCategory(category: ConfigCategory): readonly CatalogEntry[] {
  return CONFIG_CATALOG.filter((e) => e.category === category);
}

export function catalogEntry(key: string): CatalogEntry | undefined {
  return CONFIG_CATALOG.find((e) => e.key === key);
}
