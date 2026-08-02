---

description: "Task list template for feature implementation"

---

# Tasks: Pagamento Parcial de Contas

**Input**: Design documents from `/specs/008-partial-payment-split/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [quickstart.md](./quickstart.md)

**Tests**: A constituição do projeto (Princípio III) exige cobertura de teste automatizado para toda regra de negócio nova. Esta feature altera uma regra de negócio já testada (`AccountService.registerPayment`) e introduz duas regras novas dentro dela — divisão por pagamento parcial com numeração `- parte N` (US2) e ajuste por pagamento a maior com nota em `observations` (US3) — cobertas por casos novos em `AccountServiceTest`. A remoção do campo `recurring` (Foundational) não é uma regra de negócio nova; os testes existentes que hoje passam esse parâmetro são apenas ajustados para compilar contra a assinatura nova. A caixa de valor pago no frontend e a ausência do campo "Recorrente" no formulário não recebem teste unitário dedicado, seguindo o precedente já registrado no `research.md` das features 006/007 (nenhum componente Angular tem hoje teste próprio no projeto) — validadas via o roteiro de `quickstart.md` (Playwright/navegador, Princípio III).

**Organization**: Tarefas agrupadas por user story (spec.md) para permitir implementação e teste independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1, US2, US3)
- Caminhos de arquivo exatos estão incluídos em cada descrição

## Path Conventions (Web app — ver plan.md)

- Backend: `backend/src/main/java/com/financas/account/` (`api/`, `domain/`), `backend/src/main/resources/db/migration/`, `backend/src/test/java/com/financas/account/domain/`
- Frontend: `frontend/src/app/shared/` (models, `bulk-duplicate.spec.ts`), `frontend/src/app/account/account-list/`, `frontend/src/app/account/account-form/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto — não se aplica: backend e frontend já existem (feature 001) e nenhuma dependência nova é necessária (ver plan.md, Technical Context).

Nenhuma tarefa nova nesta fase.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Remove por completo o campo `recurring` (FR-010, decisão tomada durante o planejamento) e prepara `AccountPaymentRequest` para receber `paidAmount` — pré-requisitos compartilhados pelas três user stories do fluxo de pagamento. Fazer a remoção de `recurring` antes evita que a nova lógica de `registerPayment` (US1-US3) precise, em algum momento, tratá-lo como caso especial.

**⚠️ CRITICAL**: Nenhuma implementação de user story deve começar antes desta fase estar completa.

- [X] T001 [P] Add migration `backend/src/main/resources/db/migration/V14__drop_recurring_from_account.sql`: `ALTER TABLE account DROP COLUMN recurring;` (ver research.md, "Remoção completa do campo `recurring`" — `DROP` direto, não uma renomeação)
- [X] T002 [P] Remove o campo `recurring`, o parâmetro correspondente do construtor, o getter `isRecurring()` e o setter `setRecurring(boolean)` de `backend/src/main/java/com/financas/account/domain/Account.java`
- [X] T003 [P] Remove o campo `Boolean recurring` (e sua anotação `@NotNull`) de `backend/src/main/java/com/financas/account/api/AccountRequest.java`
- [X] T004 [P] Remove o campo `Boolean recurring` (e sua anotação `@NotNull`) de `backend/src/main/java/com/financas/account/api/AccountBulkRequest.java`
- [X] T005 [P] Remove o campo `recurring` do record e da fábrica `from(...)` de `backend/src/main/java/com/financas/account/api/AccountResponse.java`
- [X] T006 [P] Add o campo opcional `BigDecimal paidAmount` (`@Positive(message = "O valor pago deve ser maior que zero.")`, sem `@NotNull` — `null` é válido e significa pagamento integral) a `backend/src/main/java/com/financas/account/api/AccountPaymentRequest.java`, junto do `paymentDate` já existente
- [X] T007 Remove o parâmetro `boolean recurring` de `create`, `createForGroup` e `update`, e a leitura de `original.isRecurring()` em `duplicate`, em `backend/src/main/java/com/financas/account/domain/AccountService.java` (depends on T002)
- [X] T008 Remove `request.recurring()` das chamadas a `service.create`/`service.createForGroup`/`service.update` em `backend/src/main/java/com/financas/account/api/AccountController.java` (depends on T003, T004, T007)
- [X] T009 [P] Ajustar `backend/src/test/java/com/financas/account/domain/AccountServiceTest.java` à assinatura sem `recurring`: remover o argumento posicional de recorrência das construções diretas de `new Account(...)` (inclusive em `duplicateCopiesFieldsAndAdvancesDueDateByOneMonth`) e dos helpers privados `receivable`, `receivableWithFund`, `receivableWithPayment`, `payable`, `payableWithPayment`; remover a asserção `copy.isRecurring()`; remover o argumento `recurring` de todas as chamadas a `service.create`/`createForGroup`/`update` nos testes já existentes (depends on T002, T007)
- [X] T010 [P] Remove `recurring: boolean`/`recurring: account.recurring` de `Account`, `AccountRequest` e `AccountBulkRequest` em `frontend/src/app/shared/models/account.model.ts`; add `paidAmount?: number` a `AccountPaymentRequest`
- [X] T011 Remove o `FormControl` de `recurring`, seu mapeamento de leitura/escrita (modo simples e modo em lote), e a checkbox + `<label for="recurring">` "Recorrente" de `frontend/src/app/account/account-form/account-form.ts` e `frontend/src/app/account/account-form/account-form.html` (depends on T010)
- [X] T012 [P] Remove `recurring: account.recurring` da montagem do `AccountRequest` dentro de `confirmAmountEdit` (edição inline de valor) em `frontend/src/app/account/account-list/account-list.ts` (depends on T010)
- [X] T013 [P] Remove `recurring: false` da fixture de conta em `frontend/src/app/shared/bulk-duplicate.spec.ts` (depends on T010)

