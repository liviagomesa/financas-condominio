---

description: "Task list template for feature implementation"
---

# Tasks: Cadastro de Condôminos e Unidades

**Input**: Design documents from `/specs/001-cadastro-condominos/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [quickstart.md](./quickstart.md)

**Tests**: Não solicitados explicitamente no spec (sem TDD requerido) — nenhuma tarefa dedicada de escrita de testes automatizados foi gerada. A validação end-to-end é coberta pelo roteiro manual de `quickstart.md` (tarefa de Polish).

**Organization**: Tarefas agrupadas por user story (spec.md) para permitir implementação e teste independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1..US6)
- Caminhos de arquivo exatos estão incluídos em cada descrição

## Path Conventions (Web app — ver plan.md)

- Backend: `backend/src/main/java/com/financas/...`
- Frontend: `frontend/src/app/...`
- Banco: `docker-compose.yml` na raiz do repositório

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do repositório — `backend/`, `frontend/` e banco ainda não existem (primeira feature do projeto).

- [X] T001 [P] Create `docker-compose.yml` na raiz do repositório com serviço PostgreSQL 18.4 (porta, volume, credenciais de desenvolvimento)
- [X] T002 [P] Initialize Spring Boot backend project em `backend/` (Maven, Java 21, Spring Boot 4.1.x, base package `com.financas`) com dependências: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-flyway`, `flyway-database-postgresql`, `org.postgresql:postgresql`, starters `-test` correspondentes
- [X] T003 [P] Initialize Angular frontend project em `frontend/` (Angular 22, routing habilitado, estilos SCSS)
- [X] T004 [P] Add Bootstrap 5 ao frontend (`npm install bootstrap` em `frontend/`; import em `frontend/src/styles.scss`)

**Checkpoint**: Esqueleto de `backend/`, `frontend/` e `docker-compose.yml` criado — pronto para a fase Foundational.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura central que MUST estar pronta antes de qualquer user story

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase estar completa

- [X] T005 [P] Configure `backend/src/main/resources/application.yml` com datasource PostgreSQL (compatível com `docker-compose.yml`), `spring.jpa.hibernate.ddl-auto=validate` (schema gerenciado por Flyway) e diretório de migrations `classpath:db/migration`
- [X] T006 [P] Create `GlobalExceptionHandler` e exceptions base (`NotFoundException`, `ConflictException`) em `backend/src/main/java/com/financas/shared/`
- [X] T007 [P] Configure CORS no backend liberando `http://localhost:4200`, em `backend/src/main/java/com/financas/shared/WebConfig.java`
- [X] T008 [P] Create configuração de URL base da API em `frontend/src/environments/environment.ts` (`http://localhost:8080/api`)
- [X] T009 [P] Create interceptor HTTP de tratamento de erro em `frontend/src/app/core/error.interceptor.ts`
- [X] T010 [P] Configure esqueleto de rotas Angular em `frontend/src/app/app.routes.ts` (vazio, pronto para as rotas de `unit` e `resident`)

**Checkpoint**: Fundação pronta — implementação das user stories pode começar.

---

## Phase 3: User Story 1 - Cadastrar unidade (Priority: P1) 🎯 MVP

**Goal**: Permitir cadastrar uma unidade com identificador único (normalizado) e vê-la na listagem.

**Independent Test**: Cadastrar uma unidade com identificador "Bloco A - 101" e confirmar que ela aparece na listagem de unidades; tentar duplicar (inclusive com variação de maiúsculas/espaços) e confirmar rejeição; tentar cadastrar sem identificador e confirmar rejeição.

### Implementation for User Story 1

