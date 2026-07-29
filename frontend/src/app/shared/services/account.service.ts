import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Account,
  AccountBulkRequest,
  AccountFilters,
  AccountPaymentRequest,
  AccountRequest,
} from '../models/account.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly baseUrl = `${environment.apiBaseUrl}/accounts`;

  constructor(private readonly http: HttpClient) {}

  findAll(filters?: AccountFilters): Observable<Account[]> {
    const params: Record<string, string | number | boolean> = {};
    if (filters?.partyId != null) params['partyId'] = filters.partyId;
    if (filters?.fundId != null) params['fundId'] = filters.fundId;
    if (filters?.type != null) params['type'] = filters.type;
    if (filters?.paid != null) params['paid'] = filters.paid;
    if (filters?.overdue != null) params['overdue'] = filters.overdue;
    if (filters?.dueYearMonth) params['dueYearMonth'] = filters.dueYearMonth;
    if (filters?.paymentYearMonth) params['paymentYearMonth'] = filters.paymentYearMonth;
    return this.http.get<Account[]>(this.baseUrl, { params });
  }

  findById(id: number): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/${id}`);
  }

  create(request: AccountRequest): Observable<Account> {
    return this.http.post<Account>(this.baseUrl, request);
  }

  createBulk(request: AccountBulkRequest): Observable<Account[]> {
    return this.http.post<Account[]>(`${this.baseUrl}/bulk`, request);
  }

  update(id: number, request: AccountRequest): Observable<Account> {
    return this.http.put<Account>(`${this.baseUrl}/${id}`, request);
  }

  registerPayment(id: number, request: AccountPaymentRequest): Observable<Account> {
    return this.http.post<Account>(`${this.baseUrl}/${id}/pay`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
