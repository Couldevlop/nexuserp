import { Routes } from '@angular/router';

export const PRODUCTION_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./production-dashboard/production-dashboard.component').then(
        (m) => m.ProductionDashboardComponent
      ),
    title: 'Tableau de bord Production — NexusERP',
  },
  {
    path: 'work-orders',
    loadComponent: () =>
      import('./work-orders/work-order-list/work-order-list.component').then(
        (m) => m.WorkOrderListComponent
      ),
    title: 'Ordres de fabrication — NexusERP',
  },
  {
    path: 'work-orders/new',
    loadComponent: () =>
      import('./work-orders/work-order-create/work-order-create.component').then(
        (m) => m.WorkOrderCreateComponent
      ),
    title: 'Nouvel ordre de fabrication — NexusERP',
  },
  {
    path: 'work-orders/:id',
    loadComponent: () =>
      import('./work-orders/work-order-detail/work-order-detail.component').then(
        (m) => m.WorkOrderDetailComponent
      ),
    title: 'Ordre de fabrication — NexusERP',
  },
];
