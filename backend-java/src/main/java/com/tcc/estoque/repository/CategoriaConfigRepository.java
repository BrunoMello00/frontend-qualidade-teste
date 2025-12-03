package com.tcc.estoque.repository;

import com.tcc.estoque.model.CategoriaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaConfigRepository extends JpaRepository<CategoriaConfig, Long> {

    /**
     * Busca todas as categorias ativas ordenadas pela ordem
     */
    List<CategoriaConfig> findByAtivoTrueOrderByOrdemAsc();

    /**
     * Busca categoria por nome (case insensitive)
     */
    Optional<CategoriaConfig> findByNomeIgnoreCase(String nome);

    /**
     * Busca categoria que contém determinada quantidade de pontos
     */
    @Query(value = "SELECT c FROM CategoriaConfig c WHERE c.ativo = true " +
           "AND c.pontosMinimos <= :pontos " +
           "AND (c.pontosMaximos IS NULL OR c.pontosMaximos >= :pontos) " +
           "ORDER BY c.ordem ASC")
    Optional<CategoriaConfig> findCategoriaParaPontos(@Param("pontos") int pontos);

    /**
     * Busca a próxima categoria (para progressão)
     */
    @Query("SELECT c FROM CategoriaConfig c WHERE c.ativo = true " +
           "AND c.pontosMinimos > :pontosAtuais " +
           "ORDER BY c.pontosMinimos ASC")
    Optional<CategoriaConfig> findProximaCategoria(@Param("pontosAtuais") int pontosAtuais);

    /**
     * Verifica se já existe uma categoria com determinado range de pontos
     * Permite subcategorias fechadas dentro de categorias abertas
     * Conflitos ocorrem em:
     * 1. Categoria fechada sobrepõe outra categoria fechada
     * 2. Categoria aberta começa antes de outra categoria (fechada ou aberta)
     * 3. Categoria fechada começa antes do início de categoria aberta existente
     */
    @Query("SELECT COUNT(c) FROM CategoriaConfig c WHERE c.ativo = true " +
           "AND (" +
           "   (:pontosMax IS NOT NULL AND c.pontosMaximos IS NOT NULL " +
           "    AND :pontosMin <= c.pontosMaximos AND :pontosMax >= c.pontosMinimos) " +
           "   OR (:pontosMax IS NULL AND :pontosMin < c.pontosMinimos) " +
           "   OR (:pontosMax IS NOT NULL AND c.pontosMaximos IS NULL AND :pontosMin < c.pontosMinimos) " +
           ") " +
           "AND (:id IS NULL OR c.id != :id)")
    long countCategoriasComConflito(@Param("pontosMin") int pontosMin, 
                                   @Param("pontosMax") Integer pontosMax, 
                                   @Param("id") Long id);

    /**
     * Busca categorias por ordem de pontos
     */
    List<CategoriaConfig> findByAtivoTrueOrderByPontosMinimosAsc();
}