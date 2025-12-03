package com.tcc.estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tamanhos_produto")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TamanhoProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "tamanho", nullable = false, length = 50)
    @NotBlank(message = "Tamanho é obrigatório")
    @Size(min = 1, max = 50, message = "Tamanho deve ter entre 1 e 50 caracteres")
    private String tamanho;

    @Builder.Default
    @Column(name = "estoque", nullable = false)
    @Min(value = 0, message = "Estoque não pode ser negativo")
    private Integer estoque = 0;

    @Column(name = "preco", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Preço não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal preco;

    @Column(name = "codigo_barras", length = 100)
    private String codigoBarras;

    @Builder.Default
    @Column(name = "vendidas")
    @Min(value = 0, message = "Quantidade vendidas não pode ser negativa")
    private Integer vendidas = 0;

    @Builder.Default
    @Column(name = "ativo")
    private Boolean ativo = true;

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

    public boolean temEstoqueDisponivel(Integer quantidade) {
        return estoque >= quantidade;
    }

    public boolean removerEstoque(Integer quantidade) {
        if (quantidade > 0 && this.estoque >= quantidade) {
            this.estoque -= quantidade;
            this.updatedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public void adicionarEstoque(Integer quantidade) {
        if (quantidade > 0) {
            this.estoque += quantidade;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void adicionarVenda(Integer quantidade) {
        if (quantidade > 0) {
            this.vendidas += quantidade;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public BigDecimal getPrecoFinal() {
        return preco != null ? preco : (produto != null ? produto.getPreco() : BigDecimal.ZERO);
    }
}
