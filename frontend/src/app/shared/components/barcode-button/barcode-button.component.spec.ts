import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BarcodeButtonComponent } from './barcode-button.component';
import { BarcodeScannerService, BarcodeUnavailableError } from '../../../core/native/barcode-scanner.service';
import { PlatformService } from '../../../core/native/platform.service';
import { NotificationService } from '../../../core/services/notification.service';

describe('BarcodeButtonComponent', () => {
  let component: BarcodeButtonComponent;
  let fixture: ComponentFixture<BarcodeButtonComponent>;
  let scannerSpy: jasmine.SpyObj<BarcodeScannerService>;
  let platformSpy: jasmine.SpyObj<Pick<PlatformService, 'isNative'>>;
  let notifSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    scannerSpy = jasmine.createSpyObj<BarcodeScannerService>('BarcodeScannerService', ['scan']);
    platformSpy = jasmine.createSpyObj<Pick<PlatformService, 'isNative'>>('PlatformService', ['isNative']);
    notifSpy = jasmine.createSpyObj<NotificationService>('NotificationService', ['error', 'info']);

    await TestBed.configureTestingModule({
      imports: [BarcodeButtonComponent],
      providers: [
        { provide: BarcodeScannerService, useValue: scannerSpy },
        { provide: PlatformService, useValue: platformSpy },
        { provide: NotificationService, useValue: notifSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BarcodeButtonComponent);
    component = fixture.componentInstance;
  });

  it('should be hidden on web by default (hideOnWeb=true)', () => {
    platformSpy.isNative.and.returnValue(false);
    fixture.detectChanges();
    expect(component.visible).toBe(false);
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it('should be visible on web when hideOnWeb=false', () => {
    platformSpy.isNative.and.returnValue(false);
    component.hideOnWeb = false;
    fixture.detectChanges();
    expect(component.visible).toBe(true);
    expect(fixture.nativeElement.querySelector('button')).toBeTruthy();
  });

  it('should be visible on native', () => {
    platformSpy.isNative.and.returnValue(true);
    fixture.detectChanges();
    expect(component.visible).toBe(true);
  });

  it('should emit scanned value on successful scan', async () => {
    platformSpy.isNative.and.returnValue(true);
    scannerSpy.scan.and.resolveTo('ABC-123');
    const emitSpy = spyOn(component.scanned, 'emit');

    await component.onScan();

    expect(emitSpy).toHaveBeenCalledWith('ABC-123');
    expect(component.scanning()).toBe(false);
  });

  it('should not emit when scan is cancelled (null)', async () => {
    platformSpy.isNative.and.returnValue(true);
    scannerSpy.scan.and.resolveTo(null);
    const emitSpy = spyOn(component.scanned, 'emit');

    await component.onScan();

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('should notify with the domain message on BarcodeUnavailableError', async () => {
    platformSpy.isNative.and.returnValue(true);
    scannerSpy.scan.and.rejectWith(new BarcodeUnavailableError('Permission caméra refusée.'));

    await component.onScan();

    expect(notifSpy.error).toHaveBeenCalledWith('Permission caméra refusée.');
    expect(component.scanning()).toBe(false);
  });

  it('should notify with a generic message on unknown error', async () => {
    platformSpy.isNative.and.returnValue(true);
    scannerSpy.scan.and.rejectWith(new Error('boom'));

    await component.onScan();

    expect(notifSpy.error).toHaveBeenCalledWith('Échec du scan. Réessayez.');
  });

  it('should ignore concurrent scans while one is running', async () => {
    platformSpy.isNative.and.returnValue(true);
    let resolveScan: (v: string | null) => void = () => undefined;
    scannerSpy.scan.and.returnValue(new Promise<string | null>((res) => (resolveScan = res)));

    const first = component.onScan();
    expect(component.scanning()).toBe(true);
    await component.onScan(); // should early-return
    expect(scannerSpy.scan).toHaveBeenCalledTimes(1);

    resolveScan('X');
    await first;
  });

  it('should do nothing when disabled', async () => {
    platformSpy.isNative.and.returnValue(true);
    component.disabled = true;
    await component.onScan();
    expect(scannerSpy.scan).not.toHaveBeenCalled();
  });
});
