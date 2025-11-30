package com.tcc.estoque.security;

import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utilitário para obter o usuário logado
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UsuarioRepository usuarioRepository;

    /**
     * Obtém o usuário atualmente logado
     */
    public Usuario getUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Usuário não autenticado");
        }

        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        return usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));
    }

    /**
     * Obtém o email do usuário logado
     */
    public String getEmailUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Usuário não autenticado");
        }

        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    /**
     * Verifica se o usuário está autenticado
     */
    public boolean isUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
               && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
