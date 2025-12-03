package com.tcc.estoque.service;

import com.tcc.estoque.dto.PasswordDTO;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Serviço para operações relacionadas a senhas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+=\\[\\]{};':\"\\\\|,.<>\\/?~`-]");
    
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    
    private final SecureRandom random = new SecureRandom();

    /**
     * Valida a força de uma senha
     */
    public PasswordDTO.PasswordValidationResponse validatePasswordStrength(String password, String email) {
        log.debug("Validando força da senha");
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        boolean isValid = true;
        int score = 0;
        
        if (password == null || password.trim().isEmpty()) {
            errors.add("Senha não pode ser vazia");
            return PasswordDTO.PasswordValidationResponse.builder()
                .valida(false)
                .pontuacao(0)
                .nivel("Inválida")
                .erros(errors.toArray(new String[0]))
                .avisos(new String[0])
                .sugestoes(new String[0])
                .build();
        }
        
        if (password.length() < 8) {
            errors.add("Senha deve ter pelo menos 8 caracteres");
            isValid = false;
        } else {
            score += Math.min(25, password.length() * 2);
        }
        
        boolean hasUpper = UPPERCASE_PATTERN.matcher(password).find();
        boolean hasLower = LOWERCASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).find();
        
        if (!hasUpper) {
            errors.add("Senha deve conter pelo menos uma letra maiúscula");
            isValid = false;
        } else {
            score += 15;
        }
        
        if (!hasLower) {
            errors.add("Senha deve conter pelo menos uma letra minúscula");
            isValid = false;
        } else {
            score += 15;
        }
        
        if (!hasDigit) {
            errors.add("Senha deve conter pelo menos um dígito");
            isValid = false;
        } else {
            score += 15;
        }
        
        if (!hasSpecial) {
            errors.add("Senha deve conter pelo menos um caractere especial");
            isValid = false;
        } else {
            score += 20;
        }
        
        if (password.matches(".*(.)\\1{2,}.*")) {
            errors.add("Senha não pode ter caracteres repetidos consecutivos");
            isValid = false;
        }
        
        String lowerPassword = password.toLowerCase();
        String[] commonPasswords = {"password", "123456", "senha", "admin", "qwerty"};
        for (String common : commonPasswords) {
            if (lowerPassword.contains(common)) {
                errors.add("Senha contém padrões muito comuns");
                isValid = false;
                break;
            }
        }
        
        if (email != null && !email.isEmpty()) {
            String username = email.split("@")[0].toLowerCase();
            if (lowerPassword.contains(username)) {
                errors.add("Senha não deve conter informações pessoais");
                isValid = false;
            }
        }
        
        String level;
        if (score >= 90) level = "Muito Forte";
        else if (score >= 70) level = "Forte";
        else if (score >= 50) level = "Moderada";
        else if (score >= 30) level = "Fraca";
        else level = "Muito Fraca";
        
        if (!isValid || score < 70) {
            suggestions.add("Use uma combinação de letras maiúsculas e minúsculas");
            suggestions.add("Inclua números e caracteres especiais");
            suggestions.add("Evite informações pessoais óbvias");
            suggestions.add("Use pelo menos 12 caracteres para maior segurança");
        }
        
        return PasswordDTO.PasswordValidationResponse.builder()
            .valida(isValid)
            .pontuacao(Math.max(0, Math.min(100, score)))
            .nivel(level)
            .erros(errors.toArray(new String[0]))
            .avisos(warnings.toArray(new String[0]))
            .sugestoes(suggestions.toArray(new String[0]))
            .build();
    }

    /**
     * Analisa detalhadamente a força da senha
     */
    public PasswordDTO.PasswordStrengthResponse analyzePasswordStrength(String password) {
        log.debug("Analisando força da senha detalhadamente");
        
        boolean hasUpper = UPPERCASE_PATTERN.matcher(password).find();
        boolean hasLower = LOWERCASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).find();
        boolean adequateLength = password.length() >= 8;
        boolean noUnsafePatterns = !password.matches(".*(.)\\1{2,}.*");
        
        int score = 0;
        if (adequateLength) score += 20;
        if (hasUpper) score += 15;
        if (hasLower) score += 15;
        if (hasDigit) score += 15;
        if (hasSpecial) score += 20;
        if (noUnsafePatterns) score += 15;
        
        String level;
        if (score >= 90) level = "Muito Forte";
        else if (score >= 70) level = "Forte";
        else if (score >= 50) level = "Moderada";
        else if (score >= 30) level = "Fraca";
        else level = "Muito Fraca";
        
        List<String> improvements = new ArrayList<>();
        if (!adequateLength) improvements.add("Aumente para pelo menos 8 caracteres");
        if (!hasUpper) improvements.add("Adicione letras maiúsculas");
        if (!hasLower) improvements.add("Adicione letras minúsculas");
        if (!hasDigit) improvements.add("Adicione números");
        if (!hasSpecial) improvements.add("Adicione caracteres especiais");
        if (!noUnsafePatterns) improvements.add("Evite caracteres repetidos");
        
        return PasswordDTO.PasswordStrengthResponse.builder()
            .pontuacao(score)
            .nivel(level)
            .temMaiuscula(hasUpper)
            .temMinuscula(hasLower)
            .temNumero(hasDigit)
            .temCaractereEspecial(hasSpecial)
            .comprimentoAdequado(adequateLength)
            .semPadroesInseguros(noUnsafePatterns)
            .sugestoesMelhoria(improvements.toArray(new String[0]))
            .build();
    }

    /**
     * Gera uma senha forte aleatória
     */
    public String generateStrongPassword(int length) {
        log.debug("Gerando senha forte com {} caracteres", length);
        
        if (length < 8) length = 8;
        if (length > 128) length = 128;
        
        StringBuilder password = new StringBuilder();
        String allChars = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
        
        password.append(UPPERCASE_CHARS.charAt(random.nextInt(UPPERCASE_CHARS.length())));
        password.append(LOWERCASE_CHARS.charAt(random.nextInt(LOWERCASE_CHARS.length())));
        password.append(DIGIT_CHARS.charAt(random.nextInt(DIGIT_CHARS.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));
        
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(length);
            char temp = password.charAt(i);
            password.setCharAt(i, password.charAt(randomIndex));
            password.setCharAt(randomIndex, temp);
        }
        
        return password.toString();
    }

    /**
     * Altera a senha do usuário
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        log.info("Alterando senha para usuário: {}", email);
        
        Usuario usuario = usuarioRepository.findByEmailAndAtivo(email)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!passwordEncoder.matches(currentPassword, usuario.getSenha())) {
            throw new RuntimeException("Senha atual incorreta");
        }
        
        if (passwordEncoder.matches(newPassword, usuario.getSenha())) {
            throw new RuntimeException("A nova senha deve ser diferente da senha atual");
        }
        
        usuario.setSenha(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
        
        log.info("Senha alterada com sucesso para usuário: {}", email);
    }

    /**
     * Redefine senha usando código de recuperação
     * Implementação simplificada - em produção usaria tabela específica
     */
    @Transactional
    public void resetPassword(String codigo, String newPassword) {
        log.info("Redefinindo senha com código: {}", codigo);
        
        throw new RuntimeException("Funcionalidade de reset por código não implementada. Use a alteração de senha logado.");
    }
}
