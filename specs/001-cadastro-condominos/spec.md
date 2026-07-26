# Feature Specification: Cadastro de Condôminos e Unidades

**Feature Branch**: `001-cadastro-condominos`

**Created**: 2026-07-24

**Status**: Draft

**Input**: User description: "Cadastro de condôminos: permitir criar, editar, listar e remover condôminos, com nome, unidade, e-mail e telefone de contato. Cada condômino pertence a uma unidade única do condomínio. E-mail e telefone são nullable." + revisão: "Vai ser necessário um cadastro de unidades também. Podem haver vários condôminos em uma unidade."

## Clarifications

### Session 2026-07-24

- Q: Ao verificar se o identificador de uma unidade já existe (regra de unicidade do FR-002/SC-002), como a comparação deve ser feita? → A: Normalizada — ignora maiúsculas/minúsculas e espaços extras nas pontas antes de comparar.
- Q: O campo telefone de contato do condômino (FR-008) deve ter alguma validação de formato antes de salvar? → A: Formato brasileiro estrito — exige DDD + número, com quantidade fixa de dígitos, rejeitando qualquer valor fora desse padrão.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar unidade (Priority: P1)

Como responsável pela gestão do condomínio, quero cadastrar as unidades existentes no
condomínio (ex.: "Bloco A - 101"), para que os condôminos possam ser associados a uma
unidade real ao serem cadastrados.

**Why this priority**: É pré-requisito para o cadastro de condôminos — sem unidades
cadastradas, não há a quem associar um condômino.

**Independent Test**: Pode ser testada isoladamente cadastrando uma unidade e confirmando que
ela passa a aparecer na listagem de unidades, sem depender de nenhum condômino existir.

**Acceptance Scenarios**:

1. **Given** nenhuma unidade cadastrada, **When** a usuária cadastra uma unidade com
   identificador "Bloco A - 101", **Then** a unidade é criada com sucesso e aparece na
   listagem de unidades.
2. **Given** uma unidade já cadastrada com identificador "Bloco A - 101", **When** a usuária
   tenta cadastrar outra unidade com o mesmo identificador "Bloco A - 101", **Then** o sistema
   rejeita o cadastro e exibe mensagem informando que a unidade já existe.
3. **Given** um formulário de cadastro de unidade sem identificador preenchido, **When** a
   usuária tenta confirmar o cadastro, **Then** o sistema rejeita o cadastro e indica que o
   identificador é obrigatório.

---

### User Story 2 - Cadastrar novo condômino em uma unidade (Priority: P1)

Como responsável pela gestão do condomínio, quero cadastrar um novo condômino informando nome
e associando-o a uma unidade já cadastrada (e opcionalmente e-mail e telefone), para manter um
registro completo de quem mora em cada unidade e poder associá-lo a cobranças futuras.

**Why this priority**: Sem o cadastro de condôminos, nenhuma outra funcionalidade do sistema
(lançamentos, cobranças, saldos) tem a quem se referir. É a base de todas as demais features
do produto.

**Independent Test**: Pode ser testada isoladamente — com ao menos uma unidade já cadastrada —
preenchendo o formulário de cadastro de condômino com nome e unidade válidos e confirmando que
o condômino passa a aparecer na listagem.

**Acceptance Scenarios**:

1. **Given** a unidade "Bloco A - 101" cadastrada e sem nenhum condômino associado, **When** a
   usuária cadastra um condômino com nome "Maria Silva" associado à unidade "Bloco A - 101",
   **Then** o condômino é criado com sucesso e aparece na listagem.
2. **Given** a unidade "Bloco A - 101" já com um condômino associado, **When** a usuária
   cadastra um segundo condômino também associado à unidade "Bloco A - 101", **Then** o
   cadastro é aceito normalmente, pois uma unidade pode ter vários condôminos.
3. **Given** um formulário de cadastro preenchido apenas com nome e unidade, **When** a usuária
   confirma o cadastro sem informar e-mail e telefone, **Then** o condômino é criado com sucesso
   e os campos e-mail e telefone ficam vazios.
