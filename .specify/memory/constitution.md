<!--
Sync Impact Report
==================
Versão: 1.17.0 → 1.18.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: novo parágrafo, logo após a orientação sobre associações `LAZY`/`JOIN FETCH` e antes do parágrafo de frontend — todo processo agendado (`@Scheduled`) que gera/processa itens pendentes MUST também reagir a `@EventListener(ApplicationReadyEvent.class)` no mesmo método público, reaproveitando o mesmo cálculo de "o que está pendente" do gatilho agendado, em vez de um método de recuperação separado com lógica duplicada.

Motivação: a feature 009-recurring-charges precisou de um processo de geração mensal (`@Scheduled`, dia 25 às 6h de Brasília) capaz de se recuperar caso a aplicação não estivesse no ar exatamente naquele horário — cenário relevante porque o plano de hospedagem passou a ser um PaaS (potencialmente sujeito a cold start/ociosidade), não mais só ambiente local sempre ativo. A solução adotada — o mesmo método reagindo a `@Scheduled` e a `@EventListener(ApplicationReadyEvent.class)`, calculando o "mês-alvo mais recente" a partir da data atual em vez de checar "o ciclo já rodou?" — cobre tanto um ciclo inteiramente perdido quanto uma falha isolada de item específico, sem duplicar a regra de negócio entre um método "agendado" e um método "de recuperação" separados. Como o cenário de hospedagem em PaaS é permanente a partir de agora (não específico desta feature), formalizado como convenção de arquitetura a ser reaplicada em qualquer processo agendado futuro do projeto, e não deixado apenas como uma decisão registrada no `plan.md` desta feature.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum — o padrão já está aplicado em `RecurringChargeGenerationService.generatePendingAccounts()` (feature 009-recurring-charges).
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.15.0 → 1.16.0

Princípios modificados:
- VI. Convenções de API REST — expandido: novo trecho logo após a explicação de `POST /{recurso}/{id}/{ação}` — quando uma ação de negócio dedicada já existente precisa suportar uma variação que continua sendo a mesma operação de negócio (não uma ação distinta), o corpo da requisição dessa ação MUST ganhar um campo novo opcional, resolvido para um valor-padrão razoável quando ausente, em vez de uma rota dedicada nova.
- I. Arquitetura em Camadas — revisado: o parágrafo de frontend passa a exigir o pacote `bootstrap-icons` (`<i class="bi bi-*" aria-hidden="true">`, com `title`/`aria-label` no elemento pai clicável) para todo ícone usado em botão/link sem texto — revoga a diretriz da feature 005 de evitar biblioteca de ícones externa e colar `path`s SVG copiados à mão no template.

Motivação: a feature 008-partial-payment-split precisou decidir entre criar uma rota nova (`/pay-partial`) e estender `POST /api/accounts/{id}/pay` já existente com um campo opcional (`paidAmount`) para cobrir pagamento integral/parcial/a maior — três magnitudes da mesma ação de negócio ("registrar pagamento"), não três ações distintas. O Princípio VI já cobria quando criar uma sub-rota nova para uma ação de negócio dedicada, mas não orientava a decisão inversa: quando uma ação dedicada já existe e só precisa de uma variação. Formalizado aqui para que a próxima feature com esse mesmo dilema (mesma ação, magnitude diferente) reaproveite o raciocínio em vez de multiplicar endpoints para o que é uma única operação parametrizada. Na mesma rodada, a usuária notou o ícone de confirmar pagamento parcial visualmente deslocado para cima dentro do botão — problema já visto antes (ícones de editar/remover) e corrigido à época só na metade genérica do problema (o alinhamento do elemento `<svg>` dentro do botão, via `vertical-align`, duplicado em dois arquivos `.scss`); a causa deste caso específico era o glifo `check2` do Bootstrap Icons ter a tinta do próprio desenho deslocada dentro do seu `viewBox`, não a caixa do ícone. Uma primeira correção (checar a centralização do glifo antes de adotá-lo + `vertical-align` numa única regra global) foi cogitada e chegou a ser redigida nesta constituição, mas a usuária, ao perguntar por que o projeto usava SVG copiado à mão em vez do próprio pacote `bootstrap-icons` (do qual o projeto já usa o CSS via `bootstrap`), decidiu reverter a decisão original da feature 005 e adotar o pacote — eliminando a causa raiz (seleção manual de `path` sem garantia de centralização, `vertical-align` duplicado por componente) em vez de mitigá-la com um passo de verificação manual a cada ícone novo. `bootstrap-icons` adiciona `frontend/src/styles.scss` (`@import "bootstrap-icons/font/bootstrap-icons.css";`) e um pacote npm novo — trade-off aceito (~280KB de CSS de fonte de ícones, build ainda dentro de margem aceitável) em troca de nunca mais precisar escolher/alinhar um glifo manualmente.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum — a decisão do Princípio VI já está documentada em specs/008-partial-payment-split/research.md ("O campo `paidAmount` é adicionado ao `AccountPaymentRequest` já existente"); a decisão do Princípio I já está aplicada — `bootstrap-icons` instalado, todos os ícones de `row-actions` e `account-list` migrados de `<svg>` inline para `<i class="bi bi-*">`, a regra `.icon`/`vertical-align` removida de `frontend/src/styles.scss` e dos dois `.scss` de componente que a duplicavam.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.14.0 → 1.15.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: novo trecho no parágrafo de migrations — quando uma coluna existente se torna inteiramente sem uso (nenhuma regra de negócio ou consulta depende do seu valor) e a decisão é removê-la, sem envolver renomeação ou generalização de entidade, uma migration MAY remover a coluna diretamente via `ALTER TABLE ... DROP COLUMN`, sem o cuidado de preservação de dado real que motiva a preferência por `RENAME` já registrada acima — desde que a decisão seja explícita e registrada no spec da feature, pelo mesmo motivo já exigido para truncar/recriar.

Motivação: a feature 008-partial-payment-split removeu por completo o campo `Account.recurring`, inerte desde sua criação, para simplificar a lógica de split de pagamento parcial e abrir espaço para uma feature futura de recorrência real. Esse cenário — coluna morta sendo removida, sem nenhuma renomeação/generalização de entidade envolvida — não era coberto pela cláusula de migrations já existente no Princípio I, que trata apenas da escolha entre `RENAME` e truncar/recriar ao generalizar/renomear uma entidade. Formalizado aqui para que a próxima feature que precisar remover um campo morto reaproveite o mesmo raciocínio (DROP direto, decisão explícita no spec) em vez de decidir de novo.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum — a decisão já está documentada em specs/008-partial-payment-split/spec.md (Assumptions/Clarifications) e specs/008-partial-payment-split/research.md.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.12.0 → 1.13.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: novo trecho no parágrafo de frontend — um atalho de teclado global de uma listagem (ex.: Ctrl+C/Ctrl+V) MUST ser implementado via `@HostListener('document:keydown')` no componente da tela, com guarda por `document.activeElement` que ignora o atalho apenas durante edição de texto real (`INPUT` de tipo textual/numérico/data, `TEXTAREA`, `contentEditable`) — nunca `checkbox`/`radio`, que também são `<input>` mas representam seleção, não edição de texto.

Motivação: a feature 007-duplicate-account-next-month introduziu o primeiro atalho de teclado global do projeto (Ctrl+C/Ctrl+V para duplicar contas selecionadas). A guarda de foco inicial (ignorar o atalho quando `document.activeElement` é qualquer `INPUT`) bloqueava o atalho mesmo depois de um clique de seleção, porque o checkbox de seleção da própria linha também é um `<input>` e recebe foco ao ser clicado — bug encontrado durante a validação em navegador (Playwright) desta mesma rodada, antes de qualquer revisão da usuária. Corrigido excluindo `checkbox`/`radio` da guarda. Formalizado aqui para que a próxima feature com necessidade de atalho de teclado global numa listagem (que sempre terá pelo menos um checkbox de seleção na tela) reaproveite a guarda correta em vez de reintroduzir o mesmo bug.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum — a guarda corrigida já está aplicada em `account-list.ts` na mesma feature que motivou esta emenda.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.11.0 → 1.12.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: nova diretriz no parágrafo de frontend — quando uma listagem precisar de uma ação em lote adicional além da remoção, `BulkActionsBar` MUST ser estendido com um input opcional (valor padrão `false`) e um `output()` dedicado por ação nova, em vez de criar um componente de barra dedicado a uma única tela.

