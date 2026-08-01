# API Contracts: Edição inline de valor e seleção em intervalo com Shift

## Nenhum contrato REST novo ou alterado

Esta feature é inteiramente frontend. A edição inline do campo "Valor" reaproveita, sem nenhuma alteração de formato, o endpoint já existente:

### `PUT /api/accounts/{id}` (reaproveitado, inalterado)

**Request** (`AccountRequest`, já existente — ver `specs/003-accounts-payable-suppliers` e `specs/005-counterparty-groups`):
```json
{
  "type": "PAYABLE",
  "amount": 150.00,
  "dueDate": "2026-08-10",
  "description": "Manutenção do elevador",
  "fundId": 1,
  "recurring": false,
  "partyId": 3,
  "paymentDate": null,
  "observations": null
}
```
O frontend monta esse corpo a partir dos dados já carregados da linha (`Account` da listagem), alterando apenas `amount`.

**Response 200**: conta atualizada, mesmo formato de `AccountResponse` já existente.

**Erros** (inalterados): `400` — `amount` ausente ou negativo (`"O valor da conta é obrigatório."` / `"O valor da conta não pode ser negativo."`); `400` — tentativa de alterar `type` (`AccountTypeChangeNotAllowedException`); `404` — conta não encontrada.

A seleção em intervalo (Shift+clique) não envolve nenhuma chamada de API — é um estado inteiramente local ao frontend (ver `contracts/frontend-interfaces.md` e `data-model.md`).
