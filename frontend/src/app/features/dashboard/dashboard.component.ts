import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthStateService } from '../../core/services/auth-state.service';

interface KpiCard {
  label: string;
  value: string;
  subtitle: string;
  trend: 'up' | 'down' | 'stable';
  trendValue: string;
  icon: 'revenue' | 'invoice' | 'stock' | 'employees';
  color: 'blue' | 'amber' | 'green' | 'purple';
}

interface InvoiceRow {
  id: string;
  invoiceNumber?: string;
  customerName?: string;
  totalAmount: number;
  currency: string;
  status: string;
  dueDate?: string;
}

interface StockAlert {
  productId: string;
  productName: string;
  currentStock: number;
  reorderPoint: number;
}

@Component({
  selector: 'nx-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, DecimalPipe, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {

  readonly isLoading = signal(true);
  readonly authState = this.auth;

  readonly kpis = signal<KpiCard[]>([
    { label: 'CA mensuel',         value: '—',  subtitle: 'Avril 2026',   trend: 'up',     trendValue: '+12.4%', icon: 'revenue',   color: 'blue'   },
    { label: 'Factures en attente', value: '—',  subtitle: 'À valider',    trend: 'stable', trendValue: '0 ce mois', icon: 'invoice',   color: 'amber'  },
    { label: 'Valeur stock',        value: '—',  subtitle: 'Tous entrepôts', trend: 'down', trendValue: '-3.1%',  icon: 'stock',     color: 'green'  },
    { label: 'Employés actifs',     value: '—',  subtitle: 'CDI + CDD',    trend: 'up',     trendValue: '+2',     icon: 'employees', color: 'purple' },
  ]);

  readonly recentInvoices = signal<InvoiceRow[]>([]);
  readonly stockAlerts = signal<StockAlert[]>([]);

  readonly today = new Date();

  // Données sparkline CA (6 derniers mois — démo)
  readonly sparkPoints = '0,58 60,48 120,52 180,38 240,28 300,14';
  readonly sparkFill  = '0,58 60,48 120,52 180,38 240,28 300,14 300,80 0,80';
  readonly sparkLabels = ['Nov', 'Déc', 'Jan', 'Fév', 'Mar', 'Avr'];
  readonly sparkCircles = [
    { x: 0, y: 58 }, { x: 60, y: 48 }, { x: 120, y: 52 },
    { x: 180, y: 38 }, { x: 240, y: 28 }, { x: 300, y: 14 }
  ];

  constructor(
    private http: HttpClient,
    private auth: AuthStateService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.http.get<any>('/api/v1/finance/invoices?status=SUBMITTED&size=5').subscribe({
      next: (data: any) => {
        const invoices = data?.data ?? [];
        this.recentInvoices.set(invoices);
        // Mise à jour KPI factures
        if (invoices.length > 0) {
          this.kpis.update(k => {
            const updated = [...k];
            updated[1] = { ...updated[1], value: String(data?.meta?.total ?? invoices.length) };
            return updated;
          });
        }
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }
}
