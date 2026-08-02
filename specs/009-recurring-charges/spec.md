# Feature Specification: Geração Automática de Contas Recorrentes

**Feature Branch**: `[009-recurring-charges]`

**Created**: 2026-08-02

**Status**: Draft

**Input**: User description: "Geração automática de contas recorrentes via cron mensal. Nova entidade \"cobrança/pagamento recorrente\" (molde), separada de `Account`: cada linha representa uma cobrança ou pagamento fixo e recorrente de uma contraparte específica — uma linha por contraparte (`Party`), não um \"plano\" compartilhado por várias contrapartes ao mesmo tempo. Campos: tipo (mesmo discriminador receber/pagar de `Account`, obrigatório na criação e imutável na edição), valor fixo, dia do mês de vencimento (ajustado para o último dia do mês em meses mais curtos), descrição, fundo, contraparte (`Party`), observações opcionais. Ao criar, deve ser possível escolher entre lançar para parte específica ou para grupo. A geração mensal precisa ser idempotente — a conta gerada guarda uma referência de volta para o molde que a originou. Editar o valor de uma cobrança recorrente não deve alterar contas já geradas no passado — a edição substitui a linha atual por uma nova versão, sem mecanismo de encadeamento entre versões. Remover uma cobrança recorrente é soft delete — o registro deixa de aparecer nas listagens, mas a FK não quebra. O job de geração roda automaticamente todo dia 25 de cada mês, às 6h da manhã (horário de Brasília), gerando as contas do mês seguinte a partir de todas as cobranças recorrentes ativas no momento da execução. Precisa haver também uma tela de gerenciamento (listar, criar, editar, remover), reaproveitando os padrões de UI já estabelecidos. Existem casos de cobrança recorrente sem valor fixo — nesses casos a usuária cria a recorrência com valor R$0,00 e ajusta os valores da instância depois manualmente."

## Clarifications

### Session 2026-08-02

- Q: Dado que a aplicação vai ser hospedada num PaaS (potencialmente sujeito a cold start/ociosidade) em vez de ficar sempre em execução, se o processo agendado não estiver rodando exatamente às 6h do dia 25, o ciclo mensal deveria ter algum mecanismo de recuperação? → A: Sim — recuperação na inicialização: além do agendamento fixo, toda vez que o app inicia o sistema verifica se o ciclo atual já deveria ter gerado contas e ainda não gerou, e roda a geração imediatamente nesse caso.
- Q: Se a geração falhar para uma cobrança recorrente específica dentro do lote mensal, isso deve impedir a geração das demais cobranças daquele ciclo? → A: Não — isolado por cobrança: a geração de cada cobrança recorrente é independente; se uma falhar, as demais do mesmo ciclo são geradas normalmente.
- Q: Uma falha de geração deve ficar visível de alguma forma para a usuária, ou fica oculta? → A: Sim — indicador na tela de gerenciamento: cada cobrança recorrente exibe um aviso visível quando a última tentativa de geração falhou para ela, sem precisar de nenhum canal externo (e-mail, push).
- Q (refinamento, mesma sessão): O aviso de falha some assim que uma data fixa do calendário chega (ex.: dia 25 do mês seguinte), ou assim que uma nova tentativa efetivamente conseguir gerar a conta pendente? → A: Assim que uma nova tentativa conseguir — o aviso é por par (cobrança, mês/ano-alvo pendente), não por ciclo; qualquer execução seguinte (agendamento do próximo dia 25 ou recuperação na inicialização) que gere com sucesso a conta faltante já limpa o aviso, mesmo que aconteça antes do próximo dia 25. Se nenhuma tentativa nova ocorrer para aquele mês-alvo específico antes que o ciclo seguinte avance o alvo para o mês posterior, aquele mês fica definitivamente sem geração automática (lançamento manual necessário) e o aviso permanece como sinalização.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar uma cobrança/pagamento recorrente (Priority: P1)

