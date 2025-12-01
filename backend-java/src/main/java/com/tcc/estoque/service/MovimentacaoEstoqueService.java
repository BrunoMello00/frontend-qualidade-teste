package com.tcc.estoque.service;

import com.tcc.estoque.dto.MovimentacaoEstoqueDTO;
import com.tcc.estoque.model.MovimentacaoEstoque;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.TipoMovimentacao;
import com.tcc.estoque.repository.MovimentacaoEstoqueRepository;
import com.tcc.estoque.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela gestão de movimentações de estoque
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ProdutoRepository produtoRepository;

    /**
     * Lista movimentações com paginação e filtros
     */
    public Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> listarMovimentacoes(
            Long produtoId,
            TipoMovimentacao tipo,
            LocalDate dataInicio,
            LocalDate dataFim,
            Pageable pageable) {
        
        log.debug("Listando movimentações - Produto: {}, Tipo: {}, Período: {} a {}", 
                produtoId, tipo, dataInicio, dataFim);

        LocalDateTime dataInicioTime = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime dataFimTime = dataFim != null ? dataFim.atTime(23, 59, 59) : null;

        Page<MovimentacaoEstoque> movimentacoes = movimentacaoEstoqueRepository
                .findMovimentacoesComFiltros(produtoId, tipo, dataInicioTime, dataFimTime, pageable);

        return movimentacoes.map(this::convertToMovimentacaoResponse);
    }

    /**
     * Registra entrada de estoque
     */
    @Transactional
    public MovimentacaoEstoqueDTO.MovimentacaoResponse registrarEntrada(
            MovimentacaoEstoqueDTO.EntradaEstoqueRequest request, Usuario usuario) {
        
        log.info("Registrando entrada de estoque - Produto: {}, Quantidade: {}", 
                request.getProdutoId(), request.getQuantidade());

        Produto produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        Integer quantidadeAnterior = produto.getEstoque();
        Integer novaQuantidade = quantidadeAnterior + request.getQuantidade();

        produto.setEstoque(novaQuantidade);
        produtoRepository.save(produto);

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(produto)
                .usuario(usuario)
                .tipo(TipoMovimentacao.ENTRADA)
                .quantidade(request.getQuantidade())
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(novaQuantidade)
                .motivo(request.getMotivo())
                .observacoes(request.getObservacoes())
                .dataMovimentacao(LocalDateTime.now())
                .build();

        movimentacao = movimentacaoEstoqueRepository.save(movimentacao);

        log.info("Entrada registrada com sucesso. ID: {}, Produto: {}, Nova quantidade: {}", 
                movimentacao.getId(), produto.getNome(), novaQuantidade);

        return convertToMovimentacaoResponse(movimentacao);
    }

    /**
     * Registra saída de estoque
     */
    @Transactional
    public MovimentacaoEstoqueDTO.MovimentacaoResponse registrarSaida(
            MovimentacaoEstoqueDTO.SaidaEstoqueRequest request, Usuario usuario) {
        
        log.info("Registrando saída de estoque - Produto: {}, Quantidade: {}", 
                request.getProdutoId(), request.getQuantidade());

        Produto produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        Integer quantidadeAnterior = produto.getEstoque();

        if (quantidadeAnterior < request.getQuantidade()) {
            throw new RuntimeException(
                    String.format("Estoque insuficiente. Disponível: %d, Solicitado: %d", 
                            quantidadeAnterior, request.getQuantidade()));
        }

        Integer novaQuantidade = quantidadeAnterior - request.getQuantidade();

        produto.setEstoque(novaQuantidade);
        produtoRepository.save(produto);

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(produto)
                .usuario(usuario)
                .tipo(TipoMovimentacao.SAIDA)
                .quantidade(request.getQuantidade())
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(novaQuantidade)
                .motivo(request.getMotivo())
                .observacoes(request.getObservacoes())
                .dataMovimentacao(LocalDateTime.now())
                .build();

        movimentacao = movimentacaoEstoqueRepository.save(movimentacao);

        log.info("Saída registrada com sucesso. ID: {}, Produto: {}, Nova quantidade: {}", 
                movimentacao.getId(), produto.getNome(), novaQuantidade);

        return convertToMovimentacaoResponse(movimentacao);
    }

    /**
     * Registra ajuste de estoque
     */
    @Transactional
    public MovimentacaoEstoqueDTO.MovimentacaoResponse registrarAjuste(
            MovimentacaoEstoqueDTO.AjusteEstoqueRequest request, Usuario usuario) {
        
        log.info("Registrando ajuste de estoque - Produto: {}, Nova quantidade: {}", 
                request.getProdutoId(), request.getNovaQuantidade());

        Produto produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        Integer quantidadeAnterior = produto.getEstoque();
        Integer novaQuantidade = request.getNovaQuantidade();
        Integer diferenca = novaQuantidade - quantidadeAnterior;

        if (diferenca == 0) {
            throw new RuntimeException("A nova quantidade é igual à quantidade atual");
        }

        produto.setEstoque(novaQuantidade);
        produtoRepository.save(produto);

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(produto)
                .usuario(usuario)
                .tipo(TipoMovimentacao.AJUSTE)
                .quantidade(Math.abs(diferenca))
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(novaQuantidade)
                .motivo(request.getMotivo())
                .observacoes(request.getObservacoes())
                .dataMovimentacao(LocalDateTime.now())
                .build();

        movimentacao = movimentacaoEstoqueRepository.save(movimentacao);

        log.info("Ajuste registrado com sucesso. ID: {}, Produto: {}, Diferença: {}", 
                movimentacao.getId(), produto.getNome(), diferenca);

        return convertToMovimentacaoResponse(movimentacao);
    }

    /**
     * Busca movimentações por produto
     */
    public Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> buscarMovimentacoesPorProduto(
            Long produtoId, Pageable pageable) {
        
        log.debug("Buscando movimentações do produto: {}", produtoId);

        Page<MovimentacaoEstoque> movimentacoes = movimentacaoEstoqueRepository
                .findByProdutoIdOrderByDataMovimentacaoDesc(produtoId, pageable);

        return movimentacoes.map(this::convertToMovimentacaoResponse);
    }

    /**
     * Busca movimentações por período
     */
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO.MovimentacaoResponse> buscarMovimentacoesPorPeriodo(
            LocalDate dataInicio, LocalDate dataFim) {
        
        log.debug("Buscando movimentações no período: {} a {}", dataInicio, dataFim);

        LocalDateTime dataInicioTime = dataInicio.atStartOfDay();
        LocalDateTime dataFimTime = dataFim.atTime(23, 59, 59);

        List<MovimentacaoEstoque> movimentacoes = movimentacaoEstoqueRepository
                .findMovimentacoesPorPeriodo(dataInicioTime, dataFimTime);

        return movimentacoes.stream()
                .map(this::convertToMovimentacaoResponse)
                .collect(Collectors.toList());
    }

    /**
     * Busca últimas movimentações
     */
    public Page<MovimentacaoEstoqueDTO.MovimentacaoResponse> buscarUltimasMovimentacoes(Pageable pageable) {
        try {
            log.debug("Buscando últimas movimentações");

            Page<MovimentacaoEstoque> movimentacoes = movimentacaoEstoqueRepository
                    .findUltimasMovimentacoes(pageable);

            return movimentacoes.map(this::convertToMovimentacaoResponse);
        } catch (Exception e) {
            log.error("Erro ao buscar últimas movimentações: {}", e.getMessage(), e);
            
            return Page.empty(pageable);
        }
    }

    /**
     * Obtém estatísticas de movimentação
     */
    public MovimentacaoEstoqueDTO.EstatisticasMovimentacaoResponse obterEstatisticasMovimentacao(
            LocalDate dataInicio, LocalDate dataFim) {
        
        log.debug("Obtendo estatísticas de movimentação para período: {} a {}", dataInicio, dataFim);

        LocalDateTime dataInicioTime = dataInicio.atStartOfDay();
        LocalDateTime dataFimTime = dataFim.atTime(23, 59, 59);

        List<MovimentacaoEstoque> movimentacoes = movimentacaoEstoqueRepository
                .findMovimentacoesPorPeriodo(dataInicioTime, dataFimTime);

        long totalMovimentacoes = movimentacoes.size();
        long totalEntradas = movimentacoes.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.ENTRADA)
                .count();
        long totalSaidas = movimentacoes.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.SAIDA)
                .count();
        long totalAjustes = movimentacoes.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.AJUSTE)
                .count();

        Integer quantidadeEntradas = movimentacoes.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.ENTRADA)
                .mapToInt(MovimentacaoEstoque::getQuantidade)
                .sum();

        Integer quantidadeSaidas = movimentacoes.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.SAIDA)
                .mapToInt(MovimentacaoEstoque::getQuantidade)
                .sum();

        LocalDateTime inicioDia = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime fimDia = inicioDia.plusDays(1);
        Long movimentacoesHoje = movimentacaoEstoqueRepository.countMovimentacoesHoje(inicioDia, fimDia);

        return MovimentacaoEstoqueDTO.EstatisticasMovimentacaoResponse.builder()
                .totalMovimentacoes(totalMovimentacoes)
                .totalEntradas(totalEntradas)
                .totalSaidas(totalSaidas)
                .totalAjustes(totalAjustes)
                .quantidadeEntradas(quantidadeEntradas)
                .quantidadeSaidas(quantidadeSaidas)
                .saldoMovimentacao(quantidadeEntradas - quantidadeSaidas)
                .movimentacoesHoje(movimentacoesHoje)
                .build();
    }


    private MovimentacaoEstoqueDTO.MovimentacaoResponse convertToMovimentacaoResponse(MovimentacaoEstoque movimentacao) {
        return MovimentacaoEstoqueDTO.MovimentacaoResponse.builder()
                .id(movimentacao.getId())
                .produtoId(movimentacao.getProduto().getId())
                .nomeProduto(movimentacao.getProduto().getNome())
                .codigoProduto(movimentacao.getProduto().getCodigo())
                .tipo(movimentacao.getTipo())
                .quantidade(movimentacao.getQuantidade())
                .quantidadeAnterior(movimentacao.getQuantidadeAnterior())
                .quantidadeAtual(movimentacao.getQuantidadeAtual())
                .motivo(movimentacao.getMotivo())
                .observacoes(movimentacao.getObservacoes())
                .dataMovimentacao(movimentacao.getDataMovimentacao())
                .nomeUsuario(movimentacao.getUsuario().getNome())
                .emailUsuario(movimentacao.getUsuario().getEmail())
                .vendaId(movimentacao.getVenda() != null ? movimentacao.getVenda().getId() : null)
                .build();
    }
}
