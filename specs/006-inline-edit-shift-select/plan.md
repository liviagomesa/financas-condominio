# Implementation Plan: Edição inline de valor e seleção em intervalo com Shift

**Branch**: `006-inline-edit-shift-select` | **Date**: 2026-08-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-inline-edit-shift-select/spec.md`

## Summary

Duas melhorias de interação na listagem de contas e nas quatro listagens com seleção múltipla do sistema. (1) O campo "Valor" da listagem de contas passa a ser editável inline (clique → digita → Enter/blur confirma, Esc cancela), reaproveitando sem alteração o endpoint `PUT /api/accounts/{id}` já existente — nenhuma mudança de backend. (2) A checkbox de seleção de qualquer listagem (`account-list`, `party-list`, `fund-list`, `group-list`) passa a suportar Shift+clique para marcar um intervalo de linhas entre a última linha clicada normalmente (a "âncora", que não se move em Shift+cliques sucessivos, por decisão da Clarification) e a linha clicada — implementado uma única vez no utilitário já compartilhado `shared/list-selection.ts`, evitando duplicar a lógica nas quatro telas.

## Technical Context

**Language/Version**: TypeScript 6.0.2 — reuso da versão já adotada nas features 001–005 (Constituição, Princípio III); nenhuma mudança de backend (Java 21 permanece intocado nesta feature)

**Primary Dependencies**: Angular 22 (signals, zoneless) + Bootstrap 5.3.8 — mesmas dependências já em uso, reaproveitadas sem nova pesquisa de mercado (Princípio III). Nenhuma dependência nova.

**Storage**: N/A — nenhuma mudança de schema; a edição inline reaproveita o endpoint `PUT /api/accounts/{id}` já existente sem alterar seu contrato (ver research.md)

**Testing**: Frontend — Vitest, novo `shared/list-selection.spec.ts` cobrindo a lógica de âncora/intervalo (regra de negócio de UI introduzida por esta feature, Princípio III). Backend — nenhum teste novo necessário (regra de validação de `amount` já coberta por `AccountServiceTest`, endpoint reaproveitado sem alteração). Playwright para validação manual em navegador dos dois User Stories (Princípio III), já configurado como devDependency do projeto.

**Target Platform**: mesma aplicação web já existente (SPA Angular + API REST Spring Boot, infraestrutura local das features 001–005)

**Project Type**: web (frontend Angular + backend Spring Boot, já estabelecidos pela feature 001) — esta feature altera apenas o frontend

**Performance Goals**: sem metas específicas — uso pessoal, poucas dezenas de linhas por listagem (mesmo contexto das features anteriores)

**Constraints**: nenhuma dependência nova; o comportamento de seleção em intervalo MUST ser idêntico nas quatro listagens com caixas de seleção (FR-014), implementado uma única vez em `shared/list-selection.ts`

**Scale/Scope**: 1 utilitário compartilhado alterado (`shared/list-selection.ts` + novo `list-selection.spec.ts`), 1 componente alterado (`account-list.ts`/`.html`) para a edição inline, 4 templates alterados (`account-list`, `party-list`, `fund-list`, `group-list`) para a seleção em intervalo; 2 user stories, 14 requisitos funcionais (FR-001 a FR-014, incluindo FR-009a)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitetura em Camadas**: PASS. Nenhum pacote backend novo ou alterado. No frontend, `shared/list-selection.ts` permanece em `shared/` (utilitário de estado, não um componente de UI — distinção já estabelecida entre `shared/` solto e `shared/components/`), estendido sem quebrar a assinatura hoje usada por `toggle`/`toggleAll`/`isSelected`/`clear`. A edição inline do "Valor" fica inteiramente dentro de `account/account-list/` (não é reaproveitada por nenhuma outra tela, então não precisa virar componente compartilhado). Estado local via `signal()` (`editingAmountId`, `amountDraft`, âncora interna de `Selection<T>`) — consistente com o app zoneless.
- **II. Separação Controller → Service → Repository**: PASS/N/A. Nenhuma mudança de backend. A edição inline chama `AccountService.update` (frontend) → `AccountController` → `AccountService` (backend) exatamente como a tela de edição completa já faz hoje.
- **III. Stack Técnica Definida**: PASS. Reaproveita integralmente TypeScript 6.0.2, Angular 22, Vitest, Playwright já adotados — nenhuma dependência nova, nenhuma nova pesquisa de versão. Cobertura de teste da regra de negócio nova (âncora/intervalo) via `list-selection.spec.ts`, seguindo a mesma linha divisória já usada no projeto entre lógica pura em `shared/` (testada por Vitest) e orquestração de componente (validada via Playwright) — ver research.md para o raciocínio completo.
- **IV. Convenções de Código e Formatação**: PASS. Nenhum campo booleano duplicado é introduzido: `editingAmountId`/`payingId` são signals de id (não booleanos), e cada um zera o outro ao iniciar, sem estado redundante. Nenhum enum novo. Nenhuma data envolvida além das já existentes (inalteradas).
- **V. Idioma por Tipo de Conteúdo**: PASS. Artefatos deste plano em português; identificadores de código em inglês (`toggleWithRange`, `editingAmountId`, `amountDraft`); mensagem de erro ao usuário em português, reaproveitada de `account-form` ("O valor é obrigatório e não pode ser negativo.").
- **VI. Convenções de API REST**: PASS/N/A. Nenhuma rota nova. `PUT /api/accounts/{id}` reaproveitado sem alteração de formato (request/response/erros idênticos aos já documentados nas features 003/005).

Nenhuma violação da constituição identificada — Complexity Tracking não se aplica.

**Re-check pós-Phase 1**: PASS. `research.md`/`data-model.md`/`contracts/` não revelaram nenhuma decisão de design que contradiga a avaliação acima — a única adição foi o método novo `toggleWithRange` na interface `Selection<T>` (documentado em `contracts/frontend-interfaces.md`), uma extensão aditiva que não quebra os métodos já existentes nem exige mudança em nenhuma camada além do frontend.

## Project Structure

### Documentation (this feature)

```text
specs/006-inline-edit-shift-select/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── api.md
│   └── frontend-interfaces.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/                             # INALTERADO por esta feature

