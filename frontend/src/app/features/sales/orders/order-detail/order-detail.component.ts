import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { formatMoney } from '../sales-format';

export type OrderStatus =
  | 'DRAFT'
  | 'CONFIRMED'
  | 'PICKING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'INVOICED'
  | 'CANCELLED';

/**
 * Forme renvoyée par GET /api/v1/sales/orders/{id} (SalesOrderDto plat).
 * Le backend nexus-sales ne renvoie pas (encore) les lignes ni le détail
 * sous-total/TVA : seul totalAmount est disponible. L'écran dégrade
 * gracieusement ces sections.
 */
export interface OrderDetail {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  customerId?: string | null;
  customerName: string;
  customerRef?: string | null;
  orderDate: string;
  requestedDeliveryDate?: string | null;
  currency: string;
  totalAmount: number | null;
  shippingAddress?: string | null;
  notes?: string | null;
  lines?: OrderLineDetail[];
}

export interface OrderLineDetail {
  id: string;
  productCode?: string | null;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPct?: number;
  taxRate?: number;
  subtotal: number;
  taxAmount: number;
  total: number;
}

@Component({
  selector: 'nx-order-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderDetailComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);
  readonly authState = inject(AuthStateService);

  readonly isLoading = signal(true);
  readonly order = signal<OrderDetail | null>(null);
  readonly actionInProgress = signal<string | null>(null);

  private readonly canManage = computed(() =>
    this.authState.hasAnyRole('SALES_MANAGER', 'TENANT_ADMIN')
  );

  readonly canConfirm = computed(
    () => this.order()?.status === 'DRAFT' && this.canManage()
  );

  // L'agrégat backend autorise l'annulation tant que la commande n'est ni
  // expédiée, ni livrée, ni facturée.
  readonly canCancel = computed(() => {
    const s = this.order()?.status;
    return (
      !!s &&
      s !== 'SHIPPED' &&
      s !== 'DELIVERED' &&
      s !== 'INVOICED' &&
      s !== 'CANCELLED' &&
      this.canManage()
    );
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/sales/orders']);
      return;
    }
    this.loadOrder(id);
  }

  private loadOrder(id: string): void {
    this.isLoading.set(true);
    this.http.get<{ data?: OrderDetail } & Partial<OrderDetail>>(`/api/v1/sales/orders/${id}`).subscribe({
      next: (res) => {
        const data = (res.data ?? res) as OrderDetail;
        this.order.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.notif.error('Commande introuvable');
        this.router.navigate(['/sales/orders']);
      }
    });
  }

  confirm(): void {
    const o = this.order();
    if (!o) return;
    this.actionInProgress.set('confirm');
    const confirmedBy = this.authState.user()?.email ?? 'system';
    this.http.put(`/api/v1/sales/orders/${o.id}/confirm`, { confirmedBy }).subscribe({
      next: () => {
        this.notif.success('Commande confirmée');
        this.actionInProgress.set(null);
        this.loadOrder(o.id);
      },
      error: () => {
        this.notif.error('Échec de la confirmation');
        this.actionInProgress.set(null);
      }
    });
  }

  cancel(): void {
    const o = this.order();
    if (!o) return;
    this.actionInProgress.set('cancel');
    this.http.put(`/api/v1/sales/orders/${o.id}/cancel`, { reason: 'Annulée par l\'utilisateur' }).subscribe({
      next: () => {
        this.notif.success('Commande annulée');
        this.actionInProgress.set(null);
        this.loadOrder(o.id);
      },
      error: () => {
        this.notif.error('Échec de l\'annulation');
        this.actionInProgress.set(null);
      }
    });
  }

  formatAmount(amount: number | null | undefined, currency?: string): string {
    return formatMoney(amount, currency ?? this.order()?.currency);
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      DRAFT: 'Brouillon',
      CONFIRMED: 'Confirmée',
      PICKING: 'Préparation',
      SHIPPED: 'Expédiée',
      DELIVERED: 'Livrée',
      INVOICED: 'Facturée',
      CANCELLED: 'Annulée'
    };
    return labels[status] ?? status;
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      DRAFT: 'nx-badge--neutral',
      CONFIRMED: 'nx-badge--info',
      PICKING: 'nx-badge--warning',
      SHIPPED: 'nx-badge--warning',
      DELIVERED: 'nx-badge--success',
      INVOICED: 'nx-badge--success',
      CANCELLED: 'nx-badge--error'
    };
    return classes[status] ?? 'nx-badge--neutral';
  }
}
