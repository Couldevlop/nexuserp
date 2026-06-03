import { Routes } from '@angular/router';

export const FINANCE_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'invoices',
    pathMatch: 'full'
  },
  {
    path: 'invoices',
    loadComponent: () => import('./invoices/invoice-list/invoice-list.component').then(m => m.InvoiceListComponent),
    title: 'Factures — NexusERP'
  },
  {
    path: 'invoices/new',
    loadComponent: () => import('./invoices/invoice-create/invoice-create.component').then(m => m.InvoiceCreateComponent),
    title: 'Nouvelle facture — NexusERP'
  },
  {
    path: 'invoices/:id',
    loadComponent: () => import('./invoices/invoice-detail/invoice-detail.component').then(m => m.InvoiceDetailComponent),
    title: 'Facture — NexusERP'
  },
  {
    path: 'journal',
    loadComponent: () => import('./journal/journal-list/journal-list.component').then(m => m.JournalListComponent),
    title: 'Journal comptable — NexusERP'
  }
];
