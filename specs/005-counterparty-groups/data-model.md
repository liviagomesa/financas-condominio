# Data Model: Unificação de Unidade/Fornecedor, Contas sem Restrição de Tipo e Grupos

## Party (Parte) — nova entidade, substitui `Unit` e `Supplier`

Representa qualquer parte com quem uma conta (de entrada ou de saída) pode ser lançada — sem distinção de papel entre o que antes era uma unidade e o que antes era um fornecedor.

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `id` | Long (PK, auto-gerado) | — | Gerado pelo banco |
| `name` | String | Sim | Único, comparação case-insensitive e sem espaços nas extremidades (FR-004) — mesmo mecanismo de `Unit.identifier`/`Fund.name` |
| `pixKey` | String | Não | Opcional; sem validação de formato (FR-004) |

**Relacionamentos**:
- `Party` (1) ──< (N) `Account` — toda conta referencia exatamente uma `Party`, obrigatoriamente (FR-001), independentemente do tipo da conta.
- `Party` (N) ──< (N) `Group` — uma `Party` pode pertencer a zero ou mais `Group` (FR-013, Assumptions do spec — muitos-para-muitos).

**Regras de negócio**:
- FR-004: nome obrigatório e único — `PartyService.validateNotDuplicate` usa `PartyRepository.findByNormalizedName` (mesmo padrão de `UnitService`/`FundService`).
- FR-005: nenhum campo equivalente a `Supplier.unit` é carregado para `Party`.
- FR-006: remoção bloqueada quando houver ao menos um `Account` vinculado — `accountRepository.existsByPartyId(id)`, lançando `PartyHasAccountsException`; permitida quando não houver nenhum. Pertencer a um ou mais `Group` **não** bloqueia a remoção — a associação é removida automaticamente (`ON DELETE CASCADE` na tabela de junção).

**Persistência**: nova tabela `party`, com índice único sobre `LOWER(TRIM(name))` (mesmo padrão de `unit_identifier_normalized_idx`/`fund_name_normalized_idx`).

## Group (Grupo) — nova entidade

Conjunto nomeado de `Party`, usado para lançar contas em lote para todas as suas integrantes de uma vez (User Story 5), generalizando o antigo lançamento restrito a "todas as unidades".

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `id` | Long (PK, auto-gerado) | — | Gerado pelo banco |
| `name` | String | Sim | Único, comparação case-insensitive e sem espaços nas extremidades — mesmo mecanismo de `Party.name` (default razoável, por consistência com as demais entidades nomeadas do projeto) |
| `members` | `Set<Party>` | Não (MAY ser vazio) | Associação muitos-para-muitos via tabela `party_group_member`; editável exclusivamente pela tela do próprio Grupo (Clarifications, sessão 2026-07-29) |

**Relacionamentos**:
- `Group` (N) ──< (N) `Party` — ver acima.
- `Group` não se relaciona diretamente com `Account` — um lançamento em lote para um grupo cria contas vinculadas a cada `Party` integrante, nunca ao `Group` em si (FR-016). Não existe FK de `Account` para `Group`.

**Regras de negócio**:
- Nome obrigatório e único — mesmo mecanismo de `Party`/`Unit`/`Fund`.
- Composição (`partyIds`) só é editada via `POST`/`PUT` do próprio Grupo — o cadastro de `Party` não expõe nem edita a quais grupos ela pertence (FR-013).
- Exclusão de um `Group` é **sempre permitida**, mesmo com integrantes (User Story 5, cenário sobre exclusão) — não há verificação de bloqueio; a exclusão remove o `Group` e suas linhas de associação (`ON DELETE CASCADE`), sem afetar nenhum `Account` já lançado para seus antigos integrantes (FR-016).
- Lançamento em lote (`POST /api/accounts/bulk` com `groupId`) exige ao menos um integrante — grupo vazio lança `EmptyGroupException` (FR-015).

**Persistência**: nova tabela `party_group` (nome físico distinto de `Group` para evitar a palavra reservada SQL `GROUP`), com índice único sobre `LOWER(TRIM(name))`; tabela de junção `party_group_member` (`group_id`, `party_id`, chave composta, ambos os FKs com `ON DELETE CASCADE`, índice adicional em `party_id`).

## Account (Conta) — impacto desta feature

