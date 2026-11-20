import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../core/services/auth.service';
import { FollowUpService } from '../../core/services/followup.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AgendaItem, MeetingsStatus } from '../../core/models/followup.model';
import { ApiError } from '../../core/models/api-error.model';
import { UiFeedback } from '../../core/services/ui-feedback.service';

const UNRESOLVED = new Set(['SCHEDULED', 'CONFIRMED', 'RESCHEDULED']);

function toIsoDate(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

/**
 * Today's date at the center (e.g. Montréal), whatever the viewer's own
 * clock says. Falls back to the browser's date until the zone is known.
 */
function centerToday(zone: string | null): Date {
  if (!zone) return new Date();
  try {
    const [y, m, d] = new Intl.DateTimeFormat('en-CA', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' })
      .format(new Date())
      .split('-')
      .map(Number);
    return new Date(y, m - 1, d);
  } catch {
    return new Date();
  }
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly authService = inject(AuthService);
  private readonly followUpService = inject(FollowUpService);
  private readonly ui = inject(UiFeedback);

  readonly currentUser = this.authService.currentUser;

  readonly day = signal(new Date());
  readonly items = signal<AgendaItem[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  /** All unresolved follow-ups whose time has passed (any day). */
  readonly overdueTotal = signal<number | null>(null);

  /** The center's time zone (from the server), so "today" means today at the center. */
  readonly centerZone = signal<string | null>(null);
  readonly isToday = computed(() => toIsoDate(this.day()) === toIsoDate(centerToday(this.centerZone())));
  readonly doneCount = computed(() => this.items().filter((i) => i.status === 'COMPLETED').length);
  readonly toDoCount = computed(() => this.items().filter((i) => UNRESOLVED.has(i.status)).length);

  /** Setup state of Google Meet / email, for the warning banner. */
  readonly meetingsStatus = signal<MeetingsStatus | null>(null);
  readonly hasOnlineItems = computed(() => this.items().some((i) => i.meeting !== null));
  /** Follow-up id → in-flight action, to disable its buttons. */
  readonly busy = signal<Record<string, boolean>>({});
  /** Follow-up id → short result/error message shown on the card. */
  readonly notice = signal<Record<string, string>>({});

  constructor() {
    this.load();
    this.followUpService.list({ overdue: true, size: 1 }).subscribe({
      next: (page) => this.overdueTotal.set(page.totalElements),
      error: () => this.overdueTotal.set(null),
    });
    this.followUpService.meetingsStatus().subscribe({
      next: (status) => {
        this.meetingsStatus.set(status);
        // Still showing "today"? Switch to the center's today if it differs from the browser's.
        const wasToday = toIsoDate(this.day()) === toIsoDate(new Date());
        this.centerZone.set(status.timezone);
        const today = centerToday(status.timezone);
        if (wasToday && toIsoDate(today) !== toIsoDate(this.day())) {
          this.day.set(today);
          this.load();
        }
      },
      error: () => this.meetingsStatus.set(null),
    });
  }

  createMeeting(item: AgendaItem): void {
    this.run(item, this.followUpService.createMeeting(item.id), (r) =>
      r.joinUrl
        ? { ok: true, text: 'Google Meet link created' }
        : { ok: false, text: `No link yet: ${r.lastError ?? 'unknown error'}` },
    );
  }

  sendInvite(item: AgendaItem): void {
    const resend = !!item.meeting?.inviteSentAt;
    const recipients = item.contacts.filter((c) => c.email).map((c) => `${c.firstName} ${c.lastName} (${c.email})`);
    this.ui
      .confirm({
        title: resend ? 'Send the invite again?' : 'Email the Meet link now?',
        message:
          (resend ? 'The parent already received this link. ' : '') +
          `The link for ${item.student.firstName}'s check-in will be emailed to:\n${recipients.join('\n')}`,
        confirmText: resend ? 'Send again' : 'Send now',
        icon: 'forward_to_inbox',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.run(item, this.followUpService.sendMeetingInvite(item.id, resend), (r) =>
          r.sent > 0
            ? { ok: true, text: `Invite emailed to ${r.sent} parent${r.sent > 1 ? 's' : ''}` }
            : { ok: false, text: 'No email was sent — check the parent email addresses.' },
        );
      });
  }

  copyLink(item: AgendaItem): void {
    const url = item.meeting?.joinUrl;
    if (!url) return;
    navigator.clipboard?.writeText(url).then(
      () => this.ui.success('Meet link copied'),
      () => this.setNotice(item.id, url),
    );
  }

  private run<T>(item: AgendaItem, call$: Observable<T>, outcome: (result: T) => { ok: boolean; text: string }): void {
    this.busy.update((b) => ({ ...b, [item.id]: true }));
    this.setNotice(item.id, '');
    call$.subscribe({
      next: (result) => {
        this.busy.update((b) => ({ ...b, [item.id]: false }));
        const { ok, text } = outcome(result);
        if (ok) {
          this.ui.success(text);
        } else {
          this.ui.error(text);
          this.setNotice(item.id, text);
        }
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.busy.update((b) => ({ ...b, [item.id]: false }));
        const text = (err.error as ApiError | undefined)?.message ?? 'Something went wrong.';
        this.ui.error(text);
        this.setNotice(item.id, text);
      },
    });
  }

  private setNotice(id: string, text: string): void {
    this.notice.update((n) => ({ ...n, [id]: text }));
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.followUpService.agenda(toIsoDate(this.day())).subscribe({
      next: (items) => {
        this.items.set(items);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Could not load the check-ins for this day.');
      },
    });
  }

  shiftDay(days: number): void {
    const d = new Date(this.day());
    d.setDate(d.getDate() + days);
    this.day.set(d);
    this.load();
  }

  goToToday(): void {
    this.day.set(centerToday(this.centerZone()));
    this.load();
  }

  canCheckIn(item: AgendaItem): boolean {
    return UNRESOLVED.has(item.status);
  }

  durationMinutes(item: AgendaItem): number {
    return Math.round((new Date(item.scheduledEnd).getTime() - new Date(item.scheduledStart).getTime()) / 60000);
  }

  /** PARENT_MEETING → "Parent meeting". */
  label(value: string | null | undefined): string {
    if (!value) return '';
    const text = value.replaceAll('_', ' ').toLowerCase();
    return text.charAt(0).toUpperCase() + text.slice(1);
  }

  /** Digits-only number for tel:/wa.me links (keeps a leading +). */
  dialable(phone: string): string {
    return phone.trim().replace(/(?!^\+)[^\d]/g, '');
  }

  whatsappLink(phone: string): string {
    return `https://wa.me/${this.dialable(phone).replace(/^\+/, '')}`;
  }
}
