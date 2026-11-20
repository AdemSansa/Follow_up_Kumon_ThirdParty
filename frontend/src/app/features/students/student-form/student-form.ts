import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { MatChipsModule } from '@angular/material/chips';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
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
import { ActionItemService } from '../../../core/services/action-item.service';
import { ActionItem } from '../../../core/models/action-item.model';
import { STUDENT_STATUSES, SUBJECTS } from '../../../core/models/student.model';
import { Parent } from '../../../core/models/parent.model';
import { FollowUpPlan } from '../../../core/models/followup-plan.model';
import { FollowUp } from '../../../core/models/followup.model';
import { ApiError } from '../../../core/models/api-error.model';
import { UiFeedback } from '../../../core/services/ui-feedback.service';
import { BackNavigation } from '../../../core/services/back-navigation.service';
import { UnsavedChangesAware } from '../../../shared/unsaved-changes';

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
    MatChipsModule,
    MatAutocompleteModule,
  ],
  templateUrl: './student-form.html',
  styleUrl: './student-form.scss',
})
export class StudentForm extends UnsavedChangesAware {
  private readonly fb = inject(FormBuilder);
  private readonly studentService = inject(StudentService);
  private readonly parentService = inject(ParentService);
  private readonly planService = inject(FollowUpPlanService);
  private readonly followUpService = inject(FollowUpService);
  private readonly actionItemService = inject(ActionItemService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ui = inject(UiFeedback);
  private readonly backNav = inject(BackNavigation);

  readonly statuses = STUDENT_STATUSES;
  readonly subjects = SUBJECTS;
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly followUpPlans = signal<FollowUpPlan[]>([]);
  readonly loadingPlans = signal(false);
  readonly upcomingFollowUps = signal<FollowUp[]>([]);
  readonly loadingFollowUps = signal(false);
  readonly actionItems = signal<ActionItem[]>([]);

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

  /** Set after a successful save, so leaving afterwards doesn't warn. */
  private saved = false;

  /** Unsaved edits on this page? (used by unsavedChangesGuard and the tab-close warning) */
  hasUnsavedChanges(): boolean {
    return !this.saved && !this.saving() && this.form.dirty;
  }

  // --- Parent search (type to find, pick from suggestions, shown as chips) ---------------

  /** Parents linked to this student (chips). Kept in sync with the parentIds form control. */
  readonly selectedParents = signal<Parent[]>([]);
  /** What's typed in the parent search box (a Parent object briefly, right after picking one). */
  readonly parentSearch = new FormControl<string | Parent>('', { nonNullable: true });
  readonly parentSuggestions = signal<Parent[]>([]);
  readonly searchingParents = signal(false);
  readonly parentQuery = signal('');

  private readonly parentSearchSub = this.parentSearch.valueChanges
    .pipe(
      map((v) => (typeof v === 'string' ? v.trim() : '')),
      debounceTime(250),
      distinctUntilChanged(),
      tap((q) => {
        this.parentQuery.set(q);
        this.searchingParents.set(true);
      }),
      switchMap((q) =>
        this.parentService.list({ search: q || undefined, size: 8, sort: 'lastName' }).pipe(
          map((page) => page.content),
          catchError(() => of([] as Parent[])),
        ),
      ),
      takeUntilDestroyed(),
    )
    .subscribe((parents) => {
      const chosen = new Set(this.selectedParents().map((p) => p.id));
      this.parentSuggestions.set(parents.filter((p) => !chosen.has(p.id)));
      this.searchingParents.set(false);
    });

  /** Show some suggestions as soon as the box is focused, before typing. */
  openParentSuggestions(): void {
    if (!this.parentQuery() && this.parentSuggestions().length === 0) {
      this.searchingParents.set(true);
      this.parentService.list({ size: 8, sort: 'lastName' }).subscribe({
        next: (page) => {
          const chosen = new Set(this.selectedParents().map((p) => p.id));
          this.parentSuggestions.set(page.content.filter((p) => !chosen.has(p.id)));
          this.searchingParents.set(false);
        },
        error: () => this.searchingParents.set(false),
      });
    }
  }

  addParent(parent: Parent, input: HTMLInputElement): void {
    if (!this.selectedParents().some((p) => p.id === parent.id)) {
      this.selectedParents.update((list) => [...list, parent]);
      this.syncParentIds();
    }
    input.value = '';
    this.parentSearch.setValue('');
    this.parentSuggestions.update((list) => list.filter((p) => p.id !== parent.id));
  }

  removeParent(parent: Parent): void {
    this.selectedParents.update((list) => list.filter((p) => p.id !== parent.id));
    this.syncParentIds();
  }

  private syncParentIds(): void {
    const control = this.form.controls.parentIds;
    control.setValue(this.selectedParents().map((p) => p.id));
    control.markAsDirty();
  }

  constructor() {
    super();

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
        this.selectedParents.set(student.parents);
        this.loading.set(false);
      });
      this.loadFollowUpPlans();
      this.loadUpcomingFollowUps();
      this.loadActionItems();
    } else if (this.presetParentId) {
      this.form.patchValue({ parentIds: [this.presetParentId] });
      this.parentService.get(this.presetParentId).subscribe((parent) => this.selectedParents.set([parent]));
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
    if (!plan.active) {
      this.savePlanActive(plan); // resuming is harmless — no confirmation needed
      return;
    }
    this.ui
      .confirm({
        title: 'Pause this follow-up plan?',
        message:
          'No new check-ins will be scheduled for this plan until you resume it. ' +
          'Check-ins already on the calendar and past history are kept.',
        confirmText: 'Pause plan',
        danger: true,
        icon: 'pause_circle',
      })
      .subscribe((ok) => ok && this.savePlanActive(plan));
  }

  private savePlanActive(plan: FollowUpPlan): void {
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
      occurrenceCount: plan.occurrenceCount, // keep the check-in limit when pausing/resuming
    };
    this.planService.update(plan.id, request).subscribe({
      next: () => {
        this.ui.success(plan.active ? 'Plan paused' : 'Plan resumed');
        this.loadFollowUpPlans();
        this.loadUpcomingFollowUps();
      },
      error: (err: HttpErrorResponse) =>
        this.ui.error((err.error as ApiError | undefined)?.message ?? 'Could not update this plan.'),
    });
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

  private loadActionItems(): void {
    if (!this.studentId) return;
    this.actionItemService
      .list({ studentId: this.studentId, size: 50, sort: 'createdAt,desc' })
      .subscribe((page) => this.actionItems.set(page.content));
  }

  completeActionItem(item: ActionItem): void {
    this.ui
      .confirm({
        title: 'Mark as done?',
        message: `"${item.title}" will be marked as done.`,
        confirmText: 'Mark done',
        icon: 'task_alt',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.actionItemService.complete(item.id).subscribe({
          next: () => {
            this.ui.success('Action item done');
            this.loadActionItems();
          },
          error: () => this.ui.error('Could not update this action item.'),
        });
      });
  }

  startCheckIn(followUp: FollowUp, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    this.router.navigate(['/follow-ups', followUp.id, 'checkin']);
  }

  cancelFollowUp(followUp: FollowUp, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    const when = new Date(followUp.scheduledStart).toLocaleString(undefined, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit',
    });
    this.ui
      .confirm({
        title: 'Cancel this check-in?',
        message:
          `The check-in on ${when} will be cancelled. It stays in the history as cancelled.` +
          (followUp.type === 'VIDEO_CALL'
            ? '\n\nIts Google Meet event is removed, and the parent is told if they already had the link.'
            : ''),
        confirmText: 'Cancel check-in',
        cancelText: 'Keep it',
        danger: true,
        icon: 'event_busy',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.followUpService.cancel(followUp.id).subscribe({
          next: () => {
            this.ui.success('Check-in cancelled');
            this.loadUpcomingFollowUps();
          },
          error: (err: HttpErrorResponse) =>
            this.ui.error((err.error as ApiError | undefined)?.message ?? 'Could not cancel this check-in.'),
        });
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
        this.saved = true;
        const name = `${student.firstName} ${student.lastName}`;
        if (!this.isEditMode && this.presetParentId) {
          // Parent-first registration: back to the parent's page to add the next child.
          this.ui.success(`${name} added`);
          this.router.navigate(['/parents', this.presetParentId]);
        } else if (!this.isEditMode) {
          // New student: open their page so a follow-up plan can be added right away.
          this.ui.success(`${name} added — you can now add a follow-up plan`);
          this.router.navigate(['/students', student.id]);
        } else {
          this.ui.success(`${name} saved`);
          this.backNav.back(['/students', student.id]);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        const apiError = err.error as ApiError | undefined;
        const message = apiError?.message ?? 'Could not save this student.';
        this.errorMessage.set(message);
        this.ui.error(message);
      },
    });
  }
}
