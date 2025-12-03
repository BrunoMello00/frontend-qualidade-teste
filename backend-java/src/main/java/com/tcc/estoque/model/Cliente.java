package com.tcc.estoque.model;

import com.tcc.estoque.model.enums.CategoriaCliente;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 255)
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    private String nome;

    @Column(name = "cpf", nullable = false, unique = true, length = 14)
    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11}|000\\.000\\.000-00", 
             message = "CPF deve ter formato válido")
    private String cpf;

    @Column(name = "email", length = 255)
    @Email(message = "Email deve ter um formato válido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    private String email;

    @Column(name = "telefone", length = 20)
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String telefone;

    @Embedded
    private Endereco endereco;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Builder.Default
    @Column(name = "data_cadastro", nullable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Builder.Default
    @Column(name = "total_compras", nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Total de compras não pode ser negativo")
    private BigDecimal totalCompras = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "quantidade_compras", nullable = false)
    @Min(value = 0, message = "Quantidade de compras não pode ser negativa")
    private Integer quantidadeCompras = 0;

    @Column(name = "ultima_compra")
    private LocalDateTime ultimaCompra;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    private CategoriaCliente categoria = CategoriaCliente.BRONZE;

    @Builder.Default
    @Column(name = "pontos", nullable = false)
    @Min(value = 0, message = "Pontos não podem ser negativos")
    private Integer pontos = 0;

    @Column(name = "observacoes", columnDefinition = "CLOB")
    private String observacoes;

    @Builder.Default
    @Column(name = "is_fake", nullable = false)
    private Boolean isFake = false; // Para CPF 000.000.000-00

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HistoricoPontos> historicoPontos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Venda> vendas = new ArrayList<>();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void adicionarCompra(BigDecimal valor, Integer pontos) {
        this.totalCompras = this.totalCompras.add(valor);
        this.quantidadeCompras++;
        this.ultimaCompra = LocalDateTime.now();
        this.pontos += pontos;
        atualizarCategoria();
    }

    public void adicionarPontos(Integer pontosAdicionados, String motivo, Venda venda) {
        this.pontos += pontosAdicionados;
        
        HistoricoPontos historico = HistoricoPontos.builder()
                .cliente(this)
                .vendaId(venda != null ? venda.getId() : null)
                .pontosAdicionados(pontosAdicionados)
                .pontosAntes(this.pontos - pontosAdicionados)
                .pontosDepois(this.pontos)
                .motivo(motivo)
                .dataOperacao(LocalDateTime.now())
                .build();
        
        this.historicoPontos.add(historico);
        atualizarCategoria();
    }

    public void removerPontos(Integer pontosRemovidos, String motivo) {
        int pontosAnteriores = this.pontos;
        this.pontos = Math.max(0, this.pontos - pontosRemovidos);
        
        HistoricoPontos historico = HistoricoPontos.builder()
                .cliente(this)
                .pontosAdicionados(-pontosRemovidos)
                .pontosAntes(pontosAnteriores)
                .pontosDepois(this.pontos)
                .motivo(motivo)
                .dataOperacao(LocalDateTime.now())
                .build();
        
        this.historicoPontos.add(historico);
        atualizarCategoria();
    }

    public boolean temPontosSuficientes(Integer pontosNecessarios) {
        return this.pontos >= pontosNecessarios;
    }

    public void usarPontos(Integer pontosUsados, String motivo, Venda venda) {
        if (!temPontosSuficientes(pontosUsados)) {
            throw new IllegalArgumentException("Cliente não possui pontos suficientes. Pontos disponíveis: " + this.pontos + ", pontos necessários: " + pontosUsados);
        }
        
        int pontosAnteriores = this.pontos;
        this.pontos -= pontosUsados;
        
        HistoricoPontos historico = HistoricoPontos.builder()
                .cliente(this)
                .clienteId(this.id)
                .vendaId(venda != null ? venda.getId() : null)
                .pontosAdicionados(-pontosUsados)
                .pontosAntes(pontosAnteriores)
                .pontosDepois(this.pontos)
                .motivo(motivo)
                .dataOperacao(LocalDateTime.now())
                .build();
        
        this.historicoPontos.add(historico);
        atualizarCategoria();
    }

    private void atualizarCategoria() {
        if (this.pontos >= 1000) {
            this.categoria = CategoriaCliente.DIAMANTE;
        } else if (this.pontos >= 500) {
            this.categoria = CategoriaCliente.OURO;
        } else if (this.pontos >= 200) {
            this.categoria = CategoriaCliente.PRATA;
        } else {
            this.categoria = CategoriaCliente.BRONZE;
        }
    }

    public BigDecimal calcularTicketMedio() {
        if (quantidadeCompras == 0) {
            return BigDecimal.ZERO;
        }
        return totalCompras.divide(BigDecimal.valueOf(quantidadeCompras), 2, RoundingMode.HALF_UP);
    }

    public boolean isCpfFake() {
        return "000.000.000-00".equals(cpf) || "00000000000".equals(cpf);
    }

    public boolean isClienteAtivo() {
        return ativo != null && ativo;
    }

    public boolean isClienteFrequente() {
        return quantidadeCompras >= 5;
    }

    public String getCategoriaFormatada() {
        return categoria.getDescricao();
    }

    public String getCpfFormatado() {
        if (cpf != null && cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + 
                   cpf.substring(3, 6) + "." + 
                   cpf.substring(6, 9) + "-" + 
                   cpf.substring(9);
        }
        return cpf;
    }
}
