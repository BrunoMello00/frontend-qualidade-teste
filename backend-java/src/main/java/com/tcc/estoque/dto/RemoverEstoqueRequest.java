package com.tcc.estoque.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoverEstoqueRequest {

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;
    
    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    private String observacoes;
}