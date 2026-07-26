# Research: Lançamentos de Contas a Receber

Não há nenhum `NEEDS CLARIFICATION` pendente no Technical Context: stack, versões e
ferramentas de teste são reaproveitadas integralmente da feature 001 (Constituição, Princípio
III). As decisões abaixo cobrem pontos de design específicos desta feature.

## Nome da entidade e pacote

- **Decision**: Classe `Receivable`, pacote `com.financas.receivable`, tabela `receivable`.
- **Rationale**: Traduz "Lançamento de Conta a Receber" de forma curta e idiomática
  (termo contábil padrão em inglês para "valor a receber"); mantém a convenção de pacote
  singular já usada em `unit`/`resident`; evita colisão de nome com uma futura feature de
  "contas a pagar" (que teria sua própria entidade, ex. `Payable`).
- **Alternatives considered**: `ReceivableCharge` (mais longo, redundante com o nome do
  diretório da feature, que é só um rótulo de branch/spec, não precisa espelhar o nome de
  classe); `Charge`/`Lançamento` (ambíguo — não deixa claro que é especificamente a receber).

## Campo `targetAccount` (conta destino)

- **Decision**: Enum Java `TargetAccount` com valores `POOL`, `POOL_GARDEN`, `SIDE_GARDEN`,
  persistido como `VARCHAR` via `@Enumerated(EnumType.STRING)` (não `ORDINAL`).
- **Rationale**: Lista fixa e fechada conforme Assumptions do spec (FR-013); nomes de enum em
  inglês por convenção (Princípio IV); `STRING` em vez de `ORDINAL` evita corrupção de dados
  se a ordem de declaração dos valores mudar no futuro. Os rótulos em português ("Piscina",
  "Jardim Piscina", "Jardim Lateral") ficam só no frontend, mapeando o código do enum para o
  texto exibido — mesma separação já usada no projeto (nomes em inglês no domínio, texto em
  português apenas no que é exibido à usuária).
- **Alternatives considered**: `ORDINAL` (mais compacto no banco, mas frágil a reordenação);
  string livre sem enum (perderia a validação de conjunto fechado exigida pelo FR-013).

## Campo `recurring` (tipo)

- **Decision**: Campo booleano `recurring` (getter `isRecurring()`), coluna `recurring
  BOOLEAN NOT NULL`. `true` = recorrente, `false` = extra.
- **Rationale**: Decisão explícita da usuária durante a revisão do spec (ver Clarifications
  em spec.md) — hoje só existem dois valores e a finalidade é exclusivamente filtragem
  futura, sem comportamento funcional distinto associado a cada tipo nesta feature.
- **Alternatives considered**: enum nomeado (`RECURRING`/`EXTRA`) — descartado pela usuária
  por ser mais verboso que necessário para o caso de uso atual, ciente do trade-off de uma
  eventual migração se surgir um terceiro tipo no futuro.

## Serialização de `dueDate` na API

- **Decision**: `dueDate` é `LocalDate` no domínio; nos DTOs de request/response, o campo é
  anotado com `@JsonFormat(pattern = "dd/MM/yyyy")`, aceitando e retornando a data nesse
  formato em JSON (ex.: `"10/08/2026"`).
- **Rationale**: Princípio IV da constituição exige formato `DD/MM/AAAA` para datas
  "exibidas ou registradas em conteúdo de domínio" — esta é a primeira feature do projeto com
  um campo de data, então não havia convenção de serialização já estabelecida a reaproveitar.
- **Nota para a Revisão da Constituição pós-implementação**: esta decisão (formato de
  serialização de datas em JSON) é genérica o suficiente para qualquer feature futura com
  campo de data — deve ser levada à varredura de padronização ao final desta feature, para
  virar convenção registrada na constituição em vez de decisão implícita repetida a cada
  feature.
- **Alternatives considered**: ISO-8601 (`yyyy-MM-dd`), padrão do Jackson por omissão —
  mais comum em APIs REST, mas conflita diretamente com o Princípio IV, que rege conteúdo de
  domínio (não apenas texto exibido) — não apenas a UI, mas a resposta de API já é
  considerada "conteúdo de domínio" registrado.

## Endpoint de lançamento em lote

- **Decision**: Sub-rota dedicada `POST /api/receivables/bulk`, com corpo igual ao de `POST
  /api/receivables` porém sem `unitId` (aplica-se a todas as unidades cadastradas no momento
  da chamada).
- **Rationale**: Uma rota explícita evita a ambiguidade de "unitId omitido = aplicar a
  todas as unidades" no mesmo endpoint de criação individual, o que seria fácil de disparar
  por engano (ex.: bug de formulário enviando corpo sem `unitId`); torna a ação de lote uma
  intenção explícita da chamada, alinhado ao Edge Case do spec sobre o lote não afetar
  unidades cadastradas depois da ação (FR-005).
- **Alternatives considered**: mesmo endpoint com `unitId` opcional (rejeitado pelo risco de
  disparo acidental); `POST /api/units/{id}/receivables` + endpoint de lote separado (rejeitado
  por introduzir uma convenção de rota aninhada não usada em nenhum outro lugar do projeto).

## Listagem por unidade

- **Decision**: `GET /api/receivables?unitId={id}` (query param opcional) no mesmo recurso
  plano `/api/receivables`, em vez de uma rota aninhada `/api/units/{id}/receivables`.
- **Rationale**: Mantém a Convenção de API REST (Princípio VI) de recurso plano no plural em
  inglês, já usada por `/api/residents`; evita introduzir uma segunda convenção de rota
  (aninhada) só para esta feature. `GET /api/receivables` sem o parâmetro retorna todos os
  lançamentos, com a unidade embutida na resposta (`ReceivableResponse.unit`), permitindo à
  usuária localizar lançamentos mesmo fora do contexto de uma unidade específica.
- **Alternatives considered**: rota aninhada por unidade (mais explícita para a US3, mas
  quebra o padrão de recurso plano único já estabelecido pelo projeto).

## Impacto cruzado: bloqueio de remoção de unidade (feature 001)

- **Decision**: `UnitService.delete()` (já implementado na feature 001) passa a também
  verificar `ReceivableRepository.existsByUnitId(id)`, lançando uma nova
  `UnitHasReceivablesException` (em `com.financas.unit.domain`, mesmo pacote de
  `UnitHasResidentsException`) quando houver lançamentos vinculados — mesmo padrão já usado
  para condôminos vinculados.
- **Rationale**: Decisão de negócio já tomada na sessão de clarificação do spec (ver
  Clarifications em spec.md, FR-012); tecnicamente espelha exatamente o mecanismo já existente
  para `ResidentRepository.existsByUnitId`.
- **Processo**: por alterar o comportamento de uma feature já implementada, esta mudança
  segue a seção "Edição de Features Já Implementadas" da constituição — atualização de
  `specs/001-cadastro-condominos/spec.md`/`plan.md`/`tasks.md`, com resumo apresentado à
  usuária e aprovação explícita antes de qualquer edição de arquivo ou código. Este research
  registra a decisão técnica; a execução do processo de edição fica para depois da aprovação
  (ver Completion Report deste plano).
