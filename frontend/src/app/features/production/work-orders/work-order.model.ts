/**
 * Modèles TypeScript miroir du contrat backend nexus-production
 * (WorkOrderDto / WorkOrder.Status / WorkOrder.Priority).
 *
 * Base path REST : /api/v1/production/work-orders
 */

export type WorkOrderStatus =
  | 'PLANNED'
  | 'RELEASED'
  | 'IN_PROGRESS'
  | 'PARTIALLY_COMPLETED'
  | 'COMPLETED'
  | 'ON_HOLD'
  | 'CANCELLED';

export type WorkOrderPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';

/**
 * Forme renvoyée par GET /work-orders et /work-orders/{id} (WorkOrderDto plat).
 * Les champs reflètent exactement WorkOrderDto côté Java.
 */
export interface WorkOrder {
  id: string;
  tenantId: string;
  orderNumber: string;
  productId: string | null;
  productName: string;
  status: WorkOrderStatus;
  priority: WorkOrderPriority;
  quantityPlanned: number | null;
  quantityProduced: number | null;
  quantityRejected: number | null;
  plannedStartDate: string | null;
  plannedEndDate: string | null;
  workcenter: string | null;
  operator: string | null;
  isLate: boolean;
  yieldRate: number | null;
}

/**
 * Payload de création — aligné sur WorkOrderController.CreateWORequest.
 * Seuls productName et quantityPlanned sont @NotNull côté backend.
 */
export interface CreateWorkOrderRequest {
  productId: string | null;
  productName: string;
  quantityPlanned: number;
  plannedStartDate: string | null;
  plannedEndDate: string | null;
  priority: WorkOrderPriority;
  workcenter: string | null;
  bomId: string | null;
  routingId: string | null;
  notes: string | null;
}

/**
 * Réponse paginée — supporte le format Spring Data (Page<T>) renvoyé par
 * nexus-production ainsi que le format ApiPage (data/meta) si la gateway
 * le réécrit.
 */
export interface SpringPage<T> {
  content?: T[];
  data?: T[];
  totalPages?: number;
  totalElements?: number;
  number?: number;
  meta?: { page: number; size: number; total: number; totalPages: number };
}
