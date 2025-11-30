-- V12: Adicionar campo is_fake, limpar produtos de exemplo e garantir cliente fake correto
-- Esta migração remove os produtos criados automaticamente e garante que existe apenas o cliente fake

-- ========================================
-- 1. ADICIONAR CAMPO IS_FAKE NA TABELA CLIENTES
-- ========================================
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS is_fake BOOLEAN DEFAULT FALSE;

-- ========================================
-- 2. REMOVER PRODUTOS DE EXEMPLO
-- ========================================
-- Remove produtos criados pelas migrações anteriores
DELETE FROM produtos WHERE nome IN (
    'Produto Exemplo',
    'Produto Teste', 
    'Produto Exemplo 1',
    'Produto Exemplo 2', 
    'Produto Exemplo 3'
);

-- Remove registros de estoque órfãos
DELETE FROM estoque WHERE produto_id NOT IN (SELECT id FROM produtos);

-- ========================================
-- 3. GARANTIR CLIENTE FAKE CORRETO
-- ========================================
-- Primeiro, atualizar cliente existente com CPF 000.000.000-00 se existir
UPDATE clientes SET 
    nome = 'Cliente Não Identificado',
    email = 'nao-identificado@sistema.local',
    telefone = '(00) 00000-0000',
    endereco_logradouro = 'Endereço não informado',
    endereco_cidade = 'Cidade não informada', 
    endereco_estado = 'UF',
    endereco_cep = '00000-000',
    nivel_fidelidade = 'BRONZE',
    ativo = TRUE,
    is_fake = TRUE
WHERE cpf = '000.000.000-00';

-- Se não existir, inserir o cliente fake
INSERT INTO clientes (nome, cpf, email, telefone, endereco_logradouro, endereco_cidade, endereco_estado, endereco_cep, nivel_fidelidade, ativo, data_cadastro, is_fake) 
SELECT 'Cliente Não Identificado', '000.000.000-00', 'nao-identificado@sistema.local', '(00) 00000-0000', 
       'Endereço não informado', 'Cidade não informada', 'UF', '00000-000', 'BRONZE', TRUE, CURRENT_TIMESTAMP, TRUE
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE cpf = '000.000.000-00');

-- ========================================
-- 4. RESET SEQUENCES (se necessário)
-- ========================================
-- Reset do sequence de produtos para começar do 1
-- ALTER SEQUENCE produtos_seq RESTART WITH 1;