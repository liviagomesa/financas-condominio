import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, HostListener, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { bulkDuplicate } from '../../shared/bulk-duplicate';
import { createSelection } from '../../shared/list-selection';
import { Account, AccountFilters, AccountRequest, ACCOUNT_TYPE_LABELS } from '../../shared/models/account.model';
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
  protected readonly successMessage = signal<string | null>(null);
  protected readonly accountTypeLabels = ACCOUNT_TYPE_LABELS;

  // JavaScript não funciona como outras linguagens, em que uma função tem um frame na pilha de execução
  // que é destruído junto com suas variáveis locais assim que a função retorna.
  //
  // O resultado de createSelection está salvo em selection, variável do componente atualmente renderizado.
  // Como selection inclui funções (como isSelected) que referenciam variáveis de createSelection (ids e
  // anchorId), o JS é obrigado a mantê-las em memória, para que as funções não quebrem.
  protected readonly selection = createSelection<Account>((account) => account.id);

  protected readonly copiedIds = signal<number[]>([]);
  protected readonly recentlyDuplicatedIds = signal<ReadonlySet<number>>(new Set());
  protected readonly payingId = signal<number | null>(null);
  protected readonly editingAmountId = signal<number | null>(null);
  protected readonly amountEditError = signal<string | null>(null);
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
  protected amountDraft = '';

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
    this.editingAmountId.set(null);
    this.amountEditError.set(null);
    this.successMessage.set(null);
    this.load();
  }

  remove(account: Account): void {
    const confirmed = confirm(`Remover a conta "${account.description}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.accountService.delete(account.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    // passa lista de ids a remover + a função de delete específica deste recurso
    bulkDelete(ids, (id) => this.accountService.delete(id))
      .subscribe((result) => {
        if (result.failed.length) {
          const details = result.failed.map((f) => f.message).join('; ');
          this.errorMessage.set(`Não foi possível remover ${result.failed.length} conta(s): ${details}`);
        }
        this.selection.clear();
        this.load();
      });
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    const target = document.activeElement as HTMLInputElement | null;
    const isEditableInput =
      target?.tagName === 'INPUT' && !['checkbox', 'radio'].includes(target.type);
    if (target && (isEditableInput || target.tagName === 'TEXTAREA' || target.isContentEditable)) {
      return;
    }

    const key = event.key.toLowerCase();
    if (event.ctrlKey && key === 'c' && this.selection.selectedIds().size > 0) {
      this.copiedIds.set(Array.from(this.selection.selectedIds()));
    } else if (event.ctrlKey && key === 'v' && this.copiedIds().length > 0) {
      event.preventDefault();
      this.performDuplicate(this.copiedIds(), false);
    }
  }

  duplicateSelected(zeroAmount: boolean): void {
    this.performDuplicate(Array.from(this.selection.selectedIds()), zeroAmount);
  }

  private performDuplicate(ids: number[], zeroAmount: boolean): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    bulkDuplicate(ids, (id) => this.accountService.duplicate(id, { zeroAmount })).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(`Não foi possível duplicar ${result.failed.length} conta(s): ${details}`);
      }
      if (result.succeeded.length) {
        this.successMessage.set(`${result.succeeded.length} conta(s) duplicada(s) com sucesso.`);
      }
      this.selection.clear();
      const succeededIds = result.succeeded.map((a) => a.id);
      this.load(() => this.highlightDuplicated(succeededIds));
    });
  }

  private highlightDuplicated(ids: number[]): void {
    if (ids.length === 0) {
      return;
    }
    this.recentlyDuplicatedIds.set(new Set(ids));
    setTimeout(() => {
      document.querySelector(`tr[data-account-id="${ids[0]}"]`)?.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    });
    setTimeout(() => this.recentlyDuplicatedIds.set(new Set()), 2500);
  }

  startPayment(account: Account): void {
    this.editingAmountId.set(null);
    this.amountEditError.set(null);
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

  startAmountEdit(account: Account): void {
    this.payingId.set(null);
    this.amountDraft = String(account.amount);
    this.amountEditError.set(null);
    this.editingAmountId.set(account.id);
  }

  cancelAmountEdit(): void {
    this.editingAmountId.set(null);
    this.amountEditError.set(null);
  }

  confirmAmountEdit(account: Account): void {
    if (this.editingAmountId() !== account.id) {
      return;
    }
    const amount = Number(this.amountDraft);
    if (this.amountDraft === '' || this.amountDraft == null || Number.isNaN(amount) || amount < 0) {
      this.amountEditError.set('O valor é obrigatório e não pode ser negativo.');
      return;
    }

    this.errorMessage.set(null);
    const request: AccountRequest = {
      type: account.type,
      amount,
      dueDate: account.dueDate,
      description: account.description,
      fundId: account.fund.id,
      recurring: account.recurring,
      partyId: account.party.id,
      paymentDate: account.paymentDate,
      observations: account.observations,
    };
    this.accountService.update(account.id, request).subscribe({
      next: (updated) => {
        this.accounts.update((list) => list.map((a) => (a.id === updated.id ? updated : a)));
        this.editingAmountId.set(null);
        this.amountEditError.set(null);
      },
      error: (err: ApiError) => this.amountEditError.set(err.message),
    });
  }

  private load(onSuccess?: () => void): void {
    const filters: AccountFilters = {};
    if (this.selectedPartyId != null) filters.partyId = this.selectedPartyId;
    if (this.selectedFundId != null) filters.fundId = this.selectedFundId;
    if (this.paidFilter === 'true') filters.paid = true;
    if (this.paidFilter === 'false') filters.paid = false;
    if (this.overdueOnly) filters.overdue = true;
    if (this.dueYearMonth) filters.dueYearMonth = this.dueYearMonth;
    if (this.paymentYearMonth) filters.paymentYearMonth = this.paymentYearMonth;

    this.accountService.findAll(filters).subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        onSuccess?.();
      },
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
