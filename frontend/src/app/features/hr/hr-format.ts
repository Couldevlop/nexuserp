/**
 * Formatage monétaire localisé pour le module RH (Ressources Humaines).
 * Calque sur sales-format : Intl.NumberFormat, locale FR par défaut (terrain CI/FR).
 * XOF (Franc CFA) sans décimales, EUR/USD avec 2 décimales.
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
    const digits = ccy === 'XOF' ? 0 : 2;
    return `${amount.toLocaleString(locale, {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    })} ${ccy}`;
  }
}

/** Libellés FR des types de contrat (enum backend Employee.ContractType). */
export const CONTRACT_TYPE_LABELS: Record<string, string> = {
  CDI: 'CDI',
  CDD: 'CDD',
  INTERIM: 'Intérim',
  INTERNSHIP: 'Stage',
  FREELANCE: 'Freelance',
};

/** Libellés FR des statuts salarié (enum backend Employee.EmployeeStatus). */
export const EMPLOYEE_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  ON_LEAVE: 'En congé',
  SUSPENDED: 'Suspendu',
  TERMINATED: 'Sorti',
};

/** Libellés FR des types de congé (enum backend Leave.LeaveType). */
export const LEAVE_TYPE_LABELS: Record<string, string> = {
  ANNUAL: 'Congés payés',
  SICK: 'Maladie',
  MATERNITY: 'Maternité',
  PATERNITY: 'Paternité',
  RTT: 'RTT',
  UNPAID: 'Sans solde',
  OTHER: 'Autre',
};

/** Libellés FR des statuts de congé (enum backend Leave.LeaveStatus). */
export const LEAVE_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Brouillon',
  SUBMITTED: 'En attente',
  APPROVED: 'Approuvé',
  REJECTED: 'Rejeté',
  CANCELLED: 'Annulé',
};
