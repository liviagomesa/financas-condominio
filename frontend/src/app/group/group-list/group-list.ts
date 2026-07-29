import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { Group } from '../../shared/models/group.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { GroupService } from '../../shared/services/group.service';

@Component({
  selector: 'app-group-list',
  imports: [RouterLink, BulkActionsBar],
  templateUrl: './group-list.html',
  styleUrl: './group-list.scss',
})
export class GroupList implements OnInit {
  protected readonly groups = signal<Group[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selection = createSelection<Group>((group) => group.id);

  constructor(private readonly groupService: GroupService) {}

  ngOnInit(): void {
    this.load();
  }

  remove(group: Group): void {
    const confirmed = confirm(`Remover o grupo "${group.name}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.groupService.delete(group.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    bulkDelete(ids, (id) => this.groupService.delete(id)).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(`Não foi possível remover ${result.failed.length} grupo(s): ${details}`);
      }
      this.selection.clear();
      this.load();
    });
  }

  private load(): void {
    this.groupService.findAll().subscribe((groups) => this.groups.set(groups));
  }
}
