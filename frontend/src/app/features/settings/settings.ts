import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../core/services/auth.service';
import { GoogleAccounts, GoogleAccountService } from '../../core/services/google-account.service';
import { FollowUpService } from '../../core/services/followup.service';
import { MeetingsStatus } from '../../core/models/followup.model';
import { ApiError } from '../../core/models/api-error.model';
import { UiFeedback } from '../../core/services/ui-feedback.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings {
  private readonly google = inject(GoogleAccountService);
  private readonly followUps = inject(FollowUpService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ui = inject(UiFeedback);

  readonly isAdmin = inject(AuthService).isAdmin;
  readonly accounts = signal<GoogleAccounts | null>(null);
  readonly status = signal<MeetingsStatus | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  /** Result of the Google redirect (?google=connected|error) or of an action. */
  readonly message = signal<{ ok: boolean; text: string } | null>(null);

  constructor() {
    const q = this.route.snapshot.queryParamMap;
    if (q.get('google') === 'connected') {
      this.message.set({ ok: true, text: `Google account ${q.get('email') ?? ''} connected.` });
    } else if (q.get('google') === 'error') {
      this.message.set({ ok: false, text: q.get('reason') ?? 'Connecting Google failed.' });
    }
    if (q.has('google')) {
      // Drop the query string so a refresh doesn't show the message again.
      this.router.navigate([], { queryParams: {}, replaceUrl: true });
    }
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.google.get().subscribe({
      next: (a) => {
        this.accounts.set(a);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.message.set({ ok: false, text: 'Could not load the Google settings.' });
      },
    });
    this.followUps.meetingsStatus().subscribe({ next: (s) => this.status.set(s), error: () => undefined });
  }

  connect(kind: 'CENTER' | 'PERSONAL'): void {
    this.busy.set(true);
    this.google.connectUrl(kind).subscribe({
      // Full-page redirect to Google; it comes back to /settings?google=...
      next: ({ url }) => (window.location.href = url),
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.message.set({ ok: false, text: (err.error as ApiError | undefined)?.message ?? 'Could not start.' });
      },
    });
  }

  disconnect(which: 'center' | 'mine'): void {
    const center = which === 'center';
    this.ui
      .confirm({
        title: center ? 'Disconnect the center Google account?' : 'Disconnect your Google account?',
        message: center
          ? 'New online check-ins will get no Meet link until a Google account is connected again ' +
            '(staff who connected their own account keep hosting theirs). Links already sent keep working.'
          : 'Your check-ins will be hosted by the center account instead. Links already sent keep working.',
        confirmText: 'Disconnect',
        danger: true,
        icon: 'link_off',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.busy.set(true);
        this.google.disconnect(which).subscribe({
          next: () => {
            this.busy.set(false);
            this.message.set(null);
            this.ui.success('Google account disconnected');
            this.load();
          },
          error: () => {
            this.busy.set(false);
            this.ui.error('Could not disconnect the Google account.');
          },
        });
      });
  }
}
