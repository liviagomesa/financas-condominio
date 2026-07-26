import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Receivable,
  ReceivableBulkRequest,
  ReceivableRequest,
} from '../models/receivable.model';

@Injectable({ providedIn: 'root' })
export class ReceivableService {
  private readonly baseUrl = `${environment.apiBaseUrl}/receivables`;

  constructor(private readonly http: HttpClient) {}

  findAll(unitId?: number): Observable<Receivable[]> {
    const params = unitId != null ? { unitId } : undefined;
    return this.http.get<Receivable[]>(this.baseUrl, { params });
  }

  findById(id: number): Observable<Receivable> {
    return this.http.get<Receivable>(`${this.baseUrl}/${id}`);
  }

  create(request: ReceivableRequest): Observable<Receivable> {
    return this.http.post<Receivable>(this.baseUrl, request);
  }

  createBulk(request: ReceivableBulkRequest): Observable<Receivable[]> {
    return this.http.post<Receivable[]>(`${this.baseUrl}/bulk`, request);
  }

  update(id: number, request: ReceivableRequest): Observable<Receivable> {
    return this.http.put<Receivable>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
