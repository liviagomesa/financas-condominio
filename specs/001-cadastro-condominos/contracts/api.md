# API Contracts: Cadastro de Condôminos e Unidades

Base path: `/api`

Formato de erro padrão (todas as respostas 4xx), mensagens em português
(FR-016, Convenções de Código):

```json
{
  "message": "Já existe uma unidade cadastrada com este identificador.",
  "status": 409
}
```

## Unidades — `/api/units`

### `GET /api/units`

Lista todas as unidades cadastradas (FR-003).

**Response 200**:
```json
[
  { "id": 1, "identifier": "Bloco A - 101" },
  { "id": 2, "identifier": "Bloco A - 102" }
]
```
Lista vazia (`[]`) quando não há unidades cadastradas (FR-014).

### `GET /api/units/{id}`

Consulta uma unidade específica (usado pelo formulário de edição — US4).

**Response 200**: unidade, no mesmo formato do `GET /api/units`.

**Erros**:
- `404` — unidade `{id}` não encontrada (FR-016).

### `POST /api/units`

Cadastra uma nova unidade (FR-001).

**Request**:
```json
{ "identifier": "Bloco A - 101" }
```

**Response 201**: unidade criada, no mesmo formato do `GET`.

**Erros**:
- `400` — `identifier` vazio/ausente (FR-001).
- `409` — `identifier` já usado por outra unidade, considerando comparação
  normalizada (FR-002).

### `PUT /api/units/{id}`

Edita o identificador de uma unidade existente (FR-004).

**Request**: igual ao `POST`.

**Response 200**: unidade atualizada.

**Erros**:
- `400` — `identifier` vazio/ausente.
- `404` — unidade `{id}` não encontrada (FR-016).
- `409` — novo `identifier` já usado por outra unidade (FR-002).

### `DELETE /api/units/{id}`

Remove uma unidade (FR-005).

**Response 204**: sem corpo.

**Erros**:
- `404` — unidade `{id}` não encontrada (FR-016).
- `409` — unidade possui ao menos um condômino associado (FR-006).

> Confirmação explícita da usuária antes da chamada é responsabilidade do
> frontend (diálogo de confirmação); o endpoint executa a remoção diretamente
> quando chamado.

## Condôminos — `/api/residents`

### `GET /api/residents`

Lista todos os condôminos cadastrados, com unidade associada (FR-013).

**Response 200**:
```json
[
  {
    "id": 10,
    "name": "Maria Silva",
    "unit": { "id": 1, "identifier": "Bloco A - 101" },
    "email": "maria@example.com",
    "phone": "(11) 91111-1111"
  }
]
```
`email`/`phone` podem ser `null`. Lista vazia (`[]`) quando não há condôminos
cadastrados (FR-014).

### `GET /api/residents/{id}`

Consulta um condômino específico (usado pelo formulário de edição — US4).

**Response 200**: condômino, no mesmo formato do `GET /api/residents`.

**Erros**:
- `404` — condômino `{id}` não encontrado (FR-016).

### `POST /api/residents`

Cadastra um novo condômino (FR-007).

**Request**:
```json
{
  "name": "Maria Silva",
  "unitId": 1,
  "email": "maria@example.com",
  "phone": "(11) 91111-1111"
}
```
`email` e `phone` são opcionais (podem ser omitidos ou `null`) (FR-008).

**Response 201**: condômino criado, no mesmo formato do `GET`.

**Erros**:
- `400` — `name` ou `unitId` ausentes/vazios (FR-011); `email` em formato
  inválido quando preenchido (FR-012); `phone` fora do formato brasileiro quando
  preenchido (FR-017).
- `404` — `unitId` não corresponde a nenhuma unidade cadastrada (orienta a
  cadastrar a unidade primeiro — Edge Case do spec).

### `PUT /api/residents/{id}`

Edita nome, unidade, e-mail e/ou telefone de um condômino (FR-010).

**Request**: igual ao `POST`.

**Response 200**: condômino atualizado.

**Erros**:
- `400` — mesmas validações do `POST` (FR-011, FR-012, FR-017).
- `404` — condômino `{id}` não encontrado, ou `unitId` informado não existe
  (FR-016).

### `DELETE /api/residents/{id}`

Remove um condômino (FR-015).

**Response 204**: sem corpo.

**Erros**:
- `404` — condômino `{id}` não encontrado (FR-016).

> Confirmação explícita da usuária antes da chamada é responsabilidade do
> frontend, assim como em `DELETE /api/units/{id}`.
