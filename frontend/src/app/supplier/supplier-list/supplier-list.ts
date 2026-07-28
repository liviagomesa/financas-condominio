import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { bulkDelete } from '../../shared/bulk-delete';
import { createSelection } from '../../shared/list-selection';
import { Supplier } from '../../shared/models/supplier.model';
import { BulkActionsBar } from '../../shared/components/bulk-actions-bar/bulk-actions-bar';
import { SupplierService } from '../../shared/services/supplier.service';

@Component({
  selector: 'app-supplier-list',
  imports: [RouterLink, BulkActionsBar],
  templateUrl: './supplier-list.html',
  styleUrl: './supplier-list.scss',
})
export class SupplierList implements OnInit {
  protected readonly suppliers = signal<Supplier[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selection = createSelection<Supplier>((supplier) => supplier.id);

  constructor(private readonly supplierService: SupplierService) {}

  ngOnInit(): void {
    this.load();
  }

  remove(supplier: Supplier): void {
    const confirmed = confirm(`Remover o fornecedor "${supplier.name}"?`);
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.supplierService.delete(supplier.id).subscribe({
      next: () => this.load(),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }

  removeSelected(): void {
    this.errorMessage.set(null);
    const ids = Array.from(this.selection.selectedIds());
    bulkDelete(ids, (id) => this.supplierService.delete(id)).subscribe((result) => {
      if (result.failed.length) {
        const details = result.failed.map((f) => f.message).join('; ');
        this.errorMessage.set(`Não foi possível remover ${result.failed.length} fornecedor(es): ${details}`);
      }
      this.selection.clear();
      this.load();
    });
  }

  private load(): void {
    this.supplierService.findAll().subscribe((suppliers) => this.suppliers.set(suppliers));
  }
}
