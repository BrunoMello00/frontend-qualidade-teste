package com.qualidade.teste.service;

import com.qualidade.teste.model.Cliente;
import com.qualidade.teste.model.Produto;
import com.qualidade.teste.model.Venda;
import com.qualidade.teste.model.ItemVenda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

/**
 * Serviço responsável por calcular descontos baseados em regras de negócio complexas.
 * 
 * Esta classe implementa lógica não-trivial para cálculo de descontos considerando:
 * - Tipo de cliente e programa de fidelidade
 * - Quantidade de itens na compra
 * - Categoria de produtos e promoções ativas
 * - Histórico de compras e sazonalidade
 * - Cupons de desconto e validações
 * 
 * Complexidade: Não é uma classe CRUD simples, possui múltiplas regras de negócio
 * e validações que podem ser testadas unitariamente.
 */
public class ServiceCalculoDesconto {
    
    // Dados mockados para simular configurações do sistema
    private static final Map<Cliente.TipoCliente, BigDecimal> DESCONTOS_POR_TIPO_CLIENTE = Map.of(
        Cliente.TipoCliente.BRONZE, new BigDecimal("0"),
        Cliente.TipoCliente.PRATA, new BigDecimal("5"),
        Cliente.TipoCliente.OURO, new BigDecimal("10"),
        Cliente.TipoCliente.PREMIUM, new BigDecimal("15")
    );
    
    private static final Map<Produto.Categoria, BigDecimal> PROMOCOES_CATEGORIA = Map.of(
        Produto.Categoria.ROUPAS, new BigDecimal("15"),
        Produto.Categoria.CALCADOS, new BigDecimal("10"),
        Produto.Categoria.ELETRONICOS, new BigDecimal("5"),
        Produto.Categoria.CASA, new BigDecimal("12"),
        Produto.Categoria.ACESSORIOS, new BigDecimal("8")
    );
    
    private static final List<DescontoPorQuantidade> DESCONTOS_QUANTIDADE = Arrays.asList(
        new DescontoPorQuantidade(50, new BigDecimal("15")),
        new DescontoPorQuantidade(20, new BigDecimal("10")),
        new DescontoPorQuantidade(10, new BigDecimal("5"))
    );
    
    private static final Map<String, CupomDesconto> CUPONS_VALIDOS = Map.of(
        "PRIMEIRACOMPRA", new CupomDesconto("PRIMEIRACOMPRA", new BigDecimal("15"), Cliente.TipoCliente.BRONZE),
        "FIDELIDADE20", new CupomDesconto("FIDELIDADE20", new BigDecimal("20"), Cliente.TipoCliente.OURO),
        "BLACKFRIDAY", new CupomDesconto("BLACKFRIDAY", new BigDecimal("25"), Cliente.TipoCliente.BRONZE),
        "PREMIUM50", new CupomDesconto("PREMIUM50", new BigDecimal("50"), Cliente.TipoCliente.PREMIUM)
    );
    
