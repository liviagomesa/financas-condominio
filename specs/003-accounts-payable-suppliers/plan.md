# Implementation Plan: Contas a Pagar, Fornecedores e Unificação de Contas

**Branch**: `003-accounts-payable-suppliers` | **Date**: 2026-07-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-accounts-payable-suppliers/spec.md`

## Summary

Generalizar `Receivable` (feature 002) em uma entidade única `Account`, com um atributo `type` (`RECEIVABLE`/`PAYABLE`, imutável após a criação) e uma contraparte obrigatória — `Unit` quando a receber, a nova entidade `Supplier` quando a pagar. Adicionar o cadastro de `Supplier` (nome obrigatório, unidade opcional, chave PIX opcional), remover por completo o cadastro de `Resident` (condômino) e sua tabela, e unificar a listagem de contas numa única tela com filtro por tipo e diferenciação visual por rótulo colorido (ver nota "Atualização 2026-07-28" abaixo — cor de fundo na linha inteira foi cogitada, implementada e removida por avaliação visual da usuária). Abordagem técnica: pacote `com.financas.receivable` renomeado para `com.financas.account`; novo pacote `com.financas.supplier`; `com.financas.resident` removido; migrations Flyway que renomeiam a tabela `receivable`→`account` (preservando dados), criam `supplier` e derrubam `resident`; frontend com `account/` (renomeado de `receivable/`) e novo `supplier/`, `resident/` removido. Esta feature também estende uma regra de negócio de uma feature já implementada (bloqueio de remoção de unidade, feature 001) e absorve/generaliza a entidade central de outra feature já implementada (feature 002) — ver nota de impacto cruzado no Constitution Check e em research.md.

## Technical Context

**Language/Version**: Java 21 (LTS) no backend; TypeScript 6.0.2 no frontend — reuso da versão já adotada nas features 001/002 (Constituição, Princípio III)

**Primary Dependencies**: Spring Boot 4.1.0 (Spring Data JPA, Spring Web, Bean Validation) no backend; Angular 22 + Bootstrap 5.3.8 + SCSS no frontend — mesmas dependências e versões já em uso no projeto, reaproveitadas sem nova pesquisa de mercado (Princípio III)

**Storage**: PostgreSQL 18.4 via Docker Compose (porta 5434 do host), acessado via Spring Data JPA — mesmo container já criado pela feature 001; três migrations novas (V5–V7, ver research.md/data-model.md)

**Testing**: Backend — JUnit 5 + Mockito + Spring Boot Test (`@DataJpaTest`/`@WebMvcTest`). Frontend — Vitest + Angular Testing utilities. Cobertura obrigatória das regras de negócio desta feature (validação de valor não negativo — zero permitido — para os dois tipos, consistência tipo/contraparte, imutabilidade de `type`, bloqueio de remoção de fornecedor/unidade vinculados) — exigência já vigente do Princípio III da constituição.

**Target Platform**: Aplicação web (API REST + SPA), mesma infraestrutura local já usada pelas features 001/002 (Docker Compose + `mvn spring-boot:run` porta 8082 + `ng serve` porta 4202)

**Project Type**: web (frontend Angular + backend Spring Boot, já estabelecidos pela feature 001)

**Performance Goals**: Sem metas específicas — uso pessoal, poucas dezenas de registros (ver Assumptions do spec)

**Constraints**: Sem autenticação/autorização (mesma exceção já registrada na feature 001); sem paginação nas listagens

**Scale/Scope**: Poucas dezenas de contas e fornecedores; 1 entidade generalizada (`Receivable`→`Account`), 1 entidade nova (`Supplier`), 1 entidade removida (`Resident`); 5 user stories, 23 requisitos funcionais

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitetura em Camadas**: PASS. `com.financas.account` (renomeado de `com.financas.receivable`) com `api/` (`AccountController`, `AccountRequest`, `AccountBulkRequest`, `AccountPaymentRequest`, `AccountResponse`), `domain/` (`Account`, `AccountType`, `Fund`, `AccountRepository`, `AccountService`, `InvalidAccountAmountException`, `NoUnitsRegisteredException`, `AccountTypeChangeNotAllowedException`) e `infra/` (implementação JPA). Novo `com.financas.supplier`, mesma estrutura três-pastas. `com.financas.resident` removido por completo. Migração via Flyway (`V5`–`V7`). Frontend em `frontend/src/app/account/` (renomeado de `receivable/`) e novo `frontend/src/app/supplier/`, reaproveitando `core/` e `shared/`; `frontend/src/app/resident/` removido.
- **II. Separação Controller → Service → Repository**: PASS. `AccountController` e `SupplierController` chamam apenas seus respectivos `Service`s; toda regra de negócio (valor não negativo — zero permitido —, consistência tipo/contraparte, imutabilidade de `type`, bloqueios de remoção) fica nos `Service`s.
- **III. Stack Técnica Definida**: PASS. Reaproveita as mesmas versões já adotadas (Java 21, Spring Boot 4.1.0, Angular 22, PostgreSQL 18.4, JUnit 5 + Mockito, Vitest) — nenhuma nova pesquisa de versão "mais recente do mercado". Cobertura de teste de regra de negócio entregue junto da implementação.
- **IV. Convenções de Código e Formatação**: PASS. `dueDate`/`paymentDate` continuam `LocalDate`, ISO-8601 na API, sem `@JsonFormat` (herdado de 002). `AccountType` e `Fund` usam `EnumType.STRING`. O estado "pago" continua derivado só de `paymentDate` (sem campo booleano redundante). Entidades/enums/colunas em inglês; mensagens de erro internas em inglês, mensagens ao usuário em português.
- **V. Idioma por Tipo de Conteúdo**: PASS. Artefatos deste plano em português; código em inglês; mensagens de erro de API/frontend em português.
- **VI. Convenções de API REST**: PASS. Rota `/api/accounts` (plural, inglês, substitui `/api/receivables`); nova `/api/suppliers`; ação de lote como sub-rota `/api/accounts/bulk`; ação de pagamento como sub-rota `/api/accounts/{id}/pay`; filtros de listagem como query params combináveis (E lógico); erros 4xx no formato padrão `{ "message", "status" }`. DTOs de resposta expõem `from(Entity)` e embutem o(s) DTO(s) completo(s) da(s) entidade(s) referenciada(s) (`AccountResponse.unit`/`.supplier`, `SupplierResponse.unit`).

**Atualização (2026-07-28 — remoção da cor de fundo por linha em `account-list`)**: a diferenciação visual por cor de fundo na linha inteira (`account-row--receivable`/ `--payable`) foi implementada, avaliada visualmente pela usuária e removida — a diferenciação por tipo passa a depender só do rótulo textual colorido (badge), já existente e mantido sem alteração. Confirmação princípio a princípio de que a mudança continua respeitando a constituição:
- **I. Arquitetura em Camadas**: PASS, sem alteração — mudança restrita ao template/estilo de `account-list` (frontend), sem tocar estrutura de pastas ou camadas.
- **II. Separação Controller → Service → Repository**: PASS, sem alteração — mudança é puramente de apresentação (frontend), não envolve `Service`/`Repository`.
- **III. Stack Técnica Definida**: PASS, sem alteração — nenhuma dependência nova ou removida.
- **IV. Convenções de Código e Formatação**: PASS — remove a classe CSS por linha e o binding `[class.account-row--...]` do template, sem introduzir convenção nova.
- **V. Idioma por Tipo de Conteúdo**: PASS, sem alteração.
- **VI. Convenções de API REST**: PASS, sem alteração — mudança não toca contrato de API.

**Atualização (2026-08-02 — ordenação padrão da listagem de contas, FR-024)**: `AccountService.findAll` (`backend/src/main/java/com/financas/account/domain/AccountService.java`) já filtra em memória (comentário existente no código: "Dado o volume pequeno de registros... a filtragem é feita em memória"); a ordenação por `dueDate` decrescente, com desempate por `description` alfabética crescente e, por último, `id` decrescente, é adicionada como um `.sorted(...)` (comparator composto com `thenComparing`) sobre a lista já filtrada, imediatamente antes do retorno — sem introduzir `ORDER BY`/`Sort` na camada de `Repository`, consistente com a abordagem em memória já adotada para os filtros. Confirmação princípio a princípio:
- **I. Arquitetura em Camadas**: PASS, sem alteração — mudança restrita a um método já existente do `Service` (`com.financas.account.domain`), sem tocar `Controller`/`Repository`/estrutura de pastas.
- **II. Separação Controller → Service → Repository**: PASS, sem alteração — a ordenação é responsabilidade do `Service`, como os demais filtros de `findAll`; o `Controller` continua repassando o resultado já pronto.
- **III. Stack Técnica Definida**: PASS, sem alteração — nenhuma dependência nova; cobertura de teste adicionada em `AccountServiceTest` (regra de negócio verificável, conforme Princípio III).
- **IV. Convenções de Código e Formatação**: PASS, sem alteração — nenhuma convenção de nomenclatura/formatação nova.
- **V. Idioma por Tipo de Conteúdo**: PASS, sem alteração.
- **VI. Convenções de API REST**: PASS — `GET /api/accounts` não ganha novo query param (a ordenação é sempre aplicada, não opcional); `contracts/api.md` atualizado apenas para documentar a ordem de retorno da resposta existente.

**Impacto cruzado com features já implementadas (001 e 002)** — sinalizado, não uma violação desta feature:

1. **Feature 001** (`specs/001-cadastro-condominos/`): o FR-017 desta feature ("bloquear remoção de unidade com conta ou fornecedor vinculado, sem mais considerar condôminos") estende/substitui a regra hoje implementada em `UnitService.delete()` (feature 001, FR-006), que atualmente considera condôminos e lançamentos de conta a receber vinculados. Pela constituição (seção "Edição de Features Já Implementadas"), essa mudança exige atualizar `specs/001-cadastro-condominos/spec.md` (FR-006, User Stories 5/6, Key Entities), `plan.md` (confirmação princípio a princípio) e `tasks.md` (sem regenerar nem alterar tarefas já concluídas) — com resumo apresentado à usuária para aprovação explícita antes de qualquer edição desses arquivos ou do código.
2. **Feature 002** (`specs/002-receivable-charges/`): esta feature generaliza por completo a entidade central de 002 (`Receivable` → `Account`, com `type`/`Supplier` adicionados). `specs/002-receivable-charges/spec.md`/`plan.md` precisam de uma nota de "absorvida/ estendida pela feature 003", preservando o histórico de decisões já documentado ali (ex.: por que `paymentDate` sem campo `paid`), também sujeita à mesma aprovação explícita.

Ambos os passos são tratados separadamente, fora deste plano (ver Completion Report), e são pré-requisito antes de `/speckit-tasks` gerar as tarefas correspondentes de alteração em `UnitService` e nos artefatos de 001/002.

Nenhuma violação da constituição identificada dentro do escopo da feature 003 — Complexity Tracking não se aplica.

## Project Structure

### Documentation (this feature)

```text
specs/003-accounts-payable-suppliers/
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
│   │   └── domain/             # UnitService: - ResidentRepository, + SupplierRepository;
│   │                            # UnitHasReceivablesException → UnitHasAccountsException;
│   │                            # UnitHasResidentsException removida; + UnitHasSuppliersException
│   │                            # (impacto cruzado com feature 001 — ver acima)
│   ├── resident/                # REMOVIDO por completo (domain/api/infra)
│   ├── account/                 # renomeado de receivable/
│   │   ├── api/                 # AccountController, AccountRequest, AccountBulkRequest,
│   │   │                        # AccountPaymentRequest, AccountResponse
│   │   ├── domain/              # Account entity (+ type, supplier, observations; unit
│   │   │                        # nullable), AccountType enum (novo), Fund enum (renomeado
│   │   │                        # de TargetAccount), AccountRepository (+ existsBySupplierId),
│   │   │                        # AccountService (+ validação tipo/contraparte, imutabilidade
│   │   │                        # de type), InvalidAccountAmountException (renomeada),
│   │   │                        # AccountTypeChangeNotAllowedException (nova)
│   │   └── infra/                # AccountJpaRepository, AccountRepositoryImpl (renomeados)
│   ├── supplier/                 # novo
│   │   ├── api/                  # SupplierController, SupplierRequest, SupplierResponse
│   │   ├── domain/                # Supplier entity, SupplierRepository (+ existsByUnitId),
│   │   │                          # SupplierService
│   │   └── infra/                  # SupplierJpaRepository, SupplierRepositoryImpl
│   └── shared/                    # inalterado
├── src/main/resources/db/migration/
│   ├── V5__create_supplier_table.sql              # nova
│   ├── V6__transform_receivable_to_account.sql    # nova (rename table/column, + type,
│   │                                                # supplier_id, observations, CHECK)
│   └── V7__drop_resident_table.sql                # nova
├── src/test/java/com/financas/
│   ├── account/                 # renomeado de receivable/
│   ├── supplier/                # novo
│   └── resident/                # REMOVIDO
└── pom.xml                      # inalterado (sem novas dependências)

