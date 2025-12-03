package com.tcc.estoque.service;

import com.tcc.estoque.dto.RelatorioDTO;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.model.enums.StatusVenda;
import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.VendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class RelatorioServiceTest {

    @Autowired
    private RelatorioService relatorioService;

    @Test
    void testGerarRelatorioVendas() {
        LocalDate inicio = LocalDate.now().minusDays(30);
        LocalDate fim = LocalDate.now();
        RelatorioDTO.RelatorioVendasResponse relatorio = relatorioService.gerarRelatorioVendas(inicio, fim);
        assertThat(relatorio).isNotNull();
        assertThat(relatorio.getTotalVendas()).isGreaterThanOrEqualTo(0);
        assertThat(relatorio.getTotalFaturamento()).isNotNull();
        assertThat(relatorio.getTicketMedio()).isNotNull();
    }

    @Test
    void testGerarRelatorioEstoque() {
        RelatorioDTO.RelatorioEstoqueResponse estoque = relatorioService.gerarRelatorioEstoque();
        assertThat(estoque).isNotNull();
        assertThat(estoque.getTotalProdutos()).isGreaterThanOrEqualTo(0);
        assertThat(estoque.getProdutosAtivos()).isGreaterThanOrEqualTo(0);
        assertThat(estoque.getProdutosEstoqueBaixo()).isNotNull();
    }

    @Test
    void testGerarRelatorioVendasDetalhadas() {
        LocalDate inicio = LocalDate.now().minusDays(30);
        LocalDate fim = LocalDate.now();
        RelatorioDTO.VendasDetalhadasResponse detalhado = relatorioService.gerarRelatorioVendasDetalhadas(inicio, fim);
        assertThat(detalhado).isNotNull();
        assertThat(detalhado.getVendas()).isNotNull();
    }

    @Test
    void testObterDadosGraficoVendas() {
        LocalDate inicio = LocalDate.now().minusDays(7);
        LocalDate fim = LocalDate.now();
        RelatorioDTO.GraficoVendasResponse grafico = relatorioService.obterDadosGraficoVendas(inicio, fim);
        assertThat(grafico).isNotNull();
        assertThat(grafico.getEvolucaoVendas()).isNotNull();
    }

    // Testes unitários com mocks (migrados de RelatorioServiceUnitTest)
    @Test
    void gerarRelatorioVendasSemVendas() {
        VendaRepository vendaRepo = Mockito.mock(VendaRepository.class);
        ProdutoRepository produtoRepo = Mockito.mock(ProdutoRepository.class);

        Mockito.when(vendaRepo.findVendasPorPeriodo(Mockito.any(), Mockito.any())).thenReturn(List.of());

        RelatorioService service = new RelatorioService(vendaRepo, produtoRepo);

        RelatorioDTO.RelatorioVendasResponse resp = service.gerarRelatorioVendas(LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(0L, resp.getTotalVendas());
        assertEquals(BigDecimal.ZERO, resp.getTotalFaturamento());
        assertEquals(BigDecimal.ZERO, resp.getTicketMedio());
    }

    @Test
    void gerarRelatorioEstoqueComProdutos() {
        VendaRepository vendaRepo = Mockito.mock(VendaRepository.class);
        ProdutoRepository produtoRepo = Mockito.mock(ProdutoRepository.class);

        Produto p1 = Produto.builder().id(1L).nome("A").preco(BigDecimal.valueOf(10)).estoque(2).estoqueMinimo(5).ativo(true).codigo("X").build();
        Produto p2 = Produto.builder().id(2L).nome("B").preco(BigDecimal.valueOf(5)).estoque(10).estoqueMinimo(3).ativo(true).codigo("Y").build();

        Mockito.when(produtoRepo.findAll()).thenReturn(List.of(p1, p2));
        Mockito.when(produtoRepo.countProdutosComEstoqueBaixo()).thenReturn(1L);
        Mockito.when(produtoRepo.calcularValorTotalEstoque()).thenReturn(BigDecimal.valueOf(10*2 + 5*10));

        RelatorioService service = new RelatorioService(vendaRepo, produtoRepo);

        RelatorioDTO.RelatorioEstoqueResponse resp = service.gerarRelatorioEstoque();

        assertEquals(2L, resp.getTotalProdutos());
        assertEquals(2L, resp.getProdutosAtivos());
        assertEquals(1L, resp.getProdutosEstoqueBaixo());
    }

    // ===== NOVOS TESTES PARA MELHORAR COBERTURA =====

    @Mock
    private VendaRepository vendaRepositoryMock;
    
    @Mock
    private ProdutoRepository produtoRepositoryMock;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGerarDashboardExecutivo() {
        // Given
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        
        when(vendaRepositoryMock.countByDataVendaBetweenAndStatus(eq(inicioMes), eq(hoje), eq(StatusVenda.CONFIRMADA)))
                .thenReturn(50L);
        when(vendaRepositoryMock.sumValorTotalByDataVendaBetweenAndStatus(eq(inicioMes), any(LocalDate.class), eq(StatusVenda.CONFIRMADA)))
                .thenReturn(BigDecimal.valueOf(15000.00));
        when(vendaRepositoryMock.countByDataVendaAndStatus(eq(hoje), any(LocalDate.class), eq(StatusVenda.CONFIRMADA)))
                .thenReturn(5L);
        when(vendaRepositoryMock.sumValorTotalByDataVendaAndStatus(eq(hoje), any(LocalDate.class), eq(StatusVenda.CONFIRMADA)))
                .thenReturn(BigDecimal.valueOf(1200.00));
        when(vendaRepositoryMock.countByStatus(StatusVenda.PENDENTE))
                .thenReturn(8L);
        when(produtoRepositoryMock.countProdutosComEstoqueBaixo())
                .thenReturn(12L);
        when(produtoRepositoryMock.countByAtivoTrue())
                .thenReturn(150L);
        
        RelatorioService service = new RelatorioService(vendaRepositoryMock, produtoRepositoryMock);

        // When
        RelatorioDTO.DashboardExecutivoResponse resultado = service.gerarDashboardExecutivo();

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getVendasMes()).isEqualTo(50L);
        assertThat(resultado.getFaturamentoMes()).isEqualByComparingTo(BigDecimal.valueOf(15000.00));
        assertThat(resultado.getVendasHoje()).isEqualTo(5L);
        assertThat(resultado.getFaturamentoHoje()).isEqualByComparingTo(BigDecimal.valueOf(1200.00));
        assertThat(resultado.getVendasPendentes()).isEqualTo(8L);
        assertThat(resultado.getProdutosEstoqueBaixo()).isEqualTo(12L);
        assertThat(resultado.getTotalProdutos()).isEqualTo(150L);
        assertThat(resultado.getMovimentacoesHoje()).isEqualTo(0L);
        assertThat(resultado.getUltimaAtualizacao()).isNotNull();
    }

    @Test
    void testGerarDashboardExecutivoComErros() {
        // Given - simulando erros nas consultas
        when(vendaRepositoryMock.countByDataVendaBetweenAndStatus(any(), any(), any()))
                .thenThrow(new RuntimeException("Erro de BD"));
        when(vendaRepositoryMock.sumValorTotalByDataVendaBetweenAndStatus(any(), any(), any()))
                .thenThrow(new RuntimeException("Erro de BD"));
        when(vendaRepositoryMock.countByDataVendaAndStatus(any(), any(), any()))
                .thenThrow(new RuntimeException("Erro de BD"));
        when(vendaRepositoryMock.sumValorTotalByDataVendaAndStatus(any(), any(), any()))
                .thenThrow(new RuntimeException("Erro de BD"));
        when(vendaRepositoryMock.countByStatus(any()))
                .thenThrow(new RuntimeException("Erro de BD"));
        when(produtoRepositoryMock.countProdutosComEstoqueBaixo())
                .thenThrow(new RuntimeException("Erro de BD"));
        when(produtoRepositoryMock.countByAtivoTrue())
                .thenThrow(new RuntimeException("Erro de BD"));

        RelatorioService service = new RelatorioService(vendaRepositoryMock, produtoRepositoryMock);

        // When
        RelatorioDTO.DashboardExecutivoResponse resultado = service.gerarDashboardExecutivo();

        // Then - deve retornar valores padrão mesmo com erros
        assertThat(resultado).isNotNull();
        assertThat(resultado.getVendasMes()).isEqualTo(0L);
        assertThat(resultado.getFaturamentoMes()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getVendasHoje()).isEqualTo(0L);
        assertThat(resultado.getFaturamentoHoje()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getVendasPendentes()).isEqualTo(0L);
        assertThat(resultado.getProdutosEstoqueBaixo()).isEqualTo(0L);
        assertThat(resultado.getTotalProdutos()).isEqualTo(0L);
    }

    @Test
    void testGerarRelatorioProdutos() {
        // Given
        Produto produto1 = Produto.builder()
                .id(1L)
                .nome("Produto A")
                .departamento("Categoria 1")
                .preco(BigDecimal.valueOf(50.00))
                .estoque(25)
                .ativo(true)
                .build();
        
        Produto produto2 = Produto.builder()
                .id(2L)
                .nome("Produto B")
                .departamento("Categoria 2")
                .preco(BigDecimal.valueOf(30.00))
                .estoque(10)
                .ativo(true)
                .build();
        
        Produto produto3 = Produto.builder()
                .id(3L)
                .nome("Produto Inativo")
                .departamento("Categoria 1")
                .preco(BigDecimal.valueOf(20.00))
                .estoque(5)
                .ativo(false)
                .build();
        
        List<Produto> todosProdutos = Arrays.asList(produto1, produto2, produto3);
        
        when(produtoRepositoryMock.findAll()).thenReturn(todosProdutos);
        when(produtoRepositoryMock.countProdutosComEstoqueBaixo()).thenReturn(1L);
        when(produtoRepositoryMock.calcularValorTotalEstoque()).thenReturn(BigDecimal.valueOf(1850.00));
        
        RelatorioService service = new RelatorioService(vendaRepositoryMock, produtoRepositoryMock);

        // When
        RelatorioDTO.RelatorioProdutosResponse resultado = service.gerarRelatorioProdutos();

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getResumo()).isNotNull();
        assertThat(resultado.getResumo().getTotalProdutos()).isEqualTo(3L);
        assertThat(resultado.getResumo().getProdutosAtivos()).isEqualTo(2L);
        assertThat(resultado.getResumo().getTotalCategorias()).isEqualTo(2L);
        assertThat(resultado.getResumo().getValorEstoque()).isEqualByComparingTo(BigDecimal.valueOf(1850.00));
        assertThat(resultado.getResumo().getValorMedioUnitario()).isNotNull();
        
        assertThat(resultado.getEstoque()).isNotNull();
        assertThat(resultado.getEstoque().getProdutosEstoqueBaixo()).isEqualTo(1L);
        assertThat(resultado.getEstoque().getEstoqueTotal()).isEqualTo(35L);
        assertThat(resultado.getEstoque().getValorTotalEstoque()).isEqualByComparingTo(BigDecimal.valueOf(1850.00));
        assertThat(resultado.getEstoque().getAlertasCriticos()).isEqualTo(1);
        
        assertThat(resultado.getProdutos()).isNotNull();
        assertThat(resultado.getProdutos()).hasSize(2); // apenas produtos ativos
        assertThat(resultado.getGeradoEm()).isNotNull();
    }

    @Test
    void testGerarRelatorioProdutosComListaVazia() {
        // Given
        when(produtoRepositoryMock.findAll()).thenReturn(List.of());
        when(produtoRepositoryMock.countProdutosComEstoqueBaixo()).thenReturn(0L);
        when(produtoRepositoryMock.calcularValorTotalEstoque()).thenReturn(null);
        
        RelatorioService service = new RelatorioService(vendaRepositoryMock, produtoRepositoryMock);

        // When
        RelatorioDTO.RelatorioProdutosResponse resultado = service.gerarRelatorioProdutos();

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getResumo().getTotalProdutos()).isEqualTo(0L);
        assertThat(resultado.getResumo().getProdutosAtivos()).isEqualTo(0L);
        assertThat(resultado.getResumo().getValorEstoque()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getResumo().getValorMedioUnitario()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getProdutos()).isEmpty();
    }

    @Test
    void testGerarRelatorioClientes() {
        // Given
        RelatorioService service = new RelatorioService(vendaRepositoryMock, produtoRepositoryMock);

        // When
        RelatorioDTO.RelatorioClientesResponse resultado = service.gerarRelatorioClientes();

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getGeradoEm()).isNotNull();
        // O método atualmente retorna apenas uma resposta básica
    }

    @Test
    void testGerarRelatorioMovimentacao() {
        // Given
        LocalDate inicio = LocalDate.now().minusDays(30);
        LocalDate fim = LocalDate.now();
        RelatorioService service = new RelatorioService(vendaRepositoryMock, produtoRepositoryMock);

        // When
        RelatorioDTO.RelatorioMovimentacaoResponse resultado = service.gerarRelatorioMovimentacao(inicio, fim);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getTotalMovimentacoes()).isEqualTo(0L);
        assertThat(resultado.getTotalEntradas()).isEqualTo(0L);
        assertThat(resultado.getTotalSaidas()).isEqualTo(0L);
        // O método atualmente retorna apenas valores padrão
    }

    @Test 
    void testGerarDashboardExecutivoComValoresNulos() {
        // Given - simulando retornos nulos
        when(vendaRepositoryMock.countByDataVendaBetweenAndStatus(any(), any(), any()))
                .thenReturn(10L);
        when(vendaRepositoryMock.sumValorTotalByDataVendaBetweenAndStatus(any(), any(), any()))
                .thenReturn(null); // valor nulo
        when(vendaRepositoryMock.countByDataVendaAndStatus(any(), any(), any()))
                .thenReturn(2L);
        when(vendaRepositoryMock.sumValorTotalByDataVendaAndStatus(any(), any(), any()))
                .thenReturn(null); // valor nulo
        when(vendaRepositoryMock.countByStatus(any()))
                .thenReturn(3L);
        when(produtoRepositoryMock.countProdutosComEstoqueBaixo())
                .thenReturn(5L);
        when(produtoRepositoryMock.countByAtivoTrue())
                .thenReturn(100L);
        
        RelatorioService service = new RelatorioService(vendaRepositoryMock, produtoRepositoryMock);

        // When
        RelatorioDTO.DashboardExecutivoResponse resultado = service.gerarDashboardExecutivo();

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getVendasMes()).isEqualTo(10L);
        assertThat(resultado.getFaturamentoMes()).isEqualByComparingTo(BigDecimal.ZERO); // convertido para ZERO
        assertThat(resultado.getVendasHoje()).isEqualTo(2L);
        assertThat(resultado.getFaturamentoHoje()).isEqualByComparingTo(BigDecimal.ZERO); // convertido para ZERO
    }
}


