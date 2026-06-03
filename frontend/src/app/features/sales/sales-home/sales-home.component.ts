import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'nx-sales-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sales-home.component.html',
  styleUrl: './sales-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SalesHomeComponent {}
