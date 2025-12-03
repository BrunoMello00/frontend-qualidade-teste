package com.tcc.estoque.repository;

import com.tcc.estoque.model.Recompensa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecompensaRepository extends JpaRepository<Recompensa, Long> {

    /**
     * Busca recompensas ativas ordenadas por pontos necessários
     */
    List<Recompensa> findByAtivoTrueOrderByPontosNecessariosAsc();

    /**
     * Busca recompensas por categoria
     */
    List<Recompensa> findByCategoriaAndAtivoTrueOrderByPontosNecessariosAsc(String categoria);

    /**
     * Busca recompensas que o cliente pode resgatar (tem pontos suficientes)
     */
    @Query("SELECT r FROM Recompensa r WHERE r.ativo = true " +
           "AND r.pontosNecessarios <= :pontosCliente " +
           "AND (r.dataValidade IS NULL OR r.dataValidade > CURRENT_TIMESTAMP) " +
           "AND (r.quantidadeDisponivel IS NULL OR r.quantidadeResgatada < r.quantidadeDisponivel) " +
           "ORDER BY r.pontosNecessarios ASC")
    List<Recompensa> findRecompensasDisponiveis(@Param("pontosCliente") int pontosCliente);

    /**
     * Busca recompensas por faixa de pontos
     */
    @Query("SELECT r FROM Recompensa r WHERE r.ativo = true " +
           "AND r.pontosNecessarios BETWEEN :pontosMin AND :pontosMax " +
           "ORDER BY r.pontosNecessarios ASC")
    List<Recompensa> findByPontosBetween(@Param("pontosMin") int pontosMin, 
                                        @Param("pontosMax") int pontosMax);

    /**
     * Busca todas as categorias de recompensas
     */
    @Query("SELECT DISTINCT r.categoria FROM Recompensa r WHERE r.categoria IS NOT NULL ORDER BY r.categoria")
    List<String> findAllCategorias();
    
    /**
     * Conta recompensas ativas
     */
    long countByAtivoTrue();
}