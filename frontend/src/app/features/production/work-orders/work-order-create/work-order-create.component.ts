import { Component, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';
import { CreateWorkOrderRequest, WorkOrder, WorkOrderPriority } from '../work-order.model';
import { WO_PRIORITY_LABELS } from '../production-format';

interface CreatedWorkOrderResponse {
  id?: string;
  data?: { id: string };
}

@Component({
  selector: 'nx-work-order-create',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './work-order-create.component.html',
  styleUrl: './work-order-create.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkOrderCreateComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  readonly isSubmitting = signal(false);

  readonly priorities: { value: WorkOrderPriority; label: string }[] = (
    Object.keys(WO_PRIORITY_LABELS) as WorkOrderPriority[]
  ).map((value) => ({ value, label: WO_PRIORITY_LABELS[value] }));

  // Formulaire réactif (Reactive Forms) — autorité de validation côté serveur,
  // validation client de premier niveau ici.
  readonly form = this.fb.nonNullable.group({
    productName: ['', [Validators.required, Validators.maxLength(200)]],
    productId: [''],
    quantityPlanned: [1, [Validators.required, Validators.min(0.001)]],
    priority: ['NORMAL' as WorkOrderPriority, [Validators.required]],
    plannedStartDate: [this.today()],
    plannedEndDate: [this.defaultEndDate()],
    workcenter: [''],
    bomId: [''],
    routingId: [''],
    notes: [''],
  });

  readonly dateRangeInvalid = signal(false);

  private today(): string {
    return new Date().toISOString().split('T')[0];
  }

  private defaultEndDate(): string {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return d.toISOString().split('T')[0];
  }

  private isDateRangeValid(): boolean {
    const start = this.form.controls.plannedStartDate.value;
    const end = this.form.controls.plannedEndDate.value;
    if (!start || !end) return true;
    return new Date(end).getTime() >= new Date(start).getTime();
  }

  controlInvalid(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.invalid && (c.dirty || c.touched);
  }

  submit(): void {
    this.form.markAllAsTouched();
    const rangeOk = this.isDateRangeValid();
    this.dateRangeInvalid.set(!rangeOk);

    if (this.form.invalid || !rangeOk) {
      this.notif.error('Veuillez corriger les champs en erreur');
      return;
    }

    const v = this.form.getRawValue();
    const payload: CreateWorkOrderRequest = {
      productName: v.productName.trim(),
      productId: v.productId.trim() || null,
      quantityPlanned: v.quantityPlanned,
      priority: v.priority,
      plannedStartDate: v.plannedStartDate || null,
      plannedEndDate: v.plannedEndDate || null,
      workcenter: v.workcenter.trim() || null,
      bomId: v.bomId.trim() || null,
      routingId: v.routingId.trim() || null,
      notes: v.notes.trim() || null,
    };

    this.isSubmitting.set(true);
    this.http
      .post<CreatedWorkOrderResponse & Partial<WorkOrder>>('/api/v1/production/work-orders', payload)
      .subscribe({
        next: (res) => {
          this.notif.success('Ordre de fabrication créé avec succès');
          const id = res?.id ?? res?.data?.id;
          if (id) {
            this.router.navigate(['/production/work-orders', id]);
          } else {
            this.router.navigate(['/production/work-orders']);
          }
        },
        error: () => {
          this.notif.error("Erreur lors de la création de l'ordre de fabrication");
          this.isSubmitting.set(false);
        },
      });
  }
}
