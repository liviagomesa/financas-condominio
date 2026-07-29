# Quickstart: Contas a Pagar, Fornecedores e Unificação de Contas

## Pré-requisitos

- Ambiente das features 001/002 já rodando (ver [specs/002-receivable-charges/quickstart.md](../002-receivable-charges/quickstart.md)): Docker Desktop, Java 21 + Maven, Node.js 24 LTS + Angular CLI.
- Migrations desta feature aplicadas (V5–V7, ver data-model.md) — rodam automaticamente no `mvn spring-boot:run` (Flyway).
- Ao menos uma unidade cadastrada (feature 001) para os cenários de conta a receber e de fornecedor vinculado a unidade.

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

Os cenários abaixo espelham os Acceptance Scenarios do [spec.md](./spec.md). Pressupõe a unidade "Bloco A - 101" já cadastrada (feature 001).

1. **Cadastrar fornecedor sem unidade** (US1): cadastre o fornecedor "Empresa de Limpeza XYZ" sem selecionar unidade, opcionalmente com uma chave PIX (ex.: um CNPJ). Confirme que aparece na listagem de fornecedores, sem unidade e com a chave PIX salva.
2. **Cadastrar fornecedor vinculado a uma unidade** (US1): cadastre um fornecedor vinculado à unidade "Bloco A - 101". Confirme que a listagem exibe a unidade vinculada.
3. **Bloquear cadastro de fornecedor sem nome** (US1): tente cadastrar sem nome e confirme a rejeição.
4. **Lançar conta a pagar** (US2): com o fornecedor "Empresa de Limpeza XYZ" cadastrado, lance uma conta a pagar com valor "R$ 400,00", vencimento "05/08/2026", descrição "Limpeza - Agosto/2026", fundo "Jardim Lateral" e observação "Disse que vai pagar mês que vem". Confirme que a conta aparece na listagem unificada, marcada visualmente como "a pagar", com a observação salva.
5. **Bloquear conta a pagar inválida, mas aceitar valor zero** (US2): tente lançar sem valor, vencimento, descrição, fundo ou fornecedor; e, separadamente, com valor negativo. Confirme a rejeição em ambos os casos. Em seguida, lance uma conta com valor "R$ 0,00" (demais campos preenchidos) e confirme que ela é criada normalmente — uso como lembrete antes de saber o valor exato (FR-008).
6. **Bloquear conta a pagar sem fornecedor cadastrado**: em uma base sem nenhum fornecedor, tente lançar uma conta a pagar e confirme que o sistema orienta a cadastrar um fornecedor primeiro.
7. **Ver contas a pagar e a receber na mesma listagem** (US3): com ao menos uma conta a receber (herdada da feature 002 ou lançada agora) e uma conta a pagar (passo 4) já lançadas, acesse `/accounts` e confirme que ambas aparecem, cada uma com um rótulo textual do tipo em cor própria (verde "Entrada" para a conta a receber, vermelho "Saída" para a conta a pagar).
8. **Filtrar por tipo** (US3): aplique o filtro "Saída" e confirme que só a conta a pagar aparece; troque para "Entrada" e confirme o oposto.
9. **Combinar filtro de tipo com outro filtro** (US3): combine o filtro de tipo com "vencidos" ou com mês de vencimento/pagamento e confirme que os filtros se combinam (E lógico).
10. **Registrar pagamento de conta a pagar** (US4): registre o pagamento da conta a pagar do passo 4 informando uma data. Confirme que ela passa a aparecer como paga na listagem, com a data exibida; tente registrar sem informar a data e confirme a rejeição.
11. **Vencidos incluem contas a pagar** (US4): lance uma conta a pagar com vencimento no passado e sem pagamento; filtre por "vencidos" e confirme que ela aparece no resultado.
12. **Editar conta a pagar** (US5): edite o valor, a descrição, as observações ou o fornecedor associado de uma conta a pagar existente e confirme que a listagem reflete os novos valores.
13. **Tipo é imutável** (US5): tente editar uma conta (a pagar ou a receber) tentando trocar seu tipo. Confirme que o sistema rejeita a alteração (via API, `PUT` com `type` diferente do atual retorna 400 — não há campo de troca de tipo na UI).
14. **Remover fornecedor sem vínculo** (US5): remova um fornecedor sem contas a pagar vinculadas e confirme que ele desaparece da listagem.
15. **Bloquear remoção de fornecedor vinculado** (US5): tente remover o fornecedor "Empresa de Limpeza XYZ" (com a conta do passo 4 ainda vinculada) e confirme o bloqueio, com mensagem explicando o vínculo.
16. **Bloquear remoção de unidade vinculada a fornecedor**: tente remover a unidade "Bloco A - 101" enquanto o fornecedor do passo 2 ainda estiver vinculado a ela, mesmo sem nenhuma conta a pagar associada ao fornecedor. Confirme o bloqueio.
17. **Condôminos removidos por completo** (SC-006): confirme que não existe mais nenhum item de menu, rota (`/residents`) ou referência a "condômino"/"resident" acessível na aplicação, e que `GET /api/residents` retorna 404 (rota inexistente).
18. **Remoção em lote na listagem unificada** (FR-020): selecione contas de ambos os tipos na listagem e use "Remover selecionados"; confirme que todas somem (melhor esforço, herdado da feature 002).

> **Impacto cruzado com features 001/002**: os cenários 16 e 17, e qualquer verificação sobre a regra de remoção de unidade (contas + fornecedores, sem mais condôminos), só devem ser validados após a aprovação e implementação da atualização de `UnitService.delete()` descrita em research.md/plan.md desta feature.

## Referências

- Contratos de API: [contracts/api.md](./contracts/api.md)
- Modelo de dados: [data-model.md](./data-model.md)
- Decisões técnicas: [research.md](./research.md)
- Feature absorvida: [specs/002-receivable-charges/](../002-receivable-charges/)
