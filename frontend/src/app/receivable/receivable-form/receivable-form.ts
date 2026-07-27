import { Component, OnInit, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { TargetAccount, TARGET_ACCOUNT_LABELS } from '../../shared/models/receivable.model';
import { Unit } from '../../shared/models/unit.model';
import { ReceivableService } from '../../shared/services/receivable.service';
import { UnitService } from '../../shared/services/unit.service';

@Component({
  selector: 'app-receivable-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './receivable-form.html',
  styleUrl: './receivable-form.scss',
})
export class ReceivableForm implements OnInit {
  protected readonly units = signal<Unit[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly bulkMode = signal(false);
  protected readonly targetAccountOptions: { value: TargetAccount; label: string }[] = (
    Object.entries(TARGET_ACCOUNT_LABELS) as [TargetAccount, string][]
  ).map(([value, label]) => ({ value, label }));
  protected isEditMode = false;
  private receivableId: number | null = null;

  protected readonly form = new FormGroup({
    amount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)],
    }),
    dueDate: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    targetAccount: new FormControl<TargetAccount | null>(null, { validators: [Validators.required] }),
    recurring: new FormControl(false, { nonNullable: true }),
    unitId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    paymentDate: new FormControl('', { nonNullable: true }),
  });

  constructor(
    private readonly unitService: UnitService,
    private readonly receivableService: ReceivableService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.unitService.findAll().subscribe((units) => this.units.set(units));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.receivableId = Number(idParam);
      this.isEditMode = true;
      this.receivableService.findById(this.receivableId).subscribe((receivable) => {
        this.form.setValue({
          amount: receivable.amount,
          dueDate: receivable.dueDate,
          description: receivable.description,
          targetAccount: receivable.targetAccount,
          recurring: receivable.recurring,
          unitId: receivable.unit.id,
          paymentDate: receivable.paymentDate ?? '',
        });
      });
    }
  }

  setBulkMode(bulk: boolean): void {
    this.bulkMode.set(bulk);
    const unitIdControl = this.form.controls.unitId;
    if (bulk) {
      unitIdControl.clearValidators();
      unitIdControl.setValue(null);
    } else {
      unitIdControl.setValidators([Validators.required]);
    }
    unitIdControl.updateValueAndValidity();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const shared = {
      amount: raw.amount as number,
      dueDate: raw.dueDate,
      description: raw.description,
      targetAccount: raw.targetAccount as TargetAccount,
      recurring: raw.recurring,
      paymentDate: raw.paymentDate || null,
    };

    const onSuccess = () => this.router.navigateByUrl('/receivables');
    const onError = (err: ApiError) => this.errorMessage.set(err.message);

    if (this.bulkMode()) {
      this.receivableService.createBulk(shared).subscribe({ next: onSuccess, error: onError });
    } else if (this.receivableId) {
      this.receivableService
        .update(this.receivableId, { ...shared, unitId: raw.unitId as number })
        .subscribe({ next: onSuccess, error: onError });
    } else {
      this.receivableService
        .create({ ...shared, unitId: raw.unitId as number })
        .subscribe({ next: onSuccess, error: onError });
    }
  }
}
