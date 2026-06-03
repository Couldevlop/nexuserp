import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportListComponent } from './report-list.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { ReportHistoryStore } from '../report-history.store';
import { ReportDto } from '../reporting-format';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
  info: jasmine.createSpy('info'),
  warning: jasmine.createSpy('warning'),
};

function dto(over: Partial<ReportDto> = {}): ReportDto {
  return {
    id: 'r-1',
    type: 'BALANCE_SHEET',
    status: 'COMPLETED',
    downloadUrl: 'https://minio.local/reports/r-1.xlsx',
    errorMessage: null,
    requestedAt: '2026-06-01T10:00:00',
    completedAt: '2026-06-01T10:01:00',
    ...over,
  };
}

describe('ReportListComponent', () => {
  let component: ReportListComponent;
  let fixture: ComponentFixture<ReportListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;
  let store: jasmine.SpyObj<ReportHistoryStore>;

  beforeEach(async () => {
    mockNotif.error.calls.reset();
    mockNotif.info.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(dto()));
    const storeSpy = jasmine.createSpyObj('ReportHistoryStore', ['list', 'upsert', 'remove']);
    storeSpy.list.and.returnValue([]);

    await TestBed.configureTestingModule({
      imports: [ReportListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
        { provide: ReportHistoryStore, useValue: storeSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
    store = TestBed.inject(ReportHistoryStore) as unknown as jasmine.SpyObj<ReportHistoryStore>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading state initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load local history with no pending → no http calls', () => {
    store.list.and.returnValue([dto()]);
    fixture.detectChanges();
    expect(component.reports().length).toBe(1);
    expect(component.isLoading()).toBeFalse();
    expect(httpClient.get).not.toHaveBeenCalled();
  });

  it('should poll status for pending jobs and refresh from store', () => {
    const pending = dto({ id: 'r-2', status: 'PROCESSING', downloadUrl: null, completedAt: null });
    const completed = dto({ id: 'r-2', status: 'COMPLETED' });
    store.list.and.returnValues([pending], [completed]);
    httpClient.get.and.returnValue(of(completed));

    fixture.detectChanges();

    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/reports/r-2/status');
    expect(store.upsert).toHaveBeenCalledWith(completed);
    expect(component.reports()[0].status).toBe('COMPLETED');
    expect(component.isLoading()).toBeFalse();
  });

  it('should keep job when status polling fails (graceful)', () => {
    const pending = dto({ id: 'r-3', status: 'PENDING', downloadUrl: null });
    store.list.and.returnValue([pending]);
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();
    expect(component.isLoading()).toBeFalse();
    expect(component.reports().length).toBe(1);
  });

  it('should show empty state when no reports', () => {
    store.list.and.returnValue([]);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Aucun rapport');
  });

  it('should render report rows', () => {
    store.list.and.returnValue([dto()]);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Bilan');
    expect(compiled.textContent).toContain('Disponible');
  });

  it('should filter by type', () => {
    store.list.and.returnValue([
      dto({ id: 'a', type: 'BALANCE_SHEET' }),
      dto({ id: 'b', type: 'FEC_EXPORT' }),
    ]);
    fixture.detectChanges();
    component.onTypeChange('FEC_EXPORT');
    expect(component.filteredReports().length).toBe(1);
    expect(component.filteredReports()[0].type).toBe('FEC_EXPORT');
  });

  it('canDownload should be true only for completed with url', () => {
    expect(component.canDownload(dto())).toBeTrue();
    expect(component.canDownload(dto({ status: 'PROCESSING', downloadUrl: null }))).toBeFalse();
    expect(component.canDownload(dto({ status: 'COMPLETED', downloadUrl: null }))).toBeFalse();
  });

  it('should open download url on download', () => {
    const openSpy = spyOn(window, 'open');
    component.download(dto());
    expect(openSpy).toHaveBeenCalledWith(
      'https://minio.local/reports/r-1.xlsx',
      '_blank',
      'noopener,noreferrer',
    );
  });

  it('should not open when report is not ready', () => {
    const openSpy = spyOn(window, 'open');
    component.download(dto({ status: 'PROCESSING', downloadUrl: null }));
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('should remove a report from local history', () => {
    store.list.and.returnValue([]);
    component.remove(dto());
    expect(store.remove).toHaveBeenCalledWith('r-1');
    expect(mockNotif.info).toHaveBeenCalled();
  });

  it('should expose all backend report types as filter options', () => {
    const values = component.typeOptions.map((o) => o.value);
    expect(values).toContain('BALANCE_SHEET');
    expect(values).toContain('FEC_EXPORT');
    expect(values).toContain('SYSCOHADA_EXPORT');
    expect(values).toContain('STOCK_VALUATION');
  });

  it('should map status to badge classes and labels', () => {
    expect(component.statusBadge('COMPLETED')).toBe('nx-badge--success');
    expect(component.statusBadge('FAILED')).toBe('nx-badge--error');
    expect(component.statusBadge('PROCESSING')).toBe('nx-badge--info');
    expect(component.statusLabel('PENDING')).toBe('En attente');
    expect(component.typeLabel('BALANCE_SHEET')).toBe('Bilan');
  });
});
