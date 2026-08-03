import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { ACCOUNT_TYPE_LABELS } from '../../shared/models/account.model';
import { RecurringCharge } from '../../shared/models/recurring-charge.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { RowActions } from '../../shared/components/row-actions/row-actions';
import { RecurringChargeService } from '../../shared/services/recurring-charge.service';

@Component({
  selector: 'app-recurring-charge-list',
  imports: [RouterLink, DecimalPipe, BulkActionsBar, RowActions],
  templateUrl: './recurring-charge-list.html',
  styleUrl: './recurring-charge-list.scss',
})
export class RecurringChargeList implements OnInit {
  protected readonly recurringCharges = signal<RecurringCharge[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly accountTypeLabels = ACCOUNT_TYPE_LABELS;
  protected readonly selection = createSelection<RecurringCharge>((rc) => rc.id);

  constructor(private readonly recurringChargeService: RecurringChargeService) {}

  ngOnInit(): void {
    this.load();
  }

  remove(recurringCharge: RecurringCharge): void {
    const confirmed = confirm(`Remover a cobrança recorrente "${recurringCharge.description}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.recurringChargeService.delete(recurringCharge.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    bulkDelete(ids, (id) => this.recurringChargeService.delete(id)).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(
          `Não foi possível remover ${result.failed.length} cobrança(s) recorrente(s): ${details}`
        );
      }
      this.selection.clear();
      this.load();
    });
  }

  private load(): void {
    this.recurringChargeService.findAll().subscribe({
      next: (recurringCharges) => this.recurringCharges.set(recurringCharges),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
