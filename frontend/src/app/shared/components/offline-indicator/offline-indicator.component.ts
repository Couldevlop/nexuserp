import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConnectivityService } from '../../../core/offline/connectivity.service';
import { OutboxService } from '../../../core/offline/outbox.service';
import { SyncService } from '../../../core/offline/sync.service';

/** Variantes visuelles du badge de connectivité. */
type IndicatorVariant = 'offline' | 'syncing' | 'synced' | 'conflict';

interface IndicatorView {
  variant: IndicatorVariant;
  label: string;
  /** Affiche un point/spinner animé pour l'état "syncing". */
  busy: boolean;
}

/**
 * Badge/bannière de connectivité offline-first.
 *
 * Lie `ConnectivityService.isOnline`, `OutboxService.pendingCount` et
 * `SyncService.status` pour afficher l'un des états :
 *   - Hors ligne (avec compteur d'opérations en attente),
 *   - Synchronisation en cours,
 *   - Conflit(s) à résoudre,
 *   - À jour.
 *
 * Accessible : role="status" + aria-live="polite" (annonce non intrusive).
 * Libellés en français, prêts pour i18n (extractibles).
 */
@Component({
  selector: 'nx-offline-indicator',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './offline-indicator.component.html',
  styleUrl: './offline-indicator.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OfflineIndicatorComponent {

  private readonly connectivity = inject(ConnectivityService);
  private readonly outbox = inject(OutboxService);
  private readonly sync = inject(SyncService);

  readonly isOnline = this.connectivity.isOnline;
  readonly pendingCount = this.outbox.pendingCount;
  readonly conflictCount = computed(() => this.outbox.conflicts().length);

  /** Vue calculée : variante + libellé localisé selon l'état combiné. */
  readonly view = computed<IndicatorView>(() => {
    const online = this.connectivity.isOnline();
    const pending = this.outbox.pendingCount();
    const conflicts = this.outbox.conflicts().length;
    const status = this.sync.status();

    if (!online) {
      const suffix = pending > 0 ? ` — ${pending} opération${pending > 1 ? 's' : ''} en attente` : '';
      return { variant: 'offline', label: `Hors ligne${suffix}`, busy: false };
    }
    if (status === 'syncing') {
      return { variant: 'syncing', label: 'Synchronisation…', busy: true };
    }
    if (conflicts > 0) {
      const label = `${conflicts} conflit${conflicts > 1 ? 's' : ''} à résoudre`;
      return { variant: 'conflict', label, busy: false };
    }
    if (pending > 0) {
      return { variant: 'syncing', label: `${pending} en attente`, busy: false };
    }
    return { variant: 'synced', label: 'À jour', busy: false };
  });

  /** Déclenche une synchronisation manuelle (clic utilisateur). */
  retry(): void {
    void this.sync.syncNow();
  }
}
