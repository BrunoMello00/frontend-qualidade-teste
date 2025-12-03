-- ========================================
-- FLYWAY MIGRATION V3 - ADICIONAR PERFIL ESTOQUISTA E USUÁRIOS
-- Sistema de Gerenciamento de Estoque e Vendas
-- ========================================

-- ========================================
-- 1. ATUALIZAR CONSTRAINT PARA INCLUIR ESTOQUISTA
-- ========================================
-- Primeiro, vamos remover a constraint existente e criar uma nova
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS CONSTRAINT_392;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS CHECK_1;
ALTER TABLE usuarios ADD CONSTRAINT CHECK_TIPO_USUARIO CHECK (tipo_usuario IN ('OWNER', 'ADMIN', 'VENDEDOR', 'ESTOQUISTA'));

-- ========================================
-- 2. NOVOS USUÁRIOS COM PERFIS ESPECÍFICOS
-- ========================================
-- Senha para todos: password (hash bcrypt)
-- Hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi

INSERT INTO usuarios (nome, email, senha, tipo_usuario, ativo, role) VALUES 
-- Owner (Proprietário) - Acesso total
('Proprietário do Sistema', 'owner@sistema.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'OWNER', TRUE, 'ADMIN'),

-- Vendedor - Produtos, vendas, clientes, eventos, configurações
('João Vendedor', 'vendedor@sistema.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'VENDEDOR', TRUE, 'USER'),

-- Estoquista - Estoque e configurações
('Maria Estoquista', 'estoquista@sistema.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ESTOQUISTA', TRUE, 'USER');

-- ========================================
-- 3. ATUALIZAR USUÁRIO ADMIN EXISTENTE
-- ========================================
-- Garantir que o admin mantenha o tipo correto
UPDATE usuarios SET tipo_usuario = 'ADMIN' WHERE email = 'admin@sistema.com';