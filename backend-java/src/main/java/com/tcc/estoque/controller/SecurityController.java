package com.tcc.estoque.controller;

import com.tcc.estoque.security.annotation.RequirePageAccess;
import com.tcc.estoque.service.AccountLockoutService;
import com.tcc.estoque.service.AdvancedPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Controller para funcionalidades avançadas de segurança
 */
@RestController
@RequestMapping("/security")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Segurança", description = "Endpoints para gerenciamento de segurança")
@RequirePageAccess("seguranca") // Página de segurança - apenas ADMIN e OWNER
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class SecurityController {

    private final AccountLockoutService accountLockoutService;
    private final AdvancedPasswordService passwordService;

    /**
     * Verificar força de uma senha
     */
    @Operation(summary = "Verificar força da senha", description = "Analisa a força e segurança de uma senha")
    @PostMapping("/password/validate")
    public ResponseEntity<?> validatePassword(@Valid @RequestBody PasswordValidationRequest request) {
        log.debug("Validando força da senha para email: {}", request.getEmail());
        
        AdvancedPasswordService.PasswordValidationResult result = 
            passwordService.validatePassword(request.getPassword(), request.getEmail());
        
        return ResponseEntity.ok(Map.of(
            "valid", result.isValid(),
            "strengthScore", result.getStrengthScore(),
            "strengthLevel", result.getStrengthLevel(),
            "errors", result.getErrors(),
            "warnings", result.getWarnings()
        ));
    }

    /**
     * Gerar sugestão de senha segura
     */
    @Operation(summary = "Gerar senha segura", description = "Gera uma sugestão de senha segura")
    @GetMapping("/password/suggest")
    public ResponseEntity<?> suggestPassword() {
        String suggestion = passwordService.generatePasswordSuggestion();
        
        return ResponseEntity.ok(Map.of(
            "suggestion", suggestion,
            "tip", "Personalize esta sugestão adicionando caracteres especiais ou números únicos"
        ));
    }

    /**
     * Verificar status de bloqueio de uma conta
     */
    @Operation(summary = "Verificar status da conta", description = "Verifica se uma conta está bloqueada")
    @GetMapping("/account/status/{email}")
    
    public ResponseEntity<?> getAccountStatus(
            @Parameter(description = "Email da conta")
            @PathVariable String email) {
        
        log.debug("Verificando status da conta: {}", email);
        
        AccountLockoutService.AccountStatus status = accountLockoutService.getAccountStatus(email);
        
        return ResponseEntity.ok(Map.of(
            "email", email,
            "locked", status.isLocked(),
            "failedAttempts", status.getFailedAttempts(),
            "remainingAttempts", status.getRemainingAttempts(),
            "lockoutExpiry", status.getLockoutExpiry()
        ));
    }

    /**
     * Desbloquear uma conta manualmente (apenas administradores)
     */
    @Operation(summary = "Desbloquear conta", description = "Remove o bloqueio de uma conta (apenas administradores)")
    @PostMapping("/account/unlock")
    
    public ResponseEntity<?> unlockAccount(@Valid @RequestBody UnlockAccountRequest request) {
        log.info("Desbloqueio manual de conta solicitado para: {}", request.getEmail());
        
        accountLockoutService.unlockAccount(request.getEmail());
        
        return ResponseEntity.ok(Map.of(
            "message", "Conta desbloqueada com sucesso",
            "email", request.getEmail()
        ));
    }



    /**
     * Request para validação de senha
     */
    public static class PasswordValidationRequest {
        @NotBlank(message = "Senha é obrigatória")
        private String password;
        
        @Email(message = "Email deve ser válido")
        private String email;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * Request para desbloqueio de conta
     */
    public static class UnlockAccountRequest {
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String email;

        @NotBlank(message = "Razão é obrigatória")
        private String reason;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
