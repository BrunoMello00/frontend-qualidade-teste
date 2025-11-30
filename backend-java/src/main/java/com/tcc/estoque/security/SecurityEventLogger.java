package com.tcc.estoque.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

/**
 * Logger de eventos de segurança para auditoria e monitoramento
 */
// @Component TEMPORARIAMENTE DESABILITADO PARA TESTE
@Slf4j
public class SecurityEventLogger {

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ip = getClientIp();
        String userAgent = getUserAgent();
        
        log.info("SECURITY_EVENT=LOGIN_SUCCESS username={} ip={} user_agent={} timestamp={}", 
                username, ip, userAgent, Instant.now());
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String ip = getClientIp();
        String userAgent = getUserAgent();
        String reason = event.getException().getClass().getSimpleName();
        
        log.warn("SECURITY_EVENT=LOGIN_FAILURE username={} ip={} user_agent={} reason={} timestamp={}", 
                username, ip, userAgent, reason, Instant.now());
        
        detectBruteForceAttack(ip, username);
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        String username = getCurrentUsername();
        String ip = getClientIp();
        String resource = event.getAuthorizationDecision().toString();
        
        log.warn("SECURITY_EVENT=ACCESS_DENIED username={} ip={} resource={} timestamp={}", 
                username, ip, resource, Instant.now());
    }

    private void detectBruteForceAttack(String ip, String username) {
        
        log.warn("SECURITY_ALERT=POTENTIAL_BRUTE_FORCE ip={} username={} timestamp={}", 
                ip, username, Instant.now());
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getCurrentUsername() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
