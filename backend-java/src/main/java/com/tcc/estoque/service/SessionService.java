package com.tcc.estoque.service;

import com.tcc.estoque.model.SessaoUsuario;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.SessaoUsuarioRepository;
import com.tcc.estoque.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Serviço para gerenciamento completo de sessões de usuário
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessaoUsuarioRepository sessaoRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Cria uma nova sessão para o usuário após login bem-sucedido
     */
    @Transactional
    public SessaoUsuario createSession(Usuario usuario, String token, HttpServletRequest request) {
        log.debug("Criando nova sessão para usuário: {}", usuario.getEmail());

        invalidateOldSessions(usuario.getId());

        SessaoUsuario sessao = SessaoUsuario.builder()
                .usuario(usuario)
                .tokenHash(hashToken(token))
                .dataLogin(LocalDateTime.now())
                .dataUltimoAcesso(LocalDateTime.now())
                .dataExpiracao(LocalDateTime.now().plusHours(1)) // 1 hora de validade
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .ativo(true)
                .build();

        sessao = sessaoRepository.save(sessao);
        log.info("SECURITY_EVENT=SESSION_CREATED user={} sessionId={} ip={}", 
                usuario.getEmail(), sessao.getId(), getClientIp(request));

        return sessao;
    }

    /**
     * Valida se uma sessão é válida com base no token
     */
    @Transactional
    public boolean isValidSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            // 1. Verificar se o token JWT é válido
            if (!jwtTokenProvider.validateToken(token)) {
                log.debug("Token JWT inválido");
                return false;
            }

            // 2. Verificar se existe sessão ativa no banco
            String tokenHash = hashToken(token);
            Optional<SessaoUsuario> sessaoOpt = sessaoRepository.findByTokenHashAndAtivoTrue(tokenHash);
            
            if (sessaoOpt.isEmpty()) {
                log.debug("Sessão não encontrada para token hash");
                return false;
            }

            SessaoUsuario sessao = sessaoOpt.get();

            // 3. Verificar se a sessão não expirou
            if (sessao.getDataExpiracao().isBefore(LocalDateTime.now())) {
                log.warn("SECURITY_EVENT=SESSION_EXPIRED sessionId={} user={}", 
                        sessao.getId(), sessao.getUsuario().getEmail());
                invalidateSession(sessao.getId());
                return false;
            }

            // 4. Atualizar último acesso
            updateLastAccess(sessao.getId());
            
            return true;

        } catch (Exception e) {
            log.error("Erro ao validar sessão: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtém usuário pela sessão ativa
     */
    public Optional<Usuario> getUserByToken(String token) {
        if (token == null) {
            return Optional.empty();
        }

        String tokenHash = hashToken(token);
        return sessaoRepository.findByTokenHashAndAtivoTrue(tokenHash)
                .map(SessaoUsuario::getUsuario);
    }

    /**
     * Invalida uma sessão específica
     */
    @Transactional
    public void invalidateSession(Long sessionId) {
        sessaoRepository.findById(sessionId).ifPresent(sessao -> {
            sessao.setAtivo(false);
            sessaoRepository.save(sessao);
            log.info("SECURITY_EVENT=SESSION_INVALIDATED sessionId={} user={}", 
                    sessionId, sessao.getUsuario().getEmail());
        });
    }

    /**
     * Invalida todas as sessões de um usuário
     */
    @Transactional
    public void invalidateAllUserSessions(Long userId) {
        sessaoRepository.findByUsuarioIdAndAtivoTrue(userId).forEach(sessao -> {
            sessao.setAtivo(false);
            sessaoRepository.save(sessao);
        });
        log.info("SECURITY_EVENT=ALL_SESSIONS_INVALIDATED userId={}", userId);
    }

    /**
     * Invalida sessões antigas do usuário (mantém apenas a mais recente)
     */
    @Transactional
    public void invalidateOldSessions(Long userId) {
        var sessoesAtivas = sessaoRepository.findByUsuarioIdAndAtivoTrueOrderByDataLoginDesc(userId);
        
        for (int i = 1; i < sessoesAtivas.size(); i++) {
            SessaoUsuario sessao = sessoesAtivas.get(i);
            sessao.setAtivo(false);
            sessaoRepository.save(sessao);
        }
        
        if (sessoesAtivas.size() > 1) {
            log.info("SECURITY_EVENT=OLD_SESSIONS_INVALIDATED userId={} count={}", 
                    userId, sessoesAtivas.size() - 1);
        }
    }

    /**
     * Atualiza o último acesso de uma sessão
     */
    @Transactional
    public void updateLastAccess(Long sessionId) {
        sessaoRepository.findById(sessionId).ifPresent(sessao -> {
            sessao.setDataUltimoAcesso(LocalDateTime.now());
            sessaoRepository.save(sessao);
        });
    }

    /**
     * Limpa sessões expiradas (para ser executado periodicamente)
     */
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime agora = LocalDateTime.now();
        var sessoesExpiradas = sessaoRepository.findByDataExpiracaoBeforeAndAtivoTrue(agora);
        
        sessoesExpiradas.forEach(sessao -> {
            sessao.setAtivo(false);
            sessaoRepository.save(sessao);
        });

        if (!sessoesExpiradas.isEmpty()) {
            log.info("SECURITY_EVENT=EXPIRED_SESSIONS_CLEANED count={}", sessoesExpiradas.size());
        }
    }

    /**
     * Gera hash do token para armazenamento seguro
     */
    private String hashToken(String token) {
        return token.length() > 10 ? token.substring(token.length() - 10) : token;
    }

    /**
     * Obtém IP do cliente considerando proxies
     */
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