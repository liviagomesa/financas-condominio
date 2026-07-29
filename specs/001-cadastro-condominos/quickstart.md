# Quickstart: Cadastro de Condôminos e Unidades

## Pré-requisitos

- Docker Desktop (para o container do PostgreSQL)
- Java 21 (LTS) + Maven
- Node.js 24 LTS (>= 24.13.1) + Angular CLI (`npm install -g @angular/cli`)

## 1. Subir o banco de dados

Na raiz do projeto:

```powershell
docker compose up -d
```

O container expõe o PostgreSQL na porta `5434` do host (não `5432`, ocupada por uma instalação nativa de PostgreSQL já presente na máquina; nem `5433`, usada inicialmente e depois liberada por conflitar com um port-forward do VS Code nesta máquina — ver README.md).

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
npm install
npm start
```

Acesse `http://localhost:4202`.

## 5. Rodar os testes automatizados do frontend

```powershell
cd frontend
npm test
```

## Roteiro de validação manual (ponta a ponta)

Os cenários abaixo espelham os Acceptance Scenarios do [spec.md](./spec.md). Execute na ordem para validar o fluxo completo.

1. **Cadastrar unidade** (US1): acesse a tela de unidades, cadastre o identificador "Bloco A - 101". Confirme que ela aparece na listagem (`GET /api/units` — ver [contracts/api.md](./contracts/api.md)).
2. **Bloquear unidade duplicada** (US1): tente cadastrar novamente "bloco a - 101 " (variação de caixa e espaços). Confirme que o sistema rejeita com mensagem de identificador já existente (409).
3. **Cadastrar condômino** (US2): cadastre "Maria Silva" associada à unidade "Bloco A - 101", sem e-mail/telefone. Confirme que aparece na listagem de condôminos com e-mail/telefone vazios.
4. **Segundo condômino na mesma unidade** (US2): cadastre "João Souza" também associado a "Bloco A - 101". Confirme que ambos aparecem, vinculados à mesma unidade.
5. **Bloquear condômino sem unidade cadastrada**: em uma base sem nenhuma unidade, tente cadastrar um condômino e confirme que o sistema orienta a cadastrar a unidade primeiro.
6. **Listar** (US3): acesse as listagens de unidades e de condôminos e confirme que os dados cadastrados aparecem corretamente, incluindo o vínculo condômino → unidade.
7. **Editar telefone com validação** (US4): edite o telefone de "Maria Silva" para um valor fora do formato brasileiro (ex.: "12345") e confirme rejeição; em seguida, edite para "(11) 91111-1111" e confirme sucesso.
8. **Editar identificador duplicado** (US4): tente editar a unidade "Bloco A - 102" (crie-a antes, se necessário) para o identificador "Bloco A - 101" e confirme rejeição por duplicidade.
9. **Remover condômino** (US5): remova "João Souza", confirmando a caixa de diálogo de confirmação, e verifique que ele some da listagem enquanto a unidade "Bloco A - 101" permanece cadastrada.
10. **Bloquear remoção de unidade com vínculo** (US6): tente remover a unidade "Bloco A - 101" (que ainda tem "Maria Silva" associada) e confirme que o sistema rejeita com mensagem informando o vínculo.
11. **Remover unidade sem vínculo** (US6): remova a unidade "Bloco A - 102" (sem condôminos) e confirme que ela some da listagem.
12. **Remoção em lote "melhor esforço"** (FR-018, feature 002): cadastre uma unidade extra sem condômino associado (ex.: "Bloco B - 201"). Na listagem de unidades, selecione essa unidade junto de outra que ainda tenha condômino vinculado (ex.: "Bloco A - 101") e acione "Remover selecionados". Confirme que a unidade sem vínculo é removida normalmente, que a unidade com vínculo permanece na listagem, e que uma mensagem informa que 1 unidade não pôde ser removida. Repita o mesmo roteiro na listagem de condôminos (sem regra de bloqueio equivalente lá, então a remoção em lote deve funcionar para todos os selecionados).

## Referências

- Contratos de API: [contracts/api.md](./contracts/api.md)
- Modelo de dados: [data-model.md](./data-model.md)
- Decisões técnicas: [research.md](./research.md)
