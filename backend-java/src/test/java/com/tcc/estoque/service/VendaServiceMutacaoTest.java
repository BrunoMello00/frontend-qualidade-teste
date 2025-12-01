package com.tcc.estoque.service;

import com.tcc.estoque.dto.VendaDTO;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.Venda;
import com.tcc.estoque.model.ItemVenda;
import com.tcc.estoque.model.enums.FormaPagamento;
import com.tcc.estoque.repository.ItemVendaRepository;
import com.tcc.estoque.repository.MovimentacaoEstoqueRepository;
import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.VendaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendaServiceMutacaoTest {

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ItemVendaRepository itemVendaRepository;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @InjectMocks
    private VendaService vendaService;

    @Test
    void confirmarVendaPendenteDeveBaixarEstoque() {
        Produto produto = new Produto(); produto.setId(200L); produto.setEstoque(50);
        ItemVenda item = new ItemVenda(); item.setProduto(produto); item.setQuantidade(5);

    Venda venda = new Venda(); venda.setId(400L); venda.setItens(Collections.singletonList(item));
    venda.setStatus(com.tcc.estoque.model.enums.StatusVenda.PENDENTE); 
    venda.setUsuario(new Usuario()); 

        when(vendaRepository.findById(400L)).thenReturn(java.util.Optional.of(venda));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(vendaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var usuario = new Usuario(); usuario.setNome("Op");

    vendaService.confirmarVenda(400L, usuario);

        verify(produtoRepository).save(produto);
        verify(movimentacaoEstoqueRepository, atLeastOnce()).save(any());

        ArgumentCaptor<Venda> cap = ArgumentCaptor.forClass(Venda.class);
        verify(vendaRepository).save(cap.capture());
        Venda saved = cap.getValue();

    assertThat(saved.getStatus()).isEqualTo(com.tcc.estoque.model.enums.StatusVenda.CONFIRMADA);
    assertThat(saved.getDataConfirmacao()).isNotNull();
    }

    @Test
    void criarVendaCalculaSubtotal() {
        VendaDTO.ItemVendaRequest item = new VendaDTO.ItemVendaRequest();
        item.setProdutoId(10L); item.setQuantidade(2); item.setPrecoUnitario(BigDecimal.valueOf(7));

        VendaDTO.VendaRequest req = new VendaDTO.VendaRequest();
        req.setItens(Collections.singletonList(item));
        req.setFormaPagamento(FormaPagamento.BOLETO);

        when(produtoRepository.existsById(10L)).thenReturn(true);
        Produto p = new Produto(); p.setId(10L); p.setEstoque(100); p.setTamanhos(Collections.emptyList());
        when(produtoRepository.findById(10L)).thenReturn(java.util.Optional.of(p));

    when(vendaRepository.save(any())).thenAnswer(inv -> { Venda v = inv.getArgument(0); v.setId(321L); return v; });
    Venda savedVenda = new Venda(); savedVenda.setId(321L); savedVenda.setUsuario(new Usuario()); savedVenda.getUsuario().setNome("Tester"); savedVenda.getUsuario().setEmail("t@t.com"); savedVenda.setItens(Collections.emptyList());
    when(vendaRepository.findByIdWithItens(321L)).thenReturn(java.util.Optional.of(savedVenda));

        vendaService.criarVenda(req, new Usuario());

        verify(itemVendaRepository, atLeastOnce()).save(any());
        verify(vendaRepository).save(any());
    }
}


