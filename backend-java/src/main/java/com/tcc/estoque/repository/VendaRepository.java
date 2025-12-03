package com.tcc.estoque.repository;

import com.tcc.estoque.model.Venda;
import com.tcc.estoque.model.enums.StatusVenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    Page<Venda> findAllByOrderByDataVendaDesc(Pageable pageable);

    Page<Venda> findByStatusOrderByDataVendaDesc(StatusVenda status, Pageable pageable);

    @Query("SELECT v FROM Venda v WHERE v.dataVenda BETWEEN :dataInicio AND :dataFim ORDER BY v.dataVenda DESC")
    List<Venda> findVendasPorPeriodo(@Param("dataInicio") LocalDateTime dataInicio, 
                                     @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT v FROM Venda v WHERE " +
           "(:status IS NULL OR v.status = :status) AND " +
           "(:dataInicio IS NULL OR v.dataVenda >= :dataInicio) AND " +
           "(:dataFim IS NULL OR v.dataVenda <= :dataFim) AND " +
           "(:nomeCliente IS NULL OR LOWER(v.clienteNome) LIKE LOWER(CONCAT('%', :nomeCliente, '%'))) " +
           "ORDER BY v.dataVenda DESC")
    Page<Venda> findVendasComFiltros(@Param("status") StatusVenda status,
                                     @Param("dataInicio") LocalDateTime dataInicio,
                                     @Param("dataFim") LocalDateTime dataFim,
                                     @Param("nomeCliente") String nomeCliente,
                                     Pageable pageable);

    @Query("SELECT COUNT(v) FROM Venda v WHERE v.dataVenda BETWEEN :dataInicio AND :dataFim AND v.status = 'CONFIRMADA'")
    Long countVendasPorPeriodo(@Param("dataInicio") LocalDateTime dataInicio, 
                               @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT COALESCE(SUM(v.valorTotal), 0) FROM Venda v WHERE v.dataVenda BETWEEN :dataInicio AND :dataFim AND v.status = 'CONFIRMADA'")
    BigDecimal sumFaturamentoPorPeriodo(@Param("dataInicio") LocalDateTime dataInicio, 
                                        @Param("dataFim") LocalDateTime dataFim);

    Long countByStatusAndDataVendaBetween(StatusVenda status, LocalDateTime dataInicio, LocalDateTime dataFim);

    Page<Venda> findByUsuarioIdOrderByDataVendaDesc(Long usuarioId, Pageable pageable);

    @Query("SELECT v FROM Venda v WHERE LOWER(v.clienteNome) LIKE LOWER(CONCAT('%', :nomeCliente, '%')) ORDER BY v.dataVenda DESC")
    List<Venda> findByNomeClienteContainingIgnoreCase(@Param("nomeCliente") String nomeCliente);

    @Query("SELECT v FROM Venda v LEFT JOIN FETCH v.itens i LEFT JOIN FETCH i.produto WHERE v.id = :id")
    Optional<Venda> findByIdWithItens(@Param("id") Long id);

    @Query("SELECT COUNT(v) FROM Venda v WHERE CAST(v.dataVenda AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE) AND v.status = 'CONFIRMADA'")
    Long countVendasHoje();

    @Query("SELECT COALESCE(SUM(v.valorTotal), 0) FROM Venda v WHERE CAST(v.dataVenda AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE) AND v.status = 'CONFIRMADA'")
    BigDecimal sumFaturamentoHoje();

    @Query("SELECT v.usuario.nome, COUNT(v), SUM(v.valorTotal) FROM Venda v " +
           "WHERE v.dataVenda BETWEEN :dataInicio AND :dataFim AND v.status = 'CONFIRMADA' " +
           "GROUP BY v.usuario.id, v.usuario.nome " +
           "ORDER BY SUM(v.valorTotal) DESC")
    List<Object[]> findTopVendedores(@Param("dataInicio") LocalDateTime dataInicio,
                                     @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT COUNT(v) FROM Venda v WHERE v.status = 'PENDENTE'")
    Long countVendasPendentes();

    
    Long countByStatus(StatusVenda status);
    
    @Query("SELECT v.status, COUNT(v) FROM Venda v GROUP BY v.status")
    List<Object[]> contarVendasPorStatus();
    
    @Query("SELECT v.usuario, COUNT(v), SUM(v.valorTotal) FROM Venda v " +
           "WHERE v.dataVenda BETWEEN :dataInicio AND :dataFim AND v.status = 'CONFIRMADA' " +
           "GROUP BY v.usuario.id " +
           "ORDER BY SUM(v.valorTotal) DESC")
    List<Object[]> findTopVendedoresPorPeriodo(@Param("dataInicio") LocalDateTime dataInicio,
                                               @Param("dataFim") LocalDateTime dataFim,
                                               Pageable pageable);
    
    Long countByDataVendaBetween(LocalDateTime dataInicio, LocalDateTime dataFim);
    
    @Query("SELECT COALESCE(SUM(v.valorTotal), 0) FROM Venda v " +
           "WHERE v.usuario.id = :vendedorId AND CAST(v.dataVenda AS DATE) = :data AND v.status = 'CONFIRMADA'")
    BigDecimal sumFaturamentoPorVendedorEDia(@Param("vendedorId") Long vendedorId, 
                                             @Param("data") LocalDate data);
    
    @Query("SELECT COUNT(v) FROM Venda v " +
           "WHERE v.usuario.id = :vendedorId AND CAST(v.dataVenda AS DATE) = :data AND v.status = 'CONFIRMADA'")
    Long countByUsuarioIdAndDataVenda(@Param("vendedorId") Long vendedorId, 
                                      @Param("data") LocalDate data);
    
    
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.usuario.id = :vendedorId AND v.status = :status")
    Long countByUsuarioIdAndStatus(@Param("vendedorId") Long vendedorId, @Param("status") StatusVenda status);
    
    @Query("SELECT COALESCE(SUM(v.valorTotal), 0) FROM Venda v " +
           "WHERE v.usuario.id = :vendedorId AND v.dataVenda BETWEEN :inicio AND :fim AND v.status = 'CONFIRMADA'")
    BigDecimal sumFaturamentoPorVendedor(@Param("vendedorId") Long vendedorId,
                                         @Param("inicio") LocalDateTime inicio,
                                         @Param("fim") LocalDateTime fim);
    
    @Query("SELECT COUNT(DISTINCT v.clienteNome) FROM Venda v " +
           "WHERE v.usuario.id = :vendedorId AND v.dataVenda BETWEEN :inicio AND :fim AND v.status = 'CONFIRMADA'")
    Long countDistinctClientesByVendedor(@Param("vendedorId") Long vendedorId,
                                         @Param("inicio") LocalDateTime inicio,
                                         @Param("fim") LocalDateTime fim);
    
    @Query("SELECT COALESCE(SUM(v.valorTotal), 0) FROM Venda v " +
           "WHERE CAST(v.dataVenda AS DATE) = :data AND v.status = 'CONFIRMADA'")
    BigDecimal sumFaturamentoPorDia(@Param("data") LocalDate data);

    @Query("SELECT " +
           "p.id, " +
           "p.nome, " +
           "p.departamento, " +
           "COALESCE(SUM(iv.quantidade), 0L), " +
           "COALESCE(SUM(iv.subtotal), 0.0), " +
           "p.preco " +
           "FROM Produto p " +
           "LEFT JOIN ItemVenda iv ON p.id = iv.produto.id " +
           "LEFT JOIN Venda v ON iv.venda.id = v.id " +
           "WHERE (:dataInicio IS NULL OR v.dataVenda >= :dataInicio) " +
           "AND (:dataFim IS NULL OR v.dataVenda <= :dataFim) " +
           "AND (:status IS NULL OR v.status = :status) " +
           "GROUP BY p.id, p.nome, p.departamento, p.preco " +
           "ORDER BY COALESCE(SUM(iv.quantidade), 0L) DESC")
    List<Object[]> findTopProdutosMaisVendidos(@Param("dataInicio") LocalDateTime dataInicio,
                                               @Param("dataFim") LocalDateTime dataFim,
                                               @Param("status") StatusVenda status,
                                               Pageable pageable);

    @Query("SELECT " +
           "p.id, " +
           "p.nome, " +
           "p.departamento, " +
           "COALESCE(SUM(iv.quantidade), 0L), " +
           "COALESCE(SUM(iv.subtotal), 0.0), " +
           "p.preco " +
           "FROM Produto p " +
           "LEFT JOIN ItemVenda iv ON p.id = iv.produto.id " +
           "LEFT JOIN Venda v ON iv.venda.id = v.id " +
           "WHERE (:dataInicio IS NULL OR v.dataVenda >= :dataInicio) " +
           "AND (:dataFim IS NULL OR v.dataVenda <= :dataFim) " +
           "AND (:status IS NULL OR v.status = :status) " +
           "GROUP BY p.id, p.nome, p.departamento, p.preco " +
           "ORDER BY COALESCE(SUM(iv.subtotal), 0.0) DESC")
    List<Object[]> findTopProdutosPorReceita(@Param("dataInicio") LocalDateTime dataInicio,
                                             @Param("dataFim") LocalDateTime dataFim,
                                             @Param("status") StatusVenda status,
                                             Pageable pageable);

    @Query("SELECT COALESCE(SUM(iv.quantidade), 0L) " +
           "FROM ItemVenda iv " +
           "JOIN Venda v ON iv.venda.id = v.id " +
           "WHERE (:dataInicio IS NULL OR v.dataVenda >= :dataInicio) " +
           "AND (:dataFim IS NULL OR v.dataVenda <= :dataFim) " +
           "AND (:status IS NULL OR v.status = :status)")
    Long sumTotalProdutosVendidos(@Param("dataInicio") LocalDateTime dataInicio,
                                  @Param("dataFim") LocalDateTime dataFim,
                                  @Param("status") StatusVenda status);

    
    Long countByDataVendaBetweenAndStatus(LocalDate dataInicio, LocalDate dataFim, StatusVenda status);
    
    @Query("SELECT COALESCE(SUM(v.valorTotal), 0) FROM Venda v WHERE v.dataVenda >= :dataInicio AND v.dataVenda < :dataFim AND v.status = :status")
    BigDecimal sumValorTotalByDataVendaBetweenAndStatus(@Param("dataInicio") LocalDate dataInicio, 
                                                        @Param("dataFim") LocalDate dataFim, 
                                                        @Param("status") StatusVenda status);
    
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.dataVenda >= :data AND v.dataVenda < :dataProxima AND v.status = :status")
    Long countByDataVendaAndStatus(@Param("data") LocalDate data, @Param("dataProxima") LocalDate dataProxima, @Param("status") StatusVenda status);
    
    @Query("SELECT COALESCE(SUM(v.valorTotal), 0) FROM Venda v WHERE v.dataVenda >= :data AND v.dataVenda < :dataProxima AND v.status = :status")
    BigDecimal sumValorTotalByDataVendaAndStatus(@Param("data") LocalDate data, @Param("dataProxima") LocalDate dataProxima, @Param("status") StatusVenda status);
}
