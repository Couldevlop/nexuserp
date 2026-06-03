import { Routes } from '@angular/router';

export const INVENTORY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./inventory-dashboard/inventory-dashboard.component').then(m => m.InventoryDashboardComponent),
    title: 'Stocks — NexusERP'
  },
  {
    path: 'products',
    loadComponent: () =>
      import('./products/product-list/product-list.component').then(m => m.ProductListComponent),
    title: 'Articles — NexusERP'
  },
  {
    path: 'products/new',
    loadComponent: () =>
      import('./products/product-create/product-create.component').then(m => m.ProductCreateComponent),
    title: 'Nouvel article — NexusERP'
  },
  {
    path: 'products/:id',
    loadComponent: () =>
      import('./products/product-detail/product-detail.component').then(m => m.ProductDetailComponent),
    title: 'Article — NexusERP'
  },
  {
    path: 'movements',
    loadComponent: () =>
      import('./movements/movement-list/movement-list.component').then(m => m.MovementListComponent),
    title: 'Mouvements de stock — NexusERP'
  }
];
