import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

/**
 * Gestion globale des erreurs HTTP.
 * - 401 → purge de la session locale puis redirection vers login.
 *   IMPORTANT : si le token est encore "valide" localement mais rejeté par le
 *   backend (ex. issuer mismatch, session révoquée), il FAUT le purger avant de
 *   naviguer — sinon la page login voit hasValidAccessToken() et renvoie vers
 *   /dashboard, qui re-déclenche un 401 : boucle infinie (écran qui "saute").
 * - 403 → toast erreur permission
 * - 429 → toast rate limit
 * - 500 → toast erreur serveur
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const notif = inject(NotificationService);
  const oauthService = inject(OAuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          if (oauthService.hasValidAccessToken()) {
            // Token rejeté côté serveur : purge locale (sans redirect Keycloak).
            oauthService.logOut(true);
          }
          if (!router.url.startsWith('/auth/login')) {
            router.navigate(['/auth/login']);
          }
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
