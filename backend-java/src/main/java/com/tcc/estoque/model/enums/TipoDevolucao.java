package com.tcc.estoque.model.enums;

/**
 * Enum para representar os tipos de devolução/troca
 */
public enum TipoDevolucao {
    DEVOLUCAO_SIMPLES("Devolução Simples", "Cliente desistiu da compra", true),
    TROCA_POR_DEFEITO("Troca por Defeito", "Produto apresentou defeito", false),
    TROCA_POR_TAMANHO("Troca por Tamanho", "Cliente precisa de tamanho diferente", true),
    PRODUTO_INCORRETO("Produto Incorreto", "Produto diferente do pedido", true);

    private final String descricao;
    private final String detalhes;
    private final boolean produtoVoltaNormal; // Se o produto volta ao estoque normal

    TipoDevolucao(String descricao, String detalhes, boolean produtoVoltaNormal) {
        this.descricao = descricao;
        this.detalhes = detalhes;
        this.produtoVoltaNormal = produtoVoltaNormal;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public boolean isProdutoVoltaNormal() {
        return produtoVoltaNormal;
    }

    public StatusQualidade getStatusQualidadeRetorno() {
        return produtoVoltaNormal ? StatusQualidade.NORMAL : StatusQualidade.DEFEITUOSO;
    }
}