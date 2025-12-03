package com.tcc.estoque.controller;

import com.tcc.estoque.dto.ProdutoDTO;
import com.tcc.estoque.enums.TipoCodigoBarras;

import com.tcc.estoque.service.ProdutoService;
import com.tcc.estoque.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Produtos", description = "Gerenciamento de produtos e estoque")
@SecurityRequirement(name = "bearerAuth")

public class ProdutoController {

    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;
    
    /**
     * Utilitário para obter ID do usuário autenticado
     */
    private Long obterUsuarioId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return usuarioService.findByEmail(userDetails.getUsername()).getId();
    }

    @Operation(summary = "Listar produtos", description = "Lista produtos com paginação e filtros opcionais")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de produtos retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    
    public ResponseEntity<Page<ProdutoDTO.ProdutoResponse>> listarProdutos(
            @Parameter(description = "Número da página (baseado em 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "nome") String sort,
            @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "asc") String direction,
            @Parameter(description = "Filtro por departamento") @RequestParam(required = false) String departamento,
            @Parameter(description = "Filtro por nome") @RequestParam(required = false) String nome,
            @Parameter(description = "Filtro por tamanho (ex: P, M, G)") @RequestParam(required = false) String tamanho) {
        
        log.info("Listando produtos - página: {}, tamanho: {}, departamento: {}, nome: {}", page, size, departamento, nome);
        
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
    Page<ProdutoDTO.ProdutoResponse> produtos = produtoService.listarProdutos(pageable, departamento, nome, tamanho);
        
        return ResponseEntity.ok(produtos);
    }

    @Operation(summary = "Buscar produto por ID", description = "Retorna os detalhes de um produto específico")
    @GetMapping("/{id}")
    
    public ResponseEntity<ProdutoDTO.ProdutoResponse> buscarPorId(
            @Parameter(description = "ID do produto") @PathVariable Long id) {
        
        log.info("Buscando produto por ID: {}", id);
        ProdutoDTO.ProdutoResponse produto = produtoService.buscarPorId(id);
        return ResponseEntity.ok(produto);
    }

    @Operation(summary = "Criar novo produto", description = "Cria um novo produto no sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ESTOQUISTA')")
    public ResponseEntity<ProdutoDTO.ProdutoResponse> criarProduto(
            @Valid @RequestBody ProdutoDTO.ProdutoRequest produtoRequest) {
        
        log.info("Criando novo produto: {}", produtoRequest.getNome());
        ProdutoDTO.ProdutoResponse produtoCriado = produtoService.criarProduto(produtoRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoCriado);
    }

    @Operation(summary = "Atualizar produto", description = "Atualiza um produto existente")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ESTOQUISTA')")
    public ResponseEntity<ProdutoDTO.ProdutoResponse> atualizarProduto(
            @Parameter(description = "ID do produto") @PathVariable Long id,
            @Valid @RequestBody ProdutoDTO.ProdutoRequest produtoRequest,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        log.info("Atualizando produto ID: {}", id);
        
        Long usuarioId = obterUsuarioId(userDetails);
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        
        ProdutoDTO.ProdutoResponse produtoAtualizado = produtoService.atualizarProduto(
            id, produtoRequest, usuarioId, ipAddress, userAgent);
        
        return ResponseEntity.ok(produtoAtualizado);
    }

    @Operation(summary = "Excluir produto", description = "Remove um produto do sistema (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ESTOQUISTA')")
    public ResponseEntity<Void> excluirProduto(
            @Parameter(description = "ID do produto") @PathVariable Long id) {
        
        log.info("Excluindo produto ID: {}", id);
        produtoService.excluirProduto(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reativar produto", description = "Reativa um produto desabilitado")
    @PutMapping("/{id}/reativar")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ESTOQUISTA')")
    public ResponseEntity<Void> reativarProduto(
            @Parameter(description = "ID do produto") @PathVariable Long id) {
        
        log.info("Reativando produto ID: {}", id);
        produtoService.reativarProduto(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar produtos desabilitados", description = "Lista produtos que foram desabilitados")
    @GetMapping("/desabilitados")
    
    public ResponseEntity<Page<ProdutoDTO.ProdutoResponse>> listarProdutosDesabilitados(
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "10") int size) {
        
        log.info("Listando produtos desabilitados - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProdutoDTO.ProdutoResponse> produtos = produtoService.listarProdutosDesabilitados(pageable);
        
        return ResponseEntity.ok(produtos);
    }

    @Operation(summary = "Listar categorias", description = "Retorna todas as categorias de produtos disponíveis")
    @GetMapping("/departamentos")
    
    public ResponseEntity<List<String>> listarDepartamentos() {
        log.info("Listando departamentos de produtos");
        List<String> departamentos = produtoService.listarDepartamentos();
        return ResponseEntity.ok(departamentos);
    }

    @Operation(summary = "Listar tipos de código de barras", description = "Retorna os tipos de código de barras suportados")
    @GetMapping("/tipos-codigo")
    
    public ResponseEntity<List<Map<String, Object>>> listarTiposCodigo() {
        log.info("Listando tipos de código de barras");
        
        List<Map<String, Object>> tipos = Arrays.stream(TipoCodigoBarras.values())
            .map(tipo -> Map.<String, Object>of(
                "codigo", tipo.name(),
                "nome", tipo.getCodigo(),
                "descricao", tipo.getDescricao(),
                "tamanhoMaximo", tipo.getTamanhoMaximo(),
                "exemplo", tipo.getExemplo(),
                "padraoBrasileiro", tipo.isPadraoBrasileiro(),
                "permiteGeracaoAutomatica", tipo.permiteGeracaoAutomatica()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(tipos);
    }

    @Operation(summary = "Obter estatísticas", description = "Retorna estatísticas gerais dos produtos")
    @GetMapping("/estatisticas")
    
    public ResponseEntity<Object> obterEstatisticas() {
        log.info("Obtendo estatísticas de produtos");
        Object estatisticas = produtoService.obterEstatisticas();
        return ResponseEntity.ok(estatisticas);
    }

    @Operation(summary = "Buscar produto por código de barras", description = "Busca um produto pelo seu código de barras")
    @GetMapping("/codigo/{codigo}")
    
    public ResponseEntity<ProdutoDTO.ProdutoResponse> buscarPorCodigo(
            @Parameter(description = "Código de barras do produto") @PathVariable String codigo) {
        
        log.info("Buscando produto por código: {}", codigo);
        ProdutoDTO.ProdutoResponse produto = produtoService.buscarPorCodigoBarras(codigo);
        return ResponseEntity.ok(produto);
    }

    @Operation(summary = "Produtos com estoque baixo", description = "Lista produtos com estoque abaixo do mínimo")
    @GetMapping("/estoque-baixo")
    
    public ResponseEntity<List<ProdutoDTO.ProdutoResumo>> produtosEstoqueBaixo(
            @Parameter(description = "Limite de produtos a retornar") @RequestParam(defaultValue = "10") Integer limite) {
        
        log.info("Listando produtos com estoque baixo");
        List<ProdutoDTO.ProdutoResumo> produtos = produtoService.buscarProdutosEstoqueBaixo(limite);
        return ResponseEntity.ok(produtos);
    }

    @Operation(summary = "Atualizar estoque", description = "Atualiza a quantidade em estoque de um produto")
    @PutMapping("/{id}/estoque")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ESTOQUISTA')")
    public ResponseEntity<ProdutoDTO.ProdutoResponse> atualizarEstoque(
            @Parameter(description = "ID do produto") @PathVariable Long id,
            @Valid @RequestBody ProdutoDTO.EstoqueRequest estoqueRequest) {
        
        log.info("Atualizando estoque do produto ID: {} para quantidade: {}", id, estoqueRequest.getQuantidade());
        ProdutoDTO.ProdutoResponse produto = produtoService.atualizarEstoque(id, estoqueRequest);
        return ResponseEntity.ok(produto);
    }

    // ===================================
    // ===================================

    @Operation(summary = "Adquirir lock de produto", description = "Adquire um lock para edição exclusiva do produto")
    @PostMapping("/{id}/lock")
    
    public ResponseEntity<ProdutoDTO.LockResponse> adquirirLock(
            @Parameter(description = "ID do produto") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        log.info("Tentando adquirir lock para produto ID: {}", id);
        
        Long usuarioId = obterUsuarioId(userDetails);
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        
        ProdutoDTO.LockResponse response = produtoService.adquirirLockProduto(id, usuarioId, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Liberar lock de produto", description = "Libera o lock de um produto")
    @DeleteMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ESTOQUISTA')")
    public ResponseEntity<ProdutoDTO.LockResponse> liberarLock(
            @Parameter(description = "ID do produto") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Liberando lock do produto ID: {}", id);
        
        Long usuarioId = obterUsuarioId(userDetails);
        
        ProdutoDTO.LockResponse response = produtoService.liberarLockProduto(id, usuarioId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verificar status do lock", description = "Verifica se um produto está bloqueado para edição")
    @GetMapping("/{id}/lock")
    
    public ResponseEntity<ProdutoDTO.LockResponse> verificarLock(
            @Parameter(description = "ID do produto") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Verificando status do lock para produto ID: {}", id);
        
        Long usuarioId = obterUsuarioId(userDetails);
        
        ProdutoDTO.LockResponse response = produtoService.verificarStatusLock(id, usuarioId);
        return ResponseEntity.ok(response);
    }

    // ===================================
    // ===================================

    @Operation(summary = "Gerar código personalizado", description = "Gera um novo código de barras personalizado")
    @PostMapping("/gerar-codigo")
    public ResponseEntity<Map<String, String>> gerarCodigo(
            @Valid @RequestBody ProdutoDTO.CodigoBarrasRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Gerando código personalizado do tipo: {}", request.getTipoCodigoBarras());
        
        String codigo = produtoService.gerarCodigoPersonalizado(
            request.getTipoCodigoBarras(), 
            request.getPrefixo()
        );
        
        return ResponseEntity.ok(Map.of(
            "codigo", codigo,
            "tipo", request.getTipoCodigoBarras().name(),
            "mensagem", "Código gerado com sucesso"
        ));
    }





    @Operation(summary = "Buscar por código resumido", description = "Busca produto pelo código resumido interno")
    @GetMapping("/codigo-resumido/{codigoResumido}")
    
    public ResponseEntity<ProdutoDTO.ProdutoResponse> buscarPorCodigoResumido(
            @Parameter(description = "Código resumido do produto") @PathVariable String codigoResumido) {
        
        log.info("Buscando produto por código resumido: {}", codigoResumido);
        ProdutoDTO.ProdutoResponse produto = produtoService.buscarPorCodigoResumido(codigoResumido);
        return ResponseEntity.ok(produto);
    }

    @Operation(summary = "Atualizar qualidade do produto", description = "Atualiza o status de qualidade de um produto")
    @PutMapping("/{id}/qualidade")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ESTOQUISTA')")
    public ResponseEntity<Map<String, String>> atualizarQualidadeProduto(
            @Parameter(description = "ID do produto") @PathVariable Long id,
            @Valid @RequestBody ProdutoDTO.QualidadeRequest qualidadeRequest,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        log.info("Atualizando qualidade do produto ID: {} para status: {}", id, qualidadeRequest.getStatusQualidade());
        
        Long usuarioId = obterUsuarioId(userDetails);
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        
        produtoService.atualizarQualidadeProduto(id, qualidadeRequest, usuarioId, ipAddress, userAgent);
        
        return ResponseEntity.ok(Map.of("message", "Qualidade do produto atualizada com sucesso"));
    }

}

