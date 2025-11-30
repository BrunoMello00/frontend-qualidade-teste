-- ========================================
-- FLYWAY MIGRATION V1 - ESTRUTURA COMPLETA
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
-- 2. TABELA DE CLIENTES - ESTRUTURA COMPLETA
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
    
    -- Dados de endereço completos
    endereco_cep VARCHAR(10),
    endereco_logradouro VARCHAR(255),
    endereco_numero VARCHAR(10),
    endereco_complemento VARCHAR(100),
    endereco_bairro VARCHAR(100),
    endereco_cidade VARCHAR(100),
    endereco_estado VARCHAR(2),
    
    -- Campos de endereço alternativos (para compatibilidade)
    cep VARCHAR(10),
    rua VARCHAR(255),
    numero VARCHAR(10),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2),
    
    -- Dados de fidelização completos
    pontos_fidelidade INTEGER DEFAULT 0,
    pontos INTEGER DEFAULT 0,
    nivel_fidelidade VARCHAR(20) DEFAULT 'BRONZE',
    categoria VARCHAR(20) DEFAULT 'BRONZE',
    data_ultima_compra TIMESTAMP,
    ultima_compra TIMESTAMP,
    total_compras DECIMAL(10,2) DEFAULT 0.00,
    quantidade_compras INTEGER DEFAULT 0,
    observacoes TEXT,
    
    -- Campos de controle
    is_fake BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CHECK (LENGTH(nome) >= 2),
    CHECK (nivel_fidelidade IN ('BRONZE', 'PRATA', 'OURO', 'DIAMANTE')),
    CHECK (categoria IN ('BRONZE', 'PRATA', 'OURO', 'DIAMANTE')),
    CHECK (pontos >= 0),
    CHECK (pontos_fidelidade >= 0),
    CHECK (total_compras >= 0.00),
    CHECK (quantidade_compras >= 0)
);

-- ========================================
-- 3. TABELA DE PRODUTOS
-- ========================================
CREATE TABLE produtos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    descricao TEXT,
    codigo VARCHAR(50),
    preco DECIMAL(10,2) NOT NULL,
    custo_unitario DECIMAL(10,2) DEFAULT 0.00,
    departamento VARCHAR(100) NOT NULL,
    categoria VARCHAR(100),
    marca VARCHAR(100),
    unidade_medida VARCHAR(10) DEFAULT 'UN',
    peso DECIMAL(8,3),
    dimensoes VARCHAR(50),
    codigo_barras VARCHAR(100),
    codigo_interno_sequencial BIGINT,
    prefixo_codigo VARCHAR(10) DEFAULT 'PROD',
    tipo_codigo_barras VARCHAR(20) DEFAULT 'EAN13',
    fornecedor VARCHAR(150),
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
CREATE TABLE tamanhos_produto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    tamanho VARCHAR(50) NOT NULL,
    estoque INTEGER NOT NULL DEFAULT 0,
    preco DECIMAL(10,2),
    codigo_barras VARCHAR(100),
    vendidas INTEGER DEFAULT 0,
    ativo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (produto_id) REFERENCES produtos(id) ON DELETE CASCADE,
    CHECK (estoque >= 0),
    CHECK (vendidas >= 0)
);

-- ========================================
-- 5. TABELA DE VENDAS
-- ========================================
CREATE TABLE vendas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    cliente_nome VARCHAR(200) NOT NULL,
    cliente_email VARCHAR(150),
    cliente_telefone VARCHAR(20),
    data_venda TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_confirmacao TIMESTAMP,
    data_cancelamento TIMESTAMP,
    data_entrega TIMESTAMP,
    subtotal DECIMAL(12,2) NOT NULL,
    desconto DECIMAL(12,2) DEFAULT 0.00,
    valor_total DECIMAL(12,2) NOT NULL,
    forma_pagamento VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    observacoes TEXT,
    motivo_cancelamento TEXT,
    vendedor_id BIGINT,
    evento_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (vendedor_id) REFERENCES usuarios(id),
    CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'CANCELADA', 'ENTREGUE')),
    CHECK (subtotal >= 0),
    CHECK (desconto >= 0),
    CHECK (valor_total >= 0)
);

-- ========================================
-- 6. TABELA DE ITENS DE VENDA
-- ========================================
CREATE TABLE itens_venda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venda_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    tamanho_produto_id BIGINT,
    nome_produto VARCHAR(255) NOT NULL,
    tamanho VARCHAR(50),
    quantidade INTEGER NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    desconto_item DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (venda_id) REFERENCES vendas(id) ON DELETE CASCADE,
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (tamanho_produto_id) REFERENCES tamanhos_produto(id),
    CHECK (quantidade > 0),
    CHECK (preco_unitario >= 0),
    CHECK (subtotal >= 0),
    CHECK (desconto_item >= 0)
);

-- ========================================
-- 7. TABELA DE MOVIMENTAÇÕES DE ESTOQUE
-- ========================================
CREATE TABLE movimentacoes_estoque (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    tamanho_produto_id BIGINT,
    tipo ENUM('ENTRADA', 'SAIDA', 'AJUSTE') NOT NULL,
    quantidade INTEGER NOT NULL,
    quantidade_anterior INTEGER NOT NULL,
    quantidade_atual INTEGER NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    observacoes TEXT,
    data_movimentacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id BIGINT,
    venda_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (tamanho_produto_id) REFERENCES tamanhos_produto(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (venda_id) REFERENCES vendas(id),
    CHECK (quantidade_anterior >= 0),
    CHECK (quantidade_atual >= 0)
);

-- ========================================
-- 8. TABELA DE EVENTOS
-- ========================================
CREATE TABLE eventos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP,
    local VARCHAR(200),
    endereco_completo VARCHAR(500),
    publico BOOLEAN NOT NULL DEFAULT TRUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    status ENUM('PLANEJADO', 'ATIVO', 'PAUSADO', 'CONCLUIDO', 'CANCELADO') NOT NULL DEFAULT 'PLANEJADO',
    meta_vendas DECIMAL(15,2),
    meta_quantidade_vendas INTEGER,
    desconto_percentual DECIMAL(5,2),
    desconto_valor DECIMAL(10,2),
    observacoes TEXT,
    criado_por BIGINT NOT NULL,
    atualizado_por BIGINT,
    
    FOREIGN KEY (criado_por) REFERENCES usuarios(id),
    FOREIGN KEY (atualizado_por) REFERENCES usuarios(id),
    CHECK (data_fim >= data_inicio),
    CHECK (meta_vendas >= 0),
    CHECK (meta_quantidade_vendas >= 0),
    CHECK (desconto_percentual >= 0 AND desconto_percentual <= 100),
    CHECK (desconto_valor >= 0)
);

-- ========================================
-- 9. TABELA DE HISTÓRICO DE PONTOS
-- ========================================
CREATE TABLE historico_pontos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    pontos_antes INTEGER NOT NULL,
    pontos_adicionados INTEGER NOT NULL,
    pontos_depois INTEGER NOT NULL,
    motivo VARCHAR(100) NOT NULL,
    observacoes TEXT,
    data_operacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    venda_id BIGINT,
    produto_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    FOREIGN KEY (venda_id) REFERENCES vendas(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    CHECK (pontos_antes >= 0),
    CHECK (pontos_depois >= 0)
);

-- ========================================
-- 10. TABELA DE AUDITORIA
-- ========================================
CREATE TABLE auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tabela VARCHAR(50) NOT NULL,
    operacao VARCHAR(10) NOT NULL,
    dados_antigos TEXT,
    dados_novos TEXT,
    usuario_id BIGINT,
    data_operacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CHECK (operacao IN ('INSERT', 'UPDATE', 'DELETE'))
);

-- ========================================
-- 11. TABELA DE CONFIGURAÇÕES DO SISTEMA
-- ========================================
CREATE TABLE configuracoes_sistema (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chave VARCHAR(100) NOT NULL UNIQUE,
    valor TEXT,
    descricao TEXT,
    tipo VARCHAR(50),
    usuario_atualizacao_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (usuario_atualizacao_id) REFERENCES usuarios(id)
);

-- ========================================
-- 12. TABELA DE SESSÕES DE USUÁRIO
-- ========================================
CREATE TABLE sessoes_usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    data_login TIMESTAMP,
    data_expiracao TIMESTAMP NOT NULL,
    data_ultimo_acesso TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    ativo BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- ========================================
-- 13. TABELA DE CÓDIGOS DE REDEFINIÇÃO
-- ========================================
CREATE TABLE codigos_redefinicao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    codigo VARCHAR(10) NOT NULL,
    data_expiracao TIMESTAMP NOT NULL,
    usado BOOLEAN,
    tentativas INTEGER,
    ip_solicitante VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- ÍNDICES PARA PERFORMANCE
-- ========================================
CREATE INDEX idx_clientes_cpf ON clientes(cpf);
CREATE INDEX idx_clientes_email ON clientes(email);
CREATE INDEX idx_clientes_is_fake ON clientes(is_fake);
CREATE INDEX idx_produtos_codigo ON produtos(codigo);
CREATE INDEX idx_produtos_codigo_barras ON produtos(codigo_barras);
CREATE INDEX idx_produtos_departamento ON produtos(departamento);
CREATE INDEX idx_tamanhos_produto_id ON tamanhos_produto(produto_id);
CREATE INDEX idx_vendas_cliente_id ON vendas(cliente_id);
CREATE INDEX idx_vendas_data_venda ON vendas(data_venda);
CREATE INDEX idx_vendas_status ON vendas(status);
CREATE INDEX idx_itens_venda_id ON itens_venda(venda_id);
CREATE INDEX idx_itens_produto_id ON itens_venda(produto_id);
CREATE INDEX idx_movimentacoes_produto_id ON movimentacoes_estoque(produto_id);
CREATE INDEX idx_movimentacoes_data ON movimentacoes_estoque(data_movimentacao);
CREATE INDEX idx_historico_pontos_cliente_id ON historico_pontos(cliente_id);
CREATE INDEX idx_sessoes_token_hash ON sessoes_usuario(token_hash);
CREATE INDEX idx_sessoes_usuario_id ON sessoes_usuario(usuario_id);

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