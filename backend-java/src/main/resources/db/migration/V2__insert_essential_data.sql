-- ========================================
-- FLYWAY MIGRATION V2 - DADOS INICIAIS ESSENCIAIS
-- Sistema de Gerenciamento de Estoque e Vendas
-- ========================================

-- ========================================
-- 1. USUÁRIO ADMINISTRADOR PADRÃO
-- ========================================
-- Senha: admin123 (hash bcrypt)
INSERT INTO usuarios (nome, email, senha, tipo_usuario, ativo, role) VALUES 
('Administrador do Sistema', 'admin@sistema.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'OWNER', TRUE, 'ADMIN');

-- ========================================
-- 2. CLIENTE FAKE PARA VENDAS ANÔNIMAS
-- ========================================
INSERT INTO clientes (
    nome, cpf, email, telefone, ativo, is_fake, categoria, 
    total_compras, quantidade_compras, pontos, pontos_fidelidade
) VALUES (
    'Cliente Não Identificado', 
    '000.000.000-00', 
    'nao-identificado@sistema.local', 
    '(00) 00000-0000', 
    TRUE, 
    TRUE, 
    'BRONZE', 
    0.00, 
    0, 
    0, 
    0
);

-- ========================================
-- 3. CONFIGURAÇÕES BÁSICAS DO SISTEMA
-- ========================================
INSERT INTO configuracoes_sistema (chave, valor, descricao, tipo) VALUES 
('SISTEMA_NOME', 'Sistema de Estoque e Vendas TCC', 'Nome do sistema', 'STRING'),
('SISTEMA_VERSAO', '1.0.0', 'Versão do sistema', 'STRING'),
('SISTEMA_DESENVOLVEDOR', 'TCC - Faculdade', 'Desenvolvedor do sistema', 'STRING'),
('VENDAS_PERMITIR_ESTOQUE_NEGATIVO', 'false', 'Permitir vendas com estoque negativo', 'BOOLEAN'),
('ESTOQUE_ALERTA_MINIMO', '10', 'Quantidade mínima para alerta de estoque', 'INTEGER'),
('PONTOS_POR_REAL', '1', 'Quantidade de pontos ganhos por real gasto', 'INTEGER'),
('DESCONTO_MAXIMO_VENDEDOR', '15.00', 'Desconto máximo que um vendedor pode aplicar (%)', 'DECIMAL'),
('EMAIL_NOTIFICACOES', 'true', 'Enviar notificações por email', 'BOOLEAN'),
('BACKUP_AUTOMATICO', 'true', 'Realizar backup automático dos dados', 'BOOLEAN'),
('DIAS_BACKUP', '7', 'Intervalo em dias para backup automático', 'INTEGER');