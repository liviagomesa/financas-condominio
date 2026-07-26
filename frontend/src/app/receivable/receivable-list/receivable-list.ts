import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { Receivable, TARGET_ACCOUNT_LABELS } from '../../shared/models/receivable.model';
import { Unit } from '../../shared/models/unit.model';
import { ReceivableService } from '../../shared/services/receivable.service';
import { UnitService } from '../../shared/services/unit.service';

@Component({
  selector: 'app-receivable-list',
  imports: [RouterLink, FormsModule, DecimalPipe],
  templateUrl: './receivable-list.html',
  styleUrl: './receivable-list.scss',
})
export class ReceivableList implements OnInit {
  protected readonly receivables = signal<Receivable[]>([]);
  protected readonly units = signal<Unit[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly targetAccountLabels = TARGET_ACCOUNT_LABELS;
  protected selectedUnitId: number | null = null;

  constructor(
    private readonly receivableService: ReceivableService,
    private readonly unitService: UnitService
  ) {}

  ngOnInit(): void {
    this.unitService.findAll().subscribe((units) => this.units.set(units));
    this.load();
  }

  onUnitFilterChange(): void {
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

  private load(): void {
    const unitId = this.selectedUnitId ?? undefined;
    this.receivableService.findAll(unitId).subscribe({
      next: (receivables) => this.receivables.set(receivables),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
