# Quickstart: Pagamento Parcial de Contas

## Pré-requisitos

- Ambiente das features 001–007 já rodando (ver [specs/007-duplicate-account-next-month/quickstart.md](../007-duplicate-account-next-month/quickstart.md)): Docker Desktop, Java 21 + Maven, Node.js 24 LTS + Angular CLI.
- Nenhuma migration nova, nenhuma mudança de schema — o banco de dados das features anteriores pode ser reaproveitado sem reset.
- Ao menos uma conta pendente cadastrada em `/accounts`, com valor e vencimento conhecidos, para os testes de pagamento parcial e a maior.

## 1. Subir o banco de dados

```powershell
docker compose up -d
```

## 2. Subir o backend

```powershell
cd backend
mvn spring-boot:run
```

API disponível em `http://localhost:8082/api`.

## 3. Rodar os testes automatizados do backend

```powershell
cd backend
mvn test
```

Deve incluir os novos casos de `AccountServiceTest` para `registerPayment` (ver research.md): valor igual, valor menor (com e sem sufixo prévio), valor maior, valor zero/negativo rejeitado. Os testes já existentes que hoje passam `recurring` para `create`/`createForGroup`/`update`/`duplicate` devem continuar passando após o ajuste de assinatura (FR-010).

## 4. Subir o frontend

```powershell
cd frontend
npm start
```

Acesse `http://localhost:4202`.

## 5. Rodar os testes automatizados do frontend

```powershell
cd frontend
npm test
```

Nenhum arquivo novo de teste é esperado nesta feature (mudança de frontend restrita a orquestração de componente, validada via Playwright — ver research.md); o comando deve apenas continuar passando sem regressão.

## Roteiro de validação manual (ponta a ponta)

Os cenários abaixo espelham os Acceptance Scenarios do [spec.md](./spec.md).

1. **Caixa de valor pago pré-preenchida** (US1, FR-001): acesse `/accounts`, clique em "Registrar pagamento" de uma conta pendente. Confirme que aparece uma caixa de valor pago, ao lado da data, já preenchida com o valor total devido.
2. **Pagamento integral sem alterar o valor** (US1, FR-002): confirme o pagamento sem tocar na caixa de valor pago. A conta deve passar a constar como paga, com o mesmo valor e a mesma descrição de antes — nenhuma conta nova deve aparecer na listagem.
3. **Pagamento parcial, primeira divisão** (US2, FR-003/FR-004): registre o pagamento de uma conta "Taxa condominial" (valor R$100,00, vencimento 10/07/2026) informando R$70,00 na caixa de valor pago e uma data de pagamento. Confirme que a conta original passa a se chamar "Taxa condominial - parte 1", com valor R$70,00, marcada como paga; e que uma nova conta "Taxa condominial - parte 2" aparece na listagem, com valor R$30,00, mesmo vencimento (10/07/2026), pendente, sem marcação de recorrência.
4. **Pagamento parcial sucessivo, numeração continua** (US2, FR-003a): registre um novo pagamento parcial sobre a conta "Taxa condominial - parte 2" (valor R$20,00 de R$30,00 devidos). Confirme que essa conta mantém o nome "Taxa condominial - parte 2" (apenas passa a valer R$20,00 e ser marcada como paga), e que uma nova conta "Taxa condominial - parte 3" aparece com o saldo de R$10,00, mesmo vencimento, pendente.
5. **Pagamento a maior** (US3, FR-005): registre o pagamento de uma conta pendente de R$500,00 informando R$501,00. Confirme que a conta passa a valer R$501,00, é marcada como paga, e o campo de observações passa a conter um registro equivalente a "pago R$1,00 a mais" — nenhuma conta nova é criada.
6. **Pagamento a maior preserva observações existentes** (US3, Acceptance Scenario 2): repita o passo 5 numa conta que já tenha um texto em observações. Confirme que o texto original continua lá, com a nota de excedente acrescentada, não sobrescrevendo o que já existia.
7. **Valor pago zero é ignorado** (Edge Case, FR-009): abra o registro de pagamento de uma conta pendente, apague o valor da caixa (ou digite 0) e confirme. Nada deve acontecer — a conta continua pendente, sem data de pagamento e sem nenhuma conta nova criada.
8. **Caixa de valor pago não aparece ao alterar um pagamento já registrado** (FR-006): numa conta já paga, clique em "Alterar pagamento". Confirme que só a data é editável — nenhuma caixa de valor pago aparece nesse fluxo.
9. **Ambos os tipos de conta** (Assumptions): repita o passo 3 com uma conta a receber e uma conta a pagar, confirmando que o comportamento é idêntico para os dois tipos.
10. **Campo "Recorrente" removido** (FR-010): abra o formulário de criação/edição de uma conta em `/accounts/new` (ou editando uma existente). Confirme que não há mais nenhuma checkbox "Recorrente" no formulário, e que criar/editar uma conta funciona normalmente sem ela.
11. **Contas do split continuam totalmente utilizáveis** (FR-007): edite o valor de "Taxa condominial - parte 1" pela edição inline já existente, e remova "Taxa condominial - parte 3" (ou a conta pendente restante de qualquer split feito nos passos anteriores). Confirme que ambas as ações funcionam exatamente como em qualquer outra conta, sem nenhuma restrição especial.

## Referências

- Contratos: [contracts/api.md](./contracts/api.md) (`POST /api/accounts/{id}/pay` alterado, campo `paidAmount` novo)
- Modelo de dados: [data-model.md](./data-model.md)
- Decisões técnicas: [research.md](./research.md)
