package com.tcc.estoque.model;

import com.tcc.estoque.model.enums.TipoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
// import java.util.stream.Collectors;  // REMOVIDO - não usado mais

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter um formato válido")
    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    private String email;

    @Column(name = "senha", nullable = false)
    @NotBlank(message = "Senha é obrigatória")
    private String senha;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "cpf", length = 14)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false)
    @NotNull(message = "Tipo de usuário é obrigatório")
    @Builder.Default
    private TipoUsuario tipoUsuario = TipoUsuario.VENDEDOR;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Builder.Default
    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro = LocalDateTime.now();

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

    @Builder.Default
    @Column(name = "tentativas_login")
    private Integer tentativasLogin = 0;

    @Builder.Default
    @Column(name = "bloqueado")
    private Boolean bloqueado = false;

    @Column(name = "data_bloqueio")
    private LocalDateTime dataBloqueio;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Todos são usuários
        authorities.add(new SimpleGrantedAuthority("ROLE_" + tipoUsuario.name())); // ROLE_ADMIN, ROLE_VENDEDOR, etc.
        
        tipoUsuario.getPermissoes().stream()
                .map(permissao -> new SimpleGrantedAuthority(permissao))
                .forEach(authorities::add);
        
        return authorities;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ativo != null && ativo;
    }

    public enum Role {
        USER, ADMIN
    }

    public boolean isOwner() {
        return tipoUsuario.isOwner();
    }

    public boolean isAdmin() {
        return tipoUsuario.isAdmin();
    }

    public boolean isVendedor() {
        return tipoUsuario.isVendedor();
    }

    public boolean isCompras() {
        return tipoUsuario.isCompras();
    }

    public boolean podeExcluirUsuario(TipoUsuario tipoUsuarioAlvo) {
        return tipoUsuario.podeExcluirUsuario(tipoUsuarioAlvo);
    }

    public boolean temPermissao(String permissao) {
        return tipoUsuario.temPermissao(permissao);
    }

    public boolean podeAcessarModulo(String modulo) {
        return tipoUsuario.podeAcessarModulo(modulo);
    }

    public List<String> getPermissoesModulo(String modulo) {
        return tipoUsuario.getPermissoesModulo(modulo);
    }

    public String getTipoDescricao() {
        return tipoUsuario.getDescricao();
    }

    public String getTipoCor() {
        return tipoUsuario.getCor();
    }
}
