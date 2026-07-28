import { Component, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { Unit } from '../../shared/models/unit.model';
import { UnitService } from '../../shared/services/unit.service';
import { SupplierService } from '../../shared/services/supplier.service';

@Component({
  selector: 'app-supplier-form',
  imports: [ReactiveFormsModule],
  templateUrl: './supplier-form.html',
  styleUrl: './supplier-form.scss',
})
export class SupplierForm implements OnInit {
  protected readonly units = signal<Unit[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected isEditMode = false;
  private supplierId: number | null = null;

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    unitId: new FormControl<number | null>(null),
    pixKey: new FormControl('', { nonNullable: true }),
  });

  constructor(
    private readonly unitService: UnitService,
    private readonly supplierService: SupplierService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.unitService.findAll().subscribe((units) => this.units.set(units));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.supplierId = Number(idParam);
      this.isEditMode = true;
      this.supplierService.findById(this.supplierId).subscribe((supplier) => {
        this.form.setValue({
          name: supplier.name,
          unitId: supplier.unit?.id ?? null,
          pixKey: supplier.pixKey ?? '',
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
      unitId: raw.unitId,
      pixKey: raw.pixKey || null,
    };

    const result = this.supplierId
      ? this.supplierService.update(this.supplierId, request)
      : this.supplierService.create(request);

    result.subscribe({
      next: () => this.router.navigateByUrl('/suppliers'),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
