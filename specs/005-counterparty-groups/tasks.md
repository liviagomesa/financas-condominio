---

description: "Task list template for feature implementation"
---

# Tasks: Unificação de Unidade/Fornecedor, Contas sem Restrição de Tipo e Grupos

**Input**: Design documents from `/specs/005-counterparty-groups/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [quickstart.md](./quickstart.md)

**Tests**: OBRIGATÓRIO — a constituição do projeto (Princípio III) exige tarefas de teste
automatizado para toda regra de negócio de uma feature nova. Assim como as features 003/004,
esta feature é fundamentalmente uma **migração/unificação de modelagem** — `Unit` e `Supplier`
deixam de existir, substituídos por `Party`; `Account` passa a depender de `Party` (e,
opcionalmente, de `Group` para lançamento em lote) em vez de duas FKs mutuamente exclusivas. Por
isso, o backend completo (entidades, repositórios, serviços com todas as regras de negócio,
controllers e testes Mockito) é entregue de uma vez na Fase 2 (Foundational), junto com o
cadastro de Parte no frontend (substituindo `unit`/`supplier`, sem story própria no spec — é
infraestrutura usada por todas as 5 user stories igualmente). As fases de user story a partir
daí são majoritariamente **frontend**, ajustando incrementalmente `account-form`/`account-list`.

**Nota de sequenciamento (compilação do frontend)**: `account-form.ts`/`account-list.ts` só são
atualizados para o novo modelo (`Party` em vez de `Unit`/`Supplier`) dentro das fases de User
Story 1 e 4, respectivamente — não na Fase 2. Isso é proposital: `frontend/src/app/unit/`,
`frontend/src/app/supplier/` e seus models/services correspondentes continuam existindo
(órfãos de rota, mas compilando normalmente) até que nada mais os importe, exatamente como já
ocorreu nas features 002→003 com `receivable`. A Fase 2 só cria arquivos novos (Parte) e ajusta
rotas/menu — nunca edita `account-form`/`account-list`/`account.model.ts`.

**Organization**: Tarefas agrupadas por user story (spec.md) para permitir implementação e teste
independentes de cada uma, na medida em que a natureza de migração desta feature permite.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1..US5)
- Caminhos de arquivo exatos estão incluídos em cada descrição

## Path Conventions (Web app — ver plan.md)

- Backend: `backend/src/main/java/com/financas/party/...` (novo), `.../group/...` (novo),
  `.../account/...` (ajustado); `.../unit/...` e `.../supplier/...` são removidos
- Frontend: `frontend/src/app/party/...` (novo), `.../group/...` (novo), `.../account/...`
  (ajustado); `.../unit/...` e `.../supplier/...` são removidos
- Ambos os projetos e o banco (`docker-compose.yml`) já existem, criados pela feature 001

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto — não se aplica: `backend/`, `frontend/` e
`docker-compose.yml` já existem (feature 001) e são reaproveitados sem alteração.

Nenhuma tarefa nova nesta fase.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Unificar `Unit`/`Supplier` em `Party`, criar `Group`, e migrar `Account` para
referenciar `Party` (com lançamento em lote via `Group`) — infraestrutura que TODAS as user
stories desta feature dependem igualmente. Inclui migrations, entidades, repositórios, serviços
(com todas as regras de negócio novas), controllers, DTOs, os testes automatizados dessas
regras, e o cadastro de Parte no frontend (substituindo as telas de Unidade/Fornecedor, que não
têm uma user story própria no spec desta feature).

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase estar completa

- [X] T001 [P] Create migration Flyway
  `backend/src/main/resources/db/migration/V10__create_party_table.sql` (tabela `party`: `id`,
  `name VARCHAR(255) NOT NULL`, `pix_key VARCHAR(255) NULL`; índice único sobre
  `LOWER(TRIM(name))`, mesmo padrão de `unit_identifier_normalized_idx`/`fund_name_normalized_idx`
  — ver data-model.md)
- [X] T002 [P] Create migration Flyway
  `backend/src/main/resources/db/migration/V11__create_group_tables.sql` (tabela `party_group`:
  `id`, `name VARCHAR(255) NOT NULL`, índice único sobre `LOWER(TRIM(name))`; tabela de junção
  `party_group_member`: `group_id BIGINT NOT NULL REFERENCES party_group (id) ON DELETE CASCADE`,
  `party_id BIGINT NOT NULL REFERENCES party (id) ON DELETE CASCADE`, chave primária composta
  `(group_id, party_id)`, índice adicional em `party_id` — ver research.md sobre o nome físico
  `party_group` evitar a palavra reservada SQL `GROUP`)
- [X] T003 Create migration Flyway
  `backend/src/main/resources/db/migration/V12__migrate_account_to_party.sql`
  (`ALTER TABLE account DROP CONSTRAINT account_type_counterparty_check`; `TRUNCATE TABLE
  account` — banco apenas de desenvolvimento, sem dado a preservar, ver research.md/spec
  Assumptions; `ALTER TABLE account DROP COLUMN unit_id`; `ALTER TABLE account DROP COLUMN
  supplier_id`; `ALTER TABLE account ADD COLUMN party_id BIGINT NOT NULL REFERENCES party (id)`;
  `CREATE INDEX account_party_id_idx ON account (party_id)`) (depends on T001)
- [X] T004 Create migration Flyway
  `backend/src/main/resources/db/migration/V13__drop_unit_and_supplier_tables.sql`
  (`DROP TABLE supplier`; `DROP TABLE unit`) (depends on T003)
- [X] T005 [P] Create entidade JPA `Party` (`id`, `name` obrigatório, `pixKey` opcional) em
  `backend/src/main/java/com/financas/party/domain/Party.java`
- [X] T006 Create interface de porta `PartyRepository` (`save`, `findById`, `findAll` ordenado
  por nome, `findByNormalizedName`, `deleteById`, `existsById`) em
  `backend/src/main/java/com/financas/party/domain/PartyRepository.java` (depends on T005)
- [X] T007 Create `PartyJpaRepository` (Spring Data — query JPQL `findByNormalizedName` igual a
  `FundJpaRepository`/`UnitJpaRepository`, e `findAllByOrderByNameAsc`) e `PartyRepositoryImpl`
  em `backend/src/main/java/com/financas/party/infra/` (depends on T006)
- [X] T008 [P] Create entidade JPA `Group` (`id`, `name` obrigatório, `members: Set<Party>` via
  `@ManyToMany @JoinTable(name = "party_group_member", joinColumns = @JoinColumn(name =
  "group_id"), inverseJoinColumns = @JoinColumn(name = "party_id"))`, mapeada para a tabela
  `party_group` — ver research.md sobre `Set` em vez de `List`) em
  `backend/src/main/java/com/financas/group/domain/Group.java` (depends on T005)
- [X] T009 Create interface de porta `GroupRepository` (`save`, `findById`, `findAll` ordenado
  por nome, `findByNormalizedName`, `deleteById`, `existsById`) em
  `backend/src/main/java/com/financas/group/domain/GroupRepository.java` (depends on T008)
- [X] T010 Create `GroupJpaRepository` (Spring Data) e `GroupRepositoryImpl` em
  `backend/src/main/java/com/financas/group/infra/` (depends on T009)
- [X] T011 Update entidade `Account`: campos `unit`/`supplier` (`@ManyToOne(optional = true)`)
  removidos, substituídos por `party: Party` (`@ManyToOne(optional = false) @JoinColumn(name =
  "party_id", nullable = false)`) em
  `backend/src/main/java/com/financas/account/domain/Account.java` (depends on T005)
- [X] T012 Update interface `AccountRepository` — remove `findByUnitId`/`findBySupplierId`/
  `existsByUnitId`/`existsBySupplierId`, adiciona `List<Account> findByPartyId(Long partyId)` e
  `boolean existsByPartyId(Long partyId)` em
  `backend/src/main/java/com/financas/account/domain/AccountRepository.java` (depends on T011)
- [X] T013 Update `AccountJpaRepository` (deriva `findByPartyId`/`existsByPartyId` via
  `account.party.id`) e `AccountRepositoryImpl` (implementa os dois novos métodos, remove os
  antigos de unit/supplier) em `backend/src/main/java/com/financas/account/infra/` (depends on
  T012)
- [X] T014 [P] Create `DuplicatePartyException extends ConflictException`
  ("Já existe uma parte cadastrada com o nome '...'.") em
  `backend/src/main/java/com/financas/party/domain/DuplicatePartyException.java`
- [X] T015 [P] Create `PartyHasAccountsException extends ConflictException`
  ("Esta parte possui contas vinculadas e não pode ser removida.") em
  `backend/src/main/java/com/financas/party/domain/PartyHasAccountsException.java` (ver
  research.md — substitui o `ConflictException` genérico que `SupplierService` usava)
- [X] T016 [P] Create `DuplicateGroupException extends ConflictException`
  ("Já existe um grupo cadastrado com o nome '...'.") em
  `backend/src/main/java/com/financas/group/domain/DuplicateGroupException.java`
- [X] T017 [P] Create `EmptyGroupException extends ConflictException`
  ("O grupo selecionado não possui integrantes. Adicione integrantes ao grupo antes de lançar
  contas em lote.") em
  `backend/src/main/java/com/financas/account/domain/EmptyGroupException.java`
- [X] T018 [P] Remove `NoUnitsRegisteredException`
  (`backend/src/main/java/com/financas/account/domain/NoUnitsRegisteredException.java`) — o
  atalho implícito "todas as unidades" deixa de existir, generalizado por `Group` (FR-014/FR-015,
  ver spec Assumptions)
- [X] T019 [P] Create DTOs `PartyRequest` (`name` `@NotBlank`, `pixKey` opcional) e
  `PartyResponse` (`id`, `name`, `pixKey`; factory estático `from(Party)`) em
  `backend/src/main/java/com/financas/party/api/` (depends on T005)
- [X] T020 [P] Create DTOs `GroupRequest` (`name` `@NotBlank`, `partyIds: List<Long>`) e
  `GroupResponse` (`id`, `name`, `members: List<PartyResponse>` ordenados por nome; factory
  estático `from(Group)`) em `backend/src/main/java/com/financas/group/api/` (depends on T008,
  T019)
- [X] T021 [P] Update DTOs `AccountRequest` (`unitId`/`supplierId` → `partyId: Long`) e
  `AccountBulkRequest` (adiciona `type: AccountType` explícito e `groupId: Long`, substituindo o
  antigo tipo/contraparte implícitos) em `backend/src/main/java/com/financas/account/api/`
  (depends on T011)
- [X] T022 [P] Update `AccountResponse`: campos `unit`/`supplier` (`UnitResponse`/
  `SupplierResponse`, nullable) → campo único `party` (`PartyResponse`, nunca nulo);
  `from(Account account, FundResponse fundResponse)` monta `PartyResponse.from(account.getParty())`
  sem checagem de nulo em
  `backend/src/main/java/com/financas/account/api/AccountResponse.java` (depends on T019, T011)
- [X] T023 Implement `PartyService` — `create(name, pixKey)` validando nome único
  (`findByNormalizedName`, FR-004); `findAll()` ordenado por nome; `findById`; `update(id, name,
  pixKey)` validando nome único (ignorando o próprio id); `delete(id)` bloqueando via
  `accountRepository.existsByPartyId(id)` (`PartyHasAccountsException`, FR-006) — em
  `backend/src/main/java/com/financas/party/domain/PartyService.java` (depends on T007, T014,
  T015, T013)
- [X] T024 Implement `GroupService` — `create(name, partyIds)`/`update(id, name, partyIds)`
  validando nome único (`findByNormalizedName`) e resolvendo cada `partyId` via
  `partyRepository.findById` (`NotFoundException` se algum não existir, FR-013); `findAll()`
  ordenado por nome; `findById`; `delete(id)` **sempre permitido**, sem nenhuma checagem de
  bloqueio, mesmo com integrantes (FR-016 — a exclusão remove só o `Group` e as linhas de
  `party_group_member`, via `ON DELETE CASCADE`) — em
  `backend/src/main/java/com/financas/group/domain/GroupService.java` (depends on T010, T016,
  T006)
- [X] T025 Update `AccountService`: remove `resolveUnit`/`resolveSupplier`, adiciona
  `resolveParty(Long partyId)` (sempre obrigatório, sem ramificação por `type` — FR-001);
  `create`/`update` passam a receber `Long partyId` (em vez de `unitId`/`supplierId`);
  `createForAllUnits` é substituído por `createForGroup(AccountType type, BigDecimal amount,
  LocalDate dueDate, String description, Long fundId, boolean recurring, Long groupId,
  LocalDate paymentDate, String observations)` — resolve o `Group` via `GroupRepository`, lança
  `EmptyGroupException` se `group.getMembers()` estiver vazio (FR-015), senão cria uma `Account`
  por integrante com o `type` explícito informado (FR-014); `findAll` troca
  `unitId`/`supplierId` por `partyId` único (base da consulta: `partyId != null →
  findByPartyId(partyId)`, senão `findAll()`) e adiciona `fundId` como filtro em memória
  adicional, combinado por E lógico com os demais (FR-009/FR-010); injeta `PartyRepository` e
  `GroupRepository` — em `backend/src/main/java/com/financas/account/domain/AccountService.java`
  (depends on T013, T006, T009, T017)
- [X] T026 Implement `PartyController` — `GET /api/parties`, `GET /api/parties/{id}`, `POST
  /api/parties`, `PUT /api/parties/{id}`, `DELETE /api/parties/{id}` — em
  `backend/src/main/java/com/financas/party/api/PartyController.java` (depends on T023, T019)
- [X] T027 Implement `GroupController` — `GET /api/groups`, `GET /api/groups/{id}`, `POST
  /api/groups`, `PUT /api/groups/{id}`, `DELETE /api/groups/{id}` — em
  `backend/src/main/java/com/financas/group/api/GroupController.java` (depends on T024, T020)
- [X] T028 Update `AccountController`: `GET /api/accounts` troca `unitId`/`supplierId` por
  `partyId`, adiciona `fundId`; `POST`/`PUT` passam `request.partyId()` em vez de
  `unitId()`/`supplierId()`; `POST /api/accounts/bulk` chama `service.createForGroup(...)` com
  `request.type()` e `request.groupId()` — em
  `backend/src/main/java/com/financas/account/api/AccountController.java` (depends on T025,
  T021, T022)
- [X] T029 [P] Remove por completo o pacote `com.financas.unit`
  (`backend/src/main/java/com/financas/unit/`: `Unit`, `UnitRepository`, `UnitService`,
  `DuplicateUnitException`, `UnitHasAccountsException`, `UnitHasSuppliersException`, `api/`,
  `infra/`) — substituído por `com.financas.party` (depends on T025)
- [X] T030 [P] Remove por completo o pacote `com.financas.supplier`
  (`backend/src/main/java/com/financas/supplier/`: `Supplier`, `SupplierRepository`,
  `SupplierService`, `api/`, `infra/`) — substituído por `com.financas.party` (depends on T025)
- [X] T031 [P] Remove `backend/src/test/java/com/financas/unit/` e
  `backend/src/test/java/com/financas/supplier/` por completo (depends on T029, T030)
- [X] T032 [P] Create testes unitários `PartyServiceTest` (Mockito): `create`/`update` rejeitam
  nome duplicado (case-insensitive, espaços nas extremidades) e aceitam nomes distintos
  (FR-004); `delete` bloqueado (`PartyHasAccountsException`) quando
  `AccountRepository.existsByPartyId` retorna `true`, permitido quando `false` (FR-006) — em
  `backend/src/test/java/com/financas/party/domain/PartyServiceTest.java` (depends on T023)
- [X] T033 [P] Create testes unitários `GroupServiceTest` (Mockito): `create`/`update` rejeitam
  nome duplicado; `create`/`update` resolvem `partyIds` via `PartyRepository.findById`, lançando
  `NotFoundException` quando algum id não existir (FR-013); `delete` sempre permitido — com
  integrantes e sem integrantes, sem nenhuma checagem de bloqueio (FR-016) — em
  `backend/src/test/java/com/financas/group/domain/GroupServiceTest.java` (depends on T024)
- [X] T034 Update testes unitários `AccountServiceTest`: trocar mocks de `UnitRepository`/
  `SupplierRepository` por `PartyRepository`; casos comprovando que qualquer combinação
  `type`×`party` é aceita (SAÍDA com uma Parte, ENTRADA com a mesma Parte — FR-001), sem mais
  nenhuma restrição de `resolveUnit`/`resolveSupplier`; `AccountTypeChangeNotAllowedException`
  continua coberta; substituir os testes de `createForAllUnits` por `createForGroup`: cria uma
  `Account` por integrante do `Group` mockado, com o `type` explícito informado; lança
  `EmptyGroupException` quando `group.getMembers()` está vazio (FR-015); `findAll` com filtro
  `partyId` isolado e `fundId` combinado com os demais filtros já existentes (FR-009/FR-010) —
  em `backend/src/test/java/com/financas/account/domain/AccountServiceTest.java` (depends on
  T025)
- [X] T035 [P] Create model `Party`/`PartyRequest` em
  `frontend/src/app/shared/models/party.model.ts`
- [X] T036 Create `PartyService` (HttpClient: `findAll`, `findById`, `create`, `update`,
  `delete`) em `frontend/src/app/shared/services/party.service.ts` (depends on T035, T026)
- [X] T037 [P] Create componente `party-list` (tabela: nome, chave pix; ação individual
  "Editar" por linha, navegando para `/parties/{id}/edit`, e "Remover" por linha com `confirm()`
  exibindo a mensagem de erro 409 do backend quando houver conta vinculada — mesmo padrão de
  `fund-list`/`account-list`; seleção múltipla/remoção em lote via `list-selection.ts`/
  `bulk-delete.ts`/`bulk-actions-bar` já existentes; mensagem de "nenhuma parte cadastrada"
  quando vazia) em `frontend/src/app/party/party-list/` (depends on T036)
- [X] T038 [P] Create componente `party-form` (campos nome e chave pix, ambos os únicos campos
  da entidade — sem seletor de grupo, FR-013; suporte a modo de edição via `GET
  /api/parties/{id}` + `PUT`, mesmo padrão de `unit-form`/`supplier-form`) em
  `frontend/src/app/party/party-form/` (depends on T036)
- [X] T039 Wire rotas `/parties`, `/parties/new`, `/parties/:id/edit` em
  `frontend/src/app/app.routes.ts`; remove as rotas e imports de `UnitList`/`UnitForm`/
  `SupplierList`/`SupplierForm` (os componentes em si NÃO são apagados ainda — continuam
  compilando, só ficam órfãos de rota, ver nota de sequenciamento no topo) (depends on T037,
  T038)
- [X] T040 Update `frontend/src/app/app.html`: links de navegação "Unidades"/"Fornecedores" →
  "Partes" (depends on T039)

**Checkpoint**: `mvn test` passando; `/api/parties`, `/api/groups` e `/api/accounts` (com
`partyId`/`fundId`/`groupId`) funcionais e testados por trás da API (via `curl`); tela `/parties`
funcional pelo navegador. `account-form`/`account-list` ainda usam o modelo antigo
(`unitId`/`supplierId`) e continuam funcionando contra o **backend antigo removido** — ou seja,
lançar uma conta pela UI já não funciona neste ponto (o backend não aceita mais `unitId`), até a
User Story 1 corrigir `account-form`. Isso é esperado (ver nota de sequenciamento).

---

## Phase 3: User Story 1 - Lançar entradas e saídas para qualquer Parte (Priority: P1) 🎯 MVP

**Goal**: Permitir lançar, para qualquer Parte cadastrada, tanto uma conta do tipo SAÍDA quanto
uma do tipo ENTRADA, sem a restrição de papel fixo que existia entre `Unit`/`Supplier`.

**Independent Test**: Com ao menos uma Parte cadastrada (Foundational, `/parties`), lançar uma
conta do tipo SAÍDA para ela e, em seguida, uma conta do tipo ENTRADA para a mesma Parte;
confirmar que ambas são aceitas sem conflito; tentar alterar o tipo de uma conta já lançada e
confirmar que o sistema continua recusando.

### Implementation for User Story 1

- [X] T041 [US1] Update `frontend/src/app/shared/models/account.model.ts`: remove os tipos/campos
  `unit`/`supplier` (`Account`) e `unitId`/`supplierId` (`AccountRequest`), adiciona `party: Party`
  (`Account`) e `partyId: number` (`AccountRequest`), importando `Party` de `./party.model`
  (depends on T035, T021, T022)
- [X] T042 [US1] Update componente `account-form`: remove a ramificação por `type` na escolha da
  contraparte (hoje: `RECEIVABLE` mostra seletor de Unidade + toggle de bulk; `PAYABLE` mostra só
  seletor de Fornecedor); passa a exibir, para qualquer `type`, um único seletor "Parte" (sempre
  obrigatório) carregado via `PartyService.findAll()`; remove por completo o antigo toggle
  "Uma unidade específica"/"Todas as unidades" e o método `setBulkMode` (o lançamento em lote é
  reintroduzido de forma generalizada na User Story 5) em
  `frontend/src/app/account/account-form/` (depends on T041, T036)

**Checkpoint**: User Story 1 completa e testável de forma independente (Acceptance Scenarios
1-4 do spec) — `account-form` volta a compilar e a funcionar ponta a ponta.

---

## Phase 4: User Story 2 - Ver o total líquido da tabela de Contas, atualizado dinamicamente (Priority: P1)

**Goal**: Exibir, ao final da tabela de Contas, o valor líquido (ENTRADA − SAÍDA) das contas
atualmente exibidas, recalculado automaticamente a cada filtro/criação/edição/remoção.

**Independent Test**: Com contas de ambos os tipos cadastradas, abrir `/accounts` e conferir que
o total exibido bate com `Σ ENTRADA − Σ SAÍDA` das linhas visíveis; aplicar um filtro e conferir
que o total muda de acordo; lançar mais saídas do que entradas e conferir que o total fica
negativo, sem bloqueio.

### Implementation for User Story 2

- [X] T043 [US2] Add `protected readonly netTotal = computed(() => this.accounts().reduce((total,
  a) => a.type === 'RECEIVABLE' ? total + a.amount : total - a.amount, 0))` em
  `frontend/src/app/account/account-list/account-list.ts` (mesmo padrão de
  `totalRealBalance` em `fund-list.ts`) e uma linha `<tfoot>` exibindo `netTotal() | number:
  '1.2-2'` ao final da tabela em `frontend/src/app/account/account-list/account-list.html`
  (depends on Foundational apenas — independente de US1/US3/US4 no código, mas compartilha o
  mesmo arquivo)

**Checkpoint**: User Story 2 completa e testável de forma independente (Acceptance Scenarios
1-6 do spec, incluindo total negativo sem bloqueio e recálculo em tempo real).

---

## Phase 5: User Story 3 - Filtrar contas por Fundo (Priority: P2)

**Goal**: Permitir filtrar a listagem de Contas por Fundo, combinável por E lógico com os
filtros já existentes.

**Independent Test**: Com contas lançadas em mais de um fundo, selecionar um fundo específico no
novo filtro e confirmar que só as contas daquele fundo aparecem (e que o total da US2 reflete
só elas); combinar com outro filtro (ex.: tipo) e confirmar a combinação.

### Implementation for User Story 3

- [X] T044 [US3] Update `frontend/src/app/shared/models/account.model.ts`: `AccountFilters`
  adiciona `fundId?: number` (edita o mesmo arquivo de T041/T054, mas sem depender deles —
  interface distinta, sem relação funcional; depende só do Foundational)
- [X] T045 [US3] Add `<select id="fundFilter">` (carregado via `FundService.findAll()`, já
  existente desde a feature 004) em
  `frontend/src/app/account/account-list/account-list.html`, propriedade `selectedFundId` e
  inclusão de `filters.fundId` em `load()` em
  `frontend/src/app/account/account-list/account-list.ts` (depends on T044)

**Checkpoint**: User Story 3 completa e testável de forma independente (Acceptance Scenarios
1-3 do spec).

---

## Phase 6: User Story 4 - Ver e filtrar por Parte na tela de Contas (Priority: P2)

**Goal**: Renomear o filtro "Unidade" e a coluna "Contraparte" para "Parte", listando/filtrando
por todas as Partes cadastradas (unificadas), independentemente do tipo da conta.

**Independent Test**: Abrir o filtro antes chamado "Unidade" em `/accounts` e confirmar que
aparece como "Parte", listando todas as Partes cadastradas; selecionar uma e confirmar que a
tabela filtra corretamente contas de qualquer tipo lançadas para ela; confirmar que a coluna
antes chamada "Contraparte" exibe o rótulo "Parte".

### Implementation for User Story 4

- [X] T046 [US4] Update `frontend/src/app/account/account-list/`: troca o import de
  `UnitService`/`Unit` por `PartyService`/`Party`; renomeia o filtro `selectedUnitId`/`unitId`
  para `selectedPartyId`/`partyId` (rótulo do `<label>` e `<option>` "Todas as unidades" →
  "Todas as partes"); renomeia o cabeçalho da coluna "Contraparte" para "Parte"; remove o método
  `counterpartLabel()` e substitui a célula correspondente por `{{ account.party.name }}`
  (depends on T042, T036)
- [X] T047 [P] [US4] Remove `frontend/src/app/unit/`, `frontend/src/app/supplier/`,
  `frontend/src/app/shared/models/unit.model.ts`, `frontend/src/app/shared/models/
  supplier.model.ts`, `frontend/src/app/shared/services/unit.service.ts` e
  `frontend/src/app/shared/services/supplier.service.ts` — a partir daqui nada mais no frontend
  os importa (depends on T042, T046)

**Checkpoint**: User Story 4 completa e testável de forma independente (Acceptance Scenarios
1-3 do spec); nenhum arquivo do frontend referencia mais `Unit`/`Supplier`.

---

## Phase 7: User Story 5 - Organizar Partes em grupos e lançar contas em lote para um grupo (Priority: P3)

**Goal**: Permitir criar Grupos de Partes (composição editada só pela tela do próprio Grupo) e,
ao lançar uma conta, escolher entre uma Parte específica ou um Grupo inteiro.

**Independent Test**: Criar um grupo, adicionar duas Partes a ele pela tela do grupo; lançar uma
conta escolhendo o modo "Grupo" e esse grupo; confirmar que uma conta é criada para cada
integrante, com os mesmos dados; remover uma Parte do grupo e confirmar que ela some de
lançamentos futuros sem afetar contas já criadas; excluir o grupo e confirmar que as contas já
lançadas permanecem intactas; tentar lançar para um grupo vazio e confirmar o bloqueio.

### Implementation for User Story 5

- [X] T048 [P] [US5] Create model `Group`/`GroupRequest` em
  `frontend/src/app/shared/models/group.model.ts`
- [X] T049 [US5] Create `GroupService` (HttpClient: `findAll`, `findById`, `create`, `update`,
  `delete`) em `frontend/src/app/shared/services/group.service.ts` (depends on T048, T027)
- [X] T050 [P] [US5] Create componente `group-list` (tabela: nome, contagem de integrantes;
  ação individual "Editar" por linha, navegando para `/groups/{id}/edit`, e "Remover" por linha
  com `confirm()` — sempre permitido, mesmo com integrantes (FR-016) — mesmo padrão de
  `fund-list`/`account-list`; seleção múltipla/remoção em lote via `list-selection.ts`/
  `bulk-delete.ts`/`bulk-actions-bar`; mensagem de "nenhum grupo cadastrado" quando vazia) em
  `frontend/src/app/group/group-list/` (depends on T049)
- [X] T051 [P] [US5] Create componente `group-form` (campo nome + seletor múltiplo de Partes
  integrantes, carregado via `PartyService.findAll()`; envia `partyIds` no `POST`/`PUT`; suporte
  a modo de edição) em `frontend/src/app/group/group-form/` (depends on T049, T036)
- [X] T052 [US5] Wire rotas `/groups`, `/groups/new`, `/groups/:id/edit` em
  `frontend/src/app/app.routes.ts` (depends on T050, T051)
- [X] T053 [US5] Add link de navegação "Grupos" em `frontend/src/app/app.html` (depends on T052)
- [X] T054 [US5] Update `frontend/src/app/shared/models/account.model.ts`: `AccountBulkRequest`
  adiciona `type: AccountType` (antes implícito) e `groupId: number` (substitui a busca
  implícita por "todas as unidades") (depends on T041)
- [X] T055 [US5] Update componente `account-form`: reintroduz um toggle "Parte específica"/
  "Grupo" (só em modo criação, `!isEditMode`); modo "Grupo" carrega grupos via
  `GroupService.findAll()` e, ao submeter, chama `accountService.createBulk({ ...shared, type,
  groupId })` em vez de `create`/`update`; modo "Parte específica" continua usando `partyId`
  (T042) em `frontend/src/app/account/account-form/` (depends on T042, T049, T054)

**Checkpoint**: Todas as 5 user stories funcionam de forma independente.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validações finais e documentação

- [X] T056 [P] Run roteiro de validação manual de `quickstart.md` de ponta a ponta (API real +
  navegador), incluindo os 15 cenários (qualquer combinação tipo×Parte, total líquido negativo,
  filtro de Fundo, filtro/coluna "Parte", Grupos e lançamento em lote, bloqueios de remoção)
- [X] T057 [P] Update `README.md` com as decisões técnicas desta feature (unificação
  `Unit`+`Supplier` → `Party`, remoção do padrão de FK dupla + CHECK constraint em `Account`,
  `Group` com tabela física `party_group`, total líquido calculado no frontend, migration que
  recria `party`/`account` do zero por não haver dado real a preservar), conforme Fluxo de
  Commits da constituição
- [X] T058 [P] Review mensagens de erro do `GlobalExceptionHandler` para os novos casos desta
  feature (400/404/409 de `parties`/`groups`/`accounts`), garantindo consistência em português
  (Convenções de API REST)
- [X] T059 Revisão da usuária pós-implementação: `frontend/src/app/account/account-list/`
  estava quebrando linha horizontalmente (colunas demais). Remove o filtro "Tipo"
  (Entrada/Saída) da UI — `AccountService`/`GET /api/accounts` continuam aceitando o
  query param `type`, só a UI deixou de expor esse controle; remove a coluna "Tipo de
  lançamento" (Recorrente/Extra); substitui os botões/links de ação (Editar, Remover,
  Registrar pagamento, Alterar) por ícones SVG inline com `title`/`aria-label` (hint), sem
  adicionar biblioteca de ícones nova. Ajusta `spec.md` (FR-009, US2 AC2, US3 texto/AC2) e
  `quickstart.md` (passo 6) para não referenciar mais o filtro "tipo" removido da UI.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem tarefas novas — já entregue pela feature 001
- **Foundational (Phase 2)**: Bloqueia todas as user stories desta feature — unificação completa
  de `Unit`/`Supplier` em `Party`, criação de `Group`, e migração de `Account`
- **User Story 1 (Phase 3)**: Depende apenas do Foundational
- **User Story 2 (Phase 4)**: Depende apenas do Foundational — independente de US1 no código
  (arquivo compartilhado `account-list`, mas seções diferentes)
- **User Story 3 (Phase 5)**: Depende apenas do Foundational — independente de US1/US2 no
  código (mesmo arquivo `account-list`, seções diferentes)
- **User Story 4 (Phase 6)**: Depende de US1 (T042 — `Account.party` precisa existir no modelo
  frontend antes de `account-list` poder exibi-lo)
- **User Story 5 (Phase 7)**: Depende de US1 (T042, T041) e do Foundational (`GroupController`,
  T027)
- **Polish (Phase 8)**: Depende de todas as user stories desejadas estarem completas

### Notas de dependência entre stories

Assim como nas features 003/004, aqui o backend não cresce incrementalmente por story (ver
blurb "Tests" no topo) — toda a Fase 2 já entrega a API completa e testada de `Party`/`Group`/
`Account`, além do cadastro de Parte no frontend. A partir daí, US1-US5 são majoritariamente
ajustes em `account-form`/`account-list`: US1 corrige `account-form` (desbloqueado pela remoção
da restrição tipo×contraparte no backend); US2/US3/US4 adicionam, cada uma, uma seção nova a
`account-list` (total, filtro de fundo, filtro/coluna de parte — US4 depende de US1 ter
adicionado `Account.party` ao modelo); US5 adiciona as telas de Grupo e reintroduz o lançamento
em lote generalizado em `account-form`. Isso reflete a ordem P1 → P1 → P2 → P2 → P3 já definida
no spec.

### Parallel Opportunities

- T001, T002, T005, T008 (Foundational) podem rodar em paralelo entre si no início
- T014, T015, T016, T017, T018 (exceptions) podem rodar em paralelo entre si
- T019, T020, T021, T022 (DTOs) podem rodar em paralelo entre si
- T029, T030 podem rodar em paralelo entre si (depois de T025); T031 depende de ambas
- T032, T033 (testes) podem rodar em paralelo entre si
- T035 (model) e, depois, T037/T038 (componentes) podem rodar em paralelo entre si
- T047 (US4, remoção) é paralelizável a outras tarefas fora de US4 depois de suas dependências
- T048, T050, T051 (US5) podem rodar em paralelo entre si nos pontos indicados

---

## Parallel Example: Foundational

```bash
# Migrations e entidades base em paralelo no início:
Task: "Create migration V10__create_party_table.sql em backend/src/main/resources/db/migration/"
Task: "Create migration V11__create_group_tables.sql em backend/src/main/resources/db/migration/"
Task: "Create entidade JPA Party em backend/src/main/java/com/financas/party/domain/Party.java"

