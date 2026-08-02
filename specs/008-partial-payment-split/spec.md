# Feature Specification: Pagamento Parcial de Contas

**Feature Branch**: `[008-partial-payment-split]`

**Created**: 2026-08-02

**Status**: Draft

**Input**: User description: "quero implementar pagamento parcial de contas no projeto. vamos implementar desta forma: o botão de registrar pagamento pode vir com uma caixa de valor pago (= ao total da conta por padrão no front-end), além do input de data que já existe. se a pessoa nao editar o valor pago, a aplicação se comporta do mesmo jeito de hoje. caso contrário, a aplicação duplica aquela conta, edita o valor da conta original para o valor inserido na caixinha de pagamento e marca como pago, e edita o valor da cópia com a diferença para chegar ao total devido. além disso o sistema adiciona à descrição de ambas um sufixo que indique essa operação, para ficar registrado que foi um split. Se o valor pago for maior do que o valor devido na conta, por enquanto pode permitir, alterando o valor da conta para corresponder e adicionando ao campo de observações um registro de que isso aconteceu. A cópia gerada com o valor restante deve herdar a mesma data de vencimento da conta original."

## Clarifications

### Session 2026-08-02

- Q: Quando uma conta que já é resultado de um split anterior (já carrega o sufixo de "pagamento parcial" na descrição) sofre um novo pagamento parcial, o que deve acontecer com o sufixo na descrição das duas contas resultantes desse segundo split? → A: Sufixo com contador — cada split acrescenta uma ocorrência numerada refletindo quantos splits já ocorreram naquela linhagem, em vez de duplicar o sufixo ou deixar apenas uma marca fixa.
- Q (refinamento, mesma sessão): a numeração deveria começar já na primeira divisão (em vez de deixá-la sem número), rotulando literalmente "- parte 1" / "- parte 2"? → A: Sim — toda divisão, inclusive a primeira, rotula as duas contas com o sufixo "- parte N"; a conta que está sendo paga só ganha um número novo se ainda não tiver um (nesse caso "- parte 1"), caso já tenha um número (por já ser resultado de um split anterior) ele permanece inalterado; a nova conta com o saldo restante sempre recebe o número seguinte.
- Q (revisão durante o planejamento, mesma data): em vez de só desmarcar a recorrência nas contas resultantes de um split, o campo "Recorrente" deveria ser removido por completo do sistema nesta feature, já que a próxima feature planejada (execução agendada/cron) não vai reaproveitá-lo? → A: Sim — remoção completa (formulário, persistência, API), não apenas desmarcação (FR-010).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar pagamento integral, sem alterar o valor pago (Priority: P1)

Ao registrar o pagamento de uma conta pendente, a usuária vê uma caixa de valor pago já preenchida com o valor total devido. Se ela confirmar o pagamento sem alterar esse valor, a conta é marcada como paga exatamente como acontece hoje — sem qualquer divisão de conta.

**Why this priority**: É o caminho mais comum (pagamento integral) e precisa continuar funcionando sem regressão, já que é a base de todo o fluxo de contas existente.

**Independent Test**: Pode ser testado registrando o pagamento de uma conta pendente sem tocar na caixa de valor pago e verificando que a conta resultante tem o mesmo valor, passa a constar como paga na data informada, e nenhuma outra conta é criada.

**Acceptance Scenarios**:

1. **Given** uma conta pendente com valor devido de R$500,00, **When** a usuária registra o pagamento informando a data e mantendo a caixa de valor pago em R$500,00, **Then** a conta passa a constar como paga naquela data, com valor inalterado, e nenhuma conta nova é criada.
2. **Given** uma conta pendente, **When** a usuária abre o registro de pagamento, **Then** a caixa de valor pago já aparece preenchida com o valor total devido da conta.

---

### User Story 2 - Registrar pagamento parcial (valor pago menor que o devido) (Priority: P1)

Ao registrar o pagamento de uma conta pendente, a usuária edita a caixa de valor pago para um valor menor que o total devido. O sistema divide a conta em duas: a conta original passa a valer o valor efetivamente pago e é marcada como paga; uma nova conta é criada com o valor restante, mantendo a mesma data de vencimento, e permanece pendente. Ambas as descrições recebem um sufixo "- parte N" indicando que a conta foi dividida por um pagamento parcial, numerado sequencialmente dentro daquela linhagem de splits.

**Why this priority**: É a funcionalidade central pedida — sem ela, a feature não existe. Tem o mesmo peso do fluxo integral porque ambos precisam coexistir corretamente na mesma tela.

**Independent Test**: Pode ser testado registrando o pagamento de uma conta pendente com um valor pago menor que o devido e verificando que passam a existir duas contas: uma paga com o valor pago informado, e outra pendente com a diferença, ambas com a mesma data de vencimento e com a descrição indicando o split.

**Acceptance Scenarios**:

