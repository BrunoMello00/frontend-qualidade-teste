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
 * DTOs para operações do Dashboard
 */
public class DashboardDTO {

    /**
     * Dados gerais do dashboard
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosGeraisResponse {
        private Long totalVendas;
        private BigDecimal faturamentoMes;
        private BigDecimal faturamentoDia;
        private Long totalProdutos;
        private Long produtosEstoqueBaixo;
        private BigDecimal ticketMedio;
        private BigDecimal crescimentoMes;
        private Long vendasPendentes;
        
        private Long totalClientes;
        private Long totalUsuarios;
        private Long eventosAtivos;
        private Long clientesNovosHoje;
    }

    /**
     * Estatísticas gerenciais completas
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstatisticasGeraisResponse {
        private Map<StatusVenda, Long> vendasPorStatus;
        private Map<CategoriaCliente, Long> clientesPorCategoria;
        private Map<TipoUsuario, Long> usuariosPorTipo;
        private Map<StatusEvento, Long> eventosPorStatus;
        private List<VendedorRankingDTO> topVendedores;
        private List<ProdutoVendidoDTO> topProdutos;
        private List<EventoPerformanceDTO> topEventos;
        private List<AlertaDTO> alertas;
    }

    /**
     * Dashboard específico por vendedor
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardVendedorResponse {
        private UsuarioResumoDTO vendedor;
        private Long totalVendas;
        private BigDecimal faturamentoMes;
        private BigDecimal metaVendedor;
        private BigDecimal progressoMeta;
        private Integer posicaoRanking;
        private Long clientesAtendidos;
        private List<VendaDiaDTO> vendasDiarias;
        private List<ProdutoVendidoDTO> produtosMaisVendidos;
    }

    /**
     * Resumo de performance
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceResumoResponse {
        private BigDecimal vendasHoje;
        private BigDecimal vendasOntem;
        private BigDecimal crescimentoDiario;
        private BigDecimal vendasSemana;
        private BigDecimal vendasSemanaAnterior;
        private BigDecimal crescimentoSemanal;
        private BigDecimal vendasMes;
        private BigDecimal vendasMesAnterior;
        private BigDecimal crescimentoMensal;
        private Double taxaConversaoVendas;
        private BigDecimal ticketMedioMes;
    }

    /**
     * Vendedor no ranking
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendedorRankingDTO {
        private Long vendedorId;
        private String nome;
        private String email;
        private TipoUsuario tipo;
        private Long totalVendas;
        private BigDecimal faturamento;
        private BigDecimal ticketMedio;
        private Integer posicao;
        private BigDecimal percentualMeta;
    }

    /**
     * Performance de evento
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventoPerformanceDTO {
        private Long eventoId;
        private String nome;
        private StatusEvento status;
        private String statusCor;
        private LocalDateTime dataInicio;
        private LocalDateTime dataFim;
        private BigDecimal totalVendas;
        private Integer quantidadeVendas;
        private BigDecimal metaVendas;
        private Double progressoMeta;
        private Integer diasRestantes;
    }

    /**
     * Alerta do sistema
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertaDTO {
        private String tipo;
        private String titulo;
        private String mensagem;
        private String nivel; // INFO, WARNING, ERROR, SUCCESS
        private String cor;
        private String icone;
        private LocalDateTime dataAlerta;
        private String acao; // Ação sugerida
        private String link; // Link para resolução
    }

    /**
     * Resumo de usuário
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioResumoDTO {
        private Long id;
        private String nome;
        private String email;
        private TipoUsuario tipo;
        private String tipoDescricao;
        private String tipoCor;
        private Boolean ativo;
        private LocalDateTime ultimoAcesso;
    }

    /**
     * Vendas da semana
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendasSemanaResponse {
        private List<VendaDiaDTO> vendas;
        private BigDecimal totalSemana;
        private BigDecimal mediaDiaria;
        private BigDecimal crescimentoSemana;
    }

    /**
     * Venda por dia
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaDiaDTO {
        private LocalDate data;
        private BigDecimal valor;
        private Long quantidade;
    }

    /**
     * Vendas do mês
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendasMesResponse {
        private List<VendaDiaDTO> vendas;
        private BigDecimal totalMes;
        private BigDecimal metaMes;
        private BigDecimal percentualMeta;
        private BigDecimal crescimentoMesAnterior;
    }

    /**
     * Top produtos mais vendidos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProdutosResponse {
        private List<ProdutoVendidoDTO> produtos;
        private int periodoAnalisado;
    }

    /**
     * Produto vendido
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProdutoVendidoDTO {
        private Long produtoId;
        private String nome;
        private Long quantidadeVendida;
        private BigDecimal faturamento;
        private String categoria;
        private BigDecimal precoUnitario;
    }

    /**
     * Produtos com estoque baixo
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstoqueBaixoResponse {
        private List<ProdutoEstoqueBaixoDTO> produtos;
        private int totalProdutosCriticos;
    }

    /**
     * Produto com estoque baixo
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProdutoEstoqueBaixoDTO {
        private Long produtoId;
        private String nome;
        private Integer quantidadeAtual;
        private Integer estoqueMinimo;
        private String categoria;
        private BigDecimal preco;
        private String status; // CRITICO, BAIXO, ZERADO
    }

    /**
     * Vendas recentes
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendasRecentesResponse {
        private List<VendaRecenteDTO> vendas;
        private int totalVendas;
    }

    /**
     * Venda recente
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendaRecenteDTO {
        private Long vendaId;
        private String nomeCliente;
        private BigDecimal valorTotal;
        private LocalDateTime dataVenda;
        private String status;
        private int quantidadeItens;
    }
}
