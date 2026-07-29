import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-row-actions',
  imports: [RouterLink],
  templateUrl: './row-actions.html',
  styleUrl: './row-actions.scss',
})
export class RowActions {
  readonly editLink = input.required<(string | number)[]>();
  readonly remove = output<void>();
}
