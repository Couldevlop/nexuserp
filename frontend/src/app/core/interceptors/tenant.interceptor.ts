import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStateService } from '../services/auth-state.service';

/**
 * Injecte le X-Tenant-ID header pour le routing multi-tenant.
 * Le tenantId est extrait du JWT décodé.
 */
export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  const authState = inject(AuthStateService);
  const tenantId = authState.tenantId();

  if (!tenantId || !req.url.includes('/api/')) {
    return next(req);
  }

  const tenantReq = req.clone({
    setHeaders: { 'X-Tenant-ID': tenantId }
  });
  return next(tenantReq);
};
