package com.tcc.estoque.controller;

import com.tcc.estoque.dto.DashboardDTO;
import com.tcc.estoque.security.annotation.RequirePageAccess;
import com.tcc.estoque.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsável pelo dashboard principal
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Dados para o dashboard principal")
@SecurityRequirement(name = "bearerAuth")
@RequirePageAccess("dashboard") // Página de dashboard - acessível por todos os tipos de usuário
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Endpoint padrão do dashboard - redireciona para dados gerais
     */
    @GetMapping
    @Operation(summary = "Dashboard padrão", description = "Retorna dados gerais do dashboard (endpoint padrão)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<DashboardDTO.DadosGeraisResponse> obterDashboardPadrao() {
        log.info("Obtendo dashboard padrão (redirecionando para dados gerais)");
        return obterDadosGerais();
    }

    /**
     * Retorna dados gerais do dashboard
     */
    @GetMapping("/dados")
    @Operation(summary = "Dados do dashboard", description = "Retorna métricas gerais para o dashboard")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<DashboardDTO.DadosGeraisResponse> obterDadosGerais() {
        log.info("Obtendo dados gerais do dashboard");
        
        DashboardDTO.DadosGeraisResponse dados = dashboardService.obterDadosGerais();
        return ResponseEntity.ok(dados);
    }

    /**
     * Retorna vendas da semana
     */
    @GetMapping("/vendas-semana")
    @Operation(summary = "Vendas da semana", description = "Retorna vendas dos últimos 7 dias")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados de vendas retornados"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<DashboardDTO.VendasSemanaResponse> obterVendasSemana() {
        log.info("Obtendo vendas da semana");
        
        DashboardDTO.VendasSemanaResponse vendas = dashboardService.obterVendasSemana();
        return ResponseEntity.ok(vendas);
    }

    /**
     * Retorna vendas do mês
     */
    @GetMapping("/vendas-mes")
    @Operation(summary = "Vendas do mês", description = "Retorna vendas do mês atual")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados de vendas do mês retornados"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<DashboardDTO.VendasMesResponse> obterVendasMes() {
        log.info("Obtendo vendas do mês");
        
        DashboardDTO.VendasMesResponse vendas = dashboardService.obterVendasMes();
        return ResponseEntity.ok(vendas);
    }

    /**
     * Retorna top 5 produtos mais vendidos
     */
    @GetMapping("/top-produtos")
    @Operation(summary = "Top produtos", description = "Retorna os 5 produtos mais vendidos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top produtos retornados"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<DashboardDTO.TopProdutosResponse> obterTopProdutos(
            @Parameter(description = "Período em dias (padrão: 30)")
            @RequestParam(defaultValue = "30") int dias) {
        
        log.info("Obtendo top produtos - Últimos {} dias", dias);
        
        DashboardDTO.TopProdutosResponse topProdutos = dashboardService.obterTopProdutos(dias);
        return ResponseEntity.ok(topProdutos);
    }

    /**
     * Retorna produtos com estoque baixo
     */
    @GetMapping("/estoque-baixo")
    @Operation(summary = "Estoque baixo", description = "Retorna produtos com estoque crítico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produtos com estoque baixo retornados"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<DashboardDTO.EstoqueBaixoResponse> obterEstoqueBaixo() {
        log.info("Obtendo produtos com estoque baixo");
        
        DashboardDTO.EstoqueBaixoResponse estoqueBaixo = dashboardService.obterEstoqueBaixo();
        return ResponseEntity.ok(estoqueBaixo);
    }

    /**
     * Retorna últimas vendas
     */
    @GetMapping("/vendas-recentes")
    @Operation(summary = "Vendas recentes", description = "Retorna as vendas mais recentes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vendas recentes retornadas"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<DashboardDTO.VendasRecentesResponse> obterVendasRecentes(
            @Parameter(description = "Limite de vendas (padrão: 5)")
            @RequestParam(defaultValue = "5") int limite) {
        
        log.info("Obtendo vendas recentes - Limite: {}", limite);
        
        DashboardDTO.VendasRecentesResponse vendasRecentes = 
                dashboardService.obterVendasRecentes(limite);
        return ResponseEntity.ok(vendasRecentes);
    }
}
