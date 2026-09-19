import { Component, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FollowUpService } from '../../core/services/followup.service';
import { CONCERNS, Concern, FollowUp } from '../../core/models/followup.model';
import { ApiError } from '../../core/models/api-error.model';
import { StarRating } from '../../shared/components/star-rating/star-rating';

/**
 * The fast check-in screen (full-product-plan.md §12): ratings, concern
 * checkboxes, a quick note, a recommendation, and optional action items —
 * aimed at 30-90 seconds per check-in.
 */
@Component({
  selector: 'app-checkin',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    StarRating,
  ],
  templateUrl: './checkin.html',
  styleUrl: './checkin.scss',
})
export class Checkin {
  private readonly fb = inject(FormBuilder);
  private readonly followUpService = inject(FollowUpService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly followUpId = this.route.snapshot.paramMap.get('id')!;

  readonly concernOptions = CONCERNS;
  readonly followUp = signal<FollowUp | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedConcerns = signal<Set<Concern>>(new Set());

  readonly form = this.fb.nonNullable.group({
    progressRating: [0, Validators.min(1)],
    engagementRating: [0, Validators.min(1)],
    parentSatisfactionRating: [0, Validators.min(1)],
    summary: [''],
    recommendations: [''],
    actionRequired: [false],
  });

  readonly actionItems = new FormArray<FormGroup<{ title: FormControl<string>; dueDate: FormControl<string> }>>([]);

  constructor() {
    this.followUpService.get(this.followUpId).subscribe({
      next: (followUp) => {
        this.followUp.set(followUp);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load this follow-up.');
        this.loading.set(false);
      },
    });
  }

  get cancelLink(): string[] {
    const student = this.followUp()?.student;
    return student ? ['/students', student.id] : ['/students'];
  }

  get noConcerns(): boolean {
    return this.selectedConcerns().size === 0;
  }

  private newActionItem() {
    return this.fb.nonNullable.group({
      title: ['', Validators.required],
      dueDate: [''],
    });
  }

  toggleConcern(concern: Concern, checked: boolean): void {
    const next = new Set(this.selectedConcerns());
    if (checked) {
      next.add(concern);
    } else {
      next.delete(concern);
    }
    this.selectedConcerns.set(next);
  }

  /** The "None" checkbox: ticking it clears every concern. */
  setNone(checked: boolean): void {
    if (checked) {
      this.selectedConcerns.set(new Set());
    }
  }

  onActionRequiredChange(required: boolean): void {
    if (required && this.actionItems.length === 0) {
      this.addActionItem();
    }
    if (!required) {
      this.actionItems.clear();
    }
  }

  addActionItem(): void {
    this.actionItems.push(this.newActionItem());
  }

  removeActionItem(index: number): void {
    this.actionItems.removeAt(index);
    if (this.actionItems.length === 0) {
      this.form.controls.actionRequired.setValue(false);
    }
  }

  submit(): void {
    if (this.form.invalid || this.actionItems.invalid) {
      this.form.markAllAsTouched();
      this.actionItems.markAllAsTouched();
      this.errorMessage.set('Please give all three ratings and title any action items.');
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const actionItems = this.actionItems.getRawValue()
      .filter((item) => item.title.trim())
      .map((item) => ({ title: item.title.trim(), dueDate: item.dueDate || null }));

    this.followUpService
      .complete(this.followUpId, {
        progressRating: raw.progressRating,
        engagementRating: raw.engagementRating,
        parentSatisfactionRating: raw.parentSatisfactionRating,
        concerns: [...this.selectedConcerns()],
        summary: raw.summary.trim() || null,
        recommendations: raw.recommendations.trim() || null,
        actionItems,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.router.navigate(this.cancelLink);
        },
        error: (err: HttpErrorResponse) => {
          this.saving.set(false);
          const apiError = err.error as ApiError | undefined;
          this.errorMessage.set(apiError?.message ?? 'Could not save this check-in.');
        },
      });
  }
}
