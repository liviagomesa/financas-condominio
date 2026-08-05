---

description: "Task list template for feature implementation"

---

# Tasks: Geração Automática de Contas Recorrentes

**Input**: Design documents from `/specs/009-recurring-charges/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [quickstart.md](./quickstart.md)

**Tests**: A constituição do projeto (Princípio III) exige cobertura de teste automatizado para toda regra de negócio nova — diferente do padrão genérico do Spec Kit, que trata testes como opcionais. Por isso `RecurringChargeServiceTest` e `RecurringChargeGenerationServiceTest` (novos) e os casos novos em `PartyServiceTest`/`FundServiceTest` estão incluídos como tarefas obrigatórias, não opcionais. O frontend não recebe teste unitário dedicado a componentes novos, seguindo o precedente já registrado no `research.md` das features 006/007/008 (nenhum componente Angular tem hoje teste próprio no projeto) — validado via o roteiro de `quickstart.md` (Playwright/navegador, Princípio III).

**Organization**: Tarefas agrupadas por user story (spec.md) para permitir implementação e teste independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1–US5)
- Caminhos de arquivo exatos estão incluídos em cada descrição

## Path Conventions (Web app — ver plan.md)

- Backend: `backend/src/main/java/com/financas/recurringcharge/` (pacote novo, `api/`/`domain/`/`infra/`), `backend/src/main/java/com/financas/account/`, `backend/src/main/java/com/financas/party/domain/`, `backend/src/main/java/com/financas/fund/domain/`, `backend/src/main/resources/db/migration/`, `backend/src/test/java/com/financas/`
- Frontend: `frontend/src/app/recurring-charge/` (pacote novo, `recurring-charge-list/`/`recurring-charge-form/`), `frontend/src/app/shared/models/`, `frontend/src/app/shared/services/`, `frontend/src/app/app.routes.ts`, `frontend/src/app/app.html`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto — não se aplica: backend e frontend já existem (feature 001) e nenhuma dependência nova é necessária (`@Scheduled` já faz parte do `spring-boot-starter` já instalado — ver plan.md, Technical Context).

Nenhuma tarefa nova nesta fase.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Cria a entidade `RecurringCharge` e sua infraestrutura de persistência (usada por todas as 5 user stories), e implementa o bloqueio de remoção de `Party`/`Fund` com cobrança recorrente ativa (FR-014) — regra transversal, sem Acceptance Scenarios próprios no spec, testável assim que a entidade/repositório existem, sem depender de nenhuma user story específica.

**⚠️ CRITICAL**: Nenhuma implementação de user story deve começar antes desta fase estar completa.

- [X] T001 [P] Add migration `backend/src/main/resources/db/migration/V15__create_recurring_charge_table.sql`: tabela `recurring_charge` (`id` IDENTITY, `type VARCHAR(20) NOT NULL`, `amount NUMERIC(10,2) NOT NULL CHECK (amount >= 0)`, `due_day INTEGER NOT NULL CHECK (due_day BETWEEN 1 AND 31)`, `description VARCHAR(255) NOT NULL`, `fund_id BIGINT NOT NULL REFERENCES fund (id)`, `party_id BIGINT NOT NULL REFERENCES party (id)`, `observations TEXT`, `deactivated_at DATE`, `last_generation_failed BOOLEAN NOT NULL DEFAULT FALSE`) + índices em `fund_id`, `party_id`, `deactivated_at` (ver data-model.md)
- [X] T002 [P] Create `RecurringCharge` entity em `backend/src/main/java/com/financas/recurringcharge/domain/RecurringCharge.java`: campos `id`, `type` (`com.financas.account.domain.AccountType`, `@Enumerated(EnumType.STRING)`), `amount` (`BigDecimal`), `dueDay` (`Integer`), `description`, `fund` (`@ManyToOne` obrigatório), `party` (`@ManyToOne` obrigatório), `observations` (nullable), `deactivatedAt` (`LocalDate` nullable), `lastGenerationFailed` (`boolean`, default `false`); método derivado `isActive()` retornando `deactivatedAt == null` — **sem** campo `boolean active` persistido (Princípio IV, ver research.md)
- [X] T003 [P] Create interface `RecurringChargeRepository` em `backend/src/main/java/com/financas/recurringcharge/domain/RecurringChargeRepository.java`: `save`, `findById`, `findAll`, `existsByPartyIdAndDeactivatedAtIsNull(Long partyId)`, `existsByFundIdAndDeactivatedAtIsNull(Long fundId)` (depends on T002)
- [X] T004 [P] Create `RecurringChargeJpaRepository` em `backend/src/main/java/com/financas/recurringcharge/infra/RecurringChargeJpaRepository.java` (`extends JpaRepository<RecurringCharge, Long>`) com as duas queries derivadas de T003 (depends on T002)
- [X] T005 Create `RecurringChargeRepositoryImpl` em `backend/src/main/java/com/financas/recurringcharge/infra/RecurringChargeRepositoryImpl.java`, implementando `RecurringChargeRepository` via `RecurringChargeJpaRepository` (mesmo padrão de `AccountRepositoryImpl`) (depends on T003, T004)
- [X] T006 [P] Create `RecurringChargeTypeChangeNotAllowedException extends BadRequestException` em `backend/src/main/java/com/financas/recurringcharge/domain/RecurringChargeTypeChangeNotAllowedException.java`, mensagem `"Não é possível alterar o tipo de uma cobrança recorrente já criada."` (mesmo padrão de `AccountTypeChangeNotAllowedException`)
- [X] T007 [P] Create `InvalidRecurringChargeAmountException extends BadRequestException` em `backend/src/main/java/com/financas/recurringcharge/domain/InvalidRecurringChargeAmountException.java`, mensagem `"O valor da cobrança recorrente não pode ser negativo."` (mesmo padrão de `InvalidAccountAmountException`)
- [X] T008 [P] Create `EmptyGroupException extends ConflictException` em `backend/src/main/java/com/financas/recurringcharge/domain/EmptyGroupException.java`, mesma mensagem de `com.financas.account.domain.EmptyGroupException` — cópia dedicada, não reaproveitada entre pacotes (ver research.md)
- [X] T009 [P] Create `PartyHasActiveRecurringChargesException extends ConflictException` em `backend/src/main/java/com/financas/party/domain/PartyHasActiveRecurringChargesException.java`, mensagem `"Esta parte possui cobranças recorrentes ativas e não pode ser removida."`
- [X] T010 [P] Create `FundHasActiveRecurringChargesException extends ConflictException` em `backend/src/main/java/com/financas/fund/domain/FundHasActiveRecurringChargesException.java`, mensagem `"Este fundo possui cobranças recorrentes ativas e não pode ser removido."`
- [X] T011 Update `backend/src/main/java/com/financas/party/domain/PartyService.java`: injeta `RecurringChargeRepository`; em `delete(id)`, antes do `deleteById` já existente, checa `recurringChargeRepository.existsByPartyIdAndDeactivatedAtIsNull(id)` e lança `PartyHasActiveRecurringChargesException` se verdadeiro (FR-014) (depends on T003, T005, T009)
- [X] T012 Update `backend/src/main/java/com/financas/fund/domain/FundService.java`: mesma checagem para `Fund`, lançando `FundHasActiveRecurringChargesException` (depends on T003, T005, T010)
- [X] T013 [P] Add caso novo a `backend/src/test/java/com/financas/party/domain/PartyServiceTest.java`: `delete` lança `PartyHasActiveRecurringChargesException` quando `recurringChargeRepository.existsByPartyIdAndDeactivatedAtIsNull` retorna `true`, e não chama `repository.deleteById` nesse caso (mock de `RecurringChargeRepository` adicionado ao `@BeforeEach`) (depends on T011)
- [X] T014 [P] Add caso novo a `backend/src/test/java/com/financas/fund/domain/FundServiceTest.java`: mesmo caso para `FundHasActiveRecurringChargesException` (depends on T012)

**Checkpoint**: Entidade `RecurringCharge` e sua persistência prontas; bloqueio de remoção de `Party`/`Fund` funcionando — as cinco user stories podem ser implementadas.

---

## Phase 3: User Story 1 - Cadastrar uma cobrança/pagamento recorrente (Priority: P1) 🎯 MVP

**Goal**: A usuária cadastra uma cobrança recorrente para uma contraparte específica ou, em lote, para todos os integrantes de um grupo.

**Independent Test**: Via `POST /api/recurring-charges` (contraparte específica) e `POST /api/recurring-charges/bulk` (grupo), confirmar que a(s) linha(s) são criadas com os dados informados, incluindo valor `R$0,00` aceito e grupo vazio rejeitado — verificável por `RecurringChargeServiceTest` e/ou `GET /api/recurring-charges` (endpoint só chega em US5, então a verificação nesta fase é via teste automatizado/chamada HTTP direta, não pela tela de listagem).

### Implementation for User Story 1

- [X] T015 [P] [US1] Create `RecurringChargeRequest` em `backend/src/main/java/com/financas/recurringcharge/api/RecurringChargeRequest.java` (record): `type` (`@NotNull`), `amount` (`@NotNull @PositiveOrZero`), `dueDay` (`@NotNull @Min(1) @Max(31)`), `description` (`@NotBlank`), `fundId` (`@NotNull`), `partyId` (`@NotNull`), `observations` (livre) — mesmo padrão de mensagens em português de `AccountRequest`
- [X] T016 [P] [US1] Create `RecurringChargeBulkRequest` em `backend/src/main/java/com/financas/recurringcharge/api/RecurringChargeBulkRequest.java`: igual a T015, trocando `partyId` por `groupId` (`@NotNull`)
- [X] T017 [P] [US1] Create `RecurringChargeResponse` em `backend/src/main/java/com/financas/recurringcharge/api/RecurringChargeResponse.java` (record): `id`, `type`, `amount`, `dueDay`, `description`, `fund` (`FundResponse`), `party` (`PartyResponse`), `observations`, `lastGenerationFailed`; factory estático `from(RecurringCharge, FundResponse)` (mesmo padrão de `AccountResponse.from`, embutindo `PartyResponse.from(charge.getParty())` internamente)
- [X] T018 [US1] Create `RecurringChargeService` em `backend/src/main/java/com/financas/recurringcharge/domain/RecurringChargeService.java`: construtor com `RecurringChargeRepository`, `PartyRepository`, `GroupRepository`, `FundRepository`; método `create(type, amount, dueDay, description, fundId, partyId, observations)` (valida valor não-negativo via `validateNonNegativeAmount`/`InvalidRecurringChargeAmountException`, resolve `fund`/`party`, salva); método `createForGroup(type, amount, dueDay, description, fundId, groupId, observations)` `@Transactional`, rejeita grupo vazio (`EmptyGroupException`), salva uma linha por integrante (mesmo padrão de `AccountService.createForGroup`) (depends on T003, T005–T008)
- [X] T019 [US1] Create `RecurringChargeController` em `backend/src/main/java/com/financas/recurringcharge/api/RecurringChargeController.java`: `POST /api/recurring-charges` (`201`, `RecurringChargeResponse`) e `POST /api/recurring-charges/bulk` (`201`, `List<RecurringChargeResponse>`), injetando também `FundService` para calcular o `FundResponse` embutido (mesmo padrão de `AccountController.toResponse`) (depends on T015–T018)
- [X] T020 [P] [US1] Create `backend/src/test/java/com/financas/recurringcharge/domain/RecurringChargeServiceTest.java`: `create` com contraparte específica (linha criada com os dados informados); `createForGroup` com grupo de vários integrantes (uma linha por integrante); `createForGroup` com grupo vazio (`EmptyGroupException`, nenhuma linha salva); `create`/`createForGroup` com `amount = 0` (aceito); `create`/`createForGroup` com `amount < 0` (`InvalidRecurringChargeAmountException`) (depends on T018)
- [X] T021 [P] [US1] Create `frontend/src/app/shared/models/recurring-charge.model.ts`: interfaces `RecurringCharge` (`id`, `type`, `amount`, `dueDay`, `description`, `fund: Fund`, `party: Party`, `observations`, `lastGenerationFailed`), `RecurringChargeRequest`, `RecurringChargeBulkRequest` (mesmo formato de `account.model.ts`)
- [X] T022 [US1] Create `frontend/src/app/shared/services/recurring-charge.service.ts`: `create(request)` (`POST /recurring-charges`) e `createBulk(request)` (`POST /recurring-charges/bulk`) — demais métodos (`findAll`, `findById`, `update`, `delete`) adicionados pelas user stories seguintes que precisam deles (depends on T021)
- [X] T023 [US1] Create `frontend/src/app/recurring-charge/recurring-charge-form/recurring-charge-form.ts`: `bulkMode` signal alternando `partyId`/`groupId` (mesmo padrão de `AccountForm.setBulkMode`), campos `type`/`amount`/`dueDay`/`description`/`fundId`/`observations`, `submit()` chamando `create`/`createBulk` conforme `bulkMode()`, navegando para `/recurring-charges` ao final (rota só existe a partir de US5 — comportamento final, ver Notes); `@Component` referenciando `styleUrl: './recurring-charge-form.scss'` (arquivo criado mínimo/vazio, mesmo padrão Bootstrap-first das demais telas) (depends on T022)
- [X] T024 [US1] Create `frontend/src/app/recurring-charge/recurring-charge-form/recurring-charge-form.html`: formulário reativo com toggle "Contraparte específica"/"Grupo" (mesmo padrão de `account-form.html`), campos do form de T023 (depends on T023)
- [X] T025 [US1] Update `frontend/src/app/app.routes.ts`: add rota `{ path: 'recurring-charges/new', component: RecurringChargeForm }` (depends on T023)

**Checkpoint**: User Story 1 completa e testável de forma independente (Acceptance Scenarios 1-4 do spec) — cadastro via API/testes automatizados; a tela de listagem para verificação visual chega em US5.

---

## Phase 4: User Story 2 - Geração automática mensal das contas (Priority: P1) 🎯 MVP

**Goal**: Todo dia 25 às 6h de Brasília (e na inicialização, como recuperação), o sistema gera automaticamente uma conta por cobrança recorrente ativa, isolando falhas por cobrança.

**Independent Test**: Chamando `RecurringChargeGenerationService.generatePendingAccounts()` diretamente (via teste), confirmar que contas são criadas com vencimento no mês seguinte, referenciando o molde; reexecutar sem duplicar; uma falha isolada não impede as demais.

### Implementation for User Story 2

- [X] T026 [P] [US2] Add migration `backend/src/main/resources/db/migration/V16__add_recurring_charge_id_to_account.sql`: `ALTER TABLE account ADD COLUMN recurring_charge_id BIGINT REFERENCES recurring_charge (id);` + `CREATE INDEX account_recurring_charge_id_idx ON account (recurring_charge_id);`
- [X] T027 [P] [US2] Update `backend/src/main/java/com/financas/account/domain/Account.java`: add campo `recurringCharge` (`@ManyToOne`, `@JoinColumn(name = "recurring_charge_id", nullable = true)`, tipo `com.financas.recurringcharge.domain.RecurringCharge`) com getter/setter — **sem** novo parâmetro de construtor, só setter (ver research.md) (depends on T002)
- [X] T028 [P] [US2] Update `backend/src/main/java/com/financas/account/domain/AccountRepository.java`: add `boolean existsByRecurringChargeIdAndDueDateBetween(Long recurringChargeId, LocalDate start, LocalDate end);`
- [X] T029 [US2] Update `backend/src/main/java/com/financas/account/infra/AccountJpaRepository.java`: add a query derivada correspondente a T028 (depends on T026–T028)
- [X] T030 [US2] Update `backend/src/main/java/com/financas/account/infra/AccountRepositoryImpl.java`: implementa o método de T028 delegando à query de T029 (depends on T028, T029)
- [X] T031 [US2] Update `backend/src/main/java/com/financas/FinancasBackendApplication.java`: add `@EnableScheduling`
- [X] T032 [US2] Create `RecurringChargeGenerationService` em `backend/src/main/java/com/financas/recurringcharge/domain/RecurringChargeGenerationService.java`: construtor com `RecurringChargeRepository`/`AccountRepository`; `resolveMostRecentDueTargetMonth(LocalDate reference)` (privado — `reference.getDayOfMonth() >= 25 ? YearMonth.from(reference).plusMonths(1) : YearMonth.from(reference)`); `generatePendingAccounts()` público, anotado `@Scheduled(cron = "0 0 6 25 * *", zone = "America/Sao_Paulo")` **e** `@EventListener(ApplicationReadyEvent.class)` — calcula o mês-alvo a partir de `LocalDate.now()`, itera `repository.findAll().stream().filter(RecurringCharge::isActive)`, para cada uma checa `accountRepository.existsByRecurringChargeIdAndDueDateBetween(...)` (se existir, limpa `lastGenerationFailed` se necessário e pula) e senão chama `generateOne`; `generateOne(charge, targetMonth)` privado — resolve `dueDate` (`Math.min(charge.getDueDay(), targetMonth.lengthOfMonth())`), constrói `new Account(...)`, `setRecurringCharge(charge)`, salva, limpa `lastGenerationFailed`; todo o método/laço **sem** `@Transactional` (isolamento por cobrança, FR-016 — ver research.md/plan.md Complexity Tracking); falha de `generateOne` capturada em `try/catch(RuntimeException)` dentro do laço, loga em inglês (`LoggerFactory`) e marca `charge.setLastGenerationFailed(true)` sem interromper as demais (depends on T003, T005, T027–T030)
- [X] T033 [P] [US2] Create `backend/src/test/java/com/financas/recurringcharge/domain/RecurringChargeGenerationServiceTest.java`: geração básica (múltiplas cobranças ativas → contas com vencimento no mês seguinte, cada uma com `recurringCharge` referenciando o molde); idempotência (segunda chamada não duplica); ajuste de dia de vencimento 31 em mês curto/fevereiro; nenhuma cobrança ativa (sem erro, nenhuma conta criada); isolamento (uma cobrança lança exceção ao salvar — mock de `accountRepository.save` — as demais ainda são geradas); `lastGenerationFailed` definida `true` no catch e `false` no sucesso e quando a conta já existia; `resolveMostRecentDueTargetMonth` para datas antes/no/depois do dia 25 (depends on T032)

**Checkpoint**: User Story 2 completa e testável de forma independente (Acceptance Scenarios 1-5 do spec) — geração automática funcionando via teste direto de `generatePendingAccounts()`; o disparo real por `@Scheduled`/`ApplicationReadyEvent` só é observável rodando a aplicação de verdade (ver quickstart.md).

---

## Phase 5: User Story 3 - Reajustar uma cobrança recorrente sem afetar o histórico (Priority: P2)

**Goal**: Editar uma cobrança recorrente cria uma nova versão ativa e inativa a anterior, sem alterar contas já geradas.

**Independent Test**: Editar o valor de uma cobrança recorrente que já gerou uma conta; confirmar que a conta antiga mantém o valor original, a linha antiga fica inativa (`deactivatedAt` preenchido) mas preservada, e uma nova linha ativa surge com o valor novo.

### Implementation for User Story 3

- [X] T034 [US3] Update `backend/src/main/java/com/financas/recurringcharge/domain/RecurringChargeService.java`: add `findById(id)` (sem filtro de `isActive()`); add `update(id, type, amount, dueDay, description, fundId, partyId, observations)` `@Transactional` — valida `type` inalterado (`RecurringChargeTypeChangeNotAllowedException`), cria nova linha ativa com os valores atualizados (reaproveitando `validateNonNegativeAmount`/`InvalidRecurringChargeAmountException` e a resolução de `fund`/`party` de `create`), marca a linha antiga `deactivatedAt = LocalDate.now()`, retorna a linha nova (FR-008) (depends on T018)
- [X] T035 [US3] Update `backend/src/main/java/com/financas/recurringcharge/api/RecurringChargeController.java`: add `GET /api/recurring-charges/{id}` (`200`, `RecurringChargeResponse`) e `PUT /api/recurring-charges/{id}` (`200`, `RecurringChargeResponse` da linha **nova**) (depends on T034)
- [X] T036 [P] [US3] Add casos a `backend/src/test/java/com/financas/recurringcharge/domain/RecurringChargeServiceTest.java`: `update` cria uma linha nova ativa com os valores atualizados e marca a antiga `deactivatedAt` não-nulo (`isActive() == false`); `update` com `type` diferente do persistido lança `RecurringChargeTypeChangeNotAllowedException`; `update` com `amount < 0` lança `InvalidRecurringChargeAmountException`, sem alterar a linha antiga; `findById` retorna a linha independente de estar ativa ou não; **e um caso de ponta a ponta usando `RecurringChargeGenerationService` (T032, já disponível a partir da Phase 4): gerar uma `Account` a partir de uma cobrança recorrente ativa, editar essa cobrança via `update()`, e confirmar que a `Account` já gerada (buscada novamente) permanece com `amount`/`description` originais (SC-003)** (depends on T032, T034)
- [X] T037 [US3] Update `frontend/src/app/shared/services/recurring-charge.service.ts`: add `findById(id)` (`GET /recurring-charges/{id}`) e `update(id, request)` (`PUT /recurring-charges/{id}`) (depends on T022, T035)
- [X] T038 [US3] Update `frontend/src/app/recurring-charge/recurring-charge-form/recurring-charge-form.ts`: modo edição — lê `:id` da rota, carrega via `findById`, preenche o formulário, desabilita `type` (mesmo padrão de `AccountForm`), `submit()` chama `update` em vez de `create` quando em modo edição (depends on T023, T037)
- [X] T039 [US3] Update `frontend/src/app/app.routes.ts`: add rota `{ path: 'recurring-charges/:id/edit', component: RecurringChargeForm }` (depends on T038)

**Checkpoint**: User Story 3 completa e testável de forma independente (Acceptance Scenarios 1-3 do spec).

---

## Phase 6: User Story 4 - Remover uma cobrança recorrente sem perder histórico (Priority: P2)

**Goal**: Remover uma cobrança recorrente é soft delete — para de gerar novas contas, mas preserva a linha e as contas já geradas.

**Independent Test**: Remover uma cobrança recorrente que já gerou contas; confirmar que `deactivatedAt` é preenchido (linha preservada, `isActive() == false`), as contas antigas continuam intactas, e uma nova chamada a `generatePendingAccounts()` não cria conta nenhuma a partir dela.

### Implementation for User Story 4

- [X] T040 [US4] Update `backend/src/main/java/com/financas/recurringcharge/domain/RecurringChargeService.java`: add `delete(id)` — busca a linha (`findById`), define `deactivatedAt = LocalDate.now()`, `save` — nunca `deleteById` (FR-009) (depends on T034)
- [X] T041 [US4] Update `backend/src/main/java/com/financas/recurringcharge/api/RecurringChargeController.java`: add `DELETE /api/recurring-charges/{id}` (`204`) (depends on T040)
- [X] T042 [P] [US4] Add casos a `backend/src/test/java/com/financas/recurringcharge/domain/RecurringChargeServiceTest.java`: `delete` marca `deactivatedAt` não-nulo sem excluir a linha (`repository.deleteById` nunca chamado); cobrança removida (`isActive() == false`) não é retornada por `generatePendingAccounts()` na próxima geração (teste de integração leve entre os dois Services, ou via `RecurringChargeGenerationServiceTest`) (depends on T040)
- [X] T043 [US4] Update `frontend/src/app/shared/services/recurring-charge.service.ts`: add `delete(id)` (`DELETE /recurring-charges/{id}`) (depends on T022, T041)

**Checkpoint**: User Story 4 completa e testável de forma independente (Acceptance Scenarios 1-2 do spec).

---

## Phase 7: User Story 5 - Gerenciar cobranças recorrentes numa tela dedicada (Priority: P3)

**Goal**: Tela `/recurring-charges` lista as cobranças ativas, com ações de linha, seleção múltipla com remoção em lote, e um aviso visível quando a geração mais recente de uma cobrança falhou.

**Independent Test**: Abrir `/recurring-charges`, confirmar que todas as cobranças ativas aparecem listadas com os dados relevantes, que uma cobrança com `lastGenerationFailed = true` exibe um aviso e as demais não, e que seleção múltipla + remoção em lote funcionam como nas demais listagens do sistema.

### Implementation for User Story 5

- [X] T044 [US5] Update `backend/src/main/java/com/financas/recurringcharge/domain/RecurringChargeService.java`: add `findAll()` — filtra `isActive()` em memória (mesmo padrão de `AccountService.findAll`), ordenado por `description`, depois `id` (Princípio VI) (depends on T018)
- [X] T045 [US5] Update `backend/src/main/java/com/financas/recurringcharge/api/RecurringChargeController.java`: add `GET /api/recurring-charges` (`200`, `List<RecurringChargeResponse>`) (depends on T044)
- [X] T046 [P] [US5] Add caso a `backend/src/test/java/com/financas/recurringcharge/domain/RecurringChargeServiceTest.java`: `findAll` retorna só linhas ativas (uma removida e uma substituída por edição ficam de fora), ordenadas por `description` depois `id` (depends on T044)
- [X] T047 [US5] Update `frontend/src/app/shared/services/recurring-charge.service.ts`: add `findAll()` (`GET /recurring-charges`) (depends on T022, T045)
- [X] T048 [US5] Create `frontend/src/app/recurring-charge/recurring-charge-list/recurring-charge-list.ts`: reaproveita `createSelection` (`shared/list-selection.ts`), `bulkDelete` (`shared/bulk-delete.ts`), `BulkActionsBar`, `RowActions` (mesmo padrão de `AccountList`); carrega via `findAll()`; ação de linha "editar" navega para `/recurring-charges/:id/edit`; "remover" chama `delete` (individual e em lote); `@Component` referenciando `styleUrl: './recurring-charge-list.scss'` (arquivo criado mínimo/vazio, mesmo padrão Bootstrap-first das demais telas) (depends on T043, T047)
- [X] T049 [US5] Create `frontend/src/app/recurring-charge/recurring-charge-list/recurring-charge-list.html`: tabela com colunas tipo, valor, dia de vencimento, descrição, fundo, contraparte; checkbox de seleção com `(click)="selection.toggleWithRange(...)"` e `[class.table-active]="selection.isSelected(item)"` (mesmo padrão das demais listagens); badge de aviso (`<i class="bi bi-exclamation-triangle" ...>`, `title="A última tentativa de geração falhou para esta cobrança."`) exibido só quando `recurringCharge.lastGenerationFailed`; `RowActions` (editar/remover) e `BulkActionsBar` (depends on T048)
- [X] T050 [US5] Update `frontend/src/app/app.routes.ts`: add rota `{ path: 'recurring-charges', component: RecurringChargeList }` (depends on T048)
- [X] T051 [US5] Update `frontend/src/app/app.html`: add item de menu `<a class="nav-link" routerLink="/recurring-charges" routerLinkActive="active">Cobranças Recorrentes</a>` (depends on T050)

**Checkpoint**: As cinco user stories completas e testáveis de forma independente — feature completa de ponta a ponta.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validação final e documentação

- [X] T052 [P] Run roteiro de validação manual de `quickstart.md` de ponta a ponta (Playwright já configurado como devDependency), cobrindo os 13 cenários (cadastro individual/grupo/valor zero/grupo vazio, geração básica/idempotência/isolamento, indicador de falha, edição preservando histórico, remoção preservando histórico, bloqueio de remoção de parte/fundo, ações em lote, recuperação na inicialização)
- [X] T053 [P] Update `README.md` com as decisões técnicas desta feature (entidade `RecurringCharge` e o processo de geração automática, substituindo a nota já existente e desatualizada sobre "geração automática de lançamentos recorrentes" deixada pela feature 008), conforme Checkpoints da Rodada de Trabalho da constituição

---

## Phase 9: Aprimoramento visual do checkbox de seleção (impacto cruzado com a feature 002, sessão 2026-08-05)

**Purpose**: `specs/002-receivable-charges/spec.md` (FR-025) passou a exigir tratamento visual dedicado para o checkbox de seleção (linha e "selecionar todos") de toda listagem que reaproveita o trio de seleção múltipla, via a classe `row-select-checkbox` centralizada em `frontend/src/styles.scss` — `recurring-charge-list` precisa ganhá-lo (depends on `specs/002-receivable-charges/tasks.md` T073). Esta feature foi construída depois da generalização do destaque de linha (FR-024) e já nasceu com `table-active`, mas antes do FR-025, então o checkbox nasceu sem o tratamento visual.

- [X] T054 Add `class="form-check-input row-select-checkbox"` aos checkboxes de linha e "selecionar todos" de `frontend/src/app/recurring-charge/recurring-charge-list/recurring-charge-list.html` (FR-025)

**Checkpoint**: checkbox de seleção de `recurring-charge-list` com o mesmo tratamento visual das demais listagens.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem tarefas novas
- **Foundational (Phase 2)**: Bloqueia as cinco user stories — entidade `RecurringCharge`, persistência, e bloqueio de remoção de `Party`/`Fund` (FR-014)
- **User Story 1 (Phase 3)**: Depende apenas da Foundational completa
- **User Story 2 (Phase 4)**: Depende apenas da Foundational completa — não depende de US1 (a geração roda sobre qualquer `RecurringCharge` já persistida, inclusive criada diretamente via `RecurringChargeRepository` num teste, sem precisar do `Controller`/formulário de US1)
- **User Story 3 (Phase 5)**: Depende da Foundational e reaproveita `RecurringChargeService`/`RecurringChargeController`/`recurring-charge-form` já criados por US1 (T018, T019, T023) — não é totalmente independente de US1 nesta feature, já que edição é uma extensão natural do mesmo formulário e serviço de cadastro
- **User Story 4 (Phase 6)**: Depende da Foundational e reaproveita `RecurringChargeService`/`RecurringChargeController` de US1/US3 (T018, T034) pela mesma razão
- **User Story 5 (Phase 7)**: Depende da Foundational e reaproveita `RecurringChargeService`/`RecurringChargeController` (T018) e o `recurring-charge.service.ts`/model de US1 (T021, T022) — a listagem em si (`findAll`) é nova, mas o restante da tela (editar/remover por linha) depende de US3/US4 já existirem
- **Polish (Phase 8)**: Depende das cinco user stories estarem completas

### Parallel Opportunities

- T001–T010 (Foundational) podem começar em paralelo assim que a fase abre (arquivos distintos, sem dependência entre si além de T002 ser lido por T003/T004)
- T013, T014 podem rodar em paralelo entre si assim que T011/T012 estiverem prontas, respectivamente
- T015, T016, T017 (DTOs de US1) podem rodar em paralelo entre si
- T026, T027, T028 (US2 — migration, entidade, repositório) podem rodar em paralelo entre si
- T020 (teste de US1) pode ser escrito em paralelo com T018 (implementação), tocando o mesmo arquivo de teste mas casos distintos — mantido como "depends on" por clareza de execução sequencial
- T052, T053 (Polish) podem rodar em paralelo entre si, depois de todas as stories completas

---

## Parallel Example: Foundational (Phase 2)

```bash
# Em paralelo, assim que a Phase 2 é aberta:
Task: "Add migration V15__create_recurring_charge_table.sql"
Task: "Create RecurringCharge entity"
Task: "Create RecurringChargeTypeChangeNotAllowedException"
Task: "Create InvalidRecurringChargeAmountException"
Task: "Create EmptyGroupException (recurringcharge.domain)"
Task: "Create PartyHasActiveRecurringChargesException"
Task: "Create FundHasActiveRecurringChargesException"
```

---

## Implementation Strategy

### MVP First (User Stories 1 e 2)

1. Complete Phase 2: Foundational (entidade `RecurringCharge` + bloqueio de remoção)
2. Complete Phase 3: User Story 1 (cadastro, individual e em lote)
3. Complete Phase 4: User Story 2 (geração automática mensal — o valor central da feature)
4. **STOP and VALIDATE**: testar US1+US2 isoladamente (cadastrar uma cobrança e confirmar que `generatePendingAccounts()` gera a conta esperada)

### Incremental Delivery

1. Foundational → entidade e persistência prontas, remoção de `Party`/`Fund` protegida
2. US1 → cadastro (individual/grupo) funcionando de ponta a ponta (API + formulário)
3. US2 → geração automática mensal, com recuperação na inicialização e isolamento por cobrança (MVP completo: cadastrar + gerar já entrega o valor central)
4. US3 → edição preservando histórico
5. US4 → remoção preservando histórico
6. US5 → tela de gerenciamento completa (listagem, ações de linha, seleção múltipla, indicador de falha)
7. Polish → validação de ponta a ponta via `quickstart.md` + atualização do README

### Parallel Team Strategy

Com múltiplas pessoas: após a Foundational, US1 e US2 podem ser desenvolvidas em paralelo (não têm dependência mútua); US3, US4 e US5 dependem de artefatos de US1 (formulário/serviço) e, no caso de US5, também de US3/US4 (ações de editar/remover na listagem), então são naturalmente sequenciais a partir daí.

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes
- US2 não depende de US1 apesar de ambas serem P1 — a geração automática opera sobre `RecurringCharge` já persistida, independente de como ela foi criada
- US3/US4/US5 dependem de artefatos de US1 (`RecurringChargeService`/`RecurringChargeController`/`recurring-charge-form`/`recurring-charge.service.ts`) por reaproveitamento direto de código, não por acaso de ordenação — cada uma adiciona métodos ao mesmo arquivo em vez de recriar do zero
- O disparo real do `@Scheduled`/`ApplicationReadyEvent` (T032) só é observável rodando a aplicação de verdade; a cobertura de teste automatizado (T033) chama `generatePendingAccounts()` diretamente, sem depender da infraestrutura de agendamento do Spring (ver research.md)
- Commit ao final de cada tarefa ou grupo lógico de tarefas
- Pare em qualquer checkpoint para validar a story isoladamente antes de seguir
