import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportGenerateComponent } from './report-generate.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { ReportHistoryStore } from '../report-history.store';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

function makeRoute(type?: string) {
  return {
    snapshot: {
      queryParamMap: convertToParamMap(type ? { type } : {}),
    },
  } as unknown as ActivatedRoute;
}

describe('ReportGenerateComponent', () => {
  let component: ReportGenerateComponent;
  let fixture: ComponentFixture<ReportGenerateComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;
  let store: jasmine.SpyObj<ReportHistoryStore>;
  let router: Router;

  async function setup(type?: string) {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['post']);
    const storeSpy = jasmine.createSpyObj('ReportHistoryStore', ['upsert', 'list', 'remove']);

    await TestBed.configureTestingModule({
      imports: [ReportGenerateComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
        { provide: ReportHistoryStore, useValue: storeSpy },
        { provide: ActivatedRoute, useValue: makeRoute(type) },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportGenerateComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
    store = TestBed.inject(ReportHistoryStore) as unknown as jasmine.SpyObj<ReportHistoryStore>;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await setup();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default to BALANCE_SHEET and XLSX', () => {
    expect(component.form().type).toBe('BALANCE_SHEET');
    expect(component.form().outputFormat).toBe('XLSX');
  });

  it('should prefill type from query param', async () => {
    TestBed.resetTestingModule();
    await setup('FEC_EXPORT');
    expect(component.form().type).toBe('FEC_EXPORT');
  });

  it('should ignore an unknown query param type', async () => {
    TestBed.resetTestingModule();
    await setup('NOT_A_TYPE');
    expect(component.form().type).toBe('BALANCE_SHEET');
  });

  it('should force CSV format for FEC export', () => {
    component.updateField('type', 'FEC_EXPORT');
    expect(component.form().outputFormat).toBe('CSV');
    expect(component.recommendedFormat()).toBe('CSV');
  });

  it('should force XLSX format for SYSCOHADA export', () => {
    component.updateField('type', 'SYSCOHADA_EXPORT');
    expect(component.form().outputFormat).toBe('XLSX');
  });

  it('should be invalid when start date after end date', () => {
    component.updateField('periodFrom', '2026-12-31');
    component.updateField('periodTo', '2026-01-01');
    expect(component.isValid()).toBeFalse();
    expect(component.dateRangeInvalid()).toBeTrue();
  });

  it('should be valid with a coherent period', () => {
    component.updateField('periodFrom', '2026-01-01');
    component.updateField('periodTo', '2026-12-31');
    expect(component.isValid()).toBeTrue();
  });

  it('should not submit when invalid', () => {
    component.updateField('periodFrom', '2026-12-31');
    component.updateField('periodTo', '2026-01-01');
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should POST the correct payload and navigate on success', () => {
    const navSpy = spyOn(router, 'navigate');
    const created = {
      id: 'r-new',
      type: 'BALANCE_SHEET',
      status: 'PROCESSING',
      downloadUrl: null,
      errorMessage: null,
      requestedAt: '2026-06-01T10:00:00',
      completedAt: null,
    };
    httpClient.post.and.returnValue(of(created));

    component.updateField('type', 'BALANCE_SHEET');
    component.updateField('periodFrom', '2026-01-01');
    component.updateField('periodTo', '2026-06-01');
    component.submit();

    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/reports', jasmine.any(Object));
    const body = httpClient.post.calls.mostRecent().args[1] as Record<string, unknown>;
    expect(body['type']).toBe('BALANCE_SHEET');
    expect(body['periodFrom']).toBe('2026-01-01');
    expect(body['periodTo']).toBe('2026-06-01');
    expect(body['outputFormat']).toBe('XLSX');
    expect(store.upsert).toHaveBeenCalledWith(created);
    expect(mockNotif.success).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/reporting/reports']);
  });

  it('should show error toast on submission failure', () => {
    httpClient.post.and.returnValue(throwError(() => new Error('500')));
    component.updateField('periodFrom', '2026-01-01');
    component.updateField('periodTo', '2026-06-01');
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isSubmitting()).toBeFalse();
  });

  it('should expose all backend report types', () => {
    const values = component.typeOptions.map((o) => o.value);
    expect(values).toContain('FEC_EXPORT');
    expect(values).toContain('SYSCOHADA_EXPORT');
    expect(values).toContain('PAYROLL_SUMMARY');
    expect(values.length).toBe(15);
  });

  it('should expose all backend output formats', () => {
    const values = component.formatOptions.map((o) => o.value);
    expect(values).toEqual(jasmine.arrayContaining(['PDF', 'XLSX', 'CSV', 'JSON']));
  });
});
