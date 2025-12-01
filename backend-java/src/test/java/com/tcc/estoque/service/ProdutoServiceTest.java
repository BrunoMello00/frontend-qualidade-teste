package com.tcc.estoque.service;

import com.tcc.estoque.dto.ProdutoDTO;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.TamanhoProdutoRepository;
import com.tcc.estoque.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private TamanhoProdutoRepository tamanhoProdutoRepository;
    @Mock
    private RecordLockService recordLockService;
    @Mock
    private CodigoBarrasService codigoBarrasService;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private ProdutoService produtoService;

    @BeforeEach
    void configurar() {
    }

    @Test
    void buscarProdutoPorCodigo() {
        Produto produto = Produto.builder()
                .id(1L)
                .nome("Prod A")
                .preco(BigDecimal.valueOf(10.0))
                .estoque(5)
                .codigo("C123")
                .ativo(true)
                .build();

        when(produtoRepository.findByCodigo("C123")).thenReturn(Optional.of(produto));

        ProdutoDTO.ProdutoResponse resp = produtoService.buscarPorCodigoBarras("C123");

        assertThat(resp.getCodigoBarras()).isEqualTo("C123");
        assertThat(resp.getQuantidadeEstoque()).isEqualTo(Integer.valueOf(5));
    }

    @Test
    void atualizarEstoque() {
        Produto produto = Produto.builder()
                .id(2L)
                .nome("Prod B")
                .preco(BigDecimal.valueOf(20.0))
                .estoque(3)
                .codigo("C456")
                .ativo(true)
                .build();

        when(produtoRepository.findById(2L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProdutoDTO.EstoqueRequest req = ProdutoDTO.EstoqueRequest.builder().quantidade(10).build();

        ProdutoDTO.ProdutoResponse resp = produtoService.atualizarEstoque(2L, req);

        assertThat(resp.getQuantidadeEstoque()).isEqualTo(Integer.valueOf(10));
    }

    @Test
    void obterEstatisticas() {
        when(produtoRepository.count()).thenReturn(5L);
        when(produtoRepository.countByAtivoTrue()).thenReturn(4L);
        when(produtoRepository.countProdutosComEstoqueBaixo()).thenReturn(1L);

        ProdutoDTO.EstatisticasResponse stats = (ProdutoDTO.EstatisticasResponse) produtoService.obterEstatisticas();

        assertThat(stats.getTotalProdutos()).isEqualTo(5L);
        assertThat(stats.getProdutosAtivos()).isEqualTo(4L);
        assertThat(stats.getProdutosEstoqueBaixo()).isEqualTo(1L);
    }

    @Test
    void gerarCodigoPersonalizado() {
        when(codigoBarrasService.gerarProximoCodigo(ArgumentMatchers.any(), ArgumentMatchers.eq("PFX")))
                .thenReturn("PFX-12345");

        String codigo = produtoService.gerarCodigoPersonalizado(null, "PFX");

        assertThat(codigo).isEqualTo("PFX-12345");
        verify(codigoBarrasService).gerarProximoCodigo(null, "PFX");
    }

    @Test
    void adquirirLockProdutoInexistente() {
        when(produtoRepository.existsById(99L)).thenReturn(false);

        ProdutoDTO.LockResponse resp = produtoService.adquirirLockProduto(99L, 1L, "ip", "ua");

        assertThat(resp.getSucesso()).isFalse();
        assertThat(resp.getMensagem()).containsIgnoringCase("não encontrado");
    }

    @Test
    void adquirirLockProdutoProdutoExiste() {
        when(produtoRepository.existsById(5L)).thenReturn(true);
        when(recordLockService.isProdutoBloqueado(5L)).thenReturn(false);
        when(recordLockService.adquirirLockProduto(5L, 2L, "ip", "ua")).thenReturn(true);

        ProdutoDTO.LockResponse resp = produtoService.adquirirLockProduto(5L, 2L, "ip", "ua");

        assertThat(resp.getSucesso()).isTrue();
        assertThat(resp.getPodeEditar()).isTrue();
    }

    @Test
    void convertToResponseSemBloqueio() {
        Produto produto = Produto.builder()
                .id(10L)
                .nome("Prod C")
                .preco(BigDecimal.valueOf(15.0))
                .estoque(2)
                .codigo("C789")
                .ativo(true)
                .build();

        when(produtoRepository.findByCodigo("C789")).thenReturn(Optional.of(produto));
        when(recordLockService.obterInfoLockProduto(10L)).thenReturn(null);

        ProdutoDTO.ProdutoResponse resp = produtoService.buscarPorCodigoBarras("C789");

    assertThat(resp.getBloqueado()).isFalse();
        assertThat(resp.getBloqueadoPorUsuario()).isNull();
        assertThat(resp.getBloqueioExpiraEm()).isNull();
    }

    @Test
    void convertToResponseComBloqueioSemExpiracao() {
        Produto produto = Produto.builder()
                .id(11L)
                .nome("Prod D")
                .preco(BigDecimal.valueOf(18.0))
                .estoque(7)
                .codigo("C890")
                .ativo(true)
                .build();

        Map<String, Object> lock = new HashMap<>();
        lock.put("usuario_nome", "Joao");
        lock.put("data_expiracao", null);

        when(produtoRepository.findByCodigo("C890")).thenReturn(Optional.of(produto));
        when(recordLockService.obterInfoLockProduto(11L)).thenReturn(lock);

        ProdutoDTO.ProdutoResponse resp = produtoService.buscarPorCodigoBarras("C890");

    assertThat(resp.getBloqueado()).isTrue();
        assertThat(resp.getBloqueadoPorUsuario()).isEqualTo("Joao");
        assertThat(resp.getBloqueioExpiraEm()).isNull();
    }

    @Test
    void convertToResponseComBloqueioComExpiracao() {
        Produto produto = Produto.builder()
                .id(12L)
                .nome("Prod E")
                .preco(BigDecimal.valueOf(25.0))
                .estoque(12)
                .codigo("C901")
                .ativo(true)
                .build();

        Timestamp ts = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        Map<String, Object> lock = new HashMap<>();
        lock.put("usuario_nome", "Maria");
        lock.put("data_expiracao", ts);

        when(produtoRepository.findByCodigo("C901")).thenReturn(Optional.of(produto));
        when(recordLockService.obterInfoLockProduto(12L)).thenReturn(lock);

        ProdutoDTO.ProdutoResponse resp = produtoService.buscarPorCodigoBarras("C901");

    assertThat(resp.getBloqueado()).isTrue();
        assertThat(resp.getBloqueadoPorUsuario()).isEqualTo("Maria");
        assertThat(resp.getBloqueioExpiraEm()).isNotNull();
    }

    

    @Test
    void criarProdutoSemMaxCodigoInterno() {
        ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder()
                .nome("Pseq")
                .codigoBarras("ABC123")
                .build();

        when(codigoBarrasService.detectarTipoCodigo(anyString())).thenReturn(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO);
        when(codigoBarrasService.validarCodigoBarras(anyString(), any())).thenReturn(true);
        when(produtoRepository.findMaxCodigoInternoSequencial()).thenReturn(null);
        when(securityUtil.getUsuarioLogado()).thenReturn(new com.tcc.estoque.model.Usuario());
        when(codigoBarrasService.gerarProximoCodigoResumido()).thenReturn("R001");
        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(30L);
            return p;
        });

        ProdutoDTO.ProdutoResponse resp = produtoService.criarProduto(req);

        assertThat(resp.getCodigoInternoSequencial()).isEqualTo(1L);
    }

    @Test
    void excluirProduto() {
        Produto produto = Produto.builder().id(40L).ativo(true).build();
        when(produtoRepository.findById(40L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        produtoService.excluirProduto(40L);

        assertThat(produto.getAtivo()).isFalse();
    }

    @Test
    void listarDepartamentos() {
        when(produtoRepository.findDistinctDepartamentos()).thenReturn(java.util.List.of("A","B"));

        var res = produtoService.listarDepartamentos();

        assertThat(res).containsExactly("A","B");
    }

    @Test
    void buscarProdutosEstoqueBaixo() {
        Produto p1 = Produto.builder().id(1L).nome("p1").estoque(1).build();
        Produto p2 = Produto.builder().id(2L).nome("p2").estoque(1).build();
        Produto p3 = Produto.builder().id(3L).nome("p3").estoque(1).build();

        when(produtoRepository.findProdutosComEstoqueBaixo()).thenReturn(java.util.List.of(p1,p2,p3));

        var res = produtoService.buscarProdutosEstoqueBaixo(2);

        assertThat(res).hasSize(2);
    }

    

    @Test
    void processarCodigoBarrasCodigoManualInvalido() {
        ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder()
                .nome("Pinv")
                .codigoBarras("BADCODE")
                .build();

        when(codigoBarrasService.detectarTipoCodigo(anyString())).thenReturn(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO);
        when(codigoBarrasService.validarCodigoBarras(anyString(), any())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> produtoService.criarProduto(req));
    }

    
    @Test
    void verificarStatusLockQuandoBloqueado() {
        Map<String,Object> lock = new HashMap<>();
        lock.put("usuario_nome","Z");
        lock.put("data_expiracao", Timestamp.valueOf(LocalDateTime.now().plusMinutes(5)));

        when(recordLockService.obterInfoLockProduto(70L)).thenReturn(lock);
        when(recordLockService.usuarioTemLockProduto(70L, 4L)).thenReturn(false);

        var resp = produtoService.verificarStatusLock(70L, 4L);

        assertThat(resp.getSucesso()).isTrue();
        assertThat(resp.getBloqueadoPorUsuario()).isEqualTo("Z");
    }

    @Test
    void atualizarQualidadeProduto() {
        Produto produto = Produto.builder().id(80L).nome("q").ativo(true).build();
        when(produtoRepository.findById(80L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProdutoDTO.QualidadeRequest req = ProdutoDTO.QualidadeRequest.builder()
        .statusQualidade(com.tcc.estoque.model.enums.StatusQualidade.NORMAL)
                .observacoes("ok")
                .build();

        produtoService.atualizarQualidadeProduto(80L, req, 1L, "ip", "ua");

        verify(produtoRepository).save(any());
    }

    @Test
    void buscarPorCodigoResumido() {
        java.util.Map<String,Object> mapa = new java.util.HashMap<>();
        mapa.put("id", 90L);

        when(codigoBarrasService.buscarPorCodigoResumido("R90")).thenReturn(Optional.of(mapa));
        when(produtoRepository.findByIdWithTamanhos(90L)).thenReturn(Optional.of(Produto.builder().id(90L).nome("p").codigo("C").ativo(true).build()));
        when(recordLockService.obterInfoLockProduto(anyLong())).thenReturn(null);

        var resp = produtoService.buscarPorCodigoResumido("R90");

        assertThat(resp.getId()).isEqualTo(90L);
    }

    @Test
    void listarTiposCodigoSuportados() {
        when(codigoBarrasService.listarTiposCodigoBarras()).thenReturn(java.util.List.of(java.util.Map.of("k","v")));

        var res = produtoService.listarTiposCodigoSuportados();

        assertThat(res).hasSize(1);
    }

    

    @Test
    void listarProdutos() {
        Produto p = Produto.builder().id(102L).nome("x").estoque(2).codigo("A").ativo(true).build();
        when(produtoRepository.findDistinctByTamanhos_TamanhoIgnoreCaseAndDepartamentoContainingIgnoreCaseAndAtivoTrue(
                eq("M"), eq("D"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(p)));
        when(recordLockService.obterInfoLockProduto(anyLong())).thenReturn(null);

        var page = produtoService.listarProdutos(Pageable.unpaged(), "D", null, "M");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listarProdutosPorNomeETamanho() {
        Produto p = Produto.builder().id(103L).nome("x").estoque(2).codigo("A").ativo(true).build();
        when(produtoRepository.findDistinctByTamanhos_TamanhoIgnoreCaseAndNomeContainingIgnoreCaseAndAtivoTrue(
                eq("M"), eq("N"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(p)));
        when(recordLockService.obterInfoLockProduto(anyLong())).thenReturn(null);

        var page = produtoService.listarProdutos(Pageable.unpaged(), null, "N", "M");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listarProdutosPorTamanho() {
        Produto p = Produto.builder().id(104L).nome("x").estoque(2).codigo("A").ativo(true).build();
        when(produtoRepository.findDistinctByTamanhos_TamanhoIgnoreCaseAndAtivoTrue(eq("M"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(p)));
        when(recordLockService.obterInfoLockProduto(anyLong())).thenReturn(null);

        var page = produtoService.listarProdutos(Pageable.unpaged(), null, null, "M");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listarProdutosPorDepartamentoENome() {
        Produto p = Produto.builder().id(105L).nome("x").estoque(2).codigo("A").ativo(true).build();
        when(produtoRepository.findByAtivoTrueAndDepartamentoContainingIgnoreCaseAndNomeContainingIgnoreCase(eq("D"), eq("N"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(p)));
        when(recordLockService.obterInfoLockProduto(anyLong())).thenReturn(null);

        var page = produtoService.listarProdutos(Pageable.unpaged(), "D", "N", null);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listarProdutosPorDepartamento() {
        Produto p = Produto.builder().id(106L).nome("x").estoque(2).codigo("A").ativo(true).build();
        when(produtoRepository.findByAtivoTrueAndDepartamentoContainingIgnoreCase(eq("D"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(p)));
        when(recordLockService.obterInfoLockProduto(anyLong())).thenReturn(null);

        var page = produtoService.listarProdutos(Pageable.unpaged(), "D", null, null);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listarProdutosPorNome() {
        Produto p = Produto.builder().id(107L).nome("x").estoque(2).codigo("A").ativo(true).build();
        when(produtoRepository.findByAtivoTrueAndNomeContainingIgnoreCase(eq("N"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(p)));
        when(recordLockService.obterInfoLockProduto(anyLong())).thenReturn(null);

        var page = produtoService.listarProdutos(Pageable.unpaged(), null, "N", null);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void criarProdutoComTamanhos() {
    ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder()
        .nome("PcomT")
        .tamanhos(java.util.List.of(
            com.tcc.estoque.dto.TamanhoProdutoDTO.builder().tamanho("M").quantidade(5).build(),
            com.tcc.estoque.dto.TamanhoProdutoDTO.builder().tamanho("G").quantidade(3).build()
        ))
        .build();

    when(codigoBarrasService.gerarProximoCodigo(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn("X1");
    when(securityUtil.getUsuarioLogado()).thenReturn(new com.tcc.estoque.model.Usuario());
    when(codigoBarrasService.gerarProximoCodigoResumido()).thenReturn("R001");
    when(produtoRepository.findMaxCodigoInternoSequencial()).thenReturn(2L);

    when(produtoRepository.save(any())).thenAnswer(inv -> {
        Produto p = inv.getArgument(0);
        p.setId(200L);
        return p;
    });

    Produto produtoComTamanhos = Produto.builder()
        .id(200L)
        .nome("PcomT")
        .codigo("X1")
        .ativo(true)
        .tamanhos(java.util.List.of(
            com.tcc.estoque.model.TamanhoProduto.builder().id(1L).tamanho("M").estoque(5).build(),
            com.tcc.estoque.model.TamanhoProduto.builder().id(2L).tamanho("G").estoque(3).build()
        ))
        .build();

    when(produtoRepository.findByIdWithTamanhos(200L)).thenReturn(Optional.of(produtoComTamanhos));

    ProdutoDTO.ProdutoResponse resp = produtoService.criarProduto(req);

    verify(tamanhoProdutoRepository, times(2)).save(any());
    assertThat(resp.getTamanhos()).hasSize(2);
    }

    @Test
    void atualizarProduto() {
    Produto produto = Produto.builder()
        .id(300L)
        .nome("old")
        .codigo("C300")
        .ativo(true)
        .tamanhos(java.util.List.of(
            com.tcc.estoque.model.TamanhoProduto.builder().id(1L).tamanho("M").estoque(5).build()
        ))
        .build();

    when(produtoRepository.findById(300L)).thenReturn(Optional.of(produto));
    when(recordLockService.isProdutoBloqueado(300L)).thenReturn(false);
    when(recordLockService.usuarioTemLockProduto(300L, 5L)).thenReturn(false);
    when(recordLockService.adquirirLockProduto(300L, 5L, "ip", "ua")).thenReturn(true);

    when(tamanhoProdutoRepository.findByProdutoId(300L)).thenReturn(java.util.List.of(
        com.tcc.estoque.model.TamanhoProduto.builder().id(1L).tamanho("M").estoque(5).build()
    ));

    when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Produto produtoComNovosTamanhos = Produto.builder()
        .id(300L)
        .nome("new")
        .codigo("C300")
        .ativo(true)
        .tamanhos(java.util.List.of(
            com.tcc.estoque.model.TamanhoProduto.builder().id(10L).tamanho("M").estoque(0).build(),
            com.tcc.estoque.model.TamanhoProduto.builder().id(11L).tamanho("G").estoque(0).build()
        ))
        .build();

    when(produtoRepository.findByIdWithTamanhos(300L)).thenReturn(Optional.of(produtoComNovosTamanhos));

    ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder()
        .nome("new")
        .tamanhos(java.util.List.of(
            com.tcc.estoque.dto.TamanhoProdutoDTO.builder().tamanho("M").build(),
            com.tcc.estoque.dto.TamanhoProdutoDTO.builder().tamanho("G").build()
        ))
        .build();

    var resp = produtoService.atualizarProduto(300L, req, 5L, "ip", "ua");

    verify(tamanhoProdutoRepository).findByProdutoId(300L);
    verify(tamanhoProdutoRepository, atLeastOnce()).delete(any());
    verify(tamanhoProdutoRepository, times(2)).save(any());
    verify(recordLockService).liberarLockProduto(300L, 5L);
    assertThat(resp.getTamanhos()).hasSize(2);
    }

    @Test
    void convertToResponseComTamanhos() {
    Produto produto = Produto.builder()
        .id(400L)
        .nome("pcomt")
        .codigo("C400")
        .ativo(true)
        .tamanhos(java.util.List.of(
            com.tcc.estoque.model.TamanhoProduto.builder().id(21L).tamanho("P").estoque(2).build(),
            com.tcc.estoque.model.TamanhoProduto.builder().id(22L).tamanho("M").estoque(3).build()
        ))
        .build();

    when(produtoRepository.findByIdWithTamanhos(400L)).thenReturn(Optional.of(produto));
    when(recordLockService.obterInfoLockProduto(anyLong())).thenReturn(null);

    var resp = produtoService.buscarPorId(400L);

    assertThat(resp.getTamanhos()).hasSize(2);
    assertThat(resp.getQuantidadeEstoque()).isEqualTo(Integer.valueOf(5));
    }
}


