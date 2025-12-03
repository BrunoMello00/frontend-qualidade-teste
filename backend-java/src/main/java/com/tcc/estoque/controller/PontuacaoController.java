package com.tcc.estoque.controller;

import com.tcc.estoque.service.PontuacaoService;
import com.tcc.estoque.dto.PontuacaoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/pontuacao")
@RequiredArgsConstructor
@Tag(name = "Sistema de Pontuação", description = "Gerenciamento do sistema de pontuação e recompensas")
@CrossOrigin(origins = "*")
public class PontuacaoController {

    private final PontuacaoService pontuacaoService;

    // ========== GESTÃO DE CATEGORIAS ==========

    @PostMapping("/categorias")
    @Operation(summary = "Criar nova categoria", description = "Cria uma nova categoria de cliente")
    public ResponseEntity<PontuacaoDTO.CategoriaConfigResponse> criarCategoria(
            @Valid @RequestBody PontuacaoDTO.CategoriaConfigRequest request) {
        PontuacaoDTO.CategoriaConfigResponse response = pontuacaoService.criarCategoria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/categorias/{id}")
    @Operation(summary = "Atualizar categoria", description = "Atualiza uma categoria existente")
    public ResponseEntity<PontuacaoDTO.CategoriaConfigResponse> atualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody PontuacaoDTO.CategoriaConfigRequest request) {
        PontuacaoDTO.CategoriaConfigResponse response = pontuacaoService.atualizarCategoria(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/categorias/{id}")
    @Operation(summary = "Remover categoria", description = "Remove uma categoria (desativa)")
    public ResponseEntity<Void> removerCategoria(@PathVariable Long id) {
        pontuacaoService.removerCategoria(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorias", description = "Lista todas as categorias ativas")
    public ResponseEntity<List<PontuacaoDTO.CategoriaConfigResponse>> listarCategorias() {
        List<PontuacaoDTO.CategoriaConfigResponse> categorias = pontuacaoService.listarCategorias();
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/categorias/{id}")
    @Operation(summary = "Buscar categoria por ID", description = "Busca uma categoria específica")
    public ResponseEntity<PontuacaoDTO.CategoriaConfigResponse> buscarCategoriaPorId(@PathVariable Long id) {
        PontuacaoDTO.CategoriaConfigResponse categoria = pontuacaoService.buscarCategoriaPorId(id);
        return ResponseEntity.ok(categoria);
    }

    // ========== GESTÃO DE RECOMPENSAS ==========

    @PostMapping("/recompensas")
    @Operation(summary = "Criar nova recompensa", description = "Cria uma nova recompensa")
    public ResponseEntity<PontuacaoDTO.RecompensaResponse> criarRecompensa(
            @Valid @RequestBody PontuacaoDTO.RecompensaRequest request) {
        PontuacaoDTO.RecompensaResponse response = pontuacaoService.criarRecompensa(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/recompensas/{id}")
    @Operation(summary = "Atualizar recompensa", description = "Atualiza uma recompensa existente")
    public ResponseEntity<PontuacaoDTO.RecompensaResponse> atualizarRecompensa(
            @PathVariable Long id,
            @Valid @RequestBody PontuacaoDTO.RecompensaRequest request) {
        PontuacaoDTO.RecompensaResponse response = pontuacaoService.atualizarRecompensa(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/recompensas/{id}")
    @Operation(summary = "Remover recompensa", description = "Remove uma recompensa (desativa)")
    public ResponseEntity<Void> removerRecompensa(@PathVariable Long id) {
        pontuacaoService.removerRecompensa(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recompensas")
    @Operation(summary = "Listar recompensas", description = "Lista todas as recompensas ativas")
    public ResponseEntity<List<PontuacaoDTO.RecompensaResponse>> listarRecompensas() {
        List<PontuacaoDTO.RecompensaResponse> recompensas = pontuacaoService.listarRecompensas();
        return ResponseEntity.ok(recompensas);
    }

    @GetMapping("/recompensas/cliente/{clienteId}")
    @Operation(summary = "Listar recompensas disponíveis", description = "Lista recompensas que o cliente pode resgatar")
    public ResponseEntity<List<PontuacaoDTO.RecompensaResponse>> listarRecompensasDisponiveis(
            @PathVariable Long clienteId) {
        List<PontuacaoDTO.RecompensaResponse> recompensas = pontuacaoService.listarRecompensasDisponiveis(clienteId);
        return ResponseEntity.ok(recompensas);
    }

    // ========== GESTÃO DE PONTOS ==========

    @GetMapping("/clientes")
    @Operation(summary = "Listar clientes com pontuação", description = "Lista clientes ordenados por pontos")
    public ResponseEntity<Page<PontuacaoDTO.ClientePontuacaoResponse>> listarClientesComPontuacao(
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PontuacaoDTO.ClientePontuacaoResponse> clientes = pontuacaoService.listarClientesComPontuacao(pageable);
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Estatísticas de pontuação", description = "Obtém estatísticas do sistema de pontuação")
    public ResponseEntity<PontuacaoDTO.EstatisticasPontuacaoResponse> obterEstatisticas() {
        PontuacaoDTO.EstatisticasPontuacaoResponse stats = pontuacaoService.obterEstatisticas();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/recalcular-categorias")
    @Operation(summary = "Recalcular categorias", description = "Recalcula as categorias de todos os clientes")
    public ResponseEntity<Void> recalcularCategorias() {
        pontuacaoService.recalcularCategoriasClientes();
        return ResponseEntity.ok().build();
    }
}