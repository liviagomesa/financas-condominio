# Feature Specification: Contas a Pagar, Fornecedores e Unificação de Contas

**Feature Branch**: `003-accounts-payable-suppliers`

**Created**: 2026-07-27

**Status**: Draft

**Input**: User description: "Quero criar no meu projeto a feature 'contas a pagar'. Já temos as
contas a receber (002). Na mesma interface/tela, quero ver todas as contas, com opção de filtro
(pagamento/recebimento) e diferença de cor da linha (ex.: verde para receber e vermelho para
pagar). Apague a entidade de condôminos e tudo que se refere a ela, pois isso não será usado. No
lugar, crie a entidade 'fornecedor', que pode ou não pertencer a uma unidade. Transformar contas a
receber em apenas 'contas': pagamento tem destinatário e recebimento tem 'remetente' (unificar em
um campo). Adicionar campo de 'observações' nullable na conta."

## Clarifications

### Session 2026-07-27

- Q: O campo "conta destino" (hoje uma lista fixa — Piscina, Jardim Piscina, Jardim Lateral —
  obrigatória em toda conta a receber) deve continuar existindo na conta unificada, e se sim,
  também se aplica a contas a pagar? → A: Obrigatório para os dois tipos — toda conta, a pagar
  ou a receber, informa um dos três fundos, indicando de/para qual fundo o dinheiro sai ou
  entra. Renomeado para "fundo" nesta feature, para não colidir com o nome da nova entidade
  unificada "Conta" (ver FR-022).
- Q: Agora que o campo "fundo" passa a ser obrigatório também em contas a pagar, a lista fixa de
  três opções deve ser expandida com fundos mais genéricos (ex.: "Administração",
  "Manutenção"), ou os três fundos atuais já cobrem qualquer conta a pagar? → A: Manter os três
  fundos atuais (Piscina, Jardim Piscina, Jardim Lateral) sem expandir a lista — toda despesa do
  condomínio é classificada em um desses três, inclusive contas a pagar a fornecedores.

### Sessão de correção 2026-07-27

- Q: O fornecedor deve ter algum campo adicional para facilitar o pagamento a ele? → A: Sim —
  adicionar campo "chave PIX", opcional, no cadastro de fornecedor (ver FR-023).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar fornecedor (Priority: P1)

Como responsável pela gestão do condomínio, quero cadastrar um fornecedor informando nome e,
opcionalmente, a unidade a que ele está vinculado (quando o pagamento se refere a uma unidade
específica, e não a um prestador de serviço externo), para poder lançar contas a pagar
associadas a ele.

**Why this priority**: É pré-requisito para lançar qualquer conta a pagar — sem fornecedor
cadastrado, não há a quem associar uma conta a pagar. Equivalente, para contas a pagar, ao papel
que o cadastro de unidade tem para contas a receber.

**Independent Test**: Pode ser testada isoladamente cadastrando um fornecedor (com e sem
unidade vinculada) e confirmando que ele aparece na listagem de fornecedores.

**Acceptance Scenarios**:

1. **Given** nenhum fornecedor cadastrado, **When** a usuária cadastra um fornecedor com nome
   "Empresa de Limpeza XYZ" sem selecionar unidade, **Then** o fornecedor é criado com sucesso e
   aparece na listagem, sem unidade vinculada.
2. **Given** a unidade "Bloco A - 101" cadastrada, **When** a usuária cadastra um fornecedor com
   nome "Síndico Bloco A - 101" vinculado a essa unidade, **Then** o fornecedor é criado com
   sucesso e aparece na listagem exibindo a unidade vinculada.
3. **Given** um formulário de cadastro de fornecedor sem nome preenchido, **When** a usuária
   tenta confirmar o cadastro, **Then** o sistema rejeita o cadastro e indica que o nome é
   obrigatório.
4. **Given** o cadastro de um fornecedor, **When** a usuária informa uma chave PIX (ex.: um
   CNPJ, e-mail ou chave aleatória) junto com o nome, **Then** o fornecedor é criado com sucesso
   e a chave PIX fica salva e visível ao consultar o fornecedor.
5. **Given** o cadastro de um fornecedor, **When** a usuária confirma o cadastro sem informar
   chave PIX, **Then** o fornecedor é criado normalmente, com o campo chave PIX vazio.

---

### User Story 2 - Lançar conta a pagar para um fornecedor (Priority: P1)

