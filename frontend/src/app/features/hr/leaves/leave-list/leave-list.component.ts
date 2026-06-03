import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { LEAVE_TYPE_LABELS, LEAVE_STATUS_LABELS } from '../../hr-format';

export type LeaveStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type LeaveType = 'ANNUAL' | 'SICK' | 'MATERNITY' | 'PATERNITY' | 'RTT' | 'UNPAID' | 'OTHER';

/** Forme renvoyée par LeaveDto. */
export interface LeaveSummary {
  id: string;
  employeeId: string;
  employeeName?: string | null;
  leaveType: LeaveType;
  status: LeaveStatus;
  startDate: string;
  endDate: string;
  durationDays: number;
  reason: string | null;
  approvedBy: string | null;
  rejectedBy: string | null;
  rejectionReason: string | null;
}

interface SpringPage<T> {
  content?: T[];
  data?: T[];
  totalPages?: number;
  totalElements?: number;
  number?: number;
  meta?: { page: number; size: number; total: number; totalPages: number };
}

@Component({
  selector: 'nx-leave-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './leave-list.component.html',
  styleUrl: './leave-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LeaveListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);
  readonly authState = inject(AuthStateService);

  readonly isLoading = signal(true);
  // nexus-hr n'expose pas (encore) d'endpoint de liste des congés : on dégrade
  // gracieusement vers un état vide sans données fabriquées.
  readonly notAvailable = signal(false);
  readonly leaves = signal<LeaveSummary[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly statusFilter = signal<string>('');

  readonly actionInProgress = signal<string | null>(null);

  readonly typeLabel = LEAVE_TYPE_LABELS;
  readonly statusLabel = LEAVE_STATUS_LABELS;

  readonly statusOptions: { value: LeaveStatus | ''; label: string }[] = [
    { value: '', label: 'Tous les statuts' },
    { value: 'SUBMITTED', label: 'En attente' },
    { value: 'APPROVED', label: 'Approuvé' },
    { value: 'REJECTED', label: 'Rejeté' },
    { value: 'CANCELLED', label: 'Annulé' },
  ];

  readonly statusBadgeClass: Record<LeaveStatus, string> = {
    DRAFT: 'nx-badge--neutral',
    SUBMITTED: 'nx-badge--warning',
    APPROVED: 'nx-badge--success',
    REJECTED: 'nx-badge--error',
    CANCELLED: 'nx-badge--neutral',
  };

  // Seuls HR_MANAGER / TENANT_ADMIN peuvent approuver / rejeter (PreAuthorize backend).
  readonly canManage = computed(() =>
    this.authState.hasAnyRole('HR_MANAGER', 'TENANT_ADMIN')
  );

  ngOnInit(): void {
    this.loadLeaves();
  }

  loadLeaves(): void {
    this.isLoading.set(true);
    this.notAvailable.set(false);

    let params = new HttpParams()
      .set('page', this.currentPage())
      .set('size', this.pageSize());

    if (this.statusFilter()) {
      params = params.set('status', this.statusFilter());
    }

    this.http.get<SpringPage<LeaveSummary>>('/api/v1/hr/leaves', { params }).subscribe({
      next: (page) => {
        this.leaves.set(page.content ?? page.data ?? []);
        this.totalPages.set(page.totalPages ?? page.meta?.totalPages ?? 0);
        this.totalItems.set(page.totalElements ?? page.meta?.total ?? 0);
        this.isLoading.set(false);
      },
      error: () => {
        // Endpoint de liste indisponible : dégradation silencieuse (pas de toast
        // d'erreur agressif), affichage d'un état d'indisponibilité dédié.
        this.notAvailable.set(true);
        this.leaves.set([]);
        this.isLoading.set(false);
      }
    });
  }

  onStatusChange(status: string): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
    this.loadLeaves();
  }

  approve(leave: LeaveSummary): void {
    if (!this.canManage()) return;
    this.actionInProgress.set(leave.id);
    const approvedBy = this.authState.user()?.email ?? 'system';
    this.http.put(`/api/v1/hr/leaves/${leave.id}/approve`, { approvedBy }).subscribe({
      next: () => {
        this.notif.success('Congé approuvé');
        this.actionInProgress.set(null);
        this.loadLeaves();
      },
      error: () => {
        this.notif.error("Échec de l'approbation");
        this.actionInProgress.set(null);
      }
    });
  }

  reject(leave: LeaveSummary): void {
    if (!this.canManage()) return;
    this.actionInProgress.set(leave.id);
    const rejectedBy = this.authState.user()?.email ?? 'system';
    this.http.put(`/api/v1/hr/leaves/${leave.id}/reject`, {
      rejectedBy,
      reason: 'Rejeté par le responsable RH',
    }).subscribe({
      next: () => {
        this.notif.success('Congé rejeté');
        this.actionInProgress.set(null);
        this.loadLeaves();
      },
      error: () => {
        this.notif.error('Échec du rejet');
        this.actionInProgress.set(null);
      }
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadLeaves();
  }

  getPages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}
