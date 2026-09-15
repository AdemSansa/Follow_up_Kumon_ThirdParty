import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { Shell } from './layout/shell/shell';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'students',
        loadComponent: () => import('./features/students/student-list/student-list').then((m) => m.StudentList),
      },
      {
        path: 'students/new',
        loadComponent: () => import('./features/students/student-form/student-form').then((m) => m.StudentForm),
      },
      {
        path: 'students/:id',
        loadComponent: () => import('./features/students/student-form/student-form').then((m) => m.StudentForm),
      },
      {
        path: 'parents',
        loadComponent: () => import('./features/parents/parent-list/parent-list').then((m) => m.ParentList),
      },
      {
        path: 'parents/new',
        loadComponent: () => import('./features/parents/parent-form/parent-form').then((m) => m.ParentForm),
      },
      {
        path: 'parents/:id',
        loadComponent: () => import('./features/parents/parent-form/parent-form').then((m) => m.ParentForm),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
