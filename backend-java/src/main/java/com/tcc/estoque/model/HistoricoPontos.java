package com.tcc.estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_pontos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoPontos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, insertable = false, updatable = false)
    private Cliente cliente;

    @Column(name = "cliente_id", nullable = false)
    @NotNull(message = "Cliente ID é obrigatório")
    private Long clienteId;

    @Column(name = "venda_id")
    private Long vendaId;

    @Column(name = "produto_id")
    private Long produtoId;

    @Column(name = "pontos_adicionados", nullable = false)
    @NotNull(message = "Pontos adicionados é obrigatório")
    private Integer pontosAdicionados;

    @Column(name = "pontos_antes", nullable = false)
    @NotNull(message = "Pontos antes é obrigatório")
    @Min(value = 0, message = "Pontos antes não podem ser negativos")
    private Integer pontosAntes;

    @Column(name = "pontos_depois", nullable = false)
    @NotNull(message = "Pontos depois é obrigatório")
    @Min(value = 0, message = "Pontos depois não podem ser negativos")
    private Integer pontosDepois;

    @Column(name = "motivo", nullable = false, length = 100)
    @NotBlank(message = "Motivo é obrigatório")
    @Size(max = 100, message = "Motivo deve ter no máximo 100 caracteres")
    private String motivo; // 'compra', 'ajuste_manual', 'bonus', 'promocao', etc.

    @Builder.Default
    @Column(name = "data_operacao", nullable = false)
    private LocalDateTime dataOperacao = LocalDateTime.now();

    @Column(name = "observacoes", columnDefinition = "CLOB")
    private String observacoes;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAdicao() {
        return pontosAdicionados > 0;
    }

    public boolean isRemocao() {
        return pontosAdicionados < 0;
    }

    public String getTipoOperacao() {
        return isAdicao() ? "Adição" : "Remoção";
    }

    public Integer getPontosAbsolutos() {
        return Math.abs(pontosAdicionados);
    }

    public String getMotivoFormatado() {
        switch (motivo.toLowerCase()) {
            case "compra":
                return "Compra realizada";
            case "ajuste_manual":
                return "Ajuste manual";
            case "bonus":
                return "Bônus";
            case "promocao":
                return "Promoção";
            case "cashback":
                return "Cashback";
            case "indicacao":
                return "Indicação de amigo";
            case "correcao":
                return "Correção";
            default:
                return motivo;
        }
    }
}
