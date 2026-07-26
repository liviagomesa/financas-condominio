# Sistema de Gerenciamento de Condomínio

Sistema de gestão de recebimentos e pagamentos de um condomínio. Composto por API REST em Java, banco de
dados PostgreSQL (container Docker) e frontend em Angular.

## Sumário
- [Sistema de Gerenciamento de Condomínio](#sistema-de-gerenciamento-de-condomínio)
  - [Sumário](#sumário)
  - [Instruções de execução](#instruções-de-execução)
    - [Pré-requisitos](#pré-requisitos)
    - [1. Banco de dados](#1-banco-de-dados)
    - [2. Backend (API)](#2-backend-api)
    - [3. Testes automatizados do backend](#3-testes-automatizados-do-backend)
    - [4. Frontend](#4-frontend)
  - [Decisões técnicas e premissas](#decisões-técnicas-e-premissas)
  - [Uso de IA](#uso-de-ia)
    - [Fluxo SDD](#fluxo-sdd)
    - [Revisões e correções das entregas da IA](#revisões-e-correções-das-entregas-da-ia)
  - [O que eu faria diferente ou melhoraria com mais tempo](#o-que-eu-faria-diferente-ou-melhoraria-com-mais-tempo)

## Instruções de execução

### Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (para o container do banco)
- Java 21 (LTS) + [Maven](https://maven.apache.org/)
- [Node.js LTS](https://nodejs.org/) (>= 22.22 ou >= 24.13.1) + Angular CLI (`npm install -g @angular/cli`)

### 1. Banco de dados

Na raiz do projeto:

```powershell
docker compose up -d
```

Sobe um único container com o PostgreSQL, exposto na porta `5433` do host
(não `5432`, para não conflitar com uma instalação nativa de PostgreSQL que
já possa existir na máquina).

### 2. Backend (API)

```powershell
cd backend
mvn spring-boot:run
```

A API sobe em `http://localhost:8080/api`.

### 3. Testes automatizados do backend

```powershell
cd backend
mvn test
```

### 4. Frontend

```powershell
cd frontend
npm install
npm start
```

Acesse `http://localhost:4200`.

## Decisões técnicas e premissas

- **Stack**: Java 21 (LTS) + Spring Boot 4.1.x (Spring Data JPA, Spring Web) no
  backend, com Maven como build tool e Flyway para migrations; Angular 22 +
  TypeScript 6 + Bootstrap 5 no frontend; PostgreSQL 18.4. Versões escolhidas
  por já estarem instaladas na máquina de desenvolvimento e coincidirem com as
  mais recentes estáveis do mercado no momento (ver `research.md` da feature
  001 para o detalhamento). Novas features devem reutilizar essas mesmas
  versões (regra registrada na `constitution.md`).
- **Sem Spring Security por enquanto**: não há requisito de
  autenticação/autorização até o momento; a dependência só será adicionada
  quando essa necessidade surgir de fato.
- **Estrutura por entidade**: backend organizado em `unit/`, `resident/` e
  `shared/`, cada um com `api/`/`domain/`/`infra/`; `shared/` reservado a
  recursos verdadeiramente transversais (`GlobalExceptionHandler`, exceptions
  genéricas `NotFoundException`/`ConflictException`). Exceptions de regra de
  negócio de uma entidade (ex.: `DuplicateUnitException`) vivem no `domain/`
  dela, não em `shared/`.
- **Convenções de API**: rotas REST no padrão `/api/{recurso-plural-inglês}`;
  erros 4xx sempre no formato `{ "message": string, "status": number }`;
  confirmação de exclusão é responsabilidade do frontend (o backend executa a
  remoção diretamente quando chamado).
- **Porta do PostgreSQL no host**: o `docker-compose.yml` expõe o Postgres na
  porta `5433`, não `5432` — durante a implementação, um PostgreSQL nativo já
  instalado na máquina de desenvolvimento estava ocupando a porta padrão, e o
  backend acabou se conectando ao serviço nativo (autenticação falhando) em
  vez do container. Só foi percebido rodando a aplicação de verdade contra o
  banco, não apenas compilando/testando com mocks.
- **Volume do PostgreSQL 18+**: a partir da versão 18, a imagem oficial do
  Postgres espera o volume montado em `/var/lib/postgresql` (não mais em
  `/var/lib/postgresql/data` como nas versões anteriores) — o container
  simplesmente não sobe com o mount antigo. Também só foi percebido ao rodar
  `docker compose up` de verdade.
- **Teste automatizado de regra de negócio é obrigatório**: a primeira versão
  desta feature foi implementada sem testes (o template padrão do Spec Kit
  trata testes como opcionais salvo pedido explícito). Ao questionar como
  validar o código sem testes, ficou claro que isso deixaria toda feature
  futura exposta ao mesmo problema — daí a `constitution.md` (Princípio III)
  passar a exigir cobertura de teste para toda regra de negócio, e
  `UnitServiceTest`/`ResidentServiceTest`/`BrazilianPhoneValidatorTest` terem
  sido adicionados em retrofit (24 testes, `mvn test`).

## Uso de IA

### Fluxo SDD

Utilizei o GitHub Spec Kit integrado ao Claude Code para conduzir o desenvolvimento com uma abordagem de Spec-Driven Development (SDD). O fluxo foi:

1. **Constituição**: a partir de um arquivo-base temporário contendo orientações para arquitetura e estrutura, stack técnica, regras gerais, idioma, commits e fluxo de trabalho SDD, gerei com o Claude Code (via `/speckit.constitution`) um arquivo de princípios fundamentais do projeto. Esse arquivo idealmente raramente será modificado após a criação.

2. **Contexto para o Claude Code**: informações sobre o produto são importantes para a realização de tarefas de projeto pelo Claude Code, mas não se encaixam no `constitution.md`. Assim, guardei essas informações no `CLAUDE.md`.

3. **Especificação**: a partir das funcionalidades listadas no `CLAUDE.md`, gerei com o Claude Code uma especificação formal de cada funcionalidade. Exemplo: `/speckit.specify Cadastro de condôminos: permitir criar, editar, listar e remover condôminos, com nome, unidade, e-mail e telefone de contato. Cada condômino pertence a uma unidade única do condomínio.` 

4. **Clarificação**: Em seguida, usei `/speckit.clarify` para varrer o spec e perguntar o que ficou faltando (não é preciso antecipar todas as possíveis lacunas no `/speckit.specify`).

5. **Planejamento**: usei `/speckit.plan` e `/speckit.tasks` para definir a estrutura do projeto e quebrar o trabalho em tarefas menores.

6. **Implementação**: implementei com `/speckit.implement`.

7. **Revisão da constituição**: depois de implementada a feature, peço uma revisão da
   `constitution.md` à luz dos artefatos gerados e do código implementado, procurando
   decisões que não são específicas daquela feature e que deveriam virar padrão do projeto
   em vez de ficarem só documentadas ali — sem isso, cada feature roda numa sessão de IA
   isolada que só compartilha a `constitution.md` com as demais (não os planos de features
   já implementadas). O procedimento inteiro (o que procurar, como categorizar, quando
   commitar) está formalizado na própria `constitution.md`, na seção "Revisão da
   Constituição Pós-Implementação" — para acionar, basta pedir algo como "pode rodar a
   revisão da constituição agora", sem precisar repetir um prompt grande a cada vez.

8. **Edições de features já existentes**: para mudanças depois do fluxo completo dos
   comandos do Spec Kit (sem criar feature ou branch nova, sem rodar `/speckit.specify` ou
   `/speckit.plan` de novo), o procedimento está formalizado na `constitution.md`, seção
   "Edição de Features Já Implementadas" — basta descrever a mudança desejada e referenciar
   esse fluxo.

9. **Fluxo de trabalho paralelo:** utilizei o `/remote-control` do Claude Code para acompanhar e aprovar tarefas em execução mesmo longe do computador. Também mantive, em paralelo, uma sessão de chat separada com o Claude para discutir decisões de arquitetura, revisar premissas e planejar próximos passos antes de repassar instruções ao Claude Code — isso ajudou a economizar contexto na sessão de execução e a chegar a cada tarefa com a decisão já pensada, em vez de deixar a ferramenta decidir sozinha.

10. **Novas features**: para novas features (que não existem no enunciado inicial), rodei o fluxo do speckit novamente. Com isso, cada feature tem uma branch dentro de `.specify/`. 

> O problema disso: ao trabalhar numa feature, o Claude não sabe sobre o contexto das demais (somente o `constitution.md` é compartilhado entre features). Com isso, não há restrição que o impeça de editar e atrapalhar código originado de outras features já implementadas.
> A garantia de não conflito vem de duas fontes combinadas: testes automatizados e revisão humana.

### Revisões e correções das entregas da IA

- **Padrões implícitos não viram regra de projeto sozinhos**: ao gerar o `plan.md` e o
  `tasks.md` da primeira feature (cadastro de condôminos e unidades), o Claude tomou várias
  decisões que não eram específicas dessa feature — nome do pacote base do backend,
  ferramenta de build, ferramenta de migração de banco, onde exceptions de regra de negócio
  devem viver dentro da estrutura `api/domain/infra/shared`, formato padrão de erro da API,
  convenção de rotas REST. Nenhuma dessas decisões foi promovida à `constitution.md`
  automaticamente — ficaram só documentadas no plano daquela feature. Como cada feature roda
  numa sessão de IA isolada que só compartilha a `constitution.md` com as demais (não os
  `plan.md`/`research.md` de features já implementadas), isso é um risco real: uma próxima
  feature poderia divergir sem perceber (usar Gradle em vez de Maven, colocar uma exception
  de negócio em `shared/` em vez de `domain/`, inventar outro formato de erro). Só notei o
  risco ao revisar o plano e perguntei diretamente se aquilo não deveria virar padrão de
  projeto; o Claude concordou e propôs uma emenda à constituição (v1.0.0 → v1.1.0)
  formalizando essas decisões antes de qualquer código ser escrito.
  **Lição**: ao revisar um `plan.md`/`tasks.md` gerado por IA, vale perguntar ativamente
  quais decisões ali são genéricas o suficiente para virar regra na constituição, em vez de
  assumir que a IA vai sinalizar isso sozinha.

## O que eu faria diferente ou melhoraria com mais tempo

- **Soft delete de condôminos**: em vez de remover o registro definitivamente, marcar o condômino como inativo. Isso evita quebrar dados históricos de cobrança já existentes (o condômino deixaria de aparecer nas listagens de cadastro, mas continuaria sendo referenciado onde já existir vínculo). Para respeitar a LGPD, ao acessar um condômino inativo pelos registros em que ele é referenciado, apenas nome e unidade seriam exibidos — telefone e e-mail seriam removidos/ocultados junto com a inativação.

