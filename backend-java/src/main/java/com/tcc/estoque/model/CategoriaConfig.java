package com.tcc.estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade para configuração dinâmica de categorias de clientes
 */
@Entity
@Table(name = "categoria_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Nome da categoria é obrigatório")
    private String nome;

    @Column(name = "descricao", length = 200)
    private String descricao;

    @Column(name = "pontos_minimos", nullable = false)
    @Min(value = 0, message = "Pontos mínimos não pode ser negativo")
    private Integer pontosMinimos;

    @Column(name = "pontos_maximos")
    @Min(value = 0, message = "Pontos máximos não pode ser negativo")
    private Integer pontosMaximos;

    @Column(name = "pontos_iniciais", nullable = false)
    @Min(value = 0, message = "Pontos iniciais não pode ser negativo")
    @Builder.Default
    private Integer pontosIniciais = 0;

    @Column(name = "cor", nullable = false, length = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato hexadecimal (#RRGGBB)")
    private String cor;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Builder.Default
    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;

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
     * Verifica se um determinado número de pontos está dentro do range desta categoria
     */
    public boolean contemPontos(int pontos) {
        if (pontosMaximos == null) {
            return pontos >= pontosMinimos;
        }
        return pontos >= pontosMinimos && pontos <= pontosMaximos;
    }

    /**
     * Retorna o nome formatado da categoria
     */
    public String getNomeFormatado() {
        return nome.substring(0, 1).toUpperCase() + nome.substring(1).toLowerCase();
    }
}