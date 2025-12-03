package com.tcc.estoque.service;

import com.tcc.estoque.dto.ProdutoDTO;
import com.tcc.estoque.dto.MovimentacaoEstoqueDTO;
import com.tcc.estoque.model.enums.TipoMovimentacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.tcc.estoque.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestSecurityConfig.class)
class MovimentacaoEstoqueServiceTest {

    @Autowired
    private MovimentacaoEstoqueService movimentacaoEstoqueService;
    @Autowired
    private ProdutoService produtoService;
    @Autowired
    private com.tcc.estoque.security.SecurityUtil securityUtil;

    private ProdutoDTO.ProdutoResponse produto;
    private ProdutoDTO.ProdutoResponse produto2;

    @BeforeEach
    void setup() {
        produto = produtoService.criarProduto(
                com.tcc.estoque.dto.ProdutoDTO.ProdutoRequest.builder()
                        .nome("Produto Estoque Teste")
                        .preco(new BigDecimal("50.00"))
                        .departamento("ROUPAS")
                        .estoque(20)
                        .estoqueMinimo(5)
                        .build()
        );
        
        produto2 = produtoService.criarProduto(
                com.tcc.estoque.dto.ProdutoDTO.ProdutoRequest.builder()
                        .nome("Produto Teste 2")
                        .preco(new BigDecimal("30.00"))
                        .departamento("CALCADOS")
                        .estoque(50)
                        .estoqueMinimo(10)
                        .build()
        );
    }

    @Test
    void testRegistrarEntradaEstoque() {
        MovimentacaoEstoqueDTO.EntradaEstoqueRequest req = MovimentacaoEstoqueDTO.EntradaEstoqueRequest.builder()
                .produtoId(produto.getId())
                .quantidade(10)
                .motivo("Reposicao")
                .build();
        movimentacaoEstoqueService.registrarEntrada(req, securityUtil.getUsuarioLogado());

        ProdutoDTO.ProdutoResponse atualizado = produtoService.buscarPorId(produto.getId());
        assertThat(atualizado.getQuantidadeEstoque()).isEqualTo(30);
    }

    @Test
    void testRegistrarSaidaEstoque() {
        MovimentacaoEstoqueDTO.SaidaEstoqueRequest req = MovimentacaoEstoqueDTO.SaidaEstoqueRequest.builder()
                .produtoId(produto.getId())
                .quantidade(5)
                .motivo("Venda")
                .build();
        movimentacaoEstoqueService.registrarSaida(req, securityUtil.getUsuarioLogado());

        ProdutoDTO.ProdutoResponse atualizado = produtoService.buscarPorId(produto.getId());
        assertThat(atualizado.getQuantidadeEstoque()).isEqualTo(15);
    }

    @Test
    void testBuscarMovimentacoesPorPeriodo() {
        LocalDate inicio = LocalDate.now().minusDays(10);
        LocalDate fim = LocalDate.now();
        List<MovimentacaoEstoqueDTO.MovimentacaoResponse> movimentacoes = 
            movimentacaoEstoqueService.buscarMovimentacoesPorPeriodo(inicio, fim);
        
        assertThat(movimentacoes).isNotNull();
    }

    @Test
    void testRegistrarAjusteEstoque() {
        // Teste ajuste positivo
        MovimentacaoEstoqueDTO.AjusteEstoqueRequest request = MovimentacaoEstoqueDTO.AjusteEstoqueRequest.builder()
                .produtoId(produto.getId())
                .novaQuantidade(30)
                .motivo("Ajuste de inventario")
                .observacoes("Contagem fisica")
                .build();
        
        MovimentacaoEstoqueDTO.MovimentacaoResponse response = 
            movimentacaoEstoqueService.registrarAjuste(request, securityUtil.getUsuarioLogado());
        
        assertThat(response).isNotNull();
        assertThat(response.getTipo()).isEqualTo(TipoMovimentacao.AJUSTE);
        assertThat(response.getQuantidade()).isEqualTo(10); // |30 - 20| = 10
        assertThat(response.getQuantidadeAtual()).isEqualTo(30);
        assertThat(response.getMotivo()).isEqualTo("Ajuste de inventario");
        
        // Verificar se produto foi atualizado
        ProdutoDTO.ProdutoResponse produtoAtualizado = produtoService.buscarPorId(produto.getId());
        assertThat(produtoAtualizado.getQuantidadeEstoque()).isEqualTo(30);
    }

