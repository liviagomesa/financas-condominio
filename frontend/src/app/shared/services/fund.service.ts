import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Fund, FundRequest } from '../models/fund.model';

@Injectable({ providedIn: 'root' })
export class FundService {
  private readonly baseUrl = `${environment.apiBaseUrl}/funds`;

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Fund[]> {
    return this.http.get<Fund[]>(this.baseUrl);
  }

  findById(id: number): Observable<Fund> {
    return this.http.get<Fund>(`${this.baseUrl}/${id}`);
  }

  create(request: FundRequest): Observable<Fund> {
    return this.http.post<Fund>(this.baseUrl, request);
  }

  update(id: number, request: FundRequest): Observable<Fund> {
    return this.http.put<Fund>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
