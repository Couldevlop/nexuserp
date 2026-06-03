import { Injectable, computed, signal } from '@angular/core';

/**
 * Élément de commande affichable dans la palette.
 * - `route` : navigation Angular Router (mutuellement exclusif avec `action`).
 * - `action` : callback exécuté à la sélection.
 */
export interface CommandItem {
  readonly id: string;
  readonly label: string;
  readonly group: string;
  readonly icon: string;
  readonly route?: string;
  readonly action?: () => void;
  readonly keywords?: readonly string[];
}

/**
 * Service singleton pilotant l'état d'ouverture de la palette de commandes
 * et permettant d'enregistrer des commandes supplémentaires depuis n'importe
 * quel composant (ex : actions contextuelles d'un module).
 *
 * Tout est piloté par signaux pour rester compatible OnPush.
 */
@Injectable({ providedIn: 'root' })
export class CommandPaletteService {
  private readonly _isOpen = signal(false);
  readonly isOpen = this._isOpen.asReadonly();

  private readonly _extraCommands = signal<readonly CommandItem[]>([]);
  readonly extraCommands = this._extraCommands.asReadonly();

  /** Commandes de navigation par défaut (libellés FR, i18n-ready). */
  readonly defaultCommands = computed<readonly CommandItem[]>(() => DEFAULT_COMMANDS);

  open(): void {
    this._isOpen.set(true);
  }

  close(): void {
    this._isOpen.set(false);
  }

  toggle(): void {
    this._isOpen.update((v) => !v);
  }

  /**
   * Enregistre des commandes additionnelles (dédupliquées par id).
   * Utilisable par un module pour exposer ses actions dans la palette.
   */
  registerCommands(items: readonly CommandItem[]): void {
    this._extraCommands.update((existing) => {
      const ids = new Set(existing.map((c) => c.id));
      const merged = [...existing];
      for (const item of items) {
        if (!ids.has(item.id)) {
          merged.push(item);
          ids.add(item.id);
        }
      }
      return merged;
    });
  }

  /** Retire des commandes précédemment enregistrées (par id). */
  unregisterCommands(ids: readonly string[]): void {
    const toRemove = new Set(ids);
    this._extraCommands.update((existing) =>
      existing.filter((c) => !toRemove.has(c.id))
    );
  }
}

/**
 * Catalogue statique des commandes de navigation, dérivé des routes de l'app.
 * Facilement extensible : ajouter une entrée typée ci-dessous.
 */
export const DEFAULT_COMMANDS: readonly CommandItem[] = [
  {
    id: 'nav-dashboard',
    label: 'Tableau de bord',
    group: 'Principal',
    icon: 'grid',
    route: '/dashboard',
    keywords: ['accueil', 'home', 'dashboard'],
  },
  {
    id: 'nav-finance-invoices',
    label: 'Finance · Factures',
    group: 'Finance',
    icon: 'receipt',
    route: '/finance/invoices',
    keywords: ['facture', 'invoice', 'compta'],
  },
  {
    id: 'nav-sales-orders',
    label: 'Ventes · Commandes',
    group: 'Opérations',
    icon: 'trending-up',
    route: '/sales',
    keywords: ['commande', 'order', 'vente', 'crm'],
  },
  {
    id: 'nav-inventory-products',
    label: 'Stocks · Produits',
    group: 'Opérations',
    icon: 'package',
    route: '/inventory',
    keywords: ['produit', 'article', 'stock', 'entrepôt'],
  },
  {
    id: 'nav-procurement',
    label: 'Achats',
    group: 'Opérations',
    icon: 'shopping-cart',
    route: '/procurement',
    keywords: ['achat', 'fournisseur', 'procurement'],
  },
  {
    id: 'nav-production',
    label: 'Production',
    group: 'Opérations',
    icon: 'cog',
    route: '/production',
    keywords: ['fabrication', 'mrp', 'production'],
  },
  {
    id: 'nav-hr',
    label: 'Ressources humaines',
    group: 'RH',
    icon: 'users',
    route: '/hr/employees',
    keywords: ['rh', 'employé', 'paie', 'salarié', 'hr'],
  },
  {
    id: 'nav-reporting',
    label: 'Rapports',
    group: 'Intelligence',
    icon: 'bar-chart',
    route: '/reporting',
    keywords: ['rapport', 'reporting', 'bi', 'analyse'],
  },
  {
    id: 'nav-settings',
    label: 'Paramètres',
    group: 'Administration',
    icon: 'settings',
    route: '/settings',
    keywords: ['paramètre', 'réglage', 'config', 'settings'],
  },
  {
    id: 'nav-admin',
    label: 'Administration',
    group: 'Administration',
    icon: 'shield',
    route: '/admin',
    keywords: ['admin', 'tenant', 'utilisateur'],
  },
];
