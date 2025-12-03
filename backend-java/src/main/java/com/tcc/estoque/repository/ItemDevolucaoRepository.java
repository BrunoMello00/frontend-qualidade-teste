package com.tcc.estoque.repository;

import com.tcc.estoque.model.ItemDevolucao;
import com.tcc.estoque.model.Devolucao;
import com.tcc.estoque.model.enums.StatusQualidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemDevolucaoRepository extends JpaRepository<ItemDevolucao, Long> {

    List<ItemDevolucao> findByDevolucao(Devolucao devolucao);

    List<ItemDevolucao> findByStatusQualidadeRetorno(StatusQualidade statusQualidade);

    @Query("SELECT i FROM ItemDevolucao i WHERE i.produto.id = :produtoId")
    List<ItemDevolucao> findByProdutoId(@Param("produtoId") Long produtoId);

    @Query("SELECT i FROM ItemDevolucao i WHERE i.statusQualidadeRetorno = :status AND i.produto.id = :produtoId")
    List<ItemDevolucao> findByProdutoIdAndStatus(@Param("produtoId") Long produtoId, 
                                                @Param("status") StatusQualidade status);

    @Query("SELECT SUM(i.quantidade) FROM ItemDevolucao i WHERE i.produto.id = :produtoId AND i.statusQualidadeRetorno = :status")
    Integer getTotalQuantidadeByProdutoAndStatus(@Param("produtoId") Long produtoId, 
                                               @Param("status") StatusQualidade status);
}