import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { OfflineIndicatorComponent } from './offline-indicator.component';
import { ConnectivityService } from '../../../core/offline/connectivity.service';
import { OutboxService, OutboxEntry } from '../../../core/offline/outbox.service';
import { SyncService, SyncStatus } from '../../../core/offline/sync.service';

describe('OfflineIndicatorComponent', () => {
  let fixture: ComponentFixture<OfflineIndicatorComponent>;

  // Signals pilotables depuis les tests.
  let isOnline: WritableSignal<boolean>;
  let pendingCount: WritableSignal<number>;
  let conflicts: WritableSignal<OutboxEntry[]>;
  let status: WritableSignal<SyncStatus>;
  const syncNow = jasmine.createSpy('syncNow').and.resolveTo();

  beforeEach(async () => {
    isOnline = signal(true);
    pendingCount = signal(0);
    conflicts = signal<OutboxEntry[]>([]);
    status = signal<SyncStatus>('idle');

    await TestBed.configureTestingModule({
      imports: [OfflineIndicatorComponent],
      providers: [
        { provide: ConnectivityService, useValue: { isOnline: isOnline.asReadonly() } },
        {
          provide: OutboxService,
          useValue: { pendingCount: pendingCount.asReadonly(), conflicts: conflicts.asReadonly() },
        },
        { provide: SyncService, useValue: { status: status.asReadonly(), syncNow } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OfflineIndicatorComponent);
    fixture.detectChanges();
  });

  function text(): string {
    const el = fixture.nativeElement.querySelector('.nx-offline-indicator__label');
    return el ? el.textContent.trim() : '';
  }

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the "synced / À jour" state when online and empty', () => {
    expect(text()).toBe('À jour');
    const el = fixture.nativeElement.querySelector('.nx-offline-indicator--synced');
    expect(el).toBeTruthy();
  });

  it('should render the offline state with pending count', () => {
    isOnline.set(false);
    pendingCount.set(3);
    fixture.detectChanges();
    expect(text()).toBe('Hors ligne — 3 opérations en attente');
    expect(fixture.nativeElement.querySelector('.nx-offline-indicator--offline')).toBeTruthy();
  });

  it('should render the syncing state', () => {
    status.set('syncing');
    fixture.detectChanges();
    expect(text()).toBe('Synchronisation…');
    expect(fixture.nativeElement.querySelector('.nx-offline-indicator--syncing')).toBeTruthy();
  });

  it('should render the conflict state', () => {
    conflicts.set([{ id: '1' } as OutboxEntry]);
    fixture.detectChanges();
    expect(text()).toBe('1 conflit à résoudre');
    expect(fixture.nativeElement.querySelector('.nx-offline-indicator--conflict')).toBeTruthy();
  });

  it('should expose role=status and aria-live=polite for accessibility', () => {
    isOnline.set(false);
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-offline-indicator');
    expect(el.getAttribute('role')).toBe('status');
    expect(el.getAttribute('aria-live')).toBe('polite');
  });

  it('should call syncNow on click (manual retry)', () => {
    isOnline.set(false);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.nx-offline-indicator').click();
    expect(syncNow).toHaveBeenCalled();
  });
});
