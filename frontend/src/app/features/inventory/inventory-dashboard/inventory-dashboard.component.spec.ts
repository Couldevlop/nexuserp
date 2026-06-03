import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InventoryDashboardComponent } from './inventory-dashboard.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../core/services/notification.service';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ProductDto } from '../inventory.types';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

function product(over: Partial<ProductDto> = {}): ProductDto {
  return {
    id: 'p1', tenantId: 't1', productCode: 'ART-001', name: 'Acier',
    description: null, category: null, unit: 'KG', status: 'ACTIVE',
    quantityOnHand: 100, quantityReserved: 0, availableQuantity: 100,
    reorderPoint: 20, reorderQuantity: 50, safetyStock: 10,
    valuationMethod: 'PMP_REALTIME', averageCostAmount: 5, averageCostCurrency: 'XOF',
    warehouseId: 'WH1', warehouseLocation: 'A-1', serialTracked: false,
    lotTracked: false, expiryTracked: false, needsReorder: false, ...over
  };
}

function page(content: ProductDto[]) {
  return { content, totalPages: 1, totalElements: content.length, number: 0, size: 500, first: true, last: true, empty: content.length === 0 };
}

describe('InventoryDashboardComponent', () => {
  let component: InventoryDashboardComponent;
  let fixture: ComponentFixture<InventoryDashboardComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  function withProducts(items: ProductDto[]): void {
    httpClient.get.and.callFake(((url: string) => {
      if (url.endsWith('/products')) return of(page(items));
      return throwError(() => ({ status: 404 })); // expiry alerts absent
    }) as any);
  }

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);

    await TestBed.configureTestingModule({
      imports: [InventoryDashboardComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InventoryDashboardComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as any;
  });

  it('should create', () => {
    withProducts([product()]);
    expect(component).toBeTruthy();
  });

  it('should compute total references from real data', () => {
    withProducts([product({ id: 'a' }), product({ id: 'b' })]);
    fixture.detectChanges();
    expect(component.totalReferences()).toBe(2);
    expect(component.isLoading()).toBeFalse();
  });

  it('should compute out-of-stock count', () => {
    withProducts([product({ id: 'a', quantityOnHand: 0 }), product({ id: 'b', quantityOnHand: 50 })]);
    fixture.detectChanges();
    expect(component.outOfStockCount()).toBe(1);
  });

  it('should compute low-stock products', () => {
    withProducts([product({ id: 'a', quantityOnHand: 5, reorderPoint: 20 }), product({ id: 'b', quantityOnHand: 100, reorderPoint: 20 })]);
    fixture.detectChanges();
    expect(component.lowStockCount()).toBe(1);
  });

  it('should compute stock value from average cost', () => {
    withProducts([product({ quantityOnHand: 10, averageCostAmount: 3 })]);
    fixture.detectChanges();
    expect(component.stockValue()).toBe(30);
    expect(component.stockCurrency()).toBe('XOF');
  });

  it('should return null stock value when no cost known', () => {
    withProducts([product({ averageCostAmount: null })]);
    fixture.detectChanges();
    expect(component.stockValue()).toBeNull();
  });

  it('should keep expiry alerts null when endpoint absent (no faked numbers)', () => {
    withProducts([product()]);
    fixture.detectChanges();
    expect(component.expiryAlerts()).toBeNull();
    expect(component.expiryAlertCount()).toBeNull();
  });

  it('should set expiry alerts when endpoint responds', () => {
    httpClient.get.and.callFake(((url: string) => {
      if (url.endsWith('/products')) return of(page([product()]));
      return of([{ productCode: 'X', productName: 'Y', lotNumber: 'L', quantity: 1, unit: 'KG', expiryDate: '2026-06-01', daysLeft: 10 }]);
    }) as any);
    fixture.detectChanges();
    expect(component.expiryAlertCount()).toBe(1);
  });

  it('should set error state on products load failure', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();
    expect(component.hasError()).toBeTrue();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.totalReferences()).toBe(0);
  });

  it('should render KPI labels', () => {
    withProducts([product()]);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Références totales');
    expect(el.textContent).toContain('Alertes péremption');
  });
});
