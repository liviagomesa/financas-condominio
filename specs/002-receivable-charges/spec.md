# Feature Specification: Lançamentos de Contas a Receber

**Feature Branch**: `002-receivable-charges`

**Created**: 2026-07-26

**Status**: Draft

**Input**: User description: "Lançamentos de contas a receber (por unidade, com opção de adicionar para todas simultaneamente)"

## Status Pós-Feature 003

**Atualização (feature 003 — `specs/003-accounts-payable-suppliers/`)**: a entidade `Receivable` descrita nesta feature foi **generalizada** pela feature 003 em uma entidade única `Account`, que passa a suportar tanto contas a receber (o que esta feature já fazia) quanto contas a pagar a fornecedores (novo). Este documento **permanece como registro histórico** das decisões desta feature (ex.: por que `paymentDate` sem campo `paid` separado, por que os filtros são aplicados em memória) — nada abaixo foi alterado. O comportamento e o modelo de dados vigentes estão em `specs/003-accounts-payable-suppliers/spec.md` e `data-model.md`.

## Clarifications

### Session 2026-07-26

- Q: Quando a usuária tentar remover uma unidade (feature 001) que já possui lançamentos de contas a receber vinculados, o que deve acontecer? → A: Bloquear a remoção, exibindo mensagem de erro — mesma lógica já aplicada hoje para condôminos vinculados (FR-006 da feature 001).
- Q: O campo "Tipo" do lançamento (recorrente/extra) deve ser modelado como enum nomeado ou como boolean? → A: Boolean (`isRecorrente`): `true` representa recorrente, `false` representa extra.

### Sessão de correção 2026-07-26

- Q: Um lançamento já marcado como pago pode ser editado ou removido normalmente? → A: Sim, livremente — as mesmas regras de um lançamento pendente se aplicam, sem bloqueio adicional (ver FR-017).
- Q: É preciso uma ação para desfazer (estornar) um pagamento registrado por engano? → A: Não nesta rodada — registrar pagamento novamente sobre um lançamento já pago apenas atualiza a data informada; não há ação dedicada de "desfazer pagamento" (fica registrado como melhoria futura no README).
- Q: Ao remover vários lançamentos de uma vez pela listagem (nova seleção múltipla), se algum não puder ser removido, o que deve acontecer? → A: Melhor esforço — cada item selecionado é removido individualmente, reaproveitando a regra de remoção já existente; ao final, a usuária vê quais foram removidos e quais falharam (e por quê).
- Q: O campo "Tipo" deve continuar exigindo seleção explícita num `<select>`, ou pode virar uma caixa de seleção simples? → A: Caixa de seleção única "Recorrente", desmarcada por padrão (equivalente a "Extra" quando não marcada) — deixa de ser um campo que exige preenchimento ativo a cada lançamento.

### Sessão de correção 2026-07-26 (parte 2)

- Q: Faz sentido ter um campo `paid` (booleano) separado, além de `paymentDate`? → A: Não — `paymentDate` nulo já significa "pendente" e não nulo já significa "pago"; um campo `paid` redundante só criaria risco de os dois ficarem inconsistentes entre si. O atributo "pago" deixa de existir como campo próprio; passa a ser derivado da presença de `paymentDate`.
- Q: É preciso criar o lançamento e só depois registrar o pagamento em uma ação separada? → A: Não — a criação (individual ou em lote) e a edição MUST aceitar informar a data de pagamento diretamente, opcionalmente, no mesmo formulário/chamada. A ação dedicada `POST /{id}/pay` continua existindo para o caso de marcar como pago depois de já criado, sem precisar reenviar o lançamento inteiro.
- Q: A conversão de data ISO ⇄ DD/MM/AAAA no frontend precisa de um utilitário próprio? → A: Não — `<input type="date">` já envia/recebe o valor em ISO-8601 nativamente, e o `DatePipe` (`| date:'dd/MM/yyyy'`) do Angular já formata a exibição sem custom code. A constituição (Princípio IV) foi ajustada para não exigir mais um utilitário dedicado.

### Sessão de correção 2026-08-02 (destaque de linha selecionada)