- [X] T011 [P] [US1] Create entidade JPA `Unit` (`id`, `identifier`) em `backend/src/main/java/com/financas/unit/domain/Unit.java`
- [X] T012 [US1] Create migration Flyway `backend/src/main/resources/db/migration/V1__create_unit_table.sql` (tabela `unit` + índice único funcional em `lower(trim(identifier))`, ver research.md)
- [X] T013 [P] [US1] Create interface de porta `UnitRepository` em `backend/src/main/java/com/financas/unit/domain/UnitRepository.java` (depends on T011)
- [X] T014 [US1] Create `UnitJpaRepository` (Spring Data) e `UnitRepositoryImpl` em `backend/src/main/java/com/financas/unit/infra/` (depends on T013)
- [X] T015 [US1] Implement `UnitService` (criar com validação de unicidade normalizada — trim + case-insensitive —, e listar) em `backend/src/main/java/com/financas/unit/domain/UnitService.java` (depends on T014)
- [X] T016 [P] [US1] Create DTOs `UnitRequest`/`UnitResponse` em `backend/src/main/java/com/financas/unit/api/`
- [X] T017 [US1] Implement `UnitController` (`POST /api/units`, `GET /api/units`) em `backend/src/main/java/com/financas/unit/api/UnitController.java` (depends on T015, T016)
- [X] T018 [P] [US1] Create `DuplicateUnitException` em `backend/src/main/java/com/financas/unit/domain/` (regra de negócio da própria entidade — não em `shared/`) e mapear para 409 com mensagem em português no `GlobalExceptionHandler` (depends on T006)
- [X] T019 [P] [US1] Create model `Unit` em `frontend/src/app/shared/models/unit.model.ts`
- [X] T020 [P] [US1] Create `UnitService` (HttpClient) em `frontend/src/app/shared/services/unit.service.ts` (depends on T008, T019)
- [X] T021 [US1] Create componente `unit-list` (tabela simples com identificador) em `frontend/src/app/unit/unit-list/` (depends on T020)
- [X] T022 [US1] Create componente `unit-form` (cadastro, validação de campo obrigatório e mensagem de duplicidade) em `frontend/src/app/unit/unit-form/` (depends on T020)
- [X] T023 [US1] Wire rotas de unidade em `frontend/src/app/app.routes.ts` (depends on T010, T021, T022)

**Checkpoint**: User Story 1 completa e testável de forma independente (Acceptance Scenarios 1-3 do spec).

---

## Phase 4: User Story 2 - Cadastrar novo condômino em uma unidade (Priority: P1)

**Goal**: Permitir cadastrar um condômino com nome e unidade obrigatórios, e-mail/telefone opcionais.

**Independent Test**: Com ao menos uma unidade cadastrada, preencher nome e unidade válidos e confirmar que o condômino aparece na listagem; confirmar que múltiplos condôminos podem compartilhar a mesma unidade; confirmar rejeição sem nome/unidade; confirmar orientação para cadastrar unidade primeiro quando não houver nenhuma.

### Implementation for User Story 2

- [X] T024 [P] [US2] Create entidade JPA `Resident` (`id`, `name`, `unit` ManyToOne obrigatório, `email` nullable, `phone` nullable) em `backend/src/main/java/com/financas/resident/domain/Resident.java`
- [X] T025 [US2] Create migration Flyway `backend/src/main/resources/db/migration/V2__create_resident_table.sql` (tabela `resident` com `unit_id` FK `NOT NULL`) (depends on T012)
- [X] T026 [P] [US2] Create interface de porta `ResidentRepository` em `backend/src/main/java/com/financas/resident/domain/ResidentRepository.java` (depends on T024)
- [X] T027 [US2] Create `ResidentJpaRepository` e `ResidentRepositoryImpl` em `backend/src/main/java/com/financas/resident/infra/` (depends on T026)
- [X] T028 [P] [US2] Create validador de telefone em formato brasileiro (DDD 2 dígitos + 8 ou 9 dígitos, ver research.md) em `backend/src/main/java/com/financas/resident/domain/BrazilianPhone.java` + `BrazilianPhoneValidator.java` (`ConstraintValidator` customizado)
- [X] T029 [US2] Implement `ResidentService` (criar com nome/unidade obrigatórios, checagem de existência da unidade referenciada, e listar) em `backend/src/main/java/com/financas/resident/domain/ResidentService.java` (depends on T027, T028, T015)
- [X] T030 [P] [US2] Create DTOs `ResidentRequest`/`ResidentResponse` (com `UnitResponse` aninhado) em `backend/src/main/java/com/financas/resident/api/`
- [X] T031 [US2] Implement `ResidentController` (`POST /api/residents`, `GET /api/residents`) em `backend/src/main/java/com/financas/resident/api/ResidentController.java` (depends on T029, T030)
- [X] T032 [P] [US2] Mapear unidade referenciada inexistente para 404 com mensagem orientando a cadastrar a unidade primeiro, no `GlobalExceptionHandler` (depends on T006) — reaproveita o handler genérico de `NotFoundException`, sem código extra
- [X] T033 [P] [US2] Create model `Resident` em `frontend/src/app/shared/models/resident.model.ts`
- [X] T034 [P] [US2] Create `ResidentService` (HttpClient) em `frontend/src/app/shared/services/resident.service.ts` (depends on T008, T033)
- [X] T035 [US2] Create componente `resident-list` (lista simples por nome) em `frontend/src/app/resident/resident-list/` (depends on T034)
- [X] T036 [US2] Create componente `resident-form` (cadastro com seletor de unidade via `UnitService`, validação de nome/unidade obrigatórios, e-mail e telefone opcionais, orientação quando não há unidades) em `frontend/src/app/resident/resident-form/` (depends on T034, T020)
- [X] T037 [US2] Wire rotas de condômino em `frontend/src/app/app.routes.ts` (depends on T023, T035, T036)

