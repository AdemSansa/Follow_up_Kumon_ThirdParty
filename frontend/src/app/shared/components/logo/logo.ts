import { Component, Input } from '@angular/core';

/**
 * Brand mark for this app. This is a *third-party companion tool for
 * Kumon centers* — not an official Kumon product — so this is an original
 * mark in a similar visual spirit (rounded ring motif, bold color) rather
 * than Kumon's own logo. See docs/features/brand-identity.md.
 */
@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <span class="app-logo" [class.app-logo--compact]="compact">
      <svg class="app-logo__mark" viewBox="0 0 48 48" [attr.width]="markSize" [attr.height]="markSize" aria-hidden="true">
        <rect x="1" y="1" width="46" height="46" rx="14" fill="var(--brand-purple)" />
        <circle cx="24" cy="24" r="11" fill="none" stroke="#ffffff" stroke-width="5" />
        <circle cx="37" cy="11" r="5" fill="var(--brand-gold)" />
      </svg>
      @if (!compact) {
        <span class="app-logo__text">
          <span class="app-logo__word">Follow<span class="app-logo__dot">•</span>Up</span>
          <span class="app-logo__tag">for Kumon Centers</span>
        </span>
      }
    </span>
  `,
  styles: [
    `
      .app-logo {
        display: inline-flex;
        align-items: center;
        gap: 0.6rem;
      }

      .app-logo__mark {
        flex: none;
        display: block;
      }

      .app-logo__text {
        display: flex;
        flex-direction: column;
        line-height: 1.1;
      }

      .app-logo__word {
        font-family: 'Fira Sans', sans-serif;
        font-weight: 800;
        font-size: 1.25rem;
        color: var(--brand-purple-dark);
      }

      .app-logo__dot {
        color: var(--brand-gold);
      }

      .app-logo__tag {
        font-size: 0.7rem;
        font-weight: 600;
        letter-spacing: 0.04em;
        text-transform: uppercase;
        color: var(--brand-teal);
      }
    `,
  ],
})
export class Logo {
  @Input() compact = false;
  @Input() markSize = 40;
}
