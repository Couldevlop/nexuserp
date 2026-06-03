import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';
import { NotificationService } from '../services/notification.service';
import { roleGuard } from './role.guard';
import { signal } from '@angular/core';

const mockAuthState = {
  roles: signal(['FINANCE_USER', 'TENANT_ADMIN']),
  hasAnyRole: (...roles: string[]) =>
    roles.some(r => ['FINANCE_USER', 'TENANT_ADMIN'].includes(r)),
};

describe('roleGuard', () => {
  let router: jasmine.SpyObj<Router>;
  let notif: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const notifSpy = jasmine.createSpyObj('NotificationService', ['error']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthStateService, useValue: mockAuthState },
        { provide: Router, useValue: routerSpy },
        { provide: NotificationService, useValue: notifSpy },
      ]
    });

    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    notif = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
  });

  it('should allow access when no roles required', () => {
    const route = { data: { roles: [] } } as any;
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
  });

  it('should allow access when user has required role', () => {
    const route = { data: { roles: ['FINANCE_USER'] } } as any;
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
  });

  it('should deny access and redirect when user lacks required role', () => {
    const route = { data: { roles: ['SUPER_ADMIN'] } } as any;
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, {} as RouterStateSnapshot)
    );
    expect(result).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(notif.error).toHaveBeenCalled();
  });

  it('should allow access when user has at least one of multiple required roles', () => {
    const route = { data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN'] } } as any;
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
  });

  it('should handle undefined roles data gracefully', () => {
    const route = { data: {} } as any;
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
  });
});
