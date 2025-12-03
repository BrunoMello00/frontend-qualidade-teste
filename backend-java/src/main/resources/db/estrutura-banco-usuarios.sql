-- ===================================
-- SISTEMA DE GESTÃO DE USUÁRIOS
-- ===================================

-- Enum para tipos de usuário
CREATE TYPE tipo_usuario AS ENUM ('ADMIN', 'VENDEDOR', 'GERENTE', 'ESTOQUISTA');

-- Enum para status do usuário
CREATE TYPE status_usuario AS ENUM ('ATIVO', 'INATIVO', 'BLOQUEADO', 'PENDENTE');

-- Enum para tipos de ação na auditoria
CREATE TYPE tipo_acao AS ENUM ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'PASSWORD_CHANGE', 'ACCESS_DENIED');

-- ===================================
-- TABELA DE USUÁRIOS
-- ===================================
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    tipo_usuario tipo_usuario NOT NULL DEFAULT 'VENDEDOR',
    codigo_vendedor VARCHAR(20) UNIQUE,
    status status_usuario DEFAULT 'PENDENTE',
    primeiro_acesso BOOLEAN DEFAULT true,
    ultimo_login TIMESTAMP,
    tentativas_login INTEGER DEFAULT 0,
    bloqueado_ate TIMESTAMP,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    criado_por UUID REFERENCES usuarios(id),
    atualizado_por UUID REFERENCES usuarios(id),
    avatar_url TEXT,
    telefone VARCHAR(20),
    observacoes TEXT,
    meta_mensal DECIMAL(10,2) DEFAULT 0,
    comissao_percentual DECIMAL(5,2) DEFAULT 0,
    ativo BOOLEAN DEFAULT true
);

-- ===================================
-- TABELA DE PERMISSÕES
-- ===================================
CREATE TABLE permissoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) UNIQUE NOT NULL,
    descricao TEXT,
    modulo VARCHAR(50) NOT NULL, -- 'PRODUTOS', 'VENDAS', 'RELATORIOS', 'USUARIOS', 'CONFIGURACOES'
    acao VARCHAR(50) NOT NULL, -- 'CREATE', 'READ', 'UPDATE', 'DELETE', 'EXPORT'
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===================================
-- TABELA DE PERMISSÕES POR TIPO DE USUÁRIO
-- ===================================
CREATE TABLE tipo_usuario_permissoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_usuario tipo_usuario NOT NULL,
    permissao_id UUID NOT NULL REFERENCES permissoes(id),
    concedida BOOLEAN DEFAULT true,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tipo_usuario, permissao_id)
);

-- ===================================
-- TABELA DE AUDITORIA
-- ===================================
CREATE TABLE auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID REFERENCES usuarios(id),
    nome_usuario VARCHAR(255),
    tipo_acao tipo_acao NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    recurso VARCHAR(100),
    recurso_id VARCHAR(255),
    dados_anteriores JSONB,
    dados_novos JSONB,
    ip_address INET,
    user_agent TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sucesso BOOLEAN DEFAULT true,
    detalhes TEXT
);

-- ===================================
-- TABELA DE SESSÕES
-- ===================================
CREATE TABLE sessoes_usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    token_hash VARCHAR(255) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_expiracao TIMESTAMP NOT NULL,
    ativo BOOLEAN DEFAULT true,
    ultimo_acesso TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===================================
-- TABELA DE METAS DE VENDEDORES
-- ===================================
CREATE TABLE metas_vendedores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendedor_id UUID NOT NULL REFERENCES usuarios(id),
    ano INTEGER NOT NULL,
    mes INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    meta_vendas DECIMAL(12,2) NOT NULL DEFAULT 0,
    meta_quantidade INTEGER DEFAULT 0,
    valor_atingido DECIMAL(12,2) DEFAULT 0,
    quantidade_atingida INTEGER DEFAULT 0,
    comissao_calculada DECIMAL(10,2) DEFAULT 0,
    bonus DECIMAL(10,2) DEFAULT 0,
    observacoes TEXT,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    criado_por UUID REFERENCES usuarios(id),
    UNIQUE(vendedor_id, ano, mes)
);

-- ===================================
-- TABELA DE TOKENS DE REDEFINIÇÃO
-- ===================================
CREATE TABLE tokens_redefinicao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    tipo VARCHAR(50) NOT NULL, -- 'PASSWORD_RESET', 'EMAIL_VERIFICATION', 'INVITATION'
    usado BOOLEAN DEFAULT false,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_expiracao TIMESTAMP NOT NULL,
    dados_extras JSONB -- Para guardar informações adicionais conforme o tipo
);

-- ===================================
-- ATUALIZAR TABELA DE VENDAS
-- ===================================
ALTER TABLE vendas ADD COLUMN IF NOT EXISTS vendedor_id UUID REFERENCES usuarios(id);
ALTER TABLE vendas ADD COLUMN IF NOT EXISTS comissao_vendedor DECIMAL(10,2) DEFAULT 0;

