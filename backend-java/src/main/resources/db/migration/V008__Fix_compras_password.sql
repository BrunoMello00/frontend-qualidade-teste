-- Migração: Corrigir senha do usuário COMPRAS
-- Data: 2025-11-19
-- Descrição: Corrige o hash da senha do usuário COMPRAS para a senha padrão

UPDATE usuarios 
SET senha = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi' 
WHERE email = 'compras@sistema.com';