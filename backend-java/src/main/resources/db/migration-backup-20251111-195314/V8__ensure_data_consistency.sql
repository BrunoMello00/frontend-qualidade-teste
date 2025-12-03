-- Migração V9: Garantir consistência dos dados após V8
-- Verificações e ajustes finais

-- Garantir que não temos nenhum produto sem código de barras válido
UPDATE produtos 
SET codigo_barras = CONCAT(prefixo_codigo, '-', LPAD(codigo_interno_sequencial, 6, '0'))
WHERE codigo_barras IS NULL OR TRIM(codigo_barras) = '';

-- Garantir que data de cadastro não seja nula
UPDATE produtos 
SET data_cadastro = CURRENT_TIMESTAMP
WHERE data_cadastro IS NULL;

-- Garantir que data de atualização não seja nula  
UPDATE produtos 
SET data_atualizacao = CURRENT_TIMESTAMP
WHERE data_atualizacao IS NULL;

-- Adicionar um produto de teste se não existir nenhum produto (COMENTADO - NÃO CRIAR AUTOMATICAMENTE)
-- INSERT INTO produtos (
--     nome, descricao, preco, departamento, categoria, 
--     marca, unidade_medida, peso, dimensoes, 
--     codigo_barras, codigo_interno_sequencial, prefixo_codigo,
--     ativo, custo_unitario, data_cadastro, data_atualizacao,
--     usuario_cadastro_id
-- )
-- SELECT 
--     'Produto Teste', 'Produto para testes do sistema', 10.00, '01', 'Geral',
--     'Marca Teste', 'UN', 0.5, '10x10x10',
--     'PROD-000001', 1, 'PROD',
--     1, 5.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
--     (SELECT id FROM usuarios WHERE email = 'admin@test.com' ORDER BY id LIMIT 1)
-- WHERE NOT EXISTS (SELECT 1 FROM produtos);