package com.tcc.estoque.service;

import com.tcc.estoque.dto.DashboardDTO;
import com.tcc.estoque.model.*;
import com.tcc.estoque.model.enums.*;
import com.tcc.estoque.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VendaRepository vendaRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private ItemVendaRepository itemVendaRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Usuario vendedorMock;
    private Venda vendaMock;
    private Produto produtoMock;
    private Cliente clienteMock;
    private Evento eventoMock;

    @BeforeEach
    void setup() {
        
        vendedorMock = Usuario.builder()
                .id(1L)
                .nome("Vendedor Teste")
                .email("vendedor@teste.com")
                .tipoUsuario(TipoUsuario.VENDEDOR)
                .ativo(true)
                .ultimoAcesso(LocalDateTime.now().minusDays(1))
                .build();

        clienteMock = Cliente.builder()
                .id(1L)
                .nome("Cliente Teste")
                .email("cliente@teste.com")
                .categoria(CategoriaCliente.OURO)
                .ativo(true)
                .build();

        produtoMock = Produto.builder()
                .id(1L)
                .nome("Produto Teste")
                .preco(BigDecimal.valueOf(99.99))
                .departamento("Eletrônicos")
                .estoqueTotal(50)
                .estoqueMinimo(10)
                .ativo(true)
                .build();

        vendaMock = Venda.builder()
                .id(1L)
                .clienteNome("Cliente Teste")
                .valorTotal(BigDecimal.valueOf(199.98))
                .dataVenda(LocalDateTime.now().minusDays(1))
                .status(StatusVenda.CONFIRMADA)
                .usuario(vendedorMock)
                .build();

        eventoMock = Evento.builder()
                .id(1L)
                .nome("Evento Teste")
                .descricao("Descrição do evento")
                .dataInicio(LocalDate.now().plusDays(1))
                .dataFim(LocalDate.now().plusDays(7))
                .status(StatusEvento.ATIVO)
                .metaVendas(BigDecimal.valueOf(10000))
                .ativo(true)
                .build();
    }

    @Test
    void deveObterDadosGeraisDashboardComValoresCorretos() {
        
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        
        when(vendaRepository.countByStatus(StatusVenda.CONFIRMADA)).thenReturn(150L);
        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any())).thenReturn(BigDecimal.valueOf(25000));
        when(vendaRepository.sumFaturamentoHoje()).thenReturn(BigDecimal.valueOf(1500));
        when(produtoRepository.countByAtivoTrue()).thenReturn(300L);
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(15L);
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(8L);
        when(clienteRepository.countByAtivoTrue()).thenReturn(120L);
        when(usuarioRepository.countByAtivoTrue()).thenReturn(25L);
        when(eventoRepository.countByStatusAndAtivoTrue(StatusEvento.ATIVO)).thenReturn(3L);
        when(clienteRepository.countClientesNovosHoje()).thenReturn(2L);

        when(vendaRepository.countByStatusAndDataVendaBetween(eq(StatusVenda.CONFIRMADA), any(), any()))
                .thenReturn(100L);

        DashboardDTO.DadosGeraisResponse resultado = dashboardService.obterDadosGerais();

        assertAll("Validação dos dados gerais",
                () -> assertThat(resultado.getTotalVendas()).isEqualTo(150L),
                () -> assertThat(resultado.getFaturamentoMes()).isEqualByComparingTo(BigDecimal.valueOf(25000)),
                () -> assertThat(resultado.getFaturamentoDia()).isEqualByComparingTo(BigDecimal.valueOf(1500)),
                () -> assertThat(resultado.getTotalProdutos()).isEqualTo(300L),
                () -> assertThat(resultado.getProdutosEstoqueBaixo()).isEqualTo(15L),
                () -> assertThat(resultado.getVendasPendentes()).isEqualTo(8L),
                () -> assertThat(resultado.getTotalClientes()).isEqualTo(120L),
                () -> assertThat(resultado.getTotalUsuarios()).isEqualTo(25L),
                () -> assertThat(resultado.getEventosAtivos()).isEqualTo(3L),
                () -> assertThat(resultado.getClientesNovosHoje()).isEqualTo(2L)
        );
    }

    @Test
    void deveTratarValoresNulosEmDadosGeraisCorretamente() {
        
        when(vendaRepository.countByStatus(StatusVenda.CONFIRMADA)).thenReturn(0L);
        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any())).thenReturn(null);
        when(vendaRepository.sumFaturamentoHoje()).thenReturn(null);
        when(produtoRepository.countByAtivoTrue()).thenReturn(0L);
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(0L);
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(0L);
        when(clienteRepository.countByAtivoTrue()).thenReturn(0L);
        when(usuarioRepository.countByAtivoTrue()).thenReturn(0L);
        when(eventoRepository.countByStatusAndAtivoTrue(StatusEvento.ATIVO)).thenReturn(0L);
        when(clienteRepository.countClientesNovosHoje()).thenReturn(0L);

        when(vendaRepository.countByStatusAndDataVendaBetween(eq(StatusVenda.CONFIRMADA), any(), any()))
                .thenReturn(0L);

        DashboardDTO.DadosGeraisResponse resultado = dashboardService.obterDadosGerais();

        assertAll("Validação com valores nulos/zero",
                () -> assertThat(resultado.getFaturamentoMes()).isEqualByComparingTo(BigDecimal.ZERO),
                () -> assertThat(resultado.getFaturamentoDia()).isEqualByComparingTo(BigDecimal.ZERO),
                () -> assertThat(resultado.getTicketMedio()).isEqualByComparingTo(BigDecimal.ZERO)
        );
    }

    @Test
    void deveObterEstatisticasGerenciaisCompletas() {

        when(vendaRepository.contarVendasPorStatus()).thenReturn(Arrays.asList(
                new Object[]{StatusVenda.CONFIRMADA, 80L},
                new Object[]{StatusVenda.PENDENTE, 15L},
                new Object[]{StatusVenda.CANCELADA, 5L}
        ));

        when(clienteRepository.contarClientesPorCategoria()).thenReturn(Arrays.asList(
                new Object[]{CategoriaCliente.OURO, 30L},
                new Object[]{CategoriaCliente.PRATA, 45L},
                new Object[]{CategoriaCliente.BRONZE, 25L}
        ));

        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Arrays.asList(
                new Object[]{TipoUsuario.ADMIN, 2L},
                new Object[]{TipoUsuario.VENDEDOR, 8L},
                new Object[]{TipoUsuario.COMPRAS, 3L}
        ));

        when(eventoRepository.contarEventosPorStatus()).thenReturn(Arrays.asList(
                new Object[]{StatusEvento.ATIVO, 3L},
                new Object[]{StatusEvento.PLANEJADO, 2L},
                new Object[]{StatusEvento.CONCLUIDO, 5L}
        ));

        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Arrays.asList(
                new Object[]{vendedorMock, 25L, BigDecimal.valueOf(15000)}
        ));

        when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Arrays.asList(
                new Object[]{produtoMock, 50L, BigDecimal.valueOf(4999.50)}
        ));

        when(eventoRepository.findTopEventosPorVendasDashboard(any())).thenReturn(Arrays.asList(eventoMock));
        when(eventoRepository.calcularTotalVendasEvento(eventoMock.getId())).thenReturn(BigDecimal.valueOf(8500));
        when(eventoRepository.contarVendasEvento(eventoMock.getId())).thenReturn(15);

        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(5L);
        when(eventoRepository.findEventosProximosAoFim(any(), any())).thenReturn(Arrays.asList(eventoMock));
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(3L);

        DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

        assertAll("Validação das estatísticas gerenciais",
                () -> assertThat(resultado.getVendasPorStatus()).isNotNull(),
                () -> assertThat(resultado.getVendasPorStatus()).containsEntry(StatusVenda.CONFIRMADA, 80L),
                () -> assertThat(resultado.getClientesPorCategoria()).containsEntry(CategoriaCliente.OURO, 30L),
                () -> assertThat(resultado.getUsuariosPorTipo()).containsEntry(TipoUsuario.VENDEDOR, 8L),
                () -> assertThat(resultado.getEventosPorStatus()).containsEntry(StatusEvento.ATIVO, 3L),
                () -> assertThat(resultado.getTopVendedores()).hasSize(1),
                () -> assertThat(resultado.getTopVendedores().get(0).getNome()).isEqualTo("Vendedor Teste"),
                () -> assertThat(resultado.getTopProdutos()).hasSize(1),
                () -> assertThat(resultado.getTopEventos()).hasSize(1),
                () -> assertThat(resultado.getAlertas()).hasSizeGreaterThan(0)
        );
    }

    @Test
    void deveObterDashboardVendedorComCalculosCorretos() {
        
        Long vendedorId = 1L;
        when(usuarioRepository.findById(vendedorId)).thenReturn(Optional.of(vendedorMock));

        when(vendaRepository.countByUsuarioIdAndStatus(vendedorId, StatusVenda.CONFIRMADA)).thenReturn(12L);
        when(vendaRepository.sumFaturamentoPorVendedor(eq(vendedorId), any(), any()))
                .thenReturn(BigDecimal.valueOf(7500));
        when(vendaRepository.countDistinctClientesByVendedor(eq(vendedorId), any(), any())).thenReturn(8L);

        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Arrays.asList(
                new Object[]{Usuario.builder().id(2L).build(), 30L, BigDecimal.valueOf(20000)},
                new Object[]{Usuario.builder().id(3L).build(), 25L, BigDecimal.valueOf(15000)},
                new Object[]{vendedorMock, 12L, BigDecimal.valueOf(7500)},
                new Object[]{Usuario.builder().id(4L).build(), 8L, BigDecimal.valueOf(5000)}
        ));

        when(itemVendaRepository.findTopProdutosPorVendedor(eq(vendedorId), any(), any(), any()))
                .thenReturn(Arrays.asList(
                        new Object[]{produtoMock, 25L, BigDecimal.valueOf(2499.75)}
                ));

        when(vendaRepository.sumFaturamentoPorVendedorEDia(eq(vendedorId), any())).thenReturn(BigDecimal.valueOf(500));
        when(vendaRepository.countByUsuarioIdAndDataVenda(eq(vendedorId), any())).thenReturn(2L);

        DashboardDTO.DashboardVendedorResponse resultado = dashboardService.obterDashboardVendedor(vendedorId);

        assertAll("Validação do dashboard do vendedor",
                () -> assertThat(resultado.getVendedor().getNome()).isEqualTo("Vendedor Teste"),
                () -> assertThat(resultado.getTotalVendas()).isEqualTo(12L),
                () -> assertThat(resultado.getFaturamentoMes()).isEqualByComparingTo(BigDecimal.valueOf(7500)),
                () -> assertThat(resultado.getMetaVendedor()).isEqualByComparingTo(BigDecimal.valueOf(10000)),
                () -> assertThat(resultado.getProgressoMeta()).isEqualByComparingTo(BigDecimal.valueOf(75.00)),
                () -> assertThat(resultado.getPosicaoRanking()).isEqualTo(3),
                () -> assertThat(resultado.getClientesAtendidos()).isEqualTo(8L),
                () -> assertThat(resultado.getVendasDiarias()).isNotNull(),
                () -> assertThat(resultado.getProdutosMaisVendidos()).hasSize(1)
        );
    }

    @Test
    void deveLancarExcecaoQuandoVendedorNaoExiste() {
        
        Long vendedorIdInexistente = 999L;
        when(usuarioRepository.findById(vendedorIdInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.obterDashboardVendedor(vendedorIdInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Vendedor não encontrado");
    }

    @Test
    void deveCalcularResumoPerformanceComCrescimentosCorretos() {
        
        LocalDate hoje = LocalDate.now();
        LocalDate ontem = hoje.minusDays(1);

        when(vendaRepository.sumFaturamentoPorDia(hoje)).thenReturn(BigDecimal.valueOf(2000));
        when(vendaRepository.sumFaturamentoPorDia(ontem)).thenReturn(BigDecimal.valueOf(1600));

        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any()))
                .thenReturn(BigDecimal.valueOf(12000)) 
                .thenReturn(BigDecimal.valueOf(10000)); 

        when(vendaRepository.countByDataVendaBetween(any(), any())).thenReturn(25L);
        when(vendaRepository.countByStatusAndDataVendaBetween(eq(StatusVenda.CONFIRMADA), any(), any()))
                .thenReturn(20L);

        DashboardDTO.PerformanceResumoResponse resultado = dashboardService.obterResumoPerformance();

        assertAll("Validação do resumo de performance",
                () -> assertThat(resultado.getVendasHoje()).isEqualByComparingTo(BigDecimal.valueOf(2000)),
                () -> assertThat(resultado.getVendasOntem()).isEqualByComparingTo(BigDecimal.valueOf(1600)),
                () -> assertThat(resultado.getCrescimentoDiario()).isEqualByComparingTo(BigDecimal.valueOf(25.00)),
                () -> assertThat(resultado.getTaxaConversaoVendas()).isEqualTo(80.0), 
                () -> assertThat(resultado.getTicketMedioMes()).isNotNull()
        );
    }

    @Test
    void deveTratarValoresNulosEmCalculosPerformance() {
        
        when(vendaRepository.sumFaturamentoPorDia(any())).thenReturn(null);
        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any())).thenReturn(null);
        when(vendaRepository.countByDataVendaBetween(any(), any())).thenReturn(0L);
        when(vendaRepository.countByStatusAndDataVendaBetween(any(), any(), any())).thenReturn(0L);

        DashboardDTO.PerformanceResumoResponse resultado = dashboardService.obterResumoPerformance();

        assertAll("Validação com valores nulos",
                () -> assertThat(resultado.getVendasHoje()).isEqualByComparingTo(BigDecimal.ZERO),
                () -> assertThat(resultado.getVendasOntem()).isEqualByComparingTo(BigDecimal.ZERO),
                () -> assertThat(resultado.getVendasSemana()).isEqualByComparingTo(BigDecimal.ZERO),
                () -> assertThat(resultado.getVendasMes()).isEqualByComparingTo(BigDecimal.ZERO),
                () -> assertThat(resultado.getTaxaConversaoVendas()).isEqualTo(0.0)
        );
    }

    @Test
    void deveObterVendasSemanaComCalculosCorretos() {
        
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.minusDays(6);
        
        List<Venda> vendasSemana = Arrays.asList(
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(500))
                        .dataVenda(hoje.atTime(10, 0))
                        .build(),
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(750))
                        .dataVenda(hoje.minusDays(1).atTime(14, 30))
                        .build()
        );

        when(vendaRepository.findVendasPorPeriodo(any(), any())).thenReturn(vendasSemana);

        DashboardDTO.VendasSemanaResponse resultado = dashboardService.obterVendasSemana();

        assertAll("Validação das vendas da semana",
                () -> assertThat(resultado.getVendas()).hasSize(7), 
                () -> assertThat(resultado.getTotalSemana()).isEqualByComparingTo(BigDecimal.valueOf(1250)),
                () -> assertThat(resultado.getMediaDiaria()).isEqualByComparingTo(BigDecimal.valueOf(178.57)),
                () -> assertThat(resultado.getCrescimentoSemana()).isEqualByComparingTo(BigDecimal.valueOf(5.2))
        );
    }

    @Test
    void deveObterVendasMesComMetaPercentual() {
        
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        
        List<Venda> vendasMes = Arrays.asList(
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(15000))
                        .dataVenda(inicioMes.plusDays(5).atTime(10, 0))
                        .build(),
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(10000))
                        .dataVenda(inicioMes.plusDays(10).atTime(14, 30))
                        .build()
        );

        when(vendaRepository.findVendasPorPeriodo(any(), any())).thenReturn(vendasMes);

        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any()))
                .thenReturn(BigDecimal.valueOf(25000)) 
                .thenReturn(BigDecimal.valueOf(20000)); 

        DashboardDTO.VendasMesResponse resultado = dashboardService.obterVendasMes();

        assertAll("Validação das vendas do mês",
                () -> assertThat(resultado.getTotalMes()).isEqualByComparingTo(BigDecimal.valueOf(25000)),
                () -> assertThat(resultado.getMetaMes()).isEqualByComparingTo(BigDecimal.valueOf(50000)),
                () -> assertThat(resultado.getPercentualMeta()).isEqualByComparingTo(BigDecimal.valueOf(50.00)),
                () -> assertThat(resultado.getVendas()).hasSizeGreaterThan(0)
        );
    }

    @Test
    void deveObterTopProdutosComDadosSimulados() {
        
        List<Produto> produtosAtivos = Arrays.asList(
                produtoMock,
                Produto.builder()
                        .id(2L)
                        .nome("Produto 2")
                        .preco(BigDecimal.valueOf(149.99))
                        .departamento("Casa")
                        .ativo(true)
                        .build()
        );

        when(produtoRepository.findAll()).thenReturn(produtosAtivos);

        DashboardDTO.TopProdutosResponse resultado = dashboardService.obterTopProdutos(30);

        assertAll("Validação top produtos",
                () -> assertThat(resultado.getProdutos()).hasSizeGreaterThan(0),
                () -> assertThat(resultado.getPeriodoAnalisado()).isEqualTo(30),
                () -> assertThat(resultado.getProdutos().get(0).getNome()).isEqualTo("Produto Teste")
        );
    }

    @Test
    void deveObterProdutosEstoqueBaixoDeterminarStatusCorretamente() {
        
        List<Produto> produtosEstoqueBaixo = Arrays.asList(
                
                Produto.builder()
                        .id(1L)
                        .nome("Produto Zerado")
                        .preco(BigDecimal.valueOf(99.99))
                        .departamento("Test")
                        .estoqueTotal(0)
                        .estoqueMinimo(10)
                        .build(),
                
                Produto.builder()
                        .id(2L)
                        .nome("Produto Crítico")
                        .preco(BigDecimal.valueOf(149.99))
                        .departamento("Test")
                        .estoqueTotal(3) 
                        .estoqueMinimo(10)
                        .build(),
                
                Produto.builder()
                        .id(3L)
                        .nome("Produto Baixo")
                        .preco(BigDecimal.valueOf(79.99))
                        .departamento("Test")
                        .estoqueTotal(8) 
                        .estoqueMinimo(10)
                        .build()
        );

        when(produtoRepository.findProdutosComEstoqueBaixo()).thenReturn(produtosEstoqueBaixo);

        DashboardDTO.EstoqueBaixoResponse resultado = dashboardService.obterEstoqueBaixo();

        assertAll("Validação estoque baixo",
                () -> assertThat(resultado.getProdutos()).hasSize(3),
                () -> assertThat(resultado.getTotalProdutosCriticos()).isEqualTo(3),
                () -> assertThat(resultado.getProdutos().get(0).getStatus()).isEqualTo("ZERADO"),
                () -> assertThat(resultado.getProdutos().get(1).getStatus()).isEqualTo("CRITICO"),
                () -> assertThat(resultado.getProdutos().get(2).getStatus()).isEqualTo("BAIXO")
        );
    }

    @Test
    void deveObterVendasRecentesComLimiteCorreto() {
        
        int limite = 10;
        List<Venda> vendasRecentes = Arrays.asList(
                vendaMock,
                Venda.builder()
                        .id(2L)
                        .clienteNome("Cliente 2")
                        .valorTotal(BigDecimal.valueOf(299.99))
                        .dataVenda(LocalDateTime.now().minusHours(2))
                        .status(StatusVenda.CONFIRMADA)
                        .build()
        );

        Page<Venda> pageVendas = new PageImpl<>(vendasRecentes);
        when(vendaRepository.findAllByOrderByDataVendaDesc(any(Pageable.class))).thenReturn(pageVendas);

        DashboardDTO.VendasRecentesResponse resultado = dashboardService.obterVendasRecentes(limite);

        assertAll("Validação vendas recentes",
                () -> assertThat(resultado.getVendas()).hasSize(2),
                () -> assertThat(resultado.getTotalVendas()).isEqualTo(2),
                () -> assertThat(resultado.getVendas().get(0).getNomeCliente()).isEqualTo("Cliente Teste")
        );

        verify(vendaRepository).findAllByOrderByDataVendaDesc(PageRequest.of(0, limite));
    }

    @Test
    void deveCalcularPercentualCrescimentoCorretamenteCenariosDiversos() {

        when(vendaRepository.sumFaturamentoPorDia(any()))
                .thenReturn(BigDecimal.ZERO) 
                .thenReturn(BigDecimal.valueOf(1000)); 

        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any()))
                .thenReturn(BigDecimal.valueOf(5000)) 
                .thenReturn(BigDecimal.valueOf(4000));

        when(vendaRepository.countByDataVendaBetween(any(), any())).thenReturn(10L);
        when(vendaRepository.countByStatusAndDataVendaBetween(any(), any(), any())).thenReturn(8L);

        DashboardDTO.PerformanceResumoResponse resultado = dashboardService.obterResumoPerformance();

        assertThat(resultado.getCrescimentoDiario()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void deveCalcularTaxaConversaoCorretamenteQuandoNaoHaVendas() {
        
        when(vendaRepository.sumFaturamentoPorDia(any())).thenReturn(BigDecimal.ZERO);
        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any())).thenReturn(BigDecimal.ZERO);
        when(vendaRepository.countByDataVendaBetween(any(), any())).thenReturn(0L);
        when(vendaRepository.countByStatusAndDataVendaBetween(any(), any(), any())).thenReturn(0L);

        DashboardDTO.PerformanceResumoResponse resultado = dashboardService.obterResumoPerformance();

        assertThat(resultado.getTaxaConversaoVendas()).isEqualTo(0.0);
    }

    @Test
    void deveProcessarEventosComValoresNulosNaPerformance() {
        
        Evento eventoSemMeta = Evento.builder()
                .id(2L)
                .nome("Evento Sem Meta")
                .dataInicio(LocalDate.now().plusDays(1))
                .dataFim(LocalDate.now().plusDays(5))
                .status(StatusEvento.PLANEJADO)
                .metaVendas(null) 
                .build();

        when(eventoRepository.findTopEventosPorVendasDashboard(any()))
                .thenReturn(Arrays.asList(eventoSemMeta));
        when(eventoRepository.calcularTotalVendasEvento(eventoSemMeta.getId()))
                .thenReturn(null); 
        when(eventoRepository.contarVendasEvento(eventoSemMeta.getId()))
                .thenReturn(null); 

        when(vendaRepository.contarVendasPorStatus()).thenReturn(Collections.emptyList());
        when(clienteRepository.contarClientesPorCategoria()).thenReturn(Collections.emptyList());
        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.emptyList());
        when(eventoRepository.contarEventosPorStatus()).thenReturn(Collections.emptyList());
        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(0L);
        when(eventoRepository.findEventosProximosAoFim(any(), any())).thenReturn(Collections.emptyList());
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(0L);

        DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

        assertAll("Validação com eventos com valores nulos",
                () -> assertThat(resultado.getTopEventos()).hasSize(1),
                () -> assertThat(resultado.getTopEventos().get(0).getTotalVendas())
                        .isEqualByComparingTo(BigDecimal.ZERO),
                () -> assertThat(resultado.getTopEventos().get(0).getQuantidadeVendas()).isEqualTo(0),
                () -> assertThat(resultado.getTopEventos().get(0).getProgressoMeta()).isEqualTo(0.0)
        );
    }

    @Test
    void deveCalcularDiasRestantesCorretamenteParaDiferentesDatas() {

        LocalDate dataPassado = LocalDate.now().minusDays(5);
        Evento eventoPassado = Evento.builder()
                .id(3L)
                .nome("Evento Passado")
                .dataInicio(dataPassado.minusDays(10))
                .dataFim(dataPassado)
                .status(StatusEvento.CONCLUIDO)
                .build();

        when(eventoRepository.findTopEventosPorVendasDashboard(any()))
                .thenReturn(Arrays.asList(eventoPassado));
        when(eventoRepository.calcularTotalVendasEvento(eventoPassado.getId()))
                .thenReturn(BigDecimal.valueOf(5000));
        when(eventoRepository.contarVendasEvento(eventoPassado.getId())).thenReturn(10);

        when(vendaRepository.contarVendasPorStatus()).thenReturn(Collections.emptyList());
        when(clienteRepository.contarClientesPorCategoria()).thenReturn(Collections.emptyList());
        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.emptyList());
        when(eventoRepository.contarEventosPorStatus()).thenReturn(Collections.emptyList());
        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(0L);
        when(eventoRepository.findEventosProximosAoFim(any(), any())).thenReturn(Collections.emptyList());
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(0L);

        DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

        assertThat(resultado.getTopEventos().get(0).getDiasRestantes()).isEqualTo(0);
    }

    @Test
    void deveGerarAlertasBaseadosEmDiferentesCondicoesSistema() {
        
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(8L); 
        when(eventoRepository.findEventosProximosAoFim(any(), any()))
                .thenReturn(Arrays.asList(eventoMock, eventoMock)); 
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(10L); 

        when(vendaRepository.contarVendasPorStatus()).thenReturn(Collections.emptyList());
        when(clienteRepository.contarClientesPorCategoria()).thenReturn(Collections.emptyList());
        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.emptyList());
        when(eventoRepository.contarEventosPorStatus()).thenReturn(Collections.emptyList());
        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());

        DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

        List<DashboardDTO.AlertaDTO> alertas = resultado.getAlertas();
        assertAll("Validação dos alertas gerados",
                () -> assertThat(alertas).hasSizeGreaterThanOrEqualTo(3),
                () -> assertThat(alertas.stream().anyMatch(a -> a.getTipo().equals("ESTOQUE"))).isTrue(),
                () -> assertThat(alertas.stream().anyMatch(a -> a.getTipo().equals("EVENTO"))).isTrue(),
                () -> assertThat(alertas.stream().anyMatch(a -> a.getTipo().equals("VENDA"))).isTrue(),
                () -> assertThat(alertas.stream().allMatch(a -> a.getDataAlerta() != null)).isTrue()
        );
    }
}
