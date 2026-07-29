# API Contracts: Fundos como Entidade e Visualização de Saldo Real

Base path: `/api`

Formato de erro padrão (todas as respostas 4xx), mensagens em português (Convenções de API
REST), inalterado em relação às features anteriores:

```json
{
  "message": "Já existe um fundo cadastrado com o nome 'Piscina'.",
  "status": 409
}
```

## Fundos — `/api/funds` (nova)

### `GET /api/funds`

Lista todos os fundos cadastrados, ordenados por nome, cada um com seu saldo real (FR-007 —
visualização de saldo dos fundos).

**Response 200**:
```json
[
  { "id": 2, "name": "Jardim Piscina", "initialBalance": 0.00, "realBalance": 150.00 },
  { "id": 1, "name": "Piscina", "initialBalance": 500.00, "realBalance": 800.00 },
  { "id": 3, "name": "Jardim Lateral", "initialBalance": -100.00, "realBalance": -100.00 }
]
```
Lista vazia (`[]`) quando não há fundos cadastrados (ex.: ambiente novo, sem nenhum fundo
pré-criado — FR-009). `realBalance` = `initialBalance` + recebimentos já recebidos − pagamentos
já pagos vinculados ao fundo (FR-008/FR-011); pode ser negativo (FR-012). O saldo total somado
de todos os fundos (US1, cenário 3) é calculado pelo frontend somando `realBalance` de todos os
itens desta lista — não há campo de total nesta resposta.

### `GET /api/funds/{id}`

**Response 200**: fundo, mesmo formato do `GET`. **Erros**: `404` — fundo não encontrado.

### `POST /api/funds`

Cadastra um novo fundo (FR-001).

**Request**:
```json
{ "name": "Piscina", "initialBalance": 500.00 }
```
`initialBalance` é obrigatório no contrato da API; o frontend pré-preenche com `0` quando a
usuária não informa um valor (FR-010, Assumptions do spec).

**Response 201**: fundo criado, com `realBalance` igual a `initialBalance` (nenhum lançamento
vinculado ainda).

**Erros**:
- `400` — `name` ausente/vazio; `initialBalance` ausente.
- `409` — já existe um fundo com o mesmo nome (comparação sem diferenciar maiúsculas/minúsculas
  nem espaços nas extremidades) — FR-002.

### `PUT /api/funds/{id}`

Edita o nome e/ou o saldo inicial de um fundo existente (FR-003). Mesmo corpo do `POST`.

**Response 200**: fundo atualizado, com `realBalance` recalculado a partir do novo
`initialBalance`.

**Erros**: mesmas do `POST`, mais `404` — fundo não encontrado.

### `DELETE /api/funds/{id}`

Remove um fundo, bloqueando quando houver ao menos uma conta (a receber ou a pagar) vinculada
(FR-004/FR-005).

**Response 204**: sem corpo.

**Erros**:
- `404` — fundo não encontrado.
- `409` — fundo possui ao menos uma conta vinculada.

## Impacto em `/api/accounts` (features 002/003)

`fund` deixa de ser um valor de enum fixo (`"POOL"`/`"POOL_GARDEN"`/`"SIDE_GARDEN"`) e passa a
ser uma referência a um fundo cadastrado.

### `GET /api/accounts` / `GET /api/accounts/{id}`

O campo `fund` da resposta passa de string de enum para o objeto completo do fundo (mesmo
princípio já aplicado a `unit`/`supplier`):

```json
{
  "id": 1,
  "type": "RECEIVABLE",
  "amount": 350.00,
  "dueDate": "2026-08-10",
  "description": "Taxa condominial - Agosto/2026",
  "fund": { "id": 1, "name": "Piscina", "initialBalance": 500.00, "realBalance": 800.00 },
  "recurring": true,
  "paymentDate": null,
  "observations": null,
  "unit": { "id": 1, "identifier": "Bloco A - 101" },
  "supplier": null
}
```

Nenhum filtro novo é adicionado a `GET /api/accounts` por esta feature (ver research.md — sem
`fundId` como query param, não solicitado pelo spec).

### `POST /api/accounts` / `PUT /api/accounts/{id}` / `POST /api/accounts/bulk`

O corpo da requisição passa a usar `fundId` (Long) em vez de `fund` (string de enum) — mesmo
padrão de `unitId`/`supplierId`:

```json
{
  "type": "RECEIVABLE",
  "amount": 350.00,
  "dueDate": "2026-08-10",
  "description": "Taxa condominial - Agosto/2026",
  "fundId": 1,
  "recurring": true,
  "paymentDate": null,
  "observations": null,
  "unitId": 1,
  "supplierId": null
}
```

**Erros (novos/alterados)**:
- `400` — `fundId` ausente.
- `404` — `fundId` informado não corresponde a um fundo cadastrado (orienta a cadastrar um
  fundo antes de lançar a conta, mesmo padrão de `unitId`/`supplierId`).
