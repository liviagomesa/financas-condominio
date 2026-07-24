# Data Model: Cadastro de Condôminos e Unidades

## Unit (Unidade)

Representa uma unidade do condomínio (ex.: apartamento).

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `id` | Long (PK, auto-gerado) | — | Gerado pelo banco |
| `identifier` | String | Sim | Único entre todas as unidades, comparado de forma normalizada (`trim` + case-insensitive — ver research.md). Não pode ser vazio/branco. |

**Relacionamentos**: uma `Unit` possui zero ou mais `Resident` (`1:N`).

**Regras de negócio**:
- FR-001/FR-002: `identifier` obrigatório e único (normalizado) na criação e na edição.
- FR-006: uma `Unit` só pode ser removida se não houver nenhum `Resident` associado.

**Persistência**: tabela `unit`, com índice único funcional sobre
`lower(trim(identifier))` (ver research.md) além da validação de aplicação.

## Resident (Condômino)

Representa uma pessoa associada a uma unidade do condomínio.

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `id` | Long (PK, auto-gerado) | — | Gerado pelo banco |
| `name` | String | Sim | Não pode ser vazio/branco. Sem restrição de unicidade. |
| `unit` | Referência a `Unit` (FK `unit_id`) | Sim | Deve referenciar uma `Unit` já cadastrada. |
| `email` | String | Não | Quando preenchido, deve ter formato de e-mail válido (ex.: contém "@" e domínio). |
| `phone` | String | Não | Quando preenchido, deve seguir formato brasileiro: DDD com 2 dígitos + número com 8 ou 9 dígitos (ver research.md para regra de validação). |

**Relacionamentos**: cada `Resident` pertence a exatamente uma `Unit`
(`ManyToOne`, obrigatório).

**Regras de negócio**:
- FR-007/FR-011: `name` e `unit` obrigatórios na criação e na edição.
- FR-008/FR-009: `email` e `phone` opcionais; várias `Resident` podem
  compartilhar a mesma `Unit`.
- FR-012: formato de `email` validado quando preenchido.
- FR-017: formato de `phone` validado quando preenchido.

**Persistência**: tabela `resident`, com `unit_id` como chave estrangeira
obrigatória (`NOT NULL`) para `unit.id`.

## Diagrama de relacionamento

```text
Unit (1) ──────< (N) Resident
  id                  id
  identifier          name
                       unit_id (FK → Unit.id)
                       email (nullable)
                       phone (nullable)
```

## Estados e transições

Nenhuma das entidades possui máquina de estados — são registros de cadastro
simples (criar, editar, listar, remover), sem status ou ciclo de vida além de
existir/não existir.
