# Sistema de Gerenciamento de Condomínio

Sistema de gestão de recebimentos e pagamentos de um condomínio. Composto por API REST em Java, banco de dados PostgreSQL (container Docker) e frontend em Angular.

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
    - [Transações e lazy loading (`open-in-view: false`)](#transações-e-lazy-loading-open-in-view-false)
    - [Duplicação de código](#duplicação-de-código)
      - [Validação repetida entre DTO e Service](#validação-repetida-entre-dto-e-service)
      - [O possível ciclo de dependência entre Services](#o-possível-ciclo-de-dependência-entre-services)
      - [Busca repetida entre services](#busca-repetida-entre-services)
      - [Conclusão sobre DRY: conhecimento repetido vs. texto repetido](#conclusão-sobre-dry-conhecimento-repetido-vs-texto-repetido)
  - [Revisões e correções das entregas da IA](#revisões-e-correções-das-entregas-da-ia)
  - [Fluxo SDD](#fluxo-sdd)
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

Sobe um único container com o PostgreSQL, exposto na porta `5434` do host.

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

- **Stack**: Java 21 (LTS) + Spring Boot 4.1.x (Spring Data JPA, Spring Web) no backend, com Maven como build tool e Flyway para migrations; Angular 22 + TypeScript 6 + Bootstrap 5 no frontend; PostgreSQL 18.4. Versões escolhidas por já estarem instaladas na máquina de desenvolvimento e coincidirem com as mais recentes estáveis do mercado no momento.
- **Estrutura por entidade**: pastas do backend organizadas em entidades de domínio, cada uma com `api/`/`domain/`/`infra/`; `shared/` reservado a recursos verdadeiramente transversais (`GlobalExceptionHandler`, exceptions genéricas `NotFoundException`/`ConflictException`). Exceptions de regra de negócio de uma entidade (ex.: `DuplicateUnitException`) vivem no `domain/` dela, não em `shared/`.
- **Convenções de API**: rotas REST no padrão `/api/{recurso-plural-inglês}`; erros 4xx sempre no formato `{ "message": string, "status": number }`.
- **Playwright como devDependency permanente do frontend**: usado para validar visualmente cada mudança de UI em navegador headless. Inicialmente era instalado sob demanda (`npm install --no-save`) e removido depois (`rm -rf node_modules && npm install`) para não sujar o lockfile — mas o passo de remoção reinstalava as ~460 dependências do projeto do zero (~1 min). Como toda mudança de frontend acaba exigindo essa validação, manter o Playwright como devDependency declarada elimina esse custo repetido de reinstalação completa.
- **Estado binário sempre derivado de outro campo, nunca duplicado como booleano persistido**: um lançamento é "pago" quando `paymentDate` não é nulo, sem um campo `paid` separado; ao desenhar `RecurringCharge` (feature 009), o Claude chegou a propor um campo `active`, mas troquei por `deactivatedAt` (nullable, com `isActive()` derivado), seguindo a mesma lógica — em ambos os casos, um booleano à parte correria o risco de ficar inconsistente com o campo do qual ele já é derivável.
- **Filtros de listagem em memória**: `GET /api/accounts` aceita filtros combináveis (`paid`, `overdue`, `dueYearMonth`, `paymentYearMonth`), aplicados em memória sobre a lista já carregada em `Accountservice`, em vez de consultas SQL dedicadas — dado o volume pequeno de registros (poucas dezenas), a filtragem é feita em memória, sem necessidade de índices ou consultas otimizadas dedicadas.
- **Seleção múltipla e remoção em lote como utilitários compartilhados**: `list-selection.ts` (estado de seleção, signal-based) e `bulk-delete.ts` (remoção item a item, melhor esforço, sem endpoint transacional novo) + o componente `bulk-actions-bar` ficam em `frontend/src/app/shared/`, reaproveitados pelas telas de listagem do sistema.
- **Portas de desenvolvimento fora do padrão (backend `8082`, frontend `4202`, banco `5434`)**: originalmente `8080`/`4200`/`5432` (padrões do Spring Boot/Angular CLI/PostgreSQL). Troquei por decisão própria: rodo vários projetos Angular/Java, possuo outra instalação PostgreSQL na mesma máquina e queria evitar colisão recorrente com as portas-padrão de outros projetos.
- **Pagamento parcial por duplicação de conta, não "ledger de pagamentos"**: ao implementar pagamento parcial (feature 008), avaliei o modelo alternativo de manter `Account` com múltiplos `Payment` (saldo = valor − soma dos pagamentos) e descartei por enquanto, pois adicionaria complexidade sem necessidade. Optei por dividir a conta original em duas (valor pago + saldo restante, rotuladas "- parte N") por ser aditivo e reaproveitar a arquitetura e as telas já existentes (mesma duplicação da feature 007), proporcional ao porte do projeto.
- **Numeração "- parte N" derivada do texto da própria descrição, sem coluna dedicada**: ao dividir uma conta por pagamento parcial sucessivo, o número da parte é extraído por regex do sufixo já persistido na `description`, em vez de uma coluna `part_number` nova — evita migration e campo persistido para uma informação puramente de exibição; o risco aceito é que editar manualmente essa descrição reinicia a numeração da linhagem, um efeito cosmético sem impacto financeiro.
- **`RecurringCharge` como entidade própria, com geração automática rodando dentro do próprio backend**: uma cobrança/pagamento recorrente é um molde (`RecurringCharge`) — tipo, valor, dia de vencimento, fundo e contraparte — a partir do qual um processo agendado (`@Scheduled`, todo dia 25 às 6h de Brasília) gera uma `Account` por cobrança ativa, com vencimento no mês seguinte. O mesmo método também roda uma vez a cada `ApplicationReadyEvent` (inicialização do backend), como recuperação para ciclos perdidos — relevante porque o plano de hospedagem passou a ser PaaS, onde o processo não fica necessariamente no ar o tempo todo. Idempotência é garantida por uma FK opcional `Account.recurringCharge` mais uma checagem `existsByRecurringChargeIdAndDueDateBetween` no mês-alvo, em vez de comparar outros campos. Editar uma cobrança recorrente nunca altera contas já geradas: a edição cria uma nova linha ativa e inativa a anterior (campo `deactivatedAt`, nullable, com `isActive()` derivado — mesmo padrão de `Account.isPaid()`/`paymentDate` — em vez de um booleano `active` redundante); remover é o mesmo mecanismo (soft delete). A geração isola falhas por cobrança (uma falha não impede as demais no mesmo ciclo) — por isso `RecurringChargeGenerationService.generatePendingAccounts()`/`generateOne()` são deliberadamente **não** `@Transactional`, apesar de fazerem múltiplas escritas, divergindo do critério padrão do projeto (ver Princípio II da constitution); o pior efeito colateral dessa divergência é cosmético — a flag `lastGenerationFailed`, exibida como aviso na tela `/recurring-charges`, pode ficar presa até o próximo ciclo bem-sucedido, sem risco a dado financeiro.

### Transações e lazy loading (`open-in-view: false`)

O projeto roda com `spring.jpa.open-in-view: false` desde a feature 001 — decisão que, sozinha, parece um detalhe de configuração, mas molda várias regras do backend porque significa que **a sessão do Hibernate fecha assim que a transação do `Repository`/`Service` termina**, não fica aberta até o fim da requisição HTTP. Qualquer campo `LAZY` (`@ManyToMany`/`@OneToMany`) acessado depois disso — inclusive no `Controller`, ao montar o DTO de resposta — quebra com `LazyInitializationException`. As regras abaixo (Princípios I e II da constituição, v1.8.0) existem todas por causa disso.

1. **`@Transactional` só em métodos de `Service` com mais de uma escrita, nunca na classe inteira.** O critério é objetivo — conta quantas chamadas de `save()`/`delete()` o método faz para completar uma operação de negócio; se for mais de uma, precisa da anotação (sem ela, cada chamada ao `Repository` é sua própria transação, e uma falha no meio deixaria parte do trabalho já comitada). Cogitei anotar toda classe `Service` por padrão defensivo (nunca depender de perceber a necessidade método a método), mas isso torna toda entidade lida por qualquer método — inclusive os que só leem — gerenciada pelo `EntityManager` durante toda a execução, arriscando persistência silenciosa via dirty-checking do Hibernate se algum campo for mutado sem intenção de salvar. Único método do projeto que precisa disso hoje: `AccountService.createForGroup` (um `save()` por integrante de um lote).
2. **Associação `LAZY` que o `Controller` precisa ler para montar o DTO de resposta MUST vir já resolvida pela própria consulta de leitura do `Repository`** (`findById`/`findAll`) — nunca por uma segunda consulta corretiva depois de um `save()`, e nunca contando com `@Transactional` no `Service`: a transação já encerrou quando o `Controller` recebe o retorno, então não alcança o `Controller` de forma nenhuma. Duas formas de resolver na própria consulta:
   - **`JOIN FETCH` numa query dedicada do Spring Data** (`@Query` com `LEFT JOIN FETCH`) — preferencial, porque só paga o custo do `JOIN` nas consultas que realmente precisam da associação.
   - **`fetch = EAGER` direto na entidade** — só quando literalmente nenhum consumidor da entidade jamais precisaria dela sem aquela associação. É o caso de `Group.members`: a única razão de um `Group` existir é agrupar `Party`s, então carregar um `Group` sem seus integrantes não tem uso real — `EAGER` aqui é **preferível** a `JOIN FETCH` dedicado, não só uma exceção tolerada.
3. **Coleção `@ManyToMany`/`@OneToMany` sem ordem de negócio própria MUST ser `Set`, não `List`** — evita a complexidade de `@OrderColumn`, e evita que um `JOIN FETCH` dessa coleção junto de outra coleção da mesma entidade quebre com `MultipleBagFetchException` (o Hibernate rejeita duas coleções `List` — "bags" sem ordem — carregadas via `JOIN FETCH` na mesma query; com `Set` isso não acontece). Se duas coleções precisarem mesmo ser `List` (ordem de negócio genuína nas duas) e precisarem ser lidas juntas, a saída é aceitar N+1 — tolerável dado o volume pequeno de dados deste projeto — mas só se o acesso ficar inteiramente dentro do `Service`; se o `Controller` precisar do campo, ele MUST vir por `JOIN FETCH`/`EAGER`, nunca por N+1 tocado na camada de apresentação (mesmo argumento do item 2).
4. **Uma operação de negócio que escreve em mais de um `Service` MUST ser orquestrada por um único método `@Transactional`, nunca pelo `Controller` chamando os `Service`s em sequência** — se a segunda chamada falhar depois que o `Controller` já invocou a primeira, a primeira ficaria comitada de forma inconsistente. A propagação padrão do Spring (`Propagation.REQUIRED`) garante que qualquer `Service`/`Repository` chamado de dentro de um método `@Transactional` participa da mesma transação, sem precisar fundir os `Service`s numa classe só.

### Duplicação de código

#### Validação repetida entre DTO e Service

`AccountService.create()`/`update()` validam valor não-negativo e obrigatoriedade de `party`, mesmo essas duas regras já estando cobertas por Bean Validation no `AccountRequest` — e a mesma duplicação se repete em `RecurringChargeService`/`RecurringChargeRequest`. 

A explicação: se um método de serviço de entidade X salva uma entidade Y diretamente, sem passar pelo controller de Y (ex.: `RecurringChargeGenerationService.generateOne()` salvando `Account`), ela pula todas as validações do controller de Y e cria o risco de salvar no banco uma entidade inválida.

Por isso, estabeleci três regras neste projeto:
1. Validações do DTO no controller devem ser duplicadas para a entidade na camada de serviço.
2. Toda operação de **escrita** deve passar pelo service dono da entidade (nunca vazar `Repository` para escrita cross-domain).

As validações no DTO precisam ser mantidas: elas deixam o código mais eficiente e mais seguro (requisição com erro falha antes, sem chegar na camada de domínio).

> Obs. 1: avaliei mover as validações do service para o construtor da entidade onde fosse possível, já que é o único ponto que todo `save` atravessa. Descartei porque isso seria aplicar DDD parcialmente, o que deixaria o projeto inconsistente. Fazer DDD de verdade (setters privados, métodos de domínio nomeados, entidade como guardiã de toda a própria consistência), por sua vez, é uma reestruturação grande, desproporcional ao tamanho e ao objetivo deste projeto.
>
> Obs. 2: **leitura de entidade de outro domínio ainda pode ir direto no `Repository`** (`findById`, `existsBy*`, etc.), pois não constrói nada, não arrisca criar um estado inválido, é só consulta.

#### O possível ciclo de dependência entre Services

Ao aplicar essa regra em geral, surgiu uma objeção importante: "Service depender de Service" é perigoso, porque eventualmente duas entidades vão precisar se escrever mutuamente e isso fecha um ciclo de dependência de bean do Spring (`ServiceA → ServiceB → ServiceA`), que quebra injeção via construtor. 

Isso também deu uma regra geral: se um dia surgir uma dependência de escrita genuinamente bidirecional entre dois `Service`s de CRUD, a saída é introduzir um terceiro `Service` de caso de uso (como a `RecurringChargeGenerationService` já é) que depende dos dois lados, sem que os dois CRUDs dependam um do outro. Isso resolve o ciclo estruturalmente, sem abrir mão de nenhuma garantia de invariante. É, aliás, o mesmo motivo pelo qual DDD organiza `Service`s por caso de uso em vez de um por entidade: reduz drasticamente a chance de duas entidades precisarem depender uma da outra.

#### Busca repetida entre services

`findFundOrThrow`/`findPartyOrThrow`/`findGroupOrThrow` (métodos de leitura) existem duplicados dentro de `AccountService` e de `RecurringChargeService`, em vez de centralizados nos `Service`s donos de cada entidade. 

A única razão pra centralizá-los seria eliminar duplicação de método. Isso representaria um ganho puramente cosmético, pago com um acoplamento entre `Service`s que não precisa existir (e que favoreceria a criação de uma dependência circular se esse padrão se estendesse).

Além de eliminar esse acoplamento, manter os métodos duplicados entre services permite especificar a mensagem de exceção mais adequada para cada caso. Essa duplicação custa pouco e mantém cada `Service` dono do próprio texto de erro.

#### Conclusão sobre DRY: conhecimento repetido vs. texto repetido

Uma tendência que eu tinha era ler DRY como "todo código igual deve virar uma coisa só". Mas duas linhas podem ser idênticas hoje e representar conhecimentos diferentes: coisas que mudam por motivos diferentes, a pedido de contextos diferentes, em momentos diferentes. Sandi Metz, autora do livro *Practical Object-Oriented Design* (POODR), chama isso de duplicação incidental: *"duplication is far cheaper than the wrong abstraction"*.

Ou seja, hoje, entendo que unificar código só é necessário se todos que o utilizam puderem mudar juntos. Se mudar um trecho de código pode criar um bug em um de seus chamadores, duplicá-lo é a opção mais correta.

## Revisões e correções das entregas da IA

- **Embutimento de `FundResponse` sem verificar uso real**: ao revisar os controllers, notei que `RecurringChargeResponse`/`AccountResponse` embutiam o `FundResponse` completo (com `realBalance`, calculado via query própria a cada linha) mesmo esse valor nunca sendo exibido nas listagens que o consomem. A primeira sugestão do Claude foi só deduplicar o cálculo por request (cache); perguntei por que não separar em duas DTOs, o que expôs que a decisão original (feature 004) tinha presumido — sem checar — que todo consumidor precisava do saldo. Fix aplicado: `FundSummaryResponse` para embutimento, `FundResponse` completo só no endpoint do próprio Fundo.

## Fluxo SDD

Utilizei o GitHub Spec Kit integrado ao Claude Code para conduzir o desenvolvimento com uma abordagem de Spec-Driven Development (SDD). O fluxo foi:

1. **Constituição**: a partir de um arquivo-base temporário contendo orientações para arquitetura e estrutura, stack técnica, regras gerais, idioma, commits e fluxo de trabalho SDD, gerei com o Claude Code (via `/speckit.constitution`) um arquivo de princípios fundamentais do projeto. Esse arquivo idealmente raramente será modificado após a criação.

2. **Contexto para o Claude Code**: informações sobre o produto são importantes para a realização de tarefas de projeto pelo Claude Code, mas não se encaixam no `constitution.md`. Assim, guardei essas informações no `CLAUDE.md`.

3. **Especificação**: a partir das funcionalidades listadas no `CLAUDE.md`, gerei com o Claude Code uma especificação formal de cada funcionalidade. Exemplo: `/speckit.specify Cadastro de condôminos: permitir criar, editar, listar e remover condôminos, com nome, unidade, e-mail e telefone de contato. Cada condômino pertence a uma unidade única do condomínio.`

4. **Clarificação**: Em seguida, usei `/speckit.clarify` para varrer o spec e perguntar o que ficou faltando (não é preciso antecipar todas as possíveis lacunas no `/speckit.specify`).

5. **Revisão da spec**: Só neste momento revisei a spec, com foco no primeiro parágrafo de cada User Story e nas seções Edge Cases, Search Criteria e Assumptions.

6. **Planejamento**: usei `/speckit.plan` e `/speckit.tasks` para definir a estrutura do projeto e quebrar o trabalho em tarefas menores. Em seguida, usei `/speckit.analyze` para cruzar spec/plan/tasks contra a constitution e apontar conflito. Só então revisei os arquivos gerados, com foco no `plan.md` (da Constitution Check em diante) e no `api.md` (tudo, inclusive DTOs).

7. **Revisão da constituição**: depois de pronta a documentação, peço uma revisão da `constitution.md` à luz dos artefatos gerados, procurando decisões que não são específicas daquela feature e que deveriam virar padrão do projeto em vez de ficarem só documentadas ali. Isso é necessário porque, ao trabalhar numa feature, o Claude não sabe sobre o contexto das demais (somente o `constitution.md` é compartilhado entre features). Com isso, não há restrição que o impeça de editar e atrapalhar código originado de outras features já implementadas. A garantia de não conflito vem de três fontes combinadas: testes automatizados, revisão humana e constituição.

8. **Implementação**: implementei com `/speckit.implement` e rodei `/speckit.converge` para checar o código já implementado contra spec/plan/tasks. Em seguida, revisei a entrega (especialmente testando a aplicação via frontend) e disparei uma segunda revisão da constituição.

9.  **Edições de features já existentes**: para mudanças depois do fluxo completo dos comandos do Spec Kit (sem criar feature ou branch nova, sem rodar `/speckit.specify` ou `/speckit.plan` de novo), o Claude adota automaticamente o procedimento formalizado na `constitution.md`, seção "Edição de Features Já Implementadas" — basta descrever a mudança desejada e referenciar esse fluxo.

## O que eu faria diferente ou melhoraria com mais tempo

- **Editar um campo em lote, além de remover e duplicar** (ideia registrada durante a revisão do plano da feature 003): a seleção múltipla em contas já permite remover (`bulk-delete.ts`) e duplicar para o mês seguinte (`bulk-duplicate.ts`, feature 007). Falta ainda editar um campo (ex.: valor) de várias contas selecionadas de uma vez, útil por exemplo para reajustar o valor de várias contas recorrentes iguais em massa, sem abrir o formulário de cada uma individualmente.
- **Paginação nas listagens (a começar pela de contas)**: as listagens atuais carregam todos os registros de uma vez, premissa aceitável enquanto o volume é de poucas dezenas (ver Assumptions das features 001/002/003). A listagem de contas tende a crescer indefinidamente (uma leva nova por mês, diferente de unidades/fornecedores, que têm cardinalidade praticamente fixa), então deve ser a primeira a precisar de paginação. Avaliação sobre reaproveitar em todas as listagens: como o projeto já tem o hábito de extrair um utilitário compartilhado assim que mais de uma tela precisa da mesma capacidade (`list-selection.ts`/`bulk-delete.ts`/`bulk-actions-bar`), a recomendação é construir a paginação também como um utilitário reaproveitável (mesmo padrão signal-based), mas aplicar de fato só na listagem de contas por enquanto — unidades e fornecedores não têm o mesmo padrão de crescimento e não devem ganhar paginação só por "manter padrão" sem necessidade real (YAGNI). Construir o utilitário já pensando em reuso custa pouco a mais do que uma solução específica da tela, e evita ter que extrair a abstração correndo depois, quando unidades/fornecedores eventualmente crescerem.
- **Grupos exibidos como tags na página de partes**: hoje a associação de uma parte (`Party`) a um ou mais grupos (`Group`) só é visível/editável pela tela do próprio grupo. Ideia para melhorar a visibilidade: exibir, ao lado de cada parte na listagem, uma tag por grupo ao qual ela pertence, com um "X" na própria tag permitindo remover aquele vínculo diretamente dali (exigiria um endpoint novo de remoção pontual de um `Party` de um `Group`, sem abrir a tela do grupo) — continuaria sendo só remoção, não criação de vínculo novo, preservando a tela do grupo como única fonte de verdade para adicionar membros. Cada grupo teria uma cor atribuída automaticamente pelo sistema (ex.: hash do nome/id do grupo mapeado numa paleta fixa), garantindo que todas as tags de um mesmo grupo (ex.: "Cesan") sejam sempre da mesma cor em qualquer tela.
- **Autenticação de um único usuário administrador**: hoje a API não exige login. Para hospedar num PaaS, a ideia é implementar inicialmente um login tradicional usuário/senha via Spring Security (hash BCrypt), com o usuário provisionado automaticamente na inicialização a partir de variáveis de ambiente (nunca commitado nem numa migration), sem tela de cadastro. O backend emitiria um JWT stateless no login, exigido em todas as rotas de `/api/**`; no frontend, um guard de rota bloquearia telas sem usuário autenticado e um interceptor HTTP anexaria o token a cada chamada, tratando 401 com redirecionamento ao login. Como existe um único usuário com acesso total, não haveria papéis/permissões diferenciadas por endpoint — só autenticado ou não.