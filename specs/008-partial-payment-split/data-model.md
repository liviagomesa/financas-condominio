# Data Model: Pagamento Parcial de Contas

Esta feature não introduz nenhuma entidade nova, mas altera a estrutura de `Account` (remove o atributo `recurring`, FR-010) e adiciona uma migration (`DROP COLUMN`). O que muda é: (1) a estrutura de `Account` e dos DTOs relacionados perde `recurring`; (2) a lógica de `AccountService.registerPayment`, que agora pode produzir uma segunda `Account` a partir de uma existente; e (3) o contrato de `AccountPaymentRequest`, descritos abaixo.

## `Account` (`backend/src/main/java/com/financas/account/domain/Account.java`) — perde o atributo `recurring`

| Campo | Tipo | Papel no pagamento parcial |
|---|---|---|
| `amount` | `BigDecimal` | Atualizado para o valor pago (conta original truncada) ou para o valor pago quando maior que o devido (overpayment); herdado como "valor devido − valor pago" na nova conta do split |
| `dueDate` | `LocalDate` | Nunca muda na conta original; a nova conta do split herda o mesmo valor (nunca recalculado) |
| `description` | `String` | Ganha o sufixo `- parte N` na primeira divisão de uma linhagem; permanece inalterada numa conta que já tem o sufixo (mesmo quando ela é a que está sendo paga num split subsequente); a nova conta do split sempre recebe `descrição-base + " - parte " + (N+1)` |
| `paymentDate` | `LocalDate` (nullable) | Definido na conta que recebeu o pagamento (integral, truncada por split, ou ajustada por overpayment); permanece `null` na nova conta do split |
| ~~`recurring`~~ | ~~`boolean`~~ | **Removido nesta feature** (FR-010) — campo, parâmetro de construtor, getter/setter e coluna de banco deixam de existir; ver research.md, "Remoção completa do campo `recurring`" |
| `observations` | `String` (nullable) | Inalterado nos casos de pagamento integral/parcial; recebe a nota de excedente (acrescentada, nunca sobrescrita) no caso de overpayment |
| `fund`, `party`, `type` | referências / enum | Copiados sem alteração para a nova conta do split; nunca alterados na conta original |

**Migration nova**: `V14__drop_recurring_from_account.sql` — `ALTER TABLE account DROP COLUMN recurring;`. Sem coluna nova, sem renomeação (ver research.md para o porquê de um `DROP` direto em vez de um `RENAME`).

## DTOs que perdem `recurring` (`backend/src/main/java/com/financas/account/api/`)

| DTO | Mudança |
|---|---|
| `AccountRequest` | Remove o campo `Boolean recurring` (e sua anotação `@NotNull`) |
| `AccountBulkRequest` | Remove o campo `Boolean recurring` (e sua anotação `@NotNull`) |
| `AccountResponse` | Remove o campo `recurring` do record e da fábrica `from(...)` |

`AccountController.create`/`createBulk`/`update` deixam de repassar `request.recurring()` para `AccountService`; `AccountService.create`/`createForGroup`/`update` perdem o parâmetro `boolean recurring`; `AccountService.duplicate` deixa de ler `original.isRecurring()`.

**Operação alterada**: `AccountService.registerPayment(Long id, LocalDate paymentDate, BigDecimal paidAmount)` — substitui a assinatura atual de dois argumentos (único ponto de chamada é `AccountController.pay`). `paidAmount == null` é tratado como "= `account.getAmount()`" (pagamento integral, comportamento idêntico ao método atual). Compara `paidAmount` com o valor devido da conta e segue um dos três ramos:

1. **Igual** (FR-002): `account.setPaymentDate(paymentDate)`; `save`. Nenhuma outra alteração.
2. **Menor** (FR-003/FR-003a/FR-004): renomeia a conta (se ainda não tiver sufixo) ou mantém o nome (se já tiver); `account.setAmount(paidAmount)`; `account.setPaymentDate(paymentDate)`; `save`. Cria uma nova `Account` com `amount = devido − paidAmount`, `dueDate` igual à original, `paymentDate = null`, mesma descrição-base com o próximo número de parte, mesmo `fund`/`party`/`type`/`observations` da original; `save`.
3. **Maior** (FR-005): `account.setAmount(paidAmount)`; `account.setPaymentDate(paymentDate)`; `account.setObservations(observações anteriores + nota de excedente)`; `save`. Nenhuma conta nova.

Se `paidAmount` for `≤ 0` (zero ou negativo), a requisição nunca chega a este método — é rejeitada na validação do DTO (`@Positive`), então nenhum dos três ramos acima executa (FR-008/FR-009).

## `AccountPaymentRequest` (`backend/src/main/java/com/financas/account/api/AccountPaymentRequest.java`) — campo novo

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `paymentDate` | `LocalDate` | Sim (já existente) | `@NotNull` |
| `paidAmount` | `BigDecimal` | Não (novo) | `@Positive` quando informado; `null` = pagamento integral (equivalente ao comportamento atual do endpoint) |

## Estado efêmero de interface (`account/account-list/account-list.ts`) — campo novo

| Campo | Tipo | Descrição |
|---|---|---|
| `paidAmountDraft` | `string` | Valor digitado na caixa de valor pago, pré-preenchido com `String(account.amount)` ao abrir o registro de pagamento (mesmo padrão de `paymentDateDraft`/`amountDraft` já existentes). Só é lido/enviado quando a conta ainda está pendente (`!account.paymentDate`) — no fluxo de "Alterar pagamento" de uma conta já paga, o campo é preenchido mas nunca exibido nem enviado (FR-006). |

**Transições**:
- `startPayment(account)` → `paidAmountDraft` recebe `String(account.amount)`, junto do já existente `paymentDateDraft`.
- `confirmPayment(account)`, com `account.paymentDate == null` (registrando pela primeira vez) → envia `paidAmount: Number(this.paidAmountDraft)` no corpo da requisição; se esse valor for vazio, não-numérico ou `≤ 0`, a chamada é abortada antes do HTTP (mesmo guarda silencioso já usado para `paymentDateDraft` vazio — ver research.md).
- `confirmPayment(account)`, com `account.paymentDate != null` ("Alterar pagamento") → **não** envia `paidAmount` (campo omitido do corpo), preservando o comportamento atual de só alterar a data.
- Após qualquer confirmação bem-sucedida → `this.load()` recarrega a listagem inteira, revelando a conta truncada e (quando houve split) a nova conta do saldo restante, mesmo padrão de recarregamento já usado pelas demais ações da tela.

## Diagrama (referência)

```text
POST /api/accounts/{id}/pay
  AccountPaymentRequest { paymentDate, paidAmount? }
       │
       ▼
AccountService.registerPayment(id, paymentDate, paidAmount)
       │
       ├── paidAmount == devido  ──────────────► 1 save (conta original)
       ├── paidAmount <  devido  ──────────────► 2 saves (original truncada + nova "- parte N+1")
       └── paidAmount >  devido  ──────────────► 1 save (original com valor e observations ajustados)

AccountList (frontend)
  paidAmountDraft   string   — novo, espelha paymentDateDraft já existente
  confirmPayment()  → inclui paidAmount só quando a conta ainda está pendente (FR-006)
```

Nenhum dado novo é persistido além dos campos já existentes de `Account` — a numeração de partes vive inteiramente no texto de `description`, sem coluna dedicada (ver research.md).
