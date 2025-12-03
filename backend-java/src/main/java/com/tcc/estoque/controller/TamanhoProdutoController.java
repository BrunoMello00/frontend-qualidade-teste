package com.tcc.estoque.controller;

import com.tcc.estoque.dto.TamanhoProdutoDTO;
import com.tcc.estoque.service.TamanhoProdutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controller para operações CRUD de tamanhos de produtos
 */
@RestController
@RequestMapping("/produtos/{produtoId}/tamanhos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class TamanhoProdutoController {

    private final TamanhoProdutoService tamanhoProdutoService;

    /**
     * Lista todos os tamanhos de um produto
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<List<TamanhoProdutoDTO>> listarTamanhos(@PathVariable Long produtoId) {
        log.debug("Listando tamanhos do produto ID: {}", produtoId);
        
        List<TamanhoProdutoDTO> tamanhos = tamanhoProdutoService.buscarTamanhosPorProdutoId(produtoId);
        
        return ResponseEntity.ok(tamanhos);
    }

    /**
     * 🆕 Lista tamanhos com estoque disponível (> 0) de um produto
     */
    @GetMapping("/com-estoque")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<List<TamanhoProdutoDTO>> listarTamanhosComEstoque(@PathVariable Long produtoId) {
        log.debug("Listando tamanhos com estoque do produto ID: {}", produtoId);
        
        List<TamanhoProdutoDTO> tamanhos = tamanhoProdutoService.buscarTamanhosComEstoque(produtoId);
        
        return ResponseEntity.ok(tamanhos);
    }

    /**
     * Busca um tamanho específico
     */
    @GetMapping("/{tamanhoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<TamanhoProdutoDTO> buscarTamanho(@PathVariable Long produtoId, 
                                                          @PathVariable Long tamanhoId) {
        log.debug("Buscando tamanho ID: {} do produto ID: {}", tamanhoId, produtoId);
        
        TamanhoProdutoDTO tamanho = tamanhoProdutoService.buscarPorId(tamanhoId);
        
        return ResponseEntity.ok(tamanho);
    }

    /**
     * Cria um novo tamanho para o produto
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<TamanhoProdutoDTO> criarTamanho(@PathVariable Long produtoId,
                                                         @Valid @RequestBody TamanhoProdutoDTO tamanhoDTO) {
        log.debug("Criando novo tamanho para produto ID: {}", produtoId);
        
        try {
            TamanhoProdutoDTO tamanhoCreated = tamanhoProdutoService.criarTamanho(produtoId, tamanhoDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(tamanhoCreated);
            
        } catch (RuntimeException e) {
            log.error("Erro ao criar tamanho: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Atualiza um tamanho existente
     */
    @PutMapping("/{tamanhoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<TamanhoProdutoDTO> atualizarTamanho(@PathVariable Long produtoId,
                                                             @PathVariable Long tamanhoId,
                                                             @Valid @RequestBody TamanhoProdutoDTO tamanhoDTO) {
        log.debug("Atualizando tamanho ID: {} do produto ID: {}", tamanhoId, produtoId);
        
        try {
            TamanhoProdutoDTO tamanhoAtualizado = tamanhoProdutoService.atualizarTamanho(tamanhoId, tamanhoDTO);
            
            return ResponseEntity.ok(tamanhoAtualizado);
            
        } catch (RuntimeException e) {
            log.error("Erro ao atualizar tamanho: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Exclui um tamanho (soft delete)
     */
    @DeleteMapping("/{tamanhoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> excluirTamanho(@PathVariable Long produtoId,
                                                              @PathVariable Long tamanhoId) {
        log.debug("Excluindo tamanho ID: {} do produto ID: {}", tamanhoId, produtoId);
        
        try {
            tamanhoProdutoService.excluirTamanho(tamanhoId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Tamanho excluído com sucesso",
                "produtoId", produtoId.toString(),
                "tamanhoId", tamanhoId.toString()
            ));
            
        } catch (RuntimeException e) {
            log.error("Erro ao excluir tamanho: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Atualiza apenas o estoque de um tamanho
     */
    @PatchMapping("/{tamanhoId}/estoque")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<TamanhoProdutoDTO> atualizarEstoque(@PathVariable Long produtoId,
                                                             @PathVariable Long tamanhoId,
                                                             @RequestBody Map<String, Integer> request) {
        log.debug("Atualizando estoque do tamanho ID: {} do produto ID: {}", tamanhoId, produtoId);
        
        Integer novoEstoque = request.get("estoque");
        if (novoEstoque == null) {
            throw new RuntimeException("Campo 'estoque' é obrigatório");
        }
        
        try {
            TamanhoProdutoDTO tamanhoAtualizado = tamanhoProdutoService.atualizarEstoque(tamanhoId, novoEstoque);
            
            return ResponseEntity.ok(tamanhoAtualizado);
            
        } catch (RuntimeException e) {
            log.error("Erro ao atualizar estoque: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Remove estoque de um tamanho (para vendas)
     */
    @PatchMapping("/{tamanhoId}/estoque/remover")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<TamanhoProdutoDTO> removerEstoque(@PathVariable Long produtoId,
                                                           @PathVariable Long tamanhoId,
                                                           @RequestBody Map<String, Integer> request) {
        log.debug("Removendo estoque do tamanho ID: {} do produto ID: {}", tamanhoId, produtoId);
        
        Integer quantidade = request.get("quantidade");
        if (quantidade == null) {
            throw new RuntimeException("Campo 'quantidade' é obrigatório");
        }
        
        try {
            TamanhoProdutoDTO tamanhoAtualizado = tamanhoProdutoService.removerEstoque(tamanhoId, quantidade);
            
            return ResponseEntity.ok(tamanhoAtualizado);
            
        } catch (RuntimeException e) {
            log.error("Erro ao remover estoque: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Adiciona estoque a um tamanho
     */
    @PatchMapping("/{tamanhoId}/estoque/adicionar")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<TamanhoProdutoDTO> adicionarEstoque(@PathVariable Long produtoId,
                                                             @PathVariable Long tamanhoId,
                                                             @RequestBody Map<String, Integer> request) {
        log.debug("Adicionando estoque ao tamanho ID: {} do produto ID: {}", tamanhoId, produtoId);
        
        Integer quantidade = request.get("quantidade");
        if (quantidade == null) {
            throw new RuntimeException("Campo 'quantidade' é obrigatório");
        }
        
        try {
            TamanhoProdutoDTO tamanhoAtualizado = tamanhoProdutoService.adicionarEstoque(tamanhoId, quantidade);
            
            return ResponseEntity.ok(tamanhoAtualizado);
            
        } catch (RuntimeException e) {
            log.error("Erro ao adicionar estoque: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Busca tamanhos com estoque baixo para o produto
     */
    @GetMapping("/estoque-baixo")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<List<TamanhoProdutoDTO>> buscarTamanhosEstoqueBaixo(
            @PathVariable Long produtoId,
            @RequestParam(defaultValue = "5") Integer estoqueMinimo) {
        log.debug("Buscando tamanhos com estoque baixo para produto ID: {}", produtoId);
        
        List<TamanhoProdutoDTO> tamanhos = tamanhoProdutoService
                .buscarTamanhosEstoqueBaixo(produtoId, estoqueMinimo);
        
        return ResponseEntity.ok(tamanhos);
    }
}