package com.tcc.estoque.dto;

import com.tcc.estoque.model.enums.StatusEvento;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTOs para operações com Eventos
 */
public class EventoDTO {

    /**
     * DTO para criação e atualização de eventos
     */
    public static class EventoRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        private String nome;

        @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
        private String descricao;

        @Size(max = 100, message = "Tipo deve ter no máximo 100 caracteres")
        private String tipo;

        @NotNull(message = "Data de início é obrigatória")
        private LocalDate dataInicio;

        @NotNull(message = "Data de fim é obrigatória")
        private LocalDate dataFim;

        @Size(max = 200, message = "Local deve ter no máximo 200 caracteres")
        private String local;

        @Size(max = 500, message = "Endereço completo deve ter no máximo 500 caracteres")
        private String enderecoCompleto;

        @NotNull(message = "Status é obrigatório")
        private StatusEvento status;

        @DecimalMin(value = "0.0", inclusive = false, message = "Desconto percentual deve ser maior que 0")
        @DecimalMax(value = "100.0", message = "Desconto percentual deve ser no máximo 100%")
        private BigDecimal descontoPercentual;

        @DecimalMin(value = "0.0", inclusive = false, message = "Desconto em valor deve ser maior que 0")
        private BigDecimal descontoValor;

        @DecimalMin(value = "0.0", inclusive = false, message = "Meta de vendas deve ser maior que 0")
        private BigDecimal metaVendas;

        @Min(value = 1, message = "Meta de quantidade deve ser maior que 0")
        private Integer metaQuantidadeVendas;

        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres")
        private String observacoes;

