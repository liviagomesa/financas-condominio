import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Group, GroupRequest } from '../models/group.model';

@Injectable({ providedIn: 'root' })
export class GroupService {
  private readonly baseUrl = `${environment.apiBaseUrl}/groups`;

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Group[]> {
    return this.http.get<Group[]>(this.baseUrl);
  }

  findById(id: number): Observable<Group> {
    return this.http.get<Group>(`${this.baseUrl}/${id}`);
  }

  create(request: GroupRequest): Observable<Group> {
    return this.http.post<Group>(this.baseUrl, request);
  }

  update(id: number, request: GroupRequest): Observable<Group> {
    return this.http.put<Group>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
