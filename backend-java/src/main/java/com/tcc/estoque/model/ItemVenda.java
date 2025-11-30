package com.tcc.estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "itens_venda")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    @NotNull(message = "Venda é obrigatória")
    private Venda venda;

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

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Preço unitário é obrigatório")
    @DecimalMin(value = "0.00", message = "Preço unitário não pode ser negativo")
    private BigDecimal precoUnitario;

    @Builder.Default
    @Column(name = "desconto_item", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
    private BigDecimal descontoItem = BigDecimal.ZERO;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Subtotal é obrigatório")
    @DecimalMin(value = "0.00", message = "Subtotal não pode ser negativo")
    private BigDecimal subtotal;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    private void calcularSubtotal() {
        if (precoUnitario != null && quantidade != null) {
            this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
            if (descontoItem != null) {
                this.subtotal = this.subtotal.subtract(descontoItem);
            }
            this.subtotal = this.subtotal.max(BigDecimal.ZERO);
        }
    }

    public boolean temEstoqueSuficiente() {
        return produto != null && produto.getEstoque() >= quantidade;
    }

    public BigDecimal getPrecoUnitarioComDesconto() {
        if (quantidade == null || quantidade == 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.divide(BigDecimal.valueOf(quantidade), 2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getPercentualDesconto() {
        if (precoUnitario == null || precoUnitario.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        BigDecimal valorTotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        if (descontoItem != null && descontoItem.compareTo(BigDecimal.ZERO) > 0) {
            return descontoItem.divide(valorTotal, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
}
