import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Employee {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  department: string;
  position: string;
  contractType: string;
  hireDate: string;
  baseSalary: number;
  country: string;
  status: string;
}

export interface Leave {
  id: string;
  employeeId: string;
  type: string;
  startDate: string;
  endDate: string;
  durationDays: number;
  status: string;
  reason?: string;
}

export interface ApiPage<T> {
  data: T[];
  meta: { page: number; size: number; total: number; totalPages: number };
}

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/hr/employees';

  list(page = 0, size = 20, search?: string): Observable<ApiPage<Employee>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('q', search);
    return this.http.get<ApiPage<Employee>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/${id}`);
  }

  create(employee: Partial<Employee>): Observable<Employee> {
    return this.http.post<Employee>(this.baseUrl, employee);
  }

  update(id: string, employee: Partial<Employee>): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/${id}`, employee);
  }

  terminate(id: string, terminationDate: string): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/${id}/terminate`, { terminationDate });
  }

  getLeaves(employeeId: string): Observable<Leave[]> {
    return this.http.get<Leave[]>(`${this.baseUrl}/${employeeId}/leaves`);
  }

  requestLeave(employeeId: string, leave: Partial<Leave>): Observable<Leave> {
    return this.http.post<Leave>(`${this.baseUrl}/${employeeId}/leaves`, leave);
  }

  approveLeave(leaveId: string): Observable<Leave> {
    return this.http.put<Leave>(`/api/v1/hr/leaves/${leaveId}/approve`, {});
  }
}
