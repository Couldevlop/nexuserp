import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BadgeComponent } from './badge.component';

describe('BadgeComponent', () => {
  let component: BadgeComponent;
  let fixture: ComponentFixture<BadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BadgeComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(BadgeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default to neutral variant', () => {
    expect(component.variant).toBe('neutral');
  });

  it('should render label text', () => {
    component.label = 'Payée';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-badge');
    expect(el.textContent.trim()).toBe('Payée');
  });

  it('should apply correct CSS class for success variant', () => {
    component.variant = 'success';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-badge');
    expect(el.classList).toContain('nx-badge--success');
  });

  it('should apply correct CSS class for error variant', () => {
    component.variant = 'error';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-badge');
    expect(el.classList).toContain('nx-badge--error');
  });

  it('should apply correct CSS class for warning variant', () => {
    component.variant = 'warning';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-badge');
    expect(el.classList).toContain('nx-badge--warning');
  });

  it('should apply correct CSS class for info variant', () => {
    component.variant = 'info';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-badge');
    expect(el.classList).toContain('nx-badge--info');
  });
});
