package com.tcc.estoque.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Serviço para validação avançada de senhas
 * Implementa políticas de segurança rigorosas
 */
@Service
@Slf4j
public class AdvancedPasswordService {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+=\\[\\]{};':\"\\\\|,.<>\\/?~`-]");
    private static final Pattern REPEATED_CHARS_PATTERN = Pattern.compile("(.)\\1{2,}");
    private static final Pattern SEQUENTIAL_PATTERN = Pattern.compile("(012|123|234|345|456|567|678|789|890|abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)");

    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
        "password", "123456", "password123", "admin", "qwerty", "letmein", 
        "welcome", "monkey", "dragon", "password1", "123456789", "football",
        "iloveyou", "admin123", "welcome123", "password!", "123123123",
        "senha", "senha123", "administrador", "usuario", "12345678"
    ));

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;
    private static final int MIN_UNIQUE_CHARS = 8;

    /**
     * Valida uma senha contra todas as políticas de segurança
     */
    public PasswordValidationResult validatePassword(String password, String email) {
        PasswordValidationResult result = new PasswordValidationResult();
        
        if (password == null || password.isEmpty()) {
            result.addError("Senha não pode ser vazia");
            return result;
        }

        // 1. Verificar comprimento
        validateLength(password, result);
        
        // 2. Verificar complexidade
        validateComplexity(password, result);
        
        // 3. Verificar padrões inseguros
        validateSecurityPatterns(password, result);
        
        // 4. Verificar senhas comuns
        validateCommonPasswords(password, result);
        
        // 5. Verificar se contém informações pessoais
        validatePersonalInfo(password, email, result);
        
        // 6. Verificar diversidade de caracteres
        validateCharacterDiversity(password, result);
        
        result.setStrengthScore(calculateStrengthScore(password, result));
        
        log.debug("Password validation completed - score: {}, errors: {}", 
                result.getStrengthScore(), result.getErrors().size());
        
        return result;
    }

    private void validateLength(String password, PasswordValidationResult result) {
        if (password.length() < MIN_LENGTH) {
            result.addError(String.format("Senha deve ter pelo menos %d caracteres", MIN_LENGTH));
        }
        
        if (password.length() > MAX_LENGTH) {
            result.addError(String.format("Senha deve ter no máximo %d caracteres", MAX_LENGTH));
        }
    }

    private void validateComplexity(String password, PasswordValidationResult result) {
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            result.addError("Senha deve conter pelo menos uma letra maiúscula");
        }
        
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            result.addError("Senha deve conter pelo menos uma letra minúscula");
        }
        
        if (!DIGIT_PATTERN.matcher(password).find()) {
            result.addError("Senha deve conter pelo menos um dígito");
        }
        
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            result.addError("Senha deve conter pelo menos um caractere especial (!@#$%^&*...)");
        }
    }

    private void validateSecurityPatterns(String password, PasswordValidationResult result) {
        String lowerPassword = password.toLowerCase();
        
        if (REPEATED_CHARS_PATTERN.matcher(password).find()) {
            result.addWarning("Evite repetir o mesmo caractere 3 ou mais vezes seguidas");
        }
        
        if (SEQUENTIAL_PATTERN.matcher(lowerPassword).find()) {
            result.addWarning("Evite sequências como '123', 'abc' ou similares");
        }
        
        if (containsKeyboardPatterns(lowerPassword)) {
            result.addWarning("Evite padrões de teclado como 'qwerty', 'asdf'");
        }
    }

    private void validateCommonPasswords(String password, PasswordValidationResult result) {
        String lowerPassword = password.toLowerCase();
        
        if (COMMON_PASSWORDS.contains(lowerPassword)) {
            result.addError("Esta senha é muito comum e facilmente descoberta por atacantes");
        }
        
        String passwordWithoutTrailingNumbers = lowerPassword.replaceAll("\\d+$", "");
        if (COMMON_PASSWORDS.contains(passwordWithoutTrailingNumbers)) {
            result.addError("Adicionar números ao final de uma senha comum não a torna segura");
        }
    }

    private void validatePersonalInfo(String password, String email, PasswordValidationResult result) {
        if (email != null && !email.isEmpty()) {
            String username = email.split("@")[0].toLowerCase();
            String lowerPassword = password.toLowerCase();
            
            if (lowerPassword.contains(username)) {
                result.addError("Senha não deve conter seu nome de usuário ou email");
            }
        }
        
        if (password.matches(".*\\b(19|20)\\d{2}\\b.*")) {
            result.addWarning("Evite usar anos ou datas na senha");
        }
    }

    private void validateCharacterDiversity(String password, PasswordValidationResult result) {
        Set<Character> uniqueChars = new HashSet<>();
        for (char c : password.toCharArray()) {
            uniqueChars.add(c);
        }
        
        if (uniqueChars.size() < MIN_UNIQUE_CHARS) {
            result.addWarning(String.format("Use pelo menos %d caracteres únicos para maior segurança", MIN_UNIQUE_CHARS));
        }
    }

    private boolean containsKeyboardPatterns(String password) {
        String[] keyboardPatterns = {
            "qwerty", "asdf", "zxcv", "qwer", "asdfg", "zxcvb",
            "1234", "4567", "7890", "147", "258", "369"
        };
        
        for (String pattern : keyboardPatterns) {
            if (password.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private int calculateStrengthScore(String password, PasswordValidationResult result) {
        int score = 0;
        
        score += Math.min(25, password.length() * 2);
        
        if (UPPERCASE_PATTERN.matcher(password).find()) score += 10;
        if (LOWERCASE_PATTERN.matcher(password).find()) score += 10;
        if (DIGIT_PATTERN.matcher(password).find()) score += 10;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score += 15;
        
        Set<Character> uniqueChars = new HashSet<>();
        for (char c : password.toCharArray()) {
            uniqueChars.add(c);
        }
        score += Math.min(20, uniqueChars.size() * 2);
        
        score -= result.getErrors().size() * 20;
        score -= result.getWarnings().size() * 10;
        
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Verifica se a senha atingiu o nível mínimo de segurança
     */
    public boolean isPasswordSecure(String password, String email) {
        PasswordValidationResult result = validatePassword(password, email);
        return result.isValid() && result.getStrengthScore() >= 70;
    }

    /**
     * Gera sugestões para melhorar a senha
     */
    public String generatePasswordSuggestion() {
        String[] adjectives = {"Forte", "Segura", "Brilhante", "Rapida", "Solida"};
        String[] nouns = {"Muralha", "Chave", "Portal", "Torre", "Escudo"};
        String[] symbols = {"!", "@", "#", "$", "%", "&", "*"};
        
        int adjIndex = (int) (Math.random() * adjectives.length);
        int nounIndex = (int) (Math.random() * nouns.length);
        int symbolIndex = (int) (Math.random() * symbols.length);
        int number = (int) (Math.random() * 9999) + 1000;
        
        return adjectives[adjIndex] + nouns[nounIndex] + number + symbols[symbolIndex];
    }

    /**
     * Classe para resultado da validação
     */
    public static class PasswordValidationResult {
        private final Set<String> errors = new HashSet<>();
        private final Set<String> warnings = new HashSet<>();
        private int strengthScore = 0;

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public Set<String> getErrors() {
            return errors;
        }

        public Set<String> getWarnings() {
            return warnings;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public int getStrengthScore() {
            return strengthScore;
        }

        public void setStrengthScore(int strengthScore) {
            this.strengthScore = strengthScore;
        }

        public String getStrengthLevel() {
            if (strengthScore >= 90) return "Muito Forte";
            if (strengthScore >= 70) return "Forte";
            if (strengthScore >= 50) return "Moderada";
            if (strengthScore >= 30) return "Fraca";
            return "Muito Fraca";
        }
    }
}
