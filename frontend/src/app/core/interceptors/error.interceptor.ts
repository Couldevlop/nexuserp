import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

/**
 * Gestion globale des erreurs HTTP.
 * - 401 → redirection vers login
 * - 403 → toast erreur permission
 * - 429 → toast rate limit
 * - 500 → toast erreur serveur
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const notif = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          router.navigate(['/auth/login']);
          break;
        case 403:
          notif.error('Accès refusé — vous n\'avez pas les permissions requises');
          break;
        case 404:
          // Géré par le composant
          break;
        case 429:
          notif.warning('Trop de requêtes — veuillez patienter un moment');
          break;
        case 503:
          notif.error('Service temporairement indisponible');
          break;
        default:
          if (error.status >= 500) {
            const detail = error.error?.detail || error.message || 'Une erreur inattendue s\'est produite';
            notif.error(detail);
          }
      }
      return throwError(() => error);
    })
  );
};
