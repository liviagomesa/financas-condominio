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

## Serialização de `dueDate`/`paymentDate` na API (revisado 2026-07-26, em duas partes)

- **Decision**: `dueDate` e a nova `paymentDate` são `LocalDate` no domínio; nos DTOs de
  request/response, **sem** anotação `@JsonFormat` customizada — trafegam em JSON no formato
  ISO-8601 padrão do Jackson (ex.: `"2026-08-10"`). O formato `DD/MM/AAAA` fica só na UI,
  resolvido pelos recursos **nativos** do Angular/HTML, sem nenhum utilitário de conversão
  dedicado: `<input type="date">` já envia/recebe o valor em ISO-8601 (é o formato nativo
  desse controle HTML, independente de locale), e o `DatePipe` (`| date:'dd/MM/yyyy'`) do
  Angular já formata a exibição a partir de uma string ISO, sem exigir parsing manual.
- **Histórico (parte 1)**: a decisão original desta feature usava
  `@JsonFormat(pattern = "dd/MM/yyyy")` no contrato de API, por leitura estrita do Princípio IV
  então vigente. A usuária questionou essa leitura: o formato brasileiro é uma necessidade de
  **exibição** (UX), não de armazenamento/contrato. Correção feita diretamente no Princípio IV
  (`.specify/memory/constitution.md`), incorporada à emenda 1.2.0 → 1.3.0 ainda não commitada
  (sem novo bump de versão).
- **Histórico (parte 2)**: a primeira correção ainda previa um utilitário de frontend dedicado
  (`date-format.util.ts`) para converter ISO ⇄ DD/MM/AAAA manualmente. A usuária então
  perguntou se isso era mesmo necessário, já que o próprio `<input type="date">` HTML e o
  `DatePipe` do Angular resolvem as duas pontas sem código customizado — confirmado como
  correto; o utilitário foi removido do plano/tasks, e o Princípio IV da constituição foi
  ajustado para não prescrever mais um utilitário específico, só o resultado (conversão isolada
  no frontend).
- **Ressalva aceita**: o formato **exibido** por `<input type="date">` (o "widget" do
  calendário/campo de texto) segue o locale do navegador/SO da usuária, não é travado em
  DD/MM/AAAA por configuração da aplicação — diferente do `DatePipe`, que usa um padrão
  explícito (`dd/MM/yyyy`) e por isso é 100% independente de locale. Como o uso é pessoal e o
  navegador da usuária já está em pt-BR, essa é uma peculiaridade aceita conscientemente
  (mesmo espírito pragmático de outras decisões do projeto, ex.: sem autenticação por ora),
  não um requisito rígido de exibição.
- **Rationale**: ISO-8601 é o padrão de fato para contratos de API REST e o que o Jackson já
  produz sem configuração; usar os recursos nativos do Angular no frontend, em vez de um
  utilitário próprio, elimina código de conversão que não agrega nada além do que a
  plataforma já oferece de graça.
- **Alternatives considered**: manter `dd/MM/yyyy` na API (decisão original, revertida);
  utilitário de conversão de frontend dedicado (decisão intermediária, também revertida por
  ser desnecessária); `dueDate`/`paymentDate` como `String` livre no DTO (perderia a validação
  de formato de data gratuita que `LocalDate` já dá via Jackson).

## Campo de pagamento (`paymentDate`, sem campo `paid` separado)

- **Decision**: um único novo campo em `Receivable`: `paymentDate` (`LocalDate`, nullable).
  Não existe um campo `paid` booleano — "pago" é sempre derivado de `paymentDate != null`.
  `ReceivableRequest`/`ReceivableBulkRequest` ganham `paymentDate` como campo **opcional**
  (sem `@NotNull`), permitindo criar um lançamento já pago diretamente. Além disso, o
  endpoint de ação `POST /api/receivables/{id}/pay` continua existindo, recebendo
  `{ "paymentDate": "..." }`, para marcar/atualizar o pagamento de um lançamento já existente
  sem precisar reenviar o lançamento inteiro.
- **Rationale**: um campo `paid` ao lado de `paymentDate` seria um dado derivado guardado
  redundantemente — nada impede (a nível de código) que `paid = true` e `paymentDate = null`
  coexistam por um bug de sincronização entre os dois campos; eliminar `paid` remove essa
  classe de inconsistência por construção. Permitir `paymentDate` já na criação atende ao
  caso de uso real da usuária de lançar uma cobrança que já sabe estar paga (ex.: lançamento
  retroativo de um mês já quitado), sem forçar sempre um segundo passo. O endpoint dedicado de
  pagamento continua fazendo sentido como atalho de UX para o caso mais comum (marcar como
  pago depois, sem reabrir o formulário completo de edição).
