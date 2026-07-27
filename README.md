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

Sobe um único container com o PostgreSQL, exposto na porta `5434` do host
(não `5432`, para não conflitar com uma instalação nativa de PostgreSQL que
já possa existir na máquina; nem `5433`, usada inicialmente mas depois
liberada por conflitar com um port-forward do VS Code nesta máquina de
desenvolvimento).

### 2. Backend (API)

```powershell
cd backend
mvn spring-boot:run
```

A API sobe em `http://localhost:8082/api`.

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

Acesse `http://localhost:4202`.

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
  porta `5434`, não `5432` — durante a implementação, um PostgreSQL nativo já
  instalado na máquina de desenvolvimento estava ocupando a porta padrão, e o
  backend acabou se conectando ao serviço nativo (autenticação falhando) em
  vez do container. Só foi percebido rodando a aplicação de verdade contra o
  banco, não apenas compilando/testando com mocks. A porta intermediária
  `5433` (usada até a rodada de correções de 002-receivable-charges) foi
  abandonada depois por outro motivo: um port-forward do VS Code nesta
  máquina passou a interceptar conexões `localhost:5433`, respondendo com um
  erro de autenticação genuíno (mas de outro Postgres) em vez de alcançar o
  container — só percebido rodando a aplicação de verdade de novo, mesma
  lição de sempre validar contra o banco real antes de considerar uma rodada
  concluída.
- **Playwright como devDependency permanente do frontend**: usado para
  validar visualmente cada mudança de UI em navegador headless. Inicialmente
  era instalado sob demanda (`npm install --no-save`) e removido depois
  (`rm -rf node_modules && npm install`) para não sujar o lockfile — mas o
  passo de remoção reinstalava as ~460 dependências do projeto do zero
  (~1 min), enquanto o Playwright em si (binário do Chromium já em cache
  local) instala em segundos. Como toda mudança de frontend acaba exigindo
  essa validação, manter o Playwright como devDependency declarada elimina
  esse custo repetido de reinstalação completa.
- **Datas: ISO-8601 internamente, DD/MM/AAAA só na UI, sem utilitário de
  conversão**: `dueDate` e `paymentDate` trafegam no contrato de API e são
  persistidas no formato ISO-8601 padrão do `LocalDate` (`yyyy-MM-dd`), sem
  `@JsonFormat` customizado. A exibição em DD/MM/AAAA e a leitura da entrada
  da usuária ficam só no frontend, usando recursos nativos do Angular/HTML —
  `<input type="date">` (e `<input type="month">` para os filtros de
  mês/ano) e o `DatePipe` (`| date:'dd/MM/yyyy'`) — sem nenhum utilitário de
  conversão customizado, já que esses controles nativos resolvem as duas
  pontas sozinhos.
- **Registro de pagamento como campo único (`paymentDate`), sem `paid`
  redundante**: um lançamento é "pago" quando `paymentDate` não é nulo; não
  existe um campo `paid` separado, para não correr o risco dos dois ficarem
  inconsistentes entre si. `paymentDate` pode ser informado já na criação
  (individual ou em lote) — lançamento já nasce pago — ou registrado depois
  via `POST /api/receivables/{id}/pay`, uma sub-rota de ação dedicada
  (mesmo espírito da sub-rota `/bulk` já usada para criação em massa).
- **Filtros de listagem em memória**: `GET /api/receivables` aceita filtros
  combináveis (`paid`, `overdue`, `dueYearMonth`, `paymentYearMonth`),
  aplicados em memória sobre a lista já carregada em `ReceivableService`, em
  vez de consultas SQL dedicadas — dado o volume pequeno de registros
  (poucas dezenas), a filtragem é feita em memória, sem necessidade de
  índices ou consultas otimizadas dedicadas.
- **Seleção múltipla e remoção em lote como utilitários compartilhados**:
  `list-selection.ts` (estado de seleção, signal-based) e `bulk-delete.ts`
  (remoção item a item, melhor esforço, sem endpoint transacional novo) +
  o componente `bulk-actions-bar` ficam em `frontend/src/app/shared/`,
  reaproveitados por `receivable-list`, `unit-list` e `resident-list` —
  cada tela mantém sua própria tabela/colunas, só a seleção e a ação em
  lote são compartilhadas.
- **Portas de desenvolvimento fora do padrão (backend `8082`, frontend
  `4202`)**: originalmente `8080`/`4200` (padrões do Spring Boot/Angular
  CLI). Trocadas por decisão explícita da usuária, que roda vários projetos
  Angular/Java na mesma máquina e queria evitar colisão recorrente com as
  portas-padrão de outros projetos.
