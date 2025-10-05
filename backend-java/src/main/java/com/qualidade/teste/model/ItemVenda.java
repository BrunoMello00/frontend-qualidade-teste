package com.qualidade.teste.model;

import java.math.BigDecimal;

/**
 * Classe que representa um item dentro de uma venda.
 */
public class ItemVenda {
    private Produto produto;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
    private BigDecimal valorDesconto;
    
    // Construtores
    public ItemVenda() {}
    
    public ItemVenda(Produto produto, Integer quantidade, BigDecimal precoUnitario) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = precoUnitario.multiply(new BigDecimal(quantidade));
        this.valorDesconto = BigDecimal.ZERO;
    }
    
    // Getters e Setters
    public Produto getProduto() {
        return produto;
    }
    
    public void setProduto(Produto produto) {
        this.produto = produto;
    }
    
    public Integer getQuantidade() {
        return quantidade;
    }
    
    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }
    
    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
    
    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }
    
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    
    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }
    
    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }
    
    /**
     * Calcula o valor total do item com desconto aplicado
     */
    public BigDecimal getValorTotalComDesconto() {
        return subtotal.subtract(valorDesconto != null ? valorDesconto : BigDecimal.ZERO);
    }
    
    @Override
    public String toString() {
        return "ItemVenda{" +
                "produto=" + (produto != null ? produto.getNome() : "null") +
                ", quantidade=" + quantidade +
                ", precoUnitario=" + precoUnitario +
                ", subtotal=" + subtotal +
                '}';
    }
}