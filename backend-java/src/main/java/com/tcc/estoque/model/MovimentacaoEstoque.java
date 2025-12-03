package com.tcc.estoque.model;

import com.tcc.estoque.model.enums.TipoMovimentacao;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_estoque")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    @NotNull(message = "Produto é obrigatório")
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "Usuário é obrigatório")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @NotNull(message = "Tipo de movimentação é obrigatório")
    private TipoMovimentacao tipo;

    @Column(name = "quantidade", nullable = false)
    @NotNull(message = "Quantidade é obrigatória")
    private Integer quantidade;

    @Column(name = "quantidade_anterior", nullable = false)
    @NotNull(message = "Quantidade anterior é obrigatória")
    private Integer quantidadeAnterior;

    @Column(name = "quantidade_atual", nullable = false)
    @NotNull(message = "Quantidade atual é obrigatória")
    private Integer quantidadeAtual;

    @Column(name = "custo_unitario", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Custo unitário não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Custo unitário deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal custoUnitario;

    @Column(name = "custo_medio_anterior", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Custo médio anterior não pode ser negativo")
    private BigDecimal custoMedioAnterior;

    @Column(name = "custo_medio_atual", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Custo médio atual não pode ser negativo")
    private BigDecimal custoMedioAtual;

    @Column(name = "motivo", nullable = false, length = 500)
    @NotBlank(message = "Motivo é obrigatório")
    @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    private String motivo;

    @Column(name = "observacoes", columnDefinition = "CLOB")
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id")
    private Venda venda;

    @Builder.Default
    @Column(name = "data_movimentacao")
    private LocalDateTime dataMovimentacao = LocalDateTime.now();

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isEntrada() {
        return tipo == TipoMovimentacao.ENTRADA;
    }

    public boolean isSaida() {
        return tipo == TipoMovimentacao.SAIDA;
    }

    public boolean isAjuste() {
        return tipo == TipoMovimentacao.AJUSTE;
    }

    public Integer getVariacao() {
        return quantidadeAtual - quantidadeAnterior;
    }
}
