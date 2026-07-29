# Research: Fundos como Entidade e Visualização de Saldo Real

Não há nenhum `NEEDS CLARIFICATION` pendente no Technical Context: stack, versões e ferramentas
de teste são reaproveitadas integralmente das features 001–003 (Constituição, Princípio III).
As decisões abaixo cobrem os pontos de design específicos desta feature.

## Fund como entidade (em vez de enum)

- **Decision**: Nova entidade `Fund` (`com.financas.fund`, pacote próprio com `api/domain/infra`
  — mesmo padrão de `Unit`/`Supplier`), com `name` (obrigatório, único — comparação
  case-insensitive e sem espaços nas extremidades, mesmo mecanismo de `Unit.identifier`) e
  `initialBalance` (`BigDecimal`, obrigatório, sem restrição de sinal).
- **Rationale**: O spec exige que a usuária possa incluir e editar fundos livremente (FR-001,
  FR-003) — incompatível com um enum fixo, que exige alteração de código para cada novo fundo.
  A regra de nome único normalizado espelha exatamente `Unit.identifier`
  (`findByNormalizedIdentifier`/`unit_identifier_normalized_idx`), já validada neste projeto.
- **Alternatives considered**: manter `Fund` como enum e só adicionar um CRUD "paralelo" que
  gera código — descartado, Java não permite enums dinâmicos em tempo de execução; um campo
  `String` livre em `Account` sem entidade própria — descartado por não permitir editar/listar
  fundos como cadastro nem garantir unicidade de nome via FK real (mesmo raciocínio já registrado
  na constituição para não usar um "id solto sem FK de banco de verdade").

## Saldo inicial (abertura) por fundo

- **Decision**: `Fund.initialBalance` (`BigDecimal`, `NUMERIC(10,2)`, `NOT NULL DEFAULT 0` no
  banco), informado no cadastro e editável a qualquer momento (FR-010). Sem restrição de sinal
  no Bean Validation (`@NotNull`, sem `@PositiveOrZero`) — um fundo pode legitimamente começar
  com saldo negativo (ex.: já em déficit antes desta funcionalidade existir).
- **Rationale**: Resolve a clarificação da usuária durante `/speckit-specify` — o saldo real
  precisa refletir o valor físico que já existia antes deste recurso, não apenas o que passar a
  ser lançado no sistema a partir de agora.
- **Alternatives considered**: calcular o saldo real só a partir dos lançamentos, sem saldo
  inicial — rejeitada explicitamente pela usuária, pois o saldo exibido não bateria com o saldo
  físico real quando já existe histórico de caixa anterior ao sistema.

## Cálculo do saldo real

- **Decision**: `FundService.calculateRealBalance(Fund fund)` — método de negócio público,
  busca `accountRepository.findByFundId(fund.getId())` e soma em memória: saldo inicial +
  (recebimentos com `type = RECEIVABLE` e `paymentDate != null`) − (pagamentos com
  `type = PAYABLE` e `paymentDate != null`). Lançamentos sem `paymentDate` (em aberto) nunca
  entram na soma (FR-008/FR-011).
- **Rationale**: Filtragem em memória é o padrão já estabelecido em `AccountService.findAll`
  para o volume de dados deste projeto ("poucas dezenas de registros") — evita introduzir
  consultas agregadas dedicadas (`SUM`/`GROUP BY`) no repositório só para um ganho de
  performance que esta escala não demanda, mantendo a regra de negócio legível e testável via
  Mockito (mock de `AccountRepository.findByFundId` retornando uma lista fixa de `Account`).
- **Alternatives considered**: consulta agregada (`SUM`) via JPQL diretamente no repositório —
  mais eficiente em escala grande, mas desproporcional ao volume real do projeto e mais difícil
  de cobrir com teste de unidade puro (exigiria `@DataJpaTest` em vez de Mockito simples).

## Saldo negativo (sem bloqueio)

- **Decision**: Nenhuma validação nova em `AccountService`/`FundService` impede registrar um
  recebimento ou pagamento que deixe o saldo real de um fundo negativo (FR-012). A visualização
  de saldo é puramente informativa.
- **Rationale**: Resolução explícita da sessão de clarificação (`/speckit-clarify`,
  2026-07-29) — a usuária optou pela opção mais simples, consistente com o pedido original ser
  sobre visibilidade, não sobre um limite rígido de gasto.
- **Alternatives considered**: alertar (mas permitir confirmar) ou bloquear o registro de
  pagamento nesse cenário — ambas rejeitadas pela usuária nesta sessão; ficam registradas aqui
  caso uma feature futura queira reabrir a decisão.

