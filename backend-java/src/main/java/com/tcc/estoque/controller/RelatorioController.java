package com.tcc.estoque.controller;

import com.tcc.estoque.dto.RelatorioDTO;
import com.tcc.estoque.security.annotation.RequirePageAccess;
import com.tcc.estoque.service.RelatorioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * ✅ CONTROLADOR DE RELATÓRIOS BÁSICO
 * 
 * Fornece endpoints para geração de relatórios:
 * - Vendas, Estoque, Movimentação, Dashboard
 * 
 * @author Sistema TCC
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/relatorios")
@CrossOrigin(origins = "*")
@RequirePageAccess("relatorios") // Página de relatórios - apenas ADMIN e OWNER
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;

    // ===== RELATÓRIOS DE VENDAS =====

    /**
     * Gera relatório básico de vendas
     */
    @GetMapping("/vendas")
    public ResponseEntity<RelatorioDTO.RelatorioVendasResponse> gerarRelatorioVendas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        
        RelatorioDTO.RelatorioVendasResponse relatorio = relatorioService.gerarRelatorioVendas(inicio, fim);
        return ResponseEntity.ok(relatorio);
    }

    /**
     * Gera relatório detalhado de vendas (vendas individuais)
     */
    @GetMapping("/vendas/detalhadas")
    public ResponseEntity<RelatorioDTO.VendasDetalhadasResponse> gerarRelatorioVendasDetalhadas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        
        RelatorioDTO.VendasDetalhadasResponse relatorio = relatorioService.gerarRelatorioVendasDetalhadas(inicio, fim);
        return ResponseEntity.ok(relatorio);
    }

    // ===== RELATÓRIOS DE ESTOQUE =====

    /**
     * Obtém dados para gráfico de evolução de vendas
     */
    @GetMapping("/vendas/grafico")
    public ResponseEntity<RelatorioDTO.GraficoVendasResponse> obterDadosGraficoVendas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        
        RelatorioDTO.GraficoVendasResponse dados = relatorioService.obterDadosGraficoVendas(inicio, fim);
        return ResponseEntity.ok(dados);
    }

    // ===== RELATÓRIOS DE ESTOQUE =====

    /**
     * Gera relatório de estoque
     */
    @GetMapping("/estoque")
    public ResponseEntity<RelatorioDTO.RelatorioEstoqueResponse> gerarRelatorioEstoque() {
        
        RelatorioDTO.RelatorioEstoqueResponse relatorio = relatorioService.gerarRelatorioEstoque();
        return ResponseEntity.ok(relatorio);
    }

    /**
     * Gera relatório de movimentação de estoque
     */
    @GetMapping("/movimentacao")
    
    public ResponseEntity<RelatorioDTO.RelatorioMovimentacaoResponse> gerarRelatorioMovimentacao(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        
        RelatorioDTO.RelatorioMovimentacaoResponse relatorio = 
            relatorioService.gerarRelatorioMovimentacao(inicio, fim);
        return ResponseEntity.ok(relatorio);
    }

    // ===== RELATÓRIOS DE PRODUTOS =====

    /**
     * Gera relatório de produtos
     */
    @GetMapping("/produtos")
    
    public ResponseEntity<RelatorioDTO.RelatorioProdutosResponse> gerarRelatorioProdutos() {
        
        RelatorioDTO.RelatorioProdutosResponse relatorio = relatorioService.gerarRelatorioProdutos();
        return ResponseEntity.ok(relatorio);
    }

    // ===== RELATÓRIOS DE CLIENTES =====

    /**
     * Gera relatório de clientes
     */
    @GetMapping("/clientes")
    
    public ResponseEntity<RelatorioDTO.RelatorioClientesResponse> gerarRelatorioClientes() {
        
        RelatorioDTO.RelatorioClientesResponse relatorio = relatorioService.gerarRelatorioClientes();
        return ResponseEntity.ok(relatorio);
    }

    // ===== DASHBOARDS =====

    /**
     * Gera dashboard executivo
     */
    @GetMapping("/dashboard/executivo")
    public ResponseEntity<RelatorioDTO.DashboardExecutivoResponse> gerarDashboardExecutivo() {
        log.info("Requisição recebida para dashboard executivo");
        RelatorioDTO.DashboardExecutivoResponse dashboard = relatorioService.gerarDashboardExecutivo();
        log.info("Dashboard executivo gerado: {}", dashboard);
        return ResponseEntity.ok(dashboard);
    }
    

}
