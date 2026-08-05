# Research: Autenticação da Síndica

## Biblioteca de geração/validação de JWT

**Decision**: `io.jsonwebtoken:jjwt-api` + `jjwt-impl` (runtime) + `jjwt-jackson` (runtime), versão `0.12.6` (confirmada como a mais recente estável no Maven Central no momento desta feature).

**Rationale**: API leve e idiomática (`Jwts.builder()`/`Jwts.parser()`) para o caso deste projeto — um único emissor e um único validador (o próprio backend), com chave simétrica fixa. Evita a complexidade de configurar `spring-boot-starter-oauth2-resource-server`, que traz infraestrutura de OAuth2 (JWK sets, issuer discovery) desproporcional a esse cenário.

**Alternatives considered**: `spring-boot-starter-oauth2-resource-server` (Nimbus JOSE) — rejeitado por trazer conceitos de OAuth2 (authorization server, JWK) sem uso real aqui; `com.nimbusds:nimbus-jose-jwt` direto — API mais verbosa para HS256 com claims mínimos.

## Algoritmo de assinatura do token

**Decision**: HMAC-SHA256 (HS256), com chave simétrica lida da variável de ambiente `JWT_SECRET`.

**Rationale**: Emissor e validador são o mesmo processo (o próprio backend), então uma chave assimétrica (RS256/ES256) não traz benefício. HS256 com chave fixa via env var atende diretamente o FR-006a (token sobrevive a reinício da aplicação porque a chave não muda entre boots).

**Alternatives considered**: RS256 (par de chaves assimétrico) — rejeitado por complexidade sem benefício quando não há um terceiro validador externo ao backend.

**Nota de implementação**: HS256 exige chave de pelo menos 256 bits (32 caracteres) segundo o próprio `jjwt`; `JWT_SECRET` mais curto lança `WeakKeyException` no primeiro uso. Para não descobrir isso só na hora de logar, o mesmo componente de provisionamento que já valida `ADMIN_USERNAME`/`ADMIN_PASSWORD` na inicialização (FR-006) também valida o tamanho mínimo de `JWT_SECRET`, falhando o boot com mensagem clara — extensão natural do mesmo princípio de fail-fast, não uma nova regra de negócio.

## Nome e local do módulo backend

**Decision**: `com.financas.auth`, contendo tanto a entidade `User` quanto a infraestrutura de segurança (filtro JWT, configuração do Spring Security).

**Rationale**: Já registrado como módulo previsto em `docs/codebase-research.md`. Agrupar entidade + orquestração técnica sob um único módulo é análogo a `recurringcharge`, que combina a entidade `RecurringCharge` com o serviço de geração agendada — não há um segundo domínio reaproveitando `User` que justifique separá-lo em módulo próprio.

**Alternatives considered**: separar em `com.financas.user` (entidade) + `com.financas.auth` (segurança) — rejeitado por introduzir uma fronteira de módulo sem um segundo consumidor real da entidade `User` hoje.

## Exceção para credenciais inválidas

**Decision**: nova exceção genérica `UnauthorizedException` em `shared/exceptions`, mapeada para 401 no `GlobalExceptionHandler`, lançada diretamente por `AuthService` (sem subclasse dedicada).

**Rationale**: Segue o mesmo padrão das três exceções genéricas de infraestrutura já existentes (`NotFoundException`→404, `ConflictException`→409, `BadRequestException`→400) — 401 é um status HTTP genérico, não uma regra de negócio de uma entidade específica (Princípio I), e é reaproveitado tanto pelo login (via `GlobalExceptionHandler`) quanto conceitualmente pelo filtro JWT (via `AuthenticationEntryPoint`, ver decisão abaixo), reforçando que não é algo específico da entidade `User`.

**Alternatives considered**: subclasse dedicada em `auth/domain` (ex.: `InvalidCredentialsException`) — rejeitada porque não há uma regra de negócio nomeada além de "autenticação falhou"; reaproveitar `BadRequestException` (400) — rejeitada porque falha de autenticação é semanticamente 401, e o FR-009 já exige 401 para o mesmo tipo de falha em outras rotas, então usar 400 aqui criaria uma inconsistência entre dois pontos que representam a mesma falha.

## Falha de token nas rotas protegidas

**Decision**: `AuthenticationEntryPoint` customizado (`RestAuthenticationEntryPoint`), escrevendo diretamente o formato `{message, status}` do `ErrorResponse` já existente.

**Rationale**: O filtro JWT roda antes do `DispatcherServlet` alcançar um `@Controller`, então `@RestControllerAdvice`/`GlobalExceptionHandler` não intercepta falhas de token ausente/inválido/expirado — essas são responsabilidade do próprio `SecurityFilterChain`. Um `AuthenticationEntryPoint` dedicado é o ponto de extensão padrão do Spring Security para customizar a resposta 401, preservando a convenção do Princípio VI mesmo fora do fluxo padrão do MVC.

