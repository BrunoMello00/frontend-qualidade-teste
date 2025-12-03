package com.tcc.estoque.repository;

import com.tcc.estoque.model.Devolucao;
import com.tcc.estoque.model.Venda;
import com.tcc.estoque.model.enums.TipoDevolucao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DevolucaoRepository extends JpaRepository<Devolucao, Long> {

    List<Devolucao> findByVenda(Venda venda);

    List<Devolucao> findByTipoDevolucao(TipoDevolucao tipoDevolucao);

    @Query("SELECT d FROM Devolucao d WHERE d.dataDevolucao BETWEEN :inicio AND :fim")
    List<Devolucao> findByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT d FROM Devolucao d WHERE d.tipoDevolucao = :tipo AND d.dataDevolucao BETWEEN :inicio AND :fim")
    List<Devolucao> findByTipoAndPeriodo(@Param("tipo") TipoDevolucao tipo, 
                                        @Param("inicio") LocalDateTime inicio, 
                                        @Param("fim") LocalDateTime fim);

    @Query("SELECT d FROM Devolucao d WHERE d.usuario.id = :usuarioId")
    Page<Devolucao> findByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("SELECT COUNT(d) FROM Devolucao d WHERE d.tipoDevolucao = :tipo")
    Long countByTipoDevolucao(@Param("tipo") TipoDevolucao tipo);

    @Query("SELECT d FROM Devolucao d JOIN d.itens i WHERE i.produto.id = :produtoId")
    List<Devolucao> findByProdutoId(@Param("produtoId") Long produtoId);
}