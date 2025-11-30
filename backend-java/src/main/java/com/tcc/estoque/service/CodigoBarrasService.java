package com.tcc.estoque.service;

import com.tcc.estoque.enums.TipoCodigoBarras;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciar códigos de barras e códigos sequenciais
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodigoBarrasService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Gerar próximo código personalizado baseado no tipo
     */
    @Transactional
    public String gerarProximoCodigo(TipoCodigoBarras tipo, String prefixo) {
        try {
            log.debug("Gerando próximo código - Tipo: {}, Prefixo: {}", tipo.getCodigo(), prefixo);

            if (!tipo.permiteGeracaoAutomatica()) {
                throw new IllegalArgumentException("Tipo de código não permite geração automática: " + tipo.getCodigo());
            }

            String codigoGerado = gerarCodigoComLogicaJava(tipo, prefixo);

            log.info("✅ Código gerado com sucesso: {} (Tipo: {})", codigoGerado, tipo.getCodigo());
            return codigoGerado;

        } catch (DataAccessException e) {
            log.error("❌ Erro ao gerar código - Tipo: {}, Prefixo: {}: {}", 
                      tipo.getCodigo(), prefixo, e.getMessage());
            throw new RuntimeException("Erro ao gerar código personalizado", e);
        }
    }

  /**
   * Gerar próximo código resumido sequencial com proteção contra concorrência
   */
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
  public synchronized String gerarProximoCodigoResumido() {
    try {
      String sql = "SELECT codigo_resumido FROM produtos WHERE codigo_resumido IS NOT NULL ORDER BY CAST(codigo_resumido AS INTEGER) DESC";
      
      List<String> codigos = jdbcTemplate.queryForList(sql, String.class);
      
      int proximoCodigo = 1;
      
      if (!codigos.isEmpty()) {
        Set<Integer> codigosUsados = codigos.stream()
            .filter(c -> c.matches("\\d+")) // apenas códigos numéricos
            .map(Integer::parseInt)
            .collect(Collectors.toSet());
        
        while (codigosUsados.contains(proximoCodigo)) {
          proximoCodigo++;
        }
      }
      
      String codigoGerado = String.format("%06d", proximoCodigo);
      
      if (existeCodigoResumido(codigoGerado)) {
        long timestamp = System.currentTimeMillis();
        do {
          codigoGerado = String.format("%06d", (timestamp % 999999) + proximoCodigo);
          timestamp++;
        } while (existeCodigoResumido(codigoGerado));
      }
      
      log.debug("✅ Código resumido gerado: {}", codigoGerado);
      return codigoGerado;

    } catch (Exception e) {
      log.error("❌ Erro ao gerar código resumido: {}", e.getMessage());
      long timestamp = System.currentTimeMillis();
      java.util.Random random = new java.util.Random();
      String codigoFallback;
      
      do {
        int suffix = random.nextInt(9999);
        codigoFallback = String.format("%06d", (timestamp % 99) * 100 + suffix);
      } while (existeCodigoResumido(codigoFallback));
      
      log.info("🔄 Usando código fallback: {}", codigoFallback);
      return codigoFallback;
    }
  }
  
    /**
     * Verificar se já existe produto com este código resumido
     */
    private boolean existeCodigoResumido(String codigoResumido) {
        try {
            String sql = "SELECT COUNT(*) FROM produtos WHERE codigo_resumido = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, codigoResumido);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Erro ao verificar existência do código resumido {}: {}", codigoResumido, e.getMessage());
            return false; // Em caso de erro, assumir que não existe
        }
    }

    /**
     * Validar código de barras
     */
    public boolean validarCodigoBarras(String codigo, TipoCodigoBarras tipo) {
        try {
            if (codigo == null || codigo.trim().isEmpty()) {
                return false;
            }

            boolean valido = tipo.validar(codigo);
            
            log.debug("Validação código: {} (Tipo: {}) = {}", codigo, tipo.getCodigo(), valido);
            return valido;

        } catch (Exception e) {
            log.warn("⚠️ Erro na validação do código {}: {}", codigo, e.getMessage());
            return false;
        }
    }

    /**
     * Detectar tipo de código automaticamente
     */
    public TipoCodigoBarras detectarTipoCodigo(String codigo) {
        TipoCodigoBarras tipoDetectado = TipoCodigoBarras.detectarTipo(codigo);
        log.debug("Tipo detectado para código '{}': {}", codigo, tipoDetectado.getCodigo());
        return tipoDetectado;
    }

    /**
     * Verificar se código já existe
     */
    public boolean codigoJaExiste(String codigo) {
        try {
            String sql = "SELECT COUNT(*) > 0 FROM produtos WHERE codigo = ? AND ativo = true";
            Boolean existe = jdbcTemplate.queryForObject(sql, Boolean.class, codigo);
            
            return Boolean.TRUE.equals(existe);

        } catch (DataAccessException e) {
            log.error("❌ Erro ao verificar existência do código {}: {}", codigo, e.getMessage());
            return false;
        }
    }

    /**
     * Buscar produto por código resumido
     */
    public Optional<Map<String, Object>> buscarPorCodigoResumido(String codigoResumido) {
        try {
            String sql = "SELECT " +
                         "id, nome, codigo, codigo_resumido, preco, estoque " +
                         "FROM produtos " +
                         "WHERE codigo_resumido = ? AND ativo = true";

            List<Map<String, Object>> resultado = jdbcTemplate.queryForList(sql, codigoResumido);
            
            if (!resultado.isEmpty()) {
                return Optional.of(resultado.get(0));
            }
            
            return Optional.empty();

        } catch (DataAccessException e) {
            log.error("❌ Erro ao buscar produto por código resumido {}: {}", codigoResumido, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Listar configurações de códigos disponíveis
     * Implementação robusta com fallback para compatibilidade H2
     */
    public List<Map<String, Object>> listarConfiguracoesCodigos() {
        try {
            String sql = "SELECT " +
                         "tipo_codigo, " +
                         "prefixo_padrao, " +
                         "formato_numeracao, " +
                         "proximo_numero, " +
                         "descricao, " +
                         "ativo " +
                         "FROM configuracoes_codigo_barras " +
                         "WHERE ativo = true " +
                         "ORDER BY tipo_codigo";

            return jdbcTemplate.queryForList(sql);

        } catch (DataAccessException e) {
            log.warn("⚠️ Tabela configuracoes_codigo_barras não disponível, retornando configurações padrão: {}", e.getMessage());
            
            return List.of(
                Map.of("tipo_codigo", "PERSONALIZADO", "prefixo_padrao", "PROD", 
                       "formato_numeracao", "PROD-{numero:6d}", "proximo_numero", 1L, 
                       "descricao", "Código personalizado da empresa", "ativo", true),
                Map.of("tipo_codigo", "EAN13", "prefixo_padrao", "789", 
                       "formato_numeracao", "789{numero:10d}", "proximo_numero", 1L, 
                       "descricao", "EAN-13 com prefixo brasileiro", "ativo", true),
                Map.of("tipo_codigo", "EAN8", "prefixo_padrao", "12", 
                       "formato_numeracao", "12{numero:6d}", "proximo_numero", 1L, 
                       "descricao", "EAN-8 simplificado", "ativo", true)
            );
        }
    }

    /**
     * Atualizar configuração de código
     * Implementação robusta com fallback para compatibilidade H2
     */
    @Transactional
    public boolean atualizarConfiguracaoCodigo(String tipoCodigo, String prefixoPadrao, 
                                              String formatoNumeracao, String descricao) {
        try {
            String sql = "UPDATE configuracoes_codigo_barras " +
                         "SET prefixo_padrao = ?, " +
                         "formato_numeracao = ?, " +
                         "descricao = ?, " +
                         "updated_at = CURRENT_TIMESTAMP " +
                         "WHERE tipo_codigo = ? AND ativo = true";

            int atualizado = jdbcTemplate.update(sql, prefixoPadrao, formatoNumeracao, 
                                                descricao, tipoCodigo);

            if (atualizado > 0) {
                log.info("✅ Configuração atualizada - Tipo: {}", tipoCodigo);
                return true;
            } else {
                log.warn("⚠️ Nenhuma configuração encontrada para tipo: {}", tipoCodigo);
                return false;
            }

        } catch (DataAccessException e) {
            log.warn("⚠️ Tabela configuracoes_codigo_barras não disponível para atualização - Tipo: {}: {}", 
                     tipoCodigo, e.getMessage());
            return true; // Retorna true para não bloquear operações
        }
    }

    /**
     * Obter próximo número sequencial para um tipo
     * Implementação independente de tabelas específicas para compatibilidade H2
     */
    public Long obterProximoNumero(String tipoCodigo, String prefixo) {
        try {
            String sql = "SELECT proximo_numero " +
                         "FROM configuracoes_codigo_barras " +
                         "WHERE tipo_codigo = ? " +
                         "AND (? IS NULL OR prefixo_padrao = ?) " +
                         "AND ativo = true";

            return jdbcTemplate.queryForObject(sql, Long.class, tipoCodigo, prefixo, prefixo);

        } catch (DataAccessException e) {
            log.warn("⚠️ Tabela configuracoes_codigo_barras não encontrada, usando geração baseada em timestamp - Tipo: {}, Prefixo: {}", 
                      tipoCodigo, prefixo);
            
            long timestamp = System.currentTimeMillis();
            int tipoHash = tipoCodigo != null ? Math.abs(tipoCodigo.hashCode()) % 100 : 0;
            int prefixoHash = prefixo != null ? Math.abs(prefixo.hashCode()) % 100 : 0;
            
            long numero = (timestamp % 900000) + tipoHash + prefixoHash + 100000; // Entre 100000 e 999999
            
            log.info("✅ Número gerado automaticamente: {} para tipo: {}, prefixo: {}", numero, tipoCodigo, prefixo);
            return numero;
        }
    }

    /**
     * Gerar código EAN-13 com dígito verificador
     */
    public String gerarEAN13(String prefixo) {
        try {
            long timestamp = System.currentTimeMillis();
            int sequencial = (int) (timestamp % 9999999999L); // 10 dígitos máximo
            
            String codigo12 = String.format("%s%010d", prefixo.substring(0, Math.min(2, prefixo.length())), sequencial);
            
            int digitoVerificador = calcularDigitoVerificadorEAN13(codigo12);
            
            String codigoCompleto = codigo12 + digitoVerificador;
            
            log.info("✅ EAN-13 gerado: {}", codigoCompleto);
            return codigoCompleto;

        } catch (Exception e) {
            log.error("❌ Erro ao gerar EAN-13 com prefixo {}: {}", prefixo, e.getMessage());
            throw new RuntimeException("Erro ao gerar código EAN-13", e);
        }
    }

    /**
     * Calcular dígito verificador para EAN-13
     */
    private int calcularDigitoVerificadorEAN13(String codigo12) {
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            int digito = Character.getNumericValue(codigo12.charAt(i));
            if (i % 2 == 0) {
                soma += digito;  // Posições ímpares (0, 2, 4, ...)
            } else {
                soma += digito * 3;  // Posições pares (1, 3, 5, ...)
            }
        }
        
        int resto = soma % 10;
        return resto == 0 ? 0 : 10 - resto;
    }

    /**
     * Validar dígito verificador EAN-13
     */
    public boolean validarDigitoVerificadorEAN13(String codigo) {
        if (codigo == null || codigo.length() != 13) {
            return false;
        }
        
        try {
            String codigo12 = codigo.substring(0, 12);
            int digitoInformado = Character.getNumericValue(codigo.charAt(12));
            int digitoCalculado = calcularDigitoVerificadorEAN13(codigo12);
            
            return digitoInformado == digitoCalculado;
        } catch (Exception e) {
            log.warn("⚠️ Erro na validação do dígito verificador para {}: {}", codigo, e.getMessage());
            return false;
        }
    }

    /**
     * Obter estatísticas de códigos por tipo
     */
    public List<Map<String, Object>> obterEstatisticasCodigos() {
        try {
            String sql = "SELECT " +
                         "tipo_codigo_barras, " +
                         "COUNT(*) as quantidade, " +
                         "MIN(data_cadastro) as primeiro_cadastro, " +
                         "MAX(data_cadastro) as ultimo_cadastro " +
                         "FROM produtos " +
                         "WHERE ativo = true " +
                         "GROUP BY tipo_codigo_barras " +
                         "ORDER BY quantidade DESC";

            return jdbcTemplate.queryForList(sql);

        } catch (DataAccessException e) {
            log.error("❌ Erro ao obter estatísticas de códigos: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Listar tipos de códigos suportados
     */
    public List<Map<String, Object>> listarTiposCodigoBarras() {
        return Arrays.stream(TipoCodigoBarras.values())
                .map(tipo -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("codigo", tipo.name());
                    map.put("descricao", tipo.toString());
                    map.put("brasileiro", tipo.isPadraoBrasileiro());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gerar código com lógica Java (sem dependência de função SQL)
     */
    private String gerarCodigoComLogicaJava(TipoCodigoBarras tipo, String prefixo) {
        long timestamp = System.currentTimeMillis();
        int sequencial = (int) (timestamp % 999999); // Últimos 6 dígitos
        
        switch (tipo) {
            case PERSONALIZADO:
                return String.format("%s-%06d", prefixo != null ? prefixo : "PROD", sequencial);
            case CODE39:
                return String.format("PROD-%06d", sequencial);
            case CODE128:
                return String.format("P%06d", sequencial);
            case EAN13:
                return gerarEAN13(prefixo != null ? prefixo : "02");
            case EAN8:
                return gerarEAN8(prefixo != null ? prefixo : "02");
            case UPC_A:
                return gerarUPCA(prefixo != null ? prefixo : "0");
            default:
                return String.format("%s%06d", prefixo != null ? prefixo : "999", sequencial);
        }
    }

    /**
     * Gerar código EAN-8 com dígito verificador
     */
    public String gerarEAN8(String prefixo) {
        try {
            long timestamp = System.currentTimeMillis();
            int sequencial = (int) (timestamp % 99999); // 5 dígitos
            
            String codigo7 = String.format("%s%05d", prefixo.substring(0, Math.min(2, prefixo.length())), sequencial);
            
            int digitoVerificador = calcularDigitoVerificadorEAN8(codigo7);
            
            String codigoCompleto = codigo7 + digitoVerificador;
            
            log.info("✅ EAN-8 gerado: {}", codigoCompleto);
            return codigoCompleto;

        } catch (Exception e) {
            log.error("❌ Erro ao gerar EAN-8 com prefixo {}: {}", prefixo, e.getMessage());
            throw new RuntimeException("Erro ao gerar código EAN-8", e);
        }
    }

    /**
     * Gerar código UPC-A 
     */
    public String gerarUPCA(String prefixo) {
        try {
            long timestamp = System.currentTimeMillis();
            int sequencial = (int) (timestamp % 999999999L); // 9 dígitos
            
            String codigo11 = String.format("%s%010d", prefixo.substring(0, Math.min(1, prefixo.length())), sequencial);
            
            int digitoVerificador = calcularDigitoVerificadorEAN13(codigo11 + "0");
            
            String codigoCompleto = codigo11 + digitoVerificador;
            
            log.info("✅ UPC-A gerado: {}", codigoCompleto);
            return codigoCompleto;

        } catch (Exception e) {
            log.error("❌ Erro ao gerar UPC-A com prefixo {}: {}", prefixo, e.getMessage());
            throw new RuntimeException("Erro ao gerar código UPC-A", e);
        }
    }

    /**
     * Calcular dígito verificador para EAN-8
     */
    private int calcularDigitoVerificadorEAN8(String codigo7) {
        int soma = 0;
        for (int i = 0; i < 7; i++) {
            int digito = Character.getNumericValue(codigo7.charAt(i));
            if (i % 2 == 0) {
                soma += digito * 3;  // Posições ímpares (0, 2, 4, 6)
            } else {
                soma += digito;      // Posições pares (1, 3, 5)
            }
        }
        
        int resto = soma % 10;
        return resto == 0 ? 0 : 10 - resto;
    }
}