Como responsável pela gestão do condomínio, quero lançar uma conta a pagar para um fornecedor,
informando valor, vencimento, descrição e, opcionalmente, observações, para registrar valores
que o condomínio deve pagar (ex.: serviço de limpeza, manutenção).

**Why this priority**: É a funcionalidade central desta feature — sem lançar contas a pagar não
há controle nenhum de saídas de dinheiro do condomínio.

**Independent Test**: Pode ser testada isoladamente — com ao menos um fornecedor já cadastrado
(User Story 1) — lançando uma conta a pagar para esse fornecedor e confirmando que ela aparece
na listagem unificada de contas, marcada como "a pagar".

**Acceptance Scenarios**:

1. **Given** o fornecedor "Empresa de Limpeza XYZ" cadastrado, **When** a usuária lança uma
   conta a pagar com valor "R$ 400,00", vencimento "05/08/2026", descrição "Limpeza - Agosto/2026"
   e fundo "Jardim Lateral" para esse fornecedor, **Then** a conta é criada com sucesso e
   aparece na listagem unificada, identificada visualmente como "a pagar" e associada ao
   fornecedor e ao fundo corretos.
2. **Given** um formulário de conta a pagar sem valor, sem vencimento, sem descrição, sem fundo
   ou sem fornecedor selecionado, **When** a usuária tenta confirmar, **Then** o sistema rejeita
   e indica quais campos obrigatórios estão faltando.
3. **Given** um formulário de conta a pagar com valor zero ou negativo, **When** a usuária tenta
   confirmar, **Then** o sistema rejeita e indica que o valor deve ser positivo.
4. **Given** nenhum fornecedor cadastrado no sistema, **When** a usuária tenta lançar uma conta a
   pagar, **Then** o sistema orienta a cadastrar um fornecedor primeiro.
5. **Given** o lançamento de uma conta a pagar, **When** a usuária preenche o campo
   "observações" com o texto "Disse que vai pagar mês que vem", **Then** o texto é salvo junto
   com a conta e exibido posteriormente ao consultá-la.

---

### User Story 3 - Ver todas as contas (a pagar e a receber) na mesma listagem (Priority: P1)

Como responsável pela gestão do condomínio, quero visualizar, na mesma tela, todas as contas —
tanto as que tenho a receber das unidades quanto as que tenho a pagar a fornecedores —
diferenciadas visualmente por cor conforme o tipo, para ter uma visão financeira completa sem
precisar alternar entre telas separadas.

**Why this priority**: É o segundo pilar central desta feature junto com o lançamento de contas
a pagar — sem a listagem unificada, o valor de ter os dois tipos no mesmo sistema fica limitado,
pois a usuária continuaria precisando consultar duas telas separadas.

**Independent Test**: Pode ser testada isoladamente — com ao menos uma conta a receber e uma
conta a pagar já lançadas — acessando a listagem única e confirmando que ambas aparecem, cada
uma com a marcação visual (cor) e o rótulo de tipo correspondentes.

**Acceptance Scenarios**:

1. **Given** uma conta a receber e uma conta a pagar já lançadas, **When** a usuária acessa a
   listagem de contas, **Then** ambas aparecem na mesma lista, a conta a receber com uma cor
   (ex.: tom esverdeado) e a conta a pagar com outra (ex.: tom avermelhado), cada uma também
   exibindo um rótulo textual do tipo (não dependendo só da cor para diferenciar).
2. **Given** contas de ambos os tipos cadastradas, **When** a usuária aplica o filtro "somente a
   pagar" (ou "somente a receber"), **Then** apenas as contas do tipo selecionado aparecem na
   listagem.
3. **Given** contas de ambos os tipos cadastradas, **When** a usuária combina o filtro de tipo
   com outro filtro já existente (ex.: status de pagamento, vencidos, mês de vencimento ou de
   pagamento), **Then** o sistema aplica todos os filtros informados em conjunto (E lógico).
4. **Given** nenhuma conta cadastrada, **When** a usuária acessa a listagem, **Then** o sistema
   exibe uma indicação de que não há contas cadastradas.

---

### User Story 4 - Registrar pagamento de uma conta a pagar (Priority: P2)

Como responsável pela gestão do condomínio, quero registrar que uma conta a pagar já foi paga —
seja no momento do lançamento, seja depois, sobre uma conta já existente — informando a data em
que o pagamento ocorreu, para diferenciar o que ainda está em aberto do que já foi quitado, da
mesma forma que já funciona para contas a receber.

