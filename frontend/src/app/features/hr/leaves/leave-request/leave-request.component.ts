import { Component, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';

interface LeaveForm {
  employeeId: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  reason: string;
}

interface CreatedLeaveResponse {
  id?: string;
  data?: { id: string };
}

@Component({
  selector: 'nx-leave-request',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './leave-request.component.html',
  styleUrl: './leave-request.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LeaveRequestComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);

  readonly isSubmitting = signal(false);

  // Aligné sur enum backend Leave.LeaveType.
  readonly leaveTypes = [
    { value: 'ANNUAL', label: 'Congés payés' },
    { value: 'SICK', label: 'Maladie' },
    { value: 'MATERNITY', label: 'Maternité' },
    { value: 'PATERNITY', label: 'Paternité' },
    { value: 'RTT', label: 'RTT' },
    { value: 'UNPAID', label: 'Sans solde' },
    { value: 'OTHER', label: 'Autre' },
  ];

  readonly form = signal<LeaveForm>({
    employeeId: '',
    leaveType: 'ANNUAL',
    startDate: this.today(),
    endDate: this.today(),
    reason: '',
  });

  // Durée inclusive (comme le domaine backend : end - start + 1 jour).
  readonly durationDays = computed(() => {
    const f = this.form();
    if (!f.startDate || !f.endDate) return 0;
    const start = new Date(f.startDate).getTime();
    const end = new Date(f.endDate).getTime();
    if (Number.isNaN(start) || Number.isNaN(end) || end < start) return 0;
    return Math.floor((end - start) / 86_400_000) + 1;
  });

  readonly datesValid = computed(() => {
    const f = this.form();
    if (!f.startDate || !f.endDate) return false;
    return new Date(f.endDate).getTime() >= new Date(f.startDate).getTime();
  });

  readonly isValid = computed(() => {
    const f = this.form();
    return (
      f.employeeId.trim().length > 0 &&
      f.leaveType.length > 0 &&
      this.datesValid()
    );
  });

  private today(): string {
    return new Date().toISOString().split('T')[0];
  }

  updateField<K extends keyof LeaveForm>(field: K, value: LeaveForm[K]): void {
    this.form.update((f) => ({ ...f, [field]: value }));
  }

  submit(): void {
    if (!this.isValid()) {
      this.notif.error('Veuillez renseigner le salarié, le type et des dates valides');
      return;
    }

    const f = this.form();
    const payload = {
      employeeId: f.employeeId.trim(),
      leaveType: f.leaveType,
      startDate: f.startDate,
      endDate: f.endDate,
      reason: f.reason.trim() || null,
    };

    this.isSubmitting.set(true);
    this.http.post<CreatedLeaveResponse>('/api/v1/hr/leaves', payload).subscribe({
      next: () => {
        this.notif.success('Demande de congé soumise');
        this.router.navigate(['/hr/leaves']);
      },
      error: () => {
        this.notif.error('Erreur lors de la soumission de la demande');
        this.isSubmitting.set(false);
      }
    });
  }
}
