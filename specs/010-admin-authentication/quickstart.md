# Quickstart: Autenticação da Síndica

Guia de validação manual ponta a ponta desta feature. Contratos completos em [contracts/api.md](./contracts/api.md); modelo de dados em [data-model.md](./data-model.md).

## Pré-requisitos

- PostgreSQL rodando na porta já usada pelo projeto (ver `application.yml`/`docker-compose`).
- Três variáveis de ambiente definidas antes de subir o backend:
  - `ADMIN_USERNAME` — ex.: `sindica`
  - `ADMIN_PASSWORD` — ex.: `uma-senha-qualquer`
  - `JWT_SECRET` — string de pelo menos 32 caracteres (ex.: gerar com `openssl rand -base64 32`)

## Cenário 1 — Boot falha sem as variáveis de ambiente

1. Suba o backend (`mvn spring-boot:run`) **sem** `ADMIN_USERNAME`/`ADMIN_PASSWORD`/`JWT_SECRET` definidos.
2. **Esperado**: a aplicação falha ao iniciar, com uma mensagem de erro clara indicando qual variável está faltando (FR-006) — não deve subir e aceitar tráfego.

## Cenário 2 — Provisionamento e login (User Story 1)

1. Suba o backend com as três variáveis definidas.
2. `curl -i -X POST http://localhost:8082/api/auth/login -H "Content-Type: application/json" -d '{"username":"sindica","password":"uma-senha-qualquer"}'`
3. **Esperado**: `200 OK` com um corpo `{ "token": "..." }` (ver contrato).
4. Repita com senha errada.
5. **Esperado**: `401 Unauthorized` com mensagem genérica, sem indicar se o problema foi usuário ou senha.

## Cenário 3 — Bloqueio de rotas sem autenticação (User Story 2)

1. `curl -i http://localhost:8082/api/parties` (sem header `Authorization`).
2. **Esperado**: `401 Unauthorized`, sem retornar dados de `Party`.
3. `curl -i http://localhost:8082/api/parties -H "Authorization: Bearer <token do Cenário 2>"`.
4. **Esperado**: `200 OK` com a listagem normal.
5. `curl -i http://localhost:8082/api/rota-inexistente -H "Authorization: Bearer <token>"`.
6. **Esperado**: `404`. Repita sem o header.
7. **Esperado**: `401` (não `404`) — a checagem de autenticação precede a resolução de rota.

## Cenário 4 — Frontend: login, navegação e logout (User Stories 1, 2 e 3)

1. Suba o frontend (`npm start`) apontando para o backend do Cenário 2.
2. Acesse `http://localhost:4202/parties` diretamente, sem ter feito login.
3. **Esperado**: redirecionamento automático para `/login`, sem exibir a listagem (FR-011).
4. Faça login com as credenciais corretas.
5. **Esperado**: acesso liberado à tela inicial; navegação entre `/parties`, `/accounts`, `/funds` etc. funciona sem novo prompt de login (FR-012).
6. Recarregue a página (F5).
7. **Esperado**: sessão continua ativa (token em `localStorage` sobrevive ao reload).
8. Clique na ação de logout.
9. **Esperado**: redirecionamento imediato a `/login`; tentar voltar para `/parties` pela URL redireciona de volta ao login (FR-013a).

## Cenário 5 — Sessão sobrevive a reinício do backend

1. Faça login no frontend, mantendo o token válido.
2. Reinicie o backend (mesmo `JWT_SECRET`).
3. Navegue para qualquer tela protegida sem refazer login.
4. **Esperado**: chamadas à API continuam sendo aceitas — o token emitido antes do reinício permanece válido (FR-006a, SC-005).

## Cenário 6 — Expiração/invalidação em uso

1. Com o backend rodando, gere um token e insira manualmente um valor inválido em `localStorage` (ex.: via DevTools, sobrescrever a chave do token com uma string qualquer).
2. Acione qualquer chamada à API pela UI (ex.: navegue para uma listagem).
3. **Esperado**: a UI trata a resposta 401 limpando a sessão e redirecionando a `/login` (FR-013).
