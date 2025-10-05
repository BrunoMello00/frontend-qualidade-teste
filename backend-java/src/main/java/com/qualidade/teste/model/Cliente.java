package com.qualidade.teste.model;

import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Classe que representa um cliente no sistema.
 * Contém informações básicas e dados para cálculo de descontos e fidelidade.
 */
public class Cliente {
    
    public enum TipoCliente {
        BRONZE, PRATA, OURO, PREMIUM
    }
    
    private Long id;
    private String nome;
    private String cpf;
    private String email;
    private Integer pontosFidelidade;
    private TipoCliente tipoCliente;
    private LocalDate dataUltimaCompra;
    private BigDecimal totalComprasAno;
    private LocalDate dataAniversario;
    private Boolean ativo;
    
    // Construtores
    public Cliente() {}
    
    public Cliente(Long id, String nome, String cpf, String email, TipoCliente tipoCliente) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.tipoCliente = tipoCliente;
        this.pontosFidelidade = 0;
        this.totalComprasAno = BigDecimal.ZERO;
        this.ativo = true;
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
    
    public String getCpf() {
        return cpf;
    }
    
    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Integer getPontosFidelidade() {
        return pontosFidelidade;
    }
    
    public void setPontosFidelidade(Integer pontosFidelidade) {
        this.pontosFidelidade = pontosFidelidade;
    }
    
    public TipoCliente getTipoCliente() {
        return tipoCliente;
    }
    
    public void setTipoCliente(TipoCliente tipoCliente) {
        this.tipoCliente = tipoCliente;
    }
    
    public LocalDate getDataUltimaCompra() {
        return dataUltimaCompra;
    }
    
    public void setDataUltimaCompra(LocalDate dataUltimaCompra) {
        this.dataUltimaCompra = dataUltimaCompra;
    }
    
    public BigDecimal getTotalComprasAno() {
        return totalComprasAno;
    }
    
    public void setTotalComprasAno(BigDecimal totalComprasAno) {
        this.totalComprasAno = totalComprasAno;
    }
    
    public LocalDate getDataAniversario() {
        return dataAniversario;
    }
    
    public void setDataAniversario(LocalDate dataAniversario) {
        this.dataAniversario = dataAniversario;
    }
    
    public Boolean getAtivo() {
        return ativo;
    }
    
    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
    
    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cpf='" + cpf + '\'' +
                ", tipoCliente=" + tipoCliente +
                ", pontosFidelidade=" + pontosFidelidade +
                '}';
    }
}