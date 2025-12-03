package com.tcc.estoque.model.enums;

public enum CategoriaCliente {
    BRONZE("Bronze", 0, "#CD7F32"),
    PRATA("Prata", 200, "#C0C0C0"),
    OURO("Ouro", 500, "#FFD700"),
    DIAMANTE("Diamante", 1000, "#B9F2FF");

    private final String descricao;
    private final int pontosMinimos;
    private final String cor;

    CategoriaCliente(String descricao, int pontosMinimos, String cor) {
        this.descricao = descricao;
        this.pontosMinimos = pontosMinimos;
        this.cor = cor;
    }

    public static CategoriaCliente determinarCategoria(int pontos) {
        if (pontos >= DIAMANTE.pontosMinimos) {
            return DIAMANTE;
        } else if (pontos >= OURO.pontosMinimos) {
            return OURO;
        } else if (pontos >= PRATA.pontosMinimos) {
            return PRATA;
        } else {
            return BRONZE;
        }
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPontosMinimos() {
        return pontosMinimos;
    }

    public String getCor() {
        return cor;
    }
}
