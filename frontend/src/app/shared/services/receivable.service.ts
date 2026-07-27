import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Receivable,
  ReceivableBulkRequest,
  ReceivableFilters,
  ReceivablePaymentRequest,
  ReceivableRequest,
} from '../models/receivable.model';

@Injectable({ providedIn: 'root' })
export class ReceivableService {
  private readonly baseUrl = `${environment.apiBaseUrl}/receivables`;

  constructor(private readonly http: HttpClient) {}

  findAll(filters?: ReceivableFilters): Observable<Receivable[]> {
    const params: Record<string, string | number | boolean> = {};
    if (filters?.unitId != null) params['unitId'] = filters.unitId;
    if (filters?.paid != null) params['paid'] = filters.paid;
    if (filters?.overdue != null) params['overdue'] = filters.overdue;
    if (filters?.dueYearMonth) params['dueYearMonth'] = filters.dueYearMonth;
    if (filters?.paymentYearMonth) params['paymentYearMonth'] = filters.paymentYearMonth;
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

  registerPayment(id: number, request: ReceivablePaymentRequest): Observable<Receivable> {
    return this.http.post<Receivable>(`${this.baseUrl}/${id}/pay`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
