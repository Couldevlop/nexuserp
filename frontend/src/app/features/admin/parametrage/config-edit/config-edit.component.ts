import {
  Component,
  ChangeDetectionStrategy,
  Input,
  Output,
  EventEmitter,
  signal,
  computed,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfigService } from '../config.service';
import {
  ConfigParam,
  ConfigUpsertRequest,
  ConfigValueType,
  categoryLabel,
  valueTypeLabel
} from '../config.model';

/**
 * Formulaire d'édition d'un paramètre (modal, réutilise nx-modal).
 *
 * Sécurité (OWASP) : pour un SECRET, l'input est de type password et la
 * valeur réelle n'est JAMAIS pré-remplie dans le DOM. Laisser le champ
 * vide conserve la valeur existante (comportement write-only côté backend).
 */
@Component({
  selector: 'nx-config-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent],
  templateUrl: './config-edit.component.html',
  styleUrl: './config-edit.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfigEditComponent {
  private readonly configService = inject(ConfigService);
  private readonly notif = inject(NotificationService);

  /** Paramètre à éditer (depuis le catalogue ou le backend). */
  @Input({ required: true }) set param(value: ConfigParam) {
    this._param.set(value);
    // On ne pré-remplit JAMAIS la valeur d'un secret dans le formulaire.
    this.formValue.set(value.secret ? '' : (value.value ?? ''));
    this.formBoolValue.set(value.valueType === 'BOOLEAN' ? this.parseBool(value.value) : false);
    this.formDescription.set(value.description ?? '');
    this.jsonError.set(null);
    this.submitted.set(false);
  }
  @Input() open = false;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<ConfigParam>();

  private readonly _param = signal<ConfigParam | null>(null);
  readonly current = this._param.asReadonly();

  readonly formValue = signal('');
  readonly formBoolValue = signal(false);
  readonly formDescription = signal('');
  readonly submitted = signal(false);
  readonly isSaving = signal(false);
  readonly jsonError = signal<string | null>(null);

  readonly isSecret = computed(() => this._param()?.secret === true);
  readonly valueType = computed<ConfigValueType>(() => this._param()?.valueType ?? 'STRING');

  readonly modalTitle = computed(() => {
    const p = this._param();
    return p ? `Modifier — ${p.key}` : 'Modifier le paramètre';
  });

  readonly typeLabel = computed(() => valueTypeLabel(this.valueType()));
  readonly catLabel = computed(() => {
    const p = this._param();
    return p ? categoryLabel(p.category) : '';
  });

  /** Champ requis (texte/nombre/json) sauf secret (vide = conserver). */
  readonly isRequiredEmpty = computed(() => {
    const p = this._param();
    if (!p) return false;
    if (p.secret) return false; // write-only : vide autorisé
    if (p.valueType === 'BOOLEAN') return false;
    return this.formValue().trim().length === 0;
  });

  onSave(): void {
    const p = this._param();
    if (!p) return;

    this.submitted.set(true);
    this.jsonError.set(null);

    let outValue: string;
    if (p.valueType === 'BOOLEAN') {
      outValue = this.formBoolValue() ? 'true' : 'false';
    } else {
      const raw = this.formValue();
      // Secret laissé vide => on n'envoie pas de nouvelle valeur (conservation).
      if (p.secret && raw.trim().length === 0) {
        outValue = '';
      } else {
        if (this.isRequiredEmpty()) {
          return;
        }
        if (p.valueType === 'JSON' && raw.trim().length > 0) {
          try {
            JSON.parse(raw);
          } catch {
            this.jsonError.set('JSON invalide.');
            return;
          }
        }
        if (p.valueType === 'NUMBER' && raw.trim().length > 0 && Number.isNaN(Number(raw))) {
          return;
        }
        outValue = raw;
      }
    }

    const request: ConfigUpsertRequest = {
      value: outValue,
      type: p.valueType,
      category: p.category,
      secret: p.secret,
      description: this.formDescription().trim() || null
    };

    this.isSaving.set(true);
    this.configService.upsert(p.key, request).subscribe({
      next: (updated) => {
        this.isSaving.set(false);
        this.notif.success('Paramètre enregistré');
        this.saved.emit(updated);
      },
      error: () => {
        this.isSaving.set(false);
        this.notif.error('Échec de l\'enregistrement du paramètre');
      }
    });
  }

  onClose(): void {
    this.closed.emit();
  }

  private parseBool(value: string | null): boolean {
    return value === 'true' || value === '1';
  }

  onBoolToggle(checked: boolean): void {
    this.formBoolValue.set(checked);
  }
}
