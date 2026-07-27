# Implementation Plan: Lançamentos de Contas a Receber

**Branch**: `002-receivable-charges` | **Date**: 2026-07-26 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-receivable-charges/spec.md`

## Summary

Permitir lançar contas a receber vinculadas a uma unidade (valor, vencimento, descrição,
conta destino e tipo recorrente/extra), individualmente ou em lote para todas as unidades
cadastradas, além de listar, editar e remover esses lançamentos. Abordagem técnica: nova
entidade `Receivable` no backend Spring Boot, seguindo o mesmo padrão api/domain/infra já
usado por `unit`/`resident`, persistida em PostgreSQL via Spring Data JPA e Flyway; endpoint
dedicado de criação em lote; nova tela Angular de lançamentos, reaproveitando os padrões de
formulário/listagem/serviço já estabelecidos. Esta feature também estende uma regra de
negócio de uma feature já implementada (bloqueio de remoção de unidade, feature 001) — ver
nota de impacto cruzado no Constitution Check e em research.md.

## Technical Context

**Language/Version**: Java 21 (LTS) no backend; TypeScript 6 no frontend — reuso da versão
já adotada na feature 001 (Constituição, Princípio III)

**Primary Dependencies**: Spring Boot 4.1.0 (Spring Data JPA, Spring Web, Bean Validation) no
backend; Angular 22 + Bootstrap 5 + SCSS no frontend — mesmas dependências e versões já em uso
no projeto, reaproveitadas sem nova pesquisa de mercado (Princípio III)

**Storage**: PostgreSQL 18.4 via Docker Compose (porta 5434 do host — ajustada durante a
rodada de correções desta feature; a porta intermediária 5433 usada originalmente passou a
ser interceptada por um port-forward do VS Code nesta máquina, ver README.md), acessado via
Spring Data JPA — mesmo container já criado pela feature 001

**Testing**: Backend — JUnit 5 + Mockito + Spring Boot Test (`@DataJpaTest`/`@WebMvcTest`).
Frontend — Vitest + Angular Testing utilities. Cobertura obrigatória das regras de negócio
desta feature (validação de valor positivo, lançamento em lote restrito às unidades
existentes, bloqueio de remoção de unidade com lançamentos vinculados) — exigência da emenda
ao Princípio III da constituição.

**Target Platform**: Aplicação web (API REST + SPA), mesma infraestrutura local já usada pela
feature 001 (Docker Compose + `mvn spring-boot:run` + `ng serve`)

**Project Type**: web (frontend Angular + backend Spring Boot, já estabelecidos pela feature
001)

**Performance Goals**: Sem metas específicas — uso pessoal, poucas dezenas de unidades e
lançamentos (ver Assumptions do spec)

**Constraints**: Sem autenticação/autorização (mesma exceção já registrada na feature 001);
sem paginação nas listagens

**Scale/Scope**: Poucas dezenas de unidades, com múltiplos lançamentos por unidade; 1 entidade
nova (`Receivable`); 4 user stories, 14 requisitos funcionais

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitetura em Camadas**: PASS. Nova entidade em `com.financas.receivable`, com
  `api/` (controller, DTOs), `domain/` (entidade `Receivable`, enum `TargetAccount`,
  `ReceivableRepository`, `ReceivableService`) e `infra/` (implementação JPA). Build via
  Maven; migração via Flyway (`V3__create_receivable_table.sql`). Frontend em
  `frontend/src/app/receivable/`, com `receivable-list/` e `receivable-form/`, reaproveitando
  `core/` e `shared/` já existentes.
- **II. Separação Controller → Service → Repository**: PASS. `ReceivableController` chama
  apenas `ReceivableService`; toda regra de negócio (valor positivo, unidade obrigatória,
  escopo do lote) fica no `ReceivableService`.
- **III. Stack Técnica Definida**: PASS. Reaproveita as mesmas versões já adotadas na feature
  001 (Java 21, Spring Boot 4.1.0, Angular 22, PostgreSQL 18.4, JUnit 5 + Mockito, Vitest) —
  nenhuma nova pesquisa de versão "mais recente do mercado" foi feita, conforme exigido pela
  constituição. Cobertura de teste de regra de negócio será entregue junto da implementação
  (não em retrofit), atendendo à exigência já vigente da constituição para toda feature nova.
- **IV. Convenções de Código e Formatação**: PASS. `dueDate` e a nova `paymentDate` são
  `LocalDate` no domínio, serializadas no formato ISO-8601 padrão (`yyyy-MM-dd`), sem
  anotação `@JsonFormat` customizada — correção feita nesta rodada, já registrada
  diretamente no Princípio IV da constituição (não fica mais pendente de Revisão da
  Constituição pós-implementação, ver research.md). O formato DD/MM/AAAA passa a ser
  responsabilidade só do frontend, resolvido pelos recursos nativos do Angular (`DatePipe`
  para exibição, `<input type="date">` para entrada) — sem utilitário de conversão
  customizado (ver Project Structure). Entidade/enum/coluna em inglês (`Receivable`,
  `TargetAccount`, `target_account`); mensagens de erro internas em inglês, mensagens ao
  usuário em português.
- **V. Idioma por Tipo de Conteúdo**: PASS. Artefatos deste plano em português; código em
  inglês; mensagens de erro de API/frontend em português.
- **VI. Convenções de API REST**: PASS. Rota `/api/receivables` (plural, inglês); ação de
  lote como sub-rota `/api/receivables/bulk` (`POST`); erros 4xx no formato padrão `{
  "message", "status" }`; confirmação de remoção é responsabilidade do frontend. DTO de
  resposta expõe `from(Entity)` e embute `UnitResponse` completo (não apenas `unitId`),
  mesmo padrão de `ResidentResponse`.

**Impacto cruzado com feature já implementada (feature 001)** — sinalizado, não uma violação
desta feature: o FR-012 do spec desta feature ("bloquear remoção de unidade com lançamentos
vinculados") estende uma regra de negócio hoje implementada em `UnitService.delete()`
(feature 001), que atualmente só considera condôminos vinculados. Pela constituição (seção
"Edição de Features Já Implementadas"), essa mudança de comportamento em uma feature já
implementada exige atualizar `specs/001-cadastro-condominos/spec.md` (FR-006/edge cases),
`plan.md` (confirmação princípio a princípio) e adicionar uma tarefa a `tasks.md` (sem
regenerar nem alterar tarefas já concluídas) — com um resumo apresentado à usuária para
aprovação explícita antes de qualquer edição desses arquivos ou do código. Esse passo é
tratado separadamente, fora deste plano (ver Completion Report), e é pré-requisito antes de
`/speckit-tasks` gerar a tarefa correspondente de alteração em `UnitService`.

### Atualização (correção pós-implementação 2026-07-26 — pagamento, formato de data, tipo como checkbox, remoção em lote, filtros)

Rodada de correções solicitada após a implementação inicial desta feature, em duas partes (a
segunda parte adicionou pagamento já na criação, remoção do campo `paid` redundante, filtros
de listagem, e simplificou a abordagem de conversão de data). Confirmação princípio a
princípio:

- **I. Arquitetura em Camadas**: SEM ALTERAÇÃO NECESSÁRIA. `paymentDate` e os métodos de
  registro de pagamento/filtro continuam em `com.financas.receivable.domain`; os componentes
  de seleção múltipla/remoção em lote do frontend são recursos verdadeiramente transversais
  (usados por `unit`/`resident`/`receivable`) e por isso vivem em `frontend/src/app/shared/`,
  consistente com o critério já estabelecido para essa pasta. Não há mais um utilitário de
  conversão de data dedicado (ver Princípio IV abaixo) — um arquivo a menos que o previsto na
  primeira parte desta rodada.
- **II. Separação Controller → Service → Repository**: SEM ALTERAÇÃO NECESSÁRIA. O novo
  endpoint de pagamento e os novos filtros de `GET /api/receivables` passam por
  `ReceivableController` → `ReceivableService`, mesmo padrão já usado pelos demais endpoints;
  a remoção em lote não introduz um endpoint novo no backend (o frontend chama o `DELETE
  /{id}` já existente uma vez por item selecionado).
- **III. Stack Técnica Definida**: SEM ALTERAÇÃO NECESSÁRIA. Nenhuma dependência nova; testes
  de `registerPayment` e dos novos filtros seguem JUnit 5 + Mockito, mesma exigência já
  vigente.
- **IV. Convenções de Código e Formatação**: ALTERAÇÃO NECESSÁRIA E JÁ APLICADA — ver bullet
  acima e emenda ao Princípio IV em `.specify/memory/constitution.md` (incorporada à emenda
  1.2.0 → 1.3.0 ainda não commitada, sem novo bump de versão, conforme regra da própria
  constituição para amendas pendentes); a segunda parte desta rodada removeu até a exigência
  de um utilitário de conversão dedicado, em favor de `DatePipe`/`<input type="date">` nativos.
- **V. Idioma por Tipo de Conteúdo**: SEM ALTERAÇÃO NECESSÁRIA.
- **VI. Convenções de API REST**: PASS com extensões pontuais — `POST
  /api/receivables/{id}/pay` é uma sub-rota de ação (registra/atualiza o pagamento), no mesmo
  espírito da sub-rota `POST /{recurso}/bulk` já usada para criação em massa: uma ação que não
  é nem substituição completa do recurso (`PUT`) nem criação de um novo recurso (`POST` no
  recurso plano), então ganha sua própria sub-rota explícita; os novos filtros (`paid`,
  `overdue`, `dueYearMonth`, `paymentYearMonth`) são query params adicionais no mesmo `GET
  /api/receivables` já existente (mesmo padrão do `unitId` atual), sem rota nova. Formato de
  erro padrão mantido. Remoção em lote MUST NOT introduzir uma convenção de rota nova —
  reaproveita o `DELETE /{id}` existente, chamado repetidamente pelo frontend (decisão
  registrada em research.md).

Nenhuma violação da constituição identificada nesta rodada de correção — Complexity Tracking
não se aplica.

Nenhuma violação da constituição identificada dentro do escopo da feature 002 — Complexity
Tracking não se aplica.

## Project Structure

### Documentation (this feature)

```text
specs/002-receivable-charges/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── api.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/financas/
│   ├── unit/
│   │   └── domain/             # + UnitHasReceivablesException (impacto cruzado — ver acima)
│   ├── resident/                # inalterado
│   ├── receivable/
│   │   ├── api/                 # ReceivableController, ReceivableRequest,
│   │   │                        # ReceivableBulkRequest, ReceivablePaymentRequest (nova),
│   │   │                        # ReceivableResponse
│   │   ├── domain/              # Receivable entity (+ paymentDate), TargetAccount enum,
│   │   │                        # ReceivableRepository (port), ReceivableService
│   │   │                        # (+ registerPayment, filtros paid/overdue/dueYearMonth/
│   │   │                        # paymentYearMonth aplicados em memória)
│   │   └── infra/               # ReceivableJpaRepository, ReceivableRepositoryImpl
│   └── shared/                  # inalterado
├── src/main/resources/db/migration/
│   ├── V3__create_receivable_table.sql
│   └── V4__add_payment_fields_to_receivable.sql     # nova (correção pós-implementação)
├── src/test/java/com/financas/
│   └── receivable/
└── pom.xml                      # inalterado (sem novas dependências)

