import {
  Component,
  ChangeDetectionStrategy,
  EventEmitter,
  Input,
  Output,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  BarcodeScannerService,
  BarcodeUnavailableError,
} from '../../../core/native/barcode-scanner.service';
import { PlatformService } from '../../../core/native/platform.service';
import { NotificationService } from '../../../core/services/notification.service';

/**
 * Bouton réutilisable de scan de code-barres / QR (`nx-barcode-button`).
 *
 * - Visible/utile uniquement sur natif (caméra). Sur le web il est masqué par
 *   défaut (`hideOnWeb`), afin de ne pas perturber l'expérience PWA desktop.
 * - Émet `scanned` avec la valeur lue ; ne fait rien sur annulation.
 * - Gère les erreurs (permission, support) via le service de notification.
 *
 * Aucune dépendance native au build : le service sous-jacent charge les plugins
 * en `import()` dynamique.
 */
@Component({
  selector: 'nx-barcode-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './barcode-button.component.html',
  styleUrl: './barcode-button.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BarcodeButtonComponent {
  private readonly scanner = inject(BarcodeScannerService);
  private readonly platform = inject(PlatformService);
  private readonly notif = inject(NotificationService);

  /** Libellé du bouton. */
  @Input() label = 'Scanner';
  /** Masquer le bouton sur le web (par défaut). Mettre à `false` pour toujours l'afficher. */
  @Input() hideOnWeb = true;
  @Input() disabled = false;

  /** Émis avec la valeur scannée (code-barres / QR). */
  @Output() scanned = new EventEmitter<string>();

  /** Vrai pendant un scan en cours (désactive le bouton, montre un état). */
  readonly scanning = signal(false);

  /** Le bouton doit-il être rendu dans le DOM ? */
  get visible(): boolean {
    return this.platform.isNative() || !this.hideOnWeb;
  }

  async onScan(): Promise<void> {
    if (this.disabled || this.scanning()) {
      return;
    }
    this.scanning.set(true);
    try {
      const value = await this.scanner.scan();
      if (value) {
        this.scanned.emit(value);
      }
    } catch (err) {
      const message =
        err instanceof BarcodeUnavailableError
          ? err.message
          : 'Échec du scan. Réessayez.';
      this.notif.error(message);
    } finally {
      this.scanning.set(false);
    }
  }
}
