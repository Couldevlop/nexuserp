import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmployeeCreateComponent } from './employee-create.component';
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

describe('EmployeeCreateComponent', () => {
  let component: EmployeeCreateComponent;
  let fixture: ComponentFixture<EmployeeCreateComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;
  let router: Router;

  function fillValid(): void {
    component.updateField('employeeNumber', 'EMP-0001');
    component.updateField('firstName', 'Awa');
    component.updateField('lastName', 'Koné');
    component.updateField('grossSalary', 450000);
  }

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['post']);

    await TestBed.configureTestingModule({
      imports: [EmployeeCreateComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeCreateComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default contract to CDI, country to CI and currency to XOF', () => {
    expect(component.form().contractType).toBe('CDI');
    expect(component.form().country).toBe('CI');
    expect(component.form().salaryCurrency).toBe('XOF');
  });

  it('should update a field', () => {
    component.updateField('firstName', 'Awa');
    expect(component.form().firstName).toBe('Awa');
  });

  it('should be invalid when required fields are missing', () => {
    expect(component.isValid()).toBeFalse();
  });

  it('should be invalid when salary is zero', () => {
    component.updateField('employeeNumber', 'EMP-0001');
    component.updateField('firstName', 'Awa');
    component.updateField('lastName', 'Koné');
    expect(component.isValid()).toBeFalse();
  });

  it('should be valid with all required fields', () => {
    fillValid();
    expect(component.isValid()).toBeTrue();
  });

  it('should reject an invalid email format', () => {
    fillValid();
    component.updateField('email', 'not-an-email');
    expect(component.emailValid()).toBeFalse();
    expect(component.isValid()).toBeFalse();
  });

  it('should accept an empty (optional) email', () => {
    fillValid();
    component.updateField('email', '');
    expect(component.emailValid()).toBeTrue();
  });

  it('should not submit and show error when invalid', () => {
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should POST to the hr endpoint and navigate on success', () => {
    const navSpy = spyOn(router, 'navigate');
    httpClient.post.and.returnValue(of({ id: 'emp-new' }));
    fillValid();
    component.submit();

    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/hr/employees', jasmine.any(Object));
    expect(mockNotif.success).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/hr/employees', 'emp-new']);
  });

  it('should send the backend contract fields in the payload', () => {
    httpClient.post.and.returnValue(of({ id: 'emp-new' }));
    fillValid();
    component.submit();

    const body = httpClient.post.calls.mostRecent().args[1] as Record<string, unknown>;
    expect(body['employeeNumber']).toBe('EMP-0001');
    expect(body['firstName']).toBe('Awa');
    expect(body['lastName']).toBe('Koné');
    expect(body['contractType']).toBe('CDI');
    expect(body['grossSalary']).toBe(450000);
    expect(body['salaryCurrency']).toBe('XOF');
    expect(body['country']).toBe('CI');
    expect(body['hireDate']).toBeDefined();
  });

  it('should navigate to list when response has no id', () => {
    const navSpy = spyOn(router, 'navigate');
    httpClient.post.and.returnValue(of({}));
    fillValid();
    component.submit();
    expect(navSpy).toHaveBeenCalledWith(['/hr/employees']);
  });

  it('should show error toast on submission failure', () => {
    httpClient.post.and.returnValue(throwError(() => new Error('500')));
    fillValid();
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isSubmitting()).toBeFalse();
  });

  it('should expose all backend contract types', () => {
    const values = component.contractTypes.map(c => c.value);
    expect(values).toEqual(['CDI', 'CDD', 'INTERIM', 'INTERNSHIP', 'FREELANCE']);
  });
});
