package com.tcc.estoque.service;

import com.tcc.estoque.dto.VendaDTO;
import com.tcc.estoque.model.*;
import com.tcc.estoque.model.enums.FormaPagamento;
import com.tcc.estoque.model.enums.StatusVenda;
import com.tcc.estoque.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @Mock
    private VendaRepository vendaRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private ItemVendaRepository itemVendaRepository;
    @Mock private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private RecompensaRepository recompensaRepository;
    @Mock private ClienteService clienteService;
    @Mock private EventoService eventoService;

    @InjectMocks
    private VendaService vendaService;

    @BeforeEach
    void setup() {
    }

    @Test
    void criarVendaComPagamentoInstantaneo() {

        VendaDTO.ItemVendaRequest item = new VendaDTO.ItemVendaRequest();
        item.setProdutoId(10L);
        item.setQuantidade(2);
        item.setPrecoUnitario(BigDecimal.valueOf(15));

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item));
    req.setFormaPagamento(FormaPagamento.BOLETO); 
        req.setNomeCliente("Cliente X");
        req.setEmailCliente(null);

    lenient().when(produtoRepository.existsById(10L)).thenReturn(true);

    Produto produto = new Produto(); produto.setId(10L); produto.setNome("P"); produto.setEstoque(100);
    produto.setTamanhos(Collections.emptyList());
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));

        lenient().when(vendaRepository.save(any())).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            v.setId(123L);
            return v;
        });
        lenient().when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Usuario vendedor = new Usuario(); vendedor.setNome("Vendedor");
        Venda vendaComItens = new Venda(); vendaComItens.setId(123L);
    vendaComItens.setItens(Collections.emptyList());
    vendaComItens.setUsuario(vendedor);
    vendaComItens.setStatus(StatusVenda.CONFIRMADA);
        when(vendaRepository.findByIdWithItens(123L)).thenReturn(Optional.of(vendaComItens));

    vendaService.criarVenda(req, vendedor);

    org.mockito.ArgumentCaptor<Venda> cap = org.mockito.ArgumentCaptor.forClass(Venda.class);
    verify(vendaRepository, atLeastOnce()).save(cap.capture());
    Venda saved = cap.getValue();
    assertThat(saved.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(30));
    assertThat(saved.getValorTotal()).isEqualByComparingTo(BigDecimal.valueOf(30));

    verify(produtoRepository).existsById(10L);
    verify(itemVendaRepository, atLeastOnce()).save(any());
    }

    @Test
    void criarVendaComQuantidadeZero() {
        VendaDTO.ItemVendaRequest item = new VendaDTO.ItemVendaRequest();
        item.setProdutoId(5L);
        item.setQuantidade(0);
        item.setPrecoUnitario(BigDecimal.valueOf(10));

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item));
        req.setFormaPagamento(FormaPagamento.PIX);

        Usuario vendedor = new Usuario();

        assertThrows(RuntimeException.class, () -> vendaService.criarVenda(req, vendedor));
    }

    @Test
    void buscarVendasPorPeriodo() {
        LocalDate inicio = LocalDate.now().minusDays(5);
        LocalDate fim = LocalDate.now();

        Venda v = new Venda();
        v.setId(77L);
        v.setClienteNome("C1");
        v.setSubtotal(BigDecimal.valueOf(50));
        v.setItens(Collections.emptyList());
        Usuario u = new Usuario(); u.setNome("Joao"); v.setUsuario(u);

        when(vendaRepository.findVendasPorPeriodo(any(), any())).thenReturn(List.of(v));

        List<VendaDTO.VendaResponse> res = vendaService.buscarVendasPorPeriodo(inicio, fim);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getId()).isEqualTo(77L);
        assertThat(res.get(0).getNomeVendedor()).isEqualTo("Joao");
    }

    @Test
    void aplicarRecompensaInativa() {
        Cliente cliente = new Cliente(); cliente.setId(1L); cliente.setPontos(100);
        Recompensa recompensa = new Recompensa(); recompensa.setId(2L); recompensa.setAtivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(recompensaRepository.findById(2L)).thenReturn(Optional.of(recompensa));

        assertThrows(RuntimeException.class, () -> vendaService.aplicarRecompensaPontos(1L, 2L, BigDecimal.valueOf(50)));
    }

    @Test
    void aplicarRecompensaSemPontos() {
        Cliente cliente = new Cliente(); cliente.setId(3L); cliente.setPontos(0);
        Recompensa recompensa = new Recompensa(); recompensa.setId(4L); recompensa.setAtivo(true); recompensa.setPontosNecessarios(50);
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(recompensaRepository.findById(4L)).thenReturn(Optional.of(recompensa));

        assertThrows(RuntimeException.class, () -> vendaService.aplicarRecompensaPontos(3L, 4L, BigDecimal.valueOf(100)));
    }

    @Test
    void aplicarRecompensaPercentualEFixo() {
        Cliente cliente = new Cliente(); cliente.setId(5L); cliente.setPontos(200);
        Recompensa recompensa = new Recompensa(); recompensa.setId(6L); recompensa.setAtivo(true);
        recompensa.setPercentualDesconto(BigDecimal.valueOf(10)); 
        recompensa.setValorDesconto(BigDecimal.valueOf(5)); 
        recompensa.setPontosNecessarios(10);

        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));
        when(recompensaRepository.findById(6L)).thenReturn(Optional.of(recompensa));

        BigDecimal desconto = vendaService.aplicarRecompensaPontos(5L, 6L, BigDecimal.valueOf(100));

        assertThat(desconto).isEqualByComparingTo(BigDecimal.valueOf(15));
    }

    @Test
    void deduzirPontosRecompensaHappyPath() {
        Cliente cliente = new Cliente(); cliente.setId(7L); cliente.setPontos(100);
        Recompensa recompensa = new Recompensa(); recompensa.setId(8L); recompensa.setAtivo(true); recompensa.setPontosNecessarios(30);

        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(recompensaRepository.findById(8L)).thenReturn(Optional.of(recompensa));

        Venda venda = new Venda(); venda.setId(999L);

        vendaService.deduzirPontosRecompensa(7L, 8L, venda);

        assertThat(cliente.getPontos()).isEqualTo(70);
        verify(clienteRepository).save(cliente);
    }

    @Test
    void criarVendaComEmailE3ItensProcessaPontuacaoCliente() {

        VendaDTO.ItemVendaRequest item1 = new VendaDTO.ItemVendaRequest();
        item1.setProdutoId(101L); item1.setQuantidade(1); item1.setPrecoUnitario(BigDecimal.valueOf(10));
        VendaDTO.ItemVendaRequest item2 = new VendaDTO.ItemVendaRequest();
        item2.setProdutoId(102L); item2.setQuantidade(1); item2.setPrecoUnitario(BigDecimal.valueOf(20));
        VendaDTO.ItemVendaRequest item3 = new VendaDTO.ItemVendaRequest();
        item3.setProdutoId(103L); item3.setQuantidade(1); item3.setPrecoUnitario(BigDecimal.valueOf(30));

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item1, item2, item3));
        req.setFormaPagamento(FormaPagamento.BOLETO);
        req.setEmailCliente("cliente@example.com");
        req.setNomeCliente("Cliente Pontos");

        Cliente cliente = new Cliente(); cliente.setId(55L); cliente.setPontos(10); cliente.setEmail("cliente@example.com");
        when(clienteRepository.findByEmail("cliente@example.com")).thenReturn(Optional.of(cliente));

        Produto p1 = new Produto(); p1.setId(101L); p1.setNome("P1"); p1.setPontuacaoProduto(2); p1.setEstoque(10); p1.setTamanhos(Collections.emptyList());
        Produto p2 = new Produto(); p2.setId(102L); p2.setNome("P2"); p2.setPontuacaoProduto(3); p2.setEstoque(10); p2.setTamanhos(Collections.emptyList());
        Produto p3 = new Produto(); p3.setId(103L); p3.setNome("P3"); p3.setPontuacaoProduto(5); p3.setEstoque(10); p3.setTamanhos(Collections.emptyList());

        when(produtoRepository.existsById(101L)).thenReturn(true);
        when(produtoRepository.existsById(102L)).thenReturn(true);
        when(produtoRepository.existsById(103L)).thenReturn(true);

        when(produtoRepository.findById(101L)).thenReturn(Optional.of(p1));
        when(produtoRepository.findById(102L)).thenReturn(Optional.of(p2));
        when(produtoRepository.findById(103L)).thenReturn(Optional.of(p3));

        when(vendaRepository.save(any())).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            v.setId(777L);
            return v;
        });

        Usuario vendedor = new Usuario(); vendedor.setNome("V");
        Venda vendaReload = new Venda(); vendaReload.setId(777L); vendaReload.setItens(Collections.emptyList()); vendaReload.setClienteEmail("cliente@example.com");
        vendaReload.setUsuario(vendedor);
        when(vendaRepository.findByIdWithItens(777L)).thenReturn(Optional.of(vendaReload));

        vendaService.criarVenda(req, vendedor);

        assertThat(cliente.getPontos()).isEqualTo(20);
        verify(clienteRepository).save(cliente);
    }

    @Test
    void confirmarVendaPendenteBaixaEstoque() {

        Produto produto = new Produto(); produto.setId(200L); produto.setNome("Prod"); produto.setEstoque(50); produto.setTamanhos(Collections.emptyList());
        ItemVenda item = new ItemVenda(); item.setId(301L); item.setProduto(produto); item.setQuantidade(5);
        Usuario usuario = new Usuario(); usuario.setNome("Operador");

        Venda venda = new Venda(); venda.setId(400L); venda.setStatus(StatusVenda.PENDENTE); venda.setItens(List.of(item));
        venda.setUsuario(usuario);

        when(vendaRepository.findById(400L)).thenReturn(Optional.of(venda));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vendaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VendaDTO.VendaResponse resp = vendaService.confirmarVenda(400L, usuario);

        assertThat(resp.getStatus()).isEqualTo(StatusVenda.CONFIRMADA);
        verify(produtoRepository).save(produto);
        verify(movimentacaoEstoqueRepository, atLeastOnce()).save(any());
        verify(vendaRepository).save(any());
    }

    @Test
    void cancelarVendaConfirmadaDevolveEstoque() {

        Produto produto = new Produto(); produto.setId(500L); produto.setNome("ProdX"); produto.setEstoque(20); produto.setTamanhos(Collections.emptyList());
        ItemVenda item = new ItemVenda(); item.setId(601L); item.setProduto(produto); item.setQuantidade(4);
        Usuario operador = new Usuario(); operador.setNome("Op");

        Venda venda = new Venda(); venda.setId(700L); venda.setStatus(StatusVenda.CONFIRMADA); venda.setItens(List.of(item));
        venda.setUsuario(operador);

        when(vendaRepository.findById(700L)).thenReturn(Optional.of(venda));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vendaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VendaDTO.VendaResponse resp = vendaService.cancelarVenda(700L, "cliente desistiu", operador);

        assertThat(resp.getStatus()).isEqualTo(StatusVenda.CANCELADA);

        verify(produtoRepository).save(produto);
        verify(movimentacaoEstoqueRepository, atLeastOnce()).save(any());
    }

    @Test
    void processarPontuacaoClienteProdutosSemPontuacao() {

        Venda venda = new Venda(); venda.setId(800L); venda.setClienteEmail("no-pontos@example.com");
        VendaDTO.ItemVendaRequest item1 = new VendaDTO.ItemVendaRequest(); item1.setProdutoId(401L); item1.setQuantidade(1); item1.setPrecoUnitario(BigDecimal.TEN);
        VendaDTO.ItemVendaRequest item2 = new VendaDTO.ItemVendaRequest(); item2.setProdutoId(402L); item2.setQuantidade(1); item2.setPrecoUnitario(BigDecimal.TEN);

        Cliente cliente = new Cliente(); cliente.setId(90L); cliente.setPontos(5); cliente.setEmail("no-pontos@example.com");
        when(clienteRepository.findByEmail("no-pontos@example.com")).thenReturn(Optional.of(cliente));

        Produto p1 = new Produto(); p1.setId(401L); p1.setNome("A"); p1.setPontuacaoProduto(null); p1.setEstoque(10); p1.setTamanhos(Collections.emptyList());
        Produto p2 = new Produto(); p2.setId(402L); p2.setNome("B"); p2.setPontuacaoProduto(null); p2.setEstoque(10); p2.setTamanhos(Collections.emptyList());
    when(produtoRepository.existsById(401L)).thenReturn(true);
    when(produtoRepository.existsById(402L)).thenReturn(true);
    when(produtoRepository.findById(401L)).thenReturn(Optional.of(p1));
    when(produtoRepository.findById(402L)).thenReturn(Optional.of(p2));

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item1, item2));
        req.setFormaPagamento(FormaPagamento.BOLETO);
        req.setEmailCliente("no-pontos@example.com");
        req.setNomeCliente("No Pontos");

        when(vendaRepository.save(any())).thenAnswer(inv -> { Venda v = inv.getArgument(0); v.setId(801L); return v; });
        Venda vendaReload = new Venda(); vendaReload.setId(801L); vendaReload.setItens(Collections.emptyList()); vendaReload.setClienteEmail("no-pontos@example.com");
        vendaReload.setUsuario(new Usuario());
        when(vendaRepository.findByIdWithItens(801L)).thenReturn(Optional.of(vendaReload));

        vendaService.criarVenda(req, new Usuario());

        verify(clienteRepository, never()).save(any());
        assertThat(cliente.getPontos()).isEqualTo(5);
    }

    @Test
    void processarPontuacaoClienteMenosDe3Itens() {
        Venda venda = new Venda(); venda.setId(900L); venda.setClienteEmail("menos3@example.com");
        VendaDTO.ItemVendaRequest item1 = new VendaDTO.ItemVendaRequest(); item1.setProdutoId(501L); item1.setQuantidade(1); item1.setPrecoUnitario(BigDecimal.TEN);
        VendaDTO.ItemVendaRequest item2 = new VendaDTO.ItemVendaRequest(); item2.setProdutoId(502L); item2.setQuantidade(1); item2.setPrecoUnitario(BigDecimal.TEN);

        Cliente cliente = new Cliente(); cliente.setId(91L); cliente.setPontos(0); cliente.setEmail("menos3@example.com");
        when(clienteRepository.findByEmail("menos3@example.com")).thenReturn(Optional.of(cliente));

        Produto p1 = new Produto(); p1.setId(501L); p1.setNome("X"); p1.setPontuacaoProduto(2); p1.setEstoque(10); p1.setTamanhos(Collections.emptyList());
        Produto p2 = new Produto(); p2.setId(502L); p2.setNome("Y"); p2.setPontuacaoProduto(3); p2.setEstoque(10); p2.setTamanhos(Collections.emptyList());
    when(produtoRepository.existsById(501L)).thenReturn(true);
    when(produtoRepository.existsById(502L)).thenReturn(true);
    when(produtoRepository.findById(501L)).thenReturn(Optional.of(p1));
    when(produtoRepository.findById(502L)).thenReturn(Optional.of(p2));

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item1, item2));
        req.setFormaPagamento(FormaPagamento.BOLETO);
        req.setEmailCliente("menos3@example.com");
        req.setNomeCliente("Menos 3");

        when(vendaRepository.save(any())).thenAnswer(inv -> { Venda v = inv.getArgument(0); v.setId(902L); return v; });
        Venda vendaReload = new Venda(); vendaReload.setId(902L); vendaReload.setItens(Collections.emptyList()); vendaReload.setClienteEmail("menos3@example.com");
        vendaReload.setUsuario(new Usuario());
        when(vendaRepository.findByIdWithItens(902L)).thenReturn(Optional.of(vendaReload));

        vendaService.criarVenda(req, new Usuario());

        verify(clienteRepository, never()).save(any());
        assertThat(cliente.getPontos()).isEqualTo(0);
    }

    @Test
    void criarVendaComUsoDePontos() {
        VendaDTO.ItemVendaRequest item = new VendaDTO.ItemVendaRequest(); item.setProdutoId(601L); item.setQuantidade(1); item.setPrecoUnitario(BigDecimal.valueOf(100));
        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item));
        req.setFormaPagamento(FormaPagamento.PIX);
        req.setClienteId(33L);
        req.setRecompensaId(44L);
        req.setUsarPontos(true);

        Cliente cliente = new Cliente(); cliente.setId(33L); cliente.setPontos(100);
        Recompensa recompensa = new Recompensa(); recompensa.setId(44L); recompensa.setAtivo(true); recompensa.setPontosNecessarios(20); recompensa.setPercentualDesconto(BigDecimal.valueOf(10));

    lenient().when(produtoRepository.existsById(601L)).thenReturn(true);
        Produto prod = new Produto(); prod.setId(601L); prod.setNome("Big"); prod.setEstoque(10); prod.setTamanhos(Collections.emptyList());
    lenient().when(produtoRepository.findById(601L)).thenReturn(Optional.of(prod));

        when(clienteRepository.findById(33L)).thenReturn(Optional.of(cliente));
        when(recompensaRepository.findById(44L)).thenReturn(Optional.of(recompensa));

    BigDecimal desconto = vendaService.aplicarRecompensaPontos(33L, 44L, BigDecimal.valueOf(100));
    assertThat(desconto).isEqualByComparingTo(BigDecimal.valueOf(10));

    vendaService.deduzirPontosRecompensa(33L, 44L, new Venda());
    verify(clienteRepository).save(any());
    assertThat(cliente.getPontos()).isEqualTo(80);
    }

    @Test
    void criarVendaProdutoNaoExiste() {

        VendaDTO.ItemVendaRequest item = new VendaDTO.ItemVendaRequest();
        item.setProdutoId(9999L);
        item.setQuantidade(1);
        item.setPrecoUnitario(BigDecimal.TEN);

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item));
        req.setFormaPagamento(FormaPagamento.BOLETO);

        when(produtoRepository.existsById(9999L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> vendaService.criarVenda(req, new Usuario()));
    }

    @Test
    void confirmarVendaNaoEncontrada() {
        when(vendaRepository.findById(12345L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> vendaService.confirmarVenda(12345L, new Usuario()));
    }

    @Test
    void confirmarVendaJaConfirmada() {
        Venda venda = new Venda();
        venda.setId(555L);
        venda.setStatus(StatusVenda.CONFIRMADA);
        when(vendaRepository.findById(555L)).thenReturn(Optional.of(venda));

        assertThrows(RuntimeException.class, () -> vendaService.confirmarVenda(555L, new Usuario()));
    }
}


