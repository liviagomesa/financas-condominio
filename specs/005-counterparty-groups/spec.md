# Feature Specification: Unificação de Unidade/Fornecedor, Contas sem Restrição de Tipo e Grupos

**Feature Branch**: `[005-counterparty-groups]`

**Created**: 2026-07-29

**Status**: Draft

**Input**: User description: "Hoje, só consigo registrar contas do tipo SAÍDA para fornecedores e ENTRADA para unidades. Quero eliminar essa restrição. Agora, quero poder fazer pagamentos a unidades e registrar recebimentos de fornecedores. Além disso, quero uma linha ao final da tabela de contas que seja a somatória total dos dados na coluna VALOR, dinâmica conforme filtros/adições/remoções. O filtro Unidade se torna Unidade ou Fornecedor, e a coluna Contraparte das contas é renomeada para Unidade ou Fornecedor. Unidade passa a ter chave pix; Fornecedor deixa de ter o atributo unidade. Quero um filtro de Fundo na página de Contas. Como Fornecedor e Unidade passam a ter exatamente os mesmos campos, essas duas entidades devem ser unificadas em uma só (nome a propor). Além disso, quero o conceito de Grupos: uma unidade/fornecedor pode ser adicionada a um grupo, e ao lançar uma conta, posso selecionar uma contraparte específica ou um grupo inteiro."

## Clarifications

### Session 2026-07-29

- Q: Ao lançar uma conta, como a usuária deve escolher entre uma Parte específica e um Grupo? → A: Dois modos alternáveis ("Parte específica" vs "Grupo"), cada um com sua própria lista suspensa — reflete a distinção já existente hoje entre lançamento individual e em lote.
- Q: A tela de Contas tem checkboxes de seleção para remoção em lote. Quando algumas linhas estão selecionadas, o total dinâmico deve somar todas as linhas visíveis/filtradas ou só as selecionadas? → A: Sempre soma todas as linhas visíveis/filtradas, independentemente de seleção de checkbox.
- Q: Onde a usuária deve gerenciar quais Partes pertencem a um Grupo? → A: Somente pela tela do Grupo — criar/editar um Grupo inclui adicionar/remover Partes integrantes; o formulário de Parte não lista/edita grupos.
- Q: A entidade unificada (antes chamada "unidade"/"fornecedor" nas telas) deve continuar sendo descrita nesses termos, ou passa a ser tratada só como Parte, sem distinção de papel? → A: Só Parte — uma Parte pode ter tanto contas de entrada quanto de saída, sem qualquer distinção de papel herdado; os rótulos de UI usam apenas "Parte" (não mais "Unidade ou Fornecedor").
- Q: O total dinâmico da tabela de Contas deve ser uma soma aritmética simples ou um valor líquido? → A: Valor líquido — soma das contas do tipo ENTRADA menos a soma das contas do tipo SAÍDA, podendo resultar em valor negativo (mesmo princípio já usado no saldo real de um Fundo).
- Q: Contas, Partes ou vínculos cadastrados antes desta feature precisam ser preservados na migração? → A: Não é necessário preservar; o ambiente é de testes e os dados MAY ser recriados do zero.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lançar entradas e saídas para qualquer Parte (Priority: P1)

Hoje, cada conta só pode ser lançada com um tipo fixo (SAÍDA ou ENTRADA) de acordo com um papel herdado do cadastro da contraparte — o sistema bloqueia qualquer combinação diferente dessa. Como responsável pela administração financeira do condomínio, preciso poder lançar, para qualquer Parte cadastrada, tanto contas do tipo ENTRADA quanto do tipo SAÍDA — uma Parte não deve estar presa a um único tipo de lançamento.

**Why this priority**: É a mudança estrutural mais fundamental — sem ela, os demais itens (grupos, filtro unificado) não têm com o que trabalhar, pois pressupõem uma Parte única capaz de ter qualquer tipo de conta.

**Independent Test**: Pode ser testado lançando, para a mesma Parte, uma conta do tipo SAÍDA e uma conta do tipo ENTRADA, e confirmando que ambas são aceitas e listadas normalmente.

**Acceptance Scenarios**:

