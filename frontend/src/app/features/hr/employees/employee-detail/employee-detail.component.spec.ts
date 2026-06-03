import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmployeeDetailComponent } from './employee-detail.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

const mockAuth = {
  hasAnyRole: jasmine.createSpy('hasAnyRole').and.returnValue(true),
  user: () => ({ email: 'hr@acme.ci' }),
};

const mockRoute = {
  snapshot: { paramMap: { get: (_: string) => 'emp-1' } },
};

const mockEmployee = {
  id: 'emp-1',
  employeeNumber: 'EMP-0001',
  firstName: 'Awa',
  lastName: 'Koné',
  fullName: 'Awa Koné',
  email: 'awa.kone@acme.ci',
  phone: '+225 0102030405',
  department: 'Finance',
  jobTitle: 'Comptable',
  contractType: 'CDI' as const,
  status: 'ACTIVE' as const,
  hireDate: '2024-03-01',
  grossSalaryAmount: 450000,
  grossSalaryCurrency: 'XOF',
  country: 'CI',
};

const mockPayslip = {
  grossSalary: { amount: 450000, currency: 'XOF' },
  netSalary: { amount: 420000, currency: 'XOF' },
  totalEmployeeDeductions: { amount: 30000, currency: 'XOF' },
  totalEmployerCost: { amount: 520000, currency: 'XOF' },
  country: 'CI',
};

describe('EmployeeDetailComponent', () => {
  let component: EmployeeDetailComponent;
  let fixture: ComponentFixture<EmployeeDetailComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    mockAuth.hasAnyRole.and.returnValue(true);

    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockEmployee));

    await TestBed.configureTestingModule({
      imports: [EmployeeDetailComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: AuthStateService, useValue: mockAuth },
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeDetailComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the employee on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/hr/employees/emp-1');
    expect(component.employee()?.fullName).toBe('Awa Koné');
    expect(component.isLoading()).toBeFalse();
  });

  it('should render identity and contract', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Awa Koné');
    expect(compiled.textContent).toContain('EMP-0001');
    expect(compiled.textContent).toContain('Comptable');
  });

  it('should redirect and toast when the employee is not found', () => {
    const router = TestBed.inject(Router);
    const navSpy = spyOn(router, 'navigate');
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/hr/employees']);
  });

  it('should allow payroll view for managers', () => {
    fixture.detectChanges();
    expect(component.canViewPayroll()).toBeTrue();
  });

  it('should not allow payroll view without manager role', () => {
    mockAuth.hasAnyRole.and.returnValue(false);
    fixture.detectChanges();
    expect(component.canViewPayroll()).toBeFalse();
  });

  it('should call the payslip endpoint and store the result', () => {
    fixture.detectChanges();
    httpClient.get.and.returnValue(of(mockPayslip));
    component.computePayslip();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/hr/employees/emp-1/payslip');
    expect(component.payslip()?.netSalary.amount).toBe(420000);
    expect(mockNotif.success).toHaveBeenCalled();
    expect(component.actionInProgress()).toBeNull();
  });

  it('should not call payslip when not authorized', () => {
    mockAuth.hasAnyRole.and.returnValue(false);
    fixture.detectChanges();
    const initialCalls = httpClient.get.calls.count();
    component.computePayslip();
    expect(httpClient.get.calls.count()).toBe(initialCalls);
  });

  it('should toast an error when payslip fails', () => {
    fixture.detectChanges();
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    component.computePayslip();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.actionInProgress()).toBeNull();
  });

  it('should map statuses to classes', () => {
    expect(component.getStatusClass('ACTIVE')).toBe('nx-badge--success');
    expect(component.getStatusClass('TERMINATED')).toBe('nx-badge--error');
    expect(component.getStatusClass('UNKNOWN')).toBe('nx-badge--neutral');
  });

  it('should format Money DTO amounts', () => {
    expect(component.formatMoneyDto({ amount: 450000, currency: 'XOF' })).not.toBe('—');
    expect(component.formatMoneyDto(null)).toBe('—');
  });

  it('should support the wrapped { data } response shape', () => {
    httpClient.get.and.returnValue(of({ data: { ...mockEmployee, id: 'emp-2' } }));
    fixture.detectChanges();
    expect(component.employee()?.id).toBe('emp-2');
  });
});
