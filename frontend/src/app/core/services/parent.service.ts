import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';
import { PageResponse } from '../models/page.model';
import { Parent, ParentRequest } from '../models/parent.model';

export interface ParentListParams {
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ParentService {
  private readonly baseUrl = `${API_BASE_URL}/parents`;

  constructor(private readonly http: HttpClient) {}

  list(params: ParentListParams = {}): Observable<PageResponse<Parent>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<Parent>>(this.baseUrl, { params: httpParams });
  }

  get(id: string): Observable<Parent> {
    return this.http.get<Parent>(`${this.baseUrl}/${id}`);
  }

  create(request: ParentRequest): Observable<Parent> {
    return this.http.post<Parent>(this.baseUrl, request);
  }

  update(id: string, request: ParentRequest): Observable<Parent> {
    return this.http.put<Parent>(`${this.baseUrl}/${id}`, request);
  }
}
