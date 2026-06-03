import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatCardComponent } from './stat-card.component';

describe('StatCardComponent', () => {
  let component: StatCardComponent;
  let fixture: ComponentFixture<StatCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StatCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('label', 'Chiffre d’affaires');
    fixture.componentRef.setInput('value', '124 500 €');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render label and value', () => {
    const label = fixture.nativeElement.querySelector('.nx-stat-card__label');
    const value = fixture.nativeElement.querySelector('.nx-stat-card__value');
    expect(label.textContent.trim()).toBe('Chiffre d’affaires');
    expect(value.textContent.trim()).toBe('124 500 €');
  });

  it('should show shimmer skeleton when loading', () => {
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();
    const skeletons = fixture.nativeElement.querySelectorAll('.nx-stat-card__skeleton');
    expect(skeletons.length).toBeGreaterThan(0);
    expect(fixture.nativeElement.querySelector('.nx-stat-card__value')).toBeNull();
    expect(fixture.nativeElement.querySelector('.nx-stat-card').getAttribute('aria-busy')).toBe('true');
  });

  it('should not render delta when delta is null', () => {
    expect(component.hasDelta()).toBeFalse();
    expect(fixture.nativeElement.querySelector('.nx-stat-card__delta')).toBeNull();
  });

  it('should render an up delta with positive sign and success class', () => {
    fixture.componentRef.setInput('delta', 12.4);
    fixture.detectChanges();
    expect(component.deltaDirection()).toBe('up');
    const delta = fixture.nativeElement.querySelector('.nx-stat-card__delta');
    expect(delta.classList).toContain('nx-stat-card__delta--up');
    expect(delta.textContent).toContain('+12.4 %');
  });

  it('should render a down delta with down class', () => {
    fixture.componentRef.setInput('delta', -5);
    fixture.detectChanges();
    expect(component.deltaDirection()).toBe('down');
    const delta = fixture.nativeElement.querySelector('.nx-stat-card__delta');
    expect(delta.classList).toContain('nx-stat-card__delta--down');
    expect(delta.textContent).toContain('-5 %');
  });

  it('should treat zero delta as neutral', () => {
    fixture.componentRef.setInput('delta', 0);
    fixture.detectChanges();
    expect(component.deltaDirection()).toBe('neutral');
    const delta = fixture.nativeElement.querySelector('.nx-stat-card__delta');
    expect(delta.classList).toContain('nx-stat-card__delta--neutral');
  });

  it('should render icon initial when icon provided', () => {
    fixture.componentRef.setInput('icon', 'dollar');
    fixture.detectChanges();
    const icon = fixture.nativeElement.querySelector('.nx-stat-card__icon');
    expect(icon.textContent.trim()).toBe('d');
  });
});
