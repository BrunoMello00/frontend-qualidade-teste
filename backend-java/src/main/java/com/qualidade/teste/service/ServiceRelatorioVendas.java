package com.qualidade.teste.service;

import com.qualidade.teste.model.Cliente;
import com.qualidade.teste.model.Produto;
import com.qualidade.teste.model.Venda;
import com.qualidade.teste.model.ItemVenda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável por gerar relatórios e análises de vendas.
 * 
 * Esta classe implementa lógica complexa para:
 * - Análise de desempenho de vendas por período
 * - Relatórios de produtos mais vendidos e tendências
 * - Análise de comportamento de clientes
 * - Cálculo de métricas de performance
 * - Identificação de sazonalidades e padrões
 * 
 * Complexidade: Não é uma classe CRUD simples, possui múltiplos cálculos
 * estatísticos e agregações que podem ser testadas unitariamente.
 */
public class ServiceRelatorioVendas {
    
    // Dados mockados para simular banco de dados
    private List<Venda> vendas;
    private Map<Long, Cliente> clientes;
    private Map<Long, Produto> produtos;
    
    public ServiceRelatorioVendas() {
        inicializarDadosMockados();
    }
    
    /**
     * Gera relatório de vendas por período com métricas detalhadas.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Relatório com estatísticas de vendas do período
     */
    public RelatorioVendasPeriodo gerarRelatorioVendasPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null || dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("Período inválido");
        }
        
        List<Venda> vendasPeriodo = filtrarVendasPorPeriodo(dataInicio, dataFim);
        
        if (vendasPeriodo.isEmpty()) {
            return new RelatorioVendasPeriodo(dataInicio, dataFim, BigDecimal.ZERO, 0, 
                BigDecimal.ZERO, BigDecimal.ZERO, Collections.emptyList(), Collections.emptyList());
        }
        
        // Cálculos agregados
        BigDecimal faturamentoTotal = calcularFaturamentoTotal(vendasPeriodo);
        int totalVendas = vendasPeriodo.size();
        BigDecimal ticketMedio = calcularTicketMedio(vendasPeriodo);
        BigDecimal crescimentoPercentual = calcularCrescimentoEmRelacaoAoPeriodoAnterior(
            dataInicio, dataFim, faturamentoTotal);
        
        // Top produtos
        List<ProdutoMaisVendido> topProdutos = calcularTopProdutosMaisVendidos(vendasPeriodo, 5);
        
        // Vendas por categoria
        List<VendasPorCategoria> vendasPorCategoria = calcularVendasPorCategoria(vendasPeriodo);
        
        return new RelatorioVendasPeriodo(dataInicio, dataFim, faturamentoTotal, totalVendas,
            ticketMedio, crescimentoPercentual, topProdutos, vendasPorCategoria);
    }
    
    /**
     * Analisa o comportamento de compras de um cliente específico.
     * 
     * @param clienteId ID do cliente a ser analisado
     * @return Análise detalhada do comportamento do cliente
     */
    public AnaliseComportamentoCliente analisarComportamentoCliente(Long clienteId) {
        Cliente cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado");
        }
        
        List<Venda> vendasCliente = vendas.stream()
            .filter(v -> v.getCliente() != null && v.getCliente().getId().equals(clienteId))
            .collect(Collectors.toList());
        
        if (vendasCliente.isEmpty()) {
            return new AnaliseComportamentoCliente(cliente, BigDecimal.ZERO, 0, null, null,
                BigDecimal.ZERO, Collections.emptyList(), Collections.emptyList());
        }
        
        // Cálculos
        BigDecimal totalGasto = calcularFaturamentoTotal(vendasCliente);
        int frequenciaCompras = vendasCliente.size();
        
        LocalDateTime primeiraCompra = vendasCliente.stream()
            .map(Venda::getDataVenda)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        
        LocalDateTime ultimaCompra = vendasCliente.stream()
            .map(Venda::getDataVenda)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        BigDecimal ticketMedio = totalGasto.divide(new BigDecimal(frequenciaCompras), 2, RoundingMode.HALF_UP);
        
        List<Produto.Categoria> categoriasPreferidas = identificarCategoriasPreferidas(vendasCliente);
        List<MesComGasto> gastosPorMes = calcularGastosPorMes(vendasCliente);
        
        return new AnaliseComportamentoCliente(cliente, totalGasto, frequenciaCompras,
            primeiraCompra, ultimaCompra, ticketMedio, categoriasPreferidas, gastosPorMes);
    }
    
    /**
     * Identifica produtos com baixa performance de vendas.
     * 
     * @param numeroDias Número de dias para análise
     * @return Lista de produtos com vendas abaixo do esperado
     */
    public List<ProdutoBaixaPerformance> identificarProdutosBaixaPerformance(int numeroDias) {
        if (numeroDias <= 0) {
            throw new IllegalArgumentException("Número de dias deve ser positivo");
        }
        
        LocalDate dataLimite = LocalDate.now().minusDays(numeroDias);
        List<Venda> vendasRecentes = vendas.stream()
            .filter(v -> v.getDataVenda().toLocalDate().isAfter(dataLimite))
            .collect(Collectors.toList());
        
        // Mapear vendas por produto
        Map<Long, VendasProduto> vendasPorProduto = new HashMap<>();
        
        for (Venda venda : vendasRecentes) {
            for (ItemVenda item : venda.getItens()) {
                Long produtoId = item.getProduto().getId();
                vendasPorProduto.computeIfAbsent(produtoId, 
                    k -> new VendasProduto(item.getProduto(), 0, BigDecimal.ZERO))
                    .adicionarVenda(item.getQuantidade(), item.getSubtotal());
            }
        }
        
        List<ProdutoBaixaPerformance> produtosBaixaPerformance = new ArrayList<>();
        
        // Analisar cada produto ativo
        for (Produto produto : produtos.values()) {
            if (!produto.getAtivo()) continue;
            
            VendasProduto vendaProduto = vendasPorProduto.get(produto.getId());
            int quantidadeVendida = vendaProduto != null ? vendaProduto.quantidadeTotal : 0;
            BigDecimal faturamento = vendaProduto != null ? vendaProduto.faturamentoTotal : BigDecimal.ZERO;
            
            // Critério de baixa performance: menos de 1 venda por semana
            double vendasPorSemana = (double) quantidadeVendida / (numeroDias / 7.0);
            if (vendasPorSemana < 1.0) {
                int diasSemVenda = calcularDiasSemVenda(produto.getId());
                produtosBaixaPerformance.add(new ProdutoBaixaPerformance(
                    produto, quantidadeVendida, faturamento, diasSemVenda,
                    calcularSugestaoAcao(produto, quantidadeVendida, diasSemVenda)
                ));
            }
        }
        
        // Ordenar por dias sem venda (maior primeiro)
        produtosBaixaPerformance.sort((p1, p2) -> Integer.compare(p2.diasSemVenda, p1.diasSemVenda));
        
        return produtosBaixaPerformance;
    }
    
    /**
     * Calcula métricas de sazonalidade das vendas.
     * 
     * @return Análise de sazonalidade com dados mensais
     */
    public AnaliseSazonalidade calcularSazonalidadeVendas() {
        Map<Month, EstatisticasMensais> estatisticasPorMes = new HashMap<>();
        
        // Inicializar todos os meses
        for (Month mes : Month.values()) {
            estatisticasPorMes.put(mes, new EstatisticasMensais(mes));
        }
        
        // Processar vendas
        for (Venda venda : vendas) {
            Month mes = venda.getDataVenda().getMonth();
            EstatisticasMensais stats = estatisticasPorMes.get(mes);
            stats.adicionarVenda(venda.getValorTotal());
        }
        
        // Identificar picos e vales
        Month mesMaiorVenda = estatisticasPorMes.entrySet().stream()
            .max(Map.Entry.comparingByValue((s1, s2) -> s1.faturamentoTotal.compareTo(s2.faturamentoTotal)))
            .map(Map.Entry::getKey)
            .orElse(Month.JANUARY);
        
        Month mesMenorVenda = estatisticasPorMes.entrySet().stream()
            .min(Map.Entry.comparingByValue((s1, s2) -> s1.faturamentoTotal.compareTo(s2.faturamentoTotal)))
            .map(Map.Entry::getKey)
            .orElse(Month.JANUARY);
        
        BigDecimal faturamentoMedio = estatisticasPorMes.values().stream()
            .map(s -> s.faturamentoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);
        
        List<EstatisticasMensais> estatisticasOrdenadas = new ArrayList<>(estatisticasPorMes.values());
        estatisticasOrdenadas.sort(Comparator.comparing(EstatisticasMensais::getMes));
        
        return new AnaliseSazonalidade(mesMaiorVenda, mesMenorVenda, faturamentoMedio, 
            estatisticasOrdenadas);
    }
    
    /**
     * Gera previsão de vendas baseada em dados históricos.
     * 
     * @param mesesPrevisao Número de meses para prever
     * @return Previsão de vendas
     */
    public PrevisaoVendas gerarPrevisaoVendas(int mesesPrevisao) {
        if (mesesPrevisao <= 0 || mesesPrevisao > 12) {
            throw new IllegalArgumentException("Meses de previsão deve estar entre 1 e 12");
        }
        
        // Calcular média dos últimos 12 meses
        LocalDate dataInicio = LocalDate.now().minusMonths(12);
        List<Venda> vendasUltimoAno = filtrarVendasPorPeriodo(dataInicio, LocalDate.now());
        
        if (vendasUltimoAno.isEmpty()) {
            return new PrevisaoVendas(Collections.emptyList(), BigDecimal.ZERO, 
                "Dados insuficientes para previsão");
        }
        
        BigDecimal faturamentoMedioMensal = calcularFaturamentoTotal(vendasUltimoAno)
            .divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);
        
        // Aplicar fator de crescimento baseado na tendência
        BigDecimal fatorCrescimento = calcularFatorCrescimentoTendencia(vendasUltimoAno);
        
        List<PrevisaoMensal> previsoesMensais = new ArrayList<>();
        LocalDate mesAtual = LocalDate.now().withDayOfMonth(1);
        
        for (int i = 0; i < mesesPrevisao; i++) {
            LocalDate mesPrevisao = mesAtual.plusMonths(i + 1);
            
            // Aplicar sazonalidade
            BigDecimal fatorSazonalidade = obterFatorSazonalidadeMes(mesPrevisao.getMonth());
            
            BigDecimal faturamentoPrevisto = faturamentoMedioMensal
                .multiply(fatorCrescimento)
                .multiply(fatorSazonalidade)
                .setScale(2, RoundingMode.HALF_UP);
            
            previsoesMensais.add(new PrevisaoMensal(mesPrevisao, faturamentoPrevisto));
        }
        
        BigDecimal totalPrevisto = previsoesMensais.stream()
            .map(PrevisaoMensal::getFaturamentoPrevisto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new PrevisaoVendas(previsoesMensais, totalPrevisto, 
            "Previsão baseada em média histórica com ajuste de sazonalidade");
    }
    
    // Métodos auxiliares privados
    
    private List<Venda> filtrarVendasPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return vendas.stream()
            .filter(v -> {
                LocalDate dataVenda = v.getDataVenda().toLocalDate();
                return !dataVenda.isBefore(dataInicio) && !dataVenda.isAfter(dataFim);
            })
            .collect(Collectors.toList());
    }
    
    private BigDecimal calcularFaturamentoTotal(List<Venda> vendas) {
        return vendas.stream()
            .map(Venda::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calcularTicketMedio(List<Venda> vendas) {
        if (vendas.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = calcularFaturamentoTotal(vendas);
        return total.divide(new BigDecimal(vendas.size()), 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calcularCrescimentoEmRelacaoAoPeriodoAnterior(LocalDate dataInicio, 
            LocalDate dataFim, BigDecimal faturamentoAtual) {
        
        long diasPeriodo = java.time.temporal.ChronoUnit.DAYS.between(dataInicio, dataFim);
        LocalDate inicioAnterior = dataInicio.minusDays(diasPeriodo);
        LocalDate fimAnterior = dataInicio.minusDays(1);
        
        List<Venda> vendasPeriodoAnterior = filtrarVendasPorPeriodo(inicioAnterior, fimAnterior);
        BigDecimal faturamentoAnterior = calcularFaturamentoTotal(vendasPeriodoAnterior);
        
        if (faturamentoAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return faturamentoAtual.subtract(faturamentoAnterior)
            .divide(faturamentoAnterior, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    private List<ProdutoMaisVendido> calcularTopProdutosMaisVendidos(List<Venda> vendas, int limite) {
        Map<Long, VendasProduto> vendasPorProduto = new HashMap<>();
        
        for (Venda venda : vendas) {
            for (ItemVenda item : venda.getItens()) {
                Long produtoId = item.getProduto().getId();
                vendasPorProduto.computeIfAbsent(produtoId, 
                    k -> new VendasProduto(item.getProduto(), 0, BigDecimal.ZERO))
                    .adicionarVenda(item.getQuantidade(), item.getSubtotal());
            }
        }
        
        return vendasPorProduto.values().stream()
            .map(vp -> new ProdutoMaisVendido(vp.produto, vp.quantidadeTotal, vp.faturamentoTotal))
            .sorted((p1, p2) -> Integer.compare(p2.quantidadeVendida, p1.quantidadeVendida))
            .limit(limite)
            .collect(Collectors.toList());
    }
    
    private List<VendasPorCategoria> calcularVendasPorCategoria(List<Venda> vendas) {
        Map<Produto.Categoria, VendasCategoria> vendasPorCategoria = new HashMap<>();
        
        for (Venda venda : vendas) {
            for (ItemVenda item : venda.getItens()) {
                Produto.Categoria categoria = item.getProduto().getCategoria();
                vendasPorCategoria.computeIfAbsent(categoria, 
                    k -> new VendasCategoria(categoria, 0, BigDecimal.ZERO))
                    .adicionarVenda(item.getQuantidade(), item.getSubtotal());
            }
        }
        
        return vendasPorCategoria.values().stream()
            .map(vc -> new VendasPorCategoria(vc.categoria, vc.quantidadeTotal, vc.faturamentoTotal))
            .sorted((c1, c2) -> c2.faturamento.compareTo(c1.faturamento))
            .collect(Collectors.toList());
    }
    
    private List<Produto.Categoria> identificarCategoriasPreferidas(List<Venda> vendasCliente) {
        Map<Produto.Categoria, Integer> contagemPorCategoria = new HashMap<>();
        
        for (Venda venda : vendasCliente) {
            for (ItemVenda item : venda.getItens()) {
                Produto.Categoria categoria = item.getProduto().getCategoria();
                contagemPorCategoria.merge(categoria, item.getQuantidade(), Integer::sum);
            }
        }
        
        return contagemPorCategoria.entrySet().stream()
            .sorted(Map.Entry.<Produto.Categoria, Integer>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .limit(3)
            .collect(Collectors.toList());
    }
    
    private List<MesComGasto> calcularGastosPorMes(List<Venda> vendasCliente) {
        Map<String, BigDecimal> gastosPorMes = new HashMap<>();
        
        for (Venda venda : vendasCliente) {
            String mesAno = venda.getDataVenda().getYear() + "-" + 
                String.format("%02d", venda.getDataVenda().getMonthValue());
            gastosPorMes.merge(mesAno, venda.getValorTotal(), BigDecimal::add);
        }
        
        return gastosPorMes.entrySet().stream()
            .map(entry -> new MesComGasto(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(MesComGasto::getMesAno))
            .collect(Collectors.toList());
    }
    
    private int calcularDiasSemVenda(Long produtoId) {
        LocalDateTime ultimaVenda = vendas.stream()
            .flatMap(v -> v.getItens().stream())
            .filter(item -> item.getProduto().getId().equals(produtoId))
            .map(item -> vendas.stream()
                .filter(v -> v.getItens().contains(item))
                .findFirst()
                .map(Venda::getDataVenda)
                .orElse(null))
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        if (ultimaVenda == null) {
            return 999; // Produto nunca vendido
        }
        
        return (int) java.time.temporal.ChronoUnit.DAYS.between(ultimaVenda.toLocalDate(), LocalDate.now());
    }
    
    private String calcularSugestaoAcao(Produto produto, int quantidadeVendida, int diasSemVenda) {
        if (diasSemVenda > 180) {
            return "Considerar descontinuar produto";
        } else if (diasSemVenda > 90) {
            return "Aplicar promoção ou desconto";
        } else if (diasSemVenda > 30) {
            return "Revisar estratégia de marketing";
        } else {
            return "Monitorar performance";
        }
    }
    
    private BigDecimal calcularFatorCrescimentoTendencia(List<Venda> vendasUltimoAno) {
        // Simplificação: assumir crescimento de 2% baseado no histórico
        return new BigDecimal("1.02");
    }
    
    private BigDecimal obterFatorSazonalidadeMes(Month mes) {
        // Fatores de sazonalidade mockados
        Map<Month, BigDecimal> fatores = new HashMap<>();
        fatores.put(Month.JANUARY, new BigDecimal("0.8"));
        fatores.put(Month.FEBRUARY, new BigDecimal("0.9"));
        fatores.put(Month.MARCH, new BigDecimal("1.0"));
        fatores.put(Month.APRIL, new BigDecimal("1.1"));
        fatores.put(Month.MAY, new BigDecimal("1.2"));
        fatores.put(Month.JUNE, new BigDecimal("1.0"));
        fatores.put(Month.JULY, new BigDecimal("0.9"));
        fatores.put(Month.AUGUST, new BigDecimal("1.0"));
        fatores.put(Month.SEPTEMBER, new BigDecimal("1.1"));
        fatores.put(Month.OCTOBER, new BigDecimal("1.2"));
        fatores.put(Month.NOVEMBER, new BigDecimal("1.4"));
        fatores.put(Month.DECEMBER, new BigDecimal("1.5"));
        
        return fatores.getOrDefault(mes, new BigDecimal("1.0"));
    }
    
    private void inicializarDadosMockados() {
        // Inicializar clientes
        clientes = new HashMap<>();
        Cliente cliente1 = new Cliente(1L, "João Silva", "123.456.789-00", "joao@email.com", Cliente.TipoCliente.OURO);
        cliente1.setPontosFidelidade(2500);
        clientes.put(1L, cliente1);
        
        Cliente cliente2 = new Cliente(2L, "Maria Santos", "987.654.321-00", "maria@email.com", Cliente.TipoCliente.PREMIUM);
        cliente2.setPontosFidelidade(5000);
        clientes.put(2L, cliente2);
        
        // Inicializar produtos
        produtos = new HashMap<>();
        Produto produto1 = new Produto(1L, "Vestido Floral", new BigDecimal("49.99"), Produto.Categoria.ROUPAS, 12);
        Produto produto2 = new Produto(2L, "Camisa Polo", new BigDecimal("39.99"), Produto.Categoria.ROUPAS, 3);
        Produto produto3 = new Produto(3L, "Tênis Casual", new BigDecimal("99.99"), Produto.Categoria.CALCADOS, 20);
        
        produtos.put(1L, produto1);
        produtos.put(2L, produto2);
        produtos.put(3L, produto3);
        
        // Inicializar vendas com dados dos últimos meses
        vendas = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        
        // Criar vendas mockadas
        for (int i = 0; i < 50; i++) {
            LocalDateTime dataVenda = agora.minusDays((long) (Math.random() * 365));
            Venda venda = new Venda(cliente1, Arrays.asList(
                new ItemVenda(produto1, 1, produto1.getPreco()),
                new ItemVenda(produto2, 2, produto2.getPreco())
            ));
            venda.setId((long) i);
            venda.setDataVenda(dataVenda);
            venda.setValorTotal(produto1.getPreco().add(produto2.getPreco().multiply(new BigDecimal(2))));
            venda.setStatus(Venda.StatusVenda.FINALIZADA);
            vendas.add(venda);
        }
    }
    
    // Classes auxiliares internas para resultados
    
    public static class RelatorioVendasPeriodo {
        private final LocalDate dataInicio;
        private final LocalDate dataFim;
        private final BigDecimal faturamentoTotal;
        private final int totalVendas;
        private final BigDecimal ticketMedio;
        private final BigDecimal crescimentoPercentual;
        private final List<ProdutoMaisVendido> topProdutos;
        private final List<VendasPorCategoria> vendasPorCategoria;
        
        public RelatorioVendasPeriodo(LocalDate dataInicio, LocalDate dataFim, BigDecimal faturamentoTotal,
                int totalVendas, BigDecimal ticketMedio, BigDecimal crescimentoPercentual,
                List<ProdutoMaisVendido> topProdutos, List<VendasPorCategoria> vendasPorCategoria) {
            this.dataInicio = dataInicio;
            this.dataFim = dataFim;
            this.faturamentoTotal = faturamentoTotal;
            this.totalVendas = totalVendas;
            this.ticketMedio = ticketMedio;
            this.crescimentoPercentual = crescimentoPercentual;
            this.topProdutos = topProdutos;
            this.vendasPorCategoria = vendasPorCategoria;
        }
        
        // Getters
        public LocalDate getDataInicio() { return dataInicio; }
        public LocalDate getDataFim() { return dataFim; }
        public BigDecimal getFaturamentoTotal() { return faturamentoTotal; }
        public int getTotalVendas() { return totalVendas; }
        public BigDecimal getTicketMedio() { return ticketMedio; }
        public BigDecimal getCrescimentoPercentual() { return crescimentoPercentual; }
        public List<ProdutoMaisVendido> getTopProdutos() { return topProdutos; }
        public List<VendasPorCategoria> getVendasPorCategoria() { return vendasPorCategoria; }
    }
    
    public static class AnaliseComportamentoCliente {
        private final Cliente cliente;
        private final BigDecimal totalGasto;
        private final int frequenciaCompras;
        private final LocalDateTime primeiraCompra;
        private final LocalDateTime ultimaCompra;
        private final BigDecimal ticketMedio;
        private final List<Produto.Categoria> categoriasPreferidas;
        private final List<MesComGasto> gastosPorMes;
        
        public AnaliseComportamentoCliente(Cliente cliente, BigDecimal totalGasto, int frequenciaCompras,
                LocalDateTime primeiraCompra, LocalDateTime ultimaCompra, BigDecimal ticketMedio,
                List<Produto.Categoria> categoriasPreferidas, List<MesComGasto> gastosPorMes) {
            this.cliente = cliente;
            this.totalGasto = totalGasto;
            this.frequenciaCompras = frequenciaCompras;
            this.primeiraCompra = primeiraCompra;
            this.ultimaCompra = ultimaCompra;
            this.ticketMedio = ticketMedio;
            this.categoriasPreferidas = categoriasPreferidas;
            this.gastosPorMes = gastosPorMes;
        }
        
        // Getters
        public Cliente getCliente() { return cliente; }
        public BigDecimal getTotalGasto() { return totalGasto; }
        public int getFrequenciaCompras() { return frequenciaCompras; }
        public LocalDateTime getPrimeiraCompra() { return primeiraCompra; }
        public LocalDateTime getUltimaCompra() { return ultimaCompra; }
        public BigDecimal getTicketMedio() { return ticketMedio; }
        public List<Produto.Categoria> getCategoriasPreferidas() { return categoriasPreferidas; }
        public List<MesComGasto> getGastosPorMes() { return gastosPorMes; }
    }
    
    public static class ProdutoBaixaPerformance {
        private final Produto produto;
        private final int quantidadeVendida;
        private final BigDecimal faturamento;
        private final int diasSemVenda;
        private final String sugestaoAcao;
        
        public ProdutoBaixaPerformance(Produto produto, int quantidadeVendida, BigDecimal faturamento,
                int diasSemVenda, String sugestaoAcao) {
            this.produto = produto;
            this.quantidadeVendida = quantidadeVendida;
            this.faturamento = faturamento;
            this.diasSemVenda = diasSemVenda;
            this.sugestaoAcao = sugestaoAcao;
        }
        
        // Getters
        public Produto getProduto() { return produto; }
        public int getQuantidadeVendida() { return quantidadeVendida; }
        public BigDecimal getFaturamento() { return faturamento; }
        public int getDiasSemVenda() { return diasSemVenda; }
        public String getSugestaoAcao() { return sugestaoAcao; }
    }
    
    public static class AnaliseSazonalidade {
        private final Month mesMaiorVenda;
        private final Month mesMenorVenda;
        private final BigDecimal faturamentoMedio;
        private final List<EstatisticasMensais> estatisticasPorMes;
        
        public AnaliseSazonalidade(Month mesMaiorVenda, Month mesMenorVenda, BigDecimal faturamentoMedio,
                List<EstatisticasMensais> estatisticasPorMes) {
            this.mesMaiorVenda = mesMaiorVenda;
            this.mesMenorVenda = mesMenorVenda;
            this.faturamentoMedio = faturamentoMedio;
            this.estatisticasPorMes = estatisticasPorMes;
        }
        
        // Getters
        public Month getMesMaiorVenda() { return mesMaiorVenda; }
        public Month getMesMenorVenda() { return mesMenorVenda; }
        public BigDecimal getFaturamentoMedio() { return faturamentoMedio; }
        public List<EstatisticasMensais> getEstatisticasPorMes() { return estatisticasPorMes; }
    }
    
    public static class PrevisaoVendas {
        private final List<PrevisaoMensal> previsoesMensais;
        private final BigDecimal totalPrevisto;
        private final String metodologia;
        
        public PrevisaoVendas(List<PrevisaoMensal> previsoesMensais, BigDecimal totalPrevisto, String metodologia) {
            this.previsoesMensais = previsoesMensais;
            this.totalPrevisto = totalPrevisto;
            this.metodologia = metodologia;
        }
        
        // Getters
        public List<PrevisaoMensal> getPrevisoesMensais() { return previsoesMensais; }
        public BigDecimal getTotalPrevisto() { return totalPrevisto; }
        public String getMetodologia() { return metodologia; }
    }
    
    // Classes auxiliares menores
    
    public static class ProdutoMaisVendido {
        private final Produto produto;
        private final int quantidadeVendida;
        private final BigDecimal faturamento;
        
        public ProdutoMaisVendido(Produto produto, int quantidadeVendida, BigDecimal faturamento) {
            this.produto = produto;
            this.quantidadeVendida = quantidadeVendida;
            this.faturamento = faturamento;
        }
        
        public Produto getProduto() { return produto; }
        public int getQuantidadeVendida() { return quantidadeVendida; }
        public BigDecimal getFaturamento() { return faturamento; }
    }
    
    public static class VendasPorCategoria {
        private final Produto.Categoria categoria;
        private final int quantidade;
        private final BigDecimal faturamento;
        
        public VendasPorCategoria(Produto.Categoria categoria, int quantidade, BigDecimal faturamento) {
            this.categoria = categoria;
            this.quantidade = quantidade;
            this.faturamento = faturamento;
        }
        
        public Produto.Categoria getCategoria() { return categoria; }
        public int getQuantidade() { return quantidade; }
        public BigDecimal getFaturamento() { return faturamento; }
    }
    
    public static class MesComGasto {
        private final String mesAno;
        private final BigDecimal gasto;
        
        public MesComGasto(String mesAno, BigDecimal gasto) {
            this.mesAno = mesAno;
            this.gasto = gasto;
        }
        
        public String getMesAno() { return mesAno; }
        public BigDecimal getGasto() { return gasto; }
    }
    
    public static class EstatisticasMensais {
        private final Month mes;
        private int totalVendas;
        private BigDecimal faturamentoTotal;
        
        public EstatisticasMensais(Month mes) {
            this.mes = mes;
            this.totalVendas = 0;
            this.faturamentoTotal = BigDecimal.ZERO;
        }
        
        public void adicionarVenda(BigDecimal valor) {
            this.totalVendas++;
            this.faturamentoTotal = this.faturamentoTotal.add(valor);
        }
        
        public Month getMes() { return mes; }
        public int getTotalVendas() { return totalVendas; }
        public BigDecimal getFaturamentoTotal() { return faturamentoTotal; }
    }
    
    public static class PrevisaoMensal {
        private final LocalDate mes;
        private final BigDecimal faturamentoPrevisto;
        
        public PrevisaoMensal(LocalDate mes, BigDecimal faturamentoPrevisto) {
            this.mes = mes;
            this.faturamentoPrevisto = faturamentoPrevisto;
        }
        
        public LocalDate getMes() { return mes; }
        public BigDecimal getFaturamentoPrevisto() { return faturamentoPrevisto; }
    }
    
    private static class VendasProduto {
        final Produto produto;
        int quantidadeTotal;
        BigDecimal faturamentoTotal;
        
        VendasProduto(Produto produto, int quantidadeTotal, BigDecimal faturamentoTotal) {
            this.produto = produto;
            this.quantidadeTotal = quantidadeTotal;
            this.faturamentoTotal = faturamentoTotal;
        }
        
        void adicionarVenda(int quantidade, BigDecimal valor) {
            this.quantidadeTotal += quantidade;
            this.faturamentoTotal = this.faturamentoTotal.add(valor);
        }
    }
    
    private static class VendasCategoria {
        final Produto.Categoria categoria;
        int quantidadeTotal;
        BigDecimal faturamentoTotal;
        
        VendasCategoria(Produto.Categoria categoria, int quantidadeTotal, BigDecimal faturamentoTotal) {
            this.categoria = categoria;
            this.quantidadeTotal = quantidadeTotal;
            this.faturamentoTotal = faturamentoTotal;
        }
        
        void adicionarVenda(int quantidade, BigDecimal valor) {
            this.quantidadeTotal += quantidade;
            this.faturamentoTotal = this.faturamentoTotal.add(valor);
        }
    }
}