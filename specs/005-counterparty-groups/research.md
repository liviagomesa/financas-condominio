# Research: Unificação de Unidade/Fornecedor, Contas sem Restrição de Tipo e Grupos

Não há nenhum `NEEDS CLARIFICATION` pendente no Technical Context: stack, versões e ferramentas de teste são reaproveitadas integralmente das features 001–004 (Constituição, Princípio III). As decisões abaixo cobrem os pontos de design específicos desta feature — a maioria já foi resolvida durante `/speckit-specify`/`/speckit-clarify` (ver spec.md, seções Clarifications e Assumptions); aqui elas são traduzidas em decisões técnicas concretas.

## `Party` como entidade unificada (substitui `Unit` e `Supplier`)

- **Decision**: Nova entidade `Party` (`com.financas.party`, pacote próprio `api/domain/infra` — mesmo padrão de `Unit`/`Supplier`/`Fund`), com `name` (obrigatório, único — comparação case-insensitive e sem espaços nas extremidades, mesmo mecanismo de `Unit.identifier`/ `Fund.name`) e `pixKey` (opcional). `Unit` e `Supplier` são removidos por completo (entidade, repository, service, exceptions, DTOs, controller, testes).
- **Rationale**: FR-003/FR-004 exigem um único cadastro, sem distinção de papel. `name` foi escolhido (em vez de `identifier`, usado hoje só por `Unit`) porque descreve igualmente bem uma unidade ("Bloco A - 101") e uma parte que antes seria fornecedor ("Construtora XYZ"), e é o nome de campo já usado pela maioria das entidades nomeadas do projeto (`Fund.name`, `Supplier.name`) — só `Unit` usava `identifier`. A regra de unicidade normalizada espelha exatamente `Unit.identifier`/`Fund.name`, já validada neste projeto duas vezes.
- **Alternatives considered**: manter `identifier` (nome de campo de `Unit`) — descartado por soltar pior para o caso de uma Parte com papel de fornecedor; manter duas entidades e só remover a restrição de tipo×contraparte no `Account` — descartado, contraria diretamente FR-003 (Unit e Supplier "passam a ter exatamente os mesmos campos", pedido explícito de unificação).

## Remoção do vínculo `Supplier.unit`

- **Decision**: `Party` não tem nenhum campo equivalente a `Supplier.unit`. Nenhuma migração de dado tenta preservar esse vínculo.
- **Rationale**: FR-005; o relacionamento hoje é puramente informativo, sem regra de negócio associada (nenhum `Service`/validação o utiliza) — confirmado por leitura de `SupplierService`/`AccountService` antes desta feature. Removê-lo não afeta nenhum comportamento existente além da própria exibição do campo.
- **Alternatives considered**: nenhuma — não há caso de uso que justifique preservar esse dado.

## `Party.delete` — exception dedicada em vez do `ConflictException` genérico usado por `Supplier`

- **Decision**: `PartyHasAccountsException extends ConflictException` ("Esta parte possui contas vinculadas e não pode ser removida."), em `com.financas.party.domain` — mesmo padrão de `UnitHasAccountsException`/`FundHasAccountsException`.
- **Rationale**: `SupplierService.delete` hoje lança `ConflictException` genérica diretamente, o que já era uma pequena inconsistência com o Princípio I da constituição ("uma exception que representa uma regra de negócio específica de uma entidade NUNCA deve viver em `shared/`"). Como `Supplier` deixa de existir, a unificação é a oportunidade natural de alinhar `Party` ao padrão já usado por `Unit`/`Fund`, sem custo adicional.
- **Alternatives considered**: reaproveitar `ConflictException` genérica (como `Supplier` fazia) — descartado por já não seguir o padrão hoje documentado na constituição.

## `Group` (`Grupo`) — nome de tabela `party_group` (evita a palavra reservada `group` no SQL)

