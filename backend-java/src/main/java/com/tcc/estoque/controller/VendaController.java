package com.tcc.estoque.controller;

import com.tcc.estoque.dto.VendaDTO;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.StatusVenda;
import com.tcc.estoque.security.SecurityUtil;
import com.tcc.estoque.service.VendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller responsável pela gestão de vendas
 */
@RestController
@RequestMapping("/vendas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vendas", description = "Operações de vendas e faturamento")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN', 'OWNER')") // Vendas acessíveis apenas por VENDEDOR, ADMIN e OWNER
public class VendaController {

    private final VendaService vendaService;
    private final SecurityUtil securityUtil;

    /**
     * Lista vendas com paginação e filtros
     */
    @GetMapping
    @Operation(summary = "Listar vendas", description = "Lista vendas com paginação e filtros opcionais")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de vendas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    
    public ResponseEntity<Page<VendaDTO.VendaResponse>> listarVendas(
            @Parameter(description = "Status da venda") 
            @RequestParam(required = false) StatusVenda status,
            
            @Parameter(description = "Data início do período (formato: yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            
            @Parameter(description = "Data fim do período (formato: yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            
            @Parameter(description = "Nome do cliente para filtro")
            @RequestParam(required = false) String nomeCliente,
            
            @Parameter(description = "Número da página (padrão: 0)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Tamanho da página (padrão: 20)")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Campo de ordenação (padrão: dataVenda)")
            @RequestParam(defaultValue = "dataVenda") String sortBy,
            
            @Parameter(description = "Direção da ordenação (ASC ou DESC, padrão: DESC)")
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Listando vendas - Página: {}, Tamanho: {}, Status: {}, Período: {} a {}", 
                page, size, status, dataInicio, dataFim);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<VendaDTO.VendaResponse> vendas = vendaService.listarVendas(
                status, dataInicio, dataFim, nomeCliente, pageable);

        return ResponseEntity.ok(vendas);
    }

    /**
     * Busca venda por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar venda por ID", description = "Retorna os detalhes de uma venda específica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venda encontrada"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<VendaDTO.VendaResponse> buscarVendaPorId(
            @Parameter(description = "ID da venda") 
            @PathVariable Long id) {

        log.info("Buscando venda por ID: {}", id);

        Optional<VendaDTO.VendaResponse> venda = vendaService.buscarVendaPorId(id);
        
        if (venda.isPresent()) {
            return ResponseEntity.ok(venda.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cria nova venda
     */
    @PostMapping
    @Operation(summary = "Criar nova venda", description = "Registra uma nova venda no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venda criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "422", description = "Erro de validação de negócio")
    })
    
    public ResponseEntity<VendaDTO.VendaResponse> criarVenda(
            @Parameter(description = "Dados da nova venda")
            @Valid @RequestBody VendaDTO.VendaRequest request) {

        log.info("Criando nova venda para cliente: {}", request.getNomeCliente());

        Usuario usuario = securityUtil.getUsuarioLogado();
        VendaDTO.VendaResponse vendaCriada = vendaService.criarVenda(request, usuario);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(vendaCriada);
    }

