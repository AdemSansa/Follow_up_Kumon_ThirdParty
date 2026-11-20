import { Injectable, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

/**
 * Remembers the in-app pages visited so a form can "go back to where you
 * came from" after saving (e.g. Students list → edit → Save → Students list),
 * with a sensible fallback when the form was opened directly.
 */
@Injectable({ providedIn: 'root' })
export class BackNavigation {
  private readonly router = inject(Router);
  private readonly history: string[] = [];

  constructor() {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        if (this.history[this.history.length - 1] !== e.urlAfterRedirects) {
          this.history.push(e.urlAfterRedirects);
          if (this.history.length > 50) this.history.shift();
        }
      });
  }

  /** Leaves the current page for the previous one, or {@code fallback} if there is none. */
  back(fallback: unknown[]): void {
    const current = this.history.pop();
    let previous = this.history.pop();
    // Skip entries that are just the same page or another blank "new" form.
    while (previous && (previous === current || previous.endsWith('/new') || previous.includes('/new?'))) {
      previous = this.history.pop();
    }
    if (previous && !previous.startsWith('/login')) {
      this.router.navigateByUrl(previous);
    } else {
      this.router.navigate(fallback);
    }
  }
}
