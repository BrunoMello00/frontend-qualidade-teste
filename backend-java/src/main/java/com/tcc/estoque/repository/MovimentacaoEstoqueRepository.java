package com.tcc.estoque.repository;

import com.tcc.estoque.model.MovimentacaoEstoque;
import com.tcc.estoque.model.enums.TipoMovimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    Page<MovimentacaoEstoque> findByProdutoIdOrderByDataMovimentacaoDesc(Long produtoId, Pageable pageable);

    Page<MovimentacaoEstoque> findByTipoOrderByDataMovimentacaoDesc(TipoMovimentacao tipo, Pageable pageable);

    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.dataMovimentacao BETWEEN :dataInicio AND :dataFim ORDER BY m.dataMovimentacao DESC")
    List<MovimentacaoEstoque> findMovimentacoesPorPeriodo(@Param("dataInicio") LocalDateTime dataInicio,
                                                          @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT m FROM MovimentacaoEstoque m WHERE " +
           "(:produtoId IS NULL OR m.produto.id = :produtoId) AND " +
           "(:tipo IS NULL OR m.tipo = :tipo) AND " +
           "(:dataInicio IS NULL OR m.dataMovimentacao >= :dataInicio) AND " +
           "(:dataFim IS NULL OR m.dataMovimentacao <= :dataFim) " +
           "ORDER BY m.dataMovimentacao DESC")
    Page<MovimentacaoEstoque> findMovimentacoesComFiltros(@Param("produtoId") Long produtoId,
                                                          @Param("tipo") TipoMovimentacao tipo,
                                                          @Param("dataInicio") LocalDateTime dataInicio,
                                                          @Param("dataFim") LocalDateTime dataFim,
                                                          Pageable pageable);

    // Últimas movimentações
    @Query("SELECT m FROM MovimentacaoEstoque m ORDER BY m.dataMovimentacao DESC")
    Page<MovimentacaoEstoque> findUltimasMovimentacoes(Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.quantidade), 0) FROM MovimentacaoEstoque m WHERE m.produto.id = :produtoId AND m.tipo = 'ENTRADA'")
    Integer sumEntradasPorProduto(@Param("produtoId") Long produtoId);

    @Query("SELECT COALESCE(SUM(m.quantidade), 0) FROM MovimentacaoEstoque m WHERE m.produto.id = :produtoId AND m.tipo = 'SAIDA'")
    Integer sumSaidasPorProduto(@Param("produtoId") Long produtoId);

    @Query("SELECT COUNT(m) FROM MovimentacaoEstoque m WHERE " +
           "m.dataMovimentacao >= :inicioDia AND m.dataMovimentacao < :fimDia")
    Long countMovimentacoesHoje(@Param("inicioDia") LocalDateTime inicioDia, 
                               @Param("fimDia") LocalDateTime fimDia);

    Page<MovimentacaoEstoque> findByUsuarioIdOrderByDataMovimentacaoDesc(Long usuarioId, Pageable pageable);
}
