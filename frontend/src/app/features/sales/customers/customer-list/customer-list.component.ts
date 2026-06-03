import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';

export interface CustomerSummary {
  id: string;
  code?: string | null;
  name: string;
  email?: string | null;
  phone?: string | null;
  city?: string | null;
  country?: string | null;
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
  selector: 'nx-customer-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './customer-list.component.html',
  styleUrl: './customer-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly customers = signal<CustomerSummary[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly searchQuery = signal('');

  // Recherche locale en complément (le contrat backend client n'est pas figé).
  readonly filteredCustomers = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) {
      return this.customers();
    }
    return this.customers().filter(
      (c) =>
        c.name.toLowerCase().includes(q) ||
        (c.email ?? '').toLowerCase().includes(q) ||
        (c.code ?? '').toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    let params = new HttpParams()
      .set('page', this.currentPage())
      .set('size', this.pageSize());

    if (this.searchQuery().trim()) {
      params = params.set('q', this.searchQuery().trim());
    }

    this.http.get<SpringPage<CustomerSummary>>('/api/v1/sales/customers', { params }).subscribe({
      next: (page) => {
        this.customers.set(page.content ?? page.data ?? []);
        this.totalPages.set(page.totalPages ?? page.meta?.totalPages ?? 0);
        this.totalItems.set(page.totalElements ?? page.meta?.total ?? 0);
        this.isLoading.set(false);
      },
      error: () => {
        // Le module client backend n'est pas encore exposé : dégradation
        // gracieuse vers un état vide plutôt qu'une avalanche de toasts.
        this.customers.set([]);
        this.totalPages.set(0);
        this.totalItems.set(0);
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadCustomers();
  }

  getPages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}