- **Decision**: Nova entidade `Group` (`com.financas.group`, pacote `api/domain/infra`), mapeada para a tabela `party_group` (não `group`), com `name` (obrigatório, único, mesmo mecanismo de normalização) e uma relação `@ManyToMany` para `Party` via tabela de junção `party_group_member` (`group_id`, `party_id`, chave composta, ambos os FKs com `ON DELETE CASCADE`).
- **Rationale**: `GROUP` é palavra reservada no SQL padrão (`GROUP BY`) e especificamente no PostgreSQL — nomear a tabela `group` exigiria escapá-la (aspas duplas) em toda consulta manual e em ferramentas de inspeção do banco, um custo evitável só trocando o nome físico da tabela; a classe Java `Group` não sofre esse problema (não é palavra reservada em Java) e mantém o nome de domínio natural no código. `ON DELETE CASCADE` na tabela de junção implementa diretamente FR-013 (desassociar uma Parte de um Grupo) e a User Story 5, cenário 4 (remover uma Parte de um grupo sem excluí-la do cadastro) sem exigir lógica extra no `Service` — remover uma `Party` ou um `Group` limpa automaticamente as linhas de associação correspondentes.
- **Alternatives considered**: nomear a tabela `group` com aspas — rejeitado por gerar atrito desnecessário em qualquer acesso direto ao banco (migrations futuras, debugging); modelar associação como entidade própria (`GroupMembership`) em vez de `@ManyToMany` simples — rejeitado por complexidade desproporcional, já que a associação não carrega nenhum atributo próprio (só os dois FKs).

## Composição do Grupo é `Set<Party>`, não `List<Party>` (ordenação na resposta, não na entidade)

- **Decision**: `Group.members` é mapeado como `Set<Party>` no JPA (`@ManyToMany @JoinTable`, sem `@OrderColumn`). `GroupResponse.from(Group)` ordena os integrantes por nome antes de serializar.
- **Rationale**: Um `List` com ordem garantida em uma relação `@ManyToMany` exigiria uma coluna de ordenação extra (`@OrderColumn`) na tabela de junção — complexidade sem benefício real, já que a ordem de inserção dos integrantes não é uma informação de negócio relevante (spec não pede "ordem dos integrantes", só a lista completa). Ordenar por nome na camada de resposta garante uma saída determinística para a usuária sem exigir mudança de schema.
- **Alternatives considered**: `List<Party>` com `@OrderColumn` — rejeitado por complexidade desproporcional; não ordenar de forma alguma — rejeitado por tornar a ordem de exibição imprevisível entre requisições.

## Onde a composição do Grupo é editada — reforço da Clarification já resolvida

- **Decision**: `GroupRequest(name, partyIds)` viaja no corpo do próprio `POST`/`PUT` do Grupo. Não existe endpoint dedicado `POST /groups/{id}/members` nem qualquer campo de grupo no `PartyRequest`.
- **Rationale**: Resolução already fechada na sessão de `/speckit-clarify` (2026-07-29) — a composição de um Grupo é editada exclusivamente pela tela do Grupo. Tecnicamente, isso significa que editar a lista de integrantes é uma edição completa do recurso Grupo (`PUT`), não uma ação de negócio dedicada — não há necessidade da sub-rota `POST /{recurso}/{id}/{ação}` do Princípio VI, reservada para ações que não são nem criação nem edição completa.
- **Alternatives considered**: nenhuma — decisão já fechada na sessão de clarificação.

## Restrição tipo×contraparte removida de `AccountService` (`resolveUnit`/`resolveSupplier` → `resolveParty`)

- **Decision**: `AccountService.resolveUnit`/`resolveSupplier` (que hoje impõem `RECEIVABLE → unit obrigatório, supplier proibido` / `PAYABLE → supplier obrigatório, unit proibido`) são substituídos por um único `resolveParty(Long partyId)`, sem nenhuma ramificação por `type`: `partyId` é sempre obrigatório e sempre aceito, independentemente do tipo da conta. A CHECK constraint `account_type_counterparty_check` é removida (não faz mais sentido: há só uma FK, sempre obrigatória).
- **Rationale**: FR-001 — remove exatamente a restrição que a User Story 1 pede para eliminar. Como `Account` passa a ter uma única contraparte possível (`Party`), a necessidade original do padrão "FK nullable dupla + CHECK constraint" (Princípio I da constituição) desaparece — não há mais "exatamente uma dentre duas entidades" para arbitrar, só uma referência obrigatória comum, igual a `Account.fund`.
- **Alternatives considered**: manter duas FKs (`unitId`/`supplierId` renomeados para apontar ambos a `Party`) só para preservar a forma da CHECK constraint — rejeitado por não fazer sentido depois que as duas entidades de origem deixam de existir separadamente; seria reintroduzir a mesma pergunta "qual das duas está preenchida" para uma única entidade real.

## Total líquido da tabela de Contas — calculado no frontend, sem endpoint dedicado

