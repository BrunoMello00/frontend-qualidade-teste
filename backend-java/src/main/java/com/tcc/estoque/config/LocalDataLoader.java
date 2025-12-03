package com.tcc.estoque.config;

import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.Usuario.Role;
import com.tcc.estoque.model.enums.TipoUsuario;
import com.tcc.estoque.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Carregador de dados mínimos para desenvolvimento e testes
 * 
 * Este componente executa nos profiles 'dev', 'local' e 'test-azure' e carrega dados
 * mínimos do arquivo data-local.sql que NÃO é versionado no Git.
 * 
 * IMPORTANTE: Apenas dados de teste - nunca usar em produção!
 */
@Slf4j
@Component
@Profile({"dev", "local", "test-azure"}) // Executa no ambiente de desenvolvimento, local e test-azure
public class LocalDataLoader implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("🔒 Verificando se existem dados mínimos para carregar (dev/local only)...");
            
            criarUsuariosTeste();
            
            carregarDadosArquivo();
            
        } catch (Exception e) {
            log.error("❌ Erro ao carregar dados mínimos locais: {}", e.getMessage());
        }
    }
    
    private void criarUsuariosTeste() {
        try {
            if (usuarioRepository.count() > 0) {
                log.info("👤 Usuários já existem no banco - pulando criação de usuários teste");
                return;
            }
            
            log.info("👤 Criando usuários de teste...");
            
            if (!usuarioRepository.existsByEmail("owner@sistema.com")) {
                Usuario owner = Usuario.builder()
                    .nome("Owner Sistema")
                    .email("owner@sistema.com")
                    .senha(passwordEncoder.encode("password"))
                    .tipoUsuario(TipoUsuario.ADMIN)
                    .role(Role.ADMIN)
                    .ativo(true)
                    .dataCadastro(LocalDateTime.now())
                    .tentativasLogin(0)
                    .bloqueado(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                usuarioRepository.save(owner);
                log.info("✅ Usuário owner criado: owner@sistema.com / password");
            }
            
            if (!usuarioRepository.existsByEmail("admin@sistema.com")) {
                Usuario admin = Usuario.builder()
                    .nome("Admin Sistema")
                    .email("admin@sistema.com")
                    .senha(passwordEncoder.encode("password"))
                    .tipoUsuario(TipoUsuario.ADMIN)
                    .role(Role.ADMIN)
                    .ativo(true)
                    .dataCadastro(LocalDateTime.now())
                    .tentativasLogin(0)
                    .bloqueado(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                usuarioRepository.save(admin);
                log.info("✅ Usuário admin criado: admin@sistema.com / password");
            }
            
            if (!usuarioRepository.existsByEmail("vendedor@sistema.com")) {
                Usuario vendedor = Usuario.builder()
                    .nome("Vendedor Sistema")
                    .email("vendedor@sistema.com")
                    .senha(passwordEncoder.encode("password"))
                    .tipoUsuario(TipoUsuario.VENDEDOR)
                    .role(Role.USER)
                    .ativo(true)
                    .dataCadastro(LocalDateTime.now())
                    .tentativasLogin(0)
                    .bloqueado(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                usuarioRepository.save(vendedor);
                log.info("✅ Usuário vendedor criado: vendedor@sistema.com / password");
            }
            
            log.info("🎯 Credenciais para teste:");
            log.info("   📧 Owner: owner@sistema.com     | Senha: password");
            log.info("   📧 Admin: admin@sistema.com     | Senha: password");  
            log.info("   👤 Vendedor: vendedor@sistema.com | Senha: password");
            
        } catch (Exception e) {
            log.error("❌ Erro ao criar usuários de teste: {}", e.getMessage());
        }
    }
    
    private void carregarDadosArquivo() {
        try {
            ClassPathResource resource = new ClassPathResource("db/data-local.sql");
            
            if (resource.exists()) {
                log.info("📝 Carregando dados adicionais do arquivo data-local.sql...");
                
                String sql = FileCopyUtils.copyToString(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
                );
                
                String[] sqlStatements = sql.split(";");
                
                for (String statement : sqlStatements) {
                    statement = statement.trim();
                    if (!statement.isEmpty() && 
                        !statement.startsWith("--") && 
                        !statement.toLowerCase().contains("insert into usuarios")) {
                        try {
                            jdbcTemplate.execute(statement);
                        } catch (Exception e) {
                            if (!e.getMessage().contains("duplicate") && 
                                !e.getMessage().contains("exists") &&
                                !e.getMessage().contains("unique")) {
                                log.warn("⚠️ Erro ao executar SQL: {}", statement);
                            }
                        }
                    }
                }
                
                log.info("✅ Dados adicionais carregados com sucesso!");
                
            } else {
                log.info("ℹ️ Arquivo data-local.sql não encontrado. Usando apenas usuários básicos.");
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao carregar dados do arquivo: {}", e.getMessage());
        }
    }
}
