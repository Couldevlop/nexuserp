/**
 * Helpers de présentation pour le module Production (ordres de fabrication).
 * Aucune dépendance externe — formatage purement local (terrain CI/FR).
 */

import { WorkOrderStatus, WorkOrderPriority } from './work-order.model';

/** Libellés FR des statuts d'OF (alignés sur WorkOrder.Status backend). */
export const WO_STATUS_LABELS: Record<WorkOrderStatus, string> = {
  PLANNED: 'Planifié',
  RELEASED: 'Lancé',
  IN_PROGRESS: 'En cours',
  PARTIALLY_COMPLETED: 'Partiellement terminé',
  COMPLETED: 'Terminé',
  ON_HOLD: 'En attente',
  CANCELLED: 'Annulé',
};

/** Classes de badge (nx-badge--*) par statut. */
export const WO_STATUS_BADGE: Record<WorkOrderStatus, string> = {
  PLANNED: 'nx-badge--neutral',
  RELEASED: 'nx-badge--info',
  IN_PROGRESS: 'nx-badge--warning',
  PARTIALLY_COMPLETED: 'nx-badge--warning',
  COMPLETED: 'nx-badge--success',
  ON_HOLD: 'nx-badge--neutral',
  CANCELLED: 'nx-badge--error',
};

/** Libellés FR des priorités (alignés sur WorkOrder.Priority backend). */
export const WO_PRIORITY_LABELS: Record<WorkOrderPriority, string> = {
  LOW: 'Basse',
  NORMAL: 'Normale',
  HIGH: 'Haute',
  URGENT: 'Urgente',
};

/** Classes de badge par priorité. */
export const WO_PRIORITY_BADGE: Record<WorkOrderPriority, string> = {
  LOW: 'nx-badge--neutral',
  NORMAL: 'nx-badge--info',
  HIGH: 'nx-badge--warning',
  URGENT: 'nx-badge--error',
};

export function woStatusLabel(status: string): string {
  return WO_STATUS_LABELS[status as WorkOrderStatus] ?? status;
}

export function woStatusBadge(status: string): string {
  return WO_STATUS_BADGE[status as WorkOrderStatus] ?? 'nx-badge--neutral';
}

export function woPriorityLabel(priority: string): string {
  return WO_PRIORITY_LABELS[priority as WorkOrderPriority] ?? priority;
}

export function woPriorityBadge(priority: string): string {
  return WO_PRIORITY_BADGE[priority as WorkOrderPriority] ?? 'nx-badge--neutral';
}

/**
 * Formate une quantité décimale en évitant les zéros superflus.
 * Renvoie "—" si la valeur est absente.
 */
export function formatQuantity(
  value: number | null | undefined,
  locale = 'fr-FR'
): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—';
  }
  return value.toLocaleString(locale, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  });
}

/**
 * Formate un taux (rendement) en pourcentage. Renvoie "—" si absent.
 */
export function formatRate(
  value: number | null | undefined,
  locale = 'fr-FR'
): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—';
  }
  return `${value.toLocaleString(locale, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 1,
  })} %`;
}
