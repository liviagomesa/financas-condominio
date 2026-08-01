# Feature Specification: Edição inline de valor e seleção em intervalo com Shift

**Feature Branch**: `006-inline-edit-shift-select`

**Created**: 2026-08-01

**Status**: Draft

**Input**: User description: "opção de editar o campo 'valor' inline na tabela + opção de select segurando o shift (ao segurar shift e clicar numa caixinha de seleção, todas as caixinhas entre ela e a última selecionada serão selecionadas) (obs.: como todas as listagens do sistema têm caixas de seleção, creio que isso tenha que ser editado num componente compartilhado entre elas, que faz essa funcionalidade das caixinhas)"

## Clarifications

### Session 2026-08-01

- Q: Depois de um Shift+clique, a âncora usada para calcular o próximo Shift+clique deve ficar fixa no último clique normal (sem Shift), ou deve se mover para a linha do Shift+clique mais recente? → A: Âncora fixa no último clique normal — Shift+cliques sucessivos nunca movem essa referência, mesmo em sequência.
- Q: Quantas linhas podem estar com o campo "Valor" em edição inline simultaneamente na listagem de contas? → A: Apenas uma por vez — iniciar uma edição inline em qualquer linha cancela, sem salvar, qualquer outra edição inline em andamento (de valor ou de registro de pagamento) na listagem.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Editar o valor de uma conta direto na listagem (Priority: P1)

Como usuária que lança e corrige contas a pagar/receber com frequência, quero clicar no valor de uma conta diretamente na listagem e digitar o novo valor, sem precisar abrir a tela de edição completa, para corrigir valores rapidamente durante a conferência mensal.

**Why this priority**: É o ganho de produtividade mais direto pedido: hoje corrigir um valor exige abrir a tela de edição, alterar o campo e salvar, para depois voltar à listagem — um ciclo repetido toda vez que se percebe um valor errado ao revisar a lista.

**Independent Test**: Pode ser testado abrindo a listagem de contas, clicando no valor de uma linha, digitando um novo valor e confirmando — a listagem (incluindo o total exibido no rodapé) deve refletir o novo valor sem navegação para outra tela.

**Acceptance Scenarios**:

1. **Given** a listagem de contas exibindo uma conta com valor 100.00, **When** a usuária clica no campo "Valor" dessa conta, digita "150.00" e confirma (Enter ou clicando fora do campo), **Then** o valor da conta é atualizado para 150.00 na listagem e o total exibido no rodapé é recalculado, sem sair da tela de listagem.
2. **Given** o campo "Valor" de uma conta em modo de edição inline, **When** a usuária pressiona Esc, **Then** a edição é cancelada, o valor digitado é descartado e o valor original volta a ser exibido.
3. **Given** o campo "Valor" de uma conta em modo de edição inline, **When** a usuária confirma um valor negativo ou vazio, **Then** o sistema exibe uma mensagem de erro em português, mantém o campo em modo de edição e não persiste o valor inválido.
4. **Given** o campo "Valor" de uma conta em modo de edição inline, **When** a usuária clica no campo "Valor" de outra linha antes de confirmar a primeira, **Then** a primeira edição é cancelada sem salvar e a segunda linha entra em modo de edição.

---

### User Story 2 - Selecionar um intervalo de linhas segurando Shift (Priority: P2)

Como usuária que remove ou aplica ações em lote sobre várias linhas de uma listagem (contas, partes, fundos ou grupos), quero marcar a primeira e a última caixinha de um intervalo segurando Shift e ter todas as linhas entre elas marcadas automaticamente, para não precisar clicar em cada caixinha individualmente quando preciso selecionar muitas linhas seguidas.

**Why this priority**: Complementa a seleção em lote já existente no sistema, tornando-a utilizável em listas maiores; tem valor mesmo sem a User Story 1, mas depende de uma funcionalidade (seleção múltipla) que já existe hoje, então o ganho é incremental sobre algo já funcional.

**Independent Test**: Pode ser testado em qualquer listagem com caixas de seleção (por exemplo, partes): marcar a caixinha da 2ª linha, depois segurar Shift e marcar a caixinha da 6ª linha — as linhas 2 a 6 devem ficar marcadas.

