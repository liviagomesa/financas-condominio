# API Contracts: Duplicar lançamentos para o mês seguinte

## Novo endpoint: `POST /api/accounts/{id}/duplicate`

Cria um novo lançamento independente, copiado do lançamento `{id}`, com vencimento um mês depois e pendente de pagamento. Ver research.md ("Nova ação de negócio dedicada") para a justificativa do formato de rota.

**Request** (`AccountDuplicateRequest`, novo):
```json
{
  "zeroAmount": false
}
```
- `zeroAmount` (`boolean`, obrigatório): quando `false`, a cópia mantém o mesmo `amount` do lançamento original ("Duplicar para o mês seguinte"); quando `true`, a cópia recebe `amount = 0` ("Duplicar para o mês seguinte com valor zerado").

**Response 201** (`AccountResponse`, formato já existente — o lançamento recém-criado):
```json
{
  "id": 42,
  "type": "PAYABLE",
  "amount": 150.00,
  "dueDate": "2026-09-10",
  "description": "Manutenção do elevador",
  "fund": { "...": "FundResponse já existente" },
  "recurring": false,
  "paymentDate": null,
  "observations": null,
  "party": { "...": "PartyResponse já existente" }
}
```
Exemplo acima considera um lançamento original com `dueDate: "2026-08-10"` e `zeroAmount: false`.

**Regras aplicadas pelo backend** (`AccountService.duplicate`, ver data-model.md):
- `dueDate` da cópia = `dueDate` do original `+ 1 mês` (`LocalDate.plusMonths`, já trata o caso de dia inexistente no mês seguinte).
- `paymentDate` da cópia = sempre `null`, independentemente do original estar pago ou não.
- `amount` da cópia = `amount` do original, ou `0` quando `zeroAmount = true`.
- Demais campos (`type`, `description`, `fund`, `recurring`, `party`, `observations`) copiados sem alteração.
- O lançamento original (`{id}`) não é modificado por esta chamada.

**Erros**:
- `404` — lançamento `{id}` não encontrado (`"Conta não encontrada."`, mesma mensagem já usada por `findById`/`update`/`delete`).

## Chamadas do frontend (uma por lançamento selecionado, melhor esforço)

Não há endpoint de duplicação em lote. Ao duplicar N lançamentos selecionados (pelo botão ou pelo atalho Ctrl+C/Ctrl+V), o frontend chama `POST /api/accounts/{id}/duplicate` uma vez para cada id, via `shared/bulk-duplicate.ts` (mesmo padrão de melhor esforço de `shared/bulk-delete.ts`): falhas pontuais não impedem as demais chamadas, e o resultado agregado (sucesso/falha por id) é usado para montar a mensagem de erro exibida à usuária quando houver falhas parciais (FR-007).

## Endpoints existentes — inalterados

`GET /api/accounts`, `GET /api/accounts/{id}`, `POST /api/accounts`, `POST /api/accounts/bulk`, `PUT /api/accounts/{id}`, `POST /api/accounts/{id}/pay` e `DELETE /api/accounts/{id}` permanecem exatamente como estão hoje — nenhum deles é usado ou alterado por esta feature.
