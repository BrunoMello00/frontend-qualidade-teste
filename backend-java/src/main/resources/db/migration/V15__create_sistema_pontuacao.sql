-- V15: Cria sistema de pontuação dinâmico
-- Adiciona tabelas para categorias configuráveis e recompensas

-- ========================================
-- 1. TABELA DE CONFIGURAÇÃO DE CATEGORIAS
-- ========================================
CREATE TABLE categoria_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(200),
    pontos_minimos INT NOT NULL CHECK (pontos_minimos >= 0),
    pontos_maximos INT CHECK (pontos_maximos >= 0 AND (pontos_maximos IS NULL OR pontos_maximos >= pontos_minimos)),
    pontos_iniciais INT NOT NULL DEFAULT 0 CHECK (pontos_iniciais >= 0),
    cor VARCHAR(7) NOT NULL CHECK (cor ~ '^#[0-9A-Fa-f]{6}$'),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ordem INT NOT NULL DEFAULT 0,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para categoria_config
CREATE INDEX idx_categoria_config_ativo_ordem ON categoria_config (ativo, ordem);
CREATE INDEX idx_categoria_config_pontos ON categoria_config (pontos_minimos, pontos_maximos);

-- ========================================
-- 2. TABELA DE RECOMPENSAS
-- ========================================
CREATE TABLE recompensas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    pontos_necessarios INT NOT NULL CHECK (pontos_necessarios > 0),
    categoria VARCHAR(50) DEFAULT 'Geral',
    valor_desconto DECIMAL(10,2) CHECK (valor_desconto >= 0),
    percentual_desconto DECIMAL(5,2) CHECK (percentual_desconto >= 0 AND percentual_desconto <= 100),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    quantidade_disponivel INT CHECK (quantidade_disponivel >= 0),
    quantidade_resgatada INT NOT NULL DEFAULT 0 CHECK (quantidade_resgatada >= 0),
    data_validade DATE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para recompensas
CREATE INDEX idx_recompensas_ativo_pontos ON recompensas (ativo, pontos_necessarios);
CREATE INDEX idx_recompensas_categoria ON recompensas (categoria);
CREATE INDEX idx_recompensas_validade ON recompensas (data_validade);

-- ========================================
-- 3. ADICIONAR CAMPO PONTOS_RECOMPENSA EM PRODUTOS
-- ========================================
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS pontos_recompensa INT NOT NULL DEFAULT 1 CHECK (pontos_recompensa >= 0);

-- ========================================
-- 4. INSERIR CATEGORIAS PADRÃO
-- ========================================
INSERT INTO categoria_config (nome, descricao, pontos_minimos, pontos_maximos, pontos_iniciais, cor, ordem) VALUES
('BRONZE', 'Categoria inicial para novos clientes', 0, 199, 50, '#CD7F32', 1),
('PRATA', 'Categoria intermediária para clientes regulares', 200, 499, 200, '#C0C0C0', 2),
('OURO', 'Categoria premium para clientes frequentes', 500, 999, 500, '#FFD700', 3),
('DIAMANTE', 'Categoria VIP para clientes especiais', 1000, NULL, 1000, '#B9F2FF', 4);

-- ========================================
-- 5. INSERIR RECOMPENSAS PADRÃO
-- ========================================
INSERT INTO recompensas (nome, descricao, pontos_necessarios, categoria, percentual_desconto) VALUES
('Desconto 5%', 'Desconto de 5% na próxima compra', 100, 'Desconto', 5.00),
('Desconto 10%', 'Desconto de 10% na próxima compra', 200, 'Desconto', 10.00),
('Frete Grátis', 'Frete grátis para qualquer pedido', 150, 'Frete', NULL),
('Desconto 15%', 'Desconto de 15% na próxima compra', 350, 'Desconto', 15.00),
('Produto Grátis', 'Produto grátis até R$ 50', 500, 'Produto Grátis', NULL);

-- ========================================
-- 6. ATUALIZAR PONTOS_RECOMPENSA DOS PRODUTOS EXISTENTES
-- ========================================
-- Produtos de menor valor (até R$ 30) = 1 ponto
UPDATE produtos SET pontos_recompensa = 1 WHERE preco <= 30.00;

-- Produtos de valor médio (R$ 30-80) = 2 pontos  
UPDATE produtos SET pontos_recompensa = 2 WHERE preco > 30.00 AND preco <= 80.00;

-- Produtos de valor alto (R$ 80-200) = 3 pontos
UPDATE produtos SET pontos_recompensa = 3 WHERE preco > 80.00 AND preco <= 200.00;

-- Produtos premium (acima de R$ 200) = 5 pontos
UPDATE produtos SET pontos_recompensa = 5 WHERE preco > 200.00;

-- ========================================
-- 7. COMENTÁRIOS E DOCUMENTAÇÃO
-- ========================================
-- Esta migração implementa um sistema completo de pontuação dinâmico:
--
-- CATEGORIA_CONFIG:
-- - Permite criar/editar/remover categorias de clientes
-- - Define faixas de pontos (min/max) e pontos iniciais
-- - Suporta cores personalizadas e ordenação
--
-- RECOMPENSAS:
-- - Permite criar recompensas com pontos necessários
-- - Suporte a desconto por valor ou percentual
-- - Controle de estoque e validade
-- - Categorização das recompensas
--
-- PONTOS_RECOMPENSA em PRODUTOS:
-- - Define quantos pontos cada produto concede
-- - Permite configuração individual por produto
-- - Valor padrão baseado no preço do produto