# API Contracts: Contas a Pagar, Fornecedores e Unificação de Contas

Base path: `/api`

Formato de erro padrão (todas as respostas 4xx), mensagens em português (Convenções de API REST), inalterado em relação à feature 002:

```json
{
  "message": "O valor da conta não pode ser negativo.",
  "status": 400
}
```

Datas (`dueDate`, `paymentDate`) trafegam em ISO-8601 (`yyyy-MM-dd`), sem `@JsonFormat` customizada — mesma regra da feature 002.

## Contas — `/api/accounts` (substitui `/api/receivables` da feature 002)

### `GET /api/accounts`

Lista contas, a pagar e a receber (FR-010), ordenadas por `dueDate` decrescente por padrão — vencimento mais distante/futuro primeiro, mais antigo por último; empate em `dueDate` é desempatado por `description` em ordem alfabética crescente e, se ainda empatado, por `id` decrescente (FR-024). Aceita filtros opcionais, combináveis (E lógico).

**Query params** (todos opcionais):
- `type` (`RECEIVABLE`/`PAYABLE`): retorna só contas do tipo informado (FR-012).
- `unitId`: retorna só contas cuja unidade é a informada (aplica-se a contas `RECEIVABLE`).
- `supplierId`: retorna só contas cujo fornecedor é o informado (aplica-se a contas `PAYABLE`).
- `paid` (`true`/`false`): pago/pendente — herdado de 002.
- `overdue` (`true`): pendente com `dueDate` no passado — herdado de 002.
- `dueYearMonth` (`yyyy-MM`): herdado de 002.
- `paymentYearMonth` (`yyyy-MM`): herdado de 002.

Sem filtros, retorna todas as contas cadastradas (a pagar e a receber juntas).

**Response 200**:
```json
[
  {
    "id": 1,
    "type": "RECEIVABLE",
    "amount": 350.00,
    "dueDate": "2026-08-10",
    "description": "Taxa condominial - Agosto/2026",
    "fund": "POOL",
    "recurring": true,
    "paymentDate": null,
    "observations": null,
    "unit": { "id": 1, "identifier": "Bloco A - 101" },
    "supplier": null
  },
  {
    "id": 2,
    "type": "PAYABLE",
    "amount": 400.00,
    "dueDate": "2026-08-05",
    "description": "Limpeza - Agosto/2026",
    "fund": "SIDE_GARDEN",
    "recurring": false,
    "paymentDate": null,
    "observations": "Disse que vai pagar mês que vem",
    "unit": null,
    "supplier": { "id": 1, "name": "Empresa de Limpeza XYZ", "unit": null, "pixKey": null }
  }
]
```
Lista vazia (`[]`) quando não há contas, ou nenhuma corresponde aos filtros informados. Uma conta sempre tem exatamente um de `unit`/`supplier` preenchido, nunca os dois nem nenhum.

**Erros**:
- `404` — `unitId` ou `supplierId` informado no filtro não corresponde a um registro cadastrado.
- `400` — `type`, `paid`/`overdue` fora dos valores aceitos, ou `dueYearMonth`/ `paymentYearMonth` fora do formato `yyyy-MM`.

### `GET /api/accounts/{id}`

**Response 200**: conta, no mesmo formato do `GET /api/accounts`.

**Erros**: `404` — conta `{id}` não encontrada.

### `POST /api/accounts`

Lança uma conta individual, a pagar ou a receber (FR-001 do spec para fornecedor + FR-002 da feature 002 para unidade).

**Request (conta a receber)**:
```json
{
  "type": "RECEIVABLE",
  "amount": 350.00,
  "dueDate": "2026-08-10",
  "description": "Taxa condominial - Agosto/2026",
  "fund": "POOL",
  "recurring": true,
  "paymentDate": null,
  "observations": null,
  "unitId": 1,
  "supplierId": null
}
```

**Request (conta a pagar)**:
```json
{
  "type": "PAYABLE",
  "amount": 400.00,
  "dueDate": "2026-08-05",
  "description": "Limpeza - Agosto/2026",
  "fund": "SIDE_GARDEN",
  "recurring": false,
  "paymentDate": null,
  "observations": "Disse que vai pagar mês que vem",
  "unitId": null,
  "supplierId": 1
}
```

`paymentDate` e `observations` são opcionais em ambos os casos. `unitId` é obrigatório quando `type = RECEIVABLE` (e `supplierId` deve ser omitido/nulo); `supplierId` é obrigatório quando `type = PAYABLE` (e `unitId` deve ser omitido/nulo).

**Response 201**: conta criada, no mesmo formato do `GET`.

**Erros**:
- `400` — `amount` ausente ou negativo (FR-008 — zero é aceito); `dueDate`, `description`, `fund`, `type` ausentes; `fund` fora do conjunto fixo; contraparte ausente ou incompatível com `type` (ex.: `type = RECEIVABLE` com `supplierId` preenchido, ou sem `unitId`) — FR-007.
- `404` — `unitId`/`supplierId` informado não corresponde a um registro cadastrado (orienta a cadastrar a unidade/fornecedor primeiro — FR-021, mesmo padrão de 002).

