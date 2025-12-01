package com.tcc.estoque.service;

import com.tcc.estoque.dto.UsuarioDTO;
import com.tcc.estoque.exception.BusinessException;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.TipoUsuario;
import com.tcc.estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {

    @InjectMocks
    private UsuarioService usuarioService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void loadUserByUsernameFound() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(usuario));

        var result = usuarioService.loadUserByUsername("a@b.com");

        assertThat(result).isSameAs(usuario);
        verify(usuarioRepository).findByEmail("a@b.com");
    }

    @Test
    void loadUserByUsernameNotFound() {
        when(usuarioRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> usuarioService.loadUserByUsername("x@x.com"));
        verify(usuarioRepository).findByEmail("x@x.com");
    }

    @Test
    void criarUsuarioHappyPathSemCpf() {
        UsuarioDTO.UsuarioRequest req = mock(UsuarioDTO.UsuarioRequest.class);
    when(req.getEmail()).thenReturn("novo@e.com");
    when(req.getCpf()).thenReturn(null);
    when(req.getSenha()).thenReturn("plain");
    when(req.getTipoUsuario()).thenReturn(TipoUsuario.VENDEDOR);

        when(usuarioRepository.existsByEmail("novo@e.com")).thenReturn(false);
        when(passwordEncoder.encode("plain")).thenReturn("encoded");

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });

        var resp = usuarioService.criarUsuario(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(100L);
        verify(passwordEncoder).encode("plain");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void criarUsuarioComCpf() {
        UsuarioDTO.UsuarioRequest req = mock(UsuarioDTO.UsuarioRequest.class);
    when(req.getEmail()).thenReturn("c@c.com");
    when(req.getCpf()).thenReturn("123.456.789-00");
    when(req.getSenha()).thenReturn("pwd");
    when(req.getTipoUsuario()).thenReturn(TipoUsuario.VENDEDOR);

        when(usuarioRepository.existsByEmail("c@c.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("123.456.789-00")).thenReturn(false);
        when(passwordEncoder.encode("pwd")).thenReturn("enc");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.criarUsuario(req);

        verify(usuarioRepository).existsByCpf("123.456.789-00");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void deletarUsuarioPermissaoNegada() {
        Usuario usuarioAlvo = new Usuario();
        usuarioAlvo.setId(2L);
        usuarioAlvo.setTipoUsuario(TipoUsuario.VENDEDOR);

        Usuario usuarioLogado = mock(Usuario.class);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAlvo));
        when(usuarioLogado.podeExcluirUsuario(usuarioAlvo.getTipoUsuario())).thenReturn(false);

        assertThrows(BusinessException.class, () -> usuarioService.deletarUsuario(2L, usuarioLogado));
        verify(usuarioRepository).findById(2L);
    }

    @Test
    void deletarUsuarioSucesso() {
        Usuario usuarioAlvo = new Usuario();
        usuarioAlvo.setId(2L);
        usuarioAlvo.setTipoUsuario(TipoUsuario.VENDEDOR);
        usuarioAlvo.setAtivo(true);

        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(1L);
        usuarioLogado.setTipoUsuario(TipoUsuario.OWNER); 

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAlvo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.deletarUsuario(2L, usuarioLogado);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario saved = captor.getValue();
        assertThat(saved.getAtivo()).isFalse();
    }
}


