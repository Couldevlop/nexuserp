import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PoCreateComponent } from './po-create.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('PoCreateComponent', () => {
  let component: PoCreateComponent;
  let fixture: ComponentFixture<PoCreateComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;
  let router: Router;

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['post']);

    await TestBed.configureTestingModule({
      imports: [PoCreateComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PoCreateComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with one empty line', () => {
    expect(component.form().lines.length).toBe(1);
  });

  it('should add a line', () => {
    component.addLine();
    expect(component.form().lines.length).toBe(2);
  });

  it('should remove a line when multiple present', () => {
    component.addLine();
    component.removeLine(0);
    expect(component.form().lines.length).toBe(1);
  });

  it('should update a top-level field', () => {
    component.updateField('supplierName', 'Fournitures CI');
    expect(component.form().supplierName).toBe('Fournitures CI');
  });

  it('should update a line field', () => {
    component.updateLine(0, 'description', 'Ciment 50kg');
    expect(component.form().lines[0].description).toBe('Ciment 50kg');
  });

  it('should compute line total with tax', () => {
    const line = { productCode: '', description: 'S', quantity: 5, unitPrice: 100, taxRate: 20 };
    // (5 * 100) * 1.2 = 600
    expect(component.getLineTotal(line)).toBeCloseTo(600);
  });

  it('should compute subtotal, tax and total signals', () => {
    component.updateLine(0, 'quantity', 10);
    component.updateLine(0, 'unitPrice', 100);
    component.updateLine(0, 'taxRate', 20);
    expect(component.subtotal()).toBe(1000);
    expect(component.totalTax()).toBe(200);
    expect(component.total()).toBe(1200);
  });

  it('should be invalid when supplier name is blank', () => {
    component.updateLine(0, 'description', 'Service');
    component.updateLine(0, 'unitPrice', 100);
    expect(component.isValid()).toBeFalse();
  });

  it('should be invalid when a line has no description', () => {
    component.updateField('supplierName', 'Fournisseur');
    expect(component.isValid()).toBeFalse();
  });

  it('should be valid with supplier and one complete line', () => {
    component.updateField('supplierName', 'Fournisseur');
    component.updateLine(0, 'description', 'Service');
    component.updateLine(0, 'quantity', 1);
    component.updateLine(0, 'unitPrice', 100);
    expect(component.isValid()).toBeTrue();
  });

  it('should not submit and show error when invalid', () => {
    component.updateField('supplierName', '');
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should POST to the procurement endpoint and navigate on success', () => {
    const navSpy = spyOn(router, 'navigate');
    httpClient.post.and.returnValue(of({ id: 'po-new' }));

    component.updateField('supplierName', 'Test Fournisseur');
    component.updateLine(0, 'description', 'Service');
    component.updateLine(0, 'unitPrice', 500);
    component.submit();

    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/procurement/purchase-orders', jasmine.any(Object));
    expect(mockNotif.success).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/procurement/orders', 'po-new']);
  });

  it('should send only backend-supported line fields', () => {
    httpClient.post.and.returnValue(of({ id: 'po-new' }));
    component.updateField('supplierName', 'Test Fournisseur');
    component.updateLine(0, 'description', 'Service');
    component.updateLine(0, 'unitPrice', 500);
    component.submit();

    const body = httpClient.post.calls.mostRecent().args[1] as {
      supplierName: string;
      lines: Array<Record<string, unknown>>;
    };
    expect(body.supplierName).toBe('Test Fournisseur');
    const line = body.lines[0];
    expect(line['description']).toBe('Service');
    expect(line['quantity']).toBe(1);
    expect(line['unitPrice']).toBe(500);
    expect(line['taxRate']).toBeDefined();
  });

  it('should show error toast on submission failure', () => {
    httpClient.post.and.returnValue(throwError(() => new Error('500')));
    component.updateField('supplierName', 'Test Fournisseur');
    component.updateLine(0, 'description', 'Service');
    component.updateLine(0, 'unitPrice', 100);
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isSubmitting()).toBeFalse();
  });

  it('should default to EUR currency', () => {
    expect(component.form().currency).toBe('EUR');
  });

  it('should list UEMOA 18% tax rate', () => {
    const rates = component.taxRates.map(r => r.value);
    expect(rates).toContain(18);
  });
});
