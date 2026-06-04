import { TestBed } from '@angular/core/testing';
import { BarcodeScannerService } from './barcode-scanner.service';
import { PlatformService } from './platform.service';

describe('BarcodeScannerService (web fallback)', () => {
  let service: BarcodeScannerService;
  let platformSpy: jasmine.SpyObj<Pick<PlatformService, 'isNative'>>;

  beforeEach(() => {
    platformSpy = jasmine.createSpyObj<Pick<PlatformService, 'isNative'>>('PlatformService', ['isNative']);

    TestBed.configureTestingModule({
      providers: [
        BarcodeScannerService,
        { provide: PlatformService, useValue: platformSpy },
      ],
    });
    service = TestBed.inject(BarcodeScannerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('isAvailable should be false on web', () => {
    platformSpy.isNative.and.returnValue(false);
    expect(service.isAvailable).toBe(false);
  });

  it('scan() should resolve to null on web (no native runtime)', async () => {
    platformSpy.isNative.and.returnValue(false);
    await expectAsync(service.scan()).toBeResolvedTo(null);
  });
});
