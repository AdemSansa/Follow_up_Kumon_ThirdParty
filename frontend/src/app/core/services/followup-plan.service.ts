import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';
import { PageResponse } from '../models/page.model';
import { FollowUpPlan, FollowUpPlanRequest } from '../models/followup-plan.model';

export interface FollowUpPlanListParams {
  studentId?: string;
  active?: boolean;
  assignedStaffId?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class FollowUpPlanService {
  private readonly baseUrl = `${API_BASE_URL}/follow-up-plans`;

  constructor(private readonly http: HttpClient) {}

  list(params: FollowUpPlanListParams = {}): Observable<PageResponse<FollowUpPlan>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<FollowUpPlan>>(this.baseUrl, { params: httpParams });
  }

  get(id: string): Observable<FollowUpPlan> {
    return this.http.get<FollowUpPlan>(`${this.baseUrl}/${id}`);
  }

  create(request: FollowUpPlanRequest): Observable<FollowUpPlan> {
    return this.http.post<FollowUpPlan>(this.baseUrl, request);
  }

  update(id: string, request: FollowUpPlanRequest): Observable<FollowUpPlan> {
    return this.http.put<FollowUpPlan>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
