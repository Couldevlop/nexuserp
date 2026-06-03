import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';

export type SupplierStatus = 'ACTIVE' | 'INACTIVE' | 'BLACKLISTED';

/**
 * Aligné sur le modèle de domaine Supplier de nexus-procurement
 * (code, name, contactName, email, phone, country, status…).
 * Le contrôleur REST fournisseurs n'est pas encore exposé : l'écran dégrade
 * gracieusement vers un état "indisponible" plutôt que d'inonder de toasts.
 */
export interface SupplierSummary {
  id: string;
  code?: string | null;
  name: string;
  contactName?: string | null;
  email?: string | null;
  phone?: string | null;
  country?: string | null;
  status?: SupplierStatus | null;
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
  selector: 'nx-supplier-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './supplier-list.component.html',
  styleUrl: './supplier-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SupplierListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly suppliers = signal<SupplierSummary[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly searchQuery = signal('');

  readonly statusLabel: Record<SupplierStatus, string> = {
    ACTIVE: 'Actif',
    INACTIVE: 'Inactif',
    BLACKLISTED: 'Sur liste noire',
  };

  readonly statusBadgeClass: Record<SupplierStatus, string> = {
    ACTIVE: 'nx-badge--success',
    INACTIVE: 'nx-badge--neutral',
    BLACKLISTED: 'nx-badge--error',
  };

  // Recherche locale en complément (le contrat backend fournisseur n'est pas figé).
  readonly filteredSuppliers = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) {
      return this.suppliers();
    }
    return this.suppliers().filter(
      (s) =>
        s.name.toLowerCase().includes(q) ||
        (s.email ?? '').toLowerCase().includes(q) ||
        (s.code ?? '').toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.loadSuppliers();
  }

  loadSuppliers(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    let params = new HttpParams()
      .set('page', this.currentPage())
      .set('size', this.pageSize());

    if (this.searchQuery().trim()) {
      params = params.set('q', this.searchQuery().trim());
    }

    this.http
      .get<SpringPage<SupplierSummary>>('/api/v1/procurement/suppliers', { params })
      .subscribe({
        next: (page) => {
          this.suppliers.set(page.content ?? page.data ?? []);
          this.totalPages.set(page.totalPages ?? page.meta?.totalPages ?? 0);
          this.totalItems.set(page.totalElements ?? page.meta?.total ?? 0);
          this.isLoading.set(false);
        },
        error: () => {
          // Le contrôleur fournisseurs backend n'est pas encore exposé :
          // dégradation gracieuse vers un état vide plutôt qu'une avalanche
          // de toasts.
          this.suppliers.set([]);
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
    this.loadSuppliers();
  }

  getPages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}
