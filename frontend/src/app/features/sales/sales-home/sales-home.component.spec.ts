import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SalesHomeComponent } from './sales-home.component';

describe('SalesHomeComponent', () => {
  let component: SalesHomeComponent;
  let fixture: ComponentFixture<SalesHomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [SalesHomeComponent] }).compileComponents();
    fixture = TestBed.createComponent(SalesHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => { expect(component).toBeTruthy(); });
  it('should display Ventes title', () => {
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain('Ventes');
  });
});
