package com.tcc.estoque.security;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtro para adicionar headers de segurança em todas as respostas
 * Proteção contra XSS, Clickjacking, MIME sniffing, etc.
 */
@Slf4j
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        
        httpResponse.setHeader("X-Frame-Options", "DENY");
        
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        
        httpResponse.setHeader("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains; preload");
        
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self'; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'");
        
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        httpResponse.setHeader("Permissions-Policy", 
            "geolocation=(), " +
            "microphone=(), " +
            "camera=(), " +
            "payment=(), " +
            "usb=(), " +
            "magnetometer=(), " +
            "gyroscope=(), " +
            "accelerometer=()");
        
        httpResponse.setHeader("Server", "");
        
        if (isSensitiveEndpoint(request)) {
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isSensitiveEndpoint(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = 
                (HttpServletRequest) request;
            String uri = httpRequest.getRequestURI();
            
            return uri.contains("/api/auth/") || 
                   uri.contains("/api/usuarios/") || 
                   uri.contains("/api/relatorios/") ||
                   uri.contains("/api/vendas/");
        }
        return false;
    }
}
