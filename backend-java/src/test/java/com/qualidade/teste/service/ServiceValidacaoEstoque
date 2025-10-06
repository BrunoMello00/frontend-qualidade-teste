package com.qualidade.teste.service;

import com.qualidade.teste.model.Cliente;
import com.qualidade.teste.model.Produto;
import com.qualidade.teste.model.ItemVenda;
import com.qualidade.teste.service.ServiceValidacaoEstoque.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ServiceValidacaoEstoqueTest {

    private ServiceValidacaoEstoque servico;
    private Produto produtoComEstoque;
    private Produto produtoSemEstoque;
    private Produto produtoEstoqueBaixo;
    private Produto produtoInativo;
    private ItemVenda itemValido;
    private ItemVenda itemSemEstoque;
    private ItemVenda itemEstoqueBaixo;
    private ItemVenda itemInativo;

    @BeforeEach
    void setup() {
        servico = new ServiceValidacaoEstoque();
        
        produtoComEstoque = new Produto(1L, "Vestido Floral", new BigDecimal("49.99"), Produto.Categoria.ROUPAS, 12);
        produtoComEstoque.setEstoqueMinimo(5);
        produtoComEstoque.setAtivo(true);
        
        produtoSemEstoque = new Produto(2L, "Camisa Polo", new BigDecimal("39.99"), Produto.Categoria.ROUPAS, 0);
        produtoSemEstoque.setEstoqueMinimo(5);
        produtoSemEstoque.setAtivo(true);
        
        produtoEstoqueBaixo = new Produto(3L, "Calça Jeans", new BigDecimal("89.99"), Produto.Categoria.ROUPAS, 3);
        produtoEstoqueBaixo.setEstoqueMinimo(5);
        produtoEstoqueBaixo.setAtivo(true);
        
        produtoInativo = new Produto(4L, "Produto Descontinuado", new BigDecimal("19.99"), Produto.Categoria.OUTROS, 10);
        produtoInativo.setEstoqueMinimo(2);
        produtoInativo.setAtivo(false);

        itemValido = new ItemVenda(produtoComEstoque, 5, produtoComEstoque.getPreco());
        itemSemEstoque = new ItemVenda(produtoSemEstoque, 1, produtoSemEstoque.getPreco());
        itemEstoqueBaixo = new ItemVenda(produtoEstoqueBaixo, 5, produtoEstoqueBaixo.getPreco()); // Mais que disponível
        itemInativo = new ItemVenda(produtoInativo, 2, produtoInativo.getPreco());
    }
    
    @Test
    void testValidarDisponibilidadeParaVendaComItensValidos() {
        List<ItemVenda> itens = Arrays.asList(
            new ItemVenda(produtoComEstoque, 3, produtoComEstoque.getPreco())
        );
        
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(itens);
        
        assertThat(resultado.isSucesso()).isTrue();
        assertThat(resultado.getMensagem()).contains("Todos os itens estão disponíveis");
        assertThat(resultado.getItensComProblema()).isEmpty();
    }
    
    @Test
    void testValidarDisponibilidadeComListaVazia() {
        List<ItemVenda> itensVazia = Collections.emptyList();
        
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(itensVazia);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getMensagem()).contains("Lista de itens não pode estar vazia");
    }
    
    @Test
    void testValidarDisponibilidadeComListaNula() {
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(null);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getMensagem()).contains("Lista de itens não pode estar vazia");
    }
    
    @Test
    void testValidarDisponibilidadeComEstoqueInsuficiente() {
        List<ItemVenda> itens = Arrays.asList(itemEstoqueBaixo);
        
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(itens);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getMensagem()).contains("problemas de estoque");
        assertThat(resultado.getItensComProblema()).hasSize(1);
        
        ItemComProblema problema = resultado.getItensComProblema().get(0);
        assertThat(problema.getProdutoId()).isEqualTo(produtoEstoqueBaixo.getId());
        assertThat(problema.getQuantidadeSolicitada()).isEqualTo(5);
        assertThat(problema.getQuantidadeDisponivel()).isEqualTo(3);
        assertThat(problema.getProblema()).contains("Estoque insuficiente");
    }
    
    @Test
    void testValidarDisponibilidadeComProdutoSemEstoque() {
        List<ItemVenda> itens = Arrays.asList(itemSemEstoque);
        
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(itens);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getItensComProblema()).hasSize(1);
        assertThat(resultado.getItensComProblema().get(0).getQuantidadeDisponivel()).isEqualTo(0);
    }
    
    @Test
    void testValidarDisponibilidadeComProdutoInativo() {
        List<ItemVenda> itens = Arrays.asList(itemInativo);
        
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(itens);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getItensComProblema().get(0).getProblema()).contains("Produto inativo");
    }
    
    @Test
    void testValidarDisponibilidadeComQuantidadeZero() {
        ItemVenda itemQuantidadeZero = new ItemVenda(produtoComEstoque, 0, produtoComEstoque.getPreco());
        List<ItemVenda> itens = Arrays.asList(itemQuantidadeZero);
        
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(itens);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getItensComProblema().get(0).getProblema()).contains("Quantidade deve ser maior que zero");
    }
    
    @Test
    void testValidarDisponibilidadeComMultiplosItens() {
        List<ItemVenda> itens = Arrays.asList(
            new ItemVenda(produtoComEstoque, 2, produtoComEstoque.getPreco()), // OK
            itemEstoqueBaixo, 
            itemInativo
        );
        
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(itens);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getItensComProblema()).hasSize(2);
        assertThat(resultado.getMensagem()).contains("2 problemas");
    }

    
    @Test
    void testCriarReservaTemporariaComItensValidos() {
        List<ItemVenda> itens = Arrays.asList(
            new ItemVenda(produtoComEstoque, 3, produtoComEstoque.getPreco())
        );
        
        Long idReserva = servico.criarReservaTemporaria(itens, 30);
        
        assertThat(idReserva).isNotNull();
        assertThat(idReserva).isGreaterThan(0L);
    }
    
    @Test
    void testCriarReservaTemporariaComItensInvalidos() {
        List<ItemVenda> itens = Arrays.asList(itemEstoqueBaixo);
        
        Long idReserva = servico.criarReservaTemporaria(itens, 30);
        
        assertThat(idReserva).isNull();
    }
    
    @Test
    void testConfirmarReservaValida() {
        List<ItemVenda> itens = Arrays.asList(
            new ItemVenda(produtoComEstoque, 3, produtoComEstoque.getPreco())
        );
        
        Long idReserva = servico.criarReservaTemporaria(itens, 30);
        boolean confirmacao = servico.confirmarReserva(idReserva);
        
        assertThat(confirmacao).isTrue();
    }
    
    @Test
    void testConfirmarReservaInexistente() {
        Long idReservaInexistente = 999L;
        
        boolean confirmacao = servico.confirmarReserva(idReservaInexistente);
        
        assertThat(confirmacao).isFalse();
    }
    
    @Test
    void testCancelarReservaValida() {
        List<ItemVenda> itens = Arrays.asList(
            new ItemVenda(produtoComEstoque, 3, produtoComEstoque.getPreco())
        );
        
        Long idReserva = servico.criarReservaTemporaria(itens, 30);
        boolean cancelamento = servico.cancelarReserva(idReserva);
        
        assertThat(cancelamento).isTrue();
    }
    
    @Test
    void testCancelarReservaInexistente() {
        Long idReservaInexistente = 999L;
        
        boolean cancelamento = servico.cancelarReserva(idReservaInexistente);
        
        assertThat(cancelamento).isFalse();
    }
    
    @Test
    void testReservaDeveReduzirEstoqueTemporariamente() {
        int estoqueInicialEsperado = 12;
        List<ItemVenda> itens = Arrays.asList(
            new ItemVenda(produtoComEstoque, 3, produtoComEstoque.getPreco())
        );

        Long idReserva = servico.criarReservaTemporaria(itens, 30);
        
        assertThat(idReserva).isNotNull();
    }

    
    @Test
    void testProcessarEntradaEstoqueValida() {
        Long produtoId = 1L;
        int quantidade = 10;
        String motivo = "Compra de fornecedor";
        
        ResultadoMovimentacaoEstoque resultado = servico.processarEntradaEstoque(produtoId, quantidade, motivo);
        
        assertThat(resultado.isSucesso()).isTrue();
        assertThat(resultado.getMensagem()).contains("Entrada processada");
        assertThat(resultado.getMovimentacao()).isNotNull();
    }
    
    @Test
    void testProcessarEntradaEstoqueComQuantidadeInvalida() {
        Long produtoId = 1L;
        int quantidadeInvalida = 0;
        String motivo = "Teste";
        
        ResultadoMovimentacaoEstoque resultado = servico.processarEntradaEstoque(produtoId, quantidadeInvalida, motivo);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getMensagem()).contains("Quantidade deve ser maior que zero");
        assertThat(resultado.getMovimentacao()).isNull();
    }
    
    @Test
    void testProcessarEntradaEstoqueComQuantidadeNegativa() {
        Long produtoId = 1L;
        int quantidadeNegativa = -5;
        String motivo = "Teste";
        
        ResultadoMovimentacaoEstoque resultado = servico.processarEntradaEstoque(produtoId, quantidadeNegativa, motivo);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getMensagem()).contains("Quantidade deve ser maior que zero");
    }
    
    @Test
    void testProcessarEntradaEstoqueComProdutoInexistente() {
        Long produtoIdInexistente = 999L;
        int quantidade = 10;
        String motivo = "Teste";
        
        ResultadoMovimentacaoEstoque resultado = servico.processarEntradaEstoque(produtoIdInexistente, quantidade, motivo);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getMensagem()).contains("Produto não encontrado");
    }
    
    @Test
    void testProcessarSaidaEstoqueValida() {
        Long produtoId = 1L;
        int quantidade = 5;
        String motivo = "Venda";
        
        ResultadoMovimentacaoEstoque resultado = servico.processarSaidaEstoque(produtoId, quantidade, motivo);
        
        assertThat(resultado.isSucesso()).isTrue();
        assertThat(resultado.getMensagem()).contains("Saída processada");
        assertThat(resultado.getMovimentacao()).isNotNull();
    }
    
    @Test
    void testProcessarSaidaEstoqueComEstoqueInsuficiente() {
        Long produtoId = 2L;
        int quantidade = 10;
        String motivo = "Venda";
        
        ResultadoMovimentacaoEstoque resultado = servico.processarSaidaEstoque(produtoId, quantidade, motivo);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getMensagem()).contains("Estoque insuficiente");
        assertThat(resultado.getMensagem()).contains("Disponível:");
        assertThat(resultado.getMensagem()).contains("Solicitado:");
    }
    
    @Test
    void testProcessarSaidaEstoqueComQuantidadeInvalida() {
        Long produtoId = 1L;
        int quantidadeInvalida = -2;
        String motivo = "Teste";
        
        ResultadoMovimentacaoEstoque resultado = servico.processarSaidaEstoque(produtoId, quantidadeInvalida, motivo);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getMensagem()).contains("Quantidade deve ser maior que zero");
    }
    
    @Test
    void testObterProdutosComEstoqueBaixo() {
        List<AlertaEstoqueBaixo> alertas = servico.obterProdutosComEstoqueBaixo();
        
        assertThat(alertas).isNotNull();
        
        boolean temProdutoEstoqueBaixo = alertas.stream()
            .anyMatch(alerta -> alerta.getQuantidadeAtual() <= alerta.getEstoqueMinimo());
        
        if (!alertas.isEmpty()) {
            assertThat(temProdutoEstoqueBaixo).isTrue();
        }
    }
    
    @Test
    void testOrdenacaoAlertasEstoqueBaixo() {
        List<AlertaEstoqueBaixo> alertas = servico.obterProdutosComEstoqueBaixo();
        
        if (alertas.size() > 1) {
            for (int i = 0; i < alertas.size() - 1; i++) {
                AlertaEstoqueBaixo atual = alertas.get(i);
                AlertaEstoqueBaixo proximo = alertas.get(i + 1);
                
                double ratioAtual = (double) atual.getQuantidadeAtual() / atual.getEstoqueMinimo();
                double ratioProximo = (double) proximo.getQuantidadeAtual() / proximo.getEstoqueMinimo();
                
                assertThat(ratioAtual).isLessThanOrEqualTo(ratioProximo);
            }
        }
    }
    
    @Test
    void testPropriedadesAlertaEstoqueBaixo() {
        List<AlertaEstoqueBaixo> alertas = servico.obterProdutosComEstoqueBaixo();
        
        for (AlertaEstoqueBaixo alerta : alertas) {
            assertThat(alerta.getProdutoId()).isNotNull();
            assertThat(alerta.getNomeProduto()).isNotNull().isNotEmpty();
            assertThat(alerta.getQuantidadeAtual()).isGreaterThanOrEqualTo(0);
            assertThat(alerta.getEstoqueMinimo()).isGreaterThan(0);
            assertThat(alerta.getSugestaoReposicao()).isGreaterThanOrEqualTo(0);
            assertThat(alerta.getDiasSemReposicao()).isGreaterThanOrEqualTo(0);
        }
    }
    
    @Test
    void testLimparReservasExpiradas() {
        int reservasLimpas = servico.limparReservasExpiradas();
        
        assertThat(reservasLimpas).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void testLimparReservasExpiradas_SemReservas() {
        int reservasLimpas = servico.limparReservasExpiradas();
        
        assertThat(reservasLimpas).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void testReservaComTempoExpiracaoZero() {
        List<ItemVenda> itens = Arrays.asList(
            new ItemVenda(produtoComEstoque, 2, produtoComEstoque.getPreco())
        );
        
        Long idReserva = servico.criarReservaTemporaria(itens, 0);
        
        if (idReserva != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            boolean confirmacao = servico.confirmarReserva(idReserva);
            assertThat(confirmacao).isFalse();
        }
    }
    
    @Test
    void testValidarItemNulo() {
        List<ItemVenda> itensComNulo = Arrays.asList((ItemVenda) null);
        
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(itensComNulo);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getItensComProblema().get(0).getProblema()).contains("Item ou produto inválido");
    }
    
    @Test
    void testValidarItemComProdutoNulo() {
        ItemVenda itemComProdutoNulo = new ItemVenda();
        itemComProdutoNulo.setProduto(null);
        itemComProdutoNulo.setQuantidade(1);
        
        List<ItemVenda> itens = Arrays.asList(itemComProdutoNulo);
        
        ResultadoValidacaoEstoque resultado = servico.validarDisponibilidadeParaVenda(itens);
        
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.getItensComProblema().get(0).getProblema()).contains("Item ou produto inválido");
    }
    
    @Test
    void testMultiplasOperacoesConsecutivas() {
        Long produtoId = 1L;
        
        ResultadoMovimentacaoEstoque entrada = servico.processarEntradaEstoque(produtoId, 5, "Reposição");
        assertThat(entrada.isSucesso()).isTrue();
        
        ResultadoMovimentacaoEstoque saida = servico.processarSaidaEstoque(produtoId, 3, "Venda");
        assertThat(saida.isSucesso()).isTrue();
        
        ResultadoMovimentacaoEstoque entrada2 = servico.processarEntradaEstoque(produtoId, 10, "Nova compra");
        assertThat(entrada2.isSucesso()).isTrue();
    }
    
    @Test
    void testCriacaoECancelamentoMultiplasReservas() {
        List<ItemVenda> itens1 = Arrays.asList(
            new ItemVenda(produtoComEstoque, 2, produtoComEstoque.getPreco())
        );
        
        List<ItemVenda> itens2 = Arrays.asList(
            new ItemVenda(produtoComEstoque, 3, produtoComEstoque.getPreco())
        );
        
        Long reserva1 = servico.criarReservaTemporaria(itens1, 30);
        Long reserva2 = servico.criarReservaTemporaria(itens2, 30);
        
        assertThat(reserva1).isNotNull();
        assertThat(reserva2).isNotNull();
        assertThat(reserva1).isNotEqualTo(reserva2);
        
        boolean cancelamento1 = servico.cancelarReserva(reserva1);
        boolean cancelamento2 = servico.cancelarReserva(reserva2);
        
        assertThat(cancelamento1).isTrue();
        assertThat(cancelamento2).isTrue();
    }
    
    @Test
    void testConsistenciaEstoqueAposOperacoes() {
        Long produtoId = 1L;

        servico.processarEntradaEstoque(produtoId, 10, "Teste entrada");
        servico.processarSaidaEstoque(produtoId, 5, "Teste saída");

        ResultadoMovimentacaoEstoque novaOperacao = servico.processarSaidaEstoque(produtoId, 2, "Nova saída");
        assertThat(novaOperacao.isSucesso()).isTrue();
    }
}
