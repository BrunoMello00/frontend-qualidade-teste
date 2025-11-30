package com.tcc.estoque.service;

import com.tcc.estoque.dto.ClienteDTO;
import com.tcc.estoque.model.Cliente;
import com.tcc.estoque.model.HistoricoPontos;
import com.tcc.estoque.model.enums.CategoriaCliente;
import com.tcc.estoque.repository.ClienteRepository;
import com.tcc.estoque.repository.HistoricoPontosRepository;
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
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private HistoricoPontosRepository historicoPontosRepository;

    public ClienteDTO.ClienteResponse criarCliente(ClienteDTO.ClienteRequest request) {
        validarCpfUnico(request.getCpf(), null);
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            validarEmailUnico(request.getEmail(), null);
        }

        Cliente cliente = mapearParaEntidade(request);
        cliente.setDataCadastro(LocalDateTime.now());
        cliente.setAtivo(true);
        cliente.setPontos(0);
        cliente.setTotalCompras(BigDecimal.ZERO);
        cliente.setQuantidadeCompras(0);
        cliente.setIsFake(false);

        cliente.setCategoria(CategoriaCliente.BRONZE);

        cliente = clienteRepository.save(cliente);
        return mapearParaResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteDTO.ClienteResponse buscarPorId(Long id) {
        Cliente cliente = buscarClientePorId(id);
        return mapearParaResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteDTO.ClienteResponse buscarPorCpf(String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com CPF: " + cpf));
        return mapearParaResponse(cliente);
    }

    public ClienteDTO.ClienteResponse atualizarCliente(Long id, ClienteDTO.ClienteRequest request) {
        Cliente cliente = buscarClientePorId(id);

        validarCpfUnico(request.getCpf(), id);
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            validarEmailUnico(request.getEmail(), id);
        }

        atualizarDadosCliente(cliente, request);
        cliente = clienteRepository.save(cliente);
        return mapearParaResponse(cliente);
    }

    public void deletarCliente(Long id) {
        Cliente cliente = buscarClientePorId(id);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    public void reativarCliente(Long id) {
        Cliente cliente = buscarClientePorId(id);
        cliente.setAtivo(true);
        clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Page<ClienteDTO.ClienteResumo> listarClientes(ClienteDTO.FiltroClientes filtro, Pageable pageable) {
        Sort sort = criarOrdenacao(filtro.getOrderBy(), filtro.getOrderDirection());
        Pageable pageableComSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Cliente> clientes = clienteRepository.findComFiltros(
                filtro.getTermo(),
                filtro.getCategoria(),
                filtro.getAtivo(),
                filtro.getDataCadastroInicio() != null ? filtro.getDataCadastroInicio().atStartOfDay() : null,
                filtro.getDataCadastroFim() != null ? filtro.getDataCadastroFim().atTime(23, 59, 59) : null,
                filtro.getUltimaCompraInicio() != null ? filtro.getUltimaCompraInicio().atStartOfDay() : null,
                filtro.getUltimaCompraFim() != null ? filtro.getUltimaCompraFim().atTime(23, 59, 59) : null,
                filtro.getPontosMinimos(),
                filtro.getPontosMaximos(),
                pageableComSort
        );

        return clientes.map(this::mapearParaResumo);
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO.ClienteResumo> buscarPorTermo(String termo) {
        Page<Cliente> clientes = clienteRepository.findByTermoGeral(termo, PageRequest.of(0, 50));
        return clientes.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    public void adicionarPontos(Long clienteId, ClienteDTO.PontosRequest request) {
        Cliente cliente = buscarClientePorId(clienteId);

        int pontosAnteriores = cliente.getPontos();
        int novosPontos = pontosAnteriores + request.getPontos();

        cliente.setPontos(novosPontos);

        CategoriaCliente novaCategoria = CategoriaCliente.determinarCategoria(novosPontos);
        cliente.setCategoria(novaCategoria);

        clienteRepository.save(cliente);

        HistoricoPontos historico = new HistoricoPontos();
        historico.setClienteId(clienteId);
        historico.setVendaId(request.getVendaId());
        historico.setPontosAdicionados(request.getPontos());
        historico.setPontosAntes(pontosAnteriores);
        historico.setPontosDepois(novosPontos);
        historico.setMotivo(request.getMotivo());
        historico.setObservacoes(request.getObservacoes());
        historico.setDataOperacao(LocalDateTime.now());

        historicoPontosRepository.save(historico);
    }

    public void removerPontos(Long clienteId, ClienteDTO.PontosRequest request) {
        Cliente cliente = buscarClientePorId(clienteId);

        int pontosAnteriores = cliente.getPontos();
        int novosPontos = Math.max(0, pontosAnteriores - request.getPontos());

        cliente.setPontos(novosPontos);

        CategoriaCliente novaCategoria = CategoriaCliente.determinarCategoria(novosPontos);
        cliente.setCategoria(novaCategoria);

        clienteRepository.save(cliente);

        HistoricoPontos historico = new HistoricoPontos();
        historico.setClienteId(clienteId);
        historico.setPontosAdicionados(-request.getPontos());
        historico.setPontosAntes(pontosAnteriores);
        historico.setPontosDepois(novosPontos);
        historico.setMotivo(request.getMotivo());
        historico.setObservacoes(request.getObservacoes());
        historico.setDataOperacao(LocalDateTime.now());

        historicoPontosRepository.save(historico);
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO.HistoricoPontosResponse> buscarHistoricoPontos(Long clienteId) {
        List<HistoricoPontos> historico = historicoPontosRepository.findByClienteIdOrderByDataOperacaoDesc(clienteId);
        return historico.stream()
                .map(this::mapearHistoricoParaResponse)
                .collect(Collectors.toList());
    }

    public void registrarCompra(Long clienteId, BigDecimal valorCompra, Long vendaId) {
        Cliente cliente = buscarClientePorId(clienteId);

        cliente.setTotalCompras(cliente.getTotalCompras().add(valorCompra));
        cliente.setQuantidadeCompras(cliente.getQuantidadeCompras() + 1);
        cliente.setUltimaCompra(LocalDateTime.now());

        int pontosGanhos = valorCompra.divide(BigDecimal.TEN, RoundingMode.DOWN).intValue();

        if (pontosGanhos > 0) {
            int pontosAnteriores = cliente.getPontos();
            int novosPontos = pontosAnteriores + pontosGanhos;

            cliente.setPontos(novosPontos);

            CategoriaCliente novaCategoria = CategoriaCliente.determinarCategoria(novosPontos);
            cliente.setCategoria(novaCategoria);

            HistoricoPontos historico = new HistoricoPontos();
            historico.setClienteId(clienteId);
            historico.setVendaId(vendaId);
            historico.setPontosAdicionados(pontosGanhos);
            historico.setPontosAntes(pontosAnteriores);
            historico.setPontosDepois(novosPontos);
            historico.setMotivo("compra");
            historico.setObservacoes("Pontos ganhos por compra de R$ " + valorCompra);
            historico.setDataOperacao(LocalDateTime.now());

            historicoPontosRepository.save(historico);
        }

        clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteDTO.EstatisticasClienteResponse obterEstatisticas() {
        ClienteDTO.EstatisticasClienteResponse stats = new ClienteDTO.EstatisticasClienteResponse();

        stats.setTotalClientes(clienteRepository.count());
        stats.setClientesAtivos(clienteRepository.countByAtivoTrue());
        stats.setTicketMedioGeral(clienteRepository.calcularTicketMedioGeral());
        stats.setFaturamentoTotalClientes(clienteRepository.calcularFaturamentoTotal());
        stats.setPontosDistribuidosTotal(clienteRepository.calcularTotalPontosDistribuidos());

        List<Cliente> topClientes = clienteRepository.findClientesMaisFrequentes(PageRequest.of(0, 10));
        stats.setClientesMaisFrequentes(
                topClientes.stream()
                        .map(this::mapearParaClienteTop)
                        .collect(Collectors.toList())
        );

        List<Object[]> distribuicao = clienteRepository.contarClientesPorCategoria();
        stats.setDistribuicaoCategorias(
                distribuicao.stream()
                        .map(this::mapearParaDistribuicaoCategoria)
                        .collect(Collectors.toList())
        );

        return stats;
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO.ClienteResumo> buscarAniversariantes() {
        int mesAtual = LocalDate.now().getMonthValue();
        List<Cliente> aniversariantes = clienteRepository.findAniversariantesDoMes(mesAtual);
        return aniversariantes.stream()
                .map(this::mapearParaResumo)
                .collect(Collectors.toList());
    }

    private Cliente buscarClientePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com ID: " + id));
    }

    private void validarCpfUnico(String cpf, Long idExcluir) {
        boolean cpfExiste = (idExcluir == null) ?
                clienteRepository.existsByCpf(cpf) :
                clienteRepository.existsByCpfAndIdNot(cpf, idExcluir);

        if (cpfExiste) {
            throw new BusinessException("CPF já cadastrado: " + cpf);
        }
    }

    private void validarEmailUnico(String email, Long idExcluir) {
        boolean emailExiste = (idExcluir == null) ?
                clienteRepository.existsByEmail(email) :
                clienteRepository.existsByEmailAndIdNot(email, idExcluir);

        if (emailExiste) {
            throw new BusinessException("Email já cadastrado: " + email);
        }
    }

    private Sort criarOrdenacao(String orderBy, String orderDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(orderDirection) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;

        switch (orderBy.toLowerCase()) {
            case "nome": return Sort.by(direction, "nome");
            case "categoria": return Sort.by(direction, "categoria");
            case "pontos": return Sort.by(direction, "pontos");
            case "totalcompras": return Sort.by(direction, "totalCompras");
            case "ultimacompra": return Sort.by(direction, "ultimaCompra");
            default: return Sort.by(direction, "dataCadastro");
        }
    }

    private Cliente mapearParaEntidade(ClienteDTO.ClienteRequest request) {
        Cliente cliente = new Cliente();
        atualizarDadosCliente(cliente, request);
        return cliente;
    }

    private void atualizarDadosCliente(Cliente cliente, ClienteDTO.ClienteRequest request) {
        cliente.setNome(request.getNome());
        cliente.setCpf(request.getCpf());
        cliente.setEmail(request.getEmail());
        cliente.setTelefone(request.getTelefone());
        cliente.setDataNascimento(request.getDataNascimento());
        cliente.setObservacoes(request.getObservacoes());

        if (request.getEndereco() != null) {
            if (cliente.getEndereco() == null) {
                cliente.setEndereco(new com.tcc.estoque.model.Endereco());
            }
            cliente.getEndereco().setRua(request.getEndereco().getRua());
            cliente.getEndereco().setNumero(request.getEndereco().getNumero());
            cliente.getEndereco().setComplemento(request.getEndereco().getComplemento());
            cliente.getEndereco().setBairro(request.getEndereco().getBairro());
            cliente.getEndereco().setCidade(request.getEndereco().getCidade());
            cliente.getEndereco().setCep(request.getEndereco().getCep());
            cliente.getEndereco().setEstado(request.getEndereco().getEstado());
        }
    }

    private ClienteDTO.ClienteResponse mapearParaResponse(Cliente cliente) {
        ClienteDTO.ClienteResponse response = new ClienteDTO.ClienteResponse();
        response.setId(cliente.getId());
        response.setNome(cliente.getNome());
        response.setCpf(cliente.getCpf());
        response.setEmail(cliente.getEmail());
        response.setTelefone(cliente.getTelefone());
        response.setDataNascimento(cliente.getDataNascimento());
        response.setDataCadastro(cliente.getDataCadastro());
        response.setAtivo(cliente.getAtivo());
        response.setTotalCompras(cliente.getTotalCompras());
        response.setQuantidadeCompras(cliente.getQuantidadeCompras());
        response.setUltimaCompra(cliente.getUltimaCompra());
        response.setCategoria(cliente.getCategoria());
        response.setCategoriaDescricao(cliente.getCategoria().getDescricao());
        response.setCategoriaCor(cliente.getCategoria().getCor());
        response.setPontos(cliente.getPontos());
        response.setObservacoes(cliente.getObservacoes());
        response.setIsFake(cliente.getIsFake());

        if (cliente.getQuantidadeCompras() > 0) {
            response.setTicketMedio(cliente.getTotalCompras()
                    .divide(BigDecimal.valueOf(cliente.getQuantidadeCompras()), 2, RoundingMode.HALF_UP));
        }
        response.setIsClienteFrequente(cliente.getQuantidadeCompras() >= 5);

        if (cliente.getEndereco() != null) {
            ClienteDTO.EnderecoResponse endereco = new ClienteDTO.EnderecoResponse();
            endereco.setRua(cliente.getEndereco().getRua());
            endereco.setNumero(cliente.getEndereco().getNumero());
            endereco.setComplemento(cliente.getEndereco().getComplemento());
            endereco.setBairro(cliente.getEndereco().getBairro());
            endereco.setCidade(cliente.getEndereco().getCidade());
            endereco.setCep(cliente.getEndereco().getCep());
            endereco.setEstado(cliente.getEndereco().getEstado());
            endereco.setEnderecoCompleto(cliente.getEndereco().getEnderecoCompleto());
            response.setEndereco(endereco);
        }

        return response;
    }

    private ClienteDTO.ClienteResumo mapearParaResumo(Cliente cliente) {
        ClienteDTO.ClienteResumo resumo = new ClienteDTO.ClienteResumo();
        resumo.setId(cliente.getId());
        resumo.setNome(cliente.getNome());
        resumo.setCpf(cliente.getCpf());
        resumo.setEmail(cliente.getEmail());
        resumo.setCategoria(cliente.getCategoria());
        resumo.setCategoriaDescricao(cliente.getCategoria().getDescricao());
        resumo.setPontos(cliente.getPontos());
        resumo.setTotalCompras(cliente.getTotalCompras());
        resumo.setQuantidadeCompras(cliente.getQuantidadeCompras());
        resumo.setUltimaCompra(cliente.getUltimaCompra());
        resumo.setAtivo(cliente.getAtivo());
        return resumo;
    }

    private ClienteDTO.HistoricoPontosResponse mapearHistoricoParaResponse(HistoricoPontos historico) {
        ClienteDTO.HistoricoPontosResponse response = new ClienteDTO.HistoricoPontosResponse();
        response.setId(historico.getId());
        response.setClienteId(historico.getClienteId());
        response.setVendaId(historico.getVendaId());
        response.setProdutoId(historico.getProdutoId());
        response.setPontosAdicionados(historico.getPontosAdicionados());
        response.setPontosAntes(historico.getPontosAntes());
        response.setPontosDepois(historico.getPontosDepois());
        response.setMotivo(historico.getMotivo());
        response.setDataOperacao(historico.getDataOperacao());
        response.setObservacoes(historico.getObservacoes());
        response.setTipoOperacao(historico.getPontosAdicionados() > 0 ? "Adição" : "Remoção");
        response.setPontosAbsolutos(Math.abs(historico.getPontosAdicionados()));

        switch (historico.getMotivo()) {
            case "compra": response.setMotivoFormatado("Compra"); break;
            case "ajuste_manual": response.setMotivoFormatado("Ajuste Manual"); break;
            case "bonus": response.setMotivoFormatado("Bônus"); break;
            default: response.setMotivoFormatado(historico.getMotivo());
        }

        return response;
    }

    private ClienteDTO.ClienteTopResponse mapearParaClienteTop(Cliente cliente) {
        ClienteDTO.ClienteTopResponse top = new ClienteDTO.ClienteTopResponse();
        top.setId(cliente.getId());
        top.setNome(cliente.getNome());
        top.setCpf(cliente.getCpf());
        top.setQuantidadeCompras(cliente.getQuantidadeCompras());
        top.setValorTotal(cliente.getTotalCompras());
        top.setCategoria(cliente.getCategoria());
        top.setPontos(cliente.getPontos());
        
        if (cliente.getQuantidadeCompras() > 0) {
            top.setTicketMedio(cliente.getTotalCompras()
                    .divide(BigDecimal.valueOf(cliente.getQuantidadeCompras()), 2, RoundingMode.HALF_UP));
        }
        
        return top;
    }

    private ClienteDTO.DistribuicaoCategoriaResponse mapearParaDistribuicaoCategoria(Object[] dados) {
        ClienteDTO.DistribuicaoCategoriaResponse dist = new ClienteDTO.DistribuicaoCategoriaResponse();
        dist.setCategoria((CategoriaCliente) dados[0]);
        dist.setQuantidade((Long) dados[1]);
        dist.setCategoriaDescricao(dist.getCategoria().getDescricao());
        dist.setCategoriaCor(dist.getCategoria().getCor());
        
        long total = clienteRepository.countByAtivoTrue();
        if (total > 0) {
            dist.setPercentual(BigDecimal.valueOf(dist.getQuantidade())
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
        
        return dist;
    }

    /**
     * Busca o cliente fake para vendas sem identificação
     * Se não existir, cria automaticamente
     */
    public ClienteDTO.ClienteResponse buscarOuCriarClienteFake() {
        try {
            Cliente clienteFake = clienteRepository.findByCpf("000.000.000-00")
                    .orElse(null);
            
            if (clienteFake == null) {
                ClienteDTO.ClienteRequest requestFake = new ClienteDTO.ClienteRequest();
                requestFake.setNome("Cliente Não Identificado");
                requestFake.setCpf("000.000.000-00");
                requestFake.setEmail("nao-identificado@sistema.local");
                requestFake.setTelefone("(00) 00000-0000");
                
                ClienteDTO.EnderecoRequest enderecoFake = new ClienteDTO.EnderecoRequest();
                enderecoFake.setRua("Endereço não informado");
                enderecoFake.setNumero("S/N");
                enderecoFake.setCidade("Cidade não informada");
                enderecoFake.setEstado("UF");
                enderecoFake.setCep("00000-000");
                enderecoFake.setBairro("Bairro não informado");
                
                requestFake.setEndereco(enderecoFake);
                requestFake.setCategoria(CategoriaCliente.BRONZE);
                
                Cliente novoClienteFake = mapearParaEntidade(requestFake);
                novoClienteFake.setDataCadastro(LocalDateTime.now());
                novoClienteFake.setAtivo(true);
                novoClienteFake.setPontos(0);
                novoClienteFake.setTotalCompras(BigDecimal.ZERO);
                novoClienteFake.setQuantidadeCompras(0);
                novoClienteFake.setIsFake(true); // Marcar como cliente fake
                novoClienteFake.setCategoria(CategoriaCliente.BRONZE);
                
                clienteFake = clienteRepository.save(novoClienteFake);
            }
            
            return mapearParaResponse(clienteFake);
            
        } catch (Exception e) {
            throw new BusinessException("Erro ao buscar/criar cliente para venda sem identificação: " + e.getMessage());
        }
    }
}
