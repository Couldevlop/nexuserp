import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'nx-reporting-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reporting-home.component.html',
  styleUrl: './reporting-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReportingHomeComponent {}