- **Decision**: O total líquido (FR-007, `Σ ENTRADA − Σ SAÍDA` das linhas exibidas) é calculado inteiramente no frontend, via `computed()` sobre o mesmo signal `accounts` já usado para renderizar a tabela — mesmo padrão de `fund-list.ts` (`totalRealBalance`). Nenhum campo novo é adicionado a `AccountResponse` nem a `GET /api/accounts`.
- **Rationale**: Ao contrário do saldo real de um Fundo (que soma **todos** os lançamentos históricos vinculados a ele, independentemente do que está paginado/filtrado na tela), o total desta feature é definido pela spec como "das contas atualmente exibidas (filtradas)" — ou seja, é uma função pura da lista que a tela já tem em mãos após aplicar os filtros, sem precisar de nenhum dado adicional do backend. Calcular no cliente evita um round-trip e mantém a reatividade a filtros/CRUD local (FR-008) tão simples quanto o `computed()` de `fund-list`.
- **Alternatives considered**: expor `total`/`netTotal` como campo agregado num envelope de resposta de `GET /api/accounts` (`{ accounts: [...], total: ... }`) — rejeitado por mudar o formato da listagem de um array simples (`Account[]`, mesmo padrão de `GET /api/units`/ `GET /api/funds`) para um objeto envelope, sem necessidade real (o cálculo já é trivial no cliente com os dados que ele já tem).

## `Account.amount` permanece não-negativo — total líquido não exige mudança de schema

- **Decision**: `Account.amount` continua validado como não-negativo (`InvalidAccountAmountException` em `validateNonNegativeAmount`, inalterado). O total líquido é `Σ amount` das contas `RECEIVABLE` menos `Σ amount` das contas `PAYABLE` — a subtração acontece só no cálculo do total, nunca no valor persistido de uma conta individual.
- **Rationale**: A usuária autorizou explicitamente permitir valores negativos em `amount` **se necessário** para viabilizar o total líquido (spec, Assumptions) — mas não é necessário: o mesmo padrão já usado em `FundService.calculateRealBalance` (soma incondicional de `RECEIVABLE`, subtração de `PAYABLE`, com `amount` sempre positivo) resolve exatamente o mesmo problema sem tocar a regra de validação hoje vigente. Reaproveitar esse padrão evita duplicar a informação de sinal em dois lugares (`type` e o sinal de `amount`), o que criaria risco de inconsistência (uma conta `PAYABLE` com `amount` positivo por engano).
- **Alternatives considered**: permitir `amount` negativo para contas `PAYABLE` e somar tudo direto — rejeitado por duplicar a informação de sinal já carregada por `type`, introduzindo uma invariante nova ("`type = PAYABLE` ⟺ `amount &lt; 0`") que precisaria ser validada em toda gravação, sem ganho sobre a alternativa escolhida.

## `POST /api/accounts/bulk` generalizado — `type` explícito + `groupId` em vez de "todas as unidades"

- **Decision**: `AccountService.createForAllUnits` é substituído por `createForGroup(type, amount, dueDate, description, fundId, recurring, groupId, paymentDate, observations)` — `type` passa a ser um parâmetro explícito (antes hardcoded `RECEIVABLE`) e `groupId` substitui a busca implícita por "todas as unidades cadastradas". Lança `EmptyGroupException` (nova, `com.financas.account.domain`, `ConflictException`) se o grupo não tiver integrantes. `NoUnitsRegisteredException` é removida (não existe mais "todas as unidades" implícito).
- **Rationale**: FR-014/FR-015; a Assumption do spec já registra que o atalho "todas as unidades" é substituído/generalizado pelo mecanismo de Grupo — quem quiser o atalho cria um grupo com as Partes desejadas. Como um Grupo agora pode ser lançado com qualquer `type` (ENTRADA ou SAÍDA, já que seus integrantes não têm mais papel fixo), `type` precisa deixar de ser implícito.
- **Alternatives considered**: manter `createForAllUnits` como está e adicionar `createForGroup` em paralelo — rejeitado pela própria spec (Assumptions: "não é mantido um atalho separado além do mecanismo de Grupos"), o que tornaria os dois métodos redundantes.

## Filtro por `Parte` e por `Fundo` em `GET /api/accounts` — `partyId` único substitui `unitId`/`supplierId`

