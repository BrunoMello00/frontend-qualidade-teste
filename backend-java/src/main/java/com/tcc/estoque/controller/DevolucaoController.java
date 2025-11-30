package com.tcc.estoque.controller;

import com.tcc.estoque.dto.DevolucaoDTO;
import com.tcc.estoque.service.DevolucaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/devolucoes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Devoluções", description = "Endpoints para gerenciamento de devoluções e trocas")
public class DevolucaoController {

    private final DevolucaoService devolucaoService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('VENDEDOR')")
    @Operation(summary = "Processar devolução", description = "Processa uma nova devolução ou troca")
    @ApiResponse(responseCode = "201", description = "Devolução processada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    @ApiResponse(responseCode = "404", description = "Venda não encontrada")
    public ResponseEntity<DevolucaoDTO.DevolucaoResponse> processarDevolucao(
            @Valid @RequestBody DevolucaoDTO.DevolucaoRequest request) {
        
        log.info("Recebida solicitação de devolução para venda ID: {}", request.getVendaId());
        
        try {
            DevolucaoDTO.DevolucaoResponse response = devolucaoService.processarDevolucao(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Erro ao processar devolução: {}", e.getMessage());
            throw e; // Deixar o GlobalExceptionHandler tratar
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('VENDEDOR')")
    @Operation(summary = "Listar devoluções", description = "Lista todas as devoluções com paginação")
    @ApiResponse(responseCode = "200", description = "Lista de devoluções")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    public ResponseEntity<Page<DevolucaoDTO.DevolucaoResumo>> listarDevolucoes(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<DevolucaoDTO.DevolucaoResumo> devolucoes = devolucaoService.buscarDevolucoes(pageable);
        return ResponseEntity.ok(devolucoes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('VENDEDOR')")
    @Operation(summary = "Buscar devolução", description = "Busca uma devolução específica por ID")
    @ApiResponse(responseCode = "200", description = "Devolução encontrada")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    @ApiResponse(responseCode = "404", description = "Devolução não encontrada")
    public ResponseEntity<DevolucaoDTO.DevolucaoResponse> buscarDevolucao(
            @Parameter(description = "ID da devolução") @PathVariable Long id) {
        
        try {
            DevolucaoDTO.DevolucaoResponse response = devolucaoService.buscarPorId(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Devolução não encontrada: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/venda/{vendaId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('VENDEDOR')")
    @Operation(summary = "Buscar devoluções por venda", description = "Lista todas as devoluções de uma venda específica")
    @ApiResponse(responseCode = "200", description = "Lista de devoluções da venda")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    @ApiResponse(responseCode = "404", description = "Venda não encontrada")
    public ResponseEntity<List<DevolucaoDTO.DevolucaoResponse>> buscarDevolucoesPorVenda(
            @Parameter(description = "ID da venda") @PathVariable Long vendaId) {
        
        try {
            List<DevolucaoDTO.DevolucaoResponse> devolucoes = devolucaoService.buscarPorVenda(vendaId);
            return ResponseEntity.ok(devolucoes);
        } catch (RuntimeException e) {
            log.error("Venda não encontrada: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/estatisticas")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Estatísticas de devoluções", description = "Obtém estatísticas de devoluções por período")
    @ApiResponse(responseCode = "200", description = "Estatísticas de devoluções")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    public ResponseEntity<DevolucaoDTO.EstatisticasDevolucao> obterEstatisticas(
            @Parameter(description = "Data de início (formato: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        
        // Se não informado, usar últimos 30 dias
        if (inicio == null) {
            inicio = LocalDateTime.now().minusDays(30);
        }
        if (fim == null) {
            fim = LocalDateTime.now();
        }
        
        DevolucaoDTO.EstatisticasDevolucao estatisticas = devolucaoService.obterEstatisticas(inicio, fim);
        return ResponseEntity.ok(estatisticas);
    }

    @GetMapping("/produtos-defeituosos")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Listar produtos defeituosos", description = "Lista produtos com status defeituoso para gestão")
    @ApiResponse(responseCode = "200", description = "Lista de produtos defeituosos")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    public ResponseEntity<List<DevolucaoDTO.ProdutoDefeitosoResumo>> listarProdutosDefeituosos() {
        List<DevolucaoDTO.ProdutoDefeitosoResumo> produtos = devolucaoService.listarProdutosDefeituosos();
        return ResponseEntity.ok(produtos);
    }
}