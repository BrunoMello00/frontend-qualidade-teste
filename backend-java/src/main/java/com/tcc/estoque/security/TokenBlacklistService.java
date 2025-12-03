package com.tcc.estoque.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service para gerenciar blacklist de tokens JWT invalidados
 * Implementa limpeza automática de tokens expirados
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Date> blacklistedTokens = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public TokenBlacklistService() {
        scheduler.scheduleAtFixedRate(this::cleanExpiredTokens, 1, 1, TimeUnit.HOURS);
        log.info("TokenBlacklistService inicializado com limpeza automática");
    }

    /**
     * Adiciona token à blacklist
     */
    public void blacklistToken(String tokenId, Date expiration) {
        blacklistedTokens.put(tokenId, expiration);
        log.info("Token adicionado à blacklist: {}", tokenId);
    }

    /**
     * Verifica se token está na blacklist
     */
    public boolean isTokenBlacklisted(String tokenId) {
        return blacklistedTokens.containsKey(tokenId);
    }

    /**
     * Remove token da blacklist
     */
    public void removeFromBlacklist(String tokenId) {
        blacklistedTokens.remove(tokenId);
        log.debug("Token removido da blacklist: {}", tokenId);
    }

    /**
     * Limpa tokens expirados da blacklist
     */
    private void cleanExpiredTokens() {
        Date now = new Date();
        int removedCount = 0;

        var iterator = blacklistedTokens.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().before(now)) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Limpeza automática: {} tokens expirados removidos da blacklist", removedCount);
        }
    }

    /**
     * Obtém estatísticas da blacklist
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }

    /**
     * Limpa toda a blacklist (usar com cuidado)
     */
    public void clearBlacklist() {
        blacklistedTokens.clear();
        log.warn("SECURITY_EVENT: Blacklist completamente limpa");
    }
}