package com.tcc.estoque.dto;

import com.tcc.estoque.model.enums.TipoMovimentacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * DTOs para operações de movimentação de estoque
 */
public class MovimentacaoEstoqueDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntradaEstoqueRequest {
        
        @NotNull(message = "ID do produto é obrigatório")
        private Long produtoId;
        
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        private Integer quantidade;
        
        @NotBlank(message = "Motivo é obrigatório")
        @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
        private String motivo;
        
        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres")
        private String observacoes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaidaEstoqueRequest {
        
        @NotNull(message = "ID do produto é obrigatório")
        private Long produtoId;
        
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        private Integer quantidade;
        
        @NotBlank(message = "Motivo é obrigatório")
        @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
        private String motivo;
        
        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres")
        private String observacoes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AjusteEstoqueRequest {
        
        @NotNull(message = "ID do produto é obrigatório")
        private Long produtoId;
        
        @NotNull(message = "Nova quantidade é obrigatória")
        @Min(value = 0, message = "Nova quantidade não pode ser negativa")
        private Integer novaQuantidade;
        
        @NotBlank(message = "Motivo é obrigatório")
        @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
        private String motivo;
        
        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres")
        private String observacoes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovimentacaoResponse {
        
        private Long id;
        private Long produtoId;
        private String nomeProduto;
        private String codigoProduto;
        private TipoMovimentacao tipo;
        private Integer quantidade;
        private Integer quantidadeAnterior;
        private Integer quantidadeAtual;
        private String motivo;
        private String observacoes;
        private LocalDateTime dataMovimentacao;
        private String nomeUsuario;
        private String emailUsuario;
        private Long vendaId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovimentacaoResumoResponse {
        
        private Long id;
        private String nomeProduto;
        private TipoMovimentacao tipo;
        private Integer quantidade;
        private LocalDateTime dataMovimentacao;
        private String nomeUsuario;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstatisticasMovimentacaoResponse {
        
        private Long totalMovimentacoes;
        private Long totalEntradas;
        private Long totalSaidas;
        private Long totalAjustes;
        private Integer quantidadeEntradas;
        private Integer quantidadeSaidas;
        private Integer saldoMovimentacao;
        private Long movimentacoesHoje;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FiltroMovimentacaoRequest {
        
        private Long produtoId;
        private TipoMovimentacao tipo;
        private LocalDateTime dataInicio;
        private LocalDateTime dataFim;
        private Long usuarioId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricoEstoqueResponse {
        
        private Long produtoId;
        private String nomeProduto;
        private Integer estoqueAtual;
        private Integer estoqueMinimo;
        private Integer totalEntradas;
        private Integer totalSaidas;
        private Integer ultimaMovimentacaoQuantidade;
        private TipoMovimentacao ultimaMovimentacaoTipo;
        private LocalDateTime ultimaMovimentacaoData;
    }
}
