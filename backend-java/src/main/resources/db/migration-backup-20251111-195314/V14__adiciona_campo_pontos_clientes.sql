-- V14: Adiciona campos faltantes na tabela clientes
-- Adiciona campo 'pontos' se não existir
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS pontos INTEGER DEFAULT 0 CHECK (pontos >= 0);

-- Atualiza valores para campos já existentes mas que podem estar nulos
UPDATE clientes SET categoria = 'BRONZE' WHERE categoria IS NULL;
UPDATE clientes SET total_compras = 0.00 WHERE total_compras IS NULL;
UPDATE clientes SET pontos = 0 WHERE pontos IS NULL;