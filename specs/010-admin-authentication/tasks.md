# Tasks: Autenticação da Síndica

**Input**: Design documents from `/specs/010-admin-authentication/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [quickstart.md](./quickstart.md)

**Tests**: Incluídas — a constitution deste projeto (Princípio III) exige cobertura automatizada para toda regra de negócio (validações, bloqueios, cálculos), então testes não são opcionais aqui, ao contrário do padrão genérico do template. Segue também o padrão real já em uso no projeto: testes de `Service`/lógica pura de domínio (JUnit 5 + Mockito no backend, specs isolados de `service`/`guard`/`interceptor` no frontend); componentes Angular de formulário/tela não têm precedente de teste dedicado neste repositório (nenhum `*-form.spec.ts`/`*-list.spec.ts` existe hoje), então `login.ts` segue o mesmo padrão e não ganha spec próprio.

**Organization**: Tarefas agrupadas por user story (spec.md), em ordem de prioridade (P1 → P2 → P3).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa incompleta)
- **[Story]**: A qual user story a tarefa pertence (US1, US2, US3)
- Caminhos de arquivo exatos em cada descrição

## Path Conventions

Projeto Web já existente: `backend/src/main/java/com/financas/`, `backend/src/test/java/com/financas/`, `frontend/src/app/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar dependências e schema de banco antes de qualquer código novo.

