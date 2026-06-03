import { Component, OnInit, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  StockMovement, SpringPage, MovementType,
  MOVEMENT_LABELS, MOVEMENT_BADGE, INVENTORY_API
} from '../../inventory.types';

@Component({
  selector: 'nx-movement-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './movement-list.component.html',
  styleUrl: './movement-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MovementListComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(true);
  /** true si le backend n'expose pas (encore) l'historique des mouvements. */
  readonly notAvailable = signal(false);
  readonly movements = signal<StockMovement[]>([]);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly typeFilter = signal<MovementType | ''>('');
  readonly dateFrom = signal('');
  readonly dateTo = signal('');

  readonly movementLabel = MOVEMENT_LABELS;
  readonly movementBadge = MOVEMENT_BADGE;

  readonly typeOptions: { value: MovementType | ''; label: string }[] = [
    { value: '', label: 'Tous les types' },
    { value: 'IN', label: MOVEMENT_LABELS.IN },
    { value: 'OUT', label: MOVEMENT_LABELS.OUT },
    { value: 'TRANSFER', label: MOVEMENT_LABELS.TRANSFER },
    { value: 'ADJUSTMENT', label: MOVEMENT_LABELS.ADJUSTMENT },
  ];

  ngOnInit(): void {
    this.loadMovements();
  }

  loadMovements(): void {
    this.isLoading.set(true);
    this.notAvailable.set(false);

    let params = new HttpParams()
      .set('page', this.currentPage())
      .set('size', this.pageSize())
      .set('sort', 'createdAt')
      .set('dir', 'desc');

    if (this.typeFilter()) params = params.set('type', this.typeFilter());
    if (this.dateFrom()) params = params.set('dateFrom', this.dateFrom());
    if (this.dateTo()) params = params.set('dateTo', this.dateTo());

    this.http.get<SpringPage<StockMovement>>(`${INVENTORY_API}/movements`, { params }).subscribe({
      next: (page) => {
        this.movements.set(page.content ?? []);
        this.totalPages.set(page.totalPages ?? 0);
        this.totalItems.set(page.totalElements ?? 0);
        this.isLoading.set(false);
      },
      error: (err) => {
        // 404 = endpoint non exposé → dégradation gracieuse (pas de toast d'erreur).
        if (err?.status === 404) {
          this.notAvailable.set(true);
        } else {
          this.notif.error('Erreur lors du chargement des mouvements');
        }
        this.movements.set([]);
        this.isLoading.set(false);
      }
    });
  }

  onTypeChange(type: MovementType | ''): void {
    this.typeFilter.set(type);
    this.currentPage.set(0);
    this.loadMovements();
  }

  onDateFrom(value: string): void {
    this.dateFrom.set(value);
    this.currentPage.set(0);
    this.loadMovements();
  }

  onDateTo(value: string): void {
    this.dateTo.set(value);
    this.currentPage.set(0);
    this.loadMovements();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadMovements();
  }

  getPages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  trackById(_: number, m: StockMovement): string {
    return m.id;
  }
}
