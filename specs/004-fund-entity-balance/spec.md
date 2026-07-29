# Feature Specification: Fundos como Entidade e Visualização de Saldo Real

**Feature Branch**: `004-fund-entity-balance`

**Created**: 2026-07-29

**Status**: Draft

**Input**: User description: "quero uma visualização do saldo dos fundos. quero saber quanto tem em cada fundo (quanto REALMENTE tem, NÃO o valor previsto após todos os recebimentos/pagamentos). também quero que os fundos NÃO sejam mais enum. quero que sejam entity, para que eu possa incluir e editar fundos."

## Clarifications

### Session 2026-07-29

- Q: Quando um pagamento (conta a pagar) seria registrado como pago e isso deixaria o saldo real do fundo negativo, o que o sistema deve fazer? → A: Apenas informativo — o sistema não impede nada, apenas exibe o saldo real (podendo ficar negativo) na visualização; nenhuma validação nova é adicionada ao registrar pagamentos/recebimentos.
- Q: Os três fundos hoje fixos (Piscina, Jardim Piscina, Jardim Lateral) precisam ser preservados/recriados via migration ao converter Fundo para cadastro? → A: Não. O banco é apenas de desenvolvimento — não há dado real a preservar. Um ambiente novo passa a existir sem nenhum fundo cadastrado; a usuária cadastra os nomes que desejar.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Visualizar o saldo real de cada fundo (Priority: P1)

Como responsável pela gestão financeira do condomínio, quero ver, em uma única tela, quanto dinheiro cada fundo (ex.: Piscina, Jardim) realmente possui hoje — considerando apenas os recebimentos e pagamentos já efetivados — para saber quanto posso de fato utilizar de cada fundo, sem contar valores que ainda estão apenas previstos (lançamentos em aberto).

**Why this priority**: É o valor central pedido nesta funcionalidade — sem essa visão, a usuária continua tendo que somar manualmente lançamentos pagos/recebidos de cada fundo para saber o saldo real disponível.

**Independent Test**: Pode ser testado cadastrando lançamentos de entrada e saída vinculados a um fundo, marcando alguns como pagos/recebidos e deixando outros em aberto, e verificando que a tela de saldo mostra apenas o resultado dos já efetivados.

**Acceptance Scenarios**:

1. **Given** um fundo com um recebimento de R$500 já recebido e uma saída de R$200 já paga, **When** a usuária acessa a visualização de saldo dos fundos, **Then** o saldo real exibido para esse fundo é R$300.
2. **Given** um fundo com um recebimento de R$500 já recebido e uma saída de R$200 ainda em aberto (não paga), **When** a usuária acessa a visualização de saldo dos fundos, **Then** o saldo real exibido para esse fundo é R$500 (o valor em aberto não é descontado).
3. **Given** múltiplos fundos cadastrados, **When** a usuária acessa a visualização, **Then** o saldo real de cada fundo é exibido separadamente, junto com o saldo total somado de todos os fundos.

---

### User Story 2 - Cadastrar um novo fundo (Priority: P2)

Como responsável pela gestão financeira do condomínio, quero poder cadastrar um novo fundo a qualquer momento, para acomodar novas reservas financeiras que passem a existir no condomínio, sem depender de uma alteração no sistema para isso.

**Why this priority**: Depende logicamente de fundos existirem como cadastro (e não mais como uma lista fixa) para fazer sentido, mas só tem valor prático depois que a visualização de saldo (US1) já existe para mostrar o resultado.

**Independent Test**: Pode ser testado cadastrando um novo fundo com um nome e confirmando que ele passa a estar disponível para seleção ao lançar uma nova conta a receber ou a pagar.

**Acceptance Scenarios**:

1. **Given** a tela de cadastro de fundos, **When** a usuária informa um nome não utilizado por nenhum outro fundo, informa um saldo inicial e confirma, **Then** o novo fundo é criado e passa a aparecer na lista de fundos e na visualização de saldo, com saldo real igual ao saldo inicial informado.
2. **Given** um fundo já cadastrado com um determinado nome, **When** a usuária tenta cadastrar outro fundo com o mesmo nome, **Then** o sistema recusa a operação e exibe uma mensagem informando que o nome já está em uso.

---