- A feature 007 (`specs/007-duplicate-account-next-month/`) introduziu um destaque visual (classe `table-active` do Bootstrap) para linhas selecionadas, mas implementou-o apenas em `account-list`, como se fosse um comportamento específico daquela tela. A usuária identificou que esse destaque está associado à seleção múltipla em si — o trio `list-selection.ts`/`bulk-delete.ts`/`bulk-actions-bar` definido nesta feature (FR-019, Assumptions) — e por isso MUST ser um comportamento consistente de toda listagem que reaproveita esse trio, não uma escolha isolada de uma tela. Ver FR-024 abaixo e nota de impacto cruzado em plan.md.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lançar conta a receber para uma unidade (Priority: P1)

Como responsável pela gestão do condomínio, quero lançar uma conta a receber para uma unidade específica, informando valor, data de vencimento, descrição, conta destino e tipo (recorrente ou extra), para registrar valores que essa unidade deve pagar (ex.: taxa condominial do mês, taxa extra) já classificados para consulta e filtragem futuras.

**Why this priority**: É a funcionalidade central desta feature — sem lançar contas a receber por unidade, não há registro nenhum de valores devidos, o que impede qualquer controle financeiro do condomínio.

**Independent Test**: Pode ser testada isoladamente — com ao menos uma unidade já cadastrada (feature 001) — lançando uma conta a receber para essa unidade e confirmando que ela aparece na listagem de lançamentos daquela unidade.

**Acceptance Scenarios**:

1. **Given** a unidade "Bloco A - 101" cadastrada e sem nenhum lançamento, **When** a usuária lança uma conta a receber com valor "R$ 350,00", vencimento "10/08/2026", descrição "Taxa condominial - Agosto/2026", conta destino "Piscina" e marca a caixa "Recorrente" para essa unidade, **Then** o lançamento é criado com sucesso e aparece na listagem de lançamentos da unidade, exibindo também a conta destino e o tipo escolhidos.
2. **Given** um formulário de lançamento sem valor, sem vencimento, sem unidade ou sem conta destino, **When** a usuária tenta confirmar o lançamento, **Then** o sistema rejeita o lançamento e indica quais campos obrigatórios estão faltando (o campo "Recorrente" nunca está entre eles, pois é uma caixa de seleção sempre com um valor definido — ver FR-014).
3. **Given** um formulário de lançamento com valor igual a zero ou negativo, **When** a usuária tenta confirmar o lançamento, **Then** o sistema rejeita o lançamento e indica que o valor deve ser positivo.
4. **Given** nenhuma unidade cadastrada no sistema, **When** a usuária tenta lançar uma conta a receber, **Then** o sistema orienta a cadastrar uma unidade primeiro.

---

### User Story 2 - Lançar a mesma conta a receber para todas as unidades simultaneamente (Priority: P1)

Como responsável pela gestão do condomínio, quero lançar uma conta a receber com o mesmo valor, vencimento, descrição, conta destino e tipo para todas as unidades de uma só vez, para agilizar o lançamento mensal da taxa condominial sem precisar repetir a operação unidade por unidade.

**Why this priority**: É o principal ganho de eficiência mencionado na descrição da feature — o uso mensal mais comum é lançar a mesma taxa para todas as unidades, e repetir manualmente essa ação para cada unidade seria o principal ponto de atrito do produto.

**Independent Test**: Pode ser testada isoladamente — com pelo menos duas unidades cadastradas — disparando a ação de lançamento em lote com um valor, vencimento e descrição, e confirmando que um lançamento correspondente aparece para cada unidade cadastrada.

**Acceptance Scenarios**:

1. **Given** três unidades cadastradas, **When** a usuária lança uma conta a receber "para todas as unidades" com valor "R$ 350,00", vencimento "10/08/2026", descrição "Taxa condominial - Agosto/2026", conta destino "Piscina" e tipo "Recorrente", **Then** um lançamento independente é criado para cada uma das três unidades, todos com o mesmo valor, vencimento, descrição, conta destino e tipo.
2. **Given** nenhuma unidade cadastrada no sistema, **When** a usuária tenta usar a ação "para todas as unidades", **Then** o sistema orienta a cadastrar ao menos uma unidade primeiro.
3. **Given** um lançamento em lote já confirmado para todas as unidades, **When** a usuária acessa a listagem de lançamentos de qualquer uma dessas unidades, **Then** o lançamento correspondente aparece como um registro independente, editável e removível sem afetar os lançamentos das demais unidades.

