import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LeaveRequestComponent } from './leave-request.component';
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

describe('LeaveRequestComponent', () => {
  let component: LeaveRequestComponent;
  let fixture: ComponentFixture<LeaveRequestComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;
  let router: Router;

  function fillValid(): void {
    component.updateField('employeeId', 'emp-1');
    component.updateField('startDate', '2026-07-01');
    component.updateField('endDate', '2026-07-10');
  }

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['post']);

    await TestBed.configureTestingModule({
      imports: [LeaveRequestComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LeaveRequestComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default to ANNUAL leave type', () => {
    expect(component.form().leaveType).toBe('ANNUAL');
  });

  it('should compute inclusive duration in days', () => {
    fillValid();
    expect(component.durationDays()).toBe(10);
  });

  it('should report zero duration for an inverted range', () => {
    component.updateField('startDate', '2026-07-10');
    component.updateField('endDate', '2026-07-01');
    expect(component.durationDays()).toBe(0);
    expect(component.datesValid()).toBeFalse();
  });

  it('should be invalid without employee id', () => {
    component.updateField('startDate', '2026-07-01');
    component.updateField('endDate', '2026-07-10');
    expect(component.isValid()).toBeFalse();
  });

  it('should be valid with employee and a valid range', () => {
    fillValid();
    expect(component.isValid()).toBeTrue();
  });

  it('should not submit and show error when invalid', () => {
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should POST to the leaves endpoint and navigate on success', () => {
    const navSpy = spyOn(router, 'navigate');
    httpClient.post.and.returnValue(of({ id: 'lv-new' }));
    fillValid();
    component.submit();

    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/hr/leaves', jasmine.any(Object));
    expect(mockNotif.success).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/hr/leaves']);
  });

  it('should send the backend contract fields in the payload', () => {
    httpClient.post.and.returnValue(of({ id: 'lv-new' }));
    fillValid();
    component.updateField('leaveType', 'SICK');
    component.submit();

    const body = httpClient.post.calls.mostRecent().args[1] as Record<string, unknown>;
    expect(body['employeeId']).toBe('emp-1');
    expect(body['leaveType']).toBe('SICK');
    expect(body['startDate']).toBe('2026-07-01');
    expect(body['endDate']).toBe('2026-07-10');
  });

  it('should show error toast on submission failure', () => {
    httpClient.post.and.returnValue(throwError(() => new Error('500')));
    fillValid();
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isSubmitting()).toBeFalse();
  });

  it('should expose all backend leave types', () => {
    const values = component.leaveTypes.map(t => t.value);
    expect(values).toEqual(['ANNUAL', 'SICK', 'MATERNITY', 'PATERNITY', 'RTT', 'UNPAID', 'OTHER']);
  });
});
