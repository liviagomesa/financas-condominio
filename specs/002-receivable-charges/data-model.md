# Data Model: Lançamentos de Contas a Receber

## Receivable (Lançamento de Conta a Receber)

Representa um valor devido por uma unidade do condomínio.

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `id` | Long (PK, auto-gerado) | — | Gerado pelo banco |
| `amount` | BigDecimal (`NUMERIC(10,2)`) | Sim | Deve ser maior que zero (FR-003) |
| `dueDate` | LocalDate | Sim | Sem restrição de passado/futuro (ver Edge Cases do spec); serializado como `dd/MM/yyyy` na API (ver research.md) |
| `description` | String | Sim | Não pode ser vazia/branca |
| `targetAccount` | enum `TargetAccount` (`POOL`, `POOL_GARDEN`, `SIDE_GARDEN`) | Sim | Um dos três valores fixos (FR-013); persistido como `VARCHAR` (`EnumType.STRING`) |
| `recurring` | boolean | Sim | `true` = recorrente, `false` = extra (FR-014) |
| `unit` | Referência a `Unit` (FK `unit_id`) | Sim | Deve referenciar uma `Unit` já cadastrada (FR-002) |

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
                         unit_id (FK → Unit.id)
```

## Estados e transições

`Receivable` não possui máquina de estados nesta feature — é um registro de lançamento
simples (criar, editar, listar, remover), sem status de pagamento/quitação (fora de escopo,
ver Assumptions do spec).
