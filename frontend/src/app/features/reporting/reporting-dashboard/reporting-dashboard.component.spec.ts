import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportingDashboardComponent } from './reporting-dashboard.component';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

describe('ReportingDashboardComponent', () => {
  let component: ReportingDashboardComponent;
  let fixture: ComponentFixture<ReportingDashboardComponent>;
  let httpClient: jasmine.SpyObj<HttpClient>;

  async function setup() {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get']);
    httpSpy.get.and.returnValue(throwError(() => new Error('404')));

    await TestBed.configureTestingModule({
      imports: [ReportingDashboardComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: HttpClient, useValue: httpSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportingDashboardComponent);
    component = fixture.componentInstance;
    httpClient = TestBed.inject(HttpClient) as unknown as jasmine.SpyObj<HttpClient>;
    return httpClient;
  }

  beforeEach(async () => {
    await setup();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should NOT fabricate KPIs when endpoint is missing (404)', () => {
    fixture.detectChanges();
    expect(component.hasKpis()).toBeFalse();
    expect(component.kpis()).toBeNull();
    expect(component.isLoading()).toBeFalse();
  });

  it('should display dash for unavailable KPI values', () => {
    expect(component.display(null)).toBe('—');
    expect(component.display(undefined)).toBe('—');
    expect(component.display(NaN)).toBe('—');
  });

  it('should format available KPI values with suffix', () => {
    expect(component.display(1234, ' j')).toContain('234');
    expect(component.display(1234, ' j')).toContain('j');
  });

  it('should show the no-data note when KPIs unavailable', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('ne sont pas encore exposés');
  });

  it('should mark hasKpis true when a real KPI value is returned', async () => {
    TestBed.resetTestingModule();
    const http = await setup();
    http.get.and.returnValue(of({ revenue: 50000, currency: 'XOF' }));
    fixture.detectChanges();
    expect(component.hasKpis()).toBeTrue();
    expect(component.kpis()?.revenue).toBe(50000);
  });

  it('should treat an all-empty KPI payload as no data', async () => {
    TestBed.resetTestingModule();
    const http = await setup();
    http.get.and.returnValue(of({ revenue: null, dso: null, stockValue: null, margin: null }));
    fixture.detectChanges();
    expect(component.hasKpis()).toBeFalse();
  });

  it('should render quick links to key reports', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Bilan');
    expect(compiled.textContent).toContain('FEC (France)');
    expect(compiled.textContent).toContain('États SYSCOHADA (CI/UEMOA)');
  });

  it('should expose six quick reports', () => {
    expect(component.quickReports.length).toBe(6);
  });
});
