import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, finalize, shareReplay, tap } from 'rxjs';
import { API_BASE_URL } from '../config';
import { AuthResponse, LoginRequest } from '../models/auth.model';

const STORAGE_KEY = 'followup.auth';

/** Refresh the token once fewer than this many ms remain before it expires. */
const REFRESH_WINDOW_MS = 5 * 60 * 1000;

export const AUTH_REFRESH_URL = `${API_BASE_URL}/auth/refresh`;

interface StoredAuth {
  accessToken: string;
  /** Epoch ms when accessToken expires. Absent on sessions stored before this field existed. */
  expiresAt?: number;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'ADMIN' | 'STAFF';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authState = signal<StoredAuth | null>(this.readFromStorage());
  private refreshInFlight: Observable<AuthResponse> | null = null;

  readonly currentUser = computed(() => this.authState());
  readonly isAuthenticated = computed(() => this.authState() !== null);
  readonly isAdmin = computed(() => this.authState()?.role === 'ADMIN');
  readonly token = computed(() => this.authState()?.accessToken ?? null);

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/login`, request).pipe(
      tap((response) => this.persist(response)),
    );
  }

  /**
   * True when the current token is still valid but close to expiring. The
   * interceptor uses this to refresh on the next API call, so an active
   * user stays signed in while an idle tab still expires on schedule.
   */
  shouldRefresh(): boolean {
    const expiresAt = this.authState()?.expiresAt;
    if (!expiresAt) {
      return false;
    }
    const remaining = expiresAt - Date.now();
    return remaining > 0 && remaining < REFRESH_WINDOW_MS;
  }

  /** Exchanges the current (still valid) token for a fresh one. Concurrent callers share one request. */
  refresh(): Observable<AuthResponse> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.http.post<AuthResponse>(AUTH_REFRESH_URL, {}).pipe(
        tap((response) => this.persist(response)),
        finalize(() => (this.refreshInFlight = null)),
        shareReplay(1),
      );
    }
    return this.refreshInFlight;
  }

  logout(): void {
    const token = this.token();
    this.authState.set(null);
    localStorage.removeItem(STORAGE_KEY);
    if (token) {
      // Best-effort; stateless JWT has nothing to revoke server-side.
      this.http.post(`${API_BASE_URL}/auth/logout`, {}).subscribe({ error: () => undefined });
    }
  }

  private persist(response: AuthResponse): void {
    const stored: StoredAuth = {
      accessToken: response.accessToken,
      expiresAt: Date.now() + response.expiresInSeconds * 1000,
      userId: response.userId,
      email: response.email,
      firstName: response.firstName,
      lastName: response.lastName,
      role: response.role,
    };
    this.authState.set(stored);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  }

  private readFromStorage(): StoredAuth | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const stored = JSON.parse(raw) as StoredAuth;
      if (stored.expiresAt && stored.expiresAt <= Date.now()) {
        // Expired while the app was closed — start signed out instead of
        // showing the shell and bouncing on the first 401.
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return stored;
    } catch {
      return null;
    }
  }
}
