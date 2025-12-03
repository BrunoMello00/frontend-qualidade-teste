package com.tcc.estoque.repository;

import com.tcc.estoque.model.TamanhoProduto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TamanhoProdutoRepository extends JpaRepository<TamanhoProduto, Long> {

    List<TamanhoProduto> findByProdutoIdAndAtivoTrue(Long produtoId);
    
    List<TamanhoProduto> findByProdutoId(Long produtoId);

    TamanhoProduto findByProdutoIdAndTamanhoAndAtivoTrue(Long produtoId, String tamanho);

    boolean existsByProdutoIdAndTamanhoAndAtivoTrue(Long produtoId, String tamanho);
    
    boolean existsByProdutoIdAndTamanho(Long produtoId, String tamanho);

    @Query("SELECT tp FROM TamanhoProduto tp WHERE tp.produto.id = :produtoId AND tp.estoque <= tp.produto.estoqueMinimo AND tp.ativo = true")
    List<TamanhoProduto> findTamanhosEstoqueBaixo(@Param("produtoId") Long produtoId);
    
    List<TamanhoProduto> findByProdutoIdAndEstoqueLessThanEqualAndAtivoTrue(Long produtoId, Integer estoque);

    TamanhoProduto findByCodigoBarrasAndAtivoTrue(String codigoBarras);

    @Query("SELECT tp FROM TamanhoProduto tp WHERE tp.produto.id = :produtoId AND tp.ativo = true ORDER BY tp.vendidas DESC")
    List<TamanhoProduto> findTamanhosMaisVendidos(@Param("produtoId") Long produtoId);

    @Query("SELECT tp FROM TamanhoProduto tp WHERE tp.produto.id = :produtoId AND tp.estoque > 0 AND tp.ativo = true")
    List<TamanhoProduto> findTamanhosDisponiveis(@Param("produtoId") Long produtoId);

    @Query("SELECT COALESCE(SUM(tp.estoque), 0) FROM TamanhoProduto tp WHERE tp.produto.id = :produtoId AND tp.ativo = true")
    Integer calcularEstoqueTotalPorProduto(@Param("produtoId") Long produtoId);

    @Query("SELECT COALESCE(SUM(tp.vendidas), 0) FROM TamanhoProduto tp WHERE tp.produto.id = :produtoId AND tp.ativo = true")
    Integer calcularVendasTotalPorProduto(@Param("produtoId") Long produtoId);

    boolean existsByProdutoIdAndTamanhoIgnoreCase(Long produtoId, String tamanho);

    List<TamanhoProduto> findByEstoqueLessThanAndAtivoTrue(Integer estoque);
}