-- ===================================
-- ATUALIZAR TABELA DE PRODUTOS
-- ===================================
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS criado_por UUID REFERENCES usuarios(id);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS atualizado_por UUID REFERENCES usuarios(id);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ===================================
-- TRIGGERS PARA AUDITORIA AUTOMÁTICA
-- ===================================

-- Função para inserir logs de auditoria
CREATE OR REPLACE FUNCTION inserir_log_auditoria()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO auditoria (
            modulo, tipo_acao, recurso, recurso_id, 
            dados_novos, timestamp
        ) VALUES (
            TG_TABLE_NAME, 'CREATE', TG_TABLE_NAME, NEW.id::text, 
            row_to_json(NEW), CURRENT_TIMESTAMP
        );
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO auditoria (
            modulo, tipo_acao, recurso, recurso_id, 
            dados_anteriores, dados_novos, timestamp
        ) VALUES (
            TG_TABLE_NAME, 'UPDATE', TG_TABLE_NAME, NEW.id::text, 
            row_to_json(OLD), row_to_json(NEW), CURRENT_TIMESTAMP
        );
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO auditoria (
            modulo, tipo_acao, recurso, recurso_id, 
            dados_anteriores, timestamp
        ) VALUES (
            TG_TABLE_NAME, 'DELETE', TG_TABLE_NAME, OLD.id::text, 
            row_to_json(OLD), CURRENT_TIMESTAMP
        );
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Aplicar triggers nas tabelas principais
CREATE TRIGGER trigger_auditoria_usuarios
    AFTER INSERT OR UPDATE OR DELETE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION inserir_log_auditoria();

CREATE TRIGGER trigger_auditoria_produtos
    AFTER INSERT OR UPDATE OR DELETE ON produtos
    FOR EACH ROW EXECUTE FUNCTION inserir_log_auditoria();

CREATE TRIGGER trigger_auditoria_vendas
    AFTER INSERT OR UPDATE OR DELETE ON vendas
    FOR EACH ROW EXECUTE FUNCTION inserir_log_auditoria();

-- ===================================
-- INSERIR PERMISSÕES PADRÃO
-- ===================================

-- Permissões para módulo PRODUTOS
INSERT INTO permissoes (nome, descricao, modulo, acao) VALUES
('produtos.create', 'Criar novos produtos', 'PRODUTOS', 'CREATE'),
('produtos.read', 'Visualizar produtos', 'PRODUTOS', 'READ'),
('produtos.update', 'Editar produtos existentes', 'PRODUTOS', 'UPDATE'),
('produtos.delete', 'Excluir produtos', 'PRODUTOS', 'DELETE'),
('produtos.export', 'Exportar lista de produtos', 'PRODUTOS', 'EXPORT');

-- Permissões para módulo VENDAS
INSERT INTO permissoes (nome, descricao, modulo, acao) VALUES
('vendas.create', 'Realizar vendas', 'VENDAS', 'CREATE'),
('vendas.read', 'Visualizar vendas', 'VENDAS', 'READ'),
('vendas.read_all', 'Visualizar todas as vendas', 'VENDAS', 'READ'),
('vendas.update', 'Editar vendas', 'VENDAS', 'UPDATE'),
('vendas.delete', 'Cancelar vendas', 'VENDAS', 'DELETE'),
('vendas.export', 'Exportar relatórios de vendas', 'VENDAS', 'EXPORT');

-- Permissões para módulo RELATÓRIOS
INSERT INTO permissoes (nome, descricao, modulo, acao) VALUES
('relatorios.dashboard', 'Acessar dashboard geral', 'RELATORIOS', 'READ'),
('relatorios.vendas', 'Relatórios de vendas', 'RELATORIOS', 'READ'),
('relatorios.produtos', 'Relatórios de produtos', 'RELATORIOS', 'READ'),
('relatorios.financeiro', 'Relatórios financeiros', 'RELATORIOS', 'READ'),
('relatorios.export', 'Exportar relatórios', 'RELATORIOS', 'EXPORT');

-- Permissões para módulo USUÁRIOS
INSERT INTO permissoes (nome, descricao, modulo, acao) VALUES
('usuarios.create', 'Criar novos usuários', 'USUARIOS', 'CREATE'),
('usuarios.read', 'Visualizar usuários', 'USUARIOS', 'READ'),
('usuarios.update', 'Editar usuários', 'USUARIOS', 'UPDATE'),
('usuarios.delete', 'Excluir usuários', 'USUARIOS', 'DELETE'),
('usuarios.reset_password', 'Resetar senhas', 'USUARIOS', 'UPDATE');

-- Permissões para módulo CONFIGURAÇÕES
INSERT INTO permissoes (nome, descricao, modulo, acao) VALUES
('config.system', 'Configurações do sistema', 'CONFIGURACOES', 'UPDATE'),
('config.backup', 'Backup e restauração', 'CONFIGURACOES', 'CREATE'),
('auditoria.read', 'Visualizar logs de auditoria', 'AUDITORIA', 'READ');

