# Implementation Plan: Autenticação da Síndica

**Branch**: `010-admin-authentication` | **Date**: 2026-08-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/010-admin-authentication/spec.md`

## Summary

Autenticação de usuário único (a síndica), com login tradicional usuário/senha validado pelo backend (Spring Security + BCrypt), emissão de JWT stateless com validade de 7 dias e chave de assinatura fixa via variável de ambiente. O usuário administrador é provisionado automaticamente na inicialização a partir de `ADMIN_USERNAME`/`ADMIN_PASSWORD`, sem tela nem endpoint de cadastro. Toda rota `/api/**` exige o token, exceto o login. O frontend ganha uma tela de login, um guard de rota que bloqueia navegação sem sessão, um interceptor que anexa o token a toda chamada e reage a 401 limpando a sessão, e uma ação explícita de logout.

## Technical Context

**Language/Version**: Java 21 (backend, já adotado) / TypeScript ~6.0.2 com Angular 22 (frontend, já adotado)

**Primary Dependencies**: Spring Boot 4.1.0 + `spring-boot-starter-security` (novo) + `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson` 0.12.6 (novo, geração/validação de JWT) no backend; Angular Router (guard funcional) e `HttpClient`/`withInterceptors` (já em uso) no frontend

**Storage**: PostgreSQL — nova tabela `admin_user` via migration Flyway `V17`

**Testing**: JUnit 5 + Mockito + Spring Boot Test (backend, já adotado) / Vitest (frontend, já adotado)

**Target Platform**: Aplicação web (backend Spring Boot standalone + SPA Angular); destino final é hospedagem em PaaS (ainda não implantada — ver `CLAUDE.md`)

**Project Type**: Web (frontend Angular + backend Spring Boot, estrutura já existente no projeto)

**Performance Goals**: N/A — sem requisito de performance específico além do já implícito no restante do projeto

**Constraints**: Token JWT com validade fixa de 7 dias (FR-003); chave de assinatura fixa via variável de ambiente, sobrevivendo a reinícios (FR-006a); um único usuário, sem papéis/permissões (FR-010); sem tela/endpoint de cadastro (FR-007)

**Scale/Scope**: Um único usuário administrador; 1 endpoint novo (`POST /api/auth/login`); proteção de todas as rotas `/api/**` existentes e futuras; 1 tela nova no frontend (login) + guard global + interceptor + ação de logout

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Princípio I (Arquitetura em Camadas)**: Novo módulo `com.financas.auth` segue a estrutura `api/domain/infra`. A entidade `User` colide com a palavra reservada SQL `USER` (mesma situação já tratada pela constitution para `Group`/`GROUP`) — a classe mantém o nome de domínio (`User`), a tabela física usa nome alternativo (`admin_user`). `shared/exceptions` ganha uma quarta exceção genérica (`UnauthorizedException` → 401), no mesmo espírito de `NotFoundException`/`ConflictException`/`BadRequestException`: é um status HTTP genérico, não uma regra de negócio de uma entidade específica, portanto pertence a `shared/`, não a `auth/domain/`. PASS.
- **Princípio II (Controller → Service → Repository)**: `AuthController` chama `AuthService`, que é o único ponto que acessa `UserRepository`. Nenhuma operação de escrita cross-domain é necessária. PASS.
- **Princípio III (Stack Técnica)**: Reaproveita as versões já adotadas de Java/Spring Boot/Angular/JUnit/Mockito/Vitest. As duas dependências novas (`spring-boot-starter-security`, `jjwt`) são escolhidas pela versão mais recente e estável disponível no momento desta feature (ver `research.md`), tornando-se a partir de agora a versão "adotada" para reuso futuro. PASS.
- **Princípio IV (Convenções de Código)**: Nenhum campo de data novo. Nenhum enum novo. `User` não introduz coleção `@ManyToMany`/`@OneToMany`. PASS.
- **Princípio V (Idioma por Tipo de Conteúdo)**: Mensagens de erro internas em inglês, mensagens ao usuário final (ex.: "Usuário ou senha inválidos.") em português. PASS.
- **Princípio VI (Convenções de API REST)**: `POST /api/auth/login` seguindo o padrão de sub-rota de ação de negócio dedicada já estabelecido (`/{recurso}/{ação}`, aqui sem id porque não há recurso `User` exposto). Resposta de erro segue o formato padronizado `{message, status}` — inclusive para as falhas 401 originadas no filtro de segurança do Spring, fora do fluxo normal do `@RestControllerAdvice` (ver `research.md`, decisão do `AuthenticationEntryPoint`). PASS.

Nenhuma violação identificada. Seção "Complexity Tracking" não se aplica.

## Project Structure

### Documentation (this feature)

```text
specs/010-admin-authentication/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── api.md
└── tasks.md              # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/src/main/java/com/financas/auth/
├── api/
│   ├── AuthController.java        # POST /api/auth/login
│   ├── LoginRequest.java          # username, password (Bean Validation @NotBlank)
│   └── LoginResponse.java         # token
├── domain/
│   ├── User.java                  # entidade (@Table(name = "admin_user"))
│   ├── UserRepository.java        # interface (findByUsername, save)
│   ├── AuthService.java           # valida credenciais, emite token via JwtService
│   └── UserProvisioningService.java  # ApplicationRunner: valida env vars (fail-fast) + upsert do usuário
└── infra/
    ├── UserJpaRepository.java
    ├── UserRepositoryImpl.java
    ├── JwtService.java                    # geração/parsing/validação do token (jjwt)
    ├── SecurityConfig.java                # SecurityFilterChain, PasswordEncoder, permitAll /api/auth/login
    ├── JwtAuthenticationFilter.java        # OncePerRequestFilter, lê Authorization: Bearer
    └── RestAuthenticationEntryPoint.java  # 401 no formato ErrorResponse para falhas do filtro

backend/src/main/java/com/financas/shared/exceptions/
└── UnauthorizedException.java     # nova base genérica → 401 (mapeada em GlobalExceptionHandler)

backend/src/main/resources/db/migration/
└── V17__create_admin_user_table.sql

frontend/src/app/auth/
└── login/
    ├── login.ts
    ├── login.html
    ├── login.scss
    └── login.spec.ts

frontend/src/app/core/
├── auth.guard.ts          # CanActivateFn, redireciona para /login sem sessão
└── auth.interceptor.ts    # anexa Authorization: Bearer; limpa sessão e redireciona em 401

frontend/src/app/shared/
├── models/auth.model.ts       # LoginRequest, LoginResponse
└── services/auth.service.ts   # login(), logout(), getToken(), isAuthenticated()
```

**Structure Decision**: Reaproveita a estrutura Web já existente do projeto (`backend/` Spring Boot + `frontend/` Angular). No backend, `com.financas.auth` segue o padrão `api/domain/infra` do Princípio I, agrupando a entidade `User` e toda a infraestrutura de segurança (filtro JWT, configuração do Spring Security) sob o mesmo módulo — análogo a `recurringcharge`, que também combina entidade e orquestração técnica. No frontend, a tela de login ganha pasta própria (`auth/login/`, mesmo padrão de `party/party-form/` etc.), enquanto guard e interceptor entram em `core/` (já reservado a infraestrutura HTTP/routing transversal, ao lado de `error.interceptor.ts`) e o service/model de autenticação entram em `shared/services`/`shared/models` (mesmo padrão de `party.service.ts`/`party.model.ts`).

## Complexity Tracking

*Não aplicável — nenhuma violação da Constitution Check acima.*
