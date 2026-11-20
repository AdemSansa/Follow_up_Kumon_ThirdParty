import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, map } from 'rxjs';
import { ConfirmDialog, ConfirmDialogData } from '../../shared/components/confirm-dialog/confirm-dialog';

/**
 * One place for user feedback: confirmation pop-ups before risky actions,
 * and short toast messages after something happened.
 */
@Injectable({ providedIn: 'root' })
export class UiFeedback {
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  /** Emits true if the user confirmed, false if they cancelled or closed the dialog. */
  confirm(data: ConfirmDialogData): Observable<boolean> {
    return this.dialog
      .open(ConfirmDialog, { data, autoFocus: 'dialog', restoreFocus: true, width: '460px', maxWidth: '92vw' })
      .afterClosed()
      .pipe(map((result) => result === true));
  }

  success(message: string): void {
    this.snackBar.open(message, '✓', { duration: 3000, panelClass: 'toast--success' });
  }

  error(message: string): void {
    this.snackBar.open(message, 'Dismiss', { duration: 6000, panelClass: 'toast--error' });
  }
}
