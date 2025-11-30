package com.tcc.estoque.controller;

import com.tcc.estoque.model.Auditoria;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.service.AuditoriaService;
import com.tcc.estoque.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para auditoria e logs do sistema
 */
@RestController
@RequestMapping("/auditoria")
@CrossOrigin(origins = "*")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;
    
    @Autowired
    private SecurityUtil securityUtil;

    /**
     * Busca auditoria com filtros
     */
    @GetMapping
    public ResponseEntity<Page<Auditoria>> buscarAuditoria(
            @RequestParam(required = false) String tabela,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String operacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Page<Auditoria> auditoria = auditoriaService.buscarComFiltros(
                tabela, usuarioId, operacao, inicio, fim, page, size
            );
            
            return ResponseEntity.ok(auditoria);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Busca auditoria por tabela específica
     */
    @GetMapping("/tabela/{tabela}")
    public ResponseEntity<Page<Auditoria>> buscarPorTabela(
            @PathVariable String tabela,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Page<Auditoria> auditoria = auditoriaService.buscarPorTabela(tabela, page, size);
            return ResponseEntity.ok(auditoria);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Busca auditoria por usuário
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Page<Auditoria>> buscarPorUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Page<Auditoria> auditoria = auditoriaService.buscarPorUsuario(usuarioId, page, size);
            return ResponseEntity.ok(auditoria);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Busca histórico de um registro específico
     */
    @GetMapping("/historico/{tabela}/{registroId}")
    public ResponseEntity<List<Auditoria>> buscarHistoricoRegistro(
            @PathVariable String tabela,
            @PathVariable Long registroId) {
        
        try {
            List<Auditoria> historico = auditoriaService.buscarHistoricoRegistro(tabela, registroId);
            return ResponseEntity.ok(historico);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Busca auditoria por período
     */
    @GetMapping("/periodo")
    public ResponseEntity<Page<Auditoria>> buscarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Page<Auditoria> auditoria = auditoriaService.buscarPorPeriodo(inicio, fim, page, size);
            return ResponseEntity.ok(auditoria);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Busca auditoria por IP
     */
    @GetMapping("/ip/{ipAddress}")
    public ResponseEntity<Page<Auditoria>> buscarPorIp(
            @PathVariable String ipAddress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Page<Auditoria> auditoria = auditoriaService.buscarPorIp(ipAddress, page, size);
            return ResponseEntity.ok(auditoria);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Gera estatísticas de auditoria
     */
    @GetMapping("/estatisticas")
    public ResponseEntity<Map<String, Object>> gerarEstatisticas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        
        try {
            if (inicio == null) {
                inicio = LocalDateTime.now().minusDays(30);
            }
            if (fim == null) {
                fim = LocalDateTime.now();
            }
            
            Map<String, Object> estatisticas = auditoriaService.gerarEstatisticas(inicio, fim);
            
            Map<String, Object> response = new HashMap<>();
            response.put("periodo", Map.of("inicio", inicio, "fim", fim));
            response.put("estatisticas", estatisticas);
            response.put("geradoEm", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro ao gerar estatísticas"));
        }
    }
    
    /**
     * Busca últimas atividades do usuário logado
     */
    @GetMapping("/minhas-atividades")
    public ResponseEntity<List<Auditoria>> buscarMinhasAtividades(
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            List<Auditoria> atividades = auditoriaService.buscarUltimasAtividades(usuario, limit);
            return ResponseEntity.ok(atividades);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Busca operações de exclusão (críticas)
     */
    @GetMapping("/exclusoes")
    public ResponseEntity<Page<Auditoria>> buscarExclusoes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Page<Auditoria> exclusoes = auditoriaService.buscarExclusoes(page, size);
            return ResponseEntity.ok(exclusoes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Registra acesso a recurso sensível manualmente
     */
    @PostMapping("/acesso-recurso")
    public ResponseEntity<Map<String, Object>> registrarAcessoRecurso(
            @RequestBody Map<String, String> dados) {
        
        try {
            String recurso = dados.get("recurso");
            String acao = dados.get("acao");
            
            if (recurso == null || acao == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Recurso e ação são obrigatórios"));
            }
            
            auditoriaService.registrarAcessoRecurso(recurso, acao);
            
            return ResponseEntity.ok(Map.of(
                "sucesso", true,
                "mensagem", "Acesso a recurso registrado",
                "recurso", recurso,
                "acao", acao,
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro ao registrar acesso"));
        }
    }
    
    /**
     * Dashboard de auditoria
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboardAuditoria() {
        try {
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime inicioHoje = agora.toLocalDate().atStartOfDay();
            LocalDateTime inicioSemana = agora.minusDays(7);
            LocalDateTime inicioMes = agora.minusDays(30);
            
            Map<String, Object> dashboard = new HashMap<>();
            
            Map<String, Object> estatisticasHoje = auditoriaService.gerarEstatisticas(inicioHoje, agora);
            dashboard.put("hoje", estatisticasHoje);
            
            Map<String, Object> estatisticasSemana = auditoriaService.gerarEstatisticas(inicioSemana, agora);
            dashboard.put("semana", estatisticasSemana);
            
            Map<String, Object> estatisticasMes = auditoriaService.gerarEstatisticas(inicioMes, agora);
            dashboard.put("mes", estatisticasMes);
            
            dashboard.put("geradoEm", agora);
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro ao gerar dashboard"));
        }
    }
    
    /**
     * Limpeza de auditoria antiga (apenas admin)
     */
    @DeleteMapping("/limpeza/{dias}")
    public ResponseEntity<Map<String, Object>> limparAuditoriaAntiga(
            @PathVariable int dias) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            if (!"ADMIN".equals(usuario.getTipoUsuario().toString())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Apenas administradores podem executar limpeza"));
            }
            
            auditoriaService.limparAuditoriaAntiga(dias);
            
            return ResponseEntity.ok(Map.of(
                "sucesso", true,
                "mensagem", "Limpeza de auditoria executada",
                "diasRemovidos", dias,
                "executadoPor", usuario.getEmail(),
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro ao executar limpeza"));
        }
    }
    
    /**
     * Health check da auditoria
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime ontemInicio = agora.minusDays(1);
            
            Map<String, Object> estatisticas = auditoriaService.gerarEstatisticas(ontemInicio, agora);
            Long operacoesRecentes = (Long) estatisticas.get("totalOperacoes");
            
            Map<String, Object> health = Map.of(
                "status", "OK",
                "sistemaAuditoria", "FUNCIONANDO",
                "operacoesUltimas24h", operacoesRecentes,
                "timestamp", agora
            );
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "status", "ERROR", 
                        "sistemaAuditoria", "ERRO",
                        "erro", e.getMessage()
                    ));
        }
    }
}
