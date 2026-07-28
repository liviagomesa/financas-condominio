# Feature Specification: Cadastro de Condôminos e Unidades

**Feature Branch**: `001-cadastro-condominos`

**Created**: 2026-07-24

**Status**: Draft

**Input**: User description: "Cadastro de condôminos: permitir criar, editar, listar e remover condôminos, com nome, unidade, e-mail e telefone de contato. Cada condômino pertence a uma unidade única do condomínio. E-mail e telefone são nullable." + revisão: "Vai ser necessário um cadastro de unidades também. Podem haver vários condôminos em uma unidade."

## Status Pós-Feature 003

**Atualização (feature 003 — `specs/003-accounts-payable-suppliers/`)**: o cadastro de
condôminos (Cadastro de Condôminos) foi **removido por completo** do produto (ver FR-016 de
003). As seções abaixo que descrevem essa funcionalidade (User Stories 2 e 5, a parte de
condômino da User Story 4, os Edge Cases específicos de condômino, e as partes de FR-007 a
FR-017 relativas a condômino) permanecem no arquivo como **registro histórico**, marcadas
como removidas — não foram apagadas, para preservar o racional original desta feature. O
comportamento atual do produto está em `specs/003-accounts-payable-suppliers/spec.md`.

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

### User Story 2 - Cadastrar novo condômino em uma unidade (Priority: P1) — REMOVIDA (feature 003)

> **Removida por completo pela feature 003** (`specs/003-accounts-payable-suppliers/`,
> FR-016) — o cadastro de condôminos não existe mais no produto. Texto original preservado
> como registro histórico.

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

> **Parcialmente removida pela feature 003**: a parte de editar condômino (Acceptance
> Scenarios 1, 2 e 4 abaixo) não se aplica mais — condômino não existe no produto (ver FR-016
> de 003). A parte de editar identificador de unidade (Acceptance Scenario 3) continua válida.

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

### User Story 5 - Remover condômino (Priority: P3) — REMOVIDA (feature 003)

> **Removida por completo pela feature 003** (`specs/003-accounts-payable-suppliers/`,
> FR-016) — o cadastro de condôminos não existe mais no produto. Texto original preservado
> como registro histórico.

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
3. **Given** vários condôminos cadastrados, **When** a usuária seleciona mais de um na
   listagem e aciona "Remover selecionados", **Then** todos os selecionados são removidos em
   uma única ação, mediante confirmação explícita (ver FR-018, introduzido pela feature 002).

---

### User Story 6 - Remover unidade (Priority: P3)

> **Atualizada pela feature 003**: os Acceptance Scenarios 2 e 3 abaixo foram reescritos —
> "condômino vinculado" foi substituído por "conta ou fornecedor vinculado", já que o
> conceito de condômino não existe mais no produto (ver FR-017 de 003). Texto original
> preservado no histórico do Git; esta é a versão vigente.

Como responsável pela gestão do condomínio, quero remover uma unidade que não existe mais ou
que foi cadastrada por engano, para manter o cadastro de unidades correto.

**Why this priority**: É uma operação de manutenção pouco frequente; além disso, depende da
regra de proteção contra remoção de unidades com contas ou fornecedores vinculados, o que a
torna a operação de maior risco entre as descritas nesta feature.

**Independent Test**: Pode ser testada isoladamente cadastrando uma unidade sem contas ou
fornecedores vinculados, removendo-a e confirmando que ela deixa de aparecer na listagem; e,
separadamente, tentando remover uma unidade com conta ou fornecedor vinculado e confirmando o
bloqueio.

**Acceptance Scenarios**:

1. **Given** uma unidade cadastrada sem nenhuma conta ou fornecedor associado, **When** a
   usuária confirma a remoção dessa unidade, **Then** ela deixa de aparecer na listagem de
   unidades.
2. **Given** uma unidade cadastrada com pelo menos uma conta ou um fornecedor associado,
   **When** a usuária tenta remover essa unidade, **Then** o sistema rejeita a remoção e exibe
   mensagem informando que a unidade possui contas ou fornecedores vinculados.
3. **Given** várias unidades cadastradas, algumas com contas ou fornecedores vinculados e
   outras sem, **When** a usuária seleciona todas na listagem e aciona "Remover
   selecionados", **Then** o sistema remove as unidades sem vínculo e mantém as demais,
   informando quais falharam e o motivo (ver FR-018, introduzido pela feature 002).

---

### Edge Cases

> **Atualizado pela feature 003**: os edge cases específicos de condômino (e-mail inválido,
> nome duplicado, cadastro sem unidade, formato de telefone) foram removidos — condômino não
> existe mais no produto. O de identificador de unidade duplicado, e o de remoção de registro
> inexistente (reescrito para falar só de unidade), continuam válidos.

- O que acontece se a usuária tentar remover uma unidade que já não existe mais (ex.: removida
  em outra sessão)? O sistema deve informar que o registro não foi encontrado.
