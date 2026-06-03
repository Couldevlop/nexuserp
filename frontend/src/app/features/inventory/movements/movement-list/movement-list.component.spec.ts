import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MovementListComponent } from './movement-list.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { StockMovement } from '../../inventory.types';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

const movement: StockMovement = {
  id: 'm1', productId: 'p1', productCode: 'ART-001', productName: 'Acier',
  type: 'IN', quantity: 10, unit: 'KG', reference: 'BL-1', reason: null,
  warehouseFrom: null, warehouseTo: 'WH1', createdAt: '2026-01-01T10:00:00Z', createdBy: 'u1'
};

const mockPage = {
  content: [movement], totalPages: 1, totalElements: 1, number: 0, size: 20, first: true, last: true, empty: false
};

describe('MovementListComponent', () => {
  let component: MovementListComponent;
  let fixture: ComponentFixture<MovementListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [MovementListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MovementListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as any;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load movements from /movements', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith(
      '/api/v1/inventory/movements', jasmine.anything()
    );
    expect(component.movements().length).toBe(1);
    expect(component.isLoading()).toBeFalse();
  });

  it('should set notAvailable on 404 without error toast', () => {
    httpClient.get.and.returnValue(throwError(() => ({ status: 404 })));
    fixture.detectChanges();
    expect(component.notAvailable()).toBeTrue();
    expect(mockNotif.error).not.toHaveBeenCalled();
    expect(component.movements().length).toBe(0);
  });

  it('should show error toast on non-404 error', () => {
    httpClient.get.and.returnValue(throwError(() => ({ status: 500 })));
    fixture.detectChanges();
    expect(mockNotif.error).toHaveBeenCalled();
    expect(component.notAvailable()).toBeFalse();
  });

  it('should filter by type', () => {
    fixture.detectChanges();
    component.onTypeChange('OUT');
    expect(component.typeFilter()).toBe('OUT');
    expect(component.currentPage()).toBe(0);
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('should filter by date range', () => {
    fixture.detectChanges();
    component.onDateFrom('2026-01-01');
    component.onDateTo('2026-01-31');
    expect(component.dateFrom()).toBe('2026-01-01');
    expect(component.dateTo()).toBe('2026-01-31');
  });

  it('should not navigate beyond pages', () => {
    fixture.detectChanges();
    component.totalPages.set(2);
    component.goToPage(99);
    expect(component.currentPage()).toBe(0);
  });

  it('should render movement product code', () => {
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('ART-001');
  });

  it('should show empty state when no movements', () => {
    httpClient.get.and.returnValue(of({ ...mockPage, content: [], totalElements: 0 }));
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Aucun mouvement');
  });
});
