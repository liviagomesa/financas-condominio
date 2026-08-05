# Índice do Codebase

Este documento é um índice comprimido do codebase — não duplica regra de negócio (isso vive em cada `spec.md`) nem convenção técnica (isso vive na `constitution.md`). Serve só para responder rápido "que módulo/área cobre isso" e "quais specs afetaram esse módulo", evitando varrer `specs/` pasta por pasta a cada alteração pontual. Ver `constitution.md`, seção "Índice do Codebase (docs/codebase-research.md)", para quando e como ele MUST ser consultado e mantido atualizado.

## Módulos de Domínio do Backend (`backend/src/main/java/com/financas/`)

| Módulo | Specs relevantes | Responsabilidade |
|---|---|---|
| `account` | 002, 003, 004, 005, 006, 007, 008, 009 | Lançamento financeiro (a pagar ou a receber), vinculado a uma `Party` (unidade ou fornecedor) ou a um `Group`, com fundo (`Fund`), vencimento, pagamento total/parcial e amount imutável por tipo. Originado na 002 como `Receivable` (só contas a receber por unidade); generalizado para `Account` na 003 (contas a pagar + fornecedor); passou a embutir resumo de `Fund` na 004; perdeu a restrição de tipo obrigatório por contraparte na 005 (unificação `Unit`/`Supplier` → `Party`); ganhou edição inline de valor na 006, duplicação em lote para o mês seguinte na 007, e pagamento parcial/a maior na 008. A 009 acopla este módulo indiretamente: `RecurringChargeGenerationService` (módulo `recurringcharge`) escreve `Account`s através de `AccountService`. |
| `fund` | 004 | Fundo do condomínio (ex.: fundo de reserva), com saldo real calculado por agregação sobre `Account`s vinculadas. Criado na 004 (conversão de enum para entidade); exposto em resumo leve (`FundSummaryResponse`, sem o campo computado) quando embutido em `AccountResponse`/`RecurringChargeResponse`, reservando o DTO completo para o próprio recurso `/api/funds`. |
| `party` | 001, 003, 005 | Contraparte de um lançamento — unidade do condomínio ou fornecedor, unificados numa única entidade. Originado na 001 como `Unit` (unidade do condomínio, então acoplada a um cadastro de condômino/`Resident` removido por completo na 003); a 003 introduziu `Supplier` como entidade separada para contas a pagar; a 005 unificou `Unit` e `Supplier` em `Party`, eliminando a necessidade de FK dupla mutuamente exclusiva em `Account`. |
| `group` | 005 | Agrupamento nomeado de `Party` (ex.: todas as unidades de um bloco), usado para lançar a mesma conta em lote para todos os membros. Introduzido na 005 — primeira relação muitos-para-muitos do projeto. Composição do grupo é editada exclusivamente pela tela do próprio `Group` (Princípio I da constitution). |
| `recurringcharge` | 009 | Cobrança ou pagamento recorrente de valor fixo (reajustável ~1x/ano), que gera `Account`s automaticamente via processo agendado (`@Scheduled` dia 25 às 6h de Brasília) com recuperação em `ApplicationReadyEvent`. Criado por completo na 009. |
| `shared` (backend) | nenhuma spec dedicada — ver `constitution.md` Princípio I | Infraestrutura transversal reaproveitada por todo módulo de domínio: `GlobalExceptionHandler`, `ErrorResponse`, exceptions genéricas (`NotFoundException`, `ConflictException`, `BadRequestException`), `WebConfig` (CORS). Introduzida incrementalmente desde a 001, com `BadRequestException` adicionada na 002. |

## Áreas do Frontend (`frontend/src/app/`)

| Área | Specs relevantes | Responsabilidade |
|---|---|---|
| `account/` (`account-list`, `account-form`) | 002, 003, 004, 005, 006, 007, 008, 009 | Telas de listagem/formulário de lançamentos — espelha o módulo backend `account`. Ganhou colunas de Parte/Fundo/total líquido e ícones de ação na 005, edição inline de valor + seleção em intervalo (Shift) na 006, atalho de teclado Ctrl+C/Ctrl+V para duplicar na 007, fluxo de pagamento parcial na 008. |
| `fund/` (`fund-list`, `fund-form`) | 004 | Telas de listagem/formulário de fundos — espelha o módulo backend `fund`. |
| `party/` (`party-list`, `party-form`) | 001, 003, 005 | Telas de listagem/formulário de unidades/fornecedores unificados — espelha o módulo backend `party`. |
| `group/` (`group-list`, `group-form`) | 005 | Telas de listagem/formulário de grupos, incluindo edição da composição (membros) — espelha o módulo backend `group`. |
| `recurring-charge/` (`recurring-charge-list`, `recurring-charge-form`) | 009 | Telas de listagem/formulário de cobranças recorrentes — espelha o módulo backend `recurringcharge`. |
| `core/` (`error.interceptor.ts`) | nenhuma spec dedicada — ver `constitution.md` Princípio VI | Interceptor HTTP que normaliza toda resposta de erro 4xx no objeto `ApiError`, consumido pelos componentes em vez de `HttpErrorResponse` bruto. |
| `shared/` (models, services por entidade, `list-selection.ts`, `bulk-delete.ts`, `bulk-duplicate.ts`, `components/row-actions/`, `components/bulk-actions-bar/`) | 002, 005, 006, 007 | Infraestrutura de UI transversal reaproveitada por toda listagem. Trio `list-selection` + `bulk-delete` + `bulk-actions-bar` introduzido na 002; `row-actions` (ícones de editar/remover por linha) introduzido na 005; seleção em intervalo com Shift generalizada em `list-selection.ts` pela 006; `bulk-duplicate.ts` e extensão de `bulk-actions-bar` para ação extra introduzidos pela 007. |

## Specs sem módulo de código correspondente

| Spec | Situação |
|---|---|
| `010-admin-authentication` | Spec já escrita (`specs/010-admin-authentication/spec.md`), mas ainda não implementada — não há `SecurityConfig`, dependência `spring-boot-starter-security` no backend, nem tela de login/autenticação no frontend até o momento desta revisão. Quando implementada, este índice MUST ganhar entradas correspondentes (provável novo módulo backend, ex. `com.financas.auth`, e área de frontend dedicada). |

## Histórico de Revisões

- **2026-08-05** — Documento criado (constitution.md v1.22.0) e populado a partir de análise direta do codebase (`backend/src/main/java/com/financas/**`, `frontend/src/app/**`) e dos títulos/conteúdo de `specs/001` a `specs/010`. Nenhuma spec foi inferida sem evidência direta no código ou no texto da spec.
