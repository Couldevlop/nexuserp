import { Routes } from '@angular/router';

export const PROCUREMENT_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./procurement-dashboard/procurement-dashboard.component').then(m => m.ProcurementDashboardComponent),
    title: 'Tableau de bord Achats — NexusERP'
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./orders/po-list/po-list.component').then(m => m.PoListComponent),
    title: "Commandes d'achat — NexusERP"
  },
  {
    path: 'orders/new',
    loadComponent: () =>
      import('./orders/po-create/po-create.component').then(m => m.PoCreateComponent),
    title: "Nouvelle commande d'achat — NexusERP"
  },
  {
    path: 'orders/:id',
    loadComponent: () =>
      import('./orders/po-detail/po-detail.component').then(m => m.PoDetailComponent),
    title: "Commande d'achat — NexusERP"
  },
  {
    path: 'suppliers',
    loadComponent: () =>
      import('./suppliers/supplier-list/supplier-list.component').then(m => m.SupplierListComponent),
    title: 'Fournisseurs — NexusERP'
  }
];
