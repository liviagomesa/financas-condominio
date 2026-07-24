# Checklist para `/speckit.plan` (o como)

1. **Camadas e responsabilidades**: quais componentes serão criados/tocados, e onde especificamente mora a lógica de negócio, o acesso a dados, e a exposição HTTP dessa feature?
2. **Modelo de dados**: quais tabelas/colunas são novas ou alteradas, e existe migration?
3. **Dependências e impacto**: essa feature depende de algo externo (ex.: APIs de terceiros, uma nova biblioteca que ainda não temos, outro banco, alguma planilha, nova configuração de ambiente ou variável de ambient), ou afeta alguma feature já existente?