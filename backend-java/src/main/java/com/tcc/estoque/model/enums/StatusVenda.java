package com.tcc.estoque.model.enums;

public enum StatusVenda {
    PENDENTE("Pendente"),
    CONFIRMADA("Confirmada"),
    ENTREGUE("Entregue"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusVenda(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
