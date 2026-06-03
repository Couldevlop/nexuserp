import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SupplierListComponent } from './supplier-list.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('SupplierListComponent', () => {
  let component: SupplierListComponent;
  let fixture: ComponentFixture<SupplierListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const mockPage = {
    content: [
      {
        id: 's-1', code: 'FRN001', name: 'Fournitures CI', contactName: 'Kouassi',
        email: 'contact@fournitures.ci', phone: '+225 27', country: 'CI', status: 'ACTIVE' as const
      }
    ],
    totalPages: 1,
    totalElements: 1,
  };

  beforeEach(async () => {
    mockNotif.error.calls.reset();
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [SupplierListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SupplierListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load suppliers on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/procurement/suppliers', jasmine.anything());
    expect(component.suppliers().length).toBe(1);
    expect(component.isLoading()).toBeFalse();
  });

  it('should render a supplier row', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Fournitures CI');
    expect(compiled.textContent).toContain('FRN001');
  });

  it('should degrade gracefully on API error (no toast flood, empty + hasError)', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    expect(component.suppliers().length).toBe(0);
    expect(component.hasError()).toBeTrue();
    expect(component.isLoading()).toBeFalse();
    expect(mockNotif.error).not.toHaveBeenCalled();
  });

  it('should show the unavailable empty state on error', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Module fournisseurs indisponible');
  });

  it('should map supplier status to label and badge class', () => {
    expect(component.statusLabel['ACTIVE']).toBe('Actif');
    expect(component.statusBadgeClass['BLACKLISTED']).toBe('nx-badge--error');
  });

  it('should filter locally on search', () => {
    fixture.detectChanges();
    component.onSearch('fournitures');
    expect(component.filteredSuppliers().length).toBe(1);
    component.onSearch('zzz');
    expect(component.filteredSuppliers().length).toBe(0);
  });

  it('should not navigate beyond totalPages', () => {
    fixture.detectChanges();
    component.totalPages.set(2);
    component.goToPage(9);
    expect(component.currentPage()).toBe(0);
  });

  it('should navigate to a valid page and reload', () => {
    fixture.detectChanges();
    component.totalPages.set(5);
    component.goToPage(2);
    expect(component.currentPage()).toBe(2);
    expect(httpClient.get).toHaveBeenCalledTimes(2);
  });

  it('getPages should match totalPages', () => {
    component.totalPages.set(3);
    expect(component.getPages().length).toBe(3);
  });
});
