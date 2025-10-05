package com.qualidade.teste.service;

import com.qualidade.teste.model.Produto;
import com.qualidade.teste.model.ItemVenda;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Serviço responsável por validações e operações relacionadas ao controle de estoque.
 * 
 * Esta classe implementa lógica complexa para:
 * - Validação de disponibilidade de estoque para vendas
 * - Controle de movimentações de entrada e saída
 * - Alertas de estoque baixo e reposição
 * - Validação de regras de negócio para movimentações
 * - Simulação de reservas temporárias de produtos
 * 
 * Complexidade: Não é uma classe CRUD simples, possui múltiplas validações
 * e regras de negócio que podem ser testadas unitariamente.
 */
public class ServicoValidacaoEstoque {
    
    // Dados mockados para simular banco de dados
    private Map<Long, Produto> repositorioProdutos;
    private Map<Long, List<MovimentacaoEstoque>> historicoMovimentacoes;
    private Map<Long, ReservaEstoque> reservasAtivas;
    
    public ServicoValidacaoEstoque() {
        this.repositorioProdutos = new HashMap<>();
        this.historicoMovimentacoes = new HashMap<>();
        this.reservasAtivas = new HashMap<>();
        inicializarDadosMockados();
    }
    
    /**
     * Valida se há estoque suficiente para processar uma lista de itens de venda.
     * 
     * @param itens Lista de itens que se deseja vender
     * @return Resultado da validação indicando sucesso ou problemas encontrados
     */
    public ResultadoValidacaoEstoque validarDisponibilidadeParaVenda(List<ItemVenda> itens) {
        if (itens == null || itens.isEmpty()) {
            return new ResultadoValidacaoEstoque(false, "Lista de itens não pode estar vazia", 
                Collections.emptyList());
        }
        
        List<String> problemas = new ArrayList<>();
        List<ItemComProblema> itensComProblema = new ArrayList<>();
        
        for (ItemVenda item : itens) {
            ValidacaoItem validacao = validarItemIndividual(item);
            if (!validacao.valido) {
                problemas.add(validacao.mensagem);
                itensComProblema.add(new ItemComProblema(
                    item.getProduto().getId(),
                    item.getProduto().getNome(),
                    item.getQuantidade(),
                    validacao.quantidadeDisponivel,
                    validacao.mensagem
                ));
            }
        }
        
        boolean sucesso = problemas.isEmpty();
        String mensagem = sucesso ? "Todos os itens estão disponíveis" : 
            String.format("Encontrados %d problemas de estoque", problemas.size());
        
        return new ResultadoValidacaoEstoque(sucesso, mensagem, itensComProblema);
    }
    
    /**
     * Reserva temporariamente produtos no estoque para uma venda em andamento.
     * A reserva expira automaticamente após um tempo determinado.
     * 
     * @param itens Itens a serem reservados
     * @param tempoExpiracaoMinutos Tempo em minutos para expirar a reserva
     * @return ID da reserva criada ou null se houver problemas
     */
    public Long criarReservaTemporaria(List<ItemVenda> itens, int tempoExpiracaoMinutos) {
        ResultadoValidacaoEstoque validacao = validarDisponibilidadeParaVenda(itens);
        if (!validacao.sucesso) {
            return null;
        }
        
        Long idReserva = System.currentTimeMillis(); // Simula ID único
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(tempoExpiracaoMinutos);
        
        ReservaEstoque reserva = new ReservaEstoque(idReserva, itens, expiracao);
        reservasAtivas.put(idReserva, reserva);
        
        // Atualizar quantidades reservadas nos produtos
        for (ItemVenda item : itens) {
            Produto produto = repositorioProdutos.get(item.getProduto().getId());
            if (produto != null) {
                int novaQuantidade = produto.getQuantidadeEstoque() - item.getQuantidade();
                produto.setQuantidadeEstoque(novaQuantidade);
            }
        }
        
        return idReserva;
    }
    
    /**
     * Confirma uma reserva, efetivando a saída do estoque.
     * 
     * @param idReserva ID da reserva a ser confirmada
     * @return true se a confirmação foi bem-sucedida
     */
    public boolean confirmarReserva(Long idReserva) {
        ReservaEstoque reserva = reservasAtivas.get(idReserva);
        if (reserva == null) {
            return false;
        }
        
        if (reserva.expirou()) {
            cancelarReserva(idReserva);
            return false;
        }
        
        // Registrar movimentações de saída
        LocalDateTime agora = LocalDateTime.now();
        for (ItemVenda item : reserva.itens) {
            registrarMovimentacao(item.getProduto().getId(), 
                TipoMovimentacao.SAIDA, item.getQuantidade(), 
                "Venda confirmada - Reserva " + idReserva, agora);
        }
        
        reservasAtivas.remove(idReserva);
        return true;
    }
    