1. **Given** uma Parte cadastrada, **When** a usuária lança uma conta do tipo SAÍDA para essa Parte, **Then** a conta é criada com sucesso e aparece na listagem.
2. **Given** a mesma Parte, **When** a usuária lança também uma conta do tipo ENTRADA para ela, **Then** essa conta também é criada com sucesso, sem conflito com a conta de SAÍDA já existente.
3. **Given** uma conta já lançada, **When** a usuária edita valor, vencimento ou Parte da conta (sem alterar o tipo), **Then** a alteração é aceita para qualquer Parte.
4. **Given** uma conta em edição, **When** a usuária tenta alterar o tipo (SAÍDA↔ENTRADA) da conta, **Then** o sistema continua recusando a alteração de tipo, como já ocorre hoje.

---

### User Story 2 - Ver o total líquido da tabela de Contas, atualizado dinamicamente (Priority: P1)

Ao final da tabela de Contas, preciso ver o valor líquido (entradas menos saídas) das contas exibidas, para saber rapidamente o saldo correspondente às contas atualmente visíveis — sem precisar calcular manualmente. Esse total deve refletir sempre a lista efetivamente exibida: se eu aplico um filtro, adiciono ou removo uma conta, o total se atualiza sozinho, podendo inclusive ficar negativo se as saídas superarem as entradas.

**Why this priority**: É uma melhoria de visibilidade explicitamente pedida, independente das demais mudanças, de baixo risco e alto valor imediato (mesmo padrão já validado na tela de Fundos).

**Independent Test**: Pode ser testado abrindo a tela de Contas, conferindo que o total exibido bate com o cálculo manual (soma das entradas menos soma das saídas) das linhas visíveis, aplicando um filtro e conferindo que o total muda de acordo, e criando/removendo uma conta e conferindo que o total é recalculado sem recarregar a página manualmente.

**Acceptance Scenarios**:

1. **Given** a tela de Contas com contas de entrada e de saída cadastradas, **When** a tela é carregada sem filtros, **Then** a linha de total ao final da tabela exibe o valor líquido: soma das contas de ENTRADA menos soma das contas de SAÍDA.
2. **Given** a tela de Contas exibindo um total, **When** a usuária aplica um filtro (tipo, Parte, fundo, status, mês, etc.), **Then** o total é recalculado imediatamente para refletir somente as contas que passam no filtro.
3. **Given** a tela de Contas exibindo um total, **When** a usuária cria, edita o valor ou remove uma conta (individualmente ou em lote), **Then** o total é atualizado automaticamente para refletir a nova lista, sem exigir recarregamento manual da página.
4. **Given** um filtro aplicado que não retorna nenhuma conta, **When** a tabela é exibida vazia, **Then** o total exibido é zero.
5. **Given** algumas linhas marcadas via checkbox de seleção (usado para remoção em lote), **When** a usuária observa o total ao final da tabela, **Then** o total continua refletindo todas as linhas visíveis/filtradas, independentemente de quais estão selecionadas via checkbox.
6. **Given** contas de SAÍDA cujo total supera o total das contas de ENTRADA visíveis, **When** a usuária observa o total, **Then** ele é exibido como um valor negativo.

---

### User Story 3 - Filtrar contas por Fundo (Priority: P2)

Na tela de Contas, preciso filtrar a listagem por Fundo, da mesma forma que já filtro por tipo, Parte, status e período, para conseguir ver rapidamente todos os lançamentos de um fundo específico.

**Why this priority**: Filtro adicional autocontido, de baixo risco, que aumenta o valor da tela sem depender das demais mudanças estruturais.

**Independent Test**: Pode ser testado selecionando um fundo no novo filtro e confirmando que só as contas daquele fundo aparecem na tabela (e no total da User Story 2).

**Acceptance Scenarios**:

1. **Given** contas lançadas em mais de um fundo, **When** a usuária seleciona um fundo específico no novo filtro de Fundo, **Then** a tabela exibe somente as contas daquele fundo.
2. **Given** o filtro de Fundo combinado com outro filtro já existente (ex.: tipo ou status), **When** ambos estão preenchidos, **Then** a tabela exibe somente as contas que atendem aos dois filtros simultaneamente (E lógico).
3. **Given** o filtro de Fundo preenchido, **When** a usuária o limpa (volta para "Todos"), **Then** a tabela volta a exibir contas de todos os fundos, respeitando os demais filtros ativos.

---

### User Story 4 - Ver e filtrar por Parte na tela de Contas (Priority: P2)

