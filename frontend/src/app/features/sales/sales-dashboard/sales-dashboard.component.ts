import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../core/services/notification.service';
import { formatMoney } from '../orders/sales-format';

interface OrderRecord {
  id: string;
  orderNumber: string;
  customerName: string;
  totalAmount: number | null;
  currency: string;
  status: string;
  orderDate: string;
}

interface SpringPage<T> {
  content?: T[];
  data?: T[];
  totalElements?: number;
  meta?: { total: number };
}

interface TopCustomer {
  name: string;
  total: number;
  orders: number;
}

@Component({
  selector: 'nx-sales-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sales-dashboard.component.html',
  styleUrl: './sales-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SalesDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly hasData = signal(false);
  private readonly orders = signal<OrderRecord[]>([]);

  // Devise dominante du jeu de données (repli EUR).
  readonly currency = computed(() => this.orders()[0]?.currency ?? 'EUR');

  // CA du mois : somme TTC des commandes livrées/facturées du mois courant.
  readonly monthlyRevenue = computed(() => {
    const now = new Date();
    const m = now.getMonth();
    const y = now.getFullYear();
    const billable = new Set(['DELIVERED', 'INVOICED']);
    return this.orders().reduce((sum, o) => {
      if (!billable.has(o.status) || !o.totalAmount) return sum;
      const d = new Date(o.orderDate);
      if (d.getMonth() === m && d.getFullYear() === y) {
        return sum + o.totalAmount;
      }
      return sum;
    }, 0);
  });

  readonly openOrdersCount = computed(() => {
    const open = new Set(['DRAFT', 'CONFIRMED', 'PICKING', 'SHIPPED']);
    return this.orders().filter((o) => open.has(o.status)).length;
  });

  readonly totalOrders = computed(() => this.orders().length);

  readonly topCustomers = computed<TopCustomer[]>(() => {
    const map = new Map<string, TopCustomer>();
    for (const o of this.orders()) {
      if (o.status === 'CANCELLED') continue;
      const entry = map.get(o.customerName) ?? { name: o.customerName, total: 0, orders: 0 };
      entry.total += o.totalAmount ?? 0;
      entry.orders += 1;
      map.set(o.customerName, entry);
    }
    return [...map.values()].sort((a, b) => b.total - a.total).slice(0, 5);
  });

  ngOnInit(): void {
    this.loadOrders();
  }

  private loadOrders(): void {
    this.isLoading.set(true);
    // On agrège côté client sur un échantillon récent (pas d'endpoint KPI dédié).
    const params = new HttpParams().set('page', 0).set('size', 200);
    this.http.get<SpringPage<OrderRecord>>('/api/v1/sales/orders', { params }).subscribe({
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

  formatAmount(amount: number): string {
    return formatMoney(amount, this.currency());
  }
}
