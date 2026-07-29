import { Component, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { AccountType, ACCOUNT_TYPE_LABELS } from '../../shared/models/account.model';
import { Fund } from '../../shared/models/fund.model';
import { Supplier } from '../../shared/models/supplier.model';
import { Unit } from '../../shared/models/unit.model';
import { AccountService } from '../../shared/services/account.service';
import { FundService } from '../../shared/services/fund.service';
import { SupplierService } from '../../shared/services/supplier.service';
import { UnitService } from '../../shared/services/unit.service';

@Component({
  selector: 'app-account-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './account-form.html',
  styleUrl: './account-form.scss',
})
export class AccountForm implements OnInit {
  protected readonly units = signal<Unit[]>([]);
  protected readonly suppliers = signal<Supplier[]>([]);
  protected readonly funds = signal<Fund[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly bulkMode = signal(false);
  protected readonly accountTypeOptions: { value: AccountType; label: string }[] = (
    Object.entries(ACCOUNT_TYPE_LABELS) as [AccountType, string][]
  ).map(([value, label]) => ({ value, label }));
  protected isEditMode = false;
  private accountId: number | null = null;

  protected readonly form = new FormGroup({
    type: new FormControl<AccountType>('RECEIVABLE', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    amount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0)],
    }),
    dueDate: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    fundId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    recurring: new FormControl(false, { nonNullable: true }),
    unitId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    supplierId: new FormControl<number | null>(null),
    paymentDate: new FormControl('', { nonNullable: true }),
    observations: new FormControl('', { nonNullable: true }),
  });

  constructor(
    private readonly unitService: UnitService,
    private readonly supplierService: SupplierService,
    private readonly fundService: FundService,
    private readonly accountService: AccountService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.unitService.findAll().subscribe((units) => this.units.set(units));
    this.supplierService.findAll().subscribe((suppliers) => this.suppliers.set(suppliers));
    this.fundService.findAll().subscribe((funds) => this.funds.set(funds));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.accountId = Number(idParam);
      this.isEditMode = true;
      this.accountService.findById(this.accountId).subscribe((account) => {
        this.applyTypeValidators(account.type);
        this.form.setValue({
          type: account.type,
          amount: account.amount,
          dueDate: account.dueDate,
          description: account.description,
          fundId: account.fund.id,
          recurring: account.recurring,
          unitId: account.unit?.id ?? null,
          supplierId: account.supplier?.id ?? null,
          paymentDate: account.paymentDate ?? '',
          observations: account.observations ?? '',
        });
        this.form.controls.type.disable();
      });
    } else {
      this.applyTypeValidators(this.form.controls.type.value);
    }
  }

  setType(type: AccountType): void {
    this.form.controls.type.setValue(type);
    this.bulkMode.set(false);
    this.applyTypeValidators(type);
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
      fundId: raw.fundId as number,
      recurring: raw.recurring,
      paymentDate: raw.paymentDate || null,
      observations: raw.observations || null,
    };

    const onSuccess = () => this.router.navigateByUrl('/accounts');
    const onError = (err: ApiError) => this.errorMessage.set(err.message);

    if (this.bulkMode()) {
      this.accountService.createBulk(shared).subscribe({ next: onSuccess, error: onError });
    } else if (this.accountId) {
      this.accountService
        .update(this.accountId, {
          ...shared,
          type: raw.type,
          unitId: raw.unitId,
          supplierId: raw.supplierId,
        })
        .subscribe({ next: onSuccess, error: onError });
    } else {
      this.accountService
        .create({
          ...shared,
          type: raw.type,
          unitId: raw.unitId,
          supplierId: raw.supplierId,
        })
        .subscribe({ next: onSuccess, error: onError });
    }
  }

  private applyTypeValidators(type: AccountType): void {
    const unitIdControl = this.form.controls.unitId;
    const supplierIdControl = this.form.controls.supplierId;
    if (type === 'RECEIVABLE') {
      supplierIdControl.clearValidators();
      supplierIdControl.setValue(null);
      if (!this.bulkMode()) {
        unitIdControl.setValidators([Validators.required]);
      }
    } else {
      unitIdControl.clearValidators();
      unitIdControl.setValue(null);
      supplierIdControl.setValidators([Validators.required]);
    }
    unitIdControl.updateValueAndValidity();
    supplierIdControl.updateValueAndValidity();
  }
}
