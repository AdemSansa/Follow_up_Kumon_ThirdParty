import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { FollowUpService } from '../../core/services/followup.service';
import { FollowUp } from '../../core/models/followup.model';

type ViewMode = 'month' | 'week' | 'day';

function toIsoDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

/** Monday-start week. */
function startOfWeek(date: Date): Date {
  const day = (date.getDay() + 6) % 7; // 0 = Monday
  return addDays(date, -day);
}

function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function endOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

const WEEKDAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatButtonToggleModule, MatIconModule, MatProgressBarModule],
  templateUrl: './calendar.html',
  styleUrl: './calendar.scss',
})
export class Calendar {
  private readonly followUpService = inject(FollowUpService);

  readonly weekdayLabels = WEEKDAY_LABELS;
  readonly viewMode = signal<ViewMode>('month');
  readonly anchorDate = signal<Date>(new Date());
  readonly events = signal<FollowUp[]>([]);
  readonly loading = signal(false);
  readonly today = new Date();

  readonly rangeStart = computed<Date>(() => {
    const anchor = this.anchorDate();
    switch (this.viewMode()) {
      case 'month':
        return startOfWeek(startOfMonth(anchor));
      case 'week':
        return startOfWeek(anchor);
      case 'day':
        return anchor;
    }
  });

  readonly rangeEnd = computed<Date>(() => {
    switch (this.viewMode()) {
      case 'month':
        return addDays(startOfWeek(endOfMonth(this.anchorDate())), 6);
      case 'week':
        return addDays(this.rangeStart(), 6);
      case 'day':
        return this.anchorDate();
    }
  });

  readonly visibleDays = computed<Date[]>(() => {
    const start = this.rangeStart();
    const end = this.rangeEnd();
    const days: Date[] = [];
    for (let d = start; d <= end; d = addDays(d, 1)) {
      days.push(d);
    }
    return days;
  });

  readonly rangeLabel = computed<string>(() => {
    const anchor = this.anchorDate();
    if (this.viewMode() === 'month') {
      return anchor.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    }
    if (this.viewMode() === 'week') {
      const start = this.rangeStart();
      const end = this.rangeEnd();
      return `${start.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} – ${end.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}`;
    }
    return anchor.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
  });

  readonly eventsByDay = computed<Map<string, FollowUp[]>>(() => {
    const map = new Map<string, FollowUp[]>();
    for (const event of this.events()) {
      const key = event.scheduledStart.slice(0, 10);
      const list = map.get(key) ?? [];
      list.push(event);
      map.set(key, list);
    }
    for (const list of map.values()) {
      list.sort((a, b) => a.scheduledStart.localeCompare(b.scheduledStart));
    }
    return map;
  });

  constructor() {
    this.load();
  }

  eventsFor(day: Date): FollowUp[] {
    return this.eventsByDay().get(toIsoDate(day)) ?? [];
  }

  isCurrentMonth(day: Date): boolean {
    return day.getMonth() === this.anchorDate().getMonth();
  }

  isToday(day: Date): boolean {
    return isSameDay(day, this.today);
  }

  formatTime(isoDateTime: string): string {
    return isoDateTime.slice(11, 16);
  }

  setViewMode(mode: ViewMode): void {
    this.viewMode.set(mode);
    this.load();
  }

  previous(): void {
    this.step(-1);
  }

  next(): void {
    this.step(1);
  }

  goToday(): void {
    this.anchorDate.set(new Date());
    this.load();
  }

  private step(direction: 1 | -1): void {
    const anchor = this.anchorDate();
    if (this.viewMode() === 'month') {
      this.anchorDate.set(new Date(anchor.getFullYear(), anchor.getMonth() + direction, 1));
    } else if (this.viewMode() === 'week') {
      this.anchorDate.set(addDays(anchor, 7 * direction));
    } else {
      this.anchorDate.set(addDays(anchor, direction));
    }
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.followUpService
      .list({ from: toIsoDate(this.rangeStart()), to: toIsoDate(this.rangeEnd()), size: 500 })
      .subscribe({
        next: (page) => {
          this.events.set(page.content);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
