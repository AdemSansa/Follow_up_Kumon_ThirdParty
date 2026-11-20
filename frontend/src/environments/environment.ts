/**
 * Production build settings (`ng build`). The API is expected on the same
 * origin as the app, behind a reverse proxy that forwards /api to the
 * backend. Change apiBaseUrl if the backend lives on another host.
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api',
};
