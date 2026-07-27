import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { Resident } from '../../shared/models/resident.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { ResidentService } from '../../shared/services/resident.service';

@Component({
  selector: 'app-resident-list',
  imports: [RouterLink, BulkActionsBar],
  templateUrl: './resident-list.html',
  styleUrl: './resident-list.scss',
})
export class ResidentList implements OnInit {
  protected readonly residents = signal<Resident[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selection = createSelection<Resident>((resident) => resident.id);

  constructor(private readonly residentService: ResidentService) {}

  ngOnInit(): void {
    this.load();
  }

  remove(resident: Resident): void {
    const confirmed = confirm(`Remover o condômino "${resident.name}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.residentService.delete(resident.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    bulkDelete(ids, (id) => this.residentService.delete(id)).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(`Não foi possível remover ${result.failed.length} condômino(s): ${details}`);
      }
      this.selection.clear();
      this.load();
    });
  }

  private load(): void {
    this.residentService.findAll().subscribe((residents) => this.residents.set(residents));
  }
}
