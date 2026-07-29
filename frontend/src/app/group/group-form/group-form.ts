import { Component, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { Party } from '../../shared/models/party.model';
import { GroupService } from '../../shared/services/group.service';
import { PartyService } from '../../shared/services/party.service';

@Component({
  selector: 'app-group-form',
  imports: [ReactiveFormsModule],
  templateUrl: './group-form.html',
  styleUrl: './group-form.scss',
})
export class GroupForm implements OnInit {
  protected readonly parties = signal<Party[]>([]);
  protected readonly selectedPartyIds = signal<ReadonlySet<number>>(new Set());
  protected readonly errorMessage = signal<string | null>(null);
  protected isEditMode = false;
  private groupId: number | null = null;

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor(
    private readonly partyService: PartyService,
    private readonly groupService: GroupService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.partyService.findAll().subscribe((parties) => this.parties.set(parties));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.groupId = Number(idParam);
      this.isEditMode = true;
      this.groupService.findById(this.groupId).subscribe((group) => {
        this.form.setValue({ name: group.name });
        this.selectedPartyIds.set(new Set(group.members.map((party) => party.id)));
      });
    }
  }

  isPartySelected(party: Party): boolean {
    return this.selectedPartyIds().has(party.id);
  }

  toggleParty(party: Party): void {
    const next = new Set(this.selectedPartyIds());
    if (next.has(party.id)) {
      next.delete(party.id);
    } else {
      next.add(party.id);
    }
    this.selectedPartyIds.set(next);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);
    const request = {
      name: this.form.getRawValue().name,
      partyIds: Array.from(this.selectedPartyIds()),
    };

    const result = this.groupId
      ? this.groupService.update(this.groupId, request)
      : this.groupService.create(request);

    result.subscribe({
      next: () => this.router.navigateByUrl('/groups'),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
