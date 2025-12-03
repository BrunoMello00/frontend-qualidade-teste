package com.tcc.estoque.service;

import com.tcc.estoque.dto.ClienteDTO;
import com.tcc.estoque.model.Cliente;
import com.tcc.estoque.model.HistoricoPontos;
import com.tcc.estoque.model.enums.CategoriaCliente;
import com.tcc.estoque.repository.ClienteRepository;
import com.tcc.estoque.repository.HistoricoPontosRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceMutacaoTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private HistoricoPontosRepository historicoPontosRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void criarClienteDeveSetarCamposBasicos() {
        ClienteDTO.ClienteRequest req = new ClienteDTO.ClienteRequest();
        req.setNome("Mutante");
        req.setCpf("000.111.222-33");
        req.setEmail("mutante@example.com");

        when(clienteRepository.existsByCpf(anyString())).thenReturn(false);
        when(clienteRepository.existsByEmail(anyString())).thenReturn(false);

        when(clienteRepository.save(any())).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(42L);
            c.setDataCadastro(LocalDateTime.now());
            return c;
        });

        var resp = clienteService.criarCliente(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(42L);
        assertThat(resp.getNome()).isEqualTo("Mutante");

        ArgumentCaptor<Cliente> cap = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(cap.capture());
        Cliente saved = cap.getValue();

        assertThat(saved.getAtivo()).isTrue();
        assertThat(saved.getPontos()).isZero();
        assertThat(saved.getTotalCompras()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getQuantidadeCompras()).isZero();
        assertThat(saved.getCategoria()).isEqualTo(CategoriaCliente.BRONZE);
        assertThat(saved.getIsFake()).isFalse();

        verify(clienteRepository).existsByCpf("000.111.222-33");
        verify(clienteRepository).existsByEmail("mutante@example.com");
    }

    @Test
    void adicionarPontosDeveSalvarHistorico() {
        Cliente c = new Cliente();
        c.setId(7L);
        c.setPontos(3);

        when(clienteRepository.findById(7L)).thenReturn(java.util.Optional.of(c));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteDTO.PontosRequest req = new ClienteDTO.PontosRequest();
        req.setPontos(10);
        req.setVendaId(11L);
        req.setMotivo("teste");

        clienteService.adicionarPontos(7L, req);

        ArgumentCaptor<HistoricoPontos> cap = ArgumentCaptor.forClass(HistoricoPontos.class);
        verify(historicoPontosRepository).save(cap.capture());

        HistoricoPontos saved = cap.getValue();
        assertThat(saved.getClienteId()).isEqualTo(7L);
        assertThat(saved.getPontosAdicionados()).isEqualTo(10);
        assertThat(saved.getPontosAntes()).isEqualTo(3);
        assertThat(saved.getPontosDepois()).isEqualTo(13);

        ArgumentCaptor<Cliente> capCli = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(capCli.capture());
        Cliente updated = capCli.getValue();
        assertThat(updated.getPontos()).isEqualTo(13);
    }

    @Test
    void deletarClienteDeveSetarAtivoFalse() {
        Cliente c = new Cliente();
        c.setId(8L);
        c.setAtivo(true);

        when(clienteRepository.findById(8L)).thenReturn(java.util.Optional.of(c));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clienteService.deletarCliente(8L);

        ArgumentCaptor<Cliente> cap = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(cap.capture());
        Cliente saved = cap.getValue();
        assertThat(saved.getAtivo()).isFalse();
    }

    @Test
    void reativarClienteDeveSetarAtivoTrue() {
        Cliente c = new Cliente();
        c.setId(9L);
        c.setAtivo(false);

        when(clienteRepository.findById(9L)).thenReturn(java.util.Optional.of(c));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clienteService.reativarCliente(9L);

        ArgumentCaptor<Cliente> cap = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(cap.capture());
        Cliente saved = cap.getValue();
        assertThat(saved.getAtivo()).isTrue();
    }
}


