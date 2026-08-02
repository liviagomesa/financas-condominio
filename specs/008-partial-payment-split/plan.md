# Implementation Plan: Pagamento Parcial de Contas

**Branch**: `008-partial-payment-split` | **Date**: 2026-08-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-partial-payment-split/spec.md`

## Summary

No fluxo já existente de "Registrar pagamento" de uma conta pendente (`/accounts`), a caixa de data de pagamento ganha ao lado uma caixa de valor pago, pré-preenchida com o valor total devido. Quando a usuária confirma sem alterar esse valor, nada muda em relação ao comportamento atual. Quando o valor confirmado é menor que o devido, a conta é dividida em duas: a conta original é truncada para o valor pago e marcada como paga, e uma nova conta independente é criada com o saldo restante, mesma data de vencimento, pendente — ambas rotuladas com um sufixo `- parte N` na descrição, numerado sequencialmente (mesmo em splits sucessivos sobre o saldo remanescente, sem duplicar ou renumerar uma parte já rotulada). Quando o valor confirmado é maior que o devido, nenhuma conta nova é criada: o valor da própria conta é ajustado para cima e uma observação registra o excedente, preservando texto já existente nesse campo. Um valor pago igual a zero é ignorado (nenhuma ação). Um único endpoint já existente (`POST /api/accounts/{id}/pay`) cobre os três casos, com um novo campo opcional no corpo da requisição; toda a lógica de decisão (igual/menor/maior, numeração de partes, nota de excedente) fica em `AccountService.registerPayment`.

## Clarifications

Ver [spec.md § Clarifications](./spec.md#clarifications) — sessão 2026-08-02, resolvendo o formato do sufixo de descrição em splits sucessivos: numeração `- parte N` desde a primeira divisão, nunca reatribuída a uma conta que já a possui (refletido em FR-003/FR-003a e neste plano).

## Revisão durante o planejamento (sessão 2026-08-02)

Antes de seguir para `/speckit-tasks`, a usuária pediu para expandir o escopo desta feature: em vez de apenas desmarcar `recurring` nas duas contas resultantes de um split (decisão original de FR-003, já superada), o campo "Recorrente" é removido por completo do sistema — entidade, formulário, persistência e contratos de API (FR-010, ver spec.md § Assumptions e § Clarifications). Motivo dado pela usuária: o campo não decide nenhum comportamento hoje, e a próxima feature planejada por ela é justamente a automação real de lançamentos recorrentes (execução agendada/cron), que será desenhada do zero sem reaproveitar esse campo — mantê-lo até lá só obrigaria o split a tratá-lo como caso especial. Isso expande o footprint da feature de "só o fluxo de pagamento" para "fluxo de pagamento + remoção de um atributo em toda a vertical de Contas", refletido no Technical Context, Constitution Check e Project Structure abaixo (já atualizados para o escopo final, sem seção redundante de "antes/depois").


## Technical Context

**Language/Version**: Java 21 (backend, Spring Boot 4.1.0) + TypeScript ~6.0.2 / Angular 22 (frontend) — reuso integral das versões já adotadas pelas features 001–007 (Constituição, Princípio III); nenhuma mudança de versão.

**Primary Dependencies**: Spring Data JPA + Spring Web (`spring-boot-starter-webmvc`) + Bean Validation (`spring-boot-starter-validation`) no backend; Angular signals (zoneless) + Bootstrap/SCSS no frontend — mesmas dependências já em uso. Nenhuma dependência nova.

**Storage**: PostgreSQL, tabela `account` já existente. Uma migration Flyway nova (`V14__drop_recurring_from_account.sql`) remove a coluna `recurring` (FR-010) — nenhuma coluna nova é adicionada. A numeração `- parte N` continua derivada do texto da própria `description` a cada operação, não persistida como campo separado (ver research.md).

**Testing**: Backend — JUnit 5 + Mockito, casos novos em `AccountServiceTest` cobrindo as três faixas de valor pago (igual/menor/maior que o devido), o valor zero (ignorado), valor negativo (rejeitado), e a numeração de partes em splits sucessivos; os testes já existentes que hoje passam `recurring` para `create`/`createForGroup`/`update`/`duplicate` são ajustados para a assinatura sem esse parâmetro (ajuste mecânico, não regra de negócio nova). Frontend — validação manual via Playwright (devDependency já configurada) da caixa de valor pago e da ausência do campo "Recorrente" no formulário; nenhum teste unitário dedicado aos componentes `AccountList`/`AccountForm`, seguindo o precedente já registrado no `research.md` das features 006/007 (nenhum componente Angular tem hoje teste unitário próprio no projeto); `shared/bulk-duplicate.spec.ts` tem sua fixture ajustada para remover `recurring`.

**Target Platform**: mesma aplicação web já existente (SPA Angular + API REST Spring Boot, infraestrutura local das features 001–007).

**Project Type**: web (frontend Angular + backend Spring Boot, já estabelecidos pela feature 001) — esta feature altera as duas camadas, sem nenhum endpoint novo.

**Performance Goals**: sem metas específicas — uso pessoal, poucas dezenas de lançamentos, uma única chamada HTTP por confirmação de pagamento (mesmo padrão já existente).

**Constraints**: nenhuma dependência nova; a caixa de valor pago MUST aparecer apenas no fluxo de registro de pagamento de uma conta pendente, nunca na edição de data de uma conta já paga (FR-006); o texto do sufixo de descrição MUST seguir literalmente o formato `- parte N` (Assumptions do spec) — não é apenas uma marca livre; o campo "Recorrente" MUST deixar de existir em qualquer tela ou contrato de dados de contas (FR-010).

**Scale/Scope**: 0 endpoints novos (reaproveita `POST /api/accounts/{id}/pay`); 1 migration nova (remove a coluna `recurring`); DTOs alterados — `AccountPaymentRequest` ganha `paidAmount` opcional, `AccountRequest`/`AccountBulkRequest`/`AccountResponse` perdem `recurring`; a entidade `Account` perde o atributo `recurring` (campo, parâmetro de construtor, getter/setter); `AccountService` — `registerPayment` reescrito (lógica igual/menor/maior, numeração de partes, nota de excedente) e `create`/`createForGroup`/`update`/`duplicate` perdem o parâmetro/uso de `recurring`; ~6-8 casos de teste novos em `AccountServiceTest` para `registerPayment` + ajuste dos testes existentes; no frontend, `account-list.ts`/`.html` (caixa de valor pago) e `account-form.ts`/`.html` (remoção do campo "Recorrente") alterados, `account.model.ts` alterado, `bulk-duplicate.spec.ts` com fixture ajustada; 3 user stories, 10 requisitos funcionais (FR-001 a FR-010, incluindo FR-003a).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitetura em Camadas**: PASS. Nenhum pacote novo, nenhuma entidade nova — a remoção de `recurring` e a adição de `paidAmount` acontecem inteiramente dentro dos arquivos já existentes de `com.financas.account` (`api/` e `domain/`), e a migration nova fica em `db/migration` (mesmo diretório de todas as anteriores). No frontend, a mudança fica em `account/account-list/` (fluxo de pagamento) e `account/account-form/` (remoção do campo "Recorrente"), ambos já existentes, mais `shared/models/account.model.ts` — nenhum componente novo, nenhum pacote `shared/` tocado além do model.
- **II. Separação Controller → Service → Repository**: PASS. `AccountController` continua apenas repassando campos de DTOs para `AccountService`, único ponto a chamar `AccountRepository`. `registerPayment` passa a executar até duas escritas (`save` da conta original + `save` da nova conta, no caso de split) — por isso MUST ganhar `@Transactional` (Princípio II: critério objetivo por quantidade de escritas, independentemente do branch executado). `create`, `update` e `duplicate` continuam com uma única escrita cada (perdem apenas o parâmetro `recurring`, sem mudar de comportamento transacional); `createForGroup` continua `@Transactional` como já é hoje (múltiplas escritas em loop, motivo já registrado antes desta feature).
- **III. Stack Técnica Definida**: PASS. Reaproveita integralmente Java 21, Spring Boot 4.1.0, Angular 22, JUnit 5 + Mockito, Flyway e Playwright já adotados — nenhuma dependência nova. Cobertura de teste automatizado da regra de negócio nova (as três faixas de valor pago, valor zero, valor negativo, numeração de partes) via `AccountServiceTest`; a remoção de `recurring` não é uma regra de negócio nova a testar, apenas exige ajustar os testes existentes que hoje passam esse parâmetro para compilar contra a assinatura nova.
- **IV. Convenções de Código e Formatação**: PASS. `isPaid()` continua derivado apenas de `paymentDate` — nenhum campo booleano novo é introduzido para representar "foi dividida" ou "é parte de um split" (informação derivável da própria `description`, ver research.md). `type` (discriminador) nunca é alterado por esta feature. A migration que remove `recurring` é um `DROP COLUMN` simples, não uma renomeação/generalização de entidade — a preferência do Princípio I por `ALTER TABLE ... RENAME` (que pressupõe dado real a preservar) não se aplica aqui; a remoção é explícita e justificada em spec.md (Assumptions/Clarifications), conforme a mesma seção do Princípio I exige para esse tipo de decisão. Nenhum enum novo. Mensagens de validação e a nota de excedente em `observations` MUST estar em português (mensagem ao usuário final).
- **V. Idioma por Tipo de Conteúdo**: PASS. Identificadores de código em inglês (`paidAmount`, `registerPayment`); mensagens de validação e a nota de excedente em português, conforme já é o padrão do projeto.
- **VI. Convenções de API REST**: PASS. Nenhuma rota nova — `POST /api/accounts/{id}/pay` já é a ação de negócio dedicada correta para registrar pagamento (Princípio VI); esta feature estende o corpo da requisição com um campo opcional e remove `recurring` dos corpos/respostas já existentes de `POST`/`POST /bulk`/`PUT`. Resposta `200` de `/pay` continua usando `AccountResponse.from(...)`, mesmo factory já existente (agora sem o campo `recurring`) — a nova conta criada pelo split não é retornada por este endpoint (o frontend já recarrega a listagem inteira após a confirmação). Erros de validação continuam no formato padronizado `{ message, status }`, já tratado pelo `GlobalExceptionHandler` existente — nenhum código novo necessário.

Nenhuma violação da constituição identificada — Complexity Tracking não se aplica.

**Re-check pós-Phase 1**: PASS. `research.md`/`data-model.md`/`contracts/` não revelaram nenhuma decisão de design que contradiga a avaliação acima.

## Project Structure

### Documentation (this feature)

```text
specs/008-partial-payment-split/
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
│   └── V14__drop_recurring_from_account.sql   # novo — DROP COLUMN recurring
└── src/main/java/com/financas/account/
    ├── api/
    │   ├── AccountPaymentRequest.java     # alterado — novo campo opcional paidAmount (BigDecimal, @Positive)
    │   ├── AccountRequest.java            # alterado — remove o campo recurring
    │   ├── AccountBulkRequest.java        # alterado — remove o campo recurring
    │   ├── AccountResponse.java           # alterado — remove o campo recurring (record + from())
    │   └── AccountController.java         # alterado — repassa request.paidAmount() para o Service; para de repassar recurring
    └── domain/
        ├── Account.java                   # alterado — remove o campo/getter/setter/parâmetro de construtor recurring
        └── AccountService.java            # alterado — registerPayment reescrito (split/overpayment/numeração), @Transactional;
                                            # create/createForGroup/update/duplicate perdem o parâmetro/uso de recurring
