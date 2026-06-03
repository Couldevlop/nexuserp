import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthStateService } from '../../core/services/auth-state.service';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';

const mockAuthState = {
  fullName: signal('Jean Dupont'),
  tenantId: signal('fr-acme'),
  isAuthenticated: signal(true),
  roles: signal(['FINANCE_USER']),
  hasAnyRole: (...roles: string[]) => true,
};

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of({ data: [], meta: { page: 0, size: 5, total: 0, totalPages: 0 } }));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthStateService, useValue: mockAuthState },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as any;
  });

  it('should create the dashboard component', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading state initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should have 4 KPI cards', () => {
    expect(component.kpis().length).toBe(4);
  });

  it('should load invoices on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith(
      jasmine.stringContaining('/api/v1/finance/invoices'),
      jasmine.anything()
    );
  });

  it('should set isLoading to false after data loaded', () => {
    fixture.detectChanges();
    expect(component.isLoading()).toBeFalse();
  });

  it('should set recentInvoices from API response', () => {
    const mockInvoices = [
      { id: 'inv-1', invoiceNumber: 'FA-001', customerName: 'Test', totalAmount: 1000, currency: 'EUR', status: 'SUBMITTED', dueDate: '2026-02-15' }
    ];
    httpClient.get.and.returnValue(of({ data: mockInvoices }));

    fixture.detectChanges();
    expect(component.recentInvoices().length).toBe(1);
  });

  it('should handle API error gracefully', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('API error')));
    fixture.detectChanges();

    expect(component.isLoading()).toBeFalse();
    expect(component.recentInvoices().length).toBe(0);
  });

  it('should display user name from auth state', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Jean Dupont');
  });

  it('should display tenant ID', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('fr-acme');
  });

  it('should have stable KPI icons', () => {
    const kpis = component.kpis();
    expect(kpis[0].icon).toBe('💰');
    expect(kpis[1].icon).toBe('📄');
    expect(kpis[2].icon).toBe('📦');
    expect(kpis[3].icon).toBe('👥');
  });
});