### `POST /api/accounts/bulk`

Lança a mesma conta a receber para todas as unidades cadastradas no momento da chamada — exclusivo de contas a receber (FR-009). Comportamento herdado de `POST /api/receivables/bulk` (feature 002), sem `type`/`supplierId` no corpo.

**Request**:
```json
{
  "amount": 350.00,
  "dueDate": "2026-08-10",
  "description": "Taxa condominial - Agosto/2026",
  "fund": "POOL",
  "recurring": true,
  "paymentDate": null,
  "observations": null
}
```

**Response 201**: array com uma conta criada por unidade (todas `type: "RECEIVABLE"`).

**Erros**: mesmas do `POST /api/accounts` (exceto `unitId`, que não se aplica); `409` — nenhuma unidade cadastrada.

### `PUT /api/accounts/{id}`

Edita uma conta existente (FR-015). Mesmo corpo do `POST /api/accounts`, incluindo `type` — que MUST ser igual ao tipo já persistido da conta; a contraparte (`unitId` ou `supplierId`, conforme o tipo) pode ser trocada por outra do mesmo tipo.

**Response 200**: conta atualizada.

**Erros**:
- `400` — mesmas validações do `POST /api/accounts`, mais: `type` diferente do tipo atual da conta ("Não é possível alterar o tipo de uma conta já criada.").
- `404` — conta `{id}` não encontrada, ou `unitId`/`supplierId` informado não existe.

### `POST /api/accounts/{id}/pay`

Registra/atualiza o pagamento de uma conta, a pagar ou a receber (FR-013). Inalterado em relação a `POST /api/receivables/{id}/pay` (feature 002).

**Request**: `{ "paymentDate": "2026-08-15" }`

**Response 200**: conta atualizada. **Erros**: `400` — `paymentDate` ausente/inválida; `404` — conta não encontrada.

### `DELETE /api/accounts/{id}`

Remove uma conta (FR-015). Inalterado em relação a `DELETE /api/receivables/{id}` — sem endpoint de remoção em lote; o frontend chama esta rota uma vez por item selecionado (melhor esforço, herdado de 002).

**Response 204**: sem corpo. **Erros**: `404` — conta não encontrada.

## Fornecedores — `/api/suppliers` (nova)

### `GET /api/suppliers`

Lista todos os fornecedores cadastrados (FR-003).

**Response 200**:
```json
[
  { "id": 1, "name": "Empresa de Limpeza XYZ", "unit": null, "pixKey": "12.345.678/0001-90" },
  { "id": 2, "name": "Síndico Bloco A - 101", "unit": { "id": 1, "identifier": "Bloco A - 101" }, "pixKey": null }
]
```
Lista vazia (`[]`) quando não há fornecedores (FR-004).

### `GET /api/suppliers/{id}`

**Response 200**: fornecedor, mesmo formato do `GET`. **Erros**: `404` — não encontrado.

### `POST /api/suppliers`

**Request**:
```json
{ "name": "Empresa de Limpeza XYZ", "unitId": null, "pixKey": "12.345.678/0001-90" }
```
`unitId` e `pixKey` são opcionais (FR-001/FR-023).

**Response 201**: fornecedor criado. **Erros**: `400` — `name` ausente/vazio; `404` — `unitId` informado não corresponde a uma unidade cadastrada.

### `PUT /api/suppliers/{id}`

Edita nome, unidade vinculada e/ou chave PIX (FR-002). Mesmo corpo do `POST`.

**Response 200**: fornecedor atualizado. **Erros**: mesmas do `POST`, mais `404` — fornecedor não encontrado.

### `DELETE /api/suppliers/{id}`

Remove um fornecedor, bloqueando quando houver ao menos uma conta a pagar vinculada (FR-005).

**Response 204**: sem corpo.

**Erros**:
- `404` — fornecedor não encontrado.
- `409` — fornecedor possui ao menos uma conta a pagar vinculada.

## Impacto em `/api/units/{id}` (feature 001)

`DELETE /api/units/{id}` passa a retornar `409` também quando a unidade possuir fornecedores vinculados (além de contas vinculadas, que já era o caso na feature 002) — e **deixa** de checar condôminos vinculados, já que o cadastro de condôminos é removido (FR-016/FR-017 desta feature). Este contrato pertence à feature 001 (`specs/001-cadastro-condominos/contracts/api.md`) e só deve ser atualizado lá após a aprovação do processo de "Edição de Features Já Implementadas" (ver plan.md e research.md desta feature).

## Remoção de `/api/residents` (feature 001)

Toda a rota `/api/residents*` deixa de existir (FR-016). Este contrato também pertence à feature 001 e é removido de `specs/001-cadastro-condominos/contracts/api.md` como parte do mesmo processo de aprovação acima.
