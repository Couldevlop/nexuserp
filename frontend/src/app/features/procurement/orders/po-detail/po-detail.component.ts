import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
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
 * Forme renvoyée par GET /api/v1/procurement/purchase-orders/{id}
 * (PurchaseOrderDto plat). Le backend nexus-procurement ne renvoie pas
 * (encore) les lignes ni le détail sous-total/TVA : seul totalAmount est
 * disponible. L'écran dégrade gracieusement ces sections.
 */
export interface PurchaseOrderDetail {
  id: string;
  poNumber: string;
  status: PurchaseOrderStatus;
  supplierId?: string | null;
  supplierName: string;
  expectedDeliveryDate?: string | null;
  currency: string;
  totalAmount: number | null;
  notes?: string | null;
  approvedBy?: string | null;
  lines?: PurchaseOrderLineDetail[];
}

export interface PurchaseOrderLineDetail {
  id: string;
  productCode?: string | null;
  description: string;
  quantity: number;
  unitPrice: number;
  taxRate?: number;
  subtotal: number;
  taxAmount: number;
  total: number;
}

@Component({
  selector: 'nx-po-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './po-detail.component.html',
  styleUrl: './po-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PoDetailComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);
  readonly authState = inject(AuthStateService);

  readonly isLoading = signal(true);
  readonly order = signal<PurchaseOrderDetail | null>(null);
  readonly actionInProgress = signal<string | null>(null);

  private readonly canManage = computed(() =>
    this.authState.hasAnyRole('PROCUREMENT_MANAGER', 'TENANT_ADMIN')
  );

  // Seul l'endpoint d'approbation existe côté backend (PUT /{id}/approve),
  // qui soumet puis approuve une commande en statut DRAFT. Les actions de
  // réception/annulation ne sont pas (encore) exposées en REST : aucun bouton
  // n'y est câblé pour éviter des appels vers des endpoints inexistants.
  readonly canApprove = computed(
    () => this.order()?.status === 'DRAFT' && this.canManage()
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/procurement/orders']);
      return;
    }
    this.loadOrder(id);
  }

  private loadOrder(id: string): void {
    this.isLoading.set(true);
    this.http
      .get<{ data?: PurchaseOrderDetail } & Partial<PurchaseOrderDetail>>(
        `/api/v1/procurement/purchase-orders/${id}`
      )
      .subscribe({
        next: (res) => {
          const data = (res.data ?? res) as PurchaseOrderDetail;
          this.order.set(data);
          this.isLoading.set(false);
        },
        error: () => {
          this.notif.error("Commande d'achat introuvable");
          this.router.navigate(['/procurement/orders']);
        }
      });
  }

  approve(): void {
    const o = this.order();
    if (!o) return;
    this.actionInProgress.set('approve');
    const approvedBy = this.authState.user()?.email ?? 'system';
    this.http
      .put(`/api/v1/procurement/purchase-orders/${o.id}/approve`, { approvedBy })
      .subscribe({
        next: () => {
          this.notif.success("Commande d'achat approuvée");
          this.actionInProgress.set(null);
          this.loadOrder(o.id);
        },
        error: () => {
          this.notif.error("Échec de l'approbation");
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
      SUBMITTED: 'Soumise',
      APPROVED: 'Approuvée',
      SENT_TO_SUPPLIER: 'Envoyée au fournisseur',
      PARTIALLY_RECEIVED: 'Partiellement reçue',
      RECEIVED: 'Reçue',
      INVOICED: 'Facturée',
      CLOSED: 'Clôturée',
      CANCELLED: 'Annulée'
    };
    return labels[status] ?? status;
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      DRAFT: 'nx-badge--neutral',
      SUBMITTED: 'nx-badge--info',
      APPROVED: 'nx-badge--info',
      SENT_TO_SUPPLIER: 'nx-badge--warning',
      PARTIALLY_RECEIVED: 'nx-badge--warning',
      RECEIVED: 'nx-badge--success',
      INVOICED: 'nx-badge--success',
      CLOSED: 'nx-badge--success',
      CANCELLED: 'nx-badge--error'
    };
    return classes[status] ?? 'nx-badge--neutral';
  }
}