4. **Given** um formulário de cadastro sem nome ou sem unidade selecionados, **When** a usuária
   tenta confirmar o cadastro, **Then** o sistema rejeita o cadastro e indica quais campos
   obrigatórios estão faltando.
5. **Given** nenhuma unidade cadastrada no sistema, **When** a usuária tenta cadastrar um
   condômino, **Then** o sistema indica que é necessário cadastrar uma unidade antes de
   associar um condômino a ela.

---

### User Story 3 - Listar unidades e condôminos cadastrados (Priority: P1)

Como responsável pela gestão do condomínio, quero visualizar a lista de unidades e a lista de
condôminos cadastrados com seus dados principais, para ter uma visão geral de quem está
registrado no sistema e em qual unidade.

**Why this priority**: A listagem é o ponto de entrada para consultar, editar ou remover
qualquer unidade ou condômino; sem ela, as demais operações não têm como ser localizadas na
interface.

**Independent Test**: Pode ser testada isoladamente cadastrando algumas unidades e condôminos e
verificando que todos aparecem em suas respectivas listagens, com o vínculo entre condômino e
unidade visível.

**Acceptance Scenarios**:

1. **Given** duas unidades cadastradas, **When** a usuária acessa a listagem de unidades,
   **Then** as duas unidades aparecem, cada uma com seu identificador.
2. **Given** três condôminos cadastrados em duas unidades diferentes, **When** a usuária acessa
   a listagem de condôminos, **Then** os três condôminos aparecem, cada um exibindo nome,
   unidade associada, e-mail e telefone (quando preenchidos).
3. **Given** nenhuma unidade ou nenhum condômino cadastrado, **When** a usuária acessa a
   respectiva listagem, **Then** o sistema exibe uma indicação de que não há registros
   cadastrados.

---

### User Story 4 - Editar dados de unidade e de condômino (Priority: P2)

Como responsável pela gestão do condomínio, quero editar os dados de uma unidade (identificador)
ou de um condômino já cadastrado (nome, unidade associada, e-mail, telefone), para manter as
informações atualizadas quando houver mudança de contato, correção de cadastro ou realocação de
um condômino para outra unidade.

**Why this priority**: Dados de contato e vínculos mudam com frequência; sem edição, o cadastro
rapidamente fica desatualizado, mas o sistema ainda é utilizável só com criação e listagem.

**Independent Test**: Pode ser testada isoladamente abrindo uma unidade ou um condômino já
existente, alterando um campo e confirmando que a listagem reflete o novo valor.

**Acceptance Scenarios**:

1. **Given** um condômino cadastrado com telefone "(11) 90000-0000", **When** a usuária edita
   o telefone para "(11) 91111-1111" e salva, **Then** o condômino passa a exibir o novo
   telefone.
2. **Given** um condômino associado à unidade "Bloco A - 101", **When** a usuária edita o
   condômino para associá-lo à unidade "Bloco A - 102" (já cadastrada), **Then** o condômino
   passa a aparecer associado à unidade "Bloco A - 102".
3. **Given** uma unidade cadastrada com identificador "Bloco A - 101", **When** a usuária edita
   o identificador para um valor já usado por outra unidade, **Then** o sistema rejeita a
   alteração e exibe mensagem informando que o identificador já existe.
4. **Given** um condômino cadastrado, **When** a usuária edita o cadastro removendo o valor do
   nome (deixando-o vazio) e tenta salvar, **Then** o sistema rejeita a alteração e indica que
   o nome é obrigatório.

---

### User Story 5 - Remover condômino (Priority: P3)

Como responsável pela gestão do condomínio, quero remover um condômino que não mora mais no
condomínio, para manter o cadastro limpo e sem registros obsoletos.

**Why this priority**: É uma operação de manutenção do cadastro, útil mas menos frequente e
menos crítica que criar, editar e listar — o sistema continua funcional mesmo que a remoção
seja implementada por último.

**Independent Test**: Pode ser testada isoladamente removendo um condômino cadastrado e
confirmando que ele deixa de aparecer na listagem, sem afetar a unidade à qual pertencia.

**Acceptance Scenarios**:

1. **Given** um condômino cadastrado, **When** a usuária confirma a remoção desse condômino,
   **Then** ele deixa de aparecer na listagem e a unidade à qual pertencia permanece cadastrada.
