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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PontuacaoServiceMutacaoTest {

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

    @BeforeEach
    void setup() {
        clienteMock = Cliente.builder()
                .id(1L)
                .nome("Cliente Teste")
                .pontos(1000)
                .ativo(true)
                .build();

        categoriaBronze = CategoriaConfig.builder()
                .id(1L)
                .nome("Bronze")
                .pontosMinimos(0)
                .pontosMaximos(999)
                .pontosIniciais(100)
                .ativo(true)
                .ordem(1)
                .build();

        categoriaPrata = CategoriaConfig.builder()
                .id(2L)
                .nome("Prata")
                .pontosMinimos(1000)
                .pontosMaximos(2999)
                .pontosIniciais(1000)
                .ativo(true)
                .ordem(2)
                .build();
    }

    @Test
    void mataMutanteDeveValidarCountCategoriasComConflitoMaiorQueZero() {
        
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Nova Categoria")
                .pontosMinimos(500)
                .pontosMaximos(1500)
                .build();

        when(categoriaConfigRepository.countCategoriasComConflito(500, 1500, null)).thenReturn(1L);

        try {
            pontuacaoService.criarCategoria(request);
            assertThat(false).as("Deveria ter lançado exceção").isTrue();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("conflito no range de pontos");
        }
    }

    @Test
    void mataMutanteDeveValidarCountCategoriasComConflitoIgualZero() {
        
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Nova Categoria")
                .pontosMinimos(500)
                .pontosMaximos(1500)
                .build();

        when(categoriaConfigRepository.countCategoriasComConflito(500, 1500, null)).thenReturn(0L);
        when(categoriaConfigRepository.save(any(CategoriaConfig.class))).thenAnswer(invocation -> {
            CategoriaConfig categoria = invocation.getArgument(0);
            categoria.setId(3L);
            return categoria;
        });

        PontuacaoDTO.CategoriaConfigResponse resultado = pontuacaoService.criarCategoria(request);

        assertThat(resultado.getId()).isEqualTo(3L);
        verify(categoriaConfigRepository).save(any(CategoriaConfig.class));
    }

    @Test
    void mataMutanteDeveValidarOrdemNullVsNaoNull() {
        
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Categoria Sem Ordem")
                .pontosMinimos(100)
                .pontosMaximos(500)
                .ordem(null) 
                .build();

        when(categoriaConfigRepository.countCategoriasComConflito(100, 500, null)).thenReturn(0L);
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(categoriaBronze, categoriaPrata)); 
        when(categoriaConfigRepository.save(any(CategoriaConfig.class))).thenAnswer(invocation -> {
            CategoriaConfig categoria = invocation.getArgument(0);
            categoria.setId(3L);
            return categoria;
        });

        pontuacaoService.criarCategoria(request);

        verify(categoriaConfigRepository).save(argThat(categoria ->
                categoria.getOrdem().equals(3)
        ));
    }

    @Test
    void mataMutanteDeveMantereOrdemQuandoNaoENull() {
        
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Categoria Com Ordem")
                .pontosMinimos(100)
                .pontosMaximos(500)
                .ordem(5) 
                .build();

        when(categoriaConfigRepository.countCategoriasComConflito(100, 500, null)).thenReturn(0L);
        when(categoriaConfigRepository.save(any(CategoriaConfig.class))).thenAnswer(invocation -> {
            CategoriaConfig categoria = invocation.getArgument(0);
            categoria.setId(3L);
            return categoria;
        });

        pontuacaoService.criarCategoria(request);

        verify(categoriaConfigRepository, never()).findByAtivoTrueOrderByOrdemAsc();
        verify(categoriaConfigRepository).save(argThat(categoria ->
                categoria.getOrdem().equals(5)
        ));
    }

    @Test
    void mataMutanteDeveValidarCondicaoCategoriaSemLimiteSuperior() {
        
        CategoriaConfig categoriaIlimitada = CategoriaConfig.builder()
                .nome("Premium")
                .pontosMinimos(3000)
                .pontosMaximos(null) 
                .ativo(true)
                .build();

        when(categoriaConfigRepository.findCategoriaParaPontos(5000))
                .thenThrow(new RuntimeException("Erro simulado"));
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(categoriaIlimitada));

        Optional<CategoriaConfig> resultado = pontuacaoService.determinarCategoria(5000);

        assertAll("Validação da condição complexa",
                () -> assertThat(resultado).isPresent(),
                () -> assertThat(resultado.get().getNome()).isEqualTo("Premium"),
                () -> assertThat(resultado.get().getPontosMinimos()).isLessThanOrEqualTo(5000),
                () -> assertThat(resultado.get().getPontosMaximos()).isNull()
        );
    }

    @Test
    void mataMutanteDeveValidarCondicaoCategoriaComLimiteSuperior() {
        
        when(categoriaConfigRepository.findCategoriaParaPontos(1000))
                .thenThrow(new RuntimeException("Erro simulado"));
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(categoriaBronze, categoriaPrata));

        Optional<CategoriaConfig> resultado = pontuacaoService.determinarCategoria(1000);

        assertAll("Validação dos limites exatos",
                () -> assertThat(resultado).isPresent(),
                () -> assertThat(resultado.get().getNome()).isEqualTo("Prata"),
                () -> assertThat(resultado.get().getPontosMinimos()).isEqualTo(1000),
                () -> assertThat(resultado.get().getPontosMaximos()).isEqualTo(2999)
        );
    }

    @Test
    void mataMutanteDeveValidarTotalClientesMaiorQueZeroParaMedia() {
        
        when(clienteRepository.countByAtivoTrue()).thenReturn(1L);
        when(clienteRepository.findAll()).thenReturn(Arrays.asList(
                Cliente.builder().pontos(500).ativo(true).build()
        ));
        when(historicoPontosRepository.countByPontosAdicionadosLessThan(0)).thenReturn(0L);
        when(recompensaRepository.countByAtivoTrue()).thenReturn(0L);

        PontuacaoDTO.EstatisticasPontuacaoResponse resultado = pontuacaoService.obterEstatisticas();

        assertThat(resultado.getMediaPontosCliente()).isEqualTo(500.0);
    }

    @Test
    void mataMutanteDeveRetornareMedia0QuandoTotalClientesIgualZero() {
        
        when(clienteRepository.countByAtivoTrue()).thenReturn(0L);
        when(clienteRepository.findAll()).thenReturn(Collections.emptyList());
        when(historicoPontosRepository.countByPontosAdicionadosLessThan(0)).thenReturn(0L);
        when(recompensaRepository.countByAtivoTrue()).thenReturn(0L);

        PontuacaoDTO.EstatisticasPontuacaoResponse resultado = pontuacaoService.obterEstatisticas();

        assertThat(resultado.getMediaPontosCliente()).isEqualTo(0.0);
    }

    @Test
    void mataMutanteDeveUsarTamanhoDaListaMais1ParaOrdemAutomatica() {
        
        PontuacaoDTO.CategoriaConfigRequest request = PontuacaoDTO.CategoriaConfigRequest.builder()
                .nome("Nova Categoria")
                .pontosMinimos(100)
                .ordem(null)
                .build();

        List<CategoriaConfig> categoriasExistentes = Arrays.asList(
                CategoriaConfig.builder().ordem(1).build(),
                CategoriaConfig.builder().ordem(2).build(),
                CategoriaConfig.builder().ordem(3).build()
        );

        when(categoriaConfigRepository.countCategoriasComConflito(any(), any(), any())).thenReturn(0L);
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc()).thenReturn(categoriasExistentes);
        when(categoriaConfigRepository.save(any(CategoriaConfig.class))).thenAnswer(invocation -> {
            CategoriaConfig categoria = invocation.getArgument(0);
            categoria.setId(4L);
            return categoria;
        });

        pontuacaoService.criarCategoria(request);

        verify(categoriaConfigRepository).save(argThat(categoria ->
                categoria.getOrdem().equals(4)
        ));
    }

    @Test
    void mataMutanteDeveSomarPontosCorretamenteUsandoMapToIntSum() {
        
        List<Cliente> clientesComPontos = Arrays.asList(
                Cliente.builder().pontos(100).ativo(true).build(),
                Cliente.builder().pontos(250).ativo(true).build(),
                Cliente.builder().pontos(350).ativo(true).build(),
                Cliente.builder().pontos(500).ativo(false).build() 
        );

        when(clienteRepository.countByAtivoTrue()).thenReturn(3L);
        when(clienteRepository.findAll()).thenReturn(clientesComPontos);
        when(historicoPontosRepository.countByPontosAdicionadosLessThan(0)).thenReturn(0L);
        when(recompensaRepository.countByAtivoTrue()).thenReturn(0L);

        PontuacaoDTO.EstatisticasPontuacaoResponse resultado = pontuacaoService.obterEstatisticas();

        assertThat(resultado.getTotalPontosAtivos()).isEqualTo(700);
    }

    @Test
    void mataMutanteDeveCalcularDivisaoCorretaParaMedia() {
        
        when(clienteRepository.countByAtivoTrue()).thenReturn(4L); 
        when(clienteRepository.findAll()).thenReturn(Arrays.asList(
                Cliente.builder().pontos(200).ativo(true).build(),
                Cliente.builder().pontos(300).ativo(true).build(),
                Cliente.builder().pontos(400).ativo(true).build(),
                Cliente.builder().pontos(500).ativo(true).build()
        ));
        when(historicoPontosRepository.countByPontosAdicionadosLessThan(0)).thenReturn(0L);
        when(recompensaRepository.countByAtivoTrue()).thenReturn(0L);

        PontuacaoDTO.EstatisticasPontuacaoResponse resultado = pontuacaoService.obterEstatisticas();

        assertThat(resultado.getMediaPontosCliente()).isEqualTo(350.0);
    }

    @Test
    void mataMutanteDeveUsarFilterCorretamenteComClienteGetAtivo() {
        
        List<Cliente> clientesMisturados = Arrays.asList(
                Cliente.builder().pontos(100).ativo(true).build(),   
                Cliente.builder().pontos(200).ativo(false).build(),  
                Cliente.builder().pontos(300).ativo(true).build(),   
                Cliente.builder().pontos(400).ativo(false).build()   
        );

        when(clienteRepository.countByAtivoTrue()).thenReturn(2L);
        when(clienteRepository.findAll()).thenReturn(clientesMisturados);
        when(historicoPontosRepository.countByPontosAdicionadosLessThan(0)).thenReturn(0L);
        when(recompensaRepository.countByAtivoTrue()).thenReturn(0L);

        PontuacaoDTO.EstatisticasPontuacaoResponse resultado = pontuacaoService.obterEstatisticas();

        assertThat(resultado.getTotalPontosAtivos()).isEqualTo(400);
    }

    @Test
    void mataMutanteDeveUsarStreamMapCorretamenteParaMapeamento() {
        
        List<CategoriaConfig> categorias = Arrays.asList(categoriaBronze, categoriaPrata);
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc()).thenReturn(categorias);

        List<PontuacaoDTO.CategoriaConfigResponse> resultado = pontuacaoService.listarCategorias();

        assertAll("Validação do mapeamento stream",
                () -> assertThat(resultado).hasSize(2),
                () -> assertThat(resultado.get(0).getNome()).isEqualTo("Bronze"),
                () -> assertThat(resultado.get(1).getNome()).isEqualTo("Prata"),
                () -> assertThat(resultado.stream().allMatch(r -> r.getId() != null)).isTrue()
        );
    }

    @Test
    void mataMutanteDeveUsarFindFirstCorretamenteNaBuscaAlternativa() {
        
        CategoriaConfig primeiraValida = CategoriaConfig.builder()
                .id(1L).nome("Primeira").pontosMinimos(0).pontosMaximos(1999).ativo(true).build();
        CategoriaConfig segundaValida = CategoriaConfig.builder()
                .id(2L).nome("Segunda").pontosMinimos(0).pontosMaximos(2999).ativo(true).build();

        when(categoriaConfigRepository.findCategoriaParaPontos(1000))
                .thenThrow(new RuntimeException("Erro"));
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(primeiraValida, segundaValida));

        Optional<CategoriaConfig> resultado = pontuacaoService.determinarCategoria(1000);

        assertAll("Validação do findFirst()",
                () -> assertThat(resultado).isPresent(),
                () -> assertThat(resultado.get().getId()).isEqualTo(1L),
                () -> assertThat(resultado.get().getNome()).isEqualTo("Primeira")
        );
    }

    @Test
    void mataMutanteDeveValidarOperadorAndNaCondicaoCategoria() {
        
        CategoriaConfig categoriaLimiteMinimo = CategoriaConfig.builder()
                .nome("Limite Mínimo")
                .pontosMinimos(1500)  
                .pontosMaximos(3000)  
                .ativo(true)
                .build();

        when(categoriaConfigRepository.findCategoriaParaPontos(1000))
                .thenThrow(new RuntimeException("Erro"));
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(categoriaLimiteMinimo));

        Optional<CategoriaConfig> resultado = pontuacaoService.determinarCategoria(1000);

        assertThat(resultado).isEmpty();
    }

    @Test
    void mataMutanteDeveValidarOperadorOrNaCondicaoPontosMaximos() {
        
        CategoriaConfig categoriaComLimite = CategoriaConfig.builder()
                .nome("Com Limite")
                .pontosMinimos(500)
                .pontosMaximos(1500) 
                .ativo(true)
                .build();

        when(categoriaConfigRepository.findCategoriaParaPontos(1000))
                .thenThrow(new RuntimeException("Erro"));
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(categoriaComLimite));

        Optional<CategoriaConfig> resultado = pontuacaoService.determinarCategoria(1000);

        assertAll("Validação do operador OR",
                () -> assertThat(resultado).isPresent(),
                () -> assertThat(resultado.get().getNome()).isEqualTo("Com Limite")
        );
    }

    @Test
    void mataMutanteDeveRetorarOptionalEmptyQuandoNaoEncontraCategoria() {
        
        when(categoriaConfigRepository.findCategoriaParaPontos(-100))
                .thenThrow(new RuntimeException("Erro"));
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Arrays.asList(categoriaBronze, categoriaPrata)); 

        Optional<CategoriaConfig> resultado = pontuacaoService.determinarCategoria(-100);

        assertAll("Validação do retorno Optional.empty()",
                () -> assertThat(resultado).isNotNull(),
                () -> assertThat(resultado.isEmpty()).isTrue(),
                () -> assertThat(resultado.isPresent()).isFalse()
        );
    }

    @Test
    void mataMutanteDeveRetorarListaVaziaQuandoNaoHaCategorias() {
        
        when(categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc())
                .thenReturn(Collections.emptyList());

        List<PontuacaoDTO.CategoriaConfigResponse> resultado = pontuacaoService.listarCategorias();

        assertAll("Validação da lista vazia",
                () -> assertThat(resultado).isNotNull(),
                () -> assertThat(resultado).isEmpty(),
                () -> assertThat(resultado.size()).isEqualTo(0)
        );
    }

    @Test
    void mataMutanteDeveConvertereIntParaDoubleCorretamenteNaMedia() {
        
        when(clienteRepository.countByAtivoTrue()).thenReturn(3L);
        when(clienteRepository.findAll()).thenReturn(Arrays.asList(
                Cliente.builder().pontos(100).ativo(true).build(), 
                Cliente.builder().pontos(200).ativo(true).build(), 
                Cliente.builder().pontos(300).ativo(true).build()  
        ));
        when(historicoPontosRepository.countByPontosAdicionadosLessThan(0)).thenReturn(0L);
        when(recompensaRepository.countByAtivoTrue()).thenReturn(0L);

        PontuacaoDTO.EstatisticasPontuacaoResponse resultado = pontuacaoService.obterEstatisticas();

        assertAll("Validação da conversão para double",
                () -> assertThat(resultado.getMediaPontosCliente()).isInstanceOf(Double.class),
                () -> assertThat(resultado.getMediaPontosCliente()).isEqualTo(200.0),
                () -> assertThat(resultado.getTotalPontosAtivos()).isInstanceOf(Integer.class)
        );
    }

    @Test
    void mataMutanteDeveUsarOrElseCorretamenteParaCategoriaAusente() {
        
        Page<Cliente> clientes = new PageImpl<>(Arrays.asList(clienteMock));
        
        when(clienteRepository.findByAtivoTrueOrderByPontosDesc(any())).thenReturn(clientes);
        when(categoriaConfigRepository.findCategoriaParaPontos(1000))
                .thenReturn(Optional.empty()); 
        when(categoriaConfigRepository.findProximaCategoria(1000))
                .thenReturn(Optional.empty()); 

        Page<PontuacaoDTO.ClientePontuacaoResponse> resultado = 
                pontuacaoService.listarClientesComPontuacao(PageRequest.of(0, 10));

        PontuacaoDTO.ClientePontuacaoResponse cliente = resultado.getContent().get(0);
        assertAll("Validação dos valores padrão",
                () -> assertThat(cliente.getCategoriaAtual()).isEqualTo("Sem categoria"),
                () -> assertThat(cliente.getCorCategoria()).isEqualTo("#999999"),
                () -> assertThat(cliente.getProximaCategoria()).isNull(),
                () -> assertThat(cliente.getPontosProximaCategoria()).isNull()
        );
    }

    @Test
    void mataMutanteDeveUsarMapSeguidoDeOrElseCorretamente() {
        
        Page<Cliente> clientes = new PageImpl<>(Arrays.asList(clienteMock));
        
        when(clienteRepository.findByAtivoTrueOrderByPontosDesc(any())).thenReturn(clientes);
        when(categoriaConfigRepository.findCategoriaParaPontos(1000))
                .thenReturn(Optional.of(categoriaPrata));
        when(categoriaConfigRepository.findProximaCategoria(1000))
                .thenReturn(Optional.of(CategoriaConfig.builder()
                        .nome("Ouro")
                        .pontosMinimos(3000)
                        .cor("#FFD700")
                        .build()));

        Page<PontuacaoDTO.ClientePontuacaoResponse> resultado = 
                pontuacaoService.listarClientesComPontuacao(PageRequest.of(0, 10));

        PontuacaoDTO.ClientePontuacaoResponse cliente = resultado.getContent().get(0);
        assertAll("Validação dos valores mapeados",
                () -> assertThat(cliente.getCategoriaAtual()).isEqualTo("Prata"),
                () -> assertThat(cliente.getProximaCategoria()).isEqualTo("Ouro"),
                () -> assertThat(cliente.getPontosProximaCategoria()).isEqualTo(3000)
        );
    }

    @Test
    void mataMutanteDeveValidarCondicoesAninhadasNoRecalculoCategorias() {
        
        Cliente clienteParaRecalculo = Cliente.builder()
                .id(2L)
                .nome("Cliente Recálculo")
                .pontos(1500)
                .build();

        when(clienteRepository.findAll()).thenReturn(Arrays.asList(clienteParaRecalculo));
        when(categoriaConfigRepository.findCategoriaParaPontos(1500))
                .thenReturn(Optional.of(categoriaPrata));

        pontuacaoService.recalcularCategoriasClientes();

        verify(categoriaConfigRepository).findCategoriaParaPontos(1500);
    }

    @Test
    void mataMutanteDeveProcessarLoopCorretamenteParaTodosClientes() {
        
        List<Cliente> variosClientes = Arrays.asList(
                Cliente.builder().id(1L).pontos(100).build(),
                Cliente.builder().id(2L).pontos(1000).build(),
                Cliente.builder().id(3L).pontos(2000).build()
        );

        when(clienteRepository.findAll()).thenReturn(variosClientes);
        when(categoriaConfigRepository.findCategoriaParaPontos(100))
                .thenReturn(Optional.of(categoriaBronze));
        when(categoriaConfigRepository.findCategoriaParaPontos(1000))
                .thenReturn(Optional.of(categoriaPrata));
        when(categoriaConfigRepository.findCategoriaParaPontos(2000))
                .thenReturn(Optional.of(categoriaPrata));

        pontuacaoService.recalcularCategoriasClientes();

        verify(categoriaConfigRepository).findCategoriaParaPontos(100);
        verify(categoriaConfigRepository).findCategoriaParaPontos(1000);
        verify(categoriaConfigRepository).findCategoriaParaPontos(2000);
    }
}
