import { APP_INITIALIZER, ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { HttpClient } from '@angular/common/http';
import { AuthConfig, OAuthService, provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { tenantInterceptor } from './core/interceptors/tenant.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { offlineInterceptor } from './core/offline/offline.interceptor';
import { SyncService } from './core/offline/sync.service';

export function httpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

export const authConfig: AuthConfig = {
  issuer: 'http://localhost:8180/realms/nexuserp',
  redirectUri: window.location.origin,
  clientId: 'nexuserp-frontend',
  responseType: 'code',
  scope: 'openid profile email',
  useSilentRefresh: false,
  sessionChecksEnabled: false,
  showDebugInformation: false,
  clearHashAfterLogin: false,
  requireHttps: false,
};

function initializeOAuth(oauthService: OAuthService): () => Promise<boolean> {
  return () => {
    oauthService.configure(authConfig);
    return oauthService.loadDiscoveryDocumentAndTryLogin();
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding(), withViewTransitions()),
    provideHttpClient(withInterceptors([
      authInterceptor,
      tenantInterceptor,
      offlineInterceptor,
      errorInterceptor,
    ])),
    provideAnimations(),
    provideOAuthClient(),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeOAuth,
      deps: [OAuthService],
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: (sync: SyncService) => () => sync.init(),
      deps: [SyncService],
      multi: true,
    },
    importProvidersFrom(
      TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useFactory: httpLoaderFactory,
          deps: [HttpClient],
        },
        defaultLanguage: 'fr-FR',
      })
    ),
  ],
};