**Checkpoint**: Campo `recurring` removido de ponta a ponta (banco, backend, frontend); `AccountPaymentRequest` pronto para receber `paidAmount` — as três user stories podem ser implementadas.

---

## Phase 3: User Story 1 - Registrar pagamento integral, sem alterar o valor pago (Priority: P1) 🎯 MVP

**Goal**: A caixa de valor pago aparece pré-preenchida com o valor devido ao lado da data de pagamento; confirmar sem alterá-la mantém o comportamento atual (só a data é registrada).

**Independent Test**: Em `/accounts`, clicar em "Registrar pagamento" de uma conta pendente, confirmar que a caixa de valor pago já vem preenchida com o valor total devido, e confirmar sem alterá-la — a conta deve passar a constar como paga, com o mesmo valor e a mesma descrição de antes, sem nenhuma conta nova.

### Implementation for User Story 1

- [X] T014 [US1] Reescrever `registerPayment` em `backend/src/main/java/com/financas/account/domain/AccountService.java`: nova assinatura `registerPayment(Long id, LocalDate paymentDate, BigDecimal paidAmount)`, anotado `@Transactional` (Princípio II — antecipa os dois `save` que o ramo de split da US2 vai introduzir no mesmo método); resolve `paidAmount == null` para `account.getAmount()`; compara com o valor devido via `compareTo` e, por ora, implementa apenas o ramo "igual" (`account.setPaymentDate(paymentDate); repository.save(account);`), preservando exatamente o comportamento atual (FR-002) — os ramos "menor" (US2) e "maior" (US3) são adicionados por tarefas posteriores no mesmo método (depends on T007)
- [X] T015 [US1] Update `pay` em `backend/src/main/java/com/financas/account/api/AccountController.java` para repassar `request.paidAmount()` a `service.registerPayment` (depends on T008, T014)
- [X] T016 [US1] Update o teste já existente `registerPaymentMarksPayableAccountAsPaid` (linha ~350) em `backend/src/test/java/com/financas/account/domain/AccountServiceTest.java` para chamar a assinatura nova de 3 argumentos (`service.registerPayment(10L, LocalDate.of(2026, 8, 15), null)`), já que a assinatura antiga de 2 argumentos deixa de compilar após T014; add também um caso novo cobrindo `paidAmount` explicitamente igual ao valor devido (não só `null`) — em ambos os casos, verificar que só `paymentDate` é definido, sem alterar `amount`/`description`/`observations` e sem nenhum `save` adicional (depends on T014)
- [X] T017 [US1] Add `paidAmountDraft` (string) em `frontend/src/app/account/account-list/account-list.ts`, inicializado em `startPayment(account)` com `String(account.amount)` (mesmo padrão de `paymentDateDraft`); em `confirmPayment(account)`, quando `!account.paymentDate` (registrando pela primeira vez): abortar silenciosamente se `paidAmountDraft` for vazio, não numérico ou `<= 0` (mesmo padrão do guarda já existente para `paymentDateDraft` vazio — FR-009), senão incluir `paidAmount: Number(this.paidAmountDraft)` no corpo enviado a `registerPayment`; quando `account.paymentDate` já existe ("Alterar pagamento"), não enviar `paidAmount` (FR-006) (depends on T010, T012, T015)
- [X] T018 [US1] Add caixa `<input type="number" step="0.01">` de valor pago ao lado do campo de data em `frontend/src/app/account/account-list/account-list.html`, dentro do bloco `@if (payingId() === account.id)`, com `[(ngModel)]="paidAmountDraft"`, exibida apenas quando `!account.paymentDate` (FR-006) (depends on T017)

