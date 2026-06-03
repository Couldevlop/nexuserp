import { Component, OnInit, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';

export interface InvoiceDetail {
  id: string;
  invoiceNumber: string;
  status: string;
  customerName: string;
  customerEmail?: string;
  customerAddress?: string;
  invoiceDate: string;
  dueDate: string;
  currency: string;
  taxRate: number;
  subtotalAmount: number;
  taxAmount: number;
  totalAmount: number;
  notes?: string;
  lines: InvoiceLineDetail[];
  createdAt: string;
  updatedAt: string;
}

export interface InvoiceLineDetail {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  subtotal: number;
  taxAmount: number;
  total: number;
}

@Component({
  selector: 'nx-invoice-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './invoice-detail.component.html',
  styleUrl: './invoice-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceDetailComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);
  readonly authState = inject(AuthStateService);

  readonly isLoading = signal(true);
  readonly invoice = signal<InvoiceDetail | null>(null);
  readonly isApproving = signal(false);

  readonly canApprove = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/finance/invoices']);
      return;
    }
    this.loadInvoice(id);
  }

  private loadInvoice(id: string): void {
    this.http.get<{ data: InvoiceDetail }>(`/api/v1/finance/invoices/${id}`).subscribe({
      next: (res) => {
        this.invoice.set(res.data ?? (res as any));
        const inv = this.invoice();
        this.canApprove.set(
          inv?.status === 'SUBMITTED' &&
          this.authState.hasAnyRole('FINANCE_MANAGER', 'TENANT_ADMIN')
        );
        this.isLoading.set(false);
      },
      error: () => {
        this.notif.error('Facture introuvable');
        this.router.navigate(['/finance/invoices']);
      }
    });
  }

  approve(): void {
    const inv = this.invoice();
    if (!inv) return;
    this.isApproving.set(true);
    this.http.post(`/api/v1/finance/invoices/${inv.id}/approve`, {}).subscribe({
      next: () => {
        this.notif.success('Facture approuvée avec succès');
        this.loadInvoice(inv.id);
        this.isApproving.set(false);
      },
      error: () => {
        this.notif.error('Échec de l\'approbation');
        this.isApproving.set(false);
      }
    });
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      DRAFT: 'Brouillon', SUBMITTED: 'En attente', APPROVED: 'Approuvée',
      PAID: 'Payée', OVERDUE: 'En retard', CANCELLED: 'Annulée'
    };
    return labels[status] ?? status;
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      DRAFT: 'nx-badge--neutral', SUBMITTED: 'nx-badge--warning',
      APPROVED: 'nx-badge--info', PAID: 'nx-badge--success',
      OVERDUE: 'nx-badge--error', CANCELLED: 'nx-badge--neutral'
    };
    return classes[status] ?? 'nx-badge--neutral';
  }
}
