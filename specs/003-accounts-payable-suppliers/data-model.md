# Data Model: Contas a Pagar, Fornecedores e Unificação de Contas

## Account (Conta) — generaliza `Receivable` da feature 002

Representa um valor a receber de uma unidade ou a pagar a um fornecedor.

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `id` | Long (PK, auto-gerado) | — | Mesma sequência de `receivable.id` (preservada via `RENAME TABLE`) |
| `type` | enum `AccountType` (`RECEIVABLE`, `PAYABLE`) | Sim | Definido na criação; imutável na edição (FR-006/FR-015) — `PUT` com `type` diferente do atual é rejeitado (400) |
| `amount` | BigDecimal (`NUMERIC(10,2)`) | Sim | Não pode ser negativo — zero é aceito (FR-008), para os dois tipos |
| `dueDate` | LocalDate | Sim | Sem restrição de passado/futuro; ISO-8601 na API (`yyyy-MM-dd`), conversão para `dd/MM/yyyy` é responsabilidade do frontend (Princípio IV) |
| `description` | String | Sim | Não pode ser vazia/branca |
| `fund` | enum `Fund` (`POOL`, `POOL_GARDEN`, `SIDE_GARDEN`) — renomeado de `TargetAccount`/`targetAccount` | Sim | Um dos três valores fixos, para os dois tipos (FR-022); persistido como `VARCHAR` (`EnumType.STRING`) |
| `recurring` | boolean | Não (sempre tem valor, padrão `false`) | Herdado da feature 002 (FR-019 desta feature); mesmo significado para os dois tipos |
| `unit` | Referência a `Unit` (FK `unit_id`, **nullable**) | Sim quando `type = RECEIVABLE`; deve ser nulo quando `type = PAYABLE` | Validado em `AccountService`, reforçado por `CHECK` no banco (FR-007) |
| `supplier` | Referência a `Supplier` (FK `supplier_id`, nullable) | Sim quando `type = PAYABLE`; deve ser nulo quando `type = RECEIVABLE` | Idem — validado em `AccountService` + `CHECK` no banco (FR-007) |
| `paymentDate` | LocalDate (nullable) | Não — opcional a qualquer momento (criação, edição, ou `POST /{id}/pay`) | Presença = paga; ausência = pendente. Mesma regra da feature 002 (FR-013), para os dois tipos |
| `observations` | String/Text (nullable) | Não | Texto livre, sem limite de formato específico (FR-018, novo campo desta feature) |

**Relacionamentos**:
- `Unit` (feature 001) possui zero ou mais `Account` do tipo `RECEIVABLE` (`1:N`).
- `Supplier` (nova) possui zero ou mais `Account` do tipo `PAYABLE` (`1:N`).
- Um lançamento em lote (US2 da feature 002, restrito a `RECEIVABLE` por FR-009 desta feature) gera um `Account` independente por unidade existente no momento da ação — sem entidade de "lote".

**Regras de negócio**:
- FR-006/FR-015: `type` obrigatório na criação, imutável na edição — `AccountService.update()` rejeita (400) uma tentativa de enviar um `type` diferente do já persistido.
- FR-007: exatamente uma contraparte preenchida, compatível com `type` — validado no `AccountService` (regra cruzada entre campos) e reforçado por `CHECK (account_type_counterparty_check)` no banco.
- FR-008: `amount` não pode ser negativo, na criação e na edição, para os dois tipos; zero é um valor válido (uso como lembrete sem valor definido ainda) — diferente da regra original da feature 002, que rejeitava zero.
- FR-009: lançamento em lote (`POST /api/accounts/bulk`) continua exclusivo de `RECEIVABLE` — sem campo `type`/`supplierId` no corpo da requisição de lote.
- FR-012: filtro por `type` na listagem (`GET /api/accounts?type=`).
- FR-013/FR-014: `paymentDate` e a lógica de "vencido" (pendente + `dueDate` no passado) são idênticas para os dois tipos — herdadas sem alteração da feature 002.
- FR-018: `observations` livre, opcional, sem regra de validação de conteúdo.
- FR-022: `fund` obrigatório, um dos três valores fixos, para os dois tipos.

**Persistência**: tabela `account` (renomeada de `receivable`, preservando dados via `ALTER TABLE ... RENAME TO`), com `unit_id` agora nullable, novo `supplier_id` nullable (FK para `supplier.id`), nova coluna `observations` (nullable), coluna `fund` (renomeada de `target_account`), e `CHECK CONSTRAINT account_type_counterparty_check` garantindo a consistência `type` ⇄ contraparte preenchida. Índices em `unit_id` (renomeado de `receivable_unit_id_idx`) e novo índice em `supplier_id`.

