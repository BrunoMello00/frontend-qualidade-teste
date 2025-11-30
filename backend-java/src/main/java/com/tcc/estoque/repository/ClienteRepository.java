package com.tcc.estoque.repository;

import com.tcc.estoque.model.Cliente;
import com.tcc.estoque.model.enums.CategoriaCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Cliente> findByAtivoTrue();
    
    Page<Cliente> findByAtivoTrueOrderByPontosDesc(Pageable pageable);

    List<Cliente> findByAtivoFalse();

    List<Cliente> findByCategoria(CategoriaCliente categoria);

    List<Cliente> findByCategoriaAndAtivoTrue(CategoriaCliente categoria);

    @Query("SELECT c FROM Cliente c WHERE " +
           "(LOWER(c.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "c.cpf LIKE CONCAT('%', :termo, '%') OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<Cliente> findByTermoGeral(@Param("termo") String termo, Pageable pageable);

    @Query("SELECT c FROM Cliente c WHERE " +
           "(:termo IS NULL OR " +
           "  LOWER(c.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "  c.cpf LIKE CONCAT('%', :termo, '%') OR " +
           "  LOWER(c.email) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
           "(:categoria IS NULL OR c.categoria = :categoria) AND " +
           "(:ativo IS NULL OR c.ativo = :ativo) AND " +
           "(:dataCadastroInicio IS NULL OR c.dataCadastro >= :dataCadastroInicio) AND " +
           "(:dataCadastroFim IS NULL OR c.dataCadastro <= :dataCadastroFim) AND " +
           "(:ultimaCompraInicio IS NULL OR c.ultimaCompra >= :ultimaCompraInicio) AND " +
           "(:ultimaCompraFim IS NULL OR c.ultimaCompra <= :ultimaCompraFim) AND " +
           "(:pontosMinimos IS NULL OR c.pontos >= :pontosMinimos) AND " +
           "(:pontosMaximos IS NULL OR c.pontos <= :pontosMaximos)")
    Page<Cliente> findComFiltros(
            @Param("termo") String termo,
            @Param("categoria") CategoriaCliente categoria,
            @Param("ativo") Boolean ativo,
            @Param("dataCadastroInicio") LocalDateTime dataCadastroInicio,
            @Param("dataCadastroFim") LocalDateTime dataCadastroFim,
            @Param("ultimaCompraInicio") LocalDateTime ultimaCompraInicio,
            @Param("ultimaCompraFim") LocalDateTime ultimaCompraFim,
            @Param("pontosMinimos") Integer pontosMinimos,
            @Param("pontosMaximos") Integer pontosMaximos,
            Pageable pageable);

    long countByAtivoTrue();

    long countByCategoria(CategoriaCliente categoria);

    @Query("SELECT c FROM Cliente c WHERE c.ativo = true AND c.quantidadeCompras > 0 " +
           "ORDER BY c.quantidadeCompras DESC")
    List<Cliente> findClientesMaisFrequentes(Pageable pageable);

    @Query("SELECT c FROM Cliente c WHERE c.ativo = true AND c.totalCompras > 0 " +
           "ORDER BY c.totalCompras DESC")
    List<Cliente> findClientesMaiorValor(Pageable pageable);

    List<Cliente> findByPontosBetween(Integer pontosMin, Integer pontosMax);

    @Query("SELECT c FROM Cliente c WHERE c.ultimaCompra >= :dataLimite AND c.ativo = true")
    List<Cliente> findComComprasRecentes(@Param("dataLimite") LocalDateTime dataLimite);

    @Query("SELECT c FROM Cliente c WHERE " +
           "(c.ultimaCompra IS NULL OR c.ultimaCompra <= :dataLimite) AND c.ativo = true")
    List<Cliente> findSemComprasRecentes(@Param("dataLimite") LocalDateTime dataLimite);

    @Query("SELECT AVG(c.totalCompras) FROM Cliente c WHERE c.ativo = true AND c.totalCompras > 0")
    BigDecimal calcularTicketMedioGeral();

    @Query("SELECT SUM(c.totalCompras) FROM Cliente c WHERE c.ativo = true")
    BigDecimal calcularFaturamentoTotal();

    @Query("SELECT SUM(c.pontos) FROM Cliente c WHERE c.ativo = true")
    Integer calcularTotalPontosDistribuidos();

    List<Cliente> findByIsFakeTrue();

    List<Cliente> findByIsFakeFalse();

    @Query("SELECT c FROM Cliente c WHERE " +
           "MONTH(c.dataNascimento) = :mes AND c.ativo = true")
    List<Cliente> findAniversariantesDoMes(@Param("mes") int mes);

    @Query("SELECT c FROM Cliente c WHERE " +
           "MONTH(c.dataNascimento) = :mes AND DAY(c.dataNascimento) = :dia AND c.ativo = true")
    List<Cliente> findAniversariantesHoje(@Param("mes") int mes, @Param("dia") int dia);

    @Query("SELECT c.categoria, COUNT(c) FROM Cliente c WHERE c.ativo = true GROUP BY c.categoria")
    List<Object[]> contarClientesPorCategoria();

    @Query("SELECT c.categoria, SUM(c.totalCompras) FROM Cliente c WHERE c.ativo = true GROUP BY c.categoria")
    List<Object[]> faturamentoPorCategoria();

    List<Cliente> findByDataCadastroBetween(LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT c FROM Cliente c WHERE c.dataCadastro >= :dataLimite")
    List<Cliente> findClientesNovos(@Param("dataLimite") LocalDateTime dataLimite);

    @Query("SELECT c FROM Cliente c WHERE c.ativo = true ORDER BY c.pontos DESC")
    List<Cliente> findTopClientesPorPontos(Pageable pageable);

    @Query("SELECT c FROM Cliente c WHERE LOWER(c.endereco.cidade) LIKE LOWER(CONCAT('%', :cidade, '%'))")
    List<Cliente> findByCidade(@Param("cidade") String cidade);

    @Query("SELECT c FROM Cliente c WHERE LOWER(c.endereco.estado) = LOWER(:estado)")
    List<Cliente> findByEstado(@Param("estado") String estado);

    @Query("SELECT c FROM Cliente c WHERE LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<Cliente> findByNomeContainingIgnoreCase(@Param("nome") String nome);

    @Query("SELECT c FROM Cliente c WHERE c.observacoes IS NOT NULL AND c.observacoes != ''")
    List<Cliente> findComObservacoes();
    
    
    @Query("SELECT COUNT(c) FROM Cliente c WHERE CAST(c.dataCadastro AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)")
    Long countClientesNovosHoje();

    List<Cliente> findByDataCadastroAfter(LocalDateTime dataLimite);
}
