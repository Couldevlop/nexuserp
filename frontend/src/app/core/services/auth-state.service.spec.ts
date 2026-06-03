import { TestBed } from '@angular/core/testing';
import { AuthStateService } from './auth-state.service';
import { OAuthService } from 'angular-oauth2-oidc';

const mockOAuth = {
  getIdentityClaims: jasmine.createSpy('getIdentityClaims').and.returnValue({
    sub: 'user-001',
    email: 'jean.dupont@acme.fr',
    given_name: 'Jean',
    family_name: 'Dupont',
    tenantId: 'fr-acme',
    realm_access: { roles: ['FINANCE_USER', 'TENANT_ADMIN'] },
  }),
  hasValidAccessToken: jasmine.createSpy('hasValidAccessToken').and.returnValue(true),
  getAccessToken: jasmine.createSpy('getAccessToken').and.returnValue('mock-token'),
  events: { subscribe: jasmine.createSpy('subscribe') },
};

describe('AuthStateService', () => {
  let service: AuthStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthStateService,
        { provide: OAuthService, useValue: mockOAuth },
      ]
    });
    service = TestBed.inject(AuthStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should expose user fullName from JWT claims', () => {
    expect(service.fullName()).toBe('Jean Dupont');
  });

  it('should expose tenantId from JWT claims', () => {
    expect(service.tenantId()).toBe('fr-acme');
  });

  it('should expose roles from realm_access', () => {
    expect(service.roles()).toContain('FINANCE_USER');
    expect(service.roles()).toContain('TENANT_ADMIN');
  });

  it('should return true for hasAnyRole when user has one of the roles', () => {
    expect(service.hasAnyRole('FINANCE_USER', 'HR_MANAGER')).toBeTrue();
  });

  it('should return false for hasAnyRole when user has none of the roles', () => {
    expect(service.hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')).toBeFalse();
  });

  it('should return false for hasAnyRole when roles list is empty', () => {
    expect(service.hasAnyRole()).toBeFalse();
  });

  it('should return isAuthenticated true when valid token exists', () => {
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('should expose user email', () => {
    expect(service.email()).toBe('jean.dupont@acme.fr');
  });
});
