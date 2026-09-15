import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';
import { PageResponse } from '../models/page.model';
import { Student, StudentRequest, StudentStatus, Subject } from '../models/student.model';

export interface StudentListParams {
  status?: StudentStatus | '';
  subject?: Subject | '';
  assignedTeacherId?: string;
  parentId?: string;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly baseUrl = `${API_BASE_URL}/students`;

  constructor(private readonly http: HttpClient) {}

  list(params: StudentListParams = {}): Observable<PageResponse<Student>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<Student>>(this.baseUrl, { params: httpParams });
  }

  get(id: string): Observable<Student> {
    return this.http.get<Student>(`${this.baseUrl}/${id}`);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>(this.baseUrl, request);
  }

  update(id: string, request: StudentRequest): Observable<Student> {
    return this.http.put<Student>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
