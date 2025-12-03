package com.tcc.estoque.service;

import com.tcc.estoque.dto.UsuarioDTO;
import com.tcc.estoque.exception.BusinessException;
import com.tcc.estoque.exception.NotFoundException;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.TipoUsuario;
import com.tcc.estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    void testBuscarPorId() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Teste");
        usuario.setEmail("teste@email.com");
        usuario.setTipoUsuario(TipoUsuario.VENDEDOR);
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioDTO.UsuarioResponse response = usuarioService.buscarPorId(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Teste");
        verify(usuarioRepository).findById(1L);
    }

    @Test
    void testBuscarPorIdNaoEncontrado() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> usuarioService.buscarPorId(999L));
        verify(usuarioRepository).findById(999L);
    }

    @Test
    void testBuscarPorEmail() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("teste@email.com");
        usuario.setTipoUsuario(TipoUsuario.VENDEDOR);
        
        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));

        UsuarioDTO.UsuarioResponse response = usuarioService.buscarPorEmail("teste@email.com");

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("teste@email.com");
        verify(usuarioRepository).findByEmail("teste@email.com");
    }

    @Test
    void testAtualizarUsuario() {
        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setId(1L);
        usuarioExistente.setNome("Nome Antigo");
        usuarioExistente.setEmail("antigo@email.com");
        usuarioExistente.setTipoUsuario(TipoUsuario.VENDEDOR);

        UsuarioDTO.UsuarioRequest request = new UsuarioDTO.UsuarioRequest();
        request.setNome("Nome Novo");
        request.setEmail("novo@email.com");
        request.setTelefone("11999999999");
        request.setTipoUsuario(TipoUsuario.VENDEDOR);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.existsByEmailAndIdNot("novo@email.com", 1L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO.UsuarioResponse response = usuarioService.atualizarUsuario(1L, request);

        assertThat(response).isNotNull();
        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository).existsByEmailAndIdNot("novo@email.com", 1L);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void testAlterarSenha() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setSenha("senhaAntiga");
        
        UsuarioDTO.AlterarSenhaRequest request = new UsuarioDTO.AlterarSenhaRequest();
        request.setSenhaAtual("senhaAtual");
        request.setNovaSenha("novaSenha");
        request.setConfirmacaoSenha("novaSenha");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaAtual", "senhaAntiga")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha")).thenReturn("novaSenhaEncoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.alterarSenha(1L, request);

        verify(passwordEncoder).matches("senhaAtual", "senhaAntiga");
        verify(passwordEncoder).encode("novaSenha");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void testAlterarSenhaSenhaAtualIncorreta() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setSenha("senhaAntiga");
        
        UsuarioDTO.AlterarSenhaRequest request = new UsuarioDTO.AlterarSenhaRequest();
        request.setSenhaAtual("senhaErrada");
        request.setNovaSenha("novaSenha");
        request.setConfirmacaoSenha("novaSenha");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "senhaAntiga")).thenReturn(false);

        assertThrows(BusinessException.class, () -> usuarioService.alterarSenha(1L, request));
        
        verify(passwordEncoder).matches("senhaErrada", "senhaAntiga");
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void testAlterarSenhaConfirmacaoNaoConfere() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setSenha("senhaAntiga");
        
        UsuarioDTO.AlterarSenhaRequest request = new UsuarioDTO.AlterarSenhaRequest();
        request.setSenhaAtual("senhaAtual");
        request.setNovaSenha("novaSenha");
        request.setConfirmacaoSenha("senhaConfirmacaoDiferente");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaAtual", "senhaAntiga")).thenReturn(true);

        assertThrows(BusinessException.class, () -> usuarioService.alterarSenha(1L, request));
        
        verify(passwordEncoder).matches("senhaAtual", "senhaAntiga");
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void testResetarSenha() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTentativasLogin(5);
        usuario.setBloqueado(true);
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha")).thenReturn("novaSenhaEncoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.resetarSenha(1L, "novaSenha");

        verify(passwordEncoder).encode("novaSenha");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void testReativarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setAtivo(false);
        usuario.setBloqueado(true);
        usuario.setTentativasLogin(3);
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.reativarUsuario(1L);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario saved = captor.getValue();
        assertThat(saved.getAtivo()).isTrue();
        assertThat(saved.getBloqueado()).isFalse();
        assertThat(saved.getTentativasLogin()).isEqualTo(0);
    }

    @Test
    void testListarUsuarios() {
        UsuarioDTO.FiltroUsuarios filtro = new UsuarioDTO.FiltroUsuarios();
        filtro.setOrderBy("nome");
        filtro.setOrderDirection("ASC");
        
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Teste");
        usuario.setEmail("teste@email.com");
        usuario.setTipoUsuario(TipoUsuario.VENDEDOR);
        
        Page<Usuario> pageUsuarios = new PageImpl<>(Collections.singletonList(usuario));
        Pageable pageable = PageRequest.of(0, 10);
        
        when(usuarioRepository.findComFiltros(any(), any(), any(), any(Pageable.class))).thenReturn(pageUsuarios);

        Page<UsuarioDTO.UsuarioResumo> result = usuarioService.listarUsuarios(filtro, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(usuarioRepository).findComFiltros(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void testBuscarPorTermo() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Usuario Teste");
        usuario.setEmail("teste@email.com");
        usuario.setTipoUsuario(TipoUsuario.VENDEDOR);
        
        Page<Usuario> pageUsuarios = new PageImpl<>(Collections.singletonList(usuario));
        
        when(usuarioRepository.findByTermoGeral(eq("teste"), any(PageRequest.class))).thenReturn(pageUsuarios);

        List<UsuarioDTO.UsuarioResumo> result = usuarioService.buscarPorTermo("teste");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNome()).isEqualTo("Usuario Teste");
        verify(usuarioRepository).findByTermoGeral(eq("teste"), any(PageRequest.class));
    }

    @Test
    void testObterPermissoesUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(TipoUsuario.ADMIN);
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        List<UsuarioDTO.PermissaoResponse> result = usuarioService.obterPermissoesUsuario(1L);

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
        verify(usuarioRepository).findById(1L);
    }

    @Test
    void testUsuarioTemPermissao() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.temPermissao("READ_PRODUCT")).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        boolean result = usuarioService.usuarioTemPermissao(1L, "READ_PRODUCT");

        assertThat(result).isTrue();
        verify(usuarioRepository).findById(1L);
        verify(usuario).temPermissao("READ_PRODUCT");
    }

    @Test
    void testObterPermissoesModulo() {
        Usuario usuario = mock(Usuario.class);
        List<String> permissoes = Arrays.asList("READ_PRODUCT", "WRITE_PRODUCT");
        when(usuario.getPermissoesModulo("PRODUCT")).thenReturn(permissoes);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        List<String> result = usuarioService.obterPermissoesModulo(1L, "PRODUCT");

        assertThat(result).hasSize(2);
        assertThat(result).contains("READ_PRODUCT", "WRITE_PRODUCT");
        verify(usuarioRepository).findById(1L);
        verify(usuario).getPermissoesModulo("PRODUCT");
    }

    @Test
    void testObterTiposUsuario() {
        List<UsuarioDTO.TipoUsuarioInfo> result = usuarioService.obterTiposUsuario();

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(TipoUsuario.values().length);
    }

    @Test
    void testBuscarPorTipo() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(TipoUsuario.VENDEDOR);
        
        when(usuarioRepository.findByTipoUsuarioAndAtivoTrue(TipoUsuario.VENDEDOR))
            .thenReturn(Collections.singletonList(usuario));

        List<UsuarioDTO.UsuarioResumo> result = usuarioService.buscarPorTipo(TipoUsuario.VENDEDOR);

        assertThat(result).hasSize(1);
        verify(usuarioRepository).findByTipoUsuarioAndAtivoTrue(TipoUsuario.VENDEDOR);
    }

    @Test
    void testObterEstatisticasPorTipo() {
        Object[] estatistica = new Object[]{"ADMIN", 5L};
        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.singletonList(estatistica));

        List<Object[]> result = usuarioService.obterEstatisticasPorTipo();

        assertThat(result).hasSize(1);
        verify(usuarioRepository).contarUsuariosPorTipo();
    }

    @Test
    void testContarUsuariosAtivos() {
        when(usuarioRepository.countByAtivoTrue()).thenReturn(10L);

        long result = usuarioService.contarUsuariosAtivos();

        assertThat(result).isEqualTo(10L);
        verify(usuarioRepository).countByAtivoTrue();
    }

    @Test
    void testContarTotalUsuarios() {
        when(usuarioRepository.count()).thenReturn(25L);

        long result = usuarioService.contarTotalUsuarios();

        assertThat(result).isEqualTo(25L);
        verify(usuarioRepository).count();
    }

    @Test
    void testContarUsuariosBloqueados() {
        when(usuarioRepository.countByBloqueadoTrue()).thenReturn(3L);

        long result = usuarioService.contarUsuariosBloqueados();

        assertThat(result).isEqualTo(3L);
        verify(usuarioRepository).countByBloqueadoTrue();
    }

    @Test
    void testContarNovosCadastrosUltimos30Dias() {
        when(usuarioRepository.countByDataCadastroAfter(any(LocalDateTime.class))).thenReturn(7L);

        long result = usuarioService.contarNovosCadastrosUltimos30Dias();

        assertThat(result).isEqualTo(7L);
        verify(usuarioRepository).countByDataCadastroAfter(any(LocalDateTime.class));
    }

    @Test
    void testContarPorTipo() {
        when(usuarioRepository.countByTipoUsuarioAndAtivoTrue(TipoUsuario.ADMIN)).thenReturn(2L);

        long result = usuarioService.contarPorTipo(TipoUsuario.ADMIN);

        assertThat(result).isEqualTo(2L);
        verify(usuarioRepository).countByTipoUsuarioAndAtivoTrue(TipoUsuario.ADMIN);
    }

    @Test
    void testBloquearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(TipoUsuario.VENDEDOR);
        usuario.setBloqueado(false);
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.bloquearUsuario(1L, "Motivo teste");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario saved = captor.getValue();
        assertThat(saved.getBloqueado()).isTrue();
        assertThat(saved.getDataBloqueio()).isNotNull();
    }

    @Test
    void testBloquearUltimoAdmin() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(TipoUsuario.ADMIN);
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.countByTipoUsuarioAndAtivoTrue(TipoUsuario.ADMIN)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> usuarioService.bloquearUsuario(1L, "Motivo"));
        
        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository).countByTipoUsuarioAndAtivoTrue(TipoUsuario.ADMIN);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void testDesbloquearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setBloqueado(true);
        usuario.setDataBloqueio(LocalDateTime.now());
        usuario.setTentativasLogin(3);
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.desbloquearUsuario(1L);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario saved = captor.getValue();
        assertThat(saved.getBloqueado()).isFalse();
        assertThat(saved.getDataBloqueio()).isNull();
        assertThat(saved.getTentativasLogin()).isEqualTo(0);
    }

    @Test
    void testRegistrarAcesso() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTentativasLogin(2);
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.registrarAcesso(1L);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario saved = captor.getValue();
        assertThat(saved.getUltimoAcesso()).isNotNull();
        assertThat(saved.getTentativasLogin()).isEqualTo(0);
    }

    @Test
    void testIncrementarTentativasLogin() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("teste@email.com");
        usuario.setTentativasLogin(2);
        usuario.setBloqueado(false);
        
        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.incrementarTentativasLogin("teste@email.com");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario saved = captor.getValue();
        assertThat(saved.getTentativasLogin()).isEqualTo(3);
    }

    @Test
    void testIncrementarTentativasLoginBloquear() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("teste@email.com");
        usuario.setTentativasLogin(4);
        usuario.setBloqueado(false);
        
        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.incrementarTentativasLogin("teste@email.com");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario saved = captor.getValue();
        assertThat(saved.getTentativasLogin()).isEqualTo(5);
        assertThat(saved.getBloqueado()).isTrue();
        assertThat(saved.getDataBloqueio()).isNotNull();
    }

    @Test
    void testIncrementarTentativasLoginUsuarioNaoEncontrado() {
        when(usuarioRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

        usuarioService.incrementarTentativasLogin("inexistente@email.com");

        verify(usuarioRepository).findByEmail("inexistente@email.com");
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void testFindByEmail() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("teste@email.com");
        
        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));

        Usuario result = usuarioService.findByEmail("teste@email.com");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(usuarioRepository).findByEmail("teste@email.com");
    }

    @Test
    void testFindByEmailNaoEncontrado() {
        when(usuarioRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> usuarioService.findByEmail("inexistente@email.com"));
        verify(usuarioRepository).findByEmail("inexistente@email.com");
    }

    @Test
    void testFindById() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Usuario result = usuarioService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(usuarioRepository).findById(1L);
    }
}


