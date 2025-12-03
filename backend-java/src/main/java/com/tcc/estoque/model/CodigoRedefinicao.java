package com.tcc.estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_redefinicao")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoRedefinicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 150)
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter um formato válido")
    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    private String email;

    @Column(name = "codigo", nullable = false, length = 10)
    @NotBlank(message = "Código é obrigatório")
    @Size(min = 6, max = 10, message = "Código deve ter entre 6 e 10 caracteres")
    private String codigo;

    @Column(name = "data_expiracao", nullable = false)
    @NotNull(message = "Data de expiração é obrigatória")
    private LocalDateTime dataExpiracao;

    @Builder.Default
    @Column(name = "usado")
    private Boolean usado = false;

    @Builder.Default
    @Column(name = "tentativas")
    private Integer tentativas = 0;

    @Column(name = "ip_solicitante", columnDefinition = "varchar(45)")
    private String ipSolicitante;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(dataExpiracao);
    }

    public boolean isValido() {
        return !usado && !isExpirado();
    }

    public void marcarComoUsado() {
        this.usado = true;
    }

    public void incrementarTentativa() {
        this.tentativas++;
    }

    public boolean excedeuTentativas() {
        return tentativas >= 3; // Máximo 3 tentativas
    }
}
