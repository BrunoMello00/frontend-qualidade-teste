package com.tcc.estoque.security;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Web Application Firewall (WAF) - Proteção contra ataques comuns
 * Filtra requisições maliciosas antes que cheguem à aplicação
 */
@Slf4j
public class WebApplicationFirewall implements Filter {

    private final List<Pattern> sqlInjectionPatterns = Arrays.asList(
        Pattern.compile("(?i).*union.*select.*", Pattern.DOTALL),
        Pattern.compile("(?i).*drop\\s+table.*", Pattern.DOTALL),
        Pattern.compile("(?i).*insert\\s+into.*", Pattern.DOTALL),
        Pattern.compile("(?i).*delete\\s+from.*", Pattern.DOTALL),
        Pattern.compile("(?i).*update\\s+.*set.*", Pattern.DOTALL),
        Pattern.compile("(?i).*exec\\s*\\(.*", Pattern.DOTALL),
        Pattern.compile("(?i).*'\\s*or\\s*'1'\\s*=\\s*'1.*", Pattern.DOTALL),
        Pattern.compile("(?i).*'\\s*or\\s*1\\s*=\\s*1.*", Pattern.DOTALL),
        Pattern.compile("(?i).*--\\s*.*", Pattern.DOTALL),
        Pattern.compile("(?i).*\\/\\*.*\\*\\/.*", Pattern.DOTALL)
    );

    private final List<Pattern> xssPatterns = Arrays.asList(
        Pattern.compile("(?i).*<script[^>]*>.*", Pattern.DOTALL),
        Pattern.compile("(?i).*</script>.*", Pattern.DOTALL),
        Pattern.compile("(?i).*javascript:.*", Pattern.DOTALL),
        Pattern.compile("(?i).*on\\w+\\s*=.*", Pattern.DOTALL),
        Pattern.compile("(?i).*<iframe[^>]*>.*", Pattern.DOTALL),
        Pattern.compile("(?i).*<object[^>]*>.*", Pattern.DOTALL),
        Pattern.compile("(?i).*<embed[^>]*>.*", Pattern.DOTALL),
        Pattern.compile("(?i).*vbscript:.*", Pattern.DOTALL),
        Pattern.compile("(?i).*expression\\s*\\(.*", Pattern.DOTALL)
    );

    private final List<Pattern> commandInjectionPatterns = Arrays.asList(
        Pattern.compile("(?i).*(\\||&|;|\\$|`|\\n|\\r).*", Pattern.DOTALL),
        Pattern.compile("(?i).*\\.\\.[\\/\\\\].*", Pattern.DOTALL),
        Pattern.compile("(?i).*(cmd|command|exec|system|shell).*", Pattern.DOTALL),
        Pattern.compile("(?i).*(rm|mv|cp|cat|chmod|chown)\\s+.*", Pattern.DOTALL)
    );

    private final List<Pattern> pathTraversalPatterns = Arrays.asList(
        Pattern.compile(".*\\.\\.[\\/\\\\].*"),
        Pattern.compile(".*[\\/\\\\]\\.\\..*"),
        Pattern.compile(".*%2e%2e[\\/\\\\].*"),
        Pattern.compile(".*%252e%252e[\\/\\\\].*"),
        Pattern.compile(".*\\.\\.%2f.*"),
        Pattern.compile(".*\\.\\.%5c.*")
    );

