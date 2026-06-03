import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { formatMoney } from '../sales-format';

export type OrderStatus =
  | 'DRAFT'
  | 'CONFIRMED'
  | 'PICKING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'INVOICED'
  | 'CANCELLED';

export interface OrderSummary {
  id: string;
  orderNumber: string;
  customerName: string;
  totalAmount: number | null;
  currency: string;
  status: OrderStatus;
  orderDate: string;
  requestedDeliveryDate: string | null;
}

/**
 * Réponse paginée — supporte le format Spring Data (Page<T>) renvoyé par
 * nexus-sales (content/totalPages/totalElements/number) ainsi que le format
 * standard ApiPage (data/meta) si la gateway le réécrit.
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
  selector: 'nx-order-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly orders = signal<OrderSummary[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly statusFilter = signal<string>('');
  readonly searchQuery = signal('');

  readonly statusOptions: { value: OrderStatus | ''; label: string }[] = [
    { value: '', label: 'Tous les statuts' },
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'CONFIRMED', label: 'Confirmée' },
    { value: 'PICKING', label: 'Préparation' },
    { value: 'SHIPPED', label: 'Expédiée' },
    { value: 'DELIVERED', label: 'Livrée' },
    { value: 'INVOICED', label: 'Facturée' },
    { value: 'CANCELLED', label: 'Annulée' },
  ];

  readonly statusBadgeClass = computed(() => {
    const map: Record<OrderStatus, string> = {
      DRAFT: 'nx-badge--neutral',
      CONFIRMED: 'nx-badge--info',
      PICKING: 'nx-badge--warning',
      SHIPPED: 'nx-badge--warning',
      DELIVERED: 'nx-badge--success',
      INVOICED: 'nx-badge--success',
      CANCELLED: 'nx-badge--error',
    };
    return map;
  });

  readonly statusLabel: Record<OrderStatus, string> = {
    DRAFT: 'Brouillon',
    CONFIRMED: 'Confirmée',
    PICKING: 'Préparation',
    SHIPPED: 'Expédiée',
    DELIVERED: 'Livrée',
    INVOICED: 'Facturée',
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
        o.orderNumber.toLowerCase().includes(q) ||
        o.customerName.toLowerCase().includes(q)
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

    this.http.get<SpringPage<OrderSummary>>('/api/v1/sales/orders', { params }).subscribe({
      next: (page) => {
        this.orders.set(page.content ?? page.data ?? []);
        this.totalPages.set(page.totalPages ?? page.meta?.totalPages ?? 0);
        this.totalItems.set(page.totalElements ?? page.meta?.total ?? 0);
        this.isLoading.set(false);
      },
      error: () => {
        this.notif.error('Erreur lors du chargement des commandes');
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
