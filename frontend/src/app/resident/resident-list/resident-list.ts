import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Resident } from '../../shared/models/resident.model';
import { ResidentService } from '../../shared/services/resident.service';

@Component({
  selector: 'app-resident-list',
  imports: [RouterLink],
  templateUrl: './resident-list.html',
  styleUrl: './resident-list.scss',
})
export class ResidentList implements OnInit {
  protected readonly residents = signal<Resident[]>([]);

  constructor(private readonly residentService: ResidentService) {}

  ngOnInit(): void {
    this.load();
  }

  remove(resident: Resident): void {
    const confirmed = confirm(`Remover o condômino "${resident.name}"?`);
    if (!confirmed) {
      return;
    }
    this.residentService.delete(resident.id).subscribe(() => this.load());
  }

  private load(): void {
    this.residentService.findAll().subscribe((residents) => this.residents.set(residents));
  }
}
