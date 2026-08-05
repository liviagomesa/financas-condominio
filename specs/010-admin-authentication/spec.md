# Feature Specification: Autenticação da Síndica

**Feature Branch**: `010-admin-authentication`

**Created**: 2026-08-05

**Status**: Draft

**Input**: User description: "Autenticação de um único usuário administrador (eu mesma, a síndica) — sem múltiplos usuários, sem papéis, sem login social. Login tradicional por usuário/senha, gerenciado pelo próprio backend (Spring Security), com hash da senha (BCrypt) no banco. Nova entidade `User`. Provisionamento via variáveis de ambiente na inicialização. Fluxo de login com JWT stateless. Todas as rotas de `/api/**` exigem autenticação, exceto login. Frontend com tela de login, guard de rota e interceptor HTTP."

## Clarifications

### Session 2026-08-05

- Q: Quando a aplicação reinicia (redeploy, cold start em PaaS), a sessão da síndica (token JWT) deve continuar válida ou ela deve fazer login novamente? → A: Sessão sobrevive ao reinício — a chave usada para assinar/validar o JWT é fixa e persistida via variável de ambiente (mesmo padrão de `ADMIN_USERNAME`/`ADMIN_PASSWORD`), não gerada a cada boot.
- Q: O sistema deve oferecer uma ação explícita de logout (ex.: botão "Sair") ou o encerramento da sessão acontece apenas de forma implícita? → A: Sim, botão de logout explícito, que descarta o token armazenado e redireciona ao login imediatamente.
- Q: Qual deve ser o tempo de validade do token de sessão (JWT) antes de exigir novo login? → A: 7 dias.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Login com usuário e senha (Priority: P1)

A síndica abre o sistema, informa usuário e senha na tela de login e, se as credenciais estiverem corretas, passa a ter acesso a todas as telas e funcionalidades do sistema.

**Why this priority**: É o fluxo central da feature — sem ele, nenhuma outra parte (proteção de rotas, sessão via token) faz sentido. É também o único caminho de entrada no sistema a partir desta feature.

**Independent Test**: Pode ser testado isoladamente acessando a tela de login com as credenciais provisionadas via variável de ambiente e verificando que o acesso ao restante do sistema é liberado após o envio correto do formulário.

**Acceptance Scenarios**:

1. **Given** a aplicação foi inicializada com `ADMIN_USERNAME`/`ADMIN_PASSWORD` definidos e a síndica está na tela de login, **When** ela informa usuário e senha corretos e confirma, **Then** o sistema autentica com sucesso e a leva à tela inicial do sistema.
2. **Given** a síndica está na tela de login, **When** ela informa usuário ou senha incorretos, **Then** o sistema exibe uma mensagem de erro genérica (sem indicar qual dos dois campos está errado) e permanece na tela de login.

---

### User Story 2 - Bloqueio de acesso sem autenticação (Priority: P2)

Uma pessoa sem estar autenticada tenta acessar qualquer tela do sistema (diretamente pela URL ou após o token expirar) e é impedida, sendo redirecionada à tela de login.

**Why this priority**: É a razão de existir da feature — sem esse bloqueio, o login seria apenas decorativo, já que qualquer tela continuaria acessível livremente.

**Independent Test**: Pode ser testado isoladamente tentando acessar diretamente a URL de qualquer tela protegida sem um token válido armazenado e verificando o redirecionamento automático ao login, sem exigir que o fluxo de login em si esteja sendo exercitado no mesmo teste.

**Acceptance Scenarios**:

1. **Given** não há usuário autenticado, **When** a pessoa tenta acessar diretamente a URL de qualquer tela do sistema, **Then** ela é redirecionada para a tela de login sem ver o conteúdo da tela solicitada.
2. **Given** não há token de autenticação (ausente, inválido ou expirado), **When** o frontend faz qualquer chamada à API, **Then** o backend responde com erro de não autorizado e o frontend redireciona a pessoa à tela de login.

---

### User Story 3 - Sessão contínua entre chamadas à API (Priority: P3)

Depois de autenticada, a síndica navega pelo sistema normalmente, com o token sendo enviado automaticamente em toda chamada à API, sem precisar reinformar usuário/senha a cada ação.

