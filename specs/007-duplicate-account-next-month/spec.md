# Feature Specification: Duplicar lançamentos para o mês seguinte

**Feature Branch**: `007-duplicate-account-next-month`

**Created**: 2026-08-01

**Status**: Draft

**Input**: User description: "ao selecionar um ou mais itens das paginas de listagem, quero que tambem disponibilize as opções \"duplicar para o mês seguinte\" (porém como pendente, pagamento não efetuado ainda - atalho = ctrl c + ctrl v) e \"duplicar para o mês seguinte com valor zerado\" (mesma coisa, só muda que o valor da cópia é de zero reais)"

## Clarifications

### Session 2026-08-01

- Q: Sobre o atalho de teclado Ctrl+C/Ctrl+V (FR-009 a FR-011): mantemos como especificado, ou removemos da spec e deixamos só os dois botões na barra de ações em lote? → A: Manter o atalho como especificado (FR-009 a FR-011 permanecem sem alteração).

### Session 2026-08-02 (revisão pós-implementação)

- Durante a revisão da entrega implementada, a usuária identificou falta de feedback visual: nenhum destaque diferenciava as linhas selecionadas das demais, e depois de duplicar não havia como saber se a operação deu certo nem onde a cópia foi parar. Adicionados FR-012 a FR-014 para cobrir esse feedback (destaque de seleção, destaque temporário + scroll até a cópia, e mensagem de sucesso com a contagem).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Duplicar lançamento mantendo o valor (Priority: P1)

Como responsável pela gestão do condomínio, depois de lançar uma conta a pagar ou a receber (ex.: uma taxa mensal, uma conta de luz recorrente), quero selecionar esse lançamento na listagem e duplicá-lo para o mês seguinte com um único comando, sem precisar reabrir o formulário e redigitar todos os dados, para agilizar o lançamento de contas que se repetem mês a mês com o mesmo valor.

**Why this priority**: É o caso de uso mais comum citado pela usuária (contas recorrentes com valor estável) e já entrega valor completo sozinho — é a ação mínima viável desta feature.

**Independent Test**: Selecionar um lançamento existente, acionar "Duplicar para o mês seguinte" (pelo botão ou pelo atalho Ctrl+C seguido de Ctrl+V) e verificar que uma nova conta é criada com o mesmo valor e demais dados do original, vencimento um mês depois e sem data de pagamento.

**Acceptance Scenarios**:

1. **Given** um lançamento pago com vencimento em 10/03, **When** a usuária o seleciona e aciona "Duplicar para o mês seguinte", **Then** uma nova conta é criada com vencimento em 10/04, mesmo valor, mesma descrição, mesmo fundo e mesma contraparte do original, e sem data de pagamento (pendente).
2. **Given** dois lançamentos selecionados simultaneamente, com vencimentos em meses diferentes, **When** a usuária aciona "Duplicar para o mês seguinte", **Then** cada cópia é criada com vencimento um mês após o vencimento do seu respectivo lançamento original.
3. **Given** uma duplicação concluída, **When** a usuária verifica o lançamento original, **Then** ele permanece exatamente como estava antes (mesmo valor, mesmo vencimento, mesmo status de pagamento).
4. **Given** um ou mais lançamentos selecionados, **When** a usuária pressiona Ctrl+C e, em seguida, Ctrl+V, **Then** o mesmo resultado do botão "Duplicar para o mês seguinte" ocorre para os lançamentos que estavam selecionados no momento do Ctrl+C.
5. **Given** uma duplicação concluída com sucesso, **When** a nova conta está visível na listagem sob o filtro atual, **Then** a usuária vê a linha da cópia destacada visualmente por alguns segundos e a tela rola até ela; em qualquer caso (visível ou não), a usuária vê uma mensagem informando quantas cópias foram criadas.

---

### User Story 2 - Duplicar lançamento com valor zerado (Priority: P2)

Como responsável pela gestão do condomínio, quero duplicar um lançamento para o mês seguinte já com o valor zerado, para criar rapidamente o registro do próximo mês nos casos em que o valor ainda não é conhecido (ex.: uma conta de consumo variável), preenchendo o valor correto depois.

