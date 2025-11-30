package com.tcc.estoque.controller;

import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.service.ConfiguracaoService;
import com.tcc.estoque.dto.ConfiguracaoBackupDto;
import com.tcc.estoque.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para configurações do sistema
 */
@RestController
@RequestMapping("/configuracoes")
@CrossOrigin(origins = "*")
public class ConfiguracaoController {

    @Autowired
    private SecurityUtil securityUtil;
    
    @Autowired
    private ConfiguracaoService configuracaoService;

    /**
     * Busca todas as configurações
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> obterConfiguracoes() {
        try {
            securityUtil.getUsuarioLogado();
            
            Map<String, Object> response = new HashMap<>();
            response.put("sistema", configuracaoService.buscarConfiguracoesSistema());
            response.put("seguranca", configuracaoService.buscarConfiguracoesSeguranca());
            response.put("estoque", configuracaoService.buscarConfiguracoesEstoque());
            response.put("vendas", configuracaoService.buscarConfiguracoesVendas());
            response.put("notificacoes", configuracaoService.buscarConfiguracoesNotificacoes());
            response.put("aparencia", configuracaoService.buscarConfiguracoesAparencia());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno do servidor"));
        }
    }

    /**
     * Atualiza configurações do sistema
     */
    @PutMapping("/sistema")
    public ResponseEntity<Map<String, Object>> atualizarConfiguracoesSistema(
            @RequestBody Map<String, Object> configuracoes) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            configuracaoService.atualizarConfiguracoesSistema(configuracoes, usuario);
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Configurações do sistema atualizadas com sucesso",
                "configuracoes", configuracaoService.buscarConfiguracoesSistema(),
                "dataAtualizacao", LocalDateTime.now(),
                "atualizadoPor", usuario.getNome()
            );
            
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Erro ao atualizar configurações"));
        }
    }

    /**
     * Atualiza configurações de segurança
     */
    @PutMapping("/seguranca")
    public ResponseEntity<Map<String, Object>> atualizarConfiguracoesSeguranca(
            @RequestBody Map<String, Object> configuracoes) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            configuracaoService.atualizarConfiguracoesSeguranca(configuracoes, usuario);
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Configurações de segurança atualizadas com sucesso",
                "configuracoes", configuracaoService.buscarConfiguracoesSeguranca(),
                "dataAtualizacao", LocalDateTime.now(),
                "atualizadoPor", usuario.getNome()
            );
            
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Erro ao atualizar configurações de segurança"));
        }
    }

    /**
     * Configura alertas de estoque
     */
    @PutMapping("/estoque")
    public ResponseEntity<Map<String, Object>> configurarEstoque(
            @RequestBody Map<String, Object> configuracoes) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            configuracaoService.atualizarConfiguracoesEstoque(configuracoes, usuario);
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Configurações de estoque atualizadas",
                "configuracoes", configuracaoService.buscarConfiguracoesEstoque(),
                "dataAtualizacao", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Erro ao configurar estoque"));
        }
    }

    /**
     * Configura regras de vendas
     */
    @PutMapping("/vendas")
    public ResponseEntity<Map<String, Object>> configurarVendas(
            @RequestBody Map<String, Object> configuracoes) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            configuracaoService.atualizarConfiguracoesVendas(configuracoes, usuario);
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Configurações de vendas atualizadas",
                "configuracoes", configuracaoService.buscarConfiguracoesVendas(),
                "dataAtualizacao", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Erro ao configurar vendas"));
        }
    }

    /**
     * Configura preferências de notificações
     */
    @PutMapping("/notificacoes")
    public ResponseEntity<Map<String, Object>> configurarNotificacoes(
            @RequestBody Map<String, Object> configuracoes) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            configuracaoService.atualizarConfiguracoesNotificacoes(configuracoes, usuario);
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Configurações de notificações atualizadas",
                "configuracoes", configuracaoService.buscarConfiguracoesNotificacoes(),
                "dataAtualizacao", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Erro ao configurar notificações"));
        }
    }

    /**
     * Configura aparência e tema do sistema
     */
    @PutMapping("/aparencia")
    public ResponseEntity<Map<String, Object>> configurarAparencia(
            @RequestBody Map<String, Object> configuracoes) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            configuracaoService.atualizarConfiguracoesAparencia(configuracoes, usuario);
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Configurações de aparência atualizadas",
                "configuracoes", configuracaoService.buscarConfiguracoesAparencia(),
                "dataAtualizacao", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Erro ao configurar aparência"));
        }
    }

    /**
     * Executa backup manual das configurações
     */
    @PostMapping("/backup/executar")
    public ResponseEntity<Map<String, Object>> executarBackup() {
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            ConfiguracaoBackupDto backup = configuracaoService.executarBackup();
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Backup executado com sucesso",
                "backup", backup,
                "executadoPor", usuario.getNome()
            );
            
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro ao executar backup"));
        }
    }

    /**
     * Histórico de backups de configurações
     */
    @GetMapping("/backup/historico")
    public ResponseEntity<Map<String, Object>> historicoBackups() {
        try {
            List<ConfiguracaoBackupDto> backups = configuracaoService.buscarHistoricoBackups();
            
            Map<String, Object> resposta = Map.of(
                "backups", backups,
                "total", backups.size(),
                "dataConsulta", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno do servidor"));
        }
    }

    /**
     * Validações importantes das configurações
     */
    @GetMapping("/validacoes")
    public ResponseEntity<Map<String, Object>> obterValidacoes() {
        try {
            Map<String, Object> validacoes = Map.of(
                "mfaHabilitado", configuracaoService.isMfaHabilitado(),
                "timeoutSessao", configuracaoService.getTimeoutSessao(),
                "sistemaEmManutencao", configuracaoService.isSistemaEmManutencao()
            );
            
            return ResponseEntity.ok(validacoes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno do servidor"));
        }
    }

    /**
     * Inicializa configurações padrão
     */
    @PostMapping("/inicializar")
    public ResponseEntity<Map<String, Object>> inicializarConfiguracoes() {
        try {
            configuracaoService.inicializarConfiguracoesDefault();
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Configurações padrão inicializadas com sucesso"
            );
            
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro ao inicializar configurações"));
        }
    }
}
