import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { authGuard } from './auth.guard';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

describe('authGuard', () => {
  let oauthService: jasmine.SpyObj<OAuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const oauthSpy = jasmine.createSpyObj('OAuthService', [
      'hasValidAccessToken', 'initCodeFlow', 'loadDiscoveryDocumentAndLogin'
    ]);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: OAuthService, useValue: oauthSpy },
        { provide: Router, useValue: routerSpy },
      ]
    });

    oauthService = TestBed.inject(OAuthService) as jasmine.SpyObj<OAuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should allow navigation when user has valid token', () => {
    oauthService.hasValidAccessToken.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    expect(result).toBeTrue();
  });

  it('should initiate login when user has no valid token', () => {
    oauthService.hasValidAccessToken.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    expect(result).toBeFalse();
    expect(oauthService.initCodeFlow).toHaveBeenCalled();
  });
});
