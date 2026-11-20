import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';

export interface GoogleAccountView {
  googleEmail: string;
  connectedAt: string;
  /** Set when Google stopped accepting the connection — reconnect. */
  lastError: string | null;
}

export interface GoogleAccounts {
  /** Server has a Google OAuth client set up. */
  configured: boolean;
  configMessage: string;
  /** Shared account that hosts every meeting by default. */
  center: GoogleAccountView | null;
  /** The signed-in user's own account (optional). */
  mine: GoogleAccountView | null;
}

@Injectable({ providedIn: 'root' })
export class GoogleAccountService {
  private readonly baseUrl = `${API_BASE_URL}/google/accounts`;

  constructor(private readonly http: HttpClient) {}

  get(): Observable<GoogleAccounts> {
    return this.http.get<GoogleAccounts>(this.baseUrl);
  }

  /** Returns Google's consent URL; the caller sends the browser there. */
  connectUrl(kind: 'CENTER' | 'PERSONAL'): Observable<{ url: string }> {
    return this.http.post<{ url: string }>(`${this.baseUrl}/connect`, {}, { params: new HttpParams().set('kind', kind) });
  }

  disconnect(which: 'center' | 'mine'): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${which}`);
  }
}
