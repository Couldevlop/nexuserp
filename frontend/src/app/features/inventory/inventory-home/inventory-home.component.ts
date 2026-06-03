import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface InvKpi {
  label: string;
  value: string | number;
  subtitle: string;
  icon: 'sku' | 'alert' | 'value' | 'warehouse';
  color: 'blue' | 'amber' | 'green' | 'purple';
  trend?: string;
  trendDir?: 'up' | 'down' | 'stable';
}

interface Category {
  name: string;
  pct: number;
  value: string;
  color: string;
}

interface LowStockItem {
  sku: string;
  name: string;
  warehouse: string;
  currentQty: number;
  minQty: number;
  unit: string;
  severity: 'critical' | 'warning';
}

interface ExpiryAlert {
  lotNumber: string;
  product: string;
  qty: number;
  unit: string;
  expiresAt: string;
  daysLeft: number;
}

interface Movement {
  label: string;
  in: number;
  out: number;
  maxVal: number;
}

@Component({
  selector: 'nx-inventory-home',
  standalone: true,
  imports: [CommonModule, RouterLink, DecimalPipe],
  templateUrl: './inventory-home.component.html',
  styleUrl: './inventory-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InventoryHomeComponent implements OnInit {

  readonly kpis = signal<InvKpi[]>([
    { label: 'Articles en stock', value: '1 247',    subtitle: 'Tous entrepôts',     icon: 'sku',      color: 'blue',   trend: '+23 ce mois',  trendDir: 'up' },
    { label: 'Alertes rupture',   value: 12,          subtitle: 'À réapprovisionner', icon: 'alert',    color: 'amber',  trend: '→ 0 critique', trendDir: 'stable' },
    { label: 'Valeur totale',     value: '284 500 €', subtitle: 'Coût moyen pondéré', icon: 'value',    color: 'green',  trend: '+5.2%',        trendDir: 'up' },
    { label: 'Entrepôts actifs',  value: 3,           subtitle: 'Paris · Lyon · Abidjan', icon: 'warehouse', color: 'purple', trend: 'Opérationnels', trendDir: 'stable' },
  ]);

  readonly categories = signal<Category[]>([
    { name: 'Matières premières', pct: 35, value: '99 575 €',  color: '#1E40AF' },
    { name: 'Produits finis',     pct: 28, value: '79 660 €',  color: '#059669' },
    { name: 'Emballages',         pct: 18, value: '51 210 €',  color: '#D97706' },
    { name: 'Consommables',       pct: 12, value: '34 140 €',  color: '#7C3AED' },
    { name: 'Autres',             pct: 7,  value: '19 915 €',  color: '#6B7280' },
  ]);

  readonly lowStockItems = signal<LowStockItem[]>([
    { sku: 'MAT-001',  name: 'Acier inox 316L (kg)',    warehouse: 'Paris',    currentQty: 45,  minQty: 200,  unit: 'kg', severity: 'critical' },
    { sku: 'EMP-032',  name: 'Boîtes carton 40x30x20', warehouse: 'Lyon',     currentQty: 120, minQty: 500,  unit: 'u',  severity: 'warning'  },
    { sku: 'MAT-018',  name: 'Résine époxy (L)',        warehouse: 'Paris',    currentQty: 8,   minQty: 50,   unit: 'L',  severity: 'critical' },
    { sku: 'CONS-005', name: 'Gants latex M (boîte)',   warehouse: 'Abidjan', currentQty: 15,  minQty: 30,   unit: 'bte', severity: 'warning' },
    { sku: 'PF-201',   name: 'Circuit PCB v2 (u)',      warehouse: 'Paris',    currentQty: 30,  minQty: 100,  unit: 'u',  severity: 'warning'  },
  ]);

  readonly expiryAlerts = signal<ExpiryAlert[]>([
    { lotNumber: 'LOT-2024-041', product: 'Solvant nettoyant',  qty: 40,  unit: 'L',  expiresAt: '2026-05-10', daysLeft: 14 },
    { lotNumber: 'LOT-2024-038', product: 'Colle industrielle', qty: 12,  unit: 'kg', expiresAt: '2026-06-15', daysLeft: 50 },
    { lotNumber: 'LOT-2024-052', product: 'Peinture primer',    qty: 25,  unit: 'L',  expiresAt: '2026-05-31', daysLeft: 35 },
  ]);

  readonly movements = signal<Movement[]>([
    { label: 'Lun', in: 85,  out: 62,  maxVal: 120 },
    { label: 'Mar', in: 42,  out: 95,  maxVal: 120 },
    { label: 'Mer', in: 110, out: 78,  maxVal: 120 },
    { label: 'Jeu', in: 67,  out: 53,  maxVal: 120 },
    { label: 'Ven', in: 93,  out: 115, maxVal: 120 },
    { label: 'Sam', in: 30,  out: 22,  maxVal: 120 },
    { label: 'Dim', in: 15,  out: 8,   maxVal: 120 },
  ]);

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    // TODO: charger depuis /api/v1/inventory/dashboard
  }

  pctBar(val: number, max: number): number {
    return Math.min(100, Math.round((val / max) * 100));
  }
}
