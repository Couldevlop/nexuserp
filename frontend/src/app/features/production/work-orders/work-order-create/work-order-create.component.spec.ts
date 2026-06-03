import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WorkOrderCreateComponent } from './work-order-create.component';
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

describe('WorkOrderCreateComponent', () => {
  let component: WorkOrderCreateComponent;
  let fixture: ComponentFixture<WorkOrderCreateComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;
  let router: Router;

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['post']);

    await TestBed.configureTestingModule({
      imports: [WorkOrderCreateComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkOrderCreateComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default priority to NORMAL and quantity to 1', () => {
    expect(component.form.controls.priority.value).toBe('NORMAL');
    expect(component.form.controls.quantityPlanned.value).toBe(1);
  });

  it('should expose all backend priorities', () => {
    const values = component.priorities.map((p) => p.value);
    expect(values).toContain('LOW');
    expect(values).toContain('NORMAL');
    expect(values).toContain('HIGH');
    expect(values).toContain('URGENT');
  });

  it('should be invalid when product name is blank', () => {
    component.form.controls.productName.setValue('');
    expect(component.form.invalid).toBeTrue();
  });

  it('should be invalid when quantity is zero or negative', () => {
    component.form.controls.productName.setValue('Ciment');
    component.form.controls.quantityPlanned.setValue(0);
    expect(component.form.invalid).toBeTrue();
  });

  it('should be valid with product name and positive quantity', () => {
    component.form.controls.productName.setValue('Ciment');
    component.form.controls.quantityPlanned.setValue(50);
    expect(component.form.valid).toBeTrue();
  });

  it('should not submit and show error when invalid', () => {
    component.form.controls.productName.setValue('');
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should flag an invalid date range and block submit', () => {
    component.form.controls.productName.setValue('Ciment');
    component.form.controls.quantityPlanned.setValue(10);
    component.form.controls.plannedStartDate.setValue('2026-02-10');
    component.form.controls.plannedEndDate.setValue('2026-02-01');
    component.submit();
    expect(component.dateRangeInvalid()).toBeTrue();
    expect(httpClient.post).not.toHaveBeenCalled();
  });

  it('should POST to the production endpoint and navigate on success', () => {
    const navSpy = spyOn(router, 'navigate');
    httpClient.post.and.returnValue(of({ id: 'wo-new' }));

    component.form.controls.productName.setValue('Ciment 50kg');
    component.form.controls.quantityPlanned.setValue(100);
    component.submit();

    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/production/work-orders', jasmine.any(Object));
    expect(mockNotif.success).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/production/work-orders', 'wo-new']);
  });

  it('should send only backend-supported fields', () => {
    httpClient.post.and.returnValue(of({ id: 'wo-new' }));
    component.form.controls.productName.setValue('Ciment 50kg');
    component.form.controls.quantityPlanned.setValue(100);
    component.form.controls.priority.setValue('HIGH');
    component.submit();

    const body = httpClient.post.calls.mostRecent().args[1] as Record<string, unknown>;
    expect(body['productName']).toBe('Ciment 50kg');
    expect(body['quantityPlanned']).toBe(100);
    expect(body['priority']).toBe('HIGH');
    // Optional empty fields are sent as null, never empty strings.
    expect(body['workcenter']).toBeNull();
    expect(body['bomId']).toBeNull();
  });

  it('should navigate to list when response has no id', () => {
    const navSpy = spyOn(router, 'navigate');
    httpClient.post.and.returnValue(of({}));
    component.form.controls.productName.setValue('Ciment');
    component.form.controls.quantityPlanned.setValue(5);
    component.submit();
    expect(navSpy).toHaveBeenCalledWith(['/production/work-orders']);
  });

  it('should show error toast on submission failure', () => {
    httpClient.post.and.returnValue(throwError(() => new Error('500')));
    component.form.controls.productName.setValue('Ciment');
    component.form.controls.quantityPlanned.setValue(5);
    component.submit();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isSubmitting()).toBeFalse();
  });

  it('controlInvalid should reflect touched invalid controls', () => {
    component.form.controls.productName.setValue('');
    component.form.controls.productName.markAsTouched();
    expect(component.controlInvalid('productName')).toBeTrue();
  });
});