- O que acontece se a usuária tentar cadastrar uma unidade com identificador igual a outro já cadastrado, mas com diferença de maiúsculas/minúsculas ou espaços nas pontas (ex.: "bloco a - 101 " vs "Bloco A - 101")? O sistema deve tratar como duplicado e rejeitar o cadastro.

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
- **FR-006**: O sistema MUST impedir a remoção de uma unidade que possua ao menos uma conta
  (a pagar ou a receber) ou ao menos um fornecedor vinculado, exibindo mensagem de erro
  explicando o motivo. **Redação vigente, atualizada pela feature 003** (`specs/003-accounts-
  payable-suppliers/spec.md`, FR-017) — a redação original desta feature considerava
  condôminos vinculados; essa parte foi removida junto com o cadastro de condôminos. A
  extensão feita pela feature 002 (bloqueio também por lançamento de conta a receber
  vinculado) permanece incorporada, agora generalizada para "conta" (feature 003).
- **FR-007 a FR-013, FR-015, FR-017** (cadastro, edição, listagem, validação de e-mail/telefone
  e remoção de condômino) — **REMOVIDOS pela feature 003**: o cadastro de condôminos não
  existe mais no produto (ver FR-016 de 003). Texto original preservado no histórico do Git.
- **FR-014**: O sistema MUST indicar de forma clara quando não houver nenhuma unidade
  cadastrada em sua listagem. **Redação vigente, atualizada pela feature 003** — a redação
  original também cobria a listagem de condôminos, removida junto com o cadastro.
- **FR-016**: O sistema MUST informar a usuária quando uma operação de edição ou remoção for
  tentada sobre uma unidade que não existe (ou não existe mais). **Redação vigente, atualizada
  pela feature 003** — a redação original também cobria condômino, removido.
- **FR-018**: O sistema MUST permitir selecionar múltiplas unidades na listagem e removê-las em
  uma única ação, aplicando a mesma regra de remoção individual (FR-005/FR-006) a cada item
  selecionado — em caso de falha em algum item (ex.: unidade com conta ou fornecedor
  vinculado), o sistema remove os demais (melhor esforço) e informa quais falharam e por quê.
  Regra introduzida pela feature 002 (`specs/002-receivable-charges/spec.md`, FR-019).
  **Redação vigente, atualizada pela feature 003** — a redação original também cobria a
  listagem de condôminos, removida junto com o cadastro.

### Key Entities

- **Unidade**: Representa uma unidade do condomínio (ex.: apartamento). Atributos: identificador (obrigatório, único entre as unidades cadastradas, comparado de forma normalizada — sem diferenciar maiúsculas/minúsculas e sem espaços extras nas pontas). **Redação vigente, atualizada pela feature 003**: pode ter zero ou mais contas (a pagar ou a receber, feature 003) e zero ou mais fornecedores (feature 003) associados; não tem mais condôminos associados (cadastro removido).
- ~~**Condômino**~~: **Removida pela feature 003** (`specs/003-accounts-payable-suppliers/spec.md`, FR-016). Descrição original preservada como histórico: representava uma pessoa associada a uma unidade do condomínio, com nome (obrigatório), unidade associada (obrigatória), e-mail (opcional) e telefone de contato (opcional, formato brasileiro).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A usuária consegue cadastrar uma nova unidade em menos de 1 minuto. **Redação
  vigente, atualizada pela feature 003** — a redação original também envolvia cadastrar um
  condômino em conjunto, removido do produto.
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
  mesmo identificador). **Removido pela feature 003**: a ressalva original sobre o condômino
  (compartilhamento de unidade sem restrição de unicidade) não se aplica mais — condômino não
  existe no produto.
- **Removido pela feature 003**: a regra original de que "uma unidade só pode ser removida
  quando não possuir nenhum condômino associado" não se aplica mais — ver FR-006 atualizado
  (bloqueio agora por conta ou fornecedor vinculado).
- Como este é o primeiro cadastro do sistema, ainda não existem outras entidades (contas a
  receber, cobranças) dependentes de unidade ou condômino; o comportamento de remoção quando
  houver vínculos com outras funcionalidades será tratado quando essas features forem
  especificadas. **Atualização (feature 002)**: a feature de lançamentos de contas a receber
  já especificou esse vínculo — ver FR-006 atualizado acima. **Atualização (feature 003)**:
  generalizado para "conta" (a pagar ou a receber) e estendido a fornecedor — ver FR-006/
  FR-017 atualizados acima.
- O sistema é de uso pessoal, com poucas dezenas de unidades, portanto as listagens não
  precisam de paginação ou busca avançada nesta primeira versão.
- **Atualização (feature 002)**: a feature de lançamentos de contas a receber introduziu
  seleção múltipla + remoção em lote (best-effort) nas listagens; a mesma capacidade foi
  estendida às listagens desta feature (unidades e condôminos) reaproveitando o mesmo
  componente compartilhado — ver FR-018 atualizado acima. **Removido pela feature 003**: a
  parte referente a condôminos não se aplica mais (cadastro removido); a capacidade continua
  válida para a listagem de unidades.
- **Atualização (feature 003)**: o cadastro de condôminos (User Stories 2 e 5, e as partes
  correspondentes de FR-007 a FR-017) foi removido por completo do produto — ver
  `specs/003-accounts-payable-suppliers/spec.md`, FR-016. Em seu lugar, a feature 003
  introduziu o cadastro de fornecedores (`specs/003-accounts-payable-suppliers/`), sem
  relação estrutural com o antigo condômino além do padrão de pacote já convencionado.
