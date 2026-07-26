# Feature Specification: Lançamentos de Contas a Receber

**Feature Branch**: `002-receivable-charges`

**Created**: 2026-07-26

**Status**: Draft

**Input**: User description: "Lançamentos de contas a receber (por unidade, com opção de adicionar para todas simultaneamente)"

## Clarifications

### Session 2026-07-26

- Q: Quando a usuária tentar remover uma unidade (feature 001) que já possui lançamentos de contas a receber vinculados, o que deve acontecer? → A: Bloquear a remoção, exibindo mensagem de erro — mesma lógica já aplicada hoje para condôminos vinculados (FR-006 da feature 001).
- Q: O campo "Tipo" do lançamento (recorrente/extra) deve ser modelado como enum nomeado ou como boolean? → A: Boolean (`isRecorrente`): `true` representa recorrente, `false` representa extra.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lançar conta a receber para uma unidade (Priority: P1)

Como responsável pela gestão do condomínio, quero lançar uma conta a receber para uma unidade
específica, informando valor, data de vencimento, descrição, conta destino e tipo (recorrente ou
extra), para registrar valores que essa unidade deve pagar (ex.: taxa condominial do mês, taxa
extra) já classificados para consulta e filtragem futuras.

**Why this priority**: É a funcionalidade central desta feature — sem lançar contas a receber
por unidade, não há registro nenhum de valores devidos, o que impede qualquer controle
financeiro do condomínio.

**Independent Test**: Pode ser testada isoladamente — com ao menos uma unidade já cadastrada
(feature 001) — lançando uma conta a receber para essa unidade e confirmando que ela aparece na
listagem de lançamentos daquela unidade.

**Acceptance Scenarios**:

1. **Given** a unidade "Bloco A - 101" cadastrada e sem nenhum lançamento, **When** a usuária
   lança uma conta a receber com valor "R$ 350,00", vencimento "10/08/2026", descrição "Taxa
   condominial - Agosto/2026", conta destino "Piscina" e tipo "Recorrente" para essa unidade,
   **Then** o lançamento é criado com sucesso e aparece na listagem de lançamentos da unidade,
   exibindo também a conta destino e o tipo escolhidos.
2. **Given** um formulário de lançamento sem valor, sem vencimento, sem unidade, sem conta
   destino ou sem tipo selecionados, **When** a usuária tenta confirmar o lançamento, **Then** o
   sistema rejeita o lançamento e indica quais campos obrigatórios estão faltando.
3. **Given** um formulário de lançamento com valor igual a zero ou negativo, **When** a usuária
   tenta confirmar o lançamento, **Then** o sistema rejeita o lançamento e indica que o valor
   deve ser positivo.
4. **Given** nenhuma unidade cadastrada no sistema, **When** a usuária tenta lançar uma conta a
   receber, **Then** o sistema orienta a cadastrar uma unidade primeiro.

---

### User Story 2 - Lançar a mesma conta a receber para todas as unidades simultaneamente (Priority: P1)

Como responsável pela gestão do condomínio, quero lançar uma conta a receber com o mesmo valor,
vencimento, descrição, conta destino e tipo para todas as unidades de uma só vez, para agilizar o
lançamento mensal da taxa condominial sem precisar repetir a operação unidade por unidade.

**Why this priority**: É o principal ganho de eficiência mencionado na descrição da feature — o
uso mensal mais comum é lançar a mesma taxa para todas as unidades, e repetir manualmente essa
ação para cada unidade seria o principal ponto de atrito do produto.

**Independent Test**: Pode ser testada isoladamente — com pelo menos duas unidades cadastradas —
disparando a ação de lançamento em lote com um valor, vencimento e descrição, e confirmando que
um lançamento correspondente aparece para cada unidade cadastrada.

**Acceptance Scenarios**:

1. **Given** três unidades cadastradas, **When** a usuária lança uma conta a receber "para todas
   as unidades" com valor "R$ 350,00", vencimento "10/08/2026", descrição "Taxa condominial -
   Agosto/2026", conta destino "Piscina" e tipo "Recorrente", **Then** um lançamento independente
   é criado para cada uma das três unidades, todos com o mesmo valor, vencimento, descrição,
   conta destino e tipo.
2. **Given** nenhuma unidade cadastrada no sistema, **When** a usuária tenta usar a ação "para
   todas as unidades", **Then** o sistema orienta a cadastrar ao menos uma unidade primeiro.
3. **Given** um lançamento em lote já confirmado para todas as unidades, **When** a usuária
   acessa a listagem de lançamentos de qualquer uma dessas unidades, **Then** o lançamento
   correspondente aparece como um registro independente, editável e removível sem afetar os
   lançamentos das demais unidades.

---

### User Story 3 - Listar lançamentos de uma unidade (Priority: P2)

Como responsável pela gestão do condomínio, quero visualizar a lista de lançamentos de contas a
receber de uma unidade, com valor, vencimento, descrição, conta destino e tipo de cada um, para
conferir o que já foi lançado antes de lançar um novo valor ou de repassar a cobrança ao
condômino.

**Why this priority**: É necessária para conferir e localizar lançamentos antes de editá-los ou
removê-los, mas o sistema já entrega valor com apenas a criação (User Stories 1 e 2) mesmo antes
de existir uma listagem dedicada.

**Independent Test**: Pode ser testada isoladamente lançando algumas contas a receber para uma
unidade e verificando que todas aparecem na listagem dessa unidade, com valor, vencimento e
descrição visíveis.

**Acceptance Scenarios**:

1. **Given** dois lançamentos criados para a unidade "Bloco A - 101", **When** a usuária acessa a
   listagem de lançamentos dessa unidade, **Then** os dois lançamentos aparecem, cada um exibindo
   valor, data de vencimento, descrição, conta destino e tipo.
2. **Given** uma unidade sem nenhum lançamento, **When** a usuária acessa a listagem de
   lançamentos dessa unidade, **Then** o sistema exibe uma indicação de que não há lançamentos
   cadastrados.

---

### User Story 4 - Editar e remover um lançamento (Priority: P3)

Como responsável pela gestão do condomínio, quero editar ou remover um lançamento de conta a
receber já criado, para corrigir um valor, vencimento, descrição, conta destino, tipo ou a
unidade associada informados incorretamente (ex.: lançamento feito na unidade errada por
engano), ou para desfazer um lançamento feito por engano.

**Why this priority**: É uma operação de correção/manutenção, útil mas menos frequente que criar
e listar lançamentos — o sistema já é utilizável apenas com criação e listagem.

**Independent Test**: Pode ser testada isoladamente editando um lançamento existente
(incluindo trocar sua unidade associada) e confirmando que a listagem reflete os novos
valores; e, separadamente, removendo um lançamento e confirmando que ele deixa de aparecer da
listagem da unidade.

**Acceptance Scenarios**:

1. **Given** um lançamento cadastrado com valor "R$ 350,00", **When** a usuária edita o valor
   para "R$ 370,00" e salva, **Then** o lançamento passa a exibir o novo valor na listagem.
2. **Given** um lançamento cadastrado, **When** a usuária tenta salvar a edição com valor zero,
   negativo ou vazio, **Then** o sistema rejeita a alteração e indica que o valor deve ser
   positivo.
3. **Given** um lançamento cadastrado, **When** a usuária confirma a remoção desse lançamento,
   **Then** ele deixa de aparecer na listagem da unidade correspondente.
4. **Given** uma tentativa de editar ou remover um lançamento que já não existe mais (ex.:
   removido em outra sessão), **When** a ação é realizada, **Then** o sistema informa que o
   lançamento não foi encontrado.
5. **Given** um lançamento cadastrado por engano para a unidade "Bloco A - 101", **When** a
   usuária edita o lançamento para associá-lo à unidade correta "Bloco A - 102" (já
   cadastrada) e salva, **Then** o lançamento passa a aparecer na listagem de lançamentos de
   "Bloco A - 102" e deixa de aparecer na de "Bloco A - 101".