---

### User Story 3 - Listar e filtrar lançamentos de uma unidade (Priority: P2)

Como responsável pela gestão do condomínio, quero visualizar a lista de lançamentos de contas a receber de uma unidade, com valor, vencimento, descrição, conta destino e tipo de cada um, e poder filtrá-la por status de pagamento, por vencidos, ou por mês de vencimento ou de pagamento, para conferir o que já foi lançado e localizar rapidamente o que me interessa (ex.: o que ainda está pendente, o que já venceu, ou o que venceu/foi pago num mês específico) antes de lançar um novo valor ou de repassar a cobrança ao condômino.

**Why this priority**: É necessária para conferir e localizar lançamentos antes de editá-los ou removê-los, mas o sistema já entrega valor com apenas a criação (User Stories 1 e 2) mesmo antes de existir uma listagem dedicada.

**Independent Test**: Pode ser testada isoladamente lançando algumas contas a receber para uma unidade e verificando que todas aparecem na listagem dessa unidade, com valor, vencimento e descrição visíveis.

**Acceptance Scenarios**:

1. **Given** dois lançamentos criados para a unidade "Bloco A - 101", **When** a usuária acessa a listagem de lançamentos dessa unidade, **Then** os dois lançamentos aparecem, cada um exibindo valor, data de vencimento, descrição, conta destino e tipo.
2. **Given** uma unidade sem nenhum lançamento, **When** a usuária acessa a listagem de lançamentos dessa unidade, **Then** o sistema exibe uma indicação de que não há lançamentos cadastrados.
3. **Given** lançamentos pagos e pendentes cadastrados, **When** a usuária filtra a listagem por "pendentes" (ou por "pagos"), **Then** somente os lançamentos com aquele status aparecem.
4. **Given** um lançamento pendente com vencimento anterior à data atual e outro com vencimento futuro, **When** a usuária filtra a listagem por "vencidos", **Then** somente o lançamento pendente já vencido aparece (um lançamento já pago, mesmo com vencimento passado, não é considerado vencido).
5. **Given** lançamentos com vencimento em meses diferentes, **When** a usuária filtra a listagem por mês de vencimento (ex.: agosto/2026), **Then** somente os lançamentos com `dueDate` nesse mês aparecem.
6. **Given** lançamentos pagos em meses diferentes, **When** a usuária filtra a listagem por mês de pagamento (ex.: agosto/2026), **Then** somente os lançamentos pagos com data de pagamento nesse mês aparecem.

---

### User Story 4 - Editar e remover um lançamento (Priority: P3)

Como responsável pela gestão do condomínio, quero editar ou remover um lançamento de conta a receber já criado, para corrigir um valor, vencimento, descrição, conta destino, tipo ou a unidade associada informados incorretamente (ex.: lançamento feito na unidade errada por engano), ou para desfazer um lançamento feito por engano.

**Why this priority**: É uma operação de correção/manutenção, útil mas menos frequente que criar e listar lançamentos — o sistema já é utilizável apenas com criação e listagem.

**Independent Test**: Pode ser testada isoladamente editando um lançamento existente (incluindo trocar sua unidade associada) e confirmando que a listagem reflete os novos valores; e, separadamente, removendo um lançamento e confirmando que ele deixa de aparecer da listagem da unidade.

**Acceptance Scenarios**:

1. **Given** um lançamento cadastrado com valor "R$ 350,00", **When** a usuária edita o valor para "R$ 370,00" e salva, **Then** o lançamento passa a exibir o novo valor na listagem.
2. **Given** um lançamento cadastrado, **When** a usuária tenta salvar a edição com valor zero, negativo ou vazio, **Then** o sistema rejeita a alteração e indica que o valor deve ser positivo.
3. **Given** um lançamento cadastrado, **When** a usuária confirma a remoção desse lançamento, **Then** ele deixa de aparecer na listagem da unidade correspondente.
4. **Given** uma tentativa de editar ou remover um lançamento que já não existe mais (ex.: removido em outra sessão), **When** a ação é realizada, **Then** o sistema informa que o lançamento não foi encontrado.
5. **Given** um lançamento cadastrado por engano para a unidade "Bloco A - 101", **When** a usuária edita o lançamento para associá-lo à unidade correta "Bloco A - 102" (já cadastrada) e salva, **Then** o lançamento passa a aparecer na listagem de lançamentos de "Bloco A - 102" e deixa de aparecer na de "Bloco A - 101".
6. **Given** um lançamento cadastrado, **When** a usuária tenta editá-lo associando-o a uma unidade inexistente, **Then** o sistema rejeita a alteração e informa que a unidade não foi encontrada.

