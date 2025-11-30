package com.tcc.estoque.exception;

public class BusinessException extends RuntimeException {
    
    private String codigo;
    private Object[] parametros;

    public BusinessException(String mensagem) {
        super(mensagem);
    }

    public BusinessException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public BusinessException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public BusinessException(String codigo, String mensagem, Object... parametros) {
        super(mensagem);
        this.codigo = codigo;
        this.parametros = parametros;
    }

    public String getCodigo() {
        return codigo;
    }

    public Object[] getParametros() {
        return parametros;
    }
}
