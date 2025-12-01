package com.tcc.estoque.service;

import com.tcc.estoque.dto.ClienteDTO;
import com.tcc.estoque.model.Cliente;
import com.tcc.estoque.repository.ClienteRepository;
import com.tcc.estoque.repository.HistoricoPontosRepository;
import com.tcc.estoque.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import java.util.ArrayList;
import com.tcc.estoque.model.HistoricoPontos;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private HistoricoPontosRepository historicoPontosRepository;

    @InjectMocks
    private ClienteService clienteService;

    @BeforeEach
    void setUp() {

    }

    @Test
    void criarClienteComEmailECpf() {
        ClienteDTO.ClienteRequest req = new ClienteDTO.ClienteRequest();
        req.setNome("Fulano");
        req.setCpf("123.456.789-00");
        req.setEmail("fulano@example.com");

        when(clienteRepository.existsByCpf(anyString())).thenReturn(false);
        when(clienteRepository.existsByEmail(anyString())).thenReturn(false);

        when(clienteRepository.save(any())).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(500L);
            c.setDataCadastro(LocalDateTime.now());
            c.setTotalCompras(BigDecimal.ZERO);
            return c;
        });

        var resp = clienteService.criarCliente(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(500L);
        assertThat(resp.getNome()).isEqualTo("Fulano");
        assertThat(resp.getCpf()).isEqualTo("123.456.789-00");
        assertThat(resp.getEmail()).isEqualTo("fulano@example.com");
        assertThat(resp.getAtivo()).isTrue();

        verify(clienteRepository).existsByCpf("123.456.789-00");
        verify(clienteRepository).existsByEmail("fulano@example.com");
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void buscarPorCpfQuandoNaoExiste() {
        when(clienteRepository.findByCpf("000.000.000-00")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> clienteService.buscarPorCpf("000.000.000-00"));

        verify(clienteRepository).findByCpf("000.000.000-00");
    }

    @Test
    void buscarOuCriarClienteFakeQuandoNaoExiste() {
        when(clienteRepository.findByCpf("000.000.000-00")).thenReturn(Optional.empty());

        when(clienteRepository.save(any())).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(900L);
            return c;
        });

        var resp = clienteService.buscarOuCriarClienteFake();

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(900L);
        assertThat(resp.getIsFake()).isTrue();

        verify(clienteRepository).findByCpf("000.000.000-00");
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void adicionarPontos() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setPontos(5);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteDTO.PontosRequest req = new ClienteDTO.PontosRequest();
        req.setPontos(10);
        req.setVendaId(77L);
        req.setMotivo("ajuste_manual");
        req.setObservacoes("teste");

        clienteService.adicionarPontos(1L, req);

        ArgumentCaptor<HistoricoPontos> cap = ArgumentCaptor.forClass(HistoricoPontos.class);
        verify(historicoPontosRepository).save(cap.capture());

        HistoricoPontos saved = cap.getValue();
        assertThat(saved.getClienteId()).isEqualTo(1L);
        assertThat(saved.getVendaId()).isEqualTo(77L);
        assertThat(saved.getPontosAdicionados()).isEqualTo(10);
        assertThat(saved.getPontosAntes()).isEqualTo(5);
        assertThat(saved.getPontosDepois()).isEqualTo(15);

        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void registrarCompra() {
        Cliente c = new Cliente();
        c.setId(2L);
        c.setPontos(0);
        c.setTotalCompras(BigDecimal.ZERO);
        c.setQuantidadeCompras(0);

        when(clienteRepository.findById(2L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clienteService.registrarCompra(2L, BigDecimal.valueOf(25), 123L);

        ArgumentCaptor<HistoricoPontos> cap = ArgumentCaptor.forClass(HistoricoPontos.class);
        verify(historicoPontosRepository).save(cap.capture());

        HistoricoPontos saved = cap.getValue();
        assertThat(saved.getPontosAdicionados()).isEqualTo(2);
        assertThat(saved.getMotivo()).isEqualTo("compra");
        assertThat(saved.getVendaId()).isEqualTo(123L);

        ArgumentCaptor<Cliente> capCli = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository, atLeastOnce()).save(capCli.capture());
        Cliente updated = capCli.getValue();
        assertThat(updated.getQuantidadeCompras()).isEqualTo(1);
        assertThat(updated.getTotalCompras()).isEqualByComparingTo(BigDecimal.valueOf(25));
    }

    @Test
    void removerPontosNaoFicaNegativo() {
        Cliente c = new Cliente();
        c.setId(10L);
        c.setPontos(5);

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteDTO.PontosRequest req = new ClienteDTO.PontosRequest();
        req.setPontos(10);
        req.setMotivo("ajuste_manual");

        clienteService.removerPontos(10L, req);

        assertThat(c.getPontos()).isZero();
        ArgumentCaptor<HistoricoPontos> cap = ArgumentCaptor.forClass(HistoricoPontos.class);
        verify(historicoPontosRepository).save(cap.capture());
        HistoricoPontos saved = cap.getValue();
        assertThat(saved.getPontosAdicionados()).isEqualTo(-10);
    }

    @Test
    void adicionarPontosGrandeValor() {
        Cliente c = new Cliente();
        c.setId(20L);
        c.setPontos(0);

        when(clienteRepository.findById(20L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteDTO.PontosRequest req = new ClienteDTO.PontosRequest();
        req.setPontos(1000);
        req.setMotivo("bonus");

        clienteService.adicionarPontos(20L, req);

        assertThat(c.getPontos()).isEqualTo(1000);
        assertThat(c.getCategoria()).isNotNull();
        verify(historicoPontosRepository).save(any(HistoricoPontos.class));
    }

    @Test
    void obterEstatisticas() {
        when(clienteRepository.count()).thenReturn(50L);
        when(clienteRepository.countByAtivoTrue()).thenReturn(40L);
        when(clienteRepository.calcularTicketMedioGeral()).thenReturn(BigDecimal.valueOf(123.45));
        when(clienteRepository.calcularFaturamentoTotal()).thenReturn(BigDecimal.valueOf(6000));
    when(clienteRepository.calcularTotalPontosDistribuidos()).thenReturn(1500);

        Cliente c1 = new Cliente(); c1.setId(1L); c1.setQuantidadeCompras(2); c1.setTotalCompras(BigDecimal.valueOf(200)); c1.setPontos(10);
        when(clienteRepository.findClientesMaisFrequentes(any())).thenReturn(List.of(c1));

    Object[] dist = new Object[] { com.tcc.estoque.model.enums.CategoriaCliente.BRONZE, 10L };
    List<Object[]> distList = new ArrayList<>();
    distList.add(dist);
    when(clienteRepository.contarClientesPorCategoria()).thenReturn(distList);

        ClienteDTO.EstatisticasClienteResponse stats = clienteService.obterEstatisticas();

        assertThat(stats.getTotalClientes()).isEqualTo(50L);
        assertThat(stats.getClientesAtivos()).isEqualTo(40L);
        assertThat(stats.getTicketMedioGeral()).isEqualByComparingTo(BigDecimal.valueOf(123.45));
        assertThat(stats.getClientesMaisFrequentes()).hasSize(1);
        assertThat(stats.getDistribuicaoCategorias()).hasSize(1);
    }

    @Test
    void buscarAniversariantes() {
        Cliente c = new Cliente(); c.setId(77L); c.setNome("Aniv");
        when(clienteRepository.findAniversariantesDoMes(anyInt())).thenReturn(List.of(c));

        List<ClienteDTO.ClienteResumo> res = clienteService.buscarAniversariantes();
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getId()).isEqualTo(77L);
    }

    @Test
    void buscarOuCriarClienteFakeQuandoJaExiste() {
        Cliente existing = new Cliente();
        existing.setId(111L);
        existing.setIsFake(true);

        when(clienteRepository.findByCpf("000.000.000-00")).thenReturn(Optional.of(existing));

        var resp = clienteService.buscarOuCriarClienteFake();

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(111L);

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void registrarCompraQuandoClienteNaoExiste() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> clienteService.registrarCompra(999L, BigDecimal.valueOf(10), 1L));

        verify(clienteRepository).findById(999L);
        verify(historicoPontosRepository, never()).save(any());
    }

    @Test
    void buscarPorEmailQuandoNaoExiste() {

        ClienteDTO.ClienteRequest req = new ClienteDTO.ClienteRequest();
        req.setNome("X");
        req.setCpf("111.111.111-11");
        req.setEmail("existe@exemplo.com");

        when(clienteRepository.existsByCpf(anyString())).thenReturn(false);
        when(clienteRepository.existsByEmail("existe@exemplo.com")).thenReturn(true);

        assertThrows(com.tcc.estoque.exception.BusinessException.class, () -> clienteService.criarCliente(req));

        verify(clienteRepository).existsByEmail("existe@exemplo.com");
    }
}


