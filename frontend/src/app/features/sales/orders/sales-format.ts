/**
 * Formatage monétaire localisé pour le module Ventes.
 * Utilise Intl.NumberFormat avec la locale FR par défaut (terrain CI/FR).
 * XOF est affiché sans décimales (Franc CFA), EUR/USD avec 2 décimales.
 */
export function formatMoney(
  amount: number | null | undefined,
  currency: string | null | undefined,
  locale = 'fr-FR'
): string {
  if (amount === null || amount === undefined || Number.isNaN(amount)) {
    return '—';
  }
  const ccy = (currency ?? 'EUR').toUpperCase();
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: ccy,
      minimumFractionDigits: ccy === 'XOF' ? 0 : 2,
      maximumFractionDigits: ccy === 'XOF' ? 0 : 2,
    }).format(amount);
  } catch {
    // Devise inconnue d'Intl : repli simple
    const digits = ccy === 'XOF' ? 0 : 2;
    return `${amount.toLocaleString(locale, {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    })} ${ccy}`;
  }
}
