import { Component, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { FundService } from '../../shared/services/fund.service';

@Component({
  selector: 'app-fund-form',
  imports: [ReactiveFormsModule],
  templateUrl: './fund-form.html',
  styleUrl: './fund-form.scss',
})
export class FundForm implements OnInit {
  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    initialBalance: new FormControl<number | null>(0, {
      validators: [Validators.required],
    }),
  });

  protected readonly errorMessage = signal<string | null>(null);
  protected isEditMode = false;
  private fundId: number | null = null;

  constructor(
    private readonly fundService: FundService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.fundId = Number(idParam);
      this.isEditMode = true;
      this.fundService.findById(this.fundId).subscribe((fund) => {
        this.form.setValue({ name: fund.name, initialBalance: fund.initialBalance });
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
    const request = { name: raw.name, initialBalance: raw.initialBalance as number };
    const result = this.fundId
      ? this.fundService.update(this.fundId, request)
      : this.fundService.create(request);

    result.subscribe({
      next: () => this.router.navigateByUrl('/funds'),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
