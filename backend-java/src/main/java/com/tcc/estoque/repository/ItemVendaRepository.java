package com.tcc.estoque.repository;

import com.tcc.estoque.model.ItemVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

    List<ItemVenda> findByVendaId(Long vendaId);

    List<ItemVenda> findByProdutoId(Long produtoId);

    @Query("SELECT iv.produto.nome, SUM(iv.quantidade) as totalVendido " +
           "FROM ItemVenda iv " +
           "JOIN iv.venda v " +
           "WHERE v.dataVenda BETWEEN :dataInicio AND :dataFim " +
           "AND v.status = 'CONFIRMADA' " +
           "GROUP BY iv.produto.id, iv.produto.nome " +
           "ORDER BY totalVendido DESC")
    List<Object[]> findProdutosMaisVendidos(@Param("dataInicio") LocalDateTime dataInicio,
                                            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT COALESCE(SUM(iv.quantidade), 0) FROM ItemVenda iv " +
           "JOIN iv.venda v " +
           "WHERE iv.produto.id = :produtoId " +
           "AND v.status = 'CONFIRMADA' " +
           "AND v.dataVenda BETWEEN :dataInicio AND :dataFim")
    Integer countTotalVendidoPorProduto(@Param("produtoId") Long produtoId,
                                        @Param("dataInicio") LocalDateTime dataInicio,
                                        @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT COALESCE(SUM(iv.subtotal), 0) FROM ItemVenda iv " +
           "JOIN iv.venda v " +
           "WHERE iv.produto.id = :produtoId " +
           "AND v.status = 'CONFIRMADA' " +
           "AND v.dataVenda BETWEEN :dataInicio AND :dataFim")
    java.math.BigDecimal sumValorVendidoPorProduto(@Param("produtoId") Long produtoId,
                                                   @Param("dataInicio") LocalDateTime dataInicio,
                                                   @Param("dataFim") LocalDateTime dataFim);

    
    @Query("SELECT iv.produto, SUM(iv.quantidade), SUM(iv.subtotal) " +
           "FROM ItemVenda iv " +
           "JOIN iv.venda v " +
           "WHERE v.dataVenda BETWEEN :dataInicio AND :dataFim " +
           "AND v.status = 'CONFIRMADA' " +
           "GROUP BY iv.produto " +
           "ORDER BY SUM(iv.subtotal) DESC")
    List<Object[]> findTopProdutosPorPeriodo(@Param("dataInicio") LocalDateTime dataInicio,
                                             @Param("dataFim") LocalDateTime dataFim,
                                             org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT iv.produto, SUM(iv.quantidade), SUM(iv.subtotal) " +
           "FROM ItemVenda iv " +
           "JOIN iv.venda v " +
           "WHERE v.usuario.id = :vendedorId " +
           "AND v.dataVenda BETWEEN :dataInicio AND :dataFim " +
           "AND v.status = 'CONFIRMADA' " +
           "GROUP BY iv.produto " +
           "ORDER BY SUM(iv.subtotal) DESC")
    List<Object[]> findTopProdutosPorVendedor(@Param("vendedorId") Long vendedorId,
                                              @Param("dataInicio") LocalDateTime dataInicio,
                                              @Param("dataFim") LocalDateTime dataFim,
                                              org.springframework.data.domain.Pageable pageable);
}
