---

description: "Task list template for feature implementation"
---

# Tasks: Lançamentos de Contas a Receber

**Input**: Design documents from `/specs/002-receivable-charges/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [quickstart.md](./quickstart.md)

**Tests**: OBRIGATÓRIO — a constituição do projeto (Princípio III) exige tarefas de teste automatizado para toda regra de negócio de uma feature nova, independentemente de solicitação explícita. Tarefas de teste (JUnit 5 + Mockito, no `ReceivableService`) estão integradas nas fases US1, US2 e US4, que introduzem regras de negócio novas — colocadas após a implementação de cada fase, já que testam o `ReceivableService` recém-criado. US3 não introduz nenhuma regra de negócio nova (é só apresentação/filtro de uma listagem já validada por trás da API em US1) e por isso não tem tarefa de teste dedicada.

**Organization**: Tarefas agrupadas por user story (spec.md) para permitir implementação e teste independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1..US4)
- Caminhos de arquivo exatos estão incluídos em cada descrição

## Path Conventions (Web app — ver plan.md)

- Backend: `backend/src/main/java/com/financas/receivable/...`
- Frontend: `frontend/src/app/receivable/...`
- Ambos os projetos e o banco (`docker-compose.yml`) já existem, criados pela feature 001

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto — não se aplica: `backend/`, `frontend/` e `docker-compose.yml` já foram criados pela feature 001 e são reaproveitados sem alteração.

Nenhuma tarefa nova nesta fase.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura central compartilhada — não se aplica: datasource, CORS, `GlobalExceptionHandler`, exceptions base (`NotFoundException`/`ConflictException`), interceptor de erro do frontend e esqueleto de rotas já foram criados pela feature 001 e são reaproveitados sem alteração.

Nenhuma tarefa nova nesta fase.

---

## Phase 3: User Story 1 - Lançar conta a receber para uma unidade (Priority: P1) 🎯 MVP

**Goal**: Permitir lançar uma conta a receber para uma unidade específica (valor, vencimento, descrição, conta destino e tipo) e vê-la na listagem daquela unidade.

**Independent Test**: Com ao menos uma unidade já cadastrada (feature 001), lançar uma conta a receber preenchendo todos os campos e confirmar que ela aparece na listagem de lançamentos daquela unidade; tentar lançar com valor zero/negativo ou campo obrigatório faltando e confirmar rejeição; tentar lançar sem nenhuma unidade cadastrada e confirmar a orientação para cadastrar uma unidade primeiro.

### Implementation for User Story 1

- [X] T001 [P] [US1] Create migration Flyway `backend/src/main/resources/db/migration/V3__create_receivable_table.sql` (tabela `receivable`: `id`, `amount NUMERIC(10,2) NOT NULL`, `due_date DATE NOT NULL`, `description VARCHAR(255) NOT NULL`, `target_account VARCHAR(20) NOT NULL`, `recurring BOOLEAN NOT NULL`, `unit_id BIGINT NOT NULL REFERENCES unit (id)`, + índice em `unit_id`)
- [X] T002 [P] [US1] Create enum `TargetAccount` (`POOL`, `POOL_GARDEN`, `SIDE_GARDEN`) em `backend/src/main/java/com/financas/receivable/domain/TargetAccount.java`
- [X] T003 [US1] Create entidade JPA `Receivable` (`id`, `amount` `BigDecimal`, `dueDate` `LocalDate`, `description`, `targetAccount` `TargetAccount` via `@Enumerated(EnumType.STRING)`, `recurring` `boolean`, `unit` `ManyToOne` obrigatório) em `backend/src/main/java/com/financas/receivable/domain/Receivable.java` (depends on T002)
- [X] T004 [P] [US1] Create interface de porta `ReceivableRepository` em `backend/src/main/java/com/financas/receivable/domain/ReceivableRepository.java`, com `findAll()`, `findByUnitId(Long)`, `findById(Long)`, `save(Receivable)`, `deleteById(Long)` e `existsByUnitId(Long)` (este último necessário para a extensão de `UnitService` da feature 001 — ver `specs/001-cadastro-condominos/tasks.md`, Phase 11, T057) (depends on T003)
- [X] T005 [US1] Create `ReceivableJpaRepository` (Spring Data) e `ReceivableRepositoryImpl` em `backend/src/main/java/com/financas/receivable/infra/` (depends on T004)
- [X] T006 [US1] Implement `ReceivableService` (criar com valor positivo obrigatório — FR-003 —, todos os campos obrigatórios — FR-001 —, checagem de unidade existente — FR-002 —, listar todos e listar por unidade) em `backend/src/main/java/com/financas/receivable/domain/ReceivableService.java` (depends on T005)
- [X] T007 [P] [US1] Create DTOs `ReceivableRequest` (`amount` `@NotNull @Positive`, `dueDate` `@NotNull` com `@JsonFormat(pattern = "dd/MM/yyyy")`, `description` `@NotBlank`, `targetAccount` `@NotNull`, `recurring` `Boolean` `@NotNull`, `unitId` `@NotNull`) e `ReceivableResponse` (com `UnitResponse` aninhado e `dueDate` no mesmo formato `dd/MM/yyyy`, factory estático `from(Receivable)`) em `backend/src/main/java/com/financas/receivable/api/`
- [X] T008 [US1] Implement `ReceivableController` (`POST /api/receivables`, `GET /api/receivables` com filtro opcional `?unitId=`) em `backend/src/main/java/com/financas/receivable/api/ReceivableController.java` (depends on T006, T007)
- [X] T009 [P] [US1] Mapear `unitId` referenciado inexistente para 404 orientando a cadastrar a unidade primeiro (FR-011) — reaproveita o handler genérico de `NotFoundException` no `GlobalExceptionHandler`, sem código extra (mesmo padrão de `POST /api/residents` na feature 001) (depends on T006)
- [X] T010 [P] [US1] Add `@ExceptionHandler(HttpMessageNotReadableException.class)` ao `GlobalExceptionHandler` (retorna 400 no formato padrão `{ "message", "status" }` com mensagem genérica em português, ex.: "Dados inválidos.") em `backend/src/main/java/com/financas/shared/GlobalExceptionHandler.java` — cobre JSON malformado, `targetAccount` fora do conjunto fixo do enum (FR-013) e `dueDate` fora do formato `dd/MM/yyyy`, casos em que a deserialização do Jackson falha antes da validação Bean Validation rodar e que hoje cairiam no formato de erro padrão do Spring, violando o Princípio VI da constituição (nenhuma dependência de outra tarefa desta feature — corrige também a mesma exposição já latente em `ResidentRequest.unitId` da feature 001)
- [X] T011 [P] [US1] Create models `Receivable`, `ReceivableRequest` e union type `TargetAccount` em `frontend/src/app/shared/models/receivable.model.ts`
- [X] T012 [P] [US1] Create `ReceivableService` (HttpClient, com `findAll`, `findByUnitId`, `findById`, `create`, `update`, `delete`) em `frontend/src/app/shared/services/receivable.service.ts` (depends on T008, T011)
- [X] T013 [US1] Create componente `receivable-form` (lançamento individual: seletor de unidade via `UnitService` da feature 001, campos valor/vencimento/descrição/conta destino/tipo, validação de obrigatórios e valor positivo, orientação quando não há unidades cadastradas) em `frontend/src/app/receivable/receivable-form/` (depends on T012)
- [X] T014 [US1] Create componente `receivable-list` (tabela com valor, vencimento, descrição, conta destino e tipo) em `frontend/src/app/receivable/receivable-list/` (depends on T012)
- [X] T015 [US1] Wire rotas de lançamento em `frontend/src/app/app.routes.ts` (depends on T013, T014)

### Tests for User Story 1 ⚠️ (obrigatório pela constituição — Princípio III)

- [X] T016 [P] [US1] Create testes unitários de `ReceivableService` (Mockito: criação com valor zero/negativo rejeitada — FR-003 —, criação com `unitId` inexistente lança `NotFoundException` — FR-002/FR-011 —, criação válida persiste com todos os campos) em `backend/src/test/java/com/financas/receivable/domain/ReceivableServiceTest.java` (depends on T006)

**Checkpoint**: User Story 1 completa e testável de forma independente (Acceptance Scenarios 1-4 do spec).

---

## Phase 4: User Story 2 - Lançar a mesma conta a receber para todas as unidades simultaneamente (Priority: P1)

**Goal**: Permitir lançar, em uma única ação, a mesma conta a receber (valor, vencimento, descrição, conta destino e tipo) para todas as unidades cadastradas no momento da ação.

**Independent Test**: Com pelo menos duas unidades cadastradas, disparar o lançamento em lote e confirmar que cada unidade passa a ter um lançamento independente com os mesmos dados; confirmar que uma unidade cadastrada depois do lote não recebe o lançamento retroativamente; confirmar orientação quando não há nenhuma unidade cadastrada.

### Implementation for User Story 2

- [X] T017 [US2] Add `createForAllUnits(...)` ao `ReceivableService`, injetando `UnitRepository` (já existente da feature 001): busca todas as `Unit` cadastradas no momento da chamada e cria um `Receivable` independente por unidade, com os mesmos valor/vencimento/descrição/conta destino/tipo (FR-004/FR-005) em `backend/src/main/java/com/financas/receivable/domain/ReceivableService.java` (depends on T006)
- [X] T018 [P] [US2] Create `NoUnitsRegisteredException` (regra de negócio da própria operação de lote — não em `shared/`) em `backend/src/main/java/com/financas/receivable/domain/`, lançada quando não há nenhuma unidade cadastrada (FR-011), e mapear para 409 com mensagem em português no `GlobalExceptionHandler` (reaproveita o handler genérico de `ConflictException`, sem código extra) (depends on T006)
- [X] T019 [P] [US2] Create DTO `ReceivableBulkRequest` (igual a `ReceivableRequest`, sem `unitId`) em `backend/src/main/java/com/financas/receivable/api/`
- [X] T020 [US2] Add `POST /api/receivables/bulk` ao `ReceivableController`, retornando a lista de `ReceivableResponse` criados (201) em `backend/src/main/java/com/financas/receivable/api/ReceivableController.java` (depends on T017, T018, T019)
- [X] T021 [P] [US2] Add método `createBulk` ao `ReceivableService` (frontend) em `frontend/src/app/shared/services/receivable.service.ts` (depends on T012, T020)
- [X] T022 [US2] Add ação "lançar para todas as unidades" ao `receivable-form` (alterna entre selecionar uma unidade específica ou aplicar a todas, ocultando o seletor de unidade nesse modo; exibe a mesma orientação de "cadastre uma unidade primeiro" do modo individual quando não há nenhuma unidade cadastrada — FR-011) em `frontend/src/app/receivable/receivable-form/` (depends on T013, T021)

### Tests for User Story 2 ⚠️ (obrigatório pela constituição — Princípio III)

- [X] T023 [P] [US2] Create testes unitários de lançamento em lote no `ReceivableServiceTest` (Mockito: cria exatamente um `Receivable` por unidade retornada pelo `UnitRepository` no momento da chamada — FR-004/FR-005 —, lança `NoUnitsRegisteredException` quando `UnitRepository.findAll()` retorna lista vazia — FR-011) em `backend/src/test/java/com/financas/receivable/domain/ReceivableServiceTest.java` (depends on T017, T018, T016)

**Checkpoint**: User Stories 1 e 2 funcionam de forma independente (Acceptance Scenarios do US2).

---

## Phase 5: User Story 3 - Listar lançamentos de uma unidade (Priority: P2)

**Goal**: Exibir a listagem de lançamentos de contas a receber de uma unidade, com indicação clara quando vazia.

**Independent Test**: Lançar algumas contas a receber para uma unidade e verificar que todas aparecem na listagem dessa unidade, com valor, vencimento, descrição, conta destino e tipo visíveis; com uma unidade sem lançamentos, verificar a indicação de "nenhum lançamento cadastrado".

### Implementation for User Story 3

- [X] T024 [P] [US3] Enhance `receivable-list` para exibir mensagem de "nenhum lançamento cadastrado" quando a lista vier vazia (FR-007), em `frontend/src/app/receivable/receivable-list/` (depends on T014)
- [X] T025 [US3] Add filtro/seletor de unidade na tela de lançamentos, consumindo `GET /api/receivables?unitId=` (FR-006), em `frontend/src/app/receivable/receivable-list/` (depends on T014, T012)

**Checkpoint**: Listagem por unidade conforme FR-006/FR-007 — US1, US2 e US3 funcionam de forma independente.

---

## Phase 6: User Story 4 - Editar e remover um lançamento (Priority: P3)

**Goal**: Permitir editar valor, vencimento, descrição, conta destino, tipo e unidade associada de um lançamento existente, e removê-lo mediante confirmação explícita.

**Independent Test**: Editar um lançamento existente (incluindo trocar sua unidade associada) e confirmar que a listagem reflete os novos valores, incluindo rejeição de valor inválido e de unidade inexistente; remover um lançamento e confirmar que ele some da listagem da unidade correspondente; tentar editar/remover um lançamento inexistente e confirmar mensagem de não encontrado.

### Implementation for User Story 4

- [X] T026 [US4] Add `GET /api/receivables/{id}` (consulta usada pelo formulário de edição) e `PUT /api/receivables/{id}` ao `ReceivableController`, com método de atualização (mesma validação de valor positivo e campos obrigatórios da criação — incluindo troca de unidade associada, com a mesma checagem de existência da criação, FR-002/FR-008 —, 404 se o lançamento ou a nova unidade não existirem) ao `ReceivableService`, em `backend/src/main/java/com/financas/receivable/` (depends on T006, T008)
- [X] T027 [US4] Add `DELETE /api/receivables/{id}` ao `ReceivableController` e método de remoção (404 se não existir) ao `ReceivableService`, em `backend/src/main/java/com/financas/receivable/` (depends on T006, T008)
- [X] T028 [P] [US4] Add modo de edição ao `receivable-form` (pré-preenchimento + chamada PUT, permitindo trocar a unidade no mesmo seletor usado na criação) em `frontend/src/app/receivable/receivable-form/` (depends on T013, T026)
- [X] T029 [P] [US4] Add ação de remoção com diálogo de confirmação em `receivable-list` em `frontend/src/app/receivable/receivable-list/` (depends on T014, T027)
- [X] T030 [US4] Add ação "editar" nas linhas de `receivable-list` navegando ao formulário em modo edição (depends on T024, T028)

### Tests for User Story 4 ⚠️ (obrigatório pela constituição — Princípio III)

- [X] T031 [P] [US4] Create testes unitários de edição e remoção no `ReceivableServiceTest` (Mockito: edição com valor zero/negativo rejeitada — FR-008 —, edição trocando para um `unitId` existente persiste a nova unidade — FR-008 —, edição de `unitId` inexistente lança `NotFoundException`, edição/remoção de lançamento inexistente lança `NotFoundException` — FR-010) em `backend/src/test/java/com/financas/receivable/domain/ReceivableServiceTest.java` (depends on T026, T027, T016)

**Checkpoint**: Todas as 4 user stories funcionam de forma independente.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validações finais e documentação

- [X] T032 [P] Run roteiro de validação manual de `quickstart.md` de ponta a ponta (API real + navegador), incluindo o cenário 9 (bloqueio de remoção de unidade com lançamento vinculado), que depende de `specs/001-cadastro-condominos/tasks.md` Phase 11 (T056-T058) já ter sido implementada
- [X] T033 [P] Update `README.md` com decisões técnicas tomadas nesta feature (serialização de data `dd/MM/yyyy`, modelagem de `targetAccount`/`recurring`, endpoint dedicado de lote, tratamento padronizado de JSON malformado via `HttpMessageNotReadableException`), conforme Fluxo de Commits da constituição
- [X] T034 [P] Review mensagens de erro do `GlobalExceptionHandler` para os novos casos desta feature (400/404/409 de `receivables`, incluindo o novo handler de JSON malformado), garantindo consistência em português (Convenções de API REST)

---

## Phase 8: Correções e Extensões Pós-Implementação (rodada 2026-07-26)

**Motivo**: rodada de correções pedida pela usuária após a implementação inicial (US1-US4) — ver Clarifications ("Sessão de correção 2026-07-26") e User Story 5 em spec.md.

### Registro de pagamento, na criação ou depois (User Story 5 — Priority: P2)

**Tests**: obrigatório pela constituição (Princípio III) — T045/T046 abaixo.

**Nota**: revisão feita em duas partes. A parte 2 removeu o campo `paid` (redundante — "pago" é só `paymentDate != null`, ver Clarifications/research.md) e passou a permitir informar `paymentDate` já na criação/edição, não só via ação dedicada.

- [X] T035 [P] [US5] Create migration Flyway `backend/src/main/resources/db/migration/V4__add_payment_date_to_receivable.sql` (`ALTER TABLE receivable ADD COLUMN payment_date DATE NULL`)
- [X] T036 [US5] Add campo `paymentDate` (getter/setter) à entidade `Receivable` (sem campo `paid`) em `backend/src/main/java/com/financas/receivable/domain/Receivable.java` (depends on T035)
- [X] T037 [US5] Update `create(...)` e `createForAllUnits(...)` do `ReceivableService` para aceitar um `LocalDate paymentDate` opcional (FR-015: lançamento já criado como pago quando informado), em `backend/src/main/java/com/financas/receivable/domain/ReceivableService.java` (depends on T036)
- [X] T038 [US5] Add método `registerPayment(Long id, LocalDate paymentDate)` ao `ReceivableService` (FR-016: grava/atualiza `paymentDate`; reaplicar sobre um lançamento já pago só atualiza a data, sem estorno) em `backend/src/main/java/com/financas/receivable/domain/ReceivableService.java` (depends on T036)
- [X] T039 [P] [US5] Create DTO `ReceivablePaymentRequest` (`paymentDate` `@NotNull`) em `backend/src/main/java/com/financas/receivable/api/`
- [X] T040 [P] [US5] Add campo opcional `paymentDate` (sem `@NotNull`) a `ReceivableRequest` e `ReceivableBulkRequest`, em `backend/src/main/java/com/financas/receivable/api/` (depends on T037)
- [X] T041 [US5] Add `POST /api/receivables/{id}/pay` ao `ReceivableController`, retornando o `ReceivableResponse` atualizado (depends on T038, T039)
- [X] T042 [P] [US5] Update `ReceivableResponse`/`ReceivableResponse.from(...)` para incluir `paymentDate` (sem campo `paid`) em `backend/src/main/java/com/financas/receivable/api/ReceivableResponse.java` (depends on T036)
- [X] T045 [P] [US5] Create testes unitários no `ReceivableServiceTest` (Mockito: criação individual/em lote com `paymentDate` informado já persiste como pago — FR-015; registra pagamento em lançamento pendente; atualiza a data ao reaplicar em lançamento já pago — FR-016; 404 se o lançamento não existir; edição/remoção de lançamento pago continuam permitidas sem bloqueio — FR-017) em `backend/src/test/java/com/financas/receivable/domain/ReceivableServiceTest.java` (depends on T037, T038)
- [X] T047 [P] [US5] Add `registerPayment(id, paymentDate)` ao `ReceivableService` (frontend, HttpClient) em `frontend/src/app/shared/services/receivable.service.ts` (depends on T041)
- [X] T049 [US5] Add campo opcional de data de pagamento (nativo `<input type="date">`) ao `receivable-form`, tanto na criação (individual e em lote) quanto na edição — quando preenchido, o lançamento é enviado/atualizado já como pago (FR-015) — em `frontend/src/app/receivable/receivable-form/` (depends on T040)
- [X] T050 [US5] Add ação "Registrar pagamento" em `receivable-list` (campo de data nativo — padrão a data atual, editável — e confirmação), exibindo coluna de status (Pago/Pendente, derivado de `paymentDate`) e a data de pagamento quando pago, em `frontend/src/app/receivable/receivable-list/` (depends on T047)

### Filtros de listagem (FR-020 a FR-023)

- [X] T043 [US3] Add filtros `paid`/`overdue`/`dueYearMonth`/`paymentYearMonth` ao `ReceivableService.findAll(...)`, aplicados em memória sobre o resultado de `findAll()`/`findByUnitId(...)` (E lógico entre si e com `unitId`; `overdue` usa `LocalDate.now()` diretamente, sem abstração de `Clock` — ver research.md), em `backend/src/main/java/com/financas/receivable/domain/ReceivableService.java` (depends on T036)
- [X] T044 [US3] Add os novos `@RequestParam` (`paid`, `overdue`, `dueYearMonth`, `paymentYearMonth`) ao `GET /api/receivables` em `backend/src/main/java/com/financas/receivable/api/ReceivableController.java` (depends on T043)
- [X] T046 [P] [US3] Create testes unitários dos filtros no `ReceivableServiceTest` (Mockito: cada filtro isoladamente e combinados entre si — incluindo o caso `overdue` excluir lançamentos pagos mesmo com `dueDate` passado, FR-021) em `backend/src/test/java/com/financas/receivable/domain/ReceivableServiceTest.java` (depends on T043)
- [X] T048 [US3] Extend `ReceivableService` (frontend) `findAll(...)` para aceitar os novos filtros como parâmetros opcionais, em `frontend/src/app/shared/services/receivable.service.ts` (depends on T044)
- [X] T051 [US3] Add controles de filtro em `receivable-list` (select de status pago/pendente, checkbox "somente vencidos", seletor de mês/ano de vencimento e de mês/ano de pagamento), consumindo T048, em `frontend/src/app/receivable/receivable-list/` (depends on T048)

### Correção do formato de data (Princípio IV: ISO internamente, DD/MM/AAAA só na UI — sem utilitário customizado)

**Nota**: revisão feita em duas partes. A parte 2 descartou o utilitário de conversão (`date-format.util.ts`) previsto na parte 1, em favor de `<input type="date">` nativo (escrita) e do `DatePipe` do Angular (exibição) — ver research.md.

- [X] T052 [US-datafix] Remove `@JsonFormat(pattern = "dd/MM/yyyy")` de `ReceivableRequest.dueDate` e `ReceivableResponse.dueDate` (e não adicionar em `ReceivablePaymentRequest.paymentDate`/`ReceivableResponse.paymentDate`/ `ReceivableRequest.paymentDate`), passando a serializar no formato ISO-8601 padrão do Jackson, em `backend/src/main/java/com/financas/receivable/api/ReceivableRequest.java` e `ReceivableResponse.java` — **REABRE T007** desta rodada (motivo: correção do Princípio IV da constituição, ver research.md)
- [X] T053 Update `receivable-form`: trocar o `<input type="text">` de `dueDate` (com regex `DUE_DATE_PATTERN`) por `<input type="date">` nativo, ligado diretamente ao valor ISO do `FormControl` (sem parsing manual); mesmo tratamento para o novo campo de data de pagamento (T049), em `frontend/src/app/receivable/receivable-form/` — **REABRE parte de T013** desta rodada
- [X] T054 [P] Update `receivable-list` para exibir `dueDate`/`paymentDate` com o `DatePipe` nativo do Angular (`| date:'dd/MM/yyyy'`), sem nenhuma conversão manual, em `frontend/src/app/receivable/receivable-list/` — **REABRE parte de T014** desta rodada

### Tipo como caixa de seleção "Recorrente"

- [X] T055 Update `receivable-form` (template + `FormControl` `recurring`): trocar o `<select>` por um único checkbox "Recorrente", com valor padrão `false` (desmarcado) e sem estado de "obrigatório vazio", em `frontend/src/app/receivable/receivable-form/` — **REABRE parte de T013** desta rodada (mesmo arquivo de T053; pode ser feito na mesma tarefa de implementação)

### Seleção múltipla e remoção em lote (FR-019)

- [X] T063 [P] Create `frontend/src/app/shared/list-selection.ts` (helper `createSelection<T>`: signal com o conjunto de ids selecionados, `toggle(id)`, `toggleAll(items, getId)`, `isSelected(id)`, `clear()`, `selectedCount`)
- [X] T064 [P] Create `frontend/src/app/shared/bulk-delete.ts` (`bulkDelete(ids, deleteFn): Observable<{ succeeded: number[]; failed: { id: number; message: string }[] }>` — remove item a item, agregando sucesso/falha, sem endpoint transacional novo — ver research.md)
- [X] T065 [P] Create componente compartilhado `frontend/src/app/shared/components/bulk-actions-bar/` (barra "N selecionados" + botão "Remover selecionados", com `confirm()` antes de disparar, exibindo ao final quais itens falharam e por quê) (depends on T063, T064)
- [X] T066 Apply seleção múltipla (coluna de checkbox por linha + "selecionar todos") e o `bulk-actions-bar` a `receivable-list`, em `frontend/src/app/receivable/receivable-list/` (depends on T063, T064, T065)
- [X] T067 [P] Create teste Vitest de `bulkDelete` (agrega sucesso/falha por item corretamente, inclusive quando todos ou nenhum item falha) em `frontend/src/app/shared/bulk-delete.spec.ts` (depends on T064)

**Impacto cruzado com a feature 001**: aplicar o mesmo padrão de seleção múltipla + remoção em lote a `unit-list` e `resident-list` está registrado em `specs/001-cadastro-condominos/tasks.md`, Phase 12 — depende de T063, T064, T065 desta feature já estarem implementados (numeração escolhida para não colidir com a Phase 11 desta mesma tasks.md, que referencia T056-T058 da feature 001).

### README

- [X] T068 [P] Update `README.md`: remover o item "Registro de pagamento/quitação de um lançamento" de "O que eu faria diferente ou melhoraria com mais tempo" (implementado nesta rodada); registrar em "Decisões técnicas e premissas" a correção do formato de data (ISO internamente / DD-MM-AAAA só na UI, via recursos nativos do Angular — sem utilitário dedicado), o endpoint de ação `POST /api/receivables/{id}/pay`, o suporte a criar já pago, os filtros de listagem, e o par `list-selection`/`bulk-delete` + `bulk-actions-bar` compartilhados; registrar em "Revisões e correções das entregas da IA" tanto o pedido de reversão do formato de data quanto a remoção do utilitário de conversão como aprendizados, conforme Fluxo de Commits da constituição
- [X] T069 [P] Run roteiro de validação manual atualizado de `quickstart.md` (cenários 10-13), cobrindo registro de pagamento (na criação e depois), formato ISO na API (curl), os quatro filtros de listagem e remoção em lote

**Checkpoint**: User Story 5 completa e testável de forma independente (incluindo criar já pago); filtros de listagem funcionando e combináveis; formato de data corrigido sem utilitário customizado; tipo representado como checkbox; remoção em lote disponível em `receivable-list`.

---

## Phase 9: Convergence

**Motivo**: gaps encontrados por `/speckit-converge` ao comparar spec.md/plan.md/tasks.md com o código já implementado (backend + frontend rodando, validado via curl e Playwright).

- [X] T070 Add `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` ao `GlobalExceptionHandler` (retorna 400 no formato padrão `{ "message", "status" }` com mensagem genérica em português) per Constitution Princípio VI (CRITICAL — hoje `?paid=maybe`, `?overdue=maybe` e `?unitId=abc` retornam o formato de erro padrão do Spring, não o formato exigido pela constituição) em `backend/src/main/java/com/financas/shared/GlobalExceptionHandler.java` (missing)
- [X] T071 Validate o cenário de remoção em lote "melhor esforço" em `unit-list` (selecionar uma unidade com condômino vinculado junto de outra sem vínculo, confirmar que só a segunda é removida e a primeira é reportada como falha) e atualizar `specs/001-cadastro-condominos/quickstart.md` com esse roteiro per `specs/001-cadastro-condominos/tasks.md` T062 (partial)
- [X] T072 Add cenários 12 (filtro por status pago/pendente e por vencidos) e 13 (filtro por mês/ano de vencimento e de pagamento) ao roteiro de validação manual de `specs/002-receivable-charges/quickstart.md` per T069 (partial)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** e **Foundational (Phase 2)**: Sem tarefas novas — já entregues pela feature 001
- **User Story 1 (Phase 3)**: Depende apenas da infraestrutura já existente (feature 001)
- **User Story 2 (Phase 4)**: Depende de US1 (T006, T008, T012, T013) e do `UnitRepository` já existente (feature 001)
- **User Story 3 (Phase 5)**: Depende das listagens/filtro básicos criados em US1 (T012, T014)
- **User Story 4 (Phase 6)**: Depende de US1 (T006, T008, T013) e da listagem de US3 (T024)
- **Polish (Phase 7)**: Depende de todas as user stories desejadas estarem completas
- **Correções pós-implementação (Phase 8)**: Depende de US1/US3/US4 (T006, T008, T012, T013, T014) já implementadas; T044/T046/T047 reabrem partes de T007/T013/T014

### Impacto cruzado com a feature 001

A tarefa de bloquear a remoção de unidade quando houver lançamentos vinculados (FR-012) está registrada em `specs/001-cadastro-condominos/tasks.md`, Phase 11 (T056-T058), por alterar `UnitService` — já implementado pela feature 001. Essas tarefas dependem de T004/T005 desta feature (`ReceivableRepository` precisa existir) e devem ser executadas depois delas.

A tarefa de aplicar seleção múltipla + remoção em lote a `unit-list`/`resident-list` está registrada em `specs/001-cadastro-condominos/tasks.md`, Phase 12, por alterar telas já implementadas pela feature 001 reaproveitando componentes compartilhados criados em T056-T058 desta feature (Phase 8).

### Impacto cruzado — destaque de linha selecionada generalizado (FR-024, sessão 2026-08-02)

A tarefa de mover a regra CSS de transição de cor de `account-list.scss` para `frontend/src/styles.scss` (global) está registrada em `specs/007-duplicate-account-next-month/tasks.md`, Phase 7 — `account-list` já aplica o destaque `table-active`, criado por aquela feature, sem alteração de comportamento. As tarefas de aplicar `[class.table-active]="selection.isSelected(item)"` a `fund-list` estão registradas em `specs/004-fund-entity-balance/tasks.md`, Phase 6, e a `party-list`/`group-list` em `specs/005-counterparty-groups/tasks.md`, Phase 8 — por alterarem telas já implementadas por essas features reaproveitando o trio de seleção desta feature (FR-024).

### Notas de dependência entre stories

Assim como na feature 001, as user stories aqui não são 100% independentes entre si: US2 reaproveita o `ReceivableService`/`ReceivableService` (frontend) de US1 para o lançamento em lote; US3 reaproveita a listagem básica de US1; US4 reaproveita o formulário e a listagem de US1/US3 para adicionar edição/remoção. Isso reflete a ordem P1 → P1 → P2 → P3 já definida no spec.

### Parallel Opportunities

- T001, T002, T004, T007, T010 (US1) podem rodar em paralelo entre si antes das tarefas que dependem deles
- T018, T019 (US2) podem rodar em paralelo entre si
- T021 (US2), T024 (US3), T028, T029 (US4) podem rodar em paralelo com outras tarefas [P] de arquivos diferentes dentro da mesma fase
- T016, T023, T031 (testes) podem rodar em paralelo entre si caso implementadas em arquivos de teste separados, ou sequencialmente se acumuladas no mesmo `ReceivableServiceTest.java`

---

## Parallel Example: User Story 1

```bash
# Migration, enum, repository port e DTOs em paralelo:
Task: "Create migration Flyway V3__create_receivable_table.sql"
Task: "Create enum TargetAccount em backend/src/main/java/com/financas/receivable/domain/TargetAccount.java"
Task: "Create DTOs ReceivableRequest/ReceivableResponse em backend/src/main/java/com/financas/receivable/api/"

# Frontend model e service em paralelo (após o controller existir):
Task: "Create models Receivable/ReceivableRequest em frontend/src/app/shared/models/receivable.model.ts"
Task: "Create ReceivableService (HttpClient) em frontend/src/app/shared/services/receivable.service.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 3: User Story 1 (Phases 1 e 2 não têm tarefas novas)
2. **STOP and VALIDATE**: testar US1 isoladamente (cenários 1-4 do spec)

### Incremental Delivery

1. US1 → lançamento individual funcionando (MVP)
2. US2 → lançamento em lote reaproveitando US1
3. US3 → listagem completa com filtro por unidade e estado vazio
4. US4 → edição (incluindo troca de unidade) e remoção
5. (Cross-feature) Extensão de `UnitService` na feature 001 (Phase 11 de `specs/001-cadastro-condominos/tasks.md`) — bloqueia remoção de unidade com lançamento vinculado

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes
- Cada user story é independentemente completável e testável, exceto pelas dependências de dados descritas acima
- Commit ao final de cada tarefa ou grupo lógico de tarefas
- Pare em qualquer checkpoint para validar a story isoladamente antes de seguir
