import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { Unit } from '../../shared/models/unit.model';
import { UnitService } from '../../shared/services/unit.service';

@Component({
  selector: 'app-unit-list',
  imports: [RouterLink],
  templateUrl: './unit-list.html',
  styleUrl: './unit-list.scss',
})
export class UnitList implements OnInit {
  protected readonly units = signal<Unit[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

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

  private load(): void {
    this.unitService.findAll().subscribe((units) => this.units.set(units));
  }
}
