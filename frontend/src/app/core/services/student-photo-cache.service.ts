import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, shareReplay } from 'rxjs';
import { StudentService } from './student.service';

/**
 * Student photos as object URLs, fetched once per (student, photo version).
 * Photos need the login token, so they're downloaded with HttpClient rather
 * than a plain <img src>. A new photoUpdatedAt fetches a fresh copy.
 */
@Injectable({ providedIn: 'root' })
export class StudentPhotoCache {
  private readonly students = inject(StudentService);
  private readonly cache = new Map<string, { version: string; url$: Observable<string | null> }>();

  /** Emits the image URL, or null if there is no photo (or it couldn't be loaded). */
  url(studentId: string, photoUpdatedAt: string | null): Observable<string | null> {
    if (!photoUpdatedAt) {
      return of(null);
    }
    const cached = this.cache.get(studentId);
    if (cached && cached.version === photoUpdatedAt) {
      return cached.url$;
    }
    this.forget(studentId);
    const url$ = this.students.getPhoto(studentId).pipe(
      map((blob) => URL.createObjectURL(blob)),
      catchError(() => of(null)),
      shareReplay(1),
    );
    this.cache.set(studentId, { version: photoUpdatedAt, url$ });
    return url$;
  }

  /** Drop (and free) the cached image, e.g. after the photo was removed or replaced. */
  forget(studentId: string): void {
    const cached = this.cache.get(studentId);
    if (cached) {
      cached.url$.subscribe((u) => u && URL.revokeObjectURL(u)).unsubscribe();
      this.cache.delete(studentId);
    }
  }
}
