import { Injectable, signal, computed, inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { SyncService } from '../offline/sync.service';

export interface UserInfo {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  tenantId: string;
  roles: string[];
  locale: string;
  country: string;
}

/**
 * Service d'état d'authentification — utilise les Signals Angular 17+.
 * Single source of truth pour l'utilisateur courant.
 */
@Injectable({ providedIn: 'root' })
export class AuthStateService {

  private readonly _user = signal<UserInfo | null>(null);

  // Signals publics (readonly)
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly tenantId = computed(() => this._user()?.tenantId ?? null);
  readonly roles = computed(() => this._user()?.roles ?? []);
  readonly fullName = computed(() => {
    const u = this._user();
    return u ? `${u.firstName} ${u.lastName}` : '';
  });

  private readonly syncService = inject(SyncService);

  constructor(private oauthService: OAuthService) {
    this.loadUserFromToken();
  }

  loadUserFromToken(): void {
    const claims = this.oauthService.getIdentityClaims() as any;
    if (!claims) {
      this._user.set(null);
      return;
    }

    const accessToken = this.oauthService.getAccessToken();
    let roles: string[] = [];
    let tenantId = '';

    if (accessToken) {
      try {
        const payload = JSON.parse(atob(accessToken.split('.')[1]));
        roles = payload?.realm_access?.roles ?? [];
        tenantId = payload?.tenantId ?? payload?.tenant_id ?? '';
      } catch {
        // Token invalide
      }
    }

    this._user.set({
      userId: claims.sub,
      email: claims.email ?? '',
      firstName: claims.given_name ?? '',
      lastName: claims.family_name ?? '',
      tenantId,
      roles,
      locale: claims.locale ?? 'fr-FR',
      country: claims.country ?? 'FR',
    });
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some(r => this.roles().includes(r));
  }

  logout(): void {
    // OWASP A04 : purge l'outbox + cache offline du tenant sur appareil partagé
    const tenantId = this.tenantId();
    if (tenantId) {
      void this.syncService.clearForTenant(tenantId);
    } else {
      void this.syncService.clearAll();
    }
    this.oauthService.logOut();
    this._user.set(null);
  }
}
