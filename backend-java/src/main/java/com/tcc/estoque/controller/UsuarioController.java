package com.tcc.estoque.controller;

import com.tcc.estoque.dto.UsuarioDTO;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.TipoUsuario;
import com.tcc.estoque.service.UsuarioService;
import com.tcc.estoque.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')") // OWNER e ADMIN têm acesso a gestão de usuários
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping
    
    public ResponseEntity<UsuarioDTO.UsuarioResponse> criarUsuario(@Valid @RequestBody UsuarioDTO.UsuarioRequest request) {
        UsuarioDTO.UsuarioResponse response = usuarioService.criarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    
    public ResponseEntity<UsuarioDTO.UsuarioResponse> buscarPorId(@PathVariable Long id) {
        UsuarioDTO.UsuarioResponse response = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    
    public ResponseEntity<UsuarioDTO.UsuarioResponse> atualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioDTO.UsuarioRequest request) {
        UsuarioDTO.UsuarioResponse response = usuarioService.atualizarUsuario(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        Usuario usuarioLogado = securityUtil.getUsuarioLogado();
        usuarioService.deletarUsuario(id, usuarioLogado);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reativar")
    
    public ResponseEntity<Void> reativarUsuario(@PathVariable Long id) {
        usuarioService.reativarUsuario(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    
    public ResponseEntity<Page<UsuarioDTO.UsuarioResumo>> listarUsuarios(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) TipoUsuario tipoUsuario,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "nome") String orderBy,
            @RequestParam(defaultValue = "ASC") String orderDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UsuarioDTO.FiltroUsuarios filtro = new UsuarioDTO.FiltroUsuarios();
        filtro.setTermo(termo);
        filtro.setTipoUsuario(tipoUsuario);
        filtro.setAtivo(ativo);
        filtro.setOrderBy(orderBy);
        filtro.setOrderDirection(orderDirection);

        Pageable pageable = PageRequest.of(page, size);
        Page<UsuarioDTO.UsuarioResumo> usuarios = usuarioService.listarUsuarios(filtro, pageable);
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/buscar")
    
    public ResponseEntity<List<UsuarioDTO.UsuarioResumo>> buscarPorTermo(@RequestParam String termo) {
        List<UsuarioDTO.UsuarioResumo> usuarios = usuarioService.buscarPorTermo(termo);
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/tipo/{tipo}")
    
    public ResponseEntity<List<UsuarioDTO.UsuarioResumo>> buscarPorTipo(@PathVariable TipoUsuario tipo) {
        List<UsuarioDTO.UsuarioResumo> usuarios = usuarioService.buscarPorTipo(tipo);
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioDTO.UsuarioResponse> obterPerfilUsuario(Principal principal) {
        UsuarioDTO.UsuarioResponse response = usuarioService.buscarPorEmail(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/perfil")
    public ResponseEntity<UsuarioDTO.UsuarioResponse> atualizarPerfil(
            Principal principal,
            @Valid @RequestBody UsuarioDTO.UsuarioRequest request) {
        UsuarioDTO.UsuarioResponse usuarioAtual = usuarioService.buscarPorEmail(principal.getName());
        UsuarioDTO.UsuarioResponse response = usuarioService.atualizarUsuario(usuarioAtual.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/senha")
    
    public ResponseEntity<Void> alterarSenha(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioDTO.AlterarSenhaRequest request) {
        usuarioService.alterarSenha(id, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/perfil/senha")
    public ResponseEntity<Void> alterarSenhaPerfil(
            Principal principal,
            @Valid @RequestBody UsuarioDTO.AlterarSenhaRequest request) {
        UsuarioDTO.UsuarioResponse usuarioAtual = usuarioService.buscarPorEmail(principal.getName());
        usuarioService.alterarSenha(usuarioAtual.getId(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/resetar-senha")
    
    public ResponseEntity<Void> resetarSenha(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String novaSenha = request.get("novaSenha");
        usuarioService.resetarSenha(id, novaSenha);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/permissoes")
    
    public ResponseEntity<List<UsuarioDTO.PermissaoResponse>> obterPermissoesUsuario(@PathVariable Long id) {
        List<UsuarioDTO.PermissaoResponse> permissoes = usuarioService.obterPermissoesUsuario(id);
        return ResponseEntity.ok(permissoes);
    }

    @GetMapping("/perfil/permissoes")
    public ResponseEntity<List<UsuarioDTO.PermissaoResponse>> obterPermissoesPerfil(Principal principal) {
        UsuarioDTO.UsuarioResponse usuarioAtual = usuarioService.buscarPorEmail(principal.getName());
        List<UsuarioDTO.PermissaoResponse> permissoes = usuarioService.obterPermissoesUsuario(usuarioAtual.getId());
        return ResponseEntity.ok(permissoes);
    }

    @GetMapping("/{id}/permissoes/verificar")
    
    public ResponseEntity<Map<String, Boolean>> verificarPermissao(
            @PathVariable Long id,
            @RequestParam String permissao) {
        boolean temPermissao = usuarioService.usuarioTemPermissao(id, permissao);
        return ResponseEntity.ok(Map.of("temPermissao", temPermissao));
    }

    @GetMapping("/{id}/permissoes/modulo/{modulo}")
    
    public ResponseEntity<List<String>> obterPermissoesModulo(
            @PathVariable Long id,
            @PathVariable String modulo) {
        List<String> permissoes = usuarioService.obterPermissoesModulo(id, modulo);
        return ResponseEntity.ok(permissoes);
    }

    @GetMapping("/tipos")
    
    public ResponseEntity<List<UsuarioDTO.TipoUsuarioInfo>> obterTiposUsuario() {
        List<UsuarioDTO.TipoUsuarioInfo> tipos = usuarioService.obterTiposUsuario();
        return ResponseEntity.ok(tipos);
    }

    @PutMapping("/{id}/bloquear")
    
    public ResponseEntity<Void> bloquearUsuario(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {
        String motivo = request != null ? request.get("motivo") : "Bloqueado pelo administrador";
        usuarioService.bloquearUsuario(id, motivo);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/desbloquear")
    
    public ResponseEntity<Void> desbloquearUsuario(@PathVariable Long id) {
        usuarioService.desbloquearUsuario(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/estatisticas")
    
    public ResponseEntity<Map<String, Object>> obterEstatisticas() {
        List<Object[]> estatisticasPorTipo = usuarioService.obterEstatisticasPorTipo();
        long totalAtivos = usuarioService.contarUsuariosAtivos();
        long totalUsuarios = usuarioService.contarTotalUsuarios();
        long usuariosInativos = totalUsuarios - totalAtivos;
        long usuariosBloqueados = usuarioService.contarUsuariosBloqueados();

        Map<String, Object> estatisticas = Map.of(
                "totalUsuarios", totalUsuarios,
                "usuariosAtivos", totalAtivos,
                "usuariosInativos", usuariosInativos,
                "usuariosBloqueados", usuariosBloqueados,
                "totalAdmins", usuarioService.contarPorTipo(TipoUsuario.ADMIN),
                "totalVendedores", usuarioService.contarPorTipo(TipoUsuario.VENDEDOR),
                "usuariosOnlineHoje", 0L, // Pode implementar futuramente
                "novosCadastrosUltimos30Dias", usuarioService.contarNovosCadastrosUltimos30Dias(),
                "porTipo", estatisticasPorTipo
        );

        return ResponseEntity.ok(estatisticas);
    }

    @GetMapping("/estatisticas/tipo/{tipo}")
    
    public ResponseEntity<Map<String, Long>> obterEstatisticasPorTipo(@PathVariable TipoUsuario tipo) {
        long count = usuarioService.contarPorTipo(tipo);
        return ResponseEntity.ok(Map.of("total", count));
    }

    @PostMapping("/lote/bloquear")
    
    public ResponseEntity<Map<String, Object>> bloquearUsuariosLote(@RequestBody List<Long> ids) {
        int processados = 0;
        int erros = 0;

        for (Long id : ids) {
            try {
                usuarioService.bloquearUsuario(id, "Bloqueio em lote");
                processados++;
            } catch (Exception e) {
                erros++;
            }
        }

        Map<String, Object> resultado = Map.of(
                "processados", processados,
                "erros", erros,
                "total", ids.size()
        );

        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/lote/desbloquear")
    
    public ResponseEntity<Map<String, Object>> desbloquearUsuariosLote(@RequestBody List<Long> ids) {
        int processados = 0;
        int erros = 0;

        for (Long id : ids) {
            try {
                usuarioService.desbloquearUsuario(id);
                processados++;
            } catch (Exception e) {
                erros++;
            }
        }

        Map<String, Object> resultado = Map.of(
                "processados", processados,
                "erros", erros,
                "total", ids.size()
        );

        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/lote/reativar")
    
    public ResponseEntity<Map<String, Object>> reativarUsuariosLote(@RequestBody List<Long> ids) {
        int processados = 0;
        int erros = 0;

        for (Long id : ids) {
            try {
                usuarioService.reativarUsuario(id);
                processados++;
            } catch (Exception e) {
                erros++;
            }
        }

        Map<String, Object> resultado = Map.of(
                "processados", processados,
                "erros", erros,
                "total", ids.size()
        );

        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/{id}/acesso")
    public ResponseEntity<Void> registrarAcesso(@PathVariable Long id) {
        usuarioService.registrarAcesso(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tentativas-login")
    public ResponseEntity<Void> incrementarTentativasLogin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        usuarioService.incrementarTentativasLogin(email);
        return ResponseEntity.ok().build();
    }
}
