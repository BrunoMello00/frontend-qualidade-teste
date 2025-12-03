package com.tcc.estoque.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum para tipos de códigos de barras mais utilizados no Brasil
 */
@Getter
@AllArgsConstructor
public enum TipoCodigoBarras {
    
    EAN13("EAN-13", "European Article Number 13 dígitos", 13, "^[0-9]{13}$", 
          "Padrão internacional mais usado no Brasil. Exemplo: 7891234567890"),
    
    EAN8("EAN-8", "European Article Number 8 dígitos", 8, "^[0-9]{8}$",
         "Versão menor do EAN-13 para produtos pequenos. Exemplo: 12345678"),
    
    UPC_A("UPC-A", "Universal Product Code A", 12, "^[0-9]{12}$",
          "Padrão americano muito usado no Brasil. Exemplo: 123456789012"),
    
    CODE128("CODE-128", "Code 128", 128, "^[!-~]+$",
            "Alfanumérico, flexível para uso interno. Exemplo: ABC123"),
    
    CODE39("CODE-39", "Code 39", 43, "^[A-Z0-9\\-\\. \\$\\/\\+\\%]+$",
           "Alfanumérico básico para uso interno. Exemplo: PROD-001"),
    
    DATAMATRIX("DataMatrix", "Data Matrix 2D", 2335, "^[\\x00-\\xFF]+$",
              "Código 2D para alta densidade de dados. Exemplo: Vários formatos"),
    
    QR_CODE("QR Code", "Quick Response Code", 4296, "^[\\x00-\\xFF]+$",
            "Código 2D popular para links e dados. Exemplo: https://..."),
    
    ITF14("ITF-14", "Interleaved 2 of 5 (14 dígitos)", 14, "^[0-9]{14}$",
          "Para embalagens e distribuição. Exemplo: 01234567890123"),
    
    GS1_128("GS1-128", "GS1-128 (Code 128 Application Identifier)", 128, "^\\([0-9]+\\)[!-~]+",
            "GS1 com identificadores de aplicação. Exemplo: (01)12345678901234"),
    
    PERSONALIZADO("PERSONALIZADO", "Código personalizado da empresa", 50, "^[A-Za-z0-9\\-\\_]+$",
                  "Código interno da empresa. Exemplo: LOJA-PROD-001");

    private final String codigo;
    private final String descricao;
    private final int tamanhoMaximo;
    private final String regexValidacao;
    private final String exemplo;

    /**
     * Valida se um código está no formato correto para este tipo
     */
    public boolean validar(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return false;
        }
        
        if (codigo.length() > tamanhoMaximo) {
            return false;
        }
        
        return codigo.matches(regexValidacao);
    }

    /**
     * Gerar próximo código sequencial para tipos personalizados
     */
    public String gerarProximoCodigo(String prefixo, int proximoNumero) {
        switch (this) {
            case PERSONALIZADO:
                return String.format("%s-%06d", prefixo, proximoNumero);
            case CODE39:
                return String.format("PROD-%06d", proximoNumero);
            case CODE128:
                return String.format("P%06d", proximoNumero);
            default:
                throw new IllegalStateException("Geração automática não suportada para " + this.codigo);
        }
    }

    /**
     * Verificar se é um código de barras brasileiro padrão
     */
    public boolean isPadraoBrasileiro() {
        return this == EAN13 || this == EAN8 || this == UPC_A || this == ITF14;
    }

    /**
     * Verificar se permite geração automática
     */
    public boolean permiteGeracaoAutomatica() {
        return this == PERSONALIZADO || this == CODE39 || this == CODE128 || 
               this == EAN13 || this == EAN8 || this == UPC_A;
    }

    /**
     * Obter tipo por código
     */
    public static TipoCodigoBarras porCodigo(String codigo) {
        for (TipoCodigoBarras tipo : values()) {
            if (tipo.getCodigo().equals(codigo)) {
                return tipo;
            }
        }
        return PERSONALIZADO;
    }

    /**
     * Detectar tipo automaticamente baseado no formato do código
     */
    public static TipoCodigoBarras detectarTipo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return PERSONALIZADO;
        }

        for (TipoCodigoBarras tipo : values()) {
            if (tipo.validar(codigo)) {
                return tipo;
            }
        }
        
        return PERSONALIZADO;
    }
}
