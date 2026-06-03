import {
  Component,
  OnInit,
  signal,
  computed,
  ChangeDetectionStrategy,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { ReportHistoryStore } from '../report-history.store';
import {
  ReportDto,
  ReportType,
  OutputFormat,
  REPORT_TYPE_LABELS,
  OUTPUT_FORMAT_LABELS,
} from '../reporting-format';

interface GenerateForm {
  type: ReportType;
  periodFrom: string;
  periodTo: string;
  outputFormat: OutputFormat;
}

@Component({
  selector: 'nx-report-generate',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './report-generate.component.html',
  styleUrl: './report-generate.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportGenerateComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notif = inject(NotificationService);
  private readonly store = inject(ReportHistoryStore);

  readonly isSubmitting = signal(false);

  readonly typeOptions: { value: ReportType; label: string }[] = (
    Object.keys(REPORT_TYPE_LABELS) as ReportType[]
  ).map((value) => ({ value, label: REPORT_TYPE_LABELS[value] }));

  readonly formatOptions: { value: OutputFormat; label: string }[] = (
    Object.keys(OUTPUT_FORMAT_LABELS) as OutputFormat[]
  ).map((value) => ({ value, label: OUTPUT_FORMAT_LABELS[value] }));

  readonly form = signal<GenerateForm>({
    type: 'BALANCE_SHEET',
    periodFrom: this.startOfYear(),
    periodTo: this.today(),
    outputFormat: 'XLSX',
  });

  /**
   * Format recommandé selon le type : le FEC français impose un fichier plat
   * (CSV), SYSCOHADA est généré en XLSX. On force le format adéquat lorsque le
   * type l'exige afin de rester aligné avec le générateur backend.
   */
  readonly recommendedFormat = computed<OutputFormat | null>(() => {
    switch (this.form().type) {
      case 'FEC_EXPORT':
        return 'CSV';
      case 'SYSCOHADA_EXPORT':
        return 'XLSX';
      default:
        return null;
    }
  });

  readonly isValid = computed(() => {
    const f = this.form();
    if (!f.type || !f.periodFrom || !f.periodTo) {
      return false;
    }
    return f.periodFrom <= f.periodTo;
  });

  readonly dateRangeInvalid = computed(() => {
    const f = this.form();
    return !!f.periodFrom && !!f.periodTo && f.periodFrom > f.periodTo;
  });

  ngOnInit(): void {
    // Préremplissage du type depuis un raccourci du tableau de bord.
    const requested = this.route.snapshot.queryParamMap.get('type');
    if (requested && requested in REPORT_TYPE_LABELS) {
      this.updateField('type', requested as ReportType);
    }
  }

  private today(): string {
    return new Date().toISOString().split('T')[0];
  }

  private startOfYear(): string {
    const d = new Date();
    return `${d.getFullYear()}-01-01`;
  }

  updateField<K extends keyof GenerateForm>(field: K, value: GenerateForm[K]): void {
    this.form.update((f) => {
      const next = { ...f, [field]: value };
      // Si le nouveau type impose un format, on l'applique automatiquement.
      if (field === 'type') {
        const rec = this.recommendedFor(value as ReportType);
        if (rec) {
          next.outputFormat = rec;
        }
      }
      return next;
    });
  }

  private recommendedFor(type: ReportType): OutputFormat | null {
    if (type === 'FEC_EXPORT') {
      return 'CSV';
    }
    if (type === 'SYSCOHADA_EXPORT') {
      return 'XLSX';
    }
    return null;
  }

  submit(): void {
    if (!this.isValid()) {
      this.notif.error('Veuillez choisir un type et une période valide (début ≤ fin)');
      return;
    }

    const f = this.form();
    const payload = {
      type: f.type,
      periodFrom: f.periodFrom,
      periodTo: f.periodTo,
      outputFormat: f.outputFormat,
    };

    this.isSubmitting.set(true);
    this.http.post<ReportDto>('/api/v1/reports', payload).subscribe({
      next: (dto) => {
        if (dto && dto.id) {
          this.store.upsert(dto);
        }
        this.notif.success('Génération du rapport lancée. Le fichier sera disponible sous peu.');
        this.router.navigate(['/reporting/reports']);
      },
      error: () => {
        this.notif.error('Erreur lors du lancement de la génération du rapport');
        this.isSubmitting.set(false);
      },
    });
  }
}
