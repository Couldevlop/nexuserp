import { Component, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service';

interface EmployeeForm {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  department: string;
  jobTitle: string;
  contractType: string;
  hireDate: string;
  grossSalary: number;
  salaryCurrency: string;
  country: string;
}

interface CreatedEmployeeResponse {
  id?: string;
  data?: { id: string };
}

@Component({
  selector: 'nx-employee-create',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './employee-create.component.html',
  styleUrl: './employee-create.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmployeeCreateComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notif = inject(NotificationService);

  readonly isSubmitting = signal(false);

  readonly currencies = ['XOF', 'EUR', 'USD'];

  // Aligné sur enum backend Employee.ContractType.
  readonly contractTypes = [
    { value: 'CDI', label: 'CDI' },
    { value: 'CDD', label: 'CDD' },
    { value: 'INTERIM', label: 'Intérim' },
    { value: 'INTERNSHIP', label: 'Stage' },
    { value: 'FREELANCE', label: 'Freelance' },
  ];

  // Aligné sur enum backend Employee.Country.
  readonly countries = [
    { value: 'CI', label: "Côte d'Ivoire" },
    { value: 'FR', label: 'France' },
    { value: 'SN', label: 'Sénégal' },
    { value: 'ML', label: 'Mali' },
    { value: 'BF', label: 'Burkina Faso' },
    { value: 'BE', label: 'Belgique' },
    { value: 'OTHER', label: 'Autre' },
  ];

  readonly form = signal<EmployeeForm>({
    employeeNumber: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    department: '',
    jobTitle: '',
    contractType: 'CDI',
    hireDate: this.today(),
    grossSalary: 0,
    salaryCurrency: 'XOF',
    country: 'CI',
  });

  readonly isValid = computed(() => {
    const f = this.form();
    return (
      f.employeeNumber.trim().length > 0 &&
      f.firstName.trim().length > 0 &&
      f.lastName.trim().length > 0 &&
      f.contractType.length > 0 &&
      f.hireDate.length > 0 &&
      f.grossSalary > 0 &&
      f.salaryCurrency.length > 0 &&
      f.country.length > 0 &&
      this.emailValid()
    );
  });

  // Validation côté client uniquement — le serveur reste l'autorité.
  readonly emailValid = computed(() => {
    const email = this.form().email.trim();
    if (!email) return true; // e-mail optionnel
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  });

  private today(): string {
    return new Date().toISOString().split('T')[0];
  }

  updateField<K extends keyof EmployeeForm>(field: K, value: EmployeeForm[K]): void {
    this.form.update((f) => ({ ...f, [field]: value }));
  }

  submit(): void {
    if (!this.isValid()) {
      this.notif.error('Veuillez renseigner les champs obligatoires (matricule, nom, contrat, embauche, salaire)');
      return;
    }

    const f = this.form();
    const payload = {
      employeeNumber: f.employeeNumber.trim(),
      firstName: f.firstName.trim(),
      lastName: f.lastName.trim(),
      email: f.email.trim() || null,
      phone: f.phone.trim() || null,
      department: f.department.trim() || null,
      jobTitle: f.jobTitle.trim() || null,
      contractType: f.contractType,
      hireDate: f.hireDate,
      grossSalary: f.grossSalary,
      salaryCurrency: f.salaryCurrency,
      country: f.country,
      socialSecurityNumber: null,
      bankIban: null,
      bankBic: null,
    };

    this.isSubmitting.set(true);
    this.http.post<CreatedEmployeeResponse>('/api/v1/hr/employees', payload).subscribe({
      next: (res) => {
        this.notif.success('Salarié créé avec succès');
        const id = res?.id ?? res?.data?.id;
        if (id) {
          this.router.navigate(['/hr/employees', id]);
        } else {
          this.router.navigate(['/hr/employees']);
        }
      },
      error: () => {
        this.notif.error('Erreur lors de la création du salarié');
        this.isSubmitting.set(false);
      }
    });
  }
}