`Account` deixa de ter duas referências separadas e mutuamente exclusivas (`unit`/`supplier`) e passa a ter uma única referência obrigatória a `Party`.

| Campo | Tipo (antes) | Tipo (depois) | Obrigatório |
|---|---|---|---|
| `unit` | `Unit` (`@ManyToOne(optional = true)`, nullable) | — (removido) | — |
| `supplier` | `Supplier` (`@ManyToOne(optional = true)`, nullable) | — (removido) | — |
| `party` | — | `Party` (`@ManyToOne(optional = false)`, `party_id NOT NULL`) | Sim (FR-001) — novo |

Nenhum outro campo de `Account` é afetado por esta feature: `type` continua obrigatório e imutável na edição (FR-002); `amount` continua validado como não-negativo (ver research.md — total líquido não exige valores negativos); `fund`, `dueDate`, `description`, `recurring`, `paymentDate`, `observations` inalterados.

**Regras de negócio (impacto)**:
- FR-001: qualquer combinação `type`×`party` é aceita — `AccountService.resolveParty(partyId)` substitui `resolveUnit`/`resolveSupplier`, sem ramificação por `type`. `partyId` sempre obrigatório, sempre aceito.
- FR-002: `AccountTypeChangeNotAllowedException` continua bloqueando alteração de `type` na edição — inalterado.
- FR-006: `Account` bloqueia a remoção de uma `Party` vinculada (ver `Party` acima).
- Lançamento em lote (FR-014): `AccountService.createForGroup` cria uma `Account` por integrante do `Group` selecionado, todas com os mesmos dados informados (`type`, `amount`, `dueDate`, `description`, `fundId`, `recurring`, `paymentDate`, `observations`).

**Persistência**: colunas `unit_id`/`supplier_id` (nullable) são removidas junto da CHECK constraint `account_type_counterparty_check`; nova coluna `party_id BIGINT NOT NULL REFERENCES party (id)`, com índice em `party_id`. A tabela `account` é truncada antes da troca de colunas (banco apenas de desenvolvimento, sem dado a preservar — ver research.md e spec Assumptions).

## Diagrama de relacionamento

```text
Party (1) ──────< (N) Account        (party_id NOT NULL — toda conta pertence a exatamente uma Parte)
Fund (1) ───────< (N) Account        (fund_id NOT NULL, inalterado desde a feature 004)
Party (N) ──────< (N) Group          (via party_group_member; uma Parte pode estar em vários grupos)

Party
  id
  name              (único, normalizado)
  pix_key           (opcional)

Group (tabela física: party_group)
  id
  name              (único, normalizado)

party_group_member
  group_id          (FK → party_group.id, ON DELETE CASCADE)
  party_id          (FK → party.id, ON DELETE CASCADE)
  PK (group_id, party_id)

Account
  id
  type              (RECEIVABLE | PAYABLE — inalterado)
  amount
  due_date
  description
  fund_id           (FK → Fund.id, obrigatório — inalterado desde a feature 004)
  recurring         (boolean)
  party_id          (FK → Party.id, obrigatório — antes: unit_id/supplier_id nullable + CHECK)
  payment_date      (nullable — não nulo = pago/recebido)
  observations      (nullable, texto livre)
```

## Estados e transições

`Party` e `Group` não têm estado/ciclo de vida próprio além de existir ou ser removido. `Party` tem remoção condicionalmente bloqueada (contas vinculadas); `Group` tem remoção sempre permitida. O total líquido exibido na tela de Contas (User Story 2) não é um estado persistido — é recalculado a cada renderização a partir da lista de contas já carregada/filtrada, no frontend (ver research.md), refletindo automaticamente qualquer filtro, criação, edição ou remoção sem sincronização manual.

## Fórmula do total líquido (referência)

```text
netTotal(accounts visíveis) = Σ amount de Account(type = RECEIVABLE)
                             − Σ amount de Account(type = PAYABLE)
```

Calculado sobre as contas atualmente exibidas na tela (após filtros), independentemente de status de pagamento ou de seleção via checkbox — diferente do saldo real de um `Fund` (`FundService.calculateRealBalance`), que soma só lançamentos **pagos** de todo o histórico do fundo, não das linhas visíveis numa tela filtrada.
