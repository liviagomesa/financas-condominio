import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Resident, ResidentRequest } from '../models/resident.model';

@Injectable({ providedIn: 'root' })
export class ResidentService {
  private readonly baseUrl = `${environment.apiBaseUrl}/residents`;

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Resident[]> {
    return this.http.get<Resident[]>(this.baseUrl);
  }

  findById(id: number): Observable<Resident> {
    return this.http.get<Resident>(`${this.baseUrl}/${id}`);
  }

  create(request: ResidentRequest): Observable<Resident> {
    return this.http.post<Resident>(this.baseUrl, request);
  }

  update(id: number, request: ResidentRequest): Observable<Resident> {
    return this.http.put<Resident>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
