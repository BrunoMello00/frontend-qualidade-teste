package com.qualidade.teste.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Classe que representa um produto no sistema.
 * Contém informações para vendas, estoque e aplicação de descontos.
 */
public class Produto {
    
    public enum Categoria {
        ROUPAS, CALCADOS, ELETRONICOS, CASA, ACESSORIOS, OUTROS
    }
    
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private Categoria categoria;
    private String codigoBarras;
    private Integer quantidadeEstoque;
    private Integer estoqueMinimo;
    private BigDecimal percentualMaximoDesconto;
    private Boolean promocional;
    private String fornecedor;
    private Boolean ativo;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;
    
    // Construtores
    public Produto() {}
    
    public Produto(Long id, String nome, BigDecimal preco, Categoria categoria, Integer quantidadeEstoque) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.categoria = categoria;
        this.quantidadeEstoque = quantidadeEstoque;
        this.estoqueMinimo = 5;
        this.percentualMaximoDesconto = new BigDecimal("50.0");
        this.promocional = false;
        this.ativo = true;
        this.dataCadastro = LocalDateTime.now();
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNome() {
        return nome;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    
    public BigDecimal getPreco() {
        return preco;
    }
    
    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }
    
    public Categoria getCategoria() {
        return categoria;
    }
    
    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
    
    public String getCodigoBarras() {
        return codigoBarras;
    }
    
    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }
    
    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }
    
    public void setQuantidadeEstoque(Integer quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }
    
    public Integer getEstoqueMinimo() {
        return estoqueMinimo;
    }
    
    public void setEstoqueMinimo(Integer estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }
    
    public BigDecimal getPercentualMaximoDesconto() {
        return percentualMaximoDesconto;
    }
    
    public void setPercentualMaximoDesconto(BigDecimal percentualMaximoDesconto) {
        this.percentualMaximoDesconto = percentualMaximoDesconto;
    }
    
    public Boolean getPromocional() {
        return promocional;
    }
    
    public void setPromocional(Boolean promocional) {
        this.promocional = promocional;
    }
    
    public String getFornecedor() {
        return fornecedor;
    }
    
    public void setFornecedor(String fornecedor) {
        this.fornecedor = fornecedor;
    }
    
    public Boolean getAtivo() {
        return ativo;
    }
    
    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
    
    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }
    
    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
    
    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
    
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
    
    /**
     * Verifica se o produto está com estoque baixo
     */
    public boolean isEstoqueBaixo() {
        return quantidadeEstoque != null && estoqueMinimo != null && 
               quantidadeEstoque <= estoqueMinimo;
    }
    
    /**
     * Verifica se o produto está sem estoque
     */
    public boolean isSemEstoque() {
        return quantidadeEstoque == null || quantidadeEstoque <= 0;
    }
    
    @Override
    public String toString() {
        return "Produto{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", preco=" + preco +
                ", categoria=" + categoria +
                ", quantidadeEstoque=" + quantidadeEstoque +
                '}';
    }
}