**Why this priority**: Cobre um caso de uso adicional e mais específico (valor variável mês a mês) sobre a mesma mecânica da User Story 1; depende dela existir primeiro, por isso prioridade P2.

**Independent Test**: Selecionar um lançamento existente, acionar "Duplicar para o mês seguinte com valor zerado" e verificar que a nova conta é criada com valor R$ 0,00, mantendo os demais dados e regras da User Story 1.

**Acceptance Scenarios**:

1. **Given** um lançamento com valor de R$ 350,00 e vencimento em 15/06, **When** a usuária o seleciona e aciona "Duplicar para o mês seguinte com valor zerado", **Then** uma nova conta é criada com vencimento em 15/07, valor R$ 0,00, mesma descrição, mesmo fundo, mesma contraparte e sem data de pagamento.
2. **Given** uma duplicação com valor zerado concluída, **When** a usuária verifica o lançamento original, **Then** ele mantém seu valor original inalterado.

---

### Edge Cases

- Quando a data de vencimento do lançamento original cai num dia que não existe no mês seguinte (ex.: 31 de janeiro), a nova data de vencimento cai no último dia do mês seguinte (ex.: 28 ou 29 de fevereiro).
- Se uma das cópias não puder ser criada por violar alguma regra de negócio existente do cadastro de contas, as demais cópias selecionadas continuam sendo criadas normalmente, e a usuária é informada de quais falharam e por quê (mesmo padrão de melhor esforço já usado na remoção em lote).
- Duplicar um lançamento várias vezes gera várias cópias independentes — não há verificação de duplicidade nem bloqueio para evitar cópias repetidas.
- Os lançamentos duplicados pertencem ao mês seguinte ao do original, que pode não ser o mês atualmente exibido pelos filtros da listagem; a listagem mantém o filtro atual sem alterá-lo automaticamente após a duplicação, então as cópias podem não aparecer imediatamente na tela até a usuária ajustar o filtro.
- Lançamentos já pagos podem ser duplicados normalmente: a cópia sempre nasce pendente, independentemente do status de pagamento do original.
- Pressionar Ctrl+V sem ter pressionado Ctrl+C antes (ou sem nenhum lançamento selecionado no momento do Ctrl+C) não tem efeito nenhum.
- Pressionar Ctrl+C novamente sobre uma nova seleção substitui a memorização anterior — só a seleção do Ctrl+C mais recente é usada no próximo Ctrl+V.
- Quando a cópia cai fora do filtro atualmente exibido (edge case acima), o destaque visual e a rolagem automática da FR-013 não têm efeito (não há linha visível para destacar) — a mensagem de sucesso da FR-014 permanece a única confirmação nesse caso.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir selecionar um ou mais lançamentos (contas a pagar ou a receber) na listagem de contas, reaproveitando o mecanismo de seleção múltipla já existente na tela.
- **FR-002**: Quando houver ao menos um lançamento selecionado, o sistema MUST oferecer a ação "Duplicar para o mês seguinte" junto às demais ações em lote da listagem.
- **FR-003**: Quando houver ao menos um lançamento selecionado, o sistema MUST oferecer a ação "Duplicar para o mês seguinte com valor zerado" junto às demais ações em lote da listagem.
- **FR-004**: Ao executar "Duplicar para o mês seguinte", o sistema MUST criar, para cada lançamento selecionado, uma nova conta independente com os mesmos dados do original (tipo, descrição, valor, fundo, contraparte, indicador de recorrência e observações), exceto que: (a) a data de vencimento da cópia MUST ser um mês após a data de vencimento do original (ver regra de ajuste de dia nos Edge Cases); e (b) a cópia MUST nascer sem data de pagamento (pendente), independentemente de o original estar pago ou não.
- **FR-005**: Ao executar "Duplicar para o mês seguinte com valor zerado", o sistema MUST aplicar as mesmas regras de FR-004, exceto que o valor da cópia MUST ser zero, independentemente do valor do lançamento original.
- **FR-006**: O lançamento original MUST permanecer inalterado (mesmo valor, vencimento, status de pagamento e demais dados) após a execução de qualquer uma das duas ações de duplicação.
- **FR-007**: Quando uma ou mais cópias não puderem ser criadas, o sistema MUST concluir a criação das demais cópias da mesma operação e informar à usuária quantas falharam e o motivo de cada falha.
- **FR-008**: Ao concluir uma operação de duplicação (com ou sem falhas parciais), o sistema MUST limpar a seleção atual e manter os filtros de listagem como estavam, sem forçá-los a mudar para o mês dos lançamentos recém-criados.
- **FR-009**: Além do botão na barra de ações em lote, o sistema MUST oferecer um atalho de teclado equivalente à ação "Duplicar para o mês seguinte" (valor mantido): pressionar Ctrl+C com um ou mais lançamentos selecionados MUST memorizar essa seleção, e pressionar Ctrl+V em seguida MUST duplicar os lançamentos memorizados no momento do Ctrl+C, mesmo que a seleção na tela tenha mudado entre um comando e outro.
- **FR-010**: O atalho de teclado da FR-009 MUST se aplicar apenas à variante "valor mantido"; a variante "valor zerado" fica disponível somente pelo botão dedicado na barra de ações em lote, já que a usuária não indicou um atalho equivalente para ela.
- **FR-011**: O atalho de teclado da FR-009 MUST ser ignorado enquanto o foco estiver em um campo de edição da própria tela (ex.: edição inline de valor), para não interferir com o copiar/colar nativo de texto desse campo.
- **FR-012**: O sistema MUST destacar visualmente cada linha selecionada na listagem, para diferenciá-la das demais enquanto a seleção estiver ativa.
- **FR-013**: Ao concluir uma duplicação com pelo menos uma cópia criada com sucesso, o sistema MUST destacar visualmente, por alguns segundos, cada linha recém-criada que estiver visível na listagem sob o filtro atual, e rolar a tela até a primeira delas.
- **FR-014**: Ao concluir uma operação de duplicação com pelo menos uma cópia criada com sucesso, o sistema MUST exibir uma mensagem informando quantas cópias foram criadas — mesmo quando nenhuma delas estiver visível na listagem sob o filtro atual (FR-008), garantindo que a usuária saiba que a operação funcionou independentemente do destaque visual da FR-013.

