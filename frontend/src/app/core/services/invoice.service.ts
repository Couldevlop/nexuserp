import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InvoiceSummary {
  id: string;
  invoiceNumber: string;
  customerName: string;
  totalAmount: number;
  currency: string;
  status: string;
  invoiceDate: string;
  dueDate: string;
}

export interface InvoiceDetail extends InvoiceSummary {
  customerId: string;
  subtotalHt: number;
  vatAmount: number;
  lines: InvoiceLine[];
  notes: string;
  createdAt: string;
}

export interface InvoiceLine {
  description: string;
  quantity: number;
  unitPrice: number;
  vatRate: number;
  totalHt: number;
}

export interface CreateInvoiceRequest {
  customerId: string;
  customerName: string;
  invoiceDate: string;
  dueDate: string;
  currency?: string;
  lines: Omit<InvoiceLine, 'totalHt'>[];
  notes?: string;
}

export interface ApiPage<T> {
  data: T[];
  meta: { page: number; size: number; total: number; totalPages: number };
}

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/finance/invoices';

  list(page = 0, size = 20, status?: string): Observable<ApiPage<InvoiceSummary>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'invoiceDate,desc');
    if (status) params = params.set('status', status);
    return this.http.get<ApiPage<InvoiceSummary>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<InvoiceDetail> {
    return this.http.get<InvoiceDetail>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateInvoiceRequest): Observable<InvoiceDetail> {
    return this.http.post<InvoiceDetail>(this.baseUrl, request);
  }

  approve(id: string): Observable<InvoiceDetail> {
    return this.http.put<InvoiceDetail>(`${this.baseUrl}/${id}/approve`, {});
  }

  markPaid(id: string, paymentDate: string): Observable<InvoiceDetail> {
    return this.http.put<InvoiceDetail>(`${this.baseUrl}/${id}/pay`, { paymentDate });
  }

  cancel(id: string, reason?: string): Observable<InvoiceDetail> {
    return this.http.put<InvoiceDetail>(`${this.baseUrl}/${id}/cancel`, { reason });
  }
}
