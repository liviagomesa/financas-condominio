import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Party, PartyRequest } from '../models/party.model';

@Injectable({ providedIn: 'root' })
export class PartyService {
  private readonly baseUrl = `${environment.apiBaseUrl}/parties`;

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Party[]> {
    return this.http.get<Party[]>(this.baseUrl);
  }

  findById(id: number): Observable<Party> {
    return this.http.get<Party>(`${this.baseUrl}/${id}`);
  }

  create(request: PartyRequest): Observable<Party> {
    return this.http.post<Party>(this.baseUrl, request);
  }

  update(id: number, request: PartyRequest): Observable<Party> {
    return this.http.put<Party>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
