import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminHomeComponent } from './admin-home.component';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { signal } from '@angular/core';

describe('AdminHomeComponent', () => {
  let component: AdminHomeComponent;
  let fixture: ComponentFixture<AdminHomeComponent>;

  const mockAuthState = {
    tenantId: signal('test-tenant'),
    isAuthenticated: signal(true),
    userProfile: signal(null)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminHomeComponent],
      providers: [{ provide: AuthStateService, useValue: mockAuthState }]
    }).compileComponents();
    fixture = TestBed.createComponent(AdminHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => { expect(component).toBeTruthy(); });
  it('should display Administration title', () => {
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain('Administration');
  });
});
