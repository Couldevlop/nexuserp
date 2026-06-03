import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WorkOrderListComponent } from './work-order-list.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
  info: jasmine.createSpy('info'),
  warning: jasmine.createSpy('warning'),
};

describe('WorkOrderListComponent', () => {
  let component: WorkOrderListComponent;
  let fixture: ComponentFixture<WorkOrderListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  // Format Spring Data Page renvoyé par nexus-production.
  const mockPage = {
    content: [
      {
        id: 'wo-1',
        tenantId: 't1',
        orderNumber: 'WO-1700000000000',
        productId: 'P-1',
        productName: 'Ciment 50kg',
        status: 'IN_PROGRESS' as const,
        priority: 'HIGH' as const,
        quantityPlanned: 100,
        quantityProduced: 40,
        quantityRejected: 2,
        plannedStartDate: '2026-01-15',
        plannedEndDate: '2026-01-22',
        workcenter: 'Ligne A',
        operator: null,
        isLate: false,
        yieldRate: 38,
      },
    ],
    totalPages: 1,
    totalElements: 1,
    number: 0,
  };

  beforeEach(async () => {
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [WorkOrderListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkOrderListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading state initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load work orders on init from the Spring page shape', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/production/work-orders', jasmine.anything());
    expect(component.workOrders().length).toBe(1);
    expect(component.totalItems()).toBe(1);
    expect(component.totalPages()).toBe(1);
  });

  it('should set loading to false after load', () => {
    fixture.detectChanges();
    expect(component.isLoading()).toBeFalse();
  });

  it('should render rows in the table', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('WO-1700000000000');
    expect(compiled.textContent).toContain('Ciment 50kg');
  });

  it('should handle API error — show error toast and empty list', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isLoading()).toBeFalse();
    expect(component.workOrders().length).toBe(0);
  });

  it('should show empty state when there are no work orders', () => {
    httpClient.get.and.returnValue(of({ content: [], totalPages: 0, totalElements: 0 }));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Aucun ordre de fabrication');
  });

  it('should reload when filtering by status', () => {
    fixture.detectChanges();
    component.onStatusChange('IN_PROGRESS');
    expect(component.statusFilter()).toBe('IN_PROGRESS');
    expect(component.currentPage()).toBe(0);
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should filter locally on search without extra requests', () => {
    fixture.detectChanges();
    component.onSearch('ciment');
    expect(component.filteredWorkOrders().length).toBe(1);
    component.onSearch('inconnu');
    expect(component.filteredWorkOrders().length).toBe(0);
    expect(httpClient.get).toHaveBeenCalledTimes(1);
  });

  it('should navigate to a valid page', () => {
    fixture.detectChanges();
    component.totalPages.set(5);
    component.goToPage(3);
    expect(component.currentPage()).toBe(3);
  });

  it('should not navigate beyond totalPages', () => {
    fixture.detectChanges();
    component.totalPages.set(3);
    component.goToPage(10);
    expect(component.currentPage()).toBe(0);
  });

  it('should not navigate to negative page', () => {
    fixture.detectChanges();
    component.goToPage(-1);
    expect(component.currentPage()).toBe(0);
  });

  it('should expose all backend status options', () => {
    const values = component.statusOptions.map((o) => o.value);
    expect(values).toContain('PLANNED');
    expect(values).toContain('RELEASED');
    expect(values).toContain('IN_PROGRESS');
    expect(values).toContain('PARTIALLY_COMPLETED');
    expect(values).toContain('COMPLETED');
    expect(values).toContain('ON_HOLD');
    expect(values).toContain('CANCELLED');
  });

  it('should map status to label and badge classes', () => {
    expect(component.statusLabel('IN_PROGRESS')).toBe('En cours');
    expect(component.statusBadge('COMPLETED')).toBe('nx-badge--success');
    expect(component.statusBadge('CANCELLED')).toBe('nx-badge--error');
    expect(component.statusBadge('PLANNED')).toBe('nx-badge--neutral');
  });

  it('should map priority to label and badge classes', () => {
    expect(component.priorityLabel('URGENT')).toBe('Urgente');
    expect(component.priorityBadge('URGENT')).toBe('nx-badge--error');
    expect(component.priorityBadge('NORMAL')).toBe('nx-badge--info');
  });

  it('should format a quantity and render a dash for null', () => {
    expect(component.qty(null)).toBe('—');
    expect(component.qty(100)).toContain('100');
  });

  it('should support the ApiPage fallback shape', () => {
    httpClient.get.and.returnValue(
      of({
        data: [{ ...mockPage.content[0], id: 'wo-2' }],
        meta: { page: 0, size: 20, total: 1, totalPages: 1 },
      })
    );
    fixture.detectChanges();
    expect(component.workOrders().length).toBe(1);
    expect(component.totalItems()).toBe(1);
  });

  it('getPages should return the correct length', () => {
    component.totalPages.set(4);
    expect(component.getPages().length).toBe(4);
  });
});
