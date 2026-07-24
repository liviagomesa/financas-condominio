# Checklist para o arquivo temporário de constitution

**Sobre o processo de desenvolvimento**
- Antes de codar, existe alguma etapa obrigatória (atualizar spec, pedir aprovação, revisar documentação) que não pode ser pulada?
- Quando existe mais de uma forma de implementar algo, quem decide — a IA sozinha, ou o humano precisa ser consultado? Em que condições?
- Existe algum critério de "pronto" que vai além de testes passarem (ex: validação end-to-end manual)?

**Sobre arquitetura e fronteiras**
- Qual é o padrão arquitetural que deve ser seguido consistentemente, e quais camadas/módulos podem ou não se comunicar diretamente entre si?
- Existe alguma convenção de organização de pastas/nomenclatura que deve ser respeitada?
- Existe algum padrão de projeto (design pattern) que deve ser aplicado sempre que a situação se repetir?

**Sobre convenções de conteúdo**
- Existe alguma regra fixa de idioma, formatação, ou nomenclatura que se aplica a todo o projeto (código, comentários, documentação, mensagens de erro)?
- Existe algum padrão de commit, branch, ou versionamento que deve ser seguido?

**Sobre o que não deve mudar sem justificativa forte**
- Existe alguma decisão já tomada (arquitetura, tecnologia, abordagem) que não deve ser revisitada sem um motivo forte para isso?
- Existe algum limite de complexidade que deve ser respeitado (ex: YAGNI, evitar otimização prematura, evitar abstrações não solicitadas)?

**Sobre segurança, performance e outras restrições transversais**
- Existe alguma restrição de segurança, performance ou compliance que vale para qualquer parte do sistema, independente da feature?
- Existe algum invariante de negócio que nunca pode ser violado, não importa a implementação (ex: "saldo nunca fica negativo", "todo valor monetário é armazenado como inteiro")?

**Sobre autoridade e colaboração**
- Quem tem a palavra final quando a IA e o humano discordam?
- Existe alguma divisão de responsabilidade entre sessões diferentes (ex: uma sessão decide arquitetura, outra executa) que deve ser respeitada?

**Sobre a própria governança da constitution**
- Como futuras emendas devem ser feitas — quem pode propor, como versionar (semver, data, etc.)?
- Existe algo que, se um princípio for quebrado, deve obrigatoriamente ser registrado e justificado em vez de simplesmente ignorado?