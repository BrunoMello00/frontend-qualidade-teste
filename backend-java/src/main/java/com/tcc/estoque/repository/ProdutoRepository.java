package com.tcc.estoque.repository;

import com.tcc.estoque.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Page<Produto> findByAtivoTrue(Pageable pageable);
    
    // 🆕 Busca produtos desabilitados
    Page<Produto> findByAtivoFalse(Pageable pageable);
    
    Page<Produto> findByDepartamentoContainingIgnoreCase(String departamento, Pageable pageable);
    
    Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    
    Page<Produto> findByDepartamentoContainingIgnoreCaseAndNomeContainingIgnoreCase(
            String departamento, String nome, Pageable pageable);

    Page<Produto> findByAtivoTrueAndDepartamentoContainingIgnoreCase(String departamento, Pageable pageable);

    Page<Produto> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Pageable pageable);
    
    // 🆕 Método para busca combinada (departamento + nome) em produtos ativos
    Page<Produto> findByAtivoTrueAndDepartamentoContainingIgnoreCaseAndNomeContainingIgnoreCase(
            String departamento, String nome, Pageable pageable);

    Optional<Produto> findByCodigo(String codigo);
    
    Optional<Produto> findByCodigoAndAtivoTrue(String codigo);
    
    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndAtivoTrue(String codigo);

    @Query("SELECT p FROM Produto p LEFT JOIN FETCH p.tamanhos WHERE p.id = :id")
    Optional<Produto> findByIdWithTamanhos(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Produto p LEFT JOIN FETCH p.tamanhos t WHERE p.ativo = true AND " +
            "COALESCE((SELECT SUM(t2.estoque) FROM TamanhoProduto t2 WHERE t2.produto.id = p.id), p.estoque) <= p.estoqueMinimo")
    List<Produto> findProdutosComEstoqueBaixo();

    @Query("SELECT DISTINCT p FROM Produto p LEFT JOIN FETCH p.tamanhos t WHERE p.ativo = true AND " +
            "COALESCE((SELECT SUM(t2.estoque) FROM TamanhoProduto t2 WHERE t2.produto.id = p.id), p.estoque) <= :limite")
    List<Produto> findProdutosComEstoqueBaixo(@Param("limite") Integer limite);

    @Query("SELECT DISTINCT p.departamento FROM Produto p WHERE p.ativo = true AND p.departamento IS NOT NULL ORDER BY p.departamento")
    List<String> findDistinctDepartamentos();

    @Query("SELECT DISTINCT p.departamento FROM Produto p WHERE p.ativo = true AND p.departamento IS NOT NULL ORDER BY p.departamento")
    List<String> findDepartamentos();

    // =====================
    // =====================
    Page<Produto> findDistinctByTamanhos_TamanhoIgnoreCaseAndAtivoTrue(String tamanho, Pageable pageable);

    Page<Produto> findDistinctByTamanhos_TamanhoIgnoreCaseAndDepartamentoContainingIgnoreCaseAndAtivoTrue(
            String tamanho, String departamento, Pageable pageable);

    Page<Produto> findDistinctByTamanhos_TamanhoIgnoreCaseAndNomeContainingIgnoreCaseAndAtivoTrue(
            String tamanho, String nome, Pageable pageable);

    Page<Produto> findDistinctByTamanhos_TamanhoIgnoreCaseAndDepartamentoContainingIgnoreCaseAndNomeContainingIgnoreCase(
            String tamanho, String departamento, String nome, Pageable pageable);

    long countByAtivoTrue();
    
    @Query("SELECT COUNT(DISTINCT p) FROM Produto p WHERE p.ativo = true AND " +
            "COALESCE((SELECT SUM(t.estoque) FROM TamanhoProduto t WHERE t.produto.id = p.id), p.estoque) <= p.estoqueMinimo")
    long countProdutosComEstoqueBaixo();

    @Query("SELECT COUNT(p) FROM Produto p WHERE p.ativo = true")
    Long countProdutosAtivos();

        @Query("SELECT COALESCE(SUM(COALESCE((SELECT SUM(t.estoque) FROM TamanhoProduto t WHERE t.produto.id = p.id), p.estoque)), 0) FROM Produto p WHERE p.ativo = true")
        Long getTotalQuantidadeEstoque();

        @Query("SELECT COALESCE(SUM(p.preco * COALESCE((SELECT SUM(t.estoque) FROM TamanhoProduto t WHERE t.produto.id = p.id), p.estoque)), 0) FROM Produto p WHERE p.ativo = true")
        BigDecimal calcularValorTotalEstoque();

    @Query("SELECT p.departamento, COUNT(p), COALESCE(SUM(p.preco * COALESCE((SELECT SUM(t.estoque) FROM TamanhoProduto t WHERE t.produto.id = p.id), p.estoque)), 0) " +
            "FROM Produto p WHERE p.ativo = true AND p.departamento IS NOT NULL " +
            "GROUP BY p.departamento ORDER BY SUM(p.preco * COALESCE((SELECT SUM(t2.estoque) FROM TamanhoProduto t2 WHERE t2.produto.id = p.id), p.estoque)) DESC")
    List<Object[]> findDepartamentosPorValorEstoque();
    
    @Query("SELECT COALESCE(MAX(p.codigoInternoSequencial), 0) FROM Produto p")
    Long findMaxCodigoInternoSequencial();
}
