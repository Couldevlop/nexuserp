import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductListComponent } from './product-list.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ProductDto } from '../../inventory.types';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
  warning: jasmine.createSpy('warning'),
  info: jasmine.createSpy('info'),
};

function product(over: Partial<ProductDto> = {}): ProductDto {
  return {
    id: 'p1', tenantId: 't1', productCode: 'ART-001', name: 'Acier inox',
    description: null, category: 'Matières', unit: 'KG', status: 'ACTIVE',
    quantityOnHand: 100, quantityReserved: 0, availableQuantity: 100,
    reorderPoint: 20, reorderQuantity: 50, safetyStock: 10,
    valuationMethod: 'PMP_REALTIME', averageCostAmount: 5, averageCostCurrency: 'XOF',
    warehouseId: 'WH1', warehouseLocation: 'A-1', serialTracked: false,
    lotTracked: false, expiryTracked: false, needsReorder: false, ...over
  };
}

const mockPage = {
  content: [product()],
  totalPages: 1, totalElements: 1, number: 0, size: 20, first: true, last: true, empty: false
};

describe('ProductListComponent', () => {
  let component: ProductListComponent;
  let fixture: ComponentFixture<ProductListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [ProductListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as any;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load products from /api/v1/inventory/products', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith(
      '/api/v1/inventory/products', jasmine.anything()
    );
    expect(component.products().length).toBe(1);
    expect(component.isLoading()).toBeFalse();
    expect(component.totalItems()).toBe(1);
  });

  it('should map SpringPage content (not ApiPage.data)', () => {
    fixture.detectChanges();
    expect(component.products()[0].productCode).toBe('ART-001');
  });

  it('should show error notification and empty list on failure', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.products().length).toBe(0);
    expect(component.isLoading()).toBeFalse();
  });

  it('should flag low-stock when qtyOnHand <= reorderPoint', () => {
    expect(component.isLowStock(product({ quantityOnHand: 20, reorderPoint: 20 }))).toBeTrue();
    expect(component.isLowStock(product({ quantityOnHand: 5, reorderPoint: 20 }))).toBeTrue();
    expect(component.isLowStock(product({ quantityOnHand: 100, reorderPoint: 20, needsReorder: false }))).toBeFalse();
  });

  it('should flag low-stock when backend needsReorder is true', () => {
    expect(component.isLowStock(product({ quantityOnHand: 999, reorderPoint: 0, needsReorder: true }))).toBeTrue();
  });

  it('should filter visibleProducts by search query', () => {
    httpClient.get.and.returnValue(of({
      ...mockPage,
      content: [product({ id: 'a', productCode: 'ABC' }), product({ id: 'b', productCode: 'XYZ', name: 'Zinc' })]
    }));
    fixture.detectChanges();
    component.onSearch('xyz');
    expect(component.visibleProducts().length).toBe(1);
    expect(component.visibleProducts()[0].productCode).toBe('XYZ');
  });

  it('should filter visibleProducts when lowStockOnly toggled', () => {
    httpClient.get.and.returnValue(of({
      ...mockPage,
      content: [product({ id: 'a', quantityOnHand: 5, reorderPoint: 20 }), product({ id: 'b', quantityOnHand: 100, reorderPoint: 20 })]
    }));
    fixture.detectChanges();
    component.toggleLowStock();
    expect(component.lowStockOnly()).toBeTrue();
    expect(component.visibleProducts().length).toBe(1);
  });

  it('should compute lowStockCount', () => {
    httpClient.get.and.returnValue(of({
      ...mockPage,
      content: [product({ id: 'a', quantityOnHand: 5, reorderPoint: 20 }), product({ id: 'b', quantityOnHand: 100, reorderPoint: 20 })]
    }));
    fixture.detectChanges();
    expect(component.lowStockCount()).toBe(1);
  });

  it('should reload when category changes', () => {
    fixture.detectChanges();
    component.onCategoryChange('Matières');
    expect(component.categoryFilter()).toBe('Matières');
    expect(component.currentPage()).toBe(0);
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should not navigate beyond totalPages', () => {
    fixture.detectChanges();
    component.totalPages.set(3);
    component.goToPage(99);
    expect(component.currentPage()).toBe(0);
  });

  it('should navigate to a valid page', () => {
    fixture.detectChanges();
    component.totalPages.set(5);
    component.goToPage(2);
    expect(component.currentPage()).toBe(2);
  });

  it('should render product code in the DOM', () => {
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('ART-001');
  });

  it('should display empty state when no products', () => {
    httpClient.get.and.returnValue(of({ ...mockPage, content: [], totalElements: 0 }));
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Aucun article');
  });
});
