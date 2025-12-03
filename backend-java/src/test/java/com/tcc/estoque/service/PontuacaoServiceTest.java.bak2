package com.tcc.estoque.service;

import com.tcc.estoque.dto.PontuacaoDTO;
import com.tcc.estoque.model.*;
import com.tcc.estoque.repository.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PontuacaoServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock private CategoriaConfigRepository categoriaConfigRepository;
    @Mock private RecompensaRepository recompensaRepository;
    @Mock private HistoricoPontosRepository historicoPontosRepository;

    @InjectMocks
    private PontuacaoService pontuacaoService;

    private Cliente clienteMock;
    private CategoriaConfig categoriaBronze;
    private CategoriaConfig categoriaPrata;
    private CategoriaConfig categoriaOuro;
    private Recompensa recompensaMock;

    @BeforeEach
    void setup() {
        clienteMock = Cliente.builder()
                .id(1L)
                .nome("Cliente Teste")
                .email("cliente@teste.com")
                .pontos(500)
                .ativo(true)
                .totalCompras(BigDecimal.valueOf(2500))
                .quantidadeCompras(5)
                .ultimaCompra(LocalDateTime.now().minusDays(7))
                .build();

        categoriaBronze = CategoriaConfig.builder()
                .id(1L)
                .nome("Bronze")
                .descricao("Categoria inicial")
                .pontosMinimos(0)
                .pontosMaximos(999)
                .pontosIniciais(100)
                .cor("#CD7F32")
                .ativo(true)
                .ordem(1)
                .dataCriacao(LocalDateTime.now().minusMonths(1))
                .dataAtualizacao(LocalDateTime.now())
                .build();

        categoriaPrata = CategoriaConfig.builder()
                .id(2L)
                .nome("Prata")
                .descricao("Categoria intermediária")
                .pontosMinimos(1000)
                .pontosMaximos(2999)
                .pontosIniciais(1000)
                .cor("#C0C0C0")
                .ativo(true)
                .ordem(2)
                .dataCriacao(LocalDateTime.now().minusMonths(1))
                .dataAtualizacao(LocalDateTime.now())
                .build();

        categoriaOuro = CategoriaConfig.builder()
                .id(3L)
                .nome("Ouro")
                .descricao("Categoria premium")
                .pontosMinimos(3000)
                .pontosMaximos(null) 
                .pontosIniciais(3000)
                .cor("#FFD700")
                .ativo(true)
                .ordem(3)
                .dataCriacao(LocalDateTime.now().minusMonths(1))
                .dataAtualizacao(LocalDateTime.now())
                .build();

        recompensaMock = Recompensa.builder()
                .id(1L)
                .nome("Desconto 10%")
                .descricao("Desconto de 10% na próxima compra")
                .pontosNecessarios(200)
                .categoria("DESCONTO")
                .valorDesconto(null)
                .percentualDesconto(BigDecimal.valueOf(10))
                .quantidadeDisponivel(50)
                .quantidadeResgatada(5)
                .dataValidade(LocalDate.now().plusMonths(3))
                .ativo(true)
                .dataCriacao(LocalDateTime.now().minusDays(30))
                .build();
    }

    @Test
    void deveCriarCategoriaComDadosValidos() {
        
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Diamante")
                .descricao("Categoria VIP")
                .pontosMinimos(5000)
                .pontosMaximos(null)
                .pontosIniciais(5000)
                .cor("#B9F2FF")
                .ordem(4)
                .build();

        when(categoriaConfigRepository.countCategoriasComConflito(5000, null, null)).thenReturn(0L);
        when(categoriaConfigRepository.save(any(CategoriaConfig.class))).thenAnswer(invocation -> {
            CategoriaConfig categoria = invocation.getArgument(0);
            categoria.setId(4L);
            categoria.setDataCriacao(LocalDateTime.now());
            categoria.setDataAtualizacao(LocalDateTime.now());
            return categoria;
        });

        PontuacaoDTO.CategoriaConfigResponse resultado = pontuacaoService.criarCategoria(request);

        assertAll("Validação da criação de categoria",
                () -> assertThat(resultado.getId()).isEqualTo(4L),
                () -> assertThat(resultado.getNome()).isEqualTo("Diamante"),
                () -> assertThat(resultado.getDescricao()).isEqualTo("Categoria VIP"),
                () -> assertThat(resultado.getPontosMinimos()).isEqualTo(5000),
                () -> assertThat(resultado.getPontosMaximos()).isNull(),
                () -> assertThat(resultado.getCor()).isEqualTo("#B9F2FF"),
                () -> assertThat(resultado.getOrdem()).isEqualTo(4),
                () -> assertThat(resultado.getAtivo()).isTrue()
        );

        verify(categoriaConfigRepository).save(argThat(categoria ->
                categoria.getNome().equals("Diamante") &&
                categoria.getAtivo().equals(true) &&
                categoria.getOrdem().equals(4)
        ));
    }

    @Test
    void deveDefinirOrdemAutomaticamenteQuandoNaoInformada() {
        
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Nova Categoria")
                .pontosMinimos(1500)
                .pontosMaximos(2499)
                .ordem(null) 
                .build();

        List<CategoriaConfig> categoriasExistentes = Arrays.asList(categoriaBronze, categoriaPrata);
        
        when(categoriaConfigRepository.countCategoriasComConflito(1500, 2499, null)).thenReturn(0L);
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc()).thenReturn(categoriasExistentes);
        when(categoriaConfigRepository.save(any(CategoriaConfig.class))).thenAnswer(invocation -> {
            CategoriaConfig categoria = invocation.getArgument(0);
            categoria.setId(5L);
            return categoria;
        });

        PontuacaoDTO.CategoriaConfigResponse resultado = pontuacaoService.criarCategoria(request);

        verify(categoriaConfigRepository).save(argThat(categoria ->
                categoria.getOrdem().equals(3) 
        ));
        assertThat(resultado.getId()).isEqualTo(5L);
    }

    @Test
    void deveLancarExcecaoQuandoHaConflitoRangePontos() {
        
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Categoria Conflitante")
                .pontosMinimos(500) 
                .pontosMaximos(1500)
                .build();

        when(categoriaConfigRepository.countCategoriasComConflito(500, 1500, null)).thenReturn(1L); 

        assertThatThrownBy(() -> pontuacaoService.criarCategoria(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Já existe uma categoria com conflito no range de pontos especificado");

        verify(categoriaConfigRepository, never()).save(any());
    }

    @Test
    void deveAtualizarCategoriaExistenteComValidacaoConflitos() {
        
        Long categoriaId = 2L;
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Prata Premium")
                .descricao("Categoria prata atualizada")
                .pontosMinimos(1200) 
                .pontosMaximos(2999)
                .pontosIniciais(1200)
                .cor("#E5E5E5")
                .ordem(2)
                .build();

        when(categoriaConfigRepository.findById(categoriaId)).thenReturn(Optional.of(categoriaPrata));
        when(categoriaConfigRepository.countCategoriasComConflito(1200, 2999, categoriaId)).thenReturn(0L);
        when(categoriaConfigRepository.save(any(CategoriaConfig.class))).thenReturn(categoriaPrata);

        PontuacaoDTO.CategoriaConfigResponse resultado = pontuacaoService.atualizarCategoria(categoriaId, request);

        assertAll("Validação da atualização",
                () -> assertThat(resultado.getNome()).isEqualTo("Prata Premium"),
                () -> assertThat(resultado.getDescricao()).isEqualTo("Categoria prata atualizada"),
                () -> assertThat(resultado.getPontosMinimos()).isEqualTo(1200),
                () -> assertThat(resultado.getCor()).isEqualTo("#E5E5E5")
        );

        verify(categoriaConfigRepository).save(argThat(categoria ->
                categoria.getNome().equals("Prata Premium") &&
                categoria.getPontosMinimos().equals(1200)
        ));
    }

    @Test
    void deveLancarExcecaoAoAtualizarCategoriaInexistente() {
        
        Long categoriaIdInexistente = 999L;
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Categoria Inexistente")
                .build();

        when(categoriaConfigRepository.findById(categoriaIdInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pontuacaoService.atualizarCategoria(categoriaIdInexistente, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Categoria não encontrada");
    }

    @Test
    void deveRemoverCategoriaMarcandoComoInativa() {
        
        Long categoriaId = 1L;
        when(categoriaConfigRepository.findById(categoriaId)).thenReturn(Optional.of(categoriaBronze));

        pontuacaoService.removerCategoria(categoriaId);

        verify(categoriaConfigRepository).save(argThat(categoria ->
                categoria.getAtivo().equals(false)
        ));
    }

    @Test
    void deveListarCategoriasAtivasOrdenadasPorOrdem() {
        
        List<CategoriaConfig> categorias = Arrays.asList(categoriaBronze, categoriaPrata, categoriaOuro);
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc()).thenReturn(categorias);

        List<PontuacaoDTO.CategoriaConfigResponse> resultado = pontuacaoService.listarCategorias();

        assertAll("Validação da listagem de categorias",
                () -> assertThat(resultado).hasSize(3),
                () -> assertThat(resultado.get(0).getNome()).isEqualTo("Bronze"),
                () -> assertThat(resultado.get(1).getNome()).isEqualTo("Prata"),
                () -> assertThat(resultado.get(2).getNome()).isEqualTo("Ouro"),
                () -> assertThat(resultado.stream().allMatch(c -> c.getAtivo())).isTrue()
        );
    }

    @Test
    void deveBuscarCategoriaPorId() {
        
        when(categoriaConfigRepository.findById(1L)).thenReturn(Optional.of(categoriaBronze));

        PontuacaoDTO.CategoriaConfigResponse resultado = pontuacaoService.buscarCategoriaPorId(1L);

        assertAll("Validação da busca por ID",
                () -> assertThat(resultado.getId()).isEqualTo(1L),
                () -> assertThat(resultado.getNome()).isEqualTo("Bronze"),
                () -> assertThat(resultado.getPontosMinimos()).isEqualTo(0),
                () -> assertThat(resultado.getPontosMaximos()).isEqualTo(999)
        );
    }

    @Test
    void deveDeterminarCategoriaCorretamenteBaseadaNospontos() {
        
        when(categoriaConfigRepository.findCategoriaParaPontos(500))
                .thenReturn(Optional.of(categoriaBronze));

        Optional<CategoriaConfig> resultado = pontuacaoService.determinarCategoria(500);

        assertAll("Validação da determinação de categoria",
                () -> assertThat(resultado).isPresent(),
                () -> assertThat(resultado.get().getNome()).isEqualTo("Bronze"),
                () -> assertThat(resultado.get().getPontosMinimos()).isLessThanOrEqualTo(500),
                () -> assertThat(resultado.get().getPontosMaximos()).isGreaterThanOrEqualTo(500)
        );
    }

    @Test
    void deveUsarBuscaAlternativaQuandoQueryPrincipalFalha() {
        
        when(categoriaConfigRepository.findCategoriaParaPontos(1500))
                .thenThrow(new RuntimeException("Erro na query"));

        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(categoriaBronze, categoriaPrata, categoriaOuro));

        Optional<CategoriaConfig> resultado = pontuacaoService.determinarCategoria(1500);

        assertAll("Validação da busca alternativa",
                () -> assertThat(resultado).isPresent(),
                () -> assertThat(resultado.get().getNome()).isEqualTo("Prata"),
                () -> assertThat(1500).isBetween(resultado.get().getPontosMinimos(), resultado.get().getPontosMaximos())
        );
    }

    @Test
    void deveTratarCategoriaSemLimiteSuperior() {
        
        when(categoriaConfigRepository.findCategoriaParaPontos(5000))
                .thenThrow(new RuntimeException("Erro na query"));
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(categoriaBronze, categoriaPrata, categoriaOuro));

        Optional<CategoriaConfig> resultado = pontuacaoService.determinarCategoria(5000);

        assertAll("Validação categoria sem limite superior",
                () -> assertThat(resultado).isPresent(),
                () -> assertThat(resultado.get().getNome()).isEqualTo("Ouro"),
                () -> assertThat(resultado.get().getPontosMinimos()).isLessThanOrEqualTo(5000),
                () -> assertThat(resultado.get().getPontosMaximos()).isNull()
        );
    }

    @Test
    void deveBuscarProximaCategoriaNaProgressao() {
        
        when(categoriaConfigRepository.findProximaCategoria(800))
                .thenReturn(Optional.of(categoriaPrata));

        Optional<CategoriaConfig> resultado = pontuacaoService.buscarProximaCategoria(800);

        assertAll("Validação da próxima categoria",
                () -> assertThat(resultado).isPresent(),
                () -> assertThat(resultado.get().getNome()).isEqualTo("Prata"),
                () -> assertThat(resultado.get().getPontosMinimos()).isGreaterThan(800)
        );
    }

    @Test
    void deveCriarRecompensaComDadosValidos() {
        
        PontuacaoDTO.RecompensaRequest request = PontuacaoDTO.RecompensaRequest.builder()
                .nome("Brinde Especial")
                .descricao("Produto grátis")
                .pontosNecessarios(500)
                .categoria("PRODUTO")
                .valorDesconto(BigDecimal.valueOf(25.00))
                .percentualDesconto(null)
                .quantidadeDisponivel(20)
                .dataValidade(LocalDate.now().plusMonths(6))
                .build();

        when(recompensaRepository.save(any(Recompensa.class))).thenAnswer(invocation -> {
            Recompensa recompensa = invocation.getArgument(0);
            recompensa.setId(2L);
            recompensa.setDataCriacao(LocalDateTime.now());
            recompensa.setQuantidadeResgatada(0);
            return recompensa;
        });

        PontuacaoDTO.RecompensaResponse resultado = pontuacaoService.criarRecompensa(request);

        assertAll("Validação da criação de recompensa",
                () -> assertThat(resultado.getId()).isEqualTo(2L),
                () -> assertThat(resultado.getNome()).isEqualTo("Brinde Especial"),
                () -> assertThat(resultado.getPontosNecessarios()).isEqualTo(500),
                () -> assertThat(resultado.getCategoria()).isEqualTo("PRODUTO"),
                () -> assertThat(resultado.getValorDesconto()).isEqualByComparingTo(BigDecimal.valueOf(25.00)),
                () -> assertThat(resultado.getQuantidadeDisponivel()).isEqualTo(20),
                () -> assertThat(resultado.getAtivo()).isTrue()
        );
    }

    @Test
    void deveAtualizarRecompensaExistente() {
        
        Long recompensaId = 1L;
        PontuacaoDTO.RecompensaRequest request = PontuacaoDTO.RecompensaRequest.builder()
                .nome("Desconto 15%")
                .descricao("Desconto atualizado")
                .pontosNecessarios(250)
                .percentualDesconto(BigDecimal.valueOf(15))
                .quantidadeDisponivel(100)
                .build();

        when(recompensaRepository.findById(recompensaId)).thenReturn(Optional.of(recompensaMock));
        when(recompensaRepository.save(any(Recompensa.class))).thenReturn(recompensaMock);

        PontuacaoDTO.RecompensaResponse resultado = pontuacaoService.atualizarRecompensa(recompensaId, request);

        verify(recompensaRepository).save(argThat(recompensa ->
                recompensa.getNome().equals("Desconto 15%") &&
                recompensa.getPontosNecessarios().equals(250) &&
                recompensa.getPercentualDesconto().equals(BigDecimal.valueOf(15))
        ));
        assertThat(resultado.getId()).isEqualTo(1L);
    }

    @Test
    void deveRemoverRecompensaMarcandoComoInativa() {
        
        when(recompensaRepository.findById(1L)).thenReturn(Optional.of(recompensaMock));

        pontuacaoService.removerRecompensa(1L);

        verify(recompensaRepository).save(argThat(recompensa ->
                recompensa.getAtivo().equals(false)
        ));
    }

    @Test
    void deveListarRecompensasAtivasOrdenadasPorPontos() {
        
        List<Recompensa> recompensas = Arrays.asList(recompensaMock);
        when(recompensaRepository.findByAtivoTrueOrderByPontosNecessariosAsc()).thenReturn(recompensas);

        List<PontuacaoDTO.RecompensaResponse> resultado = pontuacaoService.listarRecompensas();

        assertAll("Validação da listagem de recompensas",
                () -> assertThat(resultado).hasSize(1),
                () -> assertThat(resultado.get(0).getNome()).isEqualTo("Desconto 10%"),
                () -> assertThat(resultado.get(0).getPontosNecessarios()).isEqualTo(200),
                () -> assertThat(resultado.get(0).getAtivo()).isTrue()
        );
    }

    @Test
    void deveListarRecompensasDisponiveisParaCliente() {
        
        Long clienteId = 1L;
        clienteMock.setPontos(300); 
        
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(recompensaRepository.findRecompensasDisponiveis(300))
                .thenReturn(Arrays.asList(recompensaMock)); 

        List<PontuacaoDTO.RecompensaResponse> resultado = pontuacaoService.listarRecompensasDisponiveis(clienteId);

        assertAll("Validação das recompensas disponíveis",
                () -> assertThat(resultado).hasSize(1),
                () -> assertThat(resultado.get(0).getPontosNecessarios()).isLessThanOrEqualTo(300),
                () -> assertThat(resultado.get(0).getNome()).isEqualTo("Desconto 10%")
        );
    }

    @Test
    void deveLancarExcecaoAoBuscarRecompensasParaClienteInexistente() {
        
        Long clienteIdInexistente = 999L;
        when(clienteRepository.findById(clienteIdInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pontuacaoService.listarRecompensasDisponiveis(clienteIdInexistente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cliente não encontrado");
    }

    @Test
    void deveListarClientesComPontuacaoOrdenados() {
        
        Pageable pageable = PageRequest.of(0, 10);
        List<Cliente> clientes = Arrays.asList(clienteMock);
        Page<Cliente> pageClientes = new PageImpl<>(clientes, pageable, 1);
        
        when(clienteRepository.findByAtivoTrueOrderByPontosDesc(pageable)).thenReturn(pageClientes);
        when(categoriaConfigRepository.findCategoriaParaPontos(500))
                .thenReturn(Optional.of(categoriaBronze));
        when(categoriaConfigRepository.findProximaCategoria(500))
                .thenReturn(Optional.of(categoriaPrata));

        Page<PontuacaoDTO.ClientePontuacaoResponse> resultado = pontuacaoService.listarClientesComPontuacao(pageable);

        assertAll("Validação da listagem de clientes",
                () -> assertThat(resultado.getContent()).hasSize(1),
                () -> assertThat(resultado.getContent().get(0).getNome()).isEqualTo("Cliente Teste"),
                () -> assertThat(resultado.getContent().get(0).getPontos()).isEqualTo(500),
                () -> assertThat(resultado.getContent().get(0).getCategoriaAtual()).isEqualTo("Bronze"),
                () -> assertThat(resultado.getContent().get(0).getProximaCategoria()).isEqualTo("Prata"),
                () -> assertThat(resultado.getContent().get(0).getPontosProximaCategoria()).isEqualTo(1000)
        );
    }

    @Test
    void deveObterEstatisticasPontuacaoCompletas() {
        
        List<Cliente> clientesAtivos = Arrays.asList(
                Cliente.builder().pontos(500).ativo(true).build(),
                Cliente.builder().pontos(1200).ativo(true).build(),
                Cliente.builder().pontos(800).ativo(true).build(),
                Cliente.builder().pontos(0).ativo(false).build() 
        );

        when(clienteRepository.countByAtivoTrue()).thenReturn(3L);
        when(clienteRepository.findAll()).thenReturn(clientesAtivos);
        when(historicoPontosRepository.countByPontosAdicionadosLessThan(0)).thenReturn(15L); 
        when(recompensaRepository.countByAtivoTrue()).thenReturn(8L);

        PontuacaoDTO.EstatisticasPontuacaoResponse resultado = pontuacaoService.obterEstatisticas();

        assertAll("Validação das estatísticas",
                () -> assertThat(resultado.getTotalClientes()).isEqualTo(3L),
                () -> assertThat(resultado.getTotalPontosAtivos()).isEqualTo(2500), 
                () -> assertThat(resultado.getMediaPontosCliente()).isEqualTo(833.33, within(0.01)), 
                () -> assertThat(resultado.getTotalResgates()).isEqualTo(15L),
                () -> assertThat(resultado.getTotalRecompensasAtivas()).isEqualTo(8L)
        );
    }

    @Test
    void deveCalcularMediaZeroQuandoNaoHaClientesAtivos() {
        
        when(clienteRepository.countByAtivoTrue()).thenReturn(0L);
        when(clienteRepository.findAll()).thenReturn(Collections.emptyList());
        when(historicoPontosRepository.countByPontosAdicionadosLessThan(0)).thenReturn(0L);
        when(recompensaRepository.countByAtivoTrue()).thenReturn(0L);

        PontuacaoDTO.EstatisticasPontuacaoResponse resultado = pontuacaoService.obterEstatisticas();

        assertAll("Validação com zero clientes",
                () -> assertThat(resultado.getTotalClientes()).isEqualTo(0L),
                () -> assertThat(resultado.getTotalPontosAtivos()).isEqualTo(0),
                () -> assertThat(resultado.getMediaPontosCliente()).isEqualTo(0.0),
                () -> assertThat(resultado.getTotalResgates()).isEqualTo(0L),
                () -> assertThat(resultado.getTotalRecompensasAtivas()).isEqualTo(0L)
        );
    }

    @Test
    void deveRecalcularCategoriasTodasOsClientes() {
        
        List<Cliente> todosClientes = Arrays.asList(
                Cliente.builder().id(1L).nome("Cliente 1").pontos(300).build(),
                Cliente.builder().id(2L).nome("Cliente 2").pontos(1500).build(),
                Cliente.builder().id(3L).nome("Cliente 3").pontos(4000).build()
        );

        when(clienteRepository.findAll()).thenReturn(todosClientes);

        when(categoriaConfigRepository.findCategoriaParaPontos(300))
                .thenReturn(Optional.of(categoriaBronze));
        when(categoriaConfigRepository.findCategoriaParaPontos(1500))
                .thenReturn(Optional.of(categoriaPrata));
        when(categoriaConfigRepository.findCategoriaParaPontos(4000))
                .thenReturn(Optional.of(categoriaOuro));

        pontuacaoService.recalcularCategoriasClientes();

        verify(categoriaConfigRepository).findCategoriaParaPontos(300);
        verify(categoriaConfigRepository).findCategoriaParaPontos(1500);
        verify(categoriaConfigRepository).findCategoriaParaPontos(4000);
    }

    @Test
    void deveTratarClienteSemCategoriaDefinida() {
        
        Pageable pageable = PageRequest.of(0, 10);
        Cliente clienteSemCategoria = Cliente.builder()
                .id(2L)
                .nome("Cliente Sem Categoria")
                .pontos(50000) 
                .ativo(true)
                .build();
        
        Page<Cliente> pageClientes = new PageImpl<>(Arrays.asList(clienteSemCategoria), pageable, 1);
        
        when(clienteRepository.findByAtivoTrueOrderByPontosDesc(pageable)).thenReturn(pageClientes);
        when(categoriaConfigRepository.findCategoriaParaPontos(50000))
                .thenReturn(Optional.empty()); 
        when(categoriaConfigRepository.findProximaCategoria(50000))
                .thenReturn(Optional.empty()); 

        Page<PontuacaoDTO.ClientePontuacaoResponse> resultado = pontuacaoService.listarClientesComPontuacao(pageable);

        PontuacaoDTO.ClientePontuacaoResponse cliente = resultado.getContent().get(0);
        assertAll("Validação cliente sem categoria",
                () -> assertThat(cliente.getCategoriaAtual()).isEqualTo("Sem categoria"),
                () -> assertThat(cliente.getCorCategoria()).isEqualTo("#999999"),
                () -> assertThat(cliente.getProximaCategoria()).isNull(),
                () -> assertThat(cliente.getPontosProximaCategoria()).isNull()
        );
    }

    @Test
    void deveMapeararCorretamenteInformacoesRecompensaComEstoque() {
        
        Recompensa recompensaComEstoque = Recompensa.builder()
                .id(3L)
                .nome("Produto Limitado")
                .pontosNecessarios(1000)
                .quantidadeDisponivel(10)
                .quantidadeResgatada(3)
                .dataValidade(LocalDate.now().plusDays(30)) 
                .ativo(true)
                .dataCriacao(LocalDateTime.now().minusDays(10))
                .build();

        when(recompensaRepository.findByAtivoTrueOrderByPontosNecessariosAsc())
                .thenReturn(Arrays.asList(recompensaComEstoque));

        List<PontuacaoDTO.RecompensaResponse> resultado = pontuacaoService.listarRecompensas();

        PontuacaoDTO.RecompensaResponse recompensa = resultado.get(0);
        assertAll("Validação do mapeamento de recompensa",
                () -> assertThat(recompensa.getQuantidadeDisponivel()).isEqualTo(10),
                () -> assertThat(recompensa.getQuantidadeResgatada()).isEqualTo(3),
                () -> assertThat(recompensa.getDisponivel()).isTrue(), 
                () -> assertThat(recompensa.getTemEstoque()).isTrue(), 
                () -> assertThat(recompensa.getDataValidade()).isAfter(LocalDate.now())
        );
    }

    @Test
    void deveProcessarCategoriaComPontosIniciaisDiferentesDosMinimos() {
        
        CategoriaConfig categoriaEspecial = CategoriaConfig.builder()
                .id(4L)
                .nome("VIP")
                .pontosMinimos(2000)
                .pontosMaximos(4999)
                .pontosIniciais(2500) 
                .cor("#FF0000")
                .ativo(true)
                .ordem(4)
                .build();

        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(categoriaEspecial));

        List<PontuacaoDTO.CategoriaConfigResponse> resultado = pontuacaoService.listarCategorias();

        PontuacaoDTO.CategoriaConfigResponse categoria = resultado.get(0);
        assertAll("Validação categoria com pontos iniciais específicos",
                () -> assertThat(categoria.getPontosMinimos()).isEqualTo(2000),
                () -> assertThat(categoria.getPontosIniciais()).isEqualTo(2500),
                () -> assertThat(categoria.getPontosIniciais()).isNotEqualTo(categoria.getPontosMinimos())
        );
    }
}
