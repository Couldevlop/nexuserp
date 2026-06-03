import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'nx-settings-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './settings-home.component.html',
  styleUrl: './settings-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsHomeComponent {}
