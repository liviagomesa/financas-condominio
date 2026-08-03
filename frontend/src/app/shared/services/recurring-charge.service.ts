import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  RecurringCharge,
  RecurringChargeBulkRequest,
  RecurringChargeRequest,
} from '../models/recurring-charge.model';

@Injectable({ providedIn: 'root' })
export class RecurringChargeService {
  private readonly baseUrl = `${environment.apiBaseUrl}/recurring-charges`;

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<RecurringCharge[]> {
    return this.http.get<RecurringCharge[]>(this.baseUrl);
  }

  findById(id: number): Observable<RecurringCharge> {
    return this.http.get<RecurringCharge>(`${this.baseUrl}/${id}`);
  }

  create(request: RecurringChargeRequest): Observable<RecurringCharge> {
    return this.http.post<RecurringCharge>(this.baseUrl, request);
  }

  createBulk(request: RecurringChargeBulkRequest): Observable<RecurringCharge[]> {
    return this.http.post<RecurringCharge[]>(`${this.baseUrl}/bulk`, request);
  }

  update(id: number, request: RecurringChargeRequest): Observable<RecurringCharge> {
    return this.http.put<RecurringCharge>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