**Acceptance Scenarios**:

1. **Given** uma listagem com 10 linhas e nenhuma seleção ativa, **When** a usuária clica na caixinha da linha 2 (clique normal) e depois, segurando Shift, clica na caixinha da linha 6, **Then** as linhas 2, 3, 4, 5 e 6 ficam todas marcadas.
2. **Given** uma listagem com as linhas 2 a 6 já marcadas por seleção em intervalo, **When** a usuária, segurando Shift, clica na caixinha de uma linha anterior à última clicada (ex.: linha 1), **Then** o novo intervalo (linha 1 até a última clicada normalmente) é marcado, preservando marcações já existentes fora do intervalo.
3. **Given** uma listagem sem nenhuma linha clicada individualmente ainda nesta sessão de uso da tela, **When** a usuária segura Shift e clica em uma caixinha, **Then** apenas aquela linha é marcada, como um clique normal (sem erro).
4. **Given** uma listagem com algumas linhas marcadas via seleção em intervalo, **When** a usuária clica na caixinha de "selecionar todas" no cabeçalho da tabela, **Then** a marcação de referência para um próximo Shift+clique é reiniciada (o próximo Shift+clique passa a formar intervalo a partir da linha clicada logo depois).
5. **Given** a funcionalidade de seleção em intervalo, **When** aplicada em qualquer uma das listagens do sistema que possuem caixas de seleção (contas, partes, fundos, grupos), **Then** o comportamento de Shift+clique é o mesmo em todas elas.

---

### Edge Cases

- O que acontece se a usuária mudar um filtro da listagem (ex.: filtro de parte ou de mês) enquanto uma edição inline de valor está aberta? A edição em andamento é cancelada sem salvar, já que a lista de linhas exibida muda.
- O que acontece se a usuária mudar um filtro da listagem enquanto há linhas selecionadas via seleção em intervalo? A seleção inteira é limpa (comportamento já existente hoje ao mudar filtros), e a referência para um próximo Shift+clique também é reiniciada.
- O que acontece se a usuária digitar um valor com formato inválido (texto não numérico) no campo de edição inline? O sistema trata como valor inválido e exibe a mesma mensagem de erro do caso de valor negativo/vazio, mantendo o campo em edição.
- O que acontece se a usuária tentar editar o valor de uma conta enquanto está no meio de registrar um pagamento para a mesma linha (fluxo já existente de "Registrar pagamento")? Apenas uma dessas duas edições inline pode estar ativa por vez nessa linha; iniciar uma cancela a outra sem salvar.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir que a usuária clique no campo "Valor" de uma linha na listagem de contas para transformá-lo em um campo editável, sem sair da tela de listagem.
- **FR-002**: O sistema MUST salvar o novo valor quando a usuária confirmar a edição (pressionando Enter ou movendo o foco para fora do campo), aplicando as mesmas regras de validação já usadas na tela de edição completa de conta (valor obrigatório, numérico, não negativo).
- **FR-003**: O sistema MUST permitir cancelar uma edição inline em andamento (pressionando Esc), descartando o valor digitado e restaurando o valor exibido anteriormente.
- **FR-004**: Se o valor confirmado falhar na validação, o sistema MUST manter o campo em modo de edição, exibir uma mensagem de erro em português e MUST NOT persistir o valor inválido.
- **FR-005**: O sistema MUST permitir apenas um campo "Valor" em edição inline por vez na listagem de contas; iniciar uma nova edição inline (em outra linha, ou o fluxo de registrar pagamento na mesma linha) MUST cancelar qualquer edição inline em andamento sem salvá-la.
- **FR-006**: Ao salvar uma edição inline de valor com sucesso, o sistema MUST atualizar imediatamente o valor exibido na linha e quaisquer totais derivados dele (ex.: total líquido exibido no rodapé da listagem), sem exigir recarregar a página.
- **FR-007**: A edição inline de valor MUST respeitar as mesmas restrições já aplicadas à edição completa de uma conta (ex.: o tipo da conta continua imutável e não é afetado pela edição inline).
- **FR-008**: Em qualquer listagem do sistema que exiba caixas de seleção por linha (contas, partes, fundos, grupos), um clique normal em uma caixinha MUST continuar marcando/desmarcando apenas aquela linha, como hoje.
- **FR-009**: Em qualquer listagem do sistema que exiba caixas de seleção por linha, um clique em uma caixinha enquanto a tecla Shift está pressionada MUST marcar todas as linhas entre essa linha e a última linha clicada com um clique normal, sem Shift (a "âncora"), inclusive ambas, considerando a ordem em que as linhas estão exibidas no momento.
- **FR-009a**: A âncora usada para calcular o intervalo MUST permanecer fixa no último clique normal; Shift+cliques sucessivos MUST NOT mover a âncora, mesmo quando aplicados em sequência — cada novo Shift+clique recalcula o intervalo a partir do mesmo ponto de partida até que um novo clique normal seja feito.
- **FR-010**: A seleção em intervalo (Shift+clique) MUST marcar todas as linhas do intervalo, independentemente do estado anterior de cada uma, e MUST NOT desmarcar linhas fora do intervalo que já estivessem marcadas.
- **FR-011**: Um Shift+clique realizado quando ainda não há nenhuma linha clicada individualmente na listagem atual MUST se comportar como um clique normal (marca apenas a linha clicada), sem gerar erro.
- **FR-012**: Marcar a caixinha de "selecionar todas" no cabeçalho de uma listagem MUST reiniciar a referência usada para seleção em intervalo, de forma que o próximo Shift+clique forme um novo intervalo a partir da linha clicada em seguida.
- **FR-013**: Qualquer ação que já limpe a seleção de uma listagem hoje (ex.: mudança de filtro) MUST também reiniciar a referência usada para seleção em intervalo.
- **FR-014**: O comportamento de seleção em intervalo (FR-008 a FR-013) MUST ser consistente entre todas as listagens do sistema que possuem caixas de seleção, de forma que uma futura listagem nova com seleção múltipla herde o mesmo comportamento sem reimplementá-lo.