A usuária cadastra uma cobrança ou pagamento recorrente informando tipo, valor fixo, dia do mês de vencimento, descrição, fundo, contraparte e observações opcionais — podendo escolher, no momento do cadastro, entre uma contraparte específica ou um grupo inteiro (nesse caso, uma linha independente é criada para cada integrante do grupo).

**Why this priority**: Sem conseguir cadastrar o molde, nenhuma das demais funcionalidades desta feature existe.

**Independent Test**: Pode ser testado cadastrando uma cobrança recorrente para uma contraparte específica e verificando que a linha é criada com os dados informados; e cadastrando uma para um grupo com múltiplos integrantes, verificando que uma linha é criada por integrante.

**Acceptance Scenarios**:

1. **Given** a tela de cadastro de cobrança recorrente, **When** a usuária preenche tipo "receber", valor R$350,00, dia de vencimento 10, descrição "Taxa condominial", fundo e contraparte específica, e confirma, **Then** uma cobrança recorrente ativa é criada com esses dados.
2. **Given** um grupo com 5 integrantes, **When** a usuária cadastra uma cobrança recorrente escolhendo esse grupo em vez de uma contraparte específica, **Then** 5 linhas de cobrança recorrente são criadas, uma por integrante, cada uma editável e removível de forma independente das demais.
3. **Given** a tela de cadastro, **When** a usuária deixa o valor como R$0,00, **Then** a cobrança recorrente é criada normalmente, sem exigir um valor fixo maior que zero.
4. **Given** um grupo sem nenhum integrante, **When** a usuária tenta cadastrar uma cobrança recorrente para esse grupo, **Then** o sistema não cria nenhuma linha e informa que o grupo está vazio.

---

### User Story 2 - Geração automática mensal das contas (Priority: P1)

Todo dia 25 de cada mês, às 6h no horário de Brasília, o sistema gera automaticamente, para cada cobrança recorrente ativa naquele momento, uma conta com vencimento no mês seguinte — sem qualquer ação manual da usuária.

**Why this priority**: É o valor central da feature — sem a geração automática, cadastrar cobranças recorrentes não pouparia nenhum trabalho manual.

**Independent Test**: Pode ser testado cadastrando cobranças recorrentes ativas, disparando o processo de geração e verificando que uma conta correspondente a cada uma foi criada com vencimento no mês seguinte, referenciando o molde de origem.

**Acceptance Scenarios**:

1. **Given** três cobranças recorrentes ativas em julho, **When** o processo de geração roda no dia 25 de julho às 6h de Brasília, **Then** três contas são criadas, cada uma com vencimento em agosto, cada uma referenciando a cobrança recorrente que a originou.
2. **Given** o processo de geração já rodou no dia 25 e gerou a conta de agosto para uma cobrança recorrente, **When** o processo roda novamente no mesmo ciclo (ex.: após um redeploy), **Then** nenhuma conta duplicada é criada para essa cobrança e mês-alvo.
3. **Given** uma cobrança recorrente com dia de vencimento configurado como 31, **When** o mês-alvo da geração tem apenas 30 dias (ou é fevereiro), **Then** a conta gerada tem vencimento no último dia daquele mês.
4. **Given** nenhuma cobrança recorrente ativa no momento da execução, **When** o processo de geração roda no dia 25, **Then** nenhuma conta é criada e o processo conclui sem erro.
5. **Given** três cobranças recorrentes ativas, sendo que a geração de uma delas falha por algum motivo, **When** o processo de geração roda, **Then** as outras duas têm suas contas geradas normalmente, e apenas a cobrança que falhou fica sem conta gerada naquele ciclo.

---

### User Story 3 - Reajustar uma cobrança recorrente sem afetar o histórico (Priority: P2)

A usuária edita uma cobrança recorrente (tipicamente o valor, no reajuste anual) e as contas já geradas anteriormente a partir dela permanecem exatamente como estavam — a mudança só passa a valer para as próximas gerações mensais.

