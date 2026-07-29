import { Component, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { AccountType, ACCOUNT_TYPE_LABELS } from '../../shared/models/account.model';
import { Fund } from '../../shared/models/fund.model';
import { Group } from '../../shared/models/group.model';
import { Party } from '../../shared/models/party.model';
import { AccountService } from '../../shared/services/account.service';
import { FundService } from '../../shared/services/fund.service';
import { GroupService } from '../../shared/services/group.service';
import { PartyService } from '../../shared/services/party.service';

@Component({
  selector: 'app-account-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './account-form.html',
  styleUrl: './account-form.scss',
})
export class AccountForm implements OnInit {
  protected readonly parties = signal<Party[]>([]);
  protected readonly groups = signal<Group[]>([]);
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
    partyId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    groupId: new FormControl<number | null>(null),
    paymentDate: new FormControl('', { nonNullable: true }),
    observations: new FormControl('', { nonNullable: true }),
  });

  constructor(
    private readonly partyService: PartyService,
    private readonly groupService: GroupService,
    private readonly fundService: FundService,
    private readonly accountService: AccountService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.partyService.findAll().subscribe((parties) => this.parties.set(parties));
    this.groupService.findAll().subscribe((groups) => this.groups.set(groups));
    this.fundService.findAll().subscribe((funds) => this.funds.set(funds));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.accountId = Number(idParam);
      this.isEditMode = true;
      this.accountService.findById(this.accountId).subscribe((account) => {
        this.form.setValue({
          type: account.type,
          amount: account.amount,
          dueDate: account.dueDate,
          description: account.description,
          fundId: account.fund.id,
          recurring: account.recurring,
          partyId: account.party.id,
          groupId: null,
          paymentDate: account.paymentDate ?? '',
          observations: account.observations ?? '',
        });
        this.form.controls.type.disable();
      });
    }
  }

  setBulkMode(bulk: boolean): void {
    this.bulkMode.set(bulk);
    const partyIdControl = this.form.controls.partyId;
    const groupIdControl = this.form.controls.groupId;
    if (bulk) {
      partyIdControl.clearValidators();
      partyIdControl.setValue(null);
      groupIdControl.setValidators([Validators.required]);
    } else {
      groupIdControl.clearValidators();
      groupIdControl.setValue(null);
      partyIdControl.setValidators([Validators.required]);
    }
    partyIdControl.updateValueAndValidity();
    groupIdControl.updateValueAndValidity();
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
      this.accountService
        .createBulk({ ...shared, type: raw.type, groupId: raw.groupId as number })
        .subscribe({ next: onSuccess, error: onError });
    } else if (this.accountId) {
      this.accountService
        .update(this.accountId, { ...shared, type: raw.type, partyId: raw.partyId as number })
        .subscribe({ next: onSuccess, error: onError });
    } else {
      this.accountService
        .create({ ...shared, type: raw.type, partyId: raw.partyId as number })
        .subscribe({ next: onSuccess, error: onError });
    }
  }
}
