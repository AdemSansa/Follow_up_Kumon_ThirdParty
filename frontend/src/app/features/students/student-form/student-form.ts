import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { StudentService } from '../../../core/services/student.service';
import { ParentService } from '../../../core/services/parent.service';
import { FollowUpPlanService } from '../../../core/services/followup-plan.service';
import { FollowUpService } from '../../../core/services/followup.service';
import { STUDENT_STATUSES, SUBJECTS } from '../../../core/models/student.model';
import { Parent } from '../../../core/models/parent.model';
import { FollowUpPlan } from '../../../core/models/followup-plan.model';
import { FollowUp } from '../../../core/models/followup.model';
import { ApiError } from '../../../core/models/api-error.model';

function toIsoDate(value: Date | string | null): string | null {
  if (!value) return null;
  if (typeof value === 'string') return value;
  return value.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-student-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatListModule,
  ],
  templateUrl: './student-form.html',
  styleUrl: './student-form.scss',
})
export class StudentForm {
  private readonly fb = inject(FormBuilder);
  private readonly studentService = inject(StudentService);
  private readonly parentService = inject(ParentService);
  private readonly planService = inject(FollowUpPlanService);
  private readonly followUpService = inject(FollowUpService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly statuses = STUDENT_STATUSES;
  readonly subjects = SUBJECTS;
  readonly availableParents = signal<Parent[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly followUpPlans = signal<FollowUpPlan[]>([]);
  readonly loadingPlans = signal(false);
  readonly upcomingFollowUps = signal<FollowUp[]>([]);
  readonly loadingFollowUps = signal(false);

  readonly studentId = this.route.snapshot.paramMap.get('id');
  readonly isEditMode = this.studentId !== null && this.studentId !== 'new';

  /**
   * Set when arriving via a parent's "Add child" action
   * (/students/new?parentId=...) — the parent-first registration flow.
   * Pre-links the new student to that parent and returns to the parent's
   * page after saving, instead of the new student's own page.
   */
  private readonly presetParentId = this.route.snapshot.queryParamMap.get('parentId');

  get cancelLink(): string[] {
    if (this.isEditMode && this.studentId) {
      return ['/students', this.studentId];
    }
    if (this.presetParentId) {
      return ['/parents', this.presetParentId];
    }
    return ['/students'];
  }

  readonly form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    dateOfBirth: this.fb.control<Date | null>(null),
    gender: [''],
    phone: [''],
    email: [''],
    address: [''],
    enrollmentDate: this.fb.control<Date | null>(new Date(), Validators.required),
    status: this.fb.nonNullable.control<(typeof STUDENT_STATUSES)[number]>('ACTIVE'),
    level: [''],
    subjects: this.fb.nonNullable.control<string[]>([]),
    parentIds: this.fb.nonNullable.control<string[]>([]),
    notes: [''],
  });

  constructor() {
    this.parentService.list({ size: 200 }).subscribe((page) => this.availableParents.set(page.content));

    if (this.isEditMode && this.studentId) {
      this.loading.set(true);
      this.studentService.get(this.studentId).subscribe((student) => {
        this.form.patchValue({
          firstName: student.firstName,
          lastName: student.lastName,
          dateOfBirth: student.dateOfBirth ? new Date(student.dateOfBirth) : null,
          gender: student.gender ?? '',
          phone: student.phone ?? '',
          email: student.email ?? '',
          address: student.address ?? '',
          enrollmentDate: new Date(student.enrollmentDate),
          status: student.status,
          level: student.level ?? '',
          subjects: student.subjects,
          parentIds: student.parents.map((p) => p.id),
          notes: student.notes ?? '',
        });
        this.loading.set(false);
      });
      this.loadFollowUpPlans();
      this.loadUpcomingFollowUps();
    } else if (this.presetParentId) {
      this.form.patchValue({ parentIds: [this.presetParentId] });
    }
  }

  private loadFollowUpPlans(): void {
    if (!this.studentId) return;
    this.loadingPlans.set(true);
    this.planService.list({ studentId: this.studentId, size: 50 }).subscribe({
      next: (page) => {
        this.followUpPlans.set(page.content);
        this.loadingPlans.set(false);
      },
      error: () => this.loadingPlans.set(false),
    });
  }

  /**
   * Plans with existing follow-ups (i.e. any plan once it's generated at
   * least one occurrence) can't be hard-deleted — the backend rejects that
   * to protect history. Pausing (active=false) is the supported way to
   * stop a plan; use the plan's own edit page to fully delete one that
   * genuinely has no follow-ups yet.
   */
  togglePlanActive(plan: FollowUpPlan, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    const request = {
      studentId: plan.student.id,
      parentId: plan.parent?.id ?? null,
      assignedStaffId: plan.assignedStaffId,
      frequencyType: plan.frequencyType,
      customIntervalValue: plan.customIntervalValue,
      customIntervalUnit: plan.customIntervalUnit,
      startDate: plan.startDate,
      preferredDay: plan.preferredDay,
      preferredTime: plan.preferredTime,
      meetingDurationMinutes: plan.meetingDurationMinutes,
      location: plan.location,
      meetingType: plan.meetingType,
      reminderOffsets: plan.reminderOffsets,
      active: !plan.active,
    };
    this.planService.update(plan.id, request).subscribe(() => this.loadFollowUpPlans());
  }

  private loadUpcomingFollowUps(): void {
    if (!this.studentId) return;
    this.loadingFollowUps.set(true);
    this.followUpService.list({ studentId: this.studentId, size: 10, sort: 'scheduledStart,asc' }).subscribe({
      next: (page) => {
        this.upcomingFollowUps.set(page.content);
        this.loadingFollowUps.set(false);
      },
      error: () => this.loadingFollowUps.set(false),
    });
  }

  cancelFollowUp(followUp: FollowUp, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    if (!confirm('Cancel this follow-up occurrence?')) {
      return;
    }
    this.followUpService.cancel(followUp.id).subscribe(() => this.loadUpcomingFollowUps());
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
      dateOfBirth: toIsoDate(raw.dateOfBirth),
      gender: raw.gender || null,
      phone: raw.phone || null,
      email: raw.email || null,
      address: raw.address || null,
      enrollmentDate: toIsoDate(raw.enrollmentDate)!,
      status: raw.status,
      level: raw.level || null,
      subjects: raw.subjects as ('MATH' | 'ENGLISH')[],
      parentIds: raw.parentIds,
      notes: raw.notes || null,
    };

    const save$ =
      this.isEditMode && this.studentId
        ? this.studentService.update(this.studentId, request)
        : this.studentService.create(request);

    save$.subscribe({
      next: (student) => {
        this.saving.set(false);
        if (!this.isEditMode && this.presetParentId) {
          this.router.navigate(['/parents', this.presetParentId]);
        } else {
          this.router.navigate(['/students', student.id]);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        const apiError = err.error as ApiError | undefined;
        this.errorMessage.set(apiError?.message ?? 'Could not save this student.');
      },
    });
  }
}
