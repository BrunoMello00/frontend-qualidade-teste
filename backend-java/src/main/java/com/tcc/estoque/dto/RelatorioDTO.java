package com.tcc.estoque.dto;

import com.tcc.estoque.model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs para relatórios
 */
public class RelatorioDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatorioVendasResponse {
        
        private String periodo;
        private Long totalVendas;
        private BigDecimal totalFaturamento;
        private BigDecimal ticketMedio;
        private List<VendedorRanking> topVendedores;
        private List<ProdutoVendidoRanking> produtosMaisVendidos;
        private LocalDateTime dataGeracao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendedorRanking {
        
        private String nomeVendedor;
        private Long quantidadeVendas;
        private BigDecimal valorTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProdutoVendidoRanking {
        
        private String nomeProduto;
        private Integer quantidadeVendida;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatorioEstoqueResponse {
        
        private Long totalProdutos;
        private Long produtosAtivos;
        private Long produtosEstoqueBaixo;
        private BigDecimal valorTotalEstoque;
        private List<ProdutoEstoqueBaixo> produtosComEstoqueBaixo; // Apenas produtos com estoque baixo (para alertas)
        private List<ProdutoEstoqueBaixo> todosProdutos; // Todos os produtos (para tabela completa)
        private List<CategoriaValorEstoque> categoriasPorValor;
        private LocalDateTime dataGeracao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProdutoEstoqueBaixo {
        
        private Long id;
        private String nome;
        private String codigo;
        private Integer estoqueAtual;
        private Integer estoqueMinimo;
        private String categoria;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaValorEstoque {
        
        private String categoria;
        private Long quantidadeProdutos;
        private BigDecimal valorTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatorioMovimentacaoResponse {
        
        private String periodo;
        private Long totalMovimentacoes;
        private Long totalEntradas;
        private Long totalSaidas;
        private Long totalAjustes;
        private Integer quantidadeEntradas;
        private Integer quantidadeSaidas;
        private Integer saldoMovimentacao;
        private List<ProdutoMovimentacaoRanking> produtosMaisMovimentados;
        private LocalDateTime dataGeracao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProdutoMovimentacaoRanking {
        
        private String nomeProduto;
        private Long quantidadeMovimentacoes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardExecutivoResponse {
        
        private Long vendasMes;
        private BigDecimal faturamentoMes;
        private Long vendasHoje;
        private BigDecimal faturamentoHoje;
        private Long vendasPendentes;
        private Long produtosEstoqueBaixo;
        private Long totalProdutos;
        private Long movimentacoesHoje;
        private LocalDateTime ultimaAtualizacao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatorioFinanceiroResponse {
        
        private String periodo;
        private BigDecimal receitaTotal;
        private BigDecimal custoTotal;
        private BigDecimal lucroTotal;
        private BigDecimal margemLucro;
        private List<VendaPorDia> vendasPorDia;
        private List<ReceitaPorCategoria> receitaPorCategoria;
        private LocalDateTime dataGeracao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaPorDia {
        
        private String data;
        private Long quantidadeVendas;
        private BigDecimal valorTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceitaPorCategoria {
        
        private String categoria;
        private BigDecimal receita;
        private Double percentual;
    }

    // ===== DTOs Expandidos para Sistema Completo de Relatórios =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodoDTO {
        private LocalDate inicio;
        private LocalDate fim;
        private Integer diasPeriodo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumoVendasDTO {
        private Long totalVendas;
        private BigDecimal valorTotal;
        private BigDecimal ticketMedio;
        private BigDecimal crescimento;
        private Long vendasConfirmadas;
        private Long vendasPendentes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaPorDiaDTO {
        private LocalDate data;
        private Long quantidade;
        private BigDecimal valor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProdutoDTO {
        private Long produtoId;
        private String nome;
        private String categoria;
        private Long quantidadeVendida;
        private BigDecimal faturamento;
        private BigDecimal precoUnitario;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopVendedorDTO {
        private Long vendedorId;
        private String nome;
        private String email;
        private TipoUsuario tipo;
        private Long totalVendas;
        private BigDecimal faturamento;
        private BigDecimal ticketMedio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaDTO {
        private Long id;
        private String nomeCliente;
        private LocalDateTime dataVenda;
        private BigDecimal valorTotal;
        private StatusVenda status;
        private FormaPagamento formaPagamento;
        private String vendedorNome;
        private String eventoNome;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumoProdutosDTO {
        private Long totalProdutos;
        private Long produtosAtivos;
        private Long totalCategorias;
        private BigDecimal valorEstoque;
        private BigDecimal valorMedioUnitario;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstoqueResumoDTO {
        private Long produtosEstoqueBaixo;
        private Long produtosSemEstoque;
        private Long estoqueTotal;
        private BigDecimal valorTotalEstoque;
        private Integer alertasCriticos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaProdutoDTO {
        private String nome;
        private Long quantidadeProdutos;
        private BigDecimal valorEstoque;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertaEstoqueDTO {
        private Long produtoId;
        private String nomeProduto;
        private Integer estoqueAtual;
        private Integer estoqueMinimo;
        private String status;
        private String categoria;
        private BigDecimal valor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumoClientesDTO {
        private Long totalClientes;
        private Long clientesAtivos;
        private Long clientesNovos;
        private BigDecimal ticketMedioGeral;
        private BigDecimal faturamentoTotal;
        private Integer totalPontosDistribuidos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopClienteDTO {
        private Long clienteId;
        private String nome;
        private String email;
        private CategoriaCliente categoria;
        private BigDecimal totalCompras;
        private Integer quantidadeCompras;
        private Integer pontos;
        private LocalDateTime ultimaCompra;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteDTO {
        private Long id;
        private String nome;
        private String email;
        private CategoriaCliente categoria;
        private BigDecimal totalCompras;
        private Integer quantidadeCompras;
        private Integer pontos;
        private LocalDateTime dataCadastro;
        private LocalDateTime ultimaCompra;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceitasDTO {
        private BigDecimal receitaBruta;
        private BigDecimal descontos;
        private BigDecimal receitaLiquida;
        private Long numeroVendas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustosDTO {
        private BigDecimal custoProdutos;
        private BigDecimal percentualCusto;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LucrosDTO {
        private BigDecimal lucroLiquido;
        private BigDecimal margemLucro;
        private BigDecimal crescimentoPeriodoAnterior;
    }

    // ===== Responses Completas =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatorioVendasResponseCompleto {
        private PeriodoDTO periodo;
        private ResumoVendasDTO resumo;
        private List<VendaDTO> vendas;
        private List<VendaPorDiaDTO> vendasPorDia;
        private Map<StatusVenda, Long> vendasPorStatus;
        private List<TopProdutoDTO> topProdutos;
        private List<TopVendedorDTO> topVendedores;
        private LocalDateTime geradoEm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatorioProdutosResponse {
        private ResumoProdutosDTO resumo;
        private EstoqueResumoDTO estoque;
        private List<CategoriaProdutoDTO> categorias;
        private List<TopProdutoDTO> produtosMaisVendidos;
        private List<AlertaEstoqueDTO> alertasEstoque;
        private List<ProdutoResumoDTO> produtos; // Lista de todos os produtos para a tabela
        private LocalDateTime geradoEm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProdutoResumoDTO {
        private Long id;
        private String produtoNome;
        private String categoria;
        private Integer quantidadeVendida;
        private BigDecimal faturamento;
        private BigDecimal margem;
        private Integer estoqueAtual;
        private BigDecimal precoUnitario;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatorioClientesResponse {
        private ResumoClientesDTO resumo;
        private Map<CategoriaCliente, Long> distribuicaoCategoria;
        private List<TopClienteDTO> topClientes;
        private List<ClienteDTO> clientesNovos;
        private LocalDateTime geradoEm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatorioFinanceiroResponseCompleto {
        private PeriodoDTO periodo;
        private ReceitasDTO receitas;
        private CustosDTO custos;
        private LucrosDTO lucros;
        private LocalDateTime geradoEm;
    }

    // ===== DTOs PARA VENDAS DETALHADAS =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendasDetalhadasResponse {
        private String periodo;
        private Long totalVendas;
        private BigDecimal totalFaturamento;
        private BigDecimal ticketMedio;
        private List<VendaIndividualDTO> vendas;
        private LocalDateTime dataGeracao;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaIndividualDTO {
        private Long id;
        private LocalDateTime dataVenda;
        private String clienteNome;
        private BigDecimal valorTotal;
        private FormaPagamento formaPagamento;
        private String nomeVendedor;
        private Integer quantidadeItens;
        private StatusVenda status;
        private List<ItemVendaDTO> itens;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemVendaDTO {
        private Long produtoId;
        private String produtoNome;
        private Integer quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal subtotal;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraficoVendasResponse {
        private String periodo;
        private List<DadoGraficoDTO> evolucaoVendas;
        private BigDecimal totalPeriodo;
        private LocalDateTime dataGeracao;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadoGraficoDTO {
        private String data; // "2025-11-20"
        private String dataFormatada; // "20/11/2025"
        private Integer quantidadeVendas;
        private BigDecimal valorTotal;
        private BigDecimal ticketMedio;
    }
}