Motivação: a feature 007-duplicate-account-next-month introduziu as primeiras ações em lote do projeto além de "remover selecionados" (duplicar para o mês seguinte, com e sem valor zerado), aplicáveis só à listagem de contas — as demais listagens (`party-list`/`fund-list`/`group-list`) não têm campos de vencimento/valor duplicáveis. Reaproveitar `BulkActionsBar` era exigido pelo trio já estabelecido, mas o componente só suportava uma ação fixa (remoção); a solução adotada foi um input booleano opcional controlando a exibição condicional dos botões extras, preservando sem nenhuma alteração o comportamento das três listagens que não usam a extensão. Formalizado aqui para que a próxima feature com necessidade parecida (ex.: a ideia já registrada no README de editar um campo em massa) reaproveite o mesmo padrão em vez de decidir de novo.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum — o padrão já foi aplicado em `BulkActionsBar` na mesma feature que motivou esta emenda.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.9.0 → 1.10.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: nova diretriz no parágrafo de frontend — toda listagem MUST reaproveitar `shared/components/row-actions/` para as ações individuais de editar/remover por linha (ícones SVG inline com `title`/`aria-label`, nunca texto, sem biblioteca de ícones externa), mesmo espírito do trio `list-selection`/`bulk-delete`/`bulk-actions-bar` já exigido para seleção múltipla.

Motivação: durante uma revisão da usuária na feature 005-counterparty-groups, `account-list` (que tinha acabado de ganhar várias colunas novas — Parte, Fundo, total líquido) estava quebrando linha horizontalmente; a correção trocou os botões/links de texto (Editar, Remover, Registrar pagamento, Alterar) por ícones. Isso criou uma inconsistência nova: `account-list` passou a usar ícones enquanto `fund-list`/`party-list`/`group-list` continuavam com texto — a própria usuária identificou o problema e pediu um componente reaproveitável em vez de cada tela decidir seu próprio estilo. `RowActions` foi extraído para `shared/components/` e aplicado às quatro listagens; a correção de alinhamento vertical dos ícones (`vertical-align: -0.125em`, problema comum de SVG inline vs. baseline de texto) também foi identificada pela usuária e corrigida no mesmo componente, beneficiando as quatro telas de uma vez.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum — `shared/components/row-actions/` já criado e aplicado a `account-list`/`fund-list`/`party-list`/`group-list` na mesma rodada.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.7.0 → 1.8.0

Princípios modificados:
- II. Separação Controller → Service → Repository — expandido: um método de `Service` que executa mais de uma operação de escrita (`save`/`delete`, sequencial ou em loop) para completar uma única operação de negócio MUST ser anotado `@Transactional` — critério objetivo e verificável (basta contar quantas chamadas de escrita o método faz), não uma convenção de "anotar sempre que parecer necessário". Deliberadamente NÃO é uma regra de anotar `@Transactional` em toda a classe `Service` por padrão: fazer isso tornaria toda entidade lida por qualquer método (inclusive métodos só de leitura) gerenciada pelo `EntityManager` durante toda a execução, arriscando persistência silenciosa via dirty-checking do Hibernate caso algum campo seja mutado por qualquer motivo que não seja a intenção de salvar — um raio de exposição maior do que o problema original, que afeta só métodos com múltiplas escritas. Nova diretriz: uma operação de negócio que escreve em mais de um `Service` MUST ser orquestrada por um único método `@Transactional` (a propagação padrão do Spring, `Propagation.REQUIRED`, garante que qualquer `Service`/`Repository` chamado de dentro desse método participe da mesma transação), nunca pelo `Controller` chamando os `Service`s em sequência — uma falha na segunda chamada deixaria a primeira já comitada de forma inconsistente.
- I. Arquitetura em Camadas — expandido: nova diretriz para associação `LAZY` que o `Controller` precisa ler ao montar o DTO de resposta (Princípio VI) — MUST vir já resolvida pela consulta de leitura do `Repository` (`findById`/`findAll`), preferencialmente via `JOIN FETCH` numa query dedicada do Spring Data, nunca por uma segunda consulta corretiva depois de um `save()` nem contando com `@Transactional` no `Service` (que não alcança o `Controller`, dado `open-in-view: false`). `fetch = EAGER` direto na entidade continua válido, mas só quando literalmente nenhum consumidor da entidade jamais precisaria dela sem aquela associação (ex.: `Group.members`). Se duas coleções `List` precisarem ser lidas juntas via `JOIN FETCH`, o Hibernate rejeita com `MultipleBagFetchException` — daí a preferência por `Set` (Princípio IV); se ambas precisarem mesmo ser `List` (ordem de negócio genuína), a saída é aceitar N+1, tolerável dado o volume pequeno de dados do projeto — mas só se o acesso ficar inteiramente dentro do `Service`.
- IV. Convenções de Código e Formatação — expandido: a regra de `Set` em vez de `List` passa a cobrir também `@OneToMany` (não só `@ManyToMany`), e ganha uma segunda razão além de evitar `@OrderColumn`: viabilizar `JOIN FETCH` de mais de uma coleção da mesma entidade na mesma consulta, o que quebra com `List` (`MultipleBagFetchException`).

Motivação: durante a implementação da feature 005-counterparty-groups, `AccountService.createForGroup` criava uma conta por integrante de um `Group` num loop de `repository.save()` sem `@Transactional` — cada `save()` é uma transação independente do Spring Data JPA, então uma falha no meio do lote deixaria contas já criadas comitadas (lote parcialmente aplicado). O mesmo padrão sem anotação já existia no método que este substituiu (`createForAllUnits`, de uma feature anterior) — não foi um problema introduzido por esta feature, só carregado adiante sem ser percebido, nem por revisão de código nem pelos testes Mockito (que mockam o `Repository` e não exercitam transação real). A discussão sobre como formalizar essa proteção passou por quatro rodadas com a usuária: (1) `@Transactional` só no método afetado; (2) a usuária trouxe a convenção (comum em outros projetos que já usou) de anotar toda classe `Service` por padrão defensivo, para nunca depender de perceber a necessidade método a método — chegou a ser aplicada nos 4 `Service`s; (3) a própria usuária identificou o efeito colateral do dirty-checking (mutação não intencional de uma entidade gerenciada sendo persistida sem `save()` explícito) como um raio de exposição maior que o problema original, e a decisão voltou a ser cirúrgica, ancorada num critério objetivo (contagem de escritas no método); (4) a usuária propôs uma política completa por escrito cobrindo também estratégia de fetch (`JOIN FETCH` como regra geral, `EAGER` só quando universalmente necessário, `Set` para viabilizar `JOIN FETCH` de múltiplas coleções) e pediu revisão antes de formalizar — três ajustes técnicos surgiram dessa revisão: `MultipleBagFetchException` só ocorre com duas ou mais coleções `List` buscadas juntas (não com uma coleção isolada); a ideia de "refazer a consulta com `JOIN FETCH` depois do `save()`" funciona mas é redundante neste projeto (o `Service` já resolve as entidades relacionadas antes de montar a entidade nova, então o retorno de `save()` já vem sem nada lazy — o ajuste certo é na consulta de leitura original); e um exemplo concreto de orquestração multi-`Service` explicando a propagação `REQUIRED` do Spring. Rodadas 2 e 3 já estavam registradas nesta mesma emenda (ainda não commitada no momento da rodada 4), por isso as mudanças da rodada 4 entram na mesma emenda, sem novo bump de versão.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum — `@Transactional` de classe revertido nos 4 `Service`s, mantido só (nível de método) em `AccountService.createForGroup`, único método com múltiplas escritas hoje; `Group.members` permanece `fetch = EAGER` (caso de exceção legítima, todo consumidor de `Group` precisa de `members`) — nenhuma mudança de código adicional necessária nesta rodada, só formalização das regras já aplicadas.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.6.0 → 1.7.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido em três pontos: (1) nota de que o padrão de FK nullable dupla + `CHECK CONSTRAINT` para contraparte obrigatória "ou-ou" (exemplo original: `Account.unit`/`Account.supplier`) foi superado nesse caso específico pela feature 005, que unificou `Unit` e `Supplier` em `Party` — o padrão em si permanece válido para uma situação futura real e análoga; (2) quando o nome de domínio natural de uma entidade colide com uma palavra reservada do SQL (ex.: `GROUP`), a classe/pacote Java MUST manter o nome de domínio, mas a tabela física MUST usar um nome alternativo (ex.: `Group` → tabela `party_group`); (3) nova diretriz para entidades que representam um agrupamento nomeado de outras (relação muitos-para-muitos, ex.: `Group` de `Party`) — a composição MUST ser editada exclusivamente pela tela do próprio agrupamento, nunca pela tela de cada entidade membro.
- IV. Convenções de Código e Formatação — expandido: uma coleção `@ManyToMany` sem ordem de negócio própria MUST ser modelada como `Set` (não `List`), com qualquer ordenação determinística da resposta de API aplicada na camada de DTO, nunca persistida via `@OrderColumn`.
- VI. Convenções de API REST — expandido: critério explícito para decidir entre calcular um valor agregado no backend (exposto via API, quando depende de dados além da lista já carregada pela tela — ex.: saldo real de um Fundo) ou inteiramente no frontend (quando é uma função pura da lista já carregada/filtrada pela tela — ex.: um total ao final de uma tabela), sem endpoint dedicado nesse segundo caso.

