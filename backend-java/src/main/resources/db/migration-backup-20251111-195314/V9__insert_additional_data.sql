-- V3__insert_initial_data.sql
-- Dados iniciais do sistema (APENAS dados não sensíveis)

-- ========================================
-- USUÁRIO ADMINISTRADOR PADRÃO DO SISTEMA
-- ========================================
-- Usuário: admin@admin.com / Senha: admin123 (para desenvolvimento)
INSERT INTO usuarios (nome, email, senha, ativo) VALUES 
('Administrador', 'admin@admin.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfEkeI.oa2T7.5K', true);

-- ========================================
-- CONFIGURAÇÕES DO SISTEMA
-- ========================================
INSERT INTO configuracao_sistema (chave, valor, descricao, tipo, categoria, obrigatorio) VALUES
('empresa_nome', 'Sistema de Estoque e Vendas', 'Nome da empresa', 'STRING', 'EMPRESA', TRUE),
('empresa_cnpj', '00.000.000/0001-00', 'CNPJ da empresa (exemplo)', 'STRING', 'EMPRESA', FALSE),
('empresa_endereco', 'Rua Exemplo, 123 - Centro - Cidade/UF', 'Endereço da empresa', 'STRING', 'EMPRESA', FALSE),
('empresa_telefone', '(00) 0000-0000', 'Telefone da empresa', 'STRING', 'EMPRESA', FALSE),
('empresa_email', 'contato@empresa.com', 'Email da empresa', 'STRING', 'EMPRESA', FALSE),
('mfa_habilitado', 'false', 'MFA habilitado por padrão', 'BOOLEAN', 'SEGURANCA', FALSE),
('backup_automatico', 'true', 'Backup automático habilitado', 'BOOLEAN', 'SISTEMA', FALSE),
('estoque_negativo_permitido', 'false', 'Permitir estoque negativo', 'BOOLEAN', 'SISTEMA', FALSE),
('version', '1.0.0', 'Versão do sistema', 'STRING', 'GERAL', TRUE),
('maintenance_mode', 'false', 'Modo manutenção', 'BOOLEAN', 'SISTEMA', FALSE),
('max_login_attempts', '5', 'Tentativas máximas de login', 'INTEGER', 'SEGURANCA', TRUE),
('session_timeout', '3600', 'Timeout da sessão em segundos', 'INTEGER', 'SEGURANCA', TRUE);

-- ========================================
-- PRODUTOS DE EXEMPLO (COMENTADO - NÃO CRIAR AUTOMATICAMENTE)
-- ========================================
-- INSERT INTO produtos (nome, descricao, preco, departamento, categoria, fornecedor, usuario_cadastro_id) VALUES
-- ('Produto Exemplo 1', 'Descrição do produto exemplo 1', 99.99, '01', 'Categoria A', 'Fornecedor A', 1),
-- ('Produto Exemplo 2', 'Descrição do produto exemplo 2', 149.99, '02', 'Categoria B', 'Fornecedor B', 1),
-- ('Produto Exemplo 3', 'Descrição do produto exemplo 3', 199.99, '03', 'Categoria A', 'Fornecedor C', 1);

-- ========================================
-- ESTOQUE INICIAL PARA OS PRODUTOS DE EXEMPLO (COMENTADO)
-- ========================================
-- INSERT INTO estoque (produto_id, quantidade, quantidade_minima) VALUES
-- ((SELECT id FROM produtos WHERE nome = 'Produto Exemplo 1'), 10, 2),
-- ((SELECT id FROM produtos WHERE nome = 'Produto Exemplo 2'), 15, 3),
-- ((SELECT id FROM produtos WHERE nome = 'Produto Exemplo 3'), 8, 2);

-- ========================================
-- NOTA IMPORTANTE
-- ========================================
-- Os usuários e dados sensíveis devem ser inseridos via:
-- 1. Script separado não versionado (data-local.sql)
-- 2. Configuração manual no ambiente
-- 3. Seeds específicos por ambiente
-- 
-- Para desenvolvimento local, use o arquivo:
-- src/main/resources/db/data-local.sql (não versionado)
