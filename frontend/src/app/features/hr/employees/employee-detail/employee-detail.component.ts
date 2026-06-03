import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  formatMoney,
  CONTRACT_TYPE_LABELS,
  EMPLOYEE_STATUS_LABELS,
} from '../../hr-format';

export type ContractType = 'CDI' | 'CDD' | 'INTERIM' | 'INTERNSHIP' | 'FREELANCE';
export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED';

/** Forme renvoyée par GET /api/v1/hr/employees/{id} (EmployeeDto plat). */
export interface EmployeeDetail {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  department: string | null;
  jobTitle: string | null;
  contractType: ContractType;
  status: EmployeeStatus;
  hireDate: string;
  grossSalaryAmount: number | null;
  grossSalaryCurrency: string;
  country: string;
}

/** Représentation Money du backend (amount/currency). */
interface MoneyDto {
  amount: number;
  currency: string;
}

/** Forme renvoyée par GET /api/v1/hr/employees/{id}/payslip (PayslipResult). */
export interface PayslipResult {
  grossSalary: MoneyDto;
  netSalary: MoneyDto;
  totalEmployeeDeductions: MoneyDto;
  totalEmployerCost: MoneyDto;
  country: string;
}

@Component({
  selector: 'nx-employee-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './employee-detail.component.html',
  styleUrl: './employee-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmployeeDetailComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);
  readonly authState = inject(AuthStateService);

  readonly isLoading = signal(true);
  readonly employee = signal<EmployeeDetail | null>(null);

  readonly actionInProgress = signal<string | null>(null);
  readonly payslip = signal<PayslipResult | null>(null);

  readonly contractLabel = CONTRACT_TYPE_LABELS;
  readonly statusLabel = EMPLOYEE_STATUS_LABELS;

  // Seuls HR_MANAGER / TENANT_ADMIN peuvent simuler la paie (PreAuthorize backend).
  readonly canViewPayroll = computed(() =>
    this.authState.hasAnyRole('HR_MANAGER', 'TENANT_ADMIN')
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/hr/employees']);
      return;
    }
    this.loadEmployee(id);
  }

  private loadEmployee(id: string): void {
    this.isLoading.set(true);
    this.http.get<{ data?: EmployeeDetail } & Partial<EmployeeDetail>>(`/api/v1/hr/employees/${id}`).subscribe({
      next: (res) => {
        const data = (res.data ?? res) as EmployeeDetail;
        this.employee.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.notif.error('Salarié introuvable');
        this.router.navigate(['/hr/employees']);
      }
    });
  }

  computePayslip(): void {
    const e = this.employee();
    if (!e || !this.canViewPayroll()) return;
    this.actionInProgress.set('payslip');
    this.http.get<PayslipResult>(`/api/v1/hr/employees/${e.id}/payslip`).subscribe({
      next: (result) => {
        this.payslip.set(result);
        this.actionInProgress.set(null);
        this.notif.success('Bulletin de paie simulé');
      },
      error: () => {
        this.notif.error('Échec de la simulation du bulletin');
        this.actionInProgress.set(null);
      }
    });
  }

  formatAmount(amount: number | null | undefined, currency?: string): string {
    return formatMoney(amount, currency ?? this.employee()?.grossSalaryCurrency);
  }

  formatMoneyDto(money: MoneyDto | null | undefined): string {
    return formatMoney(money?.amount, money?.currency);
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      ACTIVE: 'nx-badge--success',
      ON_LEAVE: 'nx-badge--info',
      SUSPENDED: 'nx-badge--warning',
      TERMINATED: 'nx-badge--error',
    };
    return classes[status] ?? 'nx-badge--neutral';
  }
}