**Why this priority**: É o mesmo pré-requisito que já existe hoje para contas a receber (feature
002) — sem essa informação, a listagem não diz o que ainda está pendente de pagamento a
fornecedores. Prioridade equivalente à da funcionalidade análoga já existente.

**Independent Test**: Pode ser testada isoladamente — com uma conta a pagar já criada (User
Story 2) — registrando seu pagamento com uma data e confirmando que ela passa a aparecer como
paga na listagem.

**Acceptance Scenarios**:

1. **Given** uma conta a pagar pendente, **When** a usuária registra o pagamento informando a
   data "10/08/2026", **Then** a conta passa a aparecer como paga na listagem, exibindo essa
   data.
2. **Given** o registro de pagamento de uma conta a pagar, **When** a usuária tenta confirmar
   sem informar a data, **Then** o sistema rejeita e indica que a data é obrigatória.
3. **Given** uma conta a pagar com vencimento no passado e ainda sem data de pagamento, **When**
   a usuária filtra a listagem por "vencidos", **Then** essa conta aparece no resultado, com a
   mesma lógica já aplicada a contas a receber vencidas.

---

### User Story 5 - Editar e remover contas e fornecedores (Priority: P3)

Como responsável pela gestão do condomínio, quero editar ou remover uma conta (a pagar ou a
receber) ou um fornecedor já cadastrados, para corrigir dados informados incorretamente ou
desfazer um lançamento feito por engano.

**Why this priority**: É uma operação de correção/manutenção, útil mas menos frequente que criar
e listar — o sistema já é utilizável apenas com criação, listagem e registro de pagamento.

**Independent Test**: Pode ser testada isoladamente editando uma conta existente e confirmando
que a listagem reflete os novos valores; e, separadamente, removendo uma conta ou um fornecedor
e confirmando que deixam de aparecer nas respectivas listagens.

**Acceptance Scenarios**:

1. **Given** uma conta a pagar cadastrada, **When** a usuária edita o valor, vencimento,
   descrição, observações ou o fornecedor associado (para outro fornecedor já cadastrado) e
   salva, **Then** a listagem passa a exibir os novos valores.
2. **Given** uma conta cadastrada (a pagar ou a receber), **When** a usuária confirma sua
   remoção, **Then** ela deixa de aparecer na listagem.
3. **Given** um fornecedor sem nenhuma conta a pagar vinculada, **When** a usuária confirma sua
   remoção, **Then** ele deixa de aparecer na listagem de fornecedores.
4. **Given** um fornecedor com ao menos uma conta a pagar vinculada, **When** a usuária tenta
   removê-lo, **Then** o sistema rejeita a remoção e exibe mensagem informando que há contas
   vinculadas — mesmo padrão já aplicado a unidades (feature 001, FR-006) e ampliado a
   lançamentos (feature 002, FR-012).
5. **Given** uma tentativa de editar ou remover uma conta ou fornecedor que já não existe mais,
   **When** a ação é realizada, **Then** o sistema informa que o registro não foi encontrado.
6. **Given** uma conta a pagar ou a receber já cadastrada, **When** a usuária tenta alterar seu
   tipo (de "a pagar" para "a receber", ou vice-versa), **Then** o sistema não permite essa
   alteração — o tipo é definido na criação e é imutável; para mudar o tipo, a conta precisa ser
   removida e recriada.

---

### Edge Cases

- O que acontece se a usuária tentar cadastrar um fornecedor sem vincular a nenhuma unidade? O
  sistema deve permitir normalmente — a unidade é opcional (ex.: prestadores de serviço externos
  ao condomínio, sem relação com uma unidade específica).
- O que acontece se a usuária tentar remover uma unidade (feature 001) vinculada a um
  fornecedor? O sistema deve bloquear a remoção da unidade, exibindo mensagem de erro, mesmo que
  o fornecedor não tenha nenhuma conta a pagar vinculada no momento — o vínculo em si já impede a
  remoção, seguindo o mesmo padrão de proteção contra remoção de registros referenciados já
  aplicado no restante do sistema.
- O que acontece se a usuária tentar lançar uma conta a pagar em lote, para todos os fornecedores
  de uma vez, como já existe para contas a receber e todas as unidades? Essa ação não está
  disponível para contas a pagar nesta versão — não faz sentido geral pagar o mesmo valor a
  fornecedores diferentes; contas a pagar são sempre lançadas individualmente.
