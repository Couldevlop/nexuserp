import { Routes } from '@angular/router';

export const HR_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./hr-dashboard/hr-dashboard.component').then(m => m.HrDashboardComponent),
    title: 'Tableau de bord RH — NexusERP'
  },
  {
    path: 'employees',
    loadComponent: () =>
      import('./employees/employee-list/employee-list.component').then(m => m.EmployeeListComponent),
    title: 'Salariés — NexusERP'
  },
  {
    path: 'employees/new',
    loadComponent: () =>
      import('./employees/employee-create/employee-create.component').then(m => m.EmployeeCreateComponent),
    title: 'Nouveau salarié — NexusERP'
  },
  {
    path: 'employees/:id',
    loadComponent: () =>
      import('./employees/employee-detail/employee-detail.component').then(m => m.EmployeeDetailComponent),
    title: 'Salarié — NexusERP'
  },
  {
    path: 'leaves',
    loadComponent: () =>
      import('./leaves/leave-list/leave-list.component').then(m => m.LeaveListComponent),
    title: 'Congés & absences — NexusERP'
  },
  {
    path: 'leaves/new',
    loadComponent: () =>
      import('./leaves/leave-request/leave-request.component').then(m => m.LeaveRequestComponent),
    title: 'Demande de congé — NexusERP'
  },
  {
    path: 'payroll',
    loadComponent: () =>
      import('./payroll/payslip-list/payslip-list.component').then(m => m.PayslipListComponent),
    title: 'Bulletins de paie — NexusERP'
  }
];
