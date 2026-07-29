import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { Fund } from '../../shared/models/fund.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { FundService } from '../../shared/services/fund.service';

@Component({
  selector: 'app-fund-list',
  imports: [RouterLink, DecimalPipe, BulkActionsBar],
  templateUrl: './fund-list.html',
  styleUrl: './fund-list.scss',
})
export class FundList implements OnInit {
  protected readonly funds = signal<Fund[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selection = createSelection<Fund>((fund) => fund.id);
  protected readonly totalRealBalance = computed(() =>
    this.funds().reduce((total, fund) => total + fund.realBalance, 0)
  );

  constructor(private readonly fundService: FundService) {}

  ngOnInit(): void {
    this.load();
  }

  remove(fund: Fund): void {
    const confirmed = confirm(`Remover o fundo "${fund.name}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.fundService.delete(fund.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    bulkDelete(ids, (id) => this.fundService.delete(id)).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(`Não foi possível remover ${result.failed.length} fundo(s): ${details}`);
      }
      this.selection.clear();
      this.load();
    });
  }

  private load(): void {
    this.fundService.findAll().subscribe((funds) => this.funds.set(funds));
  }
}
