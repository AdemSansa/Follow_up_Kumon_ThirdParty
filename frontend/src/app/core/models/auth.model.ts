export type Role = 'ADMIN' | 'STAFF';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  expiresInSeconds: number;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}
