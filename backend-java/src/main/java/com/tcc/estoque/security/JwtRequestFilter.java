package com.tcc.estoque.security;

// import com.tcc.estoque.service.SessionService;  // REMOVIDO TEMPORARIAMENTE
import com.tcc.estoque.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtro JWT que intercepta requisições e valida tokens
 */
//@Component  // ATIVANDO NOVAMENTE PARA TESTE JWT
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioService usuarioService;
    // private final SessionService sessionService;  // REMOVIDO TEMPORARIAMENTE


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        boolean isPublic = isPublicEndpoint(path);
        return isPublic;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String requestTokenHeader = request.getHeader("Authorization");
        
        String jwtToken = null;
        
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                jwtTokenProvider.getEmailFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                log.warn("SECURITY_EVENT=UNABLE_TO_GET_JWT_TOKEN ip={}", getClientIp(request));
            } catch (Exception e) {
                log.warn("SECURITY_EVENT=JWT_TOKEN_EXPIRED ip={}", getClientIp(request));
            }
        } else {
            if (isProtectedEndpoint(request.getRequestURI())) {
                log.debug("JWT Token não encontrado ou não começa com Bearer String para endpoint protegido: {}", request.getRequestURI());
            }
        }

        // *** VALIDAÇÃO JWT SIMPLIFICADA PARA TESTE ***
        if (jwtToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            try {
                // 1. Validar token JWT
                if (jwtTokenProvider.validateToken(jwtToken)) {
                    // 2. Extrair email do token
                    String userEmail = jwtTokenProvider.getEmailFromToken(jwtToken);
                    
                    // 3. Carregar dados do usuário
                    UserDetails userDetails = usuarioService.loadUserByUsername(userEmail);
                    
                    // 4. Criar autenticação
                    UsernamePasswordAuthenticationToken authenticationToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    
                    log.debug("SECURITY_EVENT=JWT_AUTHENTICATION_SUCCESS user={} ip={}", 
                             userEmail, getClientIp(request));
                } else {
                    log.warn("SECURITY_EVENT=JWT_VALIDATION_FAILED ip={} uri={}", 
                            getClientIp(request), request.getRequestURI());
                }
            } catch (Exception e) {
                log.warn("SECURITY_EVENT=JWT_PROCESSING_ERROR ip={} uri={} error={}", 
                        getClientIp(request), request.getRequestURI(), e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.equals("/api/auth/login") ||
               uri.equals("/api/auth/register") ||
               uri.equals("/api/auth/check-mfa") ||
               uri.equals("/api/auth/reset-password") ||
               uri.equals("/api/auth/new-password") ||
               uri.startsWith("/api/auth/validate") ||
               uri.startsWith("/api/auth/refresh") ||
               uri.startsWith("/public/") ||
               uri.startsWith("/api/public/") ||
               uri.equals("/actuator/health") ||
               uri.equals("/api/actuator/health") ||
               uri.startsWith("/actuator/health") ||
               uri.startsWith("/api/actuator/health") ||
               uri.startsWith("/h2-console/") ||
               uri.startsWith("/swagger-ui/") ||
               uri.startsWith("/v3/api-docs/") ||
               uri.startsWith("/api/password/");
    }

    private boolean isProtectedEndpoint(String uri) {
        return uri.startsWith("/") && 
               !uri.startsWith("/auth/") && 
               !uri.startsWith("/public/");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