**Why this priority**: Sustenta o caso de uso citado no README do produto (reajuste ~anual) sem quebrar o histórico financeiro já lançado; depende das User Stories 1 e 2 já existirem para fazer sentido.

**Independent Test**: Pode ser testado editando o valor de uma cobrança recorrente que já gerou ao menos uma conta no passado, e verificando que essa conta antiga mantém o valor original, enquanto a próxima geração usa o novo valor.

**Acceptance Scenarios**:

1. **Given** uma cobrança recorrente de R$300,00 que já gerou uma conta em julho, **When** a usuária edita o valor para R$330,00 em agosto, **Then** a conta gerada em julho continua com R$300,00, e a cobrança recorrente ativa passa a valer R$330,00.
2. **Given** o cenário anterior, **When** o processo de geração mensal roda novamente, **Then** a nova conta gerada usa o valor R$330,00.
3. **Given** uma cobrança recorrente editada, **When** a usuária consulta a listagem de cobranças recorrentes, **Then** apenas a versão vigente (com o valor novo) aparece na lista — a versão anterior não aparece, mas continua sendo referenciada pelas contas geradas antes da edição.

---

### User Story 4 - Remover uma cobrança recorrente sem perder histórico (Priority: P2)

A usuária remove uma cobrança recorrente (ex.: condômino que saiu) e o sistema para de gerar novas contas a partir dela dali em diante, sem apagar as contas já geradas nem a própria linha.

**Why this priority**: Evita que a única forma de "parar" uma cobrança recorrente seja deixar de pagar/cobrar manualmente cada mês; tem prioridade um pouco menor que editar porque afeta um caminho mais raro (saída de condômino) que ajuste de valor.

**Independent Test**: Pode ser testado removendo uma cobrança recorrente que já gerou contas no passado, e verificando que ela some da listagem, que as contas antigas continuam existindo e acessíveis, e que o próximo ciclo de geração não cria mais nenhuma conta a partir dela.

**Acceptance Scenarios**:

1. **Given** uma cobrança recorrente ativa que já gerou contas em meses anteriores, **When** a usuária a remove, **Then** ela deixa de aparecer na listagem de cobranças recorrentes, mas as contas já geradas continuam existindo e acessíveis normalmente.
2. **Given** uma cobrança recorrente removida, **When** o processo de geração mensal roda novamente, **Then** nenhuma conta nova é criada a partir dela.

---

### User Story 5 - Gerenciar cobranças recorrentes numa tela dedicada (Priority: P3)

A usuária acessa uma tela dedicada para listar todas as cobranças recorrentes ativas, com as mesmas ações de linha (editar, remover) e seleção múltipla já disponíveis nas demais listagens do sistema, e consegue identificar ali mesmo quando a geração mais recente de alguma cobrança falhou.

**Why this priority**: Cadastro, edição e remoção (User Stories 1, 3 e 4) já cobrem a funcionalidade essencial; esta história garante que a experiência de uso siga o padrão visual e de interação já estabelecido nas outras telas, além de dar visibilidade a falhas de geração, o que agrega consistência e confiança mas não é bloqueante para o valor central da feature.

**Independent Test**: Pode ser testado abrindo a tela de cobranças recorrentes, verificando que ela lista as cobranças ativas com ações de editar/remover por linha, que seleção múltipla com remoção em lote funciona da mesma forma que nas demais listagens do sistema, e que uma cobrança cuja última geração falhou aparece com um aviso visível enquanto as demais não exibem nada extra.

**Acceptance Scenarios**:

1. **Given** várias cobranças recorrentes ativas cadastradas, **When** a usuária abre a tela de gerenciamento, **Then** todas aparecem listadas com tipo, valor, dia de vencimento, descrição, fundo e contraparte visíveis.
2. **Given** a listagem de cobranças recorrentes, **When** a usuária seleciona várias linhas e aciona a remoção em lote, **Then** todas as selecionadas são removidas (soft delete), da mesma forma que a remoção individual.
3. **Given** uma cobrança recorrente cuja tentativa de geração mais recente falhou, **When** a usuária abre a tela de gerenciamento, **Then** essa cobrança aparece com um aviso visível ao lado, enquanto as demais cobranças (com geração bem-sucedida ou ainda sem tentativa) não exibem nenhum aviso.
4. **Given** o cenário anterior, **When** uma tentativa de geração seguinte (agendada ou por recuperação na inicialização) consegue gerar com sucesso a conta que estava pendente para essa cobrança, **Then** o aviso deixa de aparecer para ela na tela de gerenciamento.

