---
name: "round-checkpoint"
description: "Propose the commit message, README/constitution suggestions, and drive user approval at the end of a documentation or implementation phase of a work round."
compatibility: "Requires spec-kit project structure with .specify/ directory"
metadata:
  author: "financas-project"
  source: "constitution.md — seção Checkpoints da Rodada de Trabalho"
user-invocable: true
disable-model-invocation: false
---

## Quando disparar

Esta skill MUST ser disparada proativamente, sem que a usuária precise solicitar, em cada um dos dois momentos definidos em `constitution.md` (seção "Checkpoints da Rodada de Trabalho"): (1) fim do trabalho de documentação de uma rodada, ou (2) fim do trabalho de implementação de uma rodada — seja dentro do fluxo completo de uma feature nova (`speckit-analyze` / `speckit-converge`), seja dentro de uma alteração direta conforme a seção "Edição de Features Já Implementadas". Fora desses dois momentos, a regra genérica de commits da `constitution.md` (mensagem curta, um único commit por tarefa salvo unidades independentes) continua valendo, mas sem os Passos 2, 3 e 4 abaixo.

## Passo 1: Mensagem de commit

Propor a mensagem do commit daquele momento, sempre em inglês, seguindo o formato já definido na `constitution.md` (`tipo: descrição curta`, uma linha só, sem corpo com bullets). Apenas propor o texto — a execução do commit em si continua exigindo confirmação explícita da usuária, nunca automática.

## Passo 2: Auditoria de cobertura de teste (só no momento de fim de implementação)

Esta verificação não se aplica ao momento de fim de documentação (ainda não há código para auditar) — nesse caso, pular direto para o Passo 3.

No momento de fim de implementação, listar os arquivos de código (backend e frontend) criados ou alterados na rodada (`git status`/`git diff` desde o início da rodada ou da feature). Para cada trecho de lógica não coberto por teste automatizado (nenhum `*Test.java`/`*.spec.ts` correspondente no diff nem já existente), avaliar se ele conta como regra de negócio para efeito da obrigação de teste já fixada no Princípio III da `constitution.md` ("toda regra de negócio... MUST ter cobertura de teste automatizado") — não restrito aos exemplos citados lá (validações, unicidade, bloqueios de exclusão, cálculos), mas qualquer trecho equivalente em espírito: (i) decide um resultado a partir de mais de uma condição não puramente sintática, (ii) deriva/calcula um valor a partir de outro(s) já existente(s), ou (iii) espelha/reforça em uma camada uma regra já garantida em outra (comum no frontend, ex.: uma validação cruzada de formulário que replica uma regra do backend). Se sim a qualquer um dos três, é uma lacuna de teste. Prestar atenção especial a: método de componente com `if`/`switch` alternando comportamento por modo ou tipo; `computed()` fazendo mais que um `map`/filtro trivial; validator customizado; e utilitário novo em `shared/` que segue o padrão estrutural de um irmão já testado (ex.: uma nova função `bulk-*` ao lado de `bulk-delete.ts`) sem ganhar o mesmo tratamento.

Listar as lacunas encontradas (arquivo + trecho + qual dos três critérios se aplica) junto das demais entregas do momento, para aprovação da usuária no Passo 5 — não implementar o teste faltante sem antes dar à usuária a chance de decidir se é essa rodada ou uma tarefa futura que deve fechar a lacuna (mesmo espírito do Passo 3, que também só lista para aprovação). Se nenhuma lacuna for encontrada, declarar isso explicitamente em vez de omitir o passo silenciosamente — a ausência de achado é informação, não ruído.

## Passo 3: Sugestões para o README.md

São elegíveis para adicionar ao `README.md`:

- Toda decisão técnica nova tomada na rodada (escolha de stack, convenção, trade-off, ajuste de configuração de ambiente), acompanhada do motivo → capítulo **"Decisões técnicas e premissas"**.
- Erros, desvios ou correções que a usuária identificou no trabalho entregue naquela rodada → capítulo **"Revisões e correções das entregas da IA"**.
- Melhorias ou ideias mencionadas na conversa que ficaram fora do escopo da rodada atual → capítulo **"O que eu faria diferente ou melhoraria com mais tempo"**.

Antes de propor, verificar se a informação já existe ou conflita com algo já escrito no `README.md` atual; se conflitar, adaptar a proposta para manter o arquivo consistente em vez de duplicar ou contradizer o que já está lá. Listar as sugestões para aprovação da usuária — NÃO aplicar nenhuma edição ao `README.md` antes da aprovação.

## Passo 4: Sugestões para a constitution.md

Ler a `constitution.md`, os artefatos da feature atual (`spec.md`, `plan.md`, `tasks.md`, e `research.md`/`data-model.md`/`contracts/` quando existirem) e o código implementado, e identificar decisões tomadas que NÃO são específicas das entidades ou da funcionalidade daquela feature, mas sim convenções técnicas ou estruturais genéricas — coisas que qualquer feature futura teria que decidir de novo se não estivessem escritas ali. Exemplos do tipo de decisão a procurar (lista não exaustiva): nome de pacote/namespace base, ferramenta de build, ferramenta de migração de banco, convenção de nomenclatura de pastas/camadas, onde exceptions/erros de negócio devem viver na estrutura de pastas, convenção de rotas de API, formato padrão de resposta de erro, stack de testes, política de reuso/atualização de versão de dependências, convenções de nomenclatura de DTOs/contratos, padrões de estado/arquitetura de frontend.

Para cada decisão candidata, verificar se já está coberta pela `constitution.md` atual. Depois, checar se a última alteração da `constitution.md` já foi commitada (`git status`/`git log`): se sim, propor a emenda com nova versão semântica (MAJOR/MINOR/PATCH conforme a seção Governance); se não, propor incorporar as mudanças na mesma emenda ainda não commitada, sem bump de versão. Verificar também se os templates dependentes (`plan-template.md`, `spec-template.md`, `tasks-template.md`) permaneceriam consistentes com a mudança proposta. Listar as sugestões para aprovação da usuária — NÃO aplicar nenhuma edição à `constitution.md` antes da aprovação.

## Passo 5: Aguardar aprovação

Depois de apresentar os Passos 1–4, aguardar os comentários da usuária cobrindo tanto a entrega daquele momento (a documentação escrita, ou a implementação de código) quanto as lacunas de teste levantadas e as sugestões de `README.md` e `constitution.md` — uma única rodada de revisão, não esperas separadas. Corrigir todos os pontos levantados e apresentar novamente até a aprovação explícita. Só depois da aprovação: aplicar as edições aceitas em `README.md`/`constitution.md`, criar as tarefas de teste combinadas para lacunas que a usuária decidiu fechar já e, se a usuária confirmar, executar o commit com a mensagem já aprovada.

## Done When

- [ ] Mensagem de commit do momento proposta (em inglês)
- [ ] No momento de fim de implementação: auditoria de cobertura de teste feita contra o critério geral do Princípio III, com lacunas listadas (ou ausência de lacunas declarada explicitamente)
- [ ] Sugestões de `README.md` listadas, checadas contra conflito com o conteúdo atual
- [ ] Sugestões de `constitution.md` listadas, com decisão sobre bump de versão e consistência dos templates dependentes
- [ ] Aprovação da usuária obtida sobre entrega + auditoria + sugestões, com todos os pontos solicitados corrigidos
- [ ] Edições aprovadas aplicadas e commit executado apenas após confirmação explícita