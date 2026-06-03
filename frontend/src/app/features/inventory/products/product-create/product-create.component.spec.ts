import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductCreateComponent } from './product-create.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('ProductCreateComponent', () => {
  let component: ProductCreateComponent;
  let fixture: ComponentFixture<ProductCreateComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;
  let router: Router;

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['post']);

    await TestBed.configureTestingModule({
      imports: [ProductCreateComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductCreateComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as any;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default to PMP_REALTIME valuation', () => {
    expect(component.form().valuationMethod).toBe('PMP_REALTIME');
  });

  it('should list all valuation methods including FIFO and LIFO', () => {
    const values = component.valuationMethods.map(v => v.value);
    expect(values).toContain('FIFO');
    expect(values).toContain('LIFO');
    expect(values).toContain('PMP_REALTIME');
    expect(values).toContain('STANDARD');
  });

  it('should patch a field', () => {
    component.patch('name', 'Acier');
    expect(component.form().name).toBe('Acier');
  });

  it('should not submit when productCode is blank', () => {
    component.patch('productCode', '');
    component.patch('name', 'X');
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should not submit when name is blank', () => {
    component.patch('productCode', 'ART-1');
    component.patch('name', '');
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should not submit with negative quantities', () => {
    component.patch('productCode', 'ART-1');
    component.patch('name', 'X');
    component.patch('reorderPoint', -5);
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should POST to products endpoint and navigate on success', () => {
    const navSpy = spyOn(router, 'navigate');
    httpClient.post.and.returnValue(of({ id: 'new-id' }));

    component.patch('productCode', 'ART-9');
    component.patch('name', 'Nouveau');
    component.submit();

    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/inventory/products', jasmine.any(Object)
    );
    expect(mockNotif.success).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/inventory/products', 'new-id']);
  });

  it('should send valuationMethod and tracking flags in payload', () => {
    httpClient.post.and.returnValue(of({ id: 'x' }));
    spyOn(router, 'navigate');
    component.patch('productCode', 'ART-9');
    component.patch('name', 'Nouveau');
    component.patch('lotTracked', true);
    component.patch('valuationMethod', 'FIFO');
    component.submit();

    const payload = httpClient.post.calls.mostRecent().args[1] as any;
    expect(payload.valuationMethod).toBe('FIFO');
    expect(payload.lotTracked).toBeTrue();
    expect(payload.productCode).toBe('ART-9');
  });

  it('should show error and reset submitting on failure', () => {
    httpClient.post.and.returnValue(throwError(() => new Error('500')));
    component.patch('productCode', 'ART-9');
    component.patch('name', 'Nouveau');
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isSubmitting()).toBeFalse();
  });
});
