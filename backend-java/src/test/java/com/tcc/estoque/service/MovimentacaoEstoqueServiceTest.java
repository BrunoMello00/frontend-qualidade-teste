package com.tcc.estoque.service;

import com.tcc.estoque.dto.ProdutoDTO;
import com.tcc.estoque.dto.MovimentacaoEstoqueDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.tcc.estoque.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        movimentacaoEstoqueService.buscarMovimentacoesPorPeriodo(inicio, fim);
    }
}