---

### Edge Cases

- Dia de vencimento configurado como 31 num mês com menos dias (ex.: abril, fevereiro): a conta gerada usa o último dia do mês-alvo (coberto em US2).
- Processo de geração roda mais de uma vez no mesmo ciclo (ex.: redeploy no dia 25): idempotência evita duplicar contas (coberto em US2).
- Cobrança recorrente com valor R$0,00: aceita normalmente; o ajuste do valor de cada conta gerada é manual, fora do escopo do processo automático.
- Nenhuma cobrança recorrente ativa no momento da execução do processo: nada é gerado, sem erro.
- Cobrança recorrente removida (soft delete) entre uma execução e outra do processo mensal: deixa de ser considerada ativa e não gera mais contas dali em diante, mas as contas já geradas por ela permanecem intactas.
- Grupo vazio selecionado ao cadastrar cobrança recorrente para grupo: nenhuma linha é criada.
- Edição de qualquer campo (não só o valor) de uma cobrança recorrente ativa: substitui a linha vigente por uma nova versão ativa, preservando a antiga (inativa) e as contas já geradas que a referenciam.
- Tentativa de remover uma contraparte (Party) ou fundo (Fund) referenciado por uma cobrança recorrente ativa: bloqueada, seguindo o mesmo padrão já aplicado quando há contas vinculadas.
- App reiniciado no dia 26 (ou depois) sem que a geração daquele ciclo tenha rodado ainda (ex.: aplicação estava fora do ar às 6h do dia 25): a verificação de recuperação na inicialização dispara a geração imediatamente, cobrindo o ciclo perdido, sem depender de nenhuma ação manual.
- App reiniciado antes do dia 25 do ciclo atual: a verificação de recuperação não dispara nada para as cobranças que ainda não têm um mês-alvo pendente, pois o ciclo ainda não chegou na data-alvo.
- Geração falha para uma cobrança recorrente específica (ex.: erro inesperado): as demais cobranças do mesmo ciclo são geradas normalmente (isolamento por cobrança); a cobrança que falhou fica com um aviso visível na tela de gerenciamento e sem conta gerada para aquele mês-alvo até uma tentativa seguinte (agendada ou por recuperação na inicialização) ter sucesso.
- Nenhuma tentativa seguinte ocorre para o mês-alvo de uma cobrança que falhou antes de o ciclo seguinte avançar o alvo para o mês posterior (ex.: app não reinicia entre uma falha e o próximo dia 25): aquele mês específico fica definitivamente sem conta gerada automaticamente — a usuária precisa lançá-la manualmente; o aviso de falha permanece visível como sinalização.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir cadastrar uma cobrança/pagamento recorrente informando: tipo (receber ou pagar, obrigatório na criação e imutável na edição), valor fixo, dia do mês de vencimento, descrição, fundo, contraparte e observações opcionais.
- **FR-002**: O valor fixo de uma cobrança recorrente MAY ser R$0,00, representando uma cobrança sem valor definido de antemão, cujas contas geradas a usuária ajusta manualmente depois.
- **FR-003**: Ao cadastrar uma cobrança recorrente, o sistema MUST permitir escolher entre uma contraparte específica ou um grupo; quando um grupo é escolhido, o sistema MUST criar uma linha de cobrança recorrente independente para cada integrante do grupo no momento do cadastro, sem manter nenhum vínculo persistente com o grupo depois de criadas as linhas.
- **FR-004**: O sistema MUST executar automaticamente, todo dia 25 de cada mês às 6h no horário de Brasília, um processo que gera uma conta a partir de cada cobrança recorrente ativa naquele momento, com vencimento no mês seguinte ao da execução.
- **FR-005**: Cada conta gerada por esse processo MUST armazenar uma referência de volta para a cobrança recorrente que a originou.
- **FR-006**: Antes de gerar uma conta para uma cobrança recorrente e um mês/ano-alvo, o sistema MUST verificar diretamente, pela referência armazenada (FR-005), se já existe uma conta gerada a partir daquela cobrança para aquele mês/ano — e, se existir, MUST pular a geração para essa cobrança, sem criar duplicata.
- **FR-007**: Quando o dia do mês configurado numa cobrança recorrente for maior que a quantidade de dias do mês-alvo, a conta gerada MUST ter vencimento no último dia daquele mês.
- **FR-008**: Editar qualquer campo de uma cobrança recorrente MUST preservar inalteradas as contas já geradas a partir da versão anterior — a edição MUST ser implementada substituindo a linha vigente por uma nova linha ativa com os valores atualizados, tornando a linha anterior inativa (mas preservada, ainda referenciada pelas contas já geradas a partir dela).
- **FR-009**: O sistema MUST permitir remover uma cobrança recorrente sem excluí-la fisicamente (soft delete) — a linha e as contas já geradas a partir dela permanecem preservadas, mas a cobrança deixa de aparecer nas listagens e deixa de ser considerada ativa para a geração mensal (FR-004).
- **FR-010**: O sistema MUST prover uma tela de gerenciamento de cobranças recorrentes com listagem, criação, edição e remoção, reaproveitando os padrões de interação já estabelecidos em outras listagens do sistema (ações individuais por linha, seleção múltipla com remoção em lote).
- **FR-011**: A listagem de cobranças recorrentes MUST exibir apenas as linhas ativas — linhas removidas (FR-009) e versões antigas substituídas por edição (FR-008) MUST ficar ocultas.
- **FR-012**: O sistema MUST impedir a alteração do tipo (receber/pagar) de uma cobrança recorrente já cadastrada, da mesma forma que já impede a alteração do tipo em contas.
- **FR-013**: O processo de geração mensal (FR-004) MUST concluir sem erro mesmo quando não houver nenhuma cobrança recorrente ativa no momento da execução.
- **FR-014**: O sistema MUST impedir a remoção de uma contraparte (Party) ou fundo (Fund) referenciado por ao menos uma cobrança recorrente ativa, seguindo o mesmo padrão já aplicado quando há contas vinculadas a eles.
- **FR-015**: Além do agendamento fixo descrito em FR-004, toda vez que o sistema inicia, MUST reaplicar — cobrança recorrente ativa por cobrança recorrente ativa — a mesma verificação de idempotência do FR-006 para o mês/ano-alvo mais recente cujo prazo de geração (dia 25) já tenha passado, tentando gerar imediatamente a conta de qualquer cobrança que ainda não a tenha. Essa verificação por cobrança (não por "o ciclo rodou ou não") MUST cobrir tanto um ciclo inteiramente perdido (aplicação fora do ar às 6h do dia 25) quanto uma cobrança específica cuja geração falhou isoladamente numa execução anterior (FR-016), sem exigir que a usuária espere até o próximo ciclo mensal para uma nova tentativa.
- **FR-016**: Durante uma execução do processo de geração (agendada, FR-004, ou por recuperação, FR-015), uma falha ao gerar a conta de uma cobrança recorrente específica MUST ser isolada — MUST NOT impedir a geração das contas das demais cobranças recorrentes ativas naquele mesmo ciclo.
- **FR-017**: Quando a tentativa mais recente de geração de uma cobrança recorrente tiver falhado (sem que a conta do mês/ano-alvo pendente tenha sido gerada com sucesso ainda), a tela de gerenciamento de cobranças recorrentes (FR-010) MUST exibir um aviso visível ao lado dela; cobranças cuja geração mais recente teve sucesso, ou que ainda não tiveram nenhuma tentativa, MUST NOT exibir nenhum aviso adicional.
- **FR-018**: O aviso descrito em FR-017 MUST desaparecer automaticamente assim que uma tentativa de geração subsequente (agendada, FR-004, ou por recuperação na inicialização, FR-015) conseguir gerar com sucesso a conta pendente daquele mês/ano-alvo para aquela cobrança recorrente, sem exigir nenhuma ação manual da usuária além de a aplicação voltar a rodar.

