# Data Model: Autenticação da Síndica

## User

Representa a única pessoa com acesso ao sistema (a síndica). Mantido em sincronia com as variáveis de ambiente `ADMIN_USERNAME`/`ADMIN_PASSWORD` a cada inicialização da aplicação (criado se não existir, senha/hash atualizada se já existir) — nunca criado ou editado por uma tela/endpoint (FR-007).

**Tabela física**: `admin_user` (nome alternativo a `user`, palavra reservada em SQL — mesmo tratamento já aplicado a `Group`/`GROUP` → `party_group`, Princípio I).

| Campo | Tipo (Java) | Tipo (coluna) | Regras |
|---|---|---|---|
| `id` | `Long` | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Gerado pelo banco |
| `username` | `String` | `VARCHAR(255) NOT NULL UNIQUE` | Valor de `ADMIN_USERNAME`; texto livre, sem validação de formato de e-mail (Assumption do spec) |
| `passwordHash` | `String` | `password_hash VARCHAR(255) NOT NULL` | Hash BCrypt de `ADMIN_PASSWORD`; nunca armazenado em texto plano |

**Relacionamentos**: nenhum — `User` não referencia nem é referenciado por `Party`, `Group`, `Account`, `Fund` ou `RecurringCharge`, já que o acesso é sempre total (Key Entities do spec).

**Cardinalidade**: no máximo 1 registro em todo o ciclo de vida da aplicação. O upsert de `UserProvisioningService` busca por `username`: se o valor de `ADMIN_USERNAME` mudar entre reinícios, um novo registro é criado e o antigo permanece órfão (sem impacto funcional, já que nenhuma outra entidade referencia `User` e apenas o `username` atual é usado para autenticar) — comportamento aceitável dado o volume desprezível e o caráter de uso pessoal do projeto.

**Transições de estado**: nenhuma — não há campo de status; a única mutação é a atualização do `passwordHash` quando `ADMIN_PASSWORD` muda entre reinícios.

## Token JWT (não persistido)

Não é uma entidade de banco — é um artefato stateless assinado pelo backend e guardado pelo frontend (`localStorage`).

| Claim | Origem | Observação |
|---|---|---|
| `sub` | `User.username` | Identifica o portador do token |
| `iat` | Momento da emissão | Padrão JWT |
| `exp` | `iat` + 7 dias | FR-003 |

Assinado com HS256, chave fixa em `JWT_SECRET` (ver `research.md`). Validado a cada requisição a `/api/**` (exceto login) pelo `JwtAuthenticationFilter`, sem consulta ao banco — a validade de um token não depende de o `User` ainda existir com o mesmo `username`/hash no momento da validação (mesmo comportamento já descrito no edge case do spec sobre troca de credenciais).
