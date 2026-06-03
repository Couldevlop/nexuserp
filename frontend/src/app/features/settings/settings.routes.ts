import { Routes } from '@angular/router';

export const SETTINGS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./settings-home/settings-home.component').then(m => m.SettingsHomeComponent),
    title: 'Paramètres — NexusERP'
  }
];
