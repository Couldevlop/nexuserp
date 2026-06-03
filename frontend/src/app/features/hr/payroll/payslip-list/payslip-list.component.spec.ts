import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PayslipListComponent } from './payslip-list.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
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

describe('PayslipListComponent', () => {
  let component: PayslipListComponent;
  let fixture: ComponentFixture<PayslipListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const mockPage = {
    content: [
      {
        id: 'emp-1',
        employeeNumber: 'EMP-0001',
        fullName: 'Awa Koné',
        jobTitle: 'Comptable',
        department: 'Finance',
        contractType: 'CDI',
        status: 'ACTIVE',
        grossSalaryAmount: 450000,
        grossSalaryCurrency: 'XOF',
      },
      {
        id: 'emp-2',
        employeeNumber: 'EMP-0002',
        fullName: 'Yao Ex',
        jobTitle: 'Ancien',
        department: 'Finance',
        contractType: 'CDD',
        status: 'TERMINATED',
        grossSalaryAmount: 300000,
        grossSalaryCurrency: 'XOF',
      }
    ],
    totalElements: 2,
  };

  const mockPayslip = {
    grossSalary: { amount: 450000, currency: 'XOF' },
    netSalary: { amount: 420000, currency: 'XOF' },
    totalEmployeeDeductions: { amount: 30000, currency: 'XOF' },
    totalEmployerCost: { amount: 520000, currency: 'XOF' },
    country: 'CI',
  };

  beforeEach(async () => {
    mockNotif.error.calls.reset();
    mockAuth.hasAnyRole.and.returnValue(true);

    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [PayslipListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: AuthStateService, useValue: mockAuth },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PayslipListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load active employees only (exclude terminated)', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/hr/employees', jasmine.anything());
    expect(component.rows().length).toBe(1);
    expect(component.rows()[0].employee.id).toBe('emp-1');
  });

  it('should not render net salary numbers before computing (no fake data)', () => {
    fixture.detectChanges();
    expect(component.rows()[0].payslip).toBeNull();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('—');
  });

  it('should compute a payslip on demand', () => {
    fixture.detectChanges();
    httpClient.get.and.returnValue(of(mockPayslip));
    component.compute(component.rows()[0]);
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/hr/employees/emp-1/payslip');
    expect(component.rows()[0].payslip?.netSalary.amount).toBe(420000);
    expect(component.rows()[0].computing).toBeFalse();
  });

  it('should toast error and clear computing flag on payslip failure', () => {
    fixture.detectChanges();
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    component.compute(component.rows()[0]);
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.rows()[0].computing).toBeFalse();
  });

  it('should degrade gracefully to not-available on employees load error', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();
    expect(component.notAvailable()).toBeTrue();
    expect(component.rows().length).toBe(0);
    expect(component.isLoading()).toBeFalse();
  });

  it('should show empty state when no active employees', () => {
    httpClient.get.and.returnValue(of({ content: [], totalElements: 0 }));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Aucun salarié');
  });

  it('should restrict access without manager role', () => {
    mockAuth.hasAnyRole.and.returnValue(false);
    fixture.detectChanges();
    expect(component.canViewPayroll()).toBeFalse();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Accès restreint');
  });

  it('should not compute when not authorized', () => {
    fixture.detectChanges();
    const row = component.rows()[0];
    mockAuth.hasAnyRole.and.returnValue(false);
    const callsBefore = httpClient.get.calls.count();
    component.compute(row);
    expect(httpClient.get.calls.count()).toBe(callsBefore);
  });

  it('should format Money DTOs', () => {
    expect(component.formatMoneyDto({ amount: 1000, currency: 'XOF' })).not.toBe('—');
    expect(component.formatMoneyDto(null)).toBe('—');
  });
});
