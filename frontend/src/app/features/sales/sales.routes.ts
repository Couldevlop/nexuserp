import { Routes } from '@angular/router';

export const SALES_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'orders',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./sales-dashboard/sales-dashboard.component').then(m => m.SalesDashboardComponent),
    title: 'Tableau de bord Ventes — NexusERP'
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./orders/order-list/order-list.component').then(m => m.OrderListComponent),
    title: 'Commandes — NexusERP'
  },
  {
    path: 'orders/new',
    loadComponent: () =>
      import('./orders/order-create/order-create.component').then(m => m.OrderCreateComponent),
    title: 'Nouvelle commande — NexusERP'
  },
  {
    path: 'orders/:id',
    loadComponent: () =>
      import('./orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent),
    title: 'Commande — NexusERP'
  },
  {
    path: 'customers',
    loadComponent: () =>
      import('./customers/customer-list/customer-list.component').then(m => m.CustomerListComponent),
    title: 'Clients — NexusERP'
  }
];
