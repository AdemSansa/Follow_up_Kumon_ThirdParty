import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config';
import { AuthResponse, LoginRequest } from '../models/auth.model';

const STORAGE_KEY = 'followup.auth';

interface StoredAuth {
  accessToken: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'ADMIN' | 'STAFF';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authState = signal<StoredAuth | null>(this.readFromStorage());

  readonly currentUser = computed(() => this.authState());
  readonly isAuthenticated = computed(() => this.authState() !== null);
  readonly token = computed(() => this.authState()?.accessToken ?? null);

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/login`, request).pipe(
      tap((response) => this.persist(response)),
    );
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
      return raw ? (JSON.parse(raw) as StoredAuth) : null;
    } catch {
      return null;
    }
  }
}