- O que acontece se a usuária combinar o filtro de tipo ("a pagar") com o filtro de mês de
  pagamento? Os filtros se combinam normalmente (E lógico), retornando apenas contas a pagar já
  pagas no mês informado.
- O que acontece com as contas a receber já lançadas antes desta feature (feature 002)? Todas
  continuam existindo, agora como contas do tipo "a receber" dentro do conceito unificado de
  "conta", sem perda de dados.
- O que acontece com o cadastro de condôminos (feature 001) e seus dados já existentes? O
  cadastro de condôminos deixa de existir como funcionalidade do produto — telas, rotas e regras
  de negócio relacionadas são removidas por completo (ver FR-016).
- O que acontece com a regra que hoje bloqueia a remoção de uma unidade com condôminos vinculados
  (feature 001, FR-006)? Como o conceito de condômino deixa de existir, essa parte da regra é
  removida; a remoção de unidade passa a ser bloqueada apenas por contas ou fornecedores
  vinculados (ver FR-017).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir cadastrar um fornecedor informando nome (obrigatório) e,
  opcionalmente, uma unidade já cadastrada à qual ele está vinculado.
- **FR-002**: O sistema MUST permitir editar nome e unidade vinculada de um fornecedor já
  cadastrado, incluindo vincular, trocar ou remover a unidade associada.
- **FR-003**: O sistema MUST exibir a listagem de todos os fornecedores cadastrados, mostrando
  nome e unidade vinculada (quando houver).
- **FR-004**: O sistema MUST indicar de forma clara quando não houver nenhum fornecedor
  cadastrado na listagem.
- **FR-005**: O sistema MUST permitir remover um fornecedor cadastrado, mediante confirmação
  explícita, impedindo a remoção quando houver ao menos uma conta a pagar vinculada a ele e
  exibindo mensagem de erro explicando o motivo.
- **FR-006**: O sistema MUST unificar o conceito de "lançamento de conta a receber" (feature 002)
  em uma única entidade "Conta", com um atributo de tipo que define se é "a receber" ou "a
  pagar", definido na criação e imutável após isso.
- **FR-007**: O sistema MUST vincular toda conta a uma contraparte obrigatória: uma unidade já
  cadastrada quando a conta for do tipo "a receber" (mesma regra da feature 002, FR-002), ou um
  fornecedor já cadastrado quando for do tipo "a pagar".
- **FR-008**: O sistema MUST validar que o valor informado em qualquer conta (a pagar ou a
  receber) seja maior que zero, rejeitando valores zero, negativos ou não numéricos.
- **FR-009**: O sistema MUST manter a ação de lançamento em lote "para todas as unidades" (feature
  002, FR-004) exclusiva para contas do tipo "a receber"; contas a pagar MUST ser sempre
  lançadas individualmente, uma por fornecedor.
- **FR-010**: O sistema MUST exibir uma listagem única contendo todas as contas, tanto a pagar
  quanto a receber, sem telas separadas por tipo.
- **FR-011**: O sistema MUST diferenciar visualmente, na listagem unificada, contas a pagar de
  contas a receber por meio de cor de linha (ex.: tom esverdeado para a receber, tom avermelhado
  para a pagar) e MUST também exibir um rótulo textual do tipo em cada linha, para que a
  diferenciação não dependa exclusivamente da cor.
- **FR-012**: O sistema MUST permitir filtrar a listagem unificada por tipo de conta (somente a
  pagar, somente a receber, ou ambos), combinável (E lógico) com os filtros já existentes da
  feature 002 (status de pagamento, vencidos, mês de vencimento, mês de pagamento).
- **FR-013**: O sistema MUST permitir registrar o pagamento (data em que ocorreu) de qualquer
  conta, a pagar ou a receber, com as mesmas regras já existentes na feature 002 (FR-015/FR-016):
  opcionalmente no momento da criação, ou depois por ação dedicada, atualizável a qualquer
  momento.
- **FR-014**: O sistema MUST considerar uma conta a pagar "vencida" com a mesma lógica já
  aplicada a contas a receber (feature 002, FR-021): pendente (sem data de pagamento) e com
  vencimento anterior à data atual.
