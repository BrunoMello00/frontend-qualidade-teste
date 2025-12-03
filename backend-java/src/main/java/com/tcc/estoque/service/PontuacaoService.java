package com.tcc.estoque.service;

import com.tcc.estoque.model.*;
import com.tcc.estoque.repository.*;
import com.tcc.estoque.dto.PontuacaoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PontuacaoService {

    private final ClienteRepository clienteRepository;
    private final CategoriaConfigRepository categoriaConfigRepository;
    private final RecompensaRepository recompensaRepository;
    private final HistoricoPontosRepository historicoPontosRepository;

    // ========== GESTÃO DE CATEGORIAS ==========

    @Transactional
    public PontuacaoDTO.CategoriaConfigResponse criarCategoria(PontuacaoDTO.CategoriaConfigRequest request) {
        long conflitos = categoriaConfigRepository.countCategoriasComConflito(
            request.getPontosMinimos(), 
            request.getPontosMaximos(), 
            null
        );
        
        if (conflitos > 0) {
            throw new IllegalArgumentException("Já existe uma categoria com conflito no range de pontos especificado");
        }

        if (request.getOrdem() == null) {
            List<CategoriaConfig> categorias = categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc();
            request.setOrdem(categorias.size() + 1);
        }

        CategoriaConfig categoria = CategoriaConfig.builder()
            .nome(request.getNome())
            .descricao(request.getDescricao())
            .pontosMinimos(request.getPontosMinimos())
            .pontosMaximos(request.getPontosMaximos())
            .pontosIniciais(request.getPontosIniciais())
            .cor(request.getCor())
            .ativo(true)
            .ordem(request.getOrdem())
            .build();

        categoria = categoriaConfigRepository.save(categoria);
        return mapearParaCategoriaResponse(categoria);
    }

    @Transactional
    public PontuacaoDTO.CategoriaConfigResponse atualizarCategoria(Long id, PontuacaoDTO.CategoriaConfigRequest request) {
        CategoriaConfig categoria = categoriaConfigRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        long conflitos = categoriaConfigRepository.countCategoriasComConflito(
            request.getPontosMinimos(), 
            request.getPontosMaximos(), 
            id
        );
        
        if (conflitos > 0) {
            throw new IllegalArgumentException("Já existe uma categoria com conflito no range de pontos especificado");
        }

        categoria.setNome(request.getNome());
        categoria.setDescricao(request.getDescricao());
        categoria.setPontosMinimos(request.getPontosMinimos());
        categoria.setPontosMaximos(request.getPontosMaximos());
        categoria.setPontosIniciais(request.getPontosIniciais());
        categoria.setCor(request.getCor());
        
        if (request.getOrdem() != null) {
            categoria.setOrdem(request.getOrdem());
        }

        categoria = categoriaConfigRepository.save(categoria);
        
        recalcularCategoriasClientes();
        
        return mapearParaCategoriaResponse(categoria);
    }

    @Transactional
    public void removerCategoria(Long id) {
        CategoriaConfig categoria = categoriaConfigRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        categoria.setAtivo(false);
        categoriaConfigRepository.save(categoria);
        
        recalcularCategoriasClientes();
    }

    public List<PontuacaoDTO.CategoriaConfigResponse> listarCategorias() {
        List<CategoriaConfig> categorias = categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc();
        return categorias.stream()
            .map(this::mapearParaCategoriaResponse)
            .toList();
    }

    public PontuacaoDTO.CategoriaConfigResponse buscarCategoriaPorId(Long id) {
        CategoriaConfig categoria = categoriaConfigRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
        return mapearParaCategoriaResponse(categoria);
    }

    /**
     * Determina a categoria baseada nos pontos do cliente
     */
    public Optional<CategoriaConfig> determinarCategoria(int pontos) {
        try {
            return categoriaConfigRepository.findCategoriaParaPontos(pontos);
        } catch (Exception e) {
            log.warn("Erro ao determinar categoria para {} pontos. Usando primeira categoria disponível", pontos, e);
            List<CategoriaConfig> categorias = categoriaConfigRepository.findByAtivoTrueOrderByOrdemAsc();
            return categorias.stream()
                .filter(c -> c.getPontosMinimos() <= pontos && 
                           (c.getPontosMaximos() == null || c.getPontosMaximos() >= pontos))
                .findFirst();
        }
    }

    /**
     * Busca a próxima categoria na progressão
     */
    public Optional<CategoriaConfig> buscarProximaCategoria(int pontosAtuais) {
        return categoriaConfigRepository.findProximaCategoria(pontosAtuais);
    }

    // ========== GESTÃO DE RECOMPENSAS ==========

    @Transactional
    public PontuacaoDTO.RecompensaResponse criarRecompensa(PontuacaoDTO.RecompensaRequest request) {
        Recompensa recompensa = Recompensa.builder()
            .nome(request.getNome())
            .descricao(request.getDescricao())
            .pontosNecessarios(request.getPontosNecessarios())
            .categoria(request.getCategoria())
            .valorDesconto(request.getValorDesconto())
            .percentualDesconto(request.getPercentualDesconto())
            .quantidadeDisponivel(request.getQuantidadeDisponivel())
            .dataValidade(request.getDataValidade())
            .ativo(true)
            .build();

        recompensa = recompensaRepository.save(recompensa);
        return mapearParaRecompensaResponse(recompensa);
    }

    @Transactional
    public PontuacaoDTO.RecompensaResponse atualizarRecompensa(Long id, PontuacaoDTO.RecompensaRequest request) {
        Recompensa recompensa = recompensaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recompensa não encontrada"));

        recompensa.setNome(request.getNome());
        recompensa.setDescricao(request.getDescricao());
        recompensa.setPontosNecessarios(request.getPontosNecessarios());
        recompensa.setCategoria(request.getCategoria());
        recompensa.setValorDesconto(request.getValorDesconto());
        recompensa.setPercentualDesconto(request.getPercentualDesconto());
        recompensa.setQuantidadeDisponivel(request.getQuantidadeDisponivel());
        recompensa.setDataValidade(request.getDataValidade());

        recompensa = recompensaRepository.save(recompensa);
        return mapearParaRecompensaResponse(recompensa);
    }

    @Transactional
    public void removerRecompensa(Long id) {
        Recompensa recompensa = recompensaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recompensa não encontrada"));
        
        recompensa.setAtivo(false);
        recompensaRepository.save(recompensa);
    }

    public List<PontuacaoDTO.RecompensaResponse> listarRecompensas() {
        List<Recompensa> recompensas = recompensaRepository.findByAtivoTrueOrderByPontosNecessariosAsc();
        return recompensas.stream()
            .map(this::mapearParaRecompensaResponse)
            .toList();
    }

    public List<PontuacaoDTO.RecompensaResponse> listarRecompensasDisponiveis(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

        List<Recompensa> recompensas = recompensaRepository.findRecompensasDisponiveis(cliente.getPontos());
        return recompensas.stream()
            .map(this::mapearParaRecompensaResponse)
            .toList();
    }

    // ========== GESTÃO DE PONTOS ==========

    public Page<PontuacaoDTO.ClientePontuacaoResponse> listarClientesComPontuacao(Pageable pageable) {
        Page<Cliente> clientes = clienteRepository.findByAtivoTrueOrderByPontosDesc(pageable);
        return clientes.map(this::mapearParaClientePontuacaoResponse);
    }

    public PontuacaoDTO.EstatisticasPontuacaoResponse obterEstatisticas() {
        long totalClientes = clienteRepository.countByAtivoTrue();
        
        Integer totalPontos = clienteRepository.findAll().stream()
            .filter(Cliente::getAtivo)
            .mapToInt(Cliente::getPontos)
            .sum();

        double mediaPontos = totalClientes > 0 ? (double) totalPontos / totalClientes : 0;

        long totalResgates = historicoPontosRepository.countByPontosAdicionadosLessThan(0);

        return PontuacaoDTO.EstatisticasPontuacaoResponse.builder()
            .totalClientes(totalClientes)
            .totalPontosAtivos(totalPontos)
            .mediaPontosCliente(mediaPontos)
            .totalResgates(totalResgates)
            .totalRecompensasAtivas(recompensaRepository.countByAtivoTrue())
            .build();
    }

    @Transactional
    public void recalcularCategoriasClientes() {
        List<Cliente> clientes = clienteRepository.findAll();
        
        for (Cliente cliente : clientes) {
            Optional<CategoriaConfig> novaCategoria = determinarCategoria(cliente.getPontos());
            
            if (novaCategoria.isPresent()) {
                log.info("Cliente {} categoria atualizada para {}", 
                    cliente.getNome(), novaCategoria.get().getNome());
            }
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private PontuacaoDTO.CategoriaConfigResponse mapearParaCategoriaResponse(CategoriaConfig categoria) {
        return PontuacaoDTO.CategoriaConfigResponse.builder()
            .id(categoria.getId())
            .nome(categoria.getNome())
            .descricao(categoria.getDescricao())
            .pontosMinimos(categoria.getPontosMinimos())
            .pontosMaximos(categoria.getPontosMaximos())
            .pontosIniciais(categoria.getPontosIniciais())
            .cor(categoria.getCor())
            .ativo(categoria.getAtivo())
            .ordem(categoria.getOrdem())
            .dataCriacao(categoria.getDataCriacao())
            .dataAtualizacao(categoria.getDataAtualizacao())
            .build();
    }

    private PontuacaoDTO.RecompensaResponse mapearParaRecompensaResponse(Recompensa recompensa) {
        return PontuacaoDTO.RecompensaResponse.builder()
            .id(recompensa.getId())
            .nome(recompensa.getNome())
            .descricao(recompensa.getDescricao())
            .pontosNecessarios(recompensa.getPontosNecessarios())
            .categoria(recompensa.getCategoria())
            .valorDesconto(recompensa.getValorDesconto())
            .percentualDesconto(recompensa.getPercentualDesconto())
            .ativo(recompensa.getAtivo())
            .quantidadeDisponivel(recompensa.getQuantidadeDisponivel())
            .quantidadeResgatada(recompensa.getQuantidadeResgatada())
            .dataValidade(recompensa.getDataValidade())
            .dataCriacao(recompensa.getDataCriacao())
            .disponivel(recompensa.isDisponivel())
            .temEstoque(recompensa.temEstoque())
            .build();
    }

    private PontuacaoDTO.ClientePontuacaoResponse mapearParaClientePontuacaoResponse(Cliente cliente) {
        Optional<CategoriaConfig> categoriaAtual = determinarCategoria(cliente.getPontos());
        Optional<CategoriaConfig> proximaCategoria = buscarProximaCategoria(cliente.getPontos());

        return PontuacaoDTO.ClientePontuacaoResponse.builder()
            .id(cliente.getId())
            .nome(cliente.getNome())
            .email(cliente.getEmail())
            .pontos(cliente.getPontos())
            .categoriaAtual(categoriaAtual.map(CategoriaConfig::getNome).orElse("Sem categoria"))
            .corCategoria(categoriaAtual.map(CategoriaConfig::getCor).orElse("#999999"))
            .proximaCategoria(proximaCategoria.map(CategoriaConfig::getNome).orElse(null))
            .pontosProximaCategoria(proximaCategoria.map(CategoriaConfig::getPontosMinimos).orElse(null))
            .totalCompras(cliente.getTotalCompras())
            .quantidadeCompras(cliente.getQuantidadeCompras())
            .ultimaCompra(cliente.getUltimaCompra())
            .build();
    }
}