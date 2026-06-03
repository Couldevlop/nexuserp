import {
  Component, signal, computed, ChangeDetectionStrategy, inject,
  Input, Output, EventEmitter
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  ProductDto, MovementType, MOVEMENT_LABELS, INVENTORY_API
} from '../../inventory.types';

interface MovementFormState {
  type: MovementType;
  quantity: number;
  unitCost: number;
  currency: string;
  warehouseFrom: string;
  warehouseTo: string;
  reason: string;
  reference: string;
  lotOrSerial: string;
}

/**
 * Formulaire de mouvement de stock — utilisable en modal (depuis la fiche article).
 *
 * Contrat backend nexus-inventory :
 *  - IN          → POST /products/{id}/receive  { quantity, unitCost, currency, reference }
 *  - OUT         → POST /products/{id}/issue    { quantity, reference }
 *  - TRANSFER / ADJUSTMENT : endpoints non encore exposés → désactivés gracieusement.
 */
@Component({
  selector: 'nx-movement-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './movement-form.component.html',
  styleUrl: './movement-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MovementFormComponent {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  @Input({ required: true }) product!: ProductDto;
  /** Pré-sélectionne le type d'ajustement (bouton "Ajuster"). */
  @Input() set initialType(t: MovementType | null) {
    if (t) this.patch('type', t);
  }
  @Output() saved = new EventEmitter<ProductDto>();
  @Output() cancelled = new EventEmitter<void>();

  readonly isSubmitting = signal(false);

  readonly movementTypes: { value: MovementType; label: string; supported: boolean }[] = [
    { value: 'IN', label: MOVEMENT_LABELS.IN, supported: true },
    { value: 'OUT', label: MOVEMENT_LABELS.OUT, supported: true },
    { value: 'ADJUSTMENT', label: MOVEMENT_LABELS.ADJUSTMENT, supported: true },
    { value: 'TRANSFER', label: MOVEMENT_LABELS.TRANSFER, supported: false },
  ];

  readonly currencies = ['EUR', 'XOF', 'USD', 'GBP'];

  readonly form = signal<MovementFormState>({
    type: 'IN',
    quantity: 1,
    unitCost: 0,
    currency: 'XOF',
    warehouseFrom: '',
    warehouseTo: '',
    reason: '',
    reference: '',
    lotOrSerial: '',
  });

  readonly isIn = computed(() => this.form().type === 'IN');
  readonly isOut = computed(() => this.form().type === 'OUT');
  readonly isAdjustment = computed(() => this.form().type === 'ADJUSTMENT');
  readonly isTransfer = computed(() => this.form().type === 'TRANSFER');

  readonly typeSupported = computed(
    () => this.movementTypes.find(t => t.value === this.form().type)?.supported ?? false
  );

  patch<K extends keyof MovementFormState>(field: K, value: MovementFormState[K]): void {
    this.form.update(f => ({ ...f, [field]: value }));
  }

  cancel(): void {
    this.cancelled.emit();
  }

  submit(): void {
    const f = this.form();

    if (!this.typeSupported()) {
      this.notif.warning('Ce type de mouvement n\'est pas encore disponible.');
      return;
    }
    if (f.quantity == null || f.quantity <= 0) {
      this.notif.error('La quantité doit être positive.');
      return;
    }
    if (this.isIn() && (f.unitCost == null || f.unitCost <= 0)) {
      this.notif.error('Le coût unitaire doit être positif pour une entrée.');
      return;
    }
    if (this.isAdjustment() && !f.reason.trim()) {
      this.notif.error('Un motif est obligatoire pour un ajustement.');
      return;
    }

    const id = this.product.id;
    this.isSubmitting.set(true);

    if (this.isIn()) {
      this.http.post<ProductDto>(`${INVENTORY_API}/products/${id}/receive`, {
        quantity: f.quantity,
        unitCost: f.unitCost,
        currency: f.currency,
        reference: f.reference.trim() || null,
      }).subscribe({
        next: (p) => this.onSuccess(p, 'Entrée de stock enregistrée'),
        error: () => this.onError(),
      });
    } else if (this.isOut()) {
      this.http.post<ProductDto>(`${INVENTORY_API}/products/${id}/issue`, {
        quantity: f.quantity,
        reference: f.reference.trim() || null,
      }).subscribe({
        next: (p) => this.onSuccess(p, 'Sortie de stock enregistrée'),
        error: () => this.onError(),
      });
    } else if (this.isAdjustment()) {
      // adjustStock attend la nouvelle quantité absolue + un motif.
      this.http.post<ProductDto>(`${INVENTORY_API}/products/${id}/adjust`, {
        newQuantity: f.quantity,
        reason: f.reason.trim(),
      }).subscribe({
        next: (p) => this.onSuccess(p, 'Stock ajusté'),
        error: () => this.onError(),
      });
    }
  }

  private onSuccess(updated: ProductDto, message: string): void {
    this.notif.success(message);
    this.isSubmitting.set(false);
    this.saved.emit(updated);
  }

  private onError(): void {
    this.notif.error('Échec de l\'enregistrement du mouvement.');
    this.isSubmitting.set(false);
  }
}
