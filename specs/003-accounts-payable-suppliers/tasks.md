---

description: "Task list template for feature implementation"
---

# Tasks: Contas a Pagar, Fornecedores e Unificação de Contas

**Input**: Design documents from `/specs/003-accounts-payable-suppliers/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [quickstart.md](./quickstart.md)

**Tests**: OBRIGATÓRIO — a constituição do projeto (Princípio III) exige tarefas de teste
automatizado para toda regra de negócio de uma feature nova, independentemente de solicitação
explícita. Diferente das features 001/002 (onde o backend crescia incrementalmente por user
story), esta feature é fundamentalmente uma **migração** — `Receivable` é renomeada/generalizada
em `Account`, entidade que toda user story depende igualmente. Por isso, o backend completo
(entidades, repositórios, serviços com todas as regras de negócio, controllers e testes
Mockito) é entregue de uma vez na Fase 2 (Foundational); as fases de user story a partir daí
são majoritariamente **frontend** (novas telas ou telas portadas/estendidas de
`receivable-list`/`receivable-form`), com testes de regressão específicos de cada story onde
fizer sentido (ex.: US4 reforça cobertura de pagamento para o tipo `PAYABLE`).

**Organization**: Tarefas agrupadas por user story (spec.md) para permitir implementação e
teste independentes de cada uma, na medida em que a natureza de migração desta feature permite.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1..US5)
- Caminhos de arquivo exatos estão incluídos em cada descrição

## Path Conventions (Web app — ver plan.md)

- Backend: `backend/src/main/java/com/financas/account/...` e `.../supplier/...`
- Frontend: `frontend/src/app/account/...` e `.../supplier/...`
- Ambos os projetos e o banco (`docker-compose.yml`) já existem, criados pela feature 001

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto — não se aplica: `backend/`, `frontend/` e
`docker-compose.yml` já existem (feature 001) e são reaproveitados sem alteração.

Nenhuma tarefa nova nesta fase.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Migração completa de `Receivable` (feature 002) para `Account`, e criação da
entidade `Supplier` — infraestrutura que TODAS as user stories desta feature dependem
igualmente. Inclui migrations, entidades, repositórios, serviços (com todas as regras de
negócio novas: consistência tipo/contraparte, valor não negativo, imutabilidade de `type`),
controllers, DTOs e os testes automatizados dessas regras.

**Nota**: a migration `V7__drop_resident_table.sql` **não** é criada por esta feature — é
tarefa de `specs/001-cadastro-condominos/tasks.md`, Phase 13 (T064), que depende de T005/T008
abaixo já existirem. As migrations desta feature são só V5 (T001) e V6 (T002).

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase estar completa

- [ ] T001 [P] Create migration Flyway
  `backend/src/main/resources/db/migration/V5__create_supplier_table.sql` (tabela `supplier`:
  `id`, `name VARCHAR(255) NOT NULL`, `unit_id BIGINT NULL REFERENCES unit (id)`, `pix_key
  VARCHAR(255) NULL`, + índice em `unit_id`)
- [ ] T002 Create migration Flyway
  `backend/src/main/resources/db/migration/V6__transform_receivable_to_account.sql`
  (`ALTER TABLE receivable RENAME TO account`; `ALTER INDEX receivable_unit_id_idx RENAME TO
  account_unit_id_idx`; `ALTER TABLE account RENAME COLUMN target_account TO fund`; `ALTER
  TABLE account ALTER COLUMN unit_id DROP NOT NULL`; adiciona `type VARCHAR(20) NOT NULL
  DEFAULT 'RECEIVABLE'` e remove o `DEFAULT` em seguida; adiciona `supplier_id BIGINT NULL
  REFERENCES supplier (id)`; adiciona `observations TEXT NULL`; `CHECK CONSTRAINT
  account_type_counterparty_check` — ver data-model.md) (depends on T001)
- [ ] T003 [P] Create enum `AccountType` (`RECEIVABLE`, `PAYABLE`) em
  `backend/src/main/java/com/financas/account/domain/AccountType.java`
- [ ] T004 [P] Create enum `Fund` (`POOL`, `POOL_GARDEN`, `SIDE_GARDEN` — renomeado de
  `TargetAccount`) em `backend/src/main/java/com/financas/account/domain/Fund.java`
- [ ] T005 [P] Create entidade JPA `Supplier` (`id`, `name` obrigatório, `unit` `ManyToOne`
  opcional, `pixKey` opcional) em
  `backend/src/main/java/com/financas/supplier/domain/Supplier.java`
- [ ] T006 [P] Create interface de porta `SupplierRepository` (`findAll`, `findById`, `save`,
  `deleteById`, `existsById`, `existsByUnitId`) em
  `backend/src/main/java/com/financas/supplier/domain/SupplierRepository.java` (depends on T005)
- [ ] T007 Create `SupplierJpaRepository` (Spring Data) e `SupplierRepositoryImpl` em
  `backend/src/main/java/com/financas/supplier/infra/` (depends on T006)
- [ ] T008 Create entidade JPA `Account` (`id`, `type` `AccountType` via
  `@Enumerated(EnumType.STRING)`, `amount` `BigDecimal`, `dueDate` `LocalDate`, `description`,
  `fund` `Fund` via `@Enumerated(EnumType.STRING)`, `recurring` `boolean`, `unit` `ManyToOne`
  **opcional**, `supplier` `ManyToOne` opcional, `paymentDate` `LocalDate` opcional,
  `observations` `String` opcional) em
  `backend/src/main/java/com/financas/account/domain/Account.java` (depends on T003, T004, T005)
- [ ] T009 [P] Create interface de porta `AccountRepository` (`findAll`, `findByUnitId`,
  `findBySupplierId`, `findById`, `save`, `deleteById`, `existsByUnitId`,
  `existsBySupplierId`) em `backend/src/main/java/com/financas/account/domain/AccountRepository.java`
  (depends on T008)
- [ ] T010 Create `AccountJpaRepository` (Spring Data) e `AccountRepositoryImpl` em
  `backend/src/main/java/com/financas/account/infra/` (depends on T009)
- [ ] T011 [P] Create `InvalidAccountAmountException` (renomeada de
  `InvalidReceivableAmountException`; lançada apenas para valor **negativo** — zero é válido,
  FR-008) em `backend/src/main/java/com/financas/account/domain/`
- [ ] T012 [P] Create `AccountTypeChangeNotAllowedException` (nova; lançada quando `PUT`
  tenta alterar o `type` de uma conta já criada — FR-006/FR-015) em
  `backend/src/main/java/com/financas/account/domain/`
- [ ] T013 [P] Create `NoUnitsRegisteredException` (portada sem alteração de comportamento de
  `com.financas.receivable.domain`) em `backend/src/main/java/com/financas/account/domain/`
- [ ] T014 Implement `AccountService` — `create(type, amount, dueDate, description, fund,
  recurring, unitId, supplierId, paymentDate, observations)` validando valor não negativo
  (FR-008) e contraparte obrigatória e compatível com `type` (FR-007: `RECEIVABLE` exige
  `unitId` e rejeita `supplierId`; `PAYABLE` exige `supplierId` e rejeita `unitId`);
  `createForAllUnits(...)` portado de `ReceivableService`, sempre `type = RECEIVABLE` (FR-009);
  `findAll(unitId, supplierId, type, paid, overdue, dueYearMonth, paymentYearMonth)`: quando
  `unitId` ou `supplierId` for informado, usa a consulta dedicada do repositório
  (`findByUnitId`/`findBySupplierId`, T009) como base — nunca os dois ao mesmo tempo, já que
  são mutuamente exclusivos por `type` — lançando `NotFoundException` (404) quando o valor
  informado não corresponde a um registro cadastrado (mesmo padrão já usado para `unitId` na
  feature 002); caso contrário, parte de `findAll()`. Os demais filtros (`type`, `paid`,
  `overdue`, `dueYearMonth`, `paymentYearMonth`, incluindo o novo `type` do FR-012) são
  sempre aplicados em memória sobre esse resultado base, mesmo padrão já usado na feature 002
  para `paid`/`overdue`/mês; `findById`; `update(...)` rejeitando `type` diferente do já
  persistido (`AccountTypeChangeNotAllowedException`, FR-006/FR-015), demais campos com a
  mesma validação da criação; `registerPayment(id, paymentDate)` portado sem alteração de
  comportamento; `delete(id)` portado sem alteração — em
  `backend/src/main/java/com/financas/account/domain/AccountService.java` (depends on T010,
  T011, T012, T013)
- [ ] T015 Implement `SupplierService` — `create(name, unitId, pixKey)` com `name` obrigatório
  e `unitId` opcional (busca a unidade quando informado, 404 se não existir); `findAll`;
  `findById`; `update(...)`; `delete(id)` bloqueando via
  `accountRepository.existsBySupplierId(id)` (FR-005) — em
  `backend/src/main/java/com/financas/supplier/domain/SupplierService.java` (depends on T007, T010)
- [ ] T016 [P] Create DTOs `AccountRequest` (`type` `@NotNull`, `amount` `@NotNull
  @PositiveOrZero`, `dueDate`/`description`/`fund` `@NotNull`/`@NotBlank`, `recurring`
  `@NotNull`, `unitId`/`supplierId` nullable Long, `paymentDate`/`observations` opcionais),
  `AccountBulkRequest` (igual, sem `type`/`unitId`/`supplierId`), `AccountPaymentRequest`
  (`paymentDate` `@NotNull`) e `AccountResponse` (com `UnitResponse`/`SupplierResponse`
  aninhados quando aplicável, factory estático `from(Account)`) em
  `backend/src/main/java/com/financas/account/api/` (depends on T008)
- [ ] T017 [P] Create DTOs `SupplierRequest` (`name` `@NotBlank`, `unitId`/`pixKey`
  opcionais) e `SupplierResponse` (com `UnitResponse` aninhado quando houver, factory
  `from(Supplier)`) em `backend/src/main/java/com/financas/supplier/api/` (depends on T005)
- [ ] T018 Implement `AccountController` — `GET /api/accounts` (filtros `unitId`,
  `supplierId`, `type`, `paid`, `overdue`, `dueYearMonth`, `paymentYearMonth`), `GET
  /api/accounts/{id}`, `POST /api/accounts`, `POST /api/accounts/bulk`, `PUT
  /api/accounts/{id}`, `POST /api/accounts/{id}/pay`, `DELETE /api/accounts/{id}` — em
  `backend/src/main/java/com/financas/account/api/AccountController.java` (depends on T014, T016)
- [ ] T019 Implement `SupplierController` — `GET /api/suppliers`, `GET /api/suppliers/{id}`,
  `POST /api/suppliers`, `PUT /api/suppliers/{id}`, `DELETE /api/suppliers/{id}` — em
  `backend/src/main/java/com/financas/supplier/api/SupplierController.java` (depends on T015, T017)
- [ ] T020 [P] Remove por completo o pacote `com.financas.receivable` (domain/api/infra) —
  substituído por `com.financas.account` (depends on T018)
- [ ] T021 [P] Create model `Account`/`AccountRequest`/`AccountBulkRequest`/
  `AccountPaymentRequest`, union types `AccountType`/`Fund` (+ labels em português, ex.:
  `FUND_LABELS`) em `frontend/src/app/shared/models/account.model.ts` (portado e estendido de
  `receivable.model.ts`)
- [ ] T022 Create `AccountService` (HttpClient: `findAll` com todos os filtros, `findById`,
  `create`, `createBulk`, `update`, `registerPayment`, `delete`) em
  `frontend/src/app/shared/services/account.service.ts` (depends on T021, T018)
- [ ] T023 [P] Create model `Supplier`/`SupplierRequest` em
  `frontend/src/app/shared/models/supplier.model.ts`
- [ ] T024 Create `SupplierService` (HttpClient CRUD) em
  `frontend/src/app/shared/services/supplier.service.ts` (depends on T023, T019)
- [ ] T025 [P] Remove `frontend/src/app/shared/models/receivable.model.ts` e
  `frontend/src/app/shared/services/receivable.service.ts` (substituídos por T021/T022)
- [ ] T026 [P] Create testes unitários `AccountServiceTest` (Mockito): `create` rejeita valor
  negativo e aceita zero (FR-008); `create` rejeita contraparte ausente ou incompatível com
  `type` (`RECEIVABLE` com `supplierId`, `PAYABLE` com `unitId`, ou nenhuma informada — FR-007);
  `update` rejeita `type` diferente do persistido (`AccountTypeChangeNotAllowedException` —
  FR-006/FR-015); `createForAllUnits` sempre cria com `type = RECEIVABLE`, um por unidade,
  lança `NoUnitsRegisteredException` se não houver unidade; `findAll` com filtro `type`
  isolado e combinado com `supplierId`/`unitId`/`paid`/`overdue`/mês (FR-012); `findById`/
  `update`/`delete` de conta inexistente lançam `NotFoundException` — em
  `backend/src/test/java/com/financas/account/domain/AccountServiceTest.java` (depends on T014)
- [ ] T027 [P] Create testes unitários `SupplierServiceTest` (Mockito): criação com e sem
  `unitId`/`pixKey`; criação com `unitId` inexistente lança `NotFoundException`; `findById`/
  `update`/`delete` de fornecedor inexistente lançam `NotFoundException`; `delete` bloqueado
  (`ConflictException`) quando `AccountRepository.existsBySupplierId` retorna `true`, permitido
  quando `false` (FR-005) — em
  `backend/src/test/java/com/financas/supplier/domain/SupplierServiceTest.java` (depends on T015)

**Checkpoint**: `mvn test` passando; `/api/accounts` e `/api/suppliers` funcionais e testados
por trás da API (via `curl`). Pronto para as user stories, majoritariamente frontend a partir
daqui.

---

## Phase 3: User Story 1 - Cadastrar fornecedor (Priority: P1) 🎯 MVP

**Goal**: Permitir cadastrar um fornecedor (nome, unidade opcional, chave PIX opcional) e vê-lo
na listagem.

**Independent Test**: Cadastrar um fornecedor com e sem unidade vinculada, com e sem chave
PIX, e confirmar que aparece na listagem; tentar cadastrar sem nome e confirmar rejeição.

### Implementation for User Story 1

- [ ] T028 [US1] Create componente `supplier-form` (campos nome, seletor de unidade opcional
  via `UnitService` já existente, chave PIX opcional; validação de nome obrigatório; suporte a
  modo de edição — pré-preenchimento via `GET /api/suppliers/{id}` e submissão via `PUT`
  quando acessado com um id de rota, mesmo padrão de `account-form`/`receivable-form`) em
  `frontend/src/app/supplier/supplier-form/` (depends on T024)
- [ ] T029 [US1] Create componente `supplier-list` (tabela: nome, unidade vinculada, chave
  PIX; mensagem de "nenhum fornecedor cadastrado" quando vazia) em
  `frontend/src/app/supplier/supplier-list/` (depends on T024)
- [ ] T030 [US1] Wire rotas `/suppliers` e `/suppliers/new` em
  `frontend/src/app/app.routes.ts` (depends on T028, T029)
- [ ] T031 [US1] Add link de navegação "Fornecedores" em `frontend/src/app/app.html`
  (depends on T030)

**Checkpoint**: User Story 1 completa e testável de forma independente (Acceptance Scenarios
1-5 do spec).

---

## Phase 4: User Story 2 - Lançar conta a pagar para um fornecedor (Priority: P1)

**Goal**: Permitir lançar uma conta a pagar para um fornecedor (valor — incluindo zero —,
vencimento, descrição, fundo, observações) e vê-la na listagem unificada.

**Independent Test**: Com ao menos um fornecedor cadastrado (US1), lançar uma conta a pagar
preenchendo todos os campos (incluindo observações) e confirmar que é criada; tentar lançar
com valor negativo ou campo obrigatório faltando e confirmar rejeição; lançar com valor zero e
confirmar que é aceito; tentar lançar sem nenhum fornecedor cadastrado e confirmar a
orientação para cadastrar um fornecedor primeiro.

### Implementation for User Story 2

- [ ] T032 [US2] Create componente `account-form` (portado e estendido de
  `receivable-form`): seletor de tipo "A pagar"/"A receber" no topo, alternando o seletor de
  contraparte — unidade (com opção de lote "para todas as unidades", herdada da feature 002,
  visível só para "A receber") ou fornecedor (via `SupplierService`, com orientação para
  cadastrar um fornecedor primeiro quando não houver nenhum — FR-021, visível só para "A
  pagar"); campos comuns valor (`Validators.min(0)` — aceita zero), vencimento, descrição,
  fundo, "Recorrente", data de pagamento opcional, observações (novo, `<textarea>`); suporte a
  modo de edição (pré-preenchimento) portado do `receivable-form` original — em
  `frontend/src/app/account/account-form/` (depends on T022, T024)
- [ ] T033 [US2] Wire rota `/accounts/new` em `frontend/src/app/app.routes.ts` (depends on T032)
- [ ] T034 [P] [US2] Remove `frontend/src/app/receivable/receivable-form/` (substituído por
  `account-form`)

**Checkpoint**: User Story 2 completa e testável de forma independente (Acceptance Scenarios
1-6 do spec, incluindo valor zero aceito e observações salvas).

---

## Phase 5: User Story 3 - Ver todas as contas (a pagar e a receber) na mesma listagem (Priority: P1)

**Goal**: Exibir uma listagem única de todas as contas, diferenciadas por cor e rótulo de
tipo, com filtro por tipo combinável com os filtros já existentes.

**Independent Test**: Com ao menos uma conta a receber e uma a pagar já lançadas, acessar a
listagem única e confirmar que ambas aparecem com cor/rótulo distintos; aplicar o filtro por
tipo e confirmar que só o tipo selecionado aparece; combinar com outro filtro (ex.: vencidos)
e confirmar a combinação; com nenhuma conta cadastrada, confirmar a indicação de lista vazia.

### Implementation for User Story 3

- [ ] T035 [US3] Create componente `account-list` (portado e estendido de
  `receivable-list`): coluna/rótulo textual de tipo, classe CSS por linha
  (`account-row--receivable`/`account-row--payable`, tons verde/vermelho), coluna de
  contraparte (identificador da unidade quando "a receber", nome do fornecedor quando "a
  pagar"), indicador de observações (ex.: ícone ou texto truncado quando `observations`
  estiver preenchido, com o conteúdo completo visível ao editar — satisfaz FR-018/SC-007 sem
  exigir uma coluna larga na tabela), `<select>` de filtro por tipo (Todas/A pagar/A receber)
  combinável com os filtros já existentes (status de pagamento, vencidos, mês de
  vencimento/pagamento), mensagem de "nenhuma conta cadastrada" quando vazia — em
  `frontend/src/app/account/account-list/` (depends on T022)
- [ ] T036 [US3] Apply seleção múltipla (`list-selection.ts`) e `bulk-actions-bar` +
  `bulk-delete.ts` (já existentes, sem alteração de contrato) a `account-list`,
  independentemente do tipo de cada conta selecionada (FR-020) em
  `frontend/src/app/account/account-list/` (depends on T035)
- [ ] T037 [US3] Wire rota `/accounts` em `frontend/src/app/app.routes.ts`; update link de
  navegação de "Lançamentos" para "Contas" em `frontend/src/app/app.html` (depends on T035)
- [ ] T038 [P] [US3] Remove `frontend/src/app/receivable/receivable-list/` (substituído por
  `account-list`)

**Checkpoint**: User Story 3 completa e testável de forma independente (Acceptance Scenarios
1-4 do spec).

---

## Phase 6: User Story 4 - Registrar pagamento de uma conta a pagar (Priority: P2)

**Goal**: Confirmar que o registro de pagamento (herdado sem alteração de comportamento da
feature 002 — `registerPayment`, já portado na Fase 2) funciona igualmente para contas do
tipo "a pagar", incluindo a lógica de "vencida".

**Independent Test**: Com uma conta a pagar já criada (US2), registrar seu pagamento com uma
data e confirmar que passa a aparecer como paga; tentar registrar sem informar a data e
confirmar rejeição; com uma conta a pagar vencida e sem pagamento, confirmar que aparece no
filtro "vencidos".

### Implementation for User Story 4

- [ ] T039 [P] [US4] Add ao `AccountServiceTest` casos cobrindo explicitamente o tipo
  `PAYABLE`: `registerPayment` marca/atualiza `paymentDate` de uma conta a pagar; `findAll`
  com `overdue = true` inclui uma conta a pagar pendente com `dueDate` no passado e exclui uma
  já paga (mesma lógica de `RECEIVABLE`, reforço de cobertura "para os dois tipos" — FR-014)
  em `backend/src/test/java/com/financas/account/domain/AccountServiceTest.java` (depends on T026)
- [ ] T040 [US4] Validar manualmente que a ação "Registrar pagamento" já portada em
  `account-list` (T035, herdada de `receivable-list`) funciona sem alteração de código também
  para contas do tipo "a pagar" — sem tarefa de implementação nova esperada; se algum ajuste
  for necessário, corrigir em `frontend/src/app/account/account-list/` (depends on T035, T032)

**Checkpoint**: User Story 4 completa e testável de forma independente (Acceptance Scenarios
1-3 do spec).

---

## Phase 7: User Story 5 - Editar e remover contas e fornecedores (Priority: P3)

**Goal**: Permitir editar e remover contas (a pagar ou a receber) e fornecedores já
cadastrados, respeitando a imutabilidade do tipo da conta e o bloqueio de remoção de
fornecedor vinculado.

**Independent Test**: Editar uma conta existente (valor, observações, contraparte) e
confirmar que a listagem reflete os novos valores; tentar alterar o tipo de uma conta e
confirmar que não é permitido; remover uma conta e confirmar que some da listagem; remover um
fornecedor sem vínculo e confirmar que some da listagem; tentar remover um fornecedor com
conta a pagar vinculada e confirmar o bloqueio.

### Implementation for User Story 5

- [ ] T041 [US5] Add ação "editar" (navega ao `account-form` em modo edição) e "remover" (com
  diálogo de confirmação) nas linhas de `account-list` em
  `frontend/src/app/account/account-list/` (depends on T035, T032)
- [ ] T042 [P] [US5] Wire rota `/accounts/:id/edit` em `frontend/src/app/app.routes.ts`
  (depends on T037, T041)
- [ ] T043 [US5] Desabilitar o seletor de tipo em `account-form` quando em modo de edição
  (reflete a imutabilidade de `type`, FR-006/FR-015); exibir a mensagem de erro do backend
  caso uma tentativa indevida ainda ocorra (400) em `frontend/src/app/account/account-form/`
  (depends on T032, T042)
- [ ] T044 [US5] Add ação "editar" (navega ao `supplier-form` em modo edição) e "remover" (com
  diálogo de confirmação, exibindo a mensagem de erro 409 do backend quando houver conta a
  pagar vinculada) nas linhas de `supplier-list` em `frontend/src/app/supplier/supplier-list/`
  (depends on T029, T028)
- [ ] T045 [P] [US5] Wire rota `/suppliers/:id/edit` em `frontend/src/app/app.routes.ts`
  (depends on T030, T044)

**Checkpoint**: Todas as 5 user stories funcionam de forma independente.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validações finais e documentação

- [ ] T046 [P] Run roteiro de validação manual de `quickstart.md` de ponta a ponta (API real
  + navegador), incluindo os cenários 16-17 (bloqueio de remoção de unidade com fornecedor
  vinculado; remoção completa de condômino), que dependem de
  `specs/001-cadastro-condominos/tasks.md` Phase 13-14 já terem sido implementadas
- [ ] T047 [P] Update `README.md` com as decisões técnicas desta feature (generalização
  `Receivable`→`Account`, modelagem de contraparte com duas FKs nullable + `CHECK`
  constraint, entidade `Supplier`, remoção completa de condômino, valor não negativo — zero
  aceito), e remover/atualizar itens já obsoletos de "O que eu faria diferente ou melhoraria
  com mais tempo" (ex.: "soft delete de condôminos" deixa de fazer sentido; "`TargetAccount`
  como cadastro dinâmico" passa a se referir a `Fund`), conforme Fluxo de Commits da
  constituição
- [ ] T048 [P] Review mensagens de erro do `GlobalExceptionHandler` para os novos casos desta
  feature (400/404/409 de `accounts`/`suppliers`), garantindo consistência em português
  (Convenções de API REST)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem tarefas novas — já entregue pela feature 001
- **Foundational (Phase 2)**: Bloqueia todas as user stories desta feature — migração completa
  de `Receivable`→`Account` e criação de `Supplier`
- **User Story 1 (Phase 3)**: Depende apenas do Foundational
- **User Story 2 (Phase 4)**: Depende do Foundational; reaproveita `SupplierService`/
  `SupplierService` (frontend, T024) de US1 para o seletor de fornecedor
- **User Story 3 (Phase 5)**: Depende do Foundational; independente de US1/US2 no código
  (mas precisa de ao menos uma conta de cada tipo já lançada para o teste ponta a ponta)
- **User Story 4 (Phase 6)**: Depende de US2 (T032, para lançar uma conta a pagar de teste) e
  US3 (T035, listagem onde a ação de pagamento já foi portada)
- **User Story 5 (Phase 7)**: Depende de US1 (T028, T029), US2 (T032) e US3 (T035, T037)
- **Polish (Phase 8)**: Depende de todas as user stories desejadas estarem completas

### Impacto cruzado com a feature 001

A remoção completa do cadastro de condôminos e a atualização de `UnitService.delete()` (para
checar `Account`/`Supplier` em vez de `Resident`) estão registradas em
`specs/001-cadastro-condominos/tasks.md`, Phases 13 e 14. Essas tarefas dependem de T008
(entidade `Account`) e T005 (entidade `Supplier`) desta feature já existirem, e devem ser
executadas depois delas.

### Notas de dependência entre stories

Diferente de 001/002, aqui o backend não cresce incrementalmente por story (ver blurb
"Tests" no topo) — toda a Fase 2 já entrega a API completa e testada. A partir daí, US1-US5
são, na prática, principalmente tarefas de frontend: US2/US3 reaproveitam e estendem
`receivable-form`/`receivable-list` (agora `account-form`/`account-list`); US4 é
majoritariamente verificação; US5 adiciona as ações de edição/remoção nas listagens já
existentes. Isso reflete a ordem P1 → P1 → P1 → P2 → P3 já definida no spec.

### Parallel Opportunities

- T001, T003, T004, T005 (Foundational) podem rodar em paralelo entre si no início
- T011, T012, T013 (exceptions) podem rodar em paralelo entre si
- T016, T017 (DTOs) podem rodar em paralelo entre si
- T021, T023 (models frontend) podem rodar em paralelo entre si
- T026, T027 (testes) podem rodar em paralelo entre si
- T028, T029 (US1) podem rodar em paralelo entre si antes de T030
- T042, T045 (rotas de edição, US5) podem rodar em paralelo entre si

---

## Parallel Example: Foundational

```bash
# Enums e entidade Supplier em paralelo:
Task: "Create enum AccountType em backend/src/main/java/com/financas/account/domain/AccountType.java"
Task: "Create enum Fund em backend/src/main/java/com/financas/account/domain/Fund.java"
Task: "Create entidade JPA Supplier em backend/src/main/java/com/financas/supplier/domain/Supplier.java"

