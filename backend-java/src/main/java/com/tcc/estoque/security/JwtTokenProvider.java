package com.tcc.estoque.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.tcc.estoque.model.Usuario;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Provedor de tokens JWT com segurança robusta
 * Implementa geração, validação e refresh de tokens
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:minha-chave-secreta-super-segura-2024-para-sistema-estoque-vendas-local-h2}") String jwtSecret,
            @Value("${jwt.expiration:3600000}") long jwtExpirationMs, // 1 hora
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs // 7 dias
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        
        log.info("JWT Provider inicializado - Access Token: {}ms, Refresh Token: {}ms", 
                jwtExpirationMs, refreshExpirationMs);
    }

    /**
     * Gera token de acesso JWT
     */
    public String generateToken(Authentication authentication) {
        Usuario userPrincipal = (Usuario) authentication.getPrincipal();
        return generateAccessToken(userPrincipal);
    }

    /**
     * Gera token de acesso para usuário
     */
    public String generateAccessToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtExpirationMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId())
                .claim("role", usuario.getRole().name())
                .claim("tipoUsuario", usuario.getTipoUsuario().name())
                .claim("nome", usuario.getNome())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Gera token de refresh
     */
    public String generateRefreshToken(Authentication authentication) {
        Usuario userPrincipal = (Usuario) authentication.getPrincipal();
        return generateRefreshToken(userPrincipal);
    }

    /**
     * Gera token de refresh para usuário
     */
    public String generateRefreshToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(refreshExpirationMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extrai email do token
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Erro ao extrair email do token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    /**
     * Extrai ID do usuário do token
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("userId", Long.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Erro ao extrair userId do token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    /**
     * Extrai role do token
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("role", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Erro ao extrair role do token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    /**
     * Extrai tipo de usuário do token
     */
    public String getTipoUsuarioFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("tipoUsuario", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Erro ao extrair tipoUsuario do token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    /**
     * Valida token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("SECURITY_EVENT=EXPIRED_JWT_TOKEN token={}", token.substring(0, Math.min(10, token.length())) + "...");
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("SECURITY_EVENT=UNSUPPORTED_JWT_TOKEN token={}", token.substring(0, Math.min(10, token.length())) + "...");
            return false;
        } catch (MalformedJwtException e) {
            log.error("SECURITY_EVENT=INVALID_JWT_TOKEN token={}", token.substring(0, Math.min(10, token.length())) + "...");
            return false;
        } catch (SecurityException e) {
            log.error("SECURITY_EVENT=INVALID_JWT_SIGNATURE token={}", token.substring(0, Math.min(10, token.length())) + "...");
            return false;
        } catch (IllegalArgumentException e) {
            log.error("SECURITY_EVENT=EMPTY_JWT_CLAIMS token={}", token.substring(0, Math.min(10, token.length())) + "...");
            return false;
        }
    }

    /**
     * Verifica se é token de refresh
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return "refresh".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Obtém data de expiração do token
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Erro ao extrair data de expiração: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    /**
     * Verifica se token está expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Gera um ID único para o token (para blacklist)
     */
    public String getTokenId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            String email = claims.getSubject();
            long issuedAt = claims.getIssuedAt().getTime();
            return email + "_" + issuedAt;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Erro ao gerar ID do token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }
}
