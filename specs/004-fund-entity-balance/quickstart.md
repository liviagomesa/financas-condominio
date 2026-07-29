# Quickstart: Fundos como Entidade e Visualização de Saldo Real

## Pré-requisitos

- Ambiente das features 001–003 já rodando (ver [specs/003-accounts-payable-suppliers/quickstart.md](../003-accounts-payable-suppliers/quickstart.md)): Docker Desktop, Java 21 + Maven, Node.js 24 LTS + Angular CLI.
- Migrations desta feature aplicadas (V8–V9, ver data-model.md) — rodam automaticamente no `mvn spring-boot:run` (Flyway). **Atenção**: a migration V9 trunca a tabela `account` (banco de desenvolvimento, sem dado real a preservar — ver research.md); qualquer conta lançada em sessões de teste anteriores desta feature será perdida ao aplicar esta migration.
- Nenhum fundo pré-cadastrado após a migration — o roteiro abaixo cadastra os fundos usados nos cenários.

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

Os cenários abaixo espelham os Acceptance Scenarios do [spec.md](./spec.md). Pressupõe ao menos uma unidade cadastrada (feature 001) para lançar contas a receber de teste.

1. **Cadastrar um fundo** (US2): acesse `/funds`, cadastre o fundo "Piscina" com saldo inicial "R$ 500,00". Confirme que ele aparece na listagem com saldo real igual a R$ 500,00 (nenhum lançamento vinculado ainda).
2. **Bloquear nome duplicado** (US2): tente cadastrar outro fundo também chamado "Piscina" (varie maiúsculas/minúsculas e espaços, ex.: " piscina "). Confirme a rejeição em ambos os casos.
3. **Ver o saldo real refletir só lançamentos efetivados** (US1): com o fundo "Piscina" cadastrado, lance uma conta a receber de R$ 350,00 vinculada a ele e registre o recebimento; lance uma segunda conta a pagar de R$ 100,00 vinculada ao mesmo fundo, mas **sem** registrar o pagamento. Acesse `/funds` e confirme que o saldo real de "Piscina" é R$ 850,00 (500 + 350, sem descontar o pagamento em aberto).
4. **Registrar o pagamento em aberto e ver o saldo cair** (US1): registre o pagamento da conta de R$ 100,00 do passo 3. Confirme que o saldo real de "Piscina" passa a R$ 750,00.
5. **Ver o saldo total somado** (US1): cadastre um segundo fundo ("Jardim") sem saldo inicial (deixe em branco/zero) e sem lançamentos. Confirme que a tela de fundos exibe o saldo total somado de todos os fundos (R$ 750,00 + R$ 0,00 = R$ 750,00).
6. **Saldo pode ficar negativo, sem bloqueio** (FR-012): no fundo "Jardim", lance e pague uma conta a pagar de valor maior que o saldo disponível (ex.: R$ 50,00). Confirme que o sistema permite normalmente e que o saldo real de "Jardim" passa a ser negativo (ex.: -R$ 50,00), sem nenhum aviso ou bloqueio.
7. **Fundo aparece no lançamento de contas** (FR-006): ao lançar uma nova conta (a receber ou a pagar), confirme que o seletor de fundo lista "Piscina" e "Jardim" (carregado dinamicamente, não mais uma lista fixa).
8. **Editar nome e saldo inicial** (US3): edite o fundo "Jardim" para "Jardim Lateral" e ajuste seu saldo inicial. Confirme que o novo nome aparece na listagem de fundos e em qualquer conta já vinculada a ele, e que o saldo real é recalculado a partir do novo saldo inicial.
9. **Bloquear remoção de fundo em uso** (US3): tente remover o fundo "Piscina" (com contas vinculadas). Confirme o bloqueio, com mensagem explicando o vínculo.
10. **Remover fundo sem uso** (US3): cadastre um terceiro fundo sem nenhum lançamento vinculado e remova-o. Confirme que desaparece da listagem.
11. **Ambiente novo sem fundos pré-cadastrados** (FR-009): confirme que, logo após a migration desta feature, `GET /api/funds` retorna uma lista vazia — nenhum dos três fundos antigos (Piscina, Jardim Piscina, Jardim Lateral) é criado automaticamente.

## Referências

- Contratos de API: [contracts/api.md](./contracts/api.md)
- Modelo de dados: [data-model.md](./data-model.md)
- Decisões técnicas: [research.md](./research.md)
