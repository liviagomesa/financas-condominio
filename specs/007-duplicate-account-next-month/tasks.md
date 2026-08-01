---

description: "Task list template for feature implementation"

---

# Tasks: Duplicar lançamentos para o mês seguinte

**Input**: Design documents from `/specs/007-duplicate-account-next-month/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [contracts/frontend-interfaces.md](./contracts/frontend-interfaces.md), [quickstart.md](./quickstart.md)

**Tests**: A constituição do projeto (Princípio III) exige cobertura de teste automatizado para toda regra de negócio nova. Esta feature introduz duas regras de negócio novas testáveis automaticamente: (1) `AccountService.duplicate` no backend (cópia de campos, cálculo de vencimento com virada de mês, zeragem condicional de valor, pagamento sempre nulo), coberta por casos novos em `AccountServiceTest`; (2) `bulkDuplicate` no frontend (mesmo padrão de melhor esforço de `bulkDelete`), coberta por um `bulk-duplicate.spec.ts` novo (Vitest puro). A lógica de teclado (`onKeydown`) e a integração de `account-list.ts`/`.html` não ganham teste unitário dedicado, seguindo o mesmo padrão já usado no projeto (nenhum componente Angular tem hoje teste próprio, precedente registrado no `research.md` da feature 006) — validadas via o roteiro de `quickstart.md` (Playwright/navegador, Princípio III).

**Organization**: Tarefas agrupadas por user story (spec.md) para permitir implementação e teste independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1, US2)
- Caminhos de arquivo exatos estão incluídos em cada descrição

## Path Conventions (Web app — ver plan.md)

- Backend: `backend/src/main/java/com/financas/account/` (`api/`, `domain/`), `backend/src/test/java/com/financas/account/domain/`
- Frontend: `frontend/src/app/shared/` (models, services, `bulk-duplicate.ts`, `components/bulk-actions-bar/`), `frontend/src/app/account/account-list/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto — não se aplica: backend e frontend já existem (feature 001) e nenhuma dependência nova é necessária (ver plan.md, Technical Context).

Nenhuma tarefa nova nesta fase.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Endpoint de backend e infraestrutura de frontend compartilhados pelas duas user stories — "duplicar mantendo o valor" e "duplicar com valor zerado" são a mesma operação (`AccountService.duplicate`) parametrizada por `zeroAmount`, e ambos os botões vivem no mesmo `BulkActionsBar` estendido. Nenhuma user story pode ser validada de ponta a ponta sem esta fase completa.

**⚠️ CRITICAL**: Nenhuma implementação de user story deve começar antes desta fase estar completa.

- [ ] T001 [P] Create `AccountDuplicateRequest` record (`{ boolean zeroAmount }`, sem validação adicional necessária) em `backend/src/main/java/com/financas/account/api/AccountDuplicateRequest.java`, seguindo o mesmo padrão de `AccountPaymentRequest.java`
- [ ] T002 [P] Add método `duplicate(Long id, boolean zeroAmount)` em `backend/src/main/java/com/financas/account/domain/AccountService.java`: lê o `Account` original via `findById` (já existente, lança `NotFoundException` se não encontrado) e persiste um `Account` novo via `repository.save(new Account(...))` copiando `type`, `description`, `fund`, `recurring`, `party`, `observations` do original sem alteração; `dueDate` da cópia é `original.getDueDate().plusMonths(1)` (FR-004, cobre a virada de mês curto do Edge Case sem tratamento manual, ver research.md); `paymentDate` da cópia é sempre `null`, independentemente do original estar pago (FR-004); `amount` da cópia é `original.getAmount()` quando `zeroAmount` é `false`, ou `BigDecimal.ZERO` quando `true` (FR-005); método não precisa de `@Transactional` (uma única escrita, ver research.md)
- [ ] T003 Add endpoint `POST /{id}/duplicate` em `backend/src/main/java/com/financas/account/api/AccountController.java`: recebe `@Valid @RequestBody AccountDuplicateRequest`, chama `service.duplicate(id, request.zeroAmount())`, retorna `ResponseEntity.status(HttpStatus.CREATED).body(toResponse(account))` (mesmo padrão de `create`/`createBulk`) (depends on T001, T002)
- [ ] T004 [P] Add casos de teste para `duplicate` em `backend/src/test/java/com/financas/account/domain/AccountServiceTest.java` (JUnit 5 + Mockito, cobertura obrigatória de regra de negócio, Princípio III): cópia mantém `type`/`description`/`fund`/`recurring`/`party`/`observations` do original; `dueDate` da cópia é um mês após o original em caso comum (ex.: 10/03 → 10/04); virada de mês curto (31/01 → 28 ou 29/02); `paymentDate` da cópia é sempre `null`, inclusive quando o original está pago; `zeroAmount = false` mantém o `amount` original; `zeroAmount = true` resulta em `amount = BigDecimal.ZERO`; `NotFoundException` para id inexistente; o `Account` original permanece inalterado após a chamada (depends on T002)
- [ ] T005 [P] Add interface `AccountDuplicateRequest { zeroAmount: boolean }` em `frontend/src/app/shared/models/account.model.ts`, junto dos demais tipos de `Account`
- [ ] T006 Add método `duplicate(id: number, request: AccountDuplicateRequest): Observable<Account>` (`POST ${baseUrl}/${id}/duplicate`) em `frontend/src/app/shared/services/account.service.ts` (depends on T005)
- [ ] T007 [P] Create `frontend/src/app/shared/bulk-duplicate.ts`: função `bulkDuplicate(ids: number[], duplicateFn: (id: number) => Observable<Account>): Observable<BulkDuplicateResult>` com `BulkDuplicateResult = { succeeded: Account[]; failed: { id: number; message: string }[] }`, estrutura idêntica a `bulk-delete.ts` (`forkJoin` + `catchError`, retorna lista vazia de sucesso/falha quando `ids` é vazio) — implementa o padrão de melhor esforço exigido por FR-007
- [ ] T008 [P] Create `frontend/src/app/shared/bulk-duplicate.spec.ts` (Vitest puro, sem `TestBed`, mesmo padrão de `bulk-delete.spec.ts`, cobertura obrigatória de regra de negócio, Princípio III): todos os ids duplicados com sucesso retornam `succeeded` completo e `failed` vazio; uma falha entre várias retorna a duplicação bem-sucedida em `succeeded` e o erro (id + mensagem) em `failed`, sem interromper as demais chamadas; lista de ids vazia retorna `{ succeeded: [], failed: [] }` sem chamar `duplicateFn` (depends on T007)
- [ ] T009 [P] Extend `frontend/src/app/shared/components/bulk-actions-bar/bulk-actions-bar.ts` e `.html`: novo input `showDuplicateActions = input<boolean>(false)` e novos outputs `duplicate = output<void>()` / `duplicateZeroed = output<void>()`; o template só renderiza os dois botões novos ("Duplicar para o mês seguinte" / "Duplicar para o mês seguinte (valor zerado)") quando `showDuplicateActions()` é `true`, sem diálogo de confirmação (duplicar não é destrutivo); quando `showDuplicateActions()` é `false` (valor padrão, usado implicitamente por `party-list`/`fund-list`/`group-list`), o componente permanece idêntico ao comportamento atual — nenhuma mudança nos usos existentes

