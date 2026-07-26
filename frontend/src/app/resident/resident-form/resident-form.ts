import { Component, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { Unit } from '../../shared/models/unit.model';
import { UnitService } from '../../shared/services/unit.service';
import { ResidentService } from '../../shared/services/resident.service';

@Component({
  selector: 'app-resident-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './resident-form.html',
  styleUrl: './resident-form.scss',
})
export class ResidentForm implements OnInit {
  protected readonly units = signal<Unit[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected isEditMode = false;
  private residentId: number | null = null;

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    unitId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.email] }),
    phone: new FormControl('', { nonNullable: true }),
  });

  constructor(
    private readonly unitService: UnitService,
    private readonly residentService: ResidentService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.unitService.findAll().subscribe((units) => this.units.set(units));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.residentId = Number(idParam);
      this.isEditMode = true;
      this.residentService.findById(this.residentId).subscribe((resident) => {
        this.form.setValue({
          name: resident.name,
          unitId: resident.unit.id,
          email: resident.email ?? '',
          phone: resident.phone ?? '',
        });
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const request = {
      name: raw.name,
      unitId: raw.unitId as number,
      email: raw.email || null,
      phone: raw.phone || null,
    };

    const result = this.residentId
      ? this.residentService.update(this.residentId, request)
      : this.residentService.create(request);

    result.subscribe({
      next: () => this.router.navigateByUrl('/residents'),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
