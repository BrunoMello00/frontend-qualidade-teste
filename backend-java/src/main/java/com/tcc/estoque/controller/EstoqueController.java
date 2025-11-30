package com.tcc.estoque.controller;

import com.tcc.estoque.dto.MovimentacaoEstoqueDTO;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.TipoMovimentacao;
import com.tcc.estoque.security.SecurityUtil;
import com.tcc.estoque.service.MovimentacaoEstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller responsável por operações de movimentação de estoque
 */
@RestController
@RequestMapping("/estoque")
@Tag(name = "Estoque", description = "Endpoints para gestão de movimentações de estoque")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@RequiredArgsConstructor
@Slf4j
public class EstoqueController {

    private final MovimentacaoEstoqueService movimentacaoEstoqueService;
    private final SecurityUtil securityUtil;

    @Operation(summary = "Listar movimentações", description = "Lista movimentações de estoque com filtros opcionais")
    @GetMapping("/movimentacoes")
    public ResponseEntity<Page<MovimentacaoEstoqueDTO.MovimentacaoResponse>> listarMovimentacoes(
            @Parameter(description = "ID do produto") @RequestParam(required = false) Long produtoId,
            @Parameter(description = "Tipo de movimentação") @RequestParam(required = false) String tipo,
            @Parameter(description = "Data início (YYYY-MM-DD)") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data fim (YYYY-MM-DD)") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Pageable pageable) {
        
        log.info("Listando movimentações - Produto: {}, Tipo: {}, Período: {} a {}", 
                produtoId, tipo, dataInicio, dataFim);

        TipoMovimentacao tipoEnum = null;
        if (tipo != null && !tipo.isEmpty()) {
            try {
                tipoEnum = TipoMovimentacao.valueOf(tipo.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Tipo de movimentação inválido: {}", tipo);
                return ResponseEntity.badRequest().build();
            }
        }

        Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> movimentacoes = 
                movimentacaoEstoqueService.listarMovimentacoes(produtoId, tipoEnum, dataInicio, dataFim, pageable);

        return ResponseEntity.ok(movimentacoes);
    }

    @Operation(summary = "Registrar entrada", description = "Registra entrada de produtos no estoque")
    @PostMapping("/entrada")
    public ResponseEntity<MovimentacaoEstoqueDTO.MovimentacaoResponse> registrarEntrada(
            @RequestBody MovimentacaoEstoqueDTO.EntradaEstoqueRequest request) {
        
        log.info("Registrando entrada - Produto: {}, Quantidade: {}", 
                request.getProdutoId(), request.getQuantidade());

        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            if (!usuario.getTipoUsuario().name().equals("ADMIN") && 
                !usuario.getTipoUsuario().name().equals("OWNER") &&
                !usuario.getTipoUsuario().name().equals("ESTOQUISTA")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            if (request.getProdutoId() == null) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getQuantidade() == null || request.getQuantidade() <= 0) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getMotivo() == null || request.getMotivo().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            MovimentacaoEstoqueDTO.MovimentacaoResponse response = 
                    movimentacaoEstoqueService.registrarEntrada(request, usuario);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Erro ao registrar entrada: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Registrar saída", description = "Registra saída de produtos do estoque")
    @PostMapping("/saida")
    public ResponseEntity<MovimentacaoEstoqueDTO.MovimentacaoResponse> registrarSaida(
            @RequestBody MovimentacaoEstoqueDTO.SaidaEstoqueRequest request) {
        
        log.info("Registrando saída - Produto: {}, Quantidade: {}", 
                request.getProdutoId(), request.getQuantidade());

        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            if (!usuario.getTipoUsuario().name().equals("ADMIN") && 
                !usuario.getTipoUsuario().name().equals("OWNER") &&
                !usuario.getTipoUsuario().name().equals("ESTOQUISTA")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            if (request.getProdutoId() == null) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getQuantidade() == null || request.getQuantidade() <= 0) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getMotivo() == null || request.getMotivo().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            MovimentacaoEstoqueDTO.MovimentacaoResponse response = 
                    movimentacaoEstoqueService.registrarSaida(request, usuario);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Erro ao registrar saída: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Registrar ajuste", description = "Registra ajuste de estoque")
    @PostMapping("/ajuste")
    public ResponseEntity<MovimentacaoEstoqueDTO.MovimentacaoResponse> registrarAjuste(
            @RequestBody MovimentacaoEstoqueDTO.AjusteEstoqueRequest request) {
        
        log.info("Registrando ajuste - Produto: {}, Nova quantidade: {}", 
                request.getProdutoId(), request.getNovaQuantidade());

        try {
            Usuario usuario = securityUtil.getUsuarioLogado();
            
            if (!usuario.getTipoUsuario().name().equals("ADMIN") && 
                !usuario.getTipoUsuario().name().equals("OWNER") &&
                !usuario.getTipoUsuario().name().equals("ESTOQUISTA")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            if (request.getProdutoId() == null) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getNovaQuantidade() == null || request.getNovaQuantidade() < 0) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getMotivo() == null || request.getMotivo().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            MovimentacaoEstoqueDTO.MovimentacaoResponse response = 
                    movimentacaoEstoqueService.registrarAjuste(request, usuario);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Erro ao registrar ajuste: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Buscar movimentações por produto", description = "Retorna movimentações de um produto específico")
    @GetMapping("/movimentacoes/produto/{produtoId}")
    public ResponseEntity<Page<MovimentacaoEstoqueDTO.MovimentacaoResponse>> buscarMovimentacoesPorProduto(
            @Parameter(description = "ID do produto") @PathVariable Long produtoId,
            Pageable pageable) {
        
        log.info("Buscando movimentações do produto: {}", produtoId);

        Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> movimentacoes = 
                movimentacaoEstoqueService.buscarMovimentacoesPorProduto(produtoId, pageable);

        return ResponseEntity.ok(movimentacoes);
    }

    @Operation(summary = "Buscar movimentações por período", description = "Retorna movimentações em um período específico")
    @GetMapping("/movimentacoes/periodo")
    public ResponseEntity<List<MovimentacaoEstoqueDTO.MovimentacaoResponse>> buscarMovimentacoesPorPeriodo(
            @Parameter(description = "Data início (YYYY-MM-DD)") @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data fim (YYYY-MM-DD)") @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        
        log.info("Buscando movimentações no período: {} a {}", dataInicio, dataFim);

        List<MovimentacaoEstoqueDTO.MovimentacaoResponse> movimentacoes = 
                movimentacaoEstoqueService.buscarMovimentacoesPorPeriodo(dataInicio, dataFim);

        return ResponseEntity.ok(movimentacoes);
    }

    @Operation(summary = "Buscar últimas movimentações", description = "Retorna as últimas movimentações registradas")
    @GetMapping("/movimentacoes/ultimas")
    public ResponseEntity<Page<MovimentacaoEstoqueDTO.MovimentacaoResponse>> buscarUltimasMovimentacoes(
            Pageable pageable) {
        
        log.info("Buscando últimas movimentações");

        Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> movimentacoes = 
                movimentacaoEstoqueService.buscarUltimasMovimentacoes(pageable);

        return ResponseEntity.ok(movimentacoes);
    }

    @Operation(summary = "Obter estatísticas", description = "Retorna estatísticas de movimentação de estoque")
    @GetMapping("/estatisticas")
    public ResponseEntity<MovimentacaoEstoqueDTO.EstatisticasMovimentacaoResponse> obterEstatisticas(
            @Parameter(description = "Data início (YYYY-MM-DD)") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data fim (YYYY-MM-DD)") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        
        if (dataInicio == null) {
            dataInicio = LocalDate.now().minusMonths(1);
        }
        if (dataFim == null) {
            dataFim = LocalDate.now();
        }

        log.info("Obtendo estatísticas de movimentação para período: {} a {}", dataInicio, dataFim);

        MovimentacaoEstoqueDTO.EstatisticasMovimentacaoResponse estatisticas = 
                movimentacaoEstoqueService.obterEstatisticasMovimentacao(dataInicio, dataFim);

        return ResponseEntity.ok(estatisticas);
    }
}