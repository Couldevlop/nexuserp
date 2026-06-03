import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Product {
  id: string;
  code: string;
  name: string;
  description: string;
  unit: string;
  unitPrice: number;
  currentStock: number;
  reorderPoint: number;
  category: string;
  status: string;
}

export interface StockMovementRequest {
  type: 'RECEIVE' | 'ISSUE' | 'ADJUST';
  quantity: number;
  reference?: string;
  notes?: string;
}

export interface ApiPage<T> {
  data: T[];
  meta: { page: number; size: number; total: number; totalPages: number };
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/inventory/products';

  list(page = 0, size = 20, search?: string): Observable<ApiPage<Product>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('q', search);
    return this.http.get<ApiPage<Product>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }

  create(product: Partial<Product>): Observable<Product> {
    return this.http.post<Product>(this.baseUrl, product);
  }

  update(id: string, product: Partial<Product>): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/${id}`, product);
  }

  recordMovement(id: string, movement: StockMovementRequest): Observable<Product> {
    return this.http.post<Product>(`${this.baseUrl}/${id}/stock`, movement);
  }

  getLowStock(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.baseUrl}/low-stock`);
  }
}