2. **Given** uma solicitação de remoção de condômino, **When** a usuária inicia a ação,
   **Then** o sistema pede confirmação antes de remover definitivamente o registro.

---

### User Story 6 - Remover unidade (Priority: P3)

Como responsável pela gestão do condomínio, quero remover uma unidade que não existe mais ou
que foi cadastrada por engano, para manter o cadastro de unidades correto.

**Why this priority**: Assim como a remoção de condômino, é uma operação de manutenção pouco
frequente; além disso, depende da regra de proteção contra remoção de unidades com condôminos
vinculados, o que a torna a operação de maior risco entre as descritas nesta feature.

**Independent Test**: Pode ser testada isoladamente cadastrando uma unidade sem condôminos
vinculados, removendo-a e confirmando que ela deixa de aparecer na listagem; e, separadamente,
tentando remover uma unidade com condôminos vinculados e confirmando o bloqueio.

**Acceptance Scenarios**:

1. **Given** uma unidade cadastrada sem nenhum condômino associado, **When** a usuária confirma
   a remoção dessa unidade, **Then** ela deixa de aparecer na listagem de unidades.
2. **Given** uma unidade cadastrada com pelo menos um condômino associado, **When** a usuária
   tenta remover essa unidade, **Then** o sistema rejeita a remoção e exibe mensagem informando
   que a unidade possui condôminos vinculados.

---

### Edge Cases

- O que acontece se a usuária tentar cadastrar ou editar um condômino informando um e-mail em
  formato inválido (ex.: sem "@")? O sistema deve rejeitar e indicar o formato esperado.
- O que acontece se a usuária tentar cadastrar dois condôminos com o mesmo nome, na mesma
  unidade ou em unidades diferentes? Deve ser permitido, já que não há restrição de unicidade
  sobre o nome do condômino.
- O que acontece se a usuária tentar remover um condômino ou uma unidade que já não existe mais
  (ex.: removido em outra sessão)? O sistema deve informar que o registro não foi encontrado.
- O que acontece se a usuária tentar cadastrar um condômino sem selecionar nenhuma unidade
  existente porque nenhuma foi cadastrada ainda? O sistema deve orientar a cadastrar uma
  unidade primeiro (ver User Story 1).
- O que acontece se a usuária tentar cadastrar uma unidade com identificador igual a outro já cadastrado, mas com diferença de maiúsculas/minúsculas ou espaços nas pontas (ex.: "bloco a - 101 " vs "Bloco A - 101")? O sistema deve tratar como duplicado e rejeitar o cadastro.
- O que acontece se a usuária informar um telefone fora do formato brasileiro esperado (ex.: faltando DDD, com letras, ou com quantidade de dígitos incorreta)? O sistema deve rejeitar e indicar o formato esperado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir cadastrar uma nova unidade informando um identificador
  único (ex.: "Bloco A - 101") como campo obrigatório.
- **FR-002**: O sistema MUST impedir o cadastro ou a edição de uma unidade com identificador já usado por outra unidade, exibindo mensagem de erro explicando o motivo. A comparação MUST ser normalizada (ignorando diferenças de maiúsculas/minúsculas e espaços extras nas pontas) antes de considerar dois identificadores como duplicados.
- **FR-003**: O sistema MUST exibir a listagem de todas as unidades cadastradas.
- **FR-004**: O sistema MUST permitir editar o identificador de uma unidade já cadastrada,
  respeitando a regra de unicidade (FR-002).
- **FR-005**: O sistema MUST permitir remover uma unidade cadastrada, mediante confirmação
  explícita da usuária antes da exclusão definitiva.
- **FR-006**: O sistema MUST impedir a remoção de uma unidade que possua ao menos um condômino
  associado, exibindo mensagem de erro explicando o motivo. Esta regra foi estendida pela
  feature 002 (`specs/002-receivable-charges/spec.md`, FR-012): a remoção também MUST ser
  impedida quando a unidade possuir ao menos um lançamento de conta a receber vinculado.
- **FR-007**: O sistema MUST permitir cadastrar um novo condômino informando nome (obrigatório)
  e associando-o a uma unidade já cadastrada (obrigatório).
