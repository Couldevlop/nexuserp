import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfigListComponent } from './config-list.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfigParam } from '../config.model';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('ConfigListComponent', () => {
  let component: ConfigListComponent;
  let fixture: ComponentFixture<ConfigListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const secretParam: ConfigParam = {
    key: 'payment.orange.client-secret',
    category: 'PAYMENT',
    valueType: 'SECRET',
    secret: true,
    set: true,
    value: '••••••', // backend renvoie une valeur masquée
    description: null,
    updatedAt: '2026-01-01T00:00:00Z',
  };

  const stringParam: ConfigParam = {
    key: 'payment.orange.client-id',
    category: 'PAYMENT',
    valueType: 'STRING',
    secret: false,
    set: true,
    value: 'orange-client-123',
    description: null,
    updatedAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();

    const httpSpy = jasmine.createSpyObj('HttpClient', ['get', 'put', 'delete']);
    httpSpy.get.and.returnValue(of([secretParam, stringParam]));
    httpSpy.delete.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [ConfigListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfigListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load params on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/config', jasmine.anything());
    expect(component.isLoading()).toBeFalse();
  });

  it('should degrade gracefully on API error (catalogue still shown, no toast flood)', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('503')));
    fixture.detectChanges();
    expect(component.hasError()).toBeTrue();
    expect(component.isLoading()).toBeFalse();
    // Le catalogue PAYMENT contient des entrées => rows non vide.
    expect(component.rows().length).toBeGreaterThan(0);
    expect(mockNotif.error).not.toHaveBeenCalled();
  });

  it('should merge backend params with catalog (PAYMENT category default)', () => {
    fixture.detectChanges();
    const keys = component.rows().map((r) => r.key);
    expect(keys).toContain('payment.orange.client-secret');
    expect(keys).toContain('payment.orange.client-id');
    expect(component.activeCategory()).toBe('PAYMENT');
  });

  it('should NEVER display a real secret value (masked only)', () => {
    fixture.detectChanges();
    expect(component.displayValue(secretParam)).toBe(component.maskedPlaceholder);
    // Le DOM ne doit jamais contenir une vraie valeur secrète.
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain(component.maskedPlaceholder);
  });

  it('should show "—" for an unset secret', () => {
    const unset: ConfigParam = { ...secretParam, set: false, value: null };
    expect(component.displayValue(unset)).toBe('—');
  });

  it('should display a non-secret string value', () => {
    expect(component.displayValue(stringParam)).toBe('orange-client-123');
  });

  it('should switch category', () => {
    fixture.detectChanges();
    component.selectCategory('AI');
    expect(component.activeCategory()).toBe('AI');
    const keys = component.rows().map((r) => r.key);
    expect(keys).toContain('ai.anthropic.api-key');
  });

  it('should filter by search locally', () => {
    fixture.detectChanges();
    component.onSearch('client-id');
    const keys = component.filteredRows().map((r) => r.key);
    expect(keys).toContain('payment.orange.client-id');
    expect(keys).not.toContain('payment.orange.client-secret');
  });

  it('should call delete endpoint and remove the param', () => {
    fixture.detectChanges();
    const row = component.rows().find((r) => r.key === 'payment.orange.client-id')!;
    component.deleteParam(row);
    expect(httpClient.delete).toHaveBeenCalledWith(
      '/api/v1/config/payment.orange.client-id'
    );
    expect(mockNotif.success).toHaveBeenCalled();
  });

  it('should not delete an unset (catalog-only) param', () => {
    fixture.detectChanges();
    component.selectCategory('AI');
    const row = component.rows().find((r) => r.key === 'ai.anthropic.api-key')!;
    expect(row.param.set).toBeFalse();
    component.deleteParam(row);
    expect(httpClient.delete).not.toHaveBeenCalled();
  });

  it('should open and close the edit modal', () => {
    fixture.detectChanges();
    const row = component.rows()[0];
    component.openEdit(row);
    expect(component.editOpen()).toBeTrue();
    expect(component.editParam()).not.toBeNull();
    component.closeEdit();
    expect(component.editOpen()).toBeFalse();
    expect(component.editParam()).toBeNull();
  });

  it('should update local state on save', () => {
    fixture.detectChanges();
    const updated: ConfigParam = { ...stringParam, value: 'new-value' };
    component.onSaved(updated);
    const row = component.rows().find((r) => r.key === stringParam.key)!;
    expect(row.param.value).toBe('new-value');
    expect(component.editOpen()).toBeFalse();
  });
});
