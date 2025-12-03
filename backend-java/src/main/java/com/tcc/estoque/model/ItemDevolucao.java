package com.tcc.estoque.model;

import com.tcc.estoque.model.enums.StatusQualidade;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "itens_devolucao")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDevolucao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolucao_id", nullable = false)
    @NotNull(message = "Devolução é obrigatória")
    private Devolucao devolucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_venda_id", nullable = false)
    @NotNull(message = "Item de venda é obrigatório")
    private ItemVenda itemVenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    @NotNull(message = "Produto é obrigatório")
    private Produto produto;

    @Column(name = "nome_produto", nullable = false, length = 255)
    @NotBlank(message = "Nome do produto é obrigatório")
    private String nomeProduto;

    @Column(name = "tamanho", length = 50)
    private String tamanho;

    @Column(name = "quantidade", nullable = false)
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    @Column(name = "preco_unitario_original", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Preço unitário original é obrigatório")
    @DecimalMin(value = "0.00", message = "Preço unitário não pode ser negativo")
    private BigDecimal precoUnitarioOriginal;

    @Column(name = "valor_item", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Valor do item é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor do item não pode ser negativo")
    private BigDecimal valorItem;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status_qualidade_retorno", nullable = false, length = 20)
    private StatusQualidade statusQualidadeRetorno = StatusQualidade.NORMAL;

    @Column(name = "desconto_aplicado", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
    @DecimalMax(value = "100.00", message = "Desconto não pode ser maior que 100%")
    private BigDecimal descontoAplicado; // Desconto para revenda se defeituoso

    @Column(name = "observacoes", columnDefinition = "CLOB")
    private String observacoes;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    private void calcularValorItem() {
        if (precoUnitarioOriginal != null && quantidade != null) {
            this.valorItem = precoUnitarioOriginal.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    public boolean isComDefeito() {
        return statusQualidadeRetorno == StatusQualidade.DEFEITUOSO;
    }

    public boolean podeSerRevendido() {
        return statusQualidadeRetorno.podeSerVendido();
    }

    public BigDecimal getPrecoRevenda() {
        if (!podeSerRevendido()) {
            return BigDecimal.ZERO;
        }
        
        if (isComDefeito() && descontoAplicado != null) {
            BigDecimal desconto = precoUnitarioOriginal.multiply(descontoAplicado.divide(BigDecimal.valueOf(100)));
            return precoUnitarioOriginal.subtract(desconto);
        }
        
        return precoUnitarioOriginal;
    }
}