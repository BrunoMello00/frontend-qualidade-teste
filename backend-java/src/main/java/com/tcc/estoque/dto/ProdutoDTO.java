package com.tcc.estoque.dto;

import com.tcc.estoque.enums.TipoCodigoBarras;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProdutoDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProdutoRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        private String nome;

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        private String descricao;

        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
        private BigDecimal preco;


        @Size(max = 100, message = "Departamento deve ter no máximo 100 caracteres")
        private String departamento;

        @Size(max = 150, message = "Fornecedor deve ter no máximo 150 caracteres")
        private String fornecedor;

        @Min(value = 0, message = "Estoque mínimo não pode ser negativo")
        @Builder.Default
        private Integer estoqueMinimo = 5;
        
        @Min(value = 0, message = "Estoque inicial não pode ser negativo")
        private Integer estoque;

        @DecimalMin(value = "0.00", message = "Custo unitário não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Custo unitário deve ter no máximo 8 dígitos inteiros e 2 decimais")
        private BigDecimal custoUnitario;
        
        @Min(value = 0, message = "Pontuação do produto não pode ser negativa")
        @Max(value = 1000, message = "Pontuação do produto não pode ser maior que 1000")
        @Builder.Default
        private Integer pontuacaoProduto = 0;
        
        @DecimalMin(value = "0.00", message = "Margem não pode ser negativa")
        @DecimalMax(value = "999.99", message = "Margem deve ser menor que 1000%")
        @Digits(integer = 3, fraction = 2, message = "Margem deve ter no máximo 3 dígitos inteiros e 2 decimais")
        private BigDecimal margem;

        @Min(value = 0, message = "Pontos de recompensa não pode ser negativo")
        @Builder.Default
        private Integer pontosRecompensa = 1;

        @Size(max = 50, message = "Código de barras deve ter no máximo 50 caracteres")
        private String codigoBarras;

        private TipoCodigoBarras tipoCodigoBarras;

        @Size(max = 10, message = "Prefixo deve ter no máximo 10 caracteres")
        private String prefixoCodigo;

        private Boolean gerarCodigoAutomatico;
        
        @Size(max = 20, message = "Unidade de medida deve ter no máximo 20 caracteres")
        private String unidadeMedida;
        
        private List<TamanhoProdutoDTO> tamanhos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProdutoResponse {
        private Long id;
        private String nome;
        private String descricao;
        private BigDecimal preco;
        private Integer quantidadeEstoque;
        private String departamento;
        private String fornecedor;
        private Integer estoqueMinimo;
        
        private BigDecimal custoUnitario;
        
        private BigDecimal margem;
        
        private Integer pontosRecompensa;
        
        private Integer pontuacaoProduto;
        
        private String codigoBarras;
        private String codigoResumido;
        private Long codigoInternoSequencial;
        private TipoCodigoBarras tipoCodigoBarras;
        private String prefixoCodigo;
        private String unidadeMedida;
        private Boolean ativo;
        private LocalDateTime dataCadastro;
        private LocalDateTime dataAtualizacao;
        private Boolean estoqueBaixo;
        
        private Boolean bloqueado;
        private String bloqueadoPorUsuario;
        private LocalDateTime bloqueioExpiraEm;
        
        private Boolean codigoValido;
        private Boolean padraoBrasileiro;
        private String codigoFormatado;
        
        private List<TamanhoProdutoDTO> tamanhos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EstoqueRequest {
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 0, message = "Quantidade não pode ser negativa")
        private Integer quantidade;

        @Size(max = 255, message = "Observação deve ter no máximo 255 caracteres")
        private String observacao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProdutoResumo {
        private Long id;
        private String nome;
        private Integer quantidadeEstoque;
        private Integer estoqueMinimo;
        private BigDecimal preco;
        private Boolean estoqueBaixo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EstatisticasResponse {
        private Long totalProdutos;
        private Long produtosAtivos;
        private Long produtosEstoqueBaixo;
        private Long valorTotalEstoque;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CodigoBarrasRequest {
        @NotNull(message = "Tipo de código é obrigatório")
        private TipoCodigoBarras tipoCodigoBarras;

        @Size(max = 10, message = "Prefixo deve ter no máximo 10 caracteres")
        private String prefixo;

        private Boolean gerarAutomaticamente;
        
        @Size(max = 50, message = "Código manual deve ter no máximo 50 caracteres")
        private String codigoManual;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LockRequest {
        @NotNull(message = "ID do produto é obrigatório")
        private Long produtoId;

        @Size(max = 20, message = "Tipo de lock inválido")
        private String tipoLock;

        @Min(value = 1, message = "Tempo deve ser pelo menos 1 minuto")
        @Max(value = 120, message = "Tempo máximo é 120 minutos")
        private Integer tempoExpiracaoMinutos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LockResponse {
        private Boolean sucesso;
        private String mensagem;
        private String bloqueadoPorUsuario;
        private LocalDateTime bloqueioExpiraEm;
        private Boolean podeEditar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QualidadeRequest {
        @NotNull(message = "Status de qualidade é obrigatório")
        private com.tcc.estoque.model.enums.StatusQualidade statusQualidade;
        
        @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
        private String observacoes;
    }
}
