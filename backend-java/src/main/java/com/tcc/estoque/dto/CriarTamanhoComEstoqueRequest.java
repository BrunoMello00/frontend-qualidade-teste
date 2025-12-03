package com.tcc.estoque.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriarTamanhoComEstoqueRequest {

    @NotBlank(message = "Tamanho é obrigatório")
    @Size(min = 1, max = 50, message = "Tamanho deve ter entre 1 e 50 caracteres")
    private String tamanho;
    
    @DecimalMin(value = "0.00", message = "Preço não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal preco;
    
    @Size(max = 100, message = "Código de barras não pode exceder 100 caracteres")
    private String codigoBarras;
    
    @NotNull(message = "Quantidade inicial é obrigatória")
    @Min(value = 1, message = "Quantidade inicial deve ser maior que zero")
    private Integer quantidadeInicial;
    
    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    private String observacoes;
}