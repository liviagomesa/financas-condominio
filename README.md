# Sistema de Gerenciamento de Condomínio

Sistema de gestão de recebimentos e pagamentos de um condomínio. Composto por API REST em Java, banco de
dados PostgreSQL (container Docker) e frontend em Angular.

## Sumário
- [Sistema de Gerenciamento de Condomínio](#sistema-de-gerenciamento-de-condomínio)
  - [Sumário](#sumário)
  - [Instruções de execução](#instruções-de-execução)
    - [Pré-requisitos](#pré-requisitos)
    - [1. Banco de dados](#1-banco-de-dados)
    - [2. Backend (API)](#2-backend-api)
    - [3. Testes automatizados do backend](#3-testes-automatizados-do-backend)
    - [4. Frontend](#4-frontend)
  - [Decisões técnicas e premissas](#decisões-técnicas-e-premissas)
  - [Uso de IA](#uso-de-ia)
    - [Fluxo SDD](#fluxo-sdd)
    - [Revisões e correções das entregas da IA](#revisões-e-correções-das-entregas-da-ia)
  - [O que eu faria diferente ou melhoraria com mais tempo](#o-que-eu-faria-diferente-ou-melhoraria-com-mais-tempo)

## Instruções de execução

### Pré-requisitos

- [TO-DO]
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (para o container do banco)
- [Node.js LTS](https://nodejs.org/) + Angular CLI (`npm install -g @angular/cli`)

### 1. Banco de dados

Na raiz do projeto:

```powershell
docker compose up -d
```

Sobe um único container com o PostgreSQL.

### 2. Backend (API)

[TO-DO]

A API sobe em [TO-DO].

### 3. Testes automatizados do backend

[TO-DO]

### 4. Frontend

```powershell
cd frontend
npm install
npm start
```

Acesse `http://localhost:4200`.

## Decisões técnicas e premissas

[TO-DO, à medida que avançar no projeto]

## Uso de IA

### Fluxo SDD

Utilizei o GitHub Spec Kit integrado ao Claude Code para conduzir o desenvolvimento com uma abordagem de Spec-Driven Development (SDD). O fluxo foi:

1. **Constituição**: a partir de um arquivo-base temporário contendo orientações para arquitetura e estrutura, stack técnica, regras gerais, idioma, commits e fluxo de trabalho SDD, gerei com o Claude Code (via `/speckit.constitution`) um arquivo de princípios fundamentais do projeto. Esse arquivo idealmente raramente será modificado após a criação.

2. **Contexto para o Claude Code**: informações sobre o produto são importantes para a realização de tarefas de projeto pelo Claude Code, mas não se encaixam no `constitution.md`. Assim, guardei essas informações no `CLAUDE.md`.

3. **Especificação**: a partir das funcionalidades listadas no `CLAUDE.md`, gerei com o Claude Code uma especificação formal de cada funcionalidade. Exemplo: `/speckit.specify Cadastro de condôminos: permitir criar, editar, listar e remover condôminos, com nome, unidade, e-mail e telefone de contato. Cada condômino pertence a uma unidade única do condomínio.` 

4. **Clarificação**: Em seguida, usei `/speckit.clarify` para varrer o spec e perguntar o que ficou faltando (não é preciso antecipar todas as possíveis lacunas no `/speckit.specify`).

5. **Planejamento**: usei `/speckit.plan` e `/speckit.tasks` para definir a estrutura do projeto e quebrar o trabalho em tarefas menores.

6. **Implementação**: implementei com `/speckit.implement`.

7. **Revisão da constituição**: depois de implementada a feature, peço uma revisão da
   `constitution.md` à luz do `spec.md`/`plan.md`/`tasks.md` gerados, procurando decisões
   que não são específicas daquela feature e que deveriam virar padrão do projeto em vez de
   ficarem só documentadas ali — sem isso, cada feature roda numa sessão de IA isolada que
   só compartilha a `constitution.md` com as demais (não os planos de features já
   implementadas), e decisões técnicas genéricas (nome de pacote, ferramenta de build,
   convenção de API, etc.) tomadas "no piloto automático" numa feature podem simplesmente
   não se repetir na próxima. Uso este prompt, reaproveitável em qualquer projeto com Spec
   Kit:

  ```md
  Agora que a feature atual foi implementada, quero uma varredura de padronização antes de
  considerá-la encerrada. Leia `.specify/memory/constitution.md` e os artefatos gerados para
  esta feature (`spec.md`, `plan.md`, `tasks.md`, e `research.md`/`data-model.md`/`contracts/`
  quando existirem) e identifique decisões tomadas ali que NÃO são específicas das
  entidades ou da funcionalidade desta feature, mas sim convenções técnicas ou estruturais
  genéricas — coisas que qualquer feature futura deste projeto teria que decidir de novo se
  não estivessem escritas em algum lugar compartilhado.

  Exemplos do tipo de decisão que procuro (não se limite a esta lista): nome de
  pacote/namespace base, ferramenta de build, ferramenta de migração de banco, convenção de
  nomenclatura de pastas/camadas, onde exceptions/erros de negócio devem viver na estrutura
  de pastas, convenção de rotas de API, formato padrão de resposta de erro, stack de testes,
  política de reuso/atualização de versão de dependências, convenções de nomenclatura de
  DTOs/contratos.

  Para cada decisão candidata, verifique se ela já está coberta pela constitution.md atual.
  Monte uma lista categorizada:
  1. Vale formalizar na constitution agora (risco real de outra feature divergir).
  2. Nice-to-have / prioridade menor.
  3. Não vale — é específico desta feature, ou é detalhe de ambiente/implementação óbvio ao
     olhar o código já existente.

  NÃO edite a constitution ainda. Me mostre essa lista categorizada primeiro e espere minha confirmação de escopo. Depois que eu confirmar quais itens seguem, prepare a emenda (nova versão semântica) e aplique via `/speckit.constitution`, verificando ao final se os templates dependentes (`plan-template.md`, `spec-template.md`, `tasks-template.md`) continuam consistentes.
  ```

8. **Edições de features já existentes**: para mudanças depois do fluxo completo dos comandos do Spec Kit, utilizei este template para prompt:

  ```md
  Preciso atualizar a feature existente em `specs/[NÚMERO-NOME-DA-FEATURE]/`, sem criar uma feature nova nem uma branch nova. NÃO use os comandos /speckit.specify ou /speckit.plan — edite os arquivos diretamente, do jeito que vou descrever abaixo.

  ## Mudança solicitada
  [Descreva aqui, em algumas frases, o que precisa mudar — comportamento, regra de negócio, ou correção]

  ## O que fazer, em ordem

  1. **spec.md**: atualize apenas as seções afetadas pela mudança acima (requisitos funcionais, regras de negócio, edge cases). Preserve todo o resto do arquivo intacto. Se a mudança introduzir ambiguidade nova, sinalize com [NEEDS CLARIFICATION] em vez de assumir uma resposta.

  2. **plan.md**: atualize apenas as seções tecnicamente impactadas por essa mudança específica. Não regenere o arquivo inteiro do zero. Ao final, confirme explicitamente, princípio por princípio da constitution, como cada um relevante continua sendo respeitado após essa mudança (arquitetura de camadas, idioma, stack) — mesmo que a resposta seja "sem alteração necessária".

  3. **tasks.md**: adicione apenas as tarefas novas necessárias para implementar essa mudança. NÃO regenere a lista inteira. NÃO altere o status de tarefas já marcadas como concluídas [x]. Se alguma tarefa já concluída precisar ser refeita por causa dessa mudança, marque-a explicitamente como pendente de novo e explique o motivo, em vez de simplesmente resetar tudo.

  4. **contracts/api.md** (se existir e for afetado): atualize apenas os contratos de endpoint impactados pela mudança.

  ## Antes de implementar

  Pare aqui e me mostre um resumo do que mudou em cada arquivo (diff conceitual, não precisa ser diff literal) para eu revisar e aprovar explicitamente. Não escreva nem altere nenhum código de implementação até eu confirmar.
  ``` 

9. **Fluxo de trabalho paralelo:** utilizei o `/remote-control` do Claude Code para acompanhar e aprovar tarefas em execução mesmo longe do computador. Também mantive, em paralelo, uma sessão de chat separada com o Claude para discutir decisões de arquitetura, revisar premissas e planejar próximos passos antes de repassar instruções ao Claude Code — isso ajudou a economizar contexto na sessão de execução e a chegar a cada tarefa com a decisão já pensada, em vez de deixar a ferramenta decidir sozinha.

10. **Novas features**: para novas features (que não existem no enunciado inicial), rodei o fluxo do speckit novamente. Com isso, cada feature tem uma branch dentro de `.specify/`. 

> O problema disso: ao trabalhar numa feature, o Claude não sabe sobre o contexto das demais (somente o `constitution.md` é compartilhado entre features). Com isso, não há restrição que o impeça de editar e atrapalhar código originado de outras features já implementadas.
> A garantia de não conflito vem de duas fontes combinadas: testes automatizados e revisão humana.

### Revisões e correções das entregas da IA

- **Padrões implícitos não viram regra de projeto sozinhos**: ao gerar o `plan.md` e o
  `tasks.md` da primeira feature (cadastro de condôminos e unidades), o Claude tomou várias
  decisões que não eram específicas dessa feature — nome do pacote base do backend,
  ferramenta de build, ferramenta de migração de banco, onde exceptions de regra de negócio
  devem viver dentro da estrutura `api/domain/infra/shared`, formato padrão de erro da API,
  convenção de rotas REST. Nenhuma dessas decisões foi promovida à `constitution.md`
  automaticamente — ficaram só documentadas no plano daquela feature. Como cada feature roda
  numa sessão de IA isolada que só compartilha a `constitution.md` com as demais (não os
  `plan.md`/`research.md` de features já implementadas), isso é um risco real: uma próxima
  feature poderia divergir sem perceber (usar Gradle em vez de Maven, colocar uma exception
  de negócio em `shared/` em vez de `domain/`, inventar outro formato de erro). Só notei o
  risco ao revisar o plano e perguntei diretamente se aquilo não deveria virar padrão de
  projeto; o Claude concordou e propôs uma emenda à constituição (v1.0.0 → v1.1.0)
  formalizando essas decisões antes de qualquer código ser escrito.
  **Lição**: ao revisar um `plan.md`/`tasks.md` gerado por IA, vale perguntar ativamente
  quais decisões ali são genéricas o suficiente para virar regra na constituição, em vez de
  assumir que a IA vai sinalizar isso sozinha.

## O que eu faria diferente ou melhoraria com mais tempo

- **Soft delete de condôminos**: em vez de remover o registro definitivamente, marcar o condômino como inativo. Isso evita quebrar dados históricos de cobrança já existentes (o condômino deixaria de aparecer nas listagens de cadastro, mas continuaria sendo referenciado onde já existir vínculo). Para respeitar a LGPD, ao acessar um condômino inativo pelos registros em que ele é referenciado, apenas nome e unidade seriam exibidos — telefone e e-mail seriam removidos/ocultados junto com a inativação.

