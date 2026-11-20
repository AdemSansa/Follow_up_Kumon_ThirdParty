import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  /** Label of the confirm button, e.g. "Delete". */
  confirmText?: string;
  cancelText?: string;
  /** Red confirm button + warning icon, for destructive actions. */
  danger?: boolean;
  icon?: string;
}

/** Generic "Are you sure?" dialog — open it through ConfirmService. */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="confirm__title">
      <mat-icon [class.confirm__icon--danger]="data.danger">{{ data.icon ?? (data.danger ? 'warning' : 'help') }}</mat-icon>
      {{ data.title }}
    </h2>
    <mat-dialog-content>
      <p class="confirm__message">{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">{{ data.cancelText ?? 'Cancel' }}</button>
      <button
        mat-flat-button
        [class.confirm__danger]="data.danger"
        color="primary"
        [mat-dialog-close]="true"
        cdkFocusInitial
      >
        {{ data.confirmText ?? 'Confirm' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .confirm__title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .confirm__icon--danger {
      color: #b3261e;
    }
    .confirm__message {
      margin: 0;
      white-space: pre-line;
      max-width: 420px;
    }
    .confirm__danger {
      --mdc-filled-button-container-color: #b3261e;
      --mat-button-filled-container-color: #b3261e;
      background-color: #b3261e;
    }
  `,
})
export class ConfirmDialog {
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
