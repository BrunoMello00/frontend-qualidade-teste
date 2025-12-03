package com.tcc.estoque.service;
import java.util.stream.Collectors;

import com.tcc.estoque.dto.VendaDTO;
import com.tcc.estoque.dto.EventoDTO;
import com.tcc.estoque.model.*;
import com.tcc.estoque.model.enums.StatusVenda;
import com.tcc.estoque.model.enums.TipoMovimentacao;
import com.tcc.estoque.repository.VendaRepository;
import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.ItemVendaRepository;
import com.tcc.estoque.repository.MovimentacaoEstoqueRepository;
import com.tcc.estoque.repository.ClienteRepository;
import com.tcc.estoque.repository.RecompensaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável pela gestão de vendas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendaService {

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ClienteRepository clienteRepository;
    private final RecompensaRepository recompensaRepository;
    private final ClienteService clienteService;
    private final EventoService eventoService;

    /**
     * Lista vendas com paginação e filtros
     */
    public Page<VendaDTO.VendaResponse> listarVendas(
            StatusVenda status,
            LocalDate dataInicio,
            LocalDate dataFim,
            String nomeCliente,
            Pageable pageable) {
        
        log.debug("Listando vendas - Status: {}, Período: {} a {}, Cliente: {}", 
                status, dataInicio, dataFim, nomeCliente);

        Page<Venda> vendas;
        
        if (status != null || dataInicio != null || dataFim != null || nomeCliente != null) {
            LocalDateTime dataInicioTime = dataInicio != null ? dataInicio.atStartOfDay() : null;
            LocalDateTime dataFimTime = dataFim != null ? dataFim.atTime(23, 59, 59) : null;
            
            vendas = vendaRepository.findVendasComFiltros(
                status, dataInicioTime, dataFimTime, nomeCliente, pageable);
        } else {
            vendas = vendaRepository.findAllByOrderByDataVendaDesc(pageable);
        }

        return vendas.map(this::convertToVendaResponse);
    }

    /**
     * Busca venda por ID
     */
    public Optional<VendaDTO.VendaResponse> buscarVendaPorId(Long id) {
        log.debug("Buscando venda por ID: {}", id);
        
        return vendaRepository.findById(id)
                .map(this::convertToVendaResponseDetalhada);
    }

    /**
     * Cria uma nova venda
     */
    @Transactional
    public VendaDTO.VendaResponse criarVenda(VendaDTO.VendaRequest vendaRequest, Usuario usuario) {
        log.info("Criando nova venda para cliente: {}", vendaRequest.getNomeCliente());

        validarItensVenda(vendaRequest.getItens());

        Venda venda = new Venda();
        venda.setClienteNome(vendaRequest.getNomeCliente());
        venda.setClienteEmail(vendaRequest.getEmailCliente());
        venda.setClienteTelefone(vendaRequest.getTelefoneCliente());
        venda.setFormaPagamento(vendaRequest.getFormaPagamento());
        venda.setObservacoes(vendaRequest.getObservacoes());
        venda.setDesconto(vendaRequest.getDesconto() != null ? vendaRequest.getDesconto() : BigDecimal.ZERO);
        
        StatusVenda statusInicial = determinarStatusInicial(vendaRequest.getFormaPagamento());
        venda.setStatus(statusInicial);
        
        // Usar data customizada se fornecida, senão usar data atual
        LocalDateTime dataVenda = vendaRequest.getDataVenda() != null ? 
            vendaRequest.getDataVenda() : LocalDateTime.now();
        venda.setDataVenda(dataVenda);
        
        log.info("🔧 CRIAR VENDA - Forma de pagamento: {} | Status inicial: {}", 
                vendaRequest.getFormaPagamento(), statusInicial);
        
        if (statusInicial == StatusVenda.CONFIRMADA) {
            venda.setDataConfirmacao(LocalDateTime.now());
            log.info("🔧 CRIAR VENDA - Status CONFIRMADA, definindo dataConfirmacao");
        }
        
        venda.setUsuario(usuario);

        BigDecimal subtotal = calcularSubtotal(vendaRequest.getItens());
        venda.setSubtotal(subtotal);
        
        // 🎯 O desconto já vem calculado cumulativamente do frontend (evento + final + item)
        // NÃO recalcular desconto de evento para evitar duplicação
        log.info("💰 Desconto recebido do frontend (já cumulativo): R$ {}", venda.getDesconto());
        BigDecimal descontoTotal = venda.getDesconto();
        
        // 🎯 Aplicar desconto de pontos (se solicitado e cliente identificado)
        BigDecimal descontoPontos = BigDecimal.ZERO;
        if (vendaRequest.getUsarPontos() != null && vendaRequest.getUsarPontos() 
                && vendaRequest.getClienteId() != null && vendaRequest.getRecompensaId() != null) {
            
            log.info("🎯 Cliente solicitou uso de pontos. Cliente ID: {}, Recompensa ID: {}", 
                    vendaRequest.getClienteId(), vendaRequest.getRecompensaId());
            
            descontoPontos = aplicarRecompensaPontos(vendaRequest.getClienteId(), 
                    vendaRequest.getRecompensaId(), subtotal);
            
            log.info("🎯 Desconto de pontos aplicado: R$ {}", descontoPontos);
        }
        
        descontoTotal = descontoTotal.add(descontoPontos);
        venda.setDesconto(descontoTotal);
        venda.setValorTotal(subtotal.subtract(descontoTotal));

        venda = vendaRepository.save(venda);

        criarItensVenda(venda, vendaRequest.getItens());
        
        // 🔧 Se status for CONFIRMADA, baixar estoque automaticamente
        if (statusInicial == StatusVenda.CONFIRMADA) {
            baixarEstoqueDirecto(venda, vendaRequest.getItens(), usuario);
            log.info("Venda criada e confirmada automaticamente (forma pagamento: {}). Estoque baixado.", 
                    vendaRequest.getFormaPagamento());
        } else {
            log.info("Venda criada com status PENDENTE (forma pagamento: {}). Aguardando confirmação manual.", 
                    vendaRequest.getFormaPagamento());
        }

        log.info("Venda criada com sucesso. ID: {}, Status: {}, Total: {}", 
                venda.getId(), venda.getStatus(), venda.getValorTotal());

        // 🔧 Reload venda to ensure items are loaded due to lazy loading
        venda = vendaRepository.findByIdWithItens(venda.getId())
                .orElseThrow(() -> new RuntimeException("Venda não encontrada após criação"));

        log.info("🔧 DEBUG - Venda recarregada. ID: {}, Quantidade de itens: {}", 
                venda.getId(), venda.getItens() != null ? venda.getItens().size() : 0);

        // 🎯 Deduzir pontos se foram utilizados na venda confirmada
        if (statusInicial == StatusVenda.CONFIRMADA && vendaRequest.getUsarPontos() != null 
                && vendaRequest.getUsarPontos() && vendaRequest.getClienteId() != null 
                && vendaRequest.getRecompensaId() != null) {
            
            deduzirPontosRecompensa(vendaRequest.getClienteId(), 
                    vendaRequest.getRecompensaId(), venda);
        }

        // 🎯 Processar pontuação do cliente (se email fornecido)
        if (vendaRequest.getEmailCliente() != null && !vendaRequest.getEmailCliente().trim().isEmpty()) {
            processarPontuacaoCliente(venda, vendaRequest.getItens());
        }

        return convertToVendaResponseDetalhada(venda);
    }

    /**
     * 🆕 Determina status inicial baseado na forma de pagamento
     */
    private StatusVenda determinarStatusInicial(com.tcc.estoque.model.enums.FormaPagamento formaPagamento) {
        switch (formaPagamento) {
            case DINHEIRO:
            case PIX:
            case CARTAO_DEBITO:
            case CARTAO_CREDITO:
                return StatusVenda.CONFIRMADA; // Pagamentos instantâneos
            case TRANSFERENCIA:
            case BOLETO:
                return StatusVenda.PENDENTE; // Aguarda confirmação de pagamento
            default:
                return StatusVenda.PENDENTE;
        }
    }

    /**
     * Confirma uma venda (baixa estoque)
     */
    @Transactional
    public VendaDTO.VendaResponse confirmarVenda(Long vendaId, Usuario usuario) {
        log.info("Confirmando venda ID: {}", vendaId);

        Venda venda = vendaRepository.findById(vendaId)
                .orElseThrow(() -> new RuntimeException("Venda não encontrada"));

        if (venda.getStatus() != StatusVenda.PENDENTE) {
            throw new RuntimeException("Apenas vendas pendentes podem ser confirmadas");
        }

        verificarEstoqueDisponivel(venda);

        baixarEstoque(venda, usuario);

        venda.setStatus(StatusVenda.CONFIRMADA);
        venda.setDataConfirmacao(LocalDateTime.now());
        venda = vendaRepository.save(venda);

        log.info("Venda confirmada com sucesso. ID: {}", vendaId);

        return convertToVendaResponseDetalhada(venda);
    }

    /**
     * Cancela uma venda
     */
    @Transactional
    public VendaDTO.VendaResponse cancelarVenda(Long vendaId, String motivoCancelamento, Usuario usuario) {
        log.info("Cancelando venda ID: {}", vendaId);

        Venda venda = vendaRepository.findById(vendaId)
                .orElseThrow(() -> new RuntimeException("Venda não encontrada"));

        if (venda.getStatus() == StatusVenda.CANCELADA) {
            throw new RuntimeException("Venda já está cancelada");
        }

        if (venda.getStatus() == StatusVenda.CONFIRMADA) {
            devolverEstoque(venda, usuario);
        }

        venda.setStatus(StatusVenda.CANCELADA);
        venda.setDataCancelamento(LocalDateTime.now());
        venda.setMotivoCancelamento(motivoCancelamento);
        venda = vendaRepository.save(venda);

        log.info("Venda cancelada com sucesso. ID: {}", vendaId);

        return convertToVendaResponseDetalhada(venda);
    }

    /**
     * Busca vendas por período
     */
    public List<VendaDTO.VendaResponse> buscarVendasPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        log.debug("Buscando vendas no período: {} a {}", dataInicio, dataFim);

        LocalDateTime dataInicioTime = dataInicio.atStartOfDay();
        LocalDateTime dataFimTime = dataFim.atTime(23, 59, 59);

        List<Venda> vendas = vendaRepository.findVendasPorPeriodo(dataInicioTime, dataFimTime);

        return vendas.stream()
                .map(this::convertToVendaResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtém estatísticas de vendas
     */
    public VendaDTO.EstatisticasVendasResponse obterEstatisticasVendas(LocalDate dataInicio, LocalDate dataFim) {
        log.debug("Obtendo estatísticas de vendas para período: {} a {}", dataInicio, dataFim);

        if (dataInicio == null) {
            dataInicio = LocalDate.now().minusDays(30);
        }
        if (dataFim == null) {
            dataFim = LocalDate.now();
        }

        LocalDateTime dataInicioTime = dataInicio.atStartOfDay();
        LocalDateTime dataFimTime = dataFim.atTime(23, 59, 59);

        Long totalVendas = vendaRepository.countVendasPorPeriodo(dataInicioTime, dataFimTime);
        BigDecimal totalFaturamento = vendaRepository.sumFaturamentoPorPeriodo(dataInicioTime, dataFimTime);
        BigDecimal ticketMedio = totalVendas > 0 ? 
                totalFaturamento.divide(BigDecimal.valueOf(totalVendas), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;

        Long vendasPendentes = vendaRepository.countByStatusAndDataVendaBetween(
                StatusVenda.PENDENTE, dataInicioTime, dataFimTime);
        Long vendasConfirmadas = vendaRepository.countByStatusAndDataVendaBetween(
                StatusVenda.CONFIRMADA, dataInicioTime, dataFimTime);
        Long vendasCanceladas = vendaRepository.countByStatusAndDataVendaBetween(
                StatusVenda.CANCELADA, dataInicioTime, dataFimTime);

        return VendaDTO.EstatisticasVendasResponse.builder()
                .totalVendas(totalVendas)
                .totalFaturamento(totalFaturamento)
                .ticketMedio(ticketMedio)
                .vendasPendentes(vendasPendentes)
                .vendasConfirmadas(vendasConfirmadas)
                .vendasCanceladas(vendasCanceladas)
                .build();
    }


    private void validarItensVenda(List<VendaDTO.ItemVendaRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new RuntimeException("Venda deve ter pelo menos um item");
        }

        for (VendaDTO.ItemVendaRequest item : itens) {
            if (item.getQuantidade() <= 0) {
                throw new RuntimeException("Quantidade deve ser maior que zero");
            }
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Preço unitário deve ser maior que zero");
            }
            
            if (!produtoRepository.existsById(item.getProdutoId())) {
                throw new RuntimeException("Produto não encontrado: " + item.getProdutoId());
            }
        }
    }

    private BigDecimal calcularSubtotal(List<VendaDTO.ItemVendaRequest> itens) {
        return itens.stream()
                .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void criarItensVenda(Venda venda, List<VendaDTO.ItemVendaRequest> itensRequest) {
        log.info("🔧 CRIAR ITENS - Criando {} itens para venda ID: {}", itensRequest.size(), venda.getId());
        for (VendaDTO.ItemVendaRequest itemRequest : itensRequest) {
            Produto produto = produtoRepository.findById(itemRequest.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
            
            log.info("🔧 CRIAR ITENS - Produto encontrado: ID={}, Nome={}", produto.getId(), produto.getNome());

            ItemVenda item = new ItemVenda();
            item.setVenda(venda);
            item.setProduto(produto);
            item.setNomeProduto(produto.getNome()); // 🔧 Campo obrigatório para a tabela
            item.setQuantidade(itemRequest.getQuantidade());
            item.setPrecoUnitario(itemRequest.getPrecoUnitario());
            item.setSubtotal(itemRequest.getPrecoUnitario().multiply(BigDecimal.valueOf(itemRequest.getQuantidade())));
            item.setDescontoItem(itemRequest.getDescontoItem() != null ? itemRequest.getDescontoItem() : BigDecimal.ZERO);

            if (itemRequest.getTamanho() != null && !itemRequest.getTamanho().trim().isEmpty()) {
                item.setTamanho(itemRequest.getTamanho());
            }

            itemVendaRepository.save(item);
            log.info("🔧 CRIAR ITENS - Item salvo: ID={}, Quantidade={}", item.getId(), item.getQuantidade());
        }
        log.info("🔧 CRIAR ITENS - Processo concluído");
    }

    private void verificarEstoqueDisponivel(Venda venda) {
        for (ItemVenda item : venda.getItens()) {
            Produto produto = item.getProduto();
            int estoqueDisponivel = produto.getEstoqueTotal(); // Usa estoque total (tamanhos ou produto)
            if (estoqueDisponivel < item.getQuantidade()) {
                throw new RuntimeException(
                        String.format("Estoque insuficiente para produto %s. Disponível: %d, Solicitado: %d",
                                produto.getNome(), estoqueDisponivel, item.getQuantidade()));
            }
        }
    }

    private void baixarEstoque(Venda venda, Usuario usuario) {
        log.info("🔧 BAIXAR ESTOQUE - Iniciando para venda ID: {}", venda.getId());
        log.info("🔧 BAIXAR ESTOQUE - Quantidade de itens carregados: {}", venda.getItens().size());
        
        for (ItemVenda item : venda.getItens()) {
            log.info("🔧 BAIXAR ESTOQUE - Processando item ID: {}", item.getId());
            Produto produto = item.getProduto();
            
            if (produto == null) {
                log.error("🔧 BAIXAR ESTOQUE - ERRO: Produto é null para item ID: {}", item.getId());
                continue;
            }
            
            int estoqueAnterior = produto.getEstoqueTotal();
            
            log.info("🔧 BAIXAR ESTOQUE - Produto: {} | Estoque anterior: {} | Quantidade vendida: {}", 
                    produto.getNome(), estoqueAnterior, item.getQuantidade());
            
            if (produto.temTamanhos()) {
                int quantidadeRestante = item.getQuantidade();
                for (TamanhoProduto tamanho : produto.getTamanhos()) {
                    if (quantidadeRestante <= 0) break;
                    
                    int estoqueDisponivel = tamanho.getEstoque();
                    if (estoqueDisponivel > 0) {
                        int quantidadeBaixar = Math.min(quantidadeRestante, estoqueDisponivel);
                        tamanho.setEstoque(estoqueDisponivel - quantidadeBaixar);
                        quantidadeRestante -= quantidadeBaixar;
                        log.info("🔧 BAIXAR ESTOQUE - Tamanho {}: {} -> {}", 
                                tamanho.getTamanho(), estoqueDisponivel, tamanho.getEstoque());
                    }
                }
            } else {
                int novaQuantidade = produto.getEstoque() - item.getQuantidade();
                produto.setEstoque(novaQuantidade);
                log.info("🔧 BAIXAR ESTOQUE - Produto sem tamanhos: {} -> {}", 
                        estoqueAnterior, novaQuantidade);
            }
            
            produto = produtoRepository.save(produto);
            
            int estoqueAtual = produto.getEstoqueTotal();
            log.info("🔧 BAIXAR ESTOQUE - Produto salvo. Estoque total atual: {}", estoqueAtual);

            MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
            movimentacao.setProduto(produto);
            movimentacao.setTipo(TipoMovimentacao.SAIDA);
            movimentacao.setQuantidade(item.getQuantidade());
            movimentacao.setQuantidadeAnterior(estoqueAnterior);
            movimentacao.setQuantidadeAtual(estoqueAtual);
            movimentacao.setMotivo("Venda confirmada - ID: " + venda.getId());
            movimentacao.setDataMovimentacao(LocalDateTime.now());
            movimentacao.setUsuario(usuario);

            movimentacaoEstoqueRepository.save(movimentacao);
            log.info("🔧 BAIXAR ESTOQUE - Movimentação registrada para produto: {}", produto.getNome());
        }
        log.info("🔧 BAIXAR ESTOQUE - Processo concluído para venda ID: {}", venda.getId());
    }

    /**
     * 🆕 Método para baixar estoque diretamente usando os dados da requisição
     */
    private void baixarEstoqueDirecto(Venda venda, List<VendaDTO.ItemVendaRequest> itensRequest, Usuario usuario) {
        log.info("🔧 BAIXAR ESTOQUE DIRETO - Iniciando para venda ID: {} com {} itens", venda.getId(), itensRequest.size());
        
        for (VendaDTO.ItemVendaRequest itemRequest : itensRequest) {
            Produto produto = produtoRepository.findById(itemRequest.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
            
            int estoqueAnterior = produto.getEstoqueTotal();
            
            log.info("🔧 BAIXAR ESTOQUE DIRETO - Produto: {} | Estoque anterior: {} | Quantidade vendida: {}", 
                    produto.getNome(), estoqueAnterior, itemRequest.getQuantidade());
            
            if (produto.temTamanhos()) {
                int quantidadeRestante = itemRequest.getQuantidade();
                for (TamanhoProduto tamanho : produto.getTamanhos()) {
                    if (quantidadeRestante <= 0) break;
                    
                    int estoqueDisponivel = tamanho.getEstoque();
                    if (estoqueDisponivel > 0) {
                        int quantidadeBaixar = Math.min(quantidadeRestante, estoqueDisponivel);
                        tamanho.setEstoque(estoqueDisponivel - quantidadeBaixar);
                        quantidadeRestante -= quantidadeBaixar;
                        log.info("🔧 BAIXAR ESTOQUE DIRETO - Tamanho {}: {} -> {}", 
                                tamanho.getTamanho(), estoqueDisponivel, tamanho.getEstoque());
                    }
                }
            } else {
                int novaQuantidade = produto.getEstoque() - itemRequest.getQuantidade();
                produto.setEstoque(novaQuantidade);
                log.info("🔧 BAIXAR ESTOQUE DIRETO - Produto sem tamanhos: {} -> {}", 
                        estoqueAnterior, novaQuantidade);
            }
            
            produto = produtoRepository.save(produto);
            
            int estoqueAtual = produto.getEstoqueTotal();
            log.info("🔧 BAIXAR ESTOQUE DIRETO - Produto salvo. Estoque total atual: {}", estoqueAtual);

            MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
            movimentacao.setProduto(produto);
            movimentacao.setTipo(TipoMovimentacao.SAIDA);
            movimentacao.setQuantidade(itemRequest.getQuantidade());
            movimentacao.setQuantidadeAnterior(estoqueAnterior);
            movimentacao.setQuantidadeAtual(estoqueAtual);
            movimentacao.setMotivo("Venda confirmada - ID: " + venda.getId());
            movimentacao.setDataMovimentacao(LocalDateTime.now());
            movimentacao.setUsuario(usuario);

            movimentacaoEstoqueRepository.save(movimentacao);
            log.info("🔧 BAIXAR ESTOQUE DIRETO - Movimentação registrada para produto: {}", produto.getNome());
        }
        log.info("🔧 BAIXAR ESTOQUE DIRETO - Processo concluído para venda ID: {}", venda.getId());
    }

    /**
     * 🎉 Aplica desconto de evento ativo na venda
     * 🚫 MÉTODO DESABILITADO - Frontend já calcula desconto cumulativo
     */
    @SuppressWarnings("unused")
    private BigDecimal aplicarDescontoEvento(BigDecimal subtotal) {
        try {
            List<EventoDTO.EventoResumo> eventosComDesconto = eventoService.buscarEventosComDesconto();
            if (eventosComDesconto.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            EventoDTO.EventoResumo evento = eventosComDesconto.get(0);
            BigDecimal percentualDesconto = evento.getDescontoPercentual();
            
            if (percentualDesconto != null && percentualDesconto.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal desconto = subtotal.multiply(percentualDesconto).divide(new BigDecimal("100"));
                log.info("🎉 Desconto de evento aplicado: {}% = R$ {}", percentualDesconto, desconto);
                return desconto;
            }
        } catch (Exception e) {
            log.warn("⚠️ Erro ao aplicar desconto de evento: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private void devolverEstoque(Venda venda, Usuario usuario) {
        for (ItemVenda item : venda.getItens()) {
            Produto produto = item.getProduto();
            
            produto.setEstoque(produto.getEstoque() + item.getQuantidade());
            produtoRepository.save(produto);

            MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
            movimentacao.setProduto(produto);
            movimentacao.setTipo(TipoMovimentacao.ENTRADA);
            movimentacao.setQuantidade(item.getQuantidade());
            movimentacao.setQuantidadeAnterior(produto.getEstoque() - item.getQuantidade());
            movimentacao.setQuantidadeAtual(produto.getEstoque());
            movimentacao.setMotivo("Cancelamento de venda - ID: " + venda.getId());
            movimentacao.setDataMovimentacao(LocalDateTime.now());
            movimentacao.setUsuario(usuario);

            movimentacaoEstoqueRepository.save(movimentacao);
        }
    }

    private VendaDTO.VendaResponse convertToVendaResponse(Venda venda) {
        List<VendaDTO.ItemVendaResponse> itensBasicos = venda.getItens().stream()
                .map(item -> VendaDTO.ItemVendaResponse.builder()
                        .id(item.getId())
                        .produtoId(item.getProduto().getId())
                        .nomeProduto(item.getProduto().getNome())
                        .quantidade(item.getQuantidade())
                        .precoUnitario(item.getPrecoUnitario())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());
        
        return VendaDTO.VendaResponse.builder()
                .id(venda.getId())
                .nomeCliente(venda.getClienteNome())
                .emailCliente(venda.getClienteEmail())
                .telefoneCliente(venda.getClienteTelefone())
                .dataVenda(venda.getDataVenda())
                .status(venda.getStatus())
                .subtotal(venda.getSubtotal())
                .desconto(venda.getDesconto())
                .valorTotal(venda.getValorTotal())
                .formaPagamento(venda.getFormaPagamento())
                .quantidadeItens(venda.getItens() != null ? venda.getItens().size() : 0)
                .itens(itensBasicos) // Incluir itens básicos na listagem
                .nomeVendedor(venda.getUsuario().getNome())
                .build();
    }

    private VendaDTO.VendaResponse convertToVendaResponseDetalhada(Venda venda) {
        List<VendaDTO.ItemVendaResponse> itensResponse = venda.getItens().stream()
                .map(item -> VendaDTO.ItemVendaResponse.builder()
                        .id(item.getId())
                        .produtoId(item.getProduto().getId())
                        .nomeProduto(item.getProduto().getNome())
                        .codigoBarras(item.getProduto().getCodigo())
                        .quantidade(item.getQuantidade())
                        .precoUnitario(item.getPrecoUnitario())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return VendaDTO.VendaResponse.builder()
                .id(venda.getId())
                .nomeCliente(venda.getClienteNome())
                .emailCliente(venda.getClienteEmail())
                .telefoneCliente(venda.getClienteTelefone())
                .dataVenda(venda.getDataVenda())
                .dataConfirmacao(venda.getDataConfirmacao())
                .dataCancelamento(venda.getDataCancelamento())
                .status(venda.getStatus())
                .subtotal(venda.getSubtotal())
                .desconto(venda.getDesconto())
                .valorTotal(venda.getValorTotal())
                .formaPagamento(venda.getFormaPagamento()) // Campo de forma de pagamento
                .observacoes(venda.getObservacoes())
                .motivoCancelamento(venda.getMotivoCancelamento())
                .quantidadeItens(itensResponse.size())
                .itens(itensResponse)
                .nomeVendedor(venda.getUsuario().getNome())
                .emailVendedor(venda.getUsuario().getEmail())
                .build();
    }

    /**
     * Obtém os produtos mais vendidos
     */
    public List<VendaDTO.TopProdutoResponse> obterTopProdutos(LocalDate dataInicio, LocalDate dataFim, 
                                                             String tipoRanking, Integer limite) {
        if (limite == null || limite <= 0) {
            limite = 10; // Padrão: top 10
        }

        LocalDateTime dataInicioTime = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime dataFimTime = dataFim != null ? dataFim.atTime(23, 59, 59) : null;
        StatusVenda status = StatusVenda.CONFIRMADA; // Apenas vendas confirmadas

        Pageable pageable = PageRequest.of(0, limite);
        List<Object[]> resultados;

        if ("receita".equalsIgnoreCase(tipoRanking)) {
            resultados = vendaRepository.findTopProdutosPorReceita(dataInicioTime, dataFimTime, status, pageable);
        } else {
            resultados = vendaRepository.findTopProdutosMaisVendidos(dataInicioTime, dataFimTime, status, pageable);
        }

        Long totalGeralVendidos = vendaRepository.sumTotalProdutosVendidos(dataInicioTime, dataFimTime, status);
        if (totalGeralVendidos == null || totalGeralVendidos == 0) {
            totalGeralVendidos = 1L; // Evita divisão por zero
        }

        List<VendaDTO.TopProdutoResponse> topProdutos = new ArrayList<>();
        
        for (int i = 0; i < resultados.size(); i++) {
            Object[] resultado = resultados.get(i);
            
            Long produtoId = (Long) resultado[0];
            String nomeProduto = (String) resultado[1];
            String categoriaProduto = (String) resultado[2];
            Long quantidadeVendida = (Long) resultado[3];
            BigDecimal receitaTotal = (BigDecimal) resultado[4];
            BigDecimal precoUnitario = (BigDecimal) resultado[5];
            
            Double percentualVendas = (quantidadeVendida.doubleValue() / totalGeralVendidos.doubleValue()) * 100.0;
            
            VendaDTO.TopProdutoResponse topProduto = VendaDTO.TopProdutoResponse.builder()
                    .produtoId(produtoId)
                    .nomeProduto(nomeProduto != null ? nomeProduto : "N/A")
                    .categoriaProduto(categoriaProduto != null ? categoriaProduto : "Sem departamento")
                    .quantidadeVendida(quantidadeVendida != null ? quantidadeVendida.intValue() : 0)
                    .receitaTotal(receitaTotal != null ? receitaTotal : BigDecimal.ZERO)
                    .precoUnitario(precoUnitario != null ? precoUnitario : BigDecimal.ZERO)
                    .ranking(i + 1)
                    .percentualVendas(percentualVendas)
                    .build();
            
            topProdutos.add(topProduto);
        }

        return topProdutos;
    }

    /**
     * 🎯 Processa a pontuação do cliente baseada nos produtos comprados
     */
    private void processarPontuacaoCliente(Venda venda, List<VendaDTO.ItemVendaRequest> itensRequest) {
        try {
            log.info("🎯 Processando pontuação para cliente: {}", venda.getClienteEmail());
            
            // Buscar cliente pelo email
            Optional<Cliente> clienteOpt = clienteRepository.findByEmail(venda.getClienteEmail());
            if (clienteOpt.isEmpty()) {
                log.info("🎯 Cliente não encontrado no sistema. Pulando atribuição de pontos.");
                return;
            }
            
            Cliente cliente = clienteOpt.get();
            int totalPontos = 0;
            int quantidadeProdutos = itensRequest.size();
            
            log.info("🎯 Cliente encontrado: {} - Pontos atuais: {} - Produtos no carrinho: {}", 
                    cliente.getNome(), cliente.getPontos(), quantidadeProdutos);
            
            // Calcular pontos de cada produto
            for (VendaDTO.ItemVendaRequest itemRequest : itensRequest) {
                Produto produto = produtoRepository.findById(itemRequest.getProdutoId())
                        .orElse(null);
                        
                if (produto != null && produto.getPontuacaoProduto() != null) {
                    int pontosProduto = produto.getPontuacaoProduto() * itemRequest.getQuantidade();
                    totalPontos += pontosProduto;
                    
                    log.info("🎯 Produto: {} - Pontuação unitária: {} - Quantidade: {} - Pontos: {}", 
                            produto.getNome(), produto.getPontuacaoProduto(), 
                            itemRequest.getQuantidade(), pontosProduto);
                }
            }
            
            // Verificar regra: 3+ produtos = soma total de pontos
            if (quantidadeProdutos >= 3 && totalPontos > 0) {
                String motivo = String.format("Compra de %d produtos (Venda #%d) - Bônus por carrinho com 3+ produtos", 
                        quantidadeProdutos, venda.getId());
                        
                cliente.adicionarPontos(totalPontos, motivo, venda);
                clienteRepository.save(cliente);
                
                log.info("🎯 ✅ PONTOS ATRIBUÍDOS: {} pontos para cliente {} (3+ produtos)", 
                        totalPontos, cliente.getNome());
                log.info("🎯 Pontuação total do cliente após compra: {}", cliente.getPontos());
                
            } else if (quantidadeProdutos < 3) {
                log.info("🎯 ⚠️ Carrinho com menos de 3 produtos ({}). Pontos não atribuídos conforme regra.", 
                        quantidadeProdutos);
            } else {
                log.info("🎯 ⚠️ Nenhum ponto calculado para os produtos do carrinho.");
            }
            
        } catch (Exception e) {
            log.error("🎯 ❌ Erro ao processar pontuação do cliente: {}", e.getMessage(), e);
            // Não interrompe o fluxo de venda por erro na pontuação
        }
    }

    /**
     * 🎯 Busca informações de pontos do cliente e recompensas disponíveis
     */
    @Transactional(readOnly = true)
    public VendaDTO.ClientePontosResponse buscarPontosCliente(Long clienteId) {
        log.info("🎯 Buscando pontos do cliente ID: {}", clienteId);
        
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        List<Recompensa> recompensasAtivas = recompensaRepository.findByAtivoTrueOrderByPontosNecessariosAsc();

        List<VendaDTO.RecompensaDisponivelResponse> recompensasDisponiveis = recompensasAtivas.stream()
                .map(recompensa -> {
                    boolean podeUsar = cliente.temPontosSuficientes(recompensa.getPontosNecessarios());
                    
                    return VendaDTO.RecompensaDisponivelResponse.builder()
                            .recompensaId(recompensa.getId())
                            .nome(recompensa.getNome())
                            .descricao(recompensa.getDescricao())
                            .pontosNecessarios(recompensa.getPontosNecessarios())
                            .percentualDesconto(recompensa.getPercentualDesconto())
                            .valorDesconto(recompensa.getValorDesconto())
                            .categoria(recompensa.getCategoria())
                            .podeUsar(podeUsar)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("🎯 Cliente {} possui {} pontos, {} recompensas disponíveis", 
                cliente.getNome(), cliente.getPontos(), recompensasDisponiveis.size());

        return VendaDTO.ClientePontosResponse.builder()
                .clienteId(cliente.getId())
                .nomeCliente(cliente.getNome())
                .emailCliente(cliente.getEmail())
                .pontosDisponiveis(cliente.getPontos())
                .categoria(cliente.getCategoria() != null ? cliente.getCategoria().name() : "BRONZE")
                .recompensasDisponiveis(recompensasDisponiveis)
                .build();
    }

    /**
     * 🎯 Aplica recompensa de pontos em uma venda
     */
    public BigDecimal aplicarRecompensaPontos(Long clienteId, Long recompensaId, BigDecimal subtotal) {
        log.info("🎯 Aplicando recompensa ID: {} para cliente ID: {} em subtotal: {}", 
                recompensaId, clienteId, subtotal);
        
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Recompensa recompensa = recompensaRepository.findById(recompensaId)
                .orElseThrow(() -> new RuntimeException("Recompensa não encontrada"));

        if (!recompensa.getAtivo()) {
            throw new RuntimeException("Recompensa não está ativa");
        }

        if (!cliente.temPontosSuficientes(recompensa.getPontosNecessarios())) {
            throw new RuntimeException("Cliente não possui pontos suficientes para esta recompensa");
        }

        BigDecimal desconto = BigDecimal.ZERO;

        // Aplicar desconto percentual
        if (recompensa.getPercentualDesconto() != null && recompensa.getPercentualDesconto().compareTo(BigDecimal.ZERO) > 0) {
            desconto = subtotal.multiply(recompensa.getPercentualDesconto())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        
        // Aplicar desconto fixo
        if (recompensa.getValorDesconto() != null && recompensa.getValorDesconto().compareTo(BigDecimal.ZERO) > 0) {
            desconto = desconto.add(recompensa.getValorDesconto());
        }

        // Garantir que o desconto não seja maior que o subtotal
        if (desconto.compareTo(subtotal) > 0) {
            desconto = subtotal;
        }

        log.info("🎯 Desconto calculado: R$ {} para recompensa '{}'", desconto, recompensa.getNome());
        
        return desconto;
    }

    /**
     * 🎯 Deduz pontos do cliente após aplicar recompensa
     */
    public void deduzirPontosRecompensa(Long clienteId, Long recompensaId, Venda venda) {
        log.info("🎯 Deduzindo pontos da recompensa ID: {} para cliente ID: {} na venda ID: {}", 
                recompensaId, clienteId, venda.getId());
        
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Recompensa recompensa = recompensaRepository.findById(recompensaId)
                .orElseThrow(() -> new RuntimeException("Recompensa não encontrada"));

        String motivo = String.format("Recompensa '%s' aplicada na venda #%d", 
                recompensa.getNome(), venda.getId());
        
        cliente.usarPontos(recompensa.getPontosNecessarios(), motivo, venda);
        clienteRepository.save(cliente);
        
        log.info("🎯 ✅ {} pontos deduzidos do cliente {}. Saldo atual: {}", 
                recompensa.getPontosNecessarios(), cliente.getNome(), cliente.getPontos());
    }
}
