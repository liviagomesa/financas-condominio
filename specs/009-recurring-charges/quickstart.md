# Quickstart: Geração Automática de Contas Recorrentes

## Pré-requisitos

- Ambiente das features 001–008 já rodando: Docker Desktop, Java 21 + Maven, Node.js 24 LTS + Angular CLI.
- Duas migrations novas (`V15__create_recurring_charge_table.sql`, `V16__add_recurring_charge_id_to_account.sql`) — rodam automaticamente ao subir o backend (Flyway); não é necessário resetar o banco das features anteriores.
- Ao menos uma parte (`Party`), um fundo (`Fund`) e, para testar o cadastro em lote, um grupo com integrantes já cadastrados.

## 1. Subir o banco de dados

```powershell
docker compose up -d
```

## 2. Subir o backend

```powershell
cd backend
mvn spring-boot:run
```

API disponível em `http://localhost:8082/api`. As migrations `V15`/`V16` rodam automaticamente no start.

## 3. Rodar os testes automatizados do backend

```powershell
cd backend
mvn test
```

Deve incluir `RecurringChargeServiceTest` (CRUD, versionamento por edição, soft delete, cadastro em lote, grupo vazio) e `RecurringChargeGenerationServiceTest` (geração básica, idempotência, ajuste de dia de vencimento em mês curto, isolamento por cobrança, atualização da flag de falha, cálculo do mês-alvo antes/depois do dia 25) — ver research.md. `PartyServiceTest`/`FundServiceTest` devem ter casos novos para o bloqueio de remoção com cobrança recorrente ativa.

## 4. Subir o frontend

```powershell
cd frontend
npm start
```

Acesse `http://localhost:4202`. Um novo item de menu "Cobranças Recorrentes" (ou nome equivalente definido em `tasks.md`) leva a `/recurring-charges`.

## 5. Rodar os testes automatizados do frontend

```powershell
cd frontend
npm test
```

Nenhum arquivo novo de teste unitário é esperado (mudança de frontend restrita a orquestração de componente, validada via Playwright — mesmo precedente das features 006/007/008).

## Roteiro de validação manual (ponta a ponta)

Os cenários abaixo espelham os Acceptance Scenarios do [spec.md](./spec.md). Como o gatilho automático real só dispara às 6h do dia 25 (horário de Brasília), a validação manual do processo de geração em si depende de um mecanismo de disparo acessível em ambiente de desenvolvimento (ex.: um teste de integração chamando `RecurringChargeGenerationService.generatePendingAccounts()` diretamente, ou ajustar temporariamente a data do sistema/`LocalDate` injetado — detalhe de implementação a decidir em `tasks.md`; este roteiro assume que existe alguma forma de disparar o método manualmente durante o desenvolvimento).

1. **Cadastrar cobrança recorrente para contraparte específica** (US1, FR-001): acesse `/recurring-charges/new`, preencha tipo "Entrada" (RECEIVABLE), valor R$350,00, dia de vencimento 10, descrição "Taxa condominial", fundo e uma contraparte específica. Confirme que a cobrança aparece na listagem `/recurring-charges`.
2. **Cadastrar cobrança recorrente para grupo** (US1, FR-003): repita o passo 1 escolhendo um grupo com vários integrantes em vez de uma contraparte específica. Confirme que uma linha aparece na listagem para cada integrante do grupo.
3. **Cadastrar cobrança recorrente com valor zero** (US1, FR-002): repita o passo 1 deixando o valor como R$0,00. Confirme que o cadastro é aceito normalmente.
4. **Grupo vazio bloqueado** (US1, Acceptance Scenario 4): tente cadastrar uma cobrança recorrente para um grupo sem integrantes. Confirme que nenhuma linha é criada e uma mensagem de erro aparece.
5. **Geração automática básica** (US2, FR-004/FR-005/FR-007): com cobranças recorrentes ativas cadastradas, dispare o processo de geração (ver nota acima). Confirme que uma conta é criada em `/accounts` para cada cobrança ativa, com vencimento no mês seguinte, e que uma cobrança com dia de vencimento 31 gera conta no último dia de um mês mais curto.
6. **Idempotência** (US2, FR-006): dispare o processo de geração uma segunda vez, sem alterar nada. Confirme que nenhuma conta duplicada é criada.
7. **Isolamento por falha** (US2, FR-016, Acceptance Scenario 5): provoque uma falha de geração para uma cobrança específica (ex.: removendo o fundo referenciado diretamente no banco, contornando o bloqueio de remoção, só para fins de teste) e dispare a geração. Confirme que as demais cobranças ativas ainda geram conta normalmente.
8. **Indicador de falha na tela de gerenciamento** (US5, FR-017/FR-018, Acceptance Scenarios 3–4): após o cenário 7, abra `/recurring-charges` e confirme que a cobrança que falhou exibe um aviso visível, enquanto as demais não exibem nada. Corrija a causa da falha e dispare a geração novamente; confirme que o aviso desaparece.
9. **Editar uma cobrança recorrente sem afetar contas antigas** (US3, FR-008): edite o valor de uma cobrança recorrente que já gerou pelo menos uma conta (ex.: de R$300,00 para R$330,00). Confirme em `/accounts` que a conta antiga continua com o valor original, e que a próxima geração usa o valor novo. Confirme em `/recurring-charges` que só a versão nova aparece na listagem.
10. **Remover uma cobrança recorrente sem perder histórico** (US4, FR-009): remova uma cobrança recorrente que já gerou contas. Confirme que ela some de `/recurring-charges`, que as contas já geradas continuam em `/accounts`, e que a próxima geração não cria mais nenhuma conta a partir dela.
11. **Bloqueio de remoção de parte/fundo vinculado** (FR-014): tente remover, em `/parties` ou `/funds`, uma contraparte ou fundo referenciado por uma cobrança recorrente ativa. Confirme que a remoção é bloqueada com uma mensagem específica.
12. **Ações em lote na listagem** (US5, FR-010): em `/recurring-charges`, selecione várias linhas (com suporte a Shift+clique, mesmo padrão das demais listagens) e acione a remoção em lote. Confirme que todas as selecionadas são removidas.
13. **Recuperação na inicialização** (FR-015): pare o backend antes do dia 25 rodar, avance a data do ambiente (ou aguarde) até depois do dia 25 sem ter disparado a geração, e suba o backend novamente. Confirme, pelos logs ou pela ausência do aviso de falha nas cobranças ativas, que a geração roda automaticamente na inicialização, cobrindo o ciclo perdido.

## Referências

- Contratos: [contracts/api.md](./contracts/api.md) (`/api/recurring-charges` — novo)
- Modelo de dados: [data-model.md](./data-model.md)
- Decisões técnicas: [research.md](./research.md)