Motivação: decisões que emergiram do planejamento da feature 005-counterparty-groups (unificação de `Unit`/`Supplier` em `Party`, introdução do conceito de `Group` — a primeira relação muitos-para-muitos do projeto — e um total dinâmico calculado no frontend, diferente do padrão de saldo real de Fundo calculado no backend) e que se aplicam a qualquer entidade futura com necessidades semelhantes — uma FK "ou-ou" que deixa de ser necessária após unificação de entidades, um nome de domínio que colide com palavra reservada do banco, um agrupamento muitos-para-muitos, ou um agregado sobre uma listagem filtrada — não só a `Party`/`Group`/ `Account` desta feature. A pedido explícito da usuária, esta revisão foi rodada antes da implementação (entre `/speckit-tasks` e `/speckit-implement`), como já ocorreu na feature 004; a revisão pós-implementação de 005 ainda deve ocorrer normalmente, focada em decisões que só emergirem durante a escrita do código. Emenda anterior (1.5.0 → 1.6.0) já estava commitada no momento desta revisão (`b35b0cf`), por isso o bump de versão em vez de incorporar as mudanças na mesma emenda.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): revisão pós-implementação de 005-counterparty-groups ainda pendente, a rodar depois de `/speckit-implement`.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.5.0 → 1.6.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: a preferência por `ALTER TABLE ... RENAME TO`/`RENAME COLUMN` em vez de recriar tabela/coluna pressupõe que há dado real a preservar; quando o projeto determina explicitamente que não há (ex.: ambiente local de desenvolvimento, sem dado de produção em jogo), uma migration MAY truncar e recriar a coluna/tabela diretamente, desde que a decisão seja explícita e registrada no spec da feature, não assumida por padrão.
- VI. Convenções de API REST — expandido: o factory estático `from(Entity)` de um DTO de resposta MAY aceitar um valor computado adicional como parâmetro (`from(Entity, valorComputado)`) quando o campo de resposta é uma agregação sobre outra entidade, não um dado persistido na própria entidade — a regra de cálculo permanece no `Service` (Princípio II), e o `Controller` repassa o resultado ao factory, nunca a DTO calculando-o sozinha.

Motivação: ambas as decisões emergiram do planejamento da feature 004-fund-entity-balance (conversão de `Fund` de enum para entidade, com saldo real calculado por agregação sobre `Account`) e do `/speckit.analyze` correspondente, que identificou dois desvios pontuais e já justificados da letra literal dos Princípios I e VI — sinal de que a constituição não cobria ainda esses dois casos genéricos (migração sem dado a preservar; DTO com campo derivado de agregação), que qualquer feature futura com necessidade semelhante teria que redecidir do zero. Diferente das emendas anteriores, esta é aplicada antes da implementação da feature (a pedido explícito da usuária, "rode a revisão da constituição agora, depois rodo novamente pós implementação") — a revisão pós-implementação de 004 ainda deve ocorrer normalmente, focada em decisões que só emergirem durante a escrita do código.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): revisão pós-implementação de 004-fund-entity-balance ainda pendente, a rodar depois de `/speckit.implement`.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.4.0 → 1.5.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: quando uma migration Flyway generaliza/renomeia uma entidade já existente, MUST preferir `ALTER TABLE ... RENAME TO`/`RENAME COLUMN` a recriar a tabela e copiar dados, preservando ids e histórico de auto-incremento; nova diretriz de modelagem de contraparte obrigatória mutuamente exclusiva entre duas (ou mais) entidades não relacionadas — uma FK nullable por contraparte possível + `CHECK CONSTRAINT` no banco, reforçada no `Service`, em vez de tabela de contraparte genérica ou herança JPA. Exemplos desatualizados corrigidos (`com.financas.resident` → `com.financas.account`; `UnitHasResidentsException` → `UnitHasAccountsException`; trio de seleção múltipla exemplificado com `unit-list`/`resident-list`/`receivable-list` → `unit-list`/`account-list`/`supplier-list`).
- IV. Convenções de Código e Formatação — expandido: um campo de tipo/discriminador que define o comportamento de uma entidade (ex.: `Account.type`) MUST ser obrigatório na criação e imutável na edição, incluído no mesmo DTO de `POST`/`PUT`, com o `Service.update()` comparando e rejeitando (400) qualquer tentativa de alteração — em vez de omitir o campo do `PUT` ou ignorá-lo silenciosamente. Exemplo desatualizado corrigido (`ResidentResponse.unit` → `AccountResponse.unit`).
- VI. Convenções de API REST — exemplos desatualizados corrigidos (`/api/residents` → `/api/accounts`; `POST /api/receivables/{id}/pay` → `POST /api/accounts/{id}/pay`).

Motivação: decisões que emergiram durante a implementação da feature 003-accounts-payable-suppliers (generalização de `Receivable` em `Account`, contraparte obrigatória mutuamente exclusiva entre `Unit` e o novo `Supplier`, campo `type` imutável) e que se aplicam a qualquer entidade futura com necessidades semelhantes — uma contraparte "ou-ou" entre entidades não relacionadas, ou um discriminador de tipo que não pode mudar depois de criado — não só a `Account`. Aproveitada a revisão para corrigir exemplos ilustrativos que citavam `Resident`/`Receivable`, removidos/renomeados por essa mesma feature, e que ficariam incorretos se deixados como estavam (as menções a `Resident`/ `Receivable` dentro dos Sync Impact Reports históricos abaixo foram preservadas, por descreverem decisões tomadas quando essas entidades ainda existiam). Emenda anterior (1.3.0 → 1.4.0) já estava commitada no momento desta revisão (`74d17df`), por isso o bump de versão em vez de incorporar as mudanças na mesma emenda.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.3.0 → 1.4.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: `BadRequestException` também MUST cobrir validação de query params sem DTO correspondente (ex.: filtros de listagem em formato livre), já que Bean Validation não se aplica a eles; no frontend, `shared/components/` passa a ser o local padrão para componentes de UI reutilizáveis por mais de uma tela (distinto de models/services/validators soltos em `shared/`), e toda listagem com seleção múltipla + remoção em lote MUST reaproveitar o trio `shared/list-selection.ts` + `shared/bulk-delete.ts` + `shared/components/bulk-actions-bar/`.
- IV. Convenções de Código e Formatação — expandido: um estado binário inteiramente derivável da presença/ausência de outro campo (ex.: "pago" quando `paymentDate` não é nula) MUST ser derivado desse campo, nunca duplicado como um segundo campo booleano persistido.
- VI. Convenções de API REST — expandido: a sub-rota `POST /{recurso}/bulk` (criação em massa) se generaliza para qualquer ação de negócio dedicada sobre um recurso existente que não seja edição completa nem criação (`POST /{recurso}/{id}/{ação}`, ex.: `.../pay`); filtros de leitura numa listagem MUST ser query params adicionais no mesmo `GET` de coleção (nunca rota nova), combinados por E lógico quando mais de um for informado.