- **FR-008**: O sistema MUST permitir informar e-mail e telefone de contato como campos
  opcionais no cadastro de um condômino.
- **FR-009**: O sistema MUST permitir que uma mesma unidade tenha vários condôminos associados
  simultaneamente.
- **FR-010**: O sistema MUST permitir editar nome, unidade associada, e-mail e telefone de um
  condômino já cadastrado.
- **FR-011**: O sistema MUST validar que nome e unidade associada nunca fiquem vazios após
  criação ou edição de um condômino.
- **FR-012**: O sistema MUST validar o formato do e-mail informado, quando preenchido, antes de
  salvar o cadastro do condômino.
- **FR-013**: O sistema MUST exibir a listagem de todos os condôminos cadastrados, mostrando
  nome, unidade associada, e-mail e telefone de cada um.
- **FR-014**: O sistema MUST indicar de forma clara quando não houver nenhuma unidade ou nenhum
  condômino cadastrado em sua respectiva listagem.
- **FR-015**: O sistema MUST permitir remover um condômino cadastrado, mediante confirmação
  explícita da usuária antes da exclusão definitiva.
- **FR-016**: O sistema MUST informar a usuária quando uma operação de edição ou remoção for
  tentada sobre uma unidade ou condômino que não existe (ou não existe mais).
- **FR-017**: O sistema MUST validar que o telefone informado, quando preenchido, siga o formato brasileiro (DDD com 2 dígitos seguido do número com 8 ou 9 dígitos), rejeitando valores fora desse padrão antes de salvar o cadastro do condômino.

### Key Entities

- **Unidade**: Representa uma unidade do condomínio (ex.: apartamento). Atributos: identificador (obrigatório, único entre as unidades cadastradas, comparado de forma normalizada — sem diferenciar maiúsculas/minúsculas e sem espaços extras nas pontas). Pode ter zero ou mais condôminos associados.
- **Condômino**: Representa uma pessoa associada a uma unidade do condomínio. Atributos: nome (obrigatório), unidade associada (obrigatória, referência a uma unidade cadastrada), e-mail (opcional), telefone de contato (opcional, formato brasileiro DDD + número quando preenchido). Várias pessoas podem estar associadas à mesma unidade.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A usuária consegue cadastrar uma nova unidade e um novo condômino, em conjunto,
  em menos de 2 minutos.
- **SC-002**: 100% das tentativas de cadastro ou edição de unidade com identificador duplicado (considerando variações de maiúsculas/minúsculas e espaços nas pontas) são bloqueadas pelo sistema, sem exceção.
- **SC-003**: 100% das tentativas de remoção de unidade com condôminos vinculados são
  bloqueadas pelo sistema, sem exceção.
- **SC-004**: A usuária consegue localizar qualquer unidade ou condômino cadastrado na
  listagem em menos de 10 segundos, sem precisar de treinamento prévio.
- **SC-005**: 100% dos condôminos e unidades removidos deixam de aparecer em sua respectiva
  listagem imediatamente após a confirmação da remoção.

## Assumptions

- A "unidade" é representada como um identificador textual livre (ex.: "Bloco A - 101" ou
  "Apto 202"), sem estrutura fixa de bloco/número exigida por este cadastro.
- A unicidade é validada sobre o identificador da unidade (não pode haver duas unidades com o
  mesmo identificador); o mesmo não se aplica ao condômino, que pode compartilhar unidade com
  outros condôminos.
- Uma unidade só pode ser removida quando não possuir nenhum condômino associado; para remover
  uma unidade com condôminos, é necessário primeiro remover ou realocar esses condôminos.
- Como este é o primeiro cadastro do sistema, ainda não existem outras entidades (contas a
  receber, cobranças) dependentes de unidade ou condômino; o comportamento de remoção quando
  houver vínculos com outras funcionalidades será tratado quando essas features forem
  especificadas. **Atualização (feature 002)**: a feature de lançamentos de contas a receber
  já especificou esse vínculo — ver FR-006 atualizado acima.
- O sistema é de uso pessoal, com poucas dezenas de unidades, portanto as listagens não
  precisam de paginação ou busca avançada nesta primeira versão.