Como toda conta agora tem uma única Parte associada, sem papel fixo de origem, preciso que a tela de Contas reflita isso: a coluna hoje chamada "Contraparte" passa a se chamar "Parte", e o filtro hoje chamado "Unidade" também passa a se chamar "Parte", listando em um único lugar todas as Partes cadastradas para eu escolher qualquer uma delas como filtro.

**Why this priority**: Depende da unificação estrutural da User Story 1, mas é a parte voltada à experiência de uso da tela de Contas — pode ser entregue logo depois da liberação do lançamento livre de tipo×Parte.

**Independent Test**: Pode ser testado abrindo o filtro "Parte" na tela de Contas e conferindo que ele lista todas as Partes cadastradas, e que selecionar qualquer uma delas filtra a tabela corretamente pela conta correspondente, com a coluna da tabela também exibindo o rótulo "Parte".

**Acceptance Scenarios**:

1. **Given** Partes cadastradas, **When** a usuária abre o filtro antes chamado "Unidade" na tela de Contas, **Then** ele aparece renomeado para "Parte" e lista, numa única lista suspensa, todas as Partes cadastradas.
2. **Given** o filtro "Parte" preenchido com uma Parte específica, **When** o filtro é aplicado, **Then** a tabela exibe somente as contas lançadas para aquela Parte, independentemente do tipo da conta (SAÍDA ou ENTRADA).
3. **Given** a tabela de Contas, **When** a usuária observa o cabeçalho de colunas, **Then** a coluna antes chamada "Contraparte" aparece com o rótulo "Parte".

---

### User Story 5 - Organizar Partes em grupos e lançar contas em lote para um grupo (Priority: P3)

Preciso poder reunir Partes em Grupos, e, ao lançar uma conta, escolher lançá-la para uma Parte específica ou para todas as integrantes de um grupo selecionado de uma vez — assim eu não preciso repetir manualmente o mesmo lançamento para cada Parte de um conjunto que uso com frequência.

**Why this priority**: Depende da unificação da User Story 1 para fazer sentido (um grupo pode reunir livremente qualquer conjunto de Partes) e é a funcionalidade de maior escopo/complexidade do conjunto — entregue por último, mas mantendo (e generalizando) o valor do lançamento em lote que hoje já existe de forma restrita (sem seleção de subconjunto, fixo a um único papel).

**Independent Test**: Pode ser testado criando um grupo, adicionando duas ou mais Partes a ele, lançando uma conta selecionando esse grupo em vez de uma Parte específica, e conferindo que uma conta é criada para cada integrante do grupo com os mesmos dados (valor, vencimento, fundo, descrição).

**Acceptance Scenarios**:

1. **Given** nenhuma Parte em nenhum grupo, **When** a usuária cria um novo grupo informando um nome, **Then** o grupo é criado e passa a estar disponível para receber integrantes.
2. **Given** um grupo existente, **When** a usuária adiciona duas Partes quaisquer a esse grupo pela tela de edição do próprio grupo, **Then** ambas passam a constar como integrantes do grupo.
3. **Given** um grupo com três integrantes, **When** a usuária alterna o modo de lançamento para "Grupo" e seleciona esse grupo na lista suspensa correspondente (em vez do modo "Parte específica"), **Then** o sistema cria uma conta separada para cada um dos três integrantes, todas com o mesmo tipo, valor, vencimento, fundo, recorrência e descrição informados uma única vez.
4. **Given** uma Parte que pertence a um grupo, **When** essa Parte é removida do grupo (sem ser excluída do cadastro), **Then** ela deixa de ser incluída em lançamentos futuros para aquele grupo, mas contas já lançadas anteriormente para ela permanecem inalteradas.
5. **Given** um grupo sem nenhum integrante, **When** a usuária tenta lançar uma conta em lote para esse grupo, **Then** o sistema informa que o grupo está vazio e não cria nenhuma conta.

---

### Edge Cases

