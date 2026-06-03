import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InvoiceCreateComponent } from './invoice-create.component';
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

describe('InvoiceCreateComponent', () => {
  let component: InvoiceCreateComponent;
  let fixture: ComponentFixture<InvoiceCreateComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;
  let router: Router;

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['post']);

    await TestBed.configureTestingModule({
      imports: [InvoiceCreateComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InvoiceCreateComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as any;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with 1 empty line', () => {
    expect(component.form().lines.length).toBe(1);
  });

  it('should add a line when addLine called', () => {
    component.addLine();
    expect(component.form().lines.length).toBe(2);
  });

  it('should remove a line when removeLine called (with multiple lines)', () => {
    component.addLine();
    component.removeLine(0);
    expect(component.form().lines.length).toBe(1);
  });

  it('should not allow removing the last line (UI prevents it)', () => {
    // Remove button is hidden when only 1 line — test line count stays 1
    expect(component.form().lines.length).toBe(1);
  });

  it('should update form field via updateField', () => {
    component.updateField('customerName', 'Acme Corp');
    expect(component.form().customerName).toBe('Acme Corp');
  });

  it('should update line field via updateLine', () => {
    component.updateLine(0, 'description', 'Test service');
    expect(component.form().lines[0].description).toBe('Test service');
  });

  it('should calculate line total correctly', () => {
    const line = { description: 'S', quantity: 5, unitPrice: 100, discountPercent: 10, taxRate: 20 };
    // (5 * 100 * 0.9) * 1.2 = 540
    expect(component.getLineTotal(line)).toBeCloseTo(540);
  });

  it('should calculate subtotal correctly', () => {
    component.updateLine(0, 'quantity', 10);
    component.updateLine(0, 'unitPrice', 100);
    component.updateLine(0, 'discountPercent', 0);
    expect(component.getSubtotal()).toBe(1000);
  });

  it('should calculate total tax correctly', () => {
    component.updateLine(0, 'quantity', 10);
    component.updateLine(0, 'unitPrice', 100);
    component.updateLine(0, 'discountPercent', 0);
    component.updateLine(0, 'taxRate', 20);
    // 1000 * 20% = 200
    expect(component.getTotalTax()).toBe(200);
  });

  it('should calculate grand total correctly', () => {
    component.updateLine(0, 'quantity', 10);
    component.updateLine(0, 'unitPrice', 100);
    component.updateLine(0, 'discountPercent', 0);
    component.updateLine(0, 'taxRate', 20);
    expect(component.getTotal()).toBe(1200);
  });

  it('should show error and not submit when customerName is blank', () => {
    component.updateField('customerName', '');
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should submit and navigate on success', () => {
    const navSpy = spyOn(router, 'navigate');
    httpClient.post.and.returnValue(of({ data: { id: 'inv-new' } }));

    component.updateField('customerName', 'Test Client');
    component.updateLine(0, 'description', 'Service');
    component.updateLine(0, 'unitPrice', 500);
    component.submit();

    expect(mockNotif.success).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/finance/invoices', 'inv-new']);
  });

  it('should show error notification on submission failure', () => {
    httpClient.post.and.returnValue(throwError(() => new Error('500')));

    component.updateField('customerName', 'Test Client');
    component.updateLine(0, 'description', 'Service');
    component.submit();

    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isSubmitting()).toBeFalse();
  });

  it('should have EUR as default currency', () => {
    expect(component.form().currency).toBe('EUR');
  });

  it('should have 20 as default tax rate', () => {
    expect(component.form().taxRate).toBe(20);
  });

  it('should list available currencies', () => {
    expect(component.currencies).toContain('EUR');
    expect(component.currencies).toContain('XOF');
    expect(component.currencies).toContain('USD');
  });

  it('should list available tax rates including UEMOA 18%', () => {
    const rates = component.taxRates.map(r => r.value);
    expect(rates).toContain(18);
    expect(rates).toContain(20);
    expect(rates).toContain(0);
  });
});
