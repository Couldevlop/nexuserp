import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  formatMoney,
  CONTRACT_TYPE_LABELS,
  EMPLOYEE_STATUS_LABELS,
} from '../../hr-format';

export type ContractType = 'CDI' | 'CDD' | 'INTERIM' | 'INTERNSHIP' | 'FREELANCE';
export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED';

/** Forme renvoyée par GET /api/v1/hr/employees (EmployeeDto plat). */
export interface EmployeeSummary {
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

/**
 * Réponse paginée — Spring Data Page<T> renvoyé par nexus-hr (content/
 * totalElements/totalPages/number) avec repli ApiPage (data/meta) au cas où
 * la gateway réécrirait l'enveloppe.
 */
interface SpringPage<T> {
  content?: T[];
  data?: T[];
  totalPages?: number;
  totalElements?: number;
  number?: number;
  meta?: { page: number; size: number; total: number; totalPages: number };
}

@Component({
  selector: 'nx-employee-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmployeeListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly employees = signal<EmployeeSummary[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  // Le backend ne filtre que par `department`.
  readonly departmentFilter = signal<string>('');
  readonly searchQuery = signal('');

  readonly contractLabel = CONTRACT_TYPE_LABELS;
  readonly statusLabel = EMPLOYEE_STATUS_LABELS;

  readonly statusBadgeClass: Record<EmployeeStatus, string> = {
    ACTIVE: 'nx-badge--success',
    ON_LEAVE: 'nx-badge--info',
    SUSPENDED: 'nx-badge--warning',
    TERMINATED: 'nx-badge--error',
  };

  readonly contractBadgeClass: Record<ContractType, string> = {
    CDI: 'nx-badge--success',
    CDD: 'nx-badge--info',
    INTERIM: 'nx-badge--warning',
    INTERNSHIP: 'nx-badge--neutral',
    FREELANCE: 'nx-badge--neutral',
  };

  // Départements présents dans le jeu courant — alimente le filtre serveur.
  readonly departments = computed(() => {
    const set = new Set<string>();
    for (const e of this.employees()) {
      if (e.department) set.add(e.department);
    }
    return [...set].sort((a, b) => a.localeCompare(b));
  });

  // Recherche locale (le backend n'expose pas de paramètre `q`).
  readonly filteredEmployees = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) {
      return this.employees();
    }
    return this.employees().filter(
      (e) =>
        e.fullName.toLowerCase().includes(q) ||
        e.employeeNumber.toLowerCase().includes(q) ||
        (e.email ?? '').toLowerCase().includes(q) ||
        (e.jobTitle ?? '').toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.isLoading.set(true);

    let params = new HttpParams()
      .set('page', this.currentPage())
      .set('size', this.pageSize());

    if (this.departmentFilter()) {
      params = params.set('department', this.departmentFilter());
    }

    this.http.get<SpringPage<EmployeeSummary>>('/api/v1/hr/employees', { params }).subscribe({
      next: (page) => {
        this.employees.set(page.content ?? page.data ?? []);
        this.totalPages.set(page.totalPages ?? page.meta?.totalPages ?? 0);
        this.totalItems.set(page.totalElements ?? page.meta?.total ?? 0);
        this.isLoading.set(false);
      },
      error: () => {
        this.notif.error('Erreur lors du chargement des salariés');
        this.employees.set([]);
        this.isLoading.set(false);
      }
    });
  }

  onDepartmentChange(department: string): void {
    this.departmentFilter.set(department);
    this.currentPage.set(0);
    this.loadEmployees();
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadEmployees();
  }

  getPages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  formatSalary(amount: number | null, currency: string): string {
    return formatMoney(amount, currency);
  }
}
