package com.tcc.estoque.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TamanhoProdutoDTO {

    private Long id;
    private Long produtoId;
    private String tamanho;
    private BigDecimal preco;
    private String codigoBarras;
    private Boolean ativo;
    private Integer estoque; // Campo reabilitado para exibir estoque atual
    private Integer vendidas; // Campo reabilitado para exibir vendas
    private Integer quantidade; // Campo para receber quantidade inicial na criação

}
