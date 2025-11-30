package com.tcc.estoque.model;

import com.tcc.estoque.model.enums.FormaPagamento;
import com.tcc.estoque.model.enums.StatusVenda;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "Usuário é obrigatório")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @Column(name = "cliente_nome", nullable = false, length = 200)
    @NotBlank(message = "Nome do cliente é obrigatório")
    @Size(max = 200, message = "Nome do cliente deve ter no máximo 200 caracteres")
    private String clienteNome;

    @Column(name = "cliente_email", length = 150)
    @Email(message = "Email deve ter um formato válido")
    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    private String clienteEmail;

    @Column(name = "cliente_telefone", length = 20)
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String clienteTelefone;

    @Builder.Default
    @Column(name = "data_venda", nullable = false)
    private LocalDateTime dataVenda = LocalDateTime.now();

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Subtotal é obrigatório")
    @DecimalMin(value = "0.00", message = "Subtotal não pode ser negativo")
    private BigDecimal subtotal;

    @Builder.Default
    @Column(name = "desconto", precision = 12, scale = 2)
    @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor total não pode ser negativo")
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 50)
    @NotNull(message = "Forma de pagamento é obrigatória")
    private FormaPagamento formaPagamento;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private StatusVenda status = StatusVenda.PENDENTE;

    @Column(name = "observacoes", columnDefinition = "CLOB")
    private String observacoes;

    @Column(name = "motivo_cancelamento", columnDefinition = "CLOB")
    private String motivoCancelamento;

    @Column(name = "data_confirmacao")
    private LocalDateTime dataConfirmacao;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    @Column(name = "data_cancelamento")
    private LocalDateTime dataCancelamento;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemVenda> itens = new ArrayList<>();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void adicionarItem(ItemVenda item) {
        itens.add(item);
        item.setVenda(this);
        calcularValores();
    }

    public void removerItem(ItemVenda item) {
        itens.remove(item);
        item.setVenda(null);
        calcularValores();
    }

    public void calcularValores() {
        this.subtotal = itens.stream()
                .map(ItemVenda::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.valorTotal = subtotal.subtract(desconto != null ? desconto : BigDecimal.ZERO);
        this.valorTotal = this.valorTotal.max(BigDecimal.ZERO);
    }

    public void confirmar() {
        this.status = StatusVenda.CONFIRMADA;
        this.dataConfirmacao = LocalDateTime.now();
    }

    public void entregar() {
        this.status = StatusVenda.ENTREGUE;
        this.dataEntrega = LocalDateTime.now();
    }

    public void cancelar(String motivo) {
        this.status = StatusVenda.CANCELADA;
        this.motivoCancelamento = motivo;
        this.dataCancelamento = LocalDateTime.now();
    }

    public boolean podeSerCancelada() {
        return status == StatusVenda.PENDENTE || status == StatusVenda.CONFIRMADA;
    }

    public boolean isConfirmada() {
        return status == StatusVenda.CONFIRMADA || status == StatusVenda.ENTREGUE;
    }
}
