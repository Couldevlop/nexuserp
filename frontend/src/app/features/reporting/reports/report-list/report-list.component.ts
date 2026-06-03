import {
  Component,
  OnInit,
  signal,
  computed,
  ChangeDetectionStrategy,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { NotificationService } from '../../../../core/services/notification.service';
import { ReportHistoryStore } from '../report-history.store';
import {
  ReportDto,
  ReportType,
  REPORT_TYPE_LABELS,
  reportTypeLabel,
  reportStatusLabel,
  reportStatusBadge,
  isReady,
  isPending,
} from '../reporting-format';

@Component({
  selector: 'nx-report-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './report-list.component.html',
  styleUrl: './report-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);
  private readonly store = inject(ReportHistoryStore);

  readonly isLoading = signal(true);
  readonly reports = signal<ReportDto[]>([]);
  readonly typeFilter = signal<string>('');

  /** Options de filtre par type (alimentées par le contrat backend). */
  readonly typeOptions: { value: ReportType | ''; label: string }[] = [
    { value: '', label: 'Tous les types' },
    ...(Object.keys(REPORT_TYPE_LABELS) as ReportType[]).map((value) => ({
      value,
      label: REPORT_TYPE_LABELS[value],
    })),
  ];

  readonly filteredReports = computed(() => {
    const t = this.typeFilter();
    const all = this.reports();
    return t ? all.filter((r) => r.type === t) : all;
  });

  readonly totalItems = computed(() => this.reports().length);

  ngOnInit(): void {
    this.refresh();
  }

  /**
   * Recharge l'historique local puis interroge le serveur pour rafraîchir le
   * statut des jobs encore en cours. Les rapports terminés ne sont pas
   * re-sollicités inutilement.
   */
  refresh(): void {
    this.isLoading.set(true);
    const local = this.store.list();
    this.reports.set(local);

    const pending = local.filter((r) => isPending(r.status));
    if (pending.length === 0) {
      this.isLoading.set(false);
      return;
    }

    forkJoin(
      pending.map((r) =>
        this.http.get<ReportDto>(`/api/v1/reports/${encodeURIComponent(r.id)}/status`).pipe(
          map((dto) => dto),
          catchError(() => of(null)),
        ),
      ),
    ).subscribe({
      next: (results) => {
        for (const dto of results) {
          if (dto && dto.id) {
            this.store.upsert(dto);
          }
        }
        this.reports.set(this.store.list());
        this.isLoading.set(false);
      },
      error: () => {
        this.notif.error('Erreur lors de l’actualisation des rapports');
        this.isLoading.set(false);
      },
    });
  }

  onTypeChange(type: string): void {
    this.typeFilter.set(type);
  }

  /**
   * Télécharge un rapport prêt. Le backend renvoie un downloadUrl (URL
   * présignée MinIO ou chemin de téléchargement). On ouvre l'URL fournie par
   * le serveur — jamais une URL construite à partir d'une saisie utilisateur.
   */
  download(report: ReportDto): void {
    if (!isReady(report.status) || !report.downloadUrl) {
      return;
    }
    window.open(report.downloadUrl, '_blank', 'noopener,noreferrer');
  }

  remove(report: ReportDto): void {
    this.store.remove(report.id);
    this.reports.set(this.store.list());
    this.notif.info('Rapport retiré de l’historique local');
  }

  typeLabel(type: string): string {
    return reportTypeLabel(type);
  }

  statusLabel(status: string): string {
    return reportStatusLabel(status);
  }

  statusBadge(status: string): string {
    return reportStatusBadge(status);
  }

  canDownload(report: ReportDto): boolean {
    return isReady(report.status) && !!report.downloadUrl;
  }
}
