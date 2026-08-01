# Data Model: Edição inline de valor e seleção em intervalo com Shift

Esta feature não introduz nem altera nenhuma entidade persistida — `Account` (schema, validação, endpoints) permanece exatamente como está hoje (ver research.md, "Edição inline reaproveita o endpoint `PUT /api/accounts/{id}`"). O que muda é o **estado efêmero de interface** mantido no frontend, descrito abaixo por completude, já que orienta diretamente o desenho de `shared/list-selection.ts` e de `account-list.ts`.

## Âncora de seleção em intervalo (`Selection<T>`, `shared/list-selection.ts`)

Estado interno novo, não persistido, mantido por instância de `createSelection<T>()` — ou seja, um por listagem (`account-list`, `party-list`, `fund-list`, `group-list`), sem compartilhamento entre elas.

| Campo | Tipo | Descrição |
|---|---|---|
| `anchorId` | `number \| null` | Id do item do último clique **normal** (sem Shift) numa checkbox de linha. `null` quando nenhuma linha foi clicada individualmente ainda, ou depois de um reset. |

**Transições**:
- Clique normal numa linha → `anchorId` passa a ser o id dessa linha (FR-008/FR-009).
- Shift+clique com `anchorId` definido → marca o intervalo entre `anchorId` e a linha clicada (inclusive); `anchorId` **não muda** (FR-009a).
- Shift+clique com `anchorId` nulo → se comporta como clique normal: marca só a linha clicada e define `anchorId` (FR-011).
- `toggleAll()` (checkbox "selecionar todas") → `anchorId` volta a `null` (FR-012).
- `clear()` (ex.: chamado hoje por `onFilterChange()` em cada listagem) → `anchorId` volta a `null` (FR-013).

**Regra de seleção em intervalo**: dado `items` (ordem atualmente exibida na tela) e os ids de `anchorId` e do item clicado, todos os ids de `items` cujo índice está entre os dois índices (inclusive, em qualquer ordem de clique) são **adicionados** a `selectedIds` — nunca removidos, mesmo que já estivessem marcados fora do intervalo (FR-010).

## Edição inline do campo "Valor" (`account-list.ts`)

Estado efêmero adicional na tela de Contas, análogo ao já existente `payingId` (usado pelo fluxo "Registrar pagamento").

| Campo | Tipo | Descrição |
|---|---|---|
| `editingAmountId` | `number \| null` | Id da conta cujo campo "Valor" está em modo de edição inline no momento. `null` quando nenhuma edição está em andamento. |
| `amountDraft` | `string` | Valor digitado no campo, antes de confirmar — espelha o papel já existente de `paymentDateDraft`. |
| `amountEditError` | `string \| null` | Mensagem de erro exibida junto ao campo quando `amountDraft` é inválido (FR-004). Distinto do `errorMessage` já existente na tela (usado só para o banner de erro no topo da página, não para erros de campo). `null` quando não há erro. |

**Transições**:
- Clicar no campo "Valor" de uma linha → `editingAmountId` passa a ser o id dessa conta; `amountDraft` é inicializado com o valor atual formatado para o `<input type="number">`; `amountEditError` é limpo (`null`); `payingId` é limpo incondicionalmente (FR-005, exclusão mútua com "Registrar pagamento").
- Confirmar (Enter ou blur) com valor válido → chama `PUT /api/accounts/{id}` com o `AccountRequest` da linha, só com `amount` alterado; em caso de sucesso, `editingAmountId` e `amountEditError` voltam a `null` e a linha/rodapé são atualizados a partir da resposta (FR-002/FR-006).
- Confirmar com valor inválido (vazio, negativo ou não numérico) → `editingAmountId` permanece com o mesmo id; `amountEditError` é definido como `"O valor é obrigatório e não pode ser negativo."`, exibida junto ao campo; nenhuma chamada à API é feita (FR-004).
- Pressionar Esc → `editingAmountId` e `amountEditError` voltam a `null`; `amountDraft` é descartado sem chamar a API (FR-003).
- Iniciar uma nova edição de valor em outra linha, ou iniciar "Registrar pagamento" na mesma linha → `editingAmountId`/`amountEditError` são sobrescritos (ou zerados, no caso do pagamento) sem salvar a edição anterior (FR-005).
- Mudança de filtro (`onFilterChange()`, já limpa `selection` hoje) → também zera `editingAmountId` e `amountEditError`, já que a lista de linhas exibida muda (Edge Cases).

## Diagrama (referência)

```text
Selection<T>  (um por listagem: account-list, party-list, fund-list, group-list)
  selectedIds   Set<number>        — inalterado
  anchorId      number | null      — novo, interno

AccountList (componente)
  editingAmountId   number | null  — novo
  amountDraft       string         — novo
  amountEditError   string | null  — novo
  payingId          number | null  — já existente; agora mutuamente exclusivo com editingAmountId
  paymentDateDraft  string         — já existente
```

Nenhum desses campos é enviado ao backend nem persistido — são inteiramente descartados ao sair da tela ou recarregar.