frontend/
├── src/app/
│   ├── shared/
│   │   ├── list-selection.ts        # alterado — âncora interna + toggleWithRange(item, items, shiftKey)
│   │   │                            # (FR-008 a FR-014); toggleAll/clear passam a resetar a âncora
│   │   └── list-selection.spec.ts   # novo — testes Vitest da lógica de âncora/intervalo
│   ├── account/
│   │   └── account-list/
│   │       ├── account-list.ts      # alterado — editingAmountId/amountDraft (edição inline do
│   │       │                        # "Valor", FR-001 a FR-007), exclusão mútua com payingId;
│   │       │                        # checkbox de linha passa a usar toggleWithRange
│   │       └── account-list.html    # alterado — célula "Valor" editável; (click) com $event.shiftKey
│   │                                # na checkbox de linha em vez de (change)
│   ├── party/party-list/party-list.html   # alterado — idem troca de (change) por (click)
│   ├── fund/fund-list/fund-list.html      # alterado — idem
│   └── group/group-list/group-list.html   # alterado — idem
```

**Structure Decision**: Web application (mesma estrutura das features 001–005, sem opções alternativas). Mudança confinada ao frontend — nenhum arquivo de `backend/` é tocado. `shared/list-selection.ts` continua sendo o único ponto de verdade da lógica de seleção, agora incluindo a seleção em intervalo, reaproveitado sem duplicação pelas quatro listagens (FR-014). A edição inline de "Valor" fica isolada em `account/account-list/`, por ser específica dessa tela (única com um campo monetário em tabela, ver Assumptions do spec).

## Complexity Tracking

*Não se aplica — nenhuma violação da constituição identificada no Constitution Check dentro do escopo desta feature.*