**Why this priority**: Refina a experiência do fluxo já funcional das User Stories 1 e 2 — sem isso, o login funcionaria mas exigiria alguma forma manual de reautenticação a cada chamada, o que não é o comportamento esperado de um token JWT stateless.

**Independent Test**: Pode ser testado isoladamente autenticando uma vez e, em seguida, verificando que múltiplas chamadas subsequentes a diferentes endpoints de `/api/**` são aceitas sem novo prompt de login, até que o token expire ou seja invalidado.

**Acceptance Scenarios**:

1. **Given** a síndica está autenticada, **When** ela navega entre diferentes telas que chamam a API, **Then** cada chamada é aceita automaticamente sem exigir novo login.
2. **Given** a síndica está autenticada e o token expira durante o uso, **When** ela realiza qualquer ação que chame a API, **Then** o sistema a redireciona à tela de login para autenticar novamente.
3. **Given** a síndica está autenticada, **When** ela aciona a ação de logout, **Then** o token armazenado é descartado imediatamente e ela é redirecionada à tela de login, mesmo que o token ainda não tivesse expirado.

---

### Edge Cases

- O que acontece se a aplicação for iniciada sem `ADMIN_USERNAME`/`ADMIN_PASSWORD` definidos? A aplicação MUST falhar a inicialização com um erro claro, em vez de subir sem usuário provisionado (o que deixaria o login permanentemente impossível sem indicação do motivo).
- O que acontece se `ADMIN_USERNAME`/`ADMIN_PASSWORD` forem alterados e a aplicação reiniciada enquanto havia tokens válidos emitidos com as credenciais antigas? Os tokens já emitidos continuam válidos até expirar (o token é stateless e não referencia a senha) — a próxima tentativa de login exige as novas credenciais.
- O que acontece com a sessão da síndica quando a aplicação reinicia (redeploy de nova versão, cold start em PaaS)? A sessão MUST continuar válida — a chave de assinatura do JWT é fixa (via variável de ambiente) e não muda entre reinícios, então tokens emitidos antes do reinício permanecem aceitos até expirar.
- O que acontece se a pessoa tentar acessar o endpoint de login já autenticada (com token válido)? O endpoint de login continua aceitando a requisição normalmente e emite um novo token — não há necessidade de bloqueio especial nesse caso.
- O que acontece se o campo de usuário ou senha for enviado vazio no login? O sistema rejeita a tentativa com uma mensagem de erro, sem chegar a consultar o usuário provisionado.
- O que acontece com chamadas a rotas que não existem em `/api/**`? Continuam retornando 404 normalmente, mas apenas após a checagem de autenticação (uma pessoa não autenticada recebe 401 mesmo para uma rota inexistente, sem expor se a rota existe).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST oferecer uma tela de login com campos de usuário e senha.
- **FR-002**: O backend MUST validar as credenciais informadas contra o único usuário administrador provisionado, comparando a senha por hash (nunca em texto plano).
- **FR-003**: Quando as credenciais forem válidas, o backend MUST emitir um token JWT assinado, sem manter sessão em memória ou banco no servidor (autenticação stateless), com validade de 7 dias a partir da emissão.
- **FR-004**: Quando as credenciais forem inválidas, o backend MUST rejeitar o login com uma mensagem de erro genérica, sem indicar se o problema foi o usuário ou a senha.
- **FR-005**: O sistema MUST provisionar automaticamente o usuário administrador na inicialização da aplicação, criando-o se não existir ou atualizando a senha (novo hash) se já existir, lendo os valores de variáveis de ambiente (`ADMIN_USERNAME`, `ADMIN_PASSWORD`) — nunca de valores fixos no código-fonte ou em uma migration versionada.
- **FR-006**: A aplicação MUST falhar a inicialização com um erro claro quando `ADMIN_USERNAME` ou `ADMIN_PASSWORD` não estiverem definidos, em vez de subir sem usuário provisionado.
- **FR-006a**: A chave usada para assinar e validar o token JWT MUST ser lida de uma variável de ambiente (nunca gerada aleatoriamente a cada inicialização nem fixa no código-fonte), de forma que tokens emitidos antes de um reinício da aplicação (redeploy, cold start) continuem válidos após esse reinício, até o vencimento natural do token.
- **FR-007**: O sistema NUNCA MUST oferecer uma tela de cadastro de usuário nem um endpoint de registro — o único usuário existe exclusivamente via o provisionamento automático (FR-005).
- **FR-008**: Toda rota `/api/**` MUST exigir um token JWT válido para ser acessada, exceto o próprio endpoint de login.
- **FR-009**: O backend MUST responder com erro de não autorizado (401) a qualquer chamada a `/api/**` (exceto login) sem token, com token inválido, ou com token expirado.
- **FR-010**: Como existe um único usuário com acesso total, o sistema NÃO MUST implementar distinção de papel/permissão por endpoint — autenticado ou não é a única checagem de autorização necessária.
- **FR-011**: O frontend MUST bloquear, via guard de rota, o acesso a qualquer tela do sistema quando não houver usuário autenticado, redirecionando para a tela de login.
- **FR-012**: O frontend MUST anexar automaticamente o token JWT armazenado (cabeçalho `Authorization: Bearer`) a toda chamada feita à API.
- **FR-013**: O frontend MUST tratar uma resposta 401 de qualquer chamada à API limpando o token armazenado e redirecionando a pessoa à tela de login.
- **FR-013a**: O frontend MUST oferecer uma ação explícita de logout (visível em toda tela autenticada), que descarta imediatamente o token armazenado e redireciona a síndica à tela de login, sem esperar o vencimento natural do token.
- **FR-014**: O sistema NÃO MUST implementar, nesta feature: múltiplos usuários, papéis/permissões diferenciadas, contas de acesso para condôminos, login social/OAuth, recuperação de senha via e-mail, ou autenticação multifator (MFA). Troca de senha, quando necessária, é feita atualizando a variável de ambiente e reiniciando a aplicação.

