import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductDetailComponent } from './product-detail.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ProductDto } from '../../inventory.types';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

function product(over: Partial<ProductDto> = {}): ProductDto {
  return {
    id: 'p1', tenantId: 't1', productCode: 'ART-001', name: 'Acier inox',
    description: 'desc', category: 'Matières', unit: 'KG', status: 'ACTIVE',
    quantityOnHand: 100, quantityReserved: 10, availableQuantity: 90,
    reorderPoint: 20, reorderQuantity: 50, safetyStock: 10,
    valuationMethod: 'PMP_REALTIME', averageCostAmount: 5, averageCostCurrency: 'XOF',
    warehouseId: 'WH1', warehouseLocation: 'A-1', serialTracked: false,
    lotTracked: true, expiryTracked: true, needsReorder: false, ...over
  };
}

describe('ProductDetailComponent', () => {
  let component: ProductDetailComponent;
  let fixture: ComponentFixture<ProductDetailComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  function setup(routeId: string | null = 'p1'): void {
    httpClient.get.and.callFake(((url: string) => {
      if (url.endsWith('/p1')) return of(product());
      // extras endpoints 404 → degrade
      return throwError(() => ({ status: 404 }));
    }) as any);
    fixture = TestBed.createComponent(ProductDetailComponent);
    component = fixture.componentInstance;
    const route = TestBed.inject(ActivatedRoute);
    spyOn(route.snapshot.paramMap, 'get').and.returnValue(routeId);
  }

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get', 'post']);

    await TestBed.configureTestingModule({
      imports: [ProductDetailComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    httpClient = TestBed.inject(HttpClient) as any;
  });

  it('should create', () => {
    setup();
    expect(component).toBeTruthy();
  });

  it('should redirect when no id in route', () => {
    setup(null);
    const router = TestBed.inject(Router);
    const navSpy = spyOn(router, 'navigate');
    component.ngOnInit();
    expect(navSpy).toHaveBeenCalledWith(['/inventory/products']);
  });

  it('should load product (raw ProductDto, not wrapped)', () => {
    setup();
    fixture.detectChanges();
    expect(component.product()?.productCode).toBe('ART-001');
    expect(component.isLoading()).toBeFalse();
  });

  it('should degrade gracefully when extras endpoints 404', () => {
    setup();
    fixture.detectChanges();
    expect(component.warehouseStock()).toEqual([]);
    expect(component.lots()).toEqual([]);
  });

  it('should compute isLowStock from product', () => {
    setup();
    httpClient.get.and.callFake(((url: string) =>
      url.endsWith('/p1') ? of(product({ quantityOnHand: 5, reorderPoint: 20 })) : throwError(() => ({ status: 404 }))) as any);
    fixture.detectChanges();
    expect(component.isLowStock()).toBeTrue();
  });

  it('should redirect on product load error', () => {
    setup();
    const router = TestBed.inject(Router);
    const navSpy = spyOn(router, 'navigate');
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/inventory/products']);
  });

  it('should open and close movement modal', () => {
    setup();
    fixture.detectChanges();
    component.openMovement('IN');
    expect(component.modalOpen()).toBeTrue();
    expect(component.modalType()).toBe('IN');
    component.closeMovement();
    expect(component.modalOpen()).toBeFalse();
    expect(component.modalType()).toBeNull();
  });

  it('should update product and close modal when movement saved', () => {
    setup();
    fixture.detectChanges();
    component.openMovement('IN');
    const updated = product({ quantityOnHand: 150 });
    component.onMovementSaved(updated);
    expect(component.product()?.quantityOnHand).toBe(150);
    expect(component.modalOpen()).toBeFalse();
  });

  it('should detect near-expiry and expired dates', () => {
    setup();
    const soon = new Date(Date.now() + 5 * 86400000).toISOString();
    const past = new Date(Date.now() - 5 * 86400000).toISOString();
    const far = new Date(Date.now() + 365 * 86400000).toISOString();
    expect(component.isNearExpiry(soon)).toBeTrue();
    expect(component.isExpired(past)).toBeTrue();
    expect(component.isNearExpiry(far)).toBeFalse();
    expect(component.isNearExpiry(null)).toBeFalse();
  });
});
