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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {
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
    private Auditoria auditoriaMock;
    private LocalDateTime timestampTest;
    @BeforeEach
    void setup() {
        timestampTest = LocalDateTime.of(2024, 12, 1, 10, 0, 0);
        usuarioMock = Usuario.builder()
                .id(1L)
                .nome("Usuario Teste")
                .email("teste@email.com")
                .tipoUsuario(TipoUsuario.ADMIN)
                .ativo(true)
                .build();
        auditoriaMock = Auditoria.builder()
                .id(1L)
                .tabela("usuarios")
                .registroId(1L)
                .operacao(Auditoria.OperacaoAuditoria.INSERT)
                .dadosNovos("{\"nome\":\"Teste\"}")
                .usuario(usuarioMock)
                .ipAddress("192.168.1.1")
                .timestampOperacao(timestampTest)
                .build();
    }

    @Test
    void deveRegistrarInsercaoComDadosValidos() throws Exception {
        String tabelaTeste = "produtos";
        Long registroId = 123L;
        Map<String, Object> dadosNovos = Map.of("nome", "Produto Teste", "preco", 99.99);
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(dadosNovos)).thenReturn("{\"nome\":\"Produto Teste\",\"preco\":99.99}");
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.100");
        when(auditoriaRepository.save(any(Auditoria.class))).thenReturn(auditoriaMock);
        auditoriaService.registrarInsercao(tabelaTeste, registroId, dadosNovos);
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getTabela().equals(tabelaTeste) &&
            auditoria.getRegistroId().equals(registroId) &&
            auditoria.getOperacao() == Auditoria.OperacaoAuditoria.INSERT &&
            auditoria.getUsuario().equals(usuarioMock) &&
            auditoria.getDadosNovos().equals("{\"nome\":\"Produto Teste\",\"preco\":99.99}")
        ));
    }
    @Test
    void deveRegistrarAtualizacaoComDadosAnteriorEsNovos() throws Exception {
        String tabela = "clientes";
        Long registroId = 456L;
        Map<String, Object> dadosAnteriores = Map.of("nome", "Cliente Antigo");
        Map<String, Object> dadosNovos = Map.of("nome", "Cliente Novo");
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(dadosAnteriores)).thenReturn("{\"nome\":\"Cliente Antigo\"}");
        when(objectMapper.writeValueAsString(dadosNovos)).thenReturn("{\"nome\":\"Cliente Novo\"}");
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        auditoriaService.registrarAtualizacao(tabela, registroId, dadosAnteriores, dadosNovos);
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getOperacao() == Auditoria.OperacaoAuditoria.UPDATE &&
            auditoria.getDadosAnteriores().equals("{\"nome\":\"Cliente Antigo\"}") &&
            auditoria.getDadosNovos().equals("{\"nome\":\"Cliente Novo\"}")
        ));
    }
    @Test
    void deveRegistrarExclusaoComDadosAnteriores() throws Exception {
        String tabela = "vendas";
        Long registroId = 789L;
        Map<String, Object> dadosAnteriores = Map.of("total", 150.00, "status", "CONFIRMADA");
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(dadosAnteriores)).thenReturn("{\"total\":150.00,\"status\":\"CONFIRMADA\"}");
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("172.16.0.1");
        auditoriaService.registrarExclusao(tabela, registroId, dadosAnteriores);
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getOperacao() == Auditoria.OperacaoAuditoria.DELETE &&
            auditoria.getDadosAnteriores().equals("{\"total\":150.00,\"status\":\"CONFIRMADA\"}") &&
            auditoria.getDadosNovos() == null
        ));
    }
    @Test
    void deveCapturaraIpDeXForwardedForCorretamente() throws Exception {
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1");
        auditoriaService.registrarInsercao("test_table", 1L, Map.of());
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getIpAddress().equals("203.0.113.1")
        ));
    }
    @Test
    void deveCapturaraIpDeXRealIpQuandoXForwardedForNaoExiste() throws Exception {
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn("198.51.100.1");
        auditoriaService.registrarInsercao("test_table", 1L, Map.of());
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getIpAddress().equals("198.51.100.1")
        ));
    }
    @Test
    void deveUsarRemoteAddrQuandoHeadersProxyNaoExistem() throws Exception {
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        auditoriaService.registrarInsercao("test_table", 1L, Map.of());
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getIpAddress().equals("127.0.0.1")
        ));
    }
    @Test
    void deveUsarUnknownComoIpQuandoOcorreExcecao() throws Exception {
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        RequestContextHolder.setRequestAttributes(null); // Simula contexto inválido
        auditoriaService.registrarInsercao("test_table", 1L, Map.of());
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getIpAddress().equals("unknown")
        ));
    }
    @Test
    void deveLidarComErroSerializacaoJsonGraciosamente() throws Exception {
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Erro JSON"));
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        auditoriaService.registrarInsercao("test_table", 1L, Map.of("campo", "valor"));
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getDadosNovos().startsWith("Erro ao serializar:")
        ));
    }

    @Test
    void deveBuscarAuditoriaComFiltrosValidos() {
        String tabela = "usuarios";
        Long usuarioId = 1L;
        String operacao = "INSERT";
        LocalDateTime inicio = LocalDateTime.now().minusDays(7);
        LocalDateTime fim = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Auditoria> pageExpected = new PageImpl<>(List.of(auditoriaMock));
        when(auditoriaRepository.findComFiltros(eq(tabela), any(Usuario.class), 
                eq(Auditoria.OperacaoAuditoria.INSERT), eq(inicio), eq(fim), eq(pageable)))
                .thenReturn(pageExpected);
        Page<Auditoria> resultado = auditoriaService.buscarComFiltros(tabela, usuarioId, operacao, inicio, fim, 0, 10);
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0)).isEqualTo(auditoriaMock);
    }
    @Test
    void deveBuscarPorTabelaEspecifica() {
        String tabela = "produtos";
        Pageable pageable = PageRequest.of(0, 20);
        Page<Auditoria> pageExpected = new PageImpl<>(Arrays.asList(auditoriaMock));
        when(auditoriaRepository.findByTabelaOrderByTimestampOperacaoDesc(tabela, pageable))
                .thenReturn(pageExpected);
        Page<Auditoria> resultado = auditoriaService.buscarPorTabela(tabela, 0, 20);
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        verify(auditoriaRepository).findByTabelaOrderByTimestampOperacaoDesc(tabela, pageable);
    }
    @Test
    void deveBuscarHistoricoRegistroEspecifico() {
        String tabela = "clientes";
        Long registroId = 123L;
        List<Auditoria> historicoExpected = Arrays.asList(auditoriaMock);
        when(auditoriaRepository.findByTabelaAndRegistroIdOrderByTimestampOperacaoDesc(tabela, registroId))
                .thenReturn(historicoExpected);
        List<Auditoria> resultado = auditoriaService.buscarHistoricoRegistro(tabela, registroId);
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0)).isEqualTo(auditoriaMock);
    }
    @Test
    void deveBuscarPorPeriodoEspecifico() {
        LocalDateTime inicio = LocalDateTime.now().minusDays(30);
        LocalDateTime fim = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 50);
        Page<Auditoria> pageExpected = new PageImpl<>(Arrays.asList(auditoriaMock));
        when(auditoriaRepository.findByPeriodo(inicio, fim, pageable))
                .thenReturn(pageExpected);
        Page<Auditoria> resultado = auditoriaService.buscarPorPeriodo(inicio, fim, 0, 50);
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        verify(auditoriaRepository).findByPeriodo(inicio, fim, pageable);
    }
    @Test
    void deveBuscarPorIpEspecifico() {
        String ip = "192.168.1.100";
        Pageable pageable = PageRequest.of(0, 25);
        Page<Auditoria> pageExpected = new PageImpl<>(Arrays.asList(auditoriaMock));
        when(auditoriaRepository.findByIpAddressOrderByTimestampOperacaoDesc(ip, pageable))
                .thenReturn(pageExpected);
        Page<Auditoria> resultado = auditoriaService.buscarPorIp(ip, 0, 25);
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        verify(auditoriaRepository).findByIpAddressOrderByTimestampOperacaoDesc(ip, pageable);
    }

    @Test
    void deveGerarEstatisticasAuditoriaCompletas() {
        LocalDateTime inicio = LocalDateTime.now().minusMonths(1);
        LocalDateTime fim = LocalDateTime.now();
        when(auditoriaRepository.contarPorPeriodo(inicio, fim)).thenReturn(100L);
        when(auditoriaRepository.contarPorTabela()).thenReturn(Arrays.asList(
                new Object[]{"usuarios", 30L},
                new Object[]{"produtos", 45L},
                new Object[]{"vendas", 25L}
        ));
        when(auditoriaRepository.contarPorOperacao()).thenReturn(Arrays.asList(
                new Object[]{"INSERT", 40L},
                new Object[]{"UPDATE", 35L},
                new Object[]{"DELETE", 25L}
        ));
        when(auditoriaRepository.contarPorUsuario()).thenReturn(Arrays.asList(
                new Object[]{"admin@test.com", 60L},
                new Object[]{"user@test.com", 40L}
        ));
        when(auditoriaRepository.ipsAtivos(any(Pageable.class))).thenReturn(Arrays.asList(
                new Object[]{"192.168.1.1", 50L},
                new Object[]{"10.0.0.1", 30L},
                new Object[]{null, 20L} // IP nulo para testar edge case
        ));
        Map<String, Object> resultado = auditoriaService.gerarEstatisticas(inicio, fim);
        assertAll("Validação das estatísticas geradas",
                () -> assertThat(resultado).containsKey("totalOperacoes"),
                () -> assertThat(resultado.get("totalOperacoes")).isEqualTo(100L),
                () -> assertThat(resultado).containsKey("operacoesPorTabela"),
                () -> assertThat((Map<String, Long>) resultado.get("operacoesPorTabela"))
                        .containsEntry("usuarios", 30L)
                        .containsEntry("produtos", 45L)
                        .containsEntry("vendas", 25L),
                () -> assertThat(resultado).containsKey("operacoesPorTipo"),
                () -> assertThat((Map<String, Long>) resultado.get("operacoesPorTipo"))
                        .containsEntry("INSERT", 40L)
                        .containsEntry("UPDATE", 35L)
                        .containsEntry("DELETE", 25L),
                () -> assertThat(resultado).containsKey("operacoesPorUsuario"),
                () -> assertThat((Map<String, Long>) resultado.get("operacoesPorUsuario"))
                        .containsEntry("admin@test.com", 60L)
                        .containsEntry("user@test.com", 40L),
                () -> assertThat(resultado).containsKey("ipsAtivos"),
                () -> assertThat((Map<String, Long>) resultado.get("ipsAtivos"))
                        .containsEntry("192.168.1.1", 50L)
                        .containsEntry("10.0.0.1", 30L)
                        .doesNotContainKey(null) // IP nulo não deve aparecer
        );
    }

    @Test
    void deveRegistrarAcessoRecursoSensivel() throws Exception {
        String recurso = "DADOS_FINANCEIROS";
        String acao = "VISUALIZACAO";
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            Map<String, Object> dados = invocation.getArgument(0);
            return String.format("{\"recurso\":\"%s\",\"acao\":\"%s\"}", 
                    dados.get("recurso"), dados.get("acao"));
        });
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("10.0.0.100");
        auditoriaService.registrarAcessoRecurso(recurso, acao);
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getTabela().equals("ACESSO_RECURSO") &&
            auditoria.getOperacao() == Auditoria.OperacaoAuditoria.INSERT &&
            auditoria.getDadosNovos().contains("\"recurso\":\"" + recurso + "\"") &&
            auditoria.getDadosNovos().contains("\"acao\":\"" + acao + "\"")
        ));
    }
    @Test
    void deveRegistrarTentativaAcessoNegado() throws Exception {
        String endpoint = "/admin/usuarios";
        String motivo = "Usuário sem permissão de administrador";
        when(securityUtil.getUsuarioLogado()).thenReturn(usuarioMock);
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            Map<String, Object> dados = invocation.getArgument(0);
            return String.format("{\"endpoint\":\"%s\",\"motivo\":\"%s\"}", 
                    dados.get("endpoint"), dados.get("motivo"));
        });
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("172.16.0.50");
        auditoriaService.registrarTentativaAcessoNegado(endpoint, motivo);
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getTabela().equals("ACESSO_NEGADO") &&
            auditoria.getDadosNovos().contains("\"endpoint\":\"" + endpoint + "\"") &&
            auditoria.getDadosNovos().contains("\"motivo\":\"" + motivo + "\"")
        ));
    }
    @Test
    void deveRegistrarLoginComSucesso() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            Map<String, Object> dados = invocation.getArgument(0);
            return String.format("{\"usuarioId\":%d,\"email\":\"%s\",\"sucesso\":%s}", 
                    dados.get("usuarioId"), dados.get("email"), dados.get("sucesso"));
        });
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.0.10");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome/91.0");
        auditoriaService.registrarLogin(usuarioMock, true);
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getTabela().equals("LOGIN") &&
            auditoria.getRegistroId().equals(usuarioMock.getId()) &&
            auditoria.getDadosNovos().contains("\"sucesso\":true") &&
            auditoria.getDadosNovos().contains("\"email\":\"" + usuarioMock.getEmail() + "\"")
        ));
    }
    @Test
    void deveRegistrarLogoutDoUsuario() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            Map<String, Object> dados = invocation.getArgument(0);
            return String.format("{\"usuarioId\":%d,\"email\":\"%s\"}", 
                    dados.get("usuarioId"), dados.get("email"));
        });
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.0.10");
        auditoriaService.registrarLogout(usuarioMock);
        verify(auditoriaRepository).save(argThat(auditoria -> 
            auditoria.getTabela().equals("LOGOUT") &&
            auditoria.getRegistroId().equals(usuarioMock.getId()) &&
            auditoria.getDadosNovos().contains("\"email\":\"" + usuarioMock.getEmail() + "\"")
        ));
    }

    @Test
    void deveLimparAuditoriaAntigaCorretamente() {
        int diasParaManterAuditoria = 90;
        LocalDateTime dataEsperada = LocalDateTime.now().minusDays(diasParaManterAuditoria);
        auditoriaService.limparAuditoriaAntiga(diasParaManterAuditoria);
        verify(auditoriaRepository).deleteByTimestampOperacaoBefore(argThat(data -> 
            data.isBefore(dataEsperada.plusMinutes(1)) && data.isAfter(dataEsperada.minusMinutes(1))
        ));
    }

    @Test
    void deveLidarComOperacaoInvalidaEmFiltros() {
        String operacaoInvalida = "OPERACAO_INEXISTENTE";
        Page<Auditoria> pageExpected = new PageImpl<>(Collections.emptyList());
        when(auditoriaRepository.findComFiltros(anyString(), isNull(), isNull(), any(), any(), any()))
                .thenReturn(pageExpected);
        Page<Auditoria> resultado = auditoriaService.buscarComFiltros(
                "test_table", null, operacaoInvalida, LocalDateTime.now().minusDays(1), LocalDateTime.now(), 0, 10);
        assertThat(resultado).isNotNull();
        verify(auditoriaRepository).findComFiltros(eq("test_table"), isNull(), isNull(), any(), any(), any());
    }
    @Test
    void deveRetornarPaginaVaziaQuandoBuscarPorUsuarioFalha() {
        when(securityUtil.getUsuarioLogado()).thenThrow(new RuntimeException("Usuário não encontrado"));
        Page<Auditoria> resultado = auditoriaService.buscarPorUsuario(1L, 0, 10);
        assertThat(resultado).isNotNull();
        assertThat(resultado.isEmpty()).isTrue();
    }
    @Test
    void deveBuscarUltimasAtividadesDoUsuario() {
        int limit = 5;
        List<Auditoria> atividadesExpected = Arrays.asList(auditoriaMock);
        when(auditoriaRepository.ultimasAtividades(eq(usuarioMock), any(Pageable.class)))
                .thenReturn(atividadesExpected);
        List<Auditoria> resultado = auditoriaService.buscarUltimasAtividades(usuarioMock, limit);
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(1);
        verify(auditoriaRepository).ultimasAtividades(eq(usuarioMock), argThat(pageable -> 
            pageable.getPageSize() == limit && pageable.getPageNumber() == 0
        ));
    }
    @Test
    void deveBuscarExclusoesComPaginacao() {
        Page<Auditoria> pageExpected = new PageImpl<>(Arrays.asList(auditoriaMock));
        when(auditoriaRepository.findExclusoes(any(Pageable.class))).thenReturn(pageExpected);
        Page<Auditoria> resultado = auditoriaService.buscarExclusoes(0, 15);
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        verify(auditoriaRepository).findExclusoes(argThat(pageable -> 
            pageable.getPageSize() == 15 && pageable.getPageNumber() == 0
        ));
    }
    @Test
    void naoDeveFalharAoRegistrarAuditoriaQuandoOcorreExcecaoGeral() {
        when(securityUtil.getUsuarioLogado()).thenThrow(new RuntimeException("Erro inesperado"));

        auditoriaService.registrarInsercao("test_table", 1L, Map.of("teste", "valor"));

        verify(auditoriaRepository, never()).save(any());
    }

    private void clearRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();
    }
}
