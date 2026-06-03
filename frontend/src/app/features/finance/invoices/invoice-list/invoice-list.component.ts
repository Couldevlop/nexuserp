import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';

export type InvoiceStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface InvoiceSummary {
  id: string;
  invoiceNumber: string;
  customerName: string;
  totalAmount: number;
  currency: string;
  status: InvoiceStatus;
  invoiceDate: string;
  dueDate: string;
}

export interface ApiPage<T> {
  data: T[];
  meta: {
    page: number;
    size: number;
    total: number;
    totalPages: number;
  };
}

@Component({
  selector: 'nx-invoice-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './invoice-list.component.html',
  styleUrl: './invoice-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly invoices = signal<InvoiceSummary[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly statusFilter = signal<string>('');
  readonly searchQuery = signal('');

  readonly statusOptions: { value: InvoiceStatus | ''; label: string }[] = [
    { value: '', label: 'Tous les statuts' },
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'SUBMITTED', label: 'En attente' },
    { value: 'APPROVED', label: 'Approuvée' },
    { value: 'PAID', label: 'Payée' },
    { value: 'OVERDUE', label: 'En retard' },
    { value: 'CANCELLED', label: 'Annulée' },
  ];

  readonly statusBadgeClass = computed(() => {
    const map: Record<InvoiceStatus, string> = {
      DRAFT: 'nx-badge--neutral',
      SUBMITTED: 'nx-badge--warning',
      APPROVED: 'nx-badge--info',
      PAID: 'nx-badge--success',
      OVERDUE: 'nx-badge--error',
      CANCELLED: 'nx-badge--neutral',
    };
    return map;
  });

  readonly statusLabel: Record<InvoiceStatus, string> = {
    DRAFT: 'Brouillon',
    SUBMITTED: 'En attente',
    APPROVED: 'Approuvée',
    PAID: 'Payée',
    OVERDUE: 'En retard',
    CANCELLED: 'Annulée',
  };

  ngOnInit(): void {
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.isLoading.set(true);

    let params = new HttpParams()
      .set('page', this.currentPage())
      .set('size', this.pageSize())
      .set('sort', 'invoiceDate,desc');

    if (this.statusFilter()) {
      params = params.set('status', this.statusFilter());
    }
    if (this.searchQuery().trim()) {
      params = params.set('q', this.searchQuery().trim());
    }

    this.http.get<ApiPage<InvoiceSummary>>('/api/v1/finance/invoices', { params }).subscribe({
      next: (page) => {
        this.invoices.set(page.data ?? []);
        this.totalPages.set(page.meta?.totalPages ?? 0);
        this.totalItems.set(page.meta?.total ?? 0);
        this.isLoading.set(false);
      },
      error: () => {
        this.notif.error('Erreur lors du chargement des factures');
        this.isLoading.set(false);
      }
    });
  }

  onStatusChange(status: string): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
    this.loadInvoices();
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
    this.currentPage.set(0);
    this.loadInvoices();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadInvoices();
  }

  getPages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}
