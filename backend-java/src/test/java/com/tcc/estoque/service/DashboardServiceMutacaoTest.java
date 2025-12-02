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
class DashboardServiceMutacaoTest {

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
    private Produto produtoMock;
    private Evento eventoMock;
    private Venda vendaMock;

    @BeforeEach
    void setup() {
        vendedorMock = Usuario.builder()
                .id(1L)
                .nome("Vendedor Teste")
                .email("vendedor@teste.com")
                .tipoUsuario(TipoUsuario.VENDEDOR)
                .ativo(true)
                .build();

        produtoMock = Produto.builder()
                .id(1L)
                .nome("Produto Teste")
                .preco(BigDecimal.valueOf(100.00))
                .departamento("Eletrônicos")
                .estoqueTotal(5)
                .estoqueMinimo(10)
                .ativo(true)
                .build();

        eventoMock = Evento.builder()
                .id(1L)
                .nome("Evento Teste")
                .dataInicio(LocalDate.now().plusDays(1))
                .dataFim(LocalDate.now().plusDays(5))
                .status(StatusEvento.ATIVO)
                .metaVendas(BigDecimal.valueOf(5000))
                .build();

        vendaMock = Venda.builder()
                .id(1L)
                .clienteNome("Cliente Teste")
                .valorTotal(BigDecimal.valueOf(250.00))
                .dataVenda(LocalDateTime.now().minusHours(2))
                .status(StatusVenda.CONFIRMADA)
                .usuario(vendedorMock)
                .build();
    }

