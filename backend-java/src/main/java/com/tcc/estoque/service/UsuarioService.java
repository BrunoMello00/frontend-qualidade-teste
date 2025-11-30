package com.tcc.estoque.service;

import com.tcc.estoque.dto.UsuarioDTO;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.TipoUsuario;
import com.tcc.estoque.repository.UsuarioRepository;
import com.tcc.estoque.exception.BusinessException;
import com.tcc.estoque.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    return new UsernameNotFoundException("Usuário não encontrado: " + email);
                });
        return usuario;
    }

    public UsuarioDTO.UsuarioResponse criarUsuario(UsuarioDTO.UsuarioRequest request) {
        validarEmailUnico(request.getEmail(), null);
        
        if (request.getCpf() != null && !request.getCpf().trim().isEmpty()) {
            validarCpfUnico(request.getCpf(), null);
        }

        Usuario usuario = mapearParaEntidade(request);
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setDataCadastro(LocalDateTime.now());
        usuario.setAtivo(true);
        usuario.setBloqueado(false);
        usuario.setTentativasLogin(0);

        usuario = usuarioRepository.save(usuario);
        return mapearParaResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioDTO.UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);
        return mapearParaResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioDTO.UsuarioResponse buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com email: " + email));
        return mapearParaResponse(usuario);
    }

    public UsuarioDTO.UsuarioResponse atualizarUsuario(Long id, UsuarioDTO.UsuarioRequest request) {
        Usuario usuario = buscarUsuarioPorId(id);

        validarEmailUnico(request.getEmail(), id);
        if (request.getCpf() != null && !request.getCpf().trim().isEmpty()) {
            validarCpfUnico(request.getCpf(), id);
        }

        atualizarDadosUsuario(usuario, request);
        usuario = usuarioRepository.save(usuario);
        return mapearParaResponse(usuario);
    }

    public void deletarUsuario(Long id, Usuario usuarioLogado) {
        Usuario usuarioAlvo = buscarUsuarioPorId(id);
        
        if (!usuarioLogado.podeExcluirUsuario(usuarioAlvo.getTipoUsuario())) {
            throw new BusinessException("Você não tem permissão para excluir este usuário");
        }
        
        if (usuarioLogado.getId().equals(id)) {
            throw new BusinessException("Não é possível excluir o próprio usuário");
        }
        
        if (usuarioAlvo.isOwner()) {
            long ownersAtivos = usuarioRepository.countByTipoUsuarioAndAtivoTrue(TipoUsuario.OWNER);
            if (ownersAtivos <= 1) {
                throw new BusinessException("Não é possível desativar o último proprietário do sistema");
            }
        }
        
        if (usuarioAlvo.isAdmin() && !usuarioLogado.isOwner()) {
            long adminsAtivos = usuarioRepository.countByTipoUsuarioAndAtivoTrue(TipoUsuario.ADMIN);
            if (adminsAtivos <= 1) {
                throw new BusinessException("Não é possível desativar o último administrador do sistema");
            }
        }
        
        usuarioAlvo.setAtivo(false);
        usuarioRepository.save(usuarioAlvo);
    }

    public void reativarUsuario(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);
        usuario.setAtivo(true);
        usuario.setBloqueado(false);
        usuario.setTentativasLogin(0);
        usuarioRepository.save(usuario);
    }

    public void alterarSenha(Long id, UsuarioDTO.AlterarSenhaRequest request) {
        Usuario usuario = buscarUsuarioPorId(id);

        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenha())) {
            throw new BusinessException("Senha atual não confere");
        }

        if (!request.getNovaSenha().equals(request.getConfirmacaoSenha())) {
            throw new BusinessException("Nova senha e confirmação não conferem");
        }

        usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);
    }

    public void resetarSenha(Long id, String novaSenha) {
        Usuario usuario = buscarUsuarioPorId(id);
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setTentativasLogin(0);
        usuario.setBloqueado(false);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioDTO.UsuarioResumo> listarUsuarios(UsuarioDTO.FiltroUsuarios filtro, Pageable pageable) {
        Sort sort = criarOrdenacao(filtro.getOrderBy(), filtro.getOrderDirection());
        Pageable pageableComSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Usuario> usuarios = usuarioRepository.findComFiltros(
                filtro.getTermo(),
                filtro.getTipoUsuario(),
                filtro.getAtivo(),
                pageableComSort
        );

        return usuarios.map(this::mapearParaResumo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO.UsuarioResumo> buscarPorTermo(String termo) {
        Page<Usuario> usuarios = usuarioRepository.findByTermoGeral(termo, PageRequest.of(0, 50));
        return usuarios.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO.PermissaoResponse> obterPermissoesUsuario(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);
        return usuario.getTipoUsuario().getPermissoes().stream()
                .map(UsuarioDTO.PermissaoResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean usuarioTemPermissao(Long id, String permissao) {
        Usuario usuario = buscarUsuarioPorId(id);
        return usuario.temPermissao(permissao);
    }

    @Transactional(readOnly = true)
    public List<String> obterPermissoesModulo(Long id, String modulo) {
        Usuario usuario = buscarUsuarioPorId(id);
        return usuario.getPermissoesModulo(modulo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO.TipoUsuarioInfo> obterTiposUsuario() {
        return List.of(TipoUsuario.values()).stream()
                .map(this::mapearTipoUsuarioInfo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO.UsuarioResumo> buscarPorTipo(TipoUsuario tipo) {
        List<Usuario> usuarios = usuarioRepository.findByTipoUsuarioAndAtivoTrue(tipo);
        return usuarios.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Object[]> obterEstatisticasPorTipo() {
        return usuarioRepository.contarUsuariosPorTipo();
    }

    @Transactional(readOnly = true)
    public long contarUsuariosAtivos() {
        return usuarioRepository.countByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public long contarTotalUsuarios() {
        return usuarioRepository.count();
    }

    @Transactional(readOnly = true)
    public long contarUsuariosBloqueados() {
        return usuarioRepository.countByBloqueadoTrue();
    }

    @Transactional(readOnly = true)
    public long contarNovosCadastrosUltimos30Dias() {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(30);
        return usuarioRepository.countByDataCadastroAfter(dataLimite);
    }

    @Transactional(readOnly = true)
    public long contarPorTipo(TipoUsuario tipo) {
        return usuarioRepository.countByTipoUsuarioAndAtivoTrue(tipo);
    }

    public void bloquearUsuario(Long id, String motivo) {
        Usuario usuario = buscarUsuarioPorId(id);
        if (usuario.isAdmin()) {
            long adminsAtivos = usuarioRepository.countByTipoUsuarioAndAtivoTrue(TipoUsuario.ADMIN);
            if (adminsAtivos <= 1) {
                throw new BusinessException("Não é possível bloquear o último administrador do sistema");
            }
        }
        usuario.setBloqueado(true);
        usuario.setDataBloqueio(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    public void desbloquearUsuario(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);
        usuario.setBloqueado(false);
        usuario.setDataBloqueio(null);
        usuario.setTentativasLogin(0);
        usuarioRepository.save(usuario);
    }

    public void registrarAcesso(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);
        usuario.setUltimoAcesso(LocalDateTime.now());
        usuario.setTentativasLogin(0);
        usuarioRepository.save(usuario);
    }

    public void incrementarTentativasLogin(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario != null) {
            usuario.setTentativasLogin(usuario.getTentativasLogin() + 1);
            if (usuario.getTentativasLogin() >= 5) {
                usuario.setBloqueado(true);
                usuario.setDataBloqueio(LocalDateTime.now());
            }
            usuarioRepository.save(usuario);
        }
    }

    private Usuario buscarUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + id));
    }

    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + email));
    }

    public Usuario findById(Long id) {
        return buscarUsuarioPorId(id);
    }

    private void validarEmailUnico(String email, Long idExcluir) {
        boolean emailExiste = (idExcluir == null) ?
                usuarioRepository.existsByEmail(email) :
                usuarioRepository.existsByEmailAndIdNot(email, idExcluir);

        if (emailExiste) {
            throw new BusinessException("Email já cadastrado: " + email);
        }
    }

    private void validarCpfUnico(String cpf, Long idExcluir) {
        boolean cpfExiste = (idExcluir == null) ?
                usuarioRepository.existsByCpf(cpf) :
                usuarioRepository.existsByCpfAndIdNot(cpf, idExcluir);

        if (cpfExiste) {
            throw new BusinessException("CPF já cadastrado: " + cpf);
        }
    }

    private Sort criarOrdenacao(String orderBy, String orderDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(orderDirection) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;

        switch (orderBy.toLowerCase()) {
            case "nome": return Sort.by(direction, "nome");
            case "email": return Sort.by(direction, "email");
            case "tipousuario": return Sort.by(direction, "tipoUsuario");
            case "ultimoacesso": return Sort.by(direction, "ultimoAcesso");
            default: return Sort.by(direction, "dataCadastro");
        }
    }

    private Usuario mapearParaEntidade(UsuarioDTO.UsuarioRequest request) {
        Usuario usuario = new Usuario();
        atualizarDadosUsuario(usuario, request);
        return usuario;
    }

    private void atualizarDadosUsuario(Usuario usuario, UsuarioDTO.UsuarioRequest request) {
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());
        usuario.setCpf(request.getCpf());
        usuario.setTipoUsuario(request.getTipoUsuario());
        if (request.getAtivo() != null) {
            usuario.setAtivo(request.getAtivo());
        }
    }

    private UsuarioDTO.UsuarioResponse mapearParaResponse(Usuario usuario) {
        UsuarioDTO.UsuarioResponse response = new UsuarioDTO.UsuarioResponse();
        response.setId(usuario.getId());
        response.setNome(usuario.getNome());
        response.setEmail(usuario.getEmail());
        response.setTelefone(usuario.getTelefone());
        response.setCpf(usuario.getCpf());
        response.setTipoUsuario(usuario.getTipoUsuario());
        response.setTipoDescricao(usuario.getTipoUsuario().getDescricao());
        response.setTipoDetalhe(usuario.getTipoUsuario().getDetalhe());
        response.setTipoCor(usuario.getTipoUsuario().getCor());
        response.setAtivo(usuario.getAtivo());
        response.setDataCadastro(usuario.getDataCadastro());
        response.setUltimoAcesso(usuario.getUltimoAcesso());
        response.setTentativasLogin(usuario.getTentativasLogin());
        response.setBloqueado(usuario.getBloqueado());
        response.setDataBloqueio(usuario.getDataBloqueio());
        response.setPermissoes(usuario.getTipoUsuario().getPermissoes());
        return response;
    }

    private UsuarioDTO.UsuarioResumo mapearParaResumo(Usuario usuario) {
        UsuarioDTO.UsuarioResumo resumo = new UsuarioDTO.UsuarioResumo();
        resumo.setId(usuario.getId());
        resumo.setNome(usuario.getNome());
        resumo.setEmail(usuario.getEmail());
        resumo.setTipoUsuario(usuario.getTipoUsuario());
        resumo.setTipoDescricao(usuario.getTipoUsuario().getDescricao());
        resumo.setTipoCor(usuario.getTipoUsuario().getCor());
        resumo.setAtivo(usuario.getAtivo());
        resumo.setUltimoAcesso(usuario.getUltimoAcesso());
        return resumo;
    }

    private UsuarioDTO.TipoUsuarioInfo mapearTipoUsuarioInfo(TipoUsuario tipo) {
        UsuarioDTO.TipoUsuarioInfo info = new UsuarioDTO.TipoUsuarioInfo();
        info.setTipo(tipo);
        info.setDescricao(tipo.getDescricao());
        info.setDetalhe(tipo.getDetalhe());
        info.setCor(tipo.getCor());
        info.setPermissoes(
                tipo.getPermissoes().stream()
                        .map(UsuarioDTO.PermissaoResponse::new)
                        .collect(Collectors.toList())
        );
        return info;
    }
}
