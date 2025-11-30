package com.tcc.estoque.service;

import com.tcc.estoque.model.ConfiguracaoSistema;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.ConfiguracaoSistemaRepository;
import com.tcc.estoque.dto.ConfiguracaoBackupDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de configurações do sistema
 */
@Service
@Transactional
public class ConfiguracaoService {
    
    @Autowired
    private ConfiguracaoSistemaRepository configuracaoRepository;
    
    // ===== CONSTANTES DE CONFIGURAÇÃO =====
    
    public static final String SISTEMA_NOME = "sistema.nome";
    public static final String SISTEMA_VERSAO = "sistema.versao";
    public static final String SISTEMA_AMBIENTE = "sistema.ambiente";
    public static final String SISTEMA_MANUTENCAO = "sistema.manutencao";
    public static final String SISTEMA_TIMEZONE = "sistema.timezone";
    public static final String SISTEMA_LOGS_NIVEL = "sistema.logs.nivel";
    
    public static final String SEGURANCA_MFA_HABILITADO = "seguranca.mfa.habilitado";
    public static final String SEGURANCA_TENTATIVAS_LOGIN = "seguranca.tentativas.login";
    public static final String SEGURANCA_TIMEOUT_SESSAO = "seguranca.timeout.sessao";
    public static final String SEGURANCA_SENHA_MINIMA = "seguranca.senha.minima";
    public static final String SEGURANCA_SENHA_COMPLEXA = "seguranca.senha.complexa";
    public static final String SEGURANCA_BLOQUEIO_TEMPO = "seguranca.bloqueio.tempo";
    
    public static final String ESTOQUE_ALERTA_MINIMO = "estoque.alerta.minimo";
    public static final String ESTOQUE_ALERTA_MAXIMO = "estoque.alerta.maximo";
    public static final String ESTOQUE_AUTO_REPOSICAO = "estoque.auto.reposicao";
    public static final String ESTOQUE_VALIDADE_DIAS = "estoque.validade.dias";
    public static final String ESTOQUE_CODIGO_AUTOMATICO = "estoque.codigo.automatico";
    
    public static final String VENDAS_DESCONTO_MAXIMO = "vendas.desconto.maximo";
    public static final String VENDAS_PRAZO_PAGAMENTO = "vendas.prazo.pagamento";
    public static final String VENDAS_COMISSAO_PADRAO = "vendas.comissao.padrao";
    public static final String VENDAS_NOTA_FISCAL_AUTO = "vendas.nota.fiscal.auto";
    
    public static final String NOTIF_EMAIL_HABILITADO = "notificacoes.email.habilitado";
    public static final String NOTIF_SMS_HABILITADO = "notificacoes.sms.habilitado";
    public static final String NOTIF_PUSH_HABILITADO = "notificacoes.push.habilitado";
    public static final String NOTIF_ESTOQUE_BAIXO = "notificacoes.estoque.baixo";
    public static final String NOTIF_VENDAS_DIARIAS = "notificacoes.vendas.diarias";
    
    public static final String APARENCIA_TEMA = "aparencia.tema";
    public static final String APARENCIA_LOGO = "aparencia.logo";
    public static final String APARENCIA_COR_PRIMARIA = "aparencia.cor.primaria";
    public static final String APARENCIA_COR_SECUNDARIA = "aparencia.cor.secundaria";
    
    // ===== MÉTODOS PRINCIPAIS =====
    
    /**
     * Busca todas as configurações
     */
    public List<ConfiguracaoSistema> buscarTodasConfiguracoes() {
        return configuracaoRepository.findAllOrderByChave();
    }
    
    /**
     * Busca configuração por chave
     */
    public Optional<ConfiguracaoSistema> buscarPorChave(String chave) {
        return configuracaoRepository.findByChave(chave);
    }
    
    /**
     * Busca valor de configuração por chave
     */
    public String buscarValor(String chave) {
        return configuracaoRepository.findByChave(chave)
                .map(ConfiguracaoSistema::getValor)
                .orElse(null);
    }
    
    /**
     * Busca valor com padrão
     */
    public String buscarValor(String chave, String valorPadrao) {
        return configuracaoRepository.findByChave(chave)
                .map(ConfiguracaoSistema::getValor)
                .orElse(valorPadrao);
    }
    
    /**
     * Busca configurações por categoria
     */
    public Map<String, Object> buscarConfiguracoesPorCategoria(String categoria) {
        List<ConfiguracaoSistema> configs = configuracaoRepository.findByCategoria(categoria);
        return configs.stream()
                .collect(Collectors.toMap(
                    ConfiguracaoSistema::getChave,
                    config -> converterValor(config.getValor(), config.getTipo())
                ));
    }
    
