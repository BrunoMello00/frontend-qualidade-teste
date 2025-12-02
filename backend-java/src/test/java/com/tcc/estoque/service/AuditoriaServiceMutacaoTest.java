package com.tcc.estoque.service;

import com.tcc.estoque.model.Auditoria;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.TipoUsuario;
import com.tcc.estoque.repository.AuditoriaRepository;
import com.tcc.estoque.security.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes de Mutação para AuditoriaService
 * Focado em alcançar 80% de score de mutação
 * Testa cenários edge cases e mutantes específicos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaService - Testes de Mutação")
class AuditoriaServiceMutacaoTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @InjectMocks
    private AuditoriaService auditoriaService;

    private Usuario usuarioMock;

    @BeforeEach
    void setup() {
        usuarioMock = Usuario.builder()
                .id(1L)
                .nome("Usuario Teste")
                .email("teste@email.com")
                .tipoUsuario(TipoUsuario.ADMIN)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("Mata mutante: deve falhar quando dados são null em registrarInsercao")
    void mataMutanteDeveFalharQuandoDadosSaoNullEmRegistrarInsercao() throws Exception {
        
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(null)).thenReturn(null);
        
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        auditoriaService.registrarInsercao("tabela", 1L, null);

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getDadosNovos() == null
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve processar X-Forwarded-For vazio corretamente")
    void mataMutanteDeveProcessarXForwardedForVazioCorretamente() throws Exception {
        
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(""); 
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn("10.0.0.1");

        auditoriaService.registrarInsercao("test", 1L, Map.of());

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getIpAddress().equals("10.0.0.1")
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve processar X-Real-IP vazio corretamente")
    void mataMutanteDeveProcessarXRealIpVazioCorretamente() throws Exception {
        
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(""); 
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        auditoriaService.registrarInsercao("test", 1L, Map.of());

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getIpAddress().equals("192.168.1.1")
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve processar múltiplos IPs em X-Forwarded-For")
    void mataMutanteDeveProcessarMultiplosIpsEmXForwardedFor() throws Exception {
        
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.1  ,  192.168.1.1  ,  10.0.0.1  ");

        auditoriaService.registrarInsercao("test", 1L, Map.of());

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getIpAddress().equals("203.0.113.1")
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve usar data correta para limpeza de auditoria")
    void mataMutanteDeveUsarDataCorretaParaLimpezaAuditoria() {
        
        int dias = 30;
        LocalDateTime dataAntes = LocalDateTime.now();

        auditoriaService.limparAuditoriaAntiga(dias);

        verify(auditoriaRepository).deleteByTimestampOperacaoBefore(argThat(data -> 
            data.isBefore(dataAntes) 
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve usar pageSize correto ao buscar últimas atividades")
    void mataMutanteDeveUsarPageSizeCorretoAoBuscarUltimasAtividades() {
        
        int limit = 10;
        when(auditoriaRepository.ultimasAtividades(eq(usuarioMock), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        auditoriaService.buscarUltimasAtividades(usuarioMock, limit);

        verify(auditoriaRepository).ultimasAtividades(eq(usuarioMock), argThat(pageable -> 
            pageable.getPageSize() == limit && 
            pageable.getPageNumber() == 0      
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve lidar com string null em converterParaJson")
    void mataMutanteDeveLidarComStringNullEmConverterParaJson() throws Exception {
        
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        
        when(objectMapper.writeValueAsString(null)).thenReturn(null);
        
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        auditoriaService.registrarInsercao("test", 1L, null);

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getDadosNovos() == null 
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve usar mensagem específica para erro de serialização")
    void mataMutanteDeveUsarMensagemEspecificaParaErroSerializacao() throws Exception {
        
        String mensagemEsperada = "Erro específico de teste";
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException(mensagemEsperada));
        
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        auditoriaService.registrarInsercao("test", 1L, Map.of("key", "value"));

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getDadosNovos().contains(mensagemEsperada) &&
            auditoria.getDadosNovos().startsWith("Erro ao serializar:")
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve processar primeiro elemento de array IP corretamente")
    void mataMutanteDeveProcessarPrimeiroElementoArrayIpCorretamente() throws Exception {
        
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1");

        auditoriaService.registrarInsercao("test", 1L, Map.of());

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getIpAddress().equals("198.51.100.1")
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve validar corretamente estatísticas com listas vazias")
    void mataMutanteDeveValidarCorretamenteEstatisticasComListasVazias() {
        
        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fim = LocalDateTime.now();
        
        when(auditoriaRepository.contarPorPeriodo(inicio, fim)).thenReturn(0L);
        when(auditoriaRepository.contarPorTabela()).thenReturn(Collections.emptyList());
        when(auditoriaRepository.contarPorOperacao()).thenReturn(Collections.emptyList());
        when(auditoriaRepository.contarPorUsuario()).thenReturn(Collections.emptyList());
        when(auditoriaRepository.ipsAtivos(any(Pageable.class))).thenReturn(Collections.emptyList());

        Map<String, Object> resultado = auditoriaService.gerarEstatisticas(inicio, fim);

        assertAll("Validação com listas vazias",
                () -> assertThat(resultado.get("totalOperacoes")).isEqualTo(0L),
                () -> assertThat((Map<?, ?>) resultado.get("operacoesPorTabela")).isEmpty(),
                () -> assertThat((Map<?, ?>) resultado.get("operacoesPorTipo")).isEmpty(),
                () -> assertThat((Map<?, ?>) resultado.get("operacoesPorUsuario")).isEmpty(),
                () -> assertThat((Map<?, ?>) resultado.get("ipsAtivos")).isEmpty()
        );
    }

    @Test
    @DisplayName("Mata mutante: deve validar operação enum corretamente em filtros")
    void mataMutanteDeveValidarOperacaoEnumCorretamenteEmFiltros() {
        
        when(auditoriaRepository.findComFiltros(anyString(), isNull(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        for (Auditoria.OperacaoAuditoria op : Auditoria.OperacaoAuditoria.values()) {
            auditoriaService.buscarComFiltros("test", null, op.name(), 
                    LocalDateTime.now().minusDays(1), LocalDateTime.now(), 0, 10);
            
            verify(auditoriaRepository).findComFiltros(eq("test"), isNull(), eq(op), any(), any(), any());
            reset(auditoriaRepository); 
            when(auditoriaRepository.findComFiltros(anyString(), isNull(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
        }
    }

    @Test
    @DisplayName("Mata mutante: deve validar case-insensitive para operação enum")
    void mataMutanteDeveValidarCaseInsensitiveParaOperacaoEnum() {
        
        when(auditoriaRepository.findComFiltros(anyString(), isNull(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        auditoriaService.buscarComFiltros("test", null, "insert", 
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), 0, 10);

        verify(auditoriaRepository).findComFiltros(eq("test"), isNull(), 
                eq(Auditoria.OperacaoAuditoria.INSERT), any(), any(), any());
    }

    @Test
    @DisplayName("Mata mutante: deve capturar exceção específica em getClientIp")
    void mataMutanteDeveCapturaraExcecaoEspecificaEmGetClientIp() throws Exception {
        
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        RequestContextHolder.setRequestAttributes(null);

        auditoriaService.registrarInsercao("test", 1L, Map.of());

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getIpAddress().equals("unknown")
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve capturar exceção específica em getUserAgent")
    void mataMutanteDeveCapturaraExcecaoEspecificaEmGetUserAgent() throws Exception {
        
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            Map<String, Object> dados = invocation.getArgument(0);
            
            return dados.containsKey("userAgent") ? 
                    String.format("{\"userAgent\":\"%s\"}", dados.get("userAgent")) : "{}";
        });

        RequestContextHolder.setRequestAttributes(null);

        auditoriaService.registrarLogin(usuarioMock, true);

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getDadosNovos().contains("\"userAgent\":\"unknown\"")
        ));
    }

    @Test
    @DisplayName("Mata mutante: deve processar corretamente loop em estatísticas por tabela")
    void mataMutanteDeveProcessarCorretamenteLoopEmEstatisticasPorTabela() {
        
        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fim = LocalDateTime.now();
        
        when(auditoriaRepository.contarPorPeriodo(inicio, fim)).thenReturn(100L);

        List<Object[]> dadosTabela = Arrays.asList(
                new Object[]{"tabela1", 10L},
                new Object[]{"tabela2", 20L},
                new Object[]{"tabela3", 30L}
        );
        when(auditoriaRepository.contarPorTabela()).thenReturn(dadosTabela);

        when(auditoriaRepository.contarPorOperacao()).thenReturn(Collections.emptyList());
        when(auditoriaRepository.contarPorUsuario()).thenReturn(Collections.emptyList());
        when(auditoriaRepository.ipsAtivos(any(Pageable.class))).thenReturn(Collections.emptyList());

        Map<String, Object> resultado = auditoriaService.gerarEstatisticas(inicio, fim);

        @SuppressWarnings("unchecked")
        Map<String, Long> operacoesPorTabela = (Map<String, Long>) resultado.get("operacoesPorTabela");
        
        assertAll("Validação do processamento do loop",
                () -> assertThat(operacoesPorTabela).hasSize(3),
                () -> assertThat(operacoesPorTabela).containsEntry("tabela1", 10L),
                () -> assertThat(operacoesPorTabela).containsEntry("tabela2", 20L),
                () -> assertThat(operacoesPorTabela).containsEntry("tabela3", 30L)
        );
    }

    @Test
    @DisplayName("Mata mutante: deve processar IP null no loop de IPs ativos")
    void mataMutanteDeveProcessarIpNullNoLoopIpsAtivos() {
        
        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fim = LocalDateTime.now();
        
        when(auditoriaRepository.contarPorPeriodo(inicio, fim)).thenReturn(50L);
        when(auditoriaRepository.contarPorTabela()).thenReturn(Collections.emptyList());
        when(auditoriaRepository.contarPorOperacao()).thenReturn(Collections.emptyList());
        when(auditoriaRepository.contarPorUsuario()).thenReturn(Collections.emptyList());

        List<Object[]> ipsAtivos = Arrays.asList(
                new Object[]{"192.168.1.1", 25L},
                new Object[]{null, 15L}, 
                new Object[]{"10.0.0.1", 10L}
        );
        when(auditoriaRepository.ipsAtivos(any(Pageable.class))).thenReturn(ipsAtivos);

        Map<String, Object> resultado = auditoriaService.gerarEstatisticas(inicio, fim);

        @SuppressWarnings("unchecked")
        Map<String, Long> topIps = (Map<String, Long>) resultado.get("ipsAtivos");
        
        assertAll("Validação do processamento de IPs no loop",
                () -> assertThat(topIps).hasSize(2), 
                () -> assertThat(topIps).containsEntry("192.168.1.1", 25L),
                () -> assertThat(topIps).containsEntry("10.0.0.1", 10L),
                () -> assertThat(topIps).doesNotContainKey(null)
        );
    }

    @Test
    @DisplayName("Mata mutante: deve retornar exatamente o valor esperado para Page vazia")
    void mataMutanteDeveRetornarExatamenteValorEsperadoParaPageVazia() {
        
        when(securityUtil.getUsuarioLogado()).thenThrow(new RuntimeException("Erro"));

        Page<Auditoria> resultado = auditoriaService.buscarPorUsuario(1L, 0, 10);

        assertAll("Validação do valor de retorno específico",
                () -> assertThat(resultado).isNotNull(),
                () -> assertThat(resultado.isEmpty()).isTrue(),
                () -> assertThat(resultado.getTotalElements()).isEqualTo(0L),
                () -> assertThat(resultado.getContent()).isEmpty()
        );
    }

    @Test
    @DisplayName("Mata mutante: deve validar tipo específico de exceção capturada")
    void mataMutanteDeveValidarTipoEspecificoExcecaoCapturada() throws Exception {
        
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);

        when(objectMapper.writeValueAsString(any())).thenThrow(
                new com.fasterxml.jackson.core.JsonProcessingException("JSON error") {}
        );
        
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        auditoriaService.registrarInsercao("test", 1L, Map.of("key", "value"));

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getDadosNovos().contains("JSON error") &&
            auditoria.getDadosNovos().startsWith("Erro ao serializar:")
        ));
    }

    @Test 
    @DisplayName("Mata mutante: deve processar corretamente registros de auditoria complexos")
    void mataMutanteDeveProcessarCorretamenteRegistrosAuditoriaComplexos() throws Exception {
        
        Map<String, Object> dadosComplexos = Map.of(
                "usuario", Map.of("id", 1L, "nome", "Teste", "ativo", true),
                "produto", Map.of("id", 100L, "nome", "Produto Teste", "preco", 99.99),
                "itens", Arrays.asList(
                        Map.of("id", 1L, "quantidade", 2),
                        Map.of("id", 2L, "quantidade", 1)
                ),
                "metadados", Map.of("versao", "1.0", "timestamp", System.currentTimeMillis())
        );

        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(dadosComplexos)).thenReturn(
                "{\"usuario\":{\"id\":1,\"nome\":\"Teste\"},\"produto\":{\"id\":100,\"nome\":\"Produto Teste\"}}"
        );
        
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.100");

        auditoriaService.registrarInsercao("vendas", 12345L, dadosComplexos);

        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getTabela().equals("vendas") &&
            auditoria.getRegistroId().equals(12345L) &&
            auditoria.getDadosNovos().contains("\"usuario\":{\"id\":1") &&
            auditoria.getDadosNovos().contains("\"produto\":{\"id\":100") &&
            auditoria.getIpAddress().equals("192.168.1.100")
        ));
    }
}
