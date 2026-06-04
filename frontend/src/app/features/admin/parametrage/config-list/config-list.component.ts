import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  signal,
  computed,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../../../../core/services/notification.service';
import { BadgeComponent } from '../../../../shared/components/badge/badge.component';
import { ConfigEditComponent } from '../config-edit/config-edit.component';
import { ConfigService } from '../config.service';
import {
  ConfigParam,
  ConfigCategory,
  CONFIG_CATEGORIES,
  MASKED_PLACEHOLDER,
  valueTypeLabel
} from '../config.model';
import { CONFIG_CATALOG, CatalogEntry } from '../config-catalog';

/** Ligne affichée : fusion d'une entrée catalogue + du paramètre backend. */
export interface ConfigRow {
  key: string;
  label: string;
  category: ConfigCategory;
  param: ConfigParam;
  group: string;
  /** true si l'entrée vient uniquement du catalogue (pas encore en base). */
  catalogOnly: boolean;
}

@Component({
  selector: 'nx-config-list',
  standalone: true,
  imports: [CommonModule, FormsModule, BadgeComponent, ConfigEditComponent],
  templateUrl: './config-list.component.html',
  styleUrl: './config-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfigListComponent implements OnInit {
  private readonly configService = inject(ConfigService);
  private readonly notif = inject(NotificationService);

  readonly categories = CONFIG_CATEGORIES;
  readonly maskedPlaceholder = MASKED_PLACEHOLDER;

  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  /** Paramètres renvoyés par le backend, indexés par clé. */
  private readonly backendParams = signal<Map<string, ConfigParam>>(new Map());

  readonly activeCategory = signal<ConfigCategory>('PAYMENT');
  readonly searchQuery = signal('');

  // Édition
  readonly editOpen = signal(false);
  readonly editParam = signal<ConfigParam | null>(null);
  // Suppression
  readonly deletingKey = signal<string | null>(null);

  /** Toutes les lignes de la catégorie active (catalogue ∪ backend). */
  readonly rows = computed<ConfigRow[]>(() => {
    const cat = this.activeCategory();
    const backend = this.backendParams();
    const seen = new Set<string>();
    const rows: ConfigRow[] = [];

    // 1. Entrées du catalogue pour la catégorie (libellés conviviaux).
    for (const entry of CONFIG_CATALOG) {
      if (entry.category !== cat) continue;
      seen.add(entry.key);
      const fromBackend = backend.get(entry.key);
      rows.push(this.toRow(entry, fromBackend));
    }

    // 2. Paramètres backend hors catalogue (créés ailleurs).
    for (const param of backend.values()) {
      if (param.category !== cat || seen.has(param.key)) continue;
      rows.push({
        key: param.key,
        label: param.key,
        category: param.category,
        param,
        group: 'Autres',
        catalogOnly: false
      });
    }

    return rows;
  });

  /** Lignes filtrées par recherche locale. */
  readonly filteredRows = computed<ConfigRow[]>(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const rows = this.rows();
    if (!q) return rows;
    return rows.filter(
      (r) => r.label.toLowerCase().includes(q) || r.key.toLowerCase().includes(q)
    );
  });

  /** Lignes groupées (par `group`) pour l'affichage en sections. */
  readonly groupedRows = computed<{ group: string; rows: ConfigRow[] }[]>(() => {
    const groups = new Map<string, ConfigRow[]>();
    for (const row of this.filteredRows()) {
      const list = groups.get(row.group) ?? [];
      list.push(row);
      groups.set(row.group, list);
    }
    return Array.from(groups.entries()).map(([group, rows]) => ({ group, rows }));
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.configService.list().subscribe({
      next: (params) => {
        const map = new Map<string, ConfigParam>();
        for (const p of params) {
          map.set(p.key, p);
        }
        this.backendParams.set(map);
        this.isLoading.set(false);
      },
      error: () => {
        // Dégradation gracieuse : le catalogue reste affiché (non défini),
        // pas d'avalanche de toasts.
        this.backendParams.set(new Map());
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  selectCategory(category: ConfigCategory): void {
    this.activeCategory.set(category);
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
  }

  typeLabel(type: ConfigParam['valueType']): string {
    return valueTypeLabel(type);
  }

  /** Valeur affichée — un secret n'est JAMAIS rendu en clair. */
  displayValue(param: ConfigParam): string {
    if (param.secret) {
      return param.set ? MASKED_PLACEHOLDER : '—';
    }
    if (!param.set || param.value === null || param.value === '') {
      return '—';
    }
    if (param.valueType === 'BOOLEAN') {
      return param.value === 'true' || param.value === '1' ? 'Activé' : 'Désactivé';
    }
    return param.value;
  }

  openEdit(row: ConfigRow): void {
    // On passe une copie défensive ; pour un secret la valeur est déjà masquée.
    this.editParam.set({ ...row.param });
    this.editOpen.set(true);
  }

  closeEdit(): void {
    this.editOpen.set(false);
    this.editParam.set(null);
  }

  onSaved(updated: ConfigParam): void {
    this.backendParams.update((map) => {
      const next = new Map(map);
      next.set(updated.key, updated);
      return next;
    });
    this.closeEdit();
  }

  deleteParam(row: ConfigRow): void {
    if (!row.param.set) {
      return;
    }
    this.deletingKey.set(row.key);
    this.configService.delete(row.key).subscribe({
      next: () => {
        this.deletingKey.set(null);
        this.backendParams.update((map) => {
          const next = new Map(map);
          next.delete(row.key);
          return next;
        });
        this.notif.success('Paramètre supprimé');
      },
      error: () => {
        this.deletingKey.set(null);
        this.notif.error('Échec de la suppression');
      }
    });
  }

  /** Construit une ligne en fusionnant catalogue + backend. */
  private toRow(entry: CatalogEntry, fromBackend?: ConfigParam): ConfigRow {
    const param: ConfigParam = fromBackend ?? {
      key: entry.key,
      category: entry.category,
      valueType: entry.valueType,
      secret: entry.secret,
      set: false,
      value: null,
      description: entry.description ?? null,
      updatedAt: null
    };
    return {
      key: entry.key,
      label: entry.label,
      category: entry.category,
      param,
      group: entry.group ?? 'Général',
      catalogOnly: !fromBackend
    };
  }
}
