# Data Model: Lançamentos de Contas a Receber

## Receivable (Lançamento de Conta a Receber)

Representa um valor devido por uma unidade do condomínio.

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `id` | Long (PK, auto-gerado) | — | Gerado pelo banco |
| `amount` | BigDecimal (`NUMERIC(10,2)`) | Sim | Deve ser maior que zero (FR-003) |
| `dueDate` | LocalDate | Sim | Sem restrição de passado/futuro (ver Edge Cases do spec); serializado no formato ISO-8601 padrão (`yyyy-MM-dd`) na API — conversão para `dd/MM/yyyy` é responsabilidade do frontend (ver research.md, revisado 2026-07-26) |
| `description` | String | Sim | Não pode ser vazia/branca |
| `targetAccount` | enum `TargetAccount` (`POOL`, `POOL_GARDEN`, `SIDE_GARDEN`) | Sim | Um dos três valores fixos (FR-013); persistido como `VARCHAR` (`EnumType.STRING`) |
| `recurring` | boolean | Não (sempre tem valor, padrão `false`) | `true` = recorrente, `false` = extra (FR-014); UI representa como caixa de seleção desmarcada por padrão |
| `unit` | Referência a `Unit` (FK `unit_id`) | Sim | Deve referenciar uma `Unit` já cadastrada (FR-002) |
| `paymentDate` | LocalDate (nullable) | Não — opcional em qualquer momento (criação, edição, ou via `POST /{id}/pay`) | Presença (não nula) = lançamento pago; ausência (nula) = pendente. Não existe campo `paid` separado (FR-018). Mesma serialização ISO-8601 de `dueDate`; pode ser informada já na criação (FR-015) ou atualizada depois, inclusive reaplicando sobre um lançamento já pago (sem estorno nesta versão, FR-016) |

**Relacionamentos**: uma `Unit` (feature 001) possui zero ou mais `Receivable` (`1:N`). Um
lançamento em lote (US2) gera um `Receivable` independente por unidade existente no momento
da ação — não há entidade separada de "lote"; cada registro resultante é um `Receivable`
comum, editável e removível isoladamente.

**Regras de negócio**:
- FR-001/FR-002: `amount`, `dueDate`, `description`, `targetAccount`, `recurring` e `unit`
  obrigatórios na criação.
- FR-003/FR-008: `amount` deve ser positivo, na criação e na edição.
- FR-004/FR-005: lançamento em lote cria um `Receivable` por `Unit` existente no momento da
  ação; unidades cadastradas depois não recebem o lançamento retroativamente.
- FR-013: `targetAccount` restrito ao conjunto fixo de valores do enum.
- FR-014: `recurring` sem regra de validação de valor (é um boolean, sempre um dos dois
  estados); usado apenas para armazenamento/filtragem futura.
- FR-015/FR-016: `paymentDate` pode ser informado já na criação (individual ou em lote) ou
  registrado/atualizado depois via `POST /{id}/pay`; reaplicar a ação sobre um lançamento já
  pago apenas atualiza a data (sem estorno).
- FR-017: nenhuma regra de validação impede editar ou remover um `Receivable` com
  `paymentDate` preenchido — mesmas regras de um lançamento pendente.
- FR-019: remoção em lote não é uma regra nova no domínio — cada item é removido via o mesmo
  método `delete(id)` já existente, chamado uma vez por item selecionado (ver research.md).
- FR-020/FR-021/FR-022/FR-023: filtros de listagem (`paid`, `overdue`, `dueYearMonth`,
  `paymentYearMonth`) são aplicados em memória sobre a lista já carregada, combináveis entre
  si e com o filtro por `unitId` (E lógico); `overdue` = `paymentDate == null` E `dueDate <
  hoje` (ver research.md).

**Persistência**: tabela `receivable`, com `unit_id` como chave estrangeira obrigatória
(`NOT NULL`) para `unit.id`, e índice em `unit_id` para suportar a listagem por unidade
(FR-006, `GET /api/receivables?unitId=`).

## Impacto em `Unit` (feature 001)

`Unit` passa a também ser referenciada por `Receivable` (além de `Resident`). A regra de
remoção de `Unit` (`UnitService.delete()`) precisa considerar a existência de `Receivable`
vinculados, além de `Resident` — ver nota de impacto cruzado em research.md e plan.md; essa
mudança pertence à feature 001 e segue o processo de "Edição de Features Já Implementadas".

## Diagrama de relacionamento

```text
Unit (1) ──────< (N) Resident      (feature 001, inalterado)
Unit (1) ──────< (N) Receivable    (nova, desta feature)
  id                    id
  identifier            amount
                         dueDate
                         description
                         targetAccount (POOL | POOL_GARDEN | SIDE_GARDEN)
                         recurring (boolean)
                         payment_date (nullable — não nulo = pago)
                         unit_id (FK → Unit.id)
```

## Estados e transições

`Receivable` tem um estado simples de pagamento, derivado só de `paymentDate`: **Pendente**
(`paymentDate = null`) → **Pago** (`paymentDate` informado). A transição pode ocorrer já na
criação (informando `paymentDate` desde o início) ou depois, via `POST
/api/receivables/{id}/pay`. Não há transição de volta (estorno) nesta versão — reaplicar a
ação sobre um lançamento já pago apenas atualiza `paymentDate`. Esse estado é independente das
demais operações (criar, editar, listar, remover), que continuam permitidas em qualquer estado
(FR-017).

## Filtros suportados em `GET /api/receivables`

| Query param | Tipo | Efeito |
|---|---|---|
| `unitId` | Long | Já existente (FR-006) — só lançamentos da unidade |
| `paid` | boolean | `true` = só `paymentDate != null`; `false` = só `paymentDate == null` (FR-020) |
| `overdue` | boolean (`true`) | Só pendentes (`paymentDate == null`) com `dueDate` anterior à data atual (FR-021) |
| `dueYearMonth` | `yyyy-MM` | Só lançamentos cujo `dueDate` cai nesse mês/ano (FR-022) |
| `paymentYearMonth` | `yyyy-MM` | Só lançamentos pagos cujo `paymentDate` cai nesse mês/ano (FR-023) |

Todos combináveis entre si (E lógico); nenhum é obrigatório.
