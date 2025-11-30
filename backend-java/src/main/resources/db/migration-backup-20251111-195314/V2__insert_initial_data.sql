-- ========================================
-- FLYWAY MIGRATION V2 - DADOS INICIAIS
-- Sistema de Gerenciamento de Estoque e Vendas
-- ========================================

-- ========================================
-- 1. USUÁRIO ADMINISTRADOR PADRÃO
-- ========================================
-- Senha: admin123 (hash bcrypt)
INSERT INTO usuarios (nome, email, senha, tipo_usuario, ativo, role) VALUES 
('Administrador do Sistema', 'admin@sistema.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'OWNER', TRUE, 'ADMIN');

-- ========================================
-- 2. CONFIGURAÇÕES PADRÃO DO SISTEMA
-- ========================================
INSERT INTO configuracao_sistema (chave, valor, descricao, tipo, categoria, obrigatorio) VALUES 
('SISTEMA_NOME', 'Sistema de Estoque e Vendas TCC', 'Nome do sistema', 'STRING', 'GERAL', TRUE),
('SISTEMA_VERSAO', '1.0.0', 'Versão do sistema', 'STRING', 'GERAL', TRUE),
('SISTEMA_DESENVOLVEDOR', 'TCC - Faculdade', 'Desenvolvedor do sistema', 'STRING', 'GERAL', FALSE),

-- Configurações de fidelidade
('PONTOS_POR_REAL', '1', 'Pontos ganhos por real gasto', 'INTEGER', 'FIDELIDADE', TRUE),
('PONTOS_BRONZE_MIN', '0', 'Pontos mínimos para categoria Bronze', 'INTEGER', 'FIDELIDADE', TRUE),
('PONTOS_PRATA_MIN', '200', 'Pontos mínimos para categoria Prata', 'INTEGER', 'FIDELIDADE', TRUE),
('PONTOS_OURO_MIN', '500', 'Pontos mínimos para categoria Ouro', 'INTEGER', 'FIDELIDADE', TRUE),
('PONTOS_DIAMANTE_MIN', '1000', 'Pontos mínimos para categoria Diamante', 'INTEGER', 'FIDELIDADE', TRUE),

-- Configurações de email
('EMAIL_REMETENTE', 'noreply@sistema.com', 'Email remetente do sistema', 'STRING', 'EMAIL', TRUE),
('EMAIL_NOME_REMETENTE', 'Sistema de Estoque e Vendas', 'Nome do remetente', 'STRING', 'EMAIL', TRUE),
('EMAIL_ATIVO', 'false', 'Sistema de email ativo', 'BOOLEAN', 'EMAIL', TRUE),

-- Configurações de sistema
('BACKUP_AUTOMATICO', 'true', 'Ativar backup automático diário', 'BOOLEAN', 'SISTEMA', TRUE),
('MANUTENCAO_MODO', 'false', 'Sistema em modo manutenção', 'BOOLEAN', 'SISTEMA', TRUE),
('LOG_NIVEL', 'INFO', 'Nível de log do sistema', 'STRING', 'SISTEMA', 1),
('SESSAO_TIMEOUT', '86400', 'Timeout da sessão em segundos (24h)', 'INTEGER', 'SISTEMA', 1),

-- Configurações de estoque
('ESTOQUE_ALERTA_BAIXO', 'true', 'Alertar quando estoque baixo', 'BOOLEAN', 'ESTOQUE', 1),
('ESTOQUE_MINIMO_PADRAO', '5', 'Estoque mínimo padrão para novos produtos', 'INTEGER', 'ESTOQUE', 1),
('CODIGO_PRODUTO_PREFIXO', 'PROD', 'Prefixo padrão para códigos de produtos', 'STRING', 'ESTOQUE', 1),
('CODIGO_PRODUTO_SEQUENCIAL', '1', 'Próximo número sequencial para produtos', 'INTEGER', 'ESTOQUE', 1),

-- Configurações de vendas
('DESCONTO_MAXIMO_PERCENTUAL', '50', 'Desconto máximo permitido em percentual', 'INTEGER', 'VENDAS', 1),
('FORMA_PAGAMENTO_PADRAO', 'DINHEIRO', 'Forma de pagamento padrão', 'STRING', 'VENDAS', 1),
('NOTA_FISCAL_ATIVA', 'false', 'Emissão de nota fiscal ativa', 'BOOLEAN', 'VENDAS', 0),

-- Configurações de segurança
('LOGIN_TENTATIVAS_MAX', '5', 'Máximo de tentativas de login', 'INTEGER', 'SEGURANCA', 1),
('LOGIN_BLOQUEIO_TEMPO', '900', 'Tempo de bloqueio em segundos (15min)', 'INTEGER', 'SEGURANCA', 1),
('SENHA_TAMANHO_MIN', '6', 'Tamanho mínimo da senha', 'INTEGER', 'SEGURANCA', 1),
('SENHA_EXIGIR_MAIUSCULA', 'true', 'Exigir letra maiúscula na senha', 'BOOLEAN', 'SEGURANCA', 1),
('SENHA_EXIGIR_NUMERO', 'true', 'Exigir número na senha', 'BOOLEAN', 'SEGURANCA', 1),
('SENHA_EXIGIR_ESPECIAL', 'false', 'Exigir caractere especial na senha', 'BOOLEAN', 'SEGURANCA', 0),

-- Configurações de relatórios
('RELATORIO_REGISTROS_MAX', '1000', 'Máximo de registros por relatório', 'INTEGER', 'RELATORIO', 1),
('RELATORIO_CACHE_ATIVO', 'true', 'Cache de relatórios ativo', 'BOOLEAN', 'RELATORIO', 1),
('RELATORIO_FORMATO_PADRAO', 'PDF', 'Formato padrão para relatórios', 'STRING', 'RELATORIO', 1);

-- ========================================
-- 3. CLIENTE PADRÃO PARA VENDAS SEM IDENTIFICAÇÃO
-- ========================================
INSERT INTO clientes (nome, cpf, email, telefone, endereco_logradouro, endereco_cidade, endereco_estado, endereco_cep, nivel_fidelidade, ativo, data_cadastro) VALUES 
('Cliente Não Identificado', '000.000.000-00', 'nao-identificado@sistema.local', '(00) 00000-0000', 'Endereço não informado', 'Cidade não informada', 'UF', '00000-000', 'BRONZE', TRUE, CURRENT_TIMESTAMP);

-- ========================================
-- 4. CATEGORIAS PADRÃO DE PRODUTOS
-- ========================================
-- Estas categorias serão usadas como sugestões no frontend
-- Não é uma tabela separada, apenas dados de exemplo para facilitar o uso

-- ========================================
-- 5. USUÁRIO VENDEDOR DE EXEMPLO
-- ========================================
-- Senha: vendedor123 (hash bcrypt)
INSERT INTO usuarios (nome, email, senha, telefone, cpf, tipo_usuario, ativo, role) VALUES 
('João Silva', 'vendedor@sistema.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '(11) 99999-9999', '123.456.789-00', 'VENDEDOR', TRUE, 'USER');

-- ========================================
-- 6. PRODUTO DE EXEMPLO (COMENTADO - NÃO CRIAR AUTOMATICAMENTE)
-- ========================================
-- INSERT INTO produtos (nome, descricao, preco, custo_unitario, departamento, categoria, marca, unidade_medida, codigo_barras, prefixo_codigo, tipo_codigo_barras, fornecedor, ativo, margem, usuario_cadastro_id) VALUES 
-- ('Produto Exemplo', 'Produto de exemplo para teste do sistema', 19.90, 10.00, '01', 'Eletrônicos', 'Marca Teste', 'UN', 'PROD-000001', 'PROD', 'EAN13', 'Fornecedor Exemplo Ltda', TRUE, 50.00, 1);