    @Test
    void testRegistrarAjusteEstoqueNegativo() {
        // Teste ajuste negativo
        MovimentacaoEstoqueDTO.AjusteEstoqueRequest request = MovimentacaoEstoqueDTO.AjusteEstoqueRequest.builder()
                .produtoId(produto.getId())
                .novaQuantidade(10)
                .motivo("Perda por avaria")
                .build();
        
        MovimentacaoEstoqueDTO.MovimentacaoResponse response = 
            movimentacaoEstoqueService.registrarAjuste(request, securityUtil.getUsuarioLogado());
        
        assertThat(response).isNotNull();
        assertThat(response.getQuantidade()).isEqualTo(10); // |10 - 20| = 10
        assertThat(response.getQuantidadeAtual()).isEqualTo(10);
    }

    @Test
    void testRegistrarAjusteEstoqueQuantidadeIgual() {
        MovimentacaoEstoqueDTO.AjusteEstoqueRequest request = MovimentacaoEstoqueDTO.AjusteEstoqueRequest.builder()
                .produtoId(produto.getId())
                .novaQuantidade(20) // Mesma quantidade atual
                .motivo("Tentativa de ajuste")
                .build();
        
        assertThatThrownBy(() -> 
            movimentacaoEstoqueService.registrarAjuste(request, securityUtil.getUsuarioLogado()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("A nova quantidade é igual à quantidade atual");
    }

    @Test
    void testRegistrarAjusteProdutoInexistente() {
        MovimentacaoEstoqueDTO.AjusteEstoqueRequest request = MovimentacaoEstoqueDTO.AjusteEstoqueRequest.builder()
                .produtoId(999999L)
                .novaQuantidade(10)
                .motivo("Teste")
                .build();
        
        assertThatThrownBy(() -> 
            movimentacaoEstoqueService.registrarAjuste(request, securityUtil.getUsuarioLogado()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Produto não encontrado");
    }

    @Test
    void testListarMovimentacoes() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Teste sem filtros - apenas verificar se não lança exceção
        Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> todasMovimentacoes = 
            movimentacaoEstoqueService.listarMovimentacoes(null, null, null, null, pageable);
        
        assertThat(todasMovimentacoes).isNotNull();
        
        // Teste filtrado por produto específico
        Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> movimentacoesProduto = 
            movimentacaoEstoqueService.listarMovimentacoes(produto.getId(), null, null, null, pageable);
        
        assertThat(movimentacoesProduto).isNotNull();
        
        // Teste filtrado por tipo
        Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> movimentacoesTipo = 
            movimentacaoEstoqueService.listarMovimentacoes(null, TipoMovimentacao.ENTRADA, null, null, pageable);
        
        assertThat(movimentacoesTipo).isNotNull();
        
        // Teste filtrado por período
        LocalDate hoje = LocalDate.now();
        Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> movimentacoesPeriodo = 
            movimentacaoEstoqueService.listarMovimentacoes(null, null, hoje, hoje, pageable);
        
        assertThat(movimentacoesPeriodo).isNotNull();
    }

    @Test
    void testBuscarUltimasMovimentacoes() {
        // Criar uma movimentação primeiro
        MovimentacaoEstoqueDTO.EntradaEstoqueRequest request = MovimentacaoEstoqueDTO.EntradaEstoqueRequest.builder()
                .produtoId(produto.getId())
                .quantidade(5)
                .motivo("Entrada para teste")
                .build();
        movimentacaoEstoqueService.registrarEntrada(request, securityUtil.getUsuarioLogado());

        Pageable pageable = PageRequest.of(0, 5);
        Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> ultimasMovimentacoes = 
            movimentacaoEstoqueService.buscarUltimasMovimentacoes(pageable);
        
        assertThat(ultimasMovimentacoes).isNotNull();
        assertThat(ultimasMovimentacoes.getSize()).isEqualTo(5);
    }

    @Test
    void testBuscarMovimentacoesPorProduto() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> movimentacoesProduto = 
            movimentacaoEstoqueService.buscarMovimentacoesPorProduto(produto.getId(), pageable);
        
        assertThat(movimentacoesProduto).isNotNull();
        
        // Se há movimentações, verificar se são do produto correto
        if (!movimentacoesProduto.getContent().isEmpty()) {
            movimentacoesProduto.getContent().forEach(mov -> {
                assertThat(mov.getProdutoId()).isEqualTo(produto.getId());
            });
        }
    }

    @Test
    void testObterEstatisticasMovimentacao() {
        // Criar diferentes tipos de movimentações
        MovimentacaoEstoqueDTO.EntradaEstoqueRequest entradaRequest = MovimentacaoEstoqueDTO.EntradaEstoqueRequest.builder()
                .produtoId(produto.getId())
                .quantidade(25)
                .motivo("Entrada para estatisticas")
                .build();
        movimentacaoEstoqueService.registrarEntrada(entradaRequest, securityUtil.getUsuarioLogado());

        MovimentacaoEstoqueDTO.SaidaEstoqueRequest saidaRequest = MovimentacaoEstoqueDTO.SaidaEstoqueRequest.builder()
                .produtoId(produto.getId())
                .quantidade(10)
                .motivo("Saida para estatisticas")
                .build();
        movimentacaoEstoqueService.registrarSaida(saidaRequest, securityUtil.getUsuarioLogado());

        MovimentacaoEstoqueDTO.AjusteEstoqueRequest ajusteRequest = MovimentacaoEstoqueDTO.AjusteEstoqueRequest.builder()
                .produtoId(produto2.getId())
                .novaQuantidade(40)
                .motivo("Ajuste para estatisticas")
                .build();
        movimentacaoEstoqueService.registrarAjuste(ajusteRequest, securityUtil.getUsuarioLogado());

        LocalDate inicio = LocalDate.now().minusDays(1);
        LocalDate fim = LocalDate.now().plusDays(1);
        
        MovimentacaoEstoqueDTO.EstatisticasMovimentacaoResponse estatisticas = 
            movimentacaoEstoqueService.obterEstatisticasMovimentacao(inicio, fim);
        
        assertThat(estatisticas).isNotNull();
        assertThat(estatisticas.getTotalMovimentacoes()).isGreaterThan(0);
        assertThat(estatisticas.getTotalEntradas()).isGreaterThan(0);
        assertThat(estatisticas.getTotalSaidas()).isGreaterThan(0);
        assertThat(estatisticas.getTotalAjustes()).isGreaterThan(0);
        assertThat(estatisticas.getQuantidadeEntradas()).isGreaterThan(0);
        assertThat(estatisticas.getQuantidadeSaidas()).isGreaterThan(0);
        assertThat(estatisticas.getMovimentacoesHoje()).isNotNull();
        
        // Verificar saldo (entradas - saidas)
        Integer saldoEsperado = estatisticas.getQuantidadeEntradas() - estatisticas.getQuantidadeSaidas();
        assertThat(estatisticas.getSaldoMovimentacao()).isEqualTo(saldoEsperado);
    }

    @Test
    void testRegistrarSaidaEstoqueInsuficiente() {
        MovimentacaoEstoqueDTO.SaidaEstoqueRequest request = MovimentacaoEstoqueDTO.SaidaEstoqueRequest.builder()
                .produtoId(produto.getId())
                .quantidade(25) // Maior que o estoque de 20
                .motivo("Tentativa saida maior que estoque")
                .build();
        
        assertThatThrownBy(() -> 
            movimentacaoEstoqueService.registrarSaida(request, securityUtil.getUsuarioLogado()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    void testRegistrarEntradaProdutoInexistente() {
        MovimentacaoEstoqueDTO.EntradaEstoqueRequest request = MovimentacaoEstoqueDTO.EntradaEstoqueRequest.builder()
                .produtoId(888888L)
                .quantidade(10)
                .motivo("Entrada produto inexistente")
                .build();
        
        assertThatThrownBy(() -> 
            movimentacaoEstoqueService.registrarEntrada(request, securityUtil.getUsuarioLogado()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Produto não encontrado");
    }

    @Test
    void testRegistrarSaidaProdutoInexistente() {
        MovimentacaoEstoqueDTO.SaidaEstoqueRequest request = MovimentacaoEstoqueDTO.SaidaEstoqueRequest.builder()
                .produtoId(777777L)
                .quantidade(5)
                .motivo("Saida produto inexistente")
                .build();
        
        assertThatThrownBy(() -> 
            movimentacaoEstoqueService.registrarSaida(request, securityUtil.getUsuarioLogado()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Produto não encontrado");
    }

    @Test
    void testObterEstatisticasPeriodoSemMovimentacoes() {
        // Período muito antigo sem movimentações
        LocalDate inicioAntigo = LocalDate.now().minusYears(1);
        LocalDate fimAntigo = LocalDate.now().minusMonths(11);
        
        MovimentacaoEstoqueDTO.EstatisticasMovimentacaoResponse estatisticas = 
            movimentacaoEstoqueService.obterEstatisticasMovimentacao(inicioAntigo, fimAntigo);
        
        assertThat(estatisticas).isNotNull();
        assertThat(estatisticas.getTotalMovimentacoes()).isEqualTo(0);
        assertThat(estatisticas.getTotalEntradas()).isEqualTo(0);
        assertThat(estatisticas.getTotalSaidas()).isEqualTo(0);
        assertThat(estatisticas.getTotalAjustes()).isEqualTo(0);
        assertThat(estatisticas.getQuantidadeEntradas()).isEqualTo(0);
        assertThat(estatisticas.getQuantidadeSaidas()).isEqualTo(0);
        assertThat(estatisticas.getSaldoMovimentacao()).isEqualTo(0);
    }
}


