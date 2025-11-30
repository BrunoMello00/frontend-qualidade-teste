package com.tcc.estoque.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação para validação de senhas fortes
 * Garante que a senha atenda aos critérios de segurança
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    
    String message() default "Senha deve ser forte: mínimo 8 caracteres, contendo pelo menos 1 letra maiúscula, 1 minúscula, 1 número e 1 caractere especial";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Comprimento mínimo da senha
     */
    int minLength() default 8;
    
    /**
     * Comprimento máximo da senha
     */
    int maxLength() default 128;
    
    /**
     * Exigir pelo menos uma letra maiúscula
     */
    boolean requireUppercase() default true;
    
    /**
     * Exigir pelo menos uma letra minúscula
     */
    boolean requireLowercase() default true;
    
    /**
     * Exigir pelo menos um dígito
     */
    boolean requireDigit() default true;
    
    /**
     * Exigir pelo menos um caractere especial
     */
    boolean requireSpecialChar() default true;
    
    /**
     * Não permitir caracteres repetidos consecutivos
     */
    boolean disallowRepeatedChars() default true;
    
    /**
     * Não permitir sequências comuns (123, abc, etc.)
     */
    boolean disallowSequences() default true;
}
