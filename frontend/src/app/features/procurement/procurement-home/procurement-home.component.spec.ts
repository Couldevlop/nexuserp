import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProcurementHomeComponent } from './procurement-home.component';

describe('ProcurementHomeComponent', () => {
  let component: ProcurementHomeComponent;
  let fixture: ComponentFixture<ProcurementHomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProcurementHomeComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ProcurementHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display module title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Achats');
  });
});
