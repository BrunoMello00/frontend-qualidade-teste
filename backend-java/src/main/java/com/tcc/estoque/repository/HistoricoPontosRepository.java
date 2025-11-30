package com.tcc.estoque.repository;

import com.tcc.estoque.model.HistoricoPontos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoricoPontosRepository extends JpaRepository<HistoricoPontos, Long> {

    List<HistoricoPontos> findByClienteIdOrderByDataOperacaoDesc(Long clienteId);

    Page<HistoricoPontos> findByClienteIdOrderByDataOperacaoDesc(Long clienteId, Pageable pageable);

    List<HistoricoPontos> findByVendaId(Long vendaId);

    List<HistoricoPontos> findByProdutoId(Long produtoId);

    List<HistoricoPontos> findByDataOperacaoBetweenOrderByDataOperacaoDesc(
            LocalDateTime inicio, LocalDateTime fim);

    long countByPontosAdicionadosLessThan(int pontos);

    List<HistoricoPontos> findByClienteIdAndDataOperacaoBetweenOrderByDataOperacaoDesc(
            Long clienteId, LocalDateTime inicio, LocalDateTime fim);

    List<HistoricoPontos> findByMotivoOrderByDataOperacaoDesc(String motivo);

    @Query("SELECT h FROM HistoricoPontos h WHERE h.pontosAdicionados > 0 ORDER BY h.dataOperacao DESC")
    List<HistoricoPontos> findPontosAdicionados();

    @Query("SELECT h FROM HistoricoPontos h WHERE h.pontosAdicionados < 0 ORDER BY h.dataOperacao DESC")
    List<HistoricoPontos> findPontosRemovidos();

    @Query("SELECT COALESCE(SUM(h.pontosAdicionados), 0) FROM HistoricoPontos h WHERE h.clienteId = :clienteId")
    Integer somarPontosCliente(@Param("clienteId") Long clienteId);

    @Query("SELECT COALESCE(SUM(h.pontosAdicionados), 0) FROM HistoricoPontos h " +
           "WHERE h.dataOperacao BETWEEN :inicio AND :fim")
    Integer somarPontosPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(h.pontosAdicionados), 0) FROM HistoricoPontos h " +
           "WHERE h.clienteId = :clienteId AND h.dataOperacao BETWEEN :inicio AND :fim")
    Integer somarPontosClientePeriodo(@Param("clienteId") Long clienteId, 
                                     @Param("inicio") LocalDateTime inicio, 
                                     @Param("fim") LocalDateTime fim);

    long countByClienteId(Long clienteId);

    long countByMotivo(String motivo);

    @Query("SELECT h FROM HistoricoPontos h WHERE h.clienteId = :clienteId " +
           "ORDER BY h.dataOperacao DESC")
    List<HistoricoPontos> findUltimoHistoricoCliente(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query("SELECT h FROM HistoricoPontos h WHERE " +
           "(:clienteId IS NULL OR h.clienteId = :clienteId) AND " +
           "(:vendaId IS NULL OR h.vendaId = :vendaId) AND " +
           "(:produtoId IS NULL OR h.produtoId = :produtoId) AND " +
           "(:motivo IS NULL OR h.motivo = :motivo) AND " +
           "(:dataInicio IS NULL OR h.dataOperacao >= :dataInicio) AND " +
           "(:dataFim IS NULL OR h.dataOperacao <= :dataFim) AND " +
           "(:pontosMinimos IS NULL OR ABS(h.pontosAdicionados) >= :pontosMinimos) " +
           "ORDER BY h.dataOperacao DESC")
    Page<HistoricoPontos> findComFiltros(
            @Param("clienteId") Long clienteId,
            @Param("vendaId") Long vendaId,
            @Param("produtoId") Long produtoId,
            @Param("motivo") String motivo,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("pontosMinimos") Integer pontosMinimos,
            Pageable pageable);

    @Query("SELECT h.motivo, COUNT(h), SUM(h.pontosAdicionados), AVG(h.pontosAdicionados) " +
           "FROM HistoricoPontos h GROUP BY h.motivo")
    List<Object[]> estatisticasPorMotivo();

    @Query("SELECT h.clienteId, SUM(h.pontosAdicionados) as totalPontos FROM HistoricoPontos h " +
           "WHERE h.dataOperacao BETWEEN :inicio AND :fim AND h.pontosAdicionados > 0 " +
           "GROUP BY h.clienteId ORDER BY totalPontos DESC")
    List<Object[]> topClientesPorPontosPeriodo(@Param("inicio") LocalDateTime inicio, 
                                              @Param("fim") LocalDateTime fim, 
                                              Pageable pageable);

    @Query("SELECT h FROM HistoricoPontos h WHERE " +
           "DATE(h.dataOperacao) = DATE(:hoje) ORDER BY h.dataOperacao DESC")
    List<HistoricoPontos> findHistoricoHoje(@Param("hoje") LocalDateTime hoje);

    @Query("SELECT h FROM HistoricoPontos h WHERE " +
           "h.dataOperacao >= :dataLimite ORDER BY h.dataOperacao DESC")
    List<HistoricoPontos> findHistoricoUltimosDias(@Param("dataLimite") LocalDateTime dataLimite);

    boolean existsByClienteIdAndVendaId(Long clienteId, Long vendaId);

    @Query("SELECT h FROM HistoricoPontos h WHERE h.motivo = 'ajuste_manual' ORDER BY h.dataOperacao DESC")
    List<HistoricoPontos> findAjustesManuais();

    @Query("SELECT h FROM HistoricoPontos h WHERE h.motivo = 'bonus' ORDER BY h.dataOperacao DESC")
    List<HistoricoPontos> findBonus();

    @Query("SELECT h FROM HistoricoPontos h WHERE h.motivo = 'compra' ORDER BY h.dataOperacao DESC")
    List<HistoricoPontos> findPontosCompras();

    void deleteByDataOperacaoBefore(LocalDateTime dataLimite);

    @Query("SELECT AVG(h.pontosAdicionados) FROM HistoricoPontos h WHERE h.pontosAdicionados > 0")
    Double mediaPontosOperacao();

    @Query("SELECT COUNT(h) FROM HistoricoPontos h WHERE " +
           "YEAR(h.dataOperacao) = :ano AND MONTH(h.dataOperacao) = :mes")
    long countOperacoesMes(@Param("ano") int ano, @Param("mes") int mes);

    @Query("SELECT COALESCE(SUM(h.pontosAdicionados), 0) FROM HistoricoPontos h WHERE " +
           "YEAR(h.dataOperacao) = :ano AND MONTH(h.dataOperacao) = :mes AND h.pontosAdicionados > 0")
    Integer pontosDistribuidosMes(@Param("ano") int ano, @Param("mes") int mes);
}