    /**
     * Calcula o desconto total para uma venda considerando todas as regras de negócio.
     * 
     * @param cliente Cliente que está realizando a compra
     * @param venda Venda com os itens e valores
     * @param cupomDesconto Cupom de desconto opcional
     * @return ResultadoCalculoDesconto com detalhes do desconto aplicado
     * @throws IllegalArgumentException se os parâmetros forem inválidos
     */
    public ResultadoCalculoDesconto calcularDescontoVenda(Cliente cliente, Venda venda, String cupomDesconto) {
        validarParametrosCalculoVenda(cliente, venda);
        
        BigDecimal valorOriginal = calcularValorOriginalVenda(venda);
        BigDecimal percentualDescontoTotal = BigDecimal.ZERO;
        List<String> justificativas = new ArrayList<>();
        
        // 1. Desconto por tipo de cliente
        BigDecimal descontoTipoCliente = calcularDescontoTipoCliente(cliente);
        if (descontoTipoCliente.compareTo(BigDecimal.ZERO) > 0) {
            percentualDescontoTotal = percentualDescontoTotal.add(descontoTipoCliente);
            justificativas.add(String.format("Desconto %s: %.1f%%", 
                cliente.getTipoCliente(), descontoTipoCliente));
        }
        
        // 2. Desconto por fidelidade
        BigDecimal descontoFidelidade = calcularDescontoFidelidade(cliente.getPontosFidelidade());
        if (descontoFidelidade.compareTo(BigDecimal.ZERO) > 0) {
            percentualDescontoTotal = percentualDescontoTotal.add(descontoFidelidade);
            justificativas.add(String.format("Desconto fidelidade: %.1f%% (%d pontos)", 
                descontoFidelidade, cliente.getPontosFidelidade()));
        }
        
        // 3. Desconto por quantidade
        int quantidadeTotal = venda.getTotalItens();
        BigDecimal descontoQuantidade = calcularDescontoPorQuantidade(quantidadeTotal);
        if (descontoQuantidade.compareTo(BigDecimal.ZERO) > 0) {
            percentualDescontoTotal = percentualDescontoTotal.add(descontoQuantidade);
            justificativas.add(String.format("Desconto por quantidade: %.1f%% (%d itens)", 
                descontoQuantidade, quantidadeTotal));
        }
        
        // 4. Desconto por categoria
        BigDecimal descontoCategoria = calcularDescontoCategoria(venda);
        if (descontoCategoria.compareTo(BigDecimal.ZERO) > 0) {
            percentualDescontoTotal = percentualDescontoTotal.add(descontoCategoria);
            justificativas.add(String.format("Desconto por categoria: %.1f%%", descontoCategoria));
        }
        
        // 5. Validar e aplicar cupom
        if (cupomDesconto != null && !cupomDesconto.trim().isEmpty()) {
            BigDecimal descontoCupom = calcularDescontoCupom(cupomDesconto, cliente);
            if (descontoCupom.compareTo(BigDecimal.ZERO) > 0) {
                percentualDescontoTotal = percentualDescontoTotal.add(descontoCupom);
                justificativas.add(String.format("Cupom %s: %.1f%%", cupomDesconto, descontoCupom));
            }
        }
        
        // 6. Aplicar limites máximos
        percentualDescontoTotal = aplicarLimiteMaximoDesconto(percentualDescontoTotal, venda, cliente);
        
        // Calcular valores finais
        BigDecimal valorDesconto = valorOriginal.multiply(percentualDescontoTotal)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal valorFinal = valorOriginal.subtract(valorDesconto);
        
        return new ResultadoCalculoDesconto(
            valorOriginal,
            percentualDescontoTotal,
            valorDesconto,
            valorFinal,
            String.join("; ", justificativas)
        );
    }
    
    /**
     * Calcula desconto especial para clientes aniversariantes.
     * 
     * @param cliente Cliente para verificar aniversário
     * @return Percentual de desconto de aniversário
     */
    public BigDecimal calcularDescontoAniversario(Cliente cliente) {
        if (cliente.getDataAniversario() == null) {
            return BigDecimal.ZERO;
        }
        
        LocalDate hoje = LocalDate.now();
        Month mesAtual = hoje.getMonth();
        Month mesAniversario = cliente.getDataAniversario().getMonth();
        
        if (mesAtual != mesAniversario) {
            return BigDecimal.ZERO;
        }
        
        // Desconto base de aniversário
        BigDecimal percentualDesconto = new BigDecimal("10");
        
        // Bônus por tipo de cliente
        switch (cliente.getTipoCliente()) {
            case PRATA:
                percentualDesconto = percentualDesconto.add(new BigDecimal("5"));
                break;
            case OURO:
                percentualDesconto = percentualDesconto.add(new BigDecimal("10"));
                break;
            case PREMIUM:
                percentualDesconto = percentualDesconto.add(new BigDecimal("15"));
                break;
            default:
                break;
        }
        
        // Bônus se é a primeira compra do ano
        if (isPrimeiraCompraDoAno(cliente)) {
            percentualDesconto = percentualDesconto.add(new BigDecimal("5"));
        }
        
        return percentualDesconto;
    }
    
