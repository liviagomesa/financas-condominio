import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { Unit } from '../../shared/models/unit.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { UnitService } from '../../shared/services/unit.service';

@Component({
  selector: 'app-unit-list',
  imports: [RouterLink, BulkActionsBar],
  templateUrl: './unit-list.html',
  styleUrl: './unit-list.scss',
})
export class UnitList implements OnInit {
  protected readonly units = signal<Unit[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selection = createSelection<Unit>((unit) => unit.id);

  constructor(private readonly unitService: UnitService) {}

  ngOnInit(): void {
    this.load();
  }

  remove(unit: Unit): void {
    const confirmed = confirm(`Remover a unidade "${unit.identifier}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.unitService.delete(unit.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    bulkDelete(ids, (id) => this.unitService.delete(id)).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(`Não foi possível remover ${result.failed.length} unidade(s): ${details}`);
      }
      this.selection.clear();
      this.load();
    });
  }

  private load(): void {
    this.unitService.findAll().subscribe((units) => this.units.set(units));
  }
}
