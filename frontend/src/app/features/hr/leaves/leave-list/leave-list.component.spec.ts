import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LeaveListComponent } from './leave-list.component';
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

describe('LeaveListComponent', () => {
  let component: LeaveListComponent;
  let fixture: ComponentFixture<LeaveListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const mockPage = {
    content: [
      {
        id: 'lv-1',
        employeeId: 'emp-1',
        employeeName: 'Awa Koné',
        leaveType: 'ANNUAL' as const,
        status: 'SUBMITTED' as const,
        startDate: '2026-07-01',
        endDate: '2026-07-10',
        durationDays: 10,
        reason: 'Vacances',
        approvedBy: null,
        rejectedBy: null,
        rejectionReason: null,
      }
    ],
    totalPages: 1,
    totalElements: 1,
    number: 0,
  };

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    mockAuth.hasAnyRole.and.returnValue(true);

    const httpSpy = jasmine.createSpyObj('HttpClient', ['get', 'put']);
    httpSpy.get.and.returnValue(of(mockPage));
    httpSpy.put.and.returnValue(of({}));

    await TestBed.configureTestingModule({
      imports: [LeaveListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: AuthStateService, useValue: mockAuth },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LeaveListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading state initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load leaves on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/hr/leaves', jasmine.anything());
    expect(component.leaves().length).toBe(1);
    expect(component.totalItems()).toBe(1);
  });

  it('should render rows', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Awa Koné');
    expect(compiled.textContent).toContain('Congés payés');
  });

  it('should degrade gracefully to a not-available state on API error (no fake data, no error toast)', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    expect(component.notAvailable()).toBeTrue();
    expect(component.leaves().length).toBe(0);
    expect(component.isLoading()).toBeFalse();
    expect(mockNotif.error).not.toHaveBeenCalled();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('indisponible');
  });

  it('should show empty state when there are no leaves', () => {
    httpClient.get.and.returnValue(of({ content: [], totalPages: 0, totalElements: 0 }));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Aucune demande');
  });

  it('should reload when filtering by status', () => {
    fixture.detectChanges();
    component.onStatusChange('APPROVED');
    expect(component.statusFilter()).toBe('APPROVED');
    expect(component.currentPage()).toBe(0);
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should allow management for HR managers', () => {
    fixture.detectChanges();
    expect(component.canManage()).toBeTrue();
  });

  it('should not allow management without role', () => {
    mockAuth.hasAnyRole.and.returnValue(false);
    fixture.detectChanges();
    expect(component.canManage()).toBeFalse();
  });

  it('should call the approve endpoint and refresh', () => {
    fixture.detectChanges();
    component.approve(mockPage.content[0]);
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/hr/leaves/lv-1/approve',
      jasmine.objectContaining({ approvedBy: 'hr@acme.ci' })
    );
    expect(mockNotif.success).toHaveBeenCalled();
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should call the reject endpoint with a reason', () => {
    fixture.detectChanges();
    component.reject(mockPage.content[0]);
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/hr/leaves/lv-1/reject',
      jasmine.objectContaining({ rejectedBy: 'hr@acme.ci', reason: jasmine.any(String) })
    );
    expect(mockNotif.success).toHaveBeenCalled();
  });

  it('should not approve when not authorized', () => {
    mockAuth.hasAnyRole.and.returnValue(false);
    fixture.detectChanges();
    component.approve(mockPage.content[0]);
    expect(httpClient.put).not.toHaveBeenCalled();
  });

  it('should toast an error when approval fails', () => {
    fixture.detectChanges();
    httpClient.put.and.returnValue(throwError(() => new Error('500')));
    component.approve(mockPage.content[0]);
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.actionInProgress()).toBeNull();
  });

  it('should map statuses to badge classes', () => {
    expect(component.statusBadgeClass['SUBMITTED']).toBe('nx-badge--warning');
    expect(component.statusBadgeClass['APPROVED']).toBe('nx-badge--success');
    expect(component.statusBadgeClass['REJECTED']).toBe('nx-badge--error');
  });

  it('should expose backend leave statuses in filter', () => {
    const values = component.statusOptions.map(o => o.value);
    expect(values).toContain('SUBMITTED');
    expect(values).toContain('APPROVED');
    expect(values).toContain('REJECTED');
    expect(values).toContain('CANCELLED');
  });
});