**Checkpoint**: User Stories 1 e 2 funcionam de forma independente (Acceptance Scenarios do US2).

---

## Phase 5: User Story 3 - Listar unidades e condôminos cadastrados (Priority: P1)

**Goal**: Exibir listagens completas de unidades e condôminos, com indicação clara quando vazias.

**Independent Test**: Cadastrar unidades e condôminos e verificar que todos aparecem em suas listagens, com o vínculo condômino → unidade visível; com listas vazias, verificar indicação de "nenhum registro".

### Implementation for User Story 3

- [X] T038 [P] [US3] Enhance `unit-list` para exibir mensagem de "nenhuma unidade cadastrada" quando a lista vier vazia, em `frontend/src/app/unit/unit-list/` (depends on T021)
- [X] T039 [P] [US3] Enhance `resident-list` para exibir todas as colunas (nome, unidade associada, e-mail, telefone) e mensagem de "nenhum condômino cadastrado" quando vazia, em `frontend/src/app/resident/resident-list/` (depends on T035)

**Checkpoint**: Listagens completas conforme FR-013/FR-014 — US1, US2 e US3 funcionam de forma independente.

---

## Phase 6: User Story 4 - Editar dados de unidade e de condômino (Priority: P2)

**Goal**: Permitir editar identificador de unidade e nome/unidade/e-mail/telefone de condômino, respeitando as mesmas validações da criação.

**Independent Test**: Editar um condômino existente (telefone, unidade) e uma unidade existente (identificador) e confirmar que a listagem reflete os novos valores; confirmar rejeição de identificador duplicado e de nome vazio na edição.

### Implementation for User Story 4

- [X] T040 [US4] Add `GET /api/units/{id}` (consulta usada pelo formulário de edição — não estava em contracts/api.md original, adicionada e documentada nesta fase) e `PUT /api/units/{id}` ao `UnitController`, com método de atualização (mesma validação de unicidade normalizada, 404 se não existir) ao `UnitService`, em `backend/src/main/java/com/financas/unit/` (depends on T015, T017)
- [X] T041 [US4] Add `GET /api/residents/{id}` (idem, para o formulário de edição) e `PUT /api/residents/{id}` ao `ResidentController`, com método de atualização (mesmas validações da criação, 404 se não existir) ao `ResidentService`, em `backend/src/main/java/com/financas/resident/` (depends on T029, T031)
- [X] T042 [P] [US4] Add modo de edição ao `unit-form` (pré-preenchimento + chamada PUT) em `frontend/src/app/unit/unit-form/` (depends on T022, T040)
- [X] T043 [P] [US4] Add modo de edição ao `resident-form` (pré-preenchimento + chamada PUT) em `frontend/src/app/resident/resident-form/` (depends on T036, T041)
- [X] T044 [P] [US4] Add ação "editar" nas linhas de `unit-list` e `resident-list` navegando ao formulário em modo edição, em `frontend/src/app/unit/unit-list/` e `frontend/src/app/resident/resident-list/` (depends on T038, T039)

**Checkpoint**: Edição funcional para ambas as entidades, independente das demais stories.

---

## Phase 7: User Story 5 - Remover condômino (Priority: P3)

**Goal**: Permitir remover um condômino, mediante confirmação explícita.

**Independent Test**: Remover um condômino cadastrado e confirmar que ele some da listagem sem afetar a unidade associada.

### Implementation for User Story 5

