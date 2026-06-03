import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportingHomeComponent } from './reporting-home.component';

describe('ReportingHomeComponent', () => {
  let component: ReportingHomeComponent;
  let fixture: ComponentFixture<ReportingHomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ReportingHomeComponent] }).compileComponents();
    fixture = TestBed.createComponent(ReportingHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => { expect(component).toBeTruthy(); });
  it('should display Reporting title', () => {
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain('Rapports');
  });
});
