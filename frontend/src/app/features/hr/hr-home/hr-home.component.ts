import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface HrKpi {
  label: string;
  value: string | number;
  subtitle: string;
  icon: 'users' | 'leave' | 'hire' | 'turnover';
  color: 'blue' | 'amber' | 'green' | 'rose';
  trend?: string;
  trendDir?: 'up' | 'down' | 'stable';
}

interface Department {
  name: string;
  count: number;
  pct: number;
  color: string;
}

interface LeaveRequest {
  name: string;
  type: string;
  from: string;
  to: string;
  days: number;
  status: 'PENDING' | 'APPROVED' | 'REFUSED';
}

interface ContractAlert {
  name: string;
  contract: string;
  endDate: string;
  daysLeft: number;
}

@Component({
  selector: 'nx-hr-home',
  standalone: true,
  imports: [CommonModule, RouterLink, DecimalPipe, DatePipe],
  templateUrl: './hr-home.component.html',
  styleUrl: './hr-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HrHomeComponent implements OnInit {

  readonly isLoading = signal(false);

  readonly kpis = signal<HrKpi[]>([
    { label: 'Employés actifs',    value: 156,    subtitle: 'CDI · CDD · Intérim', icon: 'users',    color: 'blue',  trend: '+2 ce mois',  trendDir: 'up' },
    { label: 'En congé auj.',      value: 8,      subtitle: 'Congés approuvés',    icon: 'leave',    color: 'amber', trend: '5.1% des eff.', trendDir: 'stable' },
    { label: 'Recrutements',       value: 3,      subtitle: 'Postes ouverts',      icon: 'hire',     color: 'green', trend: 'ATS actif',   trendDir: 'up' },
    { label: 'Taux de turnover',   value: '4.2%', subtitle: 'Sur 12 mois glissants', icon: 'turnover', color: 'rose', trend: '-0.8pp vs N-1', trendDir: 'down' },
  ]);

  readonly departments = signal<Department[]>([
    { name: 'Production',   count: 45, pct: 29, color: '#1E40AF' },
    { name: 'Ventes',       count: 32, pct: 21, color: '#059669' },
    { name: 'Logistique',   count: 26, pct: 17, color: '#D97706' },
    { name: 'Finance',      count: 18, pct: 12, color: '#7C3AED' },
    { name: 'IT',           count: 15, pct: 10, color: '#0891B2' },
    { name: 'RH',           count: 12, pct: 8,  color: '#DB2777' },
    { name: 'Direction',    count: 8,  pct: 5,  color: '#374151' },
  ]);

  readonly leaveRequests = signal<LeaveRequest[]>([
    { name: 'Marie Dupont',    type: 'Congés payés',  from: '2026-05-05', to: '2026-05-16', days: 10, status: 'PENDING'  },
    { name: 'Jean-Paul Koné',  type: 'Maladie',       from: '2026-04-28', to: '2026-05-02', days: 5,  status: 'APPROVED' },
    { name: 'Amina Touré',     type: 'Congés payés',  from: '2026-05-12', to: '2026-05-23', days: 10, status: 'APPROVED' },
    { name: 'Pierre Martin',   type: 'Congé paternité', from: '2026-05-01', to: '2026-05-14', days: 14, status: 'PENDING' },
    { name: 'Sophie Bernard',  type: 'RTT',           from: '2026-04-30', to: '2026-04-30', days: 1,  status: 'APPROVED' },
  ]);

  readonly contractAlerts = signal<ContractAlert[]>([
    { name: 'Lucas Renard',   contract: 'CDD',    endDate: '2026-05-31', daysLeft: 35 },
    { name: 'Fatou Diallo',   contract: 'Intérim', endDate: '2026-05-15', daysLeft: 19 },
    { name: 'Marc Lefevre',   contract: 'CDD',    endDate: '2026-06-30', daysLeft: 65 },
  ]);

  readonly payrollSummary = {
    brutTotal:  '284 500 €',
    chargesTotal: '127 850 €',
    netTotal:   '156 650 €',
    month:      'Avril 2026',
  };

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    // TODO: charger depuis /api/v1/hr/dashboard
  }

  getDaysLeftColor(d: number): string {
    if (d <= 20) return 'error';
    if (d <= 45) return 'warning';
    return 'success';
  }
}