6. **Given** um lançamento cadastrado, **When** a usuária tenta editá-lo associando-o a uma
   unidade inexistente, **Then** o sistema rejeita a alteração e informa que a unidade não
   foi encontrada.

---

### Edge Cases

- O que acontece se a usuária tentar lançar uma conta a receber com data de vencimento no
  passado? O sistema deve permitir (ex.: lançamento retroativo de uma taxa em atraso), sem
  bloquear por essa razão.
- O que acontece se a usuária tentar lançar em lote para todas as unidades, mas uma unidade for
  cadastrada depois desse lançamento? O sistema não deve gerar retroativamente um lançamento para
  a unidade nova — o lote afeta somente as unidades existentes no momento da ação.
- O que acontece se a usuária remover uma unidade (feature 001) que já possui lançamentos de
  contas a receber? O sistema deve bloquear a remoção da unidade, exibindo mensagem de erro
  informando que há lançamentos vinculados (ver FR-012).
- O que acontece se a usuária tentar lançar duas contas a receber idênticas (mesmo valor,
  vencimento e descrição) para a mesma unidade? Deve ser permitido — não há regra de unicidade
  entre lançamentos, pois é válido ter mais de uma cobrança com os mesmos dados por engano ou por
  necessidade real (ex.: duas taxas iguais em meses diferentes lançadas juntas).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir lançar uma conta a receber para uma unidade específica,
  informando valor (obrigatório, positivo), data de vencimento (obrigatória), descrição
  (obrigatória), conta destino (obrigatória) e tipo (obrigatório) como campos do lançamento.
- **FR-002**: O sistema MUST vincular todo lançamento de conta a receber a uma unidade já
  cadastrada (feature 001); não é possível lançar uma conta a receber sem uma unidade associada.
- **FR-003**: O sistema MUST validar que o valor informado seja maior que zero, rejeitando
  valores zero, negativos ou não numéricos antes de salvar o lançamento.
- **FR-004**: O sistema MUST permitir lançar a mesma conta a receber (mesmo valor, vencimento,
  descrição, conta destino e tipo) para todas as unidades cadastradas em uma única ação, criando
  um lançamento independente por unidade.
- **FR-005**: O sistema MUST considerar, no lançamento em lote (FR-004), apenas as unidades
  cadastradas no momento da ação; unidades cadastradas posteriormente MUST NOT receber
  retroativamente um lançamento já confirmado.
- **FR-006**: O sistema MUST exibir a listagem de lançamentos de contas a receber de uma unidade,
  mostrando valor, data de vencimento, descrição, conta destino e tipo de cada lançamento.
- **FR-007**: O sistema MUST indicar de forma clara quando uma unidade não possuir nenhum
  lançamento de conta a receber em sua listagem.
- **FR-008**: O sistema MUST permitir editar valor, data de vencimento, descrição, conta
  destino, tipo e unidade associada de um lançamento já criado, respeitando a mesma validação
  de valor positivo (FR-003) e de unidade existente (FR-002).
- **FR-009**: O sistema MUST permitir remover um lançamento de conta a receber, mediante
  confirmação explícita da usuária antes da exclusão definitiva.
- **FR-010**: O sistema MUST informar a usuária quando uma operação de edição ou remoção for
  tentada sobre um lançamento que não existe (ou não existe mais).
- **FR-011**: O sistema MUST orientar a usuária a cadastrar ao menos uma unidade antes de
  permitir o lançamento de uma conta a receber (individual ou em lote), quando nenhuma unidade
  estiver cadastrada.
- **FR-012**: O sistema MUST impedir a remoção de uma unidade (feature 001) que possua ao menos
  um lançamento de conta a receber vinculado, exibindo mensagem de erro explicando o motivo —
  ampliando a regra de bloqueio de remoção de unidade já existente (FR-006 da feature 001), que
  hoje considera apenas condôminos vinculados.
