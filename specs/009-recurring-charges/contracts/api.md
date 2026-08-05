# API Contracts: Geração Automática de Contas Recorrentes

Todas as rotas novas ficam sob `/api/recurring-charges`, seguindo o mesmo padrão já usado por `/api/accounts` (Princípio VI). Nenhum endpoint desta feature é acionado manualmente para disparar a geração — ela só acontece pelos dois gatilhos automáticos internos (agendamento + recuperação na inicialização, ver research.md/data-model.md), sem rota HTTP correspondente.

## `GET /api/recurring-charges`

Lista as cobranças recorrentes **ativas** (linhas removidas ou substituídas por edição ficam ocultas — FR-011), ordenadas por `description`, depois `id`.

**Response 200** (`RecurringChargeResponse[]`):
```json
[
  {
    "id": 12,
    "type": "RECEIVABLE",
    "amount": 350.00,
    "dueDay": 10,
    "description": "Taxa condominial",
    "fund": { "...": "FundSummaryResponse já existente (revisado 2026-08-05 — ver specs/004-fund-entity-balance/plan.md)" },
    "party": { "...": "PartyResponse já existente" },
    "observations": null,
    "lastGenerationFailed": false
  }
]
```

## `GET /api/recurring-charges/{id}`

Busca uma cobrança recorrente por id (usado para pré-preencher o formulário de edição). **Response 200**: `RecurringChargeResponse` (mesmo formato acima). **Erros**: `404` — `"Cobrança recorrente não encontrada."`.

## `POST /api/recurring-charges`

Cria uma cobrança recorrente para **uma contraparte específica** (FR-001).

**Request** (`RecurringChargeRequest`):
```json
{
  "type": "RECEIVABLE",
  "amount": 350.00,
  "dueDay": 10,
  "description": "Taxa condominial",
  "fundId": 1,
  "partyId": 7,
  "observations": null
}
```
- `type` (obrigatório): `RECEIVABLE` ou `PAYABLE`.
- `amount` (obrigatório, `>= 0`): pode ser `0.00` (FR-002).
- `dueDay` (obrigatório, `1`–`31`).
- `description` (obrigatório).
- `fundId` (obrigatório).
- `partyId` (obrigatório).
- `observations` (opcional).

**Response 201**: `RecurringChargeResponse` (`lastGenerationFailed: false`).

**Erros**: `400` — tipo/valor/dia/descrição/fundo/parte ausentes ou inválidos (`amount < 0`, `dueDay` fora de 1–31); `404` — fundo ou parte não encontrados.

## `POST /api/recurring-charges/bulk`

Cria uma cobrança recorrente **por integrante de um grupo** (FR-003) — corpo igual ao `POST` individual, trocando `partyId` por `groupId` (mesmo padrão de `POST /api/accounts/bulk`).

**Request** (`RecurringChargeBulkRequest`):
```json
{
  "type": "RECEIVABLE",
  "amount": 350.00,
  "dueDay": 10,
  "description": "Taxa condominial",
  "fundId": 1,
  "groupId": 3,
  "observations": null
}
```

**Response 201**: `RecurringChargeResponse[]` — uma entrada por integrante do grupo no momento da chamada, sem nenhum vínculo persistente com o grupo depois de criadas (FR-003).

**Erros**: `409` — grupo sem integrantes (`"O grupo selecionado não possui integrantes..."`, mesma mensagem de `EmptyGroupException`, cópia dedicada nesta feature); `404` — grupo, fundo não encontrados; `400` — mesmos casos do `POST` individual.

## `PUT /api/recurring-charges/{id}`

Edita uma cobrança recorrente (FR-008). **Corpo igual ao `POST` individual** (`RecurringChargeRequest`) — `type` deve ser igual ao já persistido.

**Comportamento**: não atualiza a linha `{id}` em memória — cria uma **nova linha ativa** com os valores enviados e marca a linha `{id}` como inativa. A resposta traz a linha **nova**, com um `id` diferente do informado na URL. As contas já geradas a partir da linha `{id}` (agora inativa) permanecem intactas, referenciando-a normalmente.

**Response 200**: `RecurringChargeResponse` da linha nova.

**Erros**: `400` — `type` divergente do já persistido (`RecurringChargeTypeChangeNotAllowedException`, mesma mensagem/padrão de `AccountTypeChangeNotAllowedException`, adaptada), ou os mesmos casos de validação do `POST`; `404` — `{id}` não encontrado, ou fundo/parte não encontrados.

## `DELETE /api/recurring-charges/{id}`

Remove uma cobrança recorrente (FR-009) — **soft delete**: define `deactivatedAt = LocalDate.now()`, sem excluir a linha nem quebrar a FK das contas já geradas a partir dela. Deixa de aparecer em `GET /api/recurring-charges` e de participar da geração mensal.

**Response**: `204 No Content`. **Erros**: `404` — `{id}` não encontrado.

## Efeito colateral em `DELETE /api/parties/{id}` e `DELETE /api/funds/{id}` — bloqueio novo

`DELETE /api/parties/{id}` e `DELETE /api/funds/{id}` (rotas já existentes, inalteradas em forma) passam a também verificar cobranças recorrentes ativas vinculadas, além das contas já verificadas hoje (FR-014):

- `409` — `"Esta parte possui cobranças recorrentes ativas e não pode ser removida."` (nova, `PartyHasActiveRecurringChargesException`).
- `409` — `"Este fundo possui cobranças recorrentes ativas e não pode ser removido."` (nova, `FundHasActiveRecurringChargesException`).

## Endpoints existentes — inalterados

`GET/POST/PUT/DELETE /api/accounts*`, `/api/parties*`, `/api/funds*`, `/api/groups*` continuam exatamente como estão hoje, exceto o bloqueio adicional de remoção descrito acima. `AccountResponse` **não** ganha um campo novo para expor `recurringCharge` — a referência é interna (idempotência), sem necessidade de aparecer na resposta de `Account` (nenhum requisito pede essa visibilidade).