---

### User Story 5 - Registrar pagamento de um lançamento (Priority: P2)

Como responsável pela gestão do condomínio, quero registrar que um lançamento de conta a receber já foi pago — seja no momento em que eu lanço a conta a receber, seja depois, sobre um lançamento já existente — informando a data em que o pagamento ocorreu, para conseguir diferenciar lançamentos pendentes de lançamentos já quitados sem precisar de dois passos separados quando já sei que o pagamento já ocorreu.

**Why this priority**: É o pré-requisito direto para a futura visualização de condôminos devedores e saldos pendentes (citada no `CLAUDE.md` como núcleo do produto) — sem essa informação, a listagem de lançamentos não diz o que ainda está em aberto. Prioridade equivalente à listagem (P2): o sistema já entrega valor com apenas lançamento e listagem (User Stories 1-3), mas fica bem mais útil assim que o pagamento pode ser marcado.

**Independent Test**: Pode ser testada isoladamente — com um lançamento já criado (User Story 1) — registrando seu pagamento com uma data e confirmando que ele passa a aparecer como pago na listagem, com a data exibida.

**Acceptance Scenarios**:

1. **Given** uma unidade cadastrada, **When** a usuária lança uma conta a receber (individual ou em lote) já informando a data de pagamento no próprio formulário de lançamento, **Then** o lançamento é criado diretamente como pago, sem exigir uma segunda ação separada para registrar o pagamento.
2. **Given** um lançamento pendente, **When** a usuária registra o pagamento informando a data "15/08/2026", **Then** o lançamento passa a aparecer como pago na listagem, exibindo essa data de pagamento.
3. **Given** o registro de pagamento de um lançamento, **When** a usuária tenta confirmar sem informar a data de pagamento, **Then** o sistema rejeita e indica que a data é obrigatória.
4. **Given** um lançamento já marcado como pago, **When** a usuária registra o pagamento novamente informando uma nova data, **Then** a data de pagamento é atualizada para o novo valor (não há ação de "desfazer pagamento" nesta versão — ver Assumptions).
5. **Given** um lançamento já marcado como pago, **When** a usuária edita valor, vencimento, descrição, conta destino, tipo ou unidade desse lançamento, ou o remove, **Then** a operação é permitida normalmente, sem nenhum bloqueio adicional por causa do pagamento (FR-017).
6. **Given** uma tentativa de registrar pagamento de um lançamento que já não existe mais, **When** a ação é realizada, **Then** o sistema informa que o lançamento não foi encontrado.

---

### Edge Cases

