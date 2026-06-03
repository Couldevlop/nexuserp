import { Component, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';

interface InvoiceLineForm {
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  taxRate: number;
}

interface InvoiceForm {
  customerName: string;
  customerEmail: string;
  customerAddress: string;
  dueDate: string;
  currency: string;
  taxRate: number;
  notes: string;
  lines: InvoiceLineForm[];
}

@Component({
  selector: 'nx-invoice-create',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './invoice-create.component.html',
  styleUrl: './invoice-create.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceCreateComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);

  readonly isSubmitting = signal(false);

  readonly currencies = ['EUR', 'XOF', 'USD', 'GBP'];

  readonly taxRates = [
    { label: '0%', value: 0 },
    { label: '2,1%', value: 2.1 },
    { label: '5,5%', value: 5.5 },
    { label: '10%', value: 10 },
    { label: '18% (UEMOA)', value: 18 },
    { label: '20%', value: 20 },
  ];

  // Reactive form data via signals
  readonly form = signal<InvoiceForm>({
    customerName: '',
    customerEmail: '',
    customerAddress: '',
    dueDate: this.getDefaultDueDate(),
    currency: 'EUR',
    taxRate: 20,
    notes: '',
    lines: [this.newLine()]
  });

  private getDefaultDueDate(): string {
    const d = new Date();
    d.setDate(d.getDate() + 30);
    return d.toISOString().split('T')[0];
  }

  private newLine(): InvoiceLineForm {
    return { description: '', quantity: 1, unitPrice: 0, discountPercent: 0, taxRate: 20 };
  }

  addLine(): void {
    this.form.update(f => ({ ...f, lines: [...f.lines, this.newLine()] }));
  }

  removeLine(index: number): void {
    this.form.update(f => ({
      ...f,
      lines: f.lines.filter((_, i) => i !== index)
    }));
  }

  updateLine(index: number, field: keyof InvoiceLineForm, value: string | number): void {
    this.form.update(f => {
      const lines = [...f.lines];
      lines[index] = { ...lines[index], [field]: value };
      return { ...f, lines };
    });
  }

  updateField<K extends keyof InvoiceForm>(field: K, value: InvoiceForm[K]): void {
    this.form.update(f => ({ ...f, [field]: value }));
  }

  getLineTotal(line: InvoiceLineForm): number {
    const sub = line.quantity * line.unitPrice * (1 - line.discountPercent / 100);
    return sub * (1 + line.taxRate / 100);
  }

  getSubtotal(): number {
    return this.form().lines.reduce((sum, l) =>
      sum + l.quantity * l.unitPrice * (1 - l.discountPercent / 100), 0);
  }

  getTotalTax(): number {
    return this.form().lines.reduce((sum, l) => {
      const sub = l.quantity * l.unitPrice * (1 - l.discountPercent / 100);
      return sum + sub * (l.taxRate / 100);
    }, 0);
  }

  getTotal(): number {
    return this.getSubtotal() + this.getTotalTax();
  }

  submit(): void {
    const f = this.form();
    if (!f.customerName.trim() || f.lines.length === 0) {
      this.notif.error('Veuillez remplir tous les champs obligatoires');
      return;
    }

    const payload = {
      customerName: f.customerName.trim(),
      customerEmail: f.customerEmail.trim() || null,
      customerAddress: f.customerAddress.trim() || null,
      dueDate: f.dueDate,
      currency: f.currency,
      taxRate: f.taxRate,
      notes: f.notes.trim() || null,
      lines: f.lines.map(l => ({
        description: l.description.trim(),
        quantity: l.quantity,
        unitPrice: l.unitPrice,
        discountPercent: l.discountPercent,
        taxRate: l.taxRate
      }))
    };

    this.isSubmitting.set(true);
    this.http.post<{ data: { id: string } }>('/api/v1/finance/invoices', payload).subscribe({
      next: (res) => {
        this.notif.success('Facture créée avec succès');
        const id = res?.data?.id ?? (res as any)?.id;
        this.router.navigate(['/finance/invoices', id]);
      },
      error: () => {
        this.notif.error('Erreur lors de la création de la facture');
        this.isSubmitting.set(false);
      }
    });
  }
}
