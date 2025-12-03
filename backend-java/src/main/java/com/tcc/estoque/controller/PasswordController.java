package com.tcc.estoque.controller;

import com.tcc.estoque.dto.PasswordDTO;
import com.tcc.estoque.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Controller para operações relacionadas a senhas
 */
@RestController
@RequestMapping("/password")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Senhas", description = "Endpoints para gerenciamento de senhas")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class PasswordController {

    private final PasswordService passwordService;

    /**
     * Validar força de uma senha
     */
    @Operation(summary = "Validar força da senha", 
               description = "Analisa a força e segurança de uma senha fornecida")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validação realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping("/validate")
    public ResponseEntity<PasswordDTO.PasswordValidationResponse> validatePassword(
            @Parameter(description = "Dados da senha para validação")
            @Valid @RequestBody PasswordDTO.PasswordValidationRequest request) {
        
        log.debug("Validando força da senha");
        
        PasswordDTO.PasswordValidationResponse response = passwordService.validatePasswordStrength(
            request.getSenha(), 
            request.getEmail()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obter análise detalhada da força da senha
     */
    @Operation(summary = "Análise detalhada da senha", 
               description = "Retorna análise completa da força da senha com critérios específicos")
    @PostMapping("/analyze")
    public ResponseEntity<PasswordDTO.PasswordStrengthResponse> analyzePassword(
            @Parameter(description = "Dados da senha para análise")
            @Valid @RequestBody PasswordDTO.PasswordValidationRequest request) {
        
        log.debug("Analisando senha detalhadamente");
        
        PasswordDTO.PasswordStrengthResponse response = passwordService.analyzePasswordStrength(
            request.getSenha()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gerar sugestão de senha forte
     */
    @Operation(summary = "Gerar senha forte", 
               description = "Gera uma sugestão de senha que atende a todos os critérios de segurança")
    @GetMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateStrongPassword(
            @Parameter(description = "Comprimento da senha (mínimo 8, máximo 128)")
            @RequestParam(defaultValue = "12") int length) {
        
        if (length < 8 || length > 128) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Comprimento deve estar entre 8 e 128 caracteres"));
        }
        
        log.debug("Gerando senha forte com {} caracteres", length);
        
        String strongPassword = passwordService.generateStrongPassword(length);
        PasswordDTO.PasswordStrengthResponse analysis = passwordService.analyzePasswordStrength(strongPassword);
        
        return ResponseEntity.ok(Map.of(
            "senha", strongPassword,
            "pontuacao", analysis.getPontuacao(),
            "nivel", analysis.getNivel(),
            "dica", "Personalize esta senha substituindo alguns caracteres por símbolos ou números especiais"
        ));
    }

    /**
     * Alterar senha do usuário logado
     */
    @Operation(summary = "Alterar senha", 
               description = "Permite que o usuário altere sua senha atual")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha atual incorreta"),
        @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    @PostMapping("/change")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            @Parameter(description = "Dados para alteração de senha")
            @Valid @RequestBody PasswordDTO.ChangePasswordRequest request,
            Authentication authentication) {
        
        log.info("Solicitação de alteração de senha para usuário: {}", authentication.getName());
        
        if (!request.getNovaSenha().equals(request.getConfirmarSenha())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Nova senha e confirmação não coincidem"));
        }
        
        try {
            passwordService.changePassword(
                authentication.getName(),
                request.getSenhaAtual(),
                request.getNovaSenha()
            );
            
            log.info("Senha alterada com sucesso para usuário: {}", authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
            
        } catch (Exception e) {
            log.warn("Falha ao alterar senha para usuário {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Redefinir senha com código de recuperação
     */
    @Operation(summary = "Redefinir senha", 
               description = "Redefine a senha usando código de recuperação enviado por email")
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Parameter(description = "Dados para redefinição de senha")
            @Valid @RequestBody PasswordDTO.ResetPasswordRequest request) {
        
        log.info("Solicitação de redefinição de senha com código: {}", request.getCodigo());
        
        if (!request.getNovaSenha().equals(request.getConfirmarSenha())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Nova senha e confirmação não coincidem"));
        }
        
        try {
            passwordService.resetPassword(request.getCodigo(), request.getNovaSenha());
            
            log.info("Senha redefinida com sucesso para código: {}", request.getCodigo());
            return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso"));
            
        } catch (Exception e) {
            log.warn("Falha ao redefinir senha com código {}: {}", request.getCodigo(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obter política de senhas atual
     */
    @Operation(summary = "Política de senhas", 
               description = "Retorna os critérios atuais para senhas fortes")
    @GetMapping("/policy")
    public ResponseEntity<Map<String, Object>> getPasswordPolicy() {
        
        return ResponseEntity.ok(Map.of(
            "minimoCaracteres", 8,
            "maximoCaracteres", 128,
            "requerMaiuscula", true,
            "requerMinuscula", true,
            "requerNumero", true,
            "requerCaractereEspecial", true,
            "proibeCaracteresRepetidos", true,
            "proibeSequencias", true,
            "criterios", new String[]{
                "Mínimo 8 caracteres",
                "Pelo menos 1 letra maiúscula (A-Z)",
                "Pelo menos 1 letra minúscula (a-z)", 
                "Pelo menos 1 dígito (0-9)",
                "Pelo menos 1 caractere especial (!@#$%^&*...)",
                "Não pode ter caracteres repetidos consecutivos (aaa, 111)",
                "Não pode conter sequências simples (123, abc, qwerty)",
                "Não pode ser uma senha comum conhecida"
            }
        ));
    }
}