- **Decision**: `AccountService.findAll` troca `unitId`/`supplierId` (dois parâmetros mutuamente exclusivos) por um único `partyId`. A base da consulta passa a ser: `partyId != null → findByPartyId(partyId)`; senão `findAll()`. Um novo parâmetro `fundId` é aplicado como filtro em memória adicional (`accounts.stream().filter(a -> a.getFund().getId().equals(fundId))`), combinando por E lógico com os demais filtros já existentes (`type`, `paid`, `overdue`, `dueYearMonth`, `paymentYearMonth`) — mesmo padrão de filtragem sequencial em memória já documentado no código (`AccountService`, comentário sobre baixo volume de dados).
- **Rationale**: FR-009/FR-010; a unificação elimina a necessidade dos dois parâmetros mutuamente exclusivos de hoje. `fundId` segue exatamente o mesmo padrão de filtro adicional já usado pelos demais parâmetros de `findAll`, sem introduzir uma consulta agregada nova.
- **Alternatives considered**: usar `fundId` como parâmetro que decide a query base (como `partyId`) em vez de filtro em memória — equivalente em resultado, mas inconsistente com o padrão já estabelecido de "só um campo decide a busca inicial, os demais são filtros em memória"; não adotado para manter a mesma forma de código já existente.

## Migração de dados (Flyway) — sem preservação de dados existentes

- **Decision**: Quatro migrations novas: `V10__create_party_table.sql` (tabela `party`, índice único de nome normalizado, `pix_key` nullable), `V11__create_group_tables.sql` (tabela `party_group` + tabela de junção `party_group_member` com `ON DELETE CASCADE`), `V12__migrate_account_to_party.sql` (remove a CHECK constraint `account_type_counterparty_check`, `TRUNCATE TABLE account`, remove `unit_id`/`supplier_id`, adiciona `party_id BIGINT NOT NULL REFERENCES party (id)`), `V13__drop_unit_and_supplier_tables.sql` (`DROP TABLE supplier`, `DROP TABLE unit`).
- **Rationale**: A usuária confirmou explicitamente (spec, Assumptions, e reforçado na sessão de `/speckit-clarify`) que o ambiente atual é só de desenvolvimento, sem dado real a preservar — mesma decisão e mesmo precedente já registrado na constituição pela feature 004 (`V9`, que truncou `account` ao converter `Fund` de enum para entidade). Como nada precisa ser preservado, a alternativa de renomear `unit` → `party` e mesclar `supplier` via `INSERT ... SELECT` (preferência padrão do Princípio I) tem custo de implementação maior sem nenhum benefício real neste caso — a decisão explícita da usuária é exatamente a exceção que o próprio Princípio I prevê para dispensar essa preferência.
- **Alternatives considered**: `ALTER TABLE unit RENAME TO party` + `INSERT INTO party (name, pix_key) SELECT name, pix_key FROM supplier` (preservando ids de `Unit` como `Party`) — rejeitada por exigir lidar com possíveis nomes duplicados entre `unit.identifier` e `supplier.name`, sem nenhum ganho real já que `account` (a única entidade que referenciava ambas) é truncada de qualquer forma.

## Frontend: `account-form` — toggle "Parte específica"/"Grupo" substitui a ramificação por `type`

- **Decision**: O formulário de lançamento de conta deixa de ramificar a seleção de contraparte por `type` (hoje: `RECEIVABLE` mostra seletor de Unidade + toggle de bulk; `PAYABLE` mostra só seletor de Fornecedor). Passa a ter, para qualquer `type`, um único toggle "Parte específica"/"Grupo" (só em modo criação, `!isEditMode` — mesma condição já usada hoje para o toggle de bulk), com a lista suspensa correspondente (`partyId` ou `groupId`).
- **Rationale**: Resolução da Clarification (`/speckit-clarify`, 2026-07-29, Q1); como `Party` não tem mais papel fixo, a ramificação por `type` na escolha da contraparte deixa de fazer sentido — simplifica o formulário (uma seção em vez de duas ramificações espelhadas).
- **Alternatives considered**: um único campo combinando Partes e Grupos numa lista só — rejeitada na própria sessão de clarificação (Q1, opção A) em favor do toggle de dois modos.

## Nenhum novo filtro de "Grupo" na tela de Contas

- **Decision**: A tela de Contas filtra por `Party` (FR-010), não por `Group`. Um `Account` nunca referencia um `Group` diretamente (FR-016) — só a `Party` que resultou do lançamento em lote — então não há como filtrar contas "por grupo" sem antes resolver quais Partes pertencem a ele, o que não foi solicitado pela spec.
- **Rationale**: Não solicitado pelo spec (FR-009/FR-010 pedem só Fundo e Parte); adicionar um filtro por Grupo seria escopo além do requisitado, replicando o mesmo cuidado já registrado na feature 004 sobre não adicionar filtros não pedidos.
- **Alternatives considered**: nenhuma — fora de escopo.
