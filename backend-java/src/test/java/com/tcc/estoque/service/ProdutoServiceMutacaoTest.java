package com.tcc.estoque.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.TamanhoProdutoRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class ProdutoServiceMutacaoTest {

    @Test
    public void listarDepartamentos() {
        ProdutoRepository produtoRepo = Mockito.mock(ProdutoRepository.class);
        TamanhoProdutoRepository tamanhoRepo = Mockito.mock(TamanhoProdutoRepository.class);
        var recordLock = Mockito.mock(RecordLockService.class);
        var codigoBarras = Mockito.mock(CodigoBarrasService.class);
        var securityUtil = Mockito.mock(com.tcc.estoque.security.SecurityUtil.class);

        ProdutoService service = new ProdutoService(produtoRepo, tamanhoRepo, recordLock, codigoBarras, securityUtil);

        Mockito.when(produtoRepo.findDistinctDepartamentos()).thenReturn(List.of("01", "02"));

        var result = service.listarDepartamentos();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo("01");
    }
}


