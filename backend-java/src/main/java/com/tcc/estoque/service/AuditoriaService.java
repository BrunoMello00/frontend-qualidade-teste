package com.tcc.estoque.service;

import com.tcc.estoque.model.Auditoria;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.AuditoriaRepository;
import com.tcc.estoque.security.SecurityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service para gerenciamento de auditoria do sistema
 */
@Service
@Transactional
public class AuditoriaService {
    
    @Autowired
    private AuditoriaRepository auditoriaRepository;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // ===== MÉTODOS DE REGISTRO =====
    
    /**
     * Registra uma operação de inserção
     */
    public void registrarInsercao(String tabela, Long registroId, Object dadosNovos) {
        registrarAuditoria(tabela, registroId, Auditoria.OperacaoAuditoria.INSERT, null, dadosNovos);
    }
    
    /**
     * Registra uma operação de atualização
     */
    public void registrarAtualizacao(String tabela, Long registroId, Object dadosAnteriores, Object dadosNovos) {
        registrarAuditoria(tabela, registroId, Auditoria.OperacaoAuditoria.UPDATE, dadosAnteriores, dadosNovos);
    }
    
    /**
     * Registra uma operação de exclusão
     */
    public void registrarExclusao(String tabela, Long registroId, Object dadosAnteriores) {
        registrarAuditoria(tabela, registroId, Auditoria.OperacaoAuditoria.DELETE, dadosAnteriores, null);
    }
    
