package com.tcc.estoque.controller;

import com.tcc.estoque.dto.ClienteDTO;
import com.tcc.estoque.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/clientes")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN', 'OWNER')") // Clientes acessíveis apenas por VENDEDOR, ADMIN e OWNER
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteDTO.ClienteResponse> criarCliente(
            @Valid @RequestBody ClienteDTO.ClienteRequest request) {
        ClienteDTO.ClienteResponse cliente = clienteService.criarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cliente);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO.ClienteResponse> buscarPorId(@PathVariable Long id) {
        ClienteDTO.ClienteResponse cliente = clienteService.buscarPorId(id);
        return ResponseEntity.ok(cliente);
    }

    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<ClienteDTO.ClienteResponse> buscarPorCpf(@PathVariable String cpf) {
        ClienteDTO.ClienteResponse cliente = clienteService.buscarPorCpf(cpf);
        return ResponseEntity.ok(cliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO.ClienteResponse> atualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteDTO.ClienteRequest request) {
        ClienteDTO.ClienteResponse cliente = clienteService.atualizarCliente(id, request);
        return ResponseEntity.ok(cliente);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCliente(@PathVariable Long id) {
        clienteService.deletarCliente(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reativar")
    public ResponseEntity<Void> reativarCliente(@PathVariable Long id) {
        clienteService.reativarCliente(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<ClienteDTO.ClienteResumo>> listarClientes(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String dataCadastroInicio,
            @RequestParam(required = false) String dataCadastroFim,
            @RequestParam(required = false) String ultimaCompraInicio,
            @RequestParam(required = false) String ultimaCompraFim,
            @RequestParam(required = false) Integer pontosMinimos,
            @RequestParam(required = false) Integer pontosMaximos,
            @RequestParam(defaultValue = "dataCadastro") String orderBy,
            @RequestParam(defaultValue = "DESC") String orderDirection,
            @PageableDefault(size = 20) Pageable pageable) {

        ClienteDTO.FiltroClientes filtro = new ClienteDTO.FiltroClientes();
        filtro.setTermo(termo);
        if (categoria != null) {
            try {
                filtro.setCategoria(com.tcc.estoque.model.enums.CategoriaCliente.valueOf(categoria.toUpperCase()));
            } catch (IllegalArgumentException e) {
            }
        }
        filtro.setAtivo(ativo);
        filtro.setPontosMinimos(pontosMinimos);
        filtro.setPontosMaximos(pontosMaximos);
        filtro.setOrderBy(orderBy);
        filtro.setOrderDirection(orderDirection);

        Page<ClienteDTO.ClienteResumo> clientes = clienteService.listarClientes(filtro, pageable);
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/buscar/{termo}")
    public ResponseEntity<List<ClienteDTO.ClienteResumo>> buscarPorTermo(@PathVariable String termo) {
        List<ClienteDTO.ClienteResumo> clientes = clienteService.buscarPorTermo(termo);
        return ResponseEntity.ok(clientes);
    }

    @PostMapping("/{id}/pontos/adicionar")
    public ResponseEntity<Void> adicionarPontos(
            @PathVariable Long id,
            @Valid @RequestBody ClienteDTO.PontosRequest request) {
        clienteService.adicionarPontos(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/pontos/remover")
    public ResponseEntity<Void> removerPontos(
            @PathVariable Long id,
            @Valid @RequestBody ClienteDTO.PontosRequest request) {
        clienteService.removerPontos(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/historico-pontos")
    public ResponseEntity<List<ClienteDTO.HistoricoPontosResponse>> buscarHistoricoPontos(@PathVariable Long id) {
        List<ClienteDTO.HistoricoPontosResponse> historico = clienteService.buscarHistoricoPontos(id);
        return ResponseEntity.ok(historico);
    }

    @PostMapping("/{id}/registrar-compra")
    public ResponseEntity<Void> registrarCompra(
            @PathVariable Long id,
            @RequestParam BigDecimal valorCompra,
            @RequestParam(required = false) Long vendaId) {
        clienteService.registrarCompra(id, valorCompra, vendaId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/estatisticas")
    public ResponseEntity<ClienteDTO.EstatisticasClienteResponse> obterEstatisticas() {
        ClienteDTO.EstatisticasClienteResponse stats = clienteService.obterEstatisticas();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/aniversariantes")
    public ResponseEntity<List<ClienteDTO.ClienteResumo>> buscarAniversariantes() {
        List<ClienteDTO.ClienteResumo> aniversariantes = clienteService.buscarAniversariantes();
        return ResponseEntity.ok(aniversariantes);
    }

    @GetMapping("/dashboard/resumo")
    public ResponseEntity<ClienteDTO.EstatisticasClienteResponse> obterResumoClientes() {
        ClienteDTO.EstatisticasClienteResponse resumo = clienteService.obterEstatisticas();
        return ResponseEntity.ok(resumo);
    }

    @GetMapping("/top-clientes")
    public ResponseEntity<List<ClienteDTO.ClienteTopResponse>> obterTopClientes() {
        ClienteDTO.EstatisticasClienteResponse stats = clienteService.obterEstatisticas();
        return ResponseEntity.ok(stats.getClientesMaisFrequentes());
    }

    @GetMapping("/categorias/distribuicao")
    public ResponseEntity<List<ClienteDTO.DistribuicaoCategoriaResponse>> obterDistribuicaoCategorias() {
        ClienteDTO.EstatisticasClienteResponse stats = clienteService.obterEstatisticas();
        return ResponseEntity.ok(stats.getDistribuicaoCategorias());
    }

    @GetMapping("/validar-cpf/{cpf}")
    public ResponseEntity<Boolean> validarCpf(@PathVariable String cpf) {
        try {
            clienteService.buscarPorCpf(cpf);
            return ResponseEntity.ok(true); // CPF existe
        } catch (Exception e) {
            return ResponseEntity.ok(false); // CPF não existe
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Cliente API está funcionando!");
    }

    @GetMapping("/fake")
    public ResponseEntity<ClienteDTO.ClienteResponse> buscarOuCriarClienteFake() {
        ClienteDTO.ClienteResponse clienteFake = clienteService.buscarOuCriarClienteFake();
        return ResponseEntity.ok(clienteFake);
    }



    @PostMapping("/importar")
    public ResponseEntity<String> importarClientes() {
        return ResponseEntity.ok("Funcionalidade de importação em desenvolvimento");
    }

    @GetMapping("/exportar")
    public ResponseEntity<String> exportarClientes() {
        return ResponseEntity.ok("Funcionalidade de exportação em desenvolvimento");
    }

    @GetMapping("/relatorio/fidelizacao")
    public ResponseEntity<String> relatorioFidelizacao() {
        return ResponseEntity.ok("Relatório de fidelização em desenvolvimento");
    }

    @GetMapping("/relatorio/compras")
    public ResponseEntity<String> relatorioCompras() {
        return ResponseEntity.ok("Relatório de compras em desenvolvimento");
    }
}