- **Handler de `MethodArgumentTypeMismatchException` no

## Uso de IA

### Fluxo SDD

Utilizei o GitHub Spec Kit integrado ao Claude Code para conduzir o desenvolvimento com uma abordagem de Spec-Driven Development (SDD). O fluxo foi:

1. **Constituição**: a partir de um arquivo-base temporário contendo orientações para arquitetura e estrutura, stack técnica, regras gerais, idioma, commits e fluxo de trabalho SDD, gerei com o Claude Code (via `/speckit.constitution`) um arquivo de princípios fundamentais do projeto. Esse arquivo idealmente raramente será modificado após a criação.

2. **Contexto para o Claude Code**: informações sobre o produto são importantes para a realização de tarefas de projeto pelo Claude Code, mas não se encaixam no `constitution.md`. Assim, guardei essas informações no `CLAUDE.md`.

3. **Especificação**: a partir das funcionalidades listadas no `CLAUDE.md`, gerei com o Claude Code uma especificação formal de cada funcionalidade. Exemplo: `/speckit.specify Cadastro de condôminos: permitir criar, editar, listar e remover condôminos, com nome, unidade, e-mail e telefone de contato. Cada condômino pertence a uma unidade única do condomínio.` 

4. **Clarificação**: Em seguida, usei `/speckit.clarify` para varrer o spec e perguntar o que ficou faltando (não é preciso antecipar todas as possíveis lacunas no `/speckit.specify`).

5. **Planejamento**: usei `/speckit.plan` e `/speckit.tasks` para definir a estrutura do projeto e quebrar o trabalho em tarefas menores. Em seguida, usei `/speckit.analyze` para cruzar spec/plan/tasks contra a constitution e apontar conflito.

6. **Implementação**: implementei com `/speckit.implement` e rodei `/speckit.converge` para checar o código já implementado contra spec/plan/tasks.

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
- **Formato de data levado longe demais na primeira tentativa**: ao planejar a feature de
  lançamentos de contas a receber, a IA seguiu a constituição ao pé da letra e exigiu
  `dd/MM/yyyy` também no contrato de API (não só na UI), forçando conversão manual
  desnecessária no backend. Questionei essa decisão, e a correção inicial ainda propôs um
  utilitário de conversão dedicado no frontend — perguntei se isso era mesmo necessário, já
  que `<input type="date">` e o `DatePipe` do Angular já resolvem a exibição/entrada sem
  código customizado. Foi confirmado que sim, o utilitário era desnecessário, e a constituição
  foi ajustada de novo antes de qualquer código ser escrito.
- **Correções pedidas antes da implementação da rodada de pagamento**: pagamento já na
  criação do lançamento (sem precisar criar e só depois marcar como pago em uma ação
  separada), remoção do campo `paid` redundante (o status "pago" passou a ser derivado só de
  `paymentDate` não ser nulo) e, como mencionado acima, remoção de um utilitário de conversão
  de data dedicado, em favor de `DatePipe`/`<input type="date">` nativos.

## O que eu faria diferente ou melhoraria com mais tempo

- **Soft delete de condôminos**: em vez de remover o registro definitivamente, marcar o condômino como inativo. Isso evita quebrar dados históricos de cobrança já existentes (o condômino deixaria de aparecer nas listagens de cadastro, mas continuaria sendo referenciado onde já existir vínculo). Para respeitar a LGPD, ao acessar um condômino inativo pelos registros em que ele é referenciado, apenas nome e unidade seriam exibidos — telefone e e-mail seriam removidos/ocultados junto com a inativação.
- **`TargetAccount` como cadastro dinâmico em vez de enum fixo**: hoje "conta destino" tem 3 valores fixos no código (Piscina/Jardim Piscina/Jardim Lateral). Se o condomínio criar um novo centro de custo no futuro (ex.: uma nova área comum), isso exigiria alteração de código e nova migration em vez de um cadastro simples pela própria usuária — um trade-off consciente pela simplicidade agora, que valeria revisitar se a lista mudar com alguma frequência na prática.
- **Geração automática/recorrente de lançamentos mensais**: hoje tanto o lançamento individual quanto o em lote (`POST /api/receivables/bulk`) são ações manuais disparadas pela usuária todo mês. Uma automação (ex.: job agendado gerando a taxa condominial do mês automaticamente para lançamentos marcados como `recurring`) reduziria ainda mais o trabalho manual, mas dependeria de definir regras de idempotência (não duplicar o lançamento do mês se a ação manual também for usada).