## `FundResponse` com valor computado — extensão pontual do padrão `from(Entity)`

- **Decision**: `FundResponse` (`id`, `name`, `initialBalance`, `realBalance`) expõe
  `from(Fund fund, BigDecimal realBalance)` em vez de `from(Fund fund)` puro. O `Controller`
  (não o `Service` nem a própria DTO) é responsável por chamar
  `fundService.calculateRealBalance(fund)` e passar o resultado à DTO — mantendo a
  responsabilidade de montar a resposta na camada `api/`, e a regra de negócio (a fórmula do
  saldo real) na camada `domain/`, igual a todo o resto do projeto.
  `AccountResponse.fund` embute esse mesmo `FundResponse` completo (mesmo princípio de
  `AccountResponse.unit`/`.supplier`); como isso exige o valor computado, `AccountResponse.from`
  passa a receber um `FundResponse` já pronto como parâmetro adicional
  (`from(Account account, FundResponse fundResponse)`) em vez de construí-lo inline a partir só
  do `Account`, e `AccountController` passa a injetar `FundService` para montar esse parâmetro a
  cada conta retornada.
- **Rationale**: `realBalance` é o único campo de resposta deste projeto que não é derivável
  apenas da entidade — depende de uma agregação sobre outra entidade (`Account`). Manter
  `FundResponse.from` só com a entidade exigiria calcular o saldo real dentro da própria DTO
  (acoplando `api/` a `domain/repository`, quebrando a camada) ou duplicar a DTO em uma versão
  "com saldo" e outra "sem saldo" (complexidade desproporcional para uma única feature que
  sempre quer ver o saldo). Passar o valor já calculado como parâmetro extra é a extensão mínima
  do padrão existente que resolve o problema sem introduzir uma camada nova.
- **Alternatives considered**: duas DTOs (`FundResponse` simples + `FundBalanceResponse` com
  saldo) — rejeitada por duplicar campos e por não haver, nesta feature, nenhum caso de uso real
  que precise do `Fund` **sem** o saldo real (mesmo embutido em `AccountResponse`, o saldo do
  fundo é informação relevante); calcular o saldo dentro de `FundResponse.from` chamando
  diretamente um repositório estático — rejeitada por violar a Separação Controller → Service →
  Repository (Princípio II), que reserva acesso a repositório à camada `Service`.

## Nenhum filtro por fundo em `GET /api/accounts`

- **Decision**: `GET /api/accounts` não ganha um novo query param `fundId` nesta feature.
- **Rationale**: Não solicitado pelo spec; adicionar um filtro não pedido seria escopo além do
  requisitado (o spec pede visualizar saldo por fundo, não filtrar lançamentos por fundo).
- **Alternatives considered**: adicionar por simetria com `unitId`/`supplierId` (like feature
  003 fez com `supplierId`) — descartado desta vez porque, diferente daquele caso, não há
  indício de necessidade real no spec desta feature; pode ser adicionado depois, a pedido, sem
  custo de retrabalho relevante.

## Ordenação da listagem de fundos

- **Decision**: `FundRepository.findAll()` retorna os fundos ordenados por nome
  (`ORDER BY name`, via `findAllByOrderByNameAsc` no `FundJpaRepository`).
