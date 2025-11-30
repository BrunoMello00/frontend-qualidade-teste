-- V010__adicionar_pontuacao_produto.sql
-- Adiciona campo pontuacao_produto na tabela produtos

ALTER TABLE produtos 
ADD COLUMN pontuacao_produto INTEGER NOT NULL DEFAULT 0 
CHECK (pontuacao_produto >= 0 AND pontuacao_produto <= 1000);

COMMENT ON COLUMN produtos.pontuacao_produto IS 'Pontuação que o produto concede ao cliente quando comprado';

-- Adicionar alguns valores de exemplo para produtos existentes
UPDATE produtos SET pontuacao_produto = 10 WHERE preco >= 50.00 AND preco < 100.00;
UPDATE produtos SET pontuacao_produto = 20 WHERE preco >= 100.00 AND preco < 200.00;
UPDATE produtos SET pontuacao_produto = 30 WHERE preco >= 200.00 AND preco < 500.00;
UPDATE produtos SET pontuacao_produto = 50 WHERE preco >= 500.00;
UPDATE produtos SET pontuacao_produto = 5 WHERE preco < 50.00;