    /**
     * Método principal para registrar auditoria
     */
    private void registrarAuditoria(String tabela, Long registroId, Auditoria.OperacaoAuditoria operacao, 
                                   Object dadosAnteriores, Object dadosNovos) {
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            String ipAddress = getClientIp();
            
            Auditoria auditoria = Auditoria.builder()
                    .tabela(tabela)
                    .registroId(registroId)
                    .operacao(operacao)
                    .dadosAnteriores(converterParaJson(dadosAnteriores))
                    .dadosNovos(converterParaJson(dadosNovos))
                    .usuario(usuario)
                    .ipAddress(ipAddress)
                    .timestampOperacao(LocalDateTime.now())
                    .build();
                    
            auditoriaRepository.save(auditoria);
            
        } catch (Exception e) {
            System.err.println("Erro ao registrar auditoria: " + e.getMessage());
        }
    }
    
    // ===== MÉTODOS DE CONSULTA =====
    
    /**
     * Busca auditoria com filtros
     */
    public Page<Auditoria> buscarComFiltros(String tabela, Long usuarioId, String operacao, 
                                           LocalDateTime inicio, LocalDateTime fim, 
                                           int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        Usuario usuario = null;
        if (usuarioId != null) {
            // usuario = usuarioService.buscarPorId(usuarioId);
        }
        
        Auditoria.OperacaoAuditoria op = null;
        if (operacao != null) {
            try {
                op = Auditoria.OperacaoAuditoria.valueOf(operacao.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        
        return auditoriaRepository.findComFiltros(tabela, usuario, op, inicio, fim, pageable);
    }
    
    /**
     * Busca auditoria por tabela
     */
    public Page<Auditoria> buscarPorTabela(String tabela, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findByTabelaOrderByTimestampOperacaoDesc(tabela, pageable);
    }
    
    /**
     * Busca auditoria por usuário
     */
    public Page<Auditoria> buscarPorUsuario(Long usuarioId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Usuario usuario = securityUtil.getUsuarioLogado(); // Simplificado por enquanto
            return auditoriaRepository.findByUsuarioOrderByTimestampOperacaoDesc(usuario, pageable);
        } catch (Exception e) {
            return Page.empty();
        }
    }
    
    /**
     * Busca histórico de um registro específico
     */
    public List<Auditoria> buscarHistoricoRegistro(String tabela, Long registroId) {
        return auditoriaRepository.findByTabelaAndRegistroIdOrderByTimestampOperacaoDesc(tabela, registroId);
    }
    
    /**
     * Busca auditoria por período
     */
    public Page<Auditoria> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findByPeriodo(inicio, fim, pageable);
    }
    
    /**
     * Busca auditoria por IP
     */
    public Page<Auditoria> buscarPorIp(String ipAddress, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findByIpAddressOrderByTimestampOperacaoDesc(ipAddress, pageable);
    }
    
    // ===== ESTATÍSTICAS E RELATÓRIOS =====
    
    /**
     * Gera estatísticas de auditoria
     */
    public Map<String, Object> gerarEstatisticas(LocalDateTime inicio, LocalDateTime fim) {
        Map<String, Object> estatisticas = new HashMap<>();
        
        Long totalOperacoes = auditoriaRepository.contarPorPeriodo(inicio, fim);
        estatisticas.put("totalOperacoes", totalOperacoes);
        
        List<Object[]> porTabela = auditoriaRepository.contarPorTabela();
        Map<String, Long> operacoesPorTabela = new HashMap<>();
        for (Object[] item : porTabela) {
            operacoesPorTabela.put((String) item[0], (Long) item[1]);
        }
        estatisticas.put("operacoesPorTabela", operacoesPorTabela);
        
        List<Object[]> porOperacao = auditoriaRepository.contarPorOperacao();
        Map<String, Long> operacoesPorTipo = new HashMap<>();
        for (Object[] item : porOperacao) {
            operacoesPorTipo.put(item[0].toString(), (Long) item[1]);
        }
        estatisticas.put("operacoesPorTipo", operacoesPorTipo);
        
        List<Object[]> porUsuario = auditoriaRepository.contarPorUsuario();
        Map<String, Long> operacoesPorUsuario = new HashMap<>();
        for (Object[] item : porUsuario) {
            operacoesPorUsuario.put((String) item[0], (Long) item[1]);
        }
        estatisticas.put("operacoesPorUsuario", operacoesPorUsuario);
        
        Pageable topIps = PageRequest.of(0, 10);
        List<Object[]> ipsAtivos = auditoriaRepository.ipsAtivos(topIps);
        Map<String, Long> topIpsMap = new HashMap<>();
        for (Object[] item : ipsAtivos) {
            if (item[0] != null) {
                topIpsMap.put((String) item[0], (Long) item[1]);
            }
        }
        estatisticas.put("ipsAtivos", topIpsMap);
        
        return estatisticas;
    }
    
    /**
     * Busca últimas atividades do usuário
     */
    public List<Auditoria> buscarUltimasAtividades(Usuario usuario, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return auditoriaRepository.ultimasAtividades(usuario, pageable);
    }
    
    /**
     * Busca operações de exclusão
     */
    public Page<Auditoria> buscarExclusoes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findExclusoes(pageable);
    }
    
    // ===== UTILITÁRIOS DE AUDITORIA =====
    
    /**
     * Registra acesso a recurso sensível
     */
    public void registrarAcessoRecurso(String recurso, String acao) {
        try {
            Map<String, Object> dadosAcesso = Map.of(
                "recurso", recurso,
                "acao", acao,
                "timestamp", LocalDateTime.now()
            );
            
            registrarAuditoria("ACESSO_RECURSO", 0L, Auditoria.OperacaoAuditoria.INSERT, null, dadosAcesso);
        } catch (Exception e) {
            System.err.println("Erro ao registrar acesso a recurso: " + e.getMessage());
        }
    }
    
    /**
     * Registra tentativa de acesso não autorizado
     */
    public void registrarTentativaAcessoNegado(String endpoint, String motivo) {
        try {
            Map<String, Object> dadosNegacao = Map.of(
                "endpoint", endpoint,
                "motivo", motivo,
                "timestamp", LocalDateTime.now(),
                "ip", getClientIp()
            );
            
            registrarAuditoria("ACESSO_NEGADO", 0L, Auditoria.OperacaoAuditoria.INSERT, null, dadosNegacao);
        } catch (Exception e) {
            System.err.println("Erro ao registrar acesso negado: " + e.getMessage());
        }
    }
    
    /**
     * Registra login do usuário
     */
    public void registrarLogin(Usuario usuario, boolean sucesso) {
        try {
            Map<String, Object> dadosLogin = Map.of(
                "usuarioId", usuario.getId(),
                "email", usuario.getEmail(),
                "sucesso", sucesso,
                "timestamp", LocalDateTime.now(),
                "ip", getClientIp(),
                "userAgent", getUserAgent()
            );
            
            registrarAuditoria("LOGIN", usuario.getId(), Auditoria.OperacaoAuditoria.INSERT, null, dadosLogin);
        } catch (Exception e) {
            System.err.println("Erro ao registrar login: " + e.getMessage());
        }
    }
    
    /**
     * Registra logout do usuário
     */
    public void registrarLogout(Usuario usuario) {
        try {
            Map<String, Object> dadosLogout = Map.of(
                "usuarioId", usuario.getId(),
                "email", usuario.getEmail(),
                "timestamp", LocalDateTime.now(),
                "ip", getClientIp()
            );
            
            registrarAuditoria("LOGOUT", usuario.getId(), Auditoria.OperacaoAuditoria.INSERT, null, dadosLogout);
        } catch (Exception e) {
            System.err.println("Erro ao registrar logout: " + e.getMessage());
        }
    }
    
    // ===== LIMPEZA E MANUTENÇÃO =====
    
    /**
     * Remove registros de auditoria antigos
     */
    @Transactional
    public void limparAuditoriaAntiga(int diasParaManterAuditoria) {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(diasParaManterAuditoria);
        auditoriaRepository.deleteByTimestampOperacaoBefore(dataLimite);
    }
    
    // ===== MÉTODOS AUXILIARES =====
    
    /**
     * Converte objeto para JSON
     */
    private String converterParaJson(Object objeto) {
        if (objeto == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(objeto);
        } catch (JsonProcessingException e) {
            return "Erro ao serializar: " + e.getMessage();
        }
    }
    
    /**
     * Obtém IP do cliente
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Obtém User-Agent do cliente
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
