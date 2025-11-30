package com.tcc.estoque.service;

import com.tcc.estoque.dto.DevolucaoDTO;
import com.tcc.estoque.model.ItemDevolucao;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.model.enums.StatusQualidade;
import com.tcc.estoque.repository.*;
import com.tcc.estoque.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

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

    @Test
    void testListarProdutosDefeituosos() {
        // Given
        Produto produto = Produto.builder()
                .id(1L)
                .nome("Produto Teste")
                .preco(BigDecimal.valueOf(100.00))
                .build();

        ItemDevolucao itemDefeituoso = ItemDevolucao.builder()
                .id(1L)
                .produto(produto)
                .quantidade(2)
                .statusQualidadeRetorno(StatusQualidade.DEFEITUOSO)
                .build();

        when(itemDevolucaoRepository.findByStatusQualidadeRetorno(StatusQualidade.DEFEITUOSO))
                .thenReturn(Arrays.asList(itemDefeituoso));

        // When
        List<DevolucaoDTO.ProdutoDefeitosoResumo> resultado = devolucaoService.listarProdutosDefeituosos();

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        
        DevolucaoDTO.ProdutoDefeitosoResumo resumo = resultado.get(0);
        assertEquals(produto.getId(), resumo.getProdutoId());
        assertEquals(produto.getNome(), resumo.getNomeProduto());
        assertEquals(2, resumo.getQuantidadeDefeituosa());
    }

    @Test
    void testListarProdutosDefeituosos_ListaVazia() {
        // Given
        when(itemDevolucaoRepository.findByStatusQualidadeRetorno(StatusQualidade.DEFEITUOSO))
                .thenReturn(Arrays.asList());

        // When
        List<DevolucaoDTO.ProdutoDefeitosoResumo> resultado = devolucaoService.listarProdutosDefeituosos();

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}