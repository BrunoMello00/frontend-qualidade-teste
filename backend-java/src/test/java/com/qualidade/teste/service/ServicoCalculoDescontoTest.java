package com.qualidade.teste.service;

import com.qualidade.teste.model.Cliente;
import com.qualidade.teste.model.Produto;
import com.qualidade.teste.model.Venda;
import com.qualidade.teste.model.ItemVenda;
import com.qualidade.teste.service.ServicoCalculoDesconto.ResultadoCalculoDesconto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class ServicoCalculoDescontoTest {

    private ServicoCalculoDesconto servico;
    private Cliente cliente1;
    private Cliente cliente2;
    private Cliente cliente3;
    private Produto produto;

    @BeforeEach
    void setup() {
        servico = new ServicoCalculoDesconto();
        cliente1 = new Cliente(1L, "João", "123.456.789-00", "joao@email.com", Cliente.TipoCliente.BRONZE);
        cliente1.setPontosFidelidade(100);
        cliente2 = new Cliente(2L, "Maria", "987.654.321-00", "maria@email.com", Cliente.TipoCliente.OURO);
        cliente2.setPontosFidelidade(2500);
        cliente3 = new Cliente(3L, "Carlos", "555.666.777-88", "carlos@email.com", Cliente.TipoCliente.PREMIUM);
        cliente3.setPontosFidelidade(12000);
        produto = new Produto(1L, "Produto Teste", new BigDecimal("100.00"), Produto.Categoria.ROUPAS, 10);
        produto.setPercentualMaximoDesconto(new BigDecimal("50"));
    }

    @Test
    void testCalculoDescontoBronze() {
        Venda venda = new Venda(cliente1, Arrays.asList(
            new ItemVenda(produto, 1, produto.getPreco())
        ));
        ResultadoCalculoDesconto resultado = servico.calcularDescontoVenda(cliente1, venda, null);
        assertThat(resultado.getValorFinal()).isLessThanOrEqualTo(produto.getPreco());
    }

    @Test
    void testCalculoDescontoOuro() {
        Venda venda = new Venda(cliente2, Arrays.asList(
            new ItemVenda(produto, 1, produto.getPreco())
        ));
        ResultadoCalculoDesconto resultado = servico.calcularDescontoVenda(cliente2, venda, null);
        assertThat(resultado.getPercentualDesconto()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void testCupomDesconto() {
        Venda venda = new Venda(cliente1, Arrays.asList(
            new ItemVenda(produto, 1, produto.getPreco())
        ));
        ResultadoCalculoDesconto resultado = servico.calcularDescontoVenda(cliente1, venda, "PRIMEIRACOMPRA");
        assertThat(resultado.getJustificativa()).contains("PRIMEIRACOMPRA");
    }

    @Test
    void clienteNuloDeveLancarExcecao() {
        Venda venda = new Venda(cliente1, Arrays.asList(
            new ItemVenda(produto, 1, produto.getPreco())
        ));
        assertThatThrownBy(() -> servico.calcularDescontoVenda(null, venda, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void vendaNulaDeveLancarExcecao() {
        assertThatThrownBy(() -> servico.calcularDescontoVenda(cliente1, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clienteComPoucosPontos() {
        cliente1.setPontosFidelidade(500);
        Venda venda = new Venda(cliente1, Arrays.asList(
            new ItemVenda(produto, 1, produto.getPreco())
        ));
        ResultadoCalculoDesconto resultado = servico.calcularDescontoVenda(cliente1, venda, null);
        assertThat(resultado.getValorFinal()).isLessThanOrEqualTo(produto.getPreco());
    }

    @Test
    void clienteComMuitosPontos() {
        cliente1.setPontosFidelidade(15000);
        Venda venda = new Venda(cliente1, Arrays.asList(
            new ItemVenda(produto, 1, produto.getPreco())
        ));
        ResultadoCalculoDesconto resultado = servico.calcularDescontoVenda(cliente1, venda, null);
        assertThat(resultado.getJustificativa()).contains("fidelidade");
    }
}