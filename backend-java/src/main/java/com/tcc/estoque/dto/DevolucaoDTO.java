package com.tcc.estoque.dto;

import com.tcc.estoque.model.enums.TipoDevolucao;
import com.tcc.estoque.model.enums.StatusQualidade;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DevolucaoDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DevolucaoRequest {
        @NotNull(message = "ID da venda é obrigatório")
        private Long vendaId;

        @NotNull(message = "Tipo de devolução é obrigatório")
        private TipoDevolucao tipoDevolucao;

        @NotBlank(message = "Motivo é obrigatório")
        @Size(max = 1000, message = "Motivo deve ter no máximo 1000 caracteres")
        private String motivo;

        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres")
        private String observacoes;

        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @DecimalMax(value = "100.00", message = "Desconto não pode ser maior que 100%")
        private BigDecimal descontoAplicado;

        @NotEmpty(message = "Deve ter pelo menos um item para devolução")
        private List<ItemDevolucaoRequest> itens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDevolucaoRequest {
        @NotNull(message = "ID do item de venda é obrigatório")
        private Long itemVendaId;

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        private Integer quantidade;

        @NotNull(message = "Status de qualidade é obrigatório")
        private StatusQualidade statusQualidadeRetorno;

        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @DecimalMax(value = "100.00", message = "Desconto não pode ser maior que 100%")
        private BigDecimal descontoAplicado;

        @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
        private String observacoes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DevolucaoResponse {
        private Long id;
        private Long vendaId;
        private String vendaNumero;
        private String clienteNome;
        private TipoDevolucao tipoDevolucao;
        private String motivo;
        private String observacoes;
        private BigDecimal valorDevolucao;
        private BigDecimal descontoAplicado;
        private LocalDateTime dataDevolucao;
        private String usuarioResponsavel;
        private List<ItemDevolucaoResponse> itens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDevolucaoResponse {
        private Long id;
        private Long produtoId;
        private String nomeProduto;
        private String tamanho;
        private Integer quantidade;
        private BigDecimal precoUnitarioOriginal;
        private BigDecimal valorItem;
        private StatusQualidade statusQualidadeRetorno;
        private BigDecimal descontoAplicado;
        private BigDecimal precoRevenda;
        private String observacoes;
        private LocalDateTime dataDevolucao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DevolucaoResumo {
        private Long id;
        private Long vendaId;
        private String clienteNome;
        private TipoDevolucao tipoDevolucao;
        private BigDecimal valorDevolucao;
        private LocalDateTime dataDevolucao;
        private Integer totalItens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EstatisticasDevolucao {
        private Long totalDevolucoes;
        private Long devolucoesSimples;
        private Long trocasPorDefeito;
        private Long trocasPorTamanho;
        private BigDecimal valorTotalDevolvido;
        private Integer produtosDefeituosos;
        private BigDecimal valorProdutosDefeituosos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProdutoDefeitosoResumo {
        private Long produtoId;
        private String nomeProduto;
        private String codigoBarras;
        private Integer quantidadeDefeituosa;
        private BigDecimal precoOriginal;
        private BigDecimal precoDefeituoso;
        private BigDecimal percentualDesconto;
        private BigDecimal valorEstimadoDesconto;
        private LocalDateTime dataUltimaAtualizacao;
    }
}