import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SalesDashboardComponent } from './sales-dashboard.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../core/services/notification.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('SalesDashboardComponent', () => {
  let component: SalesDashboardComponent;
  let fixture: ComponentFixture<SalesDashboardComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const thisMonth = (() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-10`;
  })();

  const mockPage = {
    content: [
      { id: 'o1', orderNumber: 'SO-1', customerName: 'Acme', totalAmount: 1000, currency: 'XOF', status: 'INVOICED', orderDate: thisMonth },
      { id: 'o2', orderNumber: 'SO-2', customerName: 'Acme', totalAmount: 500, currency: 'XOF', status: 'CONFIRMED', orderDate: thisMonth },
      { id: 'o3', orderNumber: 'SO-3', customerName: 'Beta', totalAmount: 300, currency: 'XOF', status: 'DRAFT', orderDate: thisMonth },
      { id: 'o4', orderNumber: 'SO-4', customerName: 'Gamma', totalAmount: 999, currency: 'XOF', status: 'CANCELLED', orderDate: thisMonth },
    ],
    totalElements: 4,
  };

  beforeEach(async () => {
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [SalesDashboardComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SalesDashboardComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading skeleton initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load orders and compute KPIs', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/sales/orders', jasmine.anything());
    expect(component.isLoading()).toBeFalse();
    expect(component.hasData()).toBeTrue();
  });

  it('should compute monthly revenue from delivered/invoiced only', () => {
    fixture.detectChanges();
    // only INVOICED 1000 counts this month
    expect(component.monthlyRevenue()).toBe(1000);
  });

  it('should count open orders', () => {
    fixture.detectChanges();
    // DRAFT + CONFIRMED = 2
    expect(component.openOrdersCount()).toBe(2);
  });

  it('should aggregate top customers excluding cancelled', () => {
    fixture.detectChanges();
    const top = component.topCustomers();
    expect(top[0].name).toBe('Acme');
    expect(top[0].total).toBe(1500);
    expect(top.find(c => c.name === 'Gamma')).toBeUndefined();
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
});
