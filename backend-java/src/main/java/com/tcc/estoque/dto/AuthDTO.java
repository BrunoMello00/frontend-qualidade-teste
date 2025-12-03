package com.tcc.estoque.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTOs para operações de autenticação
 */
public class AuthDTO {

    /**
     * DTO para informações do usuário
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioInfo {
        private Long id;
        private String nome;
        private String email;
        private String role;
        private String tipoUsuario;
    }

    /**
     * DTO para requisições de login
     */
    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ter um formato válido")
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        private String senha;
    }

    /**
     * DTO para resposta de login
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private boolean success;
        private String message;
        private String token;
        private String refreshToken;
        private UsuarioInfo usuario;
        private Long expiresIn;
    }

    /**
     * DTO para requisições de registro
     */
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Nome é obrigatório")
        private String nome;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ter um formato válido")
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        private String senha;

        @NotBlank(message = "Confirmação de senha é obrigatória")
        private String confirmarSenha;
    }

    /**
     * DTO para resposta de usuário
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioResponse {
        private Long id;
        private String nome;
        private String email;
        private String role;
        private Boolean ativo;
    }

    /**
     * DTO para requisições de redefinição de senha
     */
    @Data
    public static class RedefinirSenhaRequest {
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ter um formato válido")
        private String email;
    }

    /**
     * DTO para requisições de nova senha
     */
    @Data
    public static class NovaSenhaRequest {
        @NotBlank(message = "Token é obrigatório")
        private String token;

        @NotBlank(message = "Nova senha é obrigatória")
        private String novaSenha;

        @NotBlank(message = "Confirmação de senha é obrigatória")
        private String confirmarSenha;
    }

    /**
     * DTO para resposta de autenticação
     */
    @Data
    public static class AuthResponse {
        private boolean success;
        private String message;
        private String token;
        private String refreshToken;
        private UsuarioInfo usuario;
        private Long expiresIn;
    }

    /**
     * DTO para resposta padrão
     */
    @Data
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object dados;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public ApiResponse(boolean success, String message, Object dados) {
            this.success = success;
            this.message = message;
            this.dados = dados;
        }
    }
}