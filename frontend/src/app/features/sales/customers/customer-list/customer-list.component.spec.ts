import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CustomerListComponent } from './customer-list.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationService } from '../../../../core/services/notification.service';
import { of, throwError } from 'rxjs';

const mockNotif = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('CustomerListComponent', () => {
  let component: CustomerListComponent;
  let fixture: ComponentFixture<CustomerListComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  const mockPage = {
    content: [
      { id: 'c-1', code: 'CLI001', name: 'Acme SARL', email: 'contact@acme.ci', phone: '+225 01', city: 'Abidjan', country: 'CI' }
    ],
    totalPages: 1,
    totalElements: 1,
  };

  beforeEach(async () => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [CustomerListComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: mockNotif },
        { provide: HttpClient, useValue: httpSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerListComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading initially', () => {
    expect(component.isLoading()).toBeTrue();
  });

  it('should load customers on init', () => {
    fixture.detectChanges();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/sales/customers', jasmine.anything());
    expect(component.customers().length).toBe(1);
    expect(component.isLoading()).toBeFalse();
  });

  it('should render a customer row', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Acme SARL');
    expect(compiled.textContent).toContain('CLI001');
  });

  it('should degrade gracefully on API error (no toast flood, empty + hasError)', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    expect(component.customers().length).toBe(0);
    expect(component.hasError()).toBeTrue();
    expect(component.isLoading()).toBeFalse();
    expect(mockNotif.error).not.toHaveBeenCalled();
  });

  it('should show the unavailable empty state on error', () => {
    httpClient.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Module clients indisponible');
  });

  it('should filter locally on search', () => {
    fixture.detectChanges();
    component.onSearch('acme');
    expect(component.filteredCustomers().length).toBe(1);
    component.onSearch('zzz');
    expect(component.filteredCustomers().length).toBe(0);
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
