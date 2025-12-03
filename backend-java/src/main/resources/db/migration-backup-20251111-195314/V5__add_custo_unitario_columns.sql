-- V5__add_custo_unitario_columns.sql
-- Adicionar campos de controle de custo (custo_unitario já existe em produtos)

-- Adicionar campos de controle de custo na tabela movimentacoes_estoque  
ALTER TABLE movimentacoes_estoque ADD COLUMN custo_unitario DECIMAL(10,2);
ALTER TABLE movimentacoes_estoque ADD COLUMN custo_medio_anterior DECIMAL(10,2);
ALTER TABLE movimentacoes_estoque ADD COLUMN custo_medio_atual DECIMAL(10,2);

-- H2 não suporta COMMENT ON COLUMN, documentação nos comentários SQL acima
-- custo_unitario: Custo unitário específico desta movimentação
-- custo_medio_anterior: Custo médio antes desta movimentação  
-- custo_medio_atual: Custo médio após esta movimentação