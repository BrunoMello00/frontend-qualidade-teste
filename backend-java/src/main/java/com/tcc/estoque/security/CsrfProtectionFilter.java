package com.tcc.estoque.security;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de proteção CSRF customizado para operações sensíveis
 * Implementa Double Submit Cookie Pattern
 * TEMPORARIAMENTE DESABILITADO - Conflitando com JWT para APIs REST
 */
// @Component  // DESABILITADO: Causando conflitos com autenticação JWT
@Slf4j
public class CsrfProtectionFilter implements Filter {

    private final ConcurrentHashMap<String, Long> csrfTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    
    private final List<String> protectedEndpoints = Arrays.asList(
        "/api/auth/register",
        "/api/auth/reset-password", 
        "/api/auth/new-password",
        "/api/usuarios",
        "/api/produtos",
        "/api/vendas",
        "/api/admin"
    );
    
    private final List<String> protectedMethods = Arrays.asList("POST", "PUT", "DELETE");
    
    private static final String CSRF_TOKEN_HEADER = "X-CSRF-Token";
    private static final String CSRF_TOKEN_COOKIE = "CSRF-TOKEN";
    private static final long TOKEN_VALIDITY_DURATION = 3600000; // 1 hora

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String method = httpRequest.getMethod();
        String requestUri = httpRequest.getRequestURI();
        
        if (requiresCsrfProtection(method, requestUri)) {
            if (!validateCsrfToken(httpRequest)) {
                log.warn("SECURITY_EVENT=CSRF_ATTACK_BLOCKED ip={} uri={} method={}", 
                        getClientIp(httpRequest), requestUri, method);
                
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(
                    "{\"error\":\"CSRF token inválido\",\"message\":\"Requisição bloqueada por proteção CSRF\"}"
                );
                return;
            }
        }
        
        if ("GET".equals(method) && isSensitiveEndpoint(requestUri)) {
            String csrfToken = generateCsrfToken();
            httpResponse.setHeader("X-CSRF-Token", csrfToken);
            
            httpResponse.setHeader("Set-Cookie", 
                CSRF_TOKEN_COOKIE + "=" + csrfToken + 
                "; HttpOnly; Secure; SameSite=Strict; Max-Age=3600");
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean requiresCsrfProtection(String method, String requestUri) {
        if (!protectedMethods.contains(method)) {
            return false;
        }
        
        return protectedEndpoints.stream()
                .anyMatch(requestUri::startsWith);
    }
    
    private boolean isSensitiveEndpoint(String requestUri) {
        return protectedEndpoints.stream()
                .anyMatch(requestUri::startsWith);
    }
    
    private boolean validateCsrfToken(HttpServletRequest request) {
        String headerToken = request.getHeader(CSRF_TOKEN_HEADER);
        String cookieToken = getCookieValue(request, CSRF_TOKEN_COOKIE);
        
        if (headerToken == null || cookieToken == null) {
            log.debug("CSRF validation failed: missing tokens - header={}, cookie={}", 
                    headerToken != null, cookieToken != null);
            return false;
        }
        
        if (!headerToken.equals(cookieToken)) {
            log.debug("CSRF validation failed: token mismatch");
            return false;
        }
        
        Long tokenTimestamp = csrfTokens.get(headerToken);
        if (tokenTimestamp == null) {
            log.debug("CSRF validation failed: token not found in cache");
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - tokenTimestamp > TOKEN_VALIDITY_DURATION) {
            csrfTokens.remove(headerToken);
            log.debug("CSRF validation failed: token expired");
            return false;
        }
        
        return true;
    }
    
    private String generateCsrfToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        
        StringBuilder token = new StringBuilder();
        for (byte b : tokenBytes) {
            token.append(String.format("%02x", b));
        }
        
        String csrfToken = token.toString();
        csrfTokens.put(csrfToken, System.currentTimeMillis());
        
        cleanExpiredTokens();
        
        log.debug("Generated new CSRF token: {}", csrfToken.substring(0, 8) + "...");
        return csrfToken;
    }
    
    private void cleanExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        csrfTokens.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > TOKEN_VALIDITY_DURATION);
    }
    
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
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
