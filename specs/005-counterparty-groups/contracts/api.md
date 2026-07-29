# API Contracts: Unificação de Unidade/Fornecedor, Contas sem Restrição de Tipo e Grupos

Base path: `/api`

Formato de erro padrão (todas as respostas 4xx), mensagens em português (Convenções de API REST), inalterado em relação às features anteriores:

```json
{
  "message": "Já existe uma parte cadastrada com o nome 'Bloco A - 101'.",
  "status": 409
}
```

## Partes — `/api/parties` (nova, substitui `/api/units` e `/api/suppliers`)

### `GET /api/parties`

Lista todas as Partes cadastradas, ordenadas por nome (FR-010 — filtro "Parte" na tela de Contas consome esta lista).

**Response 200**:
```json
[
  { "id": 1, "name": "Bloco A - 101", "pixKey": null },
  { "id": 2, "name": "Construtora XYZ Ltda", "pixKey": "12.345.678/0001-90" }
]
```
Lista vazia (`[]`) quando não há partes cadastradas.

### `GET /api/parties/{id}`

**Response 200**: parte, mesmo formato do `GET`. **Erros**: `404` — parte não encontrada.

### `POST /api/parties`

Cadastra uma nova Parte (FR-003/FR-004).

**Request**:
```json
{ "name": "Bloco A - 101", "pixKey": null }
```

**Response 201**: parte criada.

**Erros**:
- `400` — `name` ausente/vazio.
- `409` — já existe uma parte com o mesmo nome (comparação sem diferenciar maiúsculas/minúsculas nem espaços nas extremidades) — FR-004.

### `PUT /api/parties/{id}`

Edita nome e/ou chave pix de uma Parte existente. Mesmo corpo do `POST`.

**Response 200**: parte atualizada.

**Erros**: mesmas do `POST`, mais `404` — parte não encontrada.

### `DELETE /api/parties/{id}`

Remove uma Parte, bloqueando quando houver ao menos uma conta (de qualquer tipo) vinculada (FR-006). Pertencer a um ou mais Grupos **não** bloqueia a remoção — a associação é removida automaticamente.

**Response 204**: sem corpo.

**Erros**:
- `404` — parte não encontrada.
- `409` — parte possui ao menos uma conta vinculada.

## Grupos — `/api/groups` (nova)

### `GET /api/groups`

Lista todos os Grupos cadastrados, ordenados por nome, cada um com a lista completa de suas Partes integrantes (ordenadas por nome).

**Response 200**:
```json
[
  {
    "id": 1,
    "name": "Bloco A",
    "members": [
      { "id": 1, "name": "Bloco A - 101", "pixKey": null },
      { "id": 3, "name": "Bloco A - 102", "pixKey": null }
    ]
  }
]
```
Lista vazia (`[]`) quando não há grupos cadastrados. `members` pode ser `[]` (grupo sem integrantes ainda) — não é impedido de existir, só de ser usado em lançamento em lote (FR-015).

### `GET /api/groups/{id}`

**Response 200**: grupo, mesmo formato do `GET`. **Erros**: `404` — grupo não encontrado.

### `POST /api/groups`

Cadastra um novo Grupo, com sua composição inicial de integrantes (FR-012/FR-013).

**Request**:
```json
{ "name": "Bloco A", "partyIds": [1, 3] }
```
`partyIds` MAY ser `[]` ou omitido (grupo criado sem integrantes, adicionados depois via `PUT`).

**Response 201**: grupo criado, com `members` já resolvidos a partir de `partyIds`.

**Erros**:
- `400` — `name` ausente/vazio.
- `404` — algum `id` em `partyIds` não corresponde a uma parte cadastrada.
- `409` — já existe um grupo com o mesmo nome.

### `PUT /api/groups/{id}`

Edita o nome e/ou a composição completa de integrantes de um Grupo (FR-013 — único ponto de edição de composição). `partyIds` enviado substitui a lista de integrantes por completo (não é incremental).

