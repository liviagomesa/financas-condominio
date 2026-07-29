import { Component, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiError } from '../../core/error.interceptor';
import { PartyService } from '../../shared/services/party.service';

@Component({
  selector: 'app-party-form',
  imports: [ReactiveFormsModule],
  templateUrl: './party-form.html',
  styleUrl: './party-form.scss',
})
export class PartyForm implements OnInit {
  protected readonly errorMessage = signal<string | null>(null);
  protected isEditMode = false;
  private partyId: number | null = null;

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    pixKey: new FormControl('', { nonNullable: true }),
  });

  constructor(
    private readonly partyService: PartyService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.partyId = Number(idParam);
      this.isEditMode = true;
      this.partyService.findById(this.partyId).subscribe((party) => {
        this.form.setValue({ name: party.name, pixKey: party.pixKey ?? '' });
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
    const request = { name: raw.name, pixKey: raw.pixKey || null };
    const result = this.partyId
      ? this.partyService.update(this.partyId, request)
      : this.partyService.create(request);

    result.subscribe({
      next: () => this.router.navigateByUrl('/parties'),
      error: (err: ApiError) => this.errorMessage.set(err.message),
    });
  }
}
