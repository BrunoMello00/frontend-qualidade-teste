package com.tcc.estoque.dto;

import com.tcc.estoque.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTOs para operações relacionadas a senhas
 */
public class PasswordDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChangePasswordRequest {
        @NotBlank(message = "Senha atual é obrigatória")
        private String senhaAtual;

        @NotBlank(message = "Nova senha é obrigatória")
        @StrongPassword(
            minLength = 8,
            message = "Nova senha deve ser forte: mínimo 8 caracteres com maiúscula, minúscula, número e caractere especial"
        )
        private String novaSenha;

        @NotBlank(message = "Confirmação de senha é obrigatória")
        private String confirmarSenha;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResetPasswordRequest {
        @NotBlank(message = "Código de redefinição é obrigatório")
        @Size(min = 6, max = 10, message = "Código deve ter entre 6 e 10 caracteres")
        private String codigo;

        @NotBlank(message = "Nova senha é obrigatória")
        @StrongPassword(
            minLength = 8,
            message = "Nova senha deve ser forte: mínimo 8 caracteres com maiúscula, minúscula, número e caractere especial"
        )
        private String novaSenha;

        @NotBlank(message = "Confirmação de senha é obrigatória")
        private String confirmarSenha;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PasswordValidationRequest {
        @NotBlank(message = "Senha é obrigatória")
        private String senha;

        private String email; // Opcional, para validações contextuais
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PasswordValidationResponse {
        private boolean valida;
        private int pontuacao;
        private String nivel;
        private String[] erros;
        private String[] avisos;
        private String[] sugestoes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PasswordStrengthResponse {
        private int pontuacao;
        private String nivel;
        private boolean temMaiuscula;
        private boolean temMinuscula;
        private boolean temNumero;
        private boolean temCaractereEspecial;
        private boolean comprimentoAdequado;
        private boolean semPadroesInseguros;
        private String[] sugestoesMelhoria;
    }
}