    /**
     * Cancela uma reserva, devolvendo os produtos ao estoque disponível.
     * 
     * @param idReserva ID da reserva a ser cancelada
     * @return true se o cancelamento foi bem-sucedido
     */
    public boolean cancelarReserva(Long idReserva) {
        ReservaEstoque reserva = reservasAtivas.get(idReserva);
        if (reserva == null) {
            return false;
        }
        
        // Devolver quantidades ao estoque
        for (ItemVenda item : reserva.itens) {
            Produto produto = repositorioProdutos.get(item.getProduto().getId());
            if (produto != null) {
                int novaQuantidade = produto.getQuantidadeEstoque() + item.getQuantidade();
                produto.setQuantidadeEstoque(novaQuantidade);
            }
        }
        
        reservasAtivas.remove(idReserva);
        return true;
    }
    
    /**
     * Processa entrada de mercadorias no estoque.
     * 
     * @param produtoId ID do produto
     * @param quantidade Quantidade a ser adicionada
     * @param motivo Motivo da entrada
     * @return Resultado da operação
     */
    public ResultadoMovimentacaoEstoque processarEntradaEstoque(Long produtoId, int quantidade, String motivo) {
        if (quantidade <= 0) {
            return new ResultadoMovimentacaoEstoque(false, "Quantidade deve ser maior que zero", null);
        }
        
        Produto produto = repositorioProdutos.get(produtoId);
        if (produto == null) {
            return new ResultadoMovimentacaoEstoque(false, "Produto não encontrado", null);
        }
        
        if (!produto.getAtivo()) {
            return new ResultadoMovimentacaoEstoque(false, "Não é possível dar entrada em produto inativo", null);
        }
        
        // Processar entrada
        int quantidadeAnterior = produto.getQuantidadeEstoque();
        int novaQuantidade = quantidadeAnterior + quantidade;
        produto.setQuantidadeEstoque(novaQuantidade);
        
        LocalDateTime agora = LocalDateTime.now();
        MovimentacaoEstoque movimentacao = registrarMovimentacao(produtoId, 
            TipoMovimentacao.ENTRADA, quantidade, motivo, agora);
        
        return new ResultadoMovimentacaoEstoque(true, 
            String.format("Entrada processada: %s passou de %d para %d unidades", 
                produto.getNome(), quantidadeAnterior, novaQuantidade), 
            movimentacao);
    }
    
    /**
     * Processa saída de mercadorias do estoque.
     * 
     * @param produtoId ID do produto
     * @param quantidade Quantidade a ser removida
     * @param motivo Motivo da saída
     * @return Resultado da operação
     */
    public ResultadoMovimentacaoEstoque processarSaidaEstoque(Long produtoId, int quantidade, String motivo) {
        if (quantidade <= 0) {
            return new ResultadoMovimentacaoEstoque(false, "Quantidade deve ser maior que zero", null);
        }
        
        Produto produto = repositorioProdutos.get(produtoId);
        if (produto == null) {
            return new ResultadoMovimentacaoEstoque(false, "Produto não encontrado", null);
        }
        
        if (produto.getQuantidadeEstoque() < quantidade) {
            return new ResultadoMovimentacaoEstoque(false, 
                String.format("Estoque insuficiente. Disponível: %d, Solicitado: %d", 
                    produto.getQuantidadeEstoque(), quantidade), null);
        }
        
        // Processar saída
        int quantidadeAnterior = produto.getQuantidadeEstoque();
        int novaQuantidade = quantidadeAnterior - quantidade;
        produto.setQuantidadeEstoque(novaQuantidade);
        
        LocalDateTime agora = LocalDateTime.now();
        MovimentacaoEstoque movimentacao = registrarMovimentacao(produtoId, 
            TipoMovimentacao.SAIDA, quantidade, motivo, agora);
        
        return new ResultadoMovimentacaoEstoque(true, 
            String.format("Saída processada: %s passou de %d para %d unidades", 
                produto.getNome(), quantidadeAnterior, novaQuantidade), 
            movimentacao);
    }
    
    /**
     * Identifica produtos com estoque baixo que precisam de reposição.
     * 
     * @return Lista de produtos que estão abaixo do estoque mínimo
     */
    public List<AlertaEstoqueBaixo> obterProdutosComEstoqueBaixo() {
        List<AlertaEstoqueBaixo> alertas = new ArrayList<>();
        
        for (Produto produto : repositorioProdutos.values()) {
            if (produto.getAtivo() && produto.isEstoqueBaixo()) {
                int diasSemReposicao = calcularDiasSemReposicao(produto.getId());
                AlertaEstoqueBaixo alerta = new AlertaEstoqueBaixo(
                    produto.getId(),
                    produto.getNome(),
                    produto.getQuantidadeEstoque(),
                    produto.getEstoqueMinimo(),
                    calcularSugestaoReposicao(produto),
                    diasSemReposicao
                );
                alertas.add(alerta);
            }
        }
        
        // Ordenar por prioridade (menor estoque primeiro)
        alertas.sort((a1, a2) -> {
            double ratio1 = (double) a1.quantidadeAtual / a1.estoqueMinimo;
            double ratio2 = (double) a2.quantidadeAtual / a2.estoqueMinimo;
            return Double.compare(ratio1, ratio2);
        });
        
        return alertas;
    }
    
