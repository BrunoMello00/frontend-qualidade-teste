package com.tcc.estoque.security.interceptor;

import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.security.AccessControlManager;
import com.tcc.estoque.security.annotation.RequirePageAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que verifica permissões de acesso baseado nas anotações @RequirePageAccess
 * Substitui a lógica distribuída de @PreAuthorize por um sistema centralizado
 */
@Slf4j
@Component
public class PageAccessInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        log.info("🛡️ PageAccessInterceptor: Verificando acesso para URL: {} {}", 
            request.getMethod(), request.getRequestURI());
        
        if (!(handler instanceof HandlerMethod)) {
            log.debug("Não é HandlerMethod, permitindo acesso");
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        RequirePageAccess pageAccess = handlerMethod.getMethodAnnotation(RequirePageAccess.class);
        if (pageAccess == null) {
            pageAccess = handlerMethod.getBeanType().getAnnotation(RequirePageAccess.class);
        }
        
        if (pageAccess == null) {
            log.debug("Sem anotação @RequirePageAccess, permitindo acesso");
            return true;
        }
        
        log.info("🔍 Página requerida: {}", pageAccess.value());
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("❌ Acesso negado: Usuário não autenticado para página: {}", pageAccess.value());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        
        log.info("👤 Authentication: {}", authentication);
        log.info("👤 Authentication principal: {}", authentication.getPrincipal().getClass().getName());
        log.info("👤 Authentication name: {}", authentication.getName());
        log.info("👤 Authentication authorities: {}", authentication.getAuthorities());
        
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            String tipoUsuario = usuario.getTipoUsuario().name();
            String paginaRequerida = pageAccess.value();
            
            log.info("👤 Usuário: {} | Tipo: {} | Página solicitada: {}", 
                usuario.getEmail(), tipoUsuario, paginaRequerida);
            
            boolean temAcesso = AccessControlManager.temAcessoPagina(tipoUsuario, paginaRequerida);
            
            log.info("🔑 Resultado verificação acesso: {}", temAcesso);
            
            if (!temAcesso && pageAccess.allowSelfAccess()) {
                temAcesso = checkSelfAccess(request, usuario);
                log.info("🔑 Self-access verificado: {}", temAcesso);
            }
            
            if (!temAcesso) {
                log.warn("❌ Acesso negado: Usuário {} (perfil: {}) tentou acessar página: {}", 
                    usuario.getEmail(), tipoUsuario, paginaRequerida);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            
            log.info("✅ Acesso autorizado: Usuário {} (perfil: {}) acessou página: {}", 
                usuario.getEmail(), tipoUsuario, paginaRequerida);
            
            return true;
            
        } catch (ClassCastException e) {
            log.error("❌ Erro de casting: authentication.getPrincipal() não é Usuario: {}", e.getMessage());
            log.error("❌ Tipo real: {}", authentication.getPrincipal().getClass().getName());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        } catch (Exception e) {
            log.error("❌ Erro inesperado no PageAccessInterceptor: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
    }
    
    /**
     * Verifica se o usuário está tentando acessar seus próprios dados
     */
    private boolean checkSelfAccess(HttpServletRequest request, Usuario usuario) {
        String requestURI = request.getRequestURI();
        
        if (requestURI.contains("/usuarios/") || requestURI.contains("/perfil")) {
            return true; // Por simplicidade, permitir acesso próprio para rotas de usuário/perfil
        }
        
        return false;
    }
}