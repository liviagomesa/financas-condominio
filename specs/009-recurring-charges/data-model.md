# Data Model: Geração Automática de Contas Recorrentes

## `RecurringCharge` (nova) — `backend/src/main/java/com/financas/recurringcharge/domain/RecurringCharge.java`

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `Long` | Gerado (`IDENTITY`) |
| `type` | `AccountType` (reaproveitado de `com.financas.account.domain`) | Obrigatório na criação, imutável na edição (FR-012) — mesmo `@Enumerated(EnumType.STRING)` de `Account.type` |
| `amount` | `BigDecimal` | Não-negativo; **pode ser `0.00`** (FR-002) — `@PositiveOrZero` no DTO + `validateNonNegativeAmount` no `Service` (ver research.md) |
| `dueDay` | `Integer` | 1 a 31 (`@Min(1) @Max(31)` no DTO); combinado com o mês/ano-alvo no momento da geração, ajustado para o último dia do mês quando o mês-alvo for mais curto (FR-007) |
| `description` | `String` | Obrigatório |
| `fund` | `Fund` (`@ManyToOne`, obrigatório) | Mesma referência já usada por `Account.fund` |
| `party` | `Party` (`@ManyToOne`, obrigatório) | Uma cobrança recorrente pertence a exatamente uma contraparte — nunca um grupo (FR-003: grupo só existe no momento do cadastro, vira N linhas) |
| `observations` | `String` (nullable) | Livre |
| `deactivatedAt` | `LocalDate` (nullable) | `null` enquanto a linha está ativa; preenchido com a data em que a linha foi substituída por uma edição (FR-008) ou removida (FR-009) — em ambos os casos a linha nunca é apagada fisicamente. `isActive()` é um método derivado (`deactivatedAt == null`), não um campo persistido à parte — mesmo padrão já usado por `Account.isPaid()`/`paymentDate` (Princípio IV: estado binário derivável de outro campo não deve ser duplicado como booleano persistido). Usado tanto para decidir se a cobrança participa da geração mensal quanto para aparecer na listagem (FR-011) |
| `lastGenerationFailed` | `boolean` (default `false`) | `true` quando a tentativa de geração mais recente falhou para esta cobrança (FR-017); usado só para exibir o aviso na tela de gerenciamento — não afeta nenhuma regra de negócio além disso |

**Migration nova**: `V15__create_recurring_charge_table.sql`

```sql
CREATE TABLE recurring_charge (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    due_day INTEGER NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    description VARCHAR(255) NOT NULL,
    fund_id BIGINT NOT NULL REFERENCES fund (id),
    party_id BIGINT NOT NULL REFERENCES party (id),
    observations TEXT,
    deactivated_at DATE,
    last_generation_failed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX recurring_charge_fund_id_idx ON recurring_charge (fund_id);
CREATE INDEX recurring_charge_party_id_idx ON recurring_charge (party_id);
CREATE INDEX recurring_charge_deactivated_at_idx ON recurring_charge (deactivated_at);
```

## `Account` (existente) — ganha uma FK opcional

| Campo novo | Tipo | Papel |
|---|---|---|
| `recurringCharge` | `RecurringCharge` (`@ManyToOne`, nullable) | Preenchido só pelo processo de geração automática (via setter — sem novo parâmetro de construtor, ver research.md); usado exclusivamente para a checagem de idempotência (FR-006) e para rastreabilidade. O valor/descrição/demais campos da conta gerada são cópias feitas no momento da geração — nunca mudam retroativamente se a cobrança recorrente for editada ou removida depois (FR-008/FR-009). |

**Migration nova**: `V16__add_recurring_charge_id_to_account.sql`

```sql
ALTER TABLE account ADD COLUMN recurring_charge_id BIGINT REFERENCES recurring_charge (id);

CREATE INDEX account_recurring_charge_id_idx ON account (recurring_charge_id);
```

**Método novo em `AccountRepository`** (`domain/`, implementado em `infra/AccountRepositoryImpl` via `AccountJpaRepository`):

```java
boolean existsByRecurringChargeIdAndDueDateBetween(Long recurringChargeId, LocalDate start, LocalDate end);
```

## Operações de `RecurringChargeService`

