import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { Receivable, ReceivableFilters, TARGET_ACCOUNT_LABELS } from '../../shared/models/receivable.model';
import { Unit } from '../../shared/models/unit.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { ReceivableService } from '../../shared/services/receivable.service';
import { UnitService } from '../../shared/services/unit.service';

@Component({
  selector: 'app-receivable-list',
  imports: [RouterLink, FormsModule, DecimalPipe, DatePipe, BulkActionsBar],
  templateUrl: './receivable-list.html',
  styleUrl: './receivable-list.scss',
})
export class ReceivableList implements OnInit {
  protected readonly receivables = signal<Receivable[]>([]);
  protected readonly units = signal<Unit[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly targetAccountLabels = TARGET_ACCOUNT_LABELS;
  protected readonly selection = createSelection<Receivable>((receivable) => receivable.id);
  protected readonly payingId = signal<number | null>(null);
  protected selectedUnitId: number | null = null;
  protected paidFilter: '' | 'true' | 'false' = '';
  protected overdueOnly = false;
  protected dueYearMonth = '';
  protected paymentYearMonth = '';
  protected paymentDateDraft = '';

  constructor(
    private readonly receivableService: ReceivableService,
    private readonly unitService: UnitService
  ) {}

  ngOnInit(): void {
    this.unitService.findAll().subscribe((units) => this.units.set(units));
    this.load();
  }

  onFilterChange(): void {
    this.selection.clear();
    this.load();
  }

  remove(receivable: Receivable): void {
    const confirmed = confirm(`Remover o lançamento "${receivable.description}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.receivableService.delete(receivable.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    bulkDelete(ids, (id) => this.receivableService.delete(id)).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(`Não foi possível remover ${result.failed.length} lançamento(s): ${details}`);
      }
      this.selection.clear();
      this.load();
    });
  }

  startPayment(receivable: Receivable): void {
    this.payingId.set(receivable.id);
    this.paymentDateDraft = receivable.paymentDate ?? new Date().toISOString().slice(0, 10);
  }

  cancelPayment(): void {
    this.payingId.set(null);
  }

  confirmPayment(receivable: Receivable): void {
    if (!this.paymentDateDraft) {
      return;
    }
    this.errorMessage.set(null);
    this.receivableService
      .registerPayment(receivable.id, { paymentDate: this.paymentDateDraft })
      .subscribe({
        next: () => {
          this.payingId.set(null);
          this.load();
        },
        error: (err: ApiError) => this.errorMessage.set(err.message),
      });
  }

  private load(): void {
    const filters: ReceivableFilters = {};
    if (this.selectedUnitId != null) filters.unitId = this.selectedUnitId;
    if (this.paidFilter === 'true') filters.paid = true;
    if (this.paidFilter === 'false') filters.paid = false;
    if (this.overdueOnly) filters.overdue = true;
    if (this.dueYearMonth) filters.dueYearMonth = this.dueYearMonth;
    if (this.paymentYearMonth) filters.paymentYearMonth = this.paymentYearMonth;

    this.receivableService.findAll(filters).subscribe({
      next: (receivables) => this.receivables.set(receivables),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
