import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductDto, SpringPage, INVENTORY_API } from '../inventory.types';

interface ExpiryAlert {
  productCode: string;
  productName: string;
  lotNumber: string | null;
  quantity: number;
  unit: string;
  expiryDate: string;
  daysLeft: number;
}

@Component({
  selector: 'nx-inventory-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './inventory-dashboard.component.html',
  styleUrl: './inventory-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InventoryDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly products = signal<ProductDto[]>([]);

  /** Alertes péremption : endpoint non exposé → null = donnée indisponible (jamais simulée). */
  readonly expiryAlerts = signal<ExpiryAlert[] | null>(null);

  // ─── KPIs dérivés des vraies données produits ───────────────────────────────
  readonly totalReferences = computed(() => this.products().length);

  readonly outOfStockCount = computed(
    () => this.products().filter(p => p.quantityOnHand <= 0).length
  );

  readonly lowStockProducts = computed(
    () => this.products().filter(p => p.needsReorder || p.quantityOnHand <= p.reorderPoint)
  );

  readonly lowStockCount = computed(() => this.lowStockProducts().length);

  /** Valeur du stock = Σ (qté en stock × coût moyen). null si aucun coût connu. */
  readonly stockValue = computed<number | null>(() => {
    const items = this.products();
    if (items.length === 0) return null;
    let total = 0;
    let anyCost = false;
    for (const p of items) {
      if (p.averageCostAmount != null) {
        anyCost = true;
        total += p.quantityOnHand * p.averageCostAmount;
      }
    }
    return anyCost ? total : null;
  });

  /** Devise dominante pour l'affichage de la valeur. */
  readonly stockCurrency = computed<string>(() => {
    const c = this.products().find(p => p.averageCostCurrency)?.averageCostCurrency;
    return c ?? '';
  });

  readonly expiryAlertCount = computed<number | null>(() => {
    const a = this.expiryAlerts();
    return a === null ? null : a.length;
  });

  ngOnInit(): void {
    this.loadDashboard();
  }

  private loadDashboard(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    // On agrège une grande page pour calculer les KPIs (pas d'endpoint dashboard dédié).
    const params = new HttpParams()
      .set('page', 0)
      .set('size', 500)
      .set('sort', 'name')
      .set('dir', 'asc');

    this.http.get<SpringPage<ProductDto>>(`${INVENTORY_API}/products`, { params }).subscribe({
      next: (page) => {
        this.products.set(page.content ?? []);
        this.isLoading.set(false);
        this.loadExpiryAlerts();
      },
      error: () => {
        this.notif.error('Erreur lors du chargement du tableau de bord');
        this.hasError.set(true);
        this.products.set([]);
        this.isLoading.set(false);
      }
    });
  }

  /** Endpoint optionnel — absent du backend → reste null (affiché "—"). */
  private loadExpiryAlerts(): void {
    this.http.get<ExpiryAlert[]>(`${INVENTORY_API}/alerts/expiry`).subscribe({
      next: (rows) => this.expiryAlerts.set(rows ?? []),
      error: () => this.expiryAlerts.set(null),
    });
  }

  isLow(p: ProductDto): boolean {
    return p.needsReorder || p.quantityOnHand <= p.reorderPoint;
  }

  trackById(_: number, p: ProductDto): string {
    return p.id;
  }
}
