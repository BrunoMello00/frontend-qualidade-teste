-- V15: Adiciona campo quantidade_compras na tabela clientes
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS quantidade_compras INTEGER DEFAULT 0 CHECK (quantidade_compras >= 0);