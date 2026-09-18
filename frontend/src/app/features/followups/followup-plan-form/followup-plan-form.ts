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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FollowUpPlanService } from '../../../core/services/followup-plan.service';
import { StudentService } from '../../../core/services/student.service';
import {
  DAYS_OF_WEEK,
  FREQUENCY_TYPES,
  INTERVAL_UNITS,
  MEETING_TYPES,
  REMINDER_OFFSETS,
} from '../../../core/models/followup-plan.model';
import { Parent } from '../../../core/models/parent.model';
import { ApiError } from '../../../core/models/api-error.model';

function toIsoDate(value: Date | string | null): string | null {
  if (!value) return null;
  if (typeof value === 'string') return value;
  return value.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-followup-plan-form',
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
    MatSlideToggleModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './followup-plan-form.html',
  styleUrl: './followup-plan-form.scss',
})
export class FollowUpPlanForm {
  private readonly fb = inject(FormBuilder);
  private readonly planService = inject(FollowUpPlanService);
  private readonly studentService = inject(StudentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly frequencyTypes = FREQUENCY_TYPES;
  readonly intervalUnits = INTERVAL_UNITS;
  readonly meetingTypes = MEETING_TYPES;
  readonly reminderOffsets = REMINDER_OFFSETS;
  readonly daysOfWeek = DAYS_OF_WEEK;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly studentName = signal<string>('');
  readonly availableParents = signal<Parent[]>([]);

  readonly planId = this.route.snapshot.paramMap.get('id');
  readonly isEditMode = this.planId !== null && this.planId !== 'new';
  private readonly presetStudentId = this.route.snapshot.queryParamMap.get('studentId');
  private studentId = this.presetStudentId;

  readonly form = this.fb.nonNullable.group({
    parentId: this.fb.control<string | null>(null),
    frequencyType: this.fb.nonNullable.control<(typeof FREQUENCY_TYPES)[number]>('MONTHLY', Validators.required),
    customIntervalValue: this.fb.control<number | null>(null),
    customIntervalUnit: this.fb.control<(typeof INTERVAL_UNITS)[number] | null>(null),
    startDate: this.fb.control<Date | null>(new Date(), Validators.required),
    preferredDay: this.fb.control<(typeof DAYS_OF_WEEK)[number] | null>(null),
    preferredTime: [''],
    meetingDurationMinutes: this.fb.nonNullable.control(30, [Validators.required, Validators.min(5)]),
    location: [''],
    meetingType: this.fb.nonNullable.control<(typeof MEETING_TYPES)[number]>('PARENT_MEETING', Validators.required),
    reminderOffsets: this.fb.nonNullable.control<string[]>(['TWENTY_FOUR_HOURS_BEFORE']),
    active: this.fb.nonNullable.control(true),
  });

  constructor() {
    if (this.isEditMode && this.planId) {
      this.loading.set(true);
      this.planService.get(this.planId).subscribe((plan) => {
        this.studentId = plan.student.id;
        this.studentName.set(`${plan.student.firstName} ${plan.student.lastName}`);
        this.loadParents(plan.student.id);
        this.form.patchValue({
          parentId: plan.parent?.id ?? null,
          frequencyType: plan.frequencyType,
          customIntervalValue: plan.customIntervalValue,
          customIntervalUnit: plan.customIntervalUnit,
          startDate: new Date(plan.startDate),
          preferredDay: plan.preferredDay,
          preferredTime: plan.preferredTime ? plan.preferredTime.slice(0, 5) : '',
          meetingDurationMinutes: plan.meetingDurationMinutes,
          location: plan.location ?? '',
          meetingType: plan.meetingType,
          reminderOffsets: plan.reminderOffsets,
          active: plan.active,
        });
        this.loading.set(false);
      });
    } else if (this.presetStudentId) {
      this.loading.set(true);
      this.studentService.get(this.presetStudentId).subscribe((student) => {
        this.studentName.set(`${student.firstName} ${student.lastName}`);
        this.availableParents.set(student.parents);
        this.loading.set(false);
      });
    }
  }

  private loadParents(studentId: string): void {
    this.studentService.get(studentId).subscribe((student) => this.availableParents.set(student.parents));
  }

  get isCustomFrequency(): boolean {
    return this.form.controls.frequencyType.value === 'CUSTOM';
  }

  get cancelLink(): string[] {
    return this.studentId ? ['/students', this.studentId] : ['/students'];
  }

  submit(): void {
    if (!this.studentId) {
      this.errorMessage.set('No student context — open this from a student\'s page.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const request = {
      studentId: this.studentId,
      parentId: raw.parentId || null,
      frequencyType: raw.frequencyType,
      customIntervalValue: raw.frequencyType === 'CUSTOM' ? raw.customIntervalValue : null,
      customIntervalUnit: raw.frequencyType === 'CUSTOM' ? raw.customIntervalUnit : null,
      startDate: toIsoDate(raw.startDate)!,
      preferredDay: raw.preferredDay || null,
      preferredTime: raw.preferredTime ? `${raw.preferredTime}:00` : null,
      meetingDurationMinutes: raw.meetingDurationMinutes,
      location: raw.location || null,
      meetingType: raw.meetingType,
      reminderOffsets: raw.reminderOffsets as never,
      active: raw.active,
    };

    const save$ =
      this.isEditMode && this.planId
        ? this.planService.update(this.planId, request)
        : this.planService.create(request);

    save$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/students', this.studentId]);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        const apiError = err.error as ApiError | undefined;
        this.errorMessage.set(apiError?.message ?? 'Could not save this follow-up plan.');
      },
    });
  }
}
