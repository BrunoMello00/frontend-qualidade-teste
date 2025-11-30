package com.tcc.estoque.service;

import com.tcc.estoque.dto.AuthDTO;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.UsuarioRepository;
import com.tcc.estoque.security.JwtTokenProvider;
import com.tcc.estoque.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Service de autenticação robusto com JWT real e refresh tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Realiza login e retorna tokens JWT
     */
    @Transactional
    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest loginRequest, HttpServletRequest request) {
        try {

            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(loginRequest.getEmail());
            
            if (usuarioOpt.isEmpty()) {
                log.warn("SECURITY_EVENT: Tentativa de login com email inexistente: {}", loginRequest.getEmail());
                throw new RuntimeException("Credenciais inválidas");
            }

            Usuario usuario = usuarioOpt.get();

            if (!usuario.getAtivo()) {
                log.warn("SECURITY_EVENT: Tentativa de login com usuário inativo: {}", loginRequest.getEmail());
                throw new RuntimeException("Conta inativa");
            }

            if (Boolean.TRUE.equals(usuario.getBloqueado())) {
                log.warn("SECURITY_EVENT: Tentativa de login com usuário bloqueado: {}", loginRequest.getEmail());
                throw new RuntimeException("Conta bloqueada");
            }

            if (!passwordEncoder.matches(loginRequest.getSenha(), usuario.getSenha())) {
                log.warn("SECURITY_EVENT: Senha incorreta para usuário: {}", loginRequest.getEmail());
                
                incrementarTentativasLogin(usuario);
                throw new RuntimeException("Credenciais inválidas");
            }


            resetarTentativasLogin(usuario);

            String accessToken = jwtTokenProvider.generateAccessToken(usuario);
            String refreshToken = jwtTokenProvider.generateRefreshToken(usuario);

            log.info("Login realizado com sucesso para o usuário: {}", loginRequest.getEmail());

            return AuthDTO.LoginResponse.builder()
                    .success(true)
                    .message("Login realizado com sucesso")
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(3600L) // 1 hora em segundos
                    .usuario(AuthDTO.UsuarioInfo.builder()
                            .id(usuario.getId())
                            .nome(usuario.getNome())
                            .email(usuario.getEmail())
                            .role(usuario.getRole().name())
                            .tipoUsuario(usuario.getTipoUsuario().name())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Erro durante o login para o usuário: {}", loginRequest.getEmail(), e);
            throw new RuntimeException("Erro durante o login: " + e.getMessage());
        }
    }

    /**
     * Refresh do access token usando refresh token
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        try {
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                throw new RuntimeException("Refresh token inválido");
            }

            if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
                throw new RuntimeException("Token fornecido não é um refresh token");
            }

            String tokenId = jwtTokenProvider.getTokenId(refreshToken);
            if (tokenBlacklistService.isTokenBlacklisted(tokenId)) {
                throw new RuntimeException("Refresh token foi invalidado");
            }

            String email = jwtTokenProvider.getEmailFromToken(refreshToken);
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            
            if (usuarioOpt.isEmpty()) {
                throw new RuntimeException("Usuário não encontrado");
            }

            Usuario usuario = usuarioOpt.get();

            if (!usuario.getAtivo()) {
                throw new RuntimeException("Conta inativa");
            }

            String newAccessToken = jwtTokenProvider.generateAccessToken(usuario);

            log.info("Access token renovado para usuário: {}", email);

            return Map.of(
                "accessToken", newAccessToken,
                "tokenType", "Bearer",
                "expiresIn", 3600
            );

        } catch (Exception e) {
            log.error("Erro ao renovar token: {}", e.getMessage());
            throw new RuntimeException("Erro ao renovar token: " + e.getMessage());
        }
    }

    /**
     * Logout seguro - adiciona tokens à blacklist
     */
    public void logout(String accessToken, String refreshToken) {
        try {
            if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
                String accessTokenId = jwtTokenProvider.getTokenId(accessToken);
                Date accessTokenExpiration = jwtTokenProvider.getExpirationDateFromToken(accessToken);
                tokenBlacklistService.blacklistToken(accessTokenId, accessTokenExpiration);
            }

            if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
                String refreshTokenId = jwtTokenProvider.getTokenId(refreshToken);
                Date refreshTokenExpiration = jwtTokenProvider.getExpirationDateFromToken(refreshToken);
                tokenBlacklistService.blacklistToken(refreshTokenId, refreshTokenExpiration);
            }

            log.info("Logout realizado com sucesso");

        } catch (Exception e) {
            log.error("Erro durante logout: {}", e.getMessage());
        }
    }

    /**
     * Valida se token é válido e não está na blacklist
     */
    public boolean validateToken(String token) {
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                return false;
            }

            String tokenId = jwtTokenProvider.getTokenId(token);
            if (tokenBlacklistService.isTokenBlacklisted(tokenId)) {
                log.warn("SECURITY_EVENT: Tentativa de uso de token na blacklist: {}", tokenId);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Erro na validação do token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Incrementa tentativas de login
     */
    private void incrementarTentativasLogin(Usuario usuario) {
        try {
            int tentativas = usuario.getTentativasLogin() != null ? usuario.getTentativasLogin() : 0;
            tentativas++;
            usuario.setTentativasLogin(tentativas);

            if (tentativas >= 5) {
                usuario.setBloqueado(true);
                usuario.setDataBloqueio(LocalDateTime.now());
                log.warn("SECURITY_EVENT: Usuário bloqueado por excesso de tentativas: {}", usuario.getEmail());
            }

            usuarioRepository.save(usuario);
        } catch (Exception e) {
            log.error("Erro ao incrementar tentativas de login: {}", e.getMessage());
        }
    }

    /**
     * Reseta tentativas de login
     */
    private void resetarTentativasLogin(Usuario usuario) {
        try {
            usuario.setTentativasLogin(0);
            usuario.setUltimoAcesso(LocalDateTime.now());
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            log.error("Erro ao resetar tentativas de login: {}", e.getMessage());
        }
    }
}