frontend/
├── src/app/
│   ├── unit/                     # unit-list/unit-form inalterados na estrutura
│   ├── resident/                 # REMOVIDO por completo
│   ├── account/                  # renomeado de receivable/
│   │   ├── account-list/         # + coluna/rótulo colorido de tipo, filtro por tipo
│   │   └── account-form/         # + seletor de tipo (desabilitado em edição), alterna
│   │                              # unidade+lote / fornecedor, + observações
│   ├── supplier/                 # novo
│   │   ├── supplier-list/         # + seleção múltipla/remoção em lote (padrão já existente)
│   │   └── supplier-form/         # nome, unidade opcional, chave PIX opcional
│   ├── core/                     # inalterado
│   ├── app.routes.ts              # /residents* → /suppliers*; /receivables* → /accounts*
│   ├── app.html                   # nav: "Condôminos" → "Fornecedores"; "Lançamentos" → "Contas"
│   └── shared/
│       ├── models/account.model.ts    # renomeado de receivable.model.ts (+ AccountType,
│       │                               # Fund renomeado, supplier, observations)
│       ├── models/supplier.model.ts   # novo
│       ├── models/resident.model.ts   # REMOVIDO
│       ├── services/account.service.ts   # renomeado de receivable.service.ts
│       ├── services/supplier.service.ts  # novo
│       ├── services/resident.service.ts  # REMOVIDO
│       ├── list-selection.ts              # inalterado (reaproveitado)
│       ├── bulk-delete.ts                 # inalterado (reaproveitado)
│       └── components/bulk-actions-bar/   # inalterado (reaproveitado)
└── package.json                 # inalterado (sem novas dependências)
```

**Structure Decision**: Web application (mesma estrutura das features 001/002, sem opções alternativas). A entidade `Receivable` é generalizada em `Account` (renomeação de pacote, não uma entidade nova do zero); `Supplier` segue o mesmo padrão de pacote `api/domain/infra` já estabelecido; `Resident` é removido por completo. Nenhuma nova ferramenta, dependência ou convenção estrutural é introduzida — a modelagem de contraparte (duas FKs nullable + `CHECK`) é uma decisão de dados dentro do padrão já existente, não uma camada arquitetural nova.

## Complexity Tracking

*Não se aplica — nenhuma violação da constituição identificada no Constitution Check dentro do escopo desta feature.*
