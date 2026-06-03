import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderListComponent } from './order-list.component';
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

describe('OrderListComponent', () => {
  let component: OrderListComponent;
  let fixture: ComponentFixture<OrderListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  // Format Spring Data Page renvoyé par nexus-sales.
  const mockPage = {
    content: [
      {
        id: 'so-1',
        orderNumber: 'SO-1700000000000',
        customerName: 'Acme SARL',
        totalAmount: 1800,
        currency: 'XOF',
        status: 'CONFIRMED' as const,
        orderDate: '2026-01-15',
        requestedDeliveryDate: '2026-01-22',
      }
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
      imports: [OrderListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading state initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load orders on init from the Spring page shape', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/sales/orders', jasmine.anything());
    expect(component.orders().length).toBe(1);
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
    expect(compiled.textContent).toContain('SO-1700000000000');
    expect(compiled.textContent).toContain('Acme SARL');
  });

  it('should handle API error — show error toast and empty list', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isLoading()).toBeFalse();
    expect(component.orders().length).toBe(0);
  });

  it('should show empty state when there are no orders', () => {
    httpClient.get.and.returnValue(of({ content: [], totalPages: 0, totalElements: 0 }));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Aucune commande');
  });

  it('should reload when filtering by status', () => {
    fixture.detectChanges();
    component.onStatusChange('CONFIRMED');
    expect(component.statusFilter()).toBe('CONFIRMED');
    expect(component.currentPage()).toBe(0);
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should filter locally on search without extra requests', () => {
    fixture.detectChanges();
    component.onSearch('acme');
    expect(component.filteredOrders().length).toBe(1);
    component.onSearch('inconnu');
    expect(component.filteredOrders().length).toBe(0);
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
    const values = component.statusOptions.map(o => o.value);
    expect(values).toContain('DRAFT');
    expect(values).toContain('CONFIRMED');
    expect(values).toContain('PICKING');
    expect(values).toContain('SHIPPED');
    expect(values).toContain('DELIVERED');
    expect(values).toContain('INVOICED');
    expect(values).toContain('CANCELLED');
  });

  it('should map status to badge classes', () => {
    const map = component.statusBadgeClass();
    expect(map['CONFIRMED']).toBe('nx-badge--info');
    expect(map['DELIVERED']).toBe('nx-badge--success');
    expect(map['CANCELLED']).toBe('nx-badge--error');
    expect(map['DRAFT']).toBe('nx-badge--neutral');
  });

  it('should format XOF without decimals', () => {
    const out = component.formatAmount(1800, 'XOF');
    expect(out).not.toContain(',00');
    expect(out).toContain('1');
  });

  it('should render a dash for a null amount', () => {
    expect(component.formatAmount(null, 'EUR')).toBe('—');
  });

  it('should support the ApiPage fallback shape', () => {
    httpClient.get.and.returnValue(of({
      data: [{ ...mockPage.content[0], id: 'so-2' }],
      meta: { page: 0, size: 20, total: 1, totalPages: 1 }
    }));
    fixture.detectChanges();
    expect(component.orders().length).toBe(1);
    expect(component.totalItems()).toBe(1);
  });

  it('getPages should return the correct length', () => {
    component.totalPages.set(4);
    expect(component.getPages().length).toBe(4);
  });
});
