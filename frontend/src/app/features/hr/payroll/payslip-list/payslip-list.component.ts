import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { formatMoney, CONTRACT_TYPE_LABELS } from '../../hr-format';

interface EmployeeRecord {
  id: string;
  employeeNumber: string;
  fullName: string;
  jobTitle: string | null;
  department: string | null;
  contractType: string;
  status: string;
  grossSalaryAmount: number | null;
  grossSalaryCurrency: string;
}

interface SpringPage<T> {
  content?: T[];
  data?: T[];
  totalElements?: number;
  meta?: { total: number };
}

interface MoneyDto {
  amount: number;
  currency: string;
}

interface PayslipResult {
  grossSalary: MoneyDto;
  netSalary: MoneyDto;
  totalEmployeeDeductions: MoneyDto;
  totalEmployerCost: MoneyDto;
  country: string;
}

/** Ligne de bulletin combinant le salarié et son calcul de paie (à la demande). */
interface PayslipRow {
  employee: EmployeeRecord;
  payslip: PayslipResult | null;
  computing: boolean;
}

@Component({
  selector: 'nx-payslip-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './payslip-list.component.html',
  styleUrl: './payslip-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PayslipListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);
  readonly authState = inject(AuthStateService);

  readonly isLoading = signal(true);
  // nexus-hr ne fournit pas d'endpoint de liste de bulletins : on liste les
  // salariés et on simule la paie à la demande (endpoint /payslip existant).
  readonly notAvailable = signal(false);
  readonly rows = signal<PayslipRow[]>([]);

  // L'endpoint /payslip est gated HR_MANAGER / TENANT_ADMIN.
  readonly canViewPayroll = computed(() =>
    this.authState.hasAnyRole('HR_MANAGER', 'TENANT_ADMIN')
  );

  readonly contractLabel = CONTRACT_TYPE_LABELS;

  ngOnInit(): void {
    this.loadEmployees();
  }

  private loadEmployees(): void {
    this.isLoading.set(true);
    this.notAvailable.set(false);
    const params = new HttpParams().set('page', 0).set('size', 100);
    this.http.get<SpringPage<EmployeeRecord>>('/api/v1/hr/employees', { params }).subscribe({
      next: (page) => {
        const records = (page.content ?? page.data ?? []).filter((e) => e.status !== 'TERMINATED');
        this.rows.set(records.map((employee) => ({ employee, payslip: null, computing: false })));
        this.isLoading.set(false);
      },
      error: () => {
        this.notAvailable.set(true);
        this.rows.set([]);
        this.isLoading.set(false);
      }
    });
  }

  compute(row: PayslipRow): void {
    if (!this.canViewPayroll() || row.computing) return;
    this.setRow(row.employee.id, { computing: true });
    this.http.get<PayslipResult>(`/api/v1/hr/employees/${row.employee.id}/payslip`).subscribe({
      next: (payslip) => {
        this.setRow(row.employee.id, { payslip, computing: false });
      },
      error: () => {
        this.notif.error('Échec du calcul du bulletin');
        this.setRow(row.employee.id, { computing: false });
      }
    });
  }

  private setRow(employeeId: string, patch: Partial<Pick<PayslipRow, 'payslip' | 'computing'>>): void {
    this.rows.update((rows) =>
      rows.map((r) => (r.employee.id === employeeId ? { ...r, ...patch } : r))
    );
  }

  formatSalary(amount: number | null, currency: string): string {
    return formatMoney(amount, currency);
  }

  formatMoneyDto(money: MoneyDto | null | undefined): string {
    return formatMoney(money?.amount, money?.currency);
  }
}
