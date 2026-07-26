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

**Storage**: PostgreSQL 18.4 via Docker Compose (porta 5433 do host), acessado via Spring Data
JPA — mesmo container já criado pela feature 001

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
- **IV. Convenções de Código e Formatação**: PASS. `dueDate` é `LocalDate` no domínio,
  serializado como string `dd/MM/yyyy` na API (ver research.md — primeira feature do projeto
  com campo de data, decisão a ser levada à Revisão da Constituição pós-implementação).
  Entidade/enum/coluna em inglês (`Receivable`, `TargetAccount`, `target_account`); mensagens
  de erro internas em inglês, mensagens ao usuário em português.
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
│   │   │                        # ReceivableBulkRequest, ReceivableResponse
│   │   ├── domain/              # Receivable entity, TargetAccount enum,
│   │   │                        # ReceivableRepository (port), ReceivableService
│   │   └── infra/               # ReceivableJpaRepository, ReceivableRepositoryImpl
│   └── shared/                  # inalterado
├── src/main/resources/db/migration/
│   └── V3__create_receivable_table.sql
├── src/test/java/com/financas/
│   └── receivable/
└── pom.xml                      # inalterado (sem novas dependências)

frontend/
├── src/app/
│   ├── unit/                    # inalterado
│   ├── resident/                # inalterado
│   ├── receivable/
│   │   ├── receivable-list/
│   │   └── receivable-form/
│   ├── core/                    # inalterado
│   └── shared/
│       ├── models/receivable.model.ts
│       └── services/receivable.service.ts
└── package.json                 # inalterado (sem novas dependências)
```

**Structure Decision**: Web application (mesma estrutura da feature 001, sem opções
alternativas). Nova entidade de domínio `Receivable` segue o mesmo padrão de pacote
`api/domain/infra` já estabelecido; nenhuma nova ferramenta, dependência ou convenção
estrutural é introduzida.

## Complexity Tracking

*Não se aplica — nenhuma violação da constituição identificada no Constitution Check dentro
do escopo desta feature.*
