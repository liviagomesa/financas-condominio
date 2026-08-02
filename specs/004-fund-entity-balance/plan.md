# Implementation Plan: Fundos como Entidade e Visualização de Saldo Real

**Branch**: `004-fund-entity-balance` | **Date**: 2026-07-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-fund-entity-balance/spec.md`

## Summary

Transformar `Fund` de enum fixo (`POOL`, `POOL_GARDEN`, `SIDE_GARDEN`) em uma entidade de cadastro (`nome` único + `saldo inicial`), permitindo criar, editar e remover fundos livremente. `Account.fund` passa de valor de enum para referência obrigatória (`@ManyToOne`) a essa nova entidade. Uma nova tela de listagem de fundos exibe, para cada um, o **saldo real** — saldo inicial somado aos recebimentos já recebidos menos os pagamentos já pagos vinculados a ele (nunca lançamentos ainda em aberto) — além do saldo total somado de todos os fundos. Sem bloqueio de saldo negativo (FR-012): a visualização é puramente informativa. Como o banco atual é só de desenvolvimento (sem dado real a preservar — decisão explícita da usuária), a migração não precisa popular os três fundos hoje fixos nem preservar vínculos de `account` existentes.

## Technical Context

**Language/Version**: Java 21 (LTS) no backend; TypeScript 6.0.2 no frontend — reuso da versão já adotada nas features 001–003 (Constituição, Princípio III)

**Primary Dependencies**: Spring Boot 4.1.0 (Spring Data JPA, Spring Web, Bean Validation) no backend; Angular 22 + Bootstrap 5.3.8 + SCSS no frontend — mesmas dependências e versões já em uso, reaproveitadas sem nova pesquisa de mercado (Princípio III)

**Storage**: PostgreSQL 18.4 via Docker Compose (porta 5434 do host), acessado via Spring Data JPA — mesmo container já criado pela feature 001; duas migrations novas (V8–V9, ver research.md/data-model.md)

**Testing**: Backend — JUnit 5 + Mockito + Spring Boot Test. Frontend — Vitest + Angular Testing utilities. Cobertura obrigatória das regras de negócio desta feature (unicidade de nome de fundo, bloqueio de remoção de fundo vinculado, cálculo do saldo real, ausência de bloqueio por saldo negativo) — exigência já vigente do Princípio III da constituição.

**Target Platform**: Aplicação web (API REST + SPA), mesma infraestrutura local já usada pelas features 001–003 (Docker Compose + `mvn spring-boot:run` porta 8082 + `ng serve` porta 4202)

**Project Type**: web (frontend Angular + backend Spring Boot, já estabelecidos pela feature 001)

**Performance Goals**: Sem metas específicas — uso pessoal, poucas dezenas de registros (ver Assumptions do spec); cálculo do saldo real feito em memória a partir dos lançamentos do fundo, sem necessidade de consulta agregada otimizada no banco

**Constraints**: Sem autenticação/autorização (mesma exceção já registrada na feature 001); sem paginação nas listagens

**Scale/Scope**: Poucas dezenas de contas e, no máximo, poucas dezenas de fundos; 1 entidade nova (`Fund`, convertida de enum), 1 entidade existente alterada (`Account.fund`); 3 user stories, 12 requisitos funcionais

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitetura em Camadas**: PASS. Novo pacote `com.financas.fund` com `api/` (`FundController`, `FundRequest`, `FundResponse`), `domain/` (`Fund` entity, `FundRepository`, `FundService`, `DuplicateFundException`, `FundHasAccountsException`) e `infra/` (`FundJpaRepository`, `FundRepositoryImpl`) — mesma estrutura três-pastas já usada por `unit`/`supplier`. `com.financas.account.domain.Fund` (enum) é removido; `Account.fund` passa a referenciar `com.financas.fund.domain.Fund`. Migração via Flyway (`V8`–`V9`). Frontend em novo `frontend/src/app/fund/` (`fund-list/`, `fund-form/`), reaproveitando `core/` e `shared/`.
- **II. Separação Controller → Service → Repository**: PASS. `FundController` chama apenas `FundService`; toda regra de negócio (unicidade de nome, bloqueio de remoção por vínculo, cálculo do saldo real) fica em `FundService`. O cálculo do saldo real é um método de negócio público de `FundService` (não fica na DTO nem no Controller) — o Controller só monta a `FundResponse` combinando a entidade com o valor já calculado pelo Service.
- **III. Stack Técnica Definida**: PASS. Reaproveita as mesmas versões já adotadas (Java 21, Spring Boot 4.1.0, Angular 22, PostgreSQL 18.4, JUnit 5 + Mockito, Vitest) — nenhuma nova pesquisa de versão "mais recente do mercado". Cobertura de teste de regra de negócio entregue junto da implementação.
- **IV. Convenções de Código e Formatação**: PASS. `initialBalance`/`realBalance` são `BigDecimal`, mesma convenção de `Account.amount`. Nenhum enum novo introduzido (o único enum removido é `Fund`, que deixa de existir). Entidades/colunas em inglês; mensagens de erro internas em inglês, mensagens ao usuário em português.
- **V. Idioma por Tipo de Conteúdo**: PASS. Artefatos deste plano em português; código em inglês; mensagens de erro de API/frontend em português.
- **VI. Convenções de API REST**: PASS. Nova rota `/api/funds` (plural, inglês), mesmo padrão `GET`/`GET {id}`/`POST`/`PUT {id}`/`DELETE {id}` de `unit`/`supplier`. `FundResponse` expõe um factory estático `from(Fund, BigDecimal realBalance)` — uma extensão pontual do padrão `from(Entity)` já usado por `UnitResponse`/`SupplierResponse`, necessária porque `realBalance` é o único campo de resposta desta feature que não é derivável só da entidade (depende de agregação sobre `Account`); nenhuma outra DTO deste projeto precisa desse padrão até agora, documentado em research.md. `AccountResponse.fund` embute o mesmo `FundResponse` completo retornado por `GET /api/funds` (mesmo princípio de embutir a entidade referenciada por completo já aplicado a `unit`/`supplier`) — como isso exige o valor computado, `AccountResponse.from` passa a receber esse `FundResponse` já pronto como parâmetro extra, em vez de construí-lo inline a partir só do `Account` (ver research.md para o racional completo). Erros 4xx no formato padrão `{ "message", "status" }`.

Nenhuma violação da constituição identificada — Complexity Tracking não se aplica.

## Impacto cruzado (sessão 2026-08-02 — destaque de linha selecionada, feature 002 FR-024)

A feature 002 generalizou o destaque visual de linha selecionada (`[class.table-active]="selection.isSelected(item)"`) para toda listagem que reaproveita o trio de seleção múltipla — hoje já aplicado a `fund-list` (T033). `fund-list` ganha esse binding em `fund-list.html`, sem alteração de `FundService`/`FundController` nem de `list-selection.ts` em si (ver tasks.md, Phase 7). Constitution Check: sem alteração em nenhum princípio — mudança de template de uma linha, reaproveitando `Selection.isSelected()` já existente.

## Project Structure

### Documentation (this feature)

```text
specs/004-fund-entity-balance/
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
│   ├── fund/                      # novo
│   │   ├── api/                   # FundController, FundRequest, FundResponse
│   │   ├── domain/                # Fund entity, FundRepository, FundService,
│   │   │                          # DuplicateFundException, FundHasAccountsException
│   │   └── infra/                 # FundJpaRepository, FundRepositoryImpl
│   ├── account/
│   │   ├── api/                   # AccountRequest/AccountBulkRequest: fund → fundId (Long);
│   │   │                          # AccountResponse: fund (Fund) → fund (FundResponse),
│   │   │                          # from(Account, FundResponse); AccountController: injeta
│   │   │                          # FundService para montar o FundResponse embutido
│   │   ├── domain/                # Account.fund: com.financas.account.domain.Fund (enum,
│   │   │                          # REMOVIDO) → com.financas.fund.domain.Fund (entity,
│   │   │                          # @ManyToOne obrigatório); AccountRepository: +
│   │   │                          # findByFundId, existsByFundId; AccountService: +
│   │   │                          # FundRepository, findFundOrThrow(fundId)
│   │   └── infra/                 # AccountJpaRepository/AccountRepositoryImpl: + consultas
│   │                              # por fund_id
│   └── shared/                    # inalterado
├── src/main/resources/db/migration/
│   ├── V8__create_fund_table.sql             # nova
│   └── V9__convert_account_fund_to_entity.sql # nova (TRUNCATE account — sem dado a
│   │                                          # preservar —, troca coluna fund por fund_id FK)
├── src/test/java/com/financas/
│   ├── fund/                      # novo
│   └── account/                   # ajustado (fundId em vez de fund nos testes existentes)
└── pom.xml                        # inalterado (sem novas dependências)

