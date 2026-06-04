import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./admin-home/admin-home.component').then(m => m.AdminHomeComponent),
    title: 'Administration — NexusERP'
  },
  {
    path: 'parametrage',
    loadComponent: () =>
      import('./parametrage/config-list/config-list.component').then(m => m.ConfigListComponent),
    canActivate: [roleGuard],
    data: { roles: ['TENANT_ADMIN', 'SUPER_ADMIN'] },
    title: 'Paramétrage — NexusERP'
  }
];