### Key Entities

- **Cobrança/Pagamento Recorrente (molde)**: representa um lançamento fixo e recorrente associado a uma única contraparte. Atributos: tipo (receber/pagar), valor fixo (pode ser R$0,00), dia do mês de vencimento, descrição, fundo, contraparte, observações opcionais, se está ativa (usado tanto para decidir se gera contas quanto para aparecer na listagem), e se a tentativa de geração mais recente falhou (usado para exibir o aviso da FR-017, sem exigir nenhum canal externo de notificação). Relaciona-se com Fundo e Contraparte da mesma forma que uma conta comum. Editar qualquer campo gera uma nova linha ativa e inativa a anterior (sem encadeamento explícito entre versões); remover torna a linha inativa sem excluí-la.
- **Conta (existente)**: passa a poder guardar uma referência opcional para a cobrança recorrente que a originou, usada apenas para checar se já existe uma conta gerada para aquele mês/ano — o valor, descrição e demais campos da conta gerada são copiados no momento da geração e não mudam retroativamente se a cobrança recorrente for editada ou removida depois.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A partir do dia seguinte à execução do processo mensal — seja pelo agendamento fixo do dia 25, seja pela recuperação na inicialização (FR-015) —, toda cobrança recorrente que estava ativa no momento da execução tem uma conta correspondente gerada para o mês seguinte, sem qualquer ação manual da usuária, mesmo que a aplicação não estivesse em execução no horário exato agendado.
- **SC-002**: Reexecutar o processo de geração mensal no mesmo ciclo (ex.: após um redeploy) nunca produz uma segunda conta para a mesma cobrança recorrente e o mesmo mês-alvo.
- **SC-003**: Editar o valor de uma cobrança recorrente nunca altera o valor de uma conta já gerada anteriormente a partir dela — 100% das contas históricas permanecem com o valor vigente na data em que foram geradas.
- **SC-004**: Remover uma cobrança recorrente interrompe a geração de novas contas a partir do mês seguinte à remoção, sem apagar nenhuma conta já gerada nem quebrar a referência dela para a cobrança de origem.
- **SC-005**: A usuária consegue cadastrar uma cobrança recorrente para todos os integrantes de um grupo em uma única operação, sem precisar repetir o cadastro manualmente para cada integrante.
- **SC-006**: Quando a geração falha para uma cobrança recorrente específica, a usuária consegue perceber isso olhando a tela de gerenciamento (sem precisar consultar log técnico), e as demais cobranças do mesmo ciclo são geradas normalmente, sem nenhuma afetada pela falha de outra.

## Assumptions

- A geração da primeira conta de uma cobrança recorrente depende exclusivamente da execução do processo mensal (dia 25, 6h de Brasília) — criar ou editar uma cobrança recorrente não dispara geração imediata de conta fora desse ciclo.
- O processo mensal não trata o dia 25 como precisando ser dia útil — roda no calendário todo dia 25, inclusive finais de semana e feriados.
- Não há necessidade de disparo manual/sob demanda do processo de geração pela interface nesta feature — apenas as duas execuções automáticas descritas em FR-004 (agendamento fixo) e FR-015 (recuperação na inicialização).
- Como o projeto ainda não tem nenhuma cobrança recorrente cadastrada, não há necessidade de gerar retroativamente contas para meses anteriores à adoção desta feature — o processo só passa a gerar contas a partir do primeiro dia 25 após a feature entrar em produção.
