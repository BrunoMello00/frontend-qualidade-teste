package com.tcc.estoque.config;

import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.UsuarioRepository;
import com.tcc.estoque.security.SecurityUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityUtil securityUtil(UsuarioRepository usuarioRepository) {
        return new SecurityUtil(usuarioRepository) {
            @Override
            public Usuario getUsuarioLogado() {
                return usuarioRepository.findByEmail("test@local")
                        .orElseGet(() -> {
                            Usuario u = new Usuario();
                            u.setEmail("test@local");
                            u.setNome("Test User");
                            u.setSenha("test");
                            u.setAtivo(true);
                            return usuarioRepository.save(u);
                        });
            }

            @Override
            public String getEmailUsuarioLogado() {
                return "test@local";
            }

            @Override
            public boolean isUsuarioAutenticado() {
                return true;
            }
        };
    }
}

