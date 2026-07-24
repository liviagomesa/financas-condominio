<!--
Sync Impact Report
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
genéricas de infraestrutura reaproveitáveis por qualquer entidade (ex.: um
`NotFoundException`/`ConflictException` base) e configuração técnica (ex.: CORS). Uma
exception que representa uma regra de negócio específica de uma entidade NUNCA deve viver
em `shared/` — deve viver no `domain/` da própria entidade.

No frontend, cada entidade de domínio possui sua pasta de componentes, com `core/` para
tratamento de erros e `shared/` para models, services, validators e configuração de URL
base da API.

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
ferramentas. Não há, por ora, bibliotecas específicas a evitar — decisões de descontinuação
de uma biblioteca por experiência prévia negativa devem ser registradas aqui quando
ocorrerem, para não serem repetidas.

### IV. Convenções de Código e Formatação
Datas exibidas ou registradas em conteúdo de domínio MUST seguir o formato DD/MM/AAAA.
Nomes de variáveis, classes, métodos, propriedades e tabelas de banco de dados MUST estar
em inglês. Mensagens de erro internas (exceptions, logs) MUST estar em inglês. Mensagens
de erro exibidas ao usuário final (respostas de API, frontend) MUST estar em português.

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
criação, `PUT /{id}` para edição completa e `DELETE /{id}` para remoção. Toda resposta de
erro (4xx) MUST seguir o formato padronizado `{ "message": string, "status": number }`, com
`message` em português (ver Princípios IV e V). Confirmação de ações destrutivas (remoção)
é responsabilidade exclusiva do frontend (diálogo de confirmação antes da chamada); o
endpoint `DELETE` MUST executar a remoção diretamente quando chamado, sem etapa de
confirmação própria no backend.

## Restrições Transversais

Não há, por enquanto, regras de negócio ou técnicas universais obrigatórias (ex.: formato
monetário fixo, retrocompatibilidade de API) nem restrições de segurança, compliance ou
performance em vigor. É possível que autenticação/autorização sejam implementadas
futuramente; quando isso ocorrer, esta seção MUST ser emendada para registrar as novas
restrições transversais antes de sua adoção no código.

## Fluxo de Commits

- Mensagens de commit MUST ser curtas, de uma linha (`tipo: descrição curta`, ex.: `fix: ...`, `feat: ...`, `docs: ...`), sem corpo com bullets.
- Preferir um único commit por tarefa/rodada de mudanças. Só dividir em commits separados quando houver unidades claramente distintas e independentes entre si (ex.: uma correção de bug não relacionada descoberta no meio do caminho) — não dividir apenas porque a mudança tocou várias camadas ou arquivos de uma mesma tarefa.
- Ao final de uma rodada de correções/funcionalidades, o conteúdo do `README.md` MUST ser atualizado com o que for necessário (novas decisões técnicas tomadas, erros apontados pela usuária para revisão, melhorias identificadas para o futuro).
- A cada comando do Spec Kit executado ou correção solicitada, sugerir uma mensagem de commit seguindo o padrão já definido (tipo: descrição curta), sem executar o commit automaticamente — apenas propor o texto para o usuário decidir quando e se commitar.

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

**Version**: 1.1.0 | **Ratified**: 2026-07-24 | **Last Amended**: 2026-07-24
</content>
