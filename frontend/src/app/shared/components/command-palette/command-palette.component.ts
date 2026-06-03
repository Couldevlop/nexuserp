import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { fromEvent } from 'rxjs';
import { filter } from 'rxjs/operators';
import {
  CommandItem,
  CommandPaletteService,
} from './command-palette.service';

interface CommandGroup {
  readonly group: string;
  readonly items: readonly CommandItem[];
}

/**
 * Palette de commandes globale (style Spotlight / Cmd+K).
 *
 * Montage (sans modifier le shell ici) :
 *   // shell.component.ts → imports: [..., CommandPaletteComponent]
 *   // shell.component.html → <nx-command-palette /> (n'importe où dans le shell)
 * Une fois montée, Cmd/Ctrl+K l'ouvre globalement (écouteur sur document).
 */
@Component({
  selector: 'nx-command-palette',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './command-palette.component.html',
  styleUrl: './command-palette.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommandPaletteComponent {
  private readonly palette = inject(CommandPaletteService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly inputRef = viewChild<ElementRef<HTMLInputElement>>('searchInput');
  private readonly listRef = viewChild<ElementRef<HTMLElement>>('resultsList');

  readonly listboxId = 'nx-cmd-listbox';
  readonly isOpen = this.palette.isOpen;
  readonly query = signal('');
  readonly highlighted = signal(0);

  /** Élément déclencheur à re-focuser à la fermeture. */
  private triggerElement: HTMLElement | null = null;

  /** Toutes les commandes (par défaut + enregistrées dynamiquement). */
  private readonly allCommands = computed<readonly CommandItem[]>(() => [
    ...this.palette.defaultCommands(),
    ...this.palette.extraCommands(),
  ]);

  /** Résultats filtrés à plat (ordre = ordre d'affichage). */
  readonly filtered = computed<readonly CommandItem[]>(() => {
    const q = this.query().trim().toLowerCase();
    const all = this.allCommands();
    if (!q) {
      return all;
    }
    return all
      .map((cmd) => ({ cmd, score: this.fuzzyScore(q, cmd) }))
      .filter((r) => r.score > 0)
      .sort((a, b) => b.score - a.score)
      .map((r) => r.cmd);
  });

  /** Résultats groupés pour l'affichage par sections. */
  readonly grouped = computed<readonly CommandGroup[]>(() => {
    const groups = new Map<string, CommandItem[]>();
    for (const cmd of this.filtered()) {
      const list = groups.get(cmd.group) ?? [];
      list.push(cmd);
      groups.set(cmd.group, list);
    }
    return Array.from(groups.entries()).map(([group, items]) => ({ group, items }));
  });

  readonly resultCount = computed(() => this.filtered().length);

  constructor() {
    // Écouteur clavier global : Cmd/Ctrl+K ouvre / bascule la palette.
    fromEvent<KeyboardEvent>(document, 'keydown')
      .pipe(
        filter((e) => this.isToggleShortcut(e)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((e) => {
        e.preventDefault();
        this.openFromTrigger(document.activeElement as HTMLElement | null);
      });

    // Réinitialise l'état + focus à l'ouverture, restaure le focus à la fermeture.
    effect(() => {
      if (this.isOpen()) {
        this.query.set('');
        this.highlighted.set(0);
        queueMicrotask(() => this.inputRef()?.nativeElement.focus());
      } else if (this.triggerElement) {
        const el = this.triggerElement;
        this.triggerElement = null;
        queueMicrotask(() => el.focus());
      }
    });
  }

  private isToggleShortcut(e: KeyboardEvent): boolean {
    return (e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k';
  }

  private openFromTrigger(trigger: HTMLElement | null): void {
    // Ne pas mémoriser un déclencheur situé dans la palette elle-même.
    if (trigger && !trigger.closest('.nx-cmd')) {
      this.triggerElement = trigger;
    }
    this.palette.open();
  }

  close(): void {
    this.palette.close();
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('nx-cmd__overlay')) {
      this.close();
    }
  }

  onSearchInput(value: string): void {
    this.query.set(value);
    this.highlighted.set(0);
  }

  /** Gestion clavier dans la palette (input). */
  onKeydown(event: KeyboardEvent): void {
    const count = this.resultCount();
    switch (event.key) {
      case 'Escape':
        event.preventDefault();
        this.close();
        break;
      case 'ArrowDown':
        event.preventDefault();
        if (count > 0) {
          this.highlighted.update((i) => (i + 1) % count);
          this.scrollToActive();
        }
        break;
      case 'ArrowUp':
        event.preventDefault();
        if (count > 0) {
          this.highlighted.update((i) => (i - 1 + count) % count);
          this.scrollToActive();
        }
        break;
      case 'Home':
        event.preventDefault();
        this.highlighted.set(0);
        this.scrollToActive();
        break;
      case 'End':
        event.preventDefault();
        if (count > 0) {
          this.highlighted.set(count - 1);
          this.scrollToActive();
        }
        break;
      case 'Enter':
        event.preventDefault();
        this.executeHighlighted();
        break;
      default:
        break;
    }
  }

  /** Index global d'une commande (pour highlight & aria-activedescendant). */
  globalIndex(item: CommandItem): number {
    return this.filtered().findIndex((c) => c.id === item.id);
  }

  optionId(item: CommandItem): string {
    return `nx-cmd-opt-${item.id}`;
  }

  activeDescendantId(): string | null {
    const active = this.filtered()[this.highlighted()];
    return active ? this.optionId(active) : null;
  }

  isHighlighted(item: CommandItem): boolean {
    return this.globalIndex(item) === this.highlighted();
  }

  onHover(item: CommandItem): void {
    this.highlighted.set(this.globalIndex(item));
  }

  execute(item: CommandItem): void {
    this.close();
    if (item.route) {
      void this.router.navigate([item.route]);
    } else if (item.action) {
      item.action();
    }
  }

  private executeHighlighted(): void {
    const item = this.filtered()[this.highlighted()];
    if (item) {
      this.execute(item);
    }
  }

  private scrollToActive(): void {
    const id = this.activeDescendantId();
    if (!id) {
      return;
    }
    queueMicrotask(() => {
      const el = this.listRef()?.nativeElement.querySelector(`#${CSS.escape(id)}`);
      el?.scrollIntoView({ block: 'nearest' });
    });
  }

  /**
   * Score fuzzy léger : correspondance de sous-séquence sur label + keywords.
   * Bonus pour préfixe et contiguïté. Retourne 0 si aucune correspondance.
   */
  private fuzzyScore(query: string, cmd: CommandItem): number {
    const haystacks = [cmd.label.toLowerCase(), ...(cmd.keywords ?? []).map((k) => k.toLowerCase())];
    let best = 0;
    for (const hay of haystacks) {
      best = Math.max(best, this.scoreString(query, hay));
    }
    return best;
  }

  private scoreString(query: string, text: string): number {
    if (text.startsWith(query)) {
      return 1000 - text.length;
    }
    if (text.includes(query)) {
      return 500 - text.length;
    }
    // Sous-séquence (caractères dans l'ordre).
    let qi = 0;
    let contiguous = 0;
    let score = 0;
    for (let i = 0; i < text.length && qi < query.length; i++) {
      if (text[i] === query[qi]) {
        qi++;
        contiguous++;
        score += contiguous;
      } else {
        contiguous = 0;
      }
    }
    return qi === query.length ? score : 0;
  }
}
