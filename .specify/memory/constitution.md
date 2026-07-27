<!--
Sync Impact Report
==================
Versão: 1.2.0 → 1.3.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: `shared/exceptions/` passa a ter três bases
  genéricas (`NotFoundException` 404, `ConflictException` 409, `BadRequestException` 400,
  esta última nova); critério explícito de quando reforçar uma validação no `Service` via
  `BadRequestException` em vez de confiar só em Bean Validation no DTO (regra reaplicada em
  mais de um método, ou regra de negócio não puramente sintática).
- III. Stack Técnica Definida — expandido: Playwright fixado como devDependency permanente
  do frontend para validação manual em navegador (não faz parte da suíte de testes
  automatizados), reaproveitado a cada feature em vez de instalado/removido a cada rodada.
- IV. Convenções de Código e Formatação — expandido: datas em DTOs de API MUST trafegar no
  formato ISO-8601 padrão do `LocalDate` (`yyyy-MM-dd`, sem anotação `@JsonFormat`
  customizada); o formato DD/MM/AAAA fica restrito à UI, resolvido pelos recursos nativos do
  Angular/HTML (`DatePipe` para exibição, `<input type="date">` para entrada) sem exigir
  utilitário de conversão customizado (correção feita em duas rodadas ainda durante a
  implementação de 002-receivable-charges, antes do commit desta emenda: primeiro a primeira
  redação exigia dd/MM/yyyy também no contrato de API, o que forçava conversão manual
  desnecessária no backend; depois a usuária notou que os componentes nativos do Angular já
  fazem a conversão de exibição/entrada, tornando desnecessário até um utilitário de frontend
  dedicado); enums de domínio persistidos MUST usar `@Enumerated(EnumType.STRING)`, nunca
  `ORDINAL`.
- VI. Convenções de API REST — expandido: convenção de sub-rota `POST /{recurso}/bulk` para
  ações de criação em massa; formato padrão de erro 4xx agora explicitamente cobre JSON
  malformado/valor de campo inválido antes da Bean Validation rodar, via handler de
  `HttpMessageNotReadableException` já implementado em `GlobalExceptionHandler`.

Motivação: decisões que emergiram durante a implementação da feature
002-receivable-charges (primeira feature com campo de data, campo enum, ação de criação em
lote, e uma segunda exception genérica de infraestrutura) e que se aplicam a qualquer
entidade futura com essas mesmas necessidades, não só a `Receivable`. O gap de tratamento de
JSON malformado foi encontrado pelo `/speckit.analyze` antes da implementação (risco real de
violar o Princípio VI em runtime, não coberto por nenhum teste de caminho feliz) — a correção
já é código genérico reaproveitável, e esta emenda só documenta que ela existe e por quê,
para não ser removida ou duplicada por engano numa feature futura. A decisão sobre Playwright
veio de a usuária notar que a dança de instalar/remover a cada rodada de validação era o
gargalo real (reinstalação completa de `node_modules`), não o Playwright em si.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.1.0 → 1.2.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: estado local de componente Angular
  MUST usar `signal()` (não campo simples nem `BehaviorSubject`), consequência
  do app rodar zoneless; rotas de frontend MUST seguir o padrão
  `/{recurso-plural}`, `/{recurso-plural}/new`, `/{recurso-plural}/:id/edit`.
- VI. Convenções de API REST — expandido: DTOs de resposta MUST expor factory
  estático `from(Entity)`; relacionamentos em DTOs de resposta MUST embutir o
  DTO completo da entidade referenciada, não só o id; o interceptor HTTP do
  frontend MUST normalizar erros no objeto `ApiError`, consumido pelos
  componentes em vez de `HttpErrorResponse` bruto.
- III. Stack Técnica Definida — expandido: cobertura de teste automatizado
  para regras de negócio passa a ser obrigatória (não opcional como no
  template genérico do Spec Kit), para servir de proteção contra regressão
  entre features que rodam em sessões de IA isoladas.