Motivação: decisões que emergiram durante a rodada de correções pós-implementação da feature 002-receivable-charges (registro de pagamento, filtros de listagem, seleção múltipla + remoção em lote) e que se aplicam a qualquer entidade futura com necessidades semelhantes — uma ação de estado dedicada, um campo derivável, filtros combináveis, ou uma listagem que precise de remoção em lote — não só a `Receivable`/`Unit`/`Resident`. Diferente da emenda anterior (1.2.0 → 1.3.0), esta já estava commitada no momento desta revisão, por isso o bump de versão (em vez de incorporar as mudanças na mesma emenda).

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.2.0 → 1.3.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: `shared/exceptions/` passa a ter três bases genéricas (`NotFoundException` 404, `ConflictException` 409, `BadRequestException` 400, esta última nova); critério explícito de quando reforçar uma validação no `Service` via `BadRequestException` em vez de confiar só em Bean Validation no DTO (regra reaplicada em mais de um método, ou regra de negócio não puramente sintática).
- III. Stack Técnica Definida — expandido: Playwright fixado como devDependency permanente do frontend para validação manual em navegador (não faz parte da suíte de testes automatizados), reaproveitado a cada feature em vez de instalado/removido a cada rodada.
- IV. Convenções de Código e Formatação — expandido: datas em DTOs de API MUST trafegar no formato ISO-8601 padrão do `LocalDate` (`yyyy-MM-dd`, sem anotação `@JsonFormat` customizada); o formato DD/MM/AAAA fica restrito à UI, resolvido pelos recursos nativos do Angular/HTML (`DatePipe` para exibição, `<input type="date">` para entrada) sem exigir utilitário de conversão customizado (correção feita em duas rodadas ainda durante a implementação de 002-receivable-charges, antes do commit desta emenda: primeiro a primeira redação exigia dd/MM/yyyy também no contrato de API, o que forçava conversão manual desnecessária no backend; depois a usuária notou que os componentes nativos do Angular já fazem a conversão de exibição/entrada, tornando desnecessário até um utilitário de frontend dedicado); enums de domínio persistidos MUST usar `@Enumerated(EnumType.STRING)`, nunca `ORDINAL`.
- VI. Convenções de API REST — expandido: convenção de sub-rota `POST /{recurso}/bulk` para ações de criação em massa; formato padrão de erro 4xx agora explicitamente cobre JSON malformado/valor de campo inválido antes da Bean Validation rodar, via handler de `HttpMessageNotReadableException` já implementado em `GlobalExceptionHandler`.

Motivação: decisões que emergiram durante a implementação da feature 002-receivable-charges (primeira feature com campo de data, campo enum, ação de criação em lote, e uma segunda exception genérica de infraestrutura) e que se aplicam a qualquer entidade futura com essas mesmas necessidades, não só a `Receivable`. O gap de tratamento de JSON malformado foi encontrado pelo `/speckit.analyze` antes da implementação (risco real de violar o Princípio VI em runtime, não coberto por nenhum teste de caminho feliz) — a correção já é código genérico reaproveitável, e esta emenda só documenta que ela existe e por quê, para não ser removida ou duplicada por engano numa feature futura. A decisão sobre Playwright veio de a usuária notar que a dança de instalar/remover a cada rodada de validação era o gargalo real (reinstalação completa de `node_modules`), não o Playwright em si.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.1.0 → 1.2.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: estado local de componente Angular MUST usar `signal()` (não campo simples nem `BehaviorSubject`), consequência do app rodar zoneless; rotas de frontend MUST seguir o padrão `/{recurso-plural}`, `/{recurso-plural}/new`, `/{recurso-plural}/:id/edit`.
- VI. Convenções de API REST — expandido: DTOs de resposta MUST expor factory estático `from(Entity)`; relacionamentos em DTOs de resposta MUST embutir o DTO completo da entidade referenciada, não só o id; o interceptor HTTP do frontend MUST normalizar erros no objeto `ApiError`, consumido pelos componentes em vez de `HttpErrorResponse` bruto.
- III. Stack Técnica Definida — expandido: cobertura de teste automatizado para regras de negócio passa a ser obrigatória (não opcional como no template genérico do Spec Kit), para servir de proteção contra regressão entre features que rodam em sessões de IA isoladas.

Seções adicionadas:
- Revisão da Constituição Pós-Implementação — procedimento antes só documentado como prompt reaproveitável no README, agora formalizado aqui para poder ser invocado por referência curta em vez de prompt longo.
- Edição de Features Já Implementadas — idem, para o fluxo de mudança em feature existente sem rodar specify/plan/tasks do zero.

Seção expandida:
- Fluxo de Commits — detalha exatamente o que MUST entrar em cada uma das três subseções do README ("Decisões técnicas e premissas", "Revisões e correções das entregas da IA", "O que eu faria diferente...") em vez de uma instrução genérica.

Motivação: decisões que emergiram durante a implementação da feature 001-cadastro-condominos (código real, não só planejamento) e que se aplicam a qualquer entidade futura, não só a Unit/Resident. A obrigatoriedade de testes foi adicionada numa revisão posterior, quando a usuária questionou como validar que o código funciona sem testes automatizados — a resposta revelou que o template padrão do Spec Kit trata testes como opcionais por padrão, o que exigiria lembrar de pedir isso a cada feature nova sem essa regra. Os dois fluxos de processo (revisão da constituição e edição de feature existente) foram movidos do README para cá para poderem ser referenciados de forma curta em vez de exigir colar o prompt inteiro a cada vez.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.0.0 → 1.1.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: fixa pacote base do backend (`com.financas`), ferramenta de build (Maven), ferramenta de migração de schema (Flyway) e escopo exato da pasta `shared/` (só recursos verdadeiramente transversais; exceptions de regra de negócio de uma entidade vivem no `domain/` dela, não em `shared/`).
- III. Stack Técnica Definida — expandido: versões principais já adotadas MUST ser reutilizadas por novas features, em vez de re-pesquisadas a cada feature; a cláusula de "última versão estável" vale só para a escolha inicial de cada dependência. Também passa a fixar o stack de testes automatizados (backend: JUnit 5 + Mockito + Spring Boot Test; frontend: Vitest), ausente da versão anterior.

Princípios adicionados:
- VI. Convenções de API REST — novo princípio fixando padrão de rotas (`/api/{recurso-no-plural-em-inglês}` + verbos HTTP padrão), formato de erro padronizado (`{ "message": string, "status": number }`) e a regra de que confirmação de ações destrutivas é responsabilidade do frontend, não do backend.

Seções removidas: nenhuma

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem referências desatualizadas; "Constitution Check" é preenchido dinamicamente por feature
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias
- ✅ .claude/skills/speckit-*/SKILL.md — sem referências específicas de agente que precisem de ajuste
- ⚠ README.md — manter alinhado conforme decisões técnicas evoluírem (regra já registrada na seção de Commits)
- ⚠ specs/001-cadastro-condominos/plan.md e tasks.md — criados antes desta emenda com `DuplicateUnitException`/`UnitHasResidentsException` em `shared/exceptions/`; ajustados para `unit/domain/` para conformidade com o novo texto do Princípio I
- ✅ specs/001-cadastro-condominos/contracts/api.md — já segue o formato de erro e a convenção de rotas agora fixados no Princípio VI (decisão local antecipou a regra global)

Itens pendentes (TODO):
- Nenhum.
-->

# Finanças (Sistema de Gerenciamento de Condomínio) Constitution

## Core Principles

### I. Arquitetura em Camadas
O sistema segue arquitetura em camadas, com frontend (Angular) e backend (Spring Boot) organizados por domínio. O pacote base do backend Java MUST ser `com.financas`, com cada entidade de domínio em seu próprio subpacote (ex.: `com.financas.unit`, `com.financas.account`) contendo `api/` (controllers e contratos/DTOs), `domain/` (entidade, enums, repository, service, e as exceptions que representam regras de negócio daquela entidade — ex.: unicidade de um identificador, bloqueio de remoção por vínculo) e `infra/` (implementação do repository). A ferramenta de build do backend MUST ser Maven; a migração de schema de banco MUST ser feita via Flyway, com migrations versionadas em `src/main/resources/db/migration`. Quando o nome de domínio natural de uma entidade colide com uma palavra reservada do SQL (ex.: `GROUP`), a classe e o pacote Java MUST manter o nome de domínio (ex.: `Group`, `com.financas.group`), mas a tabela física MUST usar um nome alternativo que evite a colisão (ex.: `party_group`) — evita ter que escapar o identificador em toda consulta manual ou ferramenta de inspeção do banco. Quando uma migration generaliza ou renomeia uma entidade já existente (ex.: `Receivable` → `Account`), MUST preferir `ALTER TABLE ... RENAME TO`/ `RENAME COLUMN` a criar a tabela do zero e copiar dados via `INSERT ... SELECT` — preserva os `id`s e o histórico de auto-incremento já existentes, além de ser mais direto que recriar e migrar. Essa preferência pressupõe que há dado real a preservar; quando o projeto determina explicitamente que não há (ex.: ambiente local de desenvolvimento, sem dado de produção em jogo), uma migration MAY truncar e recriar a coluna/tabela diretamente em vez de renomear — desde que essa decisão seja explícita e registrada no spec da feature (Assumptions ou Clarifications), nunca assumida por padrão. Quando uma coluna existente se torna inteiramente sem uso (nenhuma regra de negócio ou consulta depende do seu valor) e a decisão é removê-la — não renomear ou generalizar a entidade que a contém —, uma migration MAY remover a coluna diretamente via `ALTER TABLE ... DROP COLUMN`, sem o cuidado de preservação de dado real que motiva a preferência por `RENAME` acima; essa decisão também MUST ser explícita e registrada no spec da feature (Assumptions ou Clarifications), pelo mesmo motivo — introduzida na feature 008-partial-payment-split, ao remover `Account.recurring`.

