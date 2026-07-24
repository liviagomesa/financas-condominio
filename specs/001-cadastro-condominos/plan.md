# Implementation Plan: Cadastro de Condôminos e Unidades

**Branch**: `001-cadastro-condominos` | **Date**: 2026-07-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-cadastro-condominos/spec.md`

## Summary

Permitir o cadastro, edição, listagem e remoção de Unidades e de Condôminos, com
vínculo obrigatório de cada condômino a uma unidade (1 unidade : N condôminos).
Unidade tem identificador único (comparado de forma normalizada). Condômino tem
nome obrigatório, unidade obrigatória, e-mail e telefone opcionais (telefone
validado em formato brasileiro quando preenchido). Remoção de unidade é bloqueada
enquanto houver condôminos vinculados. Abordagem técnica: API REST em Spring Boot
com camadas api/domain/infra por entidade, persistência em PostgreSQL via Spring
Data JPA, frontend Angular consumindo a API com telas de listagem e formulário por
entidade.

## Technical Context

**Language/Version**: Java 21 (LTS) no backend; TypeScript 6 no frontend

**Primary Dependencies**: Spring Boot 4.1.x (Spring Framework 7) com Spring Data
JPA e Spring Web; Bean Validation (Jakarta Validation) para regras de campo.
Spring Security NÃO é incluído nesta feature (ver research.md — não há requisito de
autenticação/autorização no spec; a constituição já prevê emendar a seção de
Restrições Transversais quando essa necessidade surgir). Frontend: Angular 22,
Bootstrap 5 + SCSS.

**Storage**: PostgreSQL 18.4, executado via container Docker (docker-compose),
acessado pelo backend via Spring Data JPA

**Testing**: Backend — JUnit 5 + Mockito + Spring Boot Test (`@DataJpaTest` para
repository, `@WebMvcTest`/`@SpringBootTest` para controller/service). Frontend —
Vitest (test runner padrão do Angular CLI a partir da v20+) + Angular Testing
utilities para componentes.

**Target Platform**: Aplicação web (API REST + SPA em navegador), execução local
via Docker Compose (Postgres) + servidores de desenvolvimento (backend e `ng
serve`) com Node.js 24 LTS; possível hospedagem futura em PaaS/IaaS (fora do
escopo desta feature)

**Project Type**: web (frontend Angular + backend Spring Boot detectados)

**Performance Goals**: Não há metas específicas de performance — uso pessoal com
poucas dezenas de registros (ver Assumptions do spec)

**Constraints**: Sem autenticação/autorização nesta fase; sem paginação ou busca
avançada nas listagens (poucas dezenas de unidades/condôminos)

**Scale/Scope**: Poucas dezenas de unidades e de condôminos; 2 entidades (Unidade,
Condômino); 6 user stories, 17 requisitos funcionais

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitetura em Camadas**: PASS. Backend organizado por entidade de domínio
  (`unit/`, `resident/`), cada uma com `api/`, `domain/` e `infra/`; recursos
  compartilhados em `shared/`. Frontend organizado por entidade
  (`unit/`, `resident/`) mais `core/` e `shared/`. Ver Project Structure abaixo.
- **II. Separação Controller → Service → Repository**: PASS. Controllers chamam
  apenas Services; Services concentram as regras de negócio (unicidade normalizada
  de unidade, bloqueio de remoção com vínculo, validação de nome/unidade
  obrigatórios) e são os únicos a acessar o Repository.
- **III. Stack Técnica Definida**: PASS. Java + Spring Boot (Spring Data JPA,
  Spring Web) no backend; Angular + TypeScript + Bootstrap + SCSS no frontend;
  PostgreSQL na persistência — versões mais recentes estáveis do mercado (ver
  Technical Context e research.md). Spring Security fica de fora por não haver
  requisito de auth no spec; nenhuma biblioteca é descontinuada por experiência
  prévia negativa nesta feature.
- **IV. Convenções de Código e Formatação**: PASS. Nomes de entidades/campos em
  inglês (`Unit`/`Resident` a nível de código — ver nota de nomenclatura em
  research.md), mensagens de exceção internas em inglês, mensagens de erro
  exibidas à usuária em português. Não há campos de data nesta feature.
- **V. Idioma por Tipo de Conteúdo**: PASS. Este plano, o spec e os demais
  artefatos do Spec Kit estão em português; código (classes, variáveis, tabelas)
  será em inglês; mensagens de erro de API/frontend em português.

Nenhuma violação identificada — Complexity Tracking não se aplica.

## Project Structure

### Documentation (this feature)

```text
specs/001-cadastro-condominos/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── api.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/financas/
│   ├── unit/
│   │   ├── api/               # UnitController, DTOs (request/response)
│   │   ├── domain/             # Unit entity, UnitRepository (port), UnitService
│   │   └── infra/              # UnitJpaRepository (Spring Data), UnitRepositoryImpl
│   ├── resident/
│   │   ├── api/                # ResidentController, DTOs
│   │   ├── domain/              # Resident entity, ResidentRepository (port), ResidentService
│   │   └── infra/               # ResidentJpaRepository (Spring Data), ResidentRepositoryImpl
│   └── shared/
│       ├── GlobalExceptionHandler.java
│       └── exceptions/          # DuplicateUnitException, UnitHasResidentsException, etc.
├── src/main/resources/
│   └── application.yml
├── src/test/java/com/financas/
│   ├── unit/
│   └── resident/
└── pom.xml

frontend/
├── src/app/
│   ├── unit/
│   │   ├── unit-list/
│   │   └── unit-form/
│   ├── resident/
│   │   ├── resident-list/
│   │   └── resident-form/
│   ├── core/                    # error interceptor/handling
│   └── shared/                  # models, services, validators, API base URL config
└── package.json

docker-compose.yml               # container do PostgreSQL
```

**Structure Decision**: Web application (Opção 2 do template). Como este é o
primeiro cadastro do sistema, esta feature também estabelece o esqueleto inicial
de `backend/`, `frontend/` e `docker-compose.yml` na raiz do repositório — ainda
inexistentes. Nomenclatura de código em inglês: "Unidade" → `Unit`, "Condômino" →
`Resident` (ver research.md para justificativa dos nomes escolhidos).

## Complexity Tracking

*Não se aplica — nenhuma violação da constituição identificada no Constitution Check.*
