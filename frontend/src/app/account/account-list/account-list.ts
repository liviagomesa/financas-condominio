import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { Account, AccountFilters, ACCOUNT_TYPE_LABELS } from '../../shared/models/account.model';
import { Fund } from '../../shared/models/fund.model';
import { Party } from '../../shared/models/party.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { RowActions } from '../../shared/components/row-actions/row-actions';
import { AccountService } from '../../shared/services/account.service';
import { FundService } from '../../shared/services/fund.service';
import { PartyService } from '../../shared/services/party.service';

@Component({
  selector: 'app-account-list',
  imports: [RouterLink, FormsModule, DecimalPipe, DatePipe, BulkActionsBar, RowActions],
  templateUrl: './account-list.html',
  styleUrl: './account-list.scss',
})
export class AccountList implements OnInit {
  protected readonly accounts = signal<Account[]>([]);
  protected readonly parties = signal<Party[]>([]);
  protected readonly funds = signal<Fund[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly accountTypeLabels = ACCOUNT_TYPE_LABELS;
  protected readonly selection = createSelection<Account>((account) => account.id);
  protected readonly payingId = signal<number | null>(null);
  protected readonly netTotal = computed(() =>
    this.accounts().reduce((total, a) => (a.type === 'RECEIVABLE' ? total + a.amount : total - a.amount), 0)
  );
  protected selectedPartyId: number | null = null;
  protected selectedFundId: number | null = null;
  protected paidFilter: '' | 'true' | 'false' = '';
  protected overdueOnly = false;
  protected dueYearMonth = '';
  protected paymentYearMonth = '';
  protected paymentDateDraft = '';

  constructor(
    private readonly accountService: AccountService,
    private readonly partyService: PartyService,
    private readonly fundService: FundService
  ) {}

  ngOnInit(): void {
    this.partyService.findAll().subscribe((parties) => this.parties.set(parties));
    this.fundService.findAll().subscribe((funds) => this.funds.set(funds));
    this.load();
  }

  onFilterChange(): void {
    this.selection.clear();
    this.load();
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
    if (this.selectedPartyId != null) filters.partyId = this.selectedPartyId;
    if (this.selectedFundId != null) filters.fundId = this.selectedFundId;
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