Recursos compartilhados ficam em `shared/`, mas essa pasta MUST conter apenas recursos verdadeiramente transversais a mais de uma entidade: `GlobalExceptionHandler`, exceptions genéricas de infraestrutura reaproveitáveis por qualquer entidade (`NotFoundException` → 404, `ConflictException` → 409, `BadRequestException` → 400) e configuração técnica (ex.: CORS). `BadRequestException` MUST ser usada quando uma regra de validação (ex.: "valor deve ser positivo") precisa ser reforçada no `Service` — porque é reaplicada em mais de um método (criar, editar, criar em lote) ou porque a regra de negócio em si não é puramente sintática — em vez de depender só de Bean Validation no DTO; quando a regra é puramente sintática e vale para um único ponto de entrada (campo obrigatório, formato), Bean Validation no DTO (`@NotBlank`, `@Positive`, etc.) já basta, sem duplicar a checagem no `Service`. Uma exception que representa uma regra de negócio específica de uma entidade NUNCA deve viver em `shared/` — deve viver no `domain/` da própria entidade, mesmo que estenda uma das três bases acima (ex.: `UnitHasAccountsException extends ConflictException`). `BadRequestException` também MUST ser usada para validar query params que não correspondem a nenhum campo de DTO (ex.: um filtro de listagem em formato livre, como um mês/ano) — Bean Validation não se aplica a esses parâmetros, então a validação/parsing ocorre diretamente no `Service`.

Quando uma entidade precisa de uma contraparte obrigatória que só pode ser exatamente uma dentre duas (ou mais) outras entidades não relacionadas entre si (ex.: `Account.unit`/ `Account.supplier`), MUST ser modelada como uma FK nullable por contraparte possível — nunca um campo `id` solto sem FK de banco de verdade — validada em duas camadas: uma `CHECK CONSTRAINT` no banco garantindo que exatamente uma está preenchida, coerente com o discriminador da entidade (ex.: `type`), e a mesma regra reforçada no `Service` (regra cruzada entre campos, não puramente sintática — mesmo critério de `BadRequestException` acima). Evita introduzir uma tabela de "contraparte" genérica com discriminador manual ou herança JPA entre entidades que não compartilham comportamento real além de "poder ser contraparte" — complexidade desproporcional ao tamanho deste projeto. Esse padrão foi aplicado a `Account.unit`/`Account.supplier` até a feature 005, que unificou `Unit` e `Supplier` numa única entidade (`Party`), eliminando a necessidade de duas FKs mutuamente exclusivas nesse caso específico — o padrão em si permanece válido e MUST ser reaplicado sempre que uma situação futura real exigir exatamente uma dentre duas (ou mais) entidades não relacionadas como contraparte obrigatória.

Quando uma entidade representa um agrupamento nomeado de outras entidades da mesma natureza (relação muitos-para-muitos, ex.: `Group` agrupando `Party`), a composição do agrupamento (quais entidades pertencem a ele) MUST ser editada exclusivamente pela tela/formulário do próprio agrupamento — nunca pela tela de cada entidade membro — evitando duplicar a mesma interface de associação em dois lugares sem uma fonte de verdade única.

Uma associação `LAZY` (padrão de `@ManyToMany`/`@OneToMany`) que o `Controller` precisa ler para montar o DTO de resposta (Princípio VI) MUST vir já resolvida pela própria consulta de leitura do `Repository` (`findById`/`findAll`) que alimenta esse fluxo — nunca por uma segunda consulta corretiva depois de um `save()`, nem contando com `@Transactional` no `Service` (Princípio II): a sessão do Hibernate já se encerrou quando o `Controller` recebe o retorno, dado que o projeto roda com `spring.jpa.open-in-view: false`. Duas formas válidas de resolver na própria consulta: `JOIN FETCH` numa query dedicada do Spring Data (`@Query` com `LEFT JOIN FETCH`) — preferencial, por só pagar o custo do `JOIN` nas consultas que de fato precisam da associação — ou `fetch = EAGER` direto na entidade, reservado para quando literalmente nenhum consumidor da entidade jamais precisaria dela sem aquela associação (ex.: `Group.members` — a única razão de um `Group` existir é agrupar `Party`s). Quando `JOIN FETCH` precisar trazer mais de uma coleção `List` da mesma entidade na mesma query, o Hibernate rejeita com `MultipleBagFetchException` — daí a preferência por `Set` sempre que a coleção não tiver ordem de negócio própria (Princípio IV); se duas coleções precisarem mesmo ser `List` (ordem de negócio genuína em ambas) e precisarem ser carregadas juntas, a saída é aceitar N+1 — tolerável dado o volume pequeno de dados deste projeto — mas só se o acesso ficar inteiramente dentro do `Service`; se o `Controller` precisar do campo, ele MUST vir por `JOIN FETCH`/`EAGER`, nunca por N+1 tocado na camada de apresentação.

Um processo agendado do backend (`@Scheduled`) que gera ou processa itens pendentes MUST também reagir a `@EventListener(ApplicationReadyEvent.class)` no mesmo método público — nunca em um método de recuperação separado com lógica duplicada — reaproveitando exatamente o mesmo cálculo de "o que está pendente" usado pelo gatilho agendado. Garante que um ciclo perdido por a aplicação estar fora do ar no horário exato do agendamento (relevante dado o plano de hospedagem em PaaS, potencialmente sujeito a cold start/ociosidade — ver `CLAUDE.md`) seja recuperado assim que a aplicação volta a rodar, sem exigir intervenção manual nem esperar o próximo ciclo. Introduzido na feature 009-recurring-charges, ao unificar `RecurringChargeGenerationService.generatePendingAccounts()` sob os dois gatilhos (`@Scheduled(cron = "0 0 6 25 * *", zone = "America/Sao_Paulo")` + `@EventListener(ApplicationReadyEvent.class)`), MUST ser reaplicado a qualquer processo agendado futuro deste projeto.