- [ ] T001 Adicionar `spring-boot-starter-security` e `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson` (versão `0.12.6`, `jjwt-impl`/`jjwt-jackson` em `<scope>runtime</scope>`) a `backend/pom.xml`
- [ ] T002 [P] Criar migration `backend/src/main/resources/db/migration/V17__create_admin_user_table.sql` criando a tabela `admin_user` (`id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, `username VARCHAR(255) NOT NULL UNIQUE`, `password_hash VARCHAR(255) NOT NULL`), conforme [data-model.md](./data-model.md)

**Checkpoint**: dependências resolvidas, schema pronto para a entidade `User`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura de segurança que bloqueia TODAS as rotas de `/api/**` por padrão assim que `spring-boot-starter-security` está no classpath — não há como entregar "só o login" (US1) sem que essa infraestrutura completa já exista, porque o Spring Security tranca todo o backend por padrão até ser explicitamente configurado.

**⚠️ CRITICAL**: Nenhuma user story pode ser considerada iniciada antes desta fase estar completa.

- [ ] T003 [P] Criar `UnauthorizedException` em `backend/src/main/java/com/financas/shared/exceptions/UnauthorizedException.java` (mesmo padrão de `BadRequestException`/`ConflictException`/`NotFoundException`) e mapear para 401 em `backend/src/main/java/com/financas/shared/GlobalExceptionHandler.java` (`@ExceptionHandler` retornando `ErrorResponse` com `status = 401`)
- [ ] T004 [P] Criar entidade `User` em `backend/src/main/java/com/financas/auth/domain/User.java` (`@Table(name = "admin_user")`, campos `id`, `username`, `passwordHash`, conforme [data-model.md](./data-model.md))
- [ ] T005 Criar interface `UserRepository` em `backend/src/main/java/com/financas/auth/domain/UserRepository.java` (`findByUsername(String)`, `save(User)`) — depende de T004
- [ ] T006 Criar `UserJpaRepository` (`backend/src/main/java/com/financas/auth/infra/UserJpaRepository.java`) e `UserRepositoryImpl` (`backend/src/main/java/com/financas/auth/infra/UserRepositoryImpl.java`), mesmo padrão de `PartyRepositoryImpl` — depende de T005
- [ ] T007 [P] Criar `JwtService` em `backend/src/main/java/com/financas/auth/infra/JwtService.java` (gera token HS256 com `sub`=username e expiração de 7 dias, lê a chave de `JWT_SECRET` via `@Value`/`Environment` — nunca `System.getenv()` direto, para permitir override em teste; expõe `generate(String username)` e `validate(String token)` retornando o username ou lançando exceção se inválido/expirado; mensagens de erro de validação do token ficam em inglês — Princípio V da constitution, exceção interna nunca exposta diretamente via API) — decisões em [research.md](./research.md)
- [ ] T008 Criar `JwtAuthenticationFilter` em `backend/src/main/java/com/financas/auth/infra/JwtAuthenticationFilter.java` (`OncePerRequestFilter`; lê `Authorization: Bearer`, valida via `JwtService`, popula `SecurityContextHolder` sem consultar `UserRepository` — usuário único, sem papel, per FR-010) — depende de T007
- [ ] T009 [P] Criar `RestAuthenticationEntryPoint` em `backend/src/main/java/com/financas/auth/infra/RestAuthenticationEntryPoint.java` (escreve JSON no formato `ErrorResponse` — `{"message": "Não autenticado.", "status": 401}` — para falhas de token capturadas pelo filtro, fora do alcance do `GlobalExceptionHandler`)
- [ ] T010 Criar `SecurityConfig` em `backend/src/main/java/com/financas/auth/infra/SecurityConfig.java` (`@Bean PasswordEncoder` com `BCryptPasswordEncoder`; `SecurityFilterChain` com sessão stateless (`SessionCreationPolicy.STATELESS`), CSRF desabilitado (`csrf(csrf -> csrf.disable())` — API stateless com Bearer token, sem cookie, não há CSRF token a validar), `cors(Customizer.withDefaults())` reaproveitando `WebConfig` existente, `requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()` (evita que o preflight CORS seja barrado pela autorização antes do `CorsFilter`), `permitAll` em `POST /api/auth/login`, autenticação obrigatória no restante de `/api/**`, `addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`, `exceptionHandling().authenticationEntryPoint(RestAuthenticationEntryPoint)`) — depende de T008, T009
- [ ] T011 Criar `UserProvisioningService` em `backend/src/main/java/com/financas/auth/domain/UserProvisioningService.java` (`implements ApplicationRunner`; lê `ADMIN_USERNAME`/`ADMIN_PASSWORD`/`JWT_SECRET`, lança `IllegalStateException` com mensagem clara em inglês (Princípio V — exceção interna de boot, nunca exposta via API) se alguma estiver ausente/em branco ou se `JWT_SECRET` tiver menos de 32 caracteres — FR-006; faz upsert do `User` via `UserRepository`, com hash BCrypt de `ADMIN_PASSWORD` via `PasswordEncoder` — FR-005) — depende de T006, T010
- [ ] T012 [P] Escrever `JwtServiceTest` em `backend/src/test/java/com/financas/auth/infra/JwtServiceTest.java` (token gerado tem `sub` correto e expira em 7 dias a partir da emissão; `validate` aceita token válido; `validate` rejeita token expirado e token com assinatura inválida; um token gerado por uma instância de `JwtService` é validado por uma segunda instância criada com o mesmo `JWT_SECRET` — simula sobrevivência a reinício, SC-005) — depende de T007
- [ ] T013 [P] Escrever `UserProvisioningServiceTest` em `backend/src/test/java/com/financas/auth/domain/UserProvisioningServiceTest.java` (cria usuário quando `username` não existe; atualiza `passwordHash` quando já existe; lança `IllegalStateException` quando `ADMIN_USERNAME`/`ADMIN_PASSWORD`/`JWT_SECRET` ausentes; lança quando `JWT_SECRET` é mais curto que o mínimo exigido) — depende de T011

**Checkpoint**: toda rota `/api/**` já exige token válido (exceto login, que ainda não existe); a aplicação já falha o boot sem as três variáveis de ambiente. Nenhuma tela/endpoint de negócio existente (parties, accounts etc.) é mais acessível sem token a partir daqui.

---

## Phase 3: User Story 1 - Login com usuário e senha (Priority: P1) 🎯 MVP

**Goal**: A síndica informa usuário/senha corretos e recebe um token JWT válido; credenciais incorretas são recusadas com mensagem genérica.

**Independent Test**: `curl -X POST /api/auth/login` com as credenciais provisionadas via `ADMIN_USERNAME`/`ADMIN_PASSWORD` retorna 200 com um token; com credenciais erradas retorna 401 com mensagem genérica (ver [quickstart.md](./quickstart.md), Cenário 2). A tela de login do frontend envia o formulário e recebe/armazena o token com sucesso.

> **Nota de escopo**: a navegação pós-login no navegador só carrega dados de fato depois que a US2 anexar o token automaticamente às chamadas seguintes (o Spring Security, configurado na Fase 2, já exige token em toda rota) — até lá, o teste independente desta história é validado via `curl` diretamente contra o backend e via inspeção do token armazenado no frontend após o envio do formulário.

### Implementation for User Story 1

- [ ] T014 [P] [US1] Criar `LoginRequest` em `backend/src/main/java/com/financas/auth/api/LoginRequest.java` (record `username`/`password`, `@NotBlank` com mensagens em português — mesmo padrão de `PartyRequest`; textos exatos em [contracts/api.md](./contracts/api.md))
- [ ] T015 [P] [US1] Criar `LoginResponse` em `backend/src/main/java/com/financas/auth/api/LoginResponse.java` (record `token`)
- [ ] T016 [US1] Criar `AuthService` em `backend/src/main/java/com/financas/auth/domain/AuthService.java` (`login(username, password)`: busca `User` por `username`, compara senha via `PasswordEncoder.matches`, emite token via `JwtService.generate` se válido, lança `UnauthorizedException("Usuário ou senha inválidos.")` — mesma mensagem tanto para usuário inexistente quanto para senha incorreta, FR-004) — depende de T006, T007, T010
- [ ] T017 [US1] Criar `AuthController` em `backend/src/main/java/com/financas/auth/api/AuthController.java` (`POST /api/auth/login`, `@Valid @RequestBody LoginRequest`, delega a `AuthService`, retorna `LoginResponse`) — depende de T014, T015, T016
- [ ] T018 [P] [US1] Escrever `AuthServiceTest` em `backend/src/test/java/com/financas/auth/domain/AuthServiceTest.java` (credenciais corretas emitem token; usuário inexistente lança `UnauthorizedException`; senha incorreta lança `UnauthorizedException`; a mensagem é idêntica nos dois casos de falha) — depende de T016
- [ ] T019 [P] [US1] Criar `frontend/src/app/shared/models/auth.model.ts` (interfaces `LoginRequest`, `LoginResponse`, mesmo padrão de `party.model.ts`)
- [ ] T020 [US1] Criar `frontend/src/app/shared/services/auth.service.ts` (`login(username, password): Observable<void>` chamando `POST /api/auth/login` e armazenando o token em `localStorage`; `getToken(): string | null`; `isAuthenticated` como `signal<boolean>` derivado da presença do token) — depende de T019
- [ ] T021 [P] [US1] Escrever `frontend/src/app/shared/services/auth.service.spec.ts` (`login()` armazena o token em `localStorage` em caso de sucesso; `getToken()`/`isAuthenticated()` refletem o estado armazenado) — depende de T020
- [ ] T022 [US1] Criar componente de login em `frontend/src/app/auth/login/login.ts`/`login.html`/`login.scss` (formulário usuário/senha, chama `AuthService.login()`, navega para `/` em sucesso, exibe `ApiError.message` em falha) — depende de T020
- [ ] T023 [US1] Adicionar rota `/login` (sem guarda) em `frontend/src/app/app.routes.ts` — depende de T022

**Checkpoint**: login funcional de ponta a ponta no backend (via curl) e tela de login funcional no frontend, armazenando o token.

---

## Phase 4: User Story 2 - Bloqueio de acesso sem autenticação (Priority: P2)

**Goal**: Ninguém acessa telas ou chamadas de API protegidas sem um token válido — nem diretamente pela URL, nem por chamada HTTP direta.

**Independent Test**: acessar diretamente a URL de qualquer tela do frontend sem login prévio redireciona para `/login`; uma chamada de API sem token (ou com token inválido/expirado) retorna 401 e o frontend redireciona ao login (ver [quickstart.md](./quickstart.md), Cenário 3).

### Tests for User Story 2

- [ ] T024 [US2] Escrever `AuthenticationFilterIT` em `backend/src/test/java/com/financas/auth/AuthenticationFilterIT.java` (`@SpringBootTest` + `@AutoConfigureMockMvc`, configurando `ADMIN_USERNAME`/`ADMIN_PASSWORD`/`JWT_SECRET` via `@DynamicPropertySource`/`@TestPropertySource`: `GET /api/parties` sem token → 401 no formato `{message, status}`; com token obtido via `POST /api/auth/login` → 200; `POST /api/auth/login` acessível sem token; `POST /api/auth/login` com `Authorization: Bearer <token válido>` presente ainda retorna 200 (edge case do spec.md); rota inexistente sob `/api/**` sem token → 401 (não 404); rota inexistente com token válido → 404) — depende de T010 (Foundational) e T017 (login precisa existir para obter um token real)

### Implementation for User Story 2

- [ ] T025 [P] [US2] Criar `frontend/src/app/core/auth.guard.ts` (`CanActivateFn` usando `AuthService.isAuthenticated()`; retorna `UrlTree` para `/login` quando não autenticado) — depende de T020
- [ ] T026 [US2] Criar `frontend/src/app/core/auth.interceptor.ts` (`HttpInterceptorFn`: anexa `Authorization: Bearer <token>` a toda requisição quando há token armazenado — FR-012; no `catchError`, se `status === 401`, limpa o token e redireciona a `/login` via `Router` — FR-013 —, repassando o erro adiante) — depende de T020
- [ ] T027 [US2] Reestruturar `frontend/src/app/app.routes.ts`: agrupar as 11 rotas existentes como `children` de uma rota pai sem componente com `canActivate: [authGuard]`, mantendo `/login` fora da guarda — depende de T023, T025
- [ ] T028 [US2] Registrar `authInterceptor` em `frontend/src/app/app.config.ts`, depois de `errorInterceptor` (`withInterceptors([errorInterceptor, authInterceptor])` — ordem documentada em [research.md](./research.md)) — depende de T026
- [ ] T029 [P] [US2] Escrever `frontend/src/app/core/auth.guard.spec.ts` (redireciona a `/login` sem token; permite ativação com token presente)
- [ ] T030 [P] [US2] Escrever `frontend/src/app/core/auth.interceptor.spec.ts` (anexa o header `Authorization` quando há token; em resposta 401, limpa o token e navega a `/login`; erros não-401 passam adiante inalterados)

**Checkpoint**: aplicação íntegra de ponta a ponta — login, bloqueio de rotas, anexo automático de token e redirecionamento em 401 funcionam juntos sem loop quebrado.

---

## Phase 5: User Story 3 - Sessão contínua entre chamadas à API (Priority: P3)

**Goal**: Encerramento explícito de sessão (logout) sem esperar o vencimento natural do token — a continuidade automática de sessão entre chamadas em si já é consequência de US1+US2 (armazenamento do token + anexo automático).

**Independent Test**: após autenticada, a síndica aciona a ação de logout; o token é descartado imediatamente e ela é redirecionada ao login, mesmo com o token ainda válido (ver [quickstart.md](./quickstart.md), Cenário 4, passos 8–9).

### Implementation for User Story 3

- [ ] T031 [US3] Adicionar `logout()` a `frontend/src/app/shared/services/auth.service.ts` (remove o token de `localStorage`, atualiza o signal `isAuthenticated`) — depende de T020
- [ ] T032 [US3] Adicionar botão de logout em `frontend/src/app/app.html`/`frontend/src/app/app.ts` (visível apenas quando `AuthService.isAuthenticated()`, aciona `logout()` e navega para `/login` — FR-013a) — depende de T031
- [ ] T033 [P] [US3] Estender `frontend/src/app/shared/services/auth.service.spec.ts` cobrindo `logout()` (remove o token e atualiza `isAuthenticated` para `false`) — depende de T031

**Checkpoint**: todas as user stories funcionais de ponta a ponta.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T034 Executar manualmente todos os cenários de [quickstart.md](./quickstart.md) (boot sem env vars, login via curl, bloqueio de rotas, fluxo completo no navegador, sobrevivência a reinício do backend, invalidação em uso) e confirmar os resultados esperados

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — pode começar imediatamente
- **Foundational (Phase 2)**: depende do Setup — BLOQUEIA todas as user stories (Spring Security tranca `/api/**` por padrão assim que a dependência é adicionada)
- **User Story 1 (Phase 3)**: depende do Foundational
- **User Story 2 (Phase 4)**: depende do Foundational **e** de T017/T020/T023 (US1) — precisa de um login funcional para obter token (teste de integração) e de `AuthService`/rota `/login` já existentes para reestruturar rotas
- **User Story 3 (Phase 5)**: depende de T020 (US1, `auth.service.ts` já criado)
- **Polish (Phase 6)**: depende de todas as user stories completas

### Parallel Opportunities

- Setup: T001 e T002 em paralelo
- Foundational: T003, T004, T007, T009 em paralelo entre si; T012/T013 (testes) em paralelo entre si depois de T007/T011
- US1: T014/T015 em paralelo; T018/T019 em paralelo depois de T016/T020 respectivamente
- US2: T029/T030 em paralelo depois de T025–T028
- US3: T033 pode rodar junto de T032 (arquivos diferentes)

---

## Parallel Example: Foundational

```bash
# Após T001/T002 (Setup):
Task: "Criar UnauthorizedException em backend/src/main/java/com/financas/shared/exceptions/UnauthorizedException.java"
Task: "Criar entidade User em backend/src/main/java/com/financas/auth/domain/User.java"
Task: "Criar JwtService em backend/src/main/java/com/financas/auth/infra/JwtService.java"
Task: "Criar RestAuthenticationEntryPoint em backend/src/main/java/com/financas/auth/infra/RestAuthenticationEntryPoint.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Completar Fase 1 (Setup)
2. Completar Fase 2 (Foundational — inclui todo o filtro/config de segurança, já que não há como fatiar isso)
3. Completar Fase 3 (US1)
4. **Validar** via `curl` (quickstart Cenário 2) — login funcional, credenciais erradas recusadas
5. Nota: a aplicação já exige token em toda rota `/api/**` neste ponto (efeito colateral da Fase 2) — telas existentes (parties, accounts etc.) ficam inacessíveis no navegador até a US2 anexar o token automaticamente. Isso é esperado e não é uma regressão a corrigir dentro do escopo de US1.

### Incremental Delivery

1. Setup + Foundational → toda rota `/api/**` protegida, nada mais quebra a partir daqui
2. US1 → login funcional (validável via curl; UI de login funcional isoladamente)
3. US2 → aplicação íntegra e usável de ponta a ponta no navegador (login + navegação + anexo automático + bloqueio)
4. US3 → ação explícita de logout

### Notes

- [P] = arquivos diferentes, sem dependência entre si
- Verificar que os testes falham antes de implementar a funcionalidade correspondente
- Fazer commit após cada tarefa ou grupo lógico
- Parar em cada checkpoint para validar a história independentemente
