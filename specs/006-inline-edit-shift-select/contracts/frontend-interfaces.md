# Frontend Interface Contract: `Selection<T>` (`shared/list-selection.ts`)

Esta feature não expõe uma API REST nova (ver `contracts/api.md`), mas altera um contrato interno relevante: a interface `Selection<T>` consumida pelas quatro listagens com caixas de seleção (`account-list`, `party-list`, `fund-list`, `group-list`). Documentado aqui por ser o "contrato compartilhado entre telas" mencionado explicitamente no pedido da usuária (FR-014).

## Interface atual (antes desta feature)

```typescript
export interface Selection<T> {
  readonly selectedIds: Signal<ReadonlySet<number>>;
  readonly selectedCount: Signal<number>;
  isSelected(item: T): boolean;
  toggle(item: T): void;
  toggleAll(items: T[]): void;
  isAllSelected(items: T[]): boolean;
  clear(): void;
}
```

## Interface alterada (esta feature)

```typescript
export interface Selection<T> {
  readonly selectedIds: Signal<ReadonlySet<number>>;
  readonly selectedCount: Signal<number>;
  isSelected(item: T): boolean;
  toggle(item: T): void;                                        // inalterado — clique normal isolado
  toggleWithRange(item: T, items: T[], shiftKey: boolean): void; // novo — usado pelo (click) da checkbox de linha
  toggleAll(items: T[]): void;                                   // comportamento novo: também reseta a âncora (FR-012)
  isAllSelected(items: T[]): boolean;
  clear(): void;                                                 // comportamento novo: também reseta a âncora (FR-013)
}
```

### `toggleWithRange(item, items, shiftKey)`

- **Quando `shiftKey` é falso, ou é verdadeiro mas não há âncora definida ainda**: comporta-se como `toggle(item)` hoje se comporta, e define a âncora como o id de `item` (FR-008/FR-009/FR-011).
- **Quando `shiftKey` é verdadeiro e há uma âncora definida**: marca (adiciona a `selectedIds`) todos os itens de `items` cujo índice está entre o índice da âncora e o índice de `item` (inclusive, em qualquer ordem), sem desmarcar nada fora desse intervalo e sem mover a âncora (FR-009a/FR-010).

## Uso nos templates das quatro listagens (antes → depois)

**Checkbox de linha** — antes:
```html
<input type="checkbox" [checked]="selection.isSelected(item)" (change)="selection.toggle(item)" />
```

**Checkbox de linha** — depois:
```html
<input type="checkbox" [checked]="selection.isSelected(item)" (click)="selection.toggleWithRange(item, items(), $event.shiftKey)" />
```

**Checkbox "selecionar todas" (cabeçalho)** — inalterada na assinatura do template; só o comportamento interno de `toggleAll` muda (reset da âncora):
```html
<input type="checkbox" [checked]="selection.isAllSelected(items())" (change)="selection.toggleAll(items())" />
```

Este padrão é idêntico nas quatro listagens (`accounts()`, `parties()`, `funds()`, `groups()` no lugar de `items()`), satisfazendo FR-014 sem duplicar lógica — toda a regra de âncora/intervalo vive uma única vez em `createSelection<T>()`.
