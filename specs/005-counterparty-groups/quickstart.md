# Quickstart: Unificação de Unidade/Fornecedor, Contas sem Restrição de Tipo e Grupos

## Pré-requisitos

- Ambiente das features 001–004 já rodando (ver [specs/004-fund-entity-balance/quickstart.md](../004-fund-entity-balance/quickstart.md)): Docker Desktop, Java 21 + Maven, Node.js 24 LTS + Angular CLI.
- Migrations desta feature aplicadas (V10–V13, ver data-model.md) — rodam automaticamente no `mvn spring-boot:run` (Flyway). **Atenção**: a migration V12 trunca a tabela `account` e a V13 remove as tabelas `unit`/`supplier` por completo (banco de desenvolvimento, sem dado real a preservar — ver research.md); qualquer unidade, fornecedor ou conta cadastrada em sessões de teste anteriores desta feature será perdida ao aplicar essas migrations.
- Nenhuma Parte ou Grupo pré-cadastrado após a migration — o roteiro abaixo cadastra o que for usado nos cenários.

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

## Roteiro de validação manual (ponta a ponta)

Os cenários abaixo espelham os Acceptance Scenarios do [spec.md](./spec.md).

1. **Cadastrar Partes** (FR-003/FR-004): acesse `/parties`, cadastre duas Partes: "Bloco A - 101" (sem chave pix) e "Construtora XYZ" (com chave pix). Confirme que ambas aparecem numa única listagem, sem distinção de "unidade" ou "fornecedor".
2. **Bloquear nome duplicado** (FR-004): tente cadastrar outra Parte também chamada "Bloco A - 101" (varie maiúsculas/minúsculas e espaços). Confirme a rejeição.
3. **Lançar uma conta de SAÍDA para uma Parte qualquer** (US1, FR-001): acesse `/accounts/new`, selecione tipo "Saída", modo "Parte específica", e escolha "Bloco A - 101" (a Parte que antes seria só uma unidade/ENTRADA). Confirme que o sistema aceita o lançamento normalmente.
4. **Lançar uma conta de ENTRADA para a mesma Parte** (US1, FR-001): lance uma segunda conta, tipo "Entrada", para a mesma "Bloco A - 101". Confirme que não há conflito com a conta de SAÍDA já lançada no passo 3.
5. **Tentar alterar o tipo de uma conta existente** (FR-002): edite a conta do passo 3 e tente mudar o tipo de "Saída" para "Entrada". Confirme que o sistema continua bloqueando essa alteração.
6. **Ver o total líquido na tela de Contas** (US2, FR-007/FR-008): acesse `/accounts` sem filtros. Confirme que a linha de total ao final da tabela exibe `Σ ENTRADA − Σ SAÍDA` das contas listadas (com os valores dos passos 3–4, o total deve refletir a diferença entre as duas). Aplique um filtro (ex.: por status) e confirme que o total muda de acordo. Lance uma nova conta e confirme que o total é atualizado sem recarregar a página manualmente.
7. **Total negativo, sem bloqueio** (US2, cenário 6): lance mais contas de SAÍDA do que de ENTRADA para a mesma seleção de filtros, de forma que o total fique negativo. Confirme que o sistema exibe o valor negativo normalmente, sem nenhum aviso ou bloqueio.
8. **Filtrar por Fundo** (US3, FR-009): cadastre um segundo fundo (`/funds/new`), lance uma conta nele, e confirme que o novo filtro "Fundo" em `/accounts` restringe a listagem corretamente, combinando com outros filtros já ativos (ex.: tipo).
9. **Filtrar por Parte** (US4, FR-010/FR-011): confirme que o filtro antes chamado "Unidade" em `/accounts` agora se chama "Parte", lista todas as Partes cadastradas (unidades e fornecedores antigos misturados), e que a coluna antes chamada "Contraparte" agora exibe o rótulo "Parte".
10. **Criar um Grupo e adicionar integrantes** (US5, FR-012/FR-013): acesse `/groups`, crie o grupo "Bloco A" e adicione "Bloco A - 101" e uma segunda Parte cadastrada a ele, pela própria tela de edição do grupo. Confirme que o cadastro de Parte (`/parties/.../edit`) não exibe nem permite editar a quais grupos ela pertence.
11. **Lançar uma conta em lote para um Grupo** (US5, FR-014): em `/accounts/new`, alterne o modo de lançamento para "Grupo", selecione "Bloco A", preencha os demais campos uma única vez e salve. Confirme que uma conta separada foi criada para cada integrante do grupo, todas com os mesmos dados (valor, vencimento, fundo, descrição).
12. **Bloquear lançamento em lote para grupo vazio** (US5, FR-015): crie um grupo novo sem integrantes e tente lançar uma conta em lote para ele. Confirme que o sistema informa que o grupo está vazio e não cria nenhuma conta.
13. **Remover uma Parte de um Grupo sem excluí-la** (US5, cenário 4): remova "Bloco A - 101" do grupo "Bloco A" pela tela do grupo. Confirme que ela continua existindo normalmente em `/parties` e que as contas já lançadas para ela (passos 3–4) permanecem inalteradas.
14. **Excluir um Grupo com integrantes** (Edge Cases): exclua o grupo "Bloco A" mesmo com integrantes. Confirme que a exclusão é permitida e que as contas já lançadas em lote no passo 11 continuam existindo normalmente, vinculadas diretamente às suas Partes.
15. **Bloquear remoção de Parte com contas vinculadas** (FR-006): tente remover "Bloco A - 101" (com contas vinculadas). Confirme o bloqueio, com mensagem explicando o vínculo.

## Referências

- Contratos de API: [contracts/api.md](./contracts/api.md)
- Modelo de dados: [data-model.md](./data-model.md)
- Decisões técnicas: [research.md](./research.md)
