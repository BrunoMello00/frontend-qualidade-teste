package com.tcc.estoque.service;

import com.tcc.estoque.dto.DashboardDTO;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.model.Venda;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.Evento;
import com.tcc.estoque.model.enums.StatusVenda;
import com.tcc.estoque.model.enums.StatusEvento;
import com.tcc.estoque.model.enums.TipoUsuario;
import com.tcc.estoque.model.enums.CategoriaCliente;
import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.VendaRepository;
import com.tcc.estoque.repository.ClienteRepository;
import com.tcc.estoque.repository.UsuarioRepository;
import com.tcc.estoque.repository.EventoRepository;
import com.tcc.estoque.repository.ItemVendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

/**
 * Serviço responsável pelos dados do dashboard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final ItemVendaRepository itemVendaRepository;

    /**
     * Obtém dados gerais para o dashboard
     */
    public DashboardDTO.DadosGeraisResponse obterDadosGerais() {
        log.debug("Obtendo dados gerais do dashboard");

        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDateTime inicioMesTime = inicioMes.atStartOfDay();
        LocalDateTime hoje23h59 = hoje.atTime(23, 59, 59);

        Long totalVendas = vendaRepository.countByStatus(StatusVenda.CONFIRMADA);

        BigDecimal faturamentoMes = vendaRepository.sumFaturamentoPorPeriodo(inicioMesTime, hoje23h59);
        if (faturamentoMes == null) faturamentoMes = BigDecimal.ZERO;

        BigDecimal faturamentoDia = vendaRepository.sumFaturamentoHoje();
        if (faturamentoDia == null) faturamentoDia = BigDecimal.ZERO;

        Long totalProdutos = produtoRepository.countByAtivoTrue();

        Long produtosEstoqueBaixo = produtoRepository.countProdutosComEstoqueBaixo();

        BigDecimal ticketMedio = calcularTicketMedio(inicioMesTime, hoje23h59);

        BigDecimal crescimentoMes = calcularCrescimentoMes(inicioMes);

        Long vendasPendentes = vendaRepository.countByStatus(StatusVenda.PENDENTE);

        Long totalClientes = clienteRepository.countByAtivoTrue();
        Long totalUsuarios = usuarioRepository.countByAtivoTrue();
        Long eventosAtivos = eventoRepository.countByStatusAndAtivoTrue(StatusEvento.ATIVO);
        Long clientesNovosHoje = clienteRepository.countClientesNovosHoje();

        return DashboardDTO.DadosGeraisResponse.builder()
                .totalVendas(totalVendas)
                .faturamentoMes(faturamentoMes)
                .faturamentoDia(faturamentoDia)
                .totalProdutos(totalProdutos)
                .produtosEstoqueBaixo(produtosEstoqueBaixo)
                .ticketMedio(ticketMedio)
                .crescimentoMes(crescimentoMes)
                .vendasPendentes(vendasPendentes)
                .totalClientes(totalClientes)
                .totalUsuarios(totalUsuarios)
                .eventosAtivos(eventosAtivos)
                .clientesNovosHoje(clientesNovosHoje)
                .build();
    }

    /**
     * Obtém estatísticas completas para dashboard gerencial
     */
    public DashboardDTO.EstatisticasGeraisResponse obterEstatisticasGerenciais() {
        log.debug("Obtendo estatísticas gerenciais completas");

        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);

        Map<StatusVenda, Long> vendasPorStatus = converterVendasPorStatus(
                vendaRepository.contarVendasPorStatus());
        
        Map<CategoriaCliente, Long> clientesPorCategoria = converterClientesPorCategoria(
                clienteRepository.contarClientesPorCategoria());
        
        Map<TipoUsuario, Long> usuariosPorTipo = converterUsuariosPorTipo(
                usuarioRepository.contarUsuariosPorTipo());
        
        Map<StatusEvento, Long> eventosPorStatus = converterEventosPorStatus(
                eventoRepository.contarEventosPorStatus());
        
        List<DashboardDTO.VendedorRankingDTO> topVendedores = obterTopVendedores(inicioMes.atStartOfDay(), hoje.atTime(23, 59, 59));
        
        List<DashboardDTO.ProdutoVendidoDTO> topProdutos = obterTopProdutosReais(30);
        
        List<DashboardDTO.EventoPerformanceDTO> topEventos = obterTopEventos();
        
        List<DashboardDTO.AlertaDTO> alertas = obterAlertasSistema();

        return DashboardDTO.EstatisticasGeraisResponse.builder()
                .vendasPorStatus(vendasPorStatus)
                .clientesPorCategoria(clientesPorCategoria)
                .usuariosPorTipo(usuariosPorTipo)
                .eventosPorStatus(eventosPorStatus)
                .topVendedores(topVendedores)
                .topProdutos(topProdutos)
                .topEventos(topEventos)
                .alertas(alertas)
                .build();
    }

    /**
     * Obtém dashboard específico por vendedor
     */
    public DashboardDTO.DashboardVendedorResponse obterDashboardVendedor(Long vendedorId) {
        log.debug("Obtendo dashboard do vendedor ID: {}", vendedorId);

        Usuario vendedor = usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new RuntimeException("Vendedor não encontrado"));

        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDateTime inicioMesTime = inicioMes.atStartOfDay();
        LocalDateTime hoje23h59 = hoje.atTime(23, 59, 59);

        Long totalVendasVendedor = vendaRepository.countByUsuarioIdAndStatus(vendedorId, StatusVenda.CONFIRMADA);
        BigDecimal faturamentoVendedor = vendaRepository.sumFaturamentoPorVendedor(vendedorId, inicioMesTime, hoje23h59);
        if (faturamentoVendedor == null) faturamentoVendedor = BigDecimal.ZERO;

        BigDecimal metaVendedor = BigDecimal.valueOf(10000);
        BigDecimal progressoMeta = faturamentoVendedor.divide(metaVendedor, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        Integer posicaoRanking = obterPosicaoVendedorRanking(vendedorId);

        Long clientesAtendidos = vendaRepository.countDistinctClientesByVendedor(vendedorId, inicioMesTime, hoje23h59);

        List<DashboardDTO.VendaDiaDTO> vendasDiarias = obterVendasDiariasPorVendedor(vendedorId, inicioMes, hoje);

        List<DashboardDTO.ProdutoVendidoDTO> produtosMaisVendidos = obterProdutosMaisVendidosPorVendedor(vendedorId, 30);

        return DashboardDTO.DashboardVendedorResponse.builder()
                .vendedor(convertToUsuarioResumo(vendedor))
                .totalVendas(totalVendasVendedor)
                .faturamentoMes(faturamentoVendedor)
                .metaVendedor(metaVendedor)
                .progressoMeta(progressoMeta)
                .posicaoRanking(posicaoRanking)
                .clientesAtendidos(clientesAtendidos)
                .vendasDiarias(vendasDiarias)
                .produtosMaisVendidos(produtosMaisVendidos)
                .build();
    }

    /**
     * Obtém resumo de performance para o dashboard
     */
    public DashboardDTO.PerformanceResumoResponse obterResumoPerformance() {
        log.debug("Obtendo resumo de performance");

        LocalDate hoje = LocalDate.now();
        LocalDate ontem = hoje.minusDays(1);
        LocalDate inicioSemana = hoje.minusDays(6);
        LocalDate inicioMes = hoje.withDayOfMonth(1);

        BigDecimal vendasHoje = vendaRepository.sumFaturamentoPorDia(hoje);
        BigDecimal vendasOntem = vendaRepository.sumFaturamentoPorDia(ontem);
        if (vendasHoje == null) vendasHoje = BigDecimal.ZERO;
        if (vendasOntem == null) vendasOntem = BigDecimal.ZERO;

        BigDecimal crescimentoDiario = calcularPercentualCrescimento(vendasOntem, vendasHoje);

        BigDecimal vendasSemana = vendaRepository.sumFaturamentoPorPeriodo(
                inicioSemana.atStartOfDay(), hoje.atTime(23, 59, 59));
        BigDecimal vendasSemanaAnterior = vendaRepository.sumFaturamentoPorPeriodo(
                inicioSemana.minusDays(7).atStartOfDay(), ontem.atTime(23, 59, 59));
        if (vendasSemana == null) vendasSemana = BigDecimal.ZERO;
        if (vendasSemanaAnterior == null) vendasSemanaAnterior = BigDecimal.ZERO;

        BigDecimal crescimentoSemanal = calcularPercentualCrescimento(vendasSemanaAnterior, vendasSemana);

        BigDecimal vendasMes = vendaRepository.sumFaturamentoPorPeriodo(
                inicioMes.atStartOfDay(), hoje.atTime(23, 59, 59));
        BigDecimal vendasMesAnterior = vendaRepository.sumFaturamentoPorPeriodo(
                inicioMes.minusMonths(1).atStartOfDay(), inicioMes.minusDays(1).atTime(23, 59, 59));
        if (vendasMes == null) vendasMes = BigDecimal.ZERO;
        if (vendasMesAnterior == null) vendasMesAnterior = BigDecimal.ZERO;

        BigDecimal crescimentoMensal = calcularPercentualCrescimento(vendasMesAnterior, vendasMes);

        Double taxaConversaoVendas = calcularTaxaConversaoVendas();
        BigDecimal ticketMedioMes = calcularTicketMedio(inicioMes.atStartOfDay(), hoje.atTime(23, 59, 59));

        return DashboardDTO.PerformanceResumoResponse.builder()
                .vendasHoje(vendasHoje)
                .vendasOntem(vendasOntem)
                .crescimentoDiario(crescimentoDiario)
                .vendasSemana(vendasSemana)
                .vendasSemanaAnterior(vendasSemanaAnterior)
                .crescimentoSemanal(crescimentoSemanal)
                .vendasMes(vendasMes)
                .vendasMesAnterior(vendasMesAnterior)
                .crescimentoMensal(crescimentoMensal)
                .taxaConversaoVendas(taxaConversaoVendas)
                .ticketMedioMes(ticketMedioMes)
                .build();
    }

    /**
     * Obtém vendas da semana (últimos 7 dias)
     */
    public DashboardDTO.VendasSemanaResponse obterVendasSemana() {
        log.debug("Obtendo vendas da semana");

        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.minusDays(6);

        List<Venda> vendasSemana = vendaRepository.findVendasPorPeriodo(
                inicioSemana.atStartOfDay(), hoje.atTime(23, 59, 59));

        List<DashboardDTO.VendaDiaDTO> vendas = criarVendasPorDia(vendasSemana, inicioSemana, hoje);

        BigDecimal totalSemana = vendas.stream()
                .map(DashboardDTO.VendaDiaDTO::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal mediaDiaria = totalSemana.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

        BigDecimal crescimentoSemana = BigDecimal.valueOf(5.2); // Valor fixo por enquanto

        return DashboardDTO.VendasSemanaResponse.builder()
                .vendas(vendas)
                .totalSemana(totalSemana)
                .mediaDiaria(mediaDiaria)
                .crescimentoSemana(crescimentoSemana)
                .build();
    }

    /**
     * Obtém vendas do mês atual
     */
    public DashboardDTO.VendasMesResponse obterVendasMes() {
        log.debug("Obtendo vendas do mês");

        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);

        List<Venda> vendasMes = vendaRepository.findVendasPorPeriodo(
                inicioMes.atStartOfDay(), hoje.atTime(23, 59, 59));

        List<DashboardDTO.VendaDiaDTO> vendas = criarVendasPorDia(vendasMes, inicioMes, hoje);

        BigDecimal totalMes = vendas.stream()
                .map(DashboardDTO.VendaDiaDTO::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal metaMes = BigDecimal.valueOf(50000);
        BigDecimal percentualMeta = totalMes.divide(metaMes, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        BigDecimal crescimentoMesAnterior = calcularCrescimentoMes(inicioMes);

        return DashboardDTO.VendasMesResponse.builder()
                .vendas(vendas)
                .totalMes(totalMes)
                .metaMes(metaMes)
                .percentualMeta(percentualMeta)
                .crescimentoMesAnterior(crescimentoMesAnterior)
                .build();
    }

    /**
     * Obtém top produtos mais vendidos
     */
    public DashboardDTO.TopProdutosResponse obterTopProdutos(int dias) {
        log.debug("Obtendo top produtos - Últimos {} dias", dias);

        List<Produto> produtosAtivos = produtoRepository.findAll().stream()
                .filter(p -> p.getAtivo())
                .limit(5)
                .collect(Collectors.toList());

        List<DashboardDTO.ProdutoVendidoDTO> produtos = produtosAtivos.stream()
                .map(produto -> DashboardDTO.ProdutoVendidoDTO.builder()
                        .produtoId(produto.getId())
                        .nome(produto.getNome())
                        .quantidadeVendida(50L) // Valor simulado
                        .faturamento(produto.getPreco().multiply(BigDecimal.valueOf(50)))
                        .categoria(produto.getDepartamento())
                        .precoUnitario(produto.getPreco())
                        .build())
                .collect(Collectors.toList());

        return DashboardDTO.TopProdutosResponse.builder()
                .produtos(produtos)
                .periodoAnalisado(dias)
                .build();
    }

    /**
     * Obtém produtos com estoque baixo
     */
    public DashboardDTO.EstoqueBaixoResponse obterEstoqueBaixo() {
        log.debug("Obtendo produtos com estoque baixo");

        List<Produto> produtosBaixo = produtoRepository.findProdutosComEstoqueBaixo();

        List<DashboardDTO.ProdutoEstoqueBaixoDTO> produtos = produtosBaixo.stream()
                .map(produto -> {
                    String status = determinarStatusEstoque(produto.getEstoqueTotal(), produto.getEstoqueMinimo());
                    
                    return DashboardDTO.ProdutoEstoqueBaixoDTO.builder()
                            .produtoId(produto.getId())
                            .nome(produto.getNome())
                            .quantidadeAtual(produto.getEstoqueTotal())
                            .estoqueMinimo(produto.getEstoqueMinimo())
                            .categoria(produto.getDepartamento())
                            .preco(produto.getPreco())
                            .status(status)
                            .build();
                })
                .collect(Collectors.toList());

        return DashboardDTO.EstoqueBaixoResponse.builder()
                .produtos(produtos)
                .totalProdutosCriticos(produtos.size())
                .build();
    }

    /**
     * Obtém vendas recentes
     */
    public DashboardDTO.VendasRecentesResponse obterVendasRecentes(int limite) {
        log.debug("Obtendo vendas recentes - Limite: {}", limite);

        List<Venda> vendasRecentes = vendaRepository.findAllByOrderByDataVendaDesc(
                PageRequest.of(0, limite)).getContent();

        List<DashboardDTO.VendaRecenteDTO> vendas = vendasRecentes.stream()
                .map(venda -> DashboardDTO.VendaRecenteDTO.builder()
                        .vendaId(venda.getId())
                        .nomeCliente(venda.getClienteNome())
                        .valorTotal(venda.getValorTotal())
                        .dataVenda(venda.getDataVenda())
                        .status(venda.getStatus().toString())
                        .quantidadeItens(venda.getItens() != null ? venda.getItens().size() : 0)
                        .build())
                .collect(Collectors.toList());

        return DashboardDTO.VendasRecentesResponse.builder()
                .vendas(vendas)
                .totalVendas(vendas.size())
                .build();
    }


    private List<DashboardDTO.VendaDiaDTO> criarVendasPorDia(List<Venda> vendas, LocalDate inicio, LocalDate fim) {
        List<DashboardDTO.VendaDiaDTO> resultado = new ArrayList<>();
        
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            final LocalDate dataFinal = data;
            
            BigDecimal valorDia = vendas.stream()
                    .filter(venda -> venda.getDataVenda().toLocalDate().equals(dataFinal))
                    .map(Venda::getValorTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Long quantidadeDia = vendas.stream()
                    .filter(venda -> venda.getDataVenda().toLocalDate().equals(dataFinal))
                    .count();
            
            resultado.add(DashboardDTO.VendaDiaDTO.builder()
                    .data(data)
                    .valor(valorDia)
                    .quantidade(quantidadeDia)
                    .build());
        }
        
        return resultado;
    }

    private BigDecimal calcularTicketMedio(LocalDateTime inicio, LocalDateTime fim) {
        Long totalVendas = vendaRepository.countByStatusAndDataVendaBetween(
                StatusVenda.CONFIRMADA, inicio, fim);
        
        if (totalVendas == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal faturamento = vendaRepository.sumFaturamentoPorPeriodo(inicio, fim);
        
        if (faturamento == null) {
            return BigDecimal.ZERO;
        }

        return faturamento.divide(BigDecimal.valueOf(totalVendas), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularCrescimentoMes(LocalDate inicioMes) {
        YearMonth mesAtual = YearMonth.from(inicioMes);
        YearMonth mesAnterior = mesAtual.minusMonths(1);

        LocalDateTime inicioMesAtual = mesAtual.atDay(1).atStartOfDay();
        LocalDateTime fimMesAtual = LocalDate.now().atTime(23, 59, 59);

        LocalDateTime inicioMesAnterior = mesAnterior.atDay(1).atStartOfDay();
        LocalDateTime fimMesAnterior = mesAnterior.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal faturamentoAtual = vendaRepository.sumFaturamentoPorPeriodo(inicioMesAtual, fimMesAtual);
        BigDecimal faturamentoAnterior = vendaRepository.sumFaturamentoPorPeriodo(inicioMesAnterior, fimMesAnterior);

        if (faturamentoAtual == null) faturamentoAtual = BigDecimal.ZERO;
        if (faturamentoAnterior == null || faturamentoAnterior.equals(BigDecimal.ZERO)) {
            return BigDecimal.valueOf(15.5); // Valor simulado
        }

        return faturamentoAtual.subtract(faturamentoAnterior)
                .divide(faturamentoAnterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private String determinarStatusEstoque(Integer quantidadeAtual, Integer estoqueMinimo) {
        if (quantidadeAtual == 0) {
            return "ZERADO";
        } else if (quantidadeAtual <= estoqueMinimo / 2) {
            return "CRITICO";
        } else {
            return "BAIXO";
        }
    }


    /**
     * Obtém top vendedores por período
     */
    private List<DashboardDTO.VendedorRankingDTO> obterTopVendedores(LocalDateTime inicio, LocalDateTime fim) {
        List<Object[]> resultados = vendaRepository.findTopVendedoresPorPeriodo(inicio, fim, PageRequest.of(0, 5));
        
        return resultados.stream()
                .map(resultado -> {
                    Usuario vendedor = (Usuario) resultado[0];
                    Long totalVendas = (Long) resultado[1];
                    BigDecimal faturamento = (BigDecimal) resultado[2];
                    
                    BigDecimal ticketMedio = totalVendas > 0 ? 
                            faturamento.divide(BigDecimal.valueOf(totalVendas), 2, RoundingMode.HALF_UP) : 
                            BigDecimal.ZERO;
                    
                    return DashboardDTO.VendedorRankingDTO.builder()
                            .vendedorId(vendedor.getId())
                            .nome(vendedor.getNome())
                            .email(vendedor.getEmail())
                            .tipo(vendedor.getTipoUsuario())
                            .totalVendas(totalVendas)
                            .faturamento(faturamento)
                            .ticketMedio(ticketMedio)
                            .posicao(resultados.indexOf(resultado) + 1)
                            .percentualMeta(BigDecimal.valueOf(85.5)) // Valor fictício
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtém produtos mais vendidos com dados reais
     */
    private List<DashboardDTO.ProdutoVendidoDTO> obterTopProdutosReais(int dias) {
        LocalDateTime dataInicio = LocalDate.now().minusDays(dias).atStartOfDay();
        LocalDateTime dataFim = LocalDateTime.now();
        
        List<Object[]> resultados = itemVendaRepository.findTopProdutosPorPeriodo(dataInicio, dataFim, PageRequest.of(0, 5));
        
        return resultados.stream()
                .map(resultado -> {
                    Produto produto = (Produto) resultado[0];
                    Long quantidadeVendida = (Long) resultado[1];
                    BigDecimal faturamento = (BigDecimal) resultado[2];
                    
                    return DashboardDTO.ProdutoVendidoDTO.builder()
                            .produtoId(produto.getId())
                            .nome(produto.getNome())
                            .quantidadeVendida(quantidadeVendida)
                            .faturamento(faturamento)
                            .categoria(produto.getDepartamento())
                            .precoUnitario(produto.getPreco())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtém top eventos por performance
     */
    private List<DashboardDTO.EventoPerformanceDTO> obterTopEventos() {
        List<Evento> eventos = eventoRepository.findTopEventosPorVendasDashboard(PageRequest.of(0, 5));
        
        return eventos.stream()
                .map(evento -> {
                    BigDecimal totalVendas = eventoRepository.calcularTotalVendasEvento(evento.getId());
                    if (totalVendas == null) totalVendas = BigDecimal.ZERO;
                    
                    Integer quantidadeVendas = eventoRepository.contarVendasEvento(evento.getId());
                    if (quantidadeVendas == null) quantidadeVendas = 0;
                    
                    Double progressoMeta = 0.0;
                    if (evento.getMetaVendas() != null && evento.getMetaVendas().compareTo(BigDecimal.ZERO) > 0) {
                        progressoMeta = totalVendas.divide(evento.getMetaVendas(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)).doubleValue();
                    }
                    
                    Integer diasRestantes = calcularDiasRestantes(evento.getDataFim().atTime(23, 59, 59));
                    
                    return DashboardDTO.EventoPerformanceDTO.builder()
                            .eventoId(evento.getId())
                            .nome(evento.getNome())
                            .status(evento.getStatus())
                            .statusCor(evento.getStatus().getCor())
                            .dataInicio(evento.getDataInicio().atStartOfDay())
                            .dataFim(evento.getDataFim().atTime(23, 59, 59))
                            .totalVendas(totalVendas)
                            .quantidadeVendas(quantidadeVendas)
                            .metaVendas(evento.getMetaVendas())
                            .progressoMeta(progressoMeta)
                            .diasRestantes(diasRestantes)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtém alertas do sistema
     */
    private List<DashboardDTO.AlertaDTO> obterAlertasSistema() {
        List<DashboardDTO.AlertaDTO> alertas = new ArrayList<>();
        
        Long produtosEstoqueBaixo = produtoRepository.countProdutosComEstoqueBaixo();
        if (produtosEstoqueBaixo > 0) {
            alertas.add(DashboardDTO.AlertaDTO.builder()
                    .tipo("ESTOQUE")
                    .titulo("Produtos com Estoque Baixo")
                    .mensagem(produtosEstoqueBaixo + " produto(s) com estoque crítico")
                    .nivel("WARNING")
                    .cor("#FF8800")
                    .icone("warning-triangle")
                    .dataAlerta(LocalDateTime.now())
                    .acao("Revisar estoque")
                    .link("/produtos?filtro=estoque-baixo")
                    .build());
        }
        
        List<Evento> eventosProximosAoFim = eventoRepository.findEventosProximosAoFim(
                LocalDateTime.now(), LocalDateTime.now().plusDays(3));
        if (!eventosProximosAoFim.isEmpty()) {
            alertas.add(DashboardDTO.AlertaDTO.builder()
                    .tipo("EVENTO")
                    .titulo("Eventos Próximos ao Fim")
                    .mensagem(eventosProximosAoFim.size() + " evento(s) terminando em 3 dias")
                    .nivel("INFO")
                    .cor("#4ECDC4")
                    .icone("calendar")
                    .dataAlerta(LocalDateTime.now())
                    .acao("Revisar eventos")
                    .link("/eventos?filtro=proximo-fim")
                    .build());
        }
        
        Long vendasPendentes = vendaRepository.countByStatus(StatusVenda.PENDENTE);
        if (vendasPendentes > 5) {
            alertas.add(DashboardDTO.AlertaDTO.builder()
                    .tipo("VENDA")
                    .titulo("Vendas Pendentes")
                    .mensagem(vendasPendentes + " venda(s) aguardando confirmação")
                    .nivel("WARNING")
                    .cor("#FF8800")
                    .icone("clock")
                    .dataAlerta(LocalDateTime.now())
                    .acao("Confirmar vendas")
                    .link("/vendas?status=pendente")
                    .build());
        }
        
        LocalDate hoje = LocalDate.now();
        if (hoje.getDayOfMonth() > 20) { // Última semana do mês
            alertas.add(DashboardDTO.AlertaDTO.builder()
                    .tipo("META")
                    .titulo("Meta do Mês")
                    .mensagem("Última semana para atingir meta mensal")
                    .nivel("INFO")
                    .cor("#45B7D1")
                    .icone("target")
                    .dataAlerta(LocalDateTime.now())
                    .acao("Revisar progresso")
                    .link("/dashboard/vendedor")
                    .build());
        }
        
        return alertas;
    }

    /**
     * Converte Map para distribuição por categoria
     */
    private Map<CategoriaCliente, Long> converterClientesPorCategoria(List<Object[]> resultados) {
        return resultados.stream()
                .collect(Collectors.toMap(
                        resultado -> (CategoriaCliente) resultado[0],
                        resultado -> (Long) resultado[1]
                ));
    }

    /**
     * Converte Map para distribuição por tipo de usuário
     */
    private Map<TipoUsuario, Long> converterUsuariosPorTipo(List<Object[]> resultados) {
        return resultados.stream()
                .collect(Collectors.toMap(
                        resultado -> (TipoUsuario) resultado[0],
                        resultado -> (Long) resultado[1]
                ));
    }

    /**
     * Converte Map para distribuição por status de evento
     */
    private Map<StatusEvento, Long> converterEventosPorStatus(List<Object[]> resultados) {
        return resultados.stream()
                .collect(Collectors.toMap(
                        resultado -> (StatusEvento) resultado[0],
                        resultado -> (Long) resultado[1]
                ));
    }

    /**
     * Converte Map para distribuição por status de venda
     */
    private Map<StatusVenda, Long> converterVendasPorStatus(List<Object[]> resultados) {
        return resultados.stream()
                .collect(Collectors.toMap(
                        resultado -> (StatusVenda) resultado[0],
                        resultado -> (Long) resultado[1]
                ));
    }

    /**
     * Obtém posição do vendedor no ranking
     */
    private Integer obterPosicaoVendedorRanking(Long vendedorId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        
        List<Object[]> ranking = vendaRepository.findTopVendedoresPorPeriodo(
                inicioMes.atStartOfDay(), hoje.atTime(23, 59, 59), PageRequest.of(0, 50));
        
        for (int i = 0; i < ranking.size(); i++) {
            Usuario vendedor = (Usuario) ranking.get(i)[0];
            if (vendedor.getId().equals(vendedorId)) {
                return i + 1;
            }
        }
        
        return ranking.size() + 1; // Se não encontrou, coloca no final
    }

    /**
     * Obtém vendas diárias por vendedor
     */
    private List<DashboardDTO.VendaDiaDTO> obterVendasDiariasPorVendedor(Long vendedorId, LocalDate inicio, LocalDate fim) {
        List<DashboardDTO.VendaDiaDTO> resultado = new ArrayList<>();
        
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            BigDecimal valorDia = vendaRepository.sumFaturamentoPorVendedorEDia(vendedorId, data);
            if (valorDia == null) valorDia = BigDecimal.ZERO;
            
            Long quantidadeDia = vendaRepository.countByUsuarioIdAndDataVenda(vendedorId, data);
            
            resultado.add(DashboardDTO.VendaDiaDTO.builder()
                    .data(data)
                    .valor(valorDia)
                    .quantidade(quantidadeDia)
                    .build());
        }
        
        return resultado;
    }

    /**
     * Obtém produtos mais vendidos por vendedor
     */
    private List<DashboardDTO.ProdutoVendidoDTO> obterProdutosMaisVendidosPorVendedor(Long vendedorId, int dias) {
        LocalDateTime dataInicio = LocalDate.now().minusDays(dias).atStartOfDay();
        LocalDateTime dataFim = LocalDateTime.now();
        
        List<Object[]> resultados = itemVendaRepository.findTopProdutosPorVendedor(
                vendedorId, dataInicio, dataFim, PageRequest.of(0, 5));
        
        return resultados.stream()
                .map(resultado -> {
                    Produto produto = (Produto) resultado[0];
                    Long quantidadeVendida = (Long) resultado[1];
                    BigDecimal faturamento = (BigDecimal) resultado[2];
                    
                    return DashboardDTO.ProdutoVendidoDTO.builder()
                            .produtoId(produto.getId())
                            .nome(produto.getNome())
                            .quantidadeVendida(quantidadeVendida)
                            .faturamento(faturamento)
                            .categoria(produto.getDepartamento())
                            .precoUnitario(produto.getPreco())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Converte usuário para resumo
     */
    private DashboardDTO.UsuarioResumoDTO convertToUsuarioResumo(Usuario usuario) {
        return DashboardDTO.UsuarioResumoDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .tipo(usuario.getTipoUsuario())
                .tipoDescricao(usuario.getTipoUsuario().getDescricao())
                .tipoCor(usuario.getTipoUsuario().getCor())
                .ativo(usuario.getAtivo())
                .ultimoAcesso(usuario.getUltimoAcesso())
                .build();
    }

    /**
     * Calcula percentual de crescimento
     */
    private BigDecimal calcularPercentualCrescimento(BigDecimal valorAnterior, BigDecimal valorAtual) {
        if (valorAnterior == null || valorAnterior.equals(BigDecimal.ZERO)) {
            return valorAtual.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        
        return valorAtual.subtract(valorAnterior)
                .divide(valorAnterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calcula taxa de conversão de vendas
     */
    private Double calcularTaxaConversaoVendas() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        
        Long vendasTotais = vendaRepository.countByDataVendaBetween(
                inicioMes.atStartOfDay(), hoje.atTime(23, 59, 59));
        Long vendasConfirmadas = vendaRepository.countByStatusAndDataVendaBetween(
                StatusVenda.CONFIRMADA, inicioMes.atStartOfDay(), hoje.atTime(23, 59, 59));
        
        if (vendasTotais == 0) return 0.0;
        
        return (vendasConfirmadas.doubleValue() / vendasTotais.doubleValue()) * 100;
    }

    /**
     * Calcula dias restantes até uma data
     */
    private Integer calcularDiasRestantes(LocalDateTime dataFim) {
        if (dataFim == null) return null;
        
        LocalDate hoje = LocalDate.now();
        LocalDate dataFimDate = dataFim.toLocalDate();
        
        if (dataFimDate.isBefore(hoje)) return 0;
        
        return (int) hoje.until(dataFimDate).getDays();
    }
}
