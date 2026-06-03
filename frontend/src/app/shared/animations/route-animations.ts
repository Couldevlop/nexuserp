import {
  trigger,
  transition,
  style,
  query,
  animate,
  group,
  AnimationTriggerMetadata,
} from '@angular/animations';

/**
 * Détecte la préférence utilisateur "reduced motion".
 * Si activée, on neutralise la durée des animations de route.
 */
function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

const ENTER_DURATION = '250ms cubic-bezier(0.4, 0, 0.2, 1)';
const LEAVE_DURATION = '150ms cubic-bezier(0.4, 0, 0.2, 1)';

/**
 * Animation de transition entre routes : fade + léger translate up.
 * N'anime que opacity / transform (60fps, jamais de propriété de layout).
 *
 * Câblage (sans modifier le shell ici — instructions de mise en place) :
 *
 *   // shell.component.ts
 *   import { routeFadeAnimation } from '../../shared/animations/route-animations';
 *   // dans le décorateur :
 *   animations: [routeFadeAnimation],
 *
 *   // shell.component.html — wrapper autour de l'outlet :
 *   <main [@routeFade]="getRouteState(outlet)">
 *     <router-outlet #outlet="outlet"></router-outlet>
 *   </main>
 *
 *   // shell.component.ts — helper :
 *   getRouteState(outlet: RouterOutlet): string {
 *     return outlet?.activatedRouteData?.['animation'] ?? outlet?.activatedRoute?.snapshot?.url?.join('/') ?? '';
 *   }
 *
 * `provideAnimations()` est déjà fourni dans app.config.ts — aucune modif requise.
 */
export const routeFadeAnimation: AnimationTriggerMetadata = trigger('routeFade', [
  transition('* <=> *', [
    query(
      ':enter',
      [style({ opacity: 0, transform: 'translate3d(0, 12px, 0)' })],
      { optional: true }
    ),
    group([
      query(
        ':leave',
        [
          animate(
            prefersReducedMotion() ? '0ms' : LEAVE_DURATION,
            style({ opacity: 0 })
          ),
        ],
        { optional: true }
      ),
      query(
        ':enter',
        [
          animate(
            prefersReducedMotion() ? '0ms' : ENTER_DURATION,
            style({ opacity: 1, transform: 'translate3d(0, 0, 0)' })
          ),
        ],
        { optional: true }
      ),
    ]),
  ]),
]);
