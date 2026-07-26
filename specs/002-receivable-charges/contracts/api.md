# API Contracts: Lançamentos de Contas a Receber

Base path: `/api`

Formato de erro padrão (todas as respostas 4xx), mensagens em português (Convenções de API
REST):

```json
{
  "message": "O valor do lançamento deve ser maior que zero.",
  "status": 400
}
```

Datas trafegam em JSON no formato `dd/MM/yyyy` (ver research.md).

JSON malformado, `targetAccount` fora do conjunto fixo do enum ou `dueDate` fora do formato
`dd/MM/yyyy` também retornam `400` no mesmo formato padrão acima (mensagem genérica, ex.:
"Dados inválidos."), via handler dedicado de `HttpMessageNotReadableException` — ver tasks.md
T010.

## Lançamentos — `/api/receivables`

### `GET /api/receivables`

Lista lançamentos de contas a receber (FR-006). Aceita filtro opcional por unidade.

**Query params**:
- `unitId` (opcional): quando informado, retorna apenas os lançamentos da unidade indicada
  (US3). Quando omitido, retorna todos os lançamentos cadastrados.

**Response 200**:
```json
[
  {
    "id": 1,
    "amount": 350.00,
    "dueDate": "10/08/2026",
    "description": "Taxa condominial - Agosto/2026",
    "targetAccount": "POOL",
    "recurring": true,
    "unit": { "id": 1, "identifier": "Bloco A - 101" }
  }
]
```
Lista vazia (`[]`) quando não há lançamentos (FR-007).

**Erros**:
- `404` — `unitId` informado no filtro não corresponde a nenhuma unidade cadastrada.

### `GET /api/receivables/{id}`

Consulta um lançamento específico (usado pelo formulário de edição — US4).

**Response 200**: lançamento, no mesmo formato do `GET /api/receivables`.

**Erros**:
- `404` — lançamento `{id}` não encontrado (FR-010).

### `POST /api/receivables`

Lança uma conta a receber para uma unidade específica (FR-001, US1).

**Request**:
```json
{
  "amount": 350.00,
  "dueDate": "10/08/2026",
  "description": "Taxa condominial - Agosto/2026",
  "targetAccount": "POOL",
  "recurring": true,
  "unitId": 1
}
```

**Response 201**: lançamento criado, no mesmo formato do `GET`.

**Erros**:
- `400` — `amount` ausente, zero ou negativo (FR-003); `dueDate`, `description`,
  `targetAccount`, `recurring` ou `unitId` ausentes (FR-001); `targetAccount` fora do
  conjunto fixo de valores (FR-013).
- `404` — `unitId` não corresponde a nenhuma unidade cadastrada (orienta a cadastrar a
  unidade primeiro — FR-011, mesmo padrão de `POST /api/residents`).

### `POST /api/receivables/bulk`

Lança a mesma conta a receber para todas as unidades cadastradas no momento da chamada
(FR-004, FR-005, US2).

**Request**:
```json
{
  "amount": 350.00,
  "dueDate": "10/08/2026",
  "description": "Taxa condominial - Agosto/2026",
  "targetAccount": "POOL",
  "recurring": true
}
```
Sem `unitId` — aplica-se a todas as unidades existentes no momento da chamada.

**Response 201**: array com um lançamento criado por unidade, no mesmo formato do `GET`.
```json
[
  { "id": 1, "amount": 350.00, "dueDate": "10/08/2026", "description": "Taxa condominial - Agosto/2026", "targetAccount": "POOL", "recurring": true, "unit": { "id": 1, "identifier": "Bloco A - 101" } },
  { "id": 2, "amount": 350.00, "dueDate": "10/08/2026", "description": "Taxa condominial - Agosto/2026", "targetAccount": "POOL", "recurring": true, "unit": { "id": 2, "identifier": "Bloco A - 102" } }
]
```

**Erros**:
- `400` — mesmas validações de campo do `POST /api/receivables` (FR-001, FR-003, FR-013),
  exceto `unitId` (não se aplica).
- `409` — nenhuma unidade cadastrada no sistema (orienta a cadastrar uma unidade primeiro —
  FR-011).

### `PUT /api/receivables/{id}`

Edita valor, vencimento, descrição, conta destino e/ou tipo de um lançamento (FR-008).

**Request**: igual ao `POST /api/receivables` (inclui `unitId`; a unidade associada também
pode ser trocada).

**Response 200**: lançamento atualizado.

**Erros**:
- `400` — mesmas validações do `POST /api/receivables`.
- `404` — lançamento `{id}` não encontrado, ou `unitId` informado não existe (FR-010).

### `DELETE /api/receivables/{id}`

Remove um lançamento de conta a receber (FR-009).

**Response 204**: sem corpo.

**Erros**:
- `404` — lançamento `{id}` não encontrado (FR-010).

> Confirmação explícita da usuária antes da chamada é responsabilidade do frontend (diálogo
> de confirmação); o endpoint executa a remoção diretamente quando chamado.

## Impacto em `/api/units/{id}` (feature 001)

`DELETE /api/units/{id}` passa a também retornar `409` quando a unidade possuir lançamentos
de contas a receber vinculados (FR-012 desta feature). Este contrato pertence à feature 001
(`specs/001-cadastro-condominos/contracts/api.md`) e só deve ser atualizado lá após a
aprovação do processo de "Edição de Features Já Implementadas" (ver plan.md e research.md
desta feature).
