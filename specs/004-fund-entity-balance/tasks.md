---

description: "Task list template for feature implementation"

---

# Tasks: Fundos como Entidade e Visualização de Saldo Real

**Input**: Design documents from `/specs/004-fund-entity-balance/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [quickstart.md](./quickstart.md)

**Tests**: OBRIGATÓRIO — a constituição do projeto (Princípio III) exige tarefas de teste automatizado para toda regra de negócio de uma feature nova. Assim como a feature 003, esta feature é fundamentalmente uma **migração de modelagem** — `Fund` deixa de ser enum e vira entidade, da qual `Account` passa a depender obrigatoriamente. Por isso, o backend completo (entidade, repositório, serviço com todas as regras de negócio, controller e testes Mockito) é entregue de uma vez na Fase 2 (Foundational), junto com os ajustes necessários em `Account` para continuar funcionando; as fases de user story a partir daí são inteiramente **frontend** (a nova tela de fundos, construída incrementalmente).

**Organization**: Tarefas agrupadas por user story (spec.md) para permitir implementação e teste independentes de cada uma, na medida em que a natureza de migração desta feature permite.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1..US3)
- Caminhos de arquivo exatos estão incluídos em cada descrição

## Path Conventions (Web app — ver plan.md)

- Backend: `backend/src/main/java/com/financas/fund/...` (novo) e `.../account/...` (ajustado)
- Frontend: `frontend/src/app/fund/...` (novo) e `.../account/...` (ajustado)
- Ambos os projetos e o banco (`docker-compose.yml`) já existem, criados pela feature 001

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto — não se aplica: `backend/`, `frontend/` e `docker-compose.yml` já existem (feature 001) e são reaproveitados sem alteração.

Nenhuma tarefa nova nesta fase.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Converter `Fund` de enum para entidade de cadastro completa (migrations, entidade, repositório, serviço com todas as regras de negócio, controller, DTOs e testes), e ajustar `Account` para referenciar essa nova entidade (`fund_id`) em vez do valor de enum antigo — infraestrutura que TODAS as user stories desta feature dependem igualmente.

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase estar completa

- [X] T001 [P] Create migration Flyway `backend/src/main/resources/db/migration/V8__create_fund_table.sql` (tabela `fund`: `id`, `name VARCHAR(255) NOT NULL`, `initial_balance NUMERIC(10,2) NOT NULL DEFAULT 0`; índice único sobre `LOWER(TRIM(name))`, mesmo padrão de `unit_identifier_normalized_idx` — ver data-model.md)
- [X] T002 Create migration Flyway `backend/src/main/resources/db/migration/V9__convert_account_fund_to_entity.sql` (`TRUNCATE TABLE account` — banco apenas de desenvolvimento, sem dado a preservar, ver research.md; `ALTER TABLE account DROP COLUMN fund`; `ALTER TABLE account ADD COLUMN fund_id BIGINT NOT NULL REFERENCES fund (id)`; `CREATE INDEX account_fund_id_idx ON account (fund_id)`) (depends on T001)
- [X] T003 [P] Create entidade JPA `Fund` (`id`, `name` obrigatório, `initialBalance` `BigDecimal` obrigatório) em `backend/src/main/java/com/financas/fund/domain/Fund.java`
- [X] T004 [P] Create interface de porta `FundRepository` (`save`, `findById`, `findAll` ordenado por nome, `findByNormalizedName`, `deleteById`, `existsById`) em `backend/src/main/java/com/financas/fund/domain/FundRepository.java` (depends on T003)
- [X] T005 Create `FundJpaRepository` (Spring Data — query JPQL `findByNormalizedName` igual a `UnitJpaRepository.findByNormalizedIdentifier`, e `findAllByOrderByNameAsc`) e `FundRepositoryImpl` em `backend/src/main/java/com/financas/fund/infra/` (depends on T004)
- [X] T006 [P] Create `DuplicateFundException extends ConflictException` em `backend/src/main/java/com/financas/fund/domain/DuplicateFundException.java`
- [X] T007 [P] Create `FundHasAccountsException extends ConflictException` em `backend/src/main/java/com/financas/fund/domain/FundHasAccountsException.java`
- [X] T008 Update entidade `Account`: campo `fund` deixa de ser `@Enumerated(EnumType.STRING) private Fund fund` (`com.financas.account.domain.Fund`) e passa a ser `@ManyToOne(optional = false) @JoinColumn(name = "fund_id", nullable = false) private com.financas.fund.domain.Fund fund` em `backend/src/main/java/com/financas/account/domain/Account.java` (depends on T003)
- [X] T009 [P] Remove o enum `com.financas.account.domain.Fund` (`backend/src/main/java/com/financas/account/domain/Fund.java`), substituído pela entidade criada em T003 (depends on T008)
- [X] T010 Update interface `AccountRepository` — add `List<Account> findByFundId(Long fundId)` e `boolean existsByFundId(Long fundId)` em `backend/src/main/java/com/financas/account/domain/AccountRepository.java` (depends on T008)
- [X] T011 Update `AccountJpaRepository` (deriva `findByFundId`/`existsByFundId` via `account.fund.id`) e `AccountRepositoryImpl` (implementa os dois novos métodos) em `backend/src/main/java/com/financas/account/infra/` (depends on T010)
- [X] T012 Implement `FundService` — `create(name, initialBalance)` validando nome único (`findByNormalizedName`, FR-001/FR-002); `findAll()` ordenado por nome; `findById`; `update(id, name, initialBalance)` validando nome único (ignorando o próprio id, FR-003); `delete(id)` bloqueando via `accountRepository.existsByFundId(id)` (`FundHasAccountsException`, FR-004/FR-005); `calculateRealBalance(Fund fund)` — soma em memória de `accountRepository.findByFundId(fund.getId())`: `initialBalance` + valores com `type = RECEIVABLE` e `paymentDate != null` − valores com `type = PAYABLE` e `paymentDate != null`, sem nenhuma validação de saldo mínimo (FR-008/FR-010/FR-011/FR-012) — em `backend/src/main/java/com/financas/fund/domain/FundService.java` (depends on T005, T006, T007, T011)
- [X] T013 [P] Create DTOs `FundRequest` (`name` `@NotBlank`, `initialBalance` `@NotNull`, sem restrição de sinal) e `FundResponse` (`id`, `name`, `initialBalance`, `realBalance`; factory estático `from(Fund fund, BigDecimal realBalance)`) em `backend/src/main/java/com/financas/fund/api/` (depends on T003)
- [X] T014 Implement `FundController` — `GET /api/funds`, `GET /api/funds/{id}`, `POST /api/funds`, `PUT /api/funds/{id}`, `DELETE /api/funds/{id}`, montando cada `FundResponse` via `FundResponse.from(fund, service.calculateRealBalance(fund))` — em `backend/src/main/java/com/financas/fund/api/FundController.java` (depends on T012, T013)
- [X] T015 [P] Update DTOs `AccountRequest`/`AccountBulkRequest`: campo `fund` (valor de enum) → `fundId` (`Long`, `@NotNull`) em `backend/src/main/java/com/financas/account/api/` (depends on T009)
- [X] T016 [P] Update `AccountResponse`: campo `fund` (`Fund`) → `fund` (`FundResponse`); `from(Account account, FundResponse fundResponse)` em vez de `from(Account account)` puro em `backend/src/main/java/com/financas/account/api/AccountResponse.java` (depends on T013, T008)
- [X] T017 Update `AccountService`: injeta `FundRepository`; `create`/`update`/`createForAllUnits` passam a receber `Long fundId` (em vez de `Fund fund`) e resolvem a entidade via novo `findFundOrThrow(fundId)` (mesmo padrão de `findUnitOrThrow`/`findSupplierOrThrow`, lança `NotFoundException` se não existir) em `backend/src/main/java/com/financas/account/domain/AccountService.java` (depends on T008, T004)
- [X] T018 Update `AccountController`: injeta `FundService`; monta o `FundResponse` embutido a cada `AccountResponse.from(account, FundResponse.from(account.getFund(), fundService.calculateRealBalance(account.getFund())))`, e passa `request.fundId()` em vez de `request.fund()` para `AccountService` em `backend/src/main/java/com/financas/account/api/AccountController.java` (depends on T014, T015, T016, T017)
- [X] T019 [P] Create testes unitários `FundServiceTest` (Mockito): `create`/`update` rejeitam nome duplicado (case-insensitive, espaços nas extremidades) e aceitam nomes distintos (FR-001/FR-002/FR-003); `delete` bloqueado (`FundHasAccountsException`) quando `AccountRepository.existsByFundId` retorna `true`, permitido quando `false` (FR-004/FR-005); `calculateRealBalance` soma `initialBalance` + recebimentos pagos − pagamentos pagos, ignorando lançamentos sem `paymentDate` (FR-008/FR-011); `calculateRealBalance` não lança exceção nem impede resultado negativo (FR-012) — em `backend/src/test/java/com/financas/fund/domain/FundServiceTest.java` (depends on T012)
- [X] T020 [P] Update testes unitários existentes `AccountServiceTest`: trocar `fund` (enum) por `fundId` (`Long`) em todos os casos já existentes, mockando `FundRepository.findById`/`findByNormalizedName` conforme necessário; adicionar caso cobrindo `fundId` inexistente lançando `NotFoundException` (FR-006) em `backend/src/test/java/com/financas/account/domain/AccountServiceTest.java` (depends on T017)
- [X] T021 [P] Create model `Fund`/`FundRequest` em `frontend/src/app/shared/models/fund.model.ts`
- [X] T022 Create `FundService` (HttpClient: `findAll`, `findById`, `create`, `update`, `delete`) em `frontend/src/app/shared/services/fund.service.ts` (depends on T021, T014)
- [X] T023 [P] Update `frontend/src/app/shared/models/account.model.ts`: remove o tipo `Fund` local e `FUND_LABELS` (passam a vir de `fund.model.ts`); `Account.fund` passa a ser o objeto `Fund` embutido (importado de `./fund.model`); `AccountRequest`/`AccountBulkRequest.fund` → `fundId: number` (depends on T021)
- [X] T024 Update componente `account-form`: `fundOptions` estático (derivado de `FUND_LABELS`) → lista carregada via `FundService.findAll()` (mesmo padrão de `units`/`suppliers` já existente no componente); controle do formulário `fund` → `fundId` em `frontend/src/app/account/account-form/` (depends on T022, T023)
- [X] T025 [P] Update `account-list.html`: coluna "Fundo" — `fundLabels[account.fund]` → `account.fund.name`; remove `fundLabels`/import de `FUND_LABELS` de `account-list.ts` em `frontend/src/app/account/account-list/` (depends on T023)

**Checkpoint**: `mvn test` passando; `/api/funds` e `/api/accounts` (com `fundId`) funcionais e testados por trás da API (via `curl`). Pronto para as user stories, inteiramente frontend a partir daqui.

---

## Phase 3: User Story 1 - Visualizar o saldo real de cada fundo (Priority: P1) 🎯 MVP

**Goal**: Exibir, numa única tela, o saldo real (e o total somado) de cada fundo já cadastrado via API.

**Independent Test**: Com ao menos um fundo cadastrado via API (`POST /api/funds`) e lançamentos pagos/em aberto vinculados a ele, acessar a tela de fundos e confirmar que o saldo real exibido reflete só os lançamentos já efetivados, junto com o total somado de todos os fundos.

### Implementation for User Story 1

- [X] T026 [US1] Create componente `fund-list` (tabela: nome, saldo inicial, saldo real de cada fundo + linha de total somado; mensagem de "nenhum fundo cadastrado" quando vazia — sem ações de criar/editar/remover ainda, adicionadas nas próximas stories) em `frontend/src/app/fund/fund-list/` (depends on T022)
- [X] T027 [US1] Wire rota `/funds` em `frontend/src/app/app.routes.ts` (depends on T026)
- [X] T028 [US1] Add link de navegação "Fundos" em `frontend/src/app/app.html` (depends on T027)

**Checkpoint**: User Story 1 completa e testável de forma independente (Acceptance Scenarios 1-3 do spec).

---

## Phase 4: User Story 2 - Cadastrar um novo fundo (Priority: P2)

**Goal**: Permitir cadastrar um novo fundo (nome + saldo inicial) a partir da própria interface.

**Independent Test**: Cadastrar um fundo com um nome não utilizado e confirmar que aparece na listagem com saldo real igual ao saldo inicial informado; tentar cadastrar com um nome já usado por outro fundo e confirmar a rejeição.

### Implementation for User Story 2

- [X] T029 [US2] Create componente `fund-form` (campos nome e saldo inicial; validação de nome obrigatório; suporte a modo de edição já embutido — pré-preenchimento via `GET /api/funds/{id}` e submissão via `PUT` quando acessado com um id de rota, mesmo padrão de `supplier-form`/`unit-form` — mas só acessível via `/funds/new` nesta fase) em `frontend/src/app/fund/fund-form/` (depends on T022)
- [X] T030 [US2] Add botão "Novo fundo" em `fund-list`, linkando para `/funds/new` em `frontend/src/app/fund/fund-list/` (depends on T026, T029)
- [X] T031 [US2] Wire rota `/funds/new` em `frontend/src/app/app.routes.ts` (depends on T029)

**Checkpoint**: User Story 2 completa e testável de forma independente (Acceptance Scenarios 1-2 do spec).

---

## Phase 5: User Story 3 - Editar ou remover um fundo existente (Priority: P3)

**Goal**: Permitir corrigir o nome/saldo inicial de um fundo já cadastrado e remover um fundo sem lançamentos vinculados.

**Independent Test**: Editar o nome e/ou saldo inicial de um fundo existente e confirmar que a listagem reflete a mudança; remover um fundo sem lançamentos vinculados e confirmar que some da listagem; tentar remover um fundo com lançamentos vinculados e confirmar o bloqueio, com mensagem explicando o vínculo.

### Implementation for User Story 3

- [X] T032 [US3] Add ação "editar" (navega ao `fund-form` em modo edição) e "remover" (com diálogo de confirmação, exibindo a mensagem de erro 409 do backend quando houver conta vinculada) nas linhas de `fund-list` em `frontend/src/app/fund/fund-list/` (depends on T026, T029)
- [X] T033 [US3] Apply seleção múltipla (`list-selection.ts`) e `bulk-actions-bar` + `bulk-delete.ts` (já existentes, sem alteração de contrato) a `fund-list`, por consistência com `unit-list`/`supplier-list`/`account-list`, em `frontend/src/app/fund/fund-list/` (depends on T026)
- [X] T034 [US3] Wire rota `/funds/:id/edit` em `frontend/src/app/app.routes.ts` (depends on T029, T032)

**Checkpoint**: Todas as 3 user stories funcionam de forma independente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validações finais e documentação

- [X] T035 [P] Run roteiro de validação manual de `quickstart.md` de ponta a ponta (API real + navegador), incluindo a confirmação de que `GET /api/funds` retorna lista vazia num ambiente novo (FR-009, cenário 11)
- [X] T036 [P] Update `README.md` com as decisões técnicas desta feature (`Fund` como entidade em vez de enum, saldo inicial editável, fórmula do saldo real, ausência de bloqueio por saldo negativo, migration que trunca `account` por não haver dado real a preservar), conforme Fluxo de Commits da constituição
- [X] T037 [P] Review mensagens de erro do `GlobalExceptionHandler` para os novos casos desta feature (400/404/409 de `funds`), garantindo consistência em português (Convenções de API REST)

---

## Phase 7: Destaque de linha selecionada (impacto cruzado com a feature 002, sessão 2026-08-02)

**Purpose**: A feature 002 generalizou o destaque visual de seleção (FR-024 de `specs/002-receivable-charges/spec.md`) para toda listagem que reaproveita o trio de seleção múltipla — `fund-list` precisa ganhá-lo.

- [X] T038 Add `[class.table-active]="selection.isSelected(fund)"` ao `<tr>` de `frontend/src/app/fund/fund-list/fund-list.html` (depends on T033)

**Checkpoint**: `fund-list` destaca visualmente linhas selecionadas, mesmo comportamento hoje já presente em `account-list`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem tarefas novas — já entregue pela feature 001
- **Foundational (Phase 2)**: Bloqueia todas as user stories desta feature — conversão completa de `Fund` (enum → entidade) e ajuste de `Account`
- **User Story 1 (Phase 3)**: Depende apenas do Foundational
- **User Story 2 (Phase 4)**: Depende do Foundational; reaproveita `fund-list` (T026) de US1 para o botão "Novo fundo"
- **User Story 3 (Phase 5)**: Depende do Foundational, de US1 (T026, `fund-list`) e de US2 (T029, `fund-form`, reaproveitado em modo de edição)
- **Polish (Phase 6)**: Depende de todas as user stories desejadas estarem completas

### Notas de dependência entre stories

Assim como na feature 003, aqui o backend não cresce incrementalmente por story (ver blurb "Tests" no topo) — toda a Fase 2 já entrega a API completa e testada de `Fund`, e ajusta `Account` para usá-la. A partir daí, US1-US3 são inteiramente tarefas de frontend, construindo progressivamente a mesma tela (`fund-list`): US1 entrega a visualização somente leitura, US2 adiciona a criação, US3 adiciona edição/remoção — refletindo a ordem P1 → P2 → P3 já definida no spec.

### Parallel Opportunities

- T001, T003, T006, T007 (Foundational) podem rodar em paralelo entre si no início
- T009 pode rodar em paralelo a outras tarefas depois de T008 concluída
- T013, T015, T016 podem rodar em paralelo entre si (arquivos DTO distintos)
- T019, T020 (testes) podem rodar em paralelo entre si
- T021, T023 (models frontend) podem rodar em paralelo entre si depois de T021
- T024, T025 podem rodar em paralelo entre si depois de T023
- T035, T036, T037 (Polish) podem rodar em paralelo entre si

---

## Parallel Example: Foundational

```bash
# Migration, entidade Fund e exceptions em paralelo no início:
Task: "Create migration V8__create_fund_table.sql em backend/src/main/resources/db/migration/"
Task: "Create entidade JPA Fund em backend/src/main/java/com/financas/fund/domain/Fund.java"
Task: "Create DuplicateFundException em backend/src/main/java/com/financas/fund/domain/"
Task: "Create FundHasAccountsException em backend/src/main/java/com/financas/fund/domain/"

