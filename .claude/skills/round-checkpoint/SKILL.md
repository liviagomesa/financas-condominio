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

Esta skill MUST ser disparada proativamente, sem que a usuária precise solicitar, em cada um dos dois momentos definidos em `constitution.md` (seção "Checkpoints da Rodada de Trabalho"): (1) fim do trabalho de documentação de uma rodada, ou (2) fim do trabalho de implementação de uma rodada — seja dentro do fluxo completo de uma feature nova (`speckit-analyze` / `speckit-converge`), seja dentro de uma alteração direta conforme a seção "Edição de Features Já Implementadas". Fora desses dois momentos, a regra genérica de commits da `constitution.md` (mensagem curta, um único commit por tarefa salvo unidades independentes) continua valendo, mas sem os Passos 2 e 3 abaixo.

## Passo 1: Mensagem de commit

Propor a mensagem do commit daquele momento, sempre em inglês, seguindo o formato já definido na `constitution.md` (`tipo: descrição curta`, uma linha só, sem corpo com bullets). Apenas propor o texto — a execução do commit em si continua exigindo confirmação explícita da usuária, nunca automática.

## Passo 2: Sugestões para o README.md

São elegíveis para adicionar ao `README.md`:

- Toda decisão técnica nova tomada na rodada (escolha de stack, convenção, trade-off, ajuste de configuração de ambiente), acompanhada do motivo → capítulo **"Decisões técnicas e premissas"**.
- Erros, desvios ou correções que a usuária identificou no trabalho entregue naquela rodada → capítulo **"Revisões e correções das entregas da IA"**.
- Melhorias ou ideias mencionadas na conversa que ficaram fora do escopo da rodada atual → capítulo **"O que eu faria diferente ou melhoraria com mais tempo"**.

Antes de propor, verificar se a informação já existe ou conflita com algo já escrito no `README.md` atual; se conflitar, adaptar a proposta para manter o arquivo consistente em vez de duplicar ou contradizer o que já está lá. Listar as sugestões para aprovação da usuária — NÃO aplicar nenhuma edição ao `README.md` antes da aprovação.

## Passo 3: Sugestões para a constitution.md

Ler a `constitution.md`, os artefatos da feature atual (`spec.md`, `plan.md`, `tasks.md`, e `research.md`/`data-model.md`/`contracts/` quando existirem) e o código implementado, e identificar decisões tomadas que NÃO são específicas das entidades ou da funcionalidade daquela feature, mas sim convenções técnicas ou estruturais genéricas — coisas que qualquer feature futura teria que decidir de novo se não estivessem escritas ali. Exemplos do tipo de decisão a procurar (lista não exaustiva): nome de pacote/namespace base, ferramenta de build, ferramenta de migração de banco, convenção de nomenclatura de pastas/camadas, onde exceptions/erros de negócio devem viver na estrutura de pastas, convenção de rotas de API, formato padrão de resposta de erro, stack de testes, política de reuso/atualização de versão de dependências, convenções de nomenclatura de DTOs/contratos, padrões de estado/arquitetura de frontend.

Para cada decisão candidata, verificar se já está coberta pela `constitution.md` atual. Depois, checar se a última alteração da `constitution.md` já foi commitada (`git status`/`git log`): se sim, propor a emenda com nova versão semântica (MAJOR/MINOR/PATCH conforme a seção Governance); se não, propor incorporar as mudanças na mesma emenda ainda não commitada, sem bump de versão. Verificar também se os templates dependentes (`plan-template.md`, `spec-template.md`, `tasks-template.md`) permaneceriam consistentes com a mudança proposta. Listar as sugestões para aprovação da usuária — NÃO aplicar nenhuma edição à `constitution.md` antes da aprovação.

## Passo 4: Aguardar aprovação

Depois de apresentar os Passos 1–3, aguardar os comentários da usuária cobrindo tanto a entrega daquele momento (a documentação escrita, ou a implementação de código) quanto as sugestões de `README.md` e `constitution.md` — uma única rodada de revisão, não duas esperas separadas. Corrigir todos os pontos levantados e apresentar novamente até a aprovação explícita. Só depois da aprovação: aplicar as edições aceitas em `README.md`/`constitution.md` e, se a usuária confirmar, executar o commit com a mensagem já aprovada.

## Done When

- [ ] Mensagem de commit do momento proposta (em inglês)
- [ ] Sugestões de `README.md` listadas, checadas contra conflito com o conteúdo atual
- [ ] Sugestões de `constitution.md` listadas, com decisão sobre bump de versão e consistência dos templates dependentes
- [ ] Aprovação da usuária obtida sobre entrega + sugestões, com todos os pontos solicitados corrigidos
- [ ] Edições aprovadas aplicadas e commit executado apenas após confirmação explícita