# CLAUDE.md

## Sobre o produto
- **Qual problema esse produto resolve, e para quem?** Esse produto facilita o gerenciamento de condôminos, mensalidades, pagamentos, cálculos e taxas extras do meu condomínio. Eu mesma irei utilizá-lo nas minhas tarefas.
- **Quais são as 3 a 5 funcionalidades centrais que definem o que ele é?**
  - Cadastro de condôminos
  - Lançamentos de contas a receber (por unidade, com opção de adicionar para todas simultaneamente)
  - Lançamentos de contas a pagar (com identificação do fornecedor)
  - Solução para facilitar cobranças e pagamentos recorrentes, com valor fixo (mas que podem sofrer reajuste aproximadamente 1x por ano)
  - Cálculos automáticos
  - Visualização de condôminos devedores e saldos pendentes
- **Existe algum objetivo de negócio que deveria influenciar decisões técnicas (ex: precisa escalar pra milhões de usuários desde o dia um, ou é um MVP validando hipótese)?** Trata-se de um projeto pequeno, para uso pessoal. O plano não é mais rodar localmente: a aplicação vai ser hospedada numa solução PaaS, e a partir do deploy passo a usar a aplicação de verdade no dia a dia do condomínio. Esse deploy ainda não aconteceu — quando acontecer, a usuária vai avisar explicitamente para que a constitution seja emendada (ver Governance em `.specify/memory/constitution.md`), porque a partir daquele momento passa a haver dado de produção real em jogo, o que muda o default de decisões técnicas que hoje presumem ambiente local de desenvolvimento sem dado real a preservar (ex.: a exceção de truncar/recriar coluna em vez de migrar, descrita no Princípio I da constitution).
- **Existe algo que o produto explicitamente NÃO se propõe a fazer, que vale deixar registrado pra evitar scope creep?** Por enquanto, não há nada específico.

Regras e princípios (fonte de verdade): @./.specify/memory/constitution.md