- O que acontece se a usuária tentar lançar uma conta a receber com data de vencimento no passado? O sistema deve permitir (ex.: lançamento retroativo de uma taxa em atraso), sem bloquear por essa razão.
- O que acontece se a usuária tentar lançar em lote para todas as unidades, mas uma unidade for cadastrada depois desse lançamento? O sistema não deve gerar retroativamente um lançamento para a unidade nova — o lote afeta somente as unidades existentes no momento da ação.
- O que acontece se a usuária remover uma unidade (feature 001) que já possui lançamentos de contas a receber? O sistema deve bloquear a remoção da unidade, exibindo mensagem de erro informando que há lançamentos vinculados (ver FR-012).
- O que acontece se a usuária tentar lançar duas contas a receber idênticas (mesmo valor, vencimento e descrição) para a mesma unidade? Deve ser permitido — não há regra de unicidade entre lançamentos, pois é válido ter mais de uma cobrança com os mesmos dados por engano ou por necessidade real (ex.: duas taxas iguais em meses diferentes lançadas juntas).
- O que acontece se a usuária quiser desfazer um pagamento registrado por engano? Fora de escopo nesta rodada — não existe ação de estorno; a alternativa hoje é remover o lançamento e recriá-lo, ou registrar o pagamento novamente com a data correta se o erro for só na data (ver melhoria futura registrada no README).
- O que acontece se a usuária selecionar vários lançamentos na listagem e pedir a remoção em lote, mas um deles não puder ser removido? O sistema remove os que conseguir (melhor esforço), reaproveitando a mesma regra de remoção individual (FR-009/FR-010) para cada item, e informa ao final quais foram removidos e quais falharam, com o motivo de cada falha.
- O que acontece se a usuária combinar filtros contraditórios (ex.: "vencidos" e "pagos" ao mesmo tempo)? O sistema aplica todos os filtros informados (E lógico) e, nesse caso específico, retorna uma lista vazia — nenhum lançamento pago é considerado vencido (FR-021).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir lançar uma conta a receber para uma unidade específica, informando valor (obrigatório, positivo), data de vencimento (obrigatória), descrição (obrigatória), conta destino (obrigatória) e tipo (representado como caixa de seleção "Recorrente", com valor padrão desmarcado — equivalente a "Extra" — quando a usuária não a marcar) como campos do lançamento.
- **FR-002**: O sistema MUST vincular todo lançamento de conta a receber a uma unidade já cadastrada (feature 001); não é possível lançar uma conta a receber sem uma unidade associada.
- **FR-003**: O sistema MUST validar que o valor informado seja maior que zero, rejeitando valores zero, negativos ou não numéricos antes de salvar o lançamento. **Regra substituída pela feature 003** (`specs/003-accounts-payable-suppliers/spec.md`, FR-008) — zero passou a ser um valor aceito (uso como lembrete antes de saber o valor exato); só valores negativos continuam rejeitados.
- **FR-004**: O sistema MUST permitir lançar a mesma conta a receber (mesmo valor, vencimento, descrição, conta destino e tipo) para todas as unidades cadastradas em uma única ação, criando um lançamento independente por unidade.
- **FR-005**: O sistema MUST considerar, no lançamento em lote (FR-004), apenas as unidades cadastradas no momento da ação; unidades cadastradas posteriormente MUST NOT receber retroativamente um lançamento já confirmado.
- **FR-006**: O sistema MUST exibir a listagem de lançamentos de contas a receber de uma unidade, mostrando valor, data de vencimento, descrição, conta destino e tipo de cada lançamento.
- **FR-007**: O sistema MUST indicar de forma clara quando uma unidade não possuir nenhum lançamento de conta a receber em sua listagem.
- **FR-008**: O sistema MUST permitir editar valor, data de vencimento, descrição, conta destino, tipo e unidade associada de um lançamento já criado, respeitando a mesma validação de valor positivo (FR-003) e de unidade existente (FR-002).
- **FR-009**: O sistema MUST permitir remover um lançamento de conta a receber, mediante confirmação explícita da usuária antes da exclusão definitiva.
- **FR-010**: O sistema MUST informar a usuária quando uma operação de edição ou remoção for tentada sobre um lançamento que não existe (ou não existe mais).
- **FR-011**: O sistema MUST orientar a usuária a cadastrar ao menos uma unidade antes de permitir o lançamento de uma conta a receber (individual ou em lote), quando nenhuma unidade estiver cadastrada.
- **FR-012**: O sistema MUST impedir a remoção de uma unidade (feature 001) que possua ao menos um lançamento de conta a receber vinculado, exibindo mensagem de erro explicando o motivo — ampliando a regra de bloqueio de remoção de unidade já existente (FR-006 da feature 001), que hoje considera apenas condôminos vinculados.
- **FR-013**: O sistema MUST permitir selecionar, no lançamento (individual ou em lote), uma conta destino obrigatória entre um conjunto fixo de opções: "Piscina", "Jardim Piscina" e "Jardim Lateral", rejeitando o lançamento se nenhuma for selecionada.
- **FR-014**: O sistema MUST permitir classificar o lançamento (individual ou em lote) como "Recorrente" através de uma única caixa de seleção (checkbox) na interface, desmarcada por padrão (equivalente a "Extra" quando não marcada) — representado internamente como um valor booleano `isRecorrente`, destinado a uso futuro em filtragens; esta feature não exige nenhum comportamento funcional diferente entre os dois tipos além do próprio armazenamento do campo.
- **FR-015**: O sistema MUST permitir registrar o pagamento de um lançamento informando a data em que o pagamento ocorreu (obrigatória nessa ação), seja no momento da criação (individual ou em lote, como campo opcional do próprio lançamento) seja depois, sobre um lançamento já existente (ação dedicada), marcando-o como pago em ambos os casos.
- **FR-016**: O sistema MUST permitir atualizar a data de pagamento de um lançamento já marcado como pago, ao registrar o pagamento novamente sobre ele; não há, nesta versão, uma ação dedicada para desfazer ("estornar") um pagamento já registrado.
- **FR-017**: O sistema MUST permitir editar ou remover um lançamento já marcado como pago com as mesmas regras aplicadas a um lançamento pendente (FR-008/FR-009), sem nenhum bloqueio adicional decorrente do pagamento.
- **FR-018**: O sistema MUST exibir, na listagem de lançamentos, se cada um está pago ou pendente, e a data de pagamento quando pago. Não existe um campo "pago" independente: um lançamento é considerado pago quando (e somente quando) sua data de pagamento estiver preenchida.
- **FR-019**: O sistema MUST permitir selecionar múltiplos lançamentos na listagem e removê-los em uma única ação, aplicando a mesma regra de remoção individual (FR-009/FR-010) a cada um — em caso de falha em algum item, o sistema remove os demais (melhor esforço) e informa quais falharam e por quê.
- **FR-020**: O sistema MUST permitir filtrar a listagem de lançamentos por status de pagamento (somente pagos ou somente pendentes).
- **FR-021**: O sistema MUST permitir filtrar a listagem de lançamentos por vencidos — um lançamento é considerado vencido quando está pendente (sem data de pagamento) e sua data de vencimento é anterior à data atual; um lançamento pago não é considerado vencido, mesmo que o pagamento tenha ocorrido depois do vencimento.
- **FR-022**: O sistema MUST permitir filtrar a listagem de lançamentos por mês e ano de vencimento.
- **FR-023**: O sistema MUST permitir filtrar a listagem de lançamentos por mês e ano de pagamento (aplicável apenas a lançamentos pagos).
- **FR-024**: Toda listagem que reaproveita o trio de seleção múltipla desta feature (`list-selection.ts`/`bulk-delete.ts`/`bulk-actions-bar`, ver FR-019) MUST destacar visualmente cada linha selecionada, de forma consistente entre todas elas — não um comportamento específico de uma listagem isolada. Requisito adicionado retroativamente nesta feature (dona do trio) a partir de um destaque equivalente introduzido apenas em `account-list` pela feature 007; a aplicação retroativa às demais listagens (`unit-list`/`resident-list` históricos, hoje `party-list`, `fund-list`, `group-list`) está registrada como impacto cruzado nos `tasks.md` das features que hoje possuem cada uma dessas telas (ver plan.md).

