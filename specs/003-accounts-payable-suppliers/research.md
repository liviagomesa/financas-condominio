# Research: Contas a Pagar, Fornecedores e Unificação de Contas

Não há nenhum `NEEDS CLARIFICATION` pendente no Technical Context: stack, versões e
ferramentas de teste são reaproveitadas integralmente das features 001/002 (Constituição,
Princípio III). As decisões abaixo cobrem pontos de design específicos desta feature —
sobretudo a generalização de `Receivable` em `Account` e a introdução de `Supplier`.

## Nome da entidade unificada e do pacote

- **Decision**: Classe `Account`, pacote `com.financas.account` (renomeado de
  `com.financas.receivable`), tabela `account` (renomeada de `receivable` via `ALTER TABLE
  ... RENAME TO`, preservando dados e `id`s existentes).
- **Rationale**: Traduz diretamente a própria escolha da usuária no spec ("podemos
  transformar contas a receber em apenas 'contas'"); `account` é o termo contábil de fato em
  inglês para um registro de "conta a pagar/receber" (accounts payable/receivable), e o
  projeto não tem nenhum outro conceito de "conta bancária" que colidiria com o nome.
- **Alternatives considered**: `Charge`/`Transaction`/`Entry` — mais genéricos, mas sem a
  correspondência direta e imediata com o termo de negócio "conta" já usado no spec e no
  domínio do produto (contabilidade de condomínio); `Receivable` mantido com um `Payable`
  irmão — rejeitado explicitamente pelo spec (FR-006: unificação em uma única entidade, não
  duas paralelas).

## Validação de valor (zero permitido)

- **Decision**: `amount` continua `BigDecimal`, mas a validação passa a rejeitar apenas
  valores negativos — zero é aceito. No DTO (`AccountRequest`/`AccountBulkRequest`), a
  anotação Bean Validation muda de `@Positive` para `@PositiveOrZero`; no `AccountService`,
  `validatePositiveAmount` (herdado de `ReceivableService`) passa a comparar com
  `amount.compareTo(BigDecimal.ZERO) < 0` em vez de `<= 0`. Mensagem de erro atualizada de "O
  valor da conta deve ser maior que zero." para "O valor da conta não pode ser negativo."
- **Rationale**: Decisão da usuária ao revisar o `plan.md` — às vezes uma conta é lançada como
  lembrete (ex.: "sei que vou pagar/receber algo desse fornecedor/unidade") antes de o valor
  exato ser conhecido; nesse caso zero é o placeholder natural, e forçar um valor positivo
  obrigaria a usuária a inventar um valor fictício só para poder salvar o registro. Essa regra
  substitui a original da feature 002 (FR-003: valor deve ser maior que zero) para toda conta,
  a pagar ou a receber.
- **Alternatives considered**: manter a regra original (`> 0`) e usar `null` para representar
  "valor ainda não definido" — rejeitada por exigir tornar `amount` nullable em todo o domínio
  (contraria FR-008, que mantém o campo obrigatório) só para representar um caso de uso que
  zero já resolve de forma mais simples.

## Modelagem da contraparte (unidade vs. fornecedor)

- **Decision**: `Account` mantém duas associações `@ManyToOne` **opcionais**:
  `unit` (nullable, FK `unit_id`) e `supplier` (nullable, FK `supplier_id`), mais um campo
  `type` (enum `AccountType`: `RECEIVABLE`, `PAYABLE`). Validação de que exatamente uma das
  duas está preenchida — a coerente com `type` — é feita em `AccountService` (regra de
  negócio cruzada entre campos, não puramente sintática, conforme Princípio I da
  constituição) **e** reforçada no banco por uma `CHECK CONSTRAINT`
  (`account_type_counterparty_check`) como camada extra de integridade.
- **Rationale**: O spec (Assumptions) já registra que a modelagem técnica do "campo de
  contraparte conceitualmente único" fica a critério do `plan.md`. JPA não tem um mecanismo
  idiomático simples de associação polimórfica entre duas entidades **não relacionadas**
  (`Unit` e `Supplier` não compartilham supertipo nem faz sentido criar um só para isso, dado
  o tamanho do projeto) sem introduzir complexidade desproporcional (tabela de "contraparte"
  genérica, herança JPA, discriminadores). Duas FKs nullable + um `CHECK` é a solução mais
  simples que garante integridade referencial real (diferente de um campo `counterpartyId`
  solto sem FK, que não validaria referência nenhuma).
- **Alternatives considered**: uma tabela `counterparty` genérica com `type` +
  `reference_id` (polimorfismo "manual") — rejeitada por adicionar uma camada de indireção
  sem FK de banco de verdade, exigindo validação 100% em código; herança JPA
  (`Unit`/`Supplier` implementando uma interface/superclasse `Counterparty`) — rejeitada por
  forçar uma reestruturação de `Unit` (feature 001) desproporcional ao ganho, e por
  `Unit`/`Supplier` não terem de fato atributos ou comportamento em comum além de "poder ser
  contraparte de uma conta".

## Imutabilidade do campo `type`

- **Decision**: `AccountRequest` (usado tanto em `POST` quanto em `PUT`) sempre inclui
  `type`. Na criação, define o tipo da conta. Na edição (`PUT /api/accounts/{id}`),
  `AccountService.update()` compara o `type` recebido com o `type` já persistido da conta; se
  forem diferentes, lança `AccountTypeChangeNotAllowedException` (400) antes de aplicar
  qualquer outra alteração.
- **Rationale**: Atende a FR-006/FR-015 (tipo definido na criação, imutável depois) sem
  precisar de dois DTOs de request diferentes (um para criar, outro para editar) — o mesmo
  formato de requisição já usado por toda a API deste projeto (`PUT` reenvia o objeto
  completo) é preservado, e a regra de imutabilidade vira uma validação de negócio explícita
  no `Service`, com mensagem clara em português.
- **Alternatives considered**: omitir `type` do request de `PUT` (o service simplesmente
  nunca o altera) — rejeitado por ser implícito demais: se a usuária (ou o frontend, por
  bug) enviasse um `type` diferente do atual, o sistema silenciosamente o ignoraria em vez de
  avisar sobre a tentativa inválida.

## Campo `fund` (renomeado de `targetAccount`/"conta destino")

- **Decision**: Enum `Fund` (renomeado de `TargetAccount`), mesmos três valores (`POOL`,
  `POOL_GARDEN`, `SIDE_GARDEN`), coluna `fund` (renomeada de `target_account` via `ALTER
  TABLE ... RENAME COLUMN`), obrigatório em toda `Account` — feature 002, FR-013,
  reafirmado por esta feature (FR-022) para os dois tipos.
- **Rationale**: A usuária confirmou na sessão de clarificação que os três fundos já cobrem
  qualquer conta, inclusive a pagar, sem necessidade de expandir a lista; o campo só precisa
  ser renomeado para não colidir com o novo nome da entidade unificada ("Conta" vs. "conta
  destino" seria confuso).
- **Alternatives considered**: manter o nome `targetAccount`/"conta destino" — rejeitado por
  colidir semanticamente com a nova entidade `Account`/"Conta".

## Entidade `Supplier` (fornecedor)

- **Decision**: Nova entidade `Supplier` (`com.financas.supplier`), com `name` (obrigatório),
  `unit` (opcional, `@ManyToOne` nullable, FK `unit_id`) e `pixKey` (opcional, `String` livre,
  sem validação de formato — FR-023). Sem regra de unicidade de nome (mesma decisão já
  aplicada a `Resident` na feature 001, e mantida aqui por decisão explícita da usuária).
- **Rationale**: Espelha exatamente os atributos definidos no spec (Key Entities); `pixKey`
  como texto livre evita a complexidade de detectar/validar os múltiplos formatos possíveis
  de chave PIX (CPF, CNPJ, e-mail, telefone, chave aleatória) — não solicitado e fora de
  escopo (ver Assumptions do spec).
- **Alternatives considered**: campos de contato adicionais (e-mail/telefone, como
  `Resident` tinha) — descartados por não terem sido solicitados (princípio de simplicidade
  da constituição); validação de formato de `pixKey` por tipo detectado — descartada pela
  mesma razão.

## Remoção completa do cadastro de condôminos

- **Decision**: Remoção total do pacote `com.financas.resident` (domain/api/infra) no
  backend, de `frontend/src/app/resident/` e `frontend/src/app/shared/models/resident.model.ts`
  / `resident.service.ts` no frontend, das rotas `/residents*`, e da tabela `resident`
  (`DROP TABLE`, migration dedicada). `UnitService.delete()` deixa de depender de
  `ResidentRepository`.
- **Rationale**: FR-016 do spec exige remoção completa, sem resquício funcional; não há
  reaproveitamento possível do código de `Resident` para `Supplier` além do padrão
  estrutural (api/domain/infra), já que os atributos e regras de negócio são diferentes o
  suficiente (unidade opcional vs. obrigatória, sem telefone/e-mail, com `pixKey`) para não
  justificar tentar generalizar as duas em uma única entidade.
- **Alternatives considered**: manter a tabela/entidade `Resident` "desativada" por
  segurança — rejeitada; o spec e a usuária foram explícitos sobre remoção completa (FR-016,
  SC-006), e o projeto está em ambiente local de uso pessoal sem dados de produção reais a
  preservar por obrigação.

## Impacto cruzado: regra de bloqueio de remoção de unidade (feature 001)

- **Decision**: `UnitService.delete()` (feature 001) passa a checar
  `accountRepository.existsByUnitId(id)` (renomeado de `receivableRepository`, mesma
  assinatura) e a nova `supplierRepository.existsByUnitId(id)`, lançando
  `UnitHasAccountsException` (renomeada de `UnitHasReceivablesException`) ou a nova
  `UnitHasSuppliersException`, respectivamente. A checagem de `ResidentRepository` é
  removida por completo (condômino deixa de existir).
- **Rationale**: Implementa FR-017 desta feature, que substitui a parte de "condôminos
  vinculados" da regra original (feature 001, FR-006) por "contas ou fornecedores
  vinculados". Tecnicamente espelha o mecanismo já existente (`existsByUnitId`), só trocando
  a fonte da checagem.
- **Processo**: por alterar o comportamento de uma feature já implementada, esta mudança
  segue a seção "Edição de Features Já Implementadas" da constituição — `specs/001-cadastro-
  condominos/spec.md` (FR-006 e User Stories 5/6), `plan.md` e `tasks.md` precisam ser
  atualizados, com resumo apresentado à usuária para aprovação explícita antes de qualquer
  edição desses arquivos ou do código correspondente. Este research registra a decisão
  técnica; a execução do processo de edição (para 001 **e** para 002, ver abaixo) fica para
  depois da aprovação (ver Completion Report deste plano).

## Impacto cruzado: feature 002 é absorvida por esta feature

- **Decision**: `specs/002-receivable-charges/` não é apagada, mas seu `spec.md`/`plan.md`
  passam a registrar que a entidade `Receivable` foi generalizada em `Account` por esta
  feature (003), com um ponteiro cruzado nos dois sentidos — mesmo padrão já usado por 002
  para registrar seu próprio impacto sobre a feature 001.
- **Rationale**: Preserva o histórico de decisões e o racional já documentado em 002
  (ex.: por que `paymentDate` sem campo `paid`, por que filtros em memória) sem duplicar
  esse conteúdo em 003; qualquer feature futura que precise entender a origem de um campo
  herdado (ex.: `recurring`, `fund`) encontra o histórico completo em 002.
- **Processo**: mesma ressalva do item anterior — atualização de `specs/002-receivable-
  charges/spec.md`/`plan.md`/`tasks.md` só ocorre após aprovação explícita da usuária,
  seguindo "Edição de Features Já Implementadas".

## Migração de dados (Flyway)

- **Decision**: Três migrations novas, em sequência: `V5__create_supplier_table.sql` (nova
  tabela `supplier`), `V6__transform_receivable_to_account.sql` (`ALTER TABLE receivable
  RENAME TO account`, renomeia `target_account`→`fund`, torna `unit_id` nullable, adiciona
  `type` — com um `DEFAULT 'RECEIVABLE'` temporário para preencher as linhas já existentes,
  removido logo em seguida para tornar obrigatório em novas inserções —, `supplier_id`,
  `observations`, e o `CHECK CONSTRAINT` de consistência tipo/contraparte),
  `V7__drop_resident_table.sql` (`DROP TABLE resident`).
- **Rationale**: `RENAME TO`/`RENAME COLUMN` preserva todos os dados e `id`s de contas a
  receber já lançadas (feature 002), atendendo à Assumption do spec de não haver perda de
  dados; o `DEFAULT` temporário em `type` é a forma padrão de popular uma coluna `NOT NULL`
  nova sem quebrar linhas existentes, sem exigir um `UPDATE` explícito separado.
- **Alternatives considered**: criar a tabela `account` do zero e copiar os dados de
  `receivable` via `INSERT INTO ... SELECT` — mais verboso e com o mesmo resultado; deletar e
  recriar em vez de `RENAME` — rejeitado por perder o histórico de auto-incremento e por ser
  desnecessariamente destrutivo quando `RENAME` resolve o mesmo problema de forma direta.

## Filtros de listagem (extensão)

- **Decision**: `GET /api/accounts` ganha dois novos query params opcionais, combináveis (E
  lógico) com os já existentes (`unitId`, `paid`, `overdue`, `dueYearMonth`,
  `paymentYearMonth`, todos herdados de 002): `type` (`RECEIVABLE`/`PAYABLE` — FR-012) e
  `supplierId` (simétrico a `unitId`, filtra contas de um fornecedor específico — não exigido
  explicitamente pelo spec, mas natural e de custo marginal zero dado que `unitId` já existe
  com o mesmo padrão).
  Mecanismo: quando `unitId` ou `supplierId` é informado (nunca os dois ao mesmo tempo, já que
  são mutuamente exclusivos por `type`), `AccountService.findAll` usa a consulta dedicada do
  repositório (`findByUnitId`/`findBySupplierId`, mesmo padrão de `ReceivableRepository.
  findByUnitId` na feature 002), lançando `NotFoundException` (404) se o valor não
  corresponder a um registro cadastrado; caso contrário, parte de `findAll()`. Os demais
  filtros (`type`, `paid`, `overdue`, `dueYearMonth`, `paymentYearMonth`) são sempre aplicados
  em memória sobre esse resultado base — mesmo padrão já estabelecido em 002 para
  `paid`/`overdue`/mês (volume pequeno de dados).
- **Rationale**: FR-012 exige o filtro por tipo; `supplierId` é adicionado por simetria
  direta com `unitId` (mesmo mecanismo, custo de implementação desprezível) e por utilidade
  óbvia (consultar contas de um fornecedor específico), sem introduzir nenhuma complexidade
  nova.
- **Alternatives considered**: expor `supplierId` só depois, em uma feature futura — descartado
  porque o custo de adicionar agora (mesmo padrão de `unitId`, já implementado) é
  desprezível comparado a reabrir o `AccountService`/`AccountController` depois.

## Frontend: listagem unificada e formulário com tipo condicional

- **Decision**: `account-list` (renomeada de `receivable-list`) ganha uma coluna/rótulo de
  tipo, classe CSS por linha (`account-row--receivable` / `account-row--payable`, tons
  verde/vermelho suaves) e um `<select>` de filtro por tipo (Todas/A pagar/A receber),
  combinável com os filtros já existentes. `account-form` (renomeada de `receivable-form`)
  ganha um seletor de tipo no topo do formulário: ao trocar entre "A pagar"/"A receber", o
  campo de contraparte alterna entre um `<select>` de unidades (com a opção de "lançar para
  todas as unidades" — `bulkMode`, já existente) e um `<select>` de fornecedores (sem opção
  de lote, FR-009); em modo de edição, o seletor de tipo fica desabilitado (somente leitura),
  refletindo a imutabilidade do campo.
- **Rationale**: Implementa FR-010/FR-011 (listagem única com diferenciação visual por cor +
  rótulo textual, não só cor, por acessibilidade) e o design de formulário único descrito nas
  Assumptions do spec ("a interface exibe um único seletor, cujas opções mudam conforme o
  tipo"). Reaproveita `list-selection.ts`/`bulk-delete.ts`/`bulk-actions-bar` já existentes
  sem alteração de contrato.
- **Alternatives considered**: duas telas de listagem separadas (contas a pagar / contas a
  receber) com uma terceira tela de "visão combinada" — rejeitado pelo spec (US3: "na mesma
  tela"), e por duplicar filtros/lógica sem necessidade.
