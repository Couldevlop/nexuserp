import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../core/services/notification.service';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { formatMoney, CONTRACT_TYPE_LABELS, EMPLOYEE_STATUS_LABELS } from '../hr-format';

interface EmployeeRecord {
  id: string;
  fullName: string;
  jobTitle: string | null;
  department: string | null;
  contractType: string;
  status: string;
  hireDate: string;
  grossSalaryAmount: number | null;
  grossSalaryCurrency: string;
}

interface SpringPage<T> {
  content?: T[];
  data?: T[];
  totalElements?: number;
  meta?: { total: number };
}

interface DepartmentStat {
  name: string;
  count: number;
}

@Component({
  selector: 'nx-hr-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, StatCardComponent],
  templateUrl: './hr-dashboard.component.html',
  styleUrl: './hr-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HrDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly hasData = signal(false);
  private readonly employees = signal<EmployeeRecord[]>([]);
  // totalElements renvoyé par la page (effectif réel, indépendant de l'échantillon).
  private readonly serverTotal = signal<number | null>(null);

  readonly statusLabel = EMPLOYEE_STATUS_LABELS;
  readonly contractLabel = CONTRACT_TYPE_LABELS;

  // Devise dominante du jeu de données (repli XOF, terrain CI).
  readonly currency = computed(() => this.employees()[0]?.grossSalaryCurrency ?? 'XOF');

  readonly headcount = computed(() => this.serverTotal() ?? this.employees().length);

  readonly activeCount = computed(
    () => this.employees().filter((e) => e.status === 'ACTIVE').length
  );

  // Masse salariale brute mensuelle (somme sur l'échantillon chargé, salariés non sortis).
  readonly payrollMass = computed(() =>
    this.employees().reduce((sum, e) => {
      if (e.status === 'TERMINATED') return sum;
      return sum + (e.grossSalaryAmount ?? 0);
    }, 0)
  );

  readonly recentHires = computed(() =>
    [...this.employees()]
      .filter((e) => e.hireDate)
      .sort((a, b) => b.hireDate.localeCompare(a.hireDate))
      .slice(0, 5)
  );

  readonly departments = computed<DepartmentStat[]>(() => {
    const map = new Map<string, number>();
    for (const e of this.employees()) {
      const name = e.department || 'Non affecté';
      map.set(name, (map.get(name) ?? 0) + 1);
    }
    return [...map.entries()]
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6);
  });

  ngOnInit(): void {
    this.loadEmployees();
  }

  private loadEmployees(): void {
    this.isLoading.set(true);
    // Agrégation côté client sur un échantillon récent (pas d'endpoint KPI RH dédié).
    const params = new HttpParams().set('page', 0).set('size', 200);
    this.http.get<SpringPage<EmployeeRecord>>('/api/v1/hr/employees', { params }).subscribe({
      next: (page) => {
        const records = page.content ?? page.data ?? [];
        this.employees.set(records);
        this.serverTotal.set(page.totalElements ?? page.meta?.total ?? null);
        this.hasData.set(records.length > 0);
        this.isLoading.set(false);
      },
      error: () => {
        this.notif.error('Erreur lors du chargement du tableau de bord RH');
        this.employees.set([]);
        this.serverTotal.set(null);
        this.hasData.set(false);
        this.isLoading.set(false);
      }
    });
  }

  formatHeadcount(): string {
    return this.hasData() ? String(this.headcount()) : '—';
  }

  formatActive(): string {
    return this.hasData() ? String(this.activeCount()) : '—';
  }

  formatPayroll(): string {
    return this.hasData() ? formatMoney(this.payrollMass(), this.currency()) : '—';
  }

  // Congés en attente : aucun endpoint d'agrégat disponible — non fabriqué.
  formatPendingLeaves(): string {
    return '—';
  }
}
