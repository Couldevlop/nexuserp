import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmptyStateComponent } from './empty-state.component';

describe('EmptyStateComponent', () => {
  let component: EmptyStateComponent;
  let fixture: ComponentFixture<EmptyStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyStateComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(EmptyStateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display default title', () => {
    const el = fixture.nativeElement.querySelector('.nx-empty-state__title');
    expect(el.textContent.trim()).toBe('Aucun élément');
  });

  it('should display custom title', () => {
    component.title = 'Aucune facture';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-empty-state__title');
    expect(el.textContent.trim()).toBe('Aucune facture');
  });

  it('should not render description when empty', () => {
    component.description = '';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-empty-state__description');
    expect(el).toBeNull();
  });

  it('should render description when provided', () => {
    component.description = 'Créez votre première facture';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-empty-state__description');
    expect(el.textContent.trim()).toBe('Créez votre première facture');
  });

  it('should display icon', () => {
    component.icon = '🧾';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-empty-state__icon');
    expect(el.textContent.trim()).toBe('🧾');
  });

  it('should have status role for accessibility', () => {
    const el = fixture.nativeElement.querySelector('.nx-empty-state');
    expect(el.getAttribute('role')).toBe('status');
  });
});