## CORS com Spring Security

**Decision**: `http.cors(Customizer.withDefaults())` na `SecurityFilterChain`, sem introduzir um novo bean `CorsConfigurationSource` — reaproveita a configuração já existente em `WebConfig` (`addCorsMappings`).

**Rationale**: Quando não há um bean `CorsConfigurationSource` explícito, o Spring Security resolve a configuração de CORS através do `HandlerMappingIntrospector`, que já enxerga o mapeamento registrado por `WebConfig implements WebMvcConfigurer`. Evita duplicar a lista de origens/métodos permitidos em dois lugares.

## Provisionamento do usuário administrador

**Decision**: `UserProvisioningService implements ApplicationRunner`, executado durante a inicialização do Spring Boot (antes de aceitar tráfego), validando `ADMIN_USERNAME`/`ADMIN_PASSWORD`/`JWT_SECRET` (lançando `IllegalStateException` se ausentes, abortando o boot) e fazendo o upsert do usuário (cria se não existir, atualiza o hash se já existir).

**Rationale**: `ApplicationRunner` é o idiom padrão do Spring Boot para ações de inicialização com acesso ao contexto totalmente pronto (repositórios, `PasswordEncoder`). O padrão `@Scheduled` + `@EventListener(ApplicationReadyEvent.class)` do Princípio I não se aplica aqui — esse padrão resolve recuperação de um *ciclo agendado perdido*; aqui não há ciclo recorrente, o provisionamento roda exatamente uma vez por boot, sempre.

**Alternatives considered**: `@PostConstruct` em `@Configuration` — equivalente em efeito, mas `ApplicationRunner` é mais idiomático para ações de boot que dependem de beans totalmente inicializados.

## Armazenamento do token no frontend

**Decision**: `localStorage`.

**Rationale**: A sessão deve sobreviver a reinício da aplicação/backend (SC-005) e, por extensão razoável do mesmo espírito de "sessão contínua" (User Story 3), a fechar/reabrir o navegador dentro da janela de 7 dias. `sessionStorage` seria limpo ao fechar a aba, o que contradiria essa expectativa.

**Alternatives considered**: cookie `HttpOnly` — mais resistente a XSS, mas exigiria o backend gerenciar `Set-Cookie`/CSRF e mudaria o modelo stateless simples baseado em `Authorization: Bearer` já definido no FR-012; desproporcional para este projeto pessoal de uso único.

## Ordem dos interceptors HTTP no frontend

**Decision**: `provideHttpClient(withInterceptors([errorInterceptor, authInterceptor]))` — `authInterceptor` depois de `errorInterceptor` no array.

**Rationale**: Em `withInterceptors([...])`, a resposta de uma chamada HTTP passa pelos interceptors na ordem inversa da declaração (o último do array é o mais próximo do backend, então é o primeiro a ver a resposta bruta na volta). Colocando `authInterceptor` depois de `errorInterceptor` no array, `authInterceptor` intercepta o `HttpErrorResponse` bruto (com `status` ainda acessível) antes de `errorInterceptor` convertê-lo em `ApiError` — permitindo limpar o token e redirecionar ao ver 401, e ainda repassar o erro adiante para `errorInterceptor` normalizar como já acontece hoje.

## Guard de rotas

**Decision**: rota pai sem componente próprio, agrupando as rotas existentes como `children`, com `canActivate: [authGuard]` aplicado uma única vez no nó pai.

**Rationale**: Evita repetir `canActivate` em cada uma das 11 rotas existentes (e em toda rota nova futura) — reduz o risco de uma tela nova ser esquecida na proteção.

**Alternatives considered**: `canActivate` individual em cada rota — rejeitado por exigir disciplina manual a cada rota nova, sem benefício sobre a abordagem de rota pai.

## DTO de resposta do login sem factory `from(Entity)`

**Decision**: `LoginResponse` (record com um único campo `token`) não segue o padrão `from(Entity)` do Princípio VI.

**Rationale**: o padrão `from(Entity)` existe para DTOs que representam uma entidade de domínio consultada/persistida (Party, Account, Fund...). `LoginResponse` não representa nenhuma entidade — é o resultado de uma operação (emissão de token), sem correspondência direta com `User`. Não há, portanto, uma entidade da qual "construir" o DTO.

**Alternatives considered**: `LoginResponse.from(User, token)` — rejeitada por expor a entidade `User` a um factory que nada tem a ver com ela; o único dado relevante é o token já calculado pelo `JwtService`.