No frontend, cada entidade de domínio possui sua pasta de componentes, com `core/` para tratamento de erros e `shared/` para models, services, validators e configuração de URL base da API; `shared/components/` reúne componentes de UI reutilizáveis por mais de uma tela (ex.: `bulk-actions-bar/`), distintos dos models/services/validators soltos em `shared/`. Toda listagem que precisar de seleção múltipla + remoção em lote MUST reaproveitar o trio já estabelecido `shared/list-selection.ts` (estado de seleção, signal-based) + `shared/bulk-delete.ts` (remoção item a item, melhor esforço, sem endpoint transacional) + `shared/components/bulk-actions-bar/` (UI da barra de ação) — introduzido na feature 002-receivable-charges e já aplicado a `unit-list`/`account-list`/`supplier-list` — em vez de reimplementar seleção/remoção em lote do zero numa tela nova. Quando uma listagem precisar de uma ação em lote adicional além da remoção (ex.: duplicar os itens selecionados), `BulkActionsBar` MUST ser estendido com um input opcional (valor padrão `false`) que controla a exibição do(s) botão(ões) extra(s) e um `output()` dedicado por ação nova — nunca criando um componente de barra de ações dedicado a uma única tela — preservando sem alteração o comportamento das demais listagens que reaproveitam o componente (padrão introduzido na feature 007-duplicate-account-next-month, ao adicionar duplicação em lote apenas à listagem de contas). A checkbox de seleção por linha de qualquer listagem MUST usar `(click)="selection.toggleWithRange(item, items(), $event.shiftKey)"` — nunca `(change)="selection.toggle(item)"` — porque só o evento `click` carrega o estado da tecla Shift; `toggleWithRange` (âncora fixa no último clique normal, introduzido na feature 006-inline-edit-shift-select) preserva o comportamento de clique simples existente e adiciona seleção em intervalo com Shift+clique, de forma idêntica em todas as listagens que reaproveitam `list-selection.ts`. Toda listagem que reaproveita esse trio MUST também destacar visualmente a linha selecionada via `[class.table-active]="selection.isSelected(item)"` no `<tr>` — comportamento inicialmente implementado apenas em `account-list` pela feature 007-duplicate-account-next-month e generalizado para toda listagem do trio pela feature 002-receivable-charges (FR-024), a partir de revisão da usuária apontando que o destaque está associado à seleção múltipla em si, não a uma tela isolada. O checkbox de seleção (linha e "selecionar todos") de toda listagem que reaproveita esse trio MUST usar a classe `form-check-input row-select-checkbox` — a primeira do Bootstrap (transição nativa ao marcar/desmarcar), a segunda com a regra de tamanho centralizada em `frontend/src/styles.scss` (área clicável maior que o checkbox nativo do navegador) — generalizado pela feature 002-receivable-charges (FR-025) a partir de revisão da usuária apontando que nenhuma listagem tinha tratamento visual dedicado no checkbox, mesmo espírito já aplicado ao destaque de linha (FR-024). Um atalho de teclado global de uma listagem (ex.: Ctrl+C/Ctrl+V para duplicar os itens selecionados, introduzido na feature 007-duplicate-account-next-month) MUST ser implementado via `@HostListener('document:keydown')` no componente da tela, com guarda por `document.activeElement` que ignora o atalho apenas quando o foco está em edição de texto real (`INPUT` de tipo textual/numérico/data, `TEXTAREA`, ou `contentEditable`) — nunca `checkbox`/`radio`, que também são elementos `<input>` mas representam uma ação de seleção, não de edição de texto; tratá-los como campo editável bloquearia o atalho logo depois de qualquer clique na própria checkbox de seleção da listagem. Um campo de edição inline em tabela que confirma automaticamente ao perder foco (`blur`) e precisa ser cancelável sem salvar ao iniciar outra ação na mesma tela (outra edição inline, ou outro fluxo na mesma linha) MUST suprimir o `blur` nos gatilhos dessa outra ação via `(mousedown)="$event.preventDefault()"` — o `blur` do campo em edição dispara antes do `click` do gatilho que deveria cancelá-lo, então sem essa supressão a edição abandonada seria salva em vez de descartada. Toda listagem MUST reaproveitar `shared/components/row-actions/` para as ações individuais de editar/remover por linha — ícones do pacote `bootstrap-icons` (`<i class="bi bi-*" aria-hidden="true">`, com `title`/`aria-label` no elemento pai clicável, nunca texto) — introduzido na feature 005-counterparty-groups a partir de uma revisão da usuária (`account-list` tinha ficado largo demais e ganhou ícones; `fund-list`/`party-list`/`group-list` ainda usavam texto, inconsistência corrigida generalizando o componente para as quatro telas) e MUST ser reaproveitado por qualquer listagem nova em vez de reimplementar botões de ação do zero. Qualquer ícone novo em botão/link sem texto MUST vir do pacote `bootstrap-icons` (já instalado; CSS importado uma única vez em `frontend/src/styles.scss`, via `@import "bootstrap-icons/font/bootstrap-icons.css";`) — nunca `path`s SVG copiados à mão no template: a prática original (feature 005) evitava uma dependência nova, mas deixava cada ícone sujeito a duas escolhas manuais sem garantia (o glifo certo entre variantes parecidas, e um `vertical-align` de compensação duplicado por componente); a feature 008-partial-payment-split expôs o risco real — um glifo copiado à mão (`check2`) cuja tinta não é centralizada no próprio `viewBox` deixou um ícone visivelmente torto — e a usuária decidiu trocar pelo pacote em vez de introduzir uma checagem manual a cada ícone novo, já que os glifos de `bootstrap-icons` vêm centralizados para uso em botão sem ajuste adicional. Estado local de componente MUST ser gerenciado via `signal()` do Angular — não por campo simples nem `BehaviorSubject` — consequência do app rodar em modo zoneless (sem Zone.js). Rotas MUST seguir o padrão `/{recurso-plural}` (listagem), `/{recurso-plural}/new` (criação) e `/{recurso-plural}/:id/edit` (edição), espelhando a convenção de rotas de API do Princípio VI.

A estrutura de pastas proposta é uma referência, não uma regra rígida: pode ser adaptada sempre que o padrão de mercado ou a necessidade do projeto justificar o desvio — exceto pacote base, ferramenta de build e ferramenta de migração definidos acima, que MUST permanecer estáveis entre features, salvo decisão explícita e documentada em contrário.

### II. Separação Controller → Service → Repository
O `Service` concentra as regras de negócio e é o único ponto que chama a interface `Repository` quando necessário. O `Controller` NUNCA acessa o `Repository` diretamente — toda a orquestração de dados passa pelo `Service`. Essa separação MUST ser respeitada em toda nova funcionalidade de backend, garantindo que a lógica de negócio permaneça centralizada e testável independentemente da camada de apresentação.

Um método de `Service` que executa mais de uma operação de escrita (`save`/`delete`, sequencial ou em loop) para completar uma única operação de negócio MUST ser anotado `@Transactional` — sem isso, cada chamada ao `Repository` é sua própria transação independente do Spring Data JPA, e uma falha no meio da operação deixaria parte do trabalho já comitada (ex.: um `save()` por integrante de um lote — se um falhar, os anteriores já persistiram). O critério é objetivo (quantas escritas o método faz), verificável por inspeção, em vez de uma decisão caso a caso. Deliberadamente NÃO é `@Transactional` no nível da classe inteira: isso tornaria toda entidade lida por qualquer método (inclusive os que só leem) gerenciada pelo `EntityManager` durante toda a execução, arriscando persistência silenciosa via dirty-checking do Hibernate caso algum campo seja mutado por qualquer motivo que não seja a intenção de salvar — expondo métodos que não precisam de transação nenhuma a um risco novo, só para proteger os poucos que precisam. Leitura de uma associação `LAZY` que precisa acontecer dentro do próprio método do `Service` (não durante a montagem do DTO no `Controller`, ver abaixo) MUST ser resolvida preferencialmente por estratégia de fetch (`fetch = EAGER`/`JOIN FETCH`/`@EntityGraph`, Princípio I), que não abre escopo de transação nem carrega esse risco; `@Transactional(readOnly = true)` pontual no método fica reservado para o caso raro em que a estratégia de fetch não for viável (`readOnly = true` desativa o flush automático do Hibernate, evitando o mesmo risco de escrita acidental). Em nenhum caso `@Transactional` no `Service` substitui a estratégia de fetch para associações lidas durante a montagem do DTO de resposta no `Controller` (Princípio VI) — a transação do `Service` já foi encerrada quando o `Controller` recebe o retorno, dado que este projeto roda com `spring.jpa.open-in-view: false`.

Uma operação de negócio que escreve em mais de um `Service` MUST ser orquestrada por um único método `@Transactional` — nunca pelo `Controller` chamando os `Service`s envolvidos em sequência, o que deixaria uma escrita já comitada de forma inconsistente caso uma chamada posterior falhasse. A propagação padrão do Spring (`Propagation.REQUIRED`) garante que qualquer `Service`/`Repository` chamado de dentro desse método participe da mesma transação, sem precisar fundir os `Service`s envolvidos numa classe só.

Uma operação de leitura (`findById`, `existsBy*`, agregação) sobre entidade de outro domínio MAY acessar o `Repository` daquele domínio diretamente — não constrói nem persiste nada, não arrisca criar um estado inválido. Uma operação de escrita (criar/persistir) sobre entidade de outro domínio MUST passar pelo `Service` dono dela, nunca pelo `Repository` direto — é o único ponto que garante a invariante de negócio daquela entidade para qualquer chamador, presente ou futuro; deixar o `Repository` de outro domínio aberto para escrita, mesmo que pontualmente, reabre exatamente o risco que a validação do `Service` existe para fechar. Se essa regra levar a uma dependência de escrita genuinamente bidirecional entre dois `Service`s de CRUD (`ServiceA` precisando escrever na entidade de `ServiceB` e vice-versa), a saída MUST ser um terceiro `Service` dedicado ao caso de uso que depende dos dois lados — nunca os dois CRUDs dependendo um do outro, nem `Repository` vazando para escrita cross-domain só para evitar esse ciclo. Padrão já aplicado no projeto: `RecurringChargeGenerationService` (que gera `Account`s a partir de cobranças recorrentes) depende de `AccountService` para a escrita, sem que `AccountService` dependa de volta — identificado numa revisão direta de código que descobriu `RecurringChargeGenerationService.generateOne()` construindo `Account` e gravando via `AccountRepository` diretamente, ignorando por completo a validação de `AccountService`.

