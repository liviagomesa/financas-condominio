import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { Party } from '../../shared/models/party.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { PartyService } from '../../shared/services/party.service';

@Component({
  selector: 'app-party-list',
  imports: [RouterLink, BulkActionsBar],
  templateUrl: './party-list.html',
  styleUrl: './party-list.scss',
})
export class PartyList implements OnInit {
  protected readonly parties = signal<Party[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selection = createSelection<Party>((party) => party.id);

  constructor(private readonly partyService: PartyService) {}

  ngOnInit(): void {
    this.load();
  }

  remove(party: Party): void {
    const confirmed = confirm(`Remover a parte "${party.name}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.partyService.delete(party.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    bulkDelete(ids, (id) => this.partyService.delete(id)).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(`Não foi possível remover ${result.failed.length} parte(s): ${details}`);
      }
      this.selection.clear();
      this.load();
    });
  }

  private load(): void {
    this.partyService.findAll().subscribe((parties) => this.parties.set(parties));
  }
}
