import { Component, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  ProductDto, StockValuationMethod, VALUATION_LABELS, INVENTORY_API
} from '../../inventory.types';

interface ProductForm {
  productCode: string;
  name: string;
  description: string;
  category: string;
  unit: string;
  valuationMethod: StockValuationMethod;
  reorderPoint: number;
  reorderQuantity: number;
  safetyStock: number;
  initialQty: number;
  warehouseId: string;
  warehouseLocation: string;
  serialTracked: boolean;
  lotTracked: boolean;
  expiryTracked: boolean;
}

@Component({
  selector: 'nx-product-create',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './product-create.component.html',
  styleUrl: './product-create.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductCreateComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);

  readonly isSubmitting = signal(false);

  readonly units = ['UNIT', 'KG', 'L', 'M', 'M2', 'M3', 'BOX', 'PALLET', 'HOUR'];

  readonly valuationMethods: { value: StockValuationMethod; label: string }[] =
    (Object.keys(VALUATION_LABELS) as StockValuationMethod[]).map(v => ({
      value: v, label: VALUATION_LABELS[v]
    }));

  readonly form = signal<ProductForm>({
    productCode: '',
    name: '',
    description: '',
    category: '',
    unit: 'UNIT',
    valuationMethod: 'PMP_REALTIME',
    reorderPoint: 0,
    reorderQuantity: 0,
    safetyStock: 0,
    initialQty: 0,
    warehouseId: '',
    warehouseLocation: '',
    serialTracked: false,
    lotTracked: false,
    expiryTracked: false,
  });

  patch<K extends keyof ProductForm>(field: K, value: ProductForm[K]): void {
    this.form.update(f => ({ ...f, [field]: value }));
  }

  submit(): void {
    const f = this.form();

    if (!f.productCode.trim()) {
      this.notif.error('Le code article est obligatoire.');
      return;
    }
    if (!f.name.trim()) {
      this.notif.error('La désignation est obligatoire.');
      return;
    }
    if (f.reorderPoint < 0 || f.initialQty < 0) {
      this.notif.error('Les quantités ne peuvent pas être négatives.');
      return;
    }

    const payload = {
      productCode: f.productCode.trim(),
      name: f.name.trim(),
      description: f.description.trim() || null,
      category: f.category.trim() || null,
      unit: f.unit,
      reorderPoint: f.reorderPoint,
      reorderQuantity: f.reorderQuantity,
      safetyStock: f.safetyStock,
      valuationMethod: f.valuationMethod,
      warehouseId: f.warehouseId.trim() || null,
      warehouseLocation: f.warehouseLocation.trim() || null,
      serialTracked: f.serialTracked,
      lotTracked: f.lotTracked,
      expiryTracked: f.expiryTracked,
    };

    this.isSubmitting.set(true);
    this.http.post<ProductDto>(`${INVENTORY_API}/products`, payload).subscribe({
      next: (created) => {
        const id = created?.id;
        this.notif.success('Article créé avec succès');
        if (id) {
          this.router.navigate(['/inventory/products', id]);
        } else {
          this.router.navigate(['/inventory/products']);
        }
      },
      error: () => {
        this.notif.error('Erreur lors de la création de l\'article');
        this.isSubmitting.set(false);
      }
    });
  }
}