### III. Stack Técnica Definida
O backend usa Java com Spring Boot (Spring Data JPA, Spring Security, Spring Web); o frontend usa Angular com TypeScript, Bootstrap e SCSS; a persistência usa PostgreSQL. As versões principais (linguagem, framework, banco) MUST ser as últimas estáveis e consolidadas no mercado no momento da escolha inicial de cada dependência. Uma vez que uma versão tenha sido adotada e já exista código do projeto usando-a, novas features MUST reutilizar essa mesma versão em vez de pesquisar/adotar "a mais recente do mercado" novamente — divergir de uma versão já adotada exige decisão explícita e deliberada, registrada nesta constituição como atualização, e não uma escolha implícita feita em uma nova sessão/feature. Testes automatizados usam JUnit 5 + Mockito + Spring Boot Test no backend e Vitest no frontend — mesma regra de reuso de versão já adotada se aplica a essas ferramentas. Toda regra de negócio (validações, unicidade, bloqueios de exclusão por vínculo, cálculos) MUST ter cobertura de teste automatizado antes de a funcionalidade ser considerada concluída — diferente do padrão genérico de templates do Spec Kit, que trata testes como opcionais salvo pedido explícito, este projeto exige que `/speckit.tasks` gere tarefas de teste para regras de negócio em toda feature, independentemente de solicitação explícita a cada vez; a garantia de que uma feature não quebra outra, dado que cada uma é implementada numa sessão de IA isolada, vem de revisão humana combinada com essa cobertura de teste. Não há, por ora, bibliotecas específicas a evitar — decisões de descontinuação de uma biblioteca por experiência prévia negativa devem ser registradas aqui quando ocorrerem, para não serem repetidas.

Playwright é usado como ferramenta de validação manual em navegador headless durante a implementação (não faz parte da suíte de testes automatizados definida acima, que continua sendo só JUnit 5 + Mockito + Spring Boot Test / Vitest). Uma vez que o frontend tenha telas para validar, o Playwright MUST ser mantido como devDependency permanente de `frontend/package.json` (`npm install --save-dev playwright`) e reaproveitado diretamente a cada feature, em vez de instalado/removido a cada rodada — a reinstalação completa de `node_modules` necessária para limpar uma instalação temporária custa bem mais tempo do que a instalação do Playwright em si (binário do Chromium fica em cache local, fora do projeto).

### IV. Convenções de Código e Formatação
Datas exibidas à usuária final (UI) MUST seguir o formato DD/MM/AAAA. Internamente — persistência e contrato de API (JSON de request/response) — todo campo de data MUST ser `LocalDate`, trafegando no formato ISO-8601 padrão do Jackson (`yyyy-MM-dd`), sem anotação `@JsonFormat` customizada; o backend NUNCA converte para o formato brasileiro. A conversão para exibição/entrada em DD/MM/AAAA é responsabilidade exclusiva do frontend, usando os recursos nativos do Angular/HTML já suficientes para isso — `DatePipe` (`| date:'dd/MM/yyyy'`) para exibição e `<input type="date">` para entrada, cujo valor já trafega em ISO-8601 nativamente — sem introduzir um utilitário de conversão customizado quando esses recursos nativos já resolvem. Enums de domínio persistidos via JPA MUST usar `@Enumerated(EnumType.STRING)`, nunca `ORDINAL` (evita corromper dados existentes se a ordem de declaração dos valores do enum mudar no futuro). Um estado binário inteiramente derivável da presença/ausência de outro campo (ex.: um lançamento "pago" quando sua `paymentDate` não é nula) MUST ser derivado desse campo, nunca duplicado como um segundo campo booleano persistido — evita os dois ficarem inconsistentes entre si. Um campo de tipo/discriminador que define o comportamento ou as regras de uma entidade (ex.: `Account.type`) MUST ser obrigatório na criação e imutável na edição: o mesmo DTO de request usado em `POST`/`PUT` inclui o campo, e o `Service.update()` compara o valor recebido com o já persistido, rejeitando (400, via exception dedicada) qualquer tentativa de alterá-lo — em vez de omitir o campo do `PUT` ou ignorá-lo silenciosamente, o que esconderia uma tentativa inválida (seja da usuária ou de um bug no frontend) em vez de avisar sobre ela. Uma coleção de entidades numa relação `@ManyToMany`/`@OneToMany` sem ordem de negócio própria MUST ser modelada como `Set`, não `List` — evita a complexidade de uma coluna de ordenação (`@OrderColumn`) sem necessidade real, e evita que um `JOIN FETCH` dessa coleção junto de outra coleção da mesma entidade, na mesma consulta, quebre com `MultipleBagFetchException` (o Hibernate rejeita duas coleções `List` — "bags" sem ordem — carregadas via `JOIN FETCH` simultaneamente; com `Set` isso não acontece, ver Princípio I); quando a resposta de API precisar de uma ordem determinística (ex.: alfabética), essa ordenação MUST ser aplicada na camada de DTO/resposta, nunca persistida. Nomes de variáveis, classes, métodos, propriedades e tabelas de banco de dados MUST estar em inglês. Mensagens de erro internas (exceptions, logs) MUST estar em inglês. Mensagens de erro exibidas ao usuário final (respostas de API, frontend) MUST estar em português.

### V. Idioma por Tipo de Conteúdo
Todo conteúdo gerado neste projeto (por pessoas ou por IA) MUST respeitar a tabela abaixo. Em caso de dúvida sobre uma categoria não listada, a pessoa responsável MUST perguntar antes de assumir um idioma.

