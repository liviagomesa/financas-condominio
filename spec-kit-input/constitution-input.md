# Arquivo base para o comando `/speckit.constitution`

## Sobre a arquitetura e estrutura
- **Qual é o padrão arquitetural geral (camadas, hexagonal, microsserviços, monolito)?** Camadas.
- **Como o código está organizado em pastas, e por quê (convenção de nomenclatura, separação por domínio ou por tipo técnico)?** A proposta abaixo não é fixa: se algo fugir do padrão de mercado ou se houver necessidade no projeto, pode alterar.

```
frontend/
├── core/
│   └── error-handling
├── domain-entity/
│   ├── component
│   └── component
├── domain-entity/
│   └── component
├── ...                   # cada entidade de domínio tem sua pasta de componentes
└── shared/
    ├── models
    ├── services
    ├── validators
    └── api-base-url.ts
backend/
├── domain-entity/
│   ├── api/
│   │   ├── contracts/
│   │   │   ├── DomainEntityResponse.java # exemplo
│   │   │   └── ...                       # outros DTOs
│   │   └── DomainEntityController.java
│   ├── domain/
│   │   ├── DomainEntity.java
│   │   ├── ...                         # enums, se houver
│   │   ├── DomainEntityRepository.java
│   │   └── DomainEntityService.java
│   └── infra/
│       └── DomainEntityRepositoryImpl.java
├── domain-entity/
│   └── ...
└── shared/
    ├── GlobalExceptionHandler.java
    └── Exceptions/
        └── ...
```

- **Existe algum padrão de projeto que deve ser seguido consistentemente (ex: Strategy para regras de negócio variáveis, Repository para acesso a dados)?** Service concentra regras de negócio e chama a interface repository quando necessário. Controller não conhece repository.
- **Quais decisões arquiteturais já foram tomadas e não devem ser revisitadas sem justificativa forte?** Estou certa do que já indiquei até agora, mas não hesite em me dizer se encontrar algo que pode ser melhorado, que irei avaliar.

## Sobre a stack técnica
- **Quais são as tecnologias, frameworks e versões principais (linguagem, framework web, banco de dados, etc.)?** Java, e Spring Boot (Spring Data JPA, Spring Security, Spring Web), Angular (com TypeScript, Bootstrap e SCSS) e PostgreSQL. Versões: a última estável e consolidada no mercado.
- **Existem bibliotecas ou ferramentas que devem ser preferidas, e outras que devem ser evitadas (ex: já tentamos X e não funcionou bem)?** Por enquanto, não.
- **Quais são as convenções de código não óbvias (formato de datas, tratamento de erros, padrão de logging, idioma dos comentários)?** Datas no formato DD/MM/AAAA. Ver idiomas em CLAUDE.md.

## Sobre regras e restrições que atravessam tudo
- **Existe alguma regra de negócio ou técnica que se aplica universalmente, independente da feature (ex: "todo valor monetário em centavos", "toda mudança precisa manter retrocompatibilidade com API v1")?** Por enquanto, não.
- **Existem restrições de segurança, compliance ou performance que valem para qualquer parte do sistema?** Por enquanto, não. Mas futuramente, é possível que eu queira implementar autenticação/autorização.

## Commits

- Mensagens de commit curtas, de uma linha (`tipo: descrição curta`, ex.: `fix: ...`, `feat: ...`, `docs: ...`) — sem corpo com bullets.
- Preferir um único commit por tarefa/rodada de mudanças. Só dividir em commits separados quando houver unidades claramente distintas e independentes entre si (ex.: uma correção de bug não relacionada descoberta no meio do caminho) — não dividir só porque a mudança tocou várias camadas ou arquivos de uma mesma tarefa.
- Nunca dar `git add`/`git commit` em `README.md` a menos que o usuário peça explicitamente — esse arquivo é commitado por último, pelo próprio usuário, mesmo que o resto do trabalho já tenha sido commitado em partes.
- Ao final de uma rodada de correções/funcionalidades, atualizar o conteúdo do `README.md` com o que for necessário (novas decisões técnicas que tomei, erros que eu apontei e pedi pra revisar, melhorias que identificamos para o futuro) — mesmo sem commitá-lo (ver regra acima). Editar o conteúdo e commitar são coisas independentes; a primeira não deve ser esquecida só porque a segunda espera um sinal do usuário.

## Idioma — regra obrigatória para todo conteúdo gerado neste projeto

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

Antes de gerar qualquer arquivo, verifique qual categoria ele se encaixa nesta tabela.
Em caso de dúvida sobre uma categoria não listada aqui, pergunte antes de assumir.