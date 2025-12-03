-- V3__add_barcode_and_sequential_improvements.sql
-- Melhorias para códigos de barras e códigos sequenciais (H2 Compatible)

-- ========================================
-- ALTERAÇÕES NA TABELA PRODUTOS
-- ========================================

-- Adicionar apenas a coluna que não existe na V1
-- Note: tipo_codigo_barras, codigo_interno_sequencial e prefixo_codigo já existem na V1
ALTER TABLE produtos ADD COLUMN codigo_resumido VARCHAR(20);

-- ========================================
-- TABELA DE CONFIGURAÇÕES DE CÓDIGOS
-- ========================================
CREATE TABLE configuracoes_codigo_barras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_codigo VARCHAR(20) NOT NULL,
    prefixo_padrao VARCHAR(10),
    formato_numeracao VARCHAR(50),
    proximo_numero BIGINT DEFAULT 1,
    ativo BOOLEAN DEFAULT true,
    descricao CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- TABELA DE LOCKS DE REGISTRO
-- ========================================
CREATE TABLE locks_registro (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tabela VARCHAR(100) NOT NULL,
    registro_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    tipo_lock VARCHAR(20) NOT NULL CHECK (tipo_lock IN ('read', 'write', 'exclusive')),
    data_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_expiracao TIMESTAMP NOT NULL,
    ativo BOOLEAN DEFAULT true,
    ip_address VARCHAR(45),
    user_agent CLOB,
    observacoes CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para locks de registro
CREATE INDEX idx_locks_tabela_registro ON locks_registro(tabela, registro_id);
CREATE INDEX idx_locks_usuario ON locks_registro(usuario_id);
CREATE INDEX idx_locks_ativo ON locks_registro(ativo, data_expiracao);
CREATE INDEX idx_locks_expiracao ON locks_registro(data_expiracao);

-- ========================================
-- INSERIR CONFIGURAÇÕES PADRÃO DE CÓDIGOS
-- ========================================
INSERT INTO configuracoes_codigo_barras (tipo_codigo, prefixo_padrao, formato_numeracao, proximo_numero, descricao) VALUES
('PERSONALIZADO', 'PROD', 'PROD-{numero:6d}', 1, 'Código personalizado da empresa'),
('CODE39', 'ITEM', 'ITEM-{numero:6d}', 1, 'Code 39 para produtos internos'),
('CODE128', 'P', 'P{numero:6d}', 1, 'Code 128 compacto'),
('EAN13', '789', '789{numero:10d}', 1, 'EAN-13 com prefixo brasileiro'),
('EAN8', '12', '12{numero:6d}', 1, 'EAN-8 simplificado');

-- ========================================
-- ÍNDICES PARA PERFORMANCE
-- ========================================
CREATE INDEX idx_produtos_codigo_resumido ON produtos(codigo_resumido);
CREATE INDEX idx_produtos_codigo_interno ON produtos(codigo_interno_sequencial);
CREATE INDEX idx_produtos_tipo_codigo ON produtos(tipo_codigo_barras);
CREATE INDEX idx_config_codigo_tipo ON configuracoes_codigo_barras(tipo_codigo);

-- ========================================
-- SEQUÊNCIA PARA CÓDIGOS (H2 SEQUENCE)
-- ========================================
CREATE SEQUENCE seq_codigo_produto_resumido START WITH 1;