- [X] T045 [US5] Add `DELETE /api/residents/{id}` ao `ResidentController` e método de remoção (404 se não existir) ao `ResidentService`, em `backend/src/main/java/com/financas/resident/` (depends on T029, T031)
- [X] T046 [US5] Add ação de remoção com diálogo de confirmação em `resident-list`, em `frontend/src/app/resident/resident-list/` (depends on T039)

**Checkpoint**: Remoção de condômino funcional de forma independente.

---

## Phase 8: User Story 6 - Remover unidade (Priority: P3)

**Goal**: Permitir remover uma unidade sem condôminos vinculados, bloqueando a remoção quando houver vínculo.

**Independent Test**: Remover uma unidade sem condôminos e confirmar que some da listagem; tentar remover uma unidade com condôminos vinculados e confirmar o bloqueio com mensagem explicativa.

### Implementation for User Story 6

- [X] T047 [P] [US6] Create `UnitHasResidentsException` em `backend/src/main/java/com/financas/unit/domain/` (regra de negócio da própria entidade — não em `shared/`) e mapear para 409 com mensagem em português no `GlobalExceptionHandler` (depends on T006)
- [X] T048 [US6] Add `DELETE /api/units/{id}` ao `UnitController` e método de remoção ao `UnitService` (404 se não existir; 409 via `UnitHasResidentsException` se `ResidentRepository` indicar condômino vinculado), em `backend/src/main/java/com/financas/unit/` (depends on T015, T017, T026, T047)
- [X] T049 [US6] Add ação de remoção com diálogo de confirmação em `unit-list`, tratando erro 409 com a mensagem de vínculo, em `frontend/src/app/unit/unit-list/` (depends on T038, T048)

**Checkpoint**: Todas as 6 user stories funcionam de forma independente.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Validações finais e documentação

- [X] T050 [P] Run roteiro de validação manual de `quickstart.md` de ponta a ponta — executado via API real (curl contra Postgres) e navegador real (Playwright headless), cobrindo os 6 cenários; encontrado e corrigido um bug real: a imagem `postgres:18.4` exige o volume montado em `/var/lib/postgresql` (não `/var/lib/postgresql/data`), e a porta 5432 do host já estava ocupada por um PostgreSQL nativo — `docker-compose.yml` ajustado para expor a porta `5433`
- [X] T051 [P] Update `README.md` com decisões técnicas tomadas nesta feature (versões de stack, estrutura de pastas, ausência de Spring Security por ora), conforme Fluxo de Commits da constituição
- [X] T052 [P] Review mensagens de erro do `GlobalExceptionHandler` para garantir consistência em português (FR-016) — confirmado nos testes end-to-end: todas as mensagens de erro (400/404/409) retornadas em português, no formato `{ "message", "status" }`

---

## Phase 10: Retrofit — Testes automatizados de regras de negócio

**Purpose**: adicionado numa revisão pós-implementação (constitution emendada para exigir
cobertura de teste automatizado para regras de negócio — ver `constitution.md` Princípio
III). Sem tarefas de spec/plan novas: não há mudança de requisito funcional, só cobertura
de teste para o que já foi implementado.

- [X] T053 [P] Create testes unitários de `UnitService` (Mockito: criação com identificador
  duplicado — incluindo variação de caixa/espaços —, edição mantendo o próprio identificador
  vs. colidindo com outra unidade, `findById` inexistente, remoção bloqueada com condômino
  vinculado e permitida sem vínculo) em
  `backend/src/test/java/com/financas/unit/domain/UnitServiceTest.java`
- [X] T054 [P] Create testes unitários de `ResidentService` (Mockito: criação com unidade
  inexistente, `findById`/remoção de condômino inexistente, edição referenciando unidade
  inexistente) em `backend/src/test/java/com/financas/resident/domain/ResidentServiceTest.java`
- [X] T055 [P] Create testes parametrizados de `BrazilianPhoneValidator` (formatos válidos
  com/sem formatação, DDD + 8 ou 9 dígitos; formatos inválidos; nulo/vazio aceito por ser
  campo opcional) em
  `backend/src/test/java/com/financas/resident/domain/BrazilianPhoneValidatorTest.java`

**Checkpoint**: `mvn test` — 24 testes, 0 falhas (1 de contexto Spring + 10 do validador de
telefone + 6 do `ResidentService` + 7 do `UnitService`).

---

