import { Directive, HostListener, Injector, inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { Observable, from, switchMap } from 'rxjs';

/**
 * Base for form pages: implement hasUnsavedChanges(). Closing or reloading
 * the browser tab with unsaved edits shows the browser's own "Leave site?"
 * warning; leaving inside the app is handled by unsavedChangesGuard.
 */
@Directive()
export abstract class UnsavedChangesAware {
  abstract hasUnsavedChanges(): boolean;

  @HostListener('window:beforeunload', ['$event'])
  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = ''; // required by some browsers to show the prompt
    }
  }
}

/**
 * Route guard: asks before leaving a form page that has unsaved edits.
 * The dialog code is loaded on demand so it stays out of the initial bundle.
 */
export const unsavedChangesGuard: CanDeactivateFn<UnsavedChangesAware> = (component): Observable<boolean> | boolean => {
  if (!component?.hasUnsavedChanges?.()) {
    return true;
  }
  const injector = inject(Injector);
  return from(import('../core/services/ui-feedback.service')).pipe(
    switchMap(({ UiFeedback }) =>
      injector.get(UiFeedback).confirm({
        title: 'Leave without saving?',
        message: "You have changes on this page that haven't been saved. If you leave now, they will be lost.",
        confirmText: 'Leave without saving',
        cancelText: 'Stay on page',
        danger: true,
        icon: 'edit_off',
      }),
    ),
  );
};
