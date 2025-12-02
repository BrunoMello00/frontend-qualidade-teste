package com.tcc.estoque.service;

import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;

/*
  Teste de integração mínimo para changePassword.
  Requisitos:
  - profile 'test' usando H2 (adicionar src/test/resources/application-test.properties se necessário)
  - ajustar setters/getters de Usuario conforme sua entidade real
*/
@SpringBootTest(properties = "spring.profiles.active=test")
class PasswordServiceIntegrationTest {

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void changePassword_flow_persistsNewEncodedPassword() {
        Usuario u = new Usuario();
        u.setEmail("integ@local");
        String encoded = new BCryptPasswordEncoder().encode("oldPlain");
        u.setSenha(encoded);
        u.setAtivo(true);
        usuarioRepository.save(u);

        // Executa fluxo
        passwordService.changePassword("integ@local", "oldPlain", "newStronger1!");

        Usuario persisted = usuarioRepository.findByEmailAndAtivo("integ@local").orElseThrow();
        assertThat(persisted.getSenha()).isNotEqualTo(encoded);
        assertThat(new BCryptPasswordEncoder().matches("newStronger1!", persisted.getSenha())).isTrue();
    }
}
