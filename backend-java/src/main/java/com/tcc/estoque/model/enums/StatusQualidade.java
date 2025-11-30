package com.tcc.estoque.model.enums;

/**
 * Enum para representar o status de qualidade de um produto no estoque
 */
public enum StatusQualidade {
    NORMAL("Normal", "Produto em perfeitas condições para venda"),
    DEFEITUOSO("Defeituoso", "Produto com defeito, pode ser vendido com desconto"),
    INDISPONIVEL("Indisponível", "Produto não deve ser vendido (muito danificado, vencido, etc.)");

    private final String descricao;
    private final String detalhes;

    StatusQualidade(String descricao, String detalhes) {
        this.descricao = descricao;
        this.detalhes = detalhes;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public boolean podeSerVendido() {
        return this == NORMAL || this == DEFEITUOSO;
    }

    public boolean requerDesconto() {
        return this == DEFEITUOSO;
    }
}