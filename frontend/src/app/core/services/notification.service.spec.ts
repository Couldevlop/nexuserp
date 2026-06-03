import { TestBed } from '@angular/core/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start with empty toasts', () => {
    expect(service.toasts().length).toBe(0);
  });

  it('should add a success toast', () => {
    service.success('Operation successful');
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].type).toBe('success');
    expect(service.toasts()[0].message).toBe('Operation successful');
  });

  it('should add an error toast', () => {
    service.error('Something went wrong');
    const toast = service.toasts()[0];
    expect(toast.type).toBe('error');
    expect(toast.message).toBe('Something went wrong');
  });

  it('should add an info toast', () => {
    service.info('Information message');
    expect(service.toasts()[0].type).toBe('info');
  });

  it('should add a warning toast', () => {
    service.warn('Warning message');
    expect(service.toasts()[0].type).toBe('warning');
  });

  it('should assign a unique ID to each toast', () => {
    service.success('Toast 1');
    service.success('Toast 2');
    const ids = service.toasts().map(t => t.id);
    expect(new Set(ids).size).toBe(2);
  });

  it('should dismiss a toast by ID', () => {
    service.success('Toast to dismiss');
    const id = service.toasts()[0].id;
    service.dismiss(id);
    expect(service.toasts().length).toBe(0);
  });

  it('should keep other toasts when dismissing one', () => {
    service.success('Toast 1');
    service.error('Toast 2');
    const id = service.toasts()[0].id;
    service.dismiss(id);
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].type).toBe('error');
  });

  it('should include a timestamp on each toast', () => {
    const before = Date.now();
    service.info('With timestamp');
    const after = Date.now();
    const ts = service.toasts()[0].timestamp;
    expect(ts).toBeGreaterThanOrEqualTo(before);
    expect(ts).toBeLessThanOrEqualTo(after);
  });

  it('should clear all toasts', () => {
    service.success('T1');
    service.error('T2');
    service.info('T3');
    service.clearAll();
    expect(service.toasts().length).toBe(0);
  });
});
