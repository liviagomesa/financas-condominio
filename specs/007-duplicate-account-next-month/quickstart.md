# Quickstart: Duplicar lançamentos para o mês seguinte

## Pré-requisitos

- Ambiente das features 001–006 já rodando (ver [specs/006-inline-edit-shift-select/quickstart.md](../006-inline-edit-shift-select/quickstart.md)): Docker Desktop, Java 21 + Maven, Node.js 24 LTS + Angular CLI.
- Nenhuma migration nova, nenhuma mudança de schema — o banco de dados das features anteriores pode ser reaproveitado sem reset.
- Ao menos 3 contas cadastradas em `/accounts` (tipos e vencimentos variados; pelo menos uma com vencimento em dia 29, 30 ou 31, para testar a virada de mês curto).

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

Deve incluir os novos casos de `AccountServiceTest` para `duplicate` (ver research.md).

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

Deve incluir o novo `shared/bulk-duplicate.spec.ts` (ver research.md).

## Roteiro de validação manual (ponta a ponta)

Os cenários abaixo espelham os Acceptance Scenarios do [spec.md](./spec.md).

1. **Duplicar mantendo o valor, via botão** (US1, FR-002/FR-004): acesse `/accounts`, selecione um lançamento e clique em "Duplicar para o mês seguinte". Confirme que surge uma nova conta com vencimento um mês depois, mesmo valor, mesma descrição, fundo e contraparte, e sem data de pagamento — ajuste o filtro de mês para enxergá-la, já que o filtro atual não muda sozinho (FR-008).
2. **Original permanece intocado** (Acceptance Scenario 3): após o passo 1, confira o lançamento original — deve continuar com o mesmo valor, vencimento e status de pagamento de antes.
3. **Duplicar vários de uma vez, com vencimentos em meses diferentes** (Acceptance Scenario 2): selecione dois lançamentos com vencimentos em meses diferentes e clique em "Duplicar para o mês seguinte". Confirme que cada cópia tem vencimento um mês após o vencimento do seu próprio original.
4. **Virada de mês curto** (Edge Case): duplique um lançamento com vencimento em 31 (ou 30/29) e confirme que a cópia cai no último dia válido do mês seguinte.
5. **Duplicar com valor zerado, via botão** (US2, FR-003/FR-005): selecione um lançamento com valor diferente de zero e clique em "Duplicar para o mês seguinte com valor zerado". Confirme que a cópia é criada com `R$ 0,00` e os demais dados idênticos ao original.
6. **Duplicar via atalho Ctrl+C / Ctrl+V** (FR-009, Acceptance Scenario 4): selecione um ou mais lançamentos, pressione Ctrl+C e depois Ctrl+V. Confirme que o resultado é idêntico ao do botão "Duplicar para o mês seguinte" (valor mantido).
7. **Atalho não interfere na edição inline** (FR-011): clique no campo "Valor" de uma linha para editá-lo (feature 006) e, com o campo em foco, pressione Ctrl+C/Ctrl+V. Confirme que o copiar/colar nativo do campo de texto funciona normalmente e nenhuma duplicação de lançamento é disparada.
8. **Colar sem copiar antes** (Edge Case): recarregue a página e pressione Ctrl+V sem ter pressionado Ctrl+C antes. Confirme que nada acontece.
9. **Falha parcial em lote** (FR-007): com dois lançamentos selecionados, remova o fundo de um deles diretamente no banco (ou torne inválido de outra forma) antes de duplicar, e confirme que o outro é duplicado normalmente e a usuária é avisada de qual falhou e por quê.
10. **Ambos os tipos de lançamento** (Assumptions): repita o passo 1 com uma conta a receber e uma conta a pagar, confirmando que o comportamento é idêntico para os dois tipos.

## Referências

- Contratos: [contracts/api.md](./contracts/api.md) (novo endpoint `POST /api/accounts/{id}/duplicate`) e [contracts/frontend-interfaces.md](./contracts/frontend-interfaces.md) (`BulkActionsBar`, `AccountService`, `bulk-duplicate.ts`, `AccountList`)
- Modelo de dados: [data-model.md](./data-model.md)
- Decisões técnicas: [research.md](./research.md)