**Request**: mesmo corpo do `POST`.

**Response 200**: grupo atualizado.

**Erros**: mesmas do `POST`, mais `404` — grupo não encontrado.

### `DELETE /api/groups/{id}`

Remove um Grupo. Sempre permitido, mesmo com integrantes — não afeta nenhuma conta já lançada para seus antigos integrantes (FR-016).

**Response 204**: sem corpo.

**Erros**: `404` — grupo não encontrado.

## Impacto em `/api/accounts` (features 002/003/004)

`unit`/`supplier` deixam de existir na resposta e no corpo de requisição; ambos são substituídos por `party` (resposta, objeto completo) / `partyId` (requisição, Long) — mesmo princípio já aplicado a `fund`/`fundId` na feature 004.

### `GET /api/accounts`

Novos/alterados query params:
- `partyId` (Long, opcional) — substitui `unitId`/`supplierId` (FR-010).
- `fundId` (Long, opcional, **novo**) — filtra por fundo (FR-009), combinado por E lógico com os demais filtros já existentes (`type`, `paid`, `overdue`, `dueYearMonth`, `paymentYearMonth`).

```
GET /api/accounts?partyId=1&fundId=2&type=PAYABLE&paid=false
```

**Response 200**:
```json
[
  {
    "id": 1,
    "type": "PAYABLE",
    "amount": 350.00,
    "dueDate": "2026-08-10",
    "description": "Reembolso de despesa adiantada",
    "fund": { "id": 1, "name": "Piscina", "initialBalance": 500.00, "realBalance": 800.00 },
    "recurring": false,
    "paymentDate": null,
    "observations": null,
    "party": { "id": 1, "name": "Bloco A - 101", "pixKey": null }
  }
]
```
`party` nunca é `null` (diferente de `unit`/`supplier` antes, que eram mutuamente exclusivos e um deles sempre `null`) — toda conta tem exatamente uma Parte, de qualquer tipo (FR-001). Não há campo de total nesta resposta — o total líquido exibido na tela de Contas (User Story 2) é calculado pelo frontend a partir desta mesma lista (ver research.md).

### `GET /api/accounts/{id}`

Mesmo formato de `party` do `GET` de coleção.

### `POST /api/accounts` / `PUT /api/accounts/{id}`

`unitId`/`supplierId` são substituídos por um único `partyId` (Long, sempre obrigatório, independentemente de `type` — FR-001):

```json
{
  "type": "PAYABLE",
  "amount": 350.00,
  "dueDate": "2026-08-10",
  "description": "Reembolso de despesa adiantada",
  "fundId": 1,
  "recurring": false,
  "partyId": 1,
  "paymentDate": null,
  "observations": null
}
```

**Erros (novos/alterados)**:
- `400` — `partyId` ausente.
- `404` — `partyId` informado não corresponde a uma parte cadastrada.
- `400` — tentativa de alterar `type` num `PUT` (inalterado desde a feature 002 — `AccountTypeChangeNotAllowedException`).

### `POST /api/accounts/bulk` (generalizado — antes só "todas as unidades", sempre `RECEIVABLE`)

Cria uma conta para cada Parte integrante de um Grupo (FR-014). `type` passa a ser explícito (antes implícito, sempre `RECEIVABLE`); `groupId` substitui a busca implícita por "todas as unidades cadastradas".

**Request**:
```json
{
  "type": "RECEIVABLE",
  "amount": 350.00,
  "dueDate": "2026-08-10",
  "description": "Taxa condominial - Agosto/2026",
  "fundId": 1,
  "recurring": true,
  "groupId": 1,
  "paymentDate": null,
  "observations": null
}
```

**Response 201**: array de contas criadas, uma por integrante do grupo, mesmo formato do `GET /api/accounts`.

**Erros**:
- `400` — `groupId` ou `type` ausente.
- `404` — `groupId` informado não corresponde a um grupo cadastrado.
- `409` — o grupo informado não possui integrantes (FR-015 — `EmptyGroupException`).
