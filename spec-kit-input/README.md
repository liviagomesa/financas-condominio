Arquivos temporários usados como input para comandos do Spec Kit — fica vazia a maior parte do tempo.

Exemplos de comandos: 

```bash
/speckit.constitution leia ./spec-kit-input/constitution-input.md e atualize de acordo
/speckit.specify leia ./spec-kit-input/spec-input.md
/speckit.plan leia o contexto técnico em ./spec-kit-input/plan-input.md
```

**Observações:**
- Em linhas gerais, `/speckit.specify` espera a descrição de uma feature (obrigatório) e `/speckit.plan` espera as decisões técnicas e de arquitetura para aquela feature (opcional).
- `/speckit.clarify`, `/speckit.tasks` e `/speckit.implement` não pedem argumento — só leem o spec.md e o plan.md já existentes.
- Se o argumento couber inline, não precisa criar arquivos temporários.

**As features não devem tocar na constitution.** A constitution só muda quando você percebe uma regra que se repete em qualquer feature. Isso pode acontecer a qualquer momento, inclusive sem nenhuma feature em andamento, só por reflexão sobre o projeto.

O fluxo de `/speckit.plan` e `/speckit.specify` já lê a constitution — na próxima feature, o conteúdo novo já será considerado.