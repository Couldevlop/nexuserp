import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'nx-skeleton',
  standalone: true,
  templateUrl: './skeleton.component.html',
  styleUrl: './skeleton.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SkeletonComponent {
  @Input() width = '100%';
  @Input() height = '16px';
  @Input() borderRadius = '4px';
  @Input() lines = 1;

  get lineArray(): number[] {
    return Array(this.lines).fill(0);
  }
}
