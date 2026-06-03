/**
 * Constantes et helpers partagés du module Reporting.
 *
 * Le backend nexus-reporting expose :
 *   - POST   /api/v1/reports            → lance une génération asynchrone (202)
 *   - GET    /api/v1/reports/{id}/status → statut + downloadUrl quand prêt
 *
 * Il n'existe PAS d'endpoint de liste côté serveur. La liste des rapports
 * affichée dans l'UI est donc l'historique local des demandes effectuées
 * depuis ce navigateur (persisté en localStorage), dont le statut est
 * rafraîchi via l'endpoint /status. Aucune donnée n'est fabriquée.
 */

/** Types de rapports supportés par nexus-reporting (ReportRequest.ReportType). */
export type ReportType =
  | 'BALANCE_SHEET'
  | 'INCOME_STATEMENT'
  | 'TRIAL_BALANCE'
  | 'CASH_FLOW'
  | 'GENERAL_LEDGER'
  | 'FEC_EXPORT'
  | 'SYSCOHADA_EXPORT'
  | 'INVOICE_LIST'
  | 'STOCK_VALUATION'
  | 'PAYROLL_SUMMARY'
  | 'VAT_DECLARATION'
  | 'AGED_RECEIVABLES'
  | 'AGED_PAYABLES'
  | 'BUDGET_VARIANCE'
  | 'KPI_DASHBOARD';

/** Statuts de job renvoyés par le backend (ReportRequest.Status). */
export type ReportStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

/** Formats de sortie supportés (ReportRequest.OutputFormat). */
export type OutputFormat = 'PDF' | 'XLSX' | 'CSV' | 'JSON';

/**
 * DTO renvoyé par le backend (ReportingController.ReportDto).
 * Les champs date sont des chaînes ISO (LocalDateTime.toString()).
 */
export interface ReportDto {
  id: string;
  type: string;
  status: string;
  downloadUrl: string | null;
  errorMessage: string | null;
  requestedAt: string | null;
  completedAt: string | null;
}

/** Libellés FR des types de rapports. */
export const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  BALANCE_SHEET: 'Bilan',
  INCOME_STATEMENT: 'Compte de résultat',
  TRIAL_BALANCE: 'Balance générale',
  CASH_FLOW: 'Tableau de flux de trésorerie',
  GENERAL_LEDGER: 'Grand livre',
  FEC_EXPORT: 'FEC (France)',
  SYSCOHADA_EXPORT: 'États SYSCOHADA (CI/UEMOA)',
  INVOICE_LIST: 'Liste des factures',
  STOCK_VALUATION: 'Valorisation des stocks',
  PAYROLL_SUMMARY: 'Récapitulatif de paie',
  VAT_DECLARATION: 'Déclaration TVA',
  AGED_RECEIVABLES: 'Balance âgée clients',
  AGED_PAYABLES: 'Balance âgée fournisseurs',
  BUDGET_VARIANCE: 'Réalisé vs Budget',
  KPI_DASHBOARD: 'KPI synthétiques',
};

/** Libellés FR des statuts. */
export const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
  PENDING: 'En attente',
  PROCESSING: 'En cours',
  COMPLETED: 'Disponible',
  FAILED: 'Échec',
};

/** Classe de badge nx-badge associée à chaque statut. */
export const REPORT_STATUS_BADGE: Record<ReportStatus, string> = {
  PENDING: 'nx-badge--neutral',
  PROCESSING: 'nx-badge--info',
  COMPLETED: 'nx-badge--success',
  FAILED: 'nx-badge--error',
};

/** Formats proposés à l'utilisateur, restreints selon le type de rapport. */
export const OUTPUT_FORMAT_LABELS: Record<OutputFormat, string> = {
  PDF: 'PDF',
  XLSX: 'Excel (XLSX)',
  CSV: 'CSV',
  JSON: 'JSON',
};

/** Renvoie le libellé d'un type, en repli sur la valeur brute si inconnue. */
export function reportTypeLabel(type: string): string {
  return REPORT_TYPE_LABELS[type as ReportType] ?? type;
}

/** Renvoie le libellé d'un statut, repli sur la valeur brute si inconnue. */
export function reportStatusLabel(status: string): string {
  return REPORT_STATUS_LABELS[status as ReportStatus] ?? status;
}

/** Renvoie la classe de badge d'un statut, repli neutre si inconnu. */
export function reportStatusBadge(status: string): string {
  return REPORT_STATUS_BADGE[status as ReportStatus] ?? 'nx-badge--neutral';
}

/** Vrai si le statut correspond à un rapport téléchargeable. */
export function isReady(status: string): boolean {
  return status === 'COMPLETED';
}

/** Vrai si le job est encore en cours (à poller). */
export function isPending(status: string): boolean {
  return status === 'PENDING' || status === 'PROCESSING';
}
