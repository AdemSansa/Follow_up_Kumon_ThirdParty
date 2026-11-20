import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { StudentService } from '../../../core/services/student.service';
import { AuthService } from '../../../core/services/auth.service';
import { UiFeedback } from '../../../core/services/ui-feedback.service';
import { StudentAvatar } from '../../../shared/components/student-avatar/student-avatar';
import { ApiError } from '../../../core/models/api-error.model';
import { Student, STUDENT_STATUSES, StudentStatus, SUBJECTS, Subject } from '../../../core/models/student.model';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    StudentAvatar,
  ],
  templateUrl: './student-list.html',
  styleUrl: './student-list.scss',
})
export class StudentList {
  private readonly studentService = inject(StudentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly displayedColumns = ['name', 'status', 'subjects', 'level', 'enrollmentDate', 'actions'];
  readonly statuses = STUDENT_STATUSES;
  readonly subjects = SUBJECTS;

  readonly students = signal<Student[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Deleting is ADMIN-only on the backend; staff don't see the button. */
  readonly isAdmin = inject(AuthService).isAdmin;
  private readonly ui = inject(UiFeedback);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<StudentStatus | ''>('', { nonNullable: true });
  readonly subjectControl = new FormControl<Subject | ''>('', { nonNullable: true });

  constructor() {
    this.load();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.onFilterChange());

    this.statusControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.onFilterChange());
    this.subjectControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.onFilterChange());
  }

  load(): void {
    this.loading.set(true);
    this.studentService
      .list({
        search: this.searchControl.value || undefined,
        status: this.statusControl.value || undefined,
        subject: this.subjectControl.value || undefined,
        page: this.pageIndex(),
        size: this.pageSize(),
      })
      .subscribe({
        next: (page) => {
          this.students.set(page.content);
          this.totalElements.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  onFilterChange(): void {
    this.pageIndex.set(0);
    this.load();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  deleteStudent(student: Student, event: Event): void {
    event.stopPropagation();
    const name = `${student.firstName} ${student.lastName}`;
    this.ui
      .confirm({
        title: 'Delete student?',
        message: `${name} will be permanently removed. This cannot be undone.\n\nStudents with check-in history can't be deleted — set their status to Withdrawn instead.`,
        confirmText: 'Delete',
        danger: true,
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.errorMessage.set(null);
        this.studentService.delete(student.id).subscribe({
          next: () => {
            this.ui.success(`${name} deleted`);
            this.load();
          },
          error: (err: HttpErrorResponse) => {
            // e.g. 400 when the student has follow-up history (withdraw them instead).
            const apiError = err.error as ApiError | undefined;
            const message = apiError?.message ?? 'Could not delete this student.';
            this.errorMessage.set(message);
            this.ui.error(message);
          },
        });
      });
  }
}
