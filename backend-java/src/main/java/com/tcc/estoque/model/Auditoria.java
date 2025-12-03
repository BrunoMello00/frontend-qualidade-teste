package com.tcc.estoque.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tabela", nullable = false, length = 100)
    private String tabela;

    @Column(name = "registro_id", nullable = false)
    private Long registroId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacao", nullable = false, length = 20)
    private OperacaoAuditoria operacao;

    @Column(name = "dados_anteriores", columnDefinition = "CLOB")
    private String dadosAnteriores;

    @Column(name = "dados_novos", columnDefinition = "CLOB")
    private String dadosNovos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Builder.Default
    @Column(name = "timestamp_operacao")
    private LocalDateTime timestampOperacao = LocalDateTime.now();

    public enum OperacaoAuditoria {
        INSERT("Inserção"),
        UPDATE("Atualização"),
        DELETE("Exclusão");

        private final String descricao;

        OperacaoAuditoria(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}