## Supplier (Fornecedor) — nova entidade

Representa quem recebe um pagamento do condomínio (prestador de serviço) ou, quando vinculado a uma unidade, o pagamento se refere a essa unidade especificamente.

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `id` | Long (PK, auto-gerado) | — | Gerado pelo banco |
| `name` | String | Sim | Sem regra de unicidade (FR-001) |
| `unit` | Referência a `Unit` (FK `unit_id`, nullable) | Não | Pode ser vinculado, trocado ou removido a qualquer momento (FR-002) |
| `pixKey` | String (nullable) | Não | Texto livre, sem validação de formato específica (FR-023) |

**Relacionamentos**: `Unit` (feature 001) possui zero ou mais `Supplier` vinculados (`1:N`, opcional). `Supplier` possui zero ou mais `Account` do tipo `PAYABLE` (`1:N`).

**Regras de negócio**:
- FR-005: remoção bloqueada quando houver ao menos uma `Account` (`PAYABLE`) vinculada — `accountRepository.existsBySupplierId(id)`.
- Impacto cruzado (feature 001): remoção de `Unit` bloqueada quando houver ao menos um `Supplier` vinculado — `supplierRepository.existsByUnitId(id)` (ver FR-017 e research.md).

**Persistência**: nova tabela `supplier`, com `unit_id` nullable (FK para `unit.id`) e índice em `unit_id`.

## Impacto em `Unit` (feature 001)

`Unit` não sofre alteração estrutural, mas passa a ser referenciada também por `Supplier` (além de `Account` do tipo `RECEIVABLE`). A regra de remoção de `Unit` (`UnitService.delete()`) passa a considerar `Account` (renomeado de `Receivable`) e `Supplier` vinculados, e deixa de considerar `Resident` (removido — ver abaixo). Essa mudança pertence à feature 001 e segue o processo de "Edição de Features Já Implementadas" (ver research.md).

## Remoção de `Resident` (Condômino)

A entidade `Resident`, sua tabela (`resident`) e todo o código relacionado (backend `com.financas.resident`, frontend `resident/`, rotas `/residents*`) são removidos por completo (FR-016). Não há substituição direta — `Supplier` não herda nem reaproveita a estrutura de `Resident` além do padrão de pacote `api/domain/infra` já convencionado.

## Diagrama de relacionamento

```text
Unit (1) ──────< (N) Account (type = RECEIVABLE)     (renomeada de Receivable, feature 002)
Unit (1) ──────< (N) Supplier                         (nova, opcional)
Supplier (1) ──< (N) Account (type = PAYABLE)         (nova, desta feature)

Account
  id
  type            (RECEIVABLE | PAYABLE)
  amount
  dueDate
  description
  fund             (POOL | POOL_GARDEN | SIDE_GARDEN)
  recurring        (boolean)
  unit_id          (FK → Unit.id, nullable — obrigatório se type = RECEIVABLE)
  supplier_id      (FK → Supplier.id, nullable — obrigatório se type = PAYABLE)
  payment_date     (nullable — não nulo = pago)
  observations     (nullable, texto livre)

Supplier
  id
  name
  unit_id          (FK → Unit.id, nullable)
  pix_key          (nullable, texto livre)
```

## Estados e transições

`Account` mantém o mesmo estado simples de pagamento já existente em `Receivable` (feature 002), independente do `type`: **Pendente** (`paymentDate = null`) → **Pago** (`paymentDate` informado), sem transição de volta (estorno) nesta versão. `type` não tem transição alguma — é fixado na criação e permanece constante durante todo o ciclo de vida do registro.

## Filtros suportados em `GET /api/accounts`

| Query param | Tipo | Efeito |
|---|---|---|
| `type` | `RECEIVABLE` \| `PAYABLE` | Novo nesta feature (FR-012) — só contas do tipo informado |
| `unitId` | Long | Herdado de 002 — só contas cuja `unit` é a informada (só afeta `RECEIVABLE`) |
| `supplierId` | Long | Novo nesta feature (simétrico a `unitId`) — só contas cujo `supplier` é o informado (só afeta `PAYABLE`) |
| `paid` | boolean | Herdado de 002 — `true`/`false` |
| `overdue` | boolean (`true`) | Herdado de 002 — pendente + `dueDate` no passado |
| `dueYearMonth` | `yyyy-MM` | Herdado de 002 |
| `paymentYearMonth` | `yyyy-MM` | Herdado de 002 |

Todos combináveis entre si (E lógico); nenhum é obrigatório.
