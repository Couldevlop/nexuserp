import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductionDashboardComponent } from './production-dashboard.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../core/services/notification.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

function wo(partial: Record<string, unknown>): Record<string, unknown> {
  return {
    id: 'x',
    tenantId: 't1',
    orderNumber: 'WO-x',
    productId: 'P',
    productName: 'Produit',
    status: 'PLANNED',
    priority: 'NORMAL',
    quantityPlanned: 100,
    quantityProduced: 0,
    quantityRejected: 0,
    plannedStartDate: '2026-01-01',
    plannedEndDate: '2026-01-10',
    workcenter: 'A',
    operator: null,
    isLate: false,
    yieldRate: 0,
    ...partial,
  };
}

describe('ProductionDashboardComponent', () => {
  let component: ProductionDashboardComponent;
  let fixture: ComponentFixture<ProductionDashboardComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const mockPage = {
    content: [
      wo({ id: 'o1', status: 'IN_PROGRESS' }),
      wo({ id: 'o2', status: 'PLANNED' }),
      wo({ id: 'o3', status: 'RELEASED' }),
      wo({ id: 'o4', status: 'COMPLETED' }),
      wo({ id: 'o5', status: 'CANCELLED' }),
      wo({ id: 'o6', status: 'IN_PROGRESS', isLate: true }),
    ],
    totalElements: 6,
  };

  beforeEach(async () => {
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [ProductionDashboardComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductionDashboardComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading skeleton initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load work orders and compute KPIs', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/production/work-orders', jasmine.anything());
    expect(component.isLoading()).toBeFalse();
    expect(component.hasData()).toBeTrue();
  });

  it('should count in-progress work orders', () => {
    fixture.detectChanges();
    // 2 IN_PROGRESS
    expect(component.inProgressCount()).toBe(2);
  });

  it('should count planned (PLANNED + RELEASED) work orders', () => {
    fixture.detectChanges();
    expect(component.plannedCount()).toBe(2);
  });

  it('should compute completion rate excluding cancelled', () => {
    fixture.detectChanges();
    // active = 5 (excludes 1 cancelled), completed = 1 → 20%
    expect(component.completionRate()).toBe(20);
  });

  it('should count late work orders', () => {
    fixture.detectChanges();
    expect(component.lateCount()).toBe(1);
  });

  it('should limit recent work orders to 5', () => {
    fixture.detectChanges();
    expect(component.recentWorkOrders().length).toBe(5);
  });

  it('should show empty state when no data', () => {
    httpClient.get.and.returnValue(of({ content: [], totalElements: 0 }));
    fixture.detectChanges();
    expect(component.hasData()).toBeFalse();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Aucune donnée');
  });

  it('should toast error and degrade on API failure', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.hasData()).toBeFalse();
    expect(component.isLoading()).toBeFalse();
  });

  it('should not render fabricated numbers when there is no data', () => {
    httpClient.get.and.returnValue(of({ content: [], totalElements: 0 }));
    fixture.detectChanges();
    expect(component.statValue(component.inProgressCount())).toBe('—');
    expect(component.completionValue()).toBe('—');
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('—');
  });

  it('should render real numbers when data is present', () => {
    fixture.detectChanges();
    expect(component.statValue(component.inProgressCount())).toBe('2');
    expect(component.completionValue()).toContain('%');
  });
});
