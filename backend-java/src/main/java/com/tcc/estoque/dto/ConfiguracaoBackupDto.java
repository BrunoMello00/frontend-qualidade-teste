package com.tcc.estoque.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para backup de configurações
 */
public class ConfiguracaoBackupDto {
    
    private LocalDateTime dataBackup;
    private Integer totalConfiguracoes;
    private String tamanho;
    private String status;
    private Map<String, String> configuracoes;
    
    public ConfiguracaoBackupDto() {}
    
    public ConfiguracaoBackupDto(LocalDateTime dataBackup, Integer totalConfiguracoes, 
                                String tamanho, String status) {
        this.dataBackup = dataBackup;
        this.totalConfiguracoes = totalConfiguracoes;
        this.tamanho = tamanho;
        this.status = status;
    }
    
    public LocalDateTime getDataBackup() {
        return dataBackup;
    }
    
    public void setDataBackup(LocalDateTime dataBackup) {
        this.dataBackup = dataBackup;
    }
    
    public Integer getTotalConfiguracoes() {
        return totalConfiguracoes;
    }
    
    public void setTotalConfiguracoes(Integer totalConfiguracoes) {
        this.totalConfiguracoes = totalConfiguracoes;
    }
    
    public String getTamanho() {
        return tamanho;
    }
    
    public void setTamanho(String tamanho) {
        this.tamanho = tamanho;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Map<String, String> getConfiguracoes() {
        return configuracoes;
    }
    
    public void setConfiguracoes(Map<String, String> configuracoes) {
        this.configuracoes = configuracoes;
    }
}
