package com.tcc.estoque.dto;

import com.tcc.estoque.model.Auditoria;
import java.time.LocalDateTime;

/**
 * DTO para auditoria - usado em relatórios e respostas da API
 */
public class AuditoriaDTO {
    
    private Long id;
    private String tabela;
    private Long registroId;
    private Auditoria.OperacaoAuditoria operacao;
    private String dadosAnteriores;
    private String dadosNovos;
    private String usuario;
    private String ipAddress;
    private LocalDateTime timestampOperacao;
    
    public AuditoriaDTO() {}
    
    public AuditoriaDTO(Long id, String tabela, Long registroId, 
                       Auditoria.OperacaoAuditoria operacao, String dadosAnteriores, 
                       String dadosNovos, String usuario, String ipAddress, 
                       LocalDateTime timestampOperacao) {
        this.id = id;
        this.tabela = tabela;
        this.registroId = registroId;
        this.operacao = operacao;
        this.dadosAnteriores = dadosAnteriores;
        this.dadosNovos = dadosNovos;
        this.usuario = usuario;
        this.ipAddress = ipAddress;
        this.timestampOperacao = timestampOperacao;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTabela() {
        return tabela;
    }
    
    public void setTabela(String tabela) {
        this.tabela = tabela;
    }
    
    public Long getRegistroId() {
        return registroId;
    }
    
    public void setRegistroId(Long registroId) {
        this.registroId = registroId;
    }
    
    public Auditoria.OperacaoAuditoria getOperacao() {
        return operacao;
    }
    
    public void setOperacao(Auditoria.OperacaoAuditoria operacao) {
        this.operacao = operacao;
    }
    
    public String getDadosAnteriores() {
        return dadosAnteriores;
    }
    
    public void setDadosAnteriores(String dadosAnteriores) {
        this.dadosAnteriores = dadosAnteriores;
    }
    
    public String getDadosNovos() {
        return dadosNovos;
    }
    
    public void setDadosNovos(String dadosNovos) {
        this.dadosNovos = dadosNovos;
    }
    
    public String getUsuario() {
        return usuario;
    }
    
    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public LocalDateTime getTimestampOperacao() {
        return timestampOperacao;
    }
    
    public void setTimestampOperacao(LocalDateTime timestampOperacao) {
        this.timestampOperacao = timestampOperacao;
    }
}
