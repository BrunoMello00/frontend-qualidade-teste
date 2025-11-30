package com.tcc.estoque.exception;

public class NotFoundException extends RuntimeException {
    
    private String codigo;
    private Object[] parametros;

    public NotFoundException(String mensagem) {
        super(mensagem);
    }

    public NotFoundException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public NotFoundException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public NotFoundException(String codigo, String mensagem, Object... parametros) {
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
