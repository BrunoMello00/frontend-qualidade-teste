-- V13: Adicionar campos faltantes na tabela clientes
-- Esta migração adiciona os campos necessários para compatibilidade com a entidade Cliente

-- ========================================
-- 1. ADICIONAR CAMPO CATEGORIA
-- ========================================
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS categoria VARCHAR(20) DEFAULT 'BRONZE';

-- ========================================
-- 2. ADICIONAR CAMPO TOTAL_COMPRAS
-- ========================================
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS total_compras DECIMAL(10,2) DEFAULT 0.00;

-- ========================================
-- 3. ADICIONAR CAMPO CREATED_AT
-- ========================================
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ========================================
-- 4. ADICIONAR CAMPO UPDATED_AT
-- ========================================
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ========================================
-- 5. ATUALIZAR CLIENTE FAKE SE EXISTIR
-- ========================================
UPDATE clientes SET 
    categoria = 'BRONZE',
    total_compras = 0.00,
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE cpf = '000.000.000-00';