- O que acontece se a usuária tentar excluir uma Parte que já tem contas lançadas? Deve ser bloqueado, preservando a integridade das contas já lançadas.
- O que acontece se a usuária tentar excluir um grupo que ainda tem integrantes? A exclusão do grupo em si não deve afetar contas já lançadas para os antigos integrantes (a conta referencia a Parte diretamente, nunca o grupo).
- O que acontece se duas Partes tiverem o mesmo nome/identificador? Deve continuar valendo uma regra de identificador único, como já ocorre hoje para Unidade.
- Como o total dinâmico da tabela de Contas deve se comportar com contas de tipos diferentes (SAÍDA e ENTRADA) simultaneamente visíveis? É um valor líquido — soma das ENTRADA menos soma das SAÍDA — podendo ser negativo (ver Clarifications).
- Contas, Partes ou vínculos cadastrados antes desta feature precisam continuar acessíveis após a migração? Não há essa garantia — o ambiente é de testes, e os dados MAY ser recriados do zero (ver Assumptions); não é exigida continuidade de histórico anterior a esta feature.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir lançar uma conta de qualquer tipo (SAÍDA ou ENTRADA) para qualquer Parte cadastrada, sem restringir a combinação tipo×Parte.
- **FR-002**: O sistema MUST manter o campo de tipo da conta (SAÍDA/ENTRADA) obrigatório na criação e imutável na edição, como já ocorre hoje.
- **FR-003**: O sistema MUST unificar as entidades hoje separadas de Unidade e Fornecedor em uma única entidade chamada **Parte**, com um único cadastro, uma única tela de listagem e um único formulário, sem distinção de papel entre suas integrantes.
- **FR-004**: A entidade Parte MUST manter um campo de identificação obrigatório e único (equivalente ao "identificador" de Unidade e "nome" de Fornecedor hoje) e um campo opcional de chave pix.
- **FR-005**: O sistema MUST remover o vínculo hoje existente entre Fornecedor e Unidade (campo "unidade" do fornecedor), já que essa distinção deixa de existir com a unificação.
- **FR-006**: O sistema MUST impedir a exclusão de uma Parte que tenha qualquer conta vinculada, independentemente do tipo dessa conta (SAÍDA ou ENTRADA).
- **FR-007**: A tela de Contas MUST exibir, ao final da tabela, uma linha de total com o valor líquido das contas atualmente exibidas (filtradas): soma das contas do tipo ENTRADA menos a soma das contas do tipo SAÍDA — podendo ser negativo — independentemente de quais linhas estejam marcadas pelo checkbox de seleção em lote.
- **FR-008**: O total exibido ao final da tabela de Contas MUST se recalcular automaticamente sempre que a lista de contas exibida mudar — por aplicação/remoção de filtro, ou por criação, edição ou remoção de conta — sem exigir recarregamento manual da página.
- **FR-009**: A tela de Contas MUST oferecer um filtro por Fundo, combinável por E lógico com os demais filtros já existentes (tipo, Parte, status, período, vencidos).
- **FR-010**: O filtro hoje chamado "Unidade" na tela de Contas MUST ser renomeado para "Parte" e passar a listar todas as Partes cadastradas, permitindo filtrar contas de qualquer tipo por qualquer Parte.
- **FR-011**: A coluna hoje chamada "Contraparte" na tabela de Contas MUST ser renomeada para "Parte".
- **FR-012**: O sistema MUST permitir criar, listar, editar e excluir Grupos, cada um identificado por um nome obrigatório e único (mesma regra de unicidade normalizada — sem diferenciar maiúsculas/minúsculas nem espaços nas extremidades — já aplicada à Parte em FR-004).
- **FR-013**: O sistema MUST permitir associar e desassociar qualquer Parte a um ou mais Grupos exclusivamente a partir da tela do Grupo (criação/edição de Grupo); o cadastro da Parte não exibe nem edita a quais Grupos ela pertence.
- **FR-014**: Ao lançar uma conta, o sistema MUST oferecer dois modos alternáveis de lançamento — "Parte específica" ou "Grupo" — cada um com sua própria lista suspensa de opções; quando o modo "Grupo" é escolhido, o sistema MUST criar uma conta individual para cada Parte integrante do grupo selecionado no momento do lançamento, todas com os mesmos dados informados (tipo, valor, vencimento, fundo, recorrência, descrição, observações).
- **FR-015**: O sistema MUST impedir o lançamento em lote para um Grupo sem integrantes, informando à usuária que o grupo está vazio.
- **FR-016**: A exclusão de um Grupo NUNCA MUST afetar contas já lançadas anteriormente para seus integrantes — a conta permanece vinculada diretamente à Parte, não ao grupo.

### Key Entities

