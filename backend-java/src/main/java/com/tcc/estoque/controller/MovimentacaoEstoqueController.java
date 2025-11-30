package com.tcc.estoque.controller;

import com.tcc.estoque.dto.*;
import com.tcc.estoque.service.TamanhoProdutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller específico para movimentação de estoque de produtos
 * Permite adicionar quantidade em tamanhos existentes ou criar novos tamanhos com quantidade
 */
@Slf4j
@RestController
@RequestMapping("/movimentacao-estoque")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
public class MovimentacaoEstoqueController {

    private final TamanhoProdutoService tamanhoProdutoService;

    /**
     * Lista tamanhos de um produto com informações de estoque
     */
    @GetMapping("/produto/{produtoId}/tamanhos")
    public ResponseEntity<List<TamanhoProdutoEstoqueDTO>> listarTamanhosComEstoque(
            @PathVariable Long produtoId) {
        log.debug("Listando tamanhos com estoque para produto ID: {}", produtoId);
        
        List<TamanhoProdutoEstoqueDTO> tamanhos = tamanhoProdutoService.listarTamanhosComEstoque(produtoId);
        return ResponseEntity.ok(tamanhos);
    }

    /**
     * Adiciona estoque em tamanho existente
     */
    @PatchMapping("/tamanho/{tamanhoId}/adicionar")
    public ResponseEntity<TamanhoProdutoEstoqueDTO> adicionarEstoqueTamanhoExistente(
            @PathVariable Long tamanhoId,
            @RequestBody @Valid AdicionarEstoqueRequest request) {
        log.debug("Adicionando {} unidades ao tamanho ID: {}", request.getQuantidade(), tamanhoId);
        
        TamanhoProdutoEstoqueDTO resultado = tamanhoProdutoService.adicionarEstoqueComHistorico(
                tamanhoId, request.getQuantidade(), request.getObservacoes());
        
        return ResponseEntity.ok(resultado);
    }

    /**
     * Cria novo tamanho e adiciona estoque inicial
     */
    @PostMapping("/produto/{produtoId}/tamanho-com-estoque")
    public ResponseEntity<TamanhoProdutoEstoqueDTO> criarTamanhoComEstoque(
            @PathVariable Long produtoId,
            @RequestBody @Valid CriarTamanhoComEstoqueRequest request) {
        log.debug("Criando novo tamanho com estoque para produto ID: {}", produtoId);
        
        TamanhoProdutoEstoqueDTO resultado = tamanhoProdutoService.criarTamanhoComEstoque(produtoId, request);
        
        return ResponseEntity.ok(resultado);
    }

    /**
     * Remove estoque (para vendas ou saídas)
     */
    @PatchMapping("/tamanho/{tamanhoId}/remover")
    public ResponseEntity<TamanhoProdutoEstoqueDTO> removerEstoque(
            @PathVariable Long tamanhoId,
            @RequestBody @Valid RemoverEstoqueRequest request) {
        log.debug("Removendo {} unidades do tamanho ID: {}", request.getQuantidade(), tamanhoId);
        
        TamanhoProdutoEstoqueDTO resultado = tamanhoProdutoService.removerEstoqueComHistorico(
                tamanhoId, request.getQuantidade(), request.getObservacoes());
        
        return ResponseEntity.ok(resultado);
    }

    /**
     * Lista produtos com estoque baixo
     */
    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<TamanhoProdutoEstoqueDTO>> buscarEstoqueBaixo(
            @RequestParam(defaultValue = "5") Integer estoqueMinimo) {
        log.debug("Buscando tamanhos com estoque baixo (< {})", estoqueMinimo);
        
        List<TamanhoProdutoEstoqueDTO> resultado = tamanhoProdutoService.buscarEstoqueBaixo(estoqueMinimo);
        return ResponseEntity.ok(resultado);
    }
}