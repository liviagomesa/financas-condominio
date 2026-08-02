# API Contracts: Pagamento Parcial de Contas

## Endpoint alterado: `POST /api/accounts/{id}/pay`

Rota já existente (registrar pagamento de uma conta), sem mudança de método ou caminho. O corpo da requisição (`AccountPaymentRequest`) ganha um campo novo, opcional. Ver research.md ("O campo `paidAmount` é adicionado ao `AccountPaymentRequest` já existente") para a justificativa de não criar uma rota nova.

**Request** (`AccountPaymentRequest`, alterado):
```json
{
  "paymentDate": "2026-07-15",
  "paidAmount": 70.00
}
```
- `paymentDate` (`LocalDate`, obrigatório, já existente): data em que o pagamento foi efetuado.
- `paidAmount` (`BigDecimal`, **novo**, opcional): valor efetivamente pago.
  - Omitido ou `null` → equivalente ao comportamento atual do endpoint (paga o valor total devido da conta, sem criar contas novas).
  - Informado e `> 0`: comparado ao valor devido da conta (ver "Regras aplicadas pelo backend" abaixo).
  - Informado e `≤ 0` → rejeitado com `400` (ver "Erros").

**Response 200** (`AccountResponse`, formato já existente mas sem o campo `recurring`, removido nesta feature — ver abaixo — a conta cujo pagamento foi registrado):
```json
{
  "id": 42,
  "type": "PAYABLE",
  "amount": 70.00,
  "dueDate": "2026-07-10",
  "description": "Taxa condominial - parte 1",
  "fund": { "...": "FundResponse já existente" },
  "paymentDate": "2026-07-15",
  "observations": null,
  "party": { "...": "PartyResponse já existente" }
}
```
Exemplo acima considera uma conta original "Taxa condominial", valor devido `100.00`, vencimento `2026-07-10`, paga parcialmente em `2026-07-15` com `paidAmount: 70.00` — resultando também numa segunda conta "Taxa condominial - parte 2" (valor `30.00`, mesma data de vencimento, pendente), **não** incluída nesta resposta (ver "Conta nova do split não é retornada por este endpoint" abaixo).

**Regras aplicadas pelo backend** (`AccountService.registerPayment`, ver data-model.md):
- `paidAmount` ausente ou igual ao valor devido → só `paymentDate` é definido; `amount`, `description` e `observations` permanecem inalterados; nenhuma conta nova é criada.
- `paidAmount` menor que o valor devido → a conta `{id}` tem `amount` reduzido para `paidAmount`, `paymentDate` definido, e a `description` ganha o sufixo `- parte N` (se ainda não tiver um) ou permanece como está (se já tiver); uma nova conta é criada com `amount = devido − paidAmount`, mesma `dueDate`, `paymentDate = null`, mesma `description`-base com o próximo número de parte, mesmo `fund`/`party`/`type`/`observations` da conta `{id}`.
- `paidAmount` maior que o valor devido → a conta `{id}` tem `amount` ajustado para `paidAmount`, `paymentDate` definido, e `observations` recebe uma nota do excedente (ex.: `"pago R$1,00 a mais"`), acrescentada ao conteúdo já existente sem sobrescrevê-lo; nenhuma conta nova é criada.

**Erros**:
- `404` — conta `{id}` não encontrada (`"Conta não encontrada."`, mesma mensagem já usada por `findById`/`update`/`delete`/`duplicate`).
- `400` — `paymentDate` ausente (`"A data de pagamento é obrigatória."`, já existente) ou `paidAmount` informado com valor `≤ 0` (`"O valor pago deve ser maior que zero."`, novo), no formato padronizado `{ message, status }` já produzido pelo `GlobalExceptionHandler` para qualquer `@Valid` que falhe.

## Conta nova do split não é retornada por este endpoint

Diferente de `POST /api/accounts/{id}/duplicate` (que retorna `201` com a cópia recém-criada), `POST /{id}/pay` continua retornando `200` com a `AccountResponse` de uma única conta — a que teve o pagamento registrado. Quando o pagamento resulta em split, a segunda conta (saldo restante) existe no banco mas não aparece nesta resposta; o frontend já recarrega a listagem inteira (`GET /api/accounts`) após qualquer confirmação de pagamento, revelando as duas contas resultantes sem depender do corpo desta resposta.

## `POST /api/accounts`, `POST /api/accounts/bulk` e `PUT /api/accounts/{id}` — perdem o campo `recurring`

Rotas, métodos e demais campos inalterados. O corpo da requisição (`AccountRequest`/`AccountBulkRequest`) e o corpo de resposta (`AccountResponse`) deixam de ter o campo `recurring` (FR-010) — como o projeto não configura um `ObjectMapper` customizado, o Jackson padrão do Spring Boot rejeita propriedades desconhecidas (`FAIL_ON_UNKNOWN_PROPERTIES`, habilitado por padrão); qualquer chamador que ainda envie `recurring` no corpo passa a receber `400`, já no formato padronizado `{ message, status }` (tratado genericamente pelo `HttpMessageNotReadableException` do `GlobalExceptionHandler`, sem código novo necessário). Único consumidor deste contrato hoje é o próprio frontend do projeto, atualizado nesta mesma feature para não enviar mais o campo.

## Endpoints existentes — inalterados

`GET /api/accounts`, `GET /api/accounts/{id}`, `POST /api/accounts/{id}/duplicate` e `DELETE /api/accounts/{id}` permanecem exatamente como estão hoje — nenhum deles é usado ou alterado por esta feature.
