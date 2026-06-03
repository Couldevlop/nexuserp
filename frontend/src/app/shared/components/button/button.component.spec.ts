import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ButtonComponent } from './button.component';
import { By } from '@angular/platform-browser';

describe('ButtonComponent', () => {
  let component: ButtonComponent;
  let fixture: ComponentFixture<ButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ButtonComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default to primary variant and md size', () => {
    expect(component.variant).toBe('primary');
    expect(component.size).toBe('md');
  });

  it('should emit clicked event on click', () => {
    const spy = spyOn(component.clicked, 'emit');
    const btn = fixture.debugElement.query(By.css('button'));
    btn.nativeElement.click();
    expect(spy).toHaveBeenCalled();
  });

  it('should not emit when disabled', () => {
    const spy = spyOn(component.clicked, 'emit');
    component.disabled = true;
    fixture.detectChanges();
    component.onClick(new MouseEvent('click'));
    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit when loading', () => {
    const spy = spyOn(component.clicked, 'emit');
    component.loading = true;
    fixture.detectChanges();
    component.onClick(new MouseEvent('click'));
    expect(spy).not.toHaveBeenCalled();
  });

  it('should render spinner when loading', () => {
    component.loading = true;
    fixture.detectChanges();
    const spinner = fixture.nativeElement.querySelector('.nx-btn__spinner');
    expect(spinner).toBeTruthy();
  });

  it('should apply variant CSS class', () => {
    component.variant = 'danger';
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button');
    expect(btn.className).toContain('nx-btn--danger');
  });

  it('should apply size CSS class', () => {
    component.size = 'lg';
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button');
    expect(btn.className).toContain('nx-btn--lg');
  });

  it('should set button type attribute', () => {
    component.type = 'submit';
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button');
    expect(btn.type).toBe('submit');
  });
});
