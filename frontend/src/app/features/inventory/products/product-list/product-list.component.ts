import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  ProductDto, SpringPage, ProductStatus,
  STATUS_LABELS, STATUS_BADGE, VALUATION_LABELS, INVENTORY_API
} from '../../inventory.types';

@Component({
  selector: 'nx-product-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly products = signal<ProductDto[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly categoryFilter = signal<string>('');
  readonly searchQuery = signal('');
  readonly lowStockOnly = signal(false);

  readonly statusLabel = STATUS_LABELS;
  readonly statusBadge = STATUS_BADGE;
  readonly valuationLabel = VALUATION_LABELS;

  /**
   * Le backend n'expose ni recherche texte (`q`) ni filtre low-stock côté serveur :
   * on filtre la page courante côté client (le serveur reste l'autorité).
   */
  readonly visibleProducts = computed<ProductDto[]>(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const low = this.lowStockOnly();
    return this.products().filter(p => {
      if (low && !this.isLowStock(p)) return false;
      if (!q) return true;
      return (
        p.productCode.toLowerCase().includes(q) ||
        p.name.toLowerCase().includes(q) ||
        (p.category ?? '').toLowerCase().includes(q)
      );
    });
  });

  readonly lowStockCount = computed(
    () => this.products().filter(p => this.isLowStock(p)).length
  );

  ngOnInit(): void {
    this.loadProducts();
  }

  /** Un article est en alerte si la quantité disponible est <= seuil de réappro. */
  isLowStock(p: ProductDto): boolean {
    return p.needsReorder || p.quantityOnHand <= p.reorderPoint;
  }

  statusOf(p: ProductDto): ProductStatus {
    return p.status;
  }

  loadProducts(): void {
    this.isLoading.set(true);

    let params = new HttpParams()
      .set('page', this.currentPage())
      .set('size', this.pageSize())
      .set('sort', 'name')
      .set('dir', 'asc');

    if (this.categoryFilter().trim()) {
      params = params.set('category', this.categoryFilter().trim());
    }

    this.http.get<SpringPage<ProductDto>>(`${INVENTORY_API}/products`, { params }).subscribe({
      next: (page) => {
        this.products.set(page.content ?? []);
        this.totalPages.set(page.totalPages ?? 0);
        this.totalItems.set(page.totalElements ?? 0);
        this.isLoading.set(false);
      },
      error: () => {
        this.notif.error('Erreur lors du chargement des articles');
        this.products.set([]);
        this.isLoading.set(false);
      }
    });
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
  }

  onCategoryChange(category: string): void {
    this.categoryFilter.set(category);
    this.currentPage.set(0);
    this.loadProducts();
  }

  toggleLowStock(): void {
    this.lowStockOnly.update(v => !v);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadProducts();
  }

  getPages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  trackById(_: number, p: ProductDto): string {
    return p.id;
  }
}
