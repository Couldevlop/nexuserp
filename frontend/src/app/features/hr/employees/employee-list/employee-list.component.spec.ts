import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmployeeListComponent } from './employee-list.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
  info: jasmine.createSpy('info'),
  warning: jasmine.createSpy('warning'),
};

describe('EmployeeListComponent', () => {
  let component: EmployeeListComponent;
  let fixture: ComponentFixture<EmployeeListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  // Format Spring Data Page renvoyé par nexus-hr.
  const mockPage = {
    content: [
      {
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
      }
    ],
    totalPages: 1,
    totalElements: 1,
    number: 0,
  };

  beforeEach(async () => {
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [EmployeeListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading state initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load employees on init from the Spring page shape', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/hr/employees', jasmine.anything());
    expect(component.employees().length).toBe(1);
    expect(component.totalItems()).toBe(1);
    expect(component.totalPages()).toBe(1);
  });

  it('should set loading to false after load', () => {
    fixture.detectChanges();
    expect(component.isLoading()).toBeFalse();
  });

  it('should render rows in the table', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('EMP-0001');
    expect(compiled.textContent).toContain('Awa Koné');
  });

  it('should handle API error — show error toast and empty list', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isLoading()).toBeFalse();
    expect(component.employees().length).toBe(0);
  });

  it('should show empty state when there are no employees', () => {
    httpClient.get.and.returnValue(of({ content: [], totalPages: 0, totalElements: 0 }));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Aucun salarié');
  });

  it('should reload when filtering by department', () => {
    fixture.detectChanges();
    component.onDepartmentChange('Finance');
    expect(component.departmentFilter()).toBe('Finance');
    expect(component.currentPage()).toBe(0);
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should derive department options from loaded data', () => {
    fixture.detectChanges();
    expect(component.departments()).toContain('Finance');
  });

  it('should filter locally on search without extra requests', () => {
    fixture.detectChanges();
    component.onSearch('awa');
    expect(component.filteredEmployees().length).toBe(1);
    component.onSearch('inconnu');
    expect(component.filteredEmployees().length).toBe(0);
    expect(httpClient.get).toHaveBeenCalledTimes(1);
  });

  it('should search by employee number and job title too', () => {
    fixture.detectChanges();
    component.onSearch('EMP-0001');
    expect(component.filteredEmployees().length).toBe(1);
    component.onSearch('comptable');
    expect(component.filteredEmployees().length).toBe(1);
  });

  it('should navigate to a valid page', () => {
    fixture.detectChanges();
    component.totalPages.set(5);
    component.goToPage(3);
    expect(component.currentPage()).toBe(3);
  });

  it('should not navigate beyond totalPages', () => {
    fixture.detectChanges();
    component.totalPages.set(3);
    component.goToPage(10);
    expect(component.currentPage()).toBe(0);
  });

  it('should not navigate to negative page', () => {
    fixture.detectChanges();
    component.goToPage(-1);
    expect(component.currentPage()).toBe(0);
  });

  it('should map status to badge classes', () => {
    expect(component.statusBadgeClass['ACTIVE']).toBe('nx-badge--success');
    expect(component.statusBadgeClass['TERMINATED']).toBe('nx-badge--error');
  });

  it('should map contract type to badge classes', () => {
    expect(component.contractBadgeClass['CDI']).toBe('nx-badge--success');
    expect(component.contractBadgeClass['CDD']).toBe('nx-badge--info');
  });

  it('should format XOF salary without decimals', () => {
    const out = component.formatSalary(450000, 'XOF');
    expect(out).not.toContain(',00');
  });

  it('should render a dash for a null salary', () => {
    expect(component.formatSalary(null, 'EUR')).toBe('—');
  });

  it('should support the ApiPage fallback shape', () => {
    httpClient.get.and.returnValue(of({
      data: [{ ...mockPage.content[0], id: 'emp-2' }],
      meta: { page: 0, size: 20, total: 1, totalPages: 1 }
    }));
    fixture.detectChanges();
    expect(component.employees().length).toBe(1);
    expect(component.totalItems()).toBe(1);
  });

  it('getPages should return the correct length', () => {
    component.totalPages.set(4);
    expect(component.getPages().length).toBe(4);
  });
});
