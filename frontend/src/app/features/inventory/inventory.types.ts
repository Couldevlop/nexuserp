/**
 * Types partagés du module Stocks (Inventory).
 * Alignés sur le contrat backend nexus-inventory (ProductDto, ProductController).
 */

export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED';

export type StockValuationMethod =
  | 'STANDARD'
  | 'FIFO'
  | 'LIFO'
  | 'PMP_PERIOD'
  | 'PMP_REALTIME';

/** Type de mouvement de stock (UI). Le backend expose receive (IN) / issue (OUT) / adjust. */
export type MovementType = 'IN' | 'OUT' | 'TRANSFER' | 'ADJUSTMENT';

/** Mirroir exact du ProductDto backend. */
export interface ProductDto {
  id: string;
  tenantId: string;
  productCode: string;
  name: string;
  description: string | null;
  category: string | null;
  unit: string;
  status: ProductStatus;
  quantityOnHand: number;
  quantityReserved: number;
  availableQuantity: number;
  reorderPoint: number;
  reorderQuantity: number;
  safetyStock: number;
  valuationMethod: StockValuationMethod;
  averageCostAmount: number | null;
  averageCostCurrency: string | null;
  warehouseId: string | null;
  warehouseLocation: string | null;
  serialTracked: boolean;
  lotTracked: boolean;
  expiryTracked: boolean;
  needsReorder: boolean;
}

/**
 * Forme renvoyée par Spring Data `Page<T>` (sérialisation par défaut).
 * Le backend nexus-inventory renvoie cette forme, PAS l'enveloppe finance `ApiPage`.
 */
export interface SpringPage<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

/**
 * Lot / numéro de série avec péremption.
 * Endpoint non encore exposé par le backend → dégradé gracieusement (liste vide).
 */
export interface StockLot {
  id: string;
  lotNumber: string | null;
  serialNumber: string | null;
  quantity: number;
  warehouseId: string | null;
  warehouseLocation: string | null;
  expiryDate: string | null;
}

/**
 * Ventilation du stock par entrepôt/emplacement.
 * Endpoint non encore exposé → dégradé gracieusement.
 */
export interface WarehouseStock {
  warehouseId: string;
  warehouseLocation: string | null;
  quantity: number;
}

/**
 * Ligne d'historique de mouvement.
 * Endpoint liste non encore exposé → dégradé gracieusement.
 */
export interface StockMovement {
  id: string;
  productId: string;
  productCode: string;
  productName: string;
  type: MovementType;
  quantity: number;
  unit: string;
  reference: string | null;
  reason: string | null;
  warehouseFrom: string | null;
  warehouseTo: string | null;
  createdAt: string;
  createdBy: string | null;
}

export const VALUATION_LABELS: Record<StockValuationMethod, string> = {
  STANDARD: 'Coût standard',
  FIFO: 'FIFO (PEPS)',
  LIFO: 'LIFO (DEPS)',
  PMP_PERIOD: 'CMP périodique',
  PMP_REALTIME: 'CMP permanent',
};

export const STATUS_LABELS: Record<ProductStatus, string> = {
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  DISCONTINUED: 'Arrêté',
};

export const STATUS_BADGE: Record<ProductStatus, string> = {
  ACTIVE: 'nx-badge--success',
  INACTIVE: 'nx-badge--neutral',
  DISCONTINUED: 'nx-badge--error',
};

export const MOVEMENT_LABELS: Record<MovementType, string> = {
  IN: 'Entrée',
  OUT: 'Sortie',
  TRANSFER: 'Transfert',
  ADJUSTMENT: 'Ajustement',
};

export const MOVEMENT_BADGE: Record<MovementType, string> = {
  IN: 'nx-badge--success',
  OUT: 'nx-badge--warning',
  TRANSFER: 'nx-badge--info',
  ADJUSTMENT: 'nx-badge--neutral',
};

/** Base path API du module stocks. */
export const INVENTORY_API = '/api/v1/inventory';
