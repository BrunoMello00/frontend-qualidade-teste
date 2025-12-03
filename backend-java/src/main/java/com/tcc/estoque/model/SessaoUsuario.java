package com.tcc.estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessoes_usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "Usuário é obrigatório")
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true)
    @NotBlank(message = "Hash do token é obrigatório")
    private String tokenHash;

    @Column(name = "ip_address", length = 45) // Para suportar IPv4 e IPv6
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "CLOB")
    private String userAgent;

    @Builder.Default
    @Column(name = "data_login")
    private LocalDateTime dataLogin = LocalDateTime.now();

    @Builder.Default
    @Column(name = "data_ultimo_acesso")
    private LocalDateTime dataUltimoAcesso = LocalDateTime.now();

    @Column(name = "data_expiracao", nullable = false)
    @NotNull(message = "Data de expiração é obrigatória")
    private LocalDateTime dataExpiracao;

    @Builder.Default
    @Column(name = "ativo")
    private Boolean ativo = true;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpirada() {
        return LocalDateTime.now().isAfter(dataExpiracao);
    }

    public boolean isValida() {
        return ativo && !isExpirada();
    }

    public void invalidar() {
        this.ativo = false;
    }

    public void atualizarUltimoAcesso() {
        this.dataUltimoAcesso = LocalDateTime.now();
    }

    public void estenderExpiracao(long minutosAdicionar) {
        this.dataExpiracao = LocalDateTime.now().plusMinutes(minutosAdicionar);
    }
}
