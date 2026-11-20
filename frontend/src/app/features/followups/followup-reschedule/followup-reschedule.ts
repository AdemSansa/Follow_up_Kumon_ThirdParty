import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FollowUpService } from '../../../core/services/followup.service';
import { ApiError } from '../../../core/models/api-error.model';
import { UiFeedback } from '../../../core/services/ui-feedback.service';
import { BackNavigation } from '../../../core/services/back-navigation.service';
import { UnsavedChangesAware } from '../../../shared/unsaved-changes';

@Component({
  selector: 'app-followup-reschedule',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './followup-reschedule.html',
  styleUrl: './followup-reschedule.scss',
})
export class FollowUpReschedule extends UnsavedChangesAware {
  private readonly fb = inject(FormBuilder);
  private readonly followUpService = inject(FollowUpService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ui = inject(UiFeedback);
  private readonly backNav = inject(BackNavigation);

  private readonly followUpId = this.route.snapshot.paramMap.get('id')!;

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly studentName = signal('');
  readonly studentId = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    scheduledStart: ['', Validators.required],
    scheduledEnd: ['', Validators.required],
  });

  /** Set after a successful save, so leaving afterwards doesn't warn. */
  private saved = false;

  /** Unsaved edits on this page? (used by unsavedChangesGuard and the tab-close warning) */
  hasUnsavedChanges(): boolean {
    return !this.saved && !this.saving() && this.form.dirty;
  }

  constructor() {
    super();
    this.followUpService.get(this.followUpId).subscribe((followUp) => {
      this.studentName.set(`${followUp.student.firstName} ${followUp.student.lastName}`);
      this.studentId.set(followUp.student.id);
      this.form.patchValue({
        scheduledStart: followUp.scheduledStart.slice(0, 16),
        scheduledEnd: followUp.scheduledEnd.slice(0, 16),
      });
      this.loading.set(false);
    });
  }

  get cancelLink(): string[] {
    return this.studentId() ? ['/students', this.studentId()!] : ['/students'];
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.followUpService
      .reschedule(this.followUpId, { scheduledStart: raw.scheduledStart, scheduledEnd: raw.scheduledEnd })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.saved = true;
          const when = new Date(raw.scheduledStart).toLocaleString(undefined, {
            weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
          });
          this.ui.success(`Check-in moved to ${when}`);
          this.backNav.back(this.cancelLink);
        },
        error: (err: HttpErrorResponse) => {
          this.saving.set(false);
          const apiError = err.error as ApiError | undefined;
          const message = apiError?.message ?? 'Could not reschedule this follow-up.';
          this.errorMessage.set(message);
          this.ui.error(message);
        },
      });
  }
}
