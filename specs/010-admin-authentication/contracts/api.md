# API Contracts: Autenticação da Síndica

Segue as convenções do Princípio VI da constitution: rotas em `/api/{recurso}`, resposta de erro padronizada `{ "message": string, "status": number }` (mensagens em português).

## POST /api/auth/login

Único endpoint de `/api/**` que não exige token (FR-008). Aceita a requisição normalmente mesmo se já houver um token válido no cabeçalho, emitindo um novo (edge case do spec).

**Request body**:

```json
{
  "username": "string (obrigatório, não vazio)",
  "password": "string (obrigatório, não vazio)"
}
```

**Response 200 OK**:

```json
{
  "token": "string (JWT assinado, válido por 7 dias a partir da emissão)"
}
```

**Response 400 Bad Request** — `username` ou `password` ausente/vazio (validado via Bean Validation antes de qualquer consulta ao usuário provisionado, reaproveitando o `MethodArgumentNotValidException` handler já existente em `GlobalExceptionHandler`):

```json
{ "message": "Usuário é obrigatório." | "Senha é obrigatória.", "status": 400 }
```

**Response 401 Unauthorized** — usuário ou senha não confere com o usuário provisionado (mensagem genérica, sem indicar qual campo está incorreto — FR-004):

```json
{ "message": "Usuário ou senha inválidos.", "status": 401 }
```

## Todas as demais rotas `/api/**`

Nenhum contrato de payload muda. O que muda é a exigência transversal de autenticação:

**Header obrigatório em toda requisição**:

```
Authorization: Bearer <token>
```

**Response 401 Unauthorized** — token ausente, mal formado, com assinatura inválida, ou expirado (FR-009), emitida pelo `RestAuthenticationEntryPoint` do `SecurityFilterChain` (fora do fluxo do `GlobalExceptionHandler`, mas no mesmo formato):

```json
{ "message": "Não autenticado.", "status": 401 }
```

Uma rota inexistente sob `/api/**` continua retornando 404 — mas apenas após a checagem de autenticação passar (uma requisição sem token recebe 401 mesmo para uma rota inexistente, sem expor se a rota existe — edge case do spec).

## Logout

Não há endpoint de logout no backend — o token é stateless e não há lista de revogação (Assumption do spec). O logout é inteiramente client-side: o frontend descarta o token de `localStorage` e redireciona ao login.
