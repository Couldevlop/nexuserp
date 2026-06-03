import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type StatDeltaDirection = 'up' | 'down' | 'neutral';

/**
 * Carte KPI / statistique réutilisable (dashboards Sales, Inventory, …).
 * Tokens uniquement, accessible, état de chargement avec shimmer.
 */
@Component({
  selector: 'nx-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatCardComponent {
  readonly label = input.required<string>();
  readonly value = input<string | number>('');
  /** Variation en pourcentage (ex : 12.4 → "+12.4 %"). Optionnel. */
  readonly delta = input<number | null>(null);
  readonly icon = input<string>('');
  readonly loading = input<boolean>(false);

  readonly hasDelta = computed(() => this.delta() !== null);

  readonly deltaDirection = computed<StatDeltaDirection>(() => {
    const d = this.delta();
    if (d === null || d === 0) {
      return 'neutral';
    }
    return d > 0 ? 'up' : 'down';
  });

  readonly deltaLabel = computed(() => {
    const d = this.delta();
    if (d === null) {
      return '';
    }
    const sign = d > 0 ? '+' : '';
    return `${sign}${d} %`;
  });

  readonly deltaAria = computed(() => {
    switch (this.deltaDirection()) {
      case 'up':
        return `En hausse de ${this.deltaLabel()}`;
      case 'down':
        return `En baisse de ${this.deltaLabel()}`;
      default:
        return `Stable, ${this.deltaLabel()}`;
    }
  });
}
