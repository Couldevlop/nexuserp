import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { MovementFormComponent } from '../../movements/movement-form/movement-form.component';
import {
  ProductDto, StockLot, WarehouseStock, MovementType,
  STATUS_LABELS, STATUS_BADGE, VALUATION_LABELS, INVENTORY_API
} from '../../inventory.types';

@Component({
  selector: 'nx-product-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, MovementFormComponent],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductDetailComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly product = signal<ProductDto | null>(null);

  /** Endpoints non encore exposés → dégradés gracieusement (listes vides). */
  readonly warehouseStock = signal<WarehouseStock[]>([]);
  readonly lots = signal<StockLot[]>([]);
  readonly extrasLoaded = signal(false);

  readonly modalOpen = signal(false);
  readonly modalType = signal<MovementType | null>(null);

  readonly statusLabel = STATUS_LABELS;
  readonly statusBadge = STATUS_BADGE;
  readonly valuationLabel = VALUATION_LABELS;

  /** Seuil d'alerte péremption : 30 jours. */
  private readonly NEAR_EXPIRY_DAYS = 30;

  readonly isLowStock = computed(() => {
    const p = this.product();
    if (!p) return false;
    return p.needsReorder || p.quantityOnHand <= p.reorderPoint;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/inventory/products']);
      return;
    }
    this.loadProduct(id);
  }

  private loadProduct(id: string): void {
    this.isLoading.set(true);
    this.http.get<ProductDto>(`${INVENTORY_API}/products/${id}`).subscribe({
      next: (p) => {
        this.product.set(p);
        this.isLoading.set(false);
        this.loadExtras(id);
      },
      error: () => {
        this.notif.error('Article introuvable');
        this.router.navigate(['/inventory/products']);
      }
    });
  }

  /**
   * Charge la ventilation entrepôt + lots si le backend les expose un jour.
   * En l'absence d'endpoint, on dégrade gracieusement (404/erreur → listes vides).
   */
  private loadExtras(id: string): void {
    this.http.get<WarehouseStock[]>(`${INVENTORY_API}/products/${id}/stock-by-warehouse`).subscribe({
      next: (rows) => this.warehouseStock.set(rows ?? []),
      error: () => this.warehouseStock.set([]),
    });
    this.http.get<StockLot[]>(`${INVENTORY_API}/products/${id}/lots`).subscribe({
      next: (rows) => {
        this.lots.set(rows ?? []);
        this.extrasLoaded.set(true);
      },
      error: () => {
        this.lots.set([]);
        this.extrasLoaded.set(true);
      },
    });
  }

  openMovement(type: MovementType): void {
    this.modalType.set(type);
    this.modalOpen.set(true);
  }

  closeMovement(): void {
    this.modalOpen.set(false);
    this.modalType.set(null);
  }

  onMovementSaved(updated: ProductDto): void {
    this.product.set(updated);
    this.closeMovement();
    const id = updated.id;
    this.loadExtras(id);
  }

  daysUntil(dateStr: string | null): number | null {
    if (!dateStr) return null;
    const d = new Date(dateStr).getTime();
    if (Number.isNaN(d)) return null;
    return Math.ceil((d - Date.now()) / (1000 * 60 * 60 * 24));
  }

  isNearExpiry(dateStr: string | null): boolean {
    const days = this.daysUntil(dateStr);
    return days !== null && days <= this.NEAR_EXPIRY_DAYS;
  }

  isExpired(dateStr: string | null): boolean {
    const days = this.daysUntil(dateStr);
    return days !== null && days < 0;
  }

  trackLot(_: number, l: StockLot): string {
    return l.id;
  }

  trackWarehouse(_: number, w: WarehouseStock): string {
    return w.warehouseId;
  }
}
