---

description: "Task list template for feature implementation"

---

# Tasks: Edição inline de valor e seleção em intervalo com Shift

**Input**: Design documents from `/specs/006-inline-edit-shift-select/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [contracts/frontend-interfaces.md](./contracts/frontend-interfaces.md), [quickstart.md](./quickstart.md)

**Tests**: A constituição do projeto (Princípio III) exige cobertura de teste automatizado para toda regra de negócio nova. Esta feature introduz uma regra de negócio de UI genuinamente nova (âncora + seleção em intervalo, `shared/list-selection.ts`), que ganha um `list-selection.spec.ts` dedicado (Vitest puro, mesmo padrão de `bulk-delete.spec.ts`). A validação do campo "Valor" reaproveita uma regra já coberta por `AccountServiceTest` no backend (mesmo endpoint `PUT /api/accounts/{id}`, sem alteração — ver research.md), então `account-list.ts` não ganha um spec dedicado, seguindo o mesmo padrão já usado no projeto (nenhum componente Angular tem hoje teste unitário próprio). A validação final de ambas as user stories acontece via o roteiro de `quickstart.md` (Playwright/navegador, Princípio III).

**Organization**: Tarefas agrupadas por user story (spec.md) para permitir implementação e teste independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1, US2)
- Caminhos de arquivo exatos estão incluídos em cada descrição

## Path Conventions (Web app — ver plan.md)

- Esta feature é inteiramente frontend: `frontend/src/app/shared/list-selection.ts` (alterado), `frontend/src/app/account/account-list/` (alterado), `frontend/src/app/party/party-list/`, `.../fund/fund-list/`, `.../group/group-list/` (templates alterados)
- `backend/` não é tocado por esta feature

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto — não se aplica: `frontend/` já existe (feature 001) e nenhuma dependência nova é necessária (ver plan.md, Technical Context).

Nenhuma tarefa nova nesta fase.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura bloqueante compartilhada por todas as user stories — não se aplica a esta feature: User Story 1 (edição inline do "Valor") e User Story 2 (seleção em intervalo) tocam arquivos completamente distintos (`account-list.ts`/`.html` vs. `shared/list-selection.ts` + 4 templates) e não têm nenhuma dependência real entre si.

Nenhuma tarefa nova nesta fase.

---

## Phase 3: User Story 1 - Editar o valor de uma conta direto na listagem (Priority: P1) 🎯 MVP

**Goal**: Permitir clicar no campo "Valor" de uma linha na listagem de contas, digitar um novo valor e confirmá-lo (Enter ou clicando fora), sem sair da tela nem abrir a edição completa.

**Independent Test**: Em `/accounts`, clicar no valor de uma conta, digitar um novo valor e confirmar — a linha e o "Total líquido" do rodapé devem refletir o novo valor sem navegação. Testar também Esc (cancela) e um valor negativo/vazio (rejeitado com mensagem, sem sair do modo de edição).

### Implementation for User Story 1

- [X] T001 [US1] Add estado de edição inline (`editingAmountId: Signal<number | null>`, `amountDraft: string`, `amountEditError: Signal<string | null>` — ver data-model.md) e métodos `startAmountEdit(account)`, `confirmAmountEdit(account)`, `cancelAmountEdit()` em `frontend/src/app/account/account-list/account-list.ts` — `startAmountEdit` inicializa `amountDraft` com o valor atual, limpa `amountEditError` e zera `payingId` incondicionalmente (FR-005); `confirmAmountEdit` valida `amountDraft` (obrigatório, numérico, `>= 0`, mesma regra de `account-form`), definindo `amountEditError` como `"O valor é obrigatório e não pode ser negativo."` e retornando sem chamar a API quando inválido (FR-004), ou chamando `accountService.update(account.id, { ...account, amount: <valor> })` quando válido, atualizando a lista local (via `accounts.set`/`update`, garantindo que `netTotal` recompute) e zerando `editingAmountId`/`amountEditError` em caso de sucesso (FR-002/FR-006); `cancelAmountEdit` zera `editingAmountId` e `amountEditError` sem chamar a API (FR-003); ajusta `startPayment` (já existente) para também zerar `editingAmountId`/`amountEditError` incondicionalmente (FR-005, exclusão mútua); ajusta `onFilterChange()` (já existente, já chama `selection.clear()`) para também zerar `editingAmountId`/`amountEditError`, já que a lista de linhas exibida muda (Edge Cases do spec, data-model.md)
- [X] T002 [US1] Update célula "Valor" em `frontend/src/app/account/account-list/account-list.html`: quando `editingAmountId() === account.id`, exibe `<input type="number" step="0.01" min="0">` ligado a `amountDraft` com `(keydown.enter)`/`(blur)` chamando `confirmAmountEdit(account)`, `(keydown.escape)` chamando `cancelAmountEdit()`, e `amountEditError()` exibido junto ao campo quando não nulo; caso contrário, exibe o valor formatado atual (`{{ account.amount | number: '1.2-2' }}`) com `(click)="startAmountEdit(account)"` (depends on T001)

**Checkpoint**: User Story 1 completa e testável de forma independente (Acceptance Scenarios 1-4 do spec).

---

## Phase 4: User Story 2 - Selecionar um intervalo de linhas segurando Shift (Priority: P2)

**Goal**: Permitir marcar a primeira e a última caixinha de um intervalo segurando Shift, marcando automaticamente todas as linhas entre elas, em qualquer listagem do sistema com seleção múltipla.

**Independent Test**: Em qualquer listagem com caixas de seleção (ex.: `/parties`), marcar a caixinha da 2ª linha (clique normal) e, segurando Shift, marcar a caixinha da 6ª linha — as linhas 2 a 6 devem ficar marcadas. Testar também Shift+clique sem clique normal anterior (comporta-se como clique normal) e que "selecionar todas" reinicia a referência de intervalo.

### Implementation for User Story 2

- [X] T003 [P] [US2] Extend `Selection<T>` em `frontend/src/app/shared/list-selection.ts`: adiciona signal interno de âncora (`anchorId: number | null`, não exposto na interface pública) e o método novo `toggleWithRange(item: T, items: T[], shiftKey: boolean): void` — quando `shiftKey` é falso ou não há âncora definida, comporta-se como `toggle(item)` já se comporta hoje e define a âncora como o id de `item` (FR-008/FR-009/FR-011); quando `shiftKey` é verdadeiro e há âncora definida, adiciona a `selectedIds` todos os ids de `items` cujo índice está entre o índice da âncora e o de `item` (inclusive, sem desmarcar nada fora do intervalo, sem mover a âncora — FR-009a/FR-010); `toggleAll` e `clear` (já existentes) passam a também resetar a âncora para `null` (FR-012/FR-013)
- [X] T004 [P] [US2] Create `frontend/src/app/shared/list-selection.spec.ts` (Vitest puro, sem `TestBed`, mesmo padrão de `bulk-delete.spec.ts`): clique normal (`toggleWithRange` com `shiftKey = false`) define a âncora e marca só aquela linha; Shift+clique sem âncora se comporta como clique normal, sem erro (FR-011); Shift+clique com âncora marca todas as linhas do intervalo (em ambas as direções — âncora antes ou depois do item clicado) sem desmarcar linhas fora do intervalo já marcadas (FR-009/FR-010); Shift+cliques sucessivos não movem a âncora, recalculando sempre a partir do mesmo ponto de partida (FR-009a); `toggleAll` e `clear` resetam a âncora, fazendo o próximo Shift+clique se comportar como clique normal (FR-012/FR-013) (depends on T003)
- [X] T005 [P] [US2] Update checkbox de linha em `frontend/src/app/account/account-list/account-list.html`: troca `(change)="selection.toggle(account)"` por `(click)="selection.toggleWithRange(account, accounts(), $event.shiftKey)"` (depends on T003)
- [X] T006 [P] [US2] Update checkbox de linha em `frontend/src/app/party/party-list/party-list.html`: troca `(change)="selection.toggle(party)"` por `(click)="selection.toggleWithRange(party, parties(), $event.shiftKey)"` (depends on T003)
- [X] T007 [P] [US2] Update checkbox de linha em `frontend/src/app/fund/fund-list/fund-list.html`: troca `(change)="selection.toggle(fund)"` por `(click)="selection.toggleWithRange(fund, funds(), $event.shiftKey)"` (depends on T003)
- [X] T008 [P] [US2] Update checkbox de linha em `frontend/src/app/group/group-list/group-list.html`: troca `(change)="selection.toggle(group)"` por `(click)="selection.toggleWithRange(group, groups(), $event.shiftKey)"` (depends on T003)

**Checkpoint**: User Story 2 completa e testável de forma independente (Acceptance Scenarios 1-5 do spec), idêntica nas quatro listagens.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validação final e documentação

- [X] T009 [P] Run roteiro de validação manual de `quickstart.md` de ponta a ponta (navegador, Playwright já configurado como devDependency), cobrindo os 10 cenários (edição inline do "Valor" e seleção em intervalo nas quatro listagens)
- [X] T010 [P] Update `README.md` com as decisões técnicas desta feature (edição inline do "Valor" reaproveitando `PUT /api/accounts/{id}` sem endpoint novo; extensão de `Selection<T>` com âncora + `toggleWithRange` compartilhada pelas quatro listagens), conforme Checkpoints da Rodada de Trabalho da constituição

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem tarefas novas
- **Foundational (Phase 2)**: Sem tarefas novas — US1 e US2 não têm nenhuma dependência real entre si
- **User Story 1 (Phase 3)**: Sem dependências de outras stories
- **User Story 2 (Phase 4)**: Sem dependências de outras stories — pode ser implementada antes, depois ou em paralelo com a User Story 1
- **Polish (Phase 5)**: Depende de ambas as user stories estarem completas

### Parallel Opportunities

- T001/T002 (US1) podem ser feitas em paralelo com toda a Phase 4 (US2) — arquivos completamente distintos
- T004, T005, T006, T007, T008 (US2) podem rodar em paralelo entre si, todas dependendo apenas de T003
- T009, T010 (Polish) podem rodar em paralelo entre si, depois de ambas as stories completas

---

## Parallel Example: User Story 2

```bash
# Depois de T003 (extensão de list-selection.ts) completa, em paralelo:
Task: "Create list-selection.spec.ts em frontend/src/app/shared/"
Task: "Update checkbox de linha em frontend/src/app/account/account-list/account-list.html"
Task: "Update checkbox de linha em frontend/src/app/party/party-list/party-list.html"
Task: "Update checkbox de linha em frontend/src/app/fund/fund-list/fund-list.html"
Task: "Update checkbox de linha em frontend/src/app/group/group-list/group-list.html"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 3: User Story 1 (edição inline do "Valor")
2. **STOP and VALIDATE**: testar US1 isoladamente (cenários 1-4 do spec)

### Incremental Delivery

1. US1 → edição inline do "Valor" na listagem de contas (MVP)
2. US2 → seleção em intervalo com Shift, idêntica nas quatro listagens
3. Polish → validação de ponta a ponta via `quickstart.md` + atualização do README

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes
- US1 e US2 são inteiramente independentes entre si — podem ser implementadas em qualquer ordem
- Commit ao final de cada tarefa ou grupo lógico de tarefas
- Pare em qualquer checkpoint para validar a story isoladamente antes de seguir