**Checkpoint**: Endpoint de duplicação e infraestrutura compartilhada de frontend prontos — as duas user stories podem ser implementadas.

---

## Phase 3: User Story 1 - Duplicar lançamento mantendo o valor (Priority: P1) 🎯 MVP

**Goal**: Selecionar um ou mais lançamentos em `/accounts` e duplicá-los para o mês seguinte mantendo o valor original, como pendente — pelo botão "Duplicar para o mês seguinte" ou pelo atalho de teclado Ctrl+C (memoriza a seleção) seguido de Ctrl+V (dispara a duplicação).

**Independent Test**: Em `/accounts`, selecionar um lançamento, clicar em "Duplicar para o mês seguinte" e confirmar que surge uma cópia com vencimento um mês depois, mesmo valor e demais dados, sem pagamento registrado, e que o original não muda. Repetir usando Ctrl+C seguido de Ctrl+V em vez do botão, e confirmar que Ctrl+C/Ctrl+V não disparam nada quando o foco está num campo de edição inline.

### Implementation for User Story 1

- [ ] T010 [US1] Add `duplicateSelected(zeroAmount: boolean): void` e `performDuplicate(ids: number[], zeroAmount: boolean): void` (privado) em `frontend/src/app/account/account-list/account-list.ts`: `duplicateSelected` chama `performDuplicate(Array.from(this.selection.selectedIds()), zeroAmount)`; `performDuplicate` chama `bulkDuplicate(ids, (id) => this.accountService.duplicate(id, { zeroAmount }))` (T006, T007) e, no `subscribe`, monta a mesma mensagem de erro de falha parcial já usada por `removeSelected` quando `result.failed.length > 0` (FR-007), limpa `this.selection` e recarrega a lista via `this.load()` em qualquer caso (com ou sem falhas parciais), sem alterar o filtro atual (FR-008) (depends on T006, T007)
- [ ] T011 [US1] Add `copiedIds = signal<number[]>([])` e `@HostListener('document:keydown', ['$event']) onKeydown(event: KeyboardEvent): void` em `frontend/src/app/account/account-list/account-list.ts`: se `document.activeElement` for `INPUT`/`TEXTAREA` ou tiver `isContentEditable`, retorna imediatamente sem efeito (FR-011); caso contrário, `Ctrl+C` (`event.ctrlKey && event.key.toLowerCase() === 'c'`) com `this.selection.selectedIds().size > 0` grava `this.copiedIds.set(Array.from(this.selection.selectedIds()))` (FR-009, sobrescreve qualquer memorização anterior); `Ctrl+V` (`event.ctrlKey && event.key.toLowerCase() === 'v'`) com `this.copiedIds().length > 0` chama `event.preventDefault()` e `this.performDuplicate(this.copiedIds(), false)` — sempre com `zeroAmount = false`, já que o atalho só cobre a variante "valor mantido" (FR-010) — sem limpar `copiedIds` (permite colar mais de uma vez); `Ctrl+V` sem nada memorizado não tem efeito (Edge Case) (depends on T010)
- [ ] T012 [US1] Update `<app-bulk-actions-bar>` em `frontend/src/app/account/account-list/account-list.html`: adiciona `[showDuplicateActions]="true"` e `(duplicate)="duplicateSelected(false)"` (depends on T009, T010)

