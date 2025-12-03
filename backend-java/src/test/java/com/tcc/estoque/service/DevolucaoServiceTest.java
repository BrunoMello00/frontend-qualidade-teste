package com.tcc.estoque.service;

import com.tcc.estoque.dto.DevolucaoDTO;
import com.tcc.estoque.model.ItemDevolucao;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.model.Venda;
import com.tcc.estoque.model.ItemVenda;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.StatusQualidade;
import com.tcc.estoque.repository.*;
import com.tcc.estoque.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevolucaoServiceTest {

    @Mock
    private DevolucaoRepository devolucaoRepository;

    @Mock
    private ItemDevolucaoRepository itemDevolucaoRepository;

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private ItemVendaRepository itemVendaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private DevolucaoService devolucaoService;

        private Usuario usuario;

        @BeforeEach
        void init() {
                usuario = new Usuario();
                usuario.setId(10L);
                usuario.setNome("Usuario Teste");
                usuario.setEmail("test@local");
                                lenient().when(devolucaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
                                lenient().when(itemDevolucaoRepository.save(any())).thenAnswer(invocation -> {
                                        ItemDevolucao it = invocation.getArgument(0);
                                        if (it.getValorItem() == null && it.getPrecoUnitarioOriginal() != null && it.getQuantidade() != null) {
                                                it.setValorItem(it.getPrecoUnitarioOriginal().multiply(BigDecimal.valueOf(it.getQuantidade())));
                                        }
                                        return it;
                                });
        }

    

    

        @Test
        void testProcessarDevolucaoCaminhoFeliz() {
                Venda venda = new Venda();
                venda.setId(100L);
                venda.confirmar();
                venda.setClienteNome("Cliente");

                Produto produto = Produto.builder().id(2L).nome("P").preco(new BigDecimal("50.00")).estoque(5).build();

                ItemVenda itemVenda = ItemVenda.builder()
                                .id(200L)
                                .produto(produto)
                                .quantidade(2)
                                .precoUnitario(new BigDecimal("50.00"))
                                .nomeProduto("P")
                                .build();

                when(vendaRepository.findById(100L)).thenReturn(Optional.of(venda));
                when(itemVendaRepository.findById(200L)).thenReturn(Optional.of(itemVenda));
                when(securityUtil.getUsuarioLogado()).thenReturn(usuario);

                DevolucaoDTO.ItemDevolucaoRequest itemReq = new DevolucaoDTO.ItemDevolucaoRequest();
                itemReq.setItemVendaId(200L);
                itemReq.setQuantidade(1);
                itemReq.setStatusQualidadeRetorno(StatusQualidade.NORMAL);

                DevolucaoDTO.DevolucaoRequest req = new DevolucaoDTO.DevolucaoRequest();
                req.setVendaId(100L);
                req.setTipoDevolucao(null);
                req.setItens(Arrays.asList(itemReq));

                devolucaoService.processarDevolucao(req);

                verify(itemDevolucaoRepository, times(1)).save(any(ItemDevolucao.class));
                ArgumentCaptor<Produto> produtoCaptor = ArgumentCaptor.forClass(Produto.class);
                verify(produtoRepository, atLeastOnce()).save(produtoCaptor.capture());
                Produto salvo = produtoCaptor.getValue();
                assertEquals(6, salvo.getEstoque());
        }

        @Test
        void testProcessarDevolucaoQuantidadeMaiorQueVendida() {
                Venda venda = new Venda();
                venda.setId(101L);
                venda.confirmar();
                venda.setClienteNome("Cliente");

                Produto produto = Produto.builder().id(3L).nome("P2").preco(new BigDecimal("30.00")).estoque(2).build();

                ItemVenda itemVenda = ItemVenda.builder()
                                .id(201L)
                                .produto(produto)
                                .quantidade(1)
                                .precoUnitario(new BigDecimal("30.00"))
                                .nomeProduto("P2")
                                .build();

                when(vendaRepository.findById(101L)).thenReturn(Optional.of(venda));
                when(itemVendaRepository.findById(201L)).thenReturn(Optional.of(itemVenda));
                when(securityUtil.getUsuarioLogado()).thenReturn(usuario);

                DevolucaoDTO.ItemDevolucaoRequest itemReq = new DevolucaoDTO.ItemDevolucaoRequest();
                itemReq.setItemVendaId(201L);
                itemReq.setQuantidade(2);
                itemReq.setStatusQualidadeRetorno(StatusQualidade.NORMAL);

                DevolucaoDTO.DevolucaoRequest req = new DevolucaoDTO.DevolucaoRequest();
                req.setVendaId(101L);
                req.setItens(Arrays.asList(itemReq));

                RuntimeException ex = assertThrows(RuntimeException.class, () -> devolucaoService.processarDevolucao(req));
                assertTrue(ex.getMessage().contains("Quantidade de devolução não pode ser maior que a vendida"));
        }

        @Test
        void testProcessarDevolucaoDefeituoso() {
                Venda venda = new Venda();
                venda.setId(102L);
                venda.confirmar();
                venda.setClienteNome("Cliente");

                Produto produto = Produto.builder().id(4L).nome("P3").preco(new BigDecimal("100.00")).estoque(3).build();

                ItemVenda itemVenda = ItemVenda.builder()
                                .id(202L)
                                .produto(produto)
                                .quantidade(1)
                                .precoUnitario(new BigDecimal("100.00"))
                                .nomeProduto("P3")
                                .build();

                when(vendaRepository.findById(102L)).thenReturn(Optional.of(venda));
                when(itemVendaRepository.findById(202L)).thenReturn(Optional.of(itemVenda));
                when(securityUtil.getUsuarioLogado()).thenReturn(usuario);

                DevolucaoDTO.ItemDevolucaoRequest itemReq = new DevolucaoDTO.ItemDevolucaoRequest();
                itemReq.setItemVendaId(202L);
                itemReq.setQuantidade(1);
                itemReq.setStatusQualidadeRetorno(StatusQualidade.DEFEITUOSO);
                itemReq.setDescontoAplicado(new BigDecimal("10"));

                DevolucaoDTO.DevolucaoRequest req = new DevolucaoDTO.DevolucaoRequest();
                req.setVendaId(102L);
                req.setItens(Arrays.asList(itemReq));

                devolucaoService.processarDevolucao(req);

                ArgumentCaptor<Produto> produtoCaptor = ArgumentCaptor.forClass(Produto.class);
                verify(produtoRepository, atLeastOnce()).save(produtoCaptor.capture());
                Produto salvo = produtoCaptor.getValue();
                assertEquals(new BigDecimal("90.00"), salvo.getPrecoDefeituoso());
                assertEquals(StatusQualidade.DEFEITUOSO, salvo.getStatusQualidade());
        }

        @Test
        void testBuscarDevolucoes() {
                // Dado devoluções existentes
                com.tcc.estoque.model.Devolucao devolucao = new com.tcc.estoque.model.Devolucao();
                devolucao.setId(1L);
                devolucao.setTipoDevolucao(com.tcc.estoque.model.enums.TipoDevolucao.DEVOLUCAO_SIMPLES);
                devolucao.setValorDevolucao(new BigDecimal("50.00"));
                
                Venda venda = new Venda();
                venda.setId(1L);
                venda.setClienteNome("Cliente Teste");
                devolucao.setVenda(venda);
                
                org.springframework.data.domain.PageImpl<com.tcc.estoque.model.Devolucao> page = 
                        new org.springframework.data.domain.PageImpl<>(java.util.List.of(devolucao));
                
                when(devolucaoRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

                // Quando buscar devoluções
                org.springframework.data.domain.Page<DevolucaoDTO.DevolucaoResumo> resultado = 
                        devolucaoService.buscarDevolucoes(org.springframework.data.domain.Pageable.unpaged());

                // Então deve retornar devoluções
                assertEquals(1, resultado.getContent().size());
                assertEquals(1L, resultado.getContent().get(0).getId());
        }

        @Test
        void testBuscarPorId() {
                // Dado uma devolução existente
                com.tcc.estoque.model.Devolucao devolucao = new com.tcc.estoque.model.Devolucao();
                devolucao.setId(1L);
                devolucao.setTipoDevolucao(com.tcc.estoque.model.enums.TipoDevolucao.DEVOLUCAO_SIMPLES);
                devolucao.setMotivo("Não gostou");
                devolucao.setValorDevolucao(new BigDecimal("100.00"));
                devolucao.setDataDevolucao(java.time.LocalDateTime.now());
                devolucao.setItens(java.util.Collections.emptyList());
                
                Venda venda = new Venda();
                venda.setId(1L);
                venda.setClienteNome("Cliente Teste");
                devolucao.setVenda(venda);
                
                Usuario usuario = new Usuario();
                usuario.setNome("Funcionário");
                devolucao.setUsuario(usuario);
                
                when(devolucaoRepository.findById(1L)).thenReturn(Optional.of(devolucao));

                // Quando buscar por ID
                DevolucaoDTO.DevolucaoResponse resultado = devolucaoService.buscarPorId(1L);

                // Então deve retornar a devolução
                assertEquals(1L, resultado.getId());
                assertEquals("Não gostou", resultado.getMotivo());
                assertEquals(new BigDecimal("100.00"), resultado.getValorDevolucao());
        }

        @Test
        void testBuscarPorIdNaoEncontrada() {
                // Dado que a devolução não existe
                when(devolucaoRepository.findById(99L)).thenReturn(Optional.empty());

                // Quando buscar por ID inexistente
                RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                        devolucaoService.buscarPorId(99L);
                });

                // Então deve lançar exceção
                assertEquals("Devolução não encontrada", exception.getMessage());
        }

        @Test
        void testBuscarPorVenda() {
                // Dado uma venda com devoluções
                Venda venda = new Venda();
                venda.setId(1L);
                venda.setClienteNome("Cliente Teste");
                
                com.tcc.estoque.model.Devolucao devolucao = new com.tcc.estoque.model.Devolucao();
                devolucao.setId(1L);
                devolucao.setVenda(venda);
                devolucao.setTipoDevolucao(com.tcc.estoque.model.enums.TipoDevolucao.DEVOLUCAO_SIMPLES);
                devolucao.setMotivo("Defeito");
                devolucao.setValorDevolucao(new BigDecimal("75.00"));
                devolucao.setDataDevolucao(java.time.LocalDateTime.now());
                devolucao.setItens(java.util.Collections.emptyList());
                
                Usuario usuario = new Usuario();
                usuario.setNome("Funcionário");
                devolucao.setUsuario(usuario);

                when(vendaRepository.findById(1L)).thenReturn(Optional.of(venda));
                when(devolucaoRepository.findByVenda(venda)).thenReturn(java.util.List.of(devolucao));

                // Quando buscar por venda
                List<DevolucaoDTO.DevolucaoResponse> resultado = devolucaoService.buscarPorVenda(1L);

                // Então deve retornar devoluções da venda
                assertEquals(1, resultado.size());
                assertEquals(1L, resultado.get(0).getId());
                assertEquals("Defeito", resultado.get(0).getMotivo());
        }

        @Test
        void testBuscarPorVendaInexistente() {
                // Dado que a venda não existe
                when(vendaRepository.findById(99L)).thenReturn(Optional.empty());

                // Quando buscar por venda inexistente
                RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                        devolucaoService.buscarPorVenda(99L);
                });

                // Então deve lançar exceção
                assertEquals("Venda não encontrada", exception.getMessage());
        }

        @Test
        void testObterEstatisticas() {
                // Dado devoluções no período
                java.time.LocalDateTime inicio = java.time.LocalDateTime.now().minusDays(7);
                java.time.LocalDateTime fim = java.time.LocalDateTime.now();
                
                com.tcc.estoque.model.Devolucao dev1 = new com.tcc.estoque.model.Devolucao();
                dev1.setTipoDevolucao(com.tcc.estoque.model.enums.TipoDevolucao.DEVOLUCAO_SIMPLES);
                dev1.setValorDevolucao(new BigDecimal("50.00"));
                
                com.tcc.estoque.model.Devolucao dev2 = new com.tcc.estoque.model.Devolucao();
                dev2.setTipoDevolucao(com.tcc.estoque.model.enums.TipoDevolucao.TROCA_POR_DEFEITO);
                dev2.setValorDevolucao(new BigDecimal("100.00"));
                
                com.tcc.estoque.model.Devolucao dev3 = new com.tcc.estoque.model.Devolucao();
                dev3.setTipoDevolucao(com.tcc.estoque.model.enums.TipoDevolucao.TROCA_POR_TAMANHO);
                dev3.setValorDevolucao(new BigDecimal("75.00"));

                List<com.tcc.estoque.model.Devolucao> devolucoes = Arrays.asList(dev1, dev2, dev3);
                
                when(devolucaoRepository.findByPeriodo(inicio, fim)).thenReturn(devolucoes);
                when(itemDevolucaoRepository.findByStatusQualidadeRetorno(StatusQualidade.DEFEITUOSO))
                        .thenReturn(Arrays.asList(new ItemDevolucao(), new ItemDevolucao()));

                // Quando obter estatísticas
                DevolucaoDTO.EstatisticasDevolucao resultado = devolucaoService.obterEstatisticas(inicio, fim);

                // Então deve retornar estatísticas corretas
                assertEquals(3L, resultado.getTotalDevolucoes());
                assertEquals(1L, resultado.getDevolucoesSimples());
                assertEquals(1L, resultado.getTrocasPorDefeito());
                assertEquals(1L, resultado.getTrocasPorTamanho());
                assertEquals(new BigDecimal("225.00"), resultado.getValorTotalDevolvido());
                assertEquals(2, resultado.getProdutosDefeituosos());
        }

        @Test
        void testListarProdutosDefeituosos() {
                // Dado produtos defeituosos
                Produto produto1 = Produto.builder()
                        .id(1L)
                        .nome("Produto Defeituoso 1")
                        .codigo("DEF001")
                        .preco(new BigDecimal("100.00"))
                        .build();
                
                Produto produto2 = Produto.builder()
                        .id(2L)
                        .nome("Produto Defeituoso 2")
                        .codigo("DEF002")
                        .preco(new BigDecimal("200.00"))
                        .build();

                ItemDevolucao item1 = new ItemDevolucao();
                item1.setProduto(produto1);
                item1.setQuantidade(2);
                item1.setCreatedAt(java.time.LocalDateTime.now());
                
                ItemDevolucao item2 = new ItemDevolucao();
                item2.setProduto(produto1);
                item2.setQuantidade(1);
                item2.setCreatedAt(java.time.LocalDateTime.now().minusDays(1));
                
                ItemDevolucao item3 = new ItemDevolucao();
                item3.setProduto(produto2);
                item3.setQuantidade(1);
                item3.setCreatedAt(java.time.LocalDateTime.now());

                when(itemDevolucaoRepository.findByStatusQualidadeRetorno(StatusQualidade.DEFEITUOSO))
                        .thenReturn(Arrays.asList(item1, item2, item3));

                // Quando listar produtos defeituosos
                List<DevolucaoDTO.ProdutoDefeitosoResumo> resultado = devolucaoService.listarProdutosDefeituosos();

                // Então deve agrupar e calcular corretamente
                assertEquals(2, resultado.size());
                
                DevolucaoDTO.ProdutoDefeitosoResumo resumo1 = resultado.stream()
                        .filter(r -> r.getProdutoId().equals(1L))
                        .findFirst().orElse(null);
                        
                assertNotNull(resumo1);
                assertEquals("Produto Defeituoso 1", resumo1.getNomeProduto());
                assertEquals(Integer.valueOf(3), resumo1.getQuantidadeDefeituosa()); // 2 + 1
                assertTrue(resumo1.getPrecoOriginal().compareTo(new BigDecimal("100.00")) == 0);
                assertTrue(resumo1.getPrecoDefeituoso().compareTo(new BigDecimal("70.00")) == 0); // 100 - 30%
        }
}