frontend/
├── src/app/
│   ├── fund/                      # novo
│   │   ├── fund-list/             # nome, saldo inicial, saldo real por fundo + saldo total;
│   │   │                          # seleção múltipla/remoção em lote (padrão já existente)
│   │   └── fund-form/             # nome, saldo inicial
│   ├── account/
│   │   ├── account-list/          # coluna "Fundo": fundLabels[account.fund] → account.fund.name
│   │   └── account-form/          # fundOptions estático (FUND_LABELS) → lista carregada via
│   │                              # FundService.findAll(); controle fund → fundId
│   ├── app.routes.ts              # + /funds, /funds/new, /funds/:id/edit
│   ├── app.html                   # + item de menu "Fundos"
│   └── shared/
│       ├── models/fund.model.ts        # novo (Fund, FundRequest)
│       ├── models/account.model.ts     # remove Fund/FUND_LABELS locais (passam a vir de
│       │                              # fund.model.ts); Account.fund: Fund (objeto embutido);
│       │                              # AccountRequest/AccountBulkRequest.fund → fundId
│       ├── services/fund.service.ts    # novo
│       ├── list-selection.ts           # inalterado (reaproveitado)
│       ├── bulk-delete.ts              # inalterado (reaproveitado)
│       └── components/bulk-actions-bar/ # inalterado (reaproveitado)
└── package.json                   # inalterado (sem novas dependências)
```

**Structure Decision**: Web application (mesma estrutura das features 001–003, sem opções alternativas). `Fund` é uma entidade nova, seguindo o mesmo padrão `api/domain/infra` já estabelecido por `unit`/`supplier`; a visualização de saldo real e o cadastro de fundos vivem na mesma tela (`fund-list`), evitando introduzir uma tela de "dashboard" separada para um dado que já é a própria listagem da entidade. Nenhuma nova ferramenta, dependência ou convenção estrutural é introduzida.

## Complexity Tracking

*Não se aplica — nenhuma violação da constituição identificada no Constitution Check dentro do escopo desta feature.*
