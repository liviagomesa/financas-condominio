# Implementation Plan: Geração Automática de Contas Recorrentes

**Branch**: `009-recurring-charges` | **Date**: 2026-08-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/009-recurring-charges/spec.md`

## Summary

Uma nova entidade, `RecurringCharge` ("cobrança/pagamento recorrente"), representa um lançamento fixo e recorrente por contraparte (nunca compartilhado por várias ao mesmo tempo). Uma tela de gerenciamento (`/recurring-charges`) permite cadastrar (para uma contraparte específica ou em lote para um grupo), editar, remover e listar essas cobranças, reaproveitando os padrões de UI já estabelecidos (seleção múltipla, ações de linha). Um processo automático — agendado para o dia 25 de cada mês às 6h de Brasília, e também reexecutado na inicialização do backend como recuperação para ciclos perdidos — gera, para cada cobrança ativa, uma `Account` com vencimento no mês seguinte, referenciando de volta a cobrança que a originou (idempotência por essa referência). Editar uma cobrança recorrente nunca altera contas já geradas: a edição cria uma nova linha ativa e inativa a anterior (sem contas afetadas). Remover é soft delete. A geração isola falhas por cobrança (uma falha não bloqueia as demais do mesmo ciclo) e sinaliza a falha com um aviso visível na tela de gerenciamento, que desaparece assim que uma tentativa seguinte tiver sucesso.

## Technical Context

**Language/Version**: Java 21 (backend, Spring Boot 4.1.0) + TypeScript ~6.0.2 / Angular 22 (frontend) — reuso integral das versões já adotadas pelas features 001–008 (Constituição, Princípio III); nenhuma mudança de versão.

**Primary Dependencies**: Spring Data JPA + Spring Web (`spring-boot-starter-webmvc`) + Bean Validation (`spring-boot-starter-validation`) + Spring Scheduling (`@EnableScheduling`/`@Scheduled`, já incluído em `spring-boot-starter` — nenhuma dependência Maven nova, só habilitar a anotação) no backend; Angular signals (zoneless) + Bootstrap/SCSS + `bootstrap-icons` no frontend — mesmas dependências já em uso. Nenhuma dependência nova no `pom.xml`/`package.json`.

**Storage**: PostgreSQL. Duas migrations Flyway novas: `V15__create_recurring_charge_table.sql` (tabela `recurring_charge`) e `V16__add_recurring_charge_id_to_account.sql` (FK opcional em `account`) — ver data-model.md.

**Testing**: Backend — JUnit 5 + Mockito, `RecurringChargeServiceTest` e `RecurringChargeGenerationServiceTest` novos, mais casos novos em `PartyServiceTest`/`FundServiceTest` para o bloqueio de remoção (ver research.md para a lista completa de casos). Frontend — validação manual via Playwright (devDependency já configurada), sem testes unitários dedicados a componentes novos (mesmo precedente das features 006/007/008).

**Target Platform**: mesma aplicação web já existente (SPA Angular + API REST Spring Boot). O processo de geração roda como parte do próprio backend (sem worker/processo separado) — decisão relevante dado o plano de hospedagem em PaaS (não mais só local), ver Clarifications no spec.md.

**Project Type**: web (frontend Angular + backend Spring Boot, já estabelecidos pela feature 001) — esta feature adiciona um domínio novo (`recurringcharge`) e altera `account` (FK nova) e `party`/`fund` (bloqueio de remoção).

**Performance Goals**: sem metas específicas — uso pessoal, poucas dezenas de cobranças recorrentes e contas geradas por mês; o processo de geração roda no máximo uma vez por dia (agendamento) mais uma vez por reinicialização do backend (recuperação), nunca sob carga de requisição HTTP direta da usuária.

**Constraints**: o processo de geração MUST isolar falhas por cobrança (FR-016) — por isso não pode ser uma única operação `@Transactional` sobre o lote inteiro (ver research.md, divergência deliberada do critério padrão do Princípio II); o agendamento MUST usar o fuso horário de Brasília explicitamente (`zone = "America/Sao_Paulo"` no `@Scheduled`), independente do fuso do servidor de hospedagem; a edição de uma cobrança recorrente MUST preservar contas já geradas (FR-008), nunca sobrescrevendo a linha vigente in-place.

**Scale/Scope**: 1 entidade nova (`RecurringCharge`) + 1 pacote novo (`com.financas.recurringcharge`, `api/`+`domain/`+`infra/`); 2 migrations novas; 6 endpoints novos (`GET`, `GET /{id}`, `POST`, `POST /bulk`, `PUT /{id}`, `DELETE /{id}`); `Account` ganha 1 campo (FK opcional) + 1 método de repositório novo; `PartyService`/`FundService` ganham checagem de remoção nova + 1 exception cada; 2 componentes de frontend novos (`recurring-charge-list`, `recurring-charge-form`) reaproveitando o trio `list-selection`/`bulk-delete`/`bulk-actions-bar`/`row-actions` já existente; 5 user stories, 18 requisitos funcionais (FR-001 a FR-018).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitetura em Camadas**: PASS. Pacote novo `com.financas.recurringcharge` segue exatamente a estrutura `api/`/`domain/`/`infra/` já estabelecida — `RecurringCharge` (entidade), `RecurringChargeRepository` (interface) + `RecurringChargeService`/`RecurringChargeGenerationService` em `domain/`; `RecurringChargeJpaRepository`/`RecurringChargeRepositoryImpl` em `infra/`; `RecurringChargeController`/DTOs em `api/`. Exceptions de regra de negócio específica (`RecurringChargeTypeChangeNotAllowedException`, `EmptyGroupException` dedicada) ficam em `recurringcharge.domain`; `PartyHasActiveRecurringChargesException`/`FundHasActiveRecurringChargesException` ficam nos domains de `Party`/`Fund` respectivamente (regra específica daquelas entidades, não de `RecurringCharge`) — nenhuma vai para `shared/`. A associação `Group.members` (`@ManyToMany EAGER`) já existente é reaproveitada sem alteração pelo `createForGroup` novo. O estado ativo/inativo de `RecurringCharge` é derivado de `deactivatedAt` (`isActive()`), não um booleano `active` persistido à parte — segue diretamente o mesmo padrão já usado por `Account.isPaid()`/`paymentDate` (ver Princípio IV abaixo e research.md); `lastGenerationFailed` continua um booleano persistido próprio, por não ser derivável de nenhum outro campo já existente.
- **II. Separação Controller → Service → Repository**: PASS, com uma divergência deliberada e documentada. `RecurringChargeController` só repassa DTOs para `RecurringChargeService`/`RecurringChargeGenerationService`, nunca chama Repository diretamente. `createForGroup` e `update` são `@Transactional` pelo critério objetivo padrão (≥2 escritas numa operação de negócio única). **Divergência**: `RecurringChargeGenerationService.generatePendingAccounts()` (o laço sobre todas as cobranças ativas) e `generateOne()` (2 escritas: conta + flag) deliberadamente **não** são `@Transactional`, apesar de executarem múltiplas escritas — porque o próprio FR-016 (isolamento por cobrança, decidido na sessão de clarificação) exige que a falha de uma cobrança não desfaça as demais já salvas com sucesso no mesmo ciclo, o que uma transação única sobre o lote inteiro romperia diretamente. Ver research.md ("Isolamento por cobrança exige que o método orquestrador NÃO seja `@Transactional`") para a justificativa completa — não é uma omissão, é um requisito desta feature específica se sobrepondo ao critério genérico do princípio.
- **III. Stack Técnica Definida**: PASS. Reaproveita integralmente Java 21, Spring Boot 4.1.0 (incluindo `@Scheduled`, já parte do `spring-boot-starter` sem dependência nova), Angular 22, JUnit 5 + Mockito, Flyway e Playwright já adotados. Cobertura de teste automatizado das regras de negócio novas (versionamento, soft delete, idempotência, isolamento, ajuste de dia de vencimento, bloqueio de remoção) via `RecurringChargeServiceTest`/`RecurringChargeGenerationServiceTest`/ajustes em `PartyServiceTest`/`FundServiceTest`, conforme exigido pelo Princípio III para toda regra de negócio nova.
- **IV. Convenções de Código e Formatação**: PASS. `type` é obrigatório na criação e imutável na edição (FR-012), reforçado no `Service` (`RecurringChargeTypeChangeNotAllowedException`), mesmo padrão de `Account.type`. Enum reaproveitado (`AccountType`) já usa `@Enumerated(EnumType.STRING)`. O estado ativo/inativo (booleano, do ponto de vista de negócio) é modelado como `deactivatedAt` (`LocalDate` nullable) com `isActive()` derivado — exatamente o caso que este princípio exige ("estado binário inteiramente derivável da presença/ausência de outro campo MUST ser derivado desse campo, nunca duplicado como booleano persistido"), mesmo padrão de `Account.isPaid()`/`paymentDate`; `lastGenerationFailed` permanece um booleano persistido próprio, por não ser derivável de nenhum campo já existente. Datas: `dueDay` é um `Integer` (dia do mês, não uma data completa) por design do próprio spec — não um campo de data ISO-8601, então a convenção de `LocalDate`/formato brasileiro não se aplica a ele; `deactivatedAt` e `Account.dueDate` gerado continuam `LocalDate` (persistência/API em ISO-8601), sem mudança de convenção. Nomes de variáveis/classes/tabelas em inglês (`RecurringCharge`, `recurring_charge`, `dueDay`); mensagens de erro internas em inglês (logs da falha de geração); mensagens ao usuário final em português (exceptions HTTP).
- **V. Idioma por Tipo de Conteúdo**: PASS. Identificadores em inglês; exceptions/log da falha de geração em inglês; mensagens de erro de API em português; spec.md/plan.md/data-model.md/research.md em português (Spec Kit).
- **VI. Convenções de API REST**: PASS. `/api/recurring-charges` segue o padrão plural já usado; `POST /bulk` reaproveita exatamente o padrão já definido para criação em massa vinculada a outro recurso (grupo) — mesmo formato de `POST /api/accounts/bulk`. Nenhuma ação de negócio dedicada nova (`/{id}/ação`) é necessária — cadastro, edição e remoção já cobrem o ciclo de vida manual; a geração automática não tem endpoint HTTP próprio (roda internamente, sem gatilho manual, conforme Assumptions do spec). Listagem (`GET /api/recurring-charges`) define ordenação padrão explícita e determinística (`description`, depois `id`), mesmo critério exigido pelo Princípio VI. Resposta de erro `409` para grupo vazio e para bloqueio de remoção de parte/fundo segue o formato padronizado já produzido pelo `GlobalExceptionHandler` existente — nenhum código novo de tratamento de erro necessário. `RecurringChargeResponse` expõe `from(RecurringCharge, FundResponse)` — mesma assinatura de `AccountResponse.from(Account, FundResponse)` —, montando `PartyResponse` internamente a partir de `charge.getParty()`, embutindo os DTOs completos de `Fund`/`Party` na resposta, nunca só o id.

Nenhuma violação não-justificada identificada. A única divergência (Princípio II, geração sem `@Transactional` no nível do lote) está documentada acima e detalhada em research.md/Complexity Tracking, com justificativa direta em outro requisito explícito desta mesma feature (FR-016).

**Re-check pós-Phase 1**: PASS. `research.md`/`data-model.md`/`contracts/api.md` não revelaram nenhuma decisão de design adicional que contradiga a avaliação acima — a divergência do Princípio II permanece a única, já prevista antes do desenho detalhado.

## Project Structure

### Documentation (this feature)

```text
specs/009-recurring-charges/
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
├── src/main/resources/db/migration/
│   ├── V15__create_recurring_charge_table.sql   # novo — tabela recurring_charge
│   └── V16__add_recurring_charge_id_to_account.sql  # novo — FK opcional em account
└── src/main/java/com/financas/
    ├── FinancasBackendApplication.java          # alterado — ganha @EnableScheduling
    ├── account/domain/
    │   ├── Account.java                         # alterado — campo/getter/setter recurringCharge (opcional)
    │   └── AccountRepository.java                # alterado — existsByRecurringChargeIdAndDueDateBetween
    ├── account/infra/
    │   ├── AccountJpaRepository.java             # alterado — query derivada nova
    │   └── AccountRepositoryImpl.java            # alterado — implementação do método novo
    ├── party/domain/
    │   ├── PartyService.java                     # alterado — delete() checa cobrança recorrente ativa
    │   └── PartyHasActiveRecurringChargesException.java  # novo
    ├── fund/domain/
    │   ├── FundService.java                      # alterado — delete() checa cobrança recorrente ativa
    │   └── FundHasActiveRecurringChargesException.java   # novo
    └── recurringcharge/                          # pacote novo
        ├── api/
        │   ├── RecurringChargeController.java
        │   ├── RecurringChargeRequest.java
        │   ├── RecurringChargeBulkRequest.java
        │   └── RecurringChargeResponse.java
        ├── domain/
        │   ├── RecurringCharge.java
        │   ├── RecurringChargeRepository.java
        │   ├── RecurringChargeService.java             # CRUD + versionamento por edição + soft delete
        │   ├── RecurringChargeGenerationService.java    # @Scheduled + ApplicationReadyEvent + geração
        │   ├── RecurringChargeTypeChangeNotAllowedException.java
        │   ├── InvalidRecurringChargeAmountException.java
        │   └── EmptyGroupException.java                 # cópia dedicada (ver research.md)
        └── infra/
            ├── RecurringChargeJpaRepository.java
            └── RecurringChargeRepositoryImpl.java

