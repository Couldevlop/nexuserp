import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WorkOrder {
  id: string;
  orderNumber: string;
  productId: string;
  productName: string;
  status: string;
  priority: string;
  quantityPlanned: number;
  quantityProduced: number;
  quantityRejected: number;
  plannedStartDate: string;
  plannedEndDate: string;
  workcenter: string;
  operator: string;
  isLate: boolean;
  yieldRate: number;
}

export interface ApiPage<T> {
  data: WorkOrder[];
  content?: WorkOrder[];
  meta?: { page: number; size: number; total: number; totalPages: number };
  totalElements?: number;
  totalPages?: number;
}

@Injectable({ providedIn: 'root' })
export class WorkOrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/production/work-orders';

  list(page = 0, size = 20, status?: string): Observable<ApiPage<WorkOrder>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiPage<WorkOrder>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<WorkOrder> {
    return this.http.get<WorkOrder>(`${this.baseUrl}/${id}`);
  }

  create(workOrder: Partial<WorkOrder> & { productName: string; quantityPlanned: number }): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(this.baseUrl, workOrder);
  }

  release(id: string): Observable<WorkOrder> {
    return this.http.put<WorkOrder>(`${this.baseUrl}/${id}/release`, {});
  }

  start(id: string, operatorId: string): Observable<WorkOrder> {
    return this.http.put<WorkOrder>(`${this.baseUrl}/${id}/start`, { operatorId });
  }

  recordProduction(id: string, quantity: number, rejected: number, operatorId?: string): Observable<WorkOrder> {
    return this.http.put<WorkOrder>(`${this.baseUrl}/${id}/production`, { quantity, rejected, operatorId });
  }
}
