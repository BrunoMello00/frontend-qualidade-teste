package com.tcc.estoque.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TamanhoProdutoEstoqueDTO {

    private Long id;
    private Long produtoId;
    private String tamanho;
    private BigDecimal preco;
    private String codigoBarras;
    private Boolean ativo;
    
    private Integer estoque;
    private Integer vendidas;
    
    private String nomeProduto;
    private String categoriaProduto;
}