    /**
     * Valida se um desconto proposto está dentro das regras permitidas.
     * 
     * @param percentualDesconto Percentual de desconto a ser validado
     * @param cliente Cliente que receberá o desconto
     * @param produto Produto para o qual o desconto será aplicado
     * @return Resultado da validação com sucesso/erro e mensagem
     */
    public ResultadoValidacaoDesconto validarDescontoPermitido(
            BigDecimal percentualDesconto, Cliente cliente, Produto produto) {
        
        // Validar parâmetros
        if (percentualDesconto == null || percentualDesconto.compareTo(BigDecimal.ZERO) < 0) {
            return new ResultadoValidacaoDesconto(false, "Percentual de desconto inválido");
        }
        
        if (cliente == null || produto == null) {
            return new ResultadoValidacaoDesconto(false, "Cliente e produto são obrigatórios");
        }
        
        // Validar se produto está ativo
        if (!produto.getAtivo()) {
            return new ResultadoValidacaoDesconto(false, "Produto inativo não pode receber desconto");
        }
        
        // Validar estoque
        if (produto.isSemEstoque()) {
            return new ResultadoValidacaoDesconto(false, "Produto sem estoque não pode receber desconto");
        }
        
        // Validar limite máximo do produto
        if (percentualDesconto.compareTo(produto.getPercentualMaximoDesconto()) > 0) {
            return new ResultadoValidacaoDesconto(false, 
                String.format("Desconto máximo permitido para %s é %.1f%%", 
                    produto.getNome(), produto.getPercentualMaximoDesconto()));
        }
        
        // Validar limite máximo do cliente
        BigDecimal descontoMaximoCliente = obterDescontoMaximoCliente(cliente.getTipoCliente());
        if (percentualDesconto.compareTo(descontoMaximoCliente) > 0) {
            return new ResultadoValidacaoDesconto(false, 
                String.format("Desconto máximo para cliente %s é %.1f%%", 
                    cliente.getTipoCliente(), descontoMaximoCliente));
        }
        
        return new ResultadoValidacaoDesconto(true, "Desconto válido");
    }
    
    // Métodos auxiliares privados
    