### User Story 3 - Editar ou remover um fundo existente (Priority: P3)

Como responsável pela gestão financeira do condomínio, quero poder corrigir o nome de um fundo já cadastrado e remover um fundo que não é mais necessário, para manter o cadastro de fundos organizado e sem itens obsoletos.

**Why this priority**: É um complemento de manutenção do cadastro — tem valor, mas o sistema já entrega o benefício principal (visualizar saldo real e criar novos fundos) sem essa capacidade.

**Independent Test**: Pode ser testado editando o nome de um fundo existente e verificando que os lançamentos já vinculados a ele continuam associados corretamente; e tentando remover um fundo sem lançamentos vinculados, verificando que ele desaparece da lista.

**Acceptance Scenarios**:

1. **Given** um fundo já cadastrado, **When** a usuária edita seu nome ou seu saldo inicial, **Then** o novo nome e/ou o novo saldo real (recalculado a partir do saldo inicial atualizado) passam a ser exibidos em todos os lugares do sistema (lista de fundos, saldo, lançamentos já vinculados a ele).
2. **Given** um fundo sem nenhum lançamento vinculado, **When** a usuária solicita sua remoção, **Then** o fundo é removido e deixa de aparecer na lista de fundos e na visualização de saldo.
3. **Given** um fundo com pelo menos um lançamento (conta a receber ou a pagar) vinculado a ele, **When** a usuária tenta removê-lo, **Then** o sistema recusa a remoção e exibe uma mensagem explicando que o fundo está em uso.

---

### Edge Cases

- O que acontece quando a usuária tenta cadastrar ou renomear um fundo deixando o nome em branco? O sistema recusa a operação, exigindo um nome preenchido.
- O que acontece quando a usuária tenta cadastrar ou renomear um fundo usando um nome já usado por outro fundo (ignorando maiúsculas/minúsculas)? O sistema recusa a operação e informa que o nome já está em uso.
- O que acontece com o saldo real de um fundo recém-criado, que ainda não tem nenhum lançamento vinculado? É igual ao saldo inicial informado no cadastro (zero, se nenhum valor for informado).
- O que acontece quando um lançamento vinculado a um fundo é excluído ou tem seu recebimento/pagamento estornado? O saldo real do fundo é recalculado automaticamente, refletindo o estado atual dos lançamentos.
- O que acontece com os três fundos hoje fixos (Piscina, Jardim Piscina, Jardim Lateral) quando esta funcionalidade entrar em vigor? Nada é pré-cadastrado automaticamente — o ambiente passa a ter zero fundos cadastrados, e a usuária cadastra manualmente os fundos que desejar (podendo reutilizar esses mesmos nomes ou escolher outros).
- O que acontece quando registrar um pagamento como pago deixaria o saldo real do fundo negativo? O sistema permite normalmente — o saldo negativo é apenas exibido na visualização, sem nenhum bloqueio ou aviso no registro do pagamento.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir cadastrar um novo fundo informando um nome e um saldo inicial (de abertura).
- **FR-002**: O sistema MUST impedir o cadastro de um fundo com um nome já utilizado por outro fundo existente.
- **FR-003**: O sistema MUST permitir editar o nome e o saldo inicial de um fundo já cadastrado.
- **FR-004**: O sistema MUST impedir a remoção de um fundo que possua ao menos um lançamento (conta a receber ou a pagar) vinculado a ele.
- **FR-005**: O sistema MUST permitir a remoção de um fundo que não possua nenhum lançamento vinculado a ele.
- **FR-006**: O sistema MUST continuar permitindo que todo lançamento de conta a receber ou a pagar seja vinculado a exatamente um fundo dentre os cadastrados, exatamente como já ocorre hoje.
- **FR-007**: O sistema MUST exibir uma visualização com o saldo real de cada fundo cadastrado, além do saldo total somado de todos os fundos.
- **FR-008**: O saldo real de cada fundo MUST ser calculado somando os valores de todos os lançamentos de entrada já efetivamente recebidos e subtraindo os valores de todos os lançamentos de saída já efetivamente pagos, vinculados àquele fundo. Lançamentos ainda em aberto (sem recebimento ou pagamento confirmado) MUST NOT ser incluídos nesse cálculo.
- **FR-009**: O sistema MUST permitir que um ambiente sem nenhum fundo cadastrado opere normalmente — não há necessidade de pré-cadastrar os três fundos hoje fixos (Piscina, Jardim Piscina, Jardim Lateral) nem de preservar nenhum dado de lançamento existente; a usuária cadastra manualmente os fundos que desejar após a mudança.
- **FR-010**: Cada fundo MUST possuir um saldo inicial (de abertura), informado pela usuária no cadastro e editável posteriormente, representando o valor que já existia fisicamente antes deste recurso existir.
- **FR-011**: O saldo real exibido para cada fundo MUST ser igual ao saldo inicial daquele fundo somado ao resultado do cálculo descrito em FR-008 (recebimentos já recebidos menos pagamentos já pagos vinculados ao fundo).
- **FR-012**: O saldo real de um fundo MAY ficar negativo (quando pagamentos já efetivados superam o saldo inicial somado aos recebimentos já efetivados); o sistema MUST NOT bloquear nem exigir confirmação especial ao registrar um recebimento ou pagamento apenas por isso deixar o saldo do fundo negativo — a visualização é puramente informativa.