### Key Entities

Esta feature não introduz nem altera entidades de dados — opera sobre a apresentação e edição de dados já existentes (valor de uma conta) e sobre o estado de seleção já existente nas listagens.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A usuária consegue corrigir o valor de uma conta a partir da listagem em no máximo 2 interações (clicar no campo, digitar e confirmar), contra pelo menos 4 hoje (abrir edição, alterar campo, salvar, voltar à listagem).
- **SC-002**: A usuária consegue selecionar um intervalo contíguo de 10 linhas em 2 cliques (primeira caixinha + Shift+clique na última), contra 10 cliques individuais hoje.
- **SC-003**: 100% das listagens do sistema que hoje oferecem seleção múltipla por caixinha (contas, partes, fundos, grupos) suportam seleção em intervalo com Shift+clique de forma consistente.
- **SC-004**: Uma tentativa de edição inline com valor inválido é rejeitada com mensagem clara, sem perda de nenhum outro dado já preenchido na tela.

## Assumptions

- A edição inline de valor se aplica apenas à listagem de contas (`/accounts`), único local do sistema hoje com um campo monetário "Valor" em tabela; as demais listagens (partes, fundos, grupos) não têm campo equivalente e não são afetadas pela User Story 1.
- O clique simples sobre o texto do valor é o gatilho para entrar em modo de edição inline (sem exigir duplo clique ou ícone dedicado), por ser o padrão mais comum em tabelas com edição inline.
- Apenas uma edição inline de valor pode estar ativa por vez em toda a listagem de contas — mesma restrição já aplicada hoje ao fluxo existente de registrar pagamento inline, evitando múltiplas edições simultâneas não salvas.
- A seleção em intervalo com Shift considera a ordem visual atual das linhas na listagem (após filtros/ordenação aplicados), não uma ordem de cadastro subjacente.
- Não há paginação nas listagens afetadas hoje; caso paginação seja introduzida no futuro, o comportamento de seleção em intervalo através de páginas fica fora do escopo desta feature.