### Key Entities

- **Lançamento de Conta a Receber**: Representa um valor devido por uma unidade do condomínio. Atributos: valor (obrigatório, positivo), data de vencimento (obrigatória), descrição (obrigatória, texto livre, ex.: "Taxa condominial - Agosto/2026"), conta destino (obrigatória, uma entre "Piscina", "Jardim Piscina" ou "Jardim Lateral"), tipo (booleano `isRecorrente`: `true` para recorrente, `false` para extra — representado na interface como caixa de seleção "Recorrente", desmarcada por padrão) e data de pagamento (opcional; quando preenchida, o lançamento é considerado pago — não existe um campo "pago" independente, ver FR-018), unidade associada (obrigatória, referência a uma unidade cadastrada na feature 001). Uma unidade pode ter zero ou mais lançamentos. Um lançamento em lote para todas as unidades gera um registro independente por unidade — não existe uma entidade separada de "lote"; cada lançamento resultante é igual a um lançamento individual em todos os aspectos (edição, remoção e registro de pagamento não afetam os demais).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A usuária consegue lançar uma conta a receber em lote para todas as unidades do condomínio em menos de 1 minuto, independentemente do número de unidades cadastradas.
- **SC-002**: 100% das tentativas de lançamento (individual ou em lote) com valor zero, negativo ou vazio são bloqueadas pelo sistema, sem exceção.
- **SC-003**: Um lançamento em lote para N unidades cadastradas resulta em exatamente N lançamentos, um por unidade, verificável na listagem de cada unidade imediatamente após a confirmação.
- **SC-004**: A usuária consegue localizar os lançamentos de uma unidade específica em menos de 10 segundos, sem precisar de treinamento prévio.
- **SC-005**: 100% dos lançamentos removidos deixam de aparecer na listagem da unidade correspondente imediatamente após a confirmação da remoção.
- **SC-006**: A usuária consegue registrar o pagamento de um lançamento e ver seu status atualizado (pago, com data) na listagem em menos de 10 segundos.