backend/src/test/java/com/financas/account/domain/AccountServiceTest.java  # alterado — casos novos de registerPayment;
                                                                            # testes existentes ajustados à assinatura sem recurring

frontend/
└── src/app/
    ├── shared/
    │   ├── models/account.model.ts        # alterado — AccountPaymentRequest ganha paidAmount?: number;
    │   │                                   # Account/AccountRequest/AccountBulkRequest perdem recurring
    │   └── bulk-duplicate.spec.ts         # alterado — fixture sem recurring
    └── account/
        ├── account-list/
        │   ├── account-list.ts     # alterado — paidAmountDraft, startPayment/confirmPayment atualizados (FR-001 a FR-009);
        │   │                       # para de enviar recurring na edição inline de valor
        │   └── account-list.html   # alterado — nova caixa de valor pago, condicional a conta ainda pendente (FR-006)
        └── account-form/
            ├── account-form.ts     # alterado — remove o FormControl e o mapeamento de recurring
            └── account-form.html   # alterado — remove a checkbox/label "Recorrente"
```

**Structure Decision**: Web application (mesma estrutura das features 001–007, sem opções alternativas). O backend não ganha nenhum pacote, entidade ou endpoint novo — toda a mudança fica dentro do pacote `account` já existente, reaproveitando a ação `/pay` já modelada e removendo `recurring` via uma única migration adicional. O frontend concentra a mudança em `account/account-list/` (fluxo de pagamento) e `account/account-form/` (remoção do campo "Recorrente"), mais o model compartilhado — nenhum outro componente (`party-list`, `fund-list`, `group-list`, `bulk-actions-bar`) é tocado, já que esta feature não é uma ação em lote.

## Complexity Tracking

*Não se aplica — nenhuma violação da constituição identificada no Constitution Check dentro do escopo desta feature.*