### Key Entities *(include if feature involves data)*

- **User (usuário administrador)**: representa a única pessoa com acesso ao sistema (a síndica). Atributos: identificador de login (usuário ou e-mail) e senha armazenada como hash. Não possui papel/role nem vínculo com `Party` (condôminos/fornecedores), já que o acesso é sempre total. Existe no máximo um registro desta entidade, mantido em sincronia com as variáveis de ambiente a cada inicialização da aplicação.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A síndica consegue autenticar-se com sucesso usando as credenciais provisionadas em até 3 tentativas em condições normais (sem erro de digitação repetido).
- **SC-002**: 100% das rotas de API do sistema (exceto login) recusam acesso sem um token de autenticação válido.
- **SC-003**: Uma pessoa sem autenticação que tente acessar qualquer tela do sistema é redirecionada à tela de login em 100% dos casos, sem exibir conteúdo protegido mesmo que brevemente.
- **SC-004**: Após autenticar-se uma vez, a síndica consegue navegar e usar todas as funcionalidades do sistema sem precisar reinformar credenciais, até o token expirar ou ela encerrar a sessão.
- **SC-005**: Uma sessão autenticada permanece válida por até 7 dias sem exigir novo login — inclusive após reinícios da aplicação (redeploy, cold start) — contanto que a síndica não efetue logout manualmente antes disso.

## Assumptions

- Não há requisito de renovação automática do token (refresh token) nesta feature — ao expirar (7 dias após a emissão, FR-003), a síndica simplesmente faz novo login.
- O campo de identificação de login aceita texto livre (nome de usuário ou e-mail, conforme o valor definido em `ADMIN_USERNAME`), sem validação de formato de e-mail — é tratado apenas como um identificador de texto.
- Não há necessidade de bloqueio por tentativas repetidas de login (rate limiting/lockout) nesta feature, dado que se trata de um único usuário confiável (a própria síndica) e não de um sistema exposto a múltiplos usuários públicos.
- O logout (FR-013a) descarta o token apenas localmente no frontend; como o token é stateless, não há invalidação ativa no backend antes do vencimento natural — comportamento padrão de JWT sem lista de revogação.
- Este é o primeiro mecanismo de autenticação do sistema; a constituição do projeto já prevê que, quando autenticação for implementada, a seção "Restrições Transversais" deve ser emendada para registrar essa nova restrição transversal — tratado como parte do checkpoint de fim de implementação desta feature, não do próprio spec.
