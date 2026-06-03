import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';

export const authGuard: CanActivateFn = () => {
  const oauthService = inject(OAuthService);

  if (oauthService.hasValidAccessToken()) {
    return true;
  }

  // Ne pas relancer le flux si le callback OAuth2 est en cours de traitement
  // (APP_INITIALIZER s'en charge — évite la boucle infinie)
  const params = new URLSearchParams(window.location.search);
  if (params.has('code') || params.has('state')) {
    return false;
  }

  oauthService.initCodeFlow();
  return false;
};