| Tipo de conteúdo | Idioma |
|---|---|
| Nomes de variáveis, classes, métodos, propriedades, tabelas de banco | Inglês |
| Mensagens de erro internas (exceptions, logs) | Inglês |
| Documentação de método (XML doc /// summary, JSDoc) | Inglês |
| Nomes de commit, branch, arquivo, projeto | Inglês |
| Comentários soltos no código (explicando lógica de negócio) | Português |
| Arquivos do Spec Kit: constitution.md, spec.md, plan.md, tasks.md | Português |
| Mensagens de erro exibidas ao usuário final (API response, frontend) | Português |
| README.md | Português |

### VI. Convenções de API REST
Rotas MUST seguir o padrão `/api/{recurso-no-plural-em-inglês}` (ex.: `/api/units`, `/api/accounts`), usando os verbos HTTP padrão: `GET` para listagem/consulta, `POST` para criação, `PUT /{id}` para edição completa e `DELETE /{id}` para remoção. Quando um recurso precisar de uma ação de criação em massa (aplicar os mesmos dados a todas as instâncias de outro recurso relacionado), essa ação MUST ser exposta como sub-rota `POST /{recurso}/bulk`, com corpo igual ao do `POST` individual menos o identificador do recurso relacionado (que passa a ser implícito — "todas as instâncias existentes no momento da chamada"), nunca sobrecarregando o `POST` individual com um identificador opcional. O mesmo padrão de sub-rota se generaliza para qualquer ação de negócio dedicada sobre um recurso existente que não seja nem edição completa (`PUT`) nem criação (`POST` no recurso plano) — `POST /{recurso}/{id}/{ação}` (ex.: `POST /api/accounts/{id}/pay`, para registrar pagamento) — reaproveitando esse padrão de rota em vez de inventar um novo a cada ação. Quando uma ação de negócio dedicada já existente precisar suportar uma variação que continua sendo a mesma operação de negócio (ex.: registrar pagamento integral, parcial ou a maior — magnitudes diferentes do mesmo valor informado, não três ações distintas), o corpo da requisição dessa ação MUST ganhar um campo novo opcional, resolvido para um valor-padrão razoável quando ausente, em vez de uma rota dedicada nova — evita multiplicar endpoints para o que é, na visão do domínio, uma única ação parametrizada (padrão introduzido na feature 008-partial-payment-split, ao adicionar `paidAmount` opcional a `POST /api/accounts/{id}/pay` em vez de criar `/pay-partial`). Filtros de leitura numa listagem (`GET /{recurso}`) MUST ser expostos como query params adicionais no mesmo endpoint de coleção, nunca como rotas novas; quando mais de um filtro for informado simultaneamente, MUST ser combinados por E lógico. Toda listagem exposta via `GET /{recurso}` (coleção) MUST definir um critério de ordenação padrão explícito e determinístico, aplicado mesmo sem nenhum filtro informado — nunca depender da ordem incidental de retorno do banco, que não é garantida e pode mudar conforme updates; quando não houver um critério de negócio óbvio, MUST incluir ao menos um campo estável (ex.: `id`) como desempate final, garantindo que a mesma consulta sempre retorne os registros na mesma ordem (padrão introduzido na feature 003-accounts-payable-suppliers, FR-024, ao definir a ordenação por `dueDate`/`description`/`id` da listagem de contas).

Toda resposta de erro (4xx) MUST seguir o formato padronizado `{ "message": string, "status": number }`, com `message` em português (ver Princípios IV e V) — isso inclui JSON malformado ou um valor de campo que quebre a deserialização antes da validação Bean Validation rodar (ex.: um valor de enum fora do conjunto esperado), cobertos por um `@ExceptionHandler(HttpMessageNotReadableException.class)` genérico já implementado no `GlobalExceptionHandler`, reaproveitado automaticamente por qualquer entidade nova sem precisar de código extra. Confirmação de ações destrutivas (remoção) é responsabilidade exclusiva do frontend (diálogo de confirmação antes da chamada); o endpoint `DELETE` MUST executar a remoção diretamente quando chamado, sem etapa de confirmação própria no backend.

DTOs de resposta MUST expor um factory estático `from(Entity)` que constrói o DTO a partir da entidade de domínio. Quando um DTO de resposta representa uma entidade que referencia outra, MUST embutir o DTO de resposta completo da entidade referenciada (ex.: `AccountResponse.unit: UnitResponse`), nunca apenas o identificador. Quando um campo de resposta é derivado por agregação sobre outra entidade (não um dado persistido na própria entidade — ex.: um saldo calculado a partir de lançamentos vinculados), o factory MAY aceitar esse valor já calculado como parâmetro adicional (`from(Entity, valorComputado)`) — a regra de cálculo permanece no `Service` (Princípio II), e o `Controller` é responsável por chamar o `Service` e repassar o resultado ao factory, nunca a própria DTO calculando-o sozinha. Esse padrão de campo computado no backend se aplica quando o agregado depende de dados além da lista já carregada pela tela (ex.: o saldo real de um Fundo, que soma lançamentos que podem não estar na página/filtro atual) — o cálculo MUST ficar no `Service`, exposto via API. Quando, ao contrário, o agregado é uma função pura da própria lista que a tela já carregou e filtrou (ex.: um total exibido ao final de uma tabela, refletindo só as linhas atualmente visíveis), o cálculo MAY ser feito inteiramente no frontend (ex.: `computed()` do Angular sobre o signal já populado), sem endpoint dedicado nem campo novo na resposta. No frontend, o interceptor HTTP MUST normalizar toda resposta de erro no formato acima em um objeto `ApiError` consumido pelos componentes — componentes NUNCA devem ler `HttpErrorResponse` bruto diretamente. Quando o DTO de resposta completo de uma entidade referenciada expõe um campo computado por agregação sobre outra entidade (o caso descrito acima, `from(Entity, valorComputado)`), e esse campo não é necessário nos pontos onde essa entidade é apenas referenciada por outra (embutida como estrangeira, não como o próprio recurso da requisição), o DTO embutido MAY ser uma versão resumida da entidade — mesmos campos próprios (nunca só o identificador), omitindo apenas o campo computado — reservando o DTO completo (com o campo computado) para o endpoint que representa a própria entidade referenciada. Essa divisão exige verificação real de que nenhum consumidor do embutimento precisa do campo computado, não uma suposição; havendo qualquer indício de necessidade real, o DTO completo continua sendo o padrão. Introduzido na feature 004 (`FundSummaryResponse`), ao perceber que `AccountResponse.fund`/`RecurringChargeResponse.fund` recalculavam `Fund.realBalance` a cada linha de listagem sem que o valor fosse exibido em nenhuma delas.

## Restrições Transversais

Não há, por enquanto, regras de negócio ou técnicas universais obrigatórias (ex.: formato monetário fixo, retrocompatibilidade de API) nem restrições de segurança, compliance ou performance em vigor. É possível que autenticação/autorização sejam implementadas futuramente; quando isso ocorrer, esta seção MUST ser emendada para registrar as novas restrições transversais antes de sua adoção no código.

## Checkpoints da Rodada de Trabalho

Mensagens de commit MUST ser curtas, de uma linha (`tipo: descrição curta`, sempre em inglês — ex.: `fix: ...`, `feat: ...`, `docs: ...`), sem corpo com bullets; essa regra vale para qualquer commit proposto no projeto, não só nos dois momentos abaixo. Fora desses dois momentos (ex.: uma correção avulsa que não passa pelo fluxo SDD), preferir um único commit por tarefa — só dividir em commits separados quando houver unidades claramente distintas e independentes entre si (ex.: uma correção de bug não relacionada descoberta no meio do caminho), não apenas porque a mudança tocou várias camadas ou arquivos de uma mesma tarefa.

Definem-se os seguintes momentos do fluxo de desenvolvimento SDD: (1) **fim do trabalho de documentação** — dentro de uma feature nova, ao concluir o `speckit-analyze`; dentro de uma alteração direta (feature já implementada ou ainda em andamento, ver seção "Edição de Features Já Implementadas"), ao concluir os ajustes nos artefatos de `specs/[NÚMERO-NOME-DA-FEATURE]/`; e (2) **fim do trabalho de implementação** — dentro de uma feature nova, ao concluir o `speckit-converge` (incluindo as revisões pedidas pela usuária sobre o código entregue); dentro de uma alteração direta, ao concluir a implementação de código correspondente.

Cada um desses dois momentos MUST disparar proativamente (sem que a usuária precise solicitar) a skill `round-checkpoint`, responsável por propor a mensagem do commit daquele momento, sugerir alterações ao `README.md` e à própria `constitution.md`, e conduzir a aprovação da usuária sobre a entrega daquele momento e sobre as duas sugestões, corrigindo o que for solicitado até a aprovação — o procedimento completo está detalhado na definição da skill, não duplicado aqui.

## Edição de Features Já Implementadas

Sempre que a usuária pedir uma alteração de funcionalidade do projeto diretamente, sem ser via comando do Spec Kit — inclusive durante o desenvolvimento de uma feature ainda em andamento —, NUNCA altere o código diretamente: SEMPRE encontre a(s) feature(s) impactada(s) em `specs/` (perguntando à usuária se não for óbvio pelo pedido) e edite a documentação correspondente antes de qualquer código, nesta ordem:

1. **spec.md**: atualizar apenas as seções afetadas pela mudança (requisitos funcionais, regras de negócio, edge cases), preservando o resto do arquivo intacto. Se a mudança introduzir ambiguidade nova, sinalizar com `[NEEDS CLARIFICATION]` em vez de assumir uma resposta.
2. **plan.md**: atualizar apenas as seções tecnicamente impactadas, sem regenerar o arquivo inteiro. Ao final, confirmar explicitamente, princípio por princípio desta constituição, como cada um relevante continua sendo respeitado após a mudança — mesmo que a resposta seja "sem alteração necessária".
3. **tasks.md**: adicionar apenas as tarefas novas necessárias, sem regenerar a lista inteira e sem alterar o status de tarefas já concluídas (`[X]`). Se uma tarefa já concluída precisar ser refeita por causa da mudança, marcá-la explicitamente como pendente de novo, explicando o motivo, em vez de resetar tudo.
4. **contracts/api.md** (se existir e for afetado): atualizar apenas os contratos de endpoint impactados.

Antes de implementar, mostrar um resumo do que mudou em cada artefato de documentação para a usuária revisar. NÃO escrever nem alterar código de implementação até a aprovação explícita, a menos que a usuária já tenha pedido para pular essa espera na própria solicitação.

## Governance

Esta constituição prevalece sobre qualquer outra prática ou convenção informal adotada no projeto. Qualquer decisão arquitetural já tomada e registrada aqui SHOULD ser mantida sem revisitação, a menos que surja uma justificativa forte — nesse caso, a usuária deve ser consultada antes de qualquer mudança.

Emendas a esta constituição exigem: (1) descrição clara da mudança e sua motivação, (2) atualização da versão conforme versionamento semântico (MAJOR para remoção/redefinição incompatível de princípios, MINOR para adição ou expansão material de princípio/seção, PATCH para esclarecimentos e correções de redação), e (3) verificação de que os templates dependentes (`plan-template.md`, `spec-template.md`, `tasks-template.md`) permanecem consistentes com o texto atualizado.

Toda revisão de spec, plano ou tarefas MUST verificar conformidade com os princípios definidos aqui. Complexidade adicional (novas camadas, dependências, padrões) MUST ser justificada em relação aos princípios de simplicidade implícitos na arquitetura em camadas descrita no Princípio I.

**Version**: 1.21.0 | **Ratified**: 2026-07-24 | **Last Amended**: 2026-08-05
</content>