-- ===================================
-- CONFIGURAR PERMISSÕES POR TIPO DE USUÁRIO
-- ===================================

-- ADMINISTRADOR: Todas as permissões
INSERT INTO tipo_usuario_permissoes (tipo_usuario, permissao_id)
SELECT 'ADMIN', id FROM permissoes;

-- GERENTE: Quase todas, exceto configurações críticas
INSERT INTO tipo_usuario_permissoes (tipo_usuario, permissao_id)
SELECT 'GERENTE', id FROM permissoes 
WHERE nome NOT IN ('config.system', 'config.backup', 'usuarios.delete');

-- VENDEDOR: Apenas vendas e visualização básica
INSERT INTO tipo_usuario_permissoes (tipo_usuario, permissao_id)
SELECT 'VENDEDOR', id FROM permissoes 
WHERE nome IN (
    'produtos.read', 'vendas.create', 'vendas.read', 
    'relatorios.dashboard'
);

-- ESTOQUISTA: Produtos e estoque
INSERT INTO tipo_usuario_permissoes (tipo_usuario, permissao_id)
SELECT 'ESTOQUISTA', id FROM permissoes 
WHERE modulo IN ('PRODUTOS') OR nome = 'relatorios.produtos';

-- ===================================
-- CRIAR USUÁRIO ADMINISTRADOR PADRÃO
-- ===================================
INSERT INTO usuarios (
    nome, email, senha_hash, tipo_usuario, 
    codigo_vendedor, status, primeiro_acesso,
    data_criacao
) VALUES (
    'Administrador',
    'admin@sistema.com',
    '$2b$10$example_hash_here', -- Você deve gerar um hash real
    'ADMIN',
    'ADM001',
    'ATIVO',
    false,
    CURRENT_TIMESTAMP
);

-- ===================================
-- ÍNDICES PARA PERFORMANCE
-- ===================================
CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_codigo_vendedor ON usuarios(codigo_vendedor);
CREATE INDEX idx_usuarios_tipo ON usuarios(tipo_usuario);
CREATE INDEX idx_usuarios_status ON usuarios(status);
CREATE INDEX idx_auditoria_usuario_id ON auditoria(usuario_id);
CREATE INDEX idx_auditoria_timestamp ON auditoria(timestamp);
CREATE INDEX idx_auditoria_modulo ON auditoria(modulo);
CREATE INDEX idx_sessoes_usuario_id ON sessoes_usuario(usuario_id);
CREATE INDEX idx_sessoes_token_hash ON sessoes_usuario(token_hash);
CREATE INDEX idx_metas_vendedor_ano_mes ON metas_vendedores(vendedor_id, ano, mes);

-- ===================================
-- VIEWS ÚTEIS
-- ===================================

-- View para estatísticas de vendedores
CREATE VIEW vw_estatisticas_vendedores AS
SELECT 
    u.id,
    u.nome,
    u.codigo_vendedor,
    u.email,
    COUNT(v.id) as total_vendas,
    COALESCE(SUM(v.total), 0) as valor_total_vendas,
    COALESCE(AVG(v.total), 0) as ticket_medio,
    MAX(v.data_venda) as ultima_venda,
    u.meta_mensal,
    u.comissao_percentual
FROM usuarios u
LEFT JOIN vendas v ON u.id = v.vendedor_id 
    AND v.data_venda >= date_trunc('month', CURRENT_DATE)
WHERE u.tipo_usuario IN ('VENDEDOR', 'GERENTE')
    AND u.status = 'ATIVO'
GROUP BY u.id, u.nome, u.codigo_vendedor, u.email, u.meta_mensal, u.comissao_percentual;

-- View para auditoria resumida
CREATE VIEW vw_auditoria_resumida AS
SELECT 
    a.*,
    u.nome as nome_usuario_completo,
    u.tipo_usuario
FROM auditoria a
LEFT JOIN usuarios u ON a.usuario_id = u.id
ORDER BY a.timestamp DESC;

-- ===================================
-- COMENTÁRIOS NAS TABELAS
-- ===================================
COMMENT ON TABLE usuarios IS 'Tabela principal de usuários do sistema';
COMMENT ON TABLE permissoes IS 'Definição de permissões granulares';
COMMENT ON TABLE tipo_usuario_permissoes IS 'Associação entre tipos de usuário e permissões';
COMMENT ON TABLE auditoria IS 'Log completo de todas as ações no sistema';
COMMENT ON TABLE sessoes_usuario IS 'Controle de sessões ativas';
COMMENT ON TABLE metas_vendedores IS 'Metas mensais e comissões dos vendedores';
COMMENT ON TABLE tokens_redefinicao IS 'Tokens para redefinição de senha e convites';
