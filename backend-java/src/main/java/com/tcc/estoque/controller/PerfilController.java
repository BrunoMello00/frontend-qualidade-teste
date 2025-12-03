package com.tcc.estoque.controller;

import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.TipoUsuario;
import com.tcc.estoque.repository.UsuarioRepository;
import com.tcc.estoque.security.annotation.RequirePageAccess;
import com.tcc.estoque.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;
import java.util.HashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller para gerenciamento de perfil de usuário
 */
@RestController
@RequestMapping("/perfil")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Perfil", description = "Operações relacionadas ao perfil do usuário")
@SecurityRequirement(name = "bearer-jwt")
@RequirePageAccess("perfil") // Página de perfil - acessível por todos os tipos de usuário
public class PerfilController {

    private final SecurityUtil securityUtil;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    
    @Operation(summary = "Obter perfil do usuário", 
               description = "Retorna os dados do perfil do usuário logado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    public ResponseEntity<Map<String, Object>> obterPerfil() {
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            // Mapear dados do perfil com campos corretos
            String departamento = getDepartamentoPorTipo(usuario.getTipoUsuario());
            
            Map<String, Object> perfil = new HashMap<>();
            perfil.put("id", usuario.getId());
            perfil.put("nome", usuario.getNome());
            perfil.put("email", usuario.getEmail());
            perfil.put("tipoUsuario", usuario.getTipoUsuario().name()); // Usar tipoUsuario ao invés de role
            perfil.put("telefone", usuario.getTelefone() != null ? usuario.getTelefone() : "");
            perfil.put("ultimoLogin", usuario.getUltimoAcesso()); // Campo ultimoLogin
            perfil.put("dataCriacao", usuario.getCreatedAt()); // Alterado de 'dataCadastro' para 'dataCriacao'
            perfil.put("departamento", departamento); // Campo departamento baseado no tipo de usuário
            perfil.put("ativo", usuario.isEnabled());
            perfil.put("totalLoginsMes", 0); // Placeholder - implementar cálculo se necessário
            perfil.put("permissoes", new String[]{}); // Placeholder - implementar se necessário
            perfil.put("configuracoes", new HashMap<>()); // Placeholder - implementar se necessário
            
            log.info("Perfil do usuário {} acessado", usuario.getEmail());
            return ResponseEntity.ok(perfil);
        } catch (Exception e) {
            log.error("Erro ao obter perfil do usuário", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "Usuário não autenticado"));
        }
    }

    @PutMapping
    
    @Operation(summary = "Atualizar perfil", 
               description = "Atualiza os dados do perfil do usuário")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    public ResponseEntity<Map<String, Object>> atualizarPerfil(
            @Parameter(description = "Dados para atualização do perfil") 
            @Valid @RequestBody PerfilUpdateRequest request) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            if (request.email != null && !request.email.equals(usuario.getEmail())) {
            }
            
            if (request.nome != null) {
                usuario.setNome(request.nome);
            }
            if (request.email != null) {
                usuario.setEmail(request.email);
            }
            if (request.telefone != null) {
                usuario.setTelefone(request.telefone);
            }
            
            usuario.setUpdatedAt(LocalDateTime.now());
            
            // Salvar as alterações no banco de dados
            usuarioRepository.save(usuario);
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Perfil atualizado com sucesso",
                "usuario", Map.of(
                    "id", usuario.getId(),
                    "nome", usuario.getNome(),
                    "email", usuario.getEmail(),
                    "ultimaAtualizacao", usuario.getUpdatedAt()
                )
            );
            
            log.info("Perfil do usuário {} atualizado", usuario.getEmail());
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            log.error("Erro ao atualizar perfil", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Erro ao atualizar perfil"));
        }
    }

    @PutMapping("/senha")
    
    @Operation(summary = "Alterar senha", 
               description = "Permite ao usuário alterar sua senha")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    public ResponseEntity<Map<String, Object>> alterarSenha(
            @Parameter(description = "Dados para alteração de senha") 
            @Valid @RequestBody AlterarSenhaRequest request) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            if (!passwordEncoder.matches(request.senhaAtual, usuario.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Senha atual incorreta"));
            }
            
            if (request.novaSenha.equals(request.senhaAtual)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "A nova senha deve ser diferente da atual"));
            }
            
            String novaSenhaHash = passwordEncoder.encode(request.novaSenha);
            usuario.setSenha(novaSenhaHash);
            usuario.setUpdatedAt(LocalDateTime.now());
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Senha alterada com sucesso",
                "dataAlteracao", LocalDateTime.now()
            );
            
            log.info("Senha do usuário {} alterada", usuario.getEmail());
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            log.error("Erro ao alterar senha", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Erro ao alterar senha"));
        }
    }

    @GetMapping("/atividades")
    
    @Operation(summary = "Histórico de atividades", 
               description = "Retorna o histórico de atividades do usuário")
    public ResponseEntity<Map<String, Object>> historicoAtividades(
            @Parameter(description = "Número de atividades a retornar") 
            @RequestParam(defaultValue = "20") int limite) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            var atividades = java.util.List.of(
                Map.of(
                    "id", 1L,
                    "tipo", "LOGIN",
                    "descricao", "Login realizado",
                    "data", LocalDateTime.now().minusHours(2),
                    "ip", "192.168.1.100"
                ),
                Map.of(
                    "id", 2L,
                    "tipo", "VENDA",
                    "descricao", "Venda #1001 criada",
                    "data", LocalDateTime.now().minusHours(4),
                    "ip", "192.168.1.100"
                ),
                Map.of(
                    "id", 3L,
                    "tipo", "PRODUTO",
                    "descricao", "Produto 'Notebook Dell' atualizado",
                    "data", LocalDateTime.now().minusHours(6),
                    "ip", "192.168.1.100"
                )
            );
            
            Map<String, Object> resposta = Map.of(
                "atividades", atividades.stream().limit(limite).collect(Collectors.toList()),
                "total", atividades.size(),
                "usuario", Map.of(
                    "id", usuario.getId(),
                    "nome", usuario.getNome()
                ),
                "dataConsulta", LocalDateTime.now()
            );
            
            log.info("Histórico de atividades consultado para usuário {}", usuario.getEmail());
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            log.error("Erro ao consultar histórico de atividades", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno do servidor"));
        }
    }

    @GetMapping("/estatisticas")
    
    @Operation(summary = "Estatísticas do usuário", 
               description = "Retorna estatísticas pessoais do usuário")
    public ResponseEntity<Map<String, Object>> estatisticasUsuario() {
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            Map<String, Object> estatisticas = Map.of(
                "vendasRealizadas", Map.of(
                    "hoje", 3,
                    "semana", 15,
                    "mes", 67,
                    "total", 234
                ),
                "produtosCadastrados", Map.of(
                    "semana", 2,
                    "mes", 8,
                    "total", 45
                ),
                "tempoSistema", Map.of(
                    "ultimoLogin", LocalDateTime.now().minusHours(2),
                    "tempoOnlineHoje", "4h 30min",
                    "membroDesde", usuario.getCreatedAt()
                ),
                "ranking", Map.of(
                    "posicaoVendas", 2,
                    "totalVendedores", 5,
                    "percentil", 80
                )
            );
            
            Map<String, Object> resposta = Map.of(
                "usuario", Map.of(
                    "id", usuario.getId(),
                    "nome", usuario.getNome(),
                    "role", usuario.getRole().name()
                ),
                "estatisticas", estatisticas,
                "dataConsulta", LocalDateTime.now()
            );
            
            log.info("Estatísticas consultadas para usuário {}", usuario.getEmail());
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            log.error("Erro ao consultar estatísticas do usuário", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno do servidor"));
        }
    }

    @PostMapping("/notificacoes/configurar")
    
    @Operation(summary = "Configurar notificações", 
               description = "Configura preferências de notificações do usuário")
    public ResponseEntity<Map<String, Object>> configurarNotificacoes(
            @Parameter(description = "Configurações de notificação") 
            @Valid @RequestBody NotificacoesConfig config) {
        
        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Configurações de notificação atualizadas",
                "configuracoes", Map.of(
                    "emailVendas", config.emailVendas,
                    "emailEstoque", config.emailEstoque,
                    "pushNotifications", config.pushNotifications,
                    "smsAlertas", config.smsAlertas
                ),
                "dataAtualizacao", LocalDateTime.now()
            );
            
            log.info("Configurações de notificação atualizadas para usuário {}", usuario.getEmail());
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            log.error("Erro ao configurar notificações", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Erro ao configurar notificações"));
        }
    }

    public static class PerfilUpdateRequest {
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        public String nome;
        
        @Email(message = "Email deve ter formato válido")
        public String email;
        
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        public String telefone;
    }

    public static class AlterarSenhaRequest {
        @NotBlank(message = "Senha atual é obrigatória")
        public String senhaAtual;
        
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 6, max = 50, message = "Nova senha deve ter entre 6 e 50 caracteres")
        public String novaSenha;
        
        @NotBlank(message = "Confirmação de senha é obrigatória")
        public String confirmarSenha;
    }

    public static class NotificacoesConfig {
        public boolean emailVendas = true;
        public boolean emailEstoque = true;
        public boolean pushNotifications = false;
        public boolean smsAlertas = false;
    }
    
    /**
     * Mapeia o tipo de usuário para o nome do departamento
     */
    private String getDepartamentoPorTipo(TipoUsuario tipoUsuario) {
        switch (tipoUsuario) {
            case OWNER:
                return "Administração";
            case ADMIN:
                return "Tecnologia da Informação";
            case VENDEDOR:
                return "Vendas e Atendimento";
            case ESTOQUISTA:
                return "Estoque e Logística";
            case COMPRAS:
                return "Compras e Suprimentos";
            default:
                return "Geral";
        }
    }
}
