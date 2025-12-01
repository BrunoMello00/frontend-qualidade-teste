package com.tcc.estoque.integration;

import com.tcc.estoque.model.*;
import com.tcc.estoque.model.enums.CategoriaCliente;
import com.tcc.estoque.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Testes de Integracao - Servicos e Repositorios")
class VendaFluxoCompletoIntegrationTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @BeforeEach
    void setupTestData() {

        clienteRepository.deleteAll();
        produtoRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve integrar Cliente Service e Repository para criar e buscar cliente")
    void deveIntegrarClienteServiceERepositoryParaCriarEBuscarCliente() {

        Cliente novoCliente = new Cliente();
        novoCliente.setNome("Cliente Teste Integracao");
        novoCliente.setEmail("cliente.integracao@teste.com");
        novoCliente.setCpf("123.456.789-01");
        novoCliente.setTelefone("11999999999");
        novoCliente.setCategoria(CategoriaCliente.OURO);
        novoCliente.setPontos(1200);

        Cliente clienteSalvo = clienteRepository.save(novoCliente);
        Optional<Cliente> clienteEncontrado = clienteRepository.findById(clienteSalvo.getId());
        List<Cliente> todosClientes = clienteRepository.findAll();

        assertAll("Validacao da integracao Cliente Service/Repository",
            () -> assertThat(clienteSalvo).isNotNull(),
            () -> assertThat(clienteSalvo.getId()).isNotNull(),
            () -> assertThat(clienteEncontrado).isPresent(),
            () -> assertThat(clienteEncontrado.get().getNome()).isEqualTo("Cliente Teste Integracao"),
            () -> assertThat(clienteEncontrado.get().getCategoria()).isEqualTo(CategoriaCliente.OURO),
            () -> assertThat(clienteEncontrado.get().getPontos()).isEqualTo(1200),
            () -> assertThat(todosClientes).hasSize(1),
            () -> assertThat(todosClientes.get(0).getEmail()).isEqualTo("cliente.integracao@teste.com")
        );
    }

    @Test
    @DisplayName("Deve integrar Produto Service e Repository para operacoes CRUD")
    void deveIntegrarProdutoServiceERepositoryParaOperacoesCrud() {

        Produto novoProduto = new Produto();
        novoProduto.setNome("Produto Teste Integracao");
        novoProduto.setDescricao("Produto para teste de integracao completa");
        novoProduto.setPreco(BigDecimal.valueOf(100.00));
        novoProduto.setEstoque(50);
        novoProduto.setCodigo("PROD-INT-001");
        novoProduto.setAtivo(true);

        Produto produtoSalvo = produtoRepository.save(novoProduto);
        Optional<Produto> produtoEncontrado = produtoRepository.findById(produtoSalvo.getId());

        if (produtoEncontrado.isPresent()) {
            Produto produto = produtoEncontrado.get();
            produto.setEstoque(produto.getEstoque() - 5);
            produtoRepository.save(produto);
        }

        Optional<Produto> produtoAtualizado = produtoRepository.findById(produtoSalvo.getId());
        List<Produto> todosProdutos = produtoRepository.findAll();

        assertAll("Validacao da integracao Produto Service/Repository",
            () -> assertThat(produtoSalvo).isNotNull(),
            () -> assertThat(produtoSalvo.getId()).isNotNull(),
            () -> assertThat(produtoEncontrado).isPresent(),
            () -> assertThat(produtoEncontrado.get().getNome()).isEqualTo("Produto Teste Integracao"),
            () -> assertThat(produtoEncontrado.get().getPreco()).isEqualByComparingTo(BigDecimal.valueOf(100.00)),
            () -> assertThat(produtoEncontrado.get().getEstoque()).isEqualTo(50),
            () -> assertThat(produtoAtualizado).isPresent(),
            () -> assertThat(produtoAtualizado.get().getEstoque()).isEqualTo(45), // 50 - 5
            () -> assertThat(todosProdutos).hasSize(1),
            () -> assertThat(todosProdutos.get(0).getCodigo()).isEqualTo("PROD-INT-001")
        );
    }

    @Test
    @DisplayName("Deve integrar multiplos repositorios em transacao unica")
    void deveIntegrarMultiplosRepositoriosEmTransacaoUnica() {

        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Transacao Teste");
        cliente.setEmail("transacao@teste.com");
        cliente.setCpf("987.654.321-00");
        cliente.setTelefone("11888888888");
        cliente.setCategoria(CategoriaCliente.BRONZE);
        cliente.setPontos(100);

        Produto produto = new Produto();
        produto.setNome("Produto Transacao");
        produto.setDescricao("Produto para teste de transacao");
        produto.setPreco(BigDecimal.valueOf(50.00));
        produto.setEstoque(20);
        produto.setCodigo("PROD-TRANS-001");
        produto.setAtivo(true);

        Cliente clienteSalvo = clienteRepository.save(cliente);
        Produto produtoSalvo = produtoRepository.save(produto);

        clienteSalvo.setPontos(clienteSalvo.getPontos() + 10);
        produtoSalvo.setEstoque(produtoSalvo.getEstoque() - 2);

        clienteRepository.save(clienteSalvo);
        produtoRepository.save(produtoSalvo);

        assertAll("Validacao de transacao integrada",
            () -> {
                long totalClientes = clienteRepository.count();
                long totalProdutos = produtoRepository.count();
                assertThat(totalClientes).isEqualTo(1);
                assertThat(totalProdutos).isEqualTo(1);
            },
            () -> {
                Optional<Cliente> clienteVerificacao = clienteRepository.findById(clienteSalvo.getId());
                Optional<Produto> produtoVerificacao = produtoRepository.findById(produtoSalvo.getId());

                assertThat(clienteVerificacao).isPresent();
                assertThat(clienteVerificacao.get().getPontos()).isEqualTo(110); // 100 + 10

                assertThat(produtoVerificacao).isPresent();
                assertThat(produtoVerificacao.get().getEstoque()).isEqualTo(18); // 20 - 2
            }
        );
    }
}

