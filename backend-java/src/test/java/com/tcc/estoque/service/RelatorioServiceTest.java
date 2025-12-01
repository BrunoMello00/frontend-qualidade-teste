package com.tcc.estoque.service;

import com.tcc.estoque.dto.RelatorioDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

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
}


