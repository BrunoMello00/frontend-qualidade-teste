package com.qualidade.teste.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Classe que representa uma venda no sistema.
 * Contém informações sobre itens vendidos, valores e descontos aplicados.
 */
public class Venda {
    
    public enum StatusVenda {
        PENDENTE, CONFIRMADA, CANCELADA, FINALIZADA
    }
    
    public enum FormaPagamento {
        DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX, BOLETO
    }
    
    private Long id;
    private Cliente cliente;
    private List<ItemVenda> itens;
    private BigDecimal subtotal;
    private BigDecimal valorDesconto;
    private BigDecimal valorTotal;
    private StatusVenda status;
    private FormaPagamento formaPagamento;
    private LocalDateTime dataVenda;
    private String observacoes;
    private String cupomDesconto;
    private BigDecimal percentualDescontoTotal;
    
    // Construtores
    public Venda() {}
    
    public Venda(Cliente cliente, List<ItemVenda> itens) {
        this.cliente = cliente;
        this.itens = itens;
        this.status = StatusVenda.PENDENTE;
        this.dataVenda = LocalDateTime.now();
        this.valorDesconto = BigDecimal.ZERO;
        this.percentualDescontoTotal = BigDecimal.ZERO;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Cliente getCliente() {
        return cliente;
    }
    
    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
    
    public List<ItemVenda> getItens() {
        return itens;
    }
    
    public void setItens(List<ItemVenda> itens) {
        this.itens = itens;
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
    
    public BigDecimal getValorTotal() {
        return valorTotal;
    }
    
    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }
    
    public StatusVenda getStatus() {
        return status;
    }
    
    public void setStatus(StatusVenda status) {
        this.status = status;
    }
    
    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }
    
    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }
    
    public LocalDateTime getDataVenda() {
        return dataVenda;
    }
    
    public void setDataVenda(LocalDateTime dataVenda) {
        this.dataVenda = dataVenda;
    }
    
    public String getObservacoes() {
        return observacoes;
    }
    
    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
    
    public String getCupomDesconto() {
        return cupomDesconto;
    }
    
    public void setCupomDesconto(String cupomDesconto) {
        this.cupomDesconto = cupomDesconto;
    }
    
    public BigDecimal getPercentualDescontoTotal() {
        return percentualDescontoTotal;
    }
    
    public void setPercentualDescontoTotal(BigDecimal percentualDescontoTotal) {
        this.percentualDescontoTotal = percentualDescontoTotal;
    }
    
    /**
     * Calcula o total de itens na venda
     */
    public int getTotalItens() {
        return itens != null ? itens.stream().mapToInt(ItemVenda::getQuantidade).sum() : 0;
    }
    
    /**
     * Verifica se a venda possui desconto aplicado
     */
    public boolean possuiDesconto() {
        return valorDesconto != null && valorDesconto.compareTo(BigDecimal.ZERO) > 0;
    }
    
    @Override
    public String toString() {
        return "Venda{" +
                "id=" + id +
                ", cliente=" + (cliente != null ? cliente.getNome() : "null") +
                ", valorTotal=" + valorTotal +
                ", status=" + status +
                ", dataVenda=" + dataVenda +
                '}';
    }
}