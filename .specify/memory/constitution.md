<!--
Sync Impact Report
==================
Versão: TEMPLATE → 1.0.0 (ratificação inicial)

Princípios definidos:
- I. Arquitetura em Camadas
- II. Separação Controller → Service → Repository
- III. Stack Técnica Definida
- IV. Convenções de Código e Formatação
- V. Idioma por Tipo de Conteúdo

Seções adicionadas:
- Restrições Transversais
- Fluxo de Commits
- Governança

Seções removidas: nenhuma (primeira versão)

Templates a verificar:
- ✅ .specify/templates/plan-template.md — genérico, sem referências desatualizadas; "Constitution Check" é preenchido dinamicamente por feature
- ✅ .specify/templates/spec-template.md — genérico, sem alterações necessárias
- ✅ .specify/templates/tasks-template.md — genérico, sem alterações necessárias
- ✅ .claude/skills/speckit-*/SKILL.md — sem referências específicas de agente que precisem de ajuste
- ⚠ README.md — manter alinhado conforme decisões técnicas evoluírem (regra já registrada na seção de Commits)

Itens pendentes (TODO):
- Nenhum. Campos sem informação explícita foram preenchidos com "Por enquanto, não" conforme input do usuário.
-->

# Finanças (Sistema de Gerenciamento de Condomínio) Constitution

## Core Principles

### I. Arquitetura em Camadas
O sistema segue arquitetura em camadas, com frontend (Angular) e backend (Spring Boot)
organizados por domínio. No backend, cada entidade de domínio possui sua própria pasta
contendo `api/` (controllers e contratos/DTOs), `domain/` (entidade, enums, repository,
service) e `infra/` (implementação do repository). Recursos compartilhados ficam em
`shared/` (ex.: `GlobalExceptionHandler`, exceptions customizadas). No frontend, cada
entidade de domínio possui sua pasta de componentes, com `core/` para tratamento de erros
e `shared/` para models, services, validators e configuração de URL base da API.
A estrutura de pastas proposta é uma referência, não uma regra rígida: pode ser adaptada
sempre que o padrão de mercado ou a necessidade do projeto justificar o desvio.

### II. Separação Controller → Service → Repository
O `Service` concentra as regras de negócio e é o único ponto que chama a interface
`Repository` quando necessário. O `Controller` NUNCA acessa o `Repository` diretamente —
toda a orquestração de dados passa pelo `Service`. Essa separação MUST ser respeitada em
toda nova funcionalidade de backend, garantindo que a lógica de negócio permaneça
centralizada e testável independentemente da camada de apresentação.

### III. Stack Técnica Definida
O backend usa Java com Spring Boot (Spring Data JPA, Spring Security, Spring Web); o
frontend usa Angular com TypeScript, Bootstrap e SCSS; a persistência usa PostgreSQL.
As versões MUST ser as últimas estáveis e consolidadas no mercado no momento da
implementação. Não há, por ora, bibliotecas específicas a evitar — decisões de
descontinuação de uma biblioteca por experiência prévia negativa devem ser registradas
aqui quando ocorrerem, para não serem repetidas.

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

**Version**: 1.0.0 | **Ratified**: 2026-07-24 | **Last Amended**: 2026-07-24
</content>
