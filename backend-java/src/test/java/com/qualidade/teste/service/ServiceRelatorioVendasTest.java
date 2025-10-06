package com.qualidade.teste.service;

import com.qualidade.teste.model.Cliente;
import com.qualidade.teste.model.Produto;
import com.qualidade.teste.model.Venda;
import com.qualidade.teste.model.ItemVenda;
import com.qualidade.teste.service.ServiceRelatorioVendas.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ServiceRelatorioVendasTest {

    private ServiceRelatorioVendas servico;
    private Cliente clienteBronze;
    private Cliente clienteOuro;
    private Cliente clientePremium;
    private Produto produtoRoupa;
    private Produto produtoEletronico;
    private Produto produtoCalcado;
    private List<Venda> vendasMockadas;

    @BeforeEach
    void setup() {
        servico = new ServiceRelatorioVendas();
        
        clienteBronze = new Cliente(1L, "João Bronze", "123.456.789-00", "joao@email.com", Cliente.TipoCliente.BRONZE);
        clienteBronze.setPontosFidelidade(100);
        clienteBronze.setDataAniversario(LocalDate.of(1985, 5, 15));
        clienteBronze.setDataUltimaCompra(LocalDate.now().minusDays(30));
        
        clienteOuro = new Cliente(2L, "Maria Ouro", "987.654.321-00", "maria@email.com", Cliente.TipoCliente.OURO);
        clienteOuro.setPontosFidelidade(2500);
        clienteOuro.setDataAniversario(LocalDate.of(1990, 8, 22));
        clienteOuro.setDataUltimaCompra(LocalDate.now().minusDays(10));
        
        clientePremium = new Cliente(3L, "Carlos Premium", "555.666.777-88", "carlos@email.com", Cliente.TipoCliente.PREMIUM);
        clientePremium.setPontosFidelidade(12000);
        clientePremium.setDataAniversario(LocalDate.of(1980, 12, 5));
        clientePremium.setDataUltimaCompra(LocalDate.now().minusDays(5));
        
        produtoRoupa = new Produto(1L, "Camiseta Premium", new BigDecimal("89.90"), Produto.Categoria.ROUPAS, 50);
        produtoRoupa.setPercentualMaximoDesconto(new BigDecimal("30"));
        produtoRoupa.setAtivo(true);
        
        produtoEletronico = new Produto(2L, "Smartphone XYZ", new BigDecimal("1200.00"), Produto.Categoria.ELETRONICOS, 20);
        produtoEletronico.setPercentualMaximoDesconto(new BigDecimal("15"));
        produtoEletronico.setAtivo(true);
        
        produtoCalcado = new Produto(3L, "Tênis Esportivo", new BigDecimal("250.00"), Produto.Categoria.CALCADOS, 30);
        produtoCalcado.setPercentualMaximoDesconto(new BigDecimal("25"));
        produtoCalcado.setAtivo(true);
        
        criarVendasMockadas();
    }

    private void criarVendasMockadas() {
        Venda venda1 = new Venda(clienteBronze, Arrays.asList(
            new ItemVenda(produtoRoupa, 2, produtoRoupa.getPreco())
        ));
        venda1.setId(1L);
        venda1.setDataVenda(LocalDateTime.now().minusDays(30));
        venda1.setValorTotal(produtoRoupa.getPreco().multiply(new BigDecimal("2")));
        venda1.setStatus(Venda.StatusVenda.FINALIZADA);
        
        Venda venda2 = new Venda(clienteOuro, Arrays.asList(
            new ItemVenda(produtoEletronico, 1, produtoEletronico.getPreco())
        ));
        venda2.setId(2L);
        venda2.setDataVenda(LocalDateTime.now().minusDays(15));
        venda2.setValorTotal(produtoEletronico.getPreco());
        venda2.setStatus(Venda.StatusVenda.FINALIZADA);
        
        Venda venda3 = new Venda(clientePremium, Arrays.asList(
            new ItemVenda(produtoCalcado, 3, produtoCalcado.getPreco())
        ));
        venda3.setId(3L);
        venda3.setDataVenda(LocalDateTime.now().minusDays(5));
        venda3.setValorTotal(produtoCalcado.getPreco().multiply(new BigDecimal("3")));
        venda3.setStatus(Venda.StatusVenda.FINALIZADA);
        
        vendasMockadas = Arrays.asList(venda1, venda2, venda3);
    }
    
    @Test
    void testGerarRelatorioVendasPeriodoValido() {
        LocalDate dataInicio = LocalDate.now().minusDays(40);
        LocalDate dataFim = LocalDate.now();
        
        RelatorioVendasPeriodo relatorio = servico.gerarRelatorioVendasPeriodo(dataInicio, dataFim);
        
        assertThat(relatorio).isNotNull();
        assertThat(relatorio.getDataInicio()).isEqualTo(dataInicio);
        assertThat(relatorio.getDataFim()).isEqualTo(dataFim);
        assertThat(relatorio.getFaturamentoTotal()).isGreaterThan(BigDecimal.ZERO);
        assertThat(relatorio.getTotalVendas()).isGreaterThan(0);
        assertThat(relatorio.getTicketMedio()).isGreaterThan(BigDecimal.ZERO);
    }
    
    @Test
    void testGerarRelatorioComDataInicioNula() {
        LocalDate dataFim = LocalDate.now();
        
        assertThatThrownBy(() -> servico.gerarRelatorioVendasPeriodo(null, dataFim))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Período inválido");
    }
    
    @Test
    void testGerarRelatorioComDataFimNula() {
        LocalDate dataInicio = LocalDate.now().minusDays(30);
        
        assertThatThrownBy(() -> servico.gerarRelatorioVendasPeriodo(dataInicio, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Período inválido");
    }
    
    @Test
    void testGerarRelatorioComDataInicioMaiorQueFim() {
        LocalDate dataInicio = LocalDate.now();
        LocalDate dataFim = LocalDate.now().minusDays(30);
        
        assertThatThrownBy(() -> servico.gerarRelatorioVendasPeriodo(dataInicio, dataFim))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Período inválido");
    }
    
    @Test
    void testGerarRelatorioPeriodoSemVendas() {
        LocalDate dataInicio = LocalDate.now().minusYears(2);
        LocalDate dataFim = LocalDate.now().minusYears(2).plusDays(30);
        
        RelatorioVendasPeriodo relatorio = servico.gerarRelatorioVendasPeriodo(dataInicio, dataFim);
        
        assertThat(relatorio.getFaturamentoTotal()).isEqualTo(BigDecimal.ZERO);
        assertThat(relatorio.getTotalVendas()).isEqualTo(0);
        assertThat(relatorio.getTicketMedio()).isEqualTo(BigDecimal.ZERO);
    }
    
    @Test
    void testCalculoCrescimentoPercentual() {
        LocalDate dataInicio = LocalDate.now().minusDays(30);
        LocalDate dataFim = LocalDate.now().minusDays(15);
        
        RelatorioVendasPeriodo relatorio = servico.gerarRelatorioVendasPeriodo(dataInicio, dataFim);
        
        assertThat(relatorio.getCrescimentoPercentual()).isNotNull();
    }
    
    @Test
    void testAnalisarComportamentoClienteValido() {
        Long clienteId = 1L; // Usa ID que existe nos dados mockados da classe
        
        AnaliseComportamentoCliente analise = servico.analisarComportamentoCliente(clienteId);
        
        assertThat(analise).isNotNull();
        assertThat(analise.getCliente()).isNotNull();
        assertThat(analise.getCliente().getId()).isEqualTo(clienteId);
        assertThat(analise.getTotalGasto()).isGreaterThan(BigDecimal.ZERO);
        assertThat(analise.getFrequenciaCompras()).isGreaterThan(0);
        assertThat(analise.getTicketMedio()).isGreaterThan(BigDecimal.ZERO);
    }
    
    @Test
    void testAnalisarComportamentoClienteInexistente() {
        Long clienteIdInexistente = 999L;
        
        assertThatThrownBy(() -> servico.analisarComportamentoCliente(clienteIdInexistente))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cliente não encontrado");
    }
    
    @Test
    void testAnalisarComportamentoClienteNulo() {
        assertThatThrownBy(() -> servico.analisarComportamentoCliente(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void testCalculoTicketMedioCliente() {
        Long clienteId = 1L; // Usa ID que tem vendas nos dados mockados da classe
        
        AnaliseComportamentoCliente analise = servico.analisarComportamentoCliente(clienteId);
        
        assertThat(analise.getTicketMedio()).isGreaterThan(BigDecimal.ZERO);
        BigDecimal ticketEsperado = analise.getTotalGasto()
            .divide(new BigDecimal(analise.getFrequenciaCompras()), 2, java.math.RoundingMode.HALF_UP);
        assertThat(analise.getTicketMedio()).isEqualTo(ticketEsperado);
    }
    
    @Test
    void testIdentificacaoCategoriasPreferidas() {
        Long clienteId = 1L; // Usa ID que existe nos dados mockados da classe
        
        AnaliseComportamentoCliente analise = servico.analisarComportamentoCliente(clienteId);
        
        assertThat(analise.getCategoriasPreferidas()).isNotNull();
        assertThat(analise.getCategoriasPreferidas()).isNotEmpty();
        assertThat(analise.getCategoriasPreferidas()).contains(Produto.Categoria.ROUPAS);
    }
    
    @Test
    void testIdentificarProdutosBaixaPerformanceValido() {
        int numeroDias = 30;
        
        List<ProdutoBaixaPerformance> produtos = servico.identificarProdutosBaixaPerformance(numeroDias);
        
        assertThat(produtos).isNotNull();
    }
    
    @Test
    void testIdentificarProdutosBaixaPerformanceComDiasInvalidos() {
        assertThatThrownBy(() -> servico.identificarProdutosBaixaPerformance(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Número de dias deve ser positivo");
        
        assertThatThrownBy(() -> servico.identificarProdutosBaixaPerformance(-10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Número de dias deve ser positivo");
    }
    
    @Test
    void testOrdenacaoProdutosBaixaPerformance() {
        int numeroDias = 60;
        
        List<ProdutoBaixaPerformance> produtos = servico.identificarProdutosBaixaPerformance(numeroDias);
        
        if (produtos.size() > 1) {
            for (int i = 0; i < produtos.size() - 1; i++) {
                assertThat(produtos.get(i).getDiasSemVenda())
                    .isGreaterThanOrEqualTo(produtos.get(i + 1).getDiasSemVenda());
            }
        }
    }
    
    @Test
    void testSugestaoAcaoProdutosBaixaPerformance() {
        int numeroDias = 90;
        
        List<ProdutoBaixaPerformance> produtos = servico.identificarProdutosBaixaPerformance(numeroDias);
        
        for (ProdutoBaixaPerformance produto : produtos) {
            assertThat(produto.getSugestaoAcao()).isNotNull();
            assertThat(produto.getSugestaoAcao()).isNotEmpty();
        }
    }
    
    @Test
    void testCalcularSazonalidadeVendas() {
        AnaliseSazonalidade analise = servico.calcularSazonalidadeVendas();
        
        assertThat(analise).isNotNull();
        assertThat(analise.getMesMaiorVenda()).isNotNull();
        assertThat(analise.getMesMenorVenda()).isNotNull();
        assertThat(analise.getFaturamentoMedio()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(analise.getEstatisticasPorMes()).hasSize(12); // 12 meses
    }
    
    @Test
    void testEstatisticasMensaisSazonalidade() {
        AnaliseSazonalidade analise = servico.calcularSazonalidadeVendas();
        
        for (EstatisticasMensais estatistica : analise.getEstatisticasPorMes()) {
            assertThat(estatistica.getMes()).isNotNull();
            assertThat(estatistica.getFaturamentoTotal()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(estatistica.getTotalVendas()).isGreaterThanOrEqualTo(0);
        }
    }
    
    @Test
    void testIdentificacaoPicosValesSazonalidade() {
        AnaliseSazonalidade analise = servico.calcularSazonalidadeVendas();

        EstatisticasMensais maiorMes = analise.getEstatisticasPorMes().stream()
            .filter(e -> e.getMes().equals(analise.getMesMaiorVenda()))
            .findFirst().orElse(null);
            
        EstatisticasMensais menorMes = analise.getEstatisticasPorMes().stream()
            .filter(e -> e.getMes().equals(analise.getMesMenorVenda()))
            .findFirst().orElse(null);
        
        if (maiorMes != null && menorMes != null) {
            assertThat(maiorMes.getFaturamentoTotal())
                .isGreaterThanOrEqualTo(menorMes.getFaturamentoTotal());
        }
    }
    
    @Test
    void testGerarPrevisaoVendasValida() {
        int mesesPrevisao = 6;
        
        PrevisaoVendas previsao = servico.gerarPrevisaoVendas(mesesPrevisao);
        
        assertThat(previsao).isNotNull();
        assertThat(previsao.getPrevisoesMensais()).hasSize(mesesPrevisao);
        assertThat(previsao.getTotalPrevisto()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(previsao.getMetodologia()).isNotNull().isNotEmpty();
    }
    
    @Test
    void testGerarPrevisaoVendasComMesesInvalidos() {
        assertThatThrownBy(() -> servico.gerarPrevisaoVendas(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Meses de previsão deve estar entre 1 e 12");
        
        assertThatThrownBy(() -> servico.gerarPrevisaoVendas(-5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Meses de previsão deve estar entre 1 e 12");
        
        assertThatThrownBy(() -> servico.gerarPrevisaoVendas(15))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Meses de previsão deve estar entre 1 e 12");
    }
    
    @Test
    void testConsistenciaPrevisoesMensais() {
        int mesesPrevisao = 3;
        
        PrevisaoVendas previsao = servico.gerarPrevisaoVendas(mesesPrevisao);
        
        BigDecimal somaPrevisoes = previsao.getPrevisoesMensais().stream()
            .map(PrevisaoMensal::getFaturamentoPrevisto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertThat(previsao.getTotalPrevisto()).isEqualTo(somaPrevisoes);
    }
    
    @Test
    void testSequencialMesesPrevisao() {
        int mesesPrevisao = 4;
        
        PrevisaoVendas previsao = servico.gerarPrevisaoVendas(mesesPrevisao);
        
        List<PrevisaoMensal> previsoes = previsao.getPrevisoesMensais();

        for (int i = 0; i < previsoes.size() - 1; i++) {
            LocalDate mesAtual = previsoes.get(i).getMes();
            LocalDate proximoMes = previsoes.get(i + 1).getMes();
            
            assertThat(proximoMes).isEqualTo(mesAtual.plusMonths(1));
        }
    }
    
    @Test
    void testFatorSazonalidadePrevisao() {
        int mesesPrevisao = 12;
        
        PrevisaoVendas previsao = servico.gerarPrevisaoVendas(mesesPrevisao);
        
        PrevisaoMensal dezembro = previsao.getPrevisoesMensais().stream()
            .filter(p -> p.getMes().getMonthValue() == 12)
            .findFirst().orElse(null);
            
        if (dezembro != null) {
            assertThat(dezembro.getFaturamentoPrevisto())
                .isGreaterThan(BigDecimal.ZERO); 
        }
    }

    @Test
    void testCalculosComValoresZero() {
        LocalDate dataFuturo = LocalDate.now().plusDays(30);
        LocalDate dataFuturoFim = LocalDate.now().plusDays(60);
        
        RelatorioVendasPeriodo relatorio = servico.gerarRelatorioVendasPeriodo(dataFuturo, dataFuturoFim);
        
        assertThat(relatorio.getFaturamentoTotal()).isEqualTo(BigDecimal.ZERO);
        assertThat(relatorio.getTotalVendas()).isEqualTo(0);
    }
    
    @Test
    void testPrecisaoCalculosBigDecimal() {
        LocalDate dataInicio = LocalDate.now().minusDays(30);
        LocalDate dataFim = LocalDate.now();
        
        RelatorioVendasPeriodo relatorio = servico.gerarRelatorioVendasPeriodo(dataInicio, dataFim);
        
        if (relatorio.getTotalVendas() > 0) {
            BigDecimal ticketCalculado = relatorio.getFaturamentoTotal()
                .divide(new BigDecimal(relatorio.getTotalVendas()), 2, java.math.RoundingMode.HALF_UP);
            assertThat(relatorio.getTicketMedio()).isEqualTo(ticketCalculado);
        }
    }
}