package com.tcc.estoque.repository;

import com.tcc.estoque.model.Evento;
import com.tcc.estoque.model.enums.StatusEvento;
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
public interface EventoRepository extends JpaRepository<Evento, Long> {

    Optional<Evento> findByIdAndAtivoTrue(Long id);
    
    List<Evento> findByAtivoTrueOrderByDataInicioDesc();
    
    List<Evento> findByPublicoTrueAndAtivoTrueOrderByDataInicioDesc();

    List<Evento> findByStatusAndAtivoTrue(StatusEvento status);
    
    List<Evento> findByStatusInAndAtivoTrue(List<StatusEvento> status);

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "e.dataInicio <= :data AND e.dataFim >= :data " +
           "ORDER BY e.dataInicio")
    List<Evento> findEventosVigentesNaData(@Param("data") LocalDate data);

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "e.dataInicio <= CURRENT_DATE AND e.dataFim >= CURRENT_DATE " +
           "ORDER BY e.dataInicio")
    List<Evento> findEventosVigentesHoje();

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "e.dataInicio > CURRENT_DATE " +
           "ORDER BY e.dataInicio")
    List<Evento> findEventosFuturos();

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "e.dataFim < CURRENT_DATE " +
           "ORDER BY e.dataFim DESC")
    List<Evento> findEventosPassados();

    @Query("SELECT e FROM Evento e WHERE " +
           "(:termo IS NULL OR LOWER(e.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(e.descricao) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(e.local) LIKE LOWER(CONCAT('%', :termo, '%'))) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:ativo IS NULL OR e.ativo = :ativo) " +
           "AND (:dataInicioApos IS NULL OR e.dataInicio >= :dataInicioApos) " +
           "AND (:dataInicioAntes IS NULL OR e.dataInicio <= :dataInicioAntes)")
    Page<Evento> findComFiltros(@Param("termo") String termo,
                               @Param("status") StatusEvento status,
                               @Param("ativo") Boolean ativo,
                               @Param("dataInicioApos") LocalDate dataInicioApos,
                               @Param("dataInicioAntes") LocalDate dataInicioAntes,
                               Pageable pageable);

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "(LOWER(e.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(e.descricao) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(e.local) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<Evento> findByTermoGeral(@Param("termo") String termo, Pageable pageable);

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "(e.descontoPercentual > 0 OR e.descontoValor > 0) " +
           "ORDER BY e.dataInicio")
    List<Evento> findEventosComDesconto();

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "e.status IN ('ATIVO') " +
           "ORDER BY e.dataInicio")
    List<Evento> findEventosQuePermitemVendas();

    long countByAtivoTrue();
    
    long countByStatusAndAtivoTrue(StatusEvento status);

    @Query("SELECT COUNT(e) FROM Evento e WHERE e.ativo = true AND " +
           "e.dataInicio <= CURRENT_DATE AND e.dataFim >= CURRENT_DATE")
    long countEventosVigentes();

    @Query("SELECT e.status, COUNT(e) FROM Evento e WHERE e.ativo = true GROUP BY e.status")
    List<Object[]> contarEventosPorStatus();

    @Query("SELECT COUNT(e) FROM Evento e WHERE e.ativo = true AND " +
           "e.dataCadastro >= :dataInicio AND e.dataCadastro <= :dataFim")
    long countEventosPorPeriodo(@Param("dataInicio") LocalDate dataInicio, 
                               @Param("dataFim") LocalDate dataFim);

    @Query("SELECT e, COALESCE(SUM(v.valorTotal), 0) as totalVendas, COUNT(v) as quantidadeVendas " +
           "FROM Evento e LEFT JOIN e.vendas v " +
           "WHERE e.ativo = true " +
           "GROUP BY e " +
           "ORDER BY totalVendas DESC")
    List<Object[]> findTopEventosPorVendas(Pageable pageable);

    @Query("SELECT e FROM Evento e JOIN e.vendas v " +
           "WHERE e.ativo = true AND v.usuario.id = :vendedorId " +
           "GROUP BY e " +
           "ORDER BY e.dataInicio DESC")
    List<Evento> findEventosPorVendedor(@Param("vendedorId") Long vendedorId);

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "e.status = 'ATIVO' AND " +
           "e.dataFim BETWEEN CURRENT_DATE AND :dataLimite " +
           "ORDER BY e.dataFim")
    List<Evento> findEventosProximosDoFim(@Param("dataLimite") LocalDate dataLimite);

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "e.status = 'PLANEJADO' AND " +
           "e.dataInicio BETWEEN CURRENT_DATE AND :dataLimite " +
           "ORDER BY e.dataInicio")
    List<Evento> findEventosQueComecamEmBreve(@Param("dataLimite") LocalDate dataLimite);

    @Query("SELECT YEAR(e.dataInicio), MONTH(e.dataInicio), COUNT(e) " +
           "FROM Evento e WHERE e.ativo = true " +
           "GROUP BY YEAR(e.dataInicio), MONTH(e.dataInicio) " +
           "ORDER BY YEAR(e.dataInicio) DESC, MONTH(e.dataInicio) DESC")
    List<Object[]> contarEventosPorMes();

    boolean existsByNomeAndAtivoTrue(String nome);
    
    boolean existsByNomeAndIdNotAndAtivoTrue(String nome, Long id);

    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "(e.metaVendas IS NOT NULL OR e.metaQuantidadeVendas IS NOT NULL) " +
           "ORDER BY e.dataInicio DESC")
    List<Evento> findEventosComMeta();

    @Query("SELECT e.id, COALESCE(SUM(v.valorTotal), 0) " +
           "FROM Evento e LEFT JOIN e.vendas v " +
           "WHERE e.id = :eventoId " +
           "GROUP BY e.id")
    Optional<BigDecimal> getTotalVendasEvento(@Param("eventoId") Long eventoId);

    @Query("SELECT e.id, COUNT(v) " +
           "FROM Evento e LEFT JOIN e.vendas v " +
           "WHERE e.id = :eventoId " +
           "GROUP BY e.id")
    Optional<Integer> getQuantidadeVendasEvento(@Param("eventoId") Long eventoId);

    @Query("SELECT e, SUM(v.valorTotal) as total " +
           "FROM Evento e JOIN e.vendas v " +
           "WHERE e.ativo = true " +
           "GROUP BY e " +
           "ORDER BY total DESC")
    List<Object[]> findEventoComMaiorVenda(Pageable pageable);

    @Query(value = "SELECT AVG(vendas_por_evento.total) FROM " +
           "(SELECT COALESCE(SUM(v.valor_total), 0) as total " +
           "FROM evento e LEFT JOIN venda v ON e.id = v.evento_id " +
           "WHERE e.ativo = true " +
           "GROUP BY e.id) vendas_por_evento", 
           nativeQuery = true)
    Optional<BigDecimal> getMediaVendasPorEvento();

    @Query("SELECT e.local, COUNT(e) FROM Evento e " +
           "WHERE e.ativo = true AND e.local IS NOT NULL " +
           "GROUP BY e.local " +
           "ORDER BY COUNT(e) DESC")
    List<Object[]> contarEventosPorLocal();

    
    @Query("SELECT e FROM Evento e " +
           "LEFT JOIN Venda v ON e.id = v.evento.id AND v.status = 'CONFIRMADA' " +
           "WHERE e.ativo = true " +
           "GROUP BY e " +
           "ORDER BY COALESCE(SUM(v.valorTotal), 0) DESC")
    List<Evento> findTopEventosPorVendasDashboard(Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(v.valorTotal), 0) FROM Venda v " +
           "WHERE v.evento.id = :eventoId AND v.status = 'CONFIRMADA'")
    BigDecimal calcularTotalVendasEvento(@Param("eventoId") Long eventoId);
    
    @Query("SELECT COUNT(v) FROM Venda v " +
           "WHERE v.evento.id = :eventoId AND v.status = 'CONFIRMADA'")
    Integer contarVendasEvento(@Param("eventoId") Long eventoId);
    
    @Query("SELECT e FROM Evento e WHERE e.ativo = true AND " +
           "e.status = 'ATIVO' AND " +
           "e.dataFim BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY e.dataFim")
    List<Evento> findEventosProximosAoFim(@Param("dataInicio") LocalDateTime dataInicio,
                                          @Param("dataFim") LocalDateTime dataFim);
}
