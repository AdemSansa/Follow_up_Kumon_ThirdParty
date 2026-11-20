import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, of, switchMap, throwError } from 'rxjs';
import { AUTH_REFRESH_URL, AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const send = (request: HttpRequest<unknown>, handler: HttpHandlerFn) => {
    const token = authService.token();
    const authorizedReq = token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
    return handler(authorizedReq);
  };

  // Near expiry: refresh first (one shared request), then send with the new
  // token. A failed refresh falls through and sends with the old token,
  // which is still valid until it actually expires.
  const ready$ =
    req.url !== AUTH_REFRESH_URL && authService.shouldRefresh()
      ? authService.refresh().pipe(catchError(() => of(null)))
      : of(null);

  return ready$.pipe(
    switchMap(() => send(req, next)),
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && authService.isAuthenticated()) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