    @Test
    void mataMutanteDeveTratarFaturamentoMesNullCorretamente() {
        
        when(vendaRepository.countByStatus(any())).thenReturn(50L);
        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any())).thenReturn(null); 
        when(vendaRepository.sumFaturamentoHoje()).thenReturn(BigDecimal.valueOf(1000));
        when(produtoRepository.countByAtivoTrue()).thenReturn(100L);
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(5L);
        when(clienteRepository.countByAtivoTrue()).thenReturn(75L);
        when(usuarioRepository.countByAtivoTrue()).thenReturn(10L);
        when(eventoRepository.countByStatusAndAtivoTrue(any())).thenReturn(3L);
        when(clienteRepository.countClientesNovosHoje()).thenReturn(2L);
        when(vendaRepository.countByStatusAndDataVendaBetween(any(), any(), any())).thenReturn(20L);

        DashboardDTO.DadosGeraisResponse resultado = dashboardService.obterDadosGerais();

        assertThat(resultado.getFaturamentoMes()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void mataMutanteDeveTratarFaturamentoDiaNullCorretamente() {
        
        when(vendaRepository.countByStatus(any())).thenReturn(50L);
        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any())).thenReturn(BigDecimal.valueOf(25000));
        when(vendaRepository.sumFaturamentoHoje()).thenReturn(null); 
        when(produtoRepository.countByAtivoTrue()).thenReturn(100L);
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(5L);
        when(clienteRepository.countByAtivoTrue()).thenReturn(75L);
        when(usuarioRepository.countByAtivoTrue()).thenReturn(10L);
        when(eventoRepository.countByStatusAndAtivoTrue(any())).thenReturn(3L);
        when(clienteRepository.countClientesNovosHoje()).thenReturn(2L);
        when(vendaRepository.countByStatusAndDataVendaBetween(any(), any(), any())).thenReturn(20L);

        DashboardDTO.DadosGeraisResponse resultado = dashboardService.obterDadosGerais();

        assertThat(resultado.getFaturamentoDia()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void mataMutanteDeveCalcularPercentualMetaCorretamenteUsandoDivisao() {
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedorMock));
        when(vendaRepository.countByUsuarioIdAndStatus(1L, StatusVenda.CONFIRMADA)).thenReturn(10L);
        when(vendaRepository.sumFaturamentoPorVendedor(eq(1L), any(), any()))
                .thenReturn(BigDecimal.valueOf(5000)); 
        when(vendaRepository.countDistinctClientesByVendedor(eq(1L), any(), any())).thenReturn(8L);

        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any()))
                .thenReturn(Arrays.asList(new Object[]{vendedorMock, 10L, BigDecimal.valueOf(5000)}));
        when(itemVendaRepository.findTopProdutosPorVendedor(eq(1L), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(vendaRepository.sumFaturamentoPorVendedorEDia(eq(1L), any()))
                .thenReturn(BigDecimal.valueOf(200));
        when(vendaRepository.countByUsuarioIdAndDataVenda(eq(1L), any())).thenReturn(1L);

        DashboardDTO.DashboardVendedorResponse resultado = dashboardService.obterDashboardVendedor(1L);

        assertThat(resultado.getProgressoMeta()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void mataMutanteDeveCalcularMediaDiariaUsandoDivisaoPor7() {
        
        List<Venda> vendas = Arrays.asList(
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(1400))
                        .dataVenda(LocalDate.now().atTime(10, 0))
                        .build()
        );
        when(vendaRepository.findVendasPorPeriodo(any(), any())).thenReturn(vendas);

        DashboardDTO.VendasSemanaResponse resultado = dashboardService.obterVendasSemana();

        assertThat(resultado.getMediaDiaria()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
    }

    @Test
    void mataMutanteDeveUsarMultiplicacaoPor100ParaPercentuais() {
        
        when(vendaRepository.findVendasPorPeriodo(any(), any())).thenReturn(Arrays.asList(
                Venda.builder().valorTotal(BigDecimal.valueOf(30000)).dataVenda(LocalDate.now().atTime(10, 0)).build()
        ));

        when(vendaRepository.sumFaturamentoPorPeriodo(any(), any()))
                .thenReturn(BigDecimal.valueOf(30000)) 
                .thenReturn(BigDecimal.valueOf(25000)); 

        DashboardDTO.VendasMesResponse resultado = dashboardService.obterVendasMes();

        assertThat(resultado.getPercentualMeta()).isEqualByComparingTo(BigDecimal.valueOf(60.00));
    }

    @Test
    void mataMutanteDeveDeterminarStatusCriticoQuandoQuantidadeMenorIgualEstoqueMinimoDiv2() {
        
        Produto produtoCritico = Produto.builder()
                .id(1L)
                .nome("Produto Crítico")
                .preco(BigDecimal.valueOf(100))
                .departamento("Test")
                .estoqueTotal(5) 
                .estoqueMinimo(10)
                .build();

        when(produtoRepository.findProdutosComEstoqueBaixo()).thenReturn(Arrays.asList(produtoCritico));

        DashboardDTO.EstoqueBaixoResponse resultado = dashboardService.obterEstoqueBaixo();

        assertThat(resultado.getProdutos().get(0).getStatus()).isEqualTo("CRITICO");
    }

    @Test
    void mataMutanteDeveDeterminarStatusZeradoQuandoQuantidadeIgualZero() {
        
        Produto produtoZerado = Produto.builder()
                .id(1L)
                .nome("Produto Zerado")
                .preco(BigDecimal.valueOf(100))
                .departamento("Test")
                .estoqueTotal(0) 
                .estoqueMinimo(10)
                .build();

        when(produtoRepository.findProdutosComEstoqueBaixo()).thenReturn(Arrays.asList(produtoZerado));

        DashboardDTO.EstoqueBaixoResponse resultado = dashboardService.obterEstoqueBaixo();

        assertThat(resultado.getProdutos().get(0).getStatus()).isEqualTo("ZERADO");
    }

    @Test
    void mataMutanteDeveValidarCondicaoTotalVendasMaiorQueZeroParaTicketMedio() {
        
        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any()))
                .thenReturn(Arrays.asList(
                        new Object[]{vendedorMock, 1L, BigDecimal.valueOf(200)} 
                ));

        when(vendaRepository.contarVendasPorStatus()).thenReturn(Collections.emptyList());
        when(clienteRepository.contarClientesPorCategoria()).thenReturn(Collections.emptyList());
        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.emptyList());
        when(eventoRepository.contarEventosPorStatus()).thenReturn(Collections.emptyList());
        when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(eventoRepository.findTopEventosPorVendasDashboard(any())).thenReturn(Collections.emptyList());
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(0L);
        when(eventoRepository.findEventosProximosAoFim(any(), any())).thenReturn(Collections.emptyList());
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(0L);

        DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

        assertThat(resultado.getTopVendedores().get(0).getTicketMedio())
                .isEqualByComparingTo(BigDecimal.valueOf(200.00));
    }

    @Test
    void mataMutanteDeveValidarCondicaoTotalVendasIgualZeroParaTicketMedio() {
        
        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any()))
                .thenReturn(Arrays.asList(
                        new Object[]{vendedorMock, 0L, BigDecimal.ZERO} 
                ));

        when(vendaRepository.contarVendasPorStatus()).thenReturn(Collections.emptyList());
        when(clienteRepository.contarClientesPorCategoria()).thenReturn(Collections.emptyList());
        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.emptyList());
        when(eventoRepository.contarEventosPorStatus()).thenReturn(Collections.emptyList());
        when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(eventoRepository.findTopEventosPorVendasDashboard(any())).thenReturn(Collections.emptyList());
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(0L);
        when(eventoRepository.findEventosProximosAoFim(any(), any())).thenReturn(Collections.emptyList());
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(0L);

        DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

        assertThat(resultado.getTopVendedores().get(0).getTicketMedio())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void mataMutanteDeveValidarDataIsBeforeCorretamente() {
        
        LocalDate ontem = LocalDate.now().minusDays(1);
        Evento eventoTerminadoOntem = Evento.builder()
                .id(1L)
                .nome("Evento Terminado")
                .dataInicio(ontem.minusDays(5))
                .dataFim(ontem) 
                .status(StatusEvento.CONCLUIDO)
                .build();

        when(eventoRepository.findTopEventosPorVendasDashboard(any()))
                .thenReturn(Arrays.asList(eventoTerminadoOntem));
        when(eventoRepository.calcularTotalVendasEvento(1L)).thenReturn(BigDecimal.valueOf(3000));
        when(eventoRepository.contarVendasEvento(1L)).thenReturn(10);

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
    void mataMutanteDeveValidarDataIsAfterCorretamenteParaLoop() {
        
        LocalDate inicio = LocalDate.now().minusDays(2);
        LocalDate fim = LocalDate.now();
        
        List<Venda> vendas = Arrays.asList(
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(100))
                        .dataVenda(inicio.atTime(10, 0)) 
                        .build(),
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(150))
                        .dataVenda(fim.atTime(14, 0)) 
                        .build()
        );

        when(vendaRepository.findVendasPorPeriodo(any(), any())).thenReturn(vendas);

        DashboardDTO.VendasSemanaResponse resultado = dashboardService.obterVendasSemana();

        assertThat(resultado.getVendas()).hasSize(7);
    }

    @Test
    void mataMutanteDeveUsarFilterCorretamenteComGetAtivoTrue() {
        
        List<Produto> todosProdutos = Arrays.asList(
                Produto.builder().id(1L).nome("Produto Ativo 1").preco(BigDecimal.valueOf(100)).ativo(true).build(),
                Produto.builder().id(2L).nome("Produto Inativo").preco(BigDecimal.valueOf(200)).ativo(false).build(),
                Produto.builder().id(3L).nome("Produto Ativo 2").preco(BigDecimal.valueOf(150)).ativo(true).build()
        );

        when(produtoRepository.findAll()).thenReturn(todosProdutos);

        DashboardDTO.TopProdutosResponse resultado = dashboardService.obterTopProdutos(30);

        assertAll("Validação do filtro por produtos ativos",
                () -> assertThat(resultado.getProdutos()).hasSize(2),
                () -> assertThat(resultado.getProdutos().stream()
                        .allMatch(p -> p.getNome().contains("Ativo"))).isTrue()
        );
    }

    @Test
    void mataMutanteDeveUsarLimitCorretamenteNoStream() {
        
        List<Produto> muitosProdutos = Arrays.asList(
                Produto.builder().id(1L).nome("P1").preco(BigDecimal.valueOf(100)).ativo(true).build(),
                Produto.builder().id(2L).nome("P2").preco(BigDecimal.valueOf(100)).ativo(true).build(),
                Produto.builder().id(3L).nome("P3").preco(BigDecimal.valueOf(100)).ativo(true).build(),
                Produto.builder().id(4L).nome("P4").preco(BigDecimal.valueOf(100)).ativo(true).build(),
                Produto.builder().id(5L).nome("P5").preco(BigDecimal.valueOf(100)).ativo(true).build(),
                Produto.builder().id(6L).nome("P6").preco(BigDecimal.valueOf(100)).ativo(true).build(),
                Produto.builder().id(7L).nome("P7").preco(BigDecimal.valueOf(100)).ativo(true).build()
        );

        when(produtoRepository.findAll()).thenReturn(muitosProdutos);

        DashboardDTO.TopProdutosResponse resultado = dashboardService.obterTopProdutos(30);

        assertThat(resultado.getProdutos()).hasSize(5);
    }

    @Test
    void mataMutanteDeveUsarReduceCorretamenteParaSomarValores() {
        
        List<Venda> vendas = Arrays.asList(
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(100))
                        .dataVenda(LocalDate.now().atTime(10, 0))
                        .build(),
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(250))
                        .dataVenda(LocalDate.now().minusDays(1).atTime(15, 0))
                        .build(),
                Venda.builder()
                        .valorTotal(BigDecimal.valueOf(150))
                        .dataVenda(LocalDate.now().minusDays(2).atTime(12, 0))
                        .build()
        );

        when(vendaRepository.findVendasPorPeriodo(any(), any())).thenReturn(vendas);

        DashboardDTO.VendasSemanaResponse resultado = dashboardService.obterVendasSemana();

        assertThat(resultado.getTotalSemana()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void mataMutanteDeveGerarAlertaQuandoProdutosEstoqueBaixoMaiorQueZero() {
        
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(1L);
        when(eventoRepository.findEventosProximosAoFim(any(), any())).thenReturn(Collections.emptyList());
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(0L);

        when(vendaRepository.contarVendasPorStatus()).thenReturn(Collections.emptyList());
        when(clienteRepository.contarClientesPorCategoria()).thenReturn(Collections.emptyList());
        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.emptyList());
        when(eventoRepository.contarEventosPorStatus()).thenReturn(Collections.emptyList());
        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(eventoRepository.findTopEventosPorVendasDashboard(any())).thenReturn(Collections.emptyList());

        DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

        List<DashboardDTO.AlertaDTO> alertasEstoque = resultado.getAlertas().stream()
                .filter(a -> a.getTipo().equals("ESTOQUE"))
                .toList();
        assertThat(alertasEstoque).hasSize(1);
        assertThat(alertasEstoque.get(0).getMensagem()).contains("1 produto(s)");
    }

    @Test
    void mataMutanteDeveGerarAlertaQuandoVendasPendentesMaiorQue5() {
        
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(0L);
        when(eventoRepository.findEventosProximosAoFim(any(), any())).thenReturn(Collections.emptyList());
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(6L);

        when(vendaRepository.contarVendasPorStatus()).thenReturn(Collections.emptyList());
        when(clienteRepository.contarClientesPorCategoria()).thenReturn(Collections.emptyList());
        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.emptyList());
        when(eventoRepository.contarEventosPorStatus()).thenReturn(Collections.emptyList());
        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(eventoRepository.findTopEventosPorVendasDashboard(any())).thenReturn(Collections.emptyList());

        DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

        List<DashboardDTO.AlertaDTO> alertasVenda = resultado.getAlertas().stream()
                .filter(a -> a.getTipo().equals("VENDA"))
                .toList();
        assertThat(alertasVenda).hasSize(1);
        assertThat(alertasVenda.get(0).getMensagem()).contains("6 venda(s)");
    }

    @Test
    void mataMutanteDeveGerarAlertaQuandoDiaDoMesMaiorQue20() {

        LocalDate hoje = LocalDate.now();
        
        if (hoje.getDayOfMonth() > 20) {
            
            when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(0L);
            when(eventoRepository.findEventosProximosAoFim(any(), any())).thenReturn(Collections.emptyList());
            when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(0L);

            when(vendaRepository.contarVendasPorStatus()).thenReturn(Collections.emptyList());
            when(clienteRepository.contarClientesPorCategoria()).thenReturn(Collections.emptyList());
            when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.emptyList());
            when(eventoRepository.contarEventosPorStatus()).thenReturn(Collections.emptyList());
            when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
            when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
            when(eventoRepository.findTopEventosPorVendasDashboard(any())).thenReturn(Collections.emptyList());

            DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

            List<DashboardDTO.AlertaDTO> alertasMeta = resultado.getAlertas().stream()
                    .filter(a -> a.getTipo().equals("META"))
                    .toList();
            assertThat(alertasMeta).hasSize(1);
            assertThat(alertasMeta.get(0).getMensagem()).contains("meta mensal");
        } else {
            
            assertThat(hoje.getDayOfMonth()).isLessThanOrEqualTo(20);
        }
    }

    @Test
    void mataMutanteDeveRetornarPosicaoCorretaNoRanking() {
        
        Usuario vendedor1 = Usuario.builder().id(2L).nome("Primeiro").build();
        Usuario vendedor2 = vendedorMock; 
        Usuario vendedor3 = Usuario.builder().id(3L).nome("Terceiro").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedorMock));
        when(vendaRepository.countByUsuarioIdAndStatus(1L, StatusVenda.CONFIRMADA)).thenReturn(8L);
        when(vendaRepository.sumFaturamentoPorVendedor(eq(1L), any(), any())).thenReturn(BigDecimal.valueOf(4000));
        when(vendaRepository.countDistinctClientesByVendedor(eq(1L), any(), any())).thenReturn(5L);

        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Arrays.asList(
                new Object[]{vendedor1, 15L, BigDecimal.valueOf(8000)}, 
                new Object[]{vendedor2, 8L, BigDecimal.valueOf(4000)},  
                new Object[]{vendedor3, 5L, BigDecimal.valueOf(2000)}   
        ));

        when(itemVendaRepository.findTopProdutosPorVendedor(eq(1L), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(vendaRepository.sumFaturamentoPorVendedorEDia(eq(1L), any())).thenReturn(BigDecimal.ZERO);
        when(vendaRepository.countByUsuarioIdAndDataVenda(eq(1L), any())).thenReturn(0L);

        DashboardDTO.DashboardVendedorResponse resultado = dashboardService.obterDashboardVendedor(1L);

        assertThat(resultado.getPosicaoRanking()).isEqualTo(2);
    }

    @Test
    void mataMutanteDeveRetornarUltimaPosicaoQuandoVendedorNaoEncontradoNoRanking() {
        
        Usuario outroVendedor = Usuario.builder().id(999L).nome("Outro").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedorMock));
        when(vendaRepository.countByUsuarioIdAndStatus(1L, StatusVenda.CONFIRMADA)).thenReturn(1L);
        when(vendaRepository.sumFaturamentoPorVendedor(eq(1L), any(), any())).thenReturn(BigDecimal.valueOf(100));
        when(vendaRepository.countDistinctClientesByVendedor(eq(1L), any(), any())).thenReturn(1L);

        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Arrays.asList(
                new Object[]{outroVendedor, 10L, BigDecimal.valueOf(5000)},
                new Object[]{Usuario.builder().id(888L).build(), 8L, BigDecimal.valueOf(3000)}
        ));

        when(itemVendaRepository.findTopProdutosPorVendedor(eq(1L), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(vendaRepository.sumFaturamentoPorVendedorEDia(eq(1L), any())).thenReturn(BigDecimal.ZERO);
        when(vendaRepository.countByUsuarioIdAndDataVenda(eq(1L), any())).thenReturn(0L);

        DashboardDTO.DashboardVendedorResponse resultado = dashboardService.obterDashboardVendedor(1L);

        assertThat(resultado.getPosicaoRanking()).isEqualTo(3);
    }

    @Test
    void mataMutanteDeveProcessarListaVaziaEventosProximosAoFim() {
        
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(0L);
        when(eventoRepository.findEventosProximosAoFim(any(), any()))
                .thenReturn(Collections.emptyList()); 
        when(vendaRepository.countByStatus(StatusVenda.PENDENTE)).thenReturn(0L);

        when(vendaRepository.contarVendasPorStatus()).thenReturn(Collections.emptyList());
        when(clienteRepository.contarClientesPorCategoria()).thenReturn(Collections.emptyList());
        when(usuarioRepository.contarUsuariosPorTipo()).thenReturn(Collections.emptyList());
        when(eventoRepository.contarEventosPorStatus()).thenReturn(Collections.emptyList());
        when(vendaRepository.findTopVendedoresPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(itemVendaRepository.findTopProdutosPorPeriodo(any(), any(), any())).thenReturn(Collections.emptyList());
        when(eventoRepository.findTopEventosPorVendasDashboard(any())).thenReturn(Collections.emptyList());

        DashboardDTO.EstatisticasGeraisResponse resultado = dashboardService.obterEstatisticasGerenciais();

        List<DashboardDTO.AlertaDTO> alertasEvento = resultado.getAlertas().stream()
                .filter(a -> a.getTipo().equals("EVENTO"))
                .toList();
        assertThat(alertasEvento).isEmpty();
    }

    @Test
    void mataMutanteDeveProcessarInteirosNulosCorretamente() {
        
        when(eventoRepository.findTopEventosPorVendasDashboard(any()))
                .thenReturn(Arrays.asList(eventoMock));
        when(eventoRepository.calcularTotalVendasEvento(eventoMock.getId()))
                .thenReturn(BigDecimal.valueOf(2000));
        when(eventoRepository.contarVendasEvento(eventoMock.getId()))
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

        assertThat(resultado.getTopEventos().get(0).getQuantidadeVendas()).isEqualTo(0);
    }
}