- **Rationale**: Atende à Assumption do spec ("ordenar por nome, na ausência de ordenação
  explícita solicitada") de forma determinística, sem depender de o frontend reordenar a lista
  recebida.
- **Alternatives considered**: ordenar no frontend — funcionaria, mas deixaria a ordem
  implícita e dependente de cada tela que consome `GET /api/funds` reimplementar a mesma
  ordenação; ordenar no banco é mais direto e correto por padrão em qualquer consumidor futuro
  da API.

## Saldo total somado (todos os fundos)

- **Decision**: Nenhum endpoint dedicado para o total — o frontend soma `realBalance` de todos
  os itens já retornados por `GET /api/funds` (FR-007/US1, cenário 3).
- **Rationale**: `GET /api/funds` já retorna o saldo real de cada fundo; somar poucas dezenas de
  valores no cliente é trivial e evita introduzir um endpoint novo só para uma redução simples
  sobre dados já disponíveis.
- **Alternatives considered**: campo `totalRealBalance` num envelope de resposta dedicado (ex.:
  `{ "funds": [...], "total": ... }`) — rejeitado por mudar o formato de `GET /api/funds` de uma
  lista simples (`Fund[]`, mesmo padrão de `GET /api/units`/`GET /api/suppliers`) para um objeto
  envelope, inconsistente com o resto da API sem necessidade real.

## Migração de dados (Flyway) — sem preservação de dados existentes

- **Decision**: Duas migrations novas: `V8__create_fund_table.sql` (nova tabela `fund`, com
  índice único de nome normalizado) e `V9__convert_account_fund_to_entity.sql` (`TRUNCATE TABLE
  account`, depois troca a coluna `fund` — hoje `VARCHAR`, valor de enum — pela nova
  `fund_id BIGINT NOT NULL REFERENCES fund (id)`).
- **Rationale**: A usuária foi explícita (correção durante `/speckit-clarify`): o banco atual é
  só de desenvolvimento, sem nenhum dado real a preservar; os três fundos hoje fixos (Piscina,
  Jardim Piscina, Jardim Lateral) não precisam ser pré-cadastrados nem seus vínculos com
  `account` preservados (FR-009, Assumptions do spec). Isso simplifica a migration: sem o
  `DEFAULT` temporário usado na feature 003 para popular uma coluna `NOT NULL` nova em linhas já
  existentes (ali havia dado real a preservar; aqui não há), basta truncar a tabela antes de
  trocar a coluna.
- **Alternatives considered**: manter `fund_id` nullable e migrar registros existentes um a um
  (ex.: mapear o valor de enum salvo para o novo fundo correspondente, criando os três fundos
  fixos automaticamente via `INSERT`) — rejeitada por contrariar diretamente a instrução da
  usuária de que nada precisa ser preservado, adicionando complexidade de migração sem
  necessidade real.

## Frontend: tela de fundos combina cadastro e visualização de saldo

- **Decision**: Uma única tela (`fund-list`) exibe nome, saldo inicial e saldo real de cada
  fundo, mais o total somado, com ações de editar/remover e botão para cadastrar um novo fundo
  — mesmo padrão de listagem+ações já usado por `unit-list`/`supplier-list`/`account-list`
  (incluindo seleção múltipla + remoção em lote via `list-selection.ts`/`bulk-delete.ts`/
  `bulk-actions-bar`, por consistência com essas telas irmãs). `fund-form` (rota separada,
  `/funds/new` e `/funds/:id/edit`) cuida só de criar/editar (nome + saldo inicial).
- **Rationale**: Evita introduzir uma tela de "dashboard" separada da listagem/cadastro para um
  dado (saldo real) que já é, na prática, uma coluna a mais da própria listagem de fundos — as
  três user stories do spec (visualizar saldo, cadastrar, editar/remover) cabem numa única tela
  de listagem + uma de formulário, sem abstração extra.
- **Alternatives considered**: tela de "saldo dos fundos" somente leitura, separada de uma tela
  de "cadastro de fundos" com CRUD — rejeitada por duplicar a mesma lista de fundos em duas
  telas diferentes sem ganho real, contrariando o princípio de simplicidade da constituição.

## Impacto em `Account`/`AccountRequest`/`AccountBulkRequest`

- **Decision**: `Account.fund` passa de `@Enumerated(EnumType.STRING) private Fund fund`
  (`com.financas.account.domain.Fund`) para `@ManyToOne(optional = false) @JoinColumn(name =
  "fund_id", nullable = false) private com.financas.fund.domain.Fund fund`. O enum
  `com.financas.account.domain.Fund` é removido por completo. `AccountRequest`/
  `AccountBulkRequest` trocam o campo `fund` (valor de enum) por `fundId` (`Long`, `@NotNull`),
  mesmo padrão já usado por `unitId`/`supplierId`. `AccountService.create`/`update`/
  `createForAllUnits` passam a receber `Long fundId` e resolvem a entidade via um novo
  `findFundOrThrow(fundId)` (mesmo padrão de `findUnitOrThrow`/`findSupplierOrThrow`), lançando
  `NotFoundException` (404) se o fundo não existir.
- **Rationale**: Mesmo padrão já estabelecido para toda referência obrigatória a outra entidade
  neste projeto (`unitId` em conta a receber, `supplierId` em conta a pagar) — reaproveita a
  convenção em vez de inventar uma nova para `fund`.
- **Alternatives considered**: manter o corpo da requisição aceitando o nome do fundo (`String
  fund`) em vez do `id` — rejeitada por exigir uma consulta por nome normalizado a cada
  requisição em vez de uma busca por chave primária, além de quebrar a simetria com
  `unitId`/`supplierId`.