        private Boolean publico;

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }

        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }

        public LocalDate getDataInicio() { return dataInicio; }
        public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

        public LocalDate getDataFim() { return dataFim; }
        public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

        public String getLocal() { return local; }
        public void setLocal(String local) { this.local = local; }

        public String getEnderecoCompleto() { return enderecoCompleto; }
        public void setEnderecoCompleto(String enderecoCompleto) { this.enderecoCompleto = enderecoCompleto; }

        public StatusEvento getStatus() { return status; }
        public void setStatus(StatusEvento status) { this.status = status; }

        public BigDecimal getDescontoPercentual() { return descontoPercentual; }
        public void setDescontoPercentual(BigDecimal descontoPercentual) { this.descontoPercentual = descontoPercentual; }

        public BigDecimal getDescontoValor() { return descontoValor; }
        public void setDescontoValor(BigDecimal descontoValor) { this.descontoValor = descontoValor; }

        public BigDecimal getMetaVendas() { return metaVendas; }
        public void setMetaVendas(BigDecimal metaVendas) { this.metaVendas = metaVendas; }

        public Integer getMetaQuantidadeVendas() { return metaQuantidadeVendas; }
        public void setMetaQuantidadeVendas(Integer metaQuantidadeVendas) { this.metaQuantidadeVendas = metaQuantidadeVendas; }

        public String getObservacoes() { return observacoes; }
        public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

        public Boolean getPublico() { return publico; }
        public void setPublico(Boolean publico) { this.publico = publico; }
    }

    /**
     * DTO para resposta completa de eventos
     */
    public static class EventoResponse {
        private Long id;
        private String nome;
        private String descricao;
        private LocalDate dataInicio;
        private LocalDate dataFim;
        private String local;
        private String enderecoCompleto;
        private StatusEvento status;
        private String statusDescricao;
        private String statusCor;
        private BigDecimal descontoPercentual;
        private BigDecimal descontoValor;
        private BigDecimal metaVendas;
        private Integer metaQuantidadeVendas;
        private String observacoes;
        private Boolean ativo;
        private Boolean publico;
        private LocalDateTime dataCadastro;
        private LocalDateTime dataAtualizacao;
        private String criadoPor;
        private String atualizadoPor;
        
        private BigDecimal totalVendas;
        private Integer quantidadeVendas;
        private BigDecimal percentualMeta;
        private Long duracaoEmDias;
        private Boolean eventoVigente;
        private Boolean temDesconto;
        private Boolean metaVendasAlcancada;
        private Boolean metaQuantidadeAlcancada;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }

        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }

        public LocalDate getDataInicio() { return dataInicio; }
        public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

        public LocalDate getDataFim() { return dataFim; }
        public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

        public String getLocal() { return local; }
        public void setLocal(String local) { this.local = local; }

        public String getEnderecoCompleto() { return enderecoCompleto; }
        public void setEnderecoCompleto(String enderecoCompleto) { this.enderecoCompleto = enderecoCompleto; }

        public StatusEvento getStatus() { return status; }
        public void setStatus(StatusEvento status) { this.status = status; }

        public String getStatusDescricao() { return statusDescricao; }
        public void setStatusDescricao(String statusDescricao) { this.statusDescricao = statusDescricao; }

        public String getStatusCor() { return statusCor; }
        public void setStatusCor(String statusCor) { this.statusCor = statusCor; }

        public BigDecimal getDescontoPercentual() { return descontoPercentual; }
        public void setDescontoPercentual(BigDecimal descontoPercentual) { this.descontoPercentual = descontoPercentual; }

        public BigDecimal getDescontoValor() { return descontoValor; }
        public void setDescontoValor(BigDecimal descontoValor) { this.descontoValor = descontoValor; }

        public BigDecimal getMetaVendas() { return metaVendas; }
        public void setMetaVendas(BigDecimal metaVendas) { this.metaVendas = metaVendas; }

        public Integer getMetaQuantidadeVendas() { return metaQuantidadeVendas; }
        public void setMetaQuantidadeVendas(Integer metaQuantidadeVendas) { this.metaQuantidadeVendas = metaQuantidadeVendas; }

        public String getObservacoes() { return observacoes; }
        public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

        public Boolean getAtivo() { return ativo; }
        public void setAtivo(Boolean ativo) { this.ativo = ativo; }

        public Boolean getPublico() { return publico; }
        public void setPublico(Boolean publico) { this.publico = publico; }

        public LocalDateTime getDataCadastro() { return dataCadastro; }
        public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }

        public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
        public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }

        public String getCriadoPor() { return criadoPor; }
        public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }

        public String getAtualizadoPor() { return atualizadoPor; }
        public void setAtualizadoPor(String atualizadoPor) { this.atualizadoPor = atualizadoPor; }

        public BigDecimal getTotalVendas() { return totalVendas; }
        public void setTotalVendas(BigDecimal totalVendas) { this.totalVendas = totalVendas; }

        public Integer getQuantidadeVendas() { return quantidadeVendas; }
        public void setQuantidadeVendas(Integer quantidadeVendas) { this.quantidadeVendas = quantidadeVendas; }

        public BigDecimal getPercentualMeta() { return percentualMeta; }
        public void setPercentualMeta(BigDecimal percentualMeta) { this.percentualMeta = percentualMeta; }

        public Long getDuracaoEmDias() { return duracaoEmDias; }
        public void setDuracaoEmDias(Long duracaoEmDias) { this.duracaoEmDias = duracaoEmDias; }

        public Boolean getEventoVigente() { return eventoVigente; }
        public void setEventoVigente(Boolean eventoVigente) { this.eventoVigente = eventoVigente; }

        public Boolean getTemDesconto() { return temDesconto; }
        public void setTemDesconto(Boolean temDesconto) { this.temDesconto = temDesconto; }

        public Boolean getMetaVendasAlcancada() { return metaVendasAlcancada; }
        public void setMetaVendasAlcancada(Boolean metaVendasAlcancada) { this.metaVendasAlcancada = metaVendasAlcancada; }

        public Boolean getMetaQuantidadeAlcancada() { return metaQuantidadeAlcancada; }
        public void setMetaQuantidadeAlcancada(Boolean metaQuantidadeAlcancada) { this.metaQuantidadeAlcancada = metaQuantidadeAlcancada; }
    }

    /**
     * DTO para listagem resumida de eventos
     */
    public static class EventoResumo {
        private Long id;
        private String nome;
        private String descricao;
        private LocalDate dataInicio;
        private LocalDate dataFim;
        private String local;
        private StatusEvento status;
        private String statusDescricao;
        private String statusCor;
        private Boolean ativo;
        private BigDecimal descontoPercentual;
        private BigDecimal totalVendas;
        private Integer quantidadeVendas;
        private Boolean eventoVigente;
        private Boolean temDesconto;
        private Long duracaoEmDias;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }

        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }

        public LocalDate getDataInicio() { return dataInicio; }
        public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

        public LocalDate getDataFim() { return dataFim; }
        public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

        public String getLocal() { return local; }
        public void setLocal(String local) { this.local = local; }

        public StatusEvento getStatus() { return status; }
        public void setStatus(StatusEvento status) { this.status = status; }

        public String getStatusDescricao() { return statusDescricao; }
        public void setStatusDescricao(String statusDescricao) { this.statusDescricao = statusDescricao; }

        public String getStatusCor() { return statusCor; }
        public void setStatusCor(String statusCor) { this.statusCor = statusCor; }

        public Boolean getAtivo() { return ativo; }
        public void setAtivo(Boolean ativo) { this.ativo = ativo; }

        public BigDecimal getDescontoPercentual() { return descontoPercentual; }
        public void setDescontoPercentual(BigDecimal descontoPercentual) { this.descontoPercentual = descontoPercentual; }

        public BigDecimal getTotalVendas() { return totalVendas; }
        public void setTotalVendas(BigDecimal totalVendas) { this.totalVendas = totalVendas; }

        public Integer getQuantidadeVendas() { return quantidadeVendas; }
        public void setQuantidadeVendas(Integer quantidadeVendas) { this.quantidadeVendas = quantidadeVendas; }

        public Boolean getEventoVigente() { return eventoVigente; }
        public void setEventoVigente(Boolean eventoVigente) { this.eventoVigente = eventoVigente; }

        public Boolean getTemDesconto() { return temDesconto; }
        public void setTemDesconto(Boolean temDesconto) { this.temDesconto = temDesconto; }

        public Long getDuracaoEmDias() { return duracaoEmDias; }
        public void setDuracaoEmDias(Long duracaoEmDias) { this.duracaoEmDias = duracaoEmDias; }
    }

    /**
     * DTO para filtros de busca de eventos
     */
    public static class FiltroEventos {
        private String termo;
        private StatusEvento status;
        private Boolean ativo;
        private Boolean vigente;
        private LocalDate dataInicioApos;
        private LocalDate dataInicioAntes;
        private String orderBy = "dataInicio";
        private String orderDirection = "ASC";

        public String getTermo() { return termo; }
        public void setTermo(String termo) { this.termo = termo; }

        public StatusEvento getStatus() { return status; }
        public void setStatus(StatusEvento status) { this.status = status; }

        public Boolean getAtivo() { return ativo; }
        public void setAtivo(Boolean ativo) { this.ativo = ativo; }

        public Boolean getVigente() { return vigente; }
        public void setVigente(Boolean vigente) { this.vigente = vigente; }

        public LocalDate getDataInicioApos() { return dataInicioApos; }
        public void setDataInicioApos(LocalDate dataInicioApos) { this.dataInicioApos = dataInicioApos; }

        public LocalDate getDataInicioAntes() { return dataInicioAntes; }
        public void setDataInicioAntes(LocalDate dataInicioAntes) { this.dataInicioAntes = dataInicioAntes; }

        public String getOrderBy() { return orderBy; }
        public void setOrderBy(String orderBy) { this.orderBy = orderBy; }

        public String getOrderDirection() { return orderDirection; }
        public void setOrderDirection(String orderDirection) { this.orderDirection = orderDirection; }
    }

    /**
     * DTO para estatísticas de eventos
     */
    public static class EstatisticasEventoResponse {
        private Long totalEventos;
        private Long eventosAtivos;
        private Long eventosVigentes;
        private Long eventosConcluidos;
        private BigDecimal totalVendasEventos;
        private BigDecimal mediaVendasPorEvento;
        private Integer totalVendasQuantidade;
        private Long eventoComMaiorVenda;
        private String nomeEventoMaiorVenda;
        private BigDecimal valorEventoMaiorVenda;

        public Long getTotalEventos() { return totalEventos; }
        public void setTotalEventos(Long totalEventos) { this.totalEventos = totalEventos; }

        public Long getEventosAtivos() { return eventosAtivos; }
        public void setEventosAtivos(Long eventosAtivos) { this.eventosAtivos = eventosAtivos; }

        public Long getEventosVigentes() { return eventosVigentes; }
        public void setEventosVigentes(Long eventosVigentes) { this.eventosVigentes = eventosVigentes; }

        public Long getEventosConcluidos() { return eventosConcluidos; }
        public void setEventosConcluidos(Long eventosConcluidos) { this.eventosConcluidos = eventosConcluidos; }

        public BigDecimal getTotalVendasEventos() { return totalVendasEventos; }
        public void setTotalVendasEventos(BigDecimal totalVendasEventos) { this.totalVendasEventos = totalVendasEventos; }

        public BigDecimal getMediaVendasPorEvento() { return mediaVendasPorEvento; }
        public void setMediaVendasPorEvento(BigDecimal mediaVendasPorEvento) { this.mediaVendasPorEvento = mediaVendasPorEvento; }

        public Integer getTotalVendasQuantidade() { return totalVendasQuantidade; }
        public void setTotalVendasQuantidade(Integer totalVendasQuantidade) { this.totalVendasQuantidade = totalVendasQuantidade; }

        public Long getEventoComMaiorVenda() { return eventoComMaiorVenda; }
        public void setEventoComMaiorVenda(Long eventoComMaiorVenda) { this.eventoComMaiorVenda = eventoComMaiorVenda; }

        public String getNomeEventoMaiorVenda() { return nomeEventoMaiorVenda; }
        public void setNomeEventoMaiorVenda(String nomeEventoMaiorVenda) { this.nomeEventoMaiorVenda = nomeEventoMaiorVenda; }

        public BigDecimal getValorEventoMaiorVenda() { return valorEventoMaiorVenda; }
        public void setValorEventoMaiorVenda(BigDecimal valorEventoMaiorVenda) { this.valorEventoMaiorVenda = valorEventoMaiorVenda; }
    }

    /**
     * DTO para informações dos status de evento
     */
    public static class StatusEventoInfo {
        private StatusEvento status;
        private String descricao;
        private String detalhe;
        private String cor;
        private Boolean permiteVendas;

        public StatusEventoInfo() {}

        public StatusEventoInfo(StatusEvento status) {
            this.status = status;
            this.descricao = status.getDescricao();
            this.detalhe = status.getDetalhe();
            this.cor = status.getCor();
            this.permiteVendas = status.isPermiteVendas();
        }

        public StatusEvento getStatus() { return status; }
        public void setStatus(StatusEvento status) { this.status = status; }

        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }

        public String getDetalhe() { return detalhe; }
        public void setDetalhe(String detalhe) { this.detalhe = detalhe; }

        public String getCor() { return cor; }
        public void setCor(String cor) { this.cor = cor; }

        public Boolean getPermiteVendas() { return permiteVendas; }
        public void setPermiteVendas(Boolean permiteVendas) { this.permiteVendas = permiteVendas; }
    }
}
