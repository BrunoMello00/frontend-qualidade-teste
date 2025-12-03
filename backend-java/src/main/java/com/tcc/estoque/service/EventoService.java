package com.tcc.estoque.service;

import com.tcc.estoque.dto.EventoDTO;
import com.tcc.estoque.model.Evento;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.model.enums.StatusEvento;
import com.tcc.estoque.repository.EventoRepository;
import com.tcc.estoque.exception.BusinessException;
import com.tcc.estoque.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private UsuarioService usuarioService;

    public EventoDTO.EventoResponse criarEvento(EventoDTO.EventoRequest request, String emailUsuario) {
        validarDatasEvento(request.getDataInicio(), request.getDataFim());
        validarNomeUnico(request.getNome(), null);
        
        Usuario usuarioLogado = usuarioService.findByEmail(emailUsuario);
        
        Evento evento = mapearParaEntidade(request);
        evento.setCriadoPor(usuarioLogado);
        evento.setDataCadastro(LocalDateTime.now());
        evento.setAtivo(true);

        evento = eventoRepository.save(evento);
        return mapearParaResponse(evento);
    }

    @Transactional(readOnly = true)
    public EventoDTO.EventoResponse buscarPorId(Long id) {
        Evento evento = buscarEventoPorId(id);
        return mapearParaResponse(evento);
    }

    public EventoDTO.EventoResponse atualizarEvento(Long id, EventoDTO.EventoRequest request, String emailUsuario) {
        Evento evento = buscarEventoPorId(id);
        
        validarDatasEvento(request.getDataInicio(), request.getDataFim());
        validarNomeUnico(request.getNome(), id);
        
        Usuario usuarioLogado = usuarioService.findByEmail(emailUsuario);
        
        atualizarDadosEvento(evento, request);
        evento.setAtualizadoPor(usuarioLogado);
        evento.setDataAtualizacao(LocalDateTime.now());

        evento = eventoRepository.save(evento);
        return mapearParaResponse(evento);
    }

    public void deletarEvento(Long id) {
        Evento evento = buscarEventoPorId(id);
        
        if (evento.getVendas() != null && !evento.getVendas().isEmpty()) {
            throw new BusinessException("Não é possível excluir evento com vendas associadas. Desative o evento.");
        }
        
        evento.setAtivo(false);
        eventoRepository.save(evento);
    }

    public void reativarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado com ID: " + id));
        evento.setAtivo(true);
        eventoRepository.save(evento);
    }

    public EventoDTO.EventoResponse alterarStatus(Long id, StatusEvento novoStatus, String emailUsuario) {
        Evento evento = buscarEventoPorId(id);
        Usuario usuarioLogado = usuarioService.findByEmail(emailUsuario);
        
        validarMudancaStatus(evento.getStatus(), novoStatus);
        
        evento.setStatus(novoStatus);
        evento.setAtualizadoPor(usuarioLogado);
        evento.setDataAtualizacao(LocalDateTime.now());
        
        evento = eventoRepository.save(evento);
        return mapearParaResponse(evento);
    }

    @Transactional(readOnly = true)
    public Page<EventoDTO.EventoResumo> listarEventos(EventoDTO.FiltroEventos filtro, Pageable pageable) {
        Sort sort = criarOrdenacao(filtro.getOrderBy(), filtro.getOrderDirection());
        Pageable pageableComSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Evento> eventos = eventoRepository.findComFiltros(
                filtro.getTermo(),
                filtro.getStatus(),
                filtro.getAtivo(),
                filtro.getDataInicioApos(),
                filtro.getDataInicioAntes(),
                pageableComSort
        );

        return eventos.map(this::mapearParaResumo);
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.EventoResumo> buscarPorTermo(String termo) {
        Page<Evento> eventos = eventoRepository.findByTermoGeral(termo, PageRequest.of(0, 50));
        return eventos.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.EventoResumo> buscarEventosVigentes() {
        List<Evento> eventos = eventoRepository.findEventosVigentesHoje();
        return eventos.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.EventoResumo> buscarEventosPublicos() {
        List<Evento> eventos = eventoRepository.findByPublicoTrueAndAtivoTrueOrderByDataInicioDesc();
        return eventos.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.EventoResumo> listarEventosAtivos() {
        List<Evento> eventos = eventoRepository.findByAtivoTrueOrderByDataInicioDesc();
        return eventos.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.EventoResumo> buscarEventosQuePermitemVendas() {
        List<Evento> eventos = eventoRepository.findEventosQuePermitemVendas();
        return eventos.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.EventoResumo> buscarEventosComDesconto() {
        List<Evento> eventos = eventoRepository.findEventosComDesconto();
        return eventos.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.StatusEventoInfo> obterStatusDisponiveis() {
        return List.of(StatusEvento.values()).stream()
                .map(EventoDTO.StatusEventoInfo::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.EventoResumo> buscarPorStatus(StatusEvento status) {
        List<Evento> eventos = eventoRepository.findByStatusAndAtivoTrue(status);
        return eventos.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventoDTO.EstatisticasEventoResponse obterEstatisticas() {
        EventoDTO.EstatisticasEventoResponse stats = new EventoDTO.EstatisticasEventoResponse();
        
        stats.setTotalEventos(eventoRepository.countByAtivoTrue());
        stats.setEventosAtivos(eventoRepository.countByStatusAndAtivoTrue(StatusEvento.ATIVO));
        stats.setEventosVigentes(eventoRepository.countEventosVigentes());
        stats.setEventosConcluidos(eventoRepository.countByStatusAndAtivoTrue(StatusEvento.CONCLUIDO));
        
        List<Object[]> topEventos = eventoRepository.findTopEventosPorVendas(PageRequest.of(0, 1));
        if (!topEventos.isEmpty()) {
            Object[] eventoTop = topEventos.get(0);
            Evento evento = (Evento) eventoTop[0];
            BigDecimal totalVendas = (BigDecimal) eventoTop[1];
            
            stats.setEventoComMaiorVenda(evento.getId());
            stats.setNomeEventoMaiorVenda(evento.getNome());
            stats.setValorEventoMaiorVenda(totalVendas);
        }
        
        BigDecimal mediaVendas = eventoRepository.getMediaVendasPorEvento().orElse(BigDecimal.ZERO);
        stats.setMediaVendasPorEvento(mediaVendas);
        
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Object[]> obterEstatisticasPorStatus() {
        return eventoRepository.contarEventosPorStatus();
    }

    @Transactional(readOnly = true)
    public List<Object[]> obterEstatisticasPorMes() {
        return eventoRepository.contarEventosPorMes();
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.EventoResumo> buscarEventosProximosDoFim() {
        LocalDate dataLimite = LocalDate.now().plusDays(7);
        List<Evento> eventos = eventoRepository.findEventosProximosDoFim(dataLimite);
        return eventos.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventoDTO.EventoResumo> buscarEventosQueComecamEmBreve() {
        LocalDate dataLimite = LocalDate.now().plusDays(7);
        List<Evento> eventos = eventoRepository.findEventosQueComecamEmBreve(dataLimite);
        return eventos.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    private Evento buscarEventoPorId(Long id) {
        return eventoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado com ID: " + id));
    }

    private void validarDatasEvento(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio.isAfter(dataFim)) {
            throw new BusinessException("Data de início não pode ser posterior à data de fim");
        }
        
    }

    private void validarNomeUnico(String nome, Long idExcluir) {
        boolean nomeExiste = (idExcluir == null) ?
                eventoRepository.existsByNomeAndAtivoTrue(nome) :
                eventoRepository.existsByNomeAndIdNotAndAtivoTrue(nome, idExcluir);

        if (nomeExiste) {
            throw new BusinessException("Já existe um evento com este nome: " + nome);
        }
    }

    private void validarMudancaStatus(StatusEvento statusAtual, StatusEvento novoStatus) {
        if (statusAtual == StatusEvento.CONCLUIDO || statusAtual == StatusEvento.CANCELADO) {
            throw new BusinessException("Não é possível alterar status de evento finalizado");
        }
        
        if (statusAtual == StatusEvento.ATIVO && novoStatus == StatusEvento.PLANEJADO) {
            throw new BusinessException("Não é possível voltar evento ativo para planejado");
        }
    }

    private Sort criarOrdenacao(String orderBy, String orderDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(orderDirection) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;

        switch (orderBy.toLowerCase()) {
            case "nome": return Sort.by(direction, "nome");
            case "datainicio": return Sort.by(direction, "dataInicio");
            case "datafim": return Sort.by(direction, "dataFim");
            case "status": return Sort.by(direction, "status");
            case "datacadastro": return Sort.by(direction, "dataCadastro");
            default: return Sort.by(direction, "dataInicio");
        }
    }

    private Evento mapearParaEntidade(EventoDTO.EventoRequest request) {
        Evento evento = new Evento();
        atualizarDadosEvento(evento, request);
        return evento;
    }

    private void atualizarDadosEvento(Evento evento, EventoDTO.EventoRequest request) {
        evento.setNome(request.getNome());
        evento.setDescricao(request.getDescricao());
        evento.setDataInicio(request.getDataInicio());
        evento.setDataFim(request.getDataFim());
        evento.setLocal(request.getLocal());
        evento.setEnderecoCompleto(request.getEnderecoCompleto());
        evento.setStatus(request.getStatus());
        evento.setDescontoPercentual(request.getDescontoPercentual());
        evento.setDescontoValor(request.getDescontoValor());
        evento.setMetaVendas(request.getMetaVendas());
        evento.setMetaQuantidadeVendas(request.getMetaQuantidadeVendas());
        evento.setObservacoes(request.getObservacoes());
        if (request.getPublico() != null) {
            evento.setPublico(request.getPublico());
        }
    }

    private EventoDTO.EventoResponse mapearParaResponse(Evento evento) {
        EventoDTO.EventoResponse response = new EventoDTO.EventoResponse();
        response.setId(evento.getId());
        response.setNome(evento.getNome());
        response.setDescricao(evento.getDescricao());
        response.setDataInicio(evento.getDataInicio());
        response.setDataFim(evento.getDataFim());
        response.setLocal(evento.getLocal());
        response.setEnderecoCompleto(evento.getEnderecoCompleto());
        response.setStatus(evento.getStatus());
        response.setStatusDescricao(evento.getStatusDescricao());
        response.setStatusCor(evento.getStatusCor());
        response.setDescontoPercentual(evento.getDescontoPercentual());
        response.setDescontoValor(evento.getDescontoValor());
        response.setMetaVendas(evento.getMetaVendas());
        response.setMetaQuantidadeVendas(evento.getMetaQuantidadeVendas());
        response.setObservacoes(evento.getObservacoes());
        response.setAtivo(evento.getAtivo());
        response.setPublico(evento.getPublico());
        response.setDataCadastro(evento.getDataCadastro());
        response.setDataAtualizacao(evento.getDataAtualizacao());
        
        if (evento.getCriadoPor() != null) {
            response.setCriadoPor(evento.getCriadoPor().getNome());
        }
        if (evento.getAtualizadoPor() != null) {
            response.setAtualizadoPor(evento.getAtualizadoPor().getNome());
        }
        
        response.setDuracaoEmDias(evento.getDuracaoEmDias());
        response.setEventoVigente(evento.isEventoVigente());
        response.setTemDesconto(evento.temDesconto());
        
        BigDecimal totalVendas = eventoRepository.getTotalVendasEvento(evento.getId()).orElse(BigDecimal.ZERO);
        Integer quantidadeVendas = eventoRepository.getQuantidadeVendasEvento(evento.getId()).orElse(0);
        
        response.setTotalVendas(totalVendas);
        response.setQuantidadeVendas(quantidadeVendas);
        
        if (evento.getMetaVendas() != null && evento.getMetaVendas().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentual = totalVendas.divide(evento.getMetaVendas(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            response.setPercentualMeta(percentual);
        }
        
        response.setMetaVendasAlcancada(evento.isMetaVendasAlcancada());
        response.setMetaQuantidadeAlcancada(evento.isMetaQuantidadeAlcancada());
        
        return response;
    }

    private EventoDTO.EventoResumo mapearParaResumo(Evento evento) {
        EventoDTO.EventoResumo resumo = new EventoDTO.EventoResumo();
        resumo.setId(evento.getId());
        resumo.setNome(evento.getNome());
        resumo.setDescricao(evento.getDescricao());
        resumo.setDataInicio(evento.getDataInicio());
        resumo.setDataFim(evento.getDataFim());
        resumo.setLocal(evento.getLocal());
        resumo.setStatus(evento.getStatus());
        resumo.setStatusDescricao(evento.getStatusDescricao());
        resumo.setStatusCor(evento.getStatusCor());
        resumo.setAtivo(evento.getAtivo());
        resumo.setDescontoPercentual(evento.getDescontoPercentual());
        resumo.setEventoVigente(evento.isEventoVigente());
        resumo.setTemDesconto(evento.temDesconto());
        resumo.setDuracaoEmDias(evento.getDuracaoEmDias());
        
        BigDecimal totalVendas = eventoRepository.getTotalVendasEvento(evento.getId()).orElse(BigDecimal.ZERO);
        Integer quantidadeVendas = eventoRepository.getQuantidadeVendasEvento(evento.getId()).orElse(0);
        
        resumo.setTotalVendas(totalVendas);
        resumo.setQuantidadeVendas(quantidadeVendas);
        
        return resumo;
    }
}
