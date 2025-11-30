package com.tcc.estoque.repository;

import com.tcc.estoque.model.SessaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gerenciamento de sessões de usuário
 */
@Repository
public interface SessaoUsuarioRepository extends JpaRepository<SessaoUsuario, Long> {

    /**
     * Busca sessão ativa por hash do token
     */
    Optional<SessaoUsuario> findByTokenHashAndAtivoTrue(String tokenHash);

    /**
     * Busca todas as sessões ativas de um usuário
     */
    List<SessaoUsuario> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    /**
     * Busca sessões ativas de um usuário ordenadas por data de login (mais recente primeiro)
     */
    List<SessaoUsuario> findByUsuarioIdAndAtivoTrueOrderByDataLoginDesc(Long usuarioId);

    /**
     * Busca sessões expiradas que ainda estão marcadas como ativas
     */
    List<SessaoUsuario> findByDataExpiracaoBeforeAndAtivoTrue(LocalDateTime dataExpiracao);

    /**
     * Conta quantas sessões ativas um usuário possui
     */
    @Query("SELECT COUNT(s) FROM SessaoUsuario s WHERE s.usuario.id = :usuarioId AND s.ativo = true")
    Long countActiveSessionsByUserId(@Param("usuarioId") Long usuarioId);

    /**
     * Busca sessões por IP address (para análise de segurança)
     */
    List<SessaoUsuario> findByIpAddressAndAtivoTrue(String ipAddress);

    /**
     * Remove sessões antigas (mais de X dias) - para limpeza periódica
     */
    @Query("DELETE FROM SessaoUsuario s WHERE s.dataLogin < :dataLimite AND s.ativo = false")
    void deleteOldInactiveSessions(@Param("dataLimite") LocalDateTime dataLimite);
}