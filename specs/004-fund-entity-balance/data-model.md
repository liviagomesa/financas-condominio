# Data Model: Fundos como Entidade e Visualização de Saldo Real

## Fund (Fundo) — nova entidade, converte o enum `Fund` existente

Representa uma reserva financeira do condomínio (ex.: Piscina, Jardim Piscina, Jardim Lateral)
usada para categorizar lançamentos de contas a receber e a pagar.

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `id` | Long (PK, auto-gerado) | — | Gerado pelo banco |
| `name` | String | Sim | Único, comparação case-insensitive e sem espaços nas extremidades (FR-001/FR-002) — mesmo mecanismo de `Unit.identifier` |
| `initialBalance` | BigDecimal (`NUMERIC(10,2)`) | Sim | Sem restrição de sinal — pode ser negativo, zero ou positivo (FR-010); editável a qualquer momento (FR-003); padrão zero quando não informado no formulário do frontend |

**Campo computado (não persistido)**: `realBalance` — presente apenas em `FundResponse`
(camada `api/`), calculado por `FundService.calculateRealBalance(Fund)` a partir de
`initialBalance` e dos lançamentos (`Account`) vinculados ao fundo (ver fórmula abaixo).

**Relacionamentos**:
- `Fund` (1) ──< (N) `Account` — todo lançamento de conta a receber ou a pagar referencia
  exatamente um fundo, obrigatoriamente (FR-006).

**Regras de negócio**:
- FR-001/FR-002: nome obrigatório e único — `FundService.validateNotDuplicate` usa
  `FundRepository.findByNormalizedName` (mesmo padrão de `UnitService.validateNotDuplicate`).
- FR-004/FR-005: remoção bloqueada quando houver ao menos um `Account` vinculado —
  `accountRepository.existsByFundId(id)`, lançando `FundHasAccountsException`; permitida quando
  não houver nenhum.
- FR-008/FR-010/FR-011: saldo real = `initialBalance` + soma dos `Account` com
  `type = RECEIVABLE` e `paymentDate != null` vinculados ao fundo − soma dos `Account` com
  `type = PAYABLE` e `paymentDate != null` vinculados ao fundo. Lançamentos sem `paymentDate`
  (em aberto) nunca entram na soma.
- FR-012: nenhuma validação impede o saldo real de ficar negativo; puramente informativo.

**Persistência**: nova tabela `fund`, com índice único sobre `LOWER(TRIM(name))` (mesmo padrão
de `unit_identifier_normalized_idx`).

## Account (Conta) — impacto desta feature

`Account.fund` deixa de ser um valor de enum fixo (`com.financas.account.domain.Fund`, removido)
e passa a ser uma referência obrigatória à nova entidade `Fund`
(`com.financas.fund.domain.Fund`).

| Campo | Tipo (antes) | Tipo (depois) | Obrigatório |
|---|---|---|---|
| `fund` | enum `Fund` (`POOL`\|`POOL_GARDEN`\|`SIDE_GARDEN`), `@Enumerated(EnumType.STRING)` | Referência a `Fund` (FK `fund_id`, `@ManyToOne(optional = false)`) | Sim (FR-006) — inalterado |

Nenhum outro campo de `Account` é afetado por esta feature.

**Regras de negócio (impacto)**:
- FR-006: todo lançamento continua vinculado a exatamente um fundo — agora validado por FK real
  (`fund_id NOT NULL REFERENCES fund(id)`) em vez de um valor fixo de enum;
  `AccountService.create`/`update`/`createForAllUnits` resolvem o fundo via
  `findFundOrThrow(fundId)`, lançando `NotFoundException` (404) se não existir.
- Nenhuma nova regra de validação é adicionada a `AccountService` por causa do saldo do fundo
  (FR-012 — sem bloqueio por saldo negativo).

**Persistência**: coluna `fund` (antes `VARCHAR(20)`, valor de enum) é substituída por
`fund_id BIGINT NOT NULL REFERENCES fund (id)`, com índice em `fund_id`. A tabela `account` é
truncada antes da troca de coluna (banco apenas de desenvolvimento, sem dado a preservar — ver
research.md).

## Diagrama de relacionamento

```text
Fund (1) ──────< (N) Account     (fund_id NOT NULL — toda conta pertence a exatamente um fundo)
Unit (1) ──────< (N) Account     (type = RECEIVABLE, inalterado desde a feature 003)
Supplier (1) ──< (N) Account     (type = PAYABLE, inalterado desde a feature 003)

Fund
  id
  name              (único, normalizado)
  initial_balance

Account
  id
  type              (RECEIVABLE | PAYABLE)
  amount
  due_date
  description
  fund_id           (FK → Fund.id, obrigatório — antes: coluna `fund`, valor de enum)
  recurring         (boolean)
  unit_id           (FK → Unit.id, nullable — obrigatório se type = RECEIVABLE)
  supplier_id       (FK → Supplier.id, nullable — obrigatório se type = PAYABLE)
  payment_date      (nullable — não nulo = pago/recebido)
  observations      (nullable, texto livre)
```

## Estados e transições

`Fund` não tem estado/ciclo de vida próprio além de existir ou ser removido (bloqueado enquanto
houver `Account` vinculado). O saldo real não é um estado persistido — é recalculado a cada
consulta a partir do estado atual de `initialBalance` e dos lançamentos vinculados, refletindo
automaticamente qualquer mudança (novo lançamento, pagamento registrado/estornado, edição do
saldo inicial) sem necessidade de sincronização manual (Edge Cases do spec).

## Fórmula do saldo real (referência)

```text
realBalance(fund) = fund.initialBalance
                   + Σ amount de Account(fund, type=RECEIVABLE, paymentDate != null)
                   − Σ amount de Account(fund, type=PAYABLE, paymentDate != null)
```
