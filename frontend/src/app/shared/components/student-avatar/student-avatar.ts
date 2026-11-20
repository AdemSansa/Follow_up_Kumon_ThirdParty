import { Component, computed, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { StudentPhotoCache } from '../../../core/services/student-photo-cache.service';

/**
 * Round student picture. Shows the photo when the student has one,
 * otherwise a neutral grey face.
 */
@Component({
  selector: 'app-student-avatar',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <span class="avatar" [style.width.px]="size()" [style.height.px]="size()">
      @if (url(); as src) {
        <img [src]="src" [alt]="name() ? 'Photo of ' + name() : 'Student photo'" />
      } @else {
        <mat-icon class="avatar__face" [style.font-size.px]="size() * 0.72" aria-hidden="true">face</mat-icon>
      }
    </span>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex: none;
    }
    .avatar {
      display: inline-grid;
      place-items: center;
      overflow: hidden;
      border-radius: 50%;
      background: #e3e1e8;
      box-shadow: 0 0 0 1px rgba(61, 51, 111, 0.08);
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .avatar__face {
      width: auto;
      height: auto;
      line-height: 1;
      color: #9a96a6;
    }
  `,
})
export class StudentAvatar {
  readonly studentId = input.required<string>();
  /** The student's photoUpdatedAt; null = no photo. */
  readonly photoUpdatedAt = input<string | null>(null);
  readonly name = input<string>('');
  readonly size = input<number>(36);

  private readonly cache = inject(StudentPhotoCache);
  private readonly key = computed(() => ({ id: this.studentId(), version: this.photoUpdatedAt() }));

  readonly url = toSignal(
    toObservable(this.key).pipe(switchMap(({ id, version }) => this.cache.url(id, version))),
    { initialValue: null },
  );
}
