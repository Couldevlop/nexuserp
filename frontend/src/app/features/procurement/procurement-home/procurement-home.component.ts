import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'nx-procurement-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './procurement-home.component.html',
  styleUrl: './procurement-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProcurementHomeComponent {}