**Checkpoint**: User Story 1 completa e testável de forma independente (Acceptance Scenarios 1-2 do spec).

---

## Phase 4: User Story 2 - Registrar pagamento parcial (valor pago menor que o devido) (Priority: P1)

**Goal**: Um valor pago menor que o devido divide a conta em duas, rotuladas com o sufixo `- parte N`, numerado sequencialmente mesmo em splits sucessivos sobre o saldo restante.

**Independent Test**: Registrar um pagamento parcial (ex.: R$70,00 de R$100,00 devidos) e confirmar que a conta original vira "... - parte 1" (paga, R$70,00) e surge uma nova "... - parte 2" (R$30,00, mesma data de vencimento, pendente); repetir sobre a "parte 2" e confirmar que ela mantém o nome e gera uma "parte 3".

### Implementation for User Story 2

- [X] T019 [US2] Add o ramo "menor" a `registerPayment` em `backend/src/main/java/com/financas/account/domain/AccountService.java`: usar um `Pattern` (`^(.*) - parte (\d+)$`) sobre `account.getDescription()` para extrair descrição-base e número de parte atual; se a descrição não tiver o sufixo, renomear a conta para `descrição + " - parte 1"` (número atual = 1); se já tiver, manter a descrição inalterada; em seguida `account.setAmount(paidAmount)`, `account.setPaymentDate(paymentDate)`, `repository.save(account)`; criar e salvar uma nova `Account` com `amount = devido − paidAmount`, mesma `dueDate`, `paymentDate = null`, `description = descrição-base + " - parte " + (número atual + 1)`, mesmo `fund`/`party`/`type`/`observations` da conta paga (FR-003/FR-003a/FR-004) (depends on T014)
- [X] T020 [P] [US2] Add casos de teste em `backend/src/test/java/com/financas/account/domain/AccountServiceTest.java`: split sem sufixo prévio (conta paga renomeada para "... - parte 1", nova conta "... - parte 2" com o saldo, mesma `dueDate`, mesmo `fund`/`party`/`observations`); split com sufixo prévio (ex.: pagar parcialmente uma conta já chamada "... - parte 2" mantém esse nome e cria "... - parte 3" com o número correto, sem duplicar nem reatribuir o sufixo existente) (depends on T019)

**Checkpoint**: User Story 2 completa e testável de forma independente (Acceptance Scenarios 1-3 do spec, incluindo splits sucessivos) — nenhuma alteração de frontend é necessária além da já feita na US1, já que a caixa de valor pago só repassa o número digitado e é o backend quem decide o comportamento.

---

## Phase 5: User Story 3 - Pagamento maior que o valor devido (Priority: P3)

**Goal**: Um valor pago maior que o devido ajusta o valor da própria conta para cima e registra uma nota em `observations`, sem criar conta nova nem sobrescrever observações já existentes.

**Independent Test**: Registrar um pagamento de R$501,00 numa conta de R$500,00 devidos e confirmar que a conta passa a valer R$501,00, é marcada como paga, e `observations` contém "pago R$1,00 a mais" — repetindo numa conta que já tenha observações, confirmar que o texto original é preservado.

### Implementation for User Story 3

- [X] T021 [US3] Add o ramo "maior" a `registerPayment` em `backend/src/main/java/com/financas/account/domain/AccountService.java`: montar a nota `"pago R$" + valor da diferença formatado com vírgula decimal e duas casas + " a mais"` (sem espaço após "R$" — não usar `NumberFormat.getCurrencyInstance`, ver research.md); se `observations` estiver vazio/nulo, a nota vira o conteúdo inteiro; caso já tenha conteúdo, acrescentar a nota numa nova linha, preservando o texto existente; `account.setAmount(paidAmount)`, `account.setPaymentDate(paymentDate)`, `account.setObservations(...)`, `repository.save(account)`; nenhuma conta nova (FR-005) (depends on T014)
- [X] T022 [P] [US3] Add casos de teste em `backend/src/test/java/com/financas/account/domain/AccountServiceTest.java`: overpayment com `observations` nulo/vazio (nota vira o conteúdo inteiro, formato exato "pago R$1,00 a mais"); overpayment com `observations` já preenchido (texto original preservado, nota acrescentada sem sobrescrever) (depends on T021)