## Phase 11: Extensão — Bloqueio de remoção de unidade com lançamentos vinculados (feature 002)

**Purpose**: adicionado durante o planejamento da feature `002-receivable-charges`, que
estendeu o FR-006 desta feature (ver spec.md/plan.md, seção "Atualização (feature 002)").
Depende da entidade `Receivable` da feature 002 já existir (`ReceivableRepository`).

- [X] T056 [US6] Create `UnitHasReceivablesException` em
  `backend/src/main/java/com/financas/unit/domain/` (regra de negócio da própria entidade —
  mesmo padrão de `UnitHasResidentsException`) e mapear para 409 com mensagem em português no
  `GlobalExceptionHandler` (reaproveita o handler genérico de `ConflictException`, sem código
  extra)
- [X] T057 [US6] Update `UnitService.delete()` para também verificar
  `ReceivableRepository.existsByUnitId(id)`, lançando `UnitHasReceivablesException` quando
  houver lançamento vinculado, em
  `backend/src/main/java/com/financas/unit/domain/UnitService.java` (depends on T048 desta
  feature, e na entidade `Receivable` da feature 002)
- [X] T058 [P] [US6] Update tratamento de erro 409 em `unit-list` (frontend) para exibir a
  mensagem correta também quando o vínculo for por lançamento de conta a receber (a mensagem
  já vem pronta do backend — sem lógica nova no frontend além de exibir o texto retornado),
  em `frontend/src/app/unit/unit-list/` (depends on T049, T057) — confirmado sem alteração de
  código necessária: `unit-list.ts` já exibia `err.message` genericamente antes desta feature
- [X] T059 [P] Create testes unitários da nova regra em `UnitServiceTest` (Mockito: remoção
  permitida quando não há condômino nem lançamento vinculado, remoção bloqueada quando há
  lançamento vinculado) em
  `backend/src/test/java/com/financas/unit/domain/UnitServiceTest.java` — tarefa adicionada
  durante a implementação da feature 002 para fechar a exigência de cobertura de teste de
  regra de negócio (Princípio III), que não tinha sido incluída nesta Phase 11 originalmente
  (depends on T057)

**Checkpoint**: `DELETE /api/units/{id}` bloqueia remoção tanto por condômino quanto por
lançamento de conta a receber vinculado.

---

## Phase 12: Extensão — Seleção múltipla e remoção em lote (feature 002, FR-018)

**Purpose**: adicionado durante uma rodada de correções pós-implementação da feature
`002-receivable-charges`, que introduziu seleção múltipla + remoção em lote (melhor esforço)
na listagem de lançamentos e criou, para isso, um par de utilitários compartilhados de
frontend. Esta fase aplica o mesmo padrão às listagens desta feature (ver spec.md/plan.md,
seção "Atualização (feature 002 — seleção múltipla e remoção em lote, FR-018)"). Depende de
`frontend/src/app/shared/list-selection.ts`, `bulk-delete.ts` e
`shared/components/bulk-actions-bar/` (feature 002, Phase 8: T063-T065) já existirem.

- [ ] T060 [P] Apply seleção múltipla (coluna de checkbox por linha + "selecionar todos") e o
  `bulk-actions-bar` a `unit-list`, reaproveitando `list-selection.ts`/`bulk-delete.ts`/
  `bulk-actions-bar` da feature 002, em `frontend/src/app/unit/unit-list/` (depends on
  T063-T065 de `specs/002-receivable-charges/tasks.md`)
- [ ] T061 [P] Apply o mesmo padrão a `resident-list`, em
  `frontend/src/app/resident/resident-list/` (depends on T063-T065 de
  `specs/002-receivable-charges/tasks.md`)
- [ ] T062 [P] Run roteiro de validação manual do cenário de remoção em lote em `unit-list`
  (incluindo o caso de melhor esforço: selecionar uma unidade com condôminos vinculados junto
  de outra sem vínculo, e confirmar que só a segunda é removida, com a primeira reportada como
  falha) e em `resident-list`, atualizando `quickstart.md` desta feature com o roteiro

