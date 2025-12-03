package com.tcc.estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracoes_sistema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Chave é obrigatória")
    @Size(max = 100, message = "Chave deve ter no máximo 100 caracteres")
    private String chave;

    @Column(name = "valor", columnDefinition = "CLOB")
    private String valor;

    @Column(name = "descricao", columnDefinition = "CLOB")
    private String descricao;

    @Builder.Default
    @Column(name = "tipo", length = 50)
    private String tipo = "STRING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_atualizacao_id")
    private Usuario usuarioAtualizacao;

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

    public String getValorString() {
        return valor;
    }

    public Integer getValorInteger() {
        try {
            return valor != null ? Integer.parseInt(valor) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean getValorBoolean() {
        return valor != null ? Boolean.parseBoolean(valor) : null;
    }

    public Long getValorLong() {
        try {
            return valor != null ? Long.parseLong(valor) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getValorDouble() {
        try {
            return valor != null ? Double.parseDouble(valor) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