### Key Entities *(include if feature involves data)*

- **Fundo**: representa uma reserva financeira do condomínio (ex.: Piscina, Jardim Piscina, Jardim Lateral) usada para categorizar lançamentos de contas a receber e a pagar. Possui um nome (identificador legível e único), um saldo inicial (de abertura, editável pela usuária) e um saldo real — este último obtido somando o saldo inicial ao resultado líquido dos lançamentos que o referenciam. Não é mais um valor fixo predefinido pelo sistema, e sim um cadastro que a usuária pode criar, editar e remover.
- **Lançamento (conta a receber/a pagar)**: já existente no sistema; passa a referenciar um fundo cadastrado (em vez de um valor fixo), mantendo a mesma regra de hoje de que todo lançamento pertence a exatamente um fundo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A usuária consegue ver o saldo real de todos os fundos, e o total somado, em uma única tela, sem precisar somar lançamentos manualmente.
- **SC-002**: O saldo real exibido para cada fundo reflete exclusivamente recebimentos e pagamentos já efetivados — lançamentos em aberto nunca alteram o valor exibido.
- **SC-003**: A usuária consegue cadastrar um novo fundo em menos de 30 segundos.
- **SC-004**: A usuária consegue editar o nome de um fundo existente sem que nenhum lançamento já vinculado a ele perca a associação.
- **SC-005**: Toda tentativa de remover um fundo em uso é bloqueada com uma mensagem que explica o motivo, sem exigir tentativa e erro da usuária.
- **SC-006**: Um ambiente novo (sem nenhum fundo pré-cadastrado) permite à usuária cadastrar e usar fundos normalmente, sem exigir nenhuma etapa de migração de dados prévios.

## Assumptions

- Um fundo possui, como dados cadastrais, um nome (identificador legível e único) e um saldo inicial — sem outros atributos (ex.: descrição, categoria) além do necessário para identificação e cálculo do saldo real, no mesmo nível de simplicidade de outras entidades de cadastro já existentes no sistema (ex.: Fornecedor).
- Se a usuária não informar um saldo inicial ao cadastrar um fundo, o valor padrão é zero.
- O banco de dados atual é apenas de desenvolvimento — nenhum dado de lançamento ou fundo existente precisa ser preservado nesta transição. Os três nomes hoje fixos (Piscina, Jardim Piscina, Jardim Lateral) não são pré-cadastrados automaticamente; a usuária os recria manualmente (ou usa outros nomes) após a mudança, se desejar.
- A remoção de um fundo em uso é bloqueada, seguindo o mesmo padrão já adotado para outras entidades do sistema que não podem ser removidas quando possuem vínculos (ex.: unidade, fornecedor).
- A confirmação antes de remover um fundo é responsabilidade exclusiva da interface (diálogo de confirmação), não do backend — mesmo padrão já usado para as demais entidades do sistema.
- A visualização de saldo dos fundos exibe o estado atual (saldo real "hoje"), sem necessidade de filtro por período ou data de referência nesta primeira versão.
- Comparação de nomes de fundo para verificar duplicidade não diferencia maiúsculas de minúsculas nem espaços nas extremidades.
