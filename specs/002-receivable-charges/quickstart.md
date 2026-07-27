# Quickstart: Lançamentos de Contas a Receber

## Pré-requisitos

- Ambiente da feature 001 já rodando (ver [specs/001-cadastro-condominos/quickstart.md](../001-cadastro-condominos/quickstart.md)): Docker Desktop, Java 21 + Maven, Node.js 24 LTS + Angular CLI.
- Ao menos uma unidade cadastrada (feature 001) para os cenários individuais; ao menos duas para o cenário de lote.

## 1. Subir o banco de dados

Na raiz do projeto:

```powershell
docker compose up -d
```

## 2. Subir o backend

```powershell
cd backend
mvn spring-boot:run
```

API disponível em `http://localhost:8080/api`.

## 3. Rodar os testes automatizados do backend

```powershell
cd backend
mvn test
```

## 4. Subir o frontend

```powershell
cd frontend
npm install
npm start
```

Acesse `http://localhost:4200`.

## 5. Rodar os testes automatizados do frontend

```powershell
cd frontend
npm test
```

## Roteiro de validação manual (ponta a ponta)

Os cenários abaixo espelham os Acceptance Scenarios do [spec.md](./spec.md). Execute na
ordem para validar o fluxo completo. Pressupõe a unidade "Bloco A - 101" já cadastrada
(feature 001); para o cenário 4, cadastre também "Bloco A - 102" e "Bloco A - 103".

1. **Lançar conta a receber individual** (US1): na unidade "Bloco A - 101", lance uma conta
   a receber com valor "R$ 350,00", vencimento "10/08/2026", descrição "Taxa condominial -
   Agosto/2026", conta destino "Piscina" e marque a caixa "Recorrente". Confirme que aparece
   na listagem de lançamentos da unidade (`GET /api/receivables?unitId={id}` — ver
   [contracts/api.md](./contracts/api.md)) e que a API retorna `dueDate` no formato
   `2026-08-10` (ISO), mesmo a UI exibindo `10/08/2026`.
2. **Bloquear lançamento inválido** (US1): tente lançar com valor zero ou negativo, ou sem
   preencher algum campo obrigatório (vencimento, descrição, conta destino). Confirme que o
   sistema rejeita e indica o motivo; confirme que a caixa "Recorrente" nunca aparece como
   pendência (sempre tem um valor, desmarcada por padrão).
3. **Bloquear lançamento sem unidade cadastrada**: em uma base sem nenhuma unidade, tente
   lançar uma conta a receber e confirme que o sistema orienta a cadastrar uma unidade
   primeiro.
4. **Lançar em lote para todas as unidades** (US2): com "Bloco A - 101", "Bloco A - 102" e
   "Bloco A - 103" cadastradas, use a ação "lançar para todas as unidades" com valor
   "R$ 350,00", vencimento "10/08/2026", descrição "Taxa condominial - Agosto/2026", conta
   destino "Piscina" e tipo "Recorrente". Confirme que cada uma das três unidades passa a ter
   um lançamento independente com os mesmos dados (`POST /api/receivables/bulk`).
5. **Lote não afeta unidade cadastrada depois**: após o passo 4, cadastre uma nova unidade
   "Bloco A - 104" e confirme que ela não recebe retroativamente o lançamento do lote
   anterior.
6. **Listar por unidade** (US3): acesse a listagem de lançamentos de "Bloco A - 101" e
   confirme que valor, vencimento, descrição, conta destino e tipo aparecem corretamente;
   acesse a listagem de uma unidade sem lançamentos e confirme a indicação de lista vazia.
7. **Editar lançamento** (US4): edite o valor de um lançamento de "R$ 350,00" para
   "R$ 370,00" e confirme que a listagem reflete o novo valor; tente editar para um valor
   zero/negativo e confirme rejeição.
8. **Remover lançamento** (US4): remova um lançamento, confirmando a caixa de diálogo, e
   verifique que ele some da listagem da unidade correspondente.
9. **Bloquear remoção de unidade com lançamento vinculado** (impacto cruzado com a feature
   001 — só validar após a aprovação e implementação da extensão de `UnitService.delete()`
   descrita em research.md/plan.md desta feature): tente remover uma unidade que ainda possui
   lançamentos e confirme que o sistema rejeita, informando o vínculo.
10. **Registrar pagamento** (US5): registre o pagamento de um lançamento pendente informando
    a data "15/08/2026" e confirme que ele passa a aparecer como pago na listagem, com a data
    exibida; registre novamente com outra data e confirme que ela é atualizada; tente registrar
    sem informar a data e confirme rejeição; edite e depois remova um lançamento já pago e
    confirme que ambas as operações funcionam normalmente, sem bloqueio.
11. **Remover lançamentos em lote** (FR-019): na listagem, selecione dois ou mais lançamentos
    usando os checkboxes da tabela e use a ação "Remover selecionados"; confirme que todos
    somem da listagem. Se possível, repita selecionando um lançamento de uma unidade que será
    removida no mesmo teste (ver cenário 9) para confirmar o comportamento de melhor esforço
    (o que puder ser removido é removido; o que falhar é reportado).

## Referências

- Contratos de API: [contracts/api.md](./contracts/api.md)
- Modelo de dados: [data-model.md](./data-model.md)
- Decisões técnicas: [research.md](./research.md)