### Key Entities

- **Lançamento (Account)**: representa uma conta a pagar ou a receber já cadastrada, com vencimento, valor, fundo, contraparte, indicador de recorrência, observações e situação de pagamento. A duplicação lê um lançamento existente e cria uma nova instância independente a partir dele, sem vínculo entre original e cópia após a criação.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A usuária consegue gerar o lançamento do mês seguinte a partir de um já existente com uma única ação, sem preencher novamente nenhum campo do formulário.
- **SC-002**: A usuária consegue duplicar vários lançamentos selecionados de uma só vez, com o mesmo número de passos hoje necessário para remover vários lançamentos selecionados.
- **SC-003**: 100% dos lançamentos duplicados com sucesso preservam corretamente todos os dados do original, exceto vencimento (um mês depois), pagamento (sempre pendente) e, quando aplicável, valor (zero).
- **SC-004**: Em 100% das execuções, nenhum lançamento original é alterado como efeito colateral de uma duplicação.

## Assumptions

- A ação se aplica apenas à listagem de contas a pagar/receber, por ser a única entidade do sistema com conceito de vencimento e valor monetário duplicável; as listagens de condôminos/fornecedores, fundos e grupos não ganham essas ações nesta feature.
- O "mês seguinte" é calculado individualmente a partir da data de vencimento de cada lançamento selecionado — não a partir do filtro de mês atualmente aplicado na tela nem da data corrente do sistema — permitindo duplicar corretamente lançamentos de meses diferentes numa mesma seleção.
- Ambos os tipos de lançamento (contas a receber e a pagar) suportam as duas ações de duplicação, sem distinção de regra entre eles.
- A duplicação não verifica nem impede duplicidade: cada execução gera uma nova cópia independente, mesmo que uma cópia equivalente já exista de uma execução anterior.
- A duplicação em lote segue o mesmo padrão de "melhor esforço" já adotado pela remoção em lote (sem endpoint transacional único cobrindo toda a seleção): cada cópia é criada individualmente, e a falha de uma não impede a criação das demais.
