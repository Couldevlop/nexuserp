import { TestBed } from '@angular/core/testing';
import { NativeNetworkService } from './native-network.service';
import { PlatformService } from './platform.service';

describe('NativeNetworkService (web fallback)', () => {
  let service: NativeNetworkService;
  let platformSpy: jasmine.SpyObj<Pick<PlatformService, 'isNative'>>;

  beforeEach(() => {
    platformSpy = jasmine.createSpyObj<Pick<PlatformService, 'isNative'>>('PlatformService', ['isNative']);

    TestBed.configureTestingModule({
      providers: [
        NativeNetworkService,
        { provide: PlatformService, useValue: platformSpy },
      ],
    });
    service = TestBed.inject(NativeNetworkService);
  });

  it('should be created with default online=true', () => {
    expect(service).toBeTruthy();
    expect(service.isOnline()).toBe(true);
  });

  it('init() should be a no-op on web and not throw', async () => {
    platformSpy.isNative.and.returnValue(false);
    await expectAsync(service.init()).toBeResolved();
    expect(service.isOnline()).toBe(true);
  });

  it('init() should still register the bridge callback on web without invoking it', async () => {
    platformSpy.isNative.and.returnValue(false);
    const bridge = jasmine.createSpy('bridge');
    await service.init(bridge);
    expect(bridge).not.toHaveBeenCalled();
  });

  it('ngOnDestroy should not throw when nothing was initialised', () => {
    expect(() => service.ngOnDestroy()).not.toThrow();
  });
});
