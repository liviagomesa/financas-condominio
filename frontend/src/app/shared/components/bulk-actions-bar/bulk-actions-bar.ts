import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-bulk-actions-bar',
  imports: [],
  templateUrl: './bulk-actions-bar.html',
  styleUrl: './bulk-actions-bar.scss',
})
export class BulkActionsBar {
  readonly selectedCount = input.required<number>();
  readonly entityLabelPlural = input.required<string>();
  readonly showDuplicateActions = input<boolean>(false);
  readonly remove = output<void>();
  readonly duplicate = output<void>();
  readonly duplicateZeroed = output<void>();

  confirmAndRemove(): void {
    const confirmed = confirm(
      `Remover ${this.selectedCount()} ${this.entityLabelPlural()} selecionado(s)?`
    );
    if (confirmed) {
      this.remove.emit();
    }
  }
}