| Operação | Escritas | `@Transactional`? | Efeito |
|---|---|---|---|
| `create(type, amount, dueDay, description, fundId, partyId, observations)` | 1 (`save`) | Não | Nova linha, `deactivatedAt=null` (ativa), `lastGenerationFailed=false` |
| `createForGroup(type, amount, dueDay, description, fundId, groupId, observations)` | N (`save` por integrante) | **Sim** (mesmo critério de `AccountService.createForGroup`) | Uma linha por integrante do grupo (FR-003); rejeita grupo vazio (`EmptyGroupException`, cópia dedicada em `recurringcharge.domain` — ver research.md) |
| `update(id, type, amount, dueDay, description, fundId, partyId, observations)` | 2 (`save` da linha antiga inativada + `save` da linha nova) | **Sim** | Valida `type` inalterado (`RecurringChargeTypeChangeNotAllowedException`); cria nova linha ativa (`deactivatedAt=null`) com os valores atualizados; marca a linha antiga `deactivatedAt=LocalDate.now()`. Retorna a linha nova, com `id` diferente do informado (FR-008) |
| `delete(id)` | 1 (`save` com `deactivatedAt=LocalDate.now()`) | Não | Soft delete — nunca `deleteById` (FR-009) |
| `findAll()` | — | — | Filtra `isActive()` em memória (mesmo padrão de `AccountService.findAll`, dado o volume pequeno), ordenado por `description`, depois `id` (critério determinístico, Princípio VI) |
| `findById(id)` | — | — | Sem filtro de `isActive()` — usado pelo fluxo de edição, que só navega para linhas ativas |

## Operações de `RecurringChargeGenerationService`

| Método | Gatilho | Descrição |
|---|---|---|
| `generatePendingAccounts()` | `@Scheduled(cron = "0 0 6 25 * *", zone = "America/Sao_Paulo")` **e** `@EventListener(ApplicationReadyEvent.class)` (mesmo método, dois gatilhos — ver research.md) | Calcula o mês-alvo (`resolveMostRecentDueTargetMonth`); para cada `RecurringCharge` com `isActive()` verdadeiro, checa idempotência (FR-006) e, se necessário, tenta gerar (`generateOne`), isolando falhas por cobrança (FR-016) |
| `resolveMostRecentDueTargetMonth(LocalDate reference)` (privado) | — | `reference.getDayOfMonth() >= 25 ? YearMonth.from(reference).plusMonths(1) : YearMonth.from(reference)` |
| `generateOne(RecurringCharge charge, YearMonth targetMonth)` (privado) | — | Resolve `dueDate` (dia ajustado ao mês-alvo, FR-007); constrói `new Account(...)`, `setRecurringCharge(charge)`, salva; limpa `lastGenerationFailed` se necessário |

## Diagrama (referência)

```text
@Scheduled (dia 25, 6h BRT)  ──┐
                                ├──► RecurringChargeGenerationService.generatePendingAccounts()
ApplicationReadyEvent  ────────┘         │
                                          ├── resolveMostRecentDueTargetMonth(LocalDate.now())
                                          │
                                          └── para cada RecurringCharge ativa:
                                                │
                                                ├── existsByRecurringChargeIdAndDueDateBetween? ──► sim: limpa flag se preciso, pula
                                                │
                                                └── não: generateOne()
                                                          ├── sucesso → Account salva (recurringCharge = molde), flag = false
                                                          └── falha   → flag = true (isolado — demais cobranças continuam)
```

## DTOs (`backend/src/main/java/com/financas/recurringcharge/api/`)

Ver [contracts/api.md](./contracts/api.md) para o formato completo de request/response.

| DTO | Campos |
|---|---|
| `RecurringChargeRequest` | `type`, `amount`, `dueDay`, `description`, `fundId`, `partyId`, `observations` — usado em `POST`/`PUT` |
| `RecurringChargeBulkRequest` | Igual, exceto `groupId` no lugar de `partyId` — usado em `POST /bulk` |
| `RecurringChargeResponse` | `id`, `type`, `amount`, `dueDay`, `description`, `fund` (`FundResponse`), `party` (`PartyResponse`), `observations`, `lastGenerationFailed` |

## Estado efêmero de interface (`recurring-charge/recurring-charge-form/recurring-charge-form.ts`)

Mesma estrutura de `AccountForm` (`bulkMode` signal alternando `partyId`/`groupId`, `type` desabilitado em modo edição) — padrão já estabelecido em `frontend/src/app/account/account-form/account-form.ts`, reaproveitado aqui sem alteração de forma.
