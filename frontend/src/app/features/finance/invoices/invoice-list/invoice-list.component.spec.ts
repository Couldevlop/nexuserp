import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InvoiceListComponent } from './invoice-list.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { HttpParams } from '@angular/common/http';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
  info: jasmine.createSpy('info'),
  warn: jasmine.createSpy('warn'),
};

describe('InvoiceListComponent', () => {
  let component: InvoiceListComponent;
  let fixture: ComponentFixture<InvoiceListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const mockPage = {
    data: [
      {
        id: 'inv-1',
        invoiceNumber: 'FA-2026-TEST-000001',
        customerName: 'Acme SARL',
        totalAmount: 1800,
        currency: 'EUR',
        status: 'SUBMITTED' as const,
        invoiceDate: '2026-01-15',
        dueDate: '2026-02-15',
      }
    ],
    meta: { page: 0, size: 20, total: 1, totalPages: 1 }
  };

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [InvoiceListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InvoiceListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as any;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading state initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load invoices on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith(
      '/api/v1/finance/invoices',
      jasmine.anything()
    );
    expect(component.invoices().length).toBe(1);
  });

  it('should set loading to false after load', () => {
    fixture.detectChanges();
    expect(component.isLoading()).toBeFalse();
  });

  it('should set totalItems correctly', () => {
    fixture.detectChanges();
    expect(component.totalItems()).toBe(1);
  });

  it('should handle API error — show error notification', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();

    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isLoading()).toBeFalse();
    expect(component.invoices().length).toBe(0);
  });

  it('should filter by status when onStatusChange called', () => {
    fixture.detectChanges();
    component.onStatusChange('SUBMITTED');

    expect(component.statusFilter()).toBe('SUBMITTED');
    expect(component.currentPage()).toBe(0);
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should search when onSearch called', () => {
    fixture.detectChanges();
    component.onSearch('Acme');

    expect(component.searchQuery()).toBe('Acme');
    expect(component.currentPage()).toBe(0);
  });

  it('should navigate to page when goToPage called', () => {
    fixture.detectChanges();
    component.totalPages.set(5);
    component.goToPage(3);

    expect(component.currentPage()).toBe(3);
  });

  it('should NOT navigate beyond totalPages', () => {
    fixture.detectChanges();
    component.totalPages.set(3);
    component.goToPage(10);

    expect(component.currentPage()).toBe(0); // unchanged
  });

  it('should NOT navigate to negative page', () => {
    fixture.detectChanges();
    component.goToPage(-1);

    expect(component.currentPage()).toBe(0);
  });

  it('should have all status options', () => {
    const values = component.statusOptions.map(o => o.value);
    expect(values).toContain('');
    expect(values).toContain('DRAFT');
    expect(values).toContain('SUBMITTED');
    expect(values).toContain('APPROVED');
    expect(values).toContain('PAID');
    expect(values).toContain('OVERDUE');
    expect(values).toContain('CANCELLED');
  });

  it('should return correct status badge class for each status', () => {
    const classMap = component.statusBadgeClass();
    expect(classMap['SUBMITTED']).toBe('nx-badge--warning');
    expect(classMap['PAID']).toBe('nx-badge--success');
    expect(classMap['OVERDUE']).toBe('nx-badge--error');
    expect(classMap['DRAFT']).toBe('nx-badge--neutral');
  });

  it('should display invoice number in table', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('FA-2026-TEST-000001');
  });

  it('should display customer name', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Acme SARL');
  });

  it('should getPages return correct length', () => {
    component.totalPages.set(5);
    expect(component.getPages().length).toBe(5);
  });
});
