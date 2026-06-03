import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        title: 'Tableau de bord — NexusERP'
      },
      {
        path: 'finance',
        loadChildren: () => import('./features/finance/finance.routes').then(m => m.FINANCE_ROUTES),
        canActivate: [roleGuard],
        data: { roles: ['FINANCE_USER', 'FINANCE_MANAGER', 'TENANT_ADMIN', 'AUDITOR'] },
        title: 'Finance — NexusERP'
      },
      {
        path: 'procurement',
        loadChildren: () => import('./features/procurement/procurement.routes').then(m => m.PROCUREMENT_ROUTES),
        canActivate: [roleGuard],
        data: { roles: ['PROCUREMENT_USER', 'PROCUREMENT_MANAGER', 'TENANT_ADMIN'] }
      },
      {
        path: 'inventory',
        loadChildren: () => import('./features/inventory/inventory.routes').then(m => m.INVENTORY_ROUTES),
        canActivate: [roleGuard],
        data: { roles: ['INVENTORY_MANAGER', 'TENANT_ADMIN'] }
      },
      {
        path: 'sales',
        loadChildren: () => import('./features/sales/sales.routes').then(m => m.SALES_ROUTES),
        canActivate: [roleGuard],
        data: { roles: ['SALES_USER', 'SALES_MANAGER', 'TENANT_ADMIN'] }
      },
      {
        path: 'hr',
        loadChildren: () => import('./features/hr/hr.routes').then(m => m.HR_ROUTES),
        canActivate: [roleGuard],
        data: { roles: ['HR_USER', 'HR_MANAGER', 'TENANT_ADMIN'] }
      },
      {
        path: 'production',
        loadChildren: () => import('./features/production/production.routes').then(m => m.PRODUCTION_ROUTES),
        canActivate: [roleGuard],
        data: { roles: ['PRODUCTION_USER', 'PRODUCTION_MANAGER', 'TENANT_ADMIN'] }
      },
      {
        path: 'reporting',
        loadChildren: () => import('./features/reporting/reporting.routes').then(m => m.REPORTING_ROUTES),
        canActivate: [roleGuard],
        data: { roles: ['AUDITOR', 'TENANT_ADMIN', 'AI_ANALYST'] }
      },
      {
        path: 'admin',
        loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES),
        canActivate: [roleGuard],
        data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN'] }
      },
      {
        path: 'settings',
        loadChildren: () => import('./features/settings/settings.routes').then(m => m.SETTINGS_ROUTES)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