    /**
     * Confirma uma venda
     */
    @PatchMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar venda", description = "Confirma uma venda pendente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venda confirmada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada"),
            @ApiResponse(responseCode = "400", description = "Venda não pode ser confirmada"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<VendaDTO.VendaResponse> confirmarVenda(
            @Parameter(description = "ID da venda") 
            @PathVariable Long id) {

        log.info("Confirmando venda ID: {}", id);

        Usuario usuario = securityUtil.getUsuarioLogado();
        VendaDTO.VendaResponse vendaConfirmada = vendaService.confirmarVenda(id, usuario);
        return ResponseEntity.ok(vendaConfirmada);
    }

    /**
     * Cancela uma venda
     */
    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar venda", description = "Cancela uma venda e reverte o estoque")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venda cancelada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada"),
            @ApiResponse(responseCode = "400", description = "Venda não pode ser cancelada"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<VendaDTO.VendaResponse> cancelarVenda(
            @Parameter(description = "ID da venda") 
            @PathVariable Long id,
            
            @Parameter(description = "Motivo do cancelamento")
            @RequestParam(required = false) String motivo) {

        log.info("Cancelando venda ID: {} - Motivo: {}", id, motivo);

        Usuario usuario = securityUtil.getUsuarioLogado();
        VendaDTO.VendaResponse vendaCancelada = vendaService.cancelarVenda(id, motivo, usuario);
        return ResponseEntity.ok(vendaCancelada);
    }

    /**
     * Lista vendas por período
     */
    @GetMapping("/por-periodo")
    @Operation(summary = "Vendas por período", description = "Lista vendas em um período específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vendas por período retornadas"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<List<VendaDTO.VendaResponse>> obterVendasPorPeriodo(
            @Parameter(description = "Data início do período")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            
            @Parameter(description = "Data fim do período")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        log.info("Obtendo vendas por período - {} a {}", dataInicio, dataFim);

        List<VendaDTO.VendaResponse> vendasPorPeriodo = 
                vendaService.buscarVendasPorPeriodo(dataInicio, dataFim);
        
        return ResponseEntity.ok(vendasPorPeriodo);
    }

    /**
     * Retorna estatísticas de vendas
     */
    @GetMapping("/estatisticas")
    @Operation(summary = "Estatísticas de vendas", description = "Retorna estatísticas gerais de vendas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<VendaDTO.EstatisticasVendasResponse> obterEstatisticasVendas(
            @Parameter(description = "Data início do período")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            
            @Parameter(description = "Data fim do período")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        log.info("Obtendo estatísticas de vendas - Período: {} a {}", dataInicio, dataFim);

        VendaDTO.EstatisticasVendasResponse estatisticas = vendaService.obterEstatisticasVendas(dataInicio, dataFim);
        return ResponseEntity.ok(estatisticas);
    }

    /**
     * Retorna os produtos mais vendidos
     */
    @GetMapping("/top-produtos")
    @Operation(summary = "Top produtos mais vendidos", description = "Retorna os produtos mais vendidos em um período")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top produtos retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    
    public ResponseEntity<List<VendaDTO.TopProdutoResponse>> obterTopProdutos(
            @Parameter(description = "Data início do período")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            
            @Parameter(description = "Data fim do período")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            
            @Parameter(description = "Tipo de ranking: 'quantidade' ou 'receita'")
            @RequestParam(defaultValue = "quantidade") String tipoRanking,
            
            @Parameter(description = "Limite de produtos retornados")
            @RequestParam(defaultValue = "10") Integer limite) {

        log.info("Obtendo top produtos - Período: {} a {}, Tipo: {}, Limite: {}", 
                 dataInicio, dataFim, tipoRanking, limite);

        try {
            List<VendaDTO.TopProdutoResponse> topProdutos = vendaService.obterTopProdutos(
                dataInicio, dataFim, tipoRanking, limite);
            
            log.info("Top produtos obtidos com sucesso. Total: {}", topProdutos.size());
            return ResponseEntity.ok(topProdutos);
            
        } catch (Exception e) {
            log.error("Erro ao obter top produtos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 🎯 Busca informações de pontos do cliente e recompensas disponíveis
     */
    @GetMapping("/pontos/{clienteId}")
    @Operation(summary = "Buscar pontos do cliente", 
               description = "Retorna pontos do cliente e recompensas disponíveis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pontos do cliente retornados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    public ResponseEntity<VendaDTO.ClientePontosResponse> buscarPontosCliente(
            @Parameter(description = "ID do cliente")
            @PathVariable Long clienteId) {
        
        log.info("🎯 Buscando pontos do cliente ID: {}", clienteId);
        
        try {
            VendaDTO.ClientePontosResponse pontosCliente = vendaService.buscarPontosCliente(clienteId);
            log.info("🎯 Pontos do cliente obtidos com sucesso. Pontos: {}, Recompensas: {}", 
                    pontosCliente.getPontosDisponiveis(), 
                    pontosCliente.getRecompensasDisponiveis().size());
                    
            return ResponseEntity.ok(pontosCliente);
            
        } catch (RuntimeException e) {
            log.warn("🎯 Cliente não encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("🎯 Erro ao buscar pontos do cliente", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
