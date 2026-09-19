import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';
import { PageResponse } from '../models/page.model';
import { CompleteFollowUpRequest, FollowUp, FollowUpStatus, RescheduleRequest } from '../models/followup.model';

export interface FollowUpListParams {
  studentId?: string;
  followUpPlanId?: string;
  status?: FollowUpStatus | '';
  assignedStaffId?: string;
  overdue?: boolean;
  /** ISO date (YYYY-MM-DD) — inclusive calendar-day bounds. */
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class FollowUpService {
  private readonly baseUrl = `${API_BASE_URL}/follow-ups`;

  constructor(private readonly http: HttpClient) {}

  list(params: FollowUpListParams = {}): Observable<PageResponse<FollowUp>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<FollowUp>>(this.baseUrl, { params: httpParams });
  }

  get(id: string): Observable<FollowUp> {
    return this.http.get<FollowUp>(`${this.baseUrl}/${id}`);
  }

  reschedule(id: string, request: RescheduleRequest): Observable<FollowUp> {
    return this.http.post<FollowUp>(`${this.baseUrl}/${id}/reschedule`, request);
  }

  complete(id: string, request: CompleteFollowUpRequest): Observable<FollowUp> {
    return this.http.post<FollowUp>(`${this.baseUrl}/${id}/complete`, request);
  }

  cancel(id: string): Observable<FollowUp> {
    return this.http.post<FollowUp>(`${this.baseUrl}/${id}/cancel`, {});
  }
}
