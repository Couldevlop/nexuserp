import { TestBed } from '@angular/core/testing';
import { PlatformService } from './platform.service';

describe('PlatformService', () => {
  afterEach(() => {
    delete (window as unknown as { Capacitor?: unknown }).Capacitor;
  });

  function create(): PlatformService {
    TestBed.configureTestingModule({});
    return TestBed.inject(PlatformService);
  }

  it('should default to web / non-native when Capacitor is absent', () => {
    const svc = create();
    expect(svc.isNative()).toBe(false);
    expect(svc.platform()).toBe('web');
    expect(svc.isAndroid).toBe(false);
  });

  it('should report native android when Capacitor runtime is present', () => {
    (window as unknown as { Capacitor?: unknown }).Capacitor = {
      isNativePlatform: () => true,
      getPlatform: () => 'android',
    };
    const svc = create();
    expect(svc.isNative()).toBe(true);
    expect(svc.platform()).toBe('android');
    expect(svc.isAndroid).toBe(true);
  });
});
