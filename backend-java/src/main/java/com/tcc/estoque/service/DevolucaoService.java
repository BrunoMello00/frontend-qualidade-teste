package com.tcc.estoque.service;

import com.tcc.estoque.dto.DevolucaoDTO;
import com.tcc.estoque.model.*;
import com.tcc.estoque.model.enums.StatusQualidade;
import com.tcc.estoque.model.enums.TipoDevolucao;
import com.tcc.estoque.repository.*;
import com.tcc.estoque.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DevolucaoService {

    private final DevolucaoRepository devolucaoRepository;
    private final ItemDevolucaoRepository itemDevolucaoRepository;
    private final VendaRepository vendaRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final ProdutoRepository produtoRepository;
    private final SecurityUtil securityUtil;

    public DevolucaoDTO.DevolucaoResponse processarDevolucao(DevolucaoDTO.DevolucaoRequest request) {
        log.info("Processando devolução para venda ID: {}", request.getVendaId());

        // Buscar venda
        Venda venda = vendaRepository.findById(request.getVendaId())
                .orElseThrow(() -> new RuntimeException("Venda não encontrada"));

        // Validar se venda pode ser devolvida
        if (!venda.isConfirmada()) {
            throw new RuntimeException("Apenas vendas confirmadas podem ser devolvidas");
        }

        // Obter usuário atual
        Usuario usuario = securityUtil.getUsuarioLogado();

        // Criar devolução
        Devolucao devolucao = Devolucao.builder()
                .venda(venda)
                .usuario(usuario)
                .tipoDevolucao(request.getTipoDevolucao())
                .motivo(request.getMotivo())
                .observacoes(request.getObservacoes())
                .descontoAplicado(request.getDescontoAplicado())
                .valorDevolucao(BigDecimal.ZERO) // Inicializar com zero para evitar erro de validação
                .build();

        devolucao = devolucaoRepository.save(devolucao);

        // Processar itens da devolução
        for (DevolucaoDTO.ItemDevolucaoRequest itemRequest : request.getItens()) {
            processarItemDevolucao(devolucao, itemRequest);
        }

        // Recalcular valor total
        devolucao.calcularValorDevolucao();
        devolucao = devolucaoRepository.save(devolucao);

        log.info("Devolução processada com sucesso. ID: {}", devolucao.getId());
        return converterParaResponse(devolucao);
    }

    private void processarItemDevolucao(Devolucao devolucao, DevolucaoDTO.ItemDevolucaoRequest itemRequest) {
        // Buscar item de venda
        ItemVenda itemVenda = itemVendaRepository.findById(itemRequest.getItemVendaId())
                .orElseThrow(() -> new RuntimeException("Item de venda não encontrado"));

        // Validar quantidade
        if (itemRequest.getQuantidade() > itemVenda.getQuantidade()) {
            throw new RuntimeException("Quantidade de devolução não pode ser maior que a vendida");
        }

        // Criar item de devolução
        ItemDevolucao itemDevolucao = ItemDevolucao.builder()
                .devolucao(devolucao)
                .itemVenda(itemVenda)
                .produto(itemVenda.getProduto())
                .nomeProduto(itemVenda.getNomeProduto())
                .tamanho(itemVenda.getTamanho())
                .quantidade(itemRequest.getQuantidade())
                .precoUnitarioOriginal(itemVenda.getPrecoUnitario())
                .statusQualidadeRetorno(itemRequest.getStatusQualidadeRetorno())
                .descontoAplicado(itemRequest.getDescontoAplicado())
                .observacoes(itemRequest.getObservacoes())
                .build();

        itemDevolucao = itemDevolucaoRepository.save(itemDevolucao);

        // Atualizar estoque
        atualizarEstoqueAposDevolucao(itemDevolucao);

        devolucao.adicionarItem(itemDevolucao);
    }

    private void atualizarEstoqueAposDevolucao(ItemDevolucao itemDevolucao) {
        Produto produto = itemDevolucao.getProduto();
        Integer quantidade = itemDevolucao.getQuantidade();

        // Devolver ao estoque
        produto.setEstoque(produto.getEstoque() + quantidade);

        // Se produto voltou com defeito, ajustar preço
        if (itemDevolucao.getStatusQualidadeRetorno() == StatusQualidade.DEFEITUOSO) {
            BigDecimal precoComDesconto;
            if (itemDevolucao.getDescontoAplicado() != null) {
                // Calcular desconto com precisão adequada
                BigDecimal desconto = produto.getPreco()
                    .multiply(itemDevolucao.getDescontoAplicado())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                precoComDesconto = produto.getPreco().subtract(desconto);
            } else {
                // 30% de desconto padrão - calculado com precisão
                BigDecimal desconto = produto.getPreco()
                    .multiply(BigDecimal.valueOf(30))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                precoComDesconto = produto.getPreco().subtract(desconto);
            }
            
            // Garantir que o preço não ultrapasse os limites de validação (8 dígitos inteiros, 2 decimais)
            if (precoComDesconto.precision() - precoComDesconto.scale() > 8) {
                log.warn("Preço defeituoso calculado ({}) excede limite de precisão, ajustando para valor máximo", precoComDesconto);
                precoComDesconto = new BigDecimal("99999999.99");
            }
            
            produto.marcarComoDefeituoso(precoComDesconto);
            log.info("Produto ID {} marcado como defeituoso com preço {}", produto.getId(), produto.getPrecoDefeituoso());
        } else if (itemDevolucao.getStatusQualidadeRetorno() == StatusQualidade.INDISPONIVEL) {
            produto.marcarComoIndisponivel();
            log.info("Produto ID {} marcado como indisponível", produto.getId());
        } else {
            // Produto volta normal
            if (produto.getStatusQualidade() != StatusQualidade.NORMAL) {
                produto.marcarComoNormal();
            }
        }

        produtoRepository.save(produto);
        log.info("Estoque atualizado. Produto ID: {}, Nova quantidade: {}, Status: {}", 
                produto.getId(), produto.getEstoque(), produto.getStatusQualidade());
    }

    @Transactional(readOnly = true)
    public Page<DevolucaoDTO.DevolucaoResumo> buscarDevolucoes(Pageable pageable) {
        return devolucaoRepository.findAll(pageable)
                .map(this::converterParaResumo);
    }

    @Transactional(readOnly = true)
    public DevolucaoDTO.DevolucaoResponse buscarPorId(Long id) {
        Devolucao devolucao = devolucaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devolução não encontrada"));
        return converterParaResponse(devolucao);
    }

    @Transactional(readOnly = true)
    public List<DevolucaoDTO.DevolucaoResponse> buscarPorVenda(Long vendaId) {
        Venda venda = vendaRepository.findById(vendaId)
                .orElseThrow(() -> new RuntimeException("Venda não encontrada"));
        
        return devolucaoRepository.findByVenda(venda)
                .stream()
                .map(this::converterParaResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DevolucaoDTO.EstatisticasDevolucao obterEstatisticas(LocalDateTime inicio, LocalDateTime fim) {
        List<Devolucao> devolucoes = devolucaoRepository.findByPeriodo(inicio, fim);

        return DevolucaoDTO.EstatisticasDevolucao.builder()
                .totalDevolucoes((long) devolucoes.size())
                .devolucoesSimples(devolucoes.stream()
                        .filter(d -> d.getTipoDevolucao() == TipoDevolucao.DEVOLUCAO_SIMPLES)
                        .count())
                .trocasPorDefeito(devolucoes.stream()
                        .filter(d -> d.getTipoDevolucao() == TipoDevolucao.TROCA_POR_DEFEITO)
                        .count())
                .trocasPorTamanho(devolucoes.stream()
                        .filter(d -> d.getTipoDevolucao() == TipoDevolucao.TROCA_POR_TAMANHO)
                        .count())
                .valorTotalDevolvido(devolucoes.stream()
                        .map(Devolucao::getValorDevolucao)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .produtosDefeituosos(itemDevolucaoRepository.findByStatusQualidadeRetorno(StatusQualidade.DEFEITUOSO).size())
                .build();
    }

    private DevolucaoDTO.DevolucaoResponse converterParaResponse(Devolucao devolucao) {
        return DevolucaoDTO.DevolucaoResponse.builder()
                .id(devolucao.getId())
                .vendaId(devolucao.getVenda().getId())
                .vendaNumero("V" + devolucao.getVenda().getId())
                .clienteNome(devolucao.getVenda().getClienteNome())
                .tipoDevolucao(devolucao.getTipoDevolucao())
                .motivo(devolucao.getMotivo())
                .observacoes(devolucao.getObservacoes())
                .valorDevolucao(devolucao.getValorDevolucao())
                .descontoAplicado(devolucao.getDescontoAplicado())
                .dataDevolucao(devolucao.getDataDevolucao())
                .usuarioResponsavel(devolucao.getUsuario().getNome())
                .itens(devolucao.getItens().stream()
                        .map(this::converterItemParaResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private DevolucaoDTO.ItemDevolucaoResponse converterItemParaResponse(ItemDevolucao item) {
        return DevolucaoDTO.ItemDevolucaoResponse.builder()
                .id(item.getId())
                .produtoId(item.getProduto().getId())
                .nomeProduto(item.getNomeProduto())
                .tamanho(item.getTamanho())
                .quantidade(item.getQuantidade())
                .precoUnitarioOriginal(item.getPrecoUnitarioOriginal())
                .valorItem(item.getValorItem())
                .statusQualidadeRetorno(item.getStatusQualidadeRetorno())
                .descontoAplicado(item.getDescontoAplicado())
                .precoRevenda(item.getPrecoRevenda())
                .observacoes(item.getObservacoes())
                .dataDevolucao(item.getCreatedAt())
                .build();
    }

    private DevolucaoDTO.DevolucaoResumo converterParaResumo(Devolucao devolucao) {
        return DevolucaoDTO.DevolucaoResumo.builder()
                .id(devolucao.getId())
                .vendaId(devolucao.getVenda().getId())
                .clienteNome(devolucao.getVenda().getClienteNome())
                .tipoDevolucao(devolucao.getTipoDevolucao())
                .valorDevolucao(devolucao.getValorDevolucao())
                .dataDevolucao(devolucao.getDataDevolucao())
                .totalItens(devolucao.getItens().size())
                .build();
    }

    public List<DevolucaoDTO.ProdutoDefeitosoResumo> listarProdutosDefeituosos() {
        log.info("Listando produtos defeituosos para gestão");

        // Buscar produtos com status defeituoso a partir das devoluções
        List<ItemDevolucao> itensDefeituosos = itemDevolucaoRepository
                .findByStatusQualidadeRetorno(StatusQualidade.DEFEITUOSO);

        // Agrupar por produto e calcular estatísticas
        return itensDefeituosos.stream()
                .collect(Collectors.groupingBy(item -> item.getProduto().getId()))
                .entrySet().stream()
                .<DevolucaoDTO.ProdutoDefeitosoResumo>map(entry -> {
                    Long produtoId = entry.getKey();
                    List<ItemDevolucao> itens = entry.getValue();
                    
                    // Pegar primeiro item para dados do produto
                    ItemDevolucao primeiroItem = itens.get(0);
                    Produto produto = primeiroItem.getProduto();
                    
                    // Somar quantidades
                    Integer totalQuantidade = itens.stream()
                            .mapToInt(ItemDevolucao::getQuantidade)
                            .sum();
                    
                    // Calcular valores
                    BigDecimal precoOriginal = produto.getPreco();
                    BigDecimal percentualDesconto = BigDecimal.valueOf(30); // Desconto padrão de 30%
                    BigDecimal precoDefeituoso = precoOriginal.multiply(
                            BigDecimal.ONE.subtract(percentualDesconto.divide(BigDecimal.valueOf(100)))
                    );
                    BigDecimal valorDesconto = precoOriginal.subtract(precoDefeituoso)
                            .multiply(BigDecimal.valueOf(totalQuantidade));
                    
                    // Data da última atualização
                    LocalDateTime ultimaAtualizacao = itens.stream()
                            .map(ItemDevolucao::getCreatedAt)
                            .max(LocalDateTime::compareTo)
                            .orElse(LocalDateTime.now());

                    return DevolucaoDTO.ProdutoDefeitosoResumo.builder()
                            .produtoId(produtoId)
                            .nomeProduto(produto.getNome())
                            .codigoBarras(produto.getCodigo())
                            .quantidadeDefeituosa(totalQuantidade)
                            .precoOriginal(precoOriginal)
                            .precoDefeituoso(precoDefeituoso)
                            .percentualDesconto(percentualDesconto)
                            .valorEstimadoDesconto(valorDesconto)
                            .dataUltimaAtualizacao(ultimaAtualizacao)
                            .build();
                })
                .collect(Collectors.toList());
    }
}