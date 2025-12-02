package com.tcc.estoque.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.estoque.dto.*;
import com.tcc.estoque.model.*;
import com.tcc.estoque.repository.*;
import com.tcc.estoque.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestMvc
@ActiveProfiles("test")
@Transactional
class SistemaCompletoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteService clienteService;
    @Autowired
    private ProdutoService produtoService;
    @Autowired
    private VendasService vendasService;
    @Autowired
    private PontuacaoService pontuacaoService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private VendaRepository vendaRepository;
    @Autowired
    private CategoriaConfigRepository categoriaConfigRepository;
    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Autowired
    private AuditoriaRepository auditoriaRepository;

    private Cliente clienteTeste;
    private Produto produtoTeste;
    private CategoriaConfig categoriaBronze;
    private CategoriaConfig categoriaPrata;

    @BeforeEach
    void setup() {
        
        auditoriaRepository.deleteAll();
        vendaRepository.deleteAll();
        movimentacaoEstoqueRepository.deleteAll();
        clienteRepository.deleteAll();
        produtoRepository.deleteAll();
        categoriaConfigRepository.deleteAll();

        categoriaBronze = categoriaConfigRepository.save(CategoriaConfig.builder()
                .nome("Bronze")
                .pontosMinimos(0)
                .pontosMaximos(999)
                .pontosIniciais(100)
                .cor("#CD7F32")
                .ativo(true)
                .ordem(1)
                .build());

        categoriaPrata = categoriaConfigRepository.save(CategoriaConfig.builder()
                .nome("Prata")
                .pontosMinimos(1000)
                .pontosMaximos(2999)
                .pontosIniciais(1000)
                .cor("#C0C0C0")
                .ativo(true)
                .ordem(2)
                .build());

        clienteTeste = clienteRepository.save(Cliente.builder()
                .nome("João da Silva")
                .email("joao@teste.com")
                .telefone("(21)99999-9999")
                .cpf("12345678901")
                .endereco("Rua Teste, 123")
                .pontos(100) 
                .ativo(true)
                .dataCriacao(LocalDateTime.now())
                .build());

        produtoTeste = produtoRepository.save(Produto.builder()
                .nome("Produto Teste")
                .descricao("Descrição do produto teste")
                .preco(BigDecimal.valueOf(50.00))
                .quantidadeEstoque(100)
                .ativo(true)
                .pontosRecompensa(10)
                .dataCriacao(LocalDateTime.now())
                .build());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "VENDEDOR"})
    void fluxoCompletoEntradaVendaPontuacaoDashboard() throws Exception {

        MovimentacaoEstoqueDTO.Request movimentacaoRequest = MovimentacaoEstoqueDTO.Request.builder()
                .produtoId(produtoTeste.getId())
                .tipoMovimentacao(TipoMovimentacao.ENTRADA)
                .quantidade(50)
                .motivo("Reposição de estoque")
                .observacoes("Entrada para teste de integração")
                .build();

        String movimentacaoJson = objectMapper.writeValueAsString(movimentacaoRequest);
        mockMvc.perform(post("/api/estoque/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentacaoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(50))
                .andExpect(jsonPath("$.tipoMovimentacao").value("ENTRADA"));

        Produto produtoAtualizado = produtoRepository.findById(produtoTeste.getId()).orElseThrow();
        assertThat(produtoAtualizado.getQuantidadeEstoque()).isEqualTo(150); 

        VendasDTO.VendaRequest vendaRequest = VendasDTO.VendaRequest.builder()
                .clienteId(clienteTeste.getId())
                .itens(List.of(VendasDTO.ItemVendaRequest.builder()
                        .produtoId(produtoTeste.getId())
                        .quantidade(2)
                        .precoUnitario(BigDecimal.valueOf(50.00))
                        .build()))
                .metodoPagamento("DINHEIRO")
                .observacoes("Venda de teste de integração")
                .build();

        String vendaJson = objectMapper.writeValueAsString(vendaRequest);
        mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendaJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(100.00))
                .andExpect(jsonPath("$.clienteNome").value("João da Silva"));

        produtoAtualizado = produtoRepository.findById(produtoTeste.getId()).orElseThrow();
        assertThat(produtoAtualizado.getQuantidadeEstoque()).isEqualTo(148); 

        Cliente clienteAtualizado = clienteRepository.findById(clienteTeste.getId()).orElseThrow();
        assertThat(clienteAtualizado.getPontos()).isEqualTo(120); 

        var categoriaAtual = pontuacaoService.determinarCategoria(clienteAtualizado.getPontos());
        assertThat(categoriaAtual).isPresent();
        assertThat(categoriaAtual.get().getNome()).isEqualTo("Bronze");

        for (int i = 0; i < 9; i++) {
            VendasDTO.VendaRequest vendaAdicional = VendasDTO.VendaRequest.builder()
                    .clienteId(clienteTeste.getId())
                    .itens(List.of(VendasDTO.ItemVendaRequest.builder()
                            .produtoId(produtoTeste.getId())
                            .quantidade(10)
                            .precoUnitario(BigDecimal.valueOf(50.00))
                            .build()))
                    .metodoPagamento("CARTAO_CREDITO")
                    .observacoes("Venda adicional " + (i + 1))
                    .build();

            mockMvc.perform(post("/api/vendas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vendaAdicional)))
                    .andExpect(status().isOk());
        }

        clienteAtualizado = clienteRepository.findById(clienteTeste.getId()).orElseThrow();
        
        assertThat(clienteAtualizado.getPontos()).isEqualTo(1020);

        var novaCategoria = pontuacaoService.determinarCategoria(clienteAtualizado.getPontos());
        assertThat(novaCategoria).isPresent();
        assertThat(novaCategoria.get().getNome()).isEqualTo("Prata");

        mockMvc.perform(get("/api/dashboard/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVendas").value(10)) 
                .andExpect(jsonPath("$.faturamentoPeriodo").value(5000.00)) 
                .andExpect(jsonPath("$.totalClientes").value(1));

        mockMvc.perform(get("/api/dashboard/produtos-mais-vendidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produtoNome").value("Produto Teste"))
                .andExpect(jsonPath("$[0].quantidadeVendida").value(92)); 

        List<Auditoria> auditorias = auditoriaRepository.findAll();
        assertThat(auditorias.size()).isGreaterThan(0);

        boolean temAuditoriaVenda = auditorias.stream()
                .anyMatch(a -> a.getAcao().contains("VENDA_CRIADA"));
        boolean temAuditoriaMovimentacao = auditorias.stream()
                .anyMatch(a -> a.getAcao().contains("MOVIMENTACAO_ESTOQUE"));
        
        assertAll("Validações de auditoria",
                () -> assertThat(temAuditoriaVenda).isTrue(),
                () -> assertThat(temAuditoriaMovimentacao).isTrue()
        );

        produtoAtualizado = produtoRepository.findById(produtoTeste.getId()).orElseThrow();
        int estoqueEsperado = 100 + 50 - 92; 
        assertThat(produtoAtualizado.getQuantidadeEstoque()).isEqualTo(estoqueEsperado);

        List<MovimentacaoEstoque> movimentacoes = movimentacaoEstoqueRepository.findAll();
        long entradasCount = movimentacoes.stream()
                .filter(m -> m.getTipoMovimentacao() == TipoMovimentacao.ENTRADA)
                .count();
        long saidasCount = movimentacoes.stream()
                .filter(m -> m.getTipoMovimentacao() == TipoMovimentacao.SAIDA)
                .count();
        
        assertAll("Validações de consistência",
                () -> assertThat(entradasCount).isEqualTo(1), 
                () -> assertThat(saidasCount).isEqualTo(10) 
        );
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void fluxoGestaoEstoqueProdutoFaltaReposicaoAlerta() throws Exception {

        VendasDTO.VendaRequest vendaEsgotaEstoque = VendasDTO.VendaRequest.builder()
                .clienteId(clienteTeste.getId())
                .itens(List.of(VendasDTO.ItemVendaRequest.builder()
                        .produtoId(produtoTeste.getId())
                        .quantidade(100) 
                        .precoUnitario(BigDecimal.valueOf(50.00))
                        .build()))
                .metodoPagamento("PIX")
                .build();

        mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendaEsgotaEstoque)))
                .andExpect(status().isOk());

        Produto produtoSemEstoque = produtoRepository.findById(produtoTeste.getId()).orElseThrow();
        assertThat(produtoSemEstoque.getQuantidadeEstoque()).isEqualTo(0);

        VendasDTO.VendaRequest vendaSemEstoque = VendasDTO.VendaRequest.builder()
                .clienteId(clienteTeste.getId())
                .itens(List.of(VendasDTO.ItemVendaRequest.builder()
                        .produtoId(produtoTeste.getId())
                        .quantidade(1)
                        .precoUnitario(BigDecimal.valueOf(50.00))
                        .build()))
                .metodoPagamento("DINHEIRO")
                .build();

        mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendaSemEstoque)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("estoque insuficiente")));

        mockMvc.perform(get("/api/dashboard/alertas-estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produtoNome").value("Produto Teste"))
                .andExpect(jsonPath("$[0].quantidadeAtual").value(0))
                .andExpect(jsonPath("$[0].nivel").value("CRITICO"));

        MovimentacaoEstoqueDTO.Request reposicao = MovimentacaoEstoqueDTO.Request.builder()
                .produtoId(produtoTeste.getId())
                .tipoMovimentacao(TipoMovimentacao.ENTRADA)
                .quantidade(200)
                .motivo("Reposição após esgotamento")
                .observacoes("Compra emergencial de estoque")
                .build();

        mockMvc.perform(post("/api/estoque/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reposicao)))
                .andExpect(status().isOk());

        Produto produtoReabastecido = produtoRepository.findById(produtoTeste.getId()).orElseThrow();
        assertThat(produtoReabastecido.getQuantidadeEstoque()).isEqualTo(200);

        mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendaSemEstoque)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(50.00));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void fluxoPontuacaoCriacaoCategoriaClienteEvolucaoRecompensas() throws Exception {

        PontuacaoDTO.CategoriaConfigRequest categoriaOuro = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Ouro")
                .pontosMinimos(3000)
                .pontosMaximos(null) 
                .pontosIniciais(3000)
                .cor("#FFD700")
                .descricao("Categoria premium para clientes VIP")
                .build();

        mockMvc.perform(post("/api/pontuacao/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoriaOuro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ouro"))
                .andExpect(jsonPath("$.cor").value("#FFD700"));

        Cliente clienteVip = clienteRepository.save(Cliente.builder()
                .nome("Maria VIP")
                .email("maria@vip.com")
                .telefone("(21)88888-8888")
                .cpf("98765432100")
                .endereco("Rua VIP, 456")
                .pontos(3500) 
                .ativo(true)
                .dataCriacao(LocalDateTime.now())
                .build());

        var categoriaClienteVip = pontuacaoService.determinarCategoria(clienteVip.getPontos());
        assertThat(categoriaClienteVip).isPresent();
        assertThat(categoriaClienteVip.get().getNome()).isEqualTo("Ouro");

        mockMvc.perform(get("/api/pontuacao/clientes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Maria VIP"))
                .andExpect(jsonPath("$.content[0].categoriaAtual").value("Ouro"))
                .andExpect(jsonPath("$.content[0].corCategoria").value("#FFD700"));

        mockMvc.perform(get("/api/pontuacao/estatisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientesAtivos").value(2)) 
                .andExpect(jsonPath("$.totalPontosAtivos").value(3620)) 
                .andExpect(jsonPath("$.mediaPontosCliente").value(1810.0));

        mockMvc.perform(post("/api/pontuacao/recalcular-categorias"))
                .andExpect(status().isOk());

        Cliente joaoAtualizado = clienteRepository.findById(clienteTeste.getId()).orElseThrow();
        Cliente mariaAtualizada = clienteRepository.findById(clienteVip.getId()).orElseThrow();
        
        assertAll("Verificação pós-recálculo",
                () -> {
                    var catJoao = pontuacaoService.determinarCategoria(joaoAtualizado.getPontos());
                    assertThat(catJoao.get().getNome()).isEqualTo("Bronze");
                },
                () -> {
                    var catMaria = pontuacaoService.determinarCategoria(mariaAtualizada.getPontos());
                    assertThat(catMaria.get().getNome()).isEqualTo("Ouro");
                }
        );
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void fluxoAuditoriaOperacoesDiversasLogCompletoBuscaFiltros() throws Exception {

        ProdutoDTO.Request novoProduto = ProdutoDTO.Request.builder()
                .nome("Produto Auditado")
                .descricao("Produto para testar auditoria")
                .preco(BigDecimal.valueOf(75.50))
                .quantidadeEstoque(50)
                .pontosRecompensa(15)
                .build();

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novoProduto)))
                .andExpect(status().isOk());

        ClienteDTO.Request atualizacaoCliente = ClienteDTO.Request.builder()
                .nome("João da Silva Atualizado")
                .email("joao.novo@teste.com")
                .telefone("(21)77777-7777")
                .endereco("Rua Nova, 789")
                .build();

        mockMvc.perform(put("/api/clientes/" + clienteTeste.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizacaoCliente)))
                .andExpect(status().isOk());

        Thread.sleep(100);

        mockMvc.perform(get("/api/auditoria/buscar")
                        .param("usuario", "admin")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThan(0)));

        LocalDate hoje = LocalDate.now();
        mockMvc.perform(get("/api/auditoria/buscar")
                        .param("dataInicio", hoje.toString())
                        .param("dataFim", hoje.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/auditoria/estatisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOperacoes").value(greaterThan(0)))
                .andExpect(jsonPath("$.operacoesPorTipo").exists());

        List<Auditoria> todasAuditorias = auditoriaRepository.findAll();
        assertThat(todasAuditorias).isNotEmpty();

        boolean temCriacaoProduto = todasAuditorias.stream()
                .anyMatch(a -> a.getAcao().contains("PRODUTO") && 
                              a.getDetalhes().contains("Produto Auditado"));

        boolean temAtualizacaoCliente = todasAuditorias.stream()
                .anyMatch(a -> a.getAcao().contains("CLIENTE") && 
                              a.getDetalhes().contains("Atualizado"));

        assertAll("Verificações de auditoria detalhada",
                () -> assertThat(temCriacaoProduto).isTrue(),
                () -> assertThat(temAtualizacaoCliente).isTrue(),
                () -> assertThat(todasAuditorias.stream()
                        .allMatch(a -> a.getUsuario().equals("admin"))).isTrue()
        );
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testePerformanceMultiplasOperacoesSimultaneas() throws Exception {
        long inicioTeste = System.currentTimeMillis();

        for (int i = 1; i <= 10; i++) {
            ProdutoDTO.Request produto = ProdutoDTO.Request.builder()
                    .nome("Produto Performance " + i)
                    .descricao("Produto para teste de performance")
                    .preco(BigDecimal.valueOf(10.00 * i))
                    .quantidadeEstoque(100)
                    .pontosRecompensa(i)
                    .build();

            mockMvc.perform(post("/api/produtos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(produto)))
                    .andExpect(status().isOk());
        }

        for (int i = 1; i <= 5; i++) {
            ClienteDTO.Request cliente = ClienteDTO.Request.builder()
                    .nome("Cliente Performance " + i)
                    .email("cliente" + i + "@performance.com")
                    .telefone("(21)9999-999" + i)
                    .cpf("1234567890" + i)
                    .endereco("Rua Performance, " + (100 + i))
                    .build();

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cliente)))
                    .andExpect(status().isOk());
        }

        long tempoTotalOperacoes = System.currentTimeMillis() - inicioTeste;

        long inicioDashboard = System.currentTimeMillis();
        mockMvc.perform(get("/api/dashboard/metricas"))
                .andExpect(status().isOk());
        long tempoDashboard = System.currentTimeMillis() - inicioDashboard;

        long inicioProdutos = System.currentTimeMillis();
        mockMvc.perform(get("/api/produtos")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(11)); 
        long tempoProdutos = System.currentTimeMillis() - inicioProdutos;

        assertAll("Validações de performance",
                () -> assertThat(tempoTotalOperacoes).isLessThan(5000), 
                () -> assertThat(tempoDashboard).isLessThan(1000),      
                () -> assertThat(tempoProdutos).isLessThan(500),        
                () -> assertThat(produtoRepository.count()).isEqualTo(11),
                () -> assertThat(clienteRepository.count()).isEqualTo(6)
        );
    }
}
