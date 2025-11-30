package com.tcc.estoque.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PontuacaoDTO {

    // ========== CATEGORIA CONFIG ==========
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaConfigRequest {
        @NotBlank(message = "Nome da categoria é obrigatório")
        private String nome;
        
        private String descricao;
        
        @NotNull(message = "Pontos mínimos é obrigatório")
        @Min(value = 0, message = "Pontos mínimos não pode ser negativo")
        private Integer pontosMinimos;
        
        @Min(value = 0, message = "Pontos máximos não pode ser negativo")
        private Integer pontosMaximos;
        
        @NotNull(message = "Pontos iniciais é obrigatório")
        @Min(value = 0, message = "Pontos iniciais não pode ser negativo")
        private Integer pontosIniciais;
        
        @NotBlank(message = "Cor é obrigatória")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato hexadecimal (#RRGGBB)")
        private String cor;
        
        @Min(value = 1, message = "Ordem deve ser maior que zero")
        private Integer ordem;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaConfigResponse {
        private Long id;
        private String nome;
        private String descricao;
        private Integer pontosMinimos;
        private Integer pontosMaximos;
        private Integer pontosIniciais;
        private String cor;
        private Boolean ativo;
        private Integer ordem;
        private LocalDateTime dataCriacao;
        private LocalDateTime dataAtualizacao;
        
        private String rangeFormatado; // "0 - 499 pontos"
        private Boolean isUltima; // Se é a categoria mais alta
    }

    // ========== RECOMPENSAS ==========
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecompensaRequest {
        @NotBlank(message = "Nome da recompensa é obrigatório")
        private String nome;
        
        private String descricao;
        
        @NotNull(message = "Pontos necessários é obrigatório")
        @Min(value = 1, message = "Pontos necessários deve ser maior que zero")
        private Integer pontosNecessarios;
        
        private String categoria;
        
        @DecimalMin(value = "0.00", message = "Valor do desconto não pode ser negativo")
        private BigDecimal valorDesconto;
        
        @DecimalMin(value = "0.00", message = "Percentual do desconto não pode ser negativo")
        @DecimalMax(value = "100.00", message = "Percentual do desconto não pode ser maior que 100%")
        private BigDecimal percentualDesconto;
        
        @Min(value = 1, message = "Quantidade disponível deve ser maior que zero")
        private Integer quantidadeDisponivel;
        
        private LocalDateTime dataValidade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecompensaResponse {
        private Long id;
        private String nome;
        private String descricao;
        private Integer pontosNecessarios;
        private String categoria;
        private BigDecimal valorDesconto;
        private BigDecimal percentualDesconto;
        private Boolean ativo;
        private Integer quantidadeDisponivel;
        private Integer quantidadeResgatada;
        private LocalDateTime dataValidade;
        private LocalDateTime dataCriacao;
        
        private Boolean disponivel;
        private Boolean temEstoque;
        private String tipoDesconto; // "VALOR" ou "PERCENTUAL"
        private String descontoFormatado; // "R$ 10,00" ou "10%"
    }

    // ========== CLIENTE PONTUAÇÃO ==========
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientePontuacaoResponse {
        private Long id;
        private String nome;
        private String email;
        private Integer pontos;
        private String categoriaAtual;
        private String corCategoria;
        private String proximaCategoria;
        private Integer pontosProximaCategoria;
        private BigDecimal totalCompras;
        private Integer quantidadeCompras;
        private LocalDateTime ultimaCompra;
        
        private Integer pontosParaProximaCategoria;
        private Double progressoCategoria; // % para próxima categoria
        private Boolean isClienteVip;
    }

    // ========== HISTÓRICO PONTOS ==========
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricoPontosResponse {
        private Long id;
        private Long clienteId;
        private String clienteNome;
        private Long vendaId;
        private Long produtoId;
        private String produtoNome;
        private Integer pontosAdicionados;
        private Integer pontosAntes;
        private Integer pontosDepois;
        private String motivo;
        private String motivoFormatado;
        private LocalDateTime dataOperacao;
        private String observacoes;
        private String tipoOperacao; // "ADIÇÃO" ou "REMOÇÃO" ou "RESGATE"
    }

    // ========== ESTATÍSTICAS ==========
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstatisticasPontuacaoResponse {
        private Long totalClientes;
        private Integer totalPontosAtivos;
        private Double mediaPontosCliente;
        private Long totalResgates;
        private Long totalRecompensasAtivas;
        
        private java.util.List<DistribuicaoCategoriaResponse> distribuicaoCategoria;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistribuicaoCategoriaResponse {
        private String categoria;
        private String cor;
        private Long quantidade;
        private Double percentual;
    }

    // ========== TRANSAÇÕES ==========
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransacaoPontosRequest {
        @NotNull(message = "Cliente ID é obrigatório")
        private Long clienteId;
        
        @NotNull(message = "Pontos é obrigatório")
        @Min(value = 1, message = "Pontos deve ser maior que zero")
        private Integer pontos;
        
        @NotBlank(message = "Motivo é obrigatório")
        private String motivo;
        
        private String observacoes;
        private Long vendaId;
        private Long produtoId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResgatePontosRequest {
        @NotNull(message = "Cliente ID é obrigatório")
        private Long clienteId;
        
        @NotNull(message = "Recompensa ID é obrigatório")
        private Long recompensaId;
        
        private String observacoes;
    }
}