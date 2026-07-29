import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { Account, AccountFilters, AccountType, ACCOUNT_TYPE_LABELS } from '../../shared/models/account.model';
import { Unit } from '../../shared/models/unit.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { AccountService } from '../../shared/services/account.service';
import { UnitService } from '../../shared/services/unit.service';

@Component({
  selector: 'app-account-list',
  imports: [RouterLink, FormsModule, DecimalPipe, DatePipe, BulkActionsBar],
  templateUrl: './account-list.html',
  styleUrl: './account-list.scss',
})
export class AccountList implements OnInit {
  protected readonly accounts = signal<Account[]>([]);
  protected readonly units = signal<Unit[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly accountTypeLabels = ACCOUNT_TYPE_LABELS;
  protected readonly selection = createSelection<Account>((account) => account.id);
  protected readonly payingId = signal<number | null>(null);
  protected typeFilter: '' | AccountType = '';
  protected selectedUnitId: number | null = null;
  protected paidFilter: '' | 'true' | 'false' = '';
  protected overdueOnly = false;
  protected dueYearMonth = '';
  protected paymentYearMonth = '';
  protected paymentDateDraft = '';

  constructor(
    private readonly accountService: AccountService,
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

  counterpartLabel(account: Account): string {
    return account.type === 'RECEIVABLE' ? (account.unit?.identifier ?? '-') : (account.supplier?.name ?? '-');
  }

  remove(account: Account): void {
    const confirmed = confirm(`Remover a conta "${account.description}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.accountService.delete(account.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    bulkDelete(ids, (id) => this.accountService.delete(id)).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(`Não foi possível remover ${result.failed.length} conta(s): ${details}`);
      }
      this.selection.clear();
      this.load();
    });
  }

  startPayment(account: Account): void {
    this.payingId.set(account.id);
    this.paymentDateDraft = account.paymentDate ?? new Date().toISOString().slice(0, 10);
  }

  cancelPayment(): void {
    this.payingId.set(null);
  }

  confirmPayment(account: Account): void {
    if (!this.paymentDateDraft) {
      return;
    }
    this.errorMessage.set(null);
    this.accountService.registerPayment(account.id, { paymentDate: this.paymentDateDraft }).subscribe({
      next: () => {
        this.payingId.set(null);
        this.load();
      },
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  private load(): void {
    const filters: AccountFilters = {};
    if (this.typeFilter) filters.type = this.typeFilter;
    if (this.selectedUnitId != null) filters.unitId = this.selectedUnitId;
    if (this.paidFilter === 'true') filters.paid = true;
    if (this.paidFilter === 'false') filters.paid = false;
    if (this.overdueOnly) filters.overdue = true;
    if (this.dueYearMonth) filters.dueYearMonth = this.dueYearMonth;
    if (this.paymentYearMonth) filters.paymentYearMonth = this.paymentYearMonth;

    this.accountService.findAll(filters).subscribe({
      next: (accounts) => this.accounts.set(accounts),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
