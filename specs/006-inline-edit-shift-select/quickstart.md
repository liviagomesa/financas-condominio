# Quickstart: Edição inline de valor e seleção em intervalo com Shift

## Pré-requisitos

- Ambiente das features 001–005 já rodando (ver [specs/005-counterparty-groups/quickstart.md](../005-counterparty-groups/quickstart.md)): Docker Desktop, Java 21 + Maven, Node.js 24 LTS + Angular CLI.
- Nenhuma migration nova, nenhuma mudança de schema — o banco de dados das features anteriores pode ser reaproveitado sem reset.
- Ao menos 6 contas cadastradas em `/accounts` (qualquer tipo/fundo/parte) e ao menos 6 partes cadastradas em `/parties`, para exercitar a seleção em intervalo com múltiplas linhas.

## 1. Subir o banco de dados

```powershell
docker compose up -d
```

## 2. Subir o backend (inalterado por esta feature)

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

Nenhum teste novo é esperado aqui — esta feature reaproveita a validação já existente de `AccountService.update`.

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

Deve incluir o novo `shared/list-selection.spec.ts` (ver research.md).

## Roteiro de validação manual (ponta a ponta)

Os cenários abaixo espelham os Acceptance Scenarios do [spec.md](./spec.md).

1. **Editar o valor de uma conta inline** (US1, FR-001/FR-002/FR-006): acesse `/accounts`, clique no campo "Valor" de uma linha, digite um novo valor e pressione Enter. Confirme que o valor é atualizado na linha e que o "Total líquido" no rodapé é recalculado, sem sair da tela.
2. **Confirmar clicando fora do campo** (FR-002): repita o passo 1, mas confirme clicando em qualquer área fora do campo (blur) em vez de Enter. Confirme o mesmo resultado.
3. **Cancelar com Esc** (FR-003): clique no campo "Valor" de uma conta, digite um valor diferente e pressione Esc. Confirme que o valor exibido volta a ser o original, sem chamada à API (nenhuma mudança após recarregar a página).
4. **Rejeitar valor inválido** (FR-004): clique no campo "Valor", apague tudo (ou digite um valor negativo) e confirme. Confirme que aparece a mensagem "O valor é obrigatório e não pode ser negativo.", o campo continua em edição, e o valor não muda ao recarregar a página.
5. **Uma edição por vez** (FR-005): clique no campo "Valor" de uma linha (sem confirmar), depois clique no campo "Valor" de outra linha. Confirme que a primeira edição foi cancelada (valor original) e a segunda está em modo de edição. Repita clicando em "Registrar pagamento" de uma conta enquanto o campo "Valor" dela está em edição — confirme que a edição de valor é cancelada e o fluxo de pagamento assume.
6. **Selecionar um intervalo com Shift+clique** (US2, FR-008/FR-009): em `/parties`, marque a caixinha da 2ª linha (clique normal), depois segure Shift e clique na caixinha da 6ª linha. Confirme que as linhas 2 a 6 ficam marcadas.
7. **Âncora fixa em Shift+cliques sucessivos** (FR-009a, Clarifications): com o estado do passo 6, segure Shift e clique na caixinha da 4ª linha. Confirme que o intervalo recalculado é 2–4 (a partir da mesma âncora, linha 2), não 6–4.
8. **Shift+clique sem clique normal anterior** (FR-011): recarregue a página (ou aplique um filtro, se a listagem tiver) para limpar a seleção, depois segure Shift e clique diretamente numa caixinha, sem nenhum clique normal antes. Confirme que só aquela linha fica marcada, sem erro.
9. **"Selecionar todas" reinicia a âncora** (FR-012): marque algumas linhas via intervalo, clique em "selecionar todas" no cabeçalho, depois clique numa linha (normal) e, em seguida, Shift+clique em outra. Confirme que o intervalo considera a linha do clique normal feito depois de "selecionar todas" como novo ponto de partida.
10. **Mesmo comportamento nas quatro listagens** (FR-014, SC-003): repita o passo 6 em `/accounts`, `/funds` e `/groups`. Confirme que o comportamento de Shift+clique é idêntico nas quatro telas.

## Referências

- Contratos: [contracts/api.md](./contracts/api.md) (endpoint reaproveitado) e [contracts/frontend-interfaces.md](./contracts/frontend-interfaces.md) (interface `Selection<T>`)
- Modelo de dados (estado de UI): [data-model.md](./data-model.md)
- Decisões técnicas: [research.md](./research.md)
