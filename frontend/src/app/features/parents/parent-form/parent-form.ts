import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ParentService } from '../../../core/services/parent.service';
import { StudentService } from '../../../core/services/student.service';
import { ContactMethod } from '../../../core/models/parent.model';
import { Student } from '../../../core/models/student.model';
import { ApiError } from '../../../core/models/api-error.model';

const CONTACT_METHODS: ContactMethod[] = ['PHONE', 'EMAIL', 'SMS', 'WHATSAPP'];

@Component({
  selector: 'app-parent-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './parent-form.html',
  styleUrl: './parent-form.scss',
})
export class ParentForm {
  private readonly fb = inject(FormBuilder);
  private readonly parentService = inject(ParentService);
  private readonly studentService = inject(StudentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly contactMethods = CONTACT_METHODS;
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly children = signal<Student[]>([]);
  readonly loadingChildren = signal(false);

  readonly parentId = this.route.snapshot.paramMap.get('id');
  readonly isEditMode = this.parentId !== null && this.parentId !== 'new';

  readonly form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phone: [''],
    email: [''],
    relationship: ['', Validators.required],
    preferredContactMethod: this.fb.nonNullable.control<ContactMethod>('PHONE', Validators.required),
    notes: [''],
  });

  constructor() {
    if (this.isEditMode && this.parentId) {
      this.loading.set(true);
      this.parentService.get(this.parentId).subscribe((parent) => {
        this.form.patchValue({
          firstName: parent.firstName,
          lastName: parent.lastName,
          phone: parent.phone ?? '',
          email: parent.email ?? '',
          relationship: parent.relationship,
          preferredContactMethod: parent.preferredContactMethod,
          notes: parent.notes ?? '',
        });
        this.loading.set(false);
      });
      this.loadChildren();
    }
  }

  private loadChildren(): void {
    if (!this.parentId) return;
    this.loadingChildren.set(true);
    this.studentService.list({ parentId: this.parentId, size: 100 }).subscribe({
      next: (page) => {
        this.children.set(page.content);
        this.loadingChildren.set(false);
      },
      error: () => this.loadingChildren.set(false),
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const request = {
      firstName: raw.firstName,
      lastName: raw.lastName,
      phone: raw.phone || null,
      email: raw.email || null,
      relationship: raw.relationship,
      preferredContactMethod: raw.preferredContactMethod,
      notes: raw.notes || null,
    };

    const save$ =
      this.isEditMode && this.parentId
        ? this.parentService.update(this.parentId, request)
        : this.parentService.create(request);

    save$.subscribe({
      next: (parent) => {
        this.saving.set(false);
        this.router.navigate(['/parents', parent.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        const apiError = err.error as ApiError | undefined;
        this.errorMessage.set(apiError?.message ?? 'Could not save this parent.');
      },
    });
  }
}
