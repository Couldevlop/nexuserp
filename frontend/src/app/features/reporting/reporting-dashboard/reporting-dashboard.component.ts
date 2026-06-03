import {
  Component,
  signal,
  ChangeDetectionStrategy,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { ReportType, REPORT_TYPE_LABELS } from '../reports/reporting-format';

/**
 * KPI synthétique optionnel.
 *
 * nexus-reporting n'expose PAS d'endpoint d'agrégation KPI : seules la
 * génération de rapports et la consultation de statut existent. On tente donc
 * un appel best-effort vers un éventuel endpoint KPI ; en son absence (404 /
 * erreur), les cartes restent à « — » sans aucune donnée fabriquée.
 */
interface ReportingKpis {
  revenue?: number | null;
  dso?: number | null;
  stockValue?: number | null;
  margin?: number | null;
  currency?: string | null;
}

interface QuickReport {
  type: ReportType;
  label: string;
  description: string;
  icon: string;
}

@Component({
  selector: 'nx-reporting-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, StatCardComponent],
  templateUrl: './reporting-dashboard.component.html',
  styleUrl: './reporting-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportingDashboardComponent {
  private readonly http = inject(HttpClient);

  readonly isLoading = signal(true);
  /** Vrai uniquement si un endpoint KPI réel a répondu avec des valeurs. */
  readonly hasKpis = signal(false);
  readonly kpis = signal<ReportingKpis | null>(null);

  /** Raccourcis vers les rapports clés (préremplit le type dans le formulaire). */
  readonly quickReports: QuickReport[] = [
    {
      type: 'BALANCE_SHEET',
      label: REPORT_TYPE_LABELS.BALANCE_SHEET,
      description: 'État du patrimoine à une date donnée.',
      icon: '⚖',
    },
    {
      type: 'INCOME_STATEMENT',
      label: REPORT_TYPE_LABELS.INCOME_STATEMENT,
      description: 'Produits et charges de la période.',
      icon: '📈',
    },
    {
      type: 'TRIAL_BALANCE',
      label: REPORT_TYPE_LABELS.TRIAL_BALANCE,
      description: 'Soldes de tous les comptes.',
      icon: '🧮',
    },
    {
      type: 'GENERAL_LEDGER',
      label: REPORT_TYPE_LABELS.GENERAL_LEDGER,
      description: 'Détail des écritures par compte.',
      icon: '📚',
    },
    {
      type: 'FEC_EXPORT',
      label: REPORT_TYPE_LABELS.FEC_EXPORT,
      description: 'Fichier des Écritures Comptables (DGFiP).',
      icon: '🇫🇷',
    },
    {
      type: 'SYSCOHADA_EXPORT',
      label: REPORT_TYPE_LABELS.SYSCOHADA_EXPORT,
      description: 'États de synthèse OHADA (CI/UEMOA).',
      icon: '🌍',
    },
  ];

  constructor() {
    this.loadKpis();
  }

  private loadKpis(): void {
    this.isLoading.set(true);
    // Appel best-effort — l'endpoint peut ne pas exister (graceful degradation).
    this.http
      .get<ReportingKpis>('/api/v1/reports/kpis')
      .pipe(catchError(() => of(null)))
      .subscribe((res) => {
        if (res && this.hasAnyValue(res)) {
          this.kpis.set(res);
          this.hasKpis.set(true);
        } else {
          this.kpis.set(null);
          this.hasKpis.set(false);
        }
        this.isLoading.set(false);
      });
  }

  private hasAnyValue(k: ReportingKpis): boolean {
    return [k.revenue, k.dso, k.stockValue, k.margin].some(
      (v) => v !== null && v !== undefined,
    );
  }

  /** Valeur formatée d'un KPI ou « — » si non disponible. */
  display(value: number | null | undefined, suffix = ''): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '—';
    }
    return `${value.toLocaleString('fr-FR')}${suffix}`;
  }
}
