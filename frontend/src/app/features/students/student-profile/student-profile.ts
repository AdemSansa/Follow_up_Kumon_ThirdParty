import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription, catchError, forkJoin, of } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { StudentService } from '../../../core/services/student.service';
import { FollowUpPlanService } from '../../../core/services/followup-plan.service';
import { FollowUpService } from '../../../core/services/followup.service';
import { ActionItemService } from '../../../core/services/action-item.service';
import { UiFeedback } from '../../../core/services/ui-feedback.service';
import { StudentPhotoCache } from '../../../core/services/student-photo-cache.service';
import { Student } from '../../../core/models/student.model';
import { FollowUpPlan } from '../../../core/models/followup-plan.model';
import { FollowUp } from '../../../core/models/followup.model';
import { ActionItem } from '../../../core/models/action-item.model';
import { ApiError } from '../../../core/models/api-error.model';

const OPEN = new Set(['SCHEDULED', 'CONFIRMED', 'RESCHEDULED']);

/** Read-only student profile: photo, details, family, plans, upcoming + past check-ins, action items. */
@Component({
  selector: 'app-student-profile',
  standalone: true,
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  templateUrl: './student-profile.html',
  styleUrl: './student-profile.scss',
})
export class StudentProfile implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly students = inject(StudentService);
  private readonly plansApi = inject(FollowUpPlanService);
  private readonly followUpsApi = inject(FollowUpService);
  private readonly actionsApi = inject(ActionItemService);
  private readonly ui = inject(UiFeedback);
  private readonly photos = inject(StudentPhotoCache);

  readonly studentId = this.route.snapshot.paramMap.get('id')!;

  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly student = signal<Student | null>(null);
  readonly plans = signal<FollowUpPlan[]>([]);
  readonly followUps = signal<FollowUp[]>([]);
  readonly actions = signal<ActionItem[]>([]);
  readonly photoUrl = signal<string | null>(null);
  readonly uploading = signal(false);
  readonly showAllHistory = signal(false);

  readonly initials = computed(() => {
    const s = this.student();
    return s ? `${s.firstName.charAt(0)}${s.lastName.startsWith('(') ? '' : s.lastName.charAt(0)}`.toUpperCase() : '';
  });

  /** Open check-ins (incl. overdue ones still waiting), soonest first. */
  readonly upcoming = computed(() =>
    this.followUps()
      .filter((f) => OPEN.has(f.status))
      .sort((a, b) => a.scheduledStart.localeCompare(b.scheduledStart)),
  );

  /** Completed / cancelled / missed, most recent first. */
  readonly history = computed(() =>
    this.followUps()
      .filter((f) => !OPEN.has(f.status))
      .sort((a, b) => b.scheduledStart.localeCompare(a.scheduledStart)),
  );

  readonly visibleHistory = computed(() => (this.showAllHistory() ? this.history() : this.history().slice(0, 5)));
  readonly openActions = computed(() => this.actions().filter((a) => a.status === 'TODO' || a.status === 'IN_PROGRESS'));
  readonly doneActions = computed(() => this.actions().filter((a) => a.status === 'DONE'));

  readonly stats = computed(() => {
    const completed = this.followUps().filter((f) => f.status === 'COMPLETED');
    const rated = completed.filter((f) => f.progressRating !== null);
    const avg = (pick: (f: FollowUp) => number | null) =>
      rated.length ? (rated.reduce((sum, f) => sum + (pick(f) ?? 0), 0) / rated.length).toFixed(1) : '—';
    const next = this.upcoming().find((f) => !f.overdue);
    return {
      completed: completed.length,
      overdue: this.upcoming().filter((f) => f.overdue).length,
      next: next?.scheduledStart ?? null,
      lastCompleted: completed.sort((a, b) => b.scheduledStart.localeCompare(a.scheduledStart))[0]?.scheduledStart ?? null,
      avgProgress: avg((f) => f.progressRating),
      avgEngagement: avg((f) => f.engagementRating),
      avgSatisfaction: avg((f) => f.parentSatisfactionRating),
    };
  });

  /** "1 year 3 months" since enrollment. */
  readonly tenure = computed(() => {
    const s = this.student();
    return s ? this.span(s.enrollmentDate) : '';
  });

  readonly age = computed(() => {
    const dob = this.student()?.dateOfBirth;
    if (!dob) return null;
    const d = new Date(dob);
    const now = new Date();
    let years = now.getFullYear() - d.getFullYear();
    if (now.getMonth() < d.getMonth() || (now.getMonth() === d.getMonth() && now.getDate() < d.getDate())) years--;
    return years;
  });

  constructor() {
    this.load();
  }

  ngOnDestroy(): void {
    this.photoSub?.unsubscribe();
  }

  load(): void {
    this.loading.set(true);
    forkJoin({
      student: this.students.get(this.studentId),
      plans: this.plansApi.list({ studentId: this.studentId, size: 50 }).pipe(catchError(() => of(null))),
      followUps: this.followUpsApi
        .list({ studentId: this.studentId, size: 500, sort: 'scheduledStart,asc' })
        .pipe(catchError(() => of(null))),
      actions: this.actionsApi
        .list({ studentId: this.studentId, size: 100, sort: 'createdAt,desc' })
        .pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ student, plans, followUps, actions }) => {
        this.student.set(student);
        this.showPhoto(student);
        this.plans.set(plans?.content ?? []);
        this.followUps.set(followUps?.content ?? []);
        this.actions.set(actions?.content ?? []);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.notFound.set(true);
        } else {
          this.ui.error('Could not load this student.');
        }
      },
    });
  }

  // --- photo -------------------------------------------------------------

  private photoSub?: Subscription;

  /** Only downloads when the student has a photo (photoUpdatedAt set); otherwise initials are shown. */
  private showPhoto(student: Student): void {
    this.photoSub?.unsubscribe();
    this.photoSub = this.photos.url(student.id, student.photoUpdatedAt).subscribe((url) => this.photoUrl.set(url));
  }

  onPhotoPicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // allow picking the same file again
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      this.ui.error('Please choose a JPEG, PNG or WebP image.');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.ui.error('That image is larger than 2 MB — please choose a smaller one.');
      return;
    }
    this.uploading.set(true);
    this.students.uploadPhoto(this.studentId, file).subscribe({
      next: () => {
        this.uploading.set(false);
        this.ui.success('Photo updated');
        this.photos.forget(this.studentId);
        this.load(); // picks up the new photoUpdatedAt → fresh image here and in the list
      },
      error: (err: HttpErrorResponse) => {
        this.uploading.set(false);
        this.ui.error((err.error as ApiError | undefined)?.message ?? 'Could not upload the photo.');
      },
    });
  }

  removePhoto(): void {
    this.ui
      .confirm({
        title: 'Remove the photo?',
        message: 'The profile picture will be deleted. Initials are shown instead.',
        confirmText: 'Remove',
        danger: true,
        icon: 'hide_image',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.students.deletePhoto(this.studentId).subscribe({
          next: () => {
            this.photos.forget(this.studentId);
            this.photoUrl.set(null);
            this.ui.success('Photo removed');
            this.load();
          },
          error: () => this.ui.error('Could not remove the photo.'),
        });
      });
  }

  // --- actions ---------------------------------------------------------------

  togglePlan(plan: FollowUpPlan): void {
    const pausing = plan.active;
    const run = () =>
      this.plansApi
        .update(plan.id, {
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
          occurrenceCount: plan.occurrenceCount,
        })
        .subscribe({
          next: () => {
            this.ui.success(pausing ? 'Plan paused' : 'Plan resumed');
            this.load();
          },
          error: (err: HttpErrorResponse) =>
            this.ui.error((err.error as ApiError | undefined)?.message ?? 'Could not update this plan.'),
        });
    if (!pausing) {
      run();
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
      .subscribe((ok) => ok && run());
  }

  cancelCheckIn(f: FollowUp): void {
    this.ui
      .confirm({
        title: 'Cancel this check-in?',
        message:
          `The check-in on ${new Date(f.scheduledStart).toLocaleString(undefined, {
            weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit',
          })} will be cancelled. It stays in the history as cancelled.` +
          (f.type === 'VIDEO_CALL'
            ? '\n\nIts Google Meet event is removed, and the parent is told if they already had the link.'
            : ''),
        confirmText: 'Cancel check-in',
        cancelText: 'Keep it',
        danger: true,
        icon: 'event_busy',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.followUpsApi.cancel(f.id).subscribe({
          next: () => {
            this.ui.success('Check-in cancelled');
            this.load();
          },
          error: (err: HttpErrorResponse) =>
            this.ui.error((err.error as ApiError | undefined)?.message ?? 'Could not cancel this check-in.'),
        });
      });
  }

  completeAction(item: ActionItem): void {
    this.ui
      .confirm({ title: 'Mark as done?', message: `"${item.title}" will be marked as done.`, confirmText: 'Mark done', icon: 'task_alt' })
      .subscribe((ok) => {
        if (!ok) return;
        this.actionsApi.complete(item.id).subscribe({
          next: () => {
            this.ui.success('Action item done');
            this.load();
          },
          error: () => this.ui.error('Could not update this action item.'),
        });
      });
  }

  // --- display helpers ---------------------------------------------------------

  label(value: string | null | undefined): string {
    if (!value) return '';
    const t = value.replaceAll('_', ' ').toLowerCase();
    return t.charAt(0).toUpperCase() + t.slice(1);
  }

  stars(n: number | null): string {
    return n === null ? '' : '★'.repeat(n) + '☆'.repeat(5 - n);
  }

  /** "Check-in 3 of 10" for plan occurrences, using the order of this student's follow-ups. */
  occurrence(f: FollowUp): string | null {
    if (!f.followUpPlanId) return null;
    const ofPlan = this.followUps().filter((x) => x.followUpPlanId === f.followUpPlanId);
    const index = ofPlan.findIndex((x) => x.id === f.id) + 1;
    const plan = this.plans().find((p) => p.id === f.followUpPlanId);
    return `Check-in ${index}${plan?.occurrenceCount ? ' of ' + plan.occurrenceCount : ''}`;
  }

  dialable(phone: string): string {
    return phone.trim().replace(/(?!^\+)[^\d]/g, '');
  }

  edit(): void {
    this.router.navigate(['/students', this.studentId, 'edit']);
  }

  private span(isoDate: string): string {
    const from = new Date(isoDate);
    const now = new Date();
    let months = (now.getFullYear() - from.getFullYear()) * 12 + now.getMonth() - from.getMonth();
    if (now.getDate() < from.getDate()) months--;
    if (months < 1) return 'less than a month';
    const y = Math.floor(months / 12);
    const m = months % 12;
    return [y ? `${y} year${y > 1 ? 's' : ''}` : '', m ? `${m} month${m > 1 ? 's' : ''}` : ''].filter(Boolean).join(' ');
  }
}
