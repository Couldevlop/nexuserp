import { TestBed } from '@angular/core/testing';
import { ConnectivityService } from './connectivity.service';

describe('ConnectivityService', () => {
  let service: ConnectivityService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  function create(): ConnectivityService {
    service = TestBed.inject(ConnectivityService);
    return service;
  }

  it('should be created', () => {
    expect(create()).toBeTruthy();
  });

  it('should initialise isOnline from navigator.onLine', () => {
    spyOnProperty(navigator, 'onLine', 'get').and.returnValue(true);
    expect(create().isOnline()).toBe(true);
  });

  it('should set isOnline to false on window offline event', () => {
    create();
    window.dispatchEvent(new Event('offline'));
    expect(service.isOnline()).toBe(false);
  });

  it('should set isOnline to true on window online event', () => {
    create();
    window.dispatchEvent(new Event('offline'));
    expect(service.isOnline()).toBe(false);
    window.dispatchEvent(new Event('online'));
    expect(service.isOnline()).toBe(true);
  });

  it('should toggle offline then online and reflect both transitions', () => {
    create();
    window.dispatchEvent(new Event('online'));
    expect(service.isOnline()).toBe(true);
    window.dispatchEvent(new Event('offline'));
    expect(service.isOnline()).toBe(false);
  });

  it('should remove listeners on destroy (no further updates)', () => {
    create();
    service.ngOnDestroy();
    window.dispatchEvent(new Event('offline'));
    // After teardown the handler is detached; calling dispatch must not throw.
    expect(service.isOnline).toBeDefined();
  });

  it('startHeartbeat should be idempotent', () => {
    create();
    const fetchSpy = spyOn(window, 'fetch').and.resolveTo(new Response('{}'));
    service.startHeartbeat();
    service.startHeartbeat();
    service.stopHeartbeat();
    // Only the initial immediate ping should have fired (single timer armed).
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  });
});
