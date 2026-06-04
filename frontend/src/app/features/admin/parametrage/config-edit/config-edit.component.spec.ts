import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfigEditComponent } from './config-edit.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfigParam, ConfigUpsertRequest } from '../config.model';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('ConfigEditComponent', () => {
  let component: ConfigEditComponent;
  let fixture: ComponentFixture<ConfigEditComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const secretParam: ConfigParam = {
    key: 'payment.orange.client-secret',
    category: 'PAYMENT',
    valueType: 'SECRET',
    secret: true,
    set: true,
    value: '••••••',
    description: null,
    updatedAt: null,
  };

  const stringParam: ConfigParam = {
    key: 'general.company.name',
    category: 'GENERAL',
    valueType: 'STRING',
    secret: false,
    set: true,
    value: 'Acme SARL',
    description: null,
    updatedAt: null,
  };

  const jsonParam: ConfigParam = {
    key: 'general.meta',
    category: 'GENERAL',
    valueType: 'JSON',
    secret: false,
    set: false,
    value: null,
    description: null,
    updatedAt: null,
  };

  function lastPutPayload(): ConfigUpsertRequest {
    return httpClient.put.calls.mostRecent().args[1] as ConfigUpsertRequest;
  }

  beforeEach(async () => {
    mockNotif.success.calls.reset();
    mockNotif.error.calls.reset();

    const httpSpy = jasmine.createSpyObj('HttpClient', ['put']);
    httpSpy.put.and.callFake((_url: string, body: ConfigUpsertRequest) =>
      of({ ...stringParam, ...body })
    );

    await TestBed.configureTestingModule({
      imports: [ConfigEditComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfigEditComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    component.param = stringParam;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should NOT pre-fill a secret value into the form', () => {
    component.param = secretParam;
    expect(component.formValue()).toBe('');
  });

  it('should pre-fill a non-secret string value', () => {
    component.param = stringParam;
    expect(component.formValue()).toBe('Acme SARL');
  });

  it('secret left empty => PUT keeps existing (empty value sent)', () => {
    component.param = secretParam;
    component.formValue.set(''); // admin n'a rien saisi
    component.onSave();
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/config/payment.orange.client-secret',
      jasmine.anything()
    );
    expect(lastPutPayload().value).toBe('');
    expect(lastPutPayload().secret).toBeTrue();
  });

  it('secret with a new value => sent in PUT payload', () => {
    component.param = secretParam;
    component.formValue.set('new-secret-key');
    component.onSave();
    expect(lastPutPayload().value).toBe('new-secret-key');
  });

  it('non-secret required field empty => no PUT', () => {
    component.param = stringParam;
    component.formValue.set('   ');
    component.onSave();
    expect(httpClient.put).not.toHaveBeenCalled();
    expect(component.submitted()).toBeTrue();
  });

  it('valid string => PUT with correct payload + success toast', () => {
    component.param = stringParam;
    component.formValue.set('Nouvelle Société');
    component.onSave();
    const payload = lastPutPayload();
    expect(payload.value).toBe('Nouvelle Société');
    expect(payload.type).toBe('STRING');
    expect(payload.category).toBe('GENERAL');
    expect(mockNotif.success).toHaveBeenCalled();
  });

  it('invalid JSON => no PUT, json error set', () => {
    component.param = jsonParam;
    component.formValue.set('{ not valid');
    component.onSave();
    expect(httpClient.put).not.toHaveBeenCalled();
    expect(component.jsonError()).toBeTruthy();
  });

  it('valid JSON => PUT', () => {
    component.param = jsonParam;
    component.formValue.set('{"a":1}');
    component.onSave();
    expect(httpClient.put).toHaveBeenCalled();
    expect(component.jsonError()).toBeNull();
  });

  it('boolean toggle => PUT with "true"/"false"', () => {
    const boolParam: ConfigParam = {
      key: 'ai.enabled',
      category: 'AI',
      valueType: 'BOOLEAN',
      secret: false,
      set: false,
      value: null,
      description: null,
      updatedAt: null,
    };
    component.param = boolParam;
    component.onBoolToggle(true);
    component.onSave();
    expect(lastPutPayload().value).toBe('true');
  });

  it('should emit saved on success', () => {
    const spy = jasmine.createSpy('saved');
    component.saved.subscribe(spy);
    component.param = stringParam;
    component.formValue.set('X');
    component.onSave();
    expect(spy).toHaveBeenCalled();
  });

  it('should show error toast on PUT failure', () => {
    httpClient.put.and.returnValue(throwError(() => new Error('500')));
    component.param = stringParam;
    component.formValue.set('X');
    component.onSave();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.isSaving()).toBeFalse();
  });

  it('should emit closed on cancel', () => {
    const spy = jasmine.createSpy('closed');
    component.closed.subscribe(spy);
    component.onClose();
    expect(spy).toHaveBeenCalled();
  });
});
