-- ========================================
-- FLYWAY MIGRATION V1 - H2 DATABASE
-- Sistema de Gerenciamento de Estoque e Vendas
-- ========================================

-- ========================================
-- 1. TABELA DE USUÁRIOS
-- ========================================
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    telefone VARCHAR(20),
    cpf VARCHAR(14),
    tipo_usuario VARCHAR(20) NOT NULL DEFAULT 'VENDEDOR',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso TIMESTAMP,
    tentativas_login INTEGER DEFAULT 0,
    bloqueado BOOLEAN DEFAULT FALSE,
    data_bloqueio TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(20) DEFAULT 'USER',
    
    CHECK (tipo_usuario IN ('OWNER', 'ADMIN', 'VENDEDOR')),
    CHECK (LENGTH(nome) >= 2)
);

-- ========================================
-- 2. TABELA DE CLIENTES
-- ========================================
CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    email VARCHAR(255),
    telefone VARCHAR(20),
    data_nascimento DATE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Dados de endereço
    endereco_cep VARCHAR(10),
    endereco_logradouro VARCHAR(255),
    endereco_numero VARCHAR(10),
    endereco_complemento VARCHAR(100),
    endereco_bairro VARCHAR(100),
    endereco_cidade VARCHAR(100),
    endereco_estado VARCHAR(2),
    
    -- Dados de fidelização
    pontos_fidelidade INTEGER DEFAULT 0,
    nivel_fidelidade VARCHAR(20) DEFAULT 'BRONZE',
    data_ultima_compra TIMESTAMP,
    
    CHECK (LENGTH(nome) >= 2),
    CHECK (nivel_fidelidade IN ('BRONZE', 'PRATA', 'OURO', 'DIAMANTE'))
);

-- ========================================
-- 3. TABELA DE PRODUTOS
-- ========================================
CREATE TABLE produtos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    preco DECIMAL(10,2) NOT NULL,
    custo_unitario DECIMAL(10,2) DEFAULT 0.00,
    departamento VARCHAR(10) NOT NULL,
    categoria VARCHAR(100),
    marca VARCHAR(100),
    unidade_medida VARCHAR(10) DEFAULT 'UN',
    peso DECIMAL(8,3),
    dimensoes VARCHAR(50),
    codigo_barras VARCHAR(50) UNIQUE,
    codigo_interno_sequencial BIGINT,
    prefixo_codigo VARCHAR(10) DEFAULT 'PROD',
    tipo_codigo_barras VARCHAR(20) DEFAULT 'EAN13',
    fornecedor VARCHAR(255),
    margem DECIMAL(5,2),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_cadastro_id BIGINT,
    
    CHECK (preco >= 0),
    CHECK (custo_unitario >= 0),
    CHECK (tipo_codigo_barras IN ('EAN13', 'PERSONALIZADO'))
);

-- ========================================
-- 4. TABELA DE TAMANHOS DE PRODUTO
-- ========================================
CREATE TABLE produto_tamanhos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    tamanho VARCHAR(10) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    
    UNIQUE (produto_id, tamanho)
);

-- ========================================
-- 5. TABELA DE ESTOQUE
-- ========================================
CREATE TABLE estoque (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    tamanho VARCHAR(10),
    quantidade DECIMAL(10,2) NOT NULL DEFAULT 0,
    quantidade_minima DECIMAL(10,2) DEFAULT 0,
    quantidade_maxima DECIMAL(10,2),
    localizacao VARCHAR(100),
    data_ultima_movimentacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE (produto_id, tamanho)
);

-- ========================================
-- 6. TABELA DE MOVIMENTAÇÕES DE ESTOQUE
-- ========================================
CREATE TABLE movimentacoes_estoque (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    tamanho VARCHAR(10),
    tipo_movimentacao VARCHAR(20) NOT NULL,
    quantidade DECIMAL(10,2) NOT NULL,
    quantidade_anterior DECIMAL(10,2),
    quantidade_atual DECIMAL(10,2),
    observacao TEXT,
    data_movimentacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_id BIGINT,
    venda_id BIGINT,
    
    CHECK (tipo_movimentacao IN ('ENTRADA', 'SAIDA', 'AJUSTE', 'TRANSFERENCIA'))
);

-- ========================================
-- 7. TABELA DE VENDAS
-- ========================================
CREATE TABLE vendas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    usuario_id BIGINT NOT NULL,
    data_venda TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valor_total DECIMAL(10,2) NOT NULL,
    desconto DECIMAL(10,2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'FINALIZADA',
    forma_pagamento VARCHAR(50),
    observacoes TEXT,
    
    CHECK (valor_total >= 0),
    CHECK (desconto >= 0),
    CHECK (status IN ('PENDENTE', 'FINALIZADA', 'CANCELADA'))
);