    /**
     * Salva ou atualiza configuração
     */
    public ConfiguracaoSistema salvarConfiguracao(String chave, String valor, String tipo, Usuario usuario) {
        ConfiguracaoSistema config = configuracaoRepository.findByChave(chave)
                .orElse(new ConfiguracaoSistema());
        
        config.setChave(chave);
        config.setValor(valor);
        config.setTipo(tipo);
        config.setUpdatedAt(LocalDateTime.now());
        config.setUsuarioAtualizacao(usuario);
        
        return configuracaoRepository.save(config);
    }
    
    /**
     * Salva múltiplas configurações
     */
    public List<ConfiguracaoSistema> salvarConfiguracoes(Map<String, Object> configuracoes, Usuario usuario) {
        List<ConfiguracaoSistema> configsSalvas = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : configuracoes.entrySet()) {
            String chave = entry.getKey();
            Object valor = entry.getValue();
            String tipo = determinarTipo(valor);
            
            ConfiguracaoSistema config = salvarConfiguracao(chave, valor.toString(), tipo, usuario);
            configsSalvas.add(config);
        }
        
        return configsSalvas;
    }
    
    // ===== CONFIGURAÇÕES ESPECÍFICAS =====
    
    /**
     * Busca configurações do sistema
     */
    public Map<String, Object> buscarConfiguracoesSistema() {
        Map<String, Object> configs = new HashMap<>();
        
        configs.put("nome", buscarValor(SISTEMA_NOME, "Sistema de Estoque e Vendas"));
        configs.put("versao", buscarValor(SISTEMA_VERSAO, "1.0.0"));
        configs.put("ambiente", buscarValor(SISTEMA_AMBIENTE, "DESENVOLVIMENTO"));
        configs.put("manutencao", buscarBoolean(SISTEMA_MANUTENCAO, false));
        configs.put("timezone", buscarValor(SISTEMA_TIMEZONE, "America/Sao_Paulo"));
        configs.put("logsNivel", buscarValor(SISTEMA_LOGS_NIVEL, "INFO"));
        
        return configs;
    }
    
    /**
     * Atualiza configurações do sistema
     */
    public void atualizarConfiguracoesSistema(Map<String, Object> configuracoes, Usuario usuario) {
        salvarConfiguracoes(adicionarPrefixo(configuracoes, "sistema."), usuario);
    }
    
    /**
     * Busca configurações de segurança
     */
    public Map<String, Object> buscarConfiguracoesSeguranca() {
        Map<String, Object> configs = new HashMap<>();
        
        configs.put("mfaHabilitado", buscarBoolean(SEGURANCA_MFA_HABILITADO, false));
        configs.put("tentativasLogin", buscarInteger(SEGURANCA_TENTATIVAS_LOGIN, 3));
        configs.put("timeoutSessao", buscarInteger(SEGURANCA_TIMEOUT_SESSAO, 30));
        configs.put("senhaMinima", buscarInteger(SEGURANCA_SENHA_MINIMA, 8));
        configs.put("senhaComplexa", buscarBoolean(SEGURANCA_SENHA_COMPLEXA, true));
        configs.put("bloqueioTempo", buscarInteger(SEGURANCA_BLOQUEIO_TEMPO, 15));
        
        return configs;
    }
    
    /**
     * Atualiza configurações de segurança
     */
    public void atualizarConfiguracoesSeguranca(Map<String, Object> configuracoes, Usuario usuario) {
        salvarConfiguracoes(adicionarPrefixo(configuracoes, "seguranca."), usuario);
    }
    
    /**
     * Busca configurações de estoque
     */
    public Map<String, Object> buscarConfiguracoesEstoque() {
        Map<String, Object> configs = new HashMap<>();
        
        configs.put("alertaMinimo", buscarInteger(ESTOQUE_ALERTA_MINIMO, 10));
        configs.put("alertaMaximo", buscarInteger(ESTOQUE_ALERTA_MAXIMO, 1000));
        configs.put("autoReposicao", buscarBoolean(ESTOQUE_AUTO_REPOSICAO, false));
        configs.put("validadeDias", buscarInteger(ESTOQUE_VALIDADE_DIAS, 30));
        configs.put("codigoAutomatico", buscarBoolean(ESTOQUE_CODIGO_AUTOMATICO, true));
        
        return configs;
    }
    
    /**
     * Atualiza configurações de estoque
     */
    public void atualizarConfiguracoesEstoque(Map<String, Object> configuracoes, Usuario usuario) {
        salvarConfiguracoes(adicionarPrefixo(configuracoes, "estoque."), usuario);
    }
    
    /**
     * Busca configurações de vendas
     */
    public Map<String, Object> buscarConfiguracoesVendas() {
        Map<String, Object> configs = new HashMap<>();
        
        configs.put("descontoMaximo", buscarDouble(VENDAS_DESCONTO_MAXIMO, 20.0));
        configs.put("prazoPagamento", buscarInteger(VENDAS_PRAZO_PAGAMENTO, 30));
        configs.put("comissaoPadrao", buscarDouble(VENDAS_COMISSAO_PADRAO, 5.0));
        configs.put("notaFiscalAuto", buscarBoolean(VENDAS_NOTA_FISCAL_AUTO, true));
        
        return configs;
    }
    
    /**
     * Atualiza configurações de vendas
     */
    public void atualizarConfiguracoesVendas(Map<String, Object> configuracoes, Usuario usuario) {
        salvarConfiguracoes(adicionarPrefixo(configuracoes, "vendas."), usuario);
    }
    
    /**
     * Busca configurações de notificações
     */
    public Map<String, Object> buscarConfiguracoesNotificacoes() {
        Map<String, Object> configs = new HashMap<>();
        
        configs.put("emailHabilitado", buscarBoolean(NOTIF_EMAIL_HABILITADO, true));
        configs.put("smsHabilitado", buscarBoolean(NOTIF_SMS_HABILITADO, false));
        configs.put("pushHabilitado", buscarBoolean(NOTIF_PUSH_HABILITADO, true));
        configs.put("estoqueBaixo", buscarBoolean(NOTIF_ESTOQUE_BAIXO, true));
        configs.put("vendasDiarias", buscarBoolean(NOTIF_VENDAS_DIARIAS, false));
        
        return configs;
    }
    
    /**
     * Atualiza configurações de notificações
     */
    public void atualizarConfiguracoesNotificacoes(Map<String, Object> configuracoes, Usuario usuario) {
        salvarConfiguracoes(adicionarPrefixo(configuracoes, "notificacoes."), usuario);
    }
    
    /**
     * Busca configurações de aparência
     */
    public Map<String, Object> buscarConfiguracoesAparencia() {
        Map<String, Object> configs = new HashMap<>();
        
        configs.put("tema", buscarValor(APARENCIA_TEMA, "light"));
        configs.put("logo", buscarValor(APARENCIA_LOGO, ""));
        configs.put("corPrimaria", buscarValor(APARENCIA_COR_PRIMARIA, "#007bff"));
        configs.put("corSecundaria", buscarValor(APARENCIA_COR_SECUNDARIA, "#6c757d"));
        
        return configs;
    }
    
    /**
     * Atualiza configurações de aparência
     */
    public void atualizarConfiguracoesAparencia(Map<String, Object> configuracoes, Usuario usuario) {
        salvarConfiguracoes(adicionarPrefixo(configuracoes, "aparencia."), usuario);
    }
    
    // ===== BACKUP E RESTORE =====
    
    /**
     * Executa backup das configurações
     */
    public ConfiguracaoBackupDto executarBackup() {
        List<ConfiguracaoSistema> configs = buscarTodasConfiguracoes();
        
        ConfiguracaoBackupDto backup = new ConfiguracaoBackupDto();
        backup.setDataBackup(LocalDateTime.now());
        backup.setTotalConfiguracoes(configs.size());
        backup.setConfiguracoes(configs.stream()
                .collect(Collectors.toMap(
                    ConfiguracaoSistema::getChave,
                    ConfiguracaoSistema::getValor
                )));
        
        return backup;
    }
    
    /**
     * Histórico de backups (simulado)
     */
    public List<ConfiguracaoBackupDto> buscarHistoricoBackups() {
        List<ConfiguracaoBackupDto> historico = new ArrayList<>();
        
        ConfiguracaoBackupDto backup1 = new ConfiguracaoBackupDto();
        backup1.setDataBackup(LocalDateTime.now().minusDays(1));
        backup1.setTotalConfiguracoes(25);
        backup1.setTamanho("2.5 KB");
        backup1.setStatus("SUCESSO");
        
        ConfiguracaoBackupDto backup2 = new ConfiguracaoBackupDto();
        backup2.setDataBackup(LocalDateTime.now().minusDays(7));
        backup2.setTotalConfiguracoes(23);
        backup2.setTamanho("2.3 KB");
        backup2.setStatus("SUCESSO");
        
        historico.add(backup1);
        historico.add(backup2);
        
        return historico;
    }
    
    // ===== MÉTODOS AUXILIARES =====
    
    /**
     * Busca valor como boolean
     */
    private Boolean buscarBoolean(String chave, Boolean padrao) {
        String valor = buscarValor(chave);
        return valor != null ? Boolean.valueOf(valor) : padrao;
    }
    
    /**
     * Busca valor como integer
     */
    private Integer buscarInteger(String chave, Integer padrao) {
        String valor = buscarValor(chave);
        try {
            return valor != null ? Integer.valueOf(valor) : padrao;
        } catch (NumberFormatException e) {
            return padrao;
        }
    }
    
    /**
     * Busca valor como double
     */
    private Double buscarDouble(String chave, Double padrao) {
        String valor = buscarValor(chave);
        try {
            return valor != null ? Double.valueOf(valor) : padrao;
        } catch (NumberFormatException e) {
            return padrao;
        }
    }
    
    /**
     * Adiciona prefixo às chaves
     */
    private Map<String, Object> adicionarPrefixo(Map<String, Object> configs, String prefixo) {
        Map<String, Object> resultado = new HashMap<>();
        for (Map.Entry<String, Object> entry : configs.entrySet()) {
            resultado.put(prefixo + entry.getKey(), entry.getValue());
        }
        return resultado;
    }
    
    /**
     * Determina tipo do valor
     */
    private String determinarTipo(Object valor) {
        if (valor instanceof Boolean) return "Boolean";
        if (valor instanceof Integer) return "Integer";
        if (valor instanceof Double) return "Double";
        if (valor instanceof Long) return "Long";
        return "String";
    }
    
    /**
     * Converte valor baseado no tipo
     */
    private Object converterValor(String valor, String tipo) {
        if (valor == null) return null;
        
        try {
            switch (tipo) {
                case "Boolean": return Boolean.valueOf(valor);
                case "Integer": return Integer.valueOf(valor);
                case "Double": return Double.valueOf(valor);
                case "Long": return Long.valueOf(valor);
                default: return valor;
            }
        } catch (Exception e) {
            return valor;
        }
    }
    
    // ===== VALIDAÇÕES =====
    
    /**
     * Valida se MFA está habilitado
     */
    public boolean isMfaHabilitado() {
        return buscarBoolean(SEGURANCA_MFA_HABILITADO, false);
    }
    
    /**
     * Valida timeout de sessão
     */
    public int getTimeoutSessao() {
        return buscarInteger(SEGURANCA_TIMEOUT_SESSAO, 30);
    }
    
    /**
     * Valida sistema em manutenção
     */
    public boolean isSistemaEmManutencao() {
        return buscarBoolean(SISTEMA_MANUTENCAO, false);
    }
    
    /**
     * Inicializa configurações padrão do sistema
     */
    @Transactional
    public void inicializarConfiguracoesDefault() {
        if (configuracaoRepository.count() == 0) {
            Map<String, Object> configsDefault = new HashMap<>();
            
            configsDefault.put(SISTEMA_NOME, "Sistema de Estoque e Vendas");
            configsDefault.put(SISTEMA_VERSAO, "1.0.0");
            configsDefault.put(SISTEMA_AMBIENTE, "DESENVOLVIMENTO");
            configsDefault.put(SISTEMA_MANUTENCAO, "false");
            
            configsDefault.put(SEGURANCA_MFA_HABILITADO, "false");
            configsDefault.put(SEGURANCA_TENTATIVAS_LOGIN, "3");
            configsDefault.put(SEGURANCA_TIMEOUT_SESSAO, "30");
            
            configsDefault.put(ESTOQUE_ALERTA_MINIMO, "10");
            configsDefault.put(ESTOQUE_AUTO_REPOSICAO, "false");
            
            configsDefault.put(VENDAS_DESCONTO_MAXIMO, "20.0");
            configsDefault.put(VENDAS_COMISSAO_PADRAO, "5.0");
            
            for (Map.Entry<String, Object> entry : configsDefault.entrySet()) {
                ConfiguracaoSistema config = new ConfiguracaoSistema();
                config.setChave(entry.getKey());
                config.setValor(entry.getValue().toString());
                config.setTipo(determinarTipo(entry.getValue()));
                config.setUpdatedAt(LocalDateTime.now());
                configuracaoRepository.save(config);
            }
        }
    }
}
