import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WorkOrderDetailComponent } from './work-order-detail.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

const mockAuth = {
  hasAnyRole: jasmine.createSpy('hasAnyRole').and.returnValue(true),
  user: () => ({ email: 'operator@acme.com' }),
};

const mockRoute = {
  snapshot: { paramMap: { get: (_: string) => 'wo-1' } },
};

const baseWorkOrder = {
  id: 'wo-1',
  tenantId: 't1',
  orderNumber: 'WO-1700000000000',
  productId: 'P-1',
  productName: 'Ciment 50kg',
  status: 'PLANNED' as const,
  priority: 'HIGH' as const,
  quantityPlanned: 100,
  quantityProduced: 0,
  quantityRejected: 0,
  plannedStartDate: '2026-01-15',
  plannedEndDate: '2026-01-22',
  workcenter: 'Ligne A',
  operator: null,
  isLate: false,
  yieldRate: 0,
};

describe('WorkOrderDetailComponent', () => {
  let component: WorkOrderDetailComponent;
  let fixture: ComponentFixture<WorkOrderDetailComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    mockAuth.hasAnyRole.and.returnValue(true);

    const httpSpy = jasmine.createSpyObj('HttpClient', ['get', 'put']);
    httpSpy.get.and.returnValue(of({ ...baseWorkOrder }));
    httpSpy.put.and.returnValue(of({}));

    await TestBed.configureTestingModule({
      imports: [WorkOrderDetailComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: AuthStateService, useValue: mockAuth },
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: HttpClient, useValue: httpSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkOrderDetailComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the work order on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/production/work-orders/wo-1');
    expect(component.workOrder()?.orderNumber).toBe('WO-1700000000000');
    expect(component.isLoading()).toBeFalse();
  });

  it('should render header and quantities', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('WO-1700000000000');
    expect(compiled.textContent).toContain('Ciment 50kg');
  });

  it('should redirect and toast when the work order is not found', () => {
    const router = TestBed.inject(Router);
    const navSpy = spyOn(router, 'navigate');
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/production/work-orders']);
  });

  it('should allow release only for PLANNED + manager', () => {
    fixture.detectChanges();
    expect(component.canRelease()).toBeTrue();
    expect(component.canStart()).toBeFalse();
    expect(component.canComplete()).toBeFalse();
  });

  it('should not allow release without manager role', () => {
    mockAuth.hasAnyRole.and.returnValue(false);
    fixture.detectChanges();
    expect(component.canRelease()).toBeFalse();
  });

  it('should allow start for a RELEASED order', () => {
    httpClient.get.and.returnValue(of({ ...baseWorkOrder, status: 'RELEASED' as const }));
    fixture.detectChanges();
    expect(component.canStart()).toBeTrue();
    expect(component.canRelease()).toBeFalse();
  });

  it('should allow complete for IN_PROGRESS', () => {
    httpClient.get.and.returnValue(of({ ...baseWorkOrder, status: 'IN_PROGRESS' as const }));
    fixture.detectChanges();
    expect(component.canComplete()).toBeTrue();
  });

  it('should not allow actions for a COMPLETED order', () => {
    httpClient.get.and.returnValue(of({ ...baseWorkOrder, status: 'COMPLETED' as const }));
    fixture.detectChanges();
    expect(component.canRelease()).toBeFalse();
    expect(component.canStart()).toBeFalse();
    expect(component.canComplete()).toBeFalse();
  });

  it('should call the release endpoint and refresh on success', () => {
    fixture.detectChanges();
    component.release();
    expect(httpClient.put).toHaveBeenCalledWith('/api/v1/production/work-orders/wo-1/release', jasmine.any(Object));
    expect(mockNotif.success).toHaveBeenCalled();
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should call the start endpoint with operatorId', () => {
    httpClient.get.and.returnValue(of({ ...baseWorkOrder, status: 'RELEASED' as const }));
    fixture.detectChanges();
    component.start();
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/production/work-orders/wo-1/start',
      jasmine.objectContaining({ operatorId: 'operator@acme.com' })
    );
    expect(mockNotif.success).toHaveBeenCalled();
  });

  it('should record the remaining quantity when completing', () => {
    httpClient.get.and.returnValue(
      of({ ...baseWorkOrder, status: 'IN_PROGRESS' as const, quantityProduced: 60 })
    );
    fixture.detectChanges();
    expect(component.remainingQuantity()).toBe(40);
    component.complete();
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/production/work-orders/wo-1/production',
      jasmine.objectContaining({ quantity: 40, rejected: 0 })
    );
  });

  it('should toast an error when an action fails', () => {
    fixture.detectChanges();
    httpClient.put.and.returnValue(throwError(() => new Error('500')));
    component.release();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.actionInProgress()).toBeNull();
  });

  it('should map statuses and priorities to labels and classes', () => {
    expect(component.statusLabel('IN_PROGRESS')).toBe('En cours');
    expect(component.statusBadge('COMPLETED')).toBe('nx-badge--success');
    expect(component.priorityLabel('URGENT')).toBe('Urgente');
  });

  it('should support the wrapped { data } response shape', () => {
    httpClient.get.and.returnValue(of({ data: { ...baseWorkOrder, id: 'wo-2' } }));
    fixture.detectChanges();
    expect(component.workOrder()?.id).toBe('wo-2');
  });

  it('should format quantities and rates with a dash fallback', () => {
    fixture.detectChanges();
    expect(component.qty(null)).toBe('—');
    expect(component.rate(null)).toBe('—');
    expect(component.rate(38)).toContain('%');
  });
});