**Checkpoint**: As três user stories completas e testáveis de forma independente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validação final e documentação

- [X] T023 [P] Run roteiro de validação manual de `quickstart.md` de ponta a ponta (Playwright já configurado como devDependency), cobrindo os 11 cenários (caixa pré-preenchida, pagamento integral, split, split sucessivo, pagamento a maior com e sem observações prévias, valor zero ignorado, ausência da caixa em "Alterar pagamento", ambos os tipos de conta, ausência do campo "Recorrente" no formulário, e a edição/remoção livre das contas resultantes de um split — FR-007)
- [X] T024 [P] Update `README.md` com as decisões técnicas desta feature (campo opcional `paidAmount` em `POST /api/accounts/{id}/pay`; numeração `- parte N` derivada da própria `description`, sem coluna nova; remoção completa do campo `recurring`, incluindo a nota já existente sobre geração automática de lançamentos recorrentes que ficou desatualizada), conforme Checkpoints da Rodada de Trabalho da constituição

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem tarefas novas
- **Foundational (Phase 2)**: Bloqueia as três user stories — remoção de `recurring` e preparação de `AccountPaymentRequest`
- **User Story 1 (Phase 3)**: Depende apenas da Foundational completa
- **User Story 2 (Phase 4)**: Depende da Foundational completa e de T014 (reaproveita a assinatura/dispatch de `registerPayment` já criada pela US1) — não é totalmente independente de US1 nesta feature, já que as três faixas de valor pago são a mesma operação de negócio, por design (ver research.md)
- **User Story 3 (Phase 5)**: Mesma relação de dependência com T014 que a US2 — ramo adicional do mesmo método, sem depender do ramo da US2
- **Polish (Phase 6)**: Depende das três user stories estarem completas

### Parallel Opportunities

- T001-T006 podem começar em paralelo imediatamente (arquivos totalmente distintos, sem dependência entre si)
- T009, T010, T013 podem rodar em paralelo entre si assim que suas dependências (T002/T007, nenhuma, nenhuma respectivamente) estiverem prontas
- T016, T020, T022 (casos de teste) podem ser escritos em paralelo com a tarefa de implementação equivalente da mesma story, já que tocam o mesmo arquivo de teste mas blocos de código distintos — mantidos como "depends on" a implementação por clareza de execução sequencial dentro da story
- T023, T024 (Polish) podem rodar em paralelo entre si, depois de todas as stories completas

---

## Parallel Example: Foundational (Phase 2)

```bash
# Em paralelo, assim que a Phase 2 é aberta:
Task: "Add migration V14__drop_recurring_from_account.sql"
Task: "Remove recurring de Account.java"
Task: "Remove recurring de AccountRequest.java"
Task: "Remove recurring de AccountBulkRequest.java"
Task: "Remove recurring de AccountResponse.java"
Task: "Add paidAmount a AccountPaymentRequest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 2: Foundational (remoção de `recurring` + `AccountPaymentRequest` pronto)
2. Complete Phase 3: User Story 1 (caixa de valor pago, comportamento integral preservado)
3. **STOP and VALIDATE**: testar US1 isoladamente (Acceptance Scenarios 1-2 do spec)

### Incremental Delivery

1. Foundational → `recurring` removido, `AccountPaymentRequest` com `paidAmount` opcional
2. US1 → caixa de valor pago + `registerPayment` com a assinatura nova, comportamento integral inalterado (MVP)
3. US2 → ramo de split com numeração `- parte N`, a funcionalidade central da feature
4. US3 → ramo de pagamento a maior, cenário raro mas coberto
5. Polish → validação de ponta a ponta via `quickstart.md` + atualização do README

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes
- US2 e US3 dependem da assinatura de `registerPayment` já criada pela US1 (T014), por design desta feature — as três faixas de valor pago são a mesma operação de negócio parametrizada por `paidAmount`, não três features independentes
- Nenhuma tarefa de frontend é necessária nas Phases 4 e 5 (US2/US3) — a caixa de valor pago da US1 já repassa qualquer número digitado, e é inteiramente o backend quem decide entre os três comportamentos
- Commit ao final de cada tarefa ou grupo lógico de tarefas
- Pare em qualquer checkpoint para validar a story isoladamente antes de seguir
