package com.tcc.estoque.service;

import com.tcc.estoque.dto.RelatorioDTO;
import com.tcc.estoque.model.Venda;
import com.tcc.estoque.model.enums.FormaPagamento;
import com.tcc.estoque.model.enums.StatusVenda;
import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.VendaRepository;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class RelatorioServiceMutacaoAdapterTest {

    @Test
    public void gerarRelatorioVendasComUmaVendaConfirmada() {
        VendaRepository vendaRepo = Mockito.mock(VendaRepository.class);
        ProdutoRepository produtoRepo = Mockito.mock(ProdutoRepository.class);

        RelatorioService service = new RelatorioService(vendaRepo, produtoRepo);

        LocalDate inicio = LocalDate.now().minusDays(1);
        LocalDate fim = LocalDate.now();

        Venda v = Venda.builder()
                .id(1L)
                .clienteNome("Cliente X")
                .subtotal(new BigDecimal("100.00"))
                .valorTotal(new BigDecimal("100.00"))
                .formaPagamento(FormaPagamento.DINHEIRO)
                .status(StatusVenda.CONFIRMADA)
                .dataVenda(LocalDateTime.now())
                .build();

        Mockito.when(vendaRepo.findVendasPorPeriodo(Mockito.any(), Mockito.any()))
                .thenReturn(List.of(v));

        RelatorioDTO.RelatorioVendasResponse resp = service.gerarRelatorioVendas(inicio, fim);

    assertEquals(Long.valueOf(1L), Long.valueOf(resp.getTotalVendas()));
    assertEquals(new BigDecimal("100.00"), resp.getTotalFaturamento());
    }
}