1. **Given** uma conta pendente "Taxa condominial" com valor devido de R$100,00 e vencimento em 10/07/2026, **When** em 15/07/2026 a usuária registra o pagamento informando R$70,00 na caixa de valor pago, **Then** a conta original passa a se chamar "Taxa condominial - parte 1", com valor R$70,00, marcada como paga em 15/07/2026.
2. **Given** o mesmo cenário do item anterior, **When** o pagamento parcial é confirmado, **Then** uma nova conta é criada chamada "Taxa condominial - parte 2", com valor R$30,00, vencimento em 10/07/2026 (igual ao da conta original), sem data de pagamento, com o mesmo fundo e a mesma contraparte da conta original.
3. **Given** a conta "Taxa condominial - parte 2" (valor restante R$30,00, ainda pendente), **When** em 20/07/2026 a usuária registra sobre ela um novo pagamento parcial de R$20,00, **Then** essa conta mantém sua descrição "Taxa condominial - parte 2" inalterada (apenas passa a valer R$20,00 e marcada como paga em 20/07/2026), e uma nova conta "Taxa condominial - parte 3" é criada com o valor restante de R$10,00, mesma data de vencimento (10/07/2026), pendente — o número da parte nunca é duplicado nem reatribuído a uma conta que já o possui.

---

### User Story 3 - Pagamento maior que o valor devido (Priority: P3)

Ao registrar o pagamento de uma conta pendente, a usuária edita a caixa de valor pago para um valor maior que o total devido. O sistema não cria conta nova: ajusta o valor da própria conta para o valor pago, marca como paga, e registra no campo de observações que houve pagamento a maior, sem apagar qualquer observação já existente.

**Why this priority**: Cenário reconhecidamente raro pela própria usuária ("provavelmente nunca vai acontecer"), mas precisa de um comportamento definido para não deixar a aplicação em estado inconsistente caso ocorra.

**Independent Test**: Pode ser testado registrando o pagamento de uma conta pendente com um valor pago maior que o devido e verificando que a conta é atualizada para esse valor, marcada como paga, e o campo de observações contém o registro do valor pago a maior.

**Acceptance Scenarios**:

1. **Given** uma conta pendente com valor devido de R$500,00 e sem observações registradas, **When** a usuária registra o pagamento informando R$501,00, **Then** a conta passa a ter valor R$501,00, é marcada como paga, e o campo de observações passa a conter um registro equivalente a "pago R$1,00 a mais".
2. **Given** uma conta pendente com valor devido de R$500,00 e observações já preenchidas com um texto qualquer, **When** a usuária registra um pagamento de R$550,00, **Then** o texto de observações já existente é preservado e o registro do pagamento a maior é adicionado a ele, sem sobrescrevê-lo.

---

### Edge Cases

- Valor pago informado igual a zero: a confirmação MUST ser ignorada — nenhum pagamento é registrado, nenhuma divisão ocorre, e a conta permanece pendente exatamente como estava antes da tentativa.
- Alterar a data de pagamento de uma conta que já está paga ("Alterar pagamento", diferente de "Registrar pagamento" de uma conta pendente): esse fluxo não é afetado por esta feature — continua editando apenas a data, sem caixa de valor pago nem qualquer divisão.
- Valor pago inválido (negativo ou não numérico): a confirmação deve ser bloqueada, reaproveitando a mesma validação de valor não-negativo já aplicada a contas hoje.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Ao iniciar o registro de pagamento de uma conta pendente, o sistema MUST exibir um campo de valor pago, editável, pré-preenchido com o valor total devido da conta, junto ao campo de data de pagamento já existente.
- **FR-002**: Se o valor pago confirmado for igual ao valor devido da conta, o sistema MUST registrar o pagamento da mesma forma que hoje — apenas definindo a data de pagamento, sem alterar valor, descrição, observações, nem criar qualquer conta nova.
- **FR-003**: Se o valor pago confirmado for menor que o valor devido da conta, o sistema MUST: (a) se a descrição da conta sendo paga ainda não tiver o sufixo "- parte N", acrescentar "- parte 1" a ela; (b) atualizar o valor dessa conta para o valor pago e definir sua data de pagamento; (c) criar uma nova conta com o valor restante (valor devido menos valor pago), sem data de pagamento, com a mesma descrição-base acrescida do sufixo "- parte {N+1}" — o número imediatamente seguinte ao da conta que está sendo paga —, o mesmo fundo e a mesma contraparte da conta original.
- **FR-003a**: Se a conta sendo paga já tiver o sufixo "- parte N" (por já ser resultado de um split anterior), esse sufixo MUST permanecer inalterado nela — o sistema nunca renumera nem duplica o número de uma parte já existente; apenas a nova conta criada para o valor restante recebe o sufixo com o número seguinte (N+1).
- **FR-004**: A nova conta criada com o valor restante MUST usar a mesma data de vencimento da conta original — nunca uma data futura recalculada.
- **FR-005**: Se o valor pago confirmado for maior que o valor devido da conta, o sistema MUST atualizar o valor da conta para o valor pago, registrar o pagamento normalmente (definindo a data de pagamento), e acrescentar ao campo de observações da conta um registro do valor pago a maior (ex.: "pago R$1,00 a mais"), preservando qualquer conteúdo já existente nesse campo. Nenhuma conta nova é criada nesse caso.
- **FR-006**: O campo de valor pago descrito em FR-001 MUST aparecer apenas no fluxo de registro de pagamento de uma conta ainda pendente; a edição da data de pagamento de uma conta já paga MUST continuar se comportando exatamente como hoje, sem esse campo e sem qualquer divisão.
- **FR-007**: As contas resultantes de uma divisão por pagamento parcial (a original, com valor truncado, e a nova, com o valor restante) MUST permanecer totalmente editáveis, pagáveis e removíveis como qualquer outra conta, inclusive permitindo que a conta com o valor restante receba, por sua vez, um novo pagamento parcial.
- **FR-008**: O sistema MUST rejeitar um valor pago negativo, reaproveitando a mesma validação de valor não-negativo já aplicada às contas.
- **FR-009**: O sistema MUST ignorar a confirmação de um pagamento cujo valor pago seja igual a zero — sem registrar pagamento, sem criar conta nova, e sem alterar a conta pendente original.
- **FR-010**: O sistema MUST deixar de oferecer a marcação "Recorrente" em qualquer tela ou contrato de dados relacionado a contas (cadastro, edição, listagem e respostas de API) — o campo é removido por completo, não apenas desmarcado (ver Assumptions para o motivo).

