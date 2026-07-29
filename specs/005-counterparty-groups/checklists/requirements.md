# Specification Quality Checklist: Unificação de Unidade/Fornecedor, Contas sem Restrição de Tipo e Grupos

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-07-29
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- Items marked incomplete require spec updates before `/speckit.clarify` or `/speckit.plan`.
- Duas decisões de alto impacto (nomenclatura da entidade unificada; preservação de dados existentes na migração) foram levantadas como perguntas interativas à usuária, fora do corpo do spec.md, durante o `/speckit-specify`, e resolvidas: entidade nomeada **Parte**/**Party**; ambiente confirmado como só de teste, migração MAY recriar as tabelas do zero. Ambas as decisões já estão incorporadas em spec.md (seção Key Entities e Assumptions).
- Sessão `/speckit-clarify` de 2026-07-29 resolveu 3 ambiguidades adicionais de UX/modelagem (ver `## Clarifications` em spec.md): modo de lançamento por Parte vs Grupo (dois modos alternáveis), comportamento do total dinâmico frente à seleção por checkbox (soma sempre todas as linhas filtradas), e onde gerenciar integrantes de um Grupo (exclusivamente pela tela do Grupo).
- Correções adicionais da usuária após a sessão de clarify (2026-07-29): (1) a spec não deve mais narrar cenários em termos de "unidade"/"fornecedor"/"reembolso"/"estorno" — só "Parte", que pode ter entradas e saídas livremente; (2) o rótulo de UI é só "Parte", não "Unidade ou Fornecedor"; (3) o total dinâmico da tabela de Contas é um valor líquido (ENTRADA − SAÍDA, podendo ser negativo), não uma soma aritmética simples — o campo `amount` da conta MAY passar a aceitar negativos se a abordagem técnica do plano exigir; (4) removida qualquer garantia de preservação de contas/Partes cadastradas antes da feature (SC-005 e um edge case foram reescritos para não prometer isso).
