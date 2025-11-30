package com.tcc.estoque.model;

import com.tcc.estoque.model.enums.StatusEvento;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "eventos")
@Data
@EqualsAndHashCode(callSuper = false)
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", columnDefinition = "CLOB")
    private String descricao;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "local", length = 200)
    private String local;

    @Column(name = "endereco_completo", length = 500)
    private String enderecoCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusEvento status;

    @Column(name = "desconto_percentual", precision = 5, scale = 2)
    private BigDecimal descontoPercentual;

    @Column(name = "desconto_valor", precision = 10, scale = 2)
    private BigDecimal descontoValor;

    @Column(name = "meta_vendas", precision = 15, scale = 2)
    private BigDecimal metaVendas;

    @Column(name = "meta_quantidade_vendas")
    private Integer metaQuantidadeVendas;

    @Column(name = "observacoes", columnDefinition = "CLOB")
    private String observacoes;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Column(name = "publico", nullable = false)
    private Boolean publico; // Se o evento aparece na listagem pública

    @Column(name = "data_cadastro", nullable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por", nullable = false)
    private Usuario criadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por")
    private Usuario atualizadoPor;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Venda> vendas;

    @Transient
    private BigDecimal totalVendas;

    @Transient
    private Integer quantidadeVendas;

    @Transient
    private BigDecimal percentualMeta;

    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
        if (ativo == null) {
            ativo = true;
        }
        if (publico == null) {
            publico = true;
        }
        if (status == null) {
            status = StatusEvento.PLANEJADO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    public boolean isEventoAtivo() {
        return ativo && status.isPermiteVendas();
    }

    public boolean isPeriodoValido() {
        LocalDate hoje = LocalDate.now();
        return !hoje.isBefore(dataInicio) && !hoje.isAfter(dataFim);
    }

    public boolean isEventoVigente() {
        return isEventoAtivo() && isPeriodoValido();
    }

    public long getDuracaoEmDias() {
        return java.time.temporal.ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
    }

    public boolean temDesconto() {
        return (descontoPercentual != null && descontoPercentual.compareTo(BigDecimal.ZERO) > 0) ||
               (descontoValor != null && descontoValor.compareTo(BigDecimal.ZERO) > 0);
    }

    public BigDecimal calcularDesconto(BigDecimal valorOriginal) {
        BigDecimal desconto = BigDecimal.ZERO;
        
        if (descontoPercentual != null && descontoPercentual.compareTo(BigDecimal.ZERO) > 0) {
            desconto = valorOriginal.multiply(descontoPercentual).divide(new BigDecimal("100"));
        }
        
        if (descontoValor != null && descontoValor.compareTo(BigDecimal.ZERO) > 0) {
            desconto = desconto.add(descontoValor);
        }
        
        return desconto;
    }

    public boolean isMetaVendasAlcancada() {
        if (metaVendas == null || totalVendas == null) {
            return false;
        }
        return totalVendas.compareTo(metaVendas) >= 0;
    }

    public boolean isMetaQuantidadeAlcancada() {
        if (metaQuantidadeVendas == null || quantidadeVendas == null) {
            return false;
        }
        return quantidadeVendas >= metaQuantidadeVendas;
    }

    public String getStatusDescricao() {
        return status.getDescricao();
    }

    public String getStatusCor() {
        return status.getCor();
    }
}
