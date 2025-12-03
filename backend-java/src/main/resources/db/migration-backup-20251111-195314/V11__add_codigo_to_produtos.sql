-- ========================================
-- FLYWAY MIGRATION V11 - ADD MISSING COLUMNS TO produtos
-- Adiciona colunas faltantes esperadas pelo mapeamento JPA
-- ========================================

-- Adicionar colunas faltantes
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS codigo VARCHAR(255);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS codigo_resumido VARCHAR(50);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS estoque INTEGER DEFAULT 0;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS estoque_minimo INTEGER DEFAULT 0;

-- Popula 'codigo' a partir de prefixo + sequencial quando disponível,
-- caso contrário usa prefixo + id
UPDATE produtos
SET codigo = CASE
    WHEN codigo_interno_sequencial IS NOT NULL THEN CONCAT(prefixo_codigo, codigo_interno_sequencial)
    ELSE CONCAT(prefixo_codigo, id)
END
WHERE codigo IS NULL;

-- Popula codigo_resumido como os primeiros 10 chars do codigo
UPDATE produtos
SET codigo_resumido = LEFT(codigo, 10)
WHERE codigo_resumido IS NULL;

-- Popula created_at e updated_at se forem NULL
UPDATE produtos 
SET created_at = COALESCE(data_cadastro, CURRENT_TIMESTAMP),
    updated_at = COALESCE(data_atualizacao, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

-- Índices para consultas
CREATE INDEX IF NOT EXISTS IX_produtos_codigo ON produtos(codigo);
CREATE INDEX IF NOT EXISTS IX_produtos_codigo_resumido ON produtos(codigo_resumido);
CREATE INDEX IF NOT EXISTS IX_produtos_estoque ON produtos(estoque);