- **Alternatives considered**: manter `paid` boolean redundante (rejeitado pela usuária —
  risco de inconsistência sem benefício real); enum de status (`PENDING`/`PAID`) — mesma razão
  de antes para preferir um valor simples (`recurring` também é boolean); remover o endpoint
  dedicado `/pay` e depender só do `PUT`/`POST` com `paymentDate` — rejeitado porque um atalho
  de UX dedicado para "só marcar como pago" é mais direto do que reenviar o formulário inteiro
  de edição.

## Filtros de listagem (`paid`, `overdue`, `dueYearMonth`, `paymentYearMonth`)

- **Decision**: `GET /api/receivables` ganha quatro novos query params opcionais e
  combináveis (E lógico) com o `unitId` já existente: `paid` (boolean), `overdue` (boolean —
  pendente com `dueDate` anterior a hoje), `dueYearMonth` (`yyyy-MM`) e `paymentYearMonth`
  (`yyyy-MM`, só combina com lançamentos pagos). Implementados como filtro em memória sobre o
  resultado de `repository.findAll()`/`findByUnitId(id)` dentro de `ReceivableService`, sem
  métodos novos no `ReceivableRepository` (porta) nem consultas SQL dedicadas.
- **Rationale**: a escala do projeto (poucas dezenas de lançamentos, ver Assumptions do spec)
  não justifica a complexidade de expressar 4 filtros combináveis como consultas JPA/SQL
  dedicadas (ou Specifications); um `Stream` filtrando em memória é direto, fácil de testar
  unitariamente, e evita inflar a interface do repositório com múltiplas variações de
  `findByXAndY`. `overdue` é calculado com `LocalDate.now()` diretamente no `ReceivableService`
  (sem abstração de `Clock` — não há necessidade de controlar o "agora" em testes de forma
  mais sofisticada do que construir datas relativas a `LocalDate.now()` no próprio teste).
- **Alternatives considered**: métodos de repositório dedicados por combinação de filtro
  (rejeitado — explode combinatoriamente e é otimização prematura para o volume de dados
  real); um único parâmetro de "status" (`PENDING`/`PAID`/`OVERDUE`) em vez de dois
  (`paid`+`overdue`) — rejeitado porque `overdue` e `paid` respondem perguntas diferentes e
  compostas (a usuária pode querer só "vencidos", que já implica pendente, sem precisar
  combinar dois valores de um enum de status).

## Remoção em lote na listagem

- **Decision**: sem endpoint novo no backend — o frontend chama `DELETE
  /api/receivables/{id}` individualmente para cada item selecionado (melhor esforço),
  agregando sucessos e falhas num utilitário compartilhado
  (`frontend/src/app/shared/bulk-delete.ts`) para exibir ao final quais foram removidos e
  quais falharam (e por quê).
- **Rationale**: decisão da usuária (ver AskUserQuestion desta rodada) — mais simples que um
  endpoint transacional "tudo ou nada", e reaproveita a regra de remoção individual já
  validada (incluindo qualquer bloqueio de negócio futuro por vínculo, hoje já existente para
  `Unit`).
- **Alternatives considered**: endpoint `DELETE /api/receivables/bulk` transacional (tudo ou
  nada) — rejeitado pela usuária por maior escopo sem necessidade real hoje.

## Componente de lista compartilhado (frontend)

- **Decision**: em vez de um componente genérico único que tenta renderizar qualquer tabela
  (colunas variam muito entre `unit`, `resident` e `receivable`), a parte comum vira dois
  blocos reutilizáveis: `shared/list-selection.ts` (estado de seleção múltipla, signal-based) e
  `shared/components/bulk-actions-bar/` (barra "N selecionados" + botão remover, usando
  `shared/bulk-delete.ts`). Cada tela de listagem mantém sua própria tabela/colunas, mas
  reaproveita esses dois blocos para a coluna de checkbox e a ação em lote.
- **Rationale**: Angular não tem um jeito idiomático de "table genérica com colunas
  quaisquer" sem recorrer a projeção de conteúdo complexa; concentrar só o que é
  **realmente** comum (estado de seleção + orquestração de remoção em lote + a UI da barra de
  ação) atende ao pedido da usuária de "concentrar configurações em comum" sem forçar uma
  abstração de tabela genérica que lutaria contra formatos de coluna muito diferentes entre as
  3 telas.
- **Impacto cruzado**: aplicado também a `unit-list` e `resident-list` (feature 001, já
  implementada) — ver processo "Edição de Features Já Implementadas" e atualização de
  `specs/001-cadastro-condominos/spec.md`/`plan.md`/`tasks.md` (Phase 12).
- **Alternatives considered**: componente de tabela genérico com `ng-content`/column
  templates — mais "DRY" no papel, mas adiciona complexidade de templating desproporcional ao
  tamanho do projeto (poucas telas, colunas pouco parecidas entre si).

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
