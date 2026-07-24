# Research: Cadastro de Condôminos e Unidades

## Versões de stack (Princípio III da constituição: "últimas estáveis e
consolidadas no mercado")

- **Decision**: Java 21 (LTS) — versão já instalada na máquina de
  desenvolvimento (`java -version` → Temurin 21.0.10 LTS).
  **Rationale**: É uma LTS válida e amplamente consolidada no mercado, compatível
  com o mínimo exigido pelo Spring Boot 4.1 (Java 17+); usar a versão já
  instalada evita fricção de setup (troca de JDK) sem violar o Princípio III, já
  que 21 continua sendo uma LTS ativa e suportada.
  **Alternatives considered**: Java 25 LTS (mais recente, mas exigiria instalar
  um novo JDK na máquina sem ganho concreto para o escopo desta feature); Java 26
  (release não-LTS).

- **Decision**: Spring Boot 4.1.x (Spring Framework 7), com Spring Data JPA e
  Spring Web.
  **Rationale**: Versão estável mais recente em julho/2026, com suporte até
  jul/2027.
  **Alternatives considered**: Spring Boot 3.5.x (ainda mantido, mas não é mais
  a versão mais recente).

- **Decision**: Spring Security NÃO é adicionado como dependência nesta feature.
  **Rationale**: A constituição (Restrições Transversais) já registra que
  autenticação/autorização "possivelmente serão implementadas futuramente" e que,
  quando isso ocorrer, a seção deve ser emendada. O spec desta feature não tem
  nenhum requisito de autenticação. Adicionar Spring Security agora seria
  complexidade sem uso real (viola o princípio de simplicidade do Governance).
  **Alternatives considered**: Incluir Spring Security já com configuração
  permissiva (permitAll) só para "ter a dependência pronta" — rejeitado por não
  agregar valor agora e adiar a decisão real de modelo de autenticação.

- **Decision**: Maven como ferramenta de build do backend.
  **Rationale**: Opção padrão do Spring Initializr, mais consolidada no
  mercado para projetos Spring Boot novos.
  **Alternatives considered**: Gradle — também válido, mas sem motivo específico
  para preferi-lo neste projeto.

- **Decision**: Angular 22 com TypeScript 6, Bootstrap 5 + SCSS — versão já
  instalada na máquina de desenvolvimento (`ng version` → Angular CLI 22.0.7).
  **Rationale**: Coincide com a versão estável mais recente do Angular em
  julho/2026, então não há conflito entre "usar o que já está instalado" e
  "usar a versão mais recente do mercado".
  **Alternatives considered**: Angular 21 (LTS anterior, ainda suportada, mas
  não é a instalada nem a mais recente).

- **Decision**: Node.js 24 LTS (instalado: v24.15.0) como runtime de
  desenvolvimento do frontend.
  **Rationale**: Angular 22 exige Node >= 22.22 ou >= 24.13.1 e só certifica
  releases LTS pares; Node 24.15.0 atende ao requisito e já está instalado na
  máquina, evitando troca de runtime sem necessidade.
  **Alternatives considered**: Node 22 LTS — também válido, mas exigiria trocar
  a versão já instalada sem ganho concreto.

- **Decision**: Vitest como test runner de unidade do frontend (via Angular
  CLI), em vez de Karma/Jasmine.
  **Rationale**: A partir da v20+, o Angular CLI passou a usar Vitest como
  runner padrão para novos projetos; Karma está depreciado e não recebe mais
  novas features.
  **Alternatives considered**: Karma + Jasmine — descontinuado pelo próprio
  time do Angular, não deve ser escolhido para um projeto novo.

- **Decision**: PostgreSQL 18.4.
  **Rationale**: Versão estável mais recente da série ativa em julho/2026.
  **Alternatives considered**: Nenhuma — não há motivo para fixar uma versão
  anterior num projeto novo.

## Nomenclatura de entidades (código em inglês, Princípio IV/V)

- **Decision**: "Unidade" → classe/tabela `Unit`; "Condômino" → classe/tabela
  `Resident`.
  **Rationale**: `Unit` é a tradução direta e sem ambiguidade. Para "condômino",
  `Resident` foi preferido a `Owner`/`CondominiumOwner` porque o cadastro não
  distingue proprietário de morador/inquilino — o spec trata apenas de uma pessoa
  associada a uma unidade, o que `Resident` (morador/ocupante) representa melhor
  sem presumir posse.
  **Alternatives considered**: `Condomino`/`Condominium` (mistura idiomas ou gera
  ambiguidade com o próprio condomínio como entidade); `Owner` (presume posse,
  incompatível com o domínio real onde pode haver inquilinos).

## Unicidade normalizada do identificador de Unidade (FR-002)

- **Decision**: Unicidade garantida em duas camadas: (1) validação de aplicação
  no `UnitService`, comparando `identifier.trim().toLowerCase()` contra os
  identificadores já normalizados existentes antes de inserir/atualizar; (2)
  índice único funcional no PostgreSQL sobre `lower(trim(identifier))`, como
  proteção contra condição de corrida entre duas requisições concorrentes.
  **Rationale**: A validação de aplicação garante mensagem de erro amigável em
  português; o índice de banco garante a integridade mesmo sob concorrência, que
  a validação de aplicação sozinha não cobre.
  **Alternatives considered**: Confiar apenas na validação de aplicação —
  rejeitado por não proteger contra corrida entre requisições simultâneas.

## Validação de telefone em formato brasileiro (FR-017)

- **Decision**: Validação via expressão regular customizada (Bean Validation
  `@Pattern` ou validator customizado), aceitando DDD de 2 dígitos seguido de
  número com 8 ou 9 dígitos, com ou sem formatação comum (parênteses, espaço,
  hífen) — normalizado para dígitos antes de validar a quantidade.
  **Rationale**: Atende à clarificação do spec (formato brasileiro estrito) sem
  depender de biblioteca externa, mantendo a complexidade baixa.
  **Alternatives considered**: Biblioteca de validação de telefone
  internacional (ex.: libphonenumber) — rejeitada por ser desnecessária para um
  único formato de país.

## Tratamento de erros da API

- **Decision**: `GlobalExceptionHandler` centralizado em `shared/`, mapeando
  exceptions de negócio de cada entidade (`DuplicateUnitException` e
  `UnitHasResidentsException`, ambas vivendo em `unit/domain/` — não em
  `shared/`, conforme constitution v1.1.0), além das exceptions genéricas de
  `shared/exceptions/` (`NotFoundException`/`ConflictException`) e de
  `MethodArgumentNotValidException`/Bean Validation → 400, para um corpo de
  resposta padronizado com mensagem em português.
  **Rationale**: O handler em si é transversal (evita duplicar tratamento de
  erro em cada controller), mas as exceptions que carregam uma regra de negócio
  de uma entidade específica pertencem ao `domain/` dela — só exceptions
  genéricas e reaproveitáveis por qualquer entidade vivem em `shared/`.
  **Alternatives considered**: Tratamento de erro individual por controller —
  rejeitado por gerar duplicação e inconsistência de formato de resposta.
  Colocar todas as exceptions de domínio em `shared/exceptions/` — rejeitado
  pela emenda à constitution (v1.1.0), que reserva `shared/` a recursos
  verdadeiramente transversais.
