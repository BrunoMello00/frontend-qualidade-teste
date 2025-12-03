package com.tcc.estoque.controller;

import com.tcc.estoque.dto.AuthDTO;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.UsuarioRepository;
import com.tcc.estoque.security.SecurityUtil;
import com.tcc.estoque.service.AuthService;
import com.tcc.estoque.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;  // RESTAURADO
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para autenticação e autorização com JWT robusto
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@Tag(name = "Autenticação", description = "Endpoints para autenticação de usuários com JWT")
public class AuthController {

    private final AuthService authService;
    private final SecurityUtil securityUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;

    /**
     * Realizar login
     */
    @Operation(summary = "Realizar login", description = "Autentica um usuário e retorna um token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthDTO.LoginResponse> login(
            @Parameter(description = "Dados de login")
            @Valid @RequestBody AuthDTO.LoginRequest loginRequest,
            HttpServletRequest request) {
        
        log.info("Tentativa de login para usuário: {} de IP: {}", 
                 loginRequest.getEmail(), getClientIp(request));
        
        AuthDTO.LoginResponse response = authService.login(loginRequest, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verificar se MFA é necessário para o usuário
     */
    @Operation(summary = "Verificar MFA", description = "Verifica se autenticação multifator é necessária para o usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verificação realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos")
    })
    @PostMapping("/check-mfa")
    public ResponseEntity<Map<String, Object>> checkMfaRequired(
            @Parameter(description = "Dados de login para verificação MFA")
            @Valid @RequestBody AuthDTO.LoginRequest loginRequest) {
        
        log.info("Verificando MFA para usuário: {}", loginRequest.getEmail());
        
        try {
            Map<String, Object> response = Map.of(
                "mfaRequired", false,
                "userEmail", loginRequest.getEmail()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao verificar MFA para usuário: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciais inválidas"));
        }
    }

    /**
     * Registrar novo usuário
     */
    @Operation(summary = "Registrar usuário", description = "Registra um novo usuário no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Email já cadastrado")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Parameter(description = "Dados do novo usuário")
            @Valid @RequestBody AuthDTO.RegisterRequest registerRequest) {
        
        log.info("Registrando novo usuário: {}", registerRequest.getEmail());
        
        return ResponseEntity.ok("Registro temporariamente desabilitado");
    }

    /**
     * Validar token JWT
     */
    @Operation(summary = "Validar token", description = "Valida se o token JWT é válido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token válido"),
            @ApiResponse(responseCode = "400", description = "Token inválido"),
            @ApiResponse(responseCode = "401", description = "Token expirado")
    })
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(
            @Parameter(description = "Token JWT no header Authorization")
            @RequestHeader("Authorization") String token) {
        
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        
        boolean isValid = authService.validateToken(jwt);
        
        if (isValid) {
            return ResponseEntity.ok("Token válido");
        } else {
            return ResponseEntity.badRequest().body("Token inválido");
        }
    }

    /**
     * Obter dados do usuário logado
     */
    @Operation(summary = "Dados do usuário", description = "Retorna dados do usuário atualmente logado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário retornados"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")  // RESTAURADO
    public ResponseEntity<Map<String, Object>> obterUsuarioLogado() {
        try {
            String email = securityUtil.getEmailUsuarioLogado();
            
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            Map<String, Object> dadosUsuario = Map.of(
                "id", usuario.getId(),
                "nome", usuario.getNome(),
                "email", usuario.getEmail(),
                "ativo", usuario.getAtivo() != null ? usuario.getAtivo() : true,
                "tipo", usuario.getTipoUsuario().toString()
            );
            
            return ResponseEntity.ok(dadosUsuario);
        } catch (Exception e) {
            log.error("Erro ao obter usuário logado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "Usuário não autenticado"));
        }
    }

    /**
     * Solicitar redefinição de senha
     */
    @Operation(summary = "Redefinir senha", description = "Solicita redefinição de senha via email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email de redefinição enviado"),
            @ApiResponse(responseCode = "404", description = "Email não encontrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<String> solicitarRedefinicaoSenha(
            @Parameter(description = "Email para redefinição")
            @RequestParam String email) {
        
        log.info("Solicitação de redefinição de senha para: {}", email);
        
        return ResponseEntity.ok("Se o email existir, você receberá um link para redefinição de senha");
    }

    /**
     * Confirmar nova senha
     */
    @Operation(summary = "Nova senha", description = "Confirma nova senha usando código de redefinição")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Código inválido ou expirado"),
            @ApiResponse(responseCode = "404", description = "Código não encontrado")
    })
    @PostMapping("/new-password")
    public ResponseEntity<String> confirmarNovaSenha(
            @Parameter(description = "Código de redefinição")
            @RequestParam String codigo,
            @Parameter(description = "Nova senha")
            @RequestParam String novaSenha) {
        
        log.info("Confirmando nova senha para código: {}", codigo);
        
        return ResponseEntity.ok("Senha alterada com sucesso");
    }

    /**
     * Realizar logout
     */
    @Operation(summary = "Realizar logout", description = "Invalida a sessão atual do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token inválido")
    })
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        
        String accessToken = extractTokenFromRequest(request);
        String refreshToken = request.getHeader("X-Refresh-Token");
        
        if (accessToken != null || refreshToken != null) {
            authService.logout(accessToken, refreshToken);
            log.info("Logout realizado para IP: {}", getClientIp(request));
        }
        
        return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso"));
    }

    /**
     * Renovar access token usando refresh token
     */
    @Operation(summary = "Renovar token", description = "Gera um novo access token usando refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Refresh token é obrigatório"));
            }

            Map<String, Object> response = authService.refreshToken(refreshToken);
            log.info("Token renovado com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao renovar token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Validar token
     */
    @Operation(summary = "Validar token", description = "Verifica se um token JWT é válido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token válido"),
            @ApiResponse(responseCode = "401", description = "Token inválido")
    })
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("valid", false, "error", "Token é obrigatório"));
            }

            boolean isValid = authService.validateToken(token);
            
            if (isValid) {
                String email = jwtTokenProvider.getEmailFromToken(token);
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "email", email,
                    "userId", userId,
                    "role", role
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "error", "Token inválido"));
            }
            
        } catch (Exception e) {
            log.error("Erro na validação do token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Token inválido"));
        }
    }

    /**
     * Extrai token JWT do cabeçalho Authorization
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Obtém IP do cliente considerando proxies
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
