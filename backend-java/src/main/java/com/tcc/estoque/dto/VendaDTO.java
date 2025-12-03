package com.tcc.estoque.dto;

import com.tcc.estoque.model.enums.StatusVenda;
import com.tcc.estoque.model.enums.FormaPagamento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para operações de venda
 */
public class VendaDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaRequest {
        
        @NotBlank(message = "Nome do cliente é obrigatório")
        @Size(max = 100, message = "Nome do cliente deve ter no máximo 100 caracteres")
        private String nomeCliente;
        
        @Email(message = "Email deve ter formato válido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        private String emailCliente;
        
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        private String telefoneCliente;
        
        @NotNull(message = "Forma de pagamento é obrigatória")
        private FormaPagamento formaPagamento;
        
        @DecimalMin(value = "0.0", inclusive = true, message = "Desconto deve ser maior ou igual a zero")
        private BigDecimal desconto;
        
        @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
        private String observacoes;
        
        // Campos para uso de pontos
        private Long clienteId; // Cliente cadastrado para usar pontos
        private Long recompensaId; // Recompensa escolhida
        
        @Builder.Default
        private Boolean usarPontos = false; // Se deve usar pontos
        
        // Campo para associar evento promocional (opcional)
        private Long eventoId;
        
        // Campo para definir data customizada da venda (opcional)
        private LocalDateTime dataVenda;
        
        @NotEmpty(message = "Venda deve ter pelo menos um item")
        @Valid
        private List<ItemVendaRequest> itens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemVendaRequest {
        
        @NotNull(message = "ID do produto é obrigatório")
        private Long produtoId;
        
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        private Integer quantidade;
        
        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço unitário deve ser maior que zero")
        private BigDecimal precoUnitario;
        
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        private BigDecimal descontoItem;
        
        private String tamanho;
        
        private String nomeProduto; // Para facilitar o envio do frontend
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaResponse {
        
        private Long id;
        private String nomeCliente;
        private String emailCliente;
        private String telefoneCliente;
        private LocalDateTime dataVenda;
        private LocalDateTime dataConfirmacao;
        private LocalDateTime dataCancelamento;
        private StatusVenda status;
        private BigDecimal subtotal;
        private BigDecimal desconto;
        private BigDecimal valorTotal;
        private FormaPagamento formaPagamento; // Campo de forma de pagamento
        private String observacoes;
        private String motivoCancelamento;
        private Integer quantidadeItens;
        private List<ItemVendaResponse> itens;
        private String nomeVendedor;
        private String emailVendedor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemVendaResponse {
        
        private Long id;
        private Long produtoId;
        private String nomeProduto;
        private String codigoBarras;
        private Integer quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaListResponse {
        
        private Long id;
        private String nomeCliente;
        private LocalDateTime dataVenda;
        private StatusVenda status;
        private BigDecimal valorTotal;
        private Integer quantidadeItens;
        private String nomeVendedor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstatisticasVendasResponse {
        
        private Long totalVendas;
        private BigDecimal totalFaturamento;
        private BigDecimal ticketMedio;
        private Long vendasPendentes;
        private Long vendasConfirmadas;
        private Long vendasCanceladas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProdutoMaisVendidoResponse {
        
        private Long produtoId;
        private String nomeProduto;
        private String codigoBarras;
        private Integer quantidadeVendida;
        private BigDecimal valorTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelamentoRequest {
        
        @NotBlank(message = "Motivo do cancelamento é obrigatório")
        @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
        private String motivoCancelamento;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FiltroVendasRequest {
        
        private StatusVenda status;
        private LocalDateTime dataInicio;
        private LocalDateTime dataFim;
        private String nomeCliente;
        private Long vendedorId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaResumoResponse {
        
        private Long id;
        private String nomeCliente;
        private LocalDateTime dataVenda;
        private StatusVenda status;
        private BigDecimal valorTotal;
        private String nomeVendedor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProdutoResponse {
        
        private Long produtoId;
        private String nomeProduto;
        private String categoriaProduto;
        private Integer quantidadeVendida;
        private BigDecimal receitaTotal;
        private BigDecimal precoUnitario;
        private Integer ranking;
        private Double percentualVendas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientePontosResponse {
        private Long clienteId;
        private String nomeCliente;
        private String emailCliente;
        private Integer pontosDisponiveis;
        private String categoria;
        private List<RecompensaDisponivelResponse> recompensasDisponiveis;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecompensaDisponivelResponse {
        private Long recompensaId;
        private String nome;
        private String descricao;
        private Integer pontosNecessarios;
        private BigDecimal percentualDesconto;
        private BigDecimal valorDesconto;
        private String categoria;
        private Boolean podeUsar;
    }
}
