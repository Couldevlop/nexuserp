import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { WorkOrder, WorkOrderStatus, SpringPage } from '../work-order.model';
import {
  woStatusLabel,
  woStatusBadge,
  woPriorityLabel,
  woPriorityBadge,
  formatQuantity,
} from '../production-format';

@Component({
  selector: 'nx-work-order-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './work-order-list.component.html',
  styleUrl: './work-order-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkOrderListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly workOrders = signal<WorkOrder[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly statusFilter = signal<string>('');
  readonly searchQuery = signal('');

  readonly statusOptions: { value: WorkOrderStatus | ''; label: string }[] = [
    { value: '', label: 'Tous les statuts' },
    { value: 'PLANNED', label: 'Planifié' },
    { value: 'RELEASED', label: 'Lancé' },
    { value: 'IN_PROGRESS', label: 'En cours' },
    { value: 'PARTIALLY_COMPLETED', label: 'Partiellement terminé' },
    { value: 'COMPLETED', label: 'Terminé' },
    { value: 'ON_HOLD', label: 'En attente' },
    { value: 'CANCELLED', label: 'Annulé' },
  ];

  // Recherche locale (le backend ne fournit pas encore de paramètre `q`).
  readonly filteredWorkOrders = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) {
      return this.workOrders();
    }
    return this.workOrders().filter(
      (w) =>
        w.orderNumber.toLowerCase().includes(q) ||
        (w.productName ?? '').toLowerCase().includes(q) ||
        (w.workcenter ?? '').toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.loadWorkOrders();
  }

  loadWorkOrders(): void {
    this.isLoading.set(true);

    let params = new HttpParams()
      .set('page', this.currentPage())
      .set('size', this.pageSize());

    if (this.statusFilter()) {
      params = params.set('status', this.statusFilter());
    }

    this.http
      .get<SpringPage<WorkOrder>>('/api/v1/production/work-orders', { params })
      .subscribe({
        next: (page) => {
          this.workOrders.set(page.content ?? page.data ?? []);
          this.totalPages.set(page.totalPages ?? page.meta?.totalPages ?? 0);
          this.totalItems.set(page.totalElements ?? page.meta?.total ?? 0);
          this.isLoading.set(false);
        },
        error: () => {
          this.notif.error('Erreur lors du chargement des ordres de fabrication');
          this.workOrders.set([]);
          this.isLoading.set(false);
        },
      });
  }

  onStatusChange(status: string): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
    this.loadWorkOrders();
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadWorkOrders();
  }

  getPages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  statusLabel(status: string): string {
    return woStatusLabel(status);
  }

  statusBadge(status: string): string {
    return woStatusBadge(status);
  }

  priorityLabel(priority: string): string {
    return woPriorityLabel(priority);
  }

  priorityBadge(priority: string): string {
    return woPriorityBadge(priority);
  }

  qty(value: number | null | undefined): string {
    return formatQuantity(value);
  }
}
