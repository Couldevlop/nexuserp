import { Component, signal, OnInit, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AuthStateService } from '../../core/services/auth-state.service';
import { NotificationService } from '../../core/services/notification.service';
import { ToastComponent } from '../../shared/components/toast/toast.component';
import { AiAssistantComponent } from '../../shared/components/ai-assistant/ai-assistant.component';
import { OfflineIndicatorComponent } from '../../shared/components/offline-indicator/offline-indicator.component';
import { CommandPaletteComponent } from '../../shared/components/command-palette/command-palette.component';
import { CommandPaletteService } from '../../shared/components/command-palette/command-palette.service';
import { routeFadeAnimation } from '../../shared/animations/route-animations';

@Component({
  selector: 'nx-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    CommonModule, TranslateModule,
    ToastComponent, AiAssistantComponent,
    OfflineIndicatorComponent, CommandPaletteComponent
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [routeFadeAnimation]
})
export class ShellComponent implements OnInit {

  private readonly commandPalette = inject(CommandPaletteService);

  readonly sidebarCollapsed = signal(false);
  readonly authState = this.authStateService;
  readonly notifications = this.notificationService.toasts;

  readonly navItems = [
    {
      section: 'Principal',
      items: [
        { label: 'Tableau de bord', path: '/dashboard', icon: 'grid', roles: [] },
      ]
    },
    {
      section: 'Finance',
      items: [
        { label: 'Comptabilité',   path: '/finance/journal',  icon: 'book',    roles: ['FINANCE_USER', 'FINANCE_MANAGER'] },
        { label: 'Factures',       path: '/finance/invoices', icon: 'receipt', roles: ['FINANCE_USER', 'FINANCE_MANAGER'] },
        { label: 'Trésorerie',     path: '/finance/treasury', icon: 'bank',    roles: ['FINANCE_MANAGER'] },
        { label: 'Budgets',        path: '/finance/budgets',  icon: 'chart',   roles: ['FINANCE_MANAGER'] },
      ]
    },
    {
      section: 'Opérations',
      items: [
        { label: 'Ventes',         path: '/sales',        icon: 'trending-up', roles: ['SALES_USER', 'SALES_MANAGER'] },
        { label: 'Achats',         path: '/procurement',  icon: 'shopping-cart', roles: ['PROCUREMENT_USER'] },
        { label: 'Stocks',         path: '/inventory',    icon: 'package',     roles: ['INVENTORY_MANAGER'] },
        { label: 'Production',     path: '/production',   icon: 'cog',         roles: ['PRODUCTION_USER'] },
      ]
    },
    {
      section: 'RH',
      items: [
        { label: 'Employés',       path: '/hr/employees', icon: 'users',       roles: ['HR_USER', 'HR_MANAGER'] },
        { label: 'Paie',           path: '/hr/payroll',   icon: 'dollar-sign', roles: ['HR_MANAGER'] },
        { label: 'Congés',         path: '/hr/leaves',    icon: 'calendar',    roles: ['HR_USER', 'HR_MANAGER'] },
      ]
    },
    {
      section: 'Intelligence',
      items: [
        { label: 'Rapports',       path: '/reporting',    icon: 'bar-chart',   roles: ['AUDITOR', 'AI_ANALYST'] },
      ]
    },
    {
      section: 'Administration',
      items: [
        { label: 'Paramètres',     path: '/settings',     icon: 'settings',    roles: [] },
        { label: 'Administration', path: '/admin',        icon: 'shield',      roles: ['SUPER_ADMIN', 'TENANT_ADMIN'] },
      ]
    }
  ];

  constructor(
    private authStateService: AuthStateService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {}

  toggleSidebar(): void {
    this.sidebarCollapsed.update(c => !c);
  }

  openCommandPalette(): void {
    this.commandPalette.open();
  }

  getRouteState(outlet: RouterOutlet): string {
    return outlet?.isActivated ? (outlet.activatedRoute?.snapshot?.url?.join('/') ?? '') : '';
  }

  canShowSection(item: { roles: string[] }): boolean {
    if (item.roles.length === 0) return true;
    return this.authStateService.hasAnyRole(...item.roles);
  }

  hasSectionVisible(section: { items: { roles: string[] }[] }): boolean {
    return section.items.some(item => this.canShowSection(item));
  }

  logout(): void {
    this.authStateService.logout();
  }
}
