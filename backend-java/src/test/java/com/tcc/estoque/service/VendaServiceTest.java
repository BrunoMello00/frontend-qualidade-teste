package com.tcc.estoque.service;

import com.tcc.estoque.dto.VendaDTO;
import com.tcc.estoque.dto.EventoDTO;
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

    @Test
    void criarVendaComDescontoPersonalizado() {
        VendaDTO.ItemVendaRequest item = new VendaDTO.ItemVendaRequest();
        item.setProdutoId(501L);
        item.setQuantidade(2);
        item.setPrecoUnitario(BigDecimal.valueOf(50));

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item));
        req.setFormaPagamento(FormaPagamento.BOLETO); // Muda para BOLETO para ficar PENDENTE
        req.setNomeCliente("Cliente Desconto");
        req.setDesconto(BigDecimal.valueOf(10)); // Desconto personalizado
        req.setObservacoes("Venda com desconto especial");

        when(produtoRepository.existsById(501L)).thenReturn(true);
        
        Produto produto = new Produto();
        produto.setId(501L);
        produto.setNome("Produto Teste");
        produto.setEstoque(100);
        produto.setTamanhos(Collections.emptyList());
        when(produtoRepository.findById(501L)).thenReturn(Optional.of(produto));

        when(vendaRepository.save(any())).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            v.setId(1001L);
            return v;
        });

        Usuario vendedor = new Usuario();
        vendedor.setNome("Vendedor Teste");
        
        Venda vendaReload = new Venda();
        vendaReload.setId(1001L);
        vendaReload.setItens(Collections.emptyList());
        vendaReload.setUsuario(vendedor);
        vendaReload.setStatus(StatusVenda.PENDENTE);
        vendaReload.setDesconto(BigDecimal.valueOf(10));
        vendaReload.setValorTotal(BigDecimal.valueOf(90));
        when(vendaRepository.findByIdWithItens(1001L)).thenReturn(Optional.of(vendaReload));

        VendaDTO.VendaResponse response = vendaService.criarVenda(req, vendedor);

        assertThat(response.getStatus()).isEqualTo(StatusVenda.PENDENTE);
        assertThat(response.getDesconto()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(response.getValorTotal()).isEqualByComparingTo(BigDecimal.valueOf(90)); // 100 - 10
    }

    @Test
    void criarVendaComDataCustomizada() {
        LocalDateTime dataCustomizada = LocalDateTime.of(2024, 12, 1, 10, 0);
        
        VendaDTO.ItemVendaRequest item = new VendaDTO.ItemVendaRequest();
        item.setProdutoId(502L);
        item.setQuantidade(1);
        item.setPrecoUnitario(BigDecimal.valueOf(100));

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item));
        req.setFormaPagamento(FormaPagamento.TRANSFERENCIA);
        req.setNomeCliente("Cliente Data");
        req.setDataVenda(dataCustomizada);

        when(produtoRepository.existsById(502L)).thenReturn(true);
        
        Produto produto = new Produto();
        produto.setId(502L);
        produto.setNome("Produto Data");
        produto.setEstoque(50);
        produto.setTamanhos(Collections.emptyList());
        when(produtoRepository.findById(502L)).thenReturn(Optional.of(produto));

        when(vendaRepository.save(any())).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            v.setId(1002L);
            return v;
        });

        Usuario vendedor = new Usuario();
        vendedor.setNome("Vendedor");
        
        Venda vendaReload = new Venda();
        vendaReload.setId(1002L);
        vendaReload.setItens(Collections.emptyList());
        vendaReload.setUsuario(vendedor);
        vendaReload.setStatus(StatusVenda.PENDENTE);
        vendaReload.setDataVenda(dataCustomizada);
        when(vendaRepository.findByIdWithItens(1002L)).thenReturn(Optional.of(vendaReload));

        VendaDTO.VendaResponse response = vendaService.criarVenda(req, vendedor);

        assertThat(response.getStatus()).isEqualTo(StatusVenda.PENDENTE);
        assertThat(response.getDataVenda()).isEqualTo(dataCustomizada);
    }

    @Test
    void obterEstatisticasVendasComDatasNull() {
        // Testar com datas null - devem usar defaults
        when(vendaRepository.countVendasPorPeriodo(any(), any())).thenReturn(10L);
        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any())).thenReturn(BigDecimal.valueOf(1000));
        when(vendaRepository.countByStatusAndDataVendaBetween(eq(StatusVenda.PENDENTE), any(), any())).thenReturn(2L);
        when(vendaRepository.countByStatusAndDataVendaBetween(eq(StatusVenda.CONFIRMADA), any(), any())).thenReturn(7L);
        when(vendaRepository.countByStatusAndDataVendaBetween(eq(StatusVenda.CANCELADA), any(), any())).thenReturn(1L);

        VendaDTO.EstatisticasVendasResponse stats = vendaService.obterEstatisticasVendas(null, null);

        assertThat(stats.getTotalVendas()).isEqualTo(10L);
        assertThat(stats.getTotalFaturamento()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(stats.getTicketMedio()).isEqualByComparingTo(BigDecimal.valueOf(100)); // 1000/10
        assertThat(stats.getVendasPendentes()).isEqualTo(2L);
        assertThat(stats.getVendasConfirmadas()).isEqualTo(7L);
        assertThat(stats.getVendasCanceladas()).isEqualTo(1L);
    }

    @Test
    void obterTopProdutosPorReceita() {
        LocalDate inicio = LocalDate.now().minusDays(30);
        LocalDate fim = LocalDate.now();

        // Mock dos resultados da query
        Object[] produto1 = {1L, "Produto A", "Categoria A", 10L, BigDecimal.valueOf(500), BigDecimal.valueOf(50)};
        Object[] produto2 = {2L, "Produto B", "Categoria B", 5L, BigDecimal.valueOf(250), BigDecimal.valueOf(50)};
        
        when(vendaRepository.findTopProdutosPorReceita(any(), any(), eq(StatusVenda.CONFIRMADA), any()))
                .thenReturn(List.of(produto1, produto2));
        when(vendaRepository.sumTotalProdutosVendidos(any(), any(), eq(StatusVenda.CONFIRMADA)))
                .thenReturn(15L);

        List<VendaDTO.TopProdutoResponse> topProdutos = vendaService.obterTopProdutos(inicio, fim, "receita", 5);

        assertThat(topProdutos).hasSize(2);
        assertThat(topProdutos.get(0).getRanking()).isEqualTo(1);
        assertThat(topProdutos.get(0).getReceitaTotal()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(topProdutos.get(1).getRanking()).isEqualTo(2);
    }

    @Test
    void obterTopProdutosMaisVendidos() {
        LocalDate inicio = LocalDate.now().minusDays(30);
        LocalDate fim = LocalDate.now();

        Object[] produto1 = {1L, "Produto A", null, 15L, BigDecimal.valueOf(750), null};
        List<Object[]> resultados = new java.util.ArrayList<>();
        resultados.add(produto1);
        
        when(vendaRepository.findTopProdutosMaisVendidos(any(), any(), eq(StatusVenda.CONFIRMADA), any()))
                .thenReturn(resultados);
        when(vendaRepository.sumTotalProdutosVendidos(any(), any(), eq(StatusVenda.CONFIRMADA)))
                .thenReturn(20L);

        List<VendaDTO.TopProdutoResponse> topProdutos = vendaService.obterTopProdutos(inicio, fim, "quantidade", null);

        assertThat(topProdutos).hasSize(1);
        assertThat(topProdutos.get(0).getQuantidadeVendida()).isEqualTo(15);
        assertThat(topProdutos.get(0).getCategoriaProduto()).isEqualTo("Sem departamento");
        assertThat(topProdutos.get(0).getPrecoUnitario()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void listarVendasComFiltros() {
        // Testando o método listarVendas com filtros
        StatusVenda status = StatusVenda.CONFIRMADA;
        LocalDate dataInicio = LocalDate.now().minusDays(7);
        LocalDate dataFim = LocalDate.now();
        String nomeCliente = "João";

        Usuario vendedor = new Usuario();
        vendedor.setNome("Vendedor Teste");

        Venda venda = new Venda();
        venda.setId(100L);
        venda.setClienteNome("João Silva");
        venda.setStatus(StatusVenda.CONFIRMADA);
        venda.setSubtotal(BigDecimal.valueOf(200));
        venda.setDesconto(BigDecimal.valueOf(20));
        venda.setValorTotal(BigDecimal.valueOf(180));
        venda.setUsuario(vendedor);
        venda.setItens(Collections.emptyList());

        when(vendaRepository.findVendasComFiltros(
                eq(status), any(LocalDateTime.class), any(LocalDateTime.class), 
                eq(nomeCliente), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(venda)));

        org.springframework.data.domain.Page<VendaDTO.VendaResponse> resultado = 
                vendaService.listarVendas(status, dataInicio, dataFim, nomeCliente, Pageable.unpaged());

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNomeCliente()).isEqualTo("João Silva");
    }

    @Test
    void listarVendasSemFiltros() {
        Usuario vendedor = new Usuario();
        vendedor.setNome("Vendedor");

        Venda venda = new Venda();
        venda.setId(101L);
        venda.setClienteNome("Cliente Teste");
        venda.setUsuario(vendedor);
        venda.setItens(Collections.emptyList());

        when(vendaRepository.findAllByOrderByDataVendaDesc(any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(venda)));

        org.springframework.data.domain.Page<VendaDTO.VendaResponse> resultado = 
                vendaService.listarVendas(null, null, null, null, Pageable.unpaged());

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(101L);
    }

    @Test
    void buscarVendaPorIdExistente() {
        Usuario vendedor = new Usuario();
        vendedor.setNome("Vendedor");
        vendedor.setEmail("vendedor@teste.com");

        ItemVenda item = new ItemVenda();
        item.setId(1L);
        item.setQuantidade(2);
        item.setPrecoUnitario(BigDecimal.valueOf(25));
        item.setSubtotal(BigDecimal.valueOf(50));
        
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto Teste");
        produto.setCodigo("PROD001");
        item.setProduto(produto);

        Venda venda = new Venda();
        venda.setId(102L);
        venda.setClienteNome("Cliente Busca");
        venda.setClienteEmail("cliente@teste.com");
        venda.setClienteTelefone("11999999999");
        venda.setStatus(StatusVenda.CONFIRMADA);
        venda.setDataVenda(LocalDateTime.now());
        venda.setDataConfirmacao(LocalDateTime.now());
        venda.setSubtotal(BigDecimal.valueOf(50));
        venda.setDesconto(BigDecimal.valueOf(5));
        venda.setValorTotal(BigDecimal.valueOf(45));
        venda.setObservacoes("Observação teste");
        venda.setUsuario(vendedor);
        venda.setItens(List.of(item));

        when(vendaRepository.findById(102L)).thenReturn(Optional.of(venda));

        Optional<VendaDTO.VendaResponse> resultado = vendaService.buscarVendaPorId(102L);

        assertThat(resultado).isPresent();
        VendaDTO.VendaResponse response = resultado.get();
        assertThat(response.getId()).isEqualTo(102L);
        assertThat(response.getNomeCliente()).isEqualTo("Cliente Busca");
        assertThat(response.getEmailVendedor()).isEqualTo("vendedor@teste.com");
        assertThat(response.getItens()).hasSize(1);
    }

    @Test
    void criarVendaComTelefoneEValidacaoCompleta() {
        VendaDTO.ItemVendaRequest item = new VendaDTO.ItemVendaRequest();
        item.setProdutoId(503L);
        item.setQuantidade(3);
        item.setPrecoUnitario(BigDecimal.valueOf(33.33));
        item.setDescontoItem(BigDecimal.valueOf(3.33)); // Teste desconto por item
        item.setTamanho("M"); // Teste tamanho

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(List.of(item));
        req.setFormaPagamento(FormaPagamento.BOLETO); // Muda para BOLETO para ficar PENDENTE
        req.setNomeCliente("Cliente Completo");
        req.setTelefoneCliente("11987654321");
        req.setEmailCliente("completo@teste.com");

        when(produtoRepository.existsById(503L)).thenReturn(true);
        
        Produto produto = new Produto();
        produto.setId(503L);
        produto.setNome("Produto Completo");
        produto.setEstoque(100);
        produto.setTamanhos(Collections.emptyList());
        when(produtoRepository.findById(503L)).thenReturn(Optional.of(produto));

        when(vendaRepository.save(any())).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            v.setId(1003L);
            return v;
        });

        Usuario vendedor = new Usuario();
        vendedor.setNome("Vendedor Completo");
        
        Venda vendaReload = new Venda();
        vendaReload.setId(1003L);
        vendaReload.setItens(Collections.emptyList());
        vendaReload.setUsuario(vendedor);
        vendaReload.setStatus(StatusVenda.PENDENTE);
        vendaReload.setClienteEmail("completo@teste.com");
        when(vendaRepository.findByIdWithItens(1003L)).thenReturn(Optional.of(vendaReload));

        // Mock cliente para processamento de pontuação
        Cliente cliente = new Cliente();
        cliente.setId(50L);
        cliente.setPontos(0);
        cliente.setEmail("completo@teste.com");
        when(clienteRepository.findByEmail("completo@teste.com")).thenReturn(Optional.of(cliente));

        VendaDTO.VendaResponse response = vendaService.criarVenda(req, vendedor);

        assertThat(response.getStatus()).isEqualTo(StatusVenda.PENDENTE);
        verify(itemVendaRepository, atLeastOnce()).save(any()); // Verificar criação de itens
    }

    @Test
    void buscarPontosCliente() {
        // Arrange
        Long clienteId = 1L;
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente@teste.com");
        cliente.setPontos(100);

        Recompensa recompensa = new Recompensa();
        recompensa.setId(1L);
        recompensa.setNome("Desconto 10%");
        recompensa.setDescricao("Desconto de 10% na compra");
        recompensa.setPontosNecessarios(50);
        recompensa.setPercentualDesconto(new BigDecimal("10.00"));
        recompensa.setCategoria("BRONZE");

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(recompensaRepository.findByAtivoTrueOrderByPontosNecessariosAsc())
                .thenReturn(List.of(recompensa));

        // Act
        VendaDTO.ClientePontosResponse response = vendaService.buscarPontosCliente(clienteId);

        // Assert
        assertThat(response.getClienteId()).isEqualTo(clienteId);
        assertThat(response.getNomeCliente()).isEqualTo("Cliente Teste");
        assertThat(response.getEmailCliente()).isEqualTo("cliente@teste.com");
        assertThat(response.getPontosDisponiveis()).isEqualTo(100);
        assertThat(response.getRecompensasDisponiveis()).hasSize(1);
        verify(clienteRepository).findById(clienteId);
        verify(recompensaRepository).findByAtivoTrueOrderByPontosNecessariosAsc();
    }

    @Test 
    void aplicarDescontoEventoComEventoComDesconto() throws Exception {
        // Testar o método privado aplicarDescontoEvento usando reflection
        
        // Arrange
        EventoDTO.EventoResumo eventoMock = mock(EventoDTO.EventoResumo.class);
        when(eventoMock.getDescontoPercentual()).thenReturn(new BigDecimal("10.00"));
        when(eventoService.buscarEventosComDesconto()).thenReturn(List.of(eventoMock));

        BigDecimal subtotal = new BigDecimal("100.00");
        
        // Use reflection para acessar método privado
        java.lang.reflect.Method metodoPrivado = VendaService.class.getDeclaredMethod("aplicarDescontoEvento", BigDecimal.class);
        metodoPrivado.setAccessible(true);

        // Act
        BigDecimal desconto = (BigDecimal) metodoPrivado.invoke(vendaService, subtotal);

        // Assert
        assertThat(desconto.compareTo(new BigDecimal("10.00"))).isEqualTo(0); // 10% de R$ 100,00 = R$ 10,00
        verify(eventoService).buscarEventosComDesconto();
    }

    @Test 
    void aplicarDescontoEventoSemEvento() throws Exception {
        // Testar o método privado aplicarDescontoEvento quando não há eventos
        
        // Arrange
        when(eventoService.buscarEventosComDesconto()).thenReturn(Collections.emptyList());
        BigDecimal subtotal = new BigDecimal("100.00");
        
        // Use reflection para acessar método privado
        java.lang.reflect.Method metodoPrivado = VendaService.class.getDeclaredMethod("aplicarDescontoEvento", BigDecimal.class);
        metodoPrivado.setAccessible(true);

        // Act
        BigDecimal desconto = (BigDecimal) metodoPrivado.invoke(vendaService, subtotal);

        // Assert
        assertThat(desconto).isEqualTo(BigDecimal.ZERO); // Sem eventos = sem desconto
        verify(eventoService).buscarEventosComDesconto();
    }

    @Test
    void criarVendaComPagamentoInstantaneoBaixaEstoqueDirecto() {
        // Este teste força a execução do método baixarEstoqueDirecto através de criarVenda com PIX
        
        // Arrange
        VendaDTO.ItemVendaRequest item = new VendaDTO.ItemVendaRequest();
        item.setProdutoId(201L);
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("50.00"));

        VendaDTO.VendaRequest request = new VendaDTO.VendaRequest();
        request.setItens(List.of(item));
        request.setFormaPagamento(FormaPagamento.PIX); // Pagamento instantâneo que baixa estoque direto
        request.setNomeCliente("Cliente PIX");

        // Mock produto com estoque suficiente
        Produto produto = new Produto();
        produto.setId(201L);
        produto.setNome("Produto PIX");
        produto.setPreco(new BigDecimal("50.00"));
        produto.setEstoque(10); // Estoque inicial
        produto.setTamanhos(Collections.emptyList());

        Usuario usuario = new Usuario();
        usuario.setNome("Vendedor PIX");

        when(produtoRepository.existsById(201L)).thenReturn(true);
        when(produtoRepository.findById(201L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);
        when(vendaRepository.save(any())).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            v.setId(2001L);
            return v;
        });

        // Mock para buscar venda após criação
        Venda vendaRetornada = new Venda();
        vendaRetornada.setId(2001L);
        vendaRetornada.setStatus(StatusVenda.CONFIRMADA);
        vendaRetornada.setItens(Collections.emptyList());
        vendaRetornada.setUsuario(usuario); // Definir o usuário
        when(vendaRepository.findByIdWithItens(2001L)).thenReturn(Optional.of(vendaRetornada));

        // Act
        VendaDTO.VendaResponse response = vendaService.criarVenda(request, usuario);

        // Assert
        assertThat(response.getId()).isEqualTo(2001L);
        assertThat(response.getStatus()).isEqualTo(StatusVenda.CONFIRMADA); // PIX = confirmada automaticamente
        
        // Verificar que o produto foi salvo (baixa de estoque)
        verify(produtoRepository, atLeastOnce()).save(any(Produto.class));
        // Verificar que a movimentação de estoque foi registrada
        verify(movimentacaoEstoqueRepository, atLeastOnce()).save(any(MovimentacaoEstoque.class));
    }
}


