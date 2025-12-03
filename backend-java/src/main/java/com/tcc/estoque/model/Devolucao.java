package com.tcc.estoque.model;

import com.tcc.estoque.model.enums.TipoDevolucao;
import com.tcc.estoque.model.enums.StatusQualidade;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devolucoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Devolucao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    @NotNull(message = "Venda é obrigatória")
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "Usuário é obrigatório")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_devolucao", nullable = false, length = 30)
    @NotNull(message = "Tipo de devolução é obrigatório")
    private TipoDevolucao tipoDevolucao;

    @Column(name = "motivo", nullable = false, columnDefinition = "CLOB")
    @NotBlank(message = "Motivo é obrigatório")
    private String motivo;

    @Column(name = "observacoes", columnDefinition = "CLOB")
    private String observacoes;

    @Column(name = "valor_devolucao", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Valor de devolução é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor de devolução não pode ser negativo")
    private BigDecimal valorDevolucao;

    @Column(name = "desconto_aplicado", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
    @DecimalMax(value = "100.00", message = "Desconto não pode ser maior que 100%")
    private BigDecimal descontoAplicado; // Para produtos defeituosos

    @Builder.Default
    @Column(name = "data_devolucao")
    private LocalDateTime dataDevolucao = LocalDateTime.now();

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "devolucao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemDevolucao> itens = new ArrayList<>();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void adicionarItem(ItemDevolucao item) {
        itens.add(item);
        item.setDevolucao(this);
        calcularValorDevolucao();
    }

    public void removerItem(ItemDevolucao item) {
        itens.remove(item);
        item.setDevolucao(null);
        calcularValorDevolucao();
    }

    public void calcularValorDevolucao() {
        this.valorDevolucao = itens.stream()
                .map(ItemDevolucao::getValorItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isComDefeito() {
        return tipoDevolucao == TipoDevolucao.TROCA_POR_DEFEITO;
    }

    public StatusQualidade getStatusQualidadeRetorno() {
        return tipoDevolucao.getStatusQualidadeRetorno();
    }
}