## Assumptions

- Esta feature cobre o **lançamento** (criação, listagem, edição e remoção) e o **registro de pagamento/quitação** de contas a receber (User Story 5); não há, nesta versão, ação de estorno/"desfazer pagamento" nem controle de pagamento parcial — registrar pagamento novamente sobre um lançamento já pago só atualiza a data (ver Clarifications, sessão de correção 2026-07-26). A geração automática de cobrança a partir de um pagamento recebido fora do sistema (ex.: conciliação bancária) segue fora de escopo.
- Internamente (persistência e contrato JSON da API), `dueDate` e a nova `paymentDate` usam o formato ISO-8601 padrão do `LocalDate` (`yyyy-MM-dd`); a conversão para o formato de exibição brasileiro (DD/MM/AAAA) é responsabilidade exclusiva do frontend, usando os recursos nativos do Angular (`DatePipe` para exibição, `<input type="date">` para entrada) — sem utilitário de conversão customizado — decisão revista nesta rodada de correções, substituindo a decisão original desta feature (ver `.specify/memory/constitution.md`, Princípio IV, e `research.md`).
- A remoção em lote de lançamentos (FR-019) reaproveita o endpoint `DELETE /api/receivables/{id}` já existente, chamado individualmente para cada item selecionado pelo frontend (sem endpoint transacional novo); a interface de seleção múltipla e a barra de ação em lote são fornecidas por um componente/utilitário compartilhado (ver plan.md), também aplicado retroativamente às listagens de unidades e condôminos da feature 001 (ver `specs/001-cadastro-condominos/`).
- Não existe um campo `paid` independente no modelo: o status "pago"/"pendente" é sempre derivado da presença (ou ausência) de `paymentDate`, para evitar os dois ficarem inconsistentes entre si (ver Clarifications, sessão de correção 2026-07-26, parte 2).
- Os filtros de listagem (FR-020 a FR-023) são combináveis entre si e com o filtro por unidade já existente (FR-006) — todos aplicados em conjunto (E lógico) quando informados simultaneamente; dado o volume pequeno de registros (poucas dezenas), a filtragem é feita em memória no backend, sem necessidade de índices ou consultas otimizadas dedicadas (ver research.md).
- A geração recorrente/automática de lançamentos mês a mês está fora do escopo desta feature — é tratada pela funcionalidade futura de "cobranças e pagamentos recorrentes" mencionada no README do produto; nesta feature, tanto o lançamento individual quanto o em lote são ações manuais disparadas pela usuária.
- "Conta destino" é uma lista fixa e fechada de opções ("Piscina", "Jardim Piscina", "Jardim Lateral") definida nesta feature, não um cadastro dinâmico gerenciável pela usuária; incluir uma nova conta destino no futuro exigirá alteração explícita da especificação e do código.
- O campo "Tipo" é modelado como booleano (`isRecorrente`) em vez de enum nomeado, por decisão explícita da usuária — hoje há apenas dois valores possíveis (recorrente/extra) e a finalidade é exclusivamente filtragem futura, sem comportamento funcional distinto entre os dois tipos nesta feature.
- O sistema é de uso pessoal, com poucas dezenas de unidades, portanto a ação em lote e a listagem não precisam de paginação nesta primeira versão.
- O FR-012 desta feature amplia uma regra de negócio já implementada na feature 001 (bloqueio de remoção de unidade). Por já haver código implementado para a feature 001, essa alteração MUST seguir o fluxo de "Edição de Features Já Implementadas" da constituição do projeto ao ser planejada/implementada, em vez de tratar a feature 001 como não afetada.
