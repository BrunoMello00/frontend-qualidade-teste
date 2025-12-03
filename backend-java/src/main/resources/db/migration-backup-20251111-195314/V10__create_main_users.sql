-- Primeiro, atualize os produtos que referenciam esses usuários para evitar violação de FK
UPDATE produtos SET USUARIO_CADASTRO_ID = NULL WHERE USUARIO_CADASTRO_ID IN (
    SELECT id FROM usuarios WHERE email IN ('owner@sistema.com', 'admin@sistema.com', 'vendedor@sistema.com')
);

-- Remove usuarios existentes se existirem
DELETE FROM usuarios WHERE email IN ('owner@sistema.com', 'admin@sistema.com', 'vendedor@sistema.com');

-- 1. USUARIO OWNER (PROPRIETARIO) - Email: owner@sistema.com | Senha: password
INSERT INTO usuarios (nome, email, senha, telefone, cpf, tipo_usuario, ativo, role, data_cadastro) VALUES 
('Proprietario do Sistema', 'owner@sistema.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '(11) 91111-1111', '111.111.111-11', 'OWNER', TRUE, 'ADMIN', CURRENT_TIMESTAMP);

-- 2. USUARIO ADMIN (ADMINISTRADOR) - Email: admin@sistema.com | Senha: password  
INSERT INTO usuarios (nome, email, senha, telefone, cpf, tipo_usuario, ativo, role, data_cadastro) VALUES 
('Administrador do Sistema', 'admin@sistema.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '(11) 92222-2222', '222.222.222-22', 'ADMIN', TRUE, 'ADMIN', CURRENT_TIMESTAMP);

-- 3. USUARIO VENDEDOR (OPERACIONAL) - Email: vendedor@sistema.com | Senha: password
INSERT INTO usuarios (nome, email, senha, telefone, cpf, tipo_usuario, ativo, role, data_cadastro) VALUES 
('Joao Silva - Vendedor', 'vendedor@sistema.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '(11) 93333-3333', '333.333.333-33', 'VENDEDOR', TRUE, 'USER', CURRENT_TIMESTAMP);

-- Atualiza produtos sem usuario valido para referenciar o admin
UPDATE produtos 
SET usuario_cadastro_id = (SELECT id FROM usuarios WHERE email = 'admin@sistema.com' LIMIT 1)
WHERE usuario_cadastro_id IS NULL OR usuario_cadastro_id NOT IN (SELECT id FROM usuarios);