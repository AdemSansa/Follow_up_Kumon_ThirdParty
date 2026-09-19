import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';
import { ActionItem, ActionItemStatus } from '../models/action-item.model';
import { PageResponse } from '../models/page.model';

export interface ActionItemListParams {
  studentId?: string;
  followUpId?: string;
  status?: ActionItemStatus | '';
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ActionItemService {
  private readonly baseUrl = `${API_BASE_URL}/action-items`;

  constructor(private readonly http: HttpClient) {}

  list(params: ActionItemListParams = {}): Observable<PageResponse<ActionItem>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<ActionItem>>(this.baseUrl, { params: httpParams });
  }

  complete(id: string): Observable<ActionItem> {
    return this.http.post<ActionItem>(`${this.baseUrl}/${id}/complete`, {});
  }
}