-- ========================================
-- 8. TABELA DE ITENS DE VENDA
-- ========================================
CREATE TABLE itens_venda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venda_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    tamanho VARCHAR(10),
    quantidade DECIMAL(10,2) NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    desconto_item DECIMAL(10,2) DEFAULT 0,
    
    CHECK (quantidade > 0),
    CHECK (preco_unitario >= 0),
    CHECK (subtotal >= 0),
    CHECK (desconto_item >= 0)
);

-- ========================================
-- 9. TABELA DE CONFIGURAÇÕES DO SISTEMA
-- ========================================
CREATE TABLE configuracao_sistema (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chave VARCHAR(100) NOT NULL UNIQUE,
    valor VARCHAR(500) NOT NULL,
    descricao TEXT,
    tipo VARCHAR(20) DEFAULT 'STRING',
    categoria VARCHAR(50) DEFAULT 'GERAL',
    obrigatorio BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CHECK (tipo IN ('STRING', 'INTEGER', 'BOOLEAN', 'DECIMAL', 'DATE'))
);

-- ========================================
-- FOREIGN KEYS
-- ========================================
ALTER TABLE produtos ADD CONSTRAINT FK_produtos_usuario 
    FOREIGN KEY (usuario_cadastro_id) REFERENCES usuarios(id);

ALTER TABLE produto_tamanhos ADD CONSTRAINT FK_produto_tamanhos_produto 
    FOREIGN KEY (produto_id) REFERENCES produtos(id) ON DELETE CASCADE;

ALTER TABLE estoque ADD CONSTRAINT FK_estoque_produto 
    FOREIGN KEY (produto_id) REFERENCES produtos(id) ON DELETE CASCADE;

ALTER TABLE movimentacoes_estoque ADD CONSTRAINT FK_movimentacoes_produto 
    FOREIGN KEY (produto_id) REFERENCES produtos(id);

ALTER TABLE movimentacoes_estoque ADD CONSTRAINT FK_movimentacoes_usuario 
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE movimentacoes_estoque ADD CONSTRAINT FK_movimentacoes_venda 
    FOREIGN KEY (venda_id) REFERENCES vendas(id);

ALTER TABLE vendas ADD CONSTRAINT FK_vendas_cliente 
    FOREIGN KEY (cliente_id) REFERENCES clientes(id);

ALTER TABLE vendas ADD CONSTRAINT FK_vendas_usuario 
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE itens_venda ADD CONSTRAINT FK_itens_venda_venda 
    FOREIGN KEY (venda_id) REFERENCES vendas(id) ON DELETE CASCADE;

ALTER TABLE itens_venda ADD CONSTRAINT FK_itens_venda_produto 
    FOREIGN KEY (produto_id) REFERENCES produtos(id);

-- ========================================
-- ÍNDICES PARA PERFORMANCE
-- ========================================
CREATE INDEX IX_usuarios_email ON usuarios(email);
CREATE INDEX IX_usuarios_tipo_usuario ON usuarios(tipo_usuario);
CREATE INDEX IX_usuarios_ativo ON usuarios(ativo);

CREATE INDEX IX_clientes_cpf ON clientes(cpf);
CREATE INDEX IX_clientes_ativo ON clientes(ativo);

CREATE INDEX IX_produtos_departamento ON produtos(departamento);
CREATE INDEX IX_produtos_categoria ON produtos(categoria);
CREATE INDEX IX_produtos_codigo_barras ON produtos(codigo_barras);
CREATE INDEX IX_produtos_ativo ON produtos(ativo);

CREATE INDEX IX_estoque_produto_id ON estoque(produto_id);
CREATE INDEX IX_movimentacoes_produto_id ON movimentacoes_estoque(produto_id);
CREATE INDEX IX_movimentacoes_data ON movimentacoes_estoque(data_movimentacao);

CREATE INDEX IX_vendas_data ON vendas(data_venda);
CREATE INDEX IX_vendas_cliente_id ON vendas(cliente_id);
CREATE INDEX IX_vendas_usuario_id ON vendas(usuario_id);

CREATE INDEX IX_itens_venda_venda_id ON itens_venda(venda_id);
CREATE INDEX IX_itens_venda_produto_id ON itens_venda(produto_id);

CREATE INDEX IX_configuracao_chave ON configuracao_sistema(chave);
CREATE INDEX IX_configuracao_categoria ON configuracao_sistema(categoria);
CREATE INDEX IX_configuracao_tipo ON configuracao_sistema(tipo);