frontend/
├── src/app/
│   ├── unit/                    # unit-list ganha seleção múltipla + remoção em lote (impacto
│   │                             # cruzado — ver Phase 12 de specs/001-cadastro-condominos/tasks.md)
│   ├── resident/                # resident-list, idem
│   ├── receivable/
│   │   ├── receivable-list/     # + status pago/pendente, filtros, seleção múltipla, remoção
│   │   │                        # em lote — datas exibidas via DatePipe nativo
│   │   └── receivable-form/     # + data de pagamento opcional na criação/edição, ação de
│   │                             # registrar pagamento, "Recorrente" como checkbox,
│   │                             # <input type="date"> nativo (sem parsing manual)
│   ├── core/                    # inalterado
│   └── shared/
│       ├── models/receivable.model.ts       # + paymentDate (sem campo paid)
│       ├── services/receivable.service.ts   # + registerPayment, filtros em findAll
│       ├── list-selection.ts                 # novo — estado de seleção múltipla (signal-based)
│       ├── bulk-delete.ts                     # novo — remoção em lote "melhor esforço"
│       └── components/
│           └── bulk-actions-bar/              # novo — barra "N selecionados" + remover selecionados
└── package.json                 # inalterado (sem novas dependências)
```

**Structure Decision**: Web application (mesma estrutura da feature 001, sem opções
alternativas). Nova entidade de domínio `Receivable` segue o mesmo padrão de pacote
`api/domain/infra` já estabelecido; nenhuma nova ferramenta, dependência ou convenção
estrutural é introduzida.

## Complexity Tracking

*Não se aplica — nenhuma violação da constituição identificada no Constitution Check dentro
do escopo desta feature.*
