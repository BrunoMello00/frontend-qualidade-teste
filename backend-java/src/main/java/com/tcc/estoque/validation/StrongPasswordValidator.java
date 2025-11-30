package com.tcc.estoque.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Validador para senhas fortes
 * Implementa verificações avançadas de segurança
 */
@Slf4j
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private StrongPassword annotation;
    
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+=\\[\\]{};':\"\\\\|,.<>\\/?~`-]");
    private static final Pattern REPEATED_CHARS_PATTERN = Pattern.compile("(.)\\1{2,}");
    private static final Pattern SEQUENTIAL_PATTERN = Pattern.compile("(?i)(012|123|234|345|456|567|678|789|890|abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)");

    private static final String[] COMMON_PASSWORDS = {
        "password", "123456", "password123", "admin", "qwerty", "letmein", 
        "welcome", "monkey", "dragon", "password1", "123456789", "football",
        "iloveyou", "admin123", "welcome123", "password!", "123123123",
        "senha", "senha123", "administrador", "usuario", "12345678"
    };

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.trim().isEmpty()) {
            addViolation(context, "Senha não pode ser vazia");
            return false;
        }

        boolean isValid = true;
        
        context.disableDefaultConstraintViolation();
        
        // 1. Verificar comprimento
        if (password.length() < annotation.minLength()) {
            addViolation(context, String.format("Senha deve ter pelo menos %d caracteres", annotation.minLength()));
            isValid = false;
        }
        
        if (password.length() > annotation.maxLength()) {
            addViolation(context, String.format("Senha deve ter no máximo %d caracteres", annotation.maxLength()));
            isValid = false;
        }

        // 2. Verificar complexidade
        if (annotation.requireUppercase() && !UPPERCASE_PATTERN.matcher(password).find()) {
            addViolation(context, "Senha deve conter pelo menos uma letra maiúscula (A-Z)");
            isValid = false;
        }

        if (annotation.requireLowercase() && !LOWERCASE_PATTERN.matcher(password).find()) {
            addViolation(context, "Senha deve conter pelo menos uma letra minúscula (a-z)");
            isValid = false;
        }

        if (annotation.requireDigit() && !DIGIT_PATTERN.matcher(password).find()) {
            addViolation(context, "Senha deve conter pelo menos um dígito (0-9)");
            isValid = false;
        }

        if (annotation.requireSpecialChar() && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            addViolation(context, "Senha deve conter pelo menos um caractere especial (!@#$%^&*...)");
            isValid = false;
        }

        // 3. Verificar padrões inseguros
        if (annotation.disallowRepeatedChars() && REPEATED_CHARS_PATTERN.matcher(password).find()) {
            addViolation(context, "Senha não pode ter o mesmo caractere repetido 3 ou mais vezes consecutivas");
            isValid = false;
        }

        if (annotation.disallowSequences() && SEQUENTIAL_PATTERN.matcher(password).find()) {
            addViolation(context, "Senha não pode conter sequências como '123', 'abc' ou similares");
            isValid = false;
        }

        // 4. Verificar senhas comuns
        String lowerPassword = password.toLowerCase();
        for (String commonPassword : COMMON_PASSWORDS) {
            if (lowerPassword.equals(commonPassword) || lowerPassword.contains(commonPassword)) {
                addViolation(context, "Esta senha é muito comum e facilmente descoberta por atacantes");
                isValid = false;
                break;
            }
        }

        // 5. Verificar variações com números no final
        String passwordWithoutTrailingNumbers = lowerPassword.replaceAll("\\d+$", "");
        for (String commonPassword : COMMON_PASSWORDS) {
            if (passwordWithoutTrailingNumbers.equals(commonPassword)) {
                addViolation(context, "Adicionar números ao final de uma senha comum não a torna segura");
                isValid = false;
                break;
            }
        }

        // 6. Verificar diversidade de caracteres
        long uniqueChars = password.chars().distinct().count();
        if (uniqueChars < Math.min(8, password.length() * 0.6)) {
            addViolation(context, "Senha deve ter maior diversidade de caracteres");
            isValid = false;
        }

        // 7. Verificar padrões de teclado
        if (containsKeyboardPatterns(lowerPassword)) {
            addViolation(context, "Senha não pode conter padrões de teclado como 'qwerty', 'asdf'");
            isValid = false;
        }

        if (isValid) {
            log.debug("Senha validada com sucesso - força calculada");
        } else {
            log.debug("Senha rejeitada por não atender aos critérios de segurança");
        }

        return isValid;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }

    private boolean containsKeyboardPatterns(String password) {
        String[] keyboardPatterns = {
            "qwerty", "asdf", "zxcv", "qwer", "asdfg", "zxcvb",
            "1234", "4567", "7890", "147", "258", "369",
            "qwertz", "azerty" // Layouts de teclado internacionais
        };
        
        for (String pattern : keyboardPatterns) {
            if (password.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