**Checkpoint**: User Story 1 completa e testável de forma independente (Acceptance Scenarios 1-4 do spec, botão e atalho de teclado).

---

## Phase 4: User Story 2 - Duplicar lançamento com valor zerado (Priority: P2)

**Goal**: Selecionar um ou mais lançamentos em `/accounts` e duplicá-los para o mês seguinte com valor R$ 0,00, como pendente, pelo botão "Duplicar para o mês seguinte com valor zerado".

**Independent Test**: Em `/accounts`, selecionar um lançamento com valor diferente de zero, clicar em "Duplicar para o mês seguinte com valor zerado" e confirmar que a cópia é criada com `R$ 0,00`, vencimento um mês depois, demais dados idênticos ao original e sem pagamento registrado; confirmar que o valor do original não muda.

### Implementation for User Story 2

- [ ] T013 [US2] Update `<app-bulk-actions-bar>` em `frontend/src/app/account/account-list/account-list.html`: adiciona `(duplicateZeroed)="duplicateSelected(true)"` (a lógica de `duplicateSelected`/`performDuplicate` já é genérica desde a User Story 1 — esta tarefa só conecta o segundo botão) (depends on T010, T012)

**Checkpoint**: User Stories 1 e 2 completas e testáveis de forma independente.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validação final e documentação

- [ ] T014 [P] Run roteiro de validação manual de `quickstart.md` de ponta a ponta (navegador, Playwright já configurado como devDependency), cobrindo os 10 cenários (botões de duplicação, atalho de teclado, virada de mês curto, falha parcial, ambos os tipos de lançamento)
- [ ] T015 [P] Update `README.md` com as decisões técnicas desta feature (novo endpoint `POST /api/accounts/{id}/duplicate`; extensão aditiva de `BulkActionsBar`; atalho de teclado Ctrl+C/Ctrl+V via `HostListener` de `document`), conforme Checkpoints da Rodada de Trabalho da constituição

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem tarefas novas
- **Foundational (Phase 2)**: Bloqueia as duas user stories — endpoint de backend e infraestrutura de frontend compartilhada
- **User Story 1 (Phase 3)**: Depende apenas da Foundational completa
- **User Story 2 (Phase 4)**: Depende da Foundational completa e de T010/T012 (reaproveita a lógica genérica de duplicação já implementada pela US1 e o mesmo binding de `<app-bulk-actions-bar>`) — não é totalmente independente de US1 nesta feature, já que as duas variantes compartilham o mesmo método de componente por design (ver research.md)
- **Polish (Phase 5)**: Depende de ambas as user stories estarem completas

### Parallel Opportunities

- T001, T002, T005, T007, T009 podem começar em paralelo imediatamente após a Foundational ser aberta (arquivos totalmente distintos, sem dependência entre si)
- T003 depende de T001 e T002; T004 depende de T002; T006 depende de T005; T008 depende de T007 — cada um pode rodar em paralelo com os demais desde que sua própria dependência esteja satisfeita
- T014, T015 (Polish) podem rodar em paralelo entre si, depois de ambas as stories completas

---

## Parallel Example: Foundational (Phase 2)

```bash
# Em paralelo, assim que a Phase 2 é aberta:
Task: "Create AccountDuplicateRequest.java em backend/.../account/api/"
Task: "Add AccountService.duplicate em backend/.../account/domain/AccountService.java"
Task: "Add AccountDuplicateRequest em frontend/.../models/account.model.ts"
Task: "Create bulk-duplicate.ts em frontend/.../shared/"
Task: "Extend BulkActionsBar em frontend/.../shared/components/bulk-actions-bar/"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 2: Foundational (endpoint + infraestrutura compartilhada)
2. Complete Phase 3: User Story 1 (duplicar mantendo o valor, botão + atalho de teclado)
3. **STOP and VALIDATE**: testar US1 isoladamente (Acceptance Scenarios 1-4 do spec)

### Incremental Delivery

1. Foundational → endpoint `POST /api/accounts/{id}/duplicate` + infraestrutura de frontend
2. US1 → duplicar mantendo o valor, via botão e via Ctrl+C/Ctrl+V (MVP)
3. US2 → duplicar com valor zerado (conecta o segundo botão à mesma lógica já pronta)
4. Polish → validação de ponta a ponta via `quickstart.md` + atualização do README

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes
- US2 depende de infraestrutura construída durante US1 (`duplicateSelected`/`performDuplicate`), por design desta feature — as duas variantes de duplicação são a mesma operação parametrizada, não duas features independentes
- Commit ao final de cada tarefa ou grupo lógico de tarefas
- Pare em qualquer checkpoint para validar a story isoladamente antes de seguir
