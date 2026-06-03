import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { formatMoney } from '../procurement-format';

export type PurchaseOrderStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'SENT_TO_SUPPLIER'
  | 'PARTIALLY_RECEIVED'
  | 'RECEIVED'
  | 'INVOICED'
  | 'CLOSED'
  | 'CANCELLED';

/**
 * Forme renvoyée par GET /api/v1/procurement/purchase-orders (PurchaseOrderDto plat).
 * Aligné sur le contrat nexus-procurement : poNumber, supplierName, totalAmount,
 * status, expectedDeliveryDate, currency.
 */
export interface PurchaseOrderSummary {
  id: string;
  poNumber: string;
  supplierId?: string | null;
  supplierName: string;
  totalAmount: number | null;
  currency: string;
  status: PurchaseOrderStatus;
  expectedDeliveryDate: string | null;
  approvedBy?: string | null;
}

/**
 * Réponse paginée — supporte le format Spring Data (Page<T>) renvoyé par
 * nexus-procurement (content/totalPages/totalElements/number) ainsi que le
 * format ApiPage (data/meta) si la gateway le réécrit.
 */
interface SpringPage<T> {
  content?: T[];
  data?: T[];
  totalPages?: number;
  totalElements?: number;
  number?: number;
  meta?: { page: number; size: number; total: number; totalPages: number };
}

@Component({
  selector: 'nx-po-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './po-list.component.html',
  styleUrl: './po-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PoListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly orders = signal<PurchaseOrderSummary[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly statusFilter = signal<string>('');
  readonly searchQuery = signal('');

  readonly statusOptions: { value: PurchaseOrderStatus | ''; label: string }[] = [
    { value: '', label: 'Tous les statuts' },
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'SUBMITTED', label: 'Soumise' },
    { value: 'APPROVED', label: 'Approuvée' },
    { value: 'SENT_TO_SUPPLIER', label: 'Envoyée au fournisseur' },
    { value: 'PARTIALLY_RECEIVED', label: 'Partiellement reçue' },
    { value: 'RECEIVED', label: 'Reçue' },
    { value: 'INVOICED', label: 'Facturée' },
    { value: 'CLOSED', label: 'Clôturée' },
    { value: 'CANCELLED', label: 'Annulée' },
  ];

  readonly statusBadgeClass = computed(() => {
    const map: Record<PurchaseOrderStatus, string> = {
      DRAFT: 'nx-badge--neutral',
      SUBMITTED: 'nx-badge--info',
      APPROVED: 'nx-badge--info',
      SENT_TO_SUPPLIER: 'nx-badge--warning',
      PARTIALLY_RECEIVED: 'nx-badge--warning',
      RECEIVED: 'nx-badge--success',
      INVOICED: 'nx-badge--success',
      CLOSED: 'nx-badge--success',
      CANCELLED: 'nx-badge--error',
    };
    return map;
  });

  readonly statusLabel: Record<PurchaseOrderStatus, string> = {
    DRAFT: 'Brouillon',
    SUBMITTED: 'Soumise',
    APPROVED: 'Approuvée',
    SENT_TO_SUPPLIER: 'Envoyée au fournisseur',
    PARTIALLY_RECEIVED: 'Partiellement reçue',
    RECEIVED: 'Reçue',
    INVOICED: 'Facturée',
    CLOSED: 'Clôturée',
    CANCELLED: 'Annulée',
  };

  // Recherche locale (le backend ne fournit pas encore de paramètre `q`).
  readonly filteredOrders = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) {
      return this.orders();
    }
    return this.orders().filter(
      (o) =>
        o.poNumber.toLowerCase().includes(q) ||
        o.supplierName.toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.isLoading.set(true);

    let params = new HttpParams()
      .set('page', this.currentPage())
      .set('size', this.pageSize());

    if (this.statusFilter()) {
      params = params.set('status', this.statusFilter());
    }

    this.http
      .get<SpringPage<PurchaseOrderSummary>>('/api/v1/procurement/purchase-orders', { params })
      .subscribe({
        next: (page) => {
          this.orders.set(page.content ?? page.data ?? []);
          this.totalPages.set(page.totalPages ?? page.meta?.totalPages ?? 0);
          this.totalItems.set(page.totalElements ?? page.meta?.total ?? 0);
          this.isLoading.set(false);
        },
        error: () => {
          this.notif.error("Erreur lors du chargement des commandes d'achat");
          this.orders.set([]);
          this.isLoading.set(false);
        }
      });
  }

  onStatusChange(status: string): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
    this.loadOrders();
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadOrders();
  }

  getPages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  formatAmount(amount: number | null, currency: string): string {
    return formatMoney(amount, currency);
  }
}
