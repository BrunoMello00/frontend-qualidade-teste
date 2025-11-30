package com.tcc.estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade para recompensas do sistema de pontuação
 */
@Entity
@Table(name = "recompensas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recompensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome da recompensa é obrigatório")
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "pontos_necessarios", nullable = false)
    @Min(value = 1, message = "Pontos necessários deve ser maior que zero")
    private Integer pontosNecessarios;

    @Column(name = "categoria", length = 50)
    private String categoria;

    @Column(name = "valor_desconto", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Valor do desconto não pode ser negativo")
    private java.math.BigDecimal valorDesconto;

    @Column(name = "percentual_desconto", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Percentual do desconto não pode ser negativo")
    @DecimalMax(value = "100.00", message = "Percentual do desconto não pode ser maior que 100%")
    private java.math.BigDecimal percentualDesconto;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Builder.Default
    @Column(name = "quantidade_disponivel")
    private Integer quantidadeDisponivel = null; // null = ilimitado

    @Builder.Default
    @Column(name = "quantidade_resgatada", nullable = false)
    private Integer quantidadeResgatada = 0;

    @Column(name = "data_validade")
    private LocalDateTime dataValidade;

    @Builder.Default
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Builder.Default
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao = LocalDateTime.now();

    @PreUpdate
    private void preUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Verifica se a recompensa está disponível
     */
    public boolean isDisponivel() {
        if (!ativo) return false;
        if (dataValidade != null && LocalDateTime.now().isAfter(dataValidade)) return false;
        if (quantidadeDisponivel != null) {
            return quantidadeResgatada < quantidadeDisponivel;
        }
        return true;
    }

    /**
     * Verifica se ainda há estoque da recompensa
     */
    public boolean temEstoque() {
        return quantidadeDisponivel == null || quantidadeResgatada < quantidadeDisponivel;
    }

    /**
     * Incrementa a quantidade resgatada
     */
    public void incrementarResgate() {
        this.quantidadeResgatada++;
    }
}