import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Supplier, SupplierRequest } from '../models/supplier.model';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly baseUrl = `${environment.apiBaseUrl}/suppliers`;

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Supplier[]> {
    return this.http.get<Supplier[]>(this.baseUrl);
  }

  findById(id: number): Observable<Supplier> {
    return this.http.get<Supplier>(`${this.baseUrl}/${id}`);
  }

  create(request: SupplierRequest): Observable<Supplier> {
    return this.http.post<Supplier>(this.baseUrl, request);
  }

  update(id: number, request: SupplierRequest): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