- **Parte** (nome em inglês para código: **Party**): Entidade unificada que substitui as atuais Unidade e Fornecedor, sem distinção de papel. Representa qualquer parte com quem uma conta (de entrada ou de saída) pode ser lançada. Atributos: identificador/nome (obrigatório, único), chave pix (opcional). Relaciona-se com Account (como Parte de qualquer tipo de conta) e com Group (associação muitos-para-muitos).
- **Group (Grupo)**: Conjunto nomeado de Partes, usado para lançar contas em lote para todas as suas integrantes de uma vez. Atributos: nome. Relaciona-se com a entidade Parte (associação muitos-para-muitos: uma Parte pode pertencer a mais de um grupo).
- **Account (Conta)** *(existente, modificada)*: Passa a referenciar uma única Parte, em vez das duas referências separadas e mutuamente exclusivas (unidade/fornecedor) que existem hoje. O campo de tipo (SAÍDA/ENTRADA) permanece obrigatório e imutável, mas deixa de restringir qual Parte pode ser associada.
- **Fund (Fundo)** *(existente, sem alteração de estrutura)*: Passa a ser utilizável como critério de filtro na tela de Contas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A usuária consegue lançar tanto uma conta de saída quanto uma conta de entrada para a mesma Parte, sem ser bloqueada pelo sistema — cenário hoje impossível em 100% dos casos.
- **SC-002**: O total exibido ao final da tabela de Contas corresponde exatamente ao valor líquido (entradas menos saídas) das linhas visíveis em qualquer combinação de filtros aplicados, verificado em 100% das interações de filtro, criação, edição e remoção de conta, sem necessidade de recarregar a página.
- **SC-003**: A usuária consegue restringir a listagem de Contas a um único fundo em no máximo uma interação (seleção no filtro).
- **SC-004**: A usuária consegue lançar contas para um conjunto de 5 ou mais Partes em uma única ação, em vez de repetir o lançamento manualmente uma vez por Parte.
- **SC-005**: A usuária realiza cadastro, edição e exclusão de qualquer Parte a partir de uma única tela unificada, sem precisar alternar entre duas telas separadas como antes.

## Assumptions

- **Nome da entidade unificada**: confirmado com a usuária como **Parte** (em português, telas/rótulos) / **Party** (em inglês, código — classe, tabela, pacote), dentre as opções propostas. Para evitar ambiguidade com o uso corriqueiro da palavra portuguesa "parte" (ex.: "essa é a parte mais simples"), esta especificação e os artefatos seguintes MUST usar "Parte" capitalizado quando se referir à entidade de cadastro, e "parte" minúsculo apenas em seu sentido gramatical comum.
- **Dados existentes na migração**: confirmado com a usuária que o ambiente atual é só de teste/desenvolvimento, sem dado real que precise ser preservado — a migração de Unidade/Fornecedor para a entidade unificada Parte MAY recriar as tabelas envolvidas (Unidade, Fornecedor e, por consequência, Conta) do zero, sem preservar histórico entre as tabelas antigas e a nova, seguindo o mesmo precedente já registrado na constitution do projeto (feature 004, conversão de Fundo de enum para entidade, que truncou a tabela `account`). A forma técnica exata dessa migração é decisão da fase de planejamento (`/speckit-plan`), não desta especificação.
- **Total líquido e valores negativos**: o total dinâmico da tabela de Contas (User Story 2) é o valor líquido (soma das contas de ENTRADA menos soma das contas de SAÍDA) das linhas exibidas, podendo ser negativo — mesmo princípio já usado no cálculo de saldo real de um Fundo. Para viabilizar essa exibição, o campo de valor (`amount`) de uma conta MAY passar a aceitar valores negativos internamente, caso a abordagem técnica escolhida no `/speckit-plan` assim exigir; a usuária autorizou explicitamente essa mudança na regra de não-negatividade hoje aplicada a esse campo, se necessária.
- Uma Parte pode pertencer a mais de um Grupo simultaneamente (associação muitos-para-muitos), por ser o modelo mais flexível e não haver indicação de que um grupo por Parte seria suficiente.
- Grupos não têm hierarquia (não existe "grupo de grupos") — cada grupo contém diretamente Partes, sem aninhamento, por não haver necessidade indicada para esse nível de complexidade.
- O lançamento em lote hoje restrito a um único papel fixo (equivalente a "todas as unidades", sem seleção de subconjunto) é substituído/generalizado pelo lançamento por Grupo (User Story 5): se a usuária ainda quiser um atalho equivalente, basta criar um grupo com as Partes desejadas como integrantes. Não é mantido um atalho separado além do mecanismo de Grupos.
