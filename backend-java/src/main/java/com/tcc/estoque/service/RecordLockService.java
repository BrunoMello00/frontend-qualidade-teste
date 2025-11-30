package com.tcc.estoque.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordLockService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public boolean adquirirLock(String tabela, Long registroId, Long usuarioId, 
                               String tipoLock, Integer tempoExpiracaoMinutos, 
                               String ipAddress, String userAgent) {
        try {
            limparLocksExpirados();
            
            String checkSql = "SELECT COUNT(*) FROM locks_registro WHERE tabela = ? AND registro_id = ? AND ativo = true AND data_expiracao > CURRENT_TIMESTAMP";
            Integer locksAtivos = jdbcTemplate.queryForObject(checkSql, Integer.class, tabela, registroId);
            
            if (locksAtivos > 0) {
                String checkUserSql = "SELECT COUNT(*) FROM locks_registro WHERE tabela = ? AND registro_id = ? AND usuario_id = ? AND ativo = true AND data_expiracao > CURRENT_TIMESTAMP";
                Integer lockUsuario = jdbcTemplate.queryForObject(checkUserSql, Integer.class, tabela, registroId, usuarioId);
                
                if (lockUsuario > 0) {
                    String renewSql = "UPDATE locks_registro SET data_expiracao = DATEADD('MINUTE', ?, CURRENT_TIMESTAMP), ip_address = ?, user_agent = ? WHERE tabela = ? AND registro_id = ? AND usuario_id = ? AND ativo = true";
                    jdbcTemplate.update(renewSql, tempoExpiracaoMinutos, ipAddress, userAgent, tabela, registroId, usuarioId);
                    log.info("Lock renovado - Tabela: {}, Registro: {}, Usuario: {}", tabela, registroId, usuarioId);
                    return true;
                } else {
                    log.warn("Registro ja bloqueado por outro usuario - Tabela: {}, Registro: {}", tabela, registroId);
                    return false;
                }
            }
            
            String insertSql = "INSERT INTO locks_registro (tabela, registro_id, usuario_id, tipo_lock, data_expiracao, ativo, ip_address, user_agent, created_at) VALUES (?, ?, ?, ?, DATEADD('MINUTE', ?, CURRENT_TIMESTAMP), true, ?, ?, CURRENT_TIMESTAMP)";
            int rowsAffected = jdbcTemplate.update(insertSql, tabela, registroId, usuarioId, tipoLock, tempoExpiracaoMinutos, ipAddress, userAgent);
            
            if (rowsAffected > 0) {
                log.info("Lock adquirido - Tabela: {}, Registro: {}, Usuario: {}", tabela, registroId, usuarioId);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("Erro ao adquirir lock - Tabela: {}, Registro: {}: {}", tabela, registroId, e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean liberarLock(String tabela, Long registroId, Long usuarioId) {
        try {
            String sql = "UPDATE locks_registro SET ativo = false WHERE tabela = ? AND registro_id = ? AND usuario_id = ? AND ativo = true";
            int rowsAffected = jdbcTemplate.update(sql, tabela, registroId, usuarioId);
            
            if (rowsAffected > 0) {
                log.info("Lock liberado - Tabela: {}, Registro: {}, Usuario: {}", tabela, registroId, usuarioId);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("Erro ao liberar lock - Tabela: {}, Registro: {}: {}", tabela, registroId, e.getMessage());
            return false;
        }
    }

    public boolean isRegistroBloqueado(String tabela, Long registroId) {
        try {
            String sql = "SELECT COUNT(*) FROM locks_registro WHERE tabela = ? AND registro_id = ? AND ativo = true AND data_expiracao > CURRENT_TIMESTAMP";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tabela, registroId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Erro ao verificar lock - Tabela: {}, Registro: {}: {}", tabela, registroId, e.getMessage());
            return false;
        }
    }

    public boolean usuarioTemLock(String tabela, Long registroId, Long usuarioId) {
        try {
            String sql = "SELECT COUNT(*) FROM locks_registro WHERE tabela = ? AND registro_id = ? AND usuario_id = ? AND ativo = true AND data_expiracao > CURRENT_TIMESTAMP";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tabela, registroId, usuarioId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Erro ao verificar lock do usuario: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public void limparLocksExpirados() {
        try {
            String sql = "UPDATE locks_registro SET ativo = false WHERE ativo = true AND data_expiracao <= CURRENT_TIMESTAMP";
            int removidos = jdbcTemplate.update(sql);
            if (removidos > 0) {
                log.info("Locks expirados removidos: {}", removidos);
            }
        } catch (Exception e) {
            log.error("Erro ao limpar locks expirados: {}", e.getMessage());
        }
    }

    public boolean adquirirLockProduto(Long produtoId, Long usuarioId, String ipAddress, String userAgent) {
        return adquirirLock("produtos", produtoId, usuarioId, "write", 30, ipAddress, userAgent);
    }

    public boolean liberarLockProduto(Long produtoId, Long usuarioId) {
        return liberarLock("produtos", produtoId, usuarioId);
    }

    public boolean isProdutoBloqueado(Long produtoId) {
        return isRegistroBloqueado("produtos", produtoId);
    }

    public boolean usuarioTemLockProduto(Long produtoId, Long usuarioId) {
        return usuarioTemLock("produtos", produtoId, usuarioId);
    }

    public Map<String, Object> obterInfoLockProduto(Long produtoId) {
        try {
            String sql = "SELECT * " +
                        "FROM locks_registro " +
                        "WHERE tabela = ? AND registro_id = ? AND ativo = true " +
                        "AND data_expiracao > CURRENT_TIMESTAMP " +
                        "ORDER BY created_at DESC " +
                        "LIMIT 1";

            List<Map<String, Object>> resultado = jdbcTemplate.queryForList(sql, "produtos", produtoId);
            
            if (!resultado.isEmpty()) {
                Map<String, Object> lockInfo = resultado.get(0);
                
                try {
                    String userSql = "SELECT nome FROM usuarios WHERE id = ?";
                    String userName = jdbcTemplate.queryForObject(userSql, String.class, lockInfo.get("usuario_id"));
                    lockInfo.put("usuario_nome", userName);
                } catch (Exception e) {
                    log.warn("Não foi possível obter nome do usuário: {}", e.getMessage());
                    lockInfo.put("usuario_nome", "Usuário não encontrado");
                }
                
                return lockInfo;
            }

        } catch (Exception e) {
            log.error("Erro ao obter informações do lock - Produto: {}: {}", produtoId, e.getMessage());
        }
        
        return null;
    }
}
