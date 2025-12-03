package com.tcc.estoque.dto;

import lombok.*;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoqueRequest {

    @NotNull(message = "ID do produto é obrigatório")
    private Long produtoId;
    
    private Long tamanhoId;
    
    private String novoTamanho;
    private BigDecimal precoTamanho;
    private String codigoBarras;
    
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;
    
    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    private String observacoes;
    
    @NotNull(message = "Tipo de movimentação é obrigatório")
    private TipoMovimentacao tipoMovimentacao;
    
    public enum TipoMovimentacao {
        ENTRADA_ESTOQUE,
        SAIDA_ESTOQUE,
        AJUSTE_INVENTARIO,
        VENDA,
        DEVOLUCAO
    }
}