### Key Entities

- **Account (existente)**: nenhuma entidade nova é introduzida por esta feature. Uma divisão por pagamento parcial produz dois registros de `Account` independentes (a conta original, truncada, e uma nova conta com o saldo restante), sem vínculo estrutural entre elas além do sufixo compartilhado na descrição — cada uma segue seu próprio ciclo de vida (edição, pagamento, remoção) dali em diante. Esta feature também remove o atributo "recorrente" da entidade (FR-010) — deixa de existir tanto na conta original quanto em qualquer conta nova criada por ela.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uma usuária consegue registrar um pagamento parcial e ver imediatamente, na mesma listagem, tanto a parte paga quanto o saldo restante como dois itens distintos, sem nenhum passo manual de recálculo.
- **SC-002**: 100% dos pagamentos registrados com o valor pago igual ao valor devido continuam se comportando exatamente como antes da feature (nenhuma regressão no fluxo de pagamento integral).
- **SC-003**: A partir da descrição de uma conta, uma usuária consegue identificar, sem precisar consultar outra tela, que aquele registro é resultado de uma divisão por pagamento parcial.
- **SC-004**: Um pagamento a maior é totalmente registrado (valor ajustado e observação explicativa) sem exigir nenhuma correção manual adicional fora do próprio campo de observações.

## Assumptions

- A funcionalidade se aplica tanto a contas a pagar quanto a contas a receber, já que ambas compartilham a mesma entidade `Account` e a mesma tela de listagem/registro de pagamento.
- A decisão entre "pagamento integral" e "pagamento parcial" é feita comparando o valor pago confirmado com o valor devido da conta no momento da confirmação — não se o campo foi tecnicamente tocado pela usuária; ou seja, se a usuária editar o campo e depois devolvê-lo ao valor original, o comportamento é o mesmo do pagamento integral.
- O formato do sufixo de descrição é literal: "{descrição} - parte N", com N numerado sequencialmente dentro da linhagem de splits daquela conta (ver FR-003/FR-003a). Não é apenas uma marca reconhecível genérica — a numeração em si é parte do requisito.
- Não há, por ora, um mecanismo de "desfazer" um split além das ações já existentes de editar/remover cada conta resultante manualmente.
- Pagamentos parciais sucessivos sobre o saldo restante de um split anterior são suportados naturalmente, repetindo o mesmo fluxo sobre a conta pendente resultante — sem necessidade de um relacionamento explícito entre as contas geradas.
- Decisão explícita da usuária (revisão durante o planejamento, 2026-08-02): em vez de apenas desmarcar a recorrência nas contas resultantes de um split (decisão inicial desta feature, já superada), o campo "Recorrente" é removido por completo da aplicação nesta mesma feature (FR-010) — formulário, persistência e contratos de API. Motivo: o campo não decide nenhum comportamento automático hoje, e a próxima feature planejada pela usuária é justamente a automação real de lançamentos recorrentes (execução agendada/cron), que será desenhada do zero sem reaproveitar esse campo — mantê-lo até lá só adicionaria uma marcação inerte que o split precisaria tratar como caso especial (ver histórico de FR-003 nesta mesma feature). Essa remoção simplifica FR-003, que deixa de precisar mencionar recorrência.