backend/src/test/java/com/financas/
├── recurringcharge/domain/
│   ├── RecurringChargeServiceTest.java           # novo
│   └── RecurringChargeGenerationServiceTest.java # novo
├── party/domain/PartyServiceTest.java            # alterado — casos novos de bloqueio
└── fund/domain/FundServiceTest.java              # alterado — casos novos de bloqueio

frontend/
└── src/app/
    ├── app.routes.ts                              # alterado — rotas /recurring-charges*
    ├── app.html                                   # alterado — item de menu novo
    ├── shared/models/recurring-charge.model.ts    # novo
    ├── shared/services/recurring-charge.service.ts # novo
    └── recurring-charge/                          # pacote novo, espelhando account/
        ├── recurring-charge-list/
        │   ├── recurring-charge-list.ts     # reaproveita list-selection, bulk-delete, bulk-actions-bar, row-actions
        │   ├── recurring-charge-list.html   # badge de aviso quando lastGenerationFailed
        │   └── recurring-charge-list.scss
        └── recurring-charge-form/
            ├── recurring-charge-form.ts     # bulkMode (parte específica/grupo), type desabilitado na edição
            ├── recurring-charge-form.html
            └── recurring-charge-form.scss
```

**Structure Decision**: Web application (mesma estrutura das features 001–008, sem opções alternativas). O backend ganha um pacote de domínio novo e completo (`recurringcharge`, com `api/`/`domain/`/`infra/`), seguindo à risca o Princípio I; toca `account` só para adicionar a FK opcional e o método de repositório de idempotência, e `party`/`fund` só para o bloqueio de remoção (FR-014) — nenhuma dessas duas entidades ganha comportamento novo além disso. O frontend espelha a estrutura já usada por `account/` (`-list`/`-form`, reaproveitando o trio de seleção/remoção em lote e `row-actions`), sem introduzir nenhum padrão de componente novo.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| `RecurringChargeGenerationService.generatePendingAccounts()`/`generateOne()` executam múltiplas escritas sem `@Transactional` (Princípio II) | FR-016 (isolamento por cobrança, decidido na sessão de clarificação) exige que a falha de geração de uma cobrança não desfaça as contas de outras cobranças já geradas com sucesso no mesmo ciclo | Uma única `@Transactional` sobre o laço inteiro romperia FR-016 diretamente (rollback-only desfaria todas as escritas do ciclo); extrair `generateOne` para um segundo bean só para viabilizar `@Transactional` via proxy (evitando o problema de auto-invocação) foi rejeitado por adicionar uma classe inteira só para uma anotação, num caso cujo pior risco de inconsistência é cosmético (flag `lastGenerationFailed` presa, autocorrigível no próximo ciclo bem-sucedido — ver research.md) |