Seções adicionadas:
- Revisão da Constituição Pós-Implementação — procedimento antes só documentado
  como prompt reaproveitável no README, agora formalizado aqui para poder ser
  invocado por referência curta em vez de prompt longo.
- Edição de Features Já Implementadas — idem, para o fluxo de mudança em
  feature existente sem rodar specify/plan/tasks do zero.

Seção expandida:
- Fluxo de Commits — detalha exatamente o que MUST entrar em cada uma das três
  subseções do README ("Decisões técnicas e premissas", "Revisões e correções
  das entregas da IA", "O que eu faria diferente...") em vez de uma instrução
  genérica.

Motivação: decisões que emergiram durante a implementação da feature
001-cadastro-condominos (código real, não só planejamento) e que se aplicam a
qualquer entidade futura, não só a Unit/Resident. A obrigatoriedade de testes
foi adicionada numa revisão posterior, quando a usuária questionou como
validar que o código funciona sem testes automatizados — a resposta revelou
que o template padrão do Spec Kit trata testes como opcionais por padrão, o
que exigiria lembrar de pedir isso a cada feature nova sem essa regra. Os dois
fluxos de processo (revisão da constituição e edição de feature existente)
foram movidos do README para cá para poderem ser referenciados de forma curta
em vez de exigir colar o prompt inteiro a cada vez.

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias

Itens pendentes (TODO): nenhum.
-->

<!--
Sync Impact Report (histórico — emenda anterior)
==================
Versão: 1.0.0 → 1.1.0

Princípios modificados:
- I. Arquitetura em Camadas — expandido: fixa pacote base do backend (`com.financas`),
  ferramenta de build (Maven), ferramenta de migração de schema (Flyway) e escopo exato da
  pasta `shared/` (só recursos verdadeiramente transversais; exceptions de regra de negócio
  de uma entidade vivem no `domain/` dela, não em `shared/`).
- III. Stack Técnica Definida — expandido: versões principais já adotadas MUST ser
  reutilizadas por novas features, em vez de re-pesquisadas a cada feature; a cláusula de
  "última versão estável" vale só para a escolha inicial de cada dependência. Também passa a
  fixar o stack de testes automatizados (backend: JUnit 5 + Mockito + Spring Boot Test;
  frontend: Vitest), ausente da versão anterior.

Princípios adicionados:
- VI. Convenções de API REST — novo princípio fixando padrão de rotas
  (`/api/{recurso-no-plural-em-inglês}` + verbos HTTP padrão), formato de erro padronizado
  (`{ "message": string, "status": number }`) e a regra de que confirmação de ações
  destrutivas é responsabilidade do frontend, não do backend.

Seções removidas: nenhuma

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem referências desatualizadas; "Constitution Check" é preenchido dinamicamente por feature
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias
- ✅ .claude/skills/speckit-*/SKILL.md — sem referências específicas de agente que precisem de ajuste
- ⚠ README.md — manter alinhado conforme decisões técnicas evoluírem (regra já registrada na seção de Commits)
- ⚠ specs/001-cadastro-condominos/plan.md e tasks.md — criados antes desta emenda com
  `DuplicateUnitException`/`UnitHasResidentsException` em `shared/exceptions/`; ajustados
  para `unit/domain/` para conformidade com o novo texto do Princípio I
- ✅ specs/001-cadastro-condominos/contracts/api.md — já segue o formato de erro e a
  convenção de rotas agora fixados no Princípio VI (decisão local antecipou a regra global)

Itens pendentes (TODO):
- Nenhum.
-->

# Finanças (Sistema de Gerenciamento de Condomínio) Constitution

## Core Principles

### I. Arquitetura em Camadas
O sistema segue arquitetura em camadas, com frontend (Angular) e backend (Spring Boot)
organizados por domínio. O pacote base do backend Java MUST ser `com.financas`, com cada
entidade de domínio em seu próprio subpacote (ex.: `com.financas.unit`,
`com.financas.resident`) contendo `api/` (controllers e contratos/DTOs), `domain/`
(entidade, enums, repository, service, e as exceptions que representam regras de negócio
daquela entidade — ex.: unicidade de um identificador, bloqueio de remoção por vínculo) e
`infra/` (implementação do repository). A ferramenta de build do backend MUST ser Maven; a
migração de schema de banco MUST ser feita via Flyway, com migrations versionadas em
`src/main/resources/db/migration`.

Recursos compartilhados ficam em `shared/`, mas essa pasta MUST conter apenas recursos
verdadeiramente transversais a mais de uma entidade: `GlobalExceptionHandler`, exceptions
genéricas de infraestrutura reaproveitáveis por qualquer entidade (`NotFoundException` → 404,
`ConflictException` → 409, `BadRequestException` → 400) e configuração técnica (ex.: CORS).
`BadRequestException` MUST ser usada quando uma regra de validação (ex.: "valor deve ser
positivo") precisa ser reforçada no `Service` — porque é reaplicada em mais de um método
(criar, editar, criar em lote) ou porque a regra de negócio em si não é puramente sintática —
em vez de depender só de Bean Validation no DTO; quando a regra é puramente sintática e vale
para um único ponto de entrada (campo obrigatório, formato), Bean Validation no DTO
(`@NotBlank`, `@Positive`, etc.) já basta, sem duplicar a checagem no `Service`. Uma exception
que representa uma regra de negócio específica de uma entidade NUNCA deve viver em
`shared/` — deve viver no `domain/` da própria entidade, mesmo que estenda uma das três bases
acima (ex.: `UnitHasResidentsException extends ConflictException`).

No frontend, cada entidade de domínio possui sua pasta de componentes, com `core/` para
tratamento de erros e `shared/` para models, services, validators e configuração de URL
base da API. Estado local de componente MUST ser gerenciado via `signal()` do Angular —
não por campo simples nem `BehaviorSubject` — consequência do app rodar em modo zoneless
(sem Zone.js). Rotas MUST seguir o padrão `/{recurso-plural}` (listagem),
`/{recurso-plural}/new` (criação) e `/{recurso-plural}/:id/edit` (edição), espelhando a
convenção de rotas de API do Princípio VI.

A estrutura de pastas proposta é uma referência, não uma regra rígida: pode ser adaptada
sempre que o padrão de mercado ou a necessidade do projeto justificar o desvio — exceto
pacote base, ferramenta de build e ferramenta de migração definidos acima, que MUST
permanecer estáveis entre features, salvo decisão explícita e documentada em contrário.

### II. Separação Controller → Service → Repository
O `Service` concentra as regras de negócio e é o único ponto que chama a interface
`Repository` quando necessário. O `Controller` NUNCA acessa o `Repository` diretamente —
toda a orquestração de dados passa pelo `Service`. Essa separação MUST ser respeitada em
toda nova funcionalidade de backend, garantindo que a lógica de negócio permaneça
centralizada e testável independentemente da camada de apresentação.

### III. Stack Técnica Definida
O backend usa Java com Spring Boot (Spring Data JPA, Spring Security, Spring Web); o
frontend usa Angular com TypeScript, Bootstrap e SCSS; a persistência usa PostgreSQL. As
versões principais (linguagem, framework, banco) MUST ser as últimas estáveis e
consolidadas no mercado no momento da escolha inicial de cada dependência. Uma vez que uma
versão tenha sido adotada e já exista código do projeto usando-a, novas features MUST
reutilizar essa mesma versão em vez de pesquisar/adotar "a mais recente do mercado"
novamente — divergir de uma versão já adotada exige decisão explícita e deliberada,
registrada nesta constituição como atualização, e não uma escolha implícita feita em uma
nova sessão/feature. Testes automatizados usam JUnit 5 + Mockito + Spring Boot Test no
backend e Vitest no frontend — mesma regra de reuso de versão já adotada se aplica a essas
ferramentas. Toda regra de negócio (validações, unicidade, bloqueios de exclusão por
vínculo, cálculos) MUST ter cobertura de teste automatizado antes de a funcionalidade ser
considerada concluída — diferente do padrão genérico de templates do Spec Kit, que trata
testes como opcionais salvo pedido explícito, este projeto exige que `/speckit.tasks` gere
tarefas de teste para regras de negócio em toda feature, independentemente de solicitação
explícita a cada vez; a garantia de que uma feature não quebra outra, dado que cada uma é
implementada numa sessão de IA isolada, vem de revisão humana combinada com essa cobertura
de teste. Não há, por ora, bibliotecas específicas a evitar — decisões de descontinuação de
uma biblioteca por experiência prévia negativa devem ser registradas aqui quando ocorrerem,
para não serem repetidas.

Playwright é usado como ferramenta de validação manual em navegador headless durante a
implementação (não faz parte da suíte de testes automatizados definida acima, que continua
sendo só JUnit 5 + Mockito + Spring Boot Test / Vitest). Uma vez que o frontend tenha telas
para validar, o Playwright MUST ser mantido como devDependency permanente de
`frontend/package.json` (`npm install --save-dev playwright`) e reaproveitado diretamente a
cada feature, em vez de instalado/removido a cada rodada — a reinstalação completa de
`node_modules` necessária para limpar uma instalação temporária custa bem mais tempo do que a
instalação do Playwright em si (binário do Chromium fica em cache local, fora do projeto).

### IV. Convenções de Código e Formatação
Datas exibidas à usuária final (UI) MUST seguir o formato DD/MM/AAAA. Internamente —
persistência e contrato de API (JSON de request/response) — todo campo de data MUST ser
`LocalDate`, trafegando no formato ISO-8601 padrão do Jackson (`yyyy-MM-dd`), sem anotação
`@JsonFormat` customizada; o backend NUNCA converte para o formato brasileiro. A conversão
para exibição/entrada em DD/MM/AAAA é responsabilidade exclusiva do frontend, usando os
recursos nativos do Angular/HTML já suficientes para isso — `DatePipe` (`| date:'dd/MM/yyyy'`)
para exibição e `<input type="date">` para entrada, cujo valor já trafega em ISO-8601 nativamente
— sem introduzir um utilitário de conversão customizado quando esses recursos nativos já
resolvem. Enums de
domínio persistidos via JPA MUST usar `@Enumerated(EnumType.STRING)`, nunca `ORDINAL` (evita
corromper dados existentes se a ordem de declaração dos valores do enum mudar no futuro).
Nomes de variáveis, classes, métodos, propriedades e tabelas de banco de dados MUST estar em
inglês. Mensagens de erro internas (exceptions, logs) MUST estar em inglês. Mensagens de erro
exibidas ao usuário final (respostas de API, frontend) MUST estar em português.

### V. Idioma por Tipo de Conteúdo
Todo conteúdo gerado neste projeto (por pessoas ou por IA) MUST respeitar a tabela abaixo.
Em caso de dúvida sobre uma categoria não listada, a pessoa responsável MUST perguntar
antes de assumir um idioma.

| Tipo de conteúdo | Idioma |
|---|---|
| Nomes de variáveis, classes, métodos, propriedades, tabelas de banco | Inglês |
| Mensagens de erro internas (exceptions, logs) | Inglês |
| Documentação de método (XML doc /// summary, JSDoc) | Inglês |
| Nomes de commit, branch, arquivo, projeto | Inglês |
| Comentários soltos no código (explicando lógica de negócio) | Português |
| Arquivos do Spec Kit: constitution.md, spec.md, plan.md, tasks.md | Português |
| Mensagens de erro exibidas ao usuário final (API response, frontend) | Português |
| README.md | Português |

### VI. Convenções de API REST
Rotas MUST seguir o padrão `/api/{recurso-no-plural-em-inglês}` (ex.: `/api/units`,
`/api/residents`), usando os verbos HTTP padrão: `GET` para listagem/consulta, `POST` para
criação, `PUT /{id}` para edição completa e `DELETE /{id}` para remoção. Quando um recurso
precisar de uma ação de criação em massa (aplicar os mesmos dados a todas as instâncias de
outro recurso relacionado), essa ação MUST ser exposta como sub-rota `POST
/{recurso}/bulk`, com corpo igual ao do `POST` individual menos o identificador do recurso
relacionado (que passa a ser implícito — "todas as instâncias existentes no momento da
chamada"), nunca sobrecarregando o `POST` individual com um identificador opcional.

Toda resposta de erro (4xx) MUST seguir o formato padronizado `{ "message": string, "status":
number }`, com `message` em português (ver Princípios IV e V) — isso inclui JSON malformado
ou um valor de campo que quebre a deserialização antes da validação Bean Validation rodar
(ex.: um valor de enum fora do conjunto esperado), cobertos por um
`@ExceptionHandler(HttpMessageNotReadableException.class)` genérico já implementado no
`GlobalExceptionHandler`, reaproveitado automaticamente por qualquer entidade nova sem
precisar de código extra. Confirmação de ações destrutivas (remoção) é responsabilidade
exclusiva do frontend (diálogo de confirmação antes da chamada); o endpoint `DELETE` MUST
executar a remoção diretamente quando chamado, sem etapa de confirmação própria no backend.

DTOs de resposta MUST expor um factory estático `from(Entity)` que constrói o DTO a
partir da entidade de domínio. Quando um DTO de resposta representa uma entidade que
referencia outra, MUST embutir o DTO de resposta completo da entidade referenciada (ex.:
`ResidentResponse.unit: UnitResponse`), nunca apenas o identificador. No frontend, o
interceptor HTTP MUST normalizar toda resposta de erro no formato acima em um objeto
`ApiError` consumido pelos componentes — componentes NUNCA devem ler `HttpErrorResponse`
bruto diretamente.

## Restrições Transversais

Não há, por enquanto, regras de negócio ou técnicas universais obrigatórias (ex.: formato
monetário fixo, retrocompatibilidade de API) nem restrições de segurança, compliance ou
performance em vigor. É possível que autenticação/autorização sejam implementadas
futuramente; quando isso ocorrer, esta seção MUST ser emendada para registrar as novas
restrições transversais antes de sua adoção no código.

## Fluxo de Commits

- Mensagens de commit MUST ser curtas, de uma linha (`tipo: descrição curta`, ex.: `fix: ...`, `feat: ...`, `docs: ...`), sem corpo com bullets.
- Preferir um único commit por tarefa/rodada de mudanças. Só dividir em commits separados quando houver unidades claramente distintas e independentes entre si (ex.: uma correção de bug não relacionada descoberta no meio do caminho) — não dividir apenas porque a mudança tocou várias camadas ou arquivos de uma mesma tarefa.
- Ao final de uma rodada de correções/funcionalidades, o conteúdo do `README.md` MUST ser atualizado. Especificamente:
  - **"Decisões técnicas e premissas"**: toda decisão técnica nova tomada na rodada (escolha de stack, convenção, trade-off, ajuste de configuração de ambiente) — registrar o porquê, não só o quê.
  - **"Revisões e correções das entregas da IA"**: listagem (não precisa detalhar muito, é mais um registro) de erros, desvios ou correções que a usuária identificou no trabalho entregue naquela rodada.
  - **"O que eu faria diferente ou melhoraria com mais tempo"**: melhorias ou ideias mencionadas na conversa que ficaram fora do escopo da rodada atual, registradas para retomar no futuro.
- A cada comando do Spec Kit executado ou correção solicitada, sugerir uma mensagem de commit seguindo o padrão já definido (tipo: descrição curta), sem executar o commit automaticamente — apenas propor o texto para o usuário decidir quando e se commitar.

## Revisão da Constituição Pós-Implementação

Depois de implementada uma feature (ou uma correção relevante), antes de considerá-la
encerrada, MUST ser feita uma varredura de padronização: ler esta constituição, os
artefatos da feature (`spec.md`, `plan.md`, `tasks.md`, e `research.md`/`data-model.md`/
`contracts/` quando existirem) e o código implementado, e identificar decisões tomadas que
NÃO são específicas das entidades ou da funcionalidade daquela feature, mas sim convenções
técnicas ou estruturais genéricas — coisas que qualquer feature futura teria que decidir de
novo se não estivessem escritas aqui.

Exemplos do tipo de decisão a procurar (lista não exaustiva): nome de pacote/namespace
base, ferramenta de build, ferramenta de migração de banco, convenção de nomenclatura de
pastas/camadas, onde exceptions/erros de negócio devem viver na estrutura de pastas,
convenção de rotas de API, formato padrão de resposta de erro, stack de testes, política de
reuso/atualização de versão de dependências, convenções de nomenclatura de DTOs/contratos,
padrões de estado/arquitetura de frontend.

Para cada decisão candidata, verificar se já está coberta por esta constituição. Depois, 
checar se a última alteração da constitution já foi commitada (`git status`/`git log`). 
Se sim, aplicar a emenda com nova versão semântica; se não, incorporar as mudanças na mesma 
emenda ainda não commitada, sem bump de versão. Verificar ao final se os templates 
dependentes (`plan-template.md`, `spec-template.md`, `tasks-template.md`) permanecem 
consistentes.

## Edição de Features Já Implementadas

Para mudanças em uma feature já implementada (comportamento, regra de negócio, ou correção)
que não justificam rodar `/speckit.specify`/`/speckit.plan`/`/speckit.tasks` do zero: NÃO
criar uma feature nova nem uma branch nova; editar os arquivos existentes em
`specs/[NÚMERO-NOME-DA-FEATURE]/` diretamente, nesta ordem:

1. **spec.md**: atualizar apenas as seções afetadas pela mudança (requisitos funcionais,
   regras de negócio, edge cases), preservando o resto do arquivo intacto. Se a mudança
   introduzir ambiguidade nova, sinalizar com `[NEEDS CLARIFICATION]` em vez de assumir uma
   resposta.
2. **plan.md**: atualizar apenas as seções tecnicamente impactadas, sem regenerar o arquivo
   inteiro. Ao final, confirmar explicitamente, princípio por princípio desta constituição,
   como cada um relevante continua sendo respeitado após a mudança — mesmo que a resposta
   seja "sem alteração necessária".
3. **tasks.md**: adicionar apenas as tarefas novas necessárias, sem regenerar a lista
   inteira e sem alterar o status de tarefas já concluídas (`[X]`). Se uma tarefa já
   concluída precisar ser refeita por causa da mudança, marcá-la explicitamente como
   pendente de novo, explicando o motivo, em vez de resetar tudo.
4. **contracts/api.md** (se existir e for afetado): atualizar apenas os contratos de
   endpoint impactados.

Antes de implementar: parar e mostrar um resumo do que mudou em cada arquivo (diff
conceitual) para a usuária revisar e aprovar explicitamente. Não escrever nem alterar
código de implementação até a confirmação, a menos que a usuária já tenha pedido para
pular essa espera na própria solicitação.

## Governance

Esta constituição prevalece sobre qualquer outra prática ou convenção informal adotada no
projeto. Qualquer decisão arquitetural já tomada e registrada aqui SHOULD ser mantida sem
revisitação, a menos que surja uma justificativa forte — nesse caso, a usuária deve ser
consultada antes de qualquer mudança.

Emendas a esta constituição exigem: (1) descrição clara da mudança e sua motivação, (2)
atualização da versão conforme versionamento semântico (MAJOR para remoção/redefinição
incompatível de princípios, MINOR para adição ou expansão material de princípio/seção,
PATCH para esclarecimentos e correções de redação), e (3) verificação de que os templates
dependentes (`plan-template.md`, `spec-template.md`, `tasks-template.md`) permanecem
consistentes com o texto atualizado.

Toda revisão de spec, plano ou tarefas MUST verificar conformidade com os princípios
definidos aqui. Complexidade adicional (novas camadas, dependências, padrões) MUST ser
justificada em relação aos princípios de simplicidade implícitos na arquitetura em camadas
descrita no Princípio I.

**Version**: 1.3.0 | **Ratified**: 2026-07-24 | **Last Amended**: 2026-07-26
</content>
