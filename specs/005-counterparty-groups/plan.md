# Implementation Plan: Unificação de Unidade/Fornecedor, Contas sem Restrição de Tipo e Grupos

**Branch**: `005-counterparty-groups` | **Date**: 2026-07-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-counterparty-groups/spec.md`

## Summary

Unificar as entidades hoje separadas `Unit` e `Supplier` em uma única entidade **`Party`** (`Parte`), sem distinção de papel, eliminando a restrição atual que só permite contas do tipo ENTRADA para unidades e SAÍDA para fornecedores — qualquer `Party` passa a poder ter contas de qualquer tipo. `Account` passa a referenciar uma única `Party` (FK obrigatória) em vez das duas FKs nullable mutuamente exclusivas (`unit_id`/`supplier_id`) protegidas hoje por uma CHECK constraint. Introduz o conceito de **`Group`** (`Grupo`): um conjunto nomeado de `Party`, cuja composição só é editada pela própria tela do Grupo, usado para lançar contas em lote (um modo alternável "Parte específica" / "Grupo" na tela de lançamento), generalizando o atual `POST /api/accounts/bulk` (hoje restrito a "todas as unidades", sempre ENTRADA). A tela de Contas ganha um filtro por Fundo, o filtro/coluna de contraparte é renomeado para "Parte", e uma linha de total dinâmico ao final da tabela exibe o valor líquido (ENTRADA − SAÍDA, podendo ser negativo) das contas atualmente exibidas — calculado inteiramente no frontend a partir da lista já carregada, sem endpoint dedicado, reaproveitando o padrão `computed()` já usado em `fund-list`. Como o ambiente é só de desenvolvimento (sem dado real a preservar — decisão explícita da usuária, ver spec Assumptions), a migração recria as tabelas envolvidas do zero, sem preservar `unit`/`supplier`/`account` existentes.

## Technical Context

**Language/Version**: Java 21 (LTS) no backend; TypeScript 6.0.2 no frontend — reuso da versão já adotada nas features 001–004 (Constituição, Princípio III)

**Primary Dependencies**: Spring Boot 4.1.0 (Spring Data JPA, Spring Web, Bean Validation) no backend; Angular 22 + Bootstrap 5.3.8 + SCSS no frontend — mesmas dependências e versões já em uso, reaproveitadas sem nova pesquisa de mercado (Princípio III). Nenhuma dependência nova (a relação Grupo↔Parte usa `@ManyToMany`/`@JoinTable` já suportado pelo Spring Data JPA).

**Storage**: PostgreSQL 18.4 via Docker Compose (porta 5434 do host), acessado via Spring Data JPA — mesmo container já criado pela feature 001; quatro migrations novas (V10–V13, ver research.md/data-model.md)

**Testing**: Backend — JUnit 5 + Mockito + Spring Boot Test. Frontend — Vitest + Angular Testing utilities. Cobertura obrigatória das regras de negócio desta feature (unicidade de nome de `Party`/`Group`, bloqueio de remoção de `Party` vinculada a contas, ausência de bloqueio de remoção de `Group`, cálculo do total líquido, geração de uma conta por integrante do grupo, bloqueio de lançamento em lote para grupo vazio) — exigência já vigente do Princípio III da constituição.

**Target Platform**: Aplicação web (API REST + SPA), mesma infraestrutura local já usada pelas features 001–004 (Docker Compose + `mvn spring-boot:run` porta 8082 + `ng serve` porta 4202)

**Project Type**: web (frontend Angular + backend Spring Boot, já estabelecidos pela feature 001)

**Performance Goals**: Sem metas específicas — uso pessoal, poucas dezenas de registros (ver Assumptions do spec); total líquido e filtros calculados em memória (backend) / sobre a lista já carregada (frontend), sem necessidade de consulta agregada otimizada no banco

**Constraints**: Sem autenticação/autorização (mesma exceção já registrada na feature 001); sem paginação nas listagens

**Scale/Scope**: Poucas dezenas de contas e partes, no máximo poucos grupos; 2 entidades novas (`Party`, `Group`), 2 entidades removidas (`Unit`, `Supplier`), 1 entidade existente alterada (`Account`); 5 user stories, 16 requisitos funcionais

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitetura em Camadas**: PASS. Novo pacote `com.financas.party` (`api/domain/infra`, mesmo padrão três-pastas de `unit`/`supplier`/`fund`) e novo pacote `com.financas.group` (idem). Os pacotes `com.financas.unit` e `com.financas.supplier` são removidos por completo (entidade, repository, service, exceptions, DTOs, controller). Migração via Flyway (`V10`–`V13`). Frontend em novos `frontend/src/app/party/` e `frontend/src/app/group/`, substituindo `frontend/src/app/unit/` e `frontend/src/app/supplier/` (removidos), reaproveitando `core/`, `shared/list-selection.ts`, `shared/bulk-delete.ts` e `shared/components/bulk-actions-bar/` sem alteração. **Revisão da constituição já aplicada antes da implementação** (a pedido da usuária, entre `/speckit-tasks` e `/speckit-implement`): a constituição foi emendada para v1.7.0, atualizando a nota sobre o padrão "FK nullable por contraparte possível + CHECK constraint" (Princípio I, v1.5.0) para registrar que o exemplo original (`Account.unit`/`Account.supplier`) foi superado por esta feição, e adicionando três novas diretrizes genéricas que emergiram do design desta feature: nome físico de tabela evitando palavra reservada SQL (`party_group`), composição de um agrupamento muitos-para-muitos editada só pela tela do próprio agrupamento (Princípio I), `Set` em vez de `List` para `@ManyToMany` sem ordem própria (Princípio IV), e o critério de quando um agregado é calculado no backend (Service, exposto via API) vs. inteiramente no frontend sobre a lista já carregada (Princípio VI). A revisão pós-implementação desta feature ainda MUST ocorrer normalmente, focada em decisões que só emergirem durante a escrita do código.
- **II. Separação Controller → Service → Repository**: PASS. `PartyController`/`GroupController` chamam apenas `PartyService`/`GroupService`. `AccountService` (não o Controller) resolve a `Party` e o `Group` (incluindo seus integrantes) via `PartyRepository`/`GroupRepository`, mesmo padrão já usado para `Fund`/`Unit`/`Supplier` hoje.
- **III. Stack Técnica Definida**: PASS. Reaproveita as mesmas versões já adotadas (Java 21, Spring Boot 4.1.0, Angular 22, PostgreSQL 18.4, JUnit 5 + Mockito, Vitest) — nenhuma nova pesquisa de versão "mais recente do mercado". Cobertura de teste de regra de negócio entregue junto da implementação.
- **IV. Convenções de Código e Formatação**: PASS. `AccountType` continua `@Enumerated(EnumType.STRING)`, sem alteração. `Account.type` continua obrigatório na criação e imutável na edição (FR-002) — a remoção da restrição de combinação com a contraparte não afeta essa regra, já reforçada por `AccountTypeChangeNotAllowedException`. Nenhum estado booleano duplicado é introduzido: o total líquido é sempre recalculado, nunca persistido. Identificadores em inglês (`Party`, `Group`, `party_group`); mensagens de erro internas em inglês, mensagens ao usuário em português.
- **V. Idioma por Tipo de Conteúdo**: PASS. Artefatos deste plano em português; código em inglês; mensagens de erro de API/frontend em português.
- **VI. Convenções de API REST**: PASS. Novas rotas `/api/parties` e `/api/groups` (plural, inglês), mesmo padrão `GET`/`GET {id}`/`POST`/`PUT {id}`/`DELETE {id}` de `unit`/`supplier`/ `fund` — nenhuma sub-rota nova é necessária para editar a composição de um `Group` (a lista de `partyIds` viaja no corpo do próprio `POST`/`PUT` do Grupo, por ser edição de um campo do recurso, não uma ação de negócio dedicada). `POST /api/accounts/bulk` é generalizado (mesma rota, novo formato de corpo: `type` explícito + `groupId` em vez de tipo implícito ENTRADA + "todas as unidades") — continua sendo o mesmo padrão de sub-rota de ação em lote já estabelecido. `GroupResponse` embute a lista completa de `PartyResponse` de seus integrantes (nunca só os ids), mesmo princípio de `AccountResponse.unit`/`.supplier` (agora `.party`). `PartyResponse`/`GroupResponse` expõem factory estático `from(Entity)`. Erros 4xx no formato padrão `{ "message", "status" }`.

Nenhuma violação da constituição identificada — Complexity Tracking não se aplica.

**Re-check pós-Phase 1**: PASS. `research.md`/`data-model.md`/`contracts/api.md` (Phase 0/1) não introduziram nenhuma decisão de design que contradiga a avaliação acima — as únicas observações que emergiram do design detalhado (nome físico da tabela `party_group` para evitar a palavra reservada `GROUP`; `Party.delete` ganhando exception dedicada em vez do `ConflictException` genérico que `Supplier` usava) são simplificações/correções pontuais consistentes com os princípios já avaliados, não violações novas.

## Impacto cruzado (sessão 2026-08-02 — destaque de linha selecionada, feature 002 FR-024)

A feature 002 generalizou o destaque visual de linha selecionada (`[class.table-active]="selection.isSelected(item)"`) para toda listagem que reaproveita o trio de seleção múltipla — hoje já aplicado a `party-list` e `group-list`. Ambas ganham esse binding em seus respectivos templates, sem alteração de `PartyService`/`GroupService`/`Controller`s nem de `list-selection.ts` em si (ver tasks.md, Phase 9). Constitution Check: sem alteração em nenhum princípio — mudança de template de uma linha por tela, reaproveitando `Selection.isSelected()` já existente.

## Project Structure

### Documentation (this feature)

```text
specs/005-counterparty-groups/
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
│   ├── party/                     # novo — substitui unit/ e supplier/
│   │   ├── api/                   # PartyController, PartyRequest, PartyResponse
│   │   ├── domain/                # Party entity, PartyRepository, PartyService,
│   │   │                          # DuplicatePartyException, PartyHasAccountsException
│   │   └── infra/                 # PartyJpaRepository, PartyRepositoryImpl
│   ├── group/                     # novo
│   │   ├── api/                   # GroupController, GroupRequest, GroupResponse
│   │   ├── domain/                # Group entity (@ManyToMany Party via party_group_member),
│   │   │                          # GroupRepository, GroupService, DuplicateGroupException
│   │   └── infra/                 # GroupJpaRepository, GroupRepositoryImpl
│   ├── unit/                      # REMOVIDO (Unit, UnitRepository, UnitService,
│   │                              # DuplicateUnitException, UnitHasAccountsException,
│   │                              # UnitHasSuppliersException, api/infra)
│   ├── supplier/                  # REMOVIDO (Supplier, SupplierRepository, SupplierService,
│   │                              # api/infra)
│   ├── account/
│   │   ├── api/                   # AccountRequest/Response: unitId/supplierId → partyId /
│   │   │                          # party (PartyResponse); AccountBulkRequest: + type,
│   │   │                          # unitId/supplierId implícitos → groupId;
│   │   │                          # AccountController: + fundId em GET, bulk chama
│   │   │                          # createForGroup
│   │   ├── domain/                # Account.unit/.supplier → Account.party (FK única,
│   │   │                          # obrigatória); AccountService: resolveUnit/resolveSupplier
│   │   │                          # → resolveParty; createForAllUnits → createForGroup
│   │   │                          # (+ type explícito); findAll: unitId/supplierId → partyId,
│   │   │                          # + fundId; NoUnitsRegisteredException removida,
│   │   │                          # EmptyGroupException nova; + PartyRepository,
│   │   │                          # GroupRepository
│   │   └── infra/                 # AccountJpaRepository/RepositoryImpl: unit_id/supplier_id
│   │                              # → party_id
│   └── shared/                    # inalterado
├── src/main/resources/db/migration/
│   ├── V10__create_party_table.sql              # nova
│   ├── V11__create_group_tables.sql             # nova (party_group + party_group_member)
│   ├── V12__migrate_account_to_party.sql        # nova (TRUNCATE account, drop CHECK e
│   │                                            # unit_id/supplier_id, + party_id FK)
│   └── V13__drop_unit_and_supplier_tables.sql   # nova
├── src/test/java/com/financas/
│   ├── party/                     # novo
│   ├── group/                     # novo
│   ├── unit/                      # REMOVIDO
│   ├── supplier/                  # REMOVIDO
│   └── account/                   # ajustado (partyId/groupId em vez de unitId/supplierId)
└── pom.xml                        # inalterado (sem novas dependências)