    private void validarParametrosCalculoVenda(Cliente cliente, Venda venda) {
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não pode ser nulo");
        }
        if (venda == null || venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalArgumentException("Venda deve conter pelo menos um item");
        }
    }
    
    private BigDecimal calcularValorOriginalVenda(Venda venda) {
        return venda.getItens().stream()
            .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calcularDescontoTipoCliente(Cliente cliente) {
        return DESCONTOS_POR_TIPO_CLIENTE.getOrDefault(cliente.getTipoCliente(), BigDecimal.ZERO);
    }
    
    private BigDecimal calcularDescontoFidelidade(Integer pontos) {
        if (pontos == null || pontos <= 0) return BigDecimal.ZERO;
        
        if (pontos >= 10000) return new BigDecimal("10");
        if (pontos >= 5000) return new BigDecimal("7");
        if (pontos >= 1000) return new BigDecimal("5");
        if (pontos >= 500) return new BigDecimal("3");
        
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calcularDescontoPorQuantidade(int quantidade) {
        return DESCONTOS_QUANTIDADE.stream()
            .filter(desconto -> quantidade >= desconto.quantidadeMinima)
            .map(desconto -> desconto.percentual)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }
    
    private BigDecimal calcularDescontoCategoria(Venda venda) {
        // Pega a maior promoção de categoria entre os produtos da venda
        return venda.getItens().stream()
            .map(item -> PROMOCOES_CATEGORIA.getOrDefault(
                item.getProduto().getCategoria(), BigDecimal.ZERO))
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }
    
    private BigDecimal calcularDescontoCupom(String cupom, Cliente cliente) {
        CupomDesconto cupomData = CUPONS_VALIDOS.get(cupom.toUpperCase());
        if (cupomData == null) {
            return BigDecimal.ZERO;
        }
        
        // Verificar se cliente tem nível mínimo para o cupom
        if (!possuiNivelMinimoParaCupom(cliente.getTipoCliente(), cupomData.tipoClienteMinimo)) {
            return BigDecimal.ZERO;
        }
        
        return cupomData.percentual;
    }
    
    private boolean possuiNivelMinimoParaCupom(Cliente.TipoCliente tipoCliente, Cliente.TipoCliente tipoMinimo) {
        List<Cliente.TipoCliente> hierarquia = Arrays.asList(
            Cliente.TipoCliente.BRONZE, 
            Cliente.TipoCliente.PRATA, 
            Cliente.TipoCliente.OURO, 
            Cliente.TipoCliente.PREMIUM
        );
        
        return hierarquia.indexOf(tipoCliente) >= hierarquia.indexOf(tipoMinimo);
    }
    
    private BigDecimal aplicarLimiteMaximoDesconto(BigDecimal percentual, Venda venda, Cliente cliente) {
        // Limite geral do sistema: 70%
        BigDecimal limiteGeral = new BigDecimal("70");
        
        // Limite por categoria: eletrônicos têm limite menor
        boolean temEletronicos = venda.getItens().stream()
            .anyMatch(item -> item.getProduto().getCategoria() == Produto.Categoria.ELETRONICOS);
        BigDecimal limiteCategoria = temEletronicos ? new BigDecimal("30") : new BigDecimal("50");
        
        // Limite por tipo de cliente
        BigDecimal limiteCliente = obterDescontoMaximoCliente(cliente.getTipoCliente());
        
        return percentual.min(limiteGeral).min(limiteCategoria).min(limiteCliente);
    }
    
    private BigDecimal obterDescontoMaximoCliente(Cliente.TipoCliente tipoCliente) {
        Map<Cliente.TipoCliente, BigDecimal> limites = Map.of(
            Cliente.TipoCliente.BRONZE, new BigDecimal("20"),
            Cliente.TipoCliente.PRATA, new BigDecimal("35"),
            Cliente.TipoCliente.OURO, new BigDecimal("50"),
            Cliente.TipoCliente.PREMIUM, new BigDecimal("70")
        );
        
        return limites.getOrDefault(tipoCliente, new BigDecimal("20"));
    }
    
    private boolean isPrimeiraCompraDoAno(Cliente cliente) {
        if (cliente.getDataUltimaCompra() == null) {
            return true;
        }
        
        LocalDate hoje = LocalDate.now();
        return cliente.getDataUltimaCompra().getYear() < hoje.getYear();
    }
    
    // Classes auxiliares internas
    
    public static class ResultadoCalculoDesconto {
        private final BigDecimal valorOriginal;
        private final BigDecimal percentualDesconto;
        private final BigDecimal valorDesconto;
        private final BigDecimal valorFinal;
        private final String justificativa;
        
        public ResultadoCalculoDesconto(BigDecimal valorOriginal, BigDecimal percentualDesconto, 
                BigDecimal valorDesconto, BigDecimal valorFinal, String justificativa) {
            this.valorOriginal = valorOriginal;
            this.percentualDesconto = percentualDesconto;
            this.valorDesconto = valorDesconto;
            this.valorFinal = valorFinal;
            this.justificativa = justificativa;
        }
        
        // Getters
        public BigDecimal getValorOriginal() { return valorOriginal; }
        public BigDecimal getPercentualDesconto() { return percentualDesconto; }
        public BigDecimal getValorDesconto() { return valorDesconto; }
        public BigDecimal getValorFinal() { return valorFinal; }
        public String getJustificativa() { return justificativa; }
    }
    
    public static class ResultadoValidacaoDesconto {
        private final boolean valido;
        private final String mensagem;
        
        public ResultadoValidacaoDesconto(boolean valido, String mensagem) {
            this.valido = valido;
            this.mensagem = mensagem;
        }
        
        public boolean isValido() { return valido; }
        public String getMensagem() { return mensagem; }
    }
    
    private static class DescontoPorQuantidade {
        final int quantidadeMinima;
        final BigDecimal percentual;
        
        DescontoPorQuantidade(int quantidadeMinima, BigDecimal percentual) {
            this.quantidadeMinima = quantidadeMinima;
            this.percentual = percentual;
        }
    }
    
    private static class CupomDesconto {
        final String codigo;
        final BigDecimal percentual;
        final Cliente.TipoCliente tipoClienteMinimo;
        
        CupomDesconto(String codigo, BigDecimal percentual, Cliente.TipoCliente tipoClienteMinimo) {
            this.codigo = codigo;
            this.percentual = percentual;
            this.tipoClienteMinimo = tipoClienteMinimo;
        }
    }
}