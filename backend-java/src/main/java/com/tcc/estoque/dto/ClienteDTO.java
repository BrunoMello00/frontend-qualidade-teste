package com.tcc.estoque.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tcc.estoque.model.enums.CategoriaCliente;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClienteDTO {

    @Data
    public static class ClienteRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
        private String nome;

        @NotBlank(message = "CPF é obrigatório")
        @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11}|000\\.000\\.000-00", 
                 message = "CPF deve ter formato válido")
        private String cpf;

        @Email(message = "Email deve ter um formato válido")
        private String email;

        private String telefone;

        private EnderecoRequest endereco;

        private LocalDate dataNascimento;

        private CategoriaCliente categoria;

        private String observacoes;
    }

    @Data
    public static class EnderecoRequest {
        private String rua;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String cep;
        private String estado;
    }

    @Data
    public static class ClienteResponse {
        private Long id;
        private String nome;
        private String cpf;
        private String email;
        private String telefone;
        private EnderecoResponse endereco;
        private LocalDate dataNascimento;
        private LocalDateTime dataCadastro;
        private Boolean ativo;

        private BigDecimal totalCompras;
        private Integer quantidadeCompras;
        private LocalDateTime ultimaCompra;
        private CategoriaCliente categoria;
        private String categoriaDescricao;
        private String categoriaCor;
        private Integer pontos;
        private String observacoes;
        private Boolean isFake;

        private BigDecimal ticketMedio;
        private Boolean isClienteFrequente;
    }

    @Data
    public static class EnderecoResponse {
        private String rua;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String cep;
        private String estado;
        private String enderecoCompleto;
    }

    @Data
    public static class ClienteResumo {
        private Long id;
        private String nome;
        private String cpf;
        private String email;
        private CategoriaCliente categoria;
        private String categoriaDescricao;
        private Integer pontos;
        private BigDecimal totalCompras;
        private Integer quantidadeCompras;
        private LocalDateTime ultimaCompra;
        private Boolean ativo;
    }

    @Data
    public static class PontosRequest {
        @NotNull(message = "Pontos é obrigatório")
        @Min(value = 1, message = "Pontos deve ser maior que zero")
        private Integer pontos;

        @NotBlank(message = "Motivo é obrigatório")
        private String motivo; // 'compra', 'ajuste_manual', 'bonus', etc.

        private String observacoes;

        private Long vendaId; // Opcional, caso seja relacionado a uma venda
    }

    @Data
    public static class HistoricoPontosResponse {
        private Long id;
        private Long clienteId;
        private String clienteNome;
        private Long vendaId;
        private Long produtoId;
        private String produtoNome;
        private Integer pontosAdicionados;
        private Integer pontosAntes;
        private Integer pontosDepois;
        private String motivo;
        private String motivoFormatado;
        private LocalDateTime dataOperacao;
        private String observacoes;
        private String tipoOperacao; // "Adição" ou "Remoção"
        private Integer pontosAbsolutos;
    }

    @Data
    public static class EstatisticasClienteResponse {
        private Long totalClientes;
        private Long clientesAtivos;
        private BigDecimal ticketMedioGeral;
        private List<ClienteTopResponse> clientesMaisFrequentes;
        private List<DistribuicaoCategoriaResponse> distribuicaoCategorias;
        private BigDecimal faturamentoTotalClientes;
        private Integer pontosDistribuidosTotal;
    }

    @Data
    public static class ClienteTopResponse {
        private Long id;
        private String nome;
        private String cpf;
        private Integer quantidadeCompras;
        private BigDecimal valorTotal;
        private CategoriaCliente categoria;
        private Integer pontos;
        private BigDecimal ticketMedio;
    }

    @Data
    public static class DistribuicaoCategoriaResponse {
        private CategoriaCliente categoria;
        private String categoriaDescricao;
        private String categoriaCor;
        private Long quantidade;
        private BigDecimal percentual;
        private BigDecimal faturamentoCategoria;
    }

    @Data
    public static class FiltroClientes {
        private String termo; // Para buscar por nome, CPF ou email
        private CategoriaCliente categoria;
        private Boolean ativo;
        private LocalDate dataCadastroInicio;
        private LocalDate dataCadastroFim;
        private LocalDate ultimaCompraInicio;
        private LocalDate ultimaCompraFim;
        private Integer pontosMinimos;
        private Integer pontosMaximos;
        private String orderBy = "dataCadastro";
        private String orderDirection = "DESC";
    }

    @Data
    public static class ConfiguracaoCategoriaRequest {
        private Long id; // null para criar nova categoria
        
        @NotBlank(message = "Nome da categoria é obrigatório")
        private String nome;
        
        @NotNull(message = "Pontos mínimos é obrigatório")
        @Min(value = 0, message = "Pontos mínimos não pode ser negativo")
        private Integer pontosMinimos;
        
        @NotBlank(message = "Cor é obrigatória")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato hexadecimal (#RRGGBB)")
        private String cor;
        
        private String descricao;
        
        private Boolean ativo = true;
    }

    @Data
    public static class ConfiguracaoCategoriaResponse {
        private Long id;
        private String nome;
        private Integer pontosMinimos;
        private String cor;
        private String descricao;
        private Boolean ativo;
        private LocalDateTime dataCriacao;
    }
}