# DTOs em paralelo:
Task: "Create FundRequest/FundResponse em backend/src/main/java/com/financas/fund/api/"
Task: "Update AccountRequest/AccountBulkRequest em backend/src/main/java/com/financas/account/api/"
Task: "Update AccountResponse em backend/src/main/java/com/financas/account/api/AccountResponse.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 2: Foundational (CRITICAL — bloqueia todas as stories; entrega a API completa de `Fund` e o ajuste de `Account`)
2. Complete Phase 3: User Story 1 (visualização de saldo real)
3. **STOP and VALIDATE**: testar US1 isoladamente (cenários 1-3 do spec), cadastrando fundos via `curl`/API já que a UI de cadastro (US2) ainda não existe nesta etapa

### Incremental Delivery

1. Foundational → API completa de fundos pronta e testada; `Account` usando `fundId`
2. US1 → visualização de saldo real funcionando (MVP mínimo de UI, fundos cadastrados via API)
3. US2 → cadastro de fundos pela interface, reaproveitando US1
4. US3 → edição e remoção de fundos

---

## Notes

- [P] = arquivos diferentes, sem conflito de edição simultânea
- Cada user story é independentemente completável e testável, exceto pelas dependências de dados descritas acima
- Commit ao final de cada tarefa ou grupo lógico de tarefas
- Pare em qualquer checkpoint para validar a story isoladamente antes de seguir