    private final List<String> blockedIps = Arrays.asList(
        // "127.0.0.1", // exemplo - removido para testes locais
        // "192.168.1.1" // exemplo - removido para testes locais
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientIp = getClientIp(httpRequest);
        String requestUri = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        String userAgent = httpRequest.getHeader("User-Agent");
        
        // 1. Verificar IP bloqueado
        if (isBlockedIp(clientIp)) {
            logSecurityEvent("BLOCKED_IP", clientIp, requestUri, "IP está na lista de bloqueio");
            sendSecurityResponse(httpResponse, "IP bloqueado", 403);
            return;
        }
        
        // 2. Verificar User-Agent suspeito
        if (isSuspiciousUserAgent(userAgent)) {
            logSecurityEvent("SUSPICIOUS_USER_AGENT", clientIp, requestUri, userAgent);
            sendSecurityResponse(httpResponse, "User-Agent suspeito", 403);
            return;
        }
        
        // 3. Verificar Path Traversal na URI
        if (containsPathTraversal(requestUri)) {
            logSecurityEvent("PATH_TRAVERSAL_ATTEMPT", clientIp, requestUri, "Path traversal detectado na URI");
            sendSecurityResponse(httpResponse, "Acesso negado", 403);
            return;
        }
        
        // 4. Verificar padrões maliciosos na query string
        if (queryString != null && containsMaliciousPatterns(queryString)) {
            logSecurityEvent("MALICIOUS_QUERY", clientIp, requestUri, "Padrão malicioso na query string");
            sendSecurityResponse(httpResponse, "Query inválida", 403);
            return;
        }
        
        // 5. Verificar headers maliciosos
        if (containsMaliciousHeaders(httpRequest)) {
            logSecurityEvent("MALICIOUS_HEADERS", clientIp, requestUri, "Headers maliciosos detectados");
            sendSecurityResponse(httpResponse, "Headers inválidos", 403);
            return;
        }
        
        // 6. Verificar tamanho da requisição
        if (isRequestTooLarge(httpRequest)) {
            logSecurityEvent("REQUEST_TOO_LARGE", clientIp, requestUri, "Requisição muito grande");
            sendSecurityResponse(httpResponse, "Requisição muito grande", 413);
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isBlockedIp(String clientIp) {
        return blockedIps.contains(clientIp);
    }
    
    private boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return true; // User-Agent vazio é suspeito
        }
        
        String lowerUserAgent = userAgent.toLowerCase();
        
        String[] suspiciousAgents = {
            "sqlmap", "nikto", "nmap", "masscan", "zap", "burp", 
            "nessus", "openvas", "w3af", "skipfish", "gobuster",
            "dirb", "dirbuster", "wfuzz", "ffuf", "curl/7.0"
        };
        
        for (String suspicious : suspiciousAgents) {
            if (lowerUserAgent.contains(suspicious)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean containsPathTraversal(String uri) {
        return pathTraversalPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(uri).matches());
    }
    
    private boolean containsMaliciousPatterns(String input) {
        String lowerInput = input.toLowerCase();
        
        if (sqlInjectionPatterns.stream().anyMatch(pattern -> pattern.matcher(lowerInput).matches())) {
            return true;
        }
        
        if (xssPatterns.stream().anyMatch(pattern -> pattern.matcher(lowerInput).matches())) {
            return true;
        }
        
        if (commandInjectionPatterns.stream().anyMatch(pattern -> pattern.matcher(lowerInput).matches())) {
            return true;
        }
        
        return false;
    }
    
    private boolean containsMaliciousHeaders(HttpServletRequest request) {
        String[] headersToCheck = {
            "X-Forwarded-For", "X-Real-IP", "Host", "Referer", 
            "X-Forwarded-Host", "X-Forwarded-Proto"
        };
        
        for (String headerName : headersToCheck) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && containsMaliciousPatterns(headerValue)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isRequestTooLarge(HttpServletRequest request) {
        String contentLength = request.getHeader("Content-Length");
        if (contentLength != null) {
            try {
                long length = Long.parseLong(contentLength);
                return length > 10 * 1024 * 1024; // 10MB limite
            } catch (NumberFormatException e) {
                return true; // Content-Length inválido é suspeito
            }
        }
        return false;
    }
    
    private void logSecurityEvent(String eventType, String clientIp, String uri, String details) {
        log.warn("WAF_BLOCK event_type={} client_ip={} uri={} details={} timestamp={}", 
                eventType, clientIp, uri, details, System.currentTimeMillis());
    }
    
    private void sendSecurityResponse(HttpServletResponse response, String message, int status) 
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"Security violation\",\"message\":\"%s\",\"timestamp\":%d}", 
            message, System.currentTimeMillis()
        ));
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
