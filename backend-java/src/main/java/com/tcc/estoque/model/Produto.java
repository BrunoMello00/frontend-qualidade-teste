package com.tcc.estoque.model;

import com.tcc.estoque.enums.TipoCodigoBarras;
import com.tcc.estoque.model.enums.StatusQualidade;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produtos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 200)
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 200, message = "Nome deve ter entre 2 e 200 caracteres")
    private String nome;

    @Column(name = "descricao", columnDefinition = "CLOB")
    private String descricao;

    @Column(name = "preco", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.00", message = "Preço não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal preco;

    @Builder.Default
    @Column(name = "estoque", nullable = false)
    @Min(value = 0, message = "Estoque não pode ser negativo")
    private Integer estoque = 0;

    @Builder.Default
    @Column(name = "estoque_minimo", nullable = false)
    @Min(value = 0, message = "Estoque mínimo não pode ser negativo")
    private Integer estoqueMinimo = 1;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Código é obrigatório")
    @Size(max = 50, message = "Código deve ter no máximo 50 caracteres")
    private String codigo;

    @Column(name = "codigo_resumido", unique = true, length = 20)
    @Size(max = 20, message = "Código resumido deve ter no máximo 20 caracteres")
    private String codigoResumido;

    @Column(name = "codigo_interno_sequencial")
    private Long codigoInternoSequencial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_codigo_barras", length = 20)
    @Builder.Default
    private TipoCodigoBarras tipoCodigoBarras = TipoCodigoBarras.PERSONALIZADO;

    @Column(name = "prefixo_codigo", length = 10)
    @Builder.Default
    private String prefixoCodigo = "PROD";

    @Column(name = "departamento", length = 100)
    @Size(max = 100, message = "Departamento deve ter no máximo 100 caracteres")
    private String departamento;

    @Column(name = "fornecedor", length = 150)
    @Size(max = 150, message = "Fornecedor deve ter no máximo 150 caracteres")
    private String fornecedor;

    @Builder.Default
    @Column(name = "custo_unitario", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Custo unitário não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Custo unitário deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status_qualidade", nullable = false, length = 20)
    private StatusQualidade statusQualidade = StatusQualidade.NORMAL;

    @Column(name = "preco_defeituoso", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Preço defeituoso não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Preço defeituoso deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal precoDefeituoso;

    @Builder.Default
    @Column(name = "pontuacao_produto", nullable = false)
    @Min(value = 0, message = "Pontuação do produto não pode ser negativa")
    @Max(value = 1000, message = "Pontuação do produto não pode ser maior que 1000")
    private Integer pontuacaoProduto = 0;

    @Column(name = "margem", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Margem não pode ser negativa")
    @DecimalMax(value = "999.99", message = "Margem deve ser menor que 1000%")
    private BigDecimal margem;

    @Builder.Default
    @Column(name = "pontos_recompensa", nullable = false)
    @Min(value = 0, message = "Pontos de recompensa não pode ser negativo")
    private Integer pontosRecompensa = 1;

    @Builder.Default
    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro = LocalDateTime.now();

    @Builder.Default
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_cadastro_id")
    private Usuario usuarioCadastro;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<TamanhoProduto> tamanhos = new ArrayList<>();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }

    public boolean isEstoqueBaixo() {
        return estoque <= estoqueMinimo;
    }

    public boolean temEstoqueDisponivel(Integer quantidade) {
        return estoque >= quantidade;
    }

    public void adicionarEstoque(Integer quantidade) {
        if (quantidade > 0) {
            this.estoque += quantidade;
            this.dataAtualizacao = LocalDateTime.now();
        }
    }

    public boolean removerEstoque(Integer quantidade) {
        if (quantidade > 0 && this.estoque >= quantidade) {
            this.estoque -= quantidade;
            this.dataAtualizacao = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public BigDecimal calcularValorEstoque() {
        return custoUnitario.multiply(BigDecimal.valueOf(estoque));
    }

    public BigDecimal calcularLucroUnitario() {
        return preco.subtract(custoUnitario);
    }

    public BigDecimal calcularMargemLucro() {
        if (custoUnitario.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return calcularLucroUnitario().divide(custoUnitario, 4, java.math.RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal calcularLucroTotalEstoque() {
        return calcularLucroUnitario().multiply(BigDecimal.valueOf(estoque));
    }

    /**
     * Atualiza o custo médio ponderado quando há entrada de estoque
     * Fórmula: ((EstoqueAtual × CustoAtual) + (QuantidadeEntrada × CustoEntrada)) / (EstoqueAtual + QuantidadeEntrada)
     */
    public void atualizarCustoMedioPonderado(Integer quantidadeEntrada, BigDecimal custoEntrada) {
        if (quantidadeEntrada > 0 && custoEntrada.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal valorEstoqueAtual = custoUnitario.multiply(BigDecimal.valueOf(estoque));
            BigDecimal valorEntrada = custoEntrada.multiply(BigDecimal.valueOf(quantidadeEntrada));
            BigDecimal valorTotal = valorEstoqueAtual.add(valorEntrada);
            Integer quantidadeTotal = estoque + quantidadeEntrada;
            
            if (quantidadeTotal > 0) {
                this.custoUnitario = valorTotal.divide(BigDecimal.valueOf(quantidadeTotal), 2, java.math.RoundingMode.HALF_UP);
            }
        }
    }

    public boolean isCodigoValido() {
        return tipoCodigoBarras != null && tipoCodigoBarras.validar(codigo);
    }

    public String obterCodigoFormatado() {
        if (codigoResumido != null && !codigoResumido.trim().isEmpty()) {
            return codigoResumido + " (" + codigo + ")";
        }
        return codigo;
    }

    public boolean isCodigoGeradoAutomaticamente() {
        return tipoCodigoBarras != null && tipoCodigoBarras.permiteGeracaoAutomatica();
    }

    public boolean isPadraoBrasileiro() {
        return tipoCodigoBarras != null && tipoCodigoBarras.isPadraoBrasileiro();
    }

    public boolean temTamanhos() {
        return tamanhos != null && !tamanhos.isEmpty();
    }

    public void adicionarTamanho(TamanhoProduto tamanho) {
        if (tamanho != null) {
            if (this.tamanhos == null) {
                this.tamanhos = new ArrayList<>();
            }
            tamanho.setProduto(this);
            this.tamanhos.add(tamanho);
        }
    }

    public void removerTamanho(TamanhoProduto tamanho) {
        if (tamanho != null && this.tamanhos != null) {
            this.tamanhos.remove(tamanho);
            tamanho.setProduto(null);
        }
    }

    public Integer getEstoqueTotal() {
        if (temTamanhos()) {
            return tamanhos.stream()
                    .filter(t -> t.getAtivo())
                    .mapToInt(TamanhoProduto::getEstoque)
                    .sum();
        }
        // Para produtos sem tamanhos, retorna o estoque direto do produto
        return estoque != null ? estoque : 0;
    }

    public Integer getQuantidadeVendidasTotal() {
        if (temTamanhos()) {
            return tamanhos.stream()
                    .filter(t -> t.getAtivo())
                    .mapToInt(t -> t.getVendidas() != null ? t.getVendidas() : 0)
                    .sum();
        }
        return 0;
    }

    // Métodos para gestão de qualidade
    public boolean isDefeituoso() {
        return statusQualidade == StatusQualidade.DEFEITUOSO;
    }

    public boolean isDisponivel() {
        return ativo && statusQualidade != StatusQualidade.INDISPONIVEL;
    }

    public boolean podeSerVendido() {
        return isDisponivel() && statusQualidade.podeSerVendido();
    }

    public BigDecimal getPrecoVenda() {
        if (isDefeituoso() && precoDefeituoso != null) {
            return precoDefeituoso;
        }
        return preco;
    }

    public void marcarComoDefeituoso(BigDecimal precoComDesconto) {
        this.statusQualidade = StatusQualidade.DEFEITUOSO;
        this.precoDefeituoso = precoComDesconto;
    }

    public void marcarComoNormal() {
        this.statusQualidade = StatusQualidade.NORMAL;
        this.precoDefeituoso = null;
    }

    public void marcarComoIndisponivel() {
        this.statusQualidade = StatusQualidade.INDISPONIVEL;
    }
}
