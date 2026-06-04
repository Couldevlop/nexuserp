import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConfigCategory, ConfigParam, ConfigUpsertRequest } from './config.model';

/**
 * Client REST du service nexus-config (base `/api/v1/config`, via gateway).
 * Les intercepteurs auth/tenant existants ajoutent JWT + tenant header.
 */
@Injectable({ providedIn: 'root' })
export class ConfigService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/config';

  /** Liste des paramètres, filtrable par catégorie. Secrets masqués côté serveur. */
  list(category?: ConfigCategory): Observable<ConfigParam[]> {
    let params = new HttpParams();
    if (category) {
      params = params.set('category', category);
    }
    return this.http.get<ConfigParam[]>(this.baseUrl, { params });
  }

  getByKey(key: string): Observable<ConfigParam> {
    return this.http.get<ConfigParam>(`${this.baseUrl}/${encodeURIComponent(key)}`);
  }

  /** Upsert. Pour un secret : valeur vide => conserve l'existant (write-only). */
  upsert(key: string, request: ConfigUpsertRequest): Observable<ConfigParam> {
    return this.http.put<ConfigParam>(`${this.baseUrl}/${encodeURIComponent(key)}`, request);
  }

  delete(key: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${encodeURIComponent(key)}`);
  }
}
