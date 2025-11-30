package com.tcc.estoque.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço para controle de tentativas de login e bloqueio de contas
 * Implementa proteção contra ataques de força bruta
 */
@Service
@Slf4j
public class AccountLockoutService {

    private final ConcurrentHashMap<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    private static final int RESET_PERIOD_MINUTES = 60;

    /**
     * Registra uma tentativa de login falhada
     */
    public void registerFailedAttempt(String email) {
        AttemptInfo info = loginAttempts.computeIfAbsent(email, k -> new AttemptInfo());
        
        if (info.isResetPeriodExpired()) {
            info.reset();
        }
        
        info.incrementAttempts();
        
        log.warn("SECURITY_EVENT=FAILED_LOGIN email={} attempts={} timestamp={}", 
                email, info.getAttempts(), LocalDateTime.now());
        
        if (info.getAttempts() >= MAX_ATTEMPTS) {
            info.lockAccount();
            log.error("SECURITY_EVENT=ACCOUNT_LOCKED email={} attempts={} lockout_until={}", 
                    email, info.getAttempts(), info.getLockoutExpiry());
        }
    }

    /**
     * Verifica se a conta está bloqueada
     */
    public boolean isAccountLocked(String email) {
        AttemptInfo info = loginAttempts.get(email);
        
        if (info == null) {
            return false;
        }
        
        if (info.isLockoutExpired()) {
            info.reset();
            log.info("SECURITY_EVENT=ACCOUNT_UNLOCKED email={} timestamp={}", 
                    email, LocalDateTime.now());
            return false;
        }
        
        return info.isLocked();
    }

    /**
     * Limpa as tentativas após login bem-sucedido
     */
    public void clearFailedAttempts(String email) {
        AttemptInfo info = loginAttempts.remove(email);
        if (info != null && info.getAttempts() > 0) {
            log.info("SECURITY_EVENT=LOGIN_SUCCESS_AFTER_FAILURES email={} previous_attempts={}", 
                    email, info.getAttempts());
        }
    }

    /**
     * Obtém informações sobre tentativas da conta
     */
    public AccountStatus getAccountStatus(String email) {
        AttemptInfo info = loginAttempts.get(email);
        
        if (info == null) {
            return new AccountStatus(false, 0, null);
        }
        
        return new AccountStatus(
            info.isLocked() && !info.isLockoutExpired(),
            info.getAttempts(),
            info.isLocked() ? info.getLockoutExpiry() : null
        );
    }

    /**
     * Desbloqueia uma conta manualmente (para administradores)
     */
    public void unlockAccount(String email) {
        AttemptInfo info = loginAttempts.remove(email);
        if (info != null) {
            log.info("SECURITY_EVENT=MANUAL_UNLOCK email={} admin_action=true", email);
        }
    }

    /**
     * Classe interna para rastrear tentativas de login
     */
    private static class AttemptInfo {
        private final AtomicInteger attempts = new AtomicInteger(0);
        private volatile LocalDateTime firstAttempt;
        private volatile LocalDateTime lockoutExpiry;
        private volatile boolean locked = false;

        public AttemptInfo() {
            this.firstAttempt = LocalDateTime.now();
        }

        public void incrementAttempts() {
            attempts.incrementAndGet();
        }

        public int getAttempts() {
            return attempts.get();
        }

        public void lockAccount() {
            this.locked = true;
            this.lockoutExpiry = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
        }

        public boolean isLocked() {
            return locked;
        }

        public LocalDateTime getLockoutExpiry() {
            return lockoutExpiry;
        }

        public boolean isLockoutExpired() {
            return lockoutExpiry != null && LocalDateTime.now().isAfter(lockoutExpiry);
        }

        public boolean isResetPeriodExpired() {
            return firstAttempt != null && 
                   LocalDateTime.now().isAfter(firstAttempt.plusMinutes(RESET_PERIOD_MINUTES));
        }

        public void reset() {
            attempts.set(0);
            locked = false;
            lockoutExpiry = null;
            firstAttempt = LocalDateTime.now();
        }
    }

    /**
     * Classe para retornar status da conta
     */
    public static class AccountStatus {
        private final boolean locked;
        private final int failedAttempts;
        private final LocalDateTime lockoutExpiry;

        public AccountStatus(boolean locked, int failedAttempts, LocalDateTime lockoutExpiry) {
            this.locked = locked;
            this.failedAttempts = failedAttempts;
            this.lockoutExpiry = lockoutExpiry;
        }

        public boolean isLocked() { return locked; }
        public int getFailedAttempts() { return failedAttempts; }
        public LocalDateTime getLockoutExpiry() { return lockoutExpiry; }
        
        public int getRemainingAttempts() {
            return Math.max(0, MAX_ATTEMPTS - failedAttempts);
        }
    }
}
