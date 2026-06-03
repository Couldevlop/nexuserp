import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PoDetailComponent } from './po-detail.component';
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
  user: () => ({ email: 'buyer@acme.com' }),
};

const mockRoute = {
  snapshot: { paramMap: { get: (_: string) => 'po-1' } },
};

const mockOrder = {
  id: 'po-1',
  poNumber: 'PO-1700000000000',
  status: 'DRAFT' as const,
  supplierId: 'sup-1',
  supplierName: 'Fournitures CI',
  expectedDeliveryDate: '2026-02-10',
  currency: 'XOF',
  totalAmount: 2360,
  notes: 'Livraison urgente',
  approvedBy: null,
};

describe('PoDetailComponent', () => {
  let component: PoDetailComponent;
  let fixture: ComponentFixture<PoDetailComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    mockAuth.hasAnyRole.and.returnValue(true);

    const httpSpy = jasmine.createSpyObj('HttpClient', ['get', 'put']);
    httpSpy.get.and.returnValue(of(mockOrder));
    httpSpy.put.and.returnValue(of({}));

    await TestBed.configureTestingModule({
      imports: [PoDetailComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: AuthStateService, useValue: mockAuth },
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PoDetailComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the order on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/procurement/purchase-orders/po-1');
    expect(component.order()?.poNumber).toBe('PO-1700000000000');
    expect(component.isLoading()).toBeFalse();
  });

  it('should render header and totals', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('PO-1700000000000');
    expect(compiled.textContent).toContain('Fournitures CI');
  });

  it('should redirect and toast when the order is not found', () => {
    const router = TestBed.inject(Router);
    const navSpy = spyOn(router, 'navigate');
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/procurement/orders']);
  });

  it('should allow approve for a DRAFT order when manager', () => {
    fixture.detectChanges();
    expect(component.canApprove()).toBeTrue();
  });

  it('should not allow approve without manager role', () => {
    mockAuth.hasAnyRole.and.returnValue(false);
    fixture.detectChanges();
    expect(component.canApprove()).toBeFalse();
  });

  it('should not allow approve once already approved', () => {
    httpClient.get.and.returnValue(of({ ...mockOrder, status: 'APPROVED' as const }));
    fixture.detectChanges();
    expect(component.canApprove()).toBeFalse();
  });

  it('should call the approve endpoint and refresh on success', () => {
    fixture.detectChanges();
    component.approve();
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/procurement/purchase-orders/po-1/approve',
      jasmine.objectContaining({ approvedBy: 'buyer@acme.com' })
    );
    expect(mockNotif.success).toHaveBeenCalled();
    // get called once on init + once on refresh
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should toast an error when approve fails', () => {
    fixture.detectChanges();
    httpClient.put.and.returnValue(throwError(() => new Error('500')));
    component.approve();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.actionInProgress()).toBeNull();
  });

  it('should map statuses to labels and classes', () => {
    expect(component.getStatusLabel('APPROVED')).toBe('Approuvée');
    expect(component.getStatusLabel('SENT_TO_SUPPLIER')).toBe('Envoyée au fournisseur');
    expect(component.getStatusClass('CANCELLED')).toBe('nx-badge--error');
    expect(component.getStatusClass('RECEIVED')).toBe('nx-badge--success');
  });

  it('should support the wrapped { data } response shape', () => {
    httpClient.get.and.returnValue(of({ data: { ...mockOrder, id: 'po-2' } }));
    fixture.detectChanges();
    expect(component.order()?.id).toBe('po-2');
  });

  it('should format the total amount', () => {
    fixture.detectChanges();
    expect(component.formatAmount(2360)).not.toBe('—');
  });
});
