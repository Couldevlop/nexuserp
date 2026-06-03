import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../core/services/notification.service';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { WorkOrder, SpringPage } from '../work-orders/work-order.model';
import {
  woStatusLabel,
  woStatusBadge,
  woPriorityLabel,
  woPriorityBadge,
  formatQuantity,
  formatRate,
} from '../work-orders/production-format';

@Component({
  selector: 'nx-production-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, StatCardComponent],
  templateUrl: './production-dashboard.component.html',
  styleUrl: './production-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductionDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly hasData = signal(false);
  private readonly workOrders = signal<WorkOrder[]>([]);

  // OF en cours : IN_PROGRESS ou partiellement terminés.
  readonly inProgressCount = computed(
    () =>
      this.workOrders().filter(
        (w) => w.status === 'IN_PROGRESS' || w.status === 'PARTIALLY_COMPLETED'
      ).length
  );

  // OF planifiés : PLANNED ou RELEASED (en attente de démarrage).
  readonly plannedCount = computed(
    () =>
      this.workOrders().filter(
        (w) => w.status === 'PLANNED' || w.status === 'RELEASED'
      ).length
  );

  // Taux de réalisation : OF terminés / OF non annulés (échantillon récent).
  readonly completionRate = computed(() => {
    const active = this.workOrders().filter((w) => w.status !== 'CANCELLED');
    if (active.length === 0) return 0;
    const completed = active.filter((w) => w.status === 'COMPLETED').length;
    return Math.round((completed / active.length) * 1000) / 10;
  });

  // Retards : OF marqués isLate par le backend.
  readonly lateCount = computed(() => this.workOrders().filter((w) => w.isLate).length);

  // OF récents (5 derniers reçus).
  readonly recentWorkOrders = computed(() => this.workOrders().slice(0, 5));

  ngOnInit(): void {
    this.loadWorkOrders();
  }

  private loadWorkOrders(): void {
    this.isLoading.set(true);
    // Agrégation côté client sur un échantillon récent (pas d'endpoint KPI dédié).
    const params = new HttpParams().set('page', 0).set('size', 200);
    this.http
      .get<SpringPage<WorkOrder>>('/api/v1/production/work-orders', { params })
      .subscribe({
        next: (page) => {
          const records = page.content ?? page.data ?? [];
          this.workOrders.set(records);
          this.hasData.set(records.length > 0);
          this.isLoading.set(false);
        },
        error: () => {
          this.notif.error('Erreur lors du chargement du tableau de bord');
          this.workOrders.set([]);
          this.hasData.set(false);
          this.isLoading.set(false);
        },
      });
  }

  // Valeurs affichées dans les cartes KPI : "—" tant qu'aucune donnée réelle
  // n'est disponible (aucune fabrication de chiffres).
  statValue(value: number): string {
    return this.hasData() ? String(value) : '—';
  }

  completionValue(): string {
    return this.hasData() ? formatRate(this.completionRate()) : '—';
  }

  qty(value: number | null | undefined): string {
    return formatQuantity(value);
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
}
