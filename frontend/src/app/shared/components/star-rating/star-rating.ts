import { Component, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * 1–5 star input usable with formControlName. Big tap targets on purpose —
 * the check-in has to be fast on a phone (full-product-plan.md §12, §46).
 */
@Component({
  selector: 'app-star-rating',
  standalone: true,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => StarRating), multi: true }],
  template: `
    <div class="stars" role="radiogroup">
      @for (n of options; track n) {
        <button
          type="button"
          class="stars__star"
          [class.stars__star--on]="n <= value()"
          [attr.aria-label]="n + ' out of 5'"
          [attr.aria-checked]="n === value()"
          role="radio"
          (click)="select(n)"
        >
          ★
        </button>
      }
    </div>
  `,
  styles: [
    `
      .stars {
        display: inline-flex;
        gap: 0.25rem;
      }

      .stars__star {
        border: none;
        background: none;
        cursor: pointer;
        font-size: 2rem;
        line-height: 1;
        padding: 0.1rem 0.15rem;
        color: rgba(61, 51, 111, 0.2);
        transition: transform 0.1s;
      }

      .stars__star:hover {
        transform: scale(1.15);
      }

      .stars__star--on {
        color: var(--brand-gold);
      }
    `,
  ],
})
export class StarRating implements ControlValueAccessor {
  readonly options = [1, 2, 3, 4, 5];
  readonly value = signal(0);

  private onChange: (value: number) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  select(n: number): void {
    this.value.set(n);
    this.onChange(n);
    this.onTouched();
  }

  writeValue(value: number | null): void {
    this.value.set(value ?? 0);
  }

  registerOnChange(fn: (value: number) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
}
