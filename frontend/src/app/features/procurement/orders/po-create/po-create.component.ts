import { Component, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { formatMoney } from '../procurement-format';

interface PoLineForm {
  productCode: string;
  description: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
}

interface PoForm {
  supplierName: string;
  supplierId: string;
  expectedDeliveryDate: string;
  currency: string;
  notes: string;
  lines: PoLineForm[];
}

interface CreatedPoResponse {
  id?: string;
  data?: { id: string };
}

@Component({
  selector: 'nx-po-create',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './po-create.component.html',
  styleUrl: './po-create.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PoCreateComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);

  readonly isSubmitting = signal(false);

  readonly currencies = ['EUR', 'XOF', 'USD', 'GBP'];

  readonly taxRates = [
    { label: '0%', value: 0 },
    { label: '5,5%', value: 5.5 },
    { label: '10%', value: 10 },
    { label: '18% (UEMOA)', value: 18 },
    { label: '20%', value: 20 },
  ];

  readonly form = signal<PoForm>({
    supplierName: '',
    supplierId: '',
    expectedDeliveryDate: this.getDefaultDeliveryDate(),
    currency: 'EUR',
    notes: '',
    lines: [this.newLine()]
  });

  readonly subtotal = computed(() =>
    this.form().lines.reduce((sum, l) => sum + l.quantity * l.unitPrice, 0)
  );

  readonly totalTax = computed(() =>
    this.form().lines.reduce((sum, l) => {
      const sub = l.quantity * l.unitPrice;
      return sum + sub * (l.taxRate / 100);
    }, 0)
  );

  readonly total = computed(() => this.subtotal() + this.totalTax());

  readonly isValid = computed(() => {
    const f = this.form();
    return (
      f.supplierName.trim().length > 0 &&
      f.lines.length > 0 &&
      f.lines.every(
        (l) => l.description.trim().length > 0 && l.quantity > 0 && l.unitPrice >= 0
      )
    );
  });

  private getDefaultDeliveryDate(): string {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return d.toISOString().split('T')[0];
  }

  private newLine(): PoLineForm {
    return { productCode: '', description: '', quantity: 1, unitPrice: 0, taxRate: 18 };
  }

  addLine(): void {
    this.form.update((f) => ({ ...f, lines: [...f.lines, this.newLine()] }));
  }

  removeLine(index: number): void {
    this.form.update((f) => ({
      ...f,
      lines: f.lines.filter((_, i) => i !== index)
    }));
  }

  updateLine(index: number, field: keyof PoLineForm, value: string | number): void {
    this.form.update((f) => {
      const lines = [...f.lines];
      lines[index] = { ...lines[index], [field]: value };
      return { ...f, lines };
    });
  }

  updateField<K extends keyof PoForm>(field: K, value: PoForm[K]): void {
    this.form.update((f) => ({ ...f, [field]: value }));
  }

  getLineTotal(line: PoLineForm): number {
    const sub = line.quantity * line.unitPrice;
    return sub * (1 + line.taxRate / 100);
  }

  formatAmount(amount: number): string {
    return formatMoney(amount, this.form().currency);
  }

  submit(): void {
    if (!this.isValid()) {
      this.notif.error('Veuillez renseigner le fournisseur et au moins une ligne valide');
      return;
    }

    const f = this.form();
    const payload = {
      supplierId: f.supplierId.trim() || null,
      supplierName: f.supplierName.trim(),
      expectedDeliveryDate: f.expectedDeliveryDate || null,
      currency: f.currency,
      notes: f.notes.trim() || null,
      lines: f.lines.map((l) => ({
        productCode: l.productCode.trim() || null,
        description: l.description.trim(),
        quantity: l.quantity,
        unitPrice: l.unitPrice,
        taxRate: l.taxRate
      }))
    };

    this.isSubmitting.set(true);
    this.http.post<CreatedPoResponse>('/api/v1/procurement/purchase-orders', payload).subscribe({
      next: (res) => {
        this.notif.success("Commande d'achat créée avec succès");
        const id = res?.id ?? res?.data?.id;
        if (id) {
          this.router.navigate(['/procurement/orders', id]);
        } else {
          this.router.navigate(['/procurement/orders']);
        }
      },
      error: () => {
        this.notif.error("Erreur lors de la création de la commande d'achat");
        this.isSubmitting.set(false);
      }
    });
  }
}