# Exceptions em paralelo:
Task: "Create DuplicatePartyException em backend/src/main/java/com/financas/party/domain/"
Task: "Create PartyHasAccountsException em backend/src/main/java/com/financas/party/domain/"
Task: "Create DuplicateGroupException em backend/src/main/java/com/financas/group/domain/"
Task: "Create EmptyGroupException em backend/src/main/java/com/financas/account/domain/"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 2: Foundational (CRITICAL — bloqueia todas as stories; entrega a API completa
   de `Party`/`Group`/`Account` e o cadastro de Parte no frontend)
2. Complete Phase 3: User Story 1 (qualquer combinação tipo×Parte)
3. **STOP and VALIDATE**: testar US1 isoladamente (cenários 1-4 do spec)

### Incremental Delivery

1. Foundational → API completa de Partes/Grupos/Contas pronta e testada; `/parties` funcional
2. US1 → `account-form` corrigido, qualquer tipo×Parte funcionando (MVP)
3. US2 → total líquido dinâmico na tela de Contas
4. US3 → filtro por Fundo
5. US4 → filtro/coluna "Parte" (renomeação + unificação da listagem); `Unit`/`Supplier`
   removidos do frontend
6. US5 → Grupos e lançamento em lote generalizado

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes
- Cada user story é independentemente completável e testável, exceto pelas dependências de
  dados/modelo descritas acima (US4 depende de US1; US5 depende de US1)
- Commit ao final de cada tarefa ou grupo lógico de tarefas
- Pare em qualquer checkpoint para validar a story isoladamente antes de seguir
