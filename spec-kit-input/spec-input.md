Autenticação de um único usuário administrador (eu mesma, a síndica) — sem múltiplos usuários, sem papéis, sem login social. Login tradicional por usuário/senha, gerenciado pelo próprio backend (Spring Security), com hash da senha (BCrypt) no banco.

Nova entidade `User`: apenas os campos necessários para autenticação — nome de usuário (ou e-mail) e senha (hash). Não há campo de papel/role nem vínculo com `Party`, já que existe um único usuário e ele tem acesso total.

Provisionamento: a aplicação cria (ou atualiza, se ainda não existir) esse usuário automaticamente na inicialização, lendo usuário e senha de variáveis de ambiente (ex.: `ADMIN_USERNAME`, `ADMIN_PASSWORD`) — nunca commitadas no código nem numa migration versionada. Não existe tela de cadastro de usuário nem endpoint de registro.

Fluxo de login: tela de login no frontend com campos de usuário e senha; o backend valida as credenciais e, se corretas, emite um JWT assinado, que o frontend passa a usar em todas as chamadas subsequentes à API (`Authorization: Bearer`) — token stateless, sem sessão guardada no servidor.

Autorização no backend: todas as rotas de `/api/**` exigem autenticação (exceto o próprio endpoint de login). Como existe um único usuário com acesso total, não há distinção de papel/permissão por endpoint — autenticado ou não é a única checagem necessária.

No frontend: tela de login; um guard de rota Angular bloqueando acesso a qualquer tela quando não há usuário autenticado, redirecionando para o login; um interceptor HTTP que anexa o JWT armazenado em toda chamada à API e trata resposta 401 (token expirado/inválido) redirecionando de volta ao login.

Fora de escopo: múltiplos usuários, papéis/permissões diferenciadas, contas de condôminos, login social/OAuth, recuperação de senha via e-mail (troca de senha, se necessária, é feita atualizando a variável de ambiente e reiniciando a aplicação), MFA.