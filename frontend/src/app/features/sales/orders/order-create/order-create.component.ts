import { Component, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { formatMoney } from '../sales-format';

interface OrderLineForm {
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  taxRate: number;
}

interface OrderForm {
  customerName: string;
  customerRef: string;
  requestedDeliveryDate: string;
  currency: string;
  shippingAddress: string;
  notes: string;
  lines: OrderLineForm[];
}

interface CreatedOrderResponse {
  id?: string;
  data?: { id: string };
}

@Component({
  selector: 'nx-order-create',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './order-create.component.html',
  styleUrl: './order-create.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderCreateComponent {
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

  readonly form = signal<OrderForm>({
    customerName: '',
    customerRef: '',
    requestedDeliveryDate: this.getDefaultDeliveryDate(),
    currency: 'EUR',
    shippingAddress: '',
    notes: '',
    lines: [this.newLine()]
  });

  readonly subtotal = computed(() =>
    this.form().lines.reduce(
      (sum, l) => sum + l.quantity * l.unitPrice * (1 - l.discountPercent / 100),
      0
    )
  );

  readonly totalTax = computed(() =>
    this.form().lines.reduce((sum, l) => {
      const sub = l.quantity * l.unitPrice * (1 - l.discountPercent / 100);
      return sum + sub * (l.taxRate / 100);
    }, 0)
  );

  readonly total = computed(() => this.subtotal() + this.totalTax());

  readonly isValid = computed(() => {
    const f = this.form();
    return (
      f.customerName.trim().length > 0 &&
      f.lines.length > 0 &&
      f.lines.every(
        (l) => l.productName.trim().length > 0 && l.quantity > 0 && l.unitPrice >= 0
      )
    );
  });

  private getDefaultDeliveryDate(): string {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return d.toISOString().split('T')[0];
  }

  private newLine(): OrderLineForm {
    return { productCode: '', productName: '', quantity: 1, unitPrice: 0, discountPercent: 0, taxRate: 18 };
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

  updateLine(index: number, field: keyof OrderLineForm, value: string | number): void {
    this.form.update((f) => {
      const lines = [...f.lines];
      lines[index] = { ...lines[index], [field]: value };
      return { ...f, lines };
    });
  }

  updateField<K extends keyof OrderForm>(field: K, value: OrderForm[K]): void {
    this.form.update((f) => ({ ...f, [field]: value }));
  }

  getLineTotal(line: OrderLineForm): number {
    const sub = line.quantity * line.unitPrice * (1 - line.discountPercent / 100);
    return sub * (1 + line.taxRate / 100);
  }

  formatAmount(amount: number): string {
    return formatMoney(amount, this.form().currency);
  }

  submit(): void {
    if (!this.isValid()) {
      this.notif.error('Veuillez renseigner le client et au moins une ligne valide');
      return;
    }

    const f = this.form();
    const payload = {
      customerName: f.customerName.trim(),
      customerRef: f.customerRef.trim() || null,
      requestedDeliveryDate: f.requestedDeliveryDate || null,
      currency: f.currency,
      shippingAddress: f.shippingAddress.trim() || null,
      notes: f.notes.trim() || null,
      lines: f.lines.map((l) => ({
        productCode: l.productCode.trim() || null,
        productName: l.productName.trim(),
        quantity: l.quantity,
        unitPrice: l.unitPrice,
        taxRate: l.taxRate
      }))
    };

    this.isSubmitting.set(true);
    this.http.post<CreatedOrderResponse>('/api/v1/sales/orders', payload).subscribe({
      next: (res) => {
        this.notif.success('Commande créée avec succès');
        const id = res?.id ?? res?.data?.id;
        if (id) {
          this.router.navigate(['/sales/orders', id]);
        } else {
          this.router.navigate(['/sales/orders']);
        }
      },
      error: () => {
        this.notif.error('Erreur lors de la création de la commande');
        this.isSubmitting.set(false);
      }
    });
  }
}
