import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MovementFormComponent } from './movement-form.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ProductDto } from '../../inventory.types';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
  warning: jasmine.createSpy('warning'),
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

describe('MovementFormComponent', () => {
  let component: MovementFormComponent;
  let fixture: ComponentFixture<MovementFormComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['post']);

    await TestBed.configureTestingModule({
      imports: [MovementFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MovementFormComponent);
    component = fixture.componentInstance;
    component.product = product();
    httpClient = TestBed.inject(HttpClient) as any;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default to IN type', () => {
    expect(component.form().type).toBe('IN');
    expect(component.isIn()).toBeTrue();
  });

  it('should mark TRANSFER as not supported', () => {
    component.patch('type', 'TRANSFER');
    expect(component.typeSupported()).toBeFalse();
  });

  it('should warn and not submit unsupported transfer', () => {
    component.patch('type', 'TRANSFER');
    component.submit();
    expect(mockNotif.warning).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should reject non-positive quantity', () => {
    component.patch('quantity', 0);
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should require unit cost for IN', () => {
    component.patch('type', 'IN');
    component.patch('quantity', 10);
    component.patch('unitCost', 0);
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should POST to /receive for IN and emit saved', () => {
    const savedSpy = jasmine.createSpy('saved');
    component.saved.subscribe(savedSpy);
    httpClient.post.and.returnValue(of(product({ quantityOnHand: 110 })));

    component.patch('type', 'IN');
    component.patch('quantity', 10);
    component.patch('unitCost', 5);
    component.submit();

    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/inventory/products/p1/receive', jasmine.objectContaining({ quantity: 10, unitCost: 5 })
    );
    expect(mockNotif.success).toHaveBeenCalled();
    expect(savedSpy).toHaveBeenCalled();
  });

  it('should POST to /issue for OUT', () => {
    httpClient.post.and.returnValue(of(product({ quantityOnHand: 90 })));
    component.patch('type', 'OUT');
    component.patch('quantity', 10);
    component.submit();
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/inventory/products/p1/issue', jasmine.objectContaining({ quantity: 10 })
    );
  });

  it('should require reason for ADJUSTMENT', () => {
    component.patch('type', 'ADJUSTMENT');
    component.patch('quantity', 50);
    component.patch('reason', '');
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should POST to /adjust for ADJUSTMENT with reason', () => {
    httpClient.post.and.returnValue(of(product({ quantityOnHand: 50 })));
    component.patch('type', 'ADJUSTMENT');
    component.patch('quantity', 50);
    component.patch('reason', 'Inventaire');
    component.submit();
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/inventory/products/p1/adjust', jasmine.objectContaining({ newQuantity: 50, reason: 'Inventaire' })
    );
  });

  it('should handle submit error', () => {
    httpClient.post.and.returnValue(throwError(() => new Error('500')));
    component.patch('type', 'OUT');
    component.patch('quantity', 10);
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isSubmitting()).toBeFalse();
  });

  it('should emit cancelled', () => {
    const cancelSpy = jasmine.createSpy('cancelled');
    component.cancelled.subscribe(cancelSpy);
    component.cancel();
    expect(cancelSpy).toHaveBeenCalled();
  });

  it('should preset type via initialType input', () => {
    component.initialType = 'ADJUSTMENT';
    expect(component.form().type).toBe('ADJUSTMENT');
  });
});