- **FR-015**: O sistema MUST permitir editar e remover qualquer conta (a pagar ou a receber) com
  as mesmas regras já existentes na feature 002 (FR-008/FR-009/FR-017), incluindo trocar a
  contraparte para outra do mesmo tipo (outra unidade, ou outro fornecedor), mas MUST NOT
  permitir alterar o tipo de uma conta já criada.
- **FR-016**: O sistema MUST remover completamente a funcionalidade de cadastro de condôminos
  (entidade, telas, rotas e regras de negócio da feature 001 relacionadas a ela), sem manter
  nenhum resquício funcional no produto.
- **FR-017**: O sistema MUST atualizar a regra de bloqueio de remoção de unidade (feature 001,
  FR-006) para impedir a remoção quando houver ao menos uma conta vinculada (a pagar ou a
  receber) ou ao menos um fornecedor vinculado à unidade — removendo, da regra, a parte
  referente a condôminos vinculados, que deixam de existir.
- **FR-018**: O sistema MUST adicionar um campo "observações" de texto livre, opcional
  (nullable), a toda conta (a pagar ou a receber), para registro de anotações (ex.: "disse que
  vai pagar mês que vem"), exibido e editável junto aos demais campos.
- **FR-019**: O sistema MUST manter a classificação "Recorrente" (isRecorrente, feature 002,
  FR-014) disponível para contas de ambos os tipos.
- **FR-020**: O sistema MUST permitir selecionar múltiplas contas na listagem unificada e
  removê-las em uma única ação, reaproveitando a mesma regra de remoção em lote (melhor esforço)
  já existente na feature 002 (FR-019), independentemente do tipo de cada conta selecionada.
- **FR-021**: O sistema MUST orientar a usuária a cadastrar um fornecedor antes de permitir
  lançar uma conta a pagar, quando nenhum fornecedor estiver cadastrado.
- **FR-022**: O sistema MUST manter o campo "fundo" (renomeado nesta feature a partir de "conta
  destino", feature 002 FR-013 — lista fixa "Piscina", "Jardim Piscina", "Jardim Lateral") como
  obrigatório em toda conta, independentemente do tipo — indicando de/para qual fundo o valor
  entra ou sai —, rejeitando o lançamento (a pagar ou a receber) se nenhum for selecionado.
- **FR-023**: O sistema MUST permitir informar uma chave PIX (opcional, nullable) no cadastro de
  um fornecedor, como texto livre (ex.: CPF/CNPJ, e-mail, telefone ou chave aleatória, sem
  validação de formato específica), exibida e editável junto aos demais campos do fornecedor,
  para facilitar o pagamento a ele.

### Key Entities

- **Conta**: Generalização do antigo "Lançamento de Conta a Receber" (feature 002), agora
  representando tanto valores a receber de unidades quanto valores a pagar a fornecedores.
  Atributos: tipo (obrigatório, "a receber" ou "a pagar", definido na criação e imutável), valor
  (obrigatório, positivo), data de vencimento (obrigatória), descrição (obrigatória),
  contraparte (obrigatória — referência a uma unidade quando "a receber", ou a um fornecedor
  quando "a pagar"), fundo (obrigatório, uma entre "Piscina", "Jardim Piscina" ou "Jardim
  Lateral" — renomeado nesta feature a partir do antigo "conta destino" da feature 002,
  aplicável a ambos os tipos de conta, ver FR-022), tipo de recorrência (booleano
  `isRecorrente`, herdado da feature 002), data de pagamento (opcional; quando preenchida, a
  conta é considerada paga), observações (opcional, novo campo desta feature, texto livre).
  Substitui e estende a entidade equivalente da feature 002.
- **Fornecedor**: Nova entidade. Representa quem recebe um pagamento do condomínio (ex.:
  prestador de serviço) ou, alternativamente, uma unidade específica quando o pagamento se
  refere a ela. Atributos: nome (obrigatório), unidade vinculada (opcional, referência a uma
  unidade cadastrada na feature 001), chave PIX (opcional, texto livre, sem validação de formato
  específica — ver FR-023). Pode ter zero ou mais contas a pagar associadas.
- **Unidade**: Sem alteração estrutural (feature 001); passa a poder ser referenciada também por
  fornecedores, além de por contas do tipo "a receber". Sua regra de bloqueio de remoção é
  atualizada (ver FR-017).
- ~~**Condômino**~~: Removida por completo nesta feature (ver FR-016); deixa de existir como
  entidade do produto.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A usuária consegue visualizar contas a pagar e a receber na mesma tela e identificar
  o tipo de cada uma (por cor e rótulo) em até 5 segundos, sem precisar abrir cada conta
  individualmente.
- **SC-002**: A usuária consegue filtrar a listagem para ver somente contas a pagar (ou somente a
  receber) em menos de 10 segundos.
- **SC-003**: A usuária consegue cadastrar um fornecedor e lançar uma conta a pagar para ele em
  menos de 2 minutos.
- **SC-004**: 100% das tentativas de lançar conta a pagar sem nenhum fornecedor cadastrado são
  bloqueadas, com orientação clara para cadastrar um fornecedor primeiro.
- **SC-005**: 100% das tentativas de remoção de fornecedor ou unidade com vínculos (contas ou,
  no caso de unidade, fornecedores) são bloqueadas pelo sistema, sem exceção.
- **SC-006**: Após a implementação, nenhuma tela, rota ou registro relacionado a condôminos
  permanece acessível no sistema.
- **SC-007**: A usuária consegue registrar uma observação em uma conta e revê-la posteriormente
  sem perda de conteúdo em 100% dos casos.

## Assumptions

- Esta feature substitui e estende a feature 002 (`specs/002-receivable-charges/`), generalizando
  sua entidade de "lançamento de conta a receber" para "conta" (com suporte a ambos os sentidos).
  A feature 002 e a feature 001 (`specs/001-cadastro-condominos/`) possuem código já implementado
  impactado por esta mudança; a atualização de seus artefatos (spec, plan, tasks) MUST seguir o
  fluxo "Edição de Features Já Implementadas" da constituição do projeto durante o planejamento.
- O tipo da conta (a pagar / a receber) é definido no momento da criação e não pode ser alterado
  depois — mudar o tipo de uma conta existente exige removê-la e recriar uma nova com o tipo e a
  contraparte corretos (ver FR-006/FR-015).
- O campo de contraparte é conceitualmente único (toda conta tem exatamente uma contraparte): a
  interface exibe um único seletor, cujas opções (unidades ou fornecedores) mudam conforme o
  tipo de conta selecionado. A modelagem técnica exata desse relacionamento (schema/banco de
  dados) é decisão de implementação, a ser detalhada no `plan.md`, e não restringe este
  requisito de negócio.
- Fornecedor não possui, nesta rodada, campos de contato (e-mail/telefone) — apenas nome e
  unidade opcional — podendo ser estendido no futuro se necessário, seguindo o princípio de
  simplicidade da constituição (evitar campos não solicitados).
- Não há regra de unicidade sobre o nome do fornecedor (o mesmo nome pode se repetir), assim
  como já não havia para condômino na feature 001.
- A chave PIX do fornecedor (FR-023) é armazenada como texto livre, sem validação de formato
  específica — uma chave PIX pode ser um CPF, CNPJ, e-mail, telefone ou uma chave aleatória, e
  distinguir esses formatos para validação não foi solicitado e agregaria complexidade
  desnecessária nesta rodada.
- A ação de lançamento em lote "para todas as unidades" (feature 002, FR-004) continua exclusiva
  de contas a receber; não há equivalente "para todos os fornecedores" nesta versão, pois não
  faz sentido de negócio pagar o mesmo valor a fornecedores diferentes de uma só vez.
- A funcionalidade futura de cálculo automático de saldo líquido por unidade (contas a pagar de
  fornecedores vinculados a uma unidade menos contas a receber dessa mesma unidade), mencionada
  na descrição desta feature, está fora de escopo desta rodada — fica registrada como melhoria
  futura no README do projeto.
- As contas a receber já lançadas antes desta feature (feature 002) são preservadas, passando a
  ser tratadas como contas do tipo "a receber" dentro do conceito unificado, sem perda de dados;
  os detalhes técnicos dessa migração ficam a cargo do `plan.md`.
- Cores sugeridas para a listagem unificada: tom esverdeado para contas a receber, tom
  avermelhado para contas a pagar — sempre acompanhadas de um rótulo textual do tipo, para que a
  distinção não dependa exclusivamente de cor (acessibilidade).
- O sistema é de uso pessoal, com poucas dezenas de registros, portanto a listagem unificada e o
  cadastro de fornecedores não precisam de paginação nesta primeira versão (mesma premissa das
  features 001 e 002).
