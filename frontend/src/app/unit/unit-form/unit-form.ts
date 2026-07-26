import { Component, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { UnitService } from '../../shared/services/unit.service';

@Component({
  selector: 'app-unit-form',
  imports: [ReactiveFormsModule],
  templateUrl: './unit-form.html',
  styleUrl: './unit-form.scss',
})
export class UnitForm implements OnInit {
  protected readonly form = new FormGroup({
    identifier: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected readonly errorMessage = signal<string | null>(null);
  protected isEditMode = false;
  private unitId: number | null = null;

  constructor(
    private readonly unitService: UnitService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.unitId = Number(idParam);
      this.isEditMode = true;
      this.unitService.findById(this.unitId).subscribe((unit) => {
        this.form.setValue({ identifier: unit.identifier });
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);
    const request = this.form.getRawValue();
    const result = this.unitId
      ? this.unitService.update(this.unitId, request)
      : this.unitService.create(request);

    result.subscribe({
      next: () => this.router.navigateByUrl('/units'),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
