import { Routes } from '@angular/router';

export const REPORTING_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./reporting-dashboard/reporting-dashboard.component').then(
        (m) => m.ReportingDashboardComponent,
      ),
    title: 'Tableau de bord Reporting — NexusERP',
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./reports/report-list/report-list.component').then(
        (m) => m.ReportListComponent,
      ),
    title: 'Rapports générés — NexusERP',
  },
  {
    path: 'reports/new',
    loadComponent: () =>
      import('./reports/report-generate/report-generate.component').then(
        (m) => m.ReportGenerateComponent,
      ),
    title: 'Nouveau rapport — NexusERP',
  },
];
