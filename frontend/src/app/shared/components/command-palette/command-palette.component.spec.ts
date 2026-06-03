import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CommandPaletteComponent } from './command-palette.component';
import { CommandPaletteService } from './command-palette.service';

describe('CommandPaletteComponent', () => {
  let component: CommandPaletteComponent;
  let fixture: ComponentFixture<CommandPaletteComponent>;
  let service: CommandPaletteService;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [CommandPaletteComponent],
      providers: [{ provide: Router, useValue: router }],
    }).compileComponents();

    fixture = TestBed.createComponent(CommandPaletteComponent);
    component = fixture.componentInstance;
    service = TestBed.inject(CommandPaletteService);
    fixture.detectChanges();
  });

  function dispatchCtrlK(): void {
    document.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, cancelable: true })
    );
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should be closed initially (no dialog rendered)', () => {
    expect(component.isOpen()).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
  });

  it('should open on Ctrl+K', () => {
    dispatchCtrlK();
    fixture.detectChanges();
    expect(component.isOpen()).toBeTrue();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).not.toBeNull();
  });

  it('should expose accessible dialog + combobox + listbox attributes', () => {
    service.open();
    fixture.detectChanges();
    const dialog = fixture.nativeElement.querySelector('[role="dialog"]');
    const combobox = fixture.nativeElement.querySelector('[role="combobox"]');
    const listbox = fixture.nativeElement.querySelector('[role="listbox"]');
    expect(dialog.getAttribute('aria-modal')).toBe('true');
    expect(combobox.getAttribute('aria-autocomplete')).toBe('list');
    expect(listbox).not.toBeNull();
  });

  it('should filter commands by query', () => {
    service.open();
    fixture.detectChanges();
    const total = component.resultCount();
    component.onSearchInput('factures');
    fixture.detectChanges();
    expect(component.resultCount()).toBeLessThan(total);
    expect(component.filtered().some((c) => c.id === 'nav-finance-invoices')).toBeTrue();
  });

  it('should move highlight with ArrowDown / ArrowUp', () => {
    service.open();
    fixture.detectChanges();
    expect(component.highlighted()).toBe(0);
    component.onKeydown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    expect(component.highlighted()).toBe(1);
    component.onKeydown(new KeyboardEvent('keydown', { key: 'ArrowUp' }));
    expect(component.highlighted()).toBe(0);
  });

  it('should jump with Home / End', () => {
    service.open();
    fixture.detectChanges();
    component.onKeydown(new KeyboardEvent('keydown', { key: 'End' }));
    expect(component.highlighted()).toBe(component.resultCount() - 1);
    component.onKeydown(new KeyboardEvent('keydown', { key: 'Home' }));
    expect(component.highlighted()).toBe(0);
  });

  it('should navigate via Router on Enter for a route command', () => {
    service.open();
    fixture.detectChanges();
    component.onKeydown(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.isOpen()).toBeFalse();
  });

  it('should run callback for an action command', () => {
    const spy = jasmine.createSpy('action');
    service.registerCommands([
      { id: 'act-1', label: 'Action test', group: 'Test', icon: 'x', action: spy },
    ]);
    service.open();
    fixture.detectChanges();
    component.onSearchInput('Action test');
    fixture.detectChanges();
    component.execute(component.filtered()[0]);
    expect(spy).toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should close on Escape', () => {
    service.open();
    fixture.detectChanges();
    component.onKeydown(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(component.isOpen()).toBeFalse();
  });

  it('should report aria-activedescendant matching the highlighted option', () => {
    service.open();
    fixture.detectChanges();
    const active = component.filtered()[component.highlighted()];
    expect(component.activeDescendantId()).toBe(component.optionId(active));
  });
});
