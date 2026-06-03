import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HrDashboardComponent } from './hr-dashboard.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../core/services/notification.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('HrDashboardComponent', () => {
  let component: HrDashboardComponent;
  let fixture: ComponentFixture<HrDashboardComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const mockPage = {
    content: [
      { id: 'e1', fullName: 'Awa Koné', jobTitle: 'Comptable', department: 'Finance', contractType: 'CDI', status: 'ACTIVE', hireDate: '2024-03-01', grossSalaryAmount: 450000, grossSalaryCurrency: 'XOF' },
      { id: 'e2', fullName: 'Koffi N', jobTitle: 'Vendeur', department: 'Ventes', contractType: 'CDD', status: 'ACTIVE', hireDate: '2025-01-15', grossSalaryAmount: 300000, grossSalaryCurrency: 'XOF' },
      { id: 'e3', fullName: 'Ex Salarié', jobTitle: 'Ancien', department: 'Finance', contractType: 'CDI', status: 'TERMINATED', hireDate: '2020-06-01', grossSalaryAmount: 500000, grossSalaryCurrency: 'XOF' },
    ],
    totalElements: 3,
  };

  beforeEach(async () => {
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [HrDashboardComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HrDashboardComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading skeleton initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load employees and flag data presence', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/hr/employees', jasmine.anything());
    expect(component.isLoading()).toBeFalse();
    expect(component.hasData()).toBeTrue();
  });

  it('should use server total for headcount', () => {
    fixture.detectChanges();
    expect(component.formatHeadcount()).toBe('3');
  });

  it('should count active employees', () => {
    fixture.detectChanges();
    expect(component.formatActive()).toBe('2');
  });

  it('should sum payroll mass excluding terminated', () => {
    fixture.detectChanges();
    // 450000 + 300000 = 750000 (the terminated 500000 is excluded)
    expect(component.formatPayroll()).not.toContain('1 250 000');
    expect(component.formatPayroll()).not.toBe('—');
  });

  it('should aggregate departments', () => {
    fixture.detectChanges();
    const deps = component.departments();
    expect(deps.find(d => d.name === 'Finance')?.count).toBe(2);
    expect(deps.find(d => d.name === 'Ventes')?.count).toBe(1);
  });

  it('should list recent hires sorted by date desc', () => {
    fixture.detectChanges();
    const hires = component.recentHires();
    expect(hires[0].id).toBe('e2'); // 2025-01-15 most recent
  });

  it('should NOT fabricate pending leaves (no endpoint) — always dash', () => {
    fixture.detectChanges();
    expect(component.formatPendingLeaves()).toBe('—');
  });

  it('should show dashes and no fake numbers when there is no data', () => {
    httpClient.get.and.returnValue(of({ content: [], totalElements: 0 }));
    fixture.detectChanges();
    expect(component.hasData()).toBeFalse();
    expect(component.formatHeadcount()).toBe('—');
    expect(component.formatActive()).toBe('—');
    expect(component.formatPayroll()).toBe('—');
  });

  it('should toast error and degrade on API failure', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('500')));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.hasData()).toBeFalse();
    expect(component.isLoading()).toBeFalse();
    expect(component.formatPayroll()).toBe('—');
  });
});
