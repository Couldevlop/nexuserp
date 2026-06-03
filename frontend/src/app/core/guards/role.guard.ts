import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';
import { NotificationService } from '../services/notification.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);
  const notif = inject(NotificationService);

  const requiredRoles: string[] = route.data?.['roles'] ?? [];

  if (requiredRoles.length === 0) {
    return true;
  }

  const hasRole = authState.hasAnyRole(...requiredRoles);
  if (!hasRole) {
    notif.error('Accès refusé — permissions insuffisantes pour ce module');
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};
