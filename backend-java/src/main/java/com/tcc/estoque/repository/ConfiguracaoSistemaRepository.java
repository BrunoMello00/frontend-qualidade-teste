package com.tcc.estoque.repository;

import com.tcc.estoque.model.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para configurações do sistema
 */
@Repository
public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, Long> {
    
    /**
     * Busca configuração por chave
     */
    Optional<ConfiguracaoSistema> findByChave(String chave);
    
    /**
     * Busca configurações por tipo
     */
    List<ConfiguracaoSistema> findByTipo(String tipo);
    
    /**
     * Busca configurações por prefixo da chave
     */
    @Query("SELECT c FROM ConfiguracaoSistema c WHERE c.chave LIKE :prefixo%")
    List<ConfiguracaoSistema> findByChaveStartingWith(@Param("prefixo") String prefixo);
    
    /**
     * Busca configurações por categoria (sistema, estoque, vendas, etc.)
     */
    @Query("SELECT c FROM ConfiguracaoSistema c WHERE c.chave LIKE :categoria%")
    List<ConfiguracaoSistema> findByCategoria(@Param("categoria") String categoria);
    
    /**
     * Verifica se existe configuração por chave
     */
    boolean existsByChave(String chave);
    
    /**
     * Remove configuração por chave
     */
    void deleteByChave(String chave);
    
    /**
     * Busca todas as configurações ordenadas por chave
     */
    @Query("SELECT c FROM ConfiguracaoSistema c ORDER BY c.chave ASC")
    List<ConfiguracaoSistema> findAllOrderByChave();
    
    /**
     * Busca configurações ativas (não nulas)
     */
    @Query("SELECT c FROM ConfiguracaoSistema c WHERE c.valor IS NOT NULL")
    List<ConfiguracaoSistema> findAllActive();
}