**Checkpoint**: `unit-list` e `resident-list` permitem selecionar múltiplos registros e
removê-los em uma única ação, com o mesmo comportamento de melhor esforço já validado em
`receivable-list`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências — pode começar imediatamente
- **Foundational (Phase 2)**: Depende da conclusão do Setup — bloqueia todas as user stories
- **User Story 1 (Phase 3)**: Depende apenas do Foundational
- **User Story 2 (Phase 4)**: Depende do Foundational; reutiliza `UnitService`/`UnitService`.list (T015) e `UnitService`(frontend, T020) da US1 para o seletor de unidade no formulário de condômino
- **User Story 3 (Phase 5)**: Depende das listagens básicas criadas em US1 (T021) e US2 (T035)
- **User Story 4 (Phase 6)**: Depende de US1 (T015, T017, T022) e US2 (T029, T031, T036) e das listagens de US3 (T038, T039)
- **User Story 5 (Phase 7)**: Depende de US2 (T029, T031) e US3 (T039)
- **User Story 6 (Phase 8)**: Depende de US1 (T015, T017), US2 (T026, para checar vínculo) e US3 (T038)
- **Polish (Phase 9)**: Depende de todas as user stories desejadas estarem completas
- **Extensão US6 (Phase 11)**: Depende de US6 (T048, T049) desta feature e da entidade
  `Receivable` da feature `002-receivable-charges` já existir
- **Extensão seleção múltipla (Phase 12)**: Depende de US3 (T038, T039 — listagens já
  exibindo estado vazio) desta feature e dos utilitários compartilhados T063-T065 da feature
  `002-receivable-charges` (Phase 8) já existirem

### Notas de dependência entre stories

Diferente do caso ideal onde todas as user stories seriam 100% independentes, aqui
US2 depende do `UnitService` (US1) para resolver a unidade referenciada, e US3
depende das listagens mínimas criadas em US1/US2 para exibi-las por completo —
isso reflete a ordem P1 → P1 → P1 já definida no spec (unidade é pré-requisito de
condômino, que por sua vez precisa existir para ser listado). US4, US5 e US6
dependem apenas de operações (PUT/DELETE) sobre entidades já criadas nas stories
anteriores, sem acoplar regras de negócio novas entre si.

### Parallel Opportunities

- Todas as tarefas [P] do Setup (T001-T004) podem rodar em paralelo
- Todas as tarefas [P] do Foundational (T005-T010) podem rodar em paralelo, após o Setup
- Dentro de cada user story, tarefas de entidade/DTO marcadas [P] podem rodar em paralelo antes das tarefas de service/controller que dependem delas
- T042, T043, T044 (US4) podem rodar em paralelo entre si

---

## Parallel Example: User Story 1

```bash
# Entidade e DTOs em paralelo:
Task: "Create entidade JPA Unit em backend/src/main/java/com/financas/unit/domain/Unit.java"
Task: "Create DTOs UnitRequest/UnitResponse em backend/src/main/java/com/financas/unit/api/"

# Frontend model e service em paralelo (após T008 do Foundational):
Task: "Create model Unit em frontend/src/app/shared/models/unit.model.ts"
Task: "Create UnitService (HttpClient) em frontend/src/app/shared/services/unit.service.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — bloqueia todas as stories)
3. Complete Phase 3: User Story 1 (cadastro e listagem mínima de unidades)
4. **STOP and VALIDATE**: testar US1 isoladamente (cenários 1-3 do spec)

### Incremental Delivery

1. Setup + Foundational → fundação pronta
2. US1 → cadastro de unidades funcionando (MVP mínimo, ainda que dependa de US2/US3 para o cadastro fazer sentido de ponta a ponta)
3. US2 → cadastro de condôminos funcionando, usando as unidades de US1
4. US3 → listagens completas com estado vazio
5. US4 → edição de ambas as entidades
6. US5 → remoção de condômino
7. US6 → remoção de unidade com proteção de vínculo

### Observação sobre MVP real

Como US1, US2 e US3 são todas P1 no spec, o produto só entrega valor real de
ponta a ponta (cadastrar E ver o cadastro) após as três estarem completas — US1
sozinha cadastra unidades, mas o objetivo do produto (gerenciar condôminos) só é
alcançado com US1 + US2 + US3 juntas. Considere isso ao decidir onde parar para
uma primeira validação com a usuária.

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes
- Cada user story é independentemente completável e testável, exceto pelas
  dependências de dados descritas acima (unidade é pré-requisito de condômino)
- Commit ao final de cada tarefa ou grupo lógico de tarefas
- Pare em qualquer checkpoint para validar a story isoladamente antes de seguir
