package com.tcc.estoque.service;

import com.tcc.estoque.dto.RelatorioDTO;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.VendaRepository;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RelatorioServiceUnitTest {

    @Test
    public void gerarRelatorioVendasSemVendas() {
        VendaRepository vendaRepo = Mockito.mock(VendaRepository.class);
        ProdutoRepository produtoRepo = Mockito.mock(ProdutoRepository.class);

        Mockito.when(vendaRepo.findVendasPorPeriodo(Mockito.any(), Mockito.any())).thenReturn(List.of());

        RelatorioService service = new RelatorioService(vendaRepo, produtoRepo);

        RelatorioDTO.RelatorioVendasResponse resp = service.gerarRelatorioVendas(LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(Long.valueOf(0), Long.valueOf(resp.getTotalVendas()));
        assertEquals(BigDecimal.ZERO, resp.getTotalFaturamento());
        assertEquals(BigDecimal.ZERO, resp.getTicketMedio());
    }

    @Test
    public void gerarRelatorioEstoqueComProdutos() {
        VendaRepository vendaRepo = Mockito.mock(VendaRepository.class);
        ProdutoRepository produtoRepo = Mockito.mock(ProdutoRepository.class);

        Produto p1 = Produto.builder().id(1L).nome("A").preco(BigDecimal.valueOf(10)).estoque(2).estoqueMinimo(5).ativo(true).codigo("X").build();
        Produto p2 = Produto.builder().id(2L).nome("B").preco(BigDecimal.valueOf(5)).estoque(10).estoqueMinimo(3).ativo(true).codigo("Y").build();

        Mockito.when(produtoRepo.findAll()).thenReturn(List.of(p1, p2));
        Mockito.when(produtoRepo.countProdutosComEstoqueBaixo()).thenReturn(1L);
        Mockito.when(produtoRepo.calcularValorTotalEstoque()).thenReturn(BigDecimal.valueOf(10*2 + 5*10));

        RelatorioService service = new RelatorioService(vendaRepo, produtoRepo);

        RelatorioDTO.RelatorioEstoqueResponse resp = service.gerarRelatorioEstoque();

        assertEquals(Long.valueOf(2), Long.valueOf(resp.getTotalProdutos()));
        assertEquals(Long.valueOf(2), Long.valueOf(resp.getProdutosAtivos()));
        assertEquals(Long.valueOf(1), Long.valueOf(resp.getProdutosEstoqueBaixo()));
    }

}



