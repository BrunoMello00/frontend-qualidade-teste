package com.qualidade.teste.service;

import com.qualidade.teste.model.Produto;
import com.qualidade.teste.model.ItemVenda;
import com.qualidade.teste.service.ServiceValidacaoEstoque.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ServiceValidacaoEstoqueTest {

    private ServiceValidacaoEstoque service;
    private Produto produto1;
    private Produto produto2;

    @BeforeEach
    void setup() {
        service = new ServiceValidacaoEstoque();
        
        produto1 = new Produto(1L, "Vestido Floral", 
            new java.math.BigDecimal("49.99"), Produto.Categoria.ROUPAS, 12);
        produto1.setEstoqueMinimo(5);
        
        produto2 = new Produto(2L, "Camisa Polo", 
            new java.math.BigDecimal("39.99"), Produto.Categoria.ROUPAS, 3);
        produto2.setEstoqueMinimo(5);
    }

    @Test
    void testValidarDisponibilidadeParaVendaComSucesso() {
        ItemVenda item = new ItemVenda(produto1, 5, produto1.getPreco());
        List<ItemVenda> itens = Arrays.asList(item);

        ResultadoValidacaoEstoque resultado = service.validarDisponibilidadeParaVenda(itens);

        assertThat(resultado.isSucesso()).isTrue();
        assertThat(resultado.getMensagem()).contains("Todos os itens estão disponíveis");
    }

    @Test
    void testValidarDisponibilidadeParaVendaEstoqueInsuficiente() {
        ItemVenda item = new ItemVenda(produto2, 10, produto2.getPreco()); // Mais que o estoque (3)
        List<ItemVenda> itens = Arrays.asList(item);

        ResultadoValidacaoEstoque resultado = service.validarDisponibilidadeParaVenda(itens);

        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getItensComProblema()).isNotEmpty();
    }

    @Test
    void testValidarDisponibilidadeListaVazia() {
        ResultadoValidacaoEstoque resultado = service.validarDisponibilidadeParaVenda(Collections.emptyList());

        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getMensagem()).contains("Lista de itens não pode estar vazia");
    }

    @Test
    void testCriarReservaTemporaria() {
        ItemVenda item = new ItemVenda(produto1, 2, produto1.getPreco());
        List<ItemVenda> itens = Arrays.asList(item);

        Long idReserva = service.criarReservaTemporaria(itens, 30);

        assertThat(idReserva).isNotNull();
        assertThat(idReserva).isGreaterThan(0);
    }

    @Test
    void testConfirmarReserva() {
        ItemVenda item = new ItemVenda(produto1, 2, produto1.getPreco());
        List<ItemVenda> itens = Arrays.asList(item);
        Long idReserva = service.criarReservaTemporaria(itens, 30);

        boolean confirmado = service.confirmarReserva(idReserva);

        assertThat(confirmado).isTrue();
    }

    @Test
    void testCancelarReserva() {
        ItemVenda item = new ItemVenda(produto1, 2, produto1.getPreco());
        List<ItemVenda> itens = Arrays.asList(item);
        Long idReserva = service.criarReservaTemporaria(itens, 30);

        boolean cancelado = service.cancelarReserva(idReserva);

        assertThat(cancelado).isTrue();
    }

    @Test
    void testProcessarEntradaEstoque() {
        ResultadoMovimentacaoEstoque resultado = service.processarEntradaEstoque(1L, 10, "Reposição");

        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isTrue();
    }

    @Test
    void testProcessarSaidaEstoque() {
        ResultadoMovimentacaoEstoque resultado = service.processarSaidaEstoque(1L, 5, "Venda");

        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isTrue();
    }

    @Test
    void testObterProdutosComEstoqueBaixo() {
        List<AlertaEstoqueBaixo> alertas = service.obterProdutosComEstoqueBaixo();

        assertThat(alertas).isNotNull();
        // Produto2 tem estoque 3 e mínimo 5, deve aparecer no alerta
        assertThat(alertas).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void testLimparReservasExpiradas() {
        // Criar reserva com tempo muito baixo
        ItemVenda item = new ItemVenda(produto1, 1, produto1.getPreco());
        service.criarReservaTemporaria(Arrays.asList(item), 0); // Expira imediatamente

        int limpas = service.limparReservasExpiradas();

        assertThat(limpas).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testPreventOverselling() {
        // Tentar vender mais que o estoque disponível
        ItemVenda item = new ItemVenda(produto2, 50, produto2.getPreco()); // Muito mais que estoque (3)
        List<ItemVenda> itens = Arrays.asList(item);

        ResultadoValidacaoEstoque resultado = service.validarDisponibilidadeParaVenda(itens);

        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getItensComProblema())
            .extracting(ItemComProblema::getQuantidadeSolicitada)
            .contains(50);
    }

    @Test
    void testObterHistoricoMovimentacoes() {
        // Fazer algumas movimentações
        service.processarEntradaEstoque(1L, 10, "Reposição inicial");
        service.processarSaidaEstoque(1L, 3, "Venda teste");

        List<ServiceValidacaoEstoque.MovimentacaoEstoque> historico = service.obterHistoricoMovimentacoes(1L);

        assertThat(historico).isNotEmpty();
        assertThat(historico).hasSizeGreaterThanOrEqualTo(2);
        assertThat(historico)
            .extracting(ServiceValidacaoEstoque.MovimentacaoEstoque::getMotivo)
            .contains("Reposição inicial", "Venda teste");
    }

    @Test
    void testObterHistoricoCompletoMovimentacoes() {
        // Fazer movimentações em diferentes produtos
        service.processarEntradaEstoque(1L, 5, "Entrada produto 1");
        service.processarEntradaEstoque(2L, 8, "Entrada produto 2");

        var historicoCompleto = service.obterHistoricoCompleto();

        assertThat(historicoCompleto).isNotEmpty();
        assertThat(historicoCompleto.keySet()).contains(1L, 2L);
    }

    @Test
    void testObterHistoricoProdutoInexistente() {
        List<ServiceValidacaoEstoque.MovimentacaoEstoque> historico = service.obterHistoricoMovimentacoes(999L);

        assertThat(historico).isEmpty();
    }
}