    /**
     * Limpa reservas expiradas do sistema.
     * 
     * @return Número de reservas que foram limpas
     */
    public int limparReservasExpiradas() {
        List<Long> idsParaRemover = new ArrayList<>();
        
        for (Map.Entry<Long, ReservaEstoque> entry : reservasAtivas.entrySet()) {
            if (entry.getValue().expirou()) {
                idsParaRemover.add(entry.getKey());
            }
        }
        
        for (Long id : idsParaRemover) {
            cancelarReserva(id);
        }
        
        return idsParaRemover.size();
    }
    
    // Métodos auxiliares privados
    
    private ValidacaoItem validarItemIndividual(ItemVenda item) {
        if (item == null || item.getProduto() == null) {
            return new ValidacaoItem(false, "Item ou produto inválido", 0);
        }
        
        Produto produto = repositorioProdutos.get(item.getProduto().getId());
        if (produto == null) {
            return new ValidacaoItem(false, "Produto não encontrado", 0);
        }
        
        if (!produto.getAtivo()) {
            return new ValidacaoItem(false, "Produto inativo", 0);
        }
        
        int quantidadeDisponivel = produto.getQuantidadeEstoque();
        int quantidadeSolicitada = item.getQuantidade();
        
        if (quantidadeSolicitada <= 0) {
            return new ValidacaoItem(false, "Quantidade deve ser maior que zero", quantidadeDisponivel);
        }
        
        if (quantidadeDisponivel < quantidadeSolicitada) {
            return new ValidacaoItem(false, 
                String.format("Estoque insuficiente para %s. Disponível: %d, Solicitado: %d", 
                    produto.getNome(), quantidadeDisponivel, quantidadeSolicitada), 
                quantidadeDisponivel);
        }
        
        return new ValidacaoItem(true, "OK", quantidadeDisponivel);
    }
    
    private MovimentacaoEstoque registrarMovimentacao(Long produtoId, TipoMovimentacao tipo, 
            int quantidade, String motivo, LocalDateTime dataMovimentacao) {
        
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(
            System.currentTimeMillis(), // Simula ID único
            produtoId,
            tipo,
            quantidade,
            motivo,
            dataMovimentacao
        );
        
        historicoMovimentacoes.computeIfAbsent(produtoId, k -> new ArrayList<>()).add(movimentacao);
        return movimentacao;
    }
    
    private int calcularDiasSemReposicao(Long produtoId) {
        List<MovimentacaoEstoque> movimentacoes = historicoMovimentacoes.get(produtoId);
        if (movimentacoes == null || movimentacoes.isEmpty()) {
            return 0;
        }
        
        // Buscar última entrada
        LocalDateTime ultimaEntrada = movimentacoes.stream()
            .filter(m -> m.tipo == TipoMovimentacao.ENTRADA)
            .map(m -> m.dataMovimentacao)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        if (ultimaEntrada == null) {
            return 999; // Muitos dias
        }
        
        return (int) java.time.Duration.between(ultimaEntrada, LocalDateTime.now()).toDays();
    }
    
    private int calcularSugestaoReposicao(Produto produto) {
        // Lógica simples: 3x o estoque mínimo menos o atual
        return Math.max(0, (produto.getEstoqueMinimo() * 3) - produto.getQuantidadeEstoque());
    }
    
    private void inicializarDadosMockados() {
        // Criar alguns produtos de exemplo
        Produto produto1 = new Produto(1L, "Vestido Floral", 
            new java.math.BigDecimal("49.99"), Produto.Categoria.ROUPAS, 12);
        produto1.setEstoqueMinimo(5);
        
        Produto produto2 = new Produto(2L, "Camisa Polo", 
            new java.math.BigDecimal("39.99"), Produto.Categoria.ROUPAS, 3);
        produto2.setEstoqueMinimo(5);
        
        Produto produto3 = new Produto(3L, "Tênis Casual", 
            new java.math.BigDecimal("99.99"), Produto.Categoria.CALCADOS, 20);
        produto3.setEstoqueMinimo(8);
        
        repositorioProdutos.put(1L, produto1);
        repositorioProdutos.put(2L, produto2);
        repositorioProdutos.put(3L, produto3);
    }
    
    // Classes auxiliares e enums
    
    public enum TipoMovimentacao {
        ENTRADA, SAIDA
    }
    
    public static class ResultadoValidacaoEstoque {
        private final boolean sucesso;
        private final String mensagem;
        private final List<ItemComProblema> itensComProblema;
        
        public ResultadoValidacaoEstoque(boolean sucesso, String mensagem, 
                List<ItemComProblema> itensComProblema) {
            this.sucesso = sucesso;
            this.mensagem = mensagem;
            this.itensComProblema = itensComProblema;
        }
        
        public boolean isSucesso() { return sucesso; }
        public String getMensagem() { return mensagem; }
        public List<ItemComProblema> getItensComProblema() { return itensComProblema; }
    }
    
    public static class ItemComProblema {
        private final Long produtoId;
        private final String nomeProduto;
        private final int quantidadeSolicitada;
        private final int quantidadeDisponivel;
        private final String problema;
        
        public ItemComProblema(Long produtoId, String nomeProduto, int quantidadeSolicitada, 
                int quantidadeDisponivel, String problema) {
            this.produtoId = produtoId;
            this.nomeProduto = nomeProduto;
            this.quantidadeSolicitada = quantidadeSolicitada;
            this.quantidadeDisponivel = quantidadeDisponivel;
            this.problema = problema;
        }
        
        public Long getProdutoId() { return produtoId; }
        public String getNomeProduto() { return nomeProduto; }
        public int getQuantidadeSolicitada() { return quantidadeSolicitada; }
        public int getQuantidadeDisponivel() { return quantidadeDisponivel; }
        public String getProblema() { return problema; }
    }
    
    public static class ResultadoMovimentacaoEstoque {
        private final boolean sucesso;
        private final String mensagem;
        private final MovimentacaoEstoque movimentacao;
        
        public ResultadoMovimentacaoEstoque(boolean sucesso, String mensagem, 
                MovimentacaoEstoque movimentacao) {
            this.sucesso = sucesso;
            this.mensagem = mensagem;
            this.movimentacao = movimentacao;
        }
        
        public boolean isSucesso() { return sucesso; }
        public String getMensagem() { return mensagem; }
        public MovimentacaoEstoque getMovimentacao() { return movimentacao; }
    }
    
    public static class AlertaEstoqueBaixo {
        private final Long produtoId;
        private final String nomeProduto;
        private final int quantidadeAtual;
        private final int estoqueMinimo;
        private final int sugestaoReposicao;
        private final int diasSemReposicao;
        
        public AlertaEstoqueBaixo(Long produtoId, String nomeProduto, int quantidadeAtual, 
                int estoqueMinimo, int sugestaoReposicao, int diasSemReposicao) {
            this.produtoId = produtoId;
            this.nomeProduto = nomeProduto;
            this.quantidadeAtual = quantidadeAtual;
            this.estoqueMinimo = estoqueMinimo;
            this.sugestaoReposicao = sugestaoReposicao;
            this.diasSemReposicao = diasSemReposicao;
        }
        
        public Long getProdutoId() { return produtoId; }
        public String getNomeProduto() { return nomeProduto; }
        public int getQuantidadeAtual() { return quantidadeAtual; }
        public int getEstoqueMinimo() { return estoqueMinimo; }
        public int getSugestaoReposicao() { return sugestaoReposicao; }
        public int getDiasSemReposicao() { return diasSemReposicao; }
    }
    
    private static class ValidacaoItem {
        final boolean valido;
        final String mensagem;
        final int quantidadeDisponivel;
        
        ValidacaoItem(boolean valido, String mensagem, int quantidadeDisponivel) {
            this.valido = valido;
            this.mensagem = mensagem;
            this.quantidadeDisponivel = quantidadeDisponivel;
        }
    }
    
    private static class MovimentacaoEstoque {
        final Long id;
        final Long produtoId;
        final TipoMovimentacao tipo;
        final int quantidade;
        final String motivo;
        final LocalDateTime dataMovimentacao;
        
        MovimentacaoEstoque(Long id, Long produtoId, TipoMovimentacao tipo, 
                int quantidade, String motivo, LocalDateTime dataMovimentacao) {
            this.id = id;
            this.produtoId = produtoId;
            this.tipo = tipo;
            this.quantidade = quantidade;
            this.motivo = motivo;
            this.dataMovimentacao = dataMovimentacao;
        }
    }
    
    private static class ReservaEstoque {
        final Long id;
        final List<ItemVenda> itens;
        final LocalDateTime expiracao;
        
        ReservaEstoque(Long id, List<ItemVenda> itens, LocalDateTime expiracao) {
            this.id = id;
            this.itens = new ArrayList<>(itens);
            this.expiracao = expiracao;
        }
        
        boolean expirou() {
            return LocalDateTime.now().isAfter(expiracao);
        }
    }
}