# Implementation Plan: Duplicar lançamentos para o mês seguinte

**Branch**: `007-duplicate-account-next-month` | **Date**: 2026-08-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-duplicate-account-next-month/spec.md`

## Summary

Na listagem de contas (`/accounts`), ao selecionar um ou mais lançamentos, a usuária ganha duas novas ações em lote — "Duplicar para o mês seguinte" (mantém o valor) e "Duplicar para o mês seguinte com valor zerado" — que criam, para cada lançamento selecionado, uma nova conta independente com vencimento um mês depois e sem pagamento registrado, preservando os demais dados do original. A variante "valor mantido" também é acionável pelo atalho de teclado Ctrl+C (memoriza a seleção) seguido de Ctrl+V (dispara a duplicação), ignorado enquanto o foco estiver num campo de edição inline. Um único endpoint novo no backend (`POST /api/accounts/{id}/duplicate`) cobre as duas variantes; o frontend chama-o uma vez por lançamento selecionado, em melhor esforço, reaproveitando o padrão já usado pela remoção em lote.

## Clarifications

Ver [spec.md § Clarifications](./spec.md#clarifications) — decisão de manter o atalho de teclado Ctrl+C/Ctrl+V como especificado (sessão 2026-08-01), já refletida em FR-009 a FR-011 e neste plano.

## Revisão pós-implementação (sessão 2026-08-02)

Depois da implementação inicial (Foundational + US1 + US2), a usuária revisou a entrega e pediu três melhorias de feedback visual, formalizadas como FR-012 a FR-014 (ver spec.md): destaque de linha selecionada, destaque temporário + scroll até a cópia recém-criada, e mensagem de sucesso com a contagem de cópias. Mudança inteiramente de frontend, restrita a `account/account-list/` (novos signals `recentlyDuplicatedIds`/`successMessage`, ver data-model.md) e a `account-list.scss` (transição de cor nas células, para o destaque temporário não aparecer/desaparecer abruptamente) — nenhum endpoint, DTO ou componente compartilhado (`BulkActionsBar`) é afetado.

Re-confirmação da Constitution Check, princípio por princípio, à luz dessa mudança:
- **I. Arquitetura em Camadas**: PASS, sem alteração necessária. Os signals novos ficam em `account-list.ts`, mesmo nível dos demais signals efêmeros já existentes na tela (`payingId`, `editingAmountId`); nenhum novo componente ou pacote.
- **II. Separação Controller → Service → Repository**: PASS, sem alteração necessária — nenhuma mudança de backend.
- **III. Stack Técnica Definida**: PASS, sem alteração necessária — nenhuma dependência nova; a nova regra de negócio (persistência do `Account`) não muda, então nenhum caso de teste automatizado novo é exigido pelo Princípio III. O feedback visual em si é orquestração de componente (destaque/scroll/mensagem), não regra de negócio testável isoladamente — mesma linha divisória já registrada no research.md desta feature, validado via Playwright (ver quickstart.md).
- **IV. Convenções de Código e Formatação**: PASS, sem alteração necessária.
- **V. Idioma por Tipo de Conteúdo**: PASS — mensagem de sucesso exibida à usuária em português ("N conta(s) duplicada(s) com sucesso.").
- **VI. Convenções de API REST**: PASS, sem alteração necessária — nenhum contrato de API é tocado por esta mudança.

Nenhuma violação da constituição identificada.

## Revisão pós-implementação (sessão 2026-08-02, parte 2 — generalização do destaque de seleção)

A usuária apontou que o destaque de linha selecionada (FR-012) não deveria ser específico de `account-list` — é um comportamento do mecanismo de seleção múltipla em si, compartilhado por toda listagem que o reaproveita. FR-012 foi marcado como superseded em spec.md; o requisito equivalente agora vive em `specs/002-receivable-charges/spec.md` (FR-024, feature dona do trio `list-selection.ts`/`bulk-delete.ts`/`bulk-actions-bar`). Nesta feature, o binding `[class.table-active]="selection.isSelected(account)"` em `account-list.html` permanece exatamente como está — já está correto, só deixa de ser tratado como exclusivo desta tela. A única mudança de código associada a esta feature é mover a regra CSS de transição de cor (`table > tbody > tr > td { transition: background-color 1s ease; }`), hoje em `account-list.scss`, para `frontend/src/styles.scss` (global), já que ela deixa de ser exclusiva de `account-list` — mesmo padrão já usado para `.icon` (constituição, Princípio I).

Confirmação princípio a princípio: **I** (SEM ALTERAÇÃO — a regra CSS só muda de arquivo, sem novo componente), **II**/**III** (SEM ALTERAÇÃO — nenhum backend ou dependência nova), **IV** (SEM ALTERAÇÃO — a convenção de não duplicar CSS por componente já existe e está sendo seguida), **V**/**VI** (SEM ALTERAÇÃO). Nenhuma violação identificada.

## Technical Context

**Language/Version**: Java 21 (backend) + TypeScript 6.0.2 (frontend) — reuso das versões já adotadas nas features 001–006 (Constituição, Princípio III); nenhuma mudança de versão.

**Primary Dependencies**: Spring Boot (Data JPA, Web) no backend; Angular 22 (signals, zoneless) + Bootstrap 5.3.8 no frontend — mesmas dependências já em uso, sem pesquisa de mercado nova (Princípio III). Nenhuma dependência nova.

**Storage**: PostgreSQL, tabela `account` já existente — nenhuma coluna nova, nenhuma migration Flyway nesta feature (ver research.md, "O lançamento duplicado não guarda vínculo com o original").

**Testing**: Backend — JUnit 5 + Mockito, casos novos em `AccountServiceTest` para a regra de negócio de duplicação (cópia de campos, cálculo de vencimento, zeragem de valor, pagamento sempre nulo). Frontend — Vitest, novo `shared/bulk-duplicate.spec.ts` (mesmo padrão de `bulk-delete.spec.ts`); a lógica de teclado e a integração do componente `AccountList` não recebem teste unitário dedicado, seguindo o precedente já registrado no `research.md` da feature 006 (nenhum componente Angular tem teste próprio no projeto). Playwright (devDependency já configurada) para validação manual em navegador dos dois User Stories e do atalho de teclado.

**Target Platform**: mesma aplicação web já existente (SPA Angular + API REST Spring Boot, infraestrutura local das features 001–006).

**Project Type**: web (frontend Angular + backend Spring Boot, já estabelecidos pela feature 001) — esta feature altera as duas camadas.

**Performance Goals**: sem metas específicas — uso pessoal, poucas dezenas de lançamentos por listagem, duplicação disparada uma chamada HTTP por item selecionado (mesmo padrão de `bulkDelete`, sem necessidade de otimização adicional nesta escala).

**Constraints**: nenhuma dependência nova; o atalho de teclado MUST ser ignorado durante edição inline de campo (FR-011, reaproveita o padrão de foco já introduzido na feature 006); a duplicação em lote MUST seguir o padrão de melhor esforço já estabelecido, sem endpoint transacional novo.

**Scale/Scope**: 1 endpoint novo (`POST /api/accounts/{id}/duplicate`) + 1 método novo em `AccountService` + 1 DTO novo (`AccountDuplicateRequest`) no backend; 1 componente compartilhado estendido (`BulkActionsBar`) + 1 arquivo novo (`shared/bulk-duplicate.ts`) + 1 componente alterado (`account-list.ts`/`.html`) no frontend; 2 user stories, 11 requisitos funcionais (FR-001 a FR-011).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitetura em Camadas**: PASS. Nenhum pacote novo — o DTO `AccountDuplicateRequest` fica em `com.financas.account.api`, e o método `duplicate` fica em `AccountService` (`com.financas.account.domain`), junto dos demais métodos de negócio da entidade. No frontend, `shared/bulk-duplicate.ts` fica em `shared/` (utilitário puro, mesmo nível de `bulk-delete.ts`), `BulkActionsBar` continua em `shared/components/`, e a lógica de teclado/seleção copiada fica inteiramente dentro de `account/account-list/` (não é reaproveitada por nenhuma outra tela). Estado novo via `signal()` (`copiedIds`), consistente com o app zoneless.
- **II. Separação Controller → Service → Repository**: PASS. `AccountController.duplicate` chama `AccountService.duplicate`, que chama `AccountRepository` (via `findById`/`save`, ambos já existentes) — nenhum acesso a repositório pelo controller. Método com uma única escrita (`save`), portanto sem necessidade de `@Transactional` (ver research.md).
- **III. Stack Técnica Definida**: PASS. Reaproveita integralmente Java 21, Spring Boot, Angular 22, JUnit 5 + Mockito, Vitest e Playwright já adotados — nenhuma dependência nova. Cobertura de teste da regra de negócio nova via `AccountServiceTest` (backend) e `bulk-duplicate.spec.ts` (frontend), na mesma linha divisória já usada no projeto entre lógica de negócio/pura (testada automaticamente) e orquestração de componente (validada via Playwright).
- **IV. Convenções de Código e Formatação**: PASS. `dueDate`/`paymentDate` continuam `LocalDate` em ISO-8601, sem `@JsonFormat` customizado. Nenhum campo booleano duplicado é introduzido (`isPaid()` continua derivado de `paymentDate`, inalterado). `type` (discriminador) nunca é alterado pela duplicação — a cópia nasce com o mesmo `type` do original, nunca passa por `update`. Nenhum enum novo.
- **V. Idioma por Tipo de Conteúdo**: PASS. Artefatos deste plano em português; identificadores de código em inglês (`duplicate`, `zeroAmount`, `copiedIds`, `AccountDuplicateRequest`); mensagem de erro ao usuário reaproveitada em português ("Conta não encontrada.", já existente).
- **VI. Convenções de API REST**: PASS. `POST /api/accounts/{id}/duplicate` segue o padrão já definido para ação de negócio dedicada sobre um recurso existente (`POST /{recurso}/{id}/{ação}`, mesmo padrão de `/pay`). Resposta `201` usa `AccountResponse.from(...)`, mesmo factory já existente. Erros seguem o formato padronizado `{ message, status }` já implementado pelo `GlobalExceptionHandler`, sem código novo necessário (reaproveita `NotFoundException`).

Nenhuma violação da constituição identificada — Complexity Tracking não se aplica.

**Re-check pós-Phase 1**: PASS. `research.md`/`data-model.md`/`contracts/` não revelaram nenhuma decisão de design que contradiga a avaliação acima. A única adição não prevista inicialmente foi o input opcional `showDuplicateActions` em `BulkActionsBar` (documentado em `contracts/frontend-interfaces.md`) — uma extensão aditiva com valor padrão `false`, que não altera o comportamento das outras três listagens que reaproveitam o componente, mantendo o Princípio I (reaproveitar o trio já estabelecido) sem exceção.

## Project Structure

### Documentation (this feature)

```text
specs/007-duplicate-account-next-month/
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
backend/
└── src/main/java/com/financas/account/
    ├── api/
    │   ├── AccountDuplicateRequest.java   # novo — { zeroAmount: boolean }
    │   └── AccountController.java         # alterado — novo POST /{id}/duplicate
    └── domain/
        └── AccountService.java            # alterado — novo método duplicate(id, zeroAmount)
backend/src/test/java/com/financas/account/domain/AccountServiceTest.java  # alterado — casos novos de duplicate

frontend/
├── src/app/
│   ├── shared/
│   │   ├── models/account.model.ts             # alterado — novo tipo AccountDuplicateRequest
│   │   ├── services/account.service.ts         # alterado — novo método duplicate(id, request)
│   │   ├── bulk-duplicate.ts                   # novo — mesmo padrão de bulk-delete.ts
│   │   ├── bulk-duplicate.spec.ts              # novo — testes Vitest
│   │   └── components/bulk-actions-bar/
│   │       ├── bulk-actions-bar.ts             # alterado — showDuplicateActions/duplicate/duplicateZeroed
│   │       └── bulk-actions-bar.html           # alterado — dois botões condicionais
│   └── account/account-list/
│       ├── account-list.ts     # alterado — copiedIds, onKeydown (HostListener), duplicateSelected,
│       │                       # performDuplicate (FR-001 a FR-011)
│       └── account-list.html   # alterado — bindings novos em <app-bulk-actions-bar>
```

**Structure Decision**: Web application (mesma estrutura das features 001–006, sem opções alternativas). O backend ganha um único endpoint novo dentro do pacote `account` já existente, sem novo pacote nem migration. O frontend concentra a mudança específica de contas em `account/account-list/`, e estende (sem quebrar) o trio compartilhado `list-selection.ts` (inalterado nesta feature) + `bulk-actions-bar` + um novo `bulk-duplicate.ts` ao lado do já existente `bulk-delete.ts` — nenhum arquivo de `party-list`, `fund-list` ou `group-list` é tocado.

## Complexity Tracking

*Não se aplica — nenhuma violação da constituição identificada no Constitution Check dentro do escopo desta feature.*
