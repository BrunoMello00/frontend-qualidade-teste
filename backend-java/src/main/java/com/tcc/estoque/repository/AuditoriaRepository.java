package com.tcc.estoque.repository;

import com.tcc.estoque.model.Auditoria;
import com.tcc.estoque.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para auditoria de operações do sistema
 */
@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    
    /**
     * Busca auditoria por tabela
     */
    Page<Auditoria> findByTabelaOrderByTimestampOperacaoDesc(String tabela, Pageable pageable);
    
    /**
     * Busca auditoria por usuário
     */
    Page<Auditoria> findByUsuarioOrderByTimestampOperacaoDesc(Usuario usuario, Pageable pageable);
    
    /**
     * Busca auditoria por registro específico
     */
    List<Auditoria> findByTabelaAndRegistroIdOrderByTimestampOperacaoDesc(String tabela, Long registroId);
    
    /**
     * Busca auditoria por operação
     */
    Page<Auditoria> findByOperacaoOrderByTimestampOperacaoDesc(Auditoria.OperacaoAuditoria operacao, Pageable pageable);
    
    /**
     * Busca auditoria por período
     */
    @Query("SELECT a FROM Auditoria a WHERE a.timestampOperacao BETWEEN :inicio AND :fim ORDER BY a.timestampOperacao DESC")
    Page<Auditoria> findByPeriodo(@Param("inicio") LocalDateTime inicio, 
                                 @Param("fim") LocalDateTime fim, 
                                 Pageable pageable);
    
    /**
     * Busca auditoria por IP
     */
    Page<Auditoria> findByIpAddressOrderByTimestampOperacaoDesc(String ipAddress, Pageable pageable);
    
    /**
     * Busca auditoria com filtros combinados
     */
    @Query("SELECT a FROM Auditoria a WHERE " +
           "(:tabela IS NULL OR a.tabela = :tabela) AND " +
           "(:usuario IS NULL OR a.usuario = :usuario) AND " +
           "(:operacao IS NULL OR a.operacao = :operacao) AND " +
           "(:inicio IS NULL OR a.timestampOperacao >= :inicio) AND " +
           "(:fim IS NULL OR a.timestampOperacao <= :fim) " +
           "ORDER BY a.timestampOperacao DESC")
    Page<Auditoria> findComFiltros(@Param("tabela") String tabela,
                                   @Param("usuario") Usuario usuario,
                                   @Param("operacao") Auditoria.OperacaoAuditoria operacao,
                                   @Param("inicio") LocalDateTime inicio,
                                   @Param("fim") LocalDateTime fim,
                                   Pageable pageable);
    
    /**
     * Conta registros por tabela
     */
    @Query("SELECT a.tabela, COUNT(a) FROM Auditoria a GROUP BY a.tabela")
    List<Object[]> contarPorTabela();
    
    /**
     * Conta registros por usuário
     */
    @Query("SELECT u.nome, COUNT(a) FROM Auditoria a JOIN a.usuario u GROUP BY u.nome")
    List<Object[]> contarPorUsuario();
    
    /**
     * Conta registros por operação
     */
    @Query("SELECT a.operacao, COUNT(a) FROM Auditoria a GROUP BY a.operacao")
    List<Object[]> contarPorOperacao();
    
    /**
     * Busca auditoria por faixa de datas com estatísticas
     */
    @Query("SELECT COUNT(a) FROM Auditoria a WHERE a.timestampOperacao BETWEEN :inicio AND :fim")
    Long contarPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
    
    /**
     * Busca IPs mais ativos
     */
    @Query("SELECT a.ipAddress, COUNT(a) FROM Auditoria a WHERE a.ipAddress IS NOT NULL GROUP BY a.ipAddress ORDER BY COUNT(a) DESC")
    List<Object[]> ipsAtivos(Pageable pageable);
    
    /**
     * Busca últimas atividades por usuário
     */
    @Query("SELECT a FROM Auditoria a WHERE a.usuario = :usuario ORDER BY a.timestampOperacao DESC")
    List<Auditoria> ultimasAtividades(@Param("usuario") Usuario usuario, Pageable pageable);
    
    /**
     * Remove auditoria antiga (para limpeza)
     */
    void deleteByTimestampOperacaoBefore(LocalDateTime dataLimite);
    
    /**
     * Busca auditoria por múltiplos IPs (suspeitos)
     */
    @Query("SELECT a FROM Auditoria a WHERE a.ipAddress IN :ips ORDER BY a.timestampOperacao DESC")
    List<Auditoria> findByIpsMultiplos(@Param("ips") List<String> ips);
    
    /**
     * Busca auditoria de operações de exclusão
     */
    @Query("SELECT a FROM Auditoria a WHERE a.operacao = 'DELETE' ORDER BY a.timestampOperacao DESC")
    Page<Auditoria> findExclusoes(Pageable pageable);
}
