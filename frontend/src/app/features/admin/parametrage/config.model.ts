/**
 * Modèle du domaine Paramétrage (service nexus-config).
 * Contrat REST : base `/api/v1/config`.
 */

export type ConfigCategory =
  | 'PAYMENT'
  | 'NOTIFICATION'
  | 'TAX'
  | 'GENERAL'
  | 'AI'
  | 'SECURITY';

export type ConfigValueType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON' | 'SECRET';

/**
 * Paramètre tel que renvoyé par le backend.
 * Pour les secrets, `value` est MASQUÉ ("••••••") et `set` indique
 * si une valeur existe réellement côté serveur.
 */
export interface ConfigParam {
  key: string;
  category: ConfigCategory;
  valueType: ConfigValueType;
  secret: boolean;
  set: boolean;
  value: string | null;
  description?: string | null;
  updatedAt?: string | null;
}

/**
 * Payload d'upsert (PUT /api/v1/config/{key}).
 * Pour les secrets : envoyer une nouvelle valeur la remplace ;
 * envoyer une valeur vide conserve l'existante (write-only).
 */
export interface ConfigUpsertRequest {
  value: string;
  type: ConfigValueType;
  category: ConfigCategory;
  secret: boolean;
  description?: string | null;
}

export const MASKED_PLACEHOLDER = '••••••';

export const CONFIG_CATEGORIES: ReadonlyArray<{ id: ConfigCategory; label: string; icon: string }> = [
  { id: 'PAYMENT', label: 'Paiement', icon: '💳' },
  { id: 'NOTIFICATION', label: 'Notifications', icon: '✉️' },
  { id: 'TAX', label: 'Fiscalité', icon: '🧾' },
  { id: 'GENERAL', label: 'Général', icon: '⚙️' },
  { id: 'AI', label: 'IA', icon: '🤖' },
  { id: 'SECURITY', label: 'Sécurité', icon: '🔒' },
];

export function categoryLabel(category: ConfigCategory): string {
  return CONFIG_CATEGORIES.find((c) => c.id === category)?.label ?? category;
}

export function valueTypeLabel(type: ConfigValueType): string {
  switch (type) {
    case 'STRING':
      return 'Texte';
    case 'NUMBER':
      return 'Nombre';
    case 'BOOLEAN':
      return 'Booléen';
    case 'JSON':
      return 'JSON';
    case 'SECRET':
      return 'Secret';
  }
}
