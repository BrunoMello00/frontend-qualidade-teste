package com.tcc.estoque.model.enums;

/**
 * Enum que define os diferentes status que um evento pode ter
 */
public enum StatusEvento {
    PLANEJADO("Planejado", "Evento ainda em fase de planejamento", "#FFA726", false),
    ATIVO("Ativo", "Evento em andamento, vendas liberadas", "#4CAF50", true),
    PAUSADO("Pausado", "Evento temporariamente pausado", "#FF9800", false),
    CONCLUIDO("Concluído", "Evento finalizado com sucesso", "#2196F3", false),
    CANCELADO("Cancelado", "Evento cancelado", "#F44336", false);

    private final String descricao;
    private final String detalhe;
    private final String cor;
    private final boolean permiteVendas;

    StatusEvento(String descricao, String detalhe, String cor, boolean permiteVendas) {
        this.descricao = descricao;
        this.detalhe = detalhe;
        this.cor = cor;
        this.permiteVendas = permiteVendas;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public String getCor() {
        return cor;
    }

    public boolean isPermiteVendas() {
        return permiteVendas;
    }

    public boolean isPlanejado() {
        return this == PLANEJADO;
    }

    public boolean isAtivo() {
        return this == ATIVO;
    }

    public boolean isPausado() {
        return this == PAUSADO;
    }

    public boolean isConcluido() {
        return this == CONCLUIDO;
    }

    public boolean isCancelado() {
        return this == CANCELADO;
    }

    public boolean isFinalizando() {
        return this == CONCLUIDO || this == CANCELADO;
    }

    public boolean isEmAndamento() {
        return this == ATIVO || this == PAUSADO;
    }
}
