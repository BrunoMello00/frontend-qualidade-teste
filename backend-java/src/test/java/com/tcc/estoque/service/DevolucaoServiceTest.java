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
}

