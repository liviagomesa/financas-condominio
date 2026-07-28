# API Contracts: Lançamentos de Contas a Receber

> **Substituído pela feature 003** (`specs/003-accounts-payable-suppliers/contracts/api.md`)
> — `/api/receivables*` (documentado abaixo) deixou de existir, substituído por
> `/api/accounts*`, que também passa a suportar contas a pagar a fornecedores. Contrato
> preservado abaixo como registro histórico.

Base path: `/api`

Formato de erro padrão (todas as respostas 4xx), mensagens em português (Convenções de API
REST):

```json
{
  "message": "O valor do lançamento deve ser maior que zero.",
  "status": 400
}
```

Datas (`dueDate`, `paymentDate`) trafegam em JSON no formato ISO-8601 padrão (`yyyy-MM-dd`),
sem anotação `@JsonFormat` customizada — o formato `DD/MM/AAAA` é aplicado só pelo frontend,
na exibição e na leitura de entrada da usuária (ver research.md, revisado 2026-07-26 —
Princípio IV da constituição).

JSON malformado, `targetAccount` fora do conjunto fixo do enum ou `dueDate`/`paymentDate` fora
do formato `yyyy-MM-dd` também retornam `400` no mesmo formato padrão acima (mensagem
genérica, ex.: "Dados inválidos."), via handler dedicado de `HttpMessageNotReadableException`
— ver tasks.md T010.

## Lançamentos — `/api/receivables`

### `GET /api/receivables`

Lista lançamentos de contas a receber (FR-006). Aceita filtros opcionais, combináveis entre si
(E lógico).

**Query params** (todos opcionais):
- `unitId`: retorna apenas os lançamentos da unidade indicada (US3).
- `paid` (`true`/`false`): retorna só lançamentos pagos ou só pendentes (FR-020). "Pago"
  significa `paymentDate` preenchido — não há campo `paid` na resposta (ver abaixo).
- `overdue` (`true`): retorna só lançamentos pendentes (`paymentDate` nulo) com `dueDate`
  anterior à data atual (FR-021).
- `dueYearMonth` (`yyyy-MM`, ex.: `2026-08`): retorna só lançamentos cujo `dueDate` cai nesse
  mês/ano (FR-022).
- `paymentYearMonth` (`yyyy-MM`): retorna só lançamentos pagos cujo `paymentDate` cai nesse
  mês/ano (FR-023).

Quando nenhum filtro é informado, retorna todos os lançamentos cadastrados.

**Response 200**:
```json
[
  {
    "id": 1,
    "amount": 350.00,
    "dueDate": "2026-08-10",
    "description": "Taxa condominial - Agosto/2026",
    "targetAccount": "POOL",
    "recurring": true,
    "paymentDate": null,
    "unit": { "id": 1, "identifier": "Bloco A - 101" }
  }
]
```
Lista vazia (`[]`) quando não há lançamentos, ou nenhum corresponde aos filtros informados
(FR-007). Não existe campo `paid` na resposta — um lançamento está pago quando `paymentDate`
não é `null` (FR-018).

**Erros**:
- `404` — `unitId` informado no filtro não corresponde a nenhuma unidade cadastrada.
- `400` — `dueYearMonth`/`paymentYearMonth` fora do formato `yyyy-MM`, ou `paid`/`overdue` fora
  de `true`/`false`.

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
  "dueDate": "2026-08-10",
  "description": "Taxa condominial - Agosto/2026",
  "targetAccount": "POOL",
  "recurring": true,
  "paymentDate": null,
  "unitId": 1
}
```
`paymentDate` é opcional (FR-015): quando omitido ou `null`, o lançamento é criado pendente;
quando informado, é criado já como pago com essa data — sem precisar de uma segunda chamada a
`POST /api/receivables/{id}/pay`.

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
  "dueDate": "2026-08-10",
  "description": "Taxa condominial - Agosto/2026",
  "targetAccount": "POOL",
  "recurring": true,
  "paymentDate": null
}
```
Sem `unitId` — aplica-se a todas as unidades existentes no momento da chamada. `paymentDate`
opcional, mesmo comportamento do `POST /api/receivables` (FR-015): quando informado, todos os
lançamentos do lote são criados já como pagos com essa mesma data.

**Response 201**: array com um lançamento criado por unidade, no mesmo formato do `GET`.
```json
[
  { "id": 1, "amount": 350.00, "dueDate": "2026-08-10", "description": "Taxa condominial - Agosto/2026", "targetAccount": "POOL", "recurring": true, "paymentDate": null, "unit": { "id": 1, "identifier": "Bloco A - 101" } },
  { "id": 2, "amount": 350.00, "dueDate": "2026-08-10", "description": "Taxa condominial - Agosto/2026", "targetAccount": "POOL", "recurring": true, "paymentDate": null, "unit": { "id": 2, "identifier": "Bloco A - 102" } }
]
```

**Erros**:
- `400` — mesmas validações de campo do `POST /api/receivables` (FR-001, FR-003, FR-013),
  exceto `unitId` (não se aplica).
- `409` — nenhuma unidade cadastrada no sistema (orienta a cadastrar uma unidade primeiro —
  FR-011).

### `PUT /api/receivables/{id}`

Edita valor, vencimento, descrição, conta destino, tipo e/ou data de pagamento de um
lançamento (FR-008).

**Request**: igual ao `POST /api/receivables` (inclui `unitId`; a unidade associada também
pode ser trocada; `paymentDate` também pode ser alterado aqui, embora `POST /{id}/pay` seja o
atalho dedicado para só marcar/atualizar o pagamento sem reenviar o lançamento inteiro).

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

> **Remoção em lote (US5/FR-019)**: não há endpoint de remoção em lote. O frontend chama este
> `DELETE /{id}` uma vez para cada lançamento selecionado na listagem, agregando o resultado
> (melhor esforço) — ver research.md.

### `POST /api/receivables/{id}/pay`

Registra (ou atualiza) o pagamento de um lançamento (FR-015/FR-016, US5).

**Request**:
```json
{
  "paymentDate": "2026-08-15"
}
```

**Response 200**: lançamento atualizado, no mesmo formato do `GET`, com `paymentDate` igual ao
valor informado (o que já o torna "pago", já que não há campo `paid` separado).

**Erros**:
- `400` — `paymentDate` ausente ou fora do formato `yyyy-MM-dd`.
- `404` — lançamento `{id}` não encontrado (FR-010).

> Chamar esta rota novamente sobre um lançamento já pago apenas atualiza `paymentDate` — não
> existe rota para desfazer/estornar um pagamento nesta versão (ver Assumptions do spec.md).
> Editar (`PUT`) ou remover (`DELETE`) um lançamento pago continua funcionando normalmente,
> sem nenhuma restrição adicional (FR-017).

## Impacto em `/api/units/{id}` (feature 001)

`DELETE /api/units/{id}` passa a também retornar `409` quando a unidade possuir lançamentos
de contas a receber vinculados (FR-012 desta feature). Este contrato pertence à feature 001
(`specs/001-cadastro-condominos/contracts/api.md`) e só deve ser atualizado lá após a
aprovação do processo de "Edição de Features Já Implementadas" (ver plan.md e research.md
desta feature).
