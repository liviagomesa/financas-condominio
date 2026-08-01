# Frontend Interface Contracts: Duplicar lançamentos para o mês seguinte

## `BulkActionsBar` (`shared/components/bulk-actions-bar/bulk-actions-bar.ts`) — extensão aditiva

Reaproveitada pelas quatro listagens (`account-list`, `party-list`, `fund-list`, `group-list`); esta feature adiciona duas ações opcionais, usadas apenas por `account-list`. Ver research.md ("`BulkActionsBar` ganha duas ações extras opcionais") para a justificativa.

### Interface atual (antes desta feature)

```typescript
export class BulkActionsBar {
  readonly selectedCount = input.required<number>();
  readonly entityLabelPlural = input.required<string>();
  readonly remove = output<void>();
}
```

### Interface alterada (esta feature)

```typescript
export class BulkActionsBar {
  readonly selectedCount = input.required<number>();
  readonly entityLabelPlural = input.required<string>();
  readonly showDuplicateActions = input<boolean>(false); // novo — opcional, default false
  readonly remove = output<void>();
  readonly duplicate = output<void>();                   // novo — "Duplicar para o mês seguinte"
  readonly duplicateZeroed = output<void>();              // novo — "...com valor zerado"
}
```

- Quando `showDuplicateActions()` é `false` (valor padrão, usado implicitamente por `party-list`/`fund-list`/`group-list`), a barra se comporta exatamente como hoje — nenhuma mudança visual ou de comportamento nessas três telas.
- Quando `showDuplicateActions()` é `true` (só `account-list`), a barra exibe dois botões adicionais, que emitem `duplicate`/`duplicateZeroed` ao serem clicados — sem diálogo de confirmação (duplicar não é uma ação destrutiva, mesmo critério já aplicado a "Registrar pagamento").

### Uso em `account-list.html` (novo)

```html
<app-bulk-actions-bar
  [selectedCount]="selection.selectedCount()"
  entityLabelPlural="conta(s)"
  [showDuplicateActions]="true"
  (remove)="removeSelected()"
  (duplicate)="duplicateSelected(false)"
  (duplicateZeroed)="duplicateSelected(true)"
/>
```

### Uso em `party-list.html` / `fund-list.html` / `group-list.html` — inalterado

```html
<app-bulk-actions-bar
  [selectedCount]="selection.selectedCount()"
  entityLabelPlural="..."
  (remove)="removeSelected()"
/>
```

## `AccountService` (`shared/services/account.service.ts`) — novo método

```typescript
duplicate(id: number, request: AccountDuplicateRequest): Observable<Account> {
  return this.http.post<Account>(`${this.baseUrl}/${id}/duplicate`, request);
}
```

## `shared/models/account.model.ts` — novo tipo

```typescript
export interface AccountDuplicateRequest {
  zeroAmount: boolean;
}
```

## `shared/bulk-duplicate.ts` (novo arquivo, mesmo padrão de `shared/bulk-delete.ts`)

```typescript
export interface BulkDuplicateResult {
  succeeded: Account[];
  failed: { id: number; message: string }[];
}

export function bulkDuplicate(
  ids: number[],
  duplicateFn: (id: number) => Observable<Account>
): Observable<BulkDuplicateResult> { /* mesma estrutura de forkJoin + catchError de bulkDelete */ }
```

## `AccountList` (`account/account-list/account-list.ts`) — novos membros

```typescript
export class AccountList implements OnInit {
  // ... membros já existentes ...
  protected readonly copiedIds = signal<number[]>([]);

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void { /* ver data-model.md e research.md */ }

  duplicateSelected(zeroAmount: boolean): void {
    this.performDuplicate(Array.from(this.selection.selectedIds()), zeroAmount);
  }

  private performDuplicate(ids: number[], zeroAmount: boolean): void { /* chama bulkDuplicate, reusa padrão de removeSelected */ }
}
```

`duplicateSelected` é acionado tanto pelos botões da `BulkActionsBar` (`false`/`true`) quanto, indiretamente via `performDuplicate(this.copiedIds(), false)`, pelo atalho Ctrl+V.