frontend/
├── src/app/
│   ├── party/                     # novo — substitui unit/ e supplier/
│   │   ├── party-list/            # nome, chave pix; seleção múltipla/remoção em lote
│   │   └── party-form/            # nome, chave pix (sem campo de grupo — FR-013)
│   ├── group/                     # novo
│   │   ├── group-list/            # nome, contagem de integrantes; seleção/remoção em lote
│   │   └── group-form/            # nome + seletor múltiplo de Partes integrantes
│   ├── unit/                      # REMOVIDO
│   ├── supplier/                  # REMOVIDO
│   ├── account/
│   │   ├── account-list/          # filtro "Unidade" → "Parte"; + filtro "Fundo"; coluna
│   │   │                          # "Contraparte" → "Parte" (account.party.name); + tfoot com
│   │   │                          # total líquido (computed sobre accounts()); pós-implementação:
│   │   │                          # filtro "Tipo" e coluna "Tipo de lançamento" removidos da UI
│   │   │                          # (tabela larga demais), ações por linha via row-actions/
│   │   └── account-form/          # unitId/supplierId condicionados por tipo → partyId único
│   │                              # + toggle "Parte específica"/"Grupo" (bulkMode generalizado)
│   ├── app.routes.ts              # − /units, /suppliers; + /parties, /groups
│   ├── app.html                   # menu: "Unidades"/"Fornecedores" → "Partes"; + "Grupos"
│   └── shared/
│       ├── models/party.model.ts       # novo (Party, PartyRequest)
│       ├── models/group.model.ts       # novo (Group, GroupRequest)
│       ├── models/unit.model.ts        # REMOVIDO
│       ├── models/supplier.model.ts    # REMOVIDO
│       ├── models/account.model.ts     # unit/supplier → party; AccountRequest.partyId;
│       │                              # AccountBulkRequest: + type, groupId;
│       │                              # AccountFilters: unitId/supplierId → partyId, + fundId
│       ├── services/party.service.ts   # novo
│       ├── services/group.service.ts   # novo
│       ├── services/unit.service.ts    # REMOVIDO
│       ├── services/supplier.service.ts # REMOVIDO
│       ├── list-selection.ts           # inalterado (reaproveitado)
│       ├── bulk-delete.ts              # inalterado (reaproveitado)
│       ├── components/bulk-actions-bar/ # inalterado (reaproveitado)
│       └── components/row-actions/     # novo (pós-implementação) — editar/remover por linha,
│                                        # ícones do pacote bootstrap-icons (originalmente SVG
│                                        # inline, migrado na feature 008-partial-payment-split);
│                                        # reaproveitado por account-list/fund-list/party-list/
│                                        # group-list (fund-list fora do escopo original desta
│                                        # feature, ajustado por consistência numa revisão da
│                                        # usuária)
└── package.json                   # inalterado (sem novas dependências)
```

**Structure Decision**: Web application (mesma estrutura das features 001–004, sem opções alternativas). `Party` e `Group` são entidades novas seguindo o mesmo padrão `api/domain/infra` já estabelecido; `Unit` e `Supplier` são removidos por completo (código e testes), sem shim de compatibilidade. Grupo reaproveita a mesma tela combinada listagem+formulário já usada por `unit`/`supplier`/`fund`, com o formulário de Grupo assumindo a responsabilidade extra de selecionar integrantes (Clarifications, sessão 2026-07-29). Nenhuma nova ferramenta, dependência ou convenção estrutural é introduzida.

## Complexity Tracking

*Não se aplica — nenhuma violação da constituição identificada no Constitution Check dentro do escopo desta feature.*
