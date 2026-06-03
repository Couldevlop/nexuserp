import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { WorkOrder } from '../work-order.model';
import {
  woStatusLabel,
  woStatusBadge,
  woPriorityLabel,
  woPriorityBadge,
  formatQuantity,
  formatRate,
} from '../production-format';

@Component({
  selector: 'nx-work-order-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './work-order-detail.component.html',
  styleUrl: './work-order-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkOrderDetailComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);
  readonly authState = inject(AuthStateService);

  readonly isLoading = signal(true);
  readonly workOrder = signal<WorkOrder | null>(null);
  readonly actionInProgress = signal<string | null>(null);

  // Rôles autorisés à piloter l'OF (alignés sur les @PreAuthorize backend).
  private readonly canManage = computed(() =>
    this.authState.hasAnyRole('PRODUCTION_MANAGER', 'TENANT_ADMIN')
  );

  private readonly canOperate = computed(() =>
    this.authState.hasAnyRole('PRODUCTION_MANAGER', 'PRODUCTION_USER', 'TENANT_ADMIN')
  );

  // Lancer : PUT /release — statut PLANNED, rôle manager.
  readonly canRelease = computed(
    () => this.workOrder()?.status === 'PLANNED' && this.canManage()
  );

  // Démarrer : PUT /start — statut RELEASED ou ON_HOLD, rôle opérateur.
  readonly canStart = computed(() => {
    const s = this.workOrder()?.status;
    return (s === 'RELEASED' || s === 'ON_HOLD') && this.canOperate();
  });

  // Terminer : PUT /production (enregistre la quantité restante → COMPLETED).
  // Disponible en cours / partiellement terminé, rôle opérateur.
  readonly canComplete = computed(() => {
    const s = this.workOrder()?.status;
    return (s === 'IN_PROGRESS' || s === 'PARTIALLY_COMPLETED') && this.canOperate();
  });

  readonly remainingQuantity = computed(() => {
    const w = this.workOrder();
    if (!w) return 0;
    const planned = w.quantityPlanned ?? 0;
    const produced = w.quantityProduced ?? 0;
    return Math.max(planned - produced, 0);
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/production/work-orders']);
      return;
    }
    this.loadWorkOrder(id);
  }

  private loadWorkOrder(id: string): void {
    this.isLoading.set(true);
    this.http
      .get<{ data?: WorkOrder } & Partial<WorkOrder>>(`/api/v1/production/work-orders/${id}`)
      .subscribe({
        next: (res) => {
          const data = (res.data ?? res) as WorkOrder;
          this.workOrder.set(data);
          this.isLoading.set(false);
        },
        error: () => {
          this.notif.error('Ordre de fabrication introuvable');
          this.router.navigate(['/production/work-orders']);
        },
      });
  }

  release(): void {
    const w = this.workOrder();
    if (!w) return;
    this.actionInProgress.set('release');
    this.http.put(`/api/v1/production/work-orders/${w.id}/release`, {}).subscribe({
      next: () => {
        this.notif.success('Ordre de fabrication lancé');
        this.actionInProgress.set(null);
        this.loadWorkOrder(w.id);
      },
      error: () => {
        this.notif.error('Échec du lancement');
        this.actionInProgress.set(null);
      },
    });
  }

  start(): void {
    const w = this.workOrder();
    if (!w) return;
    this.actionInProgress.set('start');
    const operatorId = this.authState.user()?.email ?? 'system';
    this.http
      .put(`/api/v1/production/work-orders/${w.id}/start`, { operatorId })
      .subscribe({
        next: () => {
          this.notif.success('Production démarrée');
          this.actionInProgress.set(null);
          this.loadWorkOrder(w.id);
        },
        error: () => {
          this.notif.error('Échec du démarrage');
          this.actionInProgress.set(null);
        },
      });
  }

  complete(): void {
    const w = this.workOrder();
    if (!w) return;
    this.actionInProgress.set('complete');
    const operatorId = this.authState.user()?.email ?? 'system';
    // Enregistre la quantité restante pour clôturer l'OF (statut → COMPLETED).
    const quantity = this.remainingQuantity();
    this.http
      .put(`/api/v1/production/work-orders/${w.id}/production`, {
        quantity,
        rejected: 0,
        operatorId,
      })
      .subscribe({
        next: () => {
          this.notif.success('Production enregistrée');
          this.actionInProgress.set(null);
          this.loadWorkOrder(w.id);
        },
        error: () => {
          this.notif.error("Échec de l'enregistrement de la production");
          this.actionInProgress.set(null);
        },
      });
  }

  qty(value: number | null | undefined): string {
    return formatQuantity(value);
  }

  rate(value: number | null | undefined): string {
    return formatRate(value);
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
