import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SkeletonComponent } from './skeleton.component';

describe('SkeletonComponent', () => {
  let component: SkeletonComponent;
  let fixture: ComponentFixture<SkeletonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render 1 skeleton line by default', () => {
    const lines = fixture.nativeElement.querySelectorAll('.nx-skeleton');
    expect(lines.length).toBe(1);
  });

  it('should render correct number of lines', () => {
    component.lines = 3;
    fixture.detectChanges();
    const lines = fixture.nativeElement.querySelectorAll('.nx-skeleton');
    expect(lines.length).toBe(3);
  });

  it('should return correct lineArray', () => {
    component.lines = 4;
    expect(component.lineArray.length).toBe(4);
  });

  it('should apply width and height styles', () => {
    component.width = '200px';
    component.height = '24px';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.nx-skeleton');
    expect(el.style.width).toBe('200px');
    expect(el.style.height).toBe('24px');
  });

  it('should have aria-busy attribute for accessibility', () => {
    const el = fixture.nativeElement.querySelector('.nx-skeleton');
    expect(el.getAttribute('aria-busy')).toBe('true');
  });
});
