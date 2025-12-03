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

    @Test
    void buscarProdutoNaoEncontrado() {
        lenient().when(produtoRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> produtoService.buscarPorId(9999L));
    }

    @Test
    void buscarPorCodigoBarrasNaoEncontrado() {
        lenient().when(produtoRepository.findByCodigo("INEXISTENTE")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> produtoService.buscarPorCodigoBarras("INEXISTENTE"));
    }

    @Test
    void criarProdutoComCodigoJaExistente() {
        ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder()
                .nome("Duplicado")
                .codigoBarras("EXIST123")
                .build();

        lenient().when(produtoRepository.existsByCodigo("EXIST123")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> produtoService.criarProduto(req));
    }

    @Test
    void atualizarEstoqueProdutoInexistente() {
        lenient().when(produtoRepository.findById(8888L)).thenReturn(Optional.empty());

        ProdutoDTO.EstoqueRequest req = ProdutoDTO.EstoqueRequest.builder().quantidade(10).build();

        assertThrows(RuntimeException.class, () -> produtoService.atualizarEstoque(8888L, req));
    }

    @Test
    void excluirProdutoInexistente() {
        lenient().when(produtoRepository.findById(7777L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> produtoService.excluirProduto(7777L));
    }

    @Test
    void criarProdutoComDepartamentoEDescricao() {
        ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder()
                .nome("Produto Completo")
                .descricao("Descrição detalhada do produto")
                .departamento("Eletrônicos")
                .preco(BigDecimal.valueOf(199.99))
                .estoqueMinimo(5)
                .pontuacaoProduto(15)
                .build();

        when(codigoBarrasService.gerarProximoCodigo(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn("AUTO001");
        when(securityUtil.getUsuarioLogado()).thenReturn(new com.tcc.estoque.model.Usuario());
        when(codigoBarrasService.gerarProximoCodigoResumido()).thenReturn("R002");
        when(produtoRepository.findMaxCodigoInternoSequencial()).thenReturn(5L);
        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(501L);
            return p;
        });

        Produto produtoCompleto = Produto.builder()
                .id(501L)
                .nome("Produto Completo")
                .descricao("Descrição detalhada do produto")
                .departamento("Eletrônicos")
                .codigo("AUTO001")
                .preco(BigDecimal.valueOf(199.99))
                .ativo(true)
                .tamanhos(java.util.Collections.emptyList())
                .build();



        ProdutoDTO.ProdutoResponse resp = produtoService.criarProduto(req);

        assertThat(resp.getId()).isEqualTo(501L);
        assertThat(resp.getNome()).isEqualTo("Produto Completo");
        assertThat(resp.getDescricao()).isEqualTo("Descrição detalhada do produto");
        assertThat(resp.getDepartamento()).isEqualTo("Eletrônicos");
    }

    @Test
    void atualizarProdutoBloqueado() {
        when(produtoRepository.findById(600L)).thenReturn(Optional.of(Produto.builder().id(600L).build()));
        when(recordLockService.isProdutoBloqueado(600L)).thenReturn(true);

        ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder().nome("Teste").build();

        assertThrows(RuntimeException.class, () -> produtoService.atualizarProduto(600L, req, 1L, "ip", "ua"));
    }

    @Test
    void atualizarProdutoSemLock() {
        Produto produto = Produto.builder().id(700L).nome("Original").ativo(true).build();
        when(produtoRepository.findById(700L)).thenReturn(Optional.of(produto));
        when(recordLockService.isProdutoBloqueado(700L)).thenReturn(false);
        when(recordLockService.usuarioTemLockProduto(700L, 2L)).thenReturn(false);
        when(recordLockService.adquirirLockProduto(700L, 2L, "ip", "ua")).thenReturn(false);

        ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder().nome("Novo").build();

        assertThrows(RuntimeException.class, () -> produtoService.atualizarProduto(700L, req, 2L, "ip", "ua"));
    }

    @Test
    void verificarStatusLockProdutoNaoEncontrado() {
        lenient().when(produtoRepository.existsById(900L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> produtoService.verificarStatusLock(900L, 1L));
    }

    @Test
    void buscarPorCodigoResumidoNaoEncontrado() {
        lenient().when(codigoBarrasService.buscarPorCodigoResumido("R999")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> produtoService.buscarPorCodigoResumido("R999"));
    }

    @Test
    void criarProdutoComMaxCodigoInternoExistente() {
        ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder()
                .nome("Com Sequencial")
                .build();

        lenient().when(codigoBarrasService.gerarProximoCodigo(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn("SEQ001");
        when(securityUtil.getUsuarioLogado()).thenReturn(new com.tcc.estoque.model.Usuario());
        when(codigoBarrasService.gerarProximoCodigoResumido()).thenReturn("R003");
        when(produtoRepository.findMaxCodigoInternoSequencial()).thenReturn(10L);
        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(1001L);
            return p;
        });

        Produto produto = Produto.builder()
                .id(1001L)
                .nome("Com Sequencial")
                .codigo("SEQ001")
                .ativo(true)
                .tamanhos(java.util.Collections.emptyList())
                .build();



        ProdutoDTO.ProdutoResponse resp = produtoService.criarProduto(req);

        assertThat(resp.getCodigoInternoSequencial()).isEqualTo(11L); // 10 + 1
    }

    @Test
    void processarCodigoBarrasPersonalizado() {
        ProdutoDTO.ProdutoRequest req = ProdutoDTO.ProdutoRequest.builder()
                .nome("Produto Personalizado")
                .codigoBarras("CUSTOM123")
                .build();

        when(codigoBarrasService.detectarTipoCodigo("CUSTOM123")).thenReturn(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO);
        when(codigoBarrasService.validarCodigoBarras("CUSTOM123", com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO)).thenReturn(true);
        when(produtoRepository.existsByCodigo("CUSTOM123")).thenReturn(false);
        when(securityUtil.getUsuarioLogado()).thenReturn(new com.tcc.estoque.model.Usuario());
        when(codigoBarrasService.gerarProximoCodigoResumido()).thenReturn("R004");
        when(produtoRepository.findMaxCodigoInternoSequencial()).thenReturn(null);
        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(1002L);
            return p;
        });

        Produto produto = Produto.builder()
                .id(1002L)
                .nome("Produto Personalizado")
                .codigo("CUSTOM123")
                .ativo(true)
                .tamanhos(java.util.Collections.emptyList())
                .build();



        ProdutoDTO.ProdutoResponse resp = produtoService.criarProduto(req);

        assertThat(resp.getCodigoBarras()).isEqualTo("CUSTOM123");
    }

    @Test
    void reativarProduto() {
        // Dado um produto existente inativo
        Produto produto = Produto.builder()
                .id(1L)
                .nome("Produto Inativo")
                .ativo(false)
                .build();
        
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);

        // Quando reativado
        produtoService.reativarProduto(1L);

        // Então deve ficar ativo
        verify(produtoRepository).save(argThat(p -> p.getAtivo()));
    }

    @Test 
    void liberarLockProduto() {
        // Quando liberar lock
        produtoService.liberarLockProduto(1L, 100L);

        // Então deve liberar o lock
        verify(recordLockService).liberarLockProduto(1L, 100L);
    }

    @Test
    void listarProdutosDesabilitados() {
        // Dado produtos desabilitados
        Produto produto = Produto.builder()
                .id(1L)
                .nome("Produto Desabilitado")
                .ativo(false)
                .build();
        
        PageImpl<Produto> page = new PageImpl<>(java.util.List.of(produto));
        when(produtoRepository.findByAtivoFalse(any(Pageable.class))).thenReturn(page);

        // Quando listar produtos desabilitados
        var resultado = produtoService.listarProdutosDesabilitados(Pageable.unpaged());

        // Então deve retornar produtos inativos
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void atualizarProdutoComSucesso() {
        // Dado um produto existente
        Produto produto = Produto.builder()
                .id(1L)
                .nome("Produto Original")
                .codigo("ORIG123")
                .preco(new BigDecimal("10.00"))
                .departamento("01")
                .tipoCodigoBarras(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO)
                .ativo(true)
                .tamanhos(java.util.Collections.emptyList())
                .build();
        
        ProdutoDTO.ProdutoRequest request = ProdutoDTO.ProdutoRequest.builder()
                .nome("Produto Atualizado")
                .descricao("Nova descrição")
                .preco(new BigDecimal("15.00"))
                .departamento("02")
                .fornecedor("Fornecedor ABC")
                .estoqueMinimo(5)
                .custoUnitario(new BigDecimal("8.00"))
                .margem(new BigDecimal("25.0"))
                .pontosRecompensa(10)
                .build();

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(recordLockService.isProdutoBloqueado(1L)).thenReturn(false);
        when(recordLockService.usuarioTemLockProduto(1L, 100L)).thenReturn(true);
        when(produtoRepository.save(any())).thenReturn(produto);

        // Quando atualizar
        ProdutoDTO.ProdutoResponse response = produtoService.atualizarProduto(1L, request, 100L, "127.0.0.1", "TestAgent");

        // Então deve atualizar com sucesso
        verify(produtoRepository).save(argThat(p -> 
            p.getNome().equals("Produto Atualizado") &&
            p.getDescricao().equals("Nova descrição") &&
            p.getPreco().compareTo(new BigDecimal("15.00")) == 0 &&
            p.getDepartamento().equals("02") &&
            p.getFornecedor().equals("Fornecedor ABC") &&
            p.getEstoqueMinimo().equals(5) &&
            p.getCustoUnitario().compareTo(new BigDecimal("8.00")) == 0 &&
            p.getMargem().compareTo(new BigDecimal("25.0")) == 0 &&
            p.getPontosRecompensa().equals(10)
        ));
    }

    @Test
    void atualizarProdutoComCodigoBarrasDiferente() {
        // Dado um produto existente
        Produto produto = Produto.builder()
                .id(1L)
                .nome("Produto Original")
                .codigo("ORIG123")
                .tipoCodigoBarras(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO)
                .ativo(true)
                .tamanhos(java.util.Collections.emptyList())
                .build();
        
        ProdutoDTO.ProdutoRequest request = ProdutoDTO.ProdutoRequest.builder()
                .nome("Produto Atualizado")
                .codigoBarras("NOVO456")
                .tipoCodigoBarras(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO)
                .build();

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(recordLockService.isProdutoBloqueado(1L)).thenReturn(false);
        when(recordLockService.usuarioTemLockProduto(1L, 100L)).thenReturn(true);
        when(produtoRepository.existsByCodigo("NOVO456")).thenReturn(false);
        when(codigoBarrasService.validarCodigoBarras("NOVO456", com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO)).thenReturn(true);
        when(produtoRepository.save(any())).thenReturn(produto);

        // Quando atualizar
        produtoService.atualizarProduto(1L, request, 100L, "127.0.0.1", "TestAgent");

        // Então deve atualizar o código
        verify(produtoRepository).save(argThat(p -> p.getCodigo().equals("NOVO456")));
    }

    @Test
    void atualizarProdutoComDepartamentoAlterado() {
        // Dado um produto sem tamanhos no departamento 06
        Produto produto = Produto.builder()
                .id(1L)
                .nome("Produto Roupas")
                .departamento("06")
                .ativo(true)
                .tamanhos(java.util.Collections.emptyList())
                .build();
        
        ProdutoDTO.ProdutoRequest request = ProdutoDTO.ProdutoRequest.builder()
                .nome("Produto Roupas")
                .departamento("01")
                .build();

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(recordLockService.isProdutoBloqueado(1L)).thenReturn(false);
        when(recordLockService.usuarioTemLockProduto(1L, 100L)).thenReturn(true);
        when(produtoRepository.save(any())).thenReturn(produto);

        // Quando atualizar
        produtoService.atualizarProduto(1L, request, 100L, "127.0.0.1", "TestAgent");

        // Então deve permitir a alteração
        verify(produtoRepository).save(argThat(p -> p.getDepartamento().equals("01")));
    }

    @Test
    void processarCodigoBarrasComGeracaoAutomatica() {
        // Dado um request com geração automática
        ProdutoDTO.ProdutoRequest request = ProdutoDTO.ProdutoRequest.builder()
                .nome("Produto Auto")
                .gerarCodigoAutomatico(true)
                .tipoCodigoBarras(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO)
                .prefixoCodigo("AUTO")
                .build();

        when(codigoBarrasService.gerarProximoCodigo(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO, "AUTO"))
                .thenReturn("AUTO001");
        when(produtoRepository.existsByCodigo("AUTO001")).thenReturn(false);
        when(securityUtil.getUsuarioLogado()).thenReturn(new com.tcc.estoque.model.Usuario());
        when(codigoBarrasService.gerarProximoCodigoResumido()).thenReturn("R005");
        when(produtoRepository.findMaxCodigoInternoSequencial()).thenReturn(null);
        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(1003L);
            return p;
        });

        // Quando criar
        ProdutoDTO.ProdutoResponse response = produtoService.criarProduto(request);

        // Então deve gerar código automaticamente
        verify(codigoBarrasService).gerarProximoCodigo(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO, "AUTO");
    }

    @Test
    void processarCodigoBarrasComDeteccaoAutomaticaTipo() {
        // Dado um request com código mas sem tipo especificado
        ProdutoDTO.ProdutoRequest request = ProdutoDTO.ProdutoRequest.builder()
                .nome("Produto Detectado")
                .codigoBarras("DET123")
                .build();

        when(codigoBarrasService.detectarTipoCodigo("DET123")).thenReturn(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO);
        when(codigoBarrasService.validarCodigoBarras("DET123", com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO)).thenReturn(true);
        when(produtoRepository.existsByCodigo("DET123")).thenReturn(false);
        when(securityUtil.getUsuarioLogado()).thenReturn(new com.tcc.estoque.model.Usuario());
        when(codigoBarrasService.gerarProximoCodigoResumido()).thenReturn("R006");
        when(produtoRepository.findMaxCodigoInternoSequencial()).thenReturn(null);
        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(1004L);
            return p;
        });

        // Quando criar
        ProdutoDTO.ProdutoResponse response = produtoService.criarProduto(request);

        // Então deve detectar o tipo automaticamente
        verify(codigoBarrasService).detectarTipoCodigo("DET123");
        verify(codigoBarrasService).validarCodigoBarras("DET123", com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO);
    }

    @Test
    void processarCodigoBarrasSemCodigoEspecificado() {
        // Dado um request sem código de barras
        ProdutoDTO.ProdutoRequest request = ProdutoDTO.ProdutoRequest.builder()
                .nome("Produto Sem Código")
                .prefixoCodigo("CUSTOM")
                .build();

        when(codigoBarrasService.gerarProximoCodigo(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO, "CUSTOM"))
                .thenReturn("CUSTOM001");
        when(produtoRepository.existsByCodigo("CUSTOM001")).thenReturn(false);
        when(securityUtil.getUsuarioLogado()).thenReturn(new com.tcc.estoque.model.Usuario());
        when(codigoBarrasService.gerarProximoCodigoResumido()).thenReturn("R007");
        when(produtoRepository.findMaxCodigoInternoSequencial()).thenReturn(null);
        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(1005L);
            return p;
        });

        // Quando criar
        ProdutoDTO.ProdutoResponse response = produtoService.criarProduto(request);

        // Então deve gerar código personalizado com prefixo
        verify(codigoBarrasService).gerarProximoCodigo(com.tcc.estoque.enums.TipoCodigoBarras.PERSONALIZADO, "CUSTOM");
    }

    @Test
    void adquirirLockProdutoComLockExistente() {
        // Dado que o produto já está bloqueado por outro usuário
        when(produtoRepository.existsById(1L)).thenReturn(true);
        when(recordLockService.isProdutoBloqueado(1L)).thenReturn(true);
        when(recordLockService.usuarioTemLockProduto(1L, 100L)).thenReturn(false);
        
        Map<String, Object> lockInfo = new HashMap<>();
        lockInfo.put("usuario_nome", "Outro Usuário");
        lockInfo.put("data_expiracao", java.sql.Timestamp.valueOf("2025-12-01 23:45:00"));
        when(recordLockService.obterInfoLockProduto(1L)).thenReturn(lockInfo);

        // Quando tentar adquirir lock
        ProdutoDTO.LockResponse response = produtoService.adquirirLockProduto(1L, 100L, "127.0.0.1", "TestAgent");

        // Então deve retornar insucesso com informações do bloqueio
        assertThat(response.getSucesso()).isFalse();
        assertThat(response.getMensagem()).isEqualTo("Produto está sendo editado por outro usuário");
        assertThat(response.getBloqueadoPorUsuario()).isEqualTo("Outro Usuário");
        assertThat(response.getPodeEditar()).isFalse();
    }
}


