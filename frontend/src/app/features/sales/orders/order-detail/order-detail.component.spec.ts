import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderDetailComponent } from './order-detail.component';
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
  user: () => ({ email: 'manager@acme.com' }),
};

const mockRoute = {
  snapshot: { paramMap: { get: (_: string) => 'so-1' } },
};

const mockOrder = {
  id: 'so-1',
  orderNumber: 'SO-1700000000000',
  status: 'DRAFT' as const,
  customerName: 'Acme SARL',
  customerRef: 'PO-42',
  orderDate: '2026-01-15',
  requestedDeliveryDate: '2026-01-22',
  currency: 'XOF',
  totalAmount: 1800,
  shippingAddress: 'Abidjan',
  notes: 'Livraison rapide',
};

describe('OrderDetailComponent', () => {
  let component: OrderDetailComponent;
  let fixture: ComponentFixture<OrderDetailComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    mockAuth.hasAnyRole.and.returnValue(true);

    const httpSpy = jasmine.createSpyObj('HttpClient', ['get', 'put']);
    httpSpy.get.and.returnValue(of(mockOrder));
    httpSpy.put.and.returnValue(of({}));

    await TestBed.configureTestingModule({
      imports: [OrderDetailComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: AuthStateService, useValue: mockAuth },
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderDetailComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the order on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/sales/orders/so-1');
    expect(component.order()?.orderNumber).toBe('SO-1700000000000');
    expect(component.isLoading()).toBeFalse();
  });

  it('should render header and totals', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('SO-1700000000000');
    expect(compiled.textContent).toContain('Acme SARL');
  });

  it('should redirect and toast when the order is not found', () => {
    const router = TestBed.inject(Router);
    const navSpy = spyOn(router, 'navigate');
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/sales/orders']);
  });

  it('should allow confirm for a DRAFT order when manager', () => {
    fixture.detectChanges();
    expect(component.canConfirm()).toBeTrue();
  });

  it('should not allow confirm without manager role', () => {
    mockAuth.hasAnyRole.and.returnValue(false);
    fixture.detectChanges();
    expect(component.canConfirm()).toBeFalse();
  });

  it('should allow cancel for a DRAFT order', () => {
    fixture.detectChanges();
    expect(component.canCancel()).toBeTrue();
  });

  it('should not allow cancel once delivered', () => {
    httpClient.get.and.returnValue(of({ ...mockOrder, status: 'DELIVERED' as const }));
    fixture.detectChanges();
    expect(component.canCancel()).toBeFalse();
    expect(component.canConfirm()).toBeFalse();
  });

  it('should call the confirm endpoint and refresh on success', () => {
    fixture.detectChanges();
    component.confirm();
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/sales/orders/so-1/confirm',
      jasmine.objectContaining({ confirmedBy: 'manager@acme.com' })
    );
    expect(mockNotif.success).toHaveBeenCalled();
    // get called once on init + once on refresh
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should call the cancel endpoint with a reason', () => {
    fixture.detectChanges();
    component.cancel();
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/sales/orders/so-1/cancel',
      jasmine.objectContaining({ reason: jasmine.any(String) })
    );
    expect(mockNotif.success).toHaveBeenCalled();
  });

  it('should toast an error when confirm fails', () => {
    fixture.detectChanges();
    httpClient.put.and.returnValue(throwError(() => new Error('500')));
    component.confirm();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.actionInProgress()).toBeNull();
  });

  it('should map statuses to labels and classes', () => {
    expect(component.getStatusLabel('CONFIRMED')).toBe('Confirmée');
    expect(component.getStatusClass('CANCELLED')).toBe('nx-badge--error');
  });

  it('should support the wrapped { data } response shape', () => {
    httpClient.get.and.returnValue(of({ data: { ...mockOrder, id: 'so-2' } }));
    fixture.detectChanges();
    expect(component.order()?.id).toBe('so-2');
  });

  it('should format the total amount', () => {
    fixture.detectChanges();
    expect(component.formatAmount(1800)).not.toBe('—');
  });
});
