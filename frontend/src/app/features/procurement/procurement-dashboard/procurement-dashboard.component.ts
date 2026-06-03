import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../core/services/notification.service';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { formatMoney } from '../orders/procurement-format';

interface PoRecord {
  id: string;
  poNumber: string;
  supplierId?: string | null;
  supplierName: string;
  totalAmount: number | null;
  currency: string;
  status: string;
  expectedDeliveryDate: string | null;
}

interface SpringPage<T> {
  content?: T[];
  data?: T[];
  totalElements?: number;
  meta?: { total: number };
}

@Component({
  selector: 'nx-procurement-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, StatCardComponent],
  templateUrl: './procurement-dashboard.component.html',
  styleUrl: './procurement-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProcurementDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly hasData = signal(false);
  private readonly orders = signal<PoRecord[]>([]);

  // Devise dominante du jeu de données (repli EUR).
  readonly currency = computed(() => this.orders()[0]?.currency ?? 'EUR');

  // Commandes en cours : tout ce qui n'est ni reçu, ni clôturé, ni annulé.
  readonly openOrdersCount = computed(() => {
    const open = new Set([
      'DRAFT', 'SUBMITTED', 'APPROVED', 'SENT_TO_SUPPLIER', 'PARTIALLY_RECEIVED'
    ]);
    return this.orders().filter((o) => open.has(o.status)).length;
  });

  // Montant engagé : somme TTC des commandes approuvées/envoyées/en réception
  // (engagement réel hors brouillons et annulations).
  readonly committedAmount = computed(() => {
    const engaged = new Set([
      'APPROVED', 'SENT_TO_SUPPLIER', 'PARTIALLY_RECEIVED', 'RECEIVED', 'INVOICED'
    ]);
    return this.orders().reduce((sum, o) => {
      if (!engaged.has(o.status) || !o.totalAmount) return sum;
      return sum + o.totalAmount;
    }, 0);
  });

  // Fournisseurs distincts présents dans l'échantillon (hors annulations).
  readonly activeSuppliersCount = computed(() => {
    const names = new Set<string>();
    for (const o of this.orders()) {
      if (o.status === 'CANCELLED') continue;
      if (o.supplierName) names.add(o.supplierName);
    }
    return names.size;
  });

  // Livraisons attendues : commandes envoyées/partiellement reçues avec une
  // date de livraison prévue dans le futur.
  readonly expectedDeliveriesCount = computed(() => {
    const awaiting = new Set(['SENT_TO_SUPPLIER', 'PARTIALLY_RECEIVED', 'APPROVED']);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.orders().filter((o) => {
      if (!awaiting.has(o.status) || !o.expectedDeliveryDate) return false;
      return new Date(o.expectedDeliveryDate) >= today;
    }).length;
  });

  readonly recentOrders = computed(() => this.orders().slice(0, 8));

  readonly statusLabel: Record<string, string> = {
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

  readonly statusBadgeClass: Record<string, string> = {
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

  ngOnInit(): void {
    this.loadOrders();
  }

  private loadOrders(): void {
    this.isLoading.set(true);
    // On agrège côté client sur un échantillon récent (pas d'endpoint KPI dédié).
    const params = new HttpParams().set('page', 0).set('size', 200);
    this.http
      .get<SpringPage<PoRecord>>('/api/v1/procurement/purchase-orders', { params })
      .subscribe({
        next: (page) => {
          const records = page.content ?? page.data ?? [];
          this.orders.set(records);
          this.hasData.set(records.length > 0);
          this.isLoading.set(false);
        },
        error: () => {
          this.notif.error('Erreur lors du chargement du tableau de bord');
          this.orders.set([]);
          this.hasData.set(false);
          this.isLoading.set(false);
        }
      });
  }

  getStatusLabel(status: string): string {
    return this.statusLabel[status] ?? status;
  }

  getStatusClass(status: string): string {
    return this.statusBadgeClass[status] ?? 'nx-badge--neutral';
  }

  formatAmount(amount: number | null, currency?: string): string {
    return formatMoney(amount, currency ?? this.currency());
  }

  /** Valeur affichée dans une carte KPI : "—" tant qu'il n'y a pas de données. */
  kpiValue(value: string | number): string | number {
    return this.hasData() ? value : '—';
  }
}
