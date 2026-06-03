import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProcurementDashboardComponent } from './procurement-dashboard.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../core/services/notification.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('ProcurementDashboardComponent', () => {
  let component: ProcurementDashboardComponent;
  let fixture: ComponentFixture<ProcurementDashboardComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const future = (() => {
    const d = new Date();
    d.setDate(d.getDate() + 30);
    return d.toISOString().split('T')[0];
  })();

  const mockPage = {
    content: [
      { id: 'p1', poNumber: 'PO-1', supplierName: 'Alpha', totalAmount: 1000, currency: 'XOF', status: 'APPROVED', expectedDeliveryDate: future },
      { id: 'p2', poNumber: 'PO-2', supplierName: 'Alpha', totalAmount: 500, currency: 'XOF', status: 'DRAFT', expectedDeliveryDate: null },
      { id: 'p3', poNumber: 'PO-3', supplierName: 'Beta', totalAmount: 300, currency: 'XOF', status: 'SENT_TO_SUPPLIER', expectedDeliveryDate: future },
      { id: 'p4', poNumber: 'PO-4', supplierName: 'Gamma', totalAmount: 999, currency: 'XOF', status: 'CANCELLED', expectedDeliveryDate: future },
    ],
    totalElements: 4,
  };

  beforeEach(async () => {
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [ProcurementDashboardComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProcurementDashboardComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading skeleton initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load purchase orders and compute KPIs', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/procurement/purchase-orders', jasmine.anything());
    expect(component.isLoading()).toBeFalse();
    expect(component.hasData()).toBeTrue();
  });

  it('should count open orders excluding received/closed/cancelled', () => {
    fixture.detectChanges();
    // DRAFT + APPROVED + SENT_TO_SUPPLIER = 3
    expect(component.openOrdersCount()).toBe(3);
  });

  it('should compute committed amount from engaged statuses only', () => {
    fixture.detectChanges();
    // APPROVED 1000 + SENT_TO_SUPPLIER 300 = 1300 (DRAFT and CANCELLED excluded)
    expect(component.committedAmount()).toBe(1300);
  });

  it('should count distinct active suppliers excluding cancelled', () => {
    fixture.detectChanges();
    // Alpha + Beta = 2 (Gamma only on a CANCELLED PO)
    expect(component.activeSuppliersCount()).toBe(2);
  });

  it('should count expected deliveries with a future date', () => {
    fixture.detectChanges();
    // APPROVED(future) + SENT_TO_SUPPLIER(future) = 2
    expect(component.expectedDeliveriesCount()).toBe(2);
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

  it('should not render fake numbers when there is no data', () => {
    httpClient.get.and.returnValue(of({ content: [], totalElements: 0 }));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('—');
  });

  it('kpiValue should return a dash when there is no data', () => {
    httpClient.get.and.returnValue(of({ content: [], totalElements: 0 }));
    fixture.detectChanges();
    expect(component.kpiValue(5)).toBe('—');
  });

  it('should map status to label and badge class', () => {
    expect(component.getStatusLabel('SENT_TO_SUPPLIER')).toBe('Envoyée au fournisseur');
    expect(component.getStatusClass('CANCELLED')).toBe('nx-badge--error');
  });
});
