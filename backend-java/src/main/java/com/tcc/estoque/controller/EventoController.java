package com.tcc.estoque.controller;

import com.tcc.estoque.dto.EventoDTO;
import com.tcc.estoque.model.enums.StatusEvento;
import com.tcc.estoque.service.EventoService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/eventos")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN', 'OWNER')") // Eventos acessíveis por VENDEDOR, ADMIN e OWNER
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @PostMapping
    
    public ResponseEntity<EventoDTO.EventoResponse> criarEvento(
            @Valid @RequestBody EventoDTO.EventoRequest request,
            Principal principal) {
        EventoDTO.EventoResponse response = eventoService.criarEvento(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    
    public ResponseEntity<EventoDTO.EventoResponse> buscarPorId(@PathVariable Long id) {
        EventoDTO.EventoResponse response = eventoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    
    public ResponseEntity<EventoDTO.EventoResponse> atualizarEvento(
            @PathVariable Long id,
            @Valid @RequestBody EventoDTO.EventoRequest request,
            Principal principal) {
        EventoDTO.EventoResponse response = eventoService.atualizarEvento(id, request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    
    public ResponseEntity<Void> deletarEvento(@PathVariable Long id) {
        eventoService.deletarEvento(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reativar")
    
    public ResponseEntity<Void> reativarEvento(@PathVariable Long id) {
        eventoService.reativarEvento(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/status")
    
    public ResponseEntity<EventoDTO.EventoResponse> alterarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Principal principal) {
        StatusEvento novoStatus = StatusEvento.valueOf(request.get("status"));
        EventoDTO.EventoResponse response = eventoService.alterarStatus(id, novoStatus, principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    
    public ResponseEntity<Page<EventoDTO.EventoResumo>> listarEventos(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) StatusEvento status,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean vigente,
            @RequestParam(required = false) LocalDate dataInicioApos,
            @RequestParam(required = false) LocalDate dataInicioAntes,
            @RequestParam(defaultValue = "dataInicio") String orderBy,
            @RequestParam(defaultValue = "ASC") String orderDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        EventoDTO.FiltroEventos filtro = new EventoDTO.FiltroEventos();
        filtro.setTermo(termo);
        filtro.setStatus(status);
        filtro.setAtivo(ativo);
        filtro.setVigente(vigente);
        filtro.setDataInicioApos(dataInicioApos);
        filtro.setDataInicioAntes(dataInicioAntes);
        filtro.setOrderBy(orderBy);
        filtro.setOrderDirection(orderDirection);

        Pageable pageable = PageRequest.of(page, size);
        Page<EventoDTO.EventoResumo> eventos = eventoService.listarEventos(filtro, pageable);
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/buscar")
    
    public ResponseEntity<List<EventoDTO.EventoResumo>> buscarPorTermo(@RequestParam String termo) {
        List<EventoDTO.EventoResumo> eventos = eventoService.buscarPorTermo(termo);
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/vigentes")
    public ResponseEntity<List<EventoDTO.EventoResumo>> buscarEventosVigentes() {
        List<EventoDTO.EventoResumo> eventos = eventoService.buscarEventosVigentes();
        return ResponseEntity.ok(eventos);
    }



    @GetMapping("/ativos")

    public ResponseEntity<List<EventoDTO.EventoResumo>> listarEventosAtivos() {
        List<EventoDTO.EventoResumo> eventos = eventoService.listarEventosAtivos();
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/publicos")
    public ResponseEntity<List<EventoDTO.EventoResumo>> buscarEventosPublicos() {
        List<EventoDTO.EventoResumo> eventos = eventoService.buscarEventosPublicos();
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/vendas-permitidas")
    
    public ResponseEntity<List<EventoDTO.EventoResumo>> buscarEventosQuePermitemVendas() {
        List<EventoDTO.EventoResumo> eventos = eventoService.buscarEventosQuePermitemVendas();
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/com-desconto")
    public ResponseEntity<List<EventoDTO.EventoResumo>> buscarEventosComDesconto() {
        List<EventoDTO.EventoResumo> eventos = eventoService.buscarEventosComDesconto();
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/status/{status}")
    
    public ResponseEntity<List<EventoDTO.EventoResumo>> buscarPorStatus(@PathVariable StatusEvento status) {
        List<EventoDTO.EventoResumo> eventos = eventoService.buscarPorStatus(status);
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/status")
    public ResponseEntity<List<EventoDTO.StatusEventoInfo>> obterStatusDisponiveis() {
        List<EventoDTO.StatusEventoInfo> status = eventoService.obterStatusDisponiveis();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/estatisticas")
    
    public ResponseEntity<EventoDTO.EstatisticasEventoResponse> obterEstatisticas() {
        EventoDTO.EstatisticasEventoResponse stats = eventoService.obterEstatisticas();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/estatisticas/status")
    
    public ResponseEntity<List<Object[]>> obterEstatisticasPorStatus() {
        List<Object[]> stats = eventoService.obterEstatisticasPorStatus();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/estatisticas/mensal")
    
    public ResponseEntity<List<Object[]>> obterEstatisticasPorMes() {
        List<Object[]> stats = eventoService.obterEstatisticasPorMes();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/alertas/proximo-fim")
    
    public ResponseEntity<List<EventoDTO.EventoResumo>> buscarEventosProximosDoFim() {
        List<EventoDTO.EventoResumo> eventos = eventoService.buscarEventosProximosDoFim();
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/alertas/inicio-breve")
    
    public ResponseEntity<List<EventoDTO.EventoResumo>> buscarEventosQueComecamEmBreve() {
        List<EventoDTO.EventoResumo> eventos = eventoService.buscarEventosQueComecamEmBreve();
        return ResponseEntity.ok(eventos);
    }


    @GetMapping("/gerencia")
    
    public ResponseEntity<Map<String, Object>> obterVisaoGerencial() {
        EventoDTO.EstatisticasEventoResponse stats = eventoService.obterEstatisticas();
        List<EventoDTO.EventoResumo> eventosVigentes = eventoService.buscarEventosVigentes();
        List<EventoDTO.EventoResumo> eventosProximoFim = eventoService.buscarEventosProximosDoFim();
        List<EventoDTO.EventoResumo> eventosInicioBreve = eventoService.buscarEventosQueComecamEmBreve();

        Map<String, Object> visao = Map.of(
                "estatisticas", stats,
                "eventosVigentes", eventosVigentes,
                "alertasProximoFim", eventosProximoFim,
                "alertasInicioBreve", eventosInicioBreve
        );

        return ResponseEntity.ok(visao);
    }

    @GetMapping("/vendas")
    
    public ResponseEntity<Map<String, Object>> obterEventosParaVenda() {
        List<EventoDTO.EventoResumo> eventosDisponiveis = eventoService.buscarEventosQuePermitemVendas();
        List<EventoDTO.EventoResumo> eventosComDesconto = eventoService.buscarEventosComDesconto();

        Map<String, Object> dadosVenda = Map.of(
                "eventosDisponiveis", eventosDisponiveis,
                "eventosComDesconto", eventosComDesconto,
                "totalEventosAtivos", eventosDisponiveis.size()
        );

        return ResponseEntity.ok(dadosVenda);
    }

    @GetMapping("/dashboard-publico")
    public ResponseEntity<Map<String, Object>> obterDashboardPublico() {
        List<EventoDTO.EventoResumo> eventosPublicos = eventoService.buscarEventosPublicos();
        List<EventoDTO.EventoResumo> eventosVigentes = eventoService.buscarEventosVigentes();
        List<EventoDTO.EventoResumo> eventosComDesconto = eventoService.buscarEventosComDesconto();

        Map<String, Object> dashboard = Map.of(
                "eventosPublicos", eventosPublicos,
                "eventosVigentes", eventosVigentes,
                "eventosComDesconto", eventosComDesconto,
                "totalEventosPublicos", eventosPublicos.size()
        );

        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/lote/ativar")
    
    public ResponseEntity<Map<String, Object>> ativarEventosLote(@RequestBody List<Long> ids) {
        int processados = 0;
        int erros = 0;

        for (Long id : ids) {
            try {
                eventoService.reativarEvento(id);
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

    @PostMapping("/lote/desativar")
    
    public ResponseEntity<Map<String, Object>> desativarEventosLote(@RequestBody List<Long> ids) {
        int processados = 0;
        int erros = 0;

        for (Long id : ids) {
            try {
                eventoService.deletarEvento(id);
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

    @PostMapping("/lote/status")
    
    public ResponseEntity<Map<String, Object>> alterarStatusLote(
            @RequestBody Map<String, Object> request,
            Principal principal) {
        
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) request.get("ids");
        String novoStatusStr = (String) request.get("status");
        StatusEvento novoStatus = StatusEvento.valueOf(novoStatusStr);
        
        int processados = 0;
        int erros = 0;

        for (Long id : ids) {
            try {
                eventoService.alterarStatus(id, novoStatus, principal.getName());
                processados++;
            } catch (Exception e) {
                erros++;
            }
        }

        Map<String, Object> resultado = Map.of(
                "processados", processados,
                "erros", erros,
                "total", ids.size(),
                "novoStatus", novoStatus.getDescricao()
        );

        return ResponseEntity.ok(resultado);
    }
}
