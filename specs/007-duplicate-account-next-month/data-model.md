# Data Model: Duplicar lançamentos para o mês seguinte

Esta feature não introduz nenhuma entidade, coluna ou migration nova — `Account` permanece exatamente como está hoje (ver research.md, "O lançamento duplicado não guarda vínculo com o original"). O que muda é: (1) uma nova operação sobre a entidade `Account` já existente, e (2) estado efêmero de interface novo no frontend, descritos abaixo.

## `Account` (`backend/src/main/java/com/financas/account/domain/Account.java`) — inalterada

| Campo | Tipo | Papel na duplicação |
|---|---|---|
| `type` | `AccountType` (enum) | Copiado sem alteração |
| `amount` | `BigDecimal` | Copiado do original, ou `BigDecimal.ZERO` quando `zeroAmount = true` |
| `dueDate` | `LocalDate` | `original.getDueDate().plusMonths(1)` — nunca copiado igual |
| `description` | `String` | Copiado sem alteração |
| `fund` | `Fund` | Copiado sem alteração (mesma referência) |
| `recurring` | `boolean` | Copiado sem alteração |
| `party` | `Party` | Copiado sem alteração (mesma referência) |
| `paymentDate` | `LocalDate` (nullable) | Sempre `null` na cópia, independentemente do valor no original |
| `observations` | `String` (nullable) | Copiado sem alteração |

**Operação nova**: `AccountService.duplicate(Long id, boolean zeroAmount)` — lê o `Account` original por `id` (reaproveita `findById`, lança `NotFoundException` se não existir) e persiste um `Account` novo e independente com os campos acima. O `id` original nunca é alterado; a cópia recebe um `id` novo gerado pelo banco, sem qualquer coluna de referência ao original.

## Estado efêmero de interface (`account/account-list/account-list.ts`)

Estado novo, não persistido, existente apenas enquanto a tela de contas está aberta.

| Campo | Tipo | Descrição |
|---|---|---|
| `copiedIds` | `number[]` (signal) | Ids dos lançamentos selecionados no momento do último `Ctrl+C` válido (fora de campo editável, com ao menos um item selecionado). Vazio quando nenhum `Ctrl+C` foi pressionado ainda, ou depois de recarregar a tela. |
| `recentlyDuplicatedIds` | `ReadonlySet<number>` (signal) | Ids das cópias criadas com sucesso na última operação de duplicação, usados para aplicar o destaque visual temporário (FR-013). Populado ao concluir a duplicação, esvaziado automaticamente ~2,5s depois. |
| `successMessage` | `string \| null` (signal) | Mensagem exibida após uma duplicação com pelo menos uma cópia criada (FR-014), com a contagem de cópias. Limpa ao iniciar qualquer nova ação na tela (mudança de filtro, remoção, nova duplicação). |

**Transições**:
- `Ctrl+C` pressionado fora de um campo de edição, com `selection.selectedIds().size > 0` → `copiedIds` passa a conter os ids atualmente selecionados (FR-009).
- `Ctrl+C` pressionado novamente sobre uma seleção diferente → `copiedIds` é sobrescrito com a nova seleção (edge case do spec).
- `Ctrl+V` pressionado fora de um campo de edição, com `copiedIds` não vazio → dispara a duplicação (valor mantido) dos ids em `copiedIds`, sem alterar `copiedIds` (permite colar mais de uma vez).
- `Ctrl+C`/`Ctrl+V` pressionado com o foco num campo de edição (`INPUT`/`TEXTAREA`/`contentEditable`) → ignorado, sem efeito sobre `copiedIds` (FR-011).
- Mudança de filtro (`onFilterChange()`, já limpa `selection` hoje) → **não** limpa `copiedIds` propositalmente: a seleção "copiada" é conceitualmente independente da seleção atual da tela, assim como um clipboard real não é afetado por mudar o que está visível na tela.
- Duplicação concluída com `result.succeeded.length > 0` → `recentlyDuplicatedIds` recebe os ids das cópias e `successMessage` recebe a contagem; um `setTimeout` de ~2,5s esvazia `recentlyDuplicatedIds` de volta, sem afetar `successMessage` (que só é limpa pela próxima ação na tela).

## Diagrama (referência)

```text
AccountList (componente)
  selection.selectedIds()   Set<number>          — já existente, seleção atual da tela
  copiedIds                 number[]             — novo, seleção memorizada pelo último Ctrl+C
  recentlyDuplicatedIds     ReadonlySet<number>   — novo, cópias recém-criadas (destaque ~2,5s)
  successMessage            string | null        — novo, mensagem de contagem de cópias

Account (entidade, backend) — inalterada
  duplicate(id, zeroAmount) → novo Account independente, sem vínculo com o original
```

Nenhum desses campos novos é persistido no backend além da própria cópia criada — `copiedIds`/`recentlyDuplicatedIds`/`successMessage` são inteiramente descartados ao sair da tela ou recarregar a página.
