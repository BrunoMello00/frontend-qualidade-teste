package com.tcc.estoque.repository;

import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.TipoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Usuario> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    List<Usuario> findByAtivoTrue();

    List<Usuario> findByAtivoFalse();

    List<Usuario> findByTipoUsuario(TipoUsuario tipoUsuario);

    List<Usuario> findByTipoUsuarioAndAtivoTrue(TipoUsuario tipoUsuario);

    List<Usuario> findByBloqueadoTrue();

    List<Usuario> findByBloqueadoFalse();

    @Query("SELECT u FROM Usuario u WHERE " +
           "(LOWER(u.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<Usuario> findByTermoGeral(@Param("termo") String termo, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE " +
           "(:termo IS NULL OR " +
           "  LOWER(u.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "  LOWER(u.email) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
           "(:tipoUsuario IS NULL OR u.tipoUsuario = :tipoUsuario) AND " +
           "(:ativo IS NULL OR u.ativo = :ativo)")
    Page<Usuario> findComFiltros(
            @Param("termo") String termo,
            @Param("tipoUsuario") TipoUsuario tipoUsuario,
            @Param("ativo") Boolean ativo,
            Pageable pageable);

    long countByAtivoTrue();

    long countByTipoUsuario(TipoUsuario tipoUsuario);

    long countByTipoUsuarioAndAtivoTrue(TipoUsuario tipoUsuario);

    @Query("SELECT u FROM Usuario u WHERE u.tentativasLogin >= :tentativas AND u.ativo = true")
    List<Usuario> findComMuitasTentativasLogin(@Param("tentativas") Integer tentativas);

    @Query("SELECT u FROM Usuario u WHERE " +
           "(u.ultimoAcesso IS NULL OR u.ultimoAcesso <= :dataLimite) AND u.ativo = true")
    List<Usuario> findSemAcessoRecente(@Param("dataLimite") LocalDateTime dataLimite);

    List<Usuario> findByDataCadastroBetween(LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT u FROM Usuario u WHERE DATE(u.dataCadastro) = DATE(:hoje)")
    List<Usuario> findCadastradosHoje(@Param("hoje") LocalDateTime hoje);

    @Query("SELECT u FROM Usuario u WHERE u.dataCadastro >= :dataLimite")
    List<Usuario> findCadastradosRecentemente(@Param("dataLimite") LocalDateTime dataLimite);

    @Query("SELECT u.tipoUsuario, COUNT(u) FROM Usuario u WHERE u.ativo = true GROUP BY u.tipoUsuario")
    List<Object[]> contarUsuariosPorTipo();

    @Query("SELECT u FROM Usuario u WHERE u.ultimoAcesso IS NOT NULL AND u.ativo = true " +
           "ORDER BY u.ultimoAcesso DESC")
    List<Usuario> findUsuariosMaisAtivos(Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE u.tipoUsuario = 'ADMIN' AND u.ativo = true")
    List<Usuario> findAdministradoresAtivos();

    @Query("SELECT u FROM Usuario u WHERE u.tipoUsuario = 'GERENTE' AND u.ativo = true")
    List<Usuario> findGerentesAtivos();

    @Query("SELECT u FROM Usuario u WHERE u.tipoUsuario = 'VENDEDOR' AND u.ativo = true")
    List<Usuario> findVendedoresAtivos();

    @Query("SELECT u FROM Usuario u WHERE u.tipoUsuario = 'ESTOQUISTA' AND u.ativo = true")
    List<Usuario> findEstoquistasAtivos();

    @Query("SELECT u FROM Usuario u WHERE " +
           "u.tipoUsuario IN ('ADMIN', 'GERENTE', 'VENDEDOR') AND u.ativo = true")
    List<Usuario> findUsuariosQuePodemVender();

    @Query("SELECT u FROM Usuario u WHERE " +
           "u.tipoUsuario IN ('ADMIN', 'GERENTE', 'ESTOQUISTA') AND u.ativo = true")
    List<Usuario> findUsuariosQuePodemGerenciarEstoque();

    @Query("SELECT u FROM Usuario u WHERE " +
           "u.tipoUsuario IN ('ADMIN', 'GERENTE') AND u.ativo = true")
    List<Usuario> findUsuariosComAcessoRelatoriosCompletos();

    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.ativo = true")
    Optional<Usuario> findByEmailAndAtivo(@Param("email") String email);

    Long countByBloqueadoTrue();

    Long countByDataCadastroAfter(LocalDateTime data);
}
