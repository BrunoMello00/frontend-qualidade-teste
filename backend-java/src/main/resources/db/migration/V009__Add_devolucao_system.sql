-- V009: Adicionar campos de status de qualidade aos produtos e criar tabelas de devolução

-- Adicionar campos de status de qualidade aos produtos
ALTER TABLE produtos ADD COLUMN status_qualidade VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE produtos ADD COLUMN preco_defeituoso DECIMAL(10,2);

-- Adicionar constraints para status_qualidade
ALTER TABLE produtos ADD CONSTRAINT chk_status_qualidade 
    CHECK (status_qualidade IN ('NORMAL', 'DEFEITUOSO', 'INDISPONIVEL'));

-- Criar tabela de devoluções
CREATE TABLE devolucoes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    venda_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    tipo_devolucao VARCHAR(30) NOT NULL,
    motivo CLOB NOT NULL,
    observacoes CLOB,
    valor_devolucao DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    desconto_aplicado DECIMAL(5,2),
    data_devolucao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_devolucao_venda FOREIGN KEY (venda_id) REFERENCES vendas(id),
    CONSTRAINT fk_devolucao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_tipo_devolucao CHECK (tipo_devolucao IN ('DEVOLUCAO_SIMPLES', 'TROCA_POR_DEFEITO', 'TROCA_POR_TAMANHO', 'PRODUTO_INCORRETO')),
    CONSTRAINT chk_valor_devolucao CHECK (valor_devolucao >= 0),
    CONSTRAINT chk_desconto_aplicado CHECK (desconto_aplicado IS NULL OR (desconto_aplicado >= 0 AND desconto_aplicado <= 100))
);

-- Criar tabela de itens de devolução
CREATE TABLE itens_devolucao (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    devolucao_id BIGINT NOT NULL,
    item_venda_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    nome_produto VARCHAR(255) NOT NULL,
    tamanho VARCHAR(50),
    quantidade INTEGER NOT NULL,
    preco_unitario_original DECIMAL(10,2) NOT NULL,
    valor_item DECIMAL(12,2) NOT NULL,
    status_qualidade_retorno VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    desconto_aplicado DECIMAL(5,2),
    observacoes CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_item_devolucao_devolucao FOREIGN KEY (devolucao_id) REFERENCES devolucoes(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_devolucao_item_venda FOREIGN KEY (item_venda_id) REFERENCES itens_venda(id),
    CONSTRAINT fk_item_devolucao_produto FOREIGN KEY (produto_id) REFERENCES produtos(id),
    CONSTRAINT chk_quantidade_devolucao CHECK (quantidade > 0),
    CONSTRAINT chk_preco_unitario_original CHECK (preco_unitario_original >= 0),
    CONSTRAINT chk_valor_item CHECK (valor_item >= 0),
    CONSTRAINT chk_status_qualidade_retorno CHECK (status_qualidade_retorno IN ('NORMAL', 'DEFEITUOSO', 'INDISPONIVEL')),
    CONSTRAINT chk_desconto_aplicado_item CHECK (desconto_aplicado IS NULL OR (desconto_aplicado >= 0 AND desconto_aplicado <= 100))
);

-- Criar índices para melhor performance
CREATE INDEX idx_devolucoes_venda_id ON devolucoes(venda_id);
CREATE INDEX idx_devolucoes_usuario_id ON devolucoes(usuario_id);
CREATE INDEX idx_devolucoes_tipo_devolucao ON devolucoes(tipo_devolucao);
CREATE INDEX idx_devolucoes_data_devolucao ON devolucoes(data_devolucao);

CREATE INDEX idx_itens_devolucao_devolucao_id ON itens_devolucao(devolucao_id);
CREATE INDEX idx_itens_devolucao_produto_id ON itens_devolucao(produto_id);
CREATE INDEX idx_itens_devolucao_status_qualidade ON itens_devolucao(status_qualidade_retorno);

CREATE INDEX idx_produtos_status_qualidade ON produtos(status_qualidade);