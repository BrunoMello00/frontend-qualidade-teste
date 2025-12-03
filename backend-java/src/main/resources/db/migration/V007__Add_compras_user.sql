-- Migração: Adicionar perfil COMPRAS
-- Data: 2025-11-19
-- Descrição: Adiciona perfil COMPRAS ao sistema e usuário de exemplo

-- 1. ATUALIZAR CONSTRAINT PARA INCLUIR COMPRAS
ALTER TABLE usuarios DROP CONSTRAINT CHECK_TIPO_USUARIO;
ALTER TABLE usuarios ADD CONSTRAINT CHECK_TIPO_USUARIO CHECK (tipo_usuario IN ('OWNER', 'ADMIN', 'VENDEDOR', 'ESTOQUISTA', 'COMPRAS'));

-- 2. INSERIR USUÁRIO COMPRAS DE EXEMPLO
INSERT INTO usuarios (
    nome, 
    email, 
    senha, 
    telefone, 
    cpf, 
    tipo_usuario, 
    ativo, 
    data_cadastro, 
    tentativas_login, 
    bloqueado, 
    created_at, 
    updated_at,
    role
) VALUES (
    'Responsável por Compras',
    'compras@sistema.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    '(11) 99999-8888',
    '12345678901',
    'COMPRAS',
    true,
    NOW(),
    0,
    false,
    NOW(),
    NOW(),
    'USER'
);