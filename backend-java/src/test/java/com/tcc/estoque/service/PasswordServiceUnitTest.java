package com.tcc.estoque.service;

import com.tcc.estoque.dto.PasswordDTO;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para PasswordService.
 * - isolam UsuarioRepository e PasswordEncoder com Mockito
 * - cobrem validação, análise, geração e fluxo de troca de senha
 */
class PasswordServiceUnitTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordService service;

    @BeforeEach
    void setup() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new PasswordService(usuarioRepository, passwordEncoder);
    }

    @Test
    void validatePasswordStrength_nullOrEmpty_returnsInvalid() {
        PasswordDTO.PasswordValidationResponse resp = service.validatePasswordStrength(null, null);
        assertThat(resp).isNotNull();
        // getters/nomes podem variar conforme DTO; ajuste se necessário
        assertThat(resp.isValida()).isFalse();
        assertThat(resp.getPontuacao()).isEqualTo(0);
        assertThat(resp.getNivel()).isEqualTo("Inválida");
        assertThat(resp.getErros()).contains("Senha não pode ser vazia");
    }

    @Test
    void validatePasswordStrength_strongPassword_returnsMuitoForte() {
        String pwd = "Aa1!abcdefghijkl";
        PasswordDTO.PasswordValidationResponse resp = service.validatePasswordStrength(pwd, "user@example.com");

        assertThat(resp).isNotNull();
        assertThat(resp.isValida()).isTrue();
        assertThat(resp.getPontuacao()).isGreaterThanOrEqualTo(90);
        assertThat(resp.getNivel()).isEqualTo("Muito Forte");
        assertThat(resp.getErros()).isEmpty();
    }

    @Test
    void analyzePasswordStrength_flagsAndScore() {
        String pwd = "Ab1!cdef";
        PasswordDTO.PasswordStrengthResponse resp = service.analyzePasswordStrength(pwd);

        assertThat(resp).isNotNull();
        assertThat(resp.getPontuacao()).isGreaterThan(0);
        assertThat(resp.isTemMaiuscula()).isTrue();
        assertThat(resp.isTemMinuscula()).isTrue();
        assertThat(resp.isTemNumero()).isTrue();
        assertThat(resp.isTemCaractereEspecial()).isTrue();
        assertThat(resp.isComprimentoAdequado()).isTrue();
    }

    @Test
    void generateStrongPassword_lengthBoundsAndCharTypes() {
        String small = service.generateStrongPassword(4); // normalized to 8
        assertThat(small).hasSize(8);
        assertThat(small.chars().anyMatch(Character::isUpperCase)).isTrue();
        assertThat(small.chars().anyMatch(Character::isLowerCase)).isTrue();
        assertThat(small.chars().anyMatch(Character::isDigit)).isTrue();
        assertThat(small.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0)).isTrue();

        String large = service.generateStrongPassword(200);
        assertThat(large).hasSize(128); // capped at 128
    }

    @Test
    void changePassword_userNotFound_throws() {
        when(usuarioRepository.findByEmailAndAtivo("no@exist.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changePassword("no@exist.com", "a", "b"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    void changePassword_incorrectCurrent_throws() {
        Usuario usuario = mock(Usuario.class);
        when(usuarioRepository.findByEmailAndAtivo("u@e.com")).thenReturn(Optional.of(usuario));
        when(usuario.getSenha()).thenReturn("encodedCurrent");
        when(passwordEncoder.matches("wrong", "encodedCurrent")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword("u@e.com", "wrong", "newPass"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Senha atual incorreta");
    }

    @Test
    void changePassword_success_encodesAndSaves() {
        String email = "u@e.com";
        String current = "oldPass";
        String newPass = "newPassStrong1!";

        Usuario usuario = mock(Usuario.class);
        when(usuarioRepository.findByEmailAndAtivo(email)).thenReturn(Optional.of(usuario));
        when(usuario.getSenha()).thenReturn("encodedOld");
        when(passwordEncoder.matches(current, "encodedOld")).thenReturn(true);
        when(passwordEncoder.matches(newPass, "encodedOld")).thenReturn(false);
        when(passwordEncoder.encode(newPass)).thenReturn("encodedNew");

        service.changePassword(email, current, newPass);

        verify(usuario).setSenha("encodedNew");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void resetPassword_notImplemented_throws() {
        assertThatThrownBy(() -> service.resetPassword("anyCode", "newPassword"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Funcionalidade de reset por código não implementada");
    }
}
