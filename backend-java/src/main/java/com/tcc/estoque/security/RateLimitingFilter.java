package com.tcc.estoque.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de Rate Limiting para proteção contra ataques DoS
 * Limita número de requisições por IP
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter implements Filter {

    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int MAX_LOGIN_ATTEMPTS_PER_MINUTE = 5;
    private static final long MINUTE_IN_MILLIS = 60 * 1000;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientIp = getClientIp(httpRequest);
        String requestUri = httpRequest.getRequestURI();
        
        if (isRateLimited(clientIp, requestUri)) {
            log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, requestUri);
            
            httpResponse.setStatus(429); // HTTP 429 Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isRateLimited(String clientIp, String requestUri) {
        long currentTime = System.currentTimeMillis();
        
        RequestCounter counter = requestCounts.computeIfAbsent(clientIp, k -> new RequestCounter());
        
        if (currentTime - counter.getWindowStart() > MINUTE_IN_MILLIS) {
            counter.reset(currentTime);
        }
        
        if (requestUri.contains("/api/auth/login")) {
            if (counter.getLoginAttempts().incrementAndGet() > MAX_LOGIN_ATTEMPTS_PER_MINUTE) {
                return true;
            }
        }
        
        if (counter.getTotalRequests().incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            return true;
        }
        
        return false;
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
    
    /**
     * Classe interna para rastrear contadores de requisições
     */
    private static class RequestCounter {
        private volatile long windowStart;
        private final AtomicInteger totalRequests = new AtomicInteger(0);
        private final AtomicInteger loginAttempts = new AtomicInteger(0);
        
        public RequestCounter() {
            this.windowStart = System.currentTimeMillis();
        }
        
        public void reset(long newWindowStart) {
            this.windowStart = newWindowStart;
            this.totalRequests.set(0);
            this.loginAttempts.set(0);
        }
        
        public long getWindowStart() {
            return windowStart;
        }
        
        public AtomicInteger getTotalRequests() {
            return totalRequests;
        }
        
        public AtomicInteger getLoginAttempts() {
            return loginAttempts;
        }
    }
}