# Exceptions em paralelo:
Task: "Create InvalidAccountAmountException em backend/src/main/java/com/financas/account/domain/"
Task: "Create AccountTypeChangeNotAllowedException em backend/src/main/java/com/financas/account/domain/"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 2: Foundational (CRITICAL — bloqueia todas as stories; entrega a API
   completa de `Account`/`Supplier`)
2. Complete Phase 3: User Story 1 (cadastro de fornecedor)
3. **STOP and VALIDATE**: testar US1 isoladamente (cenários 1-5 do spec)

### Incremental Delivery

1. Foundational → API completa de contas e fornecedores pronta e testada
2. US1 → cadastro de fornecedores funcionando (MVP mínimo de UI)
3. US2 → lançar conta a pagar, reaproveitando US1
4. US3 → listagem unificada com cor/rótulo/filtro por tipo
5. US4 → confirmação de que pagamento funciona para contas a pagar
6. US5 → edição e remoção de contas e fornecedores
7. (Cross-feature) Extensão de `UnitService`/remoção de condômino na feature 001 (Phases 13-14
   de `specs/001-cadastro-condominos/tasks.md`)

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes
- Cada user story é independentemente completável e testável, exceto pelas dependências de
  dados descritas acima
- Commit ao final de cada tarefa ou grupo lógico de tarefas
- Pare em qualquer checkpoint para validar a story isoladamente antes de seguir
