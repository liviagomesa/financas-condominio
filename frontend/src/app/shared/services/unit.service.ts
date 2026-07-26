import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Unit, UnitRequest } from '../models/unit.model';

@Injectable({ providedIn: 'root' })
export class UnitService {
  private readonly baseUrl = `${environment.apiBaseUrl}/units`;

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Unit[]> {
    return this.http.get<Unit[]>(this.baseUrl);
  }

  findById(id: number): Observable<Unit> {
    return this.http.get<Unit>(`${this.baseUrl}/${id}`);
  }

  create(request: UnitRequest): Observable<Unit> {
    return this.http.post<Unit>(this.baseUrl, request);
  }

  update(id: number, request: UnitRequest): Observable<Unit> {
    return this.http.put<Unit>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