- **FR-013**: O sistema MUST permitir selecionar, no lançamento (individual ou em lote), uma
  conta destino obrigatória entre um conjunto fixo de opções: "Piscina", "Jardim Piscina" e
  "Jardim Lateral", rejeitando o lançamento se nenhuma for selecionada.
- **FR-014**: O sistema MUST permitir classificar, no lançamento (individual ou em lote), um
  tipo obrigatório como "Recorrente" ou "Extra" (representado internamente como um valor
  booleano `isRecorrente`), destinado a uso futuro em filtragens; esta feature não exige nenhum
  comportamento funcional diferente entre os dois tipos além do próprio armazenamento do campo.

### Key Entities

- **Lançamento de Conta a Receber**: Representa um valor devido por uma unidade do condomínio.
  Atributos: valor (obrigatório, positivo), data de vencimento (obrigatória), descrição
  (obrigatória, texto livre, ex.: "Taxa condominial - Agosto/2026"), conta destino (obrigatória,
  uma entre "Piscina", "Jardim Piscina" ou "Jardim Lateral"), tipo (obrigatório, booleano
  `isRecorrente`: `true` para recorrente, `false` para extra), unidade associada (obrigatória,
  referência a uma unidade cadastrada na feature 001). Uma unidade pode ter zero ou mais
  lançamentos. Um lançamento em lote para todas as unidades gera um registro independente por
  unidade — não existe uma entidade separada de "lote"; cada lançamento resultante é igual a um
  lançamento individual em todos os aspectos (edição e remoção não afetam os demais).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A usuária consegue lançar uma conta a receber em lote para todas as unidades do
  condomínio em menos de 1 minuto, independentemente do número de unidades cadastradas.
- **SC-002**: 100% das tentativas de lançamento (individual ou em lote) com valor zero, negativo
  ou vazio são bloqueadas pelo sistema, sem exceção.
- **SC-003**: Um lançamento em lote para N unidades cadastradas resulta em exatamente N
  lançamentos, um por unidade, verificável na listagem de cada unidade imediatamente após a
  confirmação.
- **SC-004**: A usuária consegue localizar os lançamentos de uma unidade específica em menos de
  10 segundos, sem precisar de treinamento prévio.
- **SC-005**: 100% dos lançamentos removidos deixam de aparecer na listagem da unidade
  correspondente imediatamente após a confirmação da remoção.

## Assumptions

- Esta feature cobre apenas o **lançamento** (criação, listagem, edição e remoção) de contas a
  receber; o registro de pagamento/quitação de um lançamento é tratado por uma feature futura,
  conforme decisão registrada nas Clarifications abaixo.
- A geração recorrente/automática de lançamentos mês a mês está fora do escopo desta feature —
  é tratada pela funcionalidade futura de "cobranças e pagamentos recorrentes" mencionada no
  README do produto; nesta feature, tanto o lançamento individual quanto o em lote são ações
  manuais disparadas pela usuária.
- "Conta destino" é uma lista fixa e fechada de opções ("Piscina", "Jardim Piscina", "Jardim
  Lateral") definida nesta feature, não um cadastro dinâmico gerenciável pela usuária; incluir
  uma nova conta destino no futuro exigirá alteração explícita da especificação e do código.
- O campo "Tipo" é modelado como booleano (`isRecorrente`) em vez de enum nomeado, por decisão
  explícita da usuária — hoje há apenas dois valores possíveis (recorrente/extra) e a finalidade
  é exclusivamente filtragem futura, sem comportamento funcional distinto entre os dois tipos
  nesta feature.
- O sistema é de uso pessoal, com poucas dezenas de unidades, portanto a ação em lote e a
  listagem não precisam de paginação nesta primeira versão.
- O FR-012 desta feature amplia uma regra de negócio já implementada na feature 001 (bloqueio de
  remoção de unidade). Por já haver código implementado para a feature 001, essa alteração
  MUST seguir o fluxo de "Edição de Features Já Implementadas" da constituição do projeto ao ser
  planejada/implementada, em vez de tratar a feature 001 como não afetada.
