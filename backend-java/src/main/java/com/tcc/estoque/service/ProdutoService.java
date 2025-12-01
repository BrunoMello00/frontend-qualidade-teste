package com.tcc.estoque.service;

import com.tcc.estoque.dto.ProdutoDTO;
import com.tcc.estoque.dto.TamanhoProdutoDTO;
import com.tcc.estoque.enums.TipoCodigoBarras;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.model.TamanhoProduto;
import com.tcc.estoque.model.Usuario;
import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.TamanhoProdutoRepository;
import com.tcc.estoque.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço de produtos com melhorias para locks e códigos de barras
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final TamanhoProdutoRepository tamanhoProdutoRepository;
    private final RecordLockService recordLockService;
    private final CodigoBarrasService codigoBarrasService;
    private final SecurityUtil securityUtil;

    public Page<ProdutoDTO.ProdutoResponse> listarProdutos(Pageable pageable, String departamento, String nome, String tamanho) {
        log.debug("Listando produtos ATIVOS - departamento: {}, nome: {}, tamanho: {}", departamento, nome, tamanho);

        Page<Produto> produtos;

    if (tamanho != null && departamento != null && nome != null) {
        produtos = produtoRepository.findDistinctByTamanhos_TamanhoIgnoreCaseAndDepartamentoContainingIgnoreCaseAndNomeContainingIgnoreCase(
            tamanho, departamento, nome, pageable);
        } else if (tamanho != null && departamento != null) {
            produtos = produtoRepository.findDistinctByTamanhos_TamanhoIgnoreCaseAndDepartamentoContainingIgnoreCaseAndAtivoTrue(
                    tamanho, departamento, pageable);
        } else if (tamanho != null && nome != null) {
            produtos = produtoRepository.findDistinctByTamanhos_TamanhoIgnoreCaseAndNomeContainingIgnoreCaseAndAtivoTrue(
                    tamanho, nome, pageable);
        } else if (tamanho != null) {
            // 🔍 Buscar por tamanho (join com tamanhos) - produtos ativos
            produtos = produtoRepository.findDistinctByTamanhos_TamanhoIgnoreCaseAndAtivoTrue(tamanho, pageable);
        } else if (departamento != null && nome != null) {
            // 🔍 Buscar por departamento E nome (apenas produtos ativos)
            produtos = produtoRepository.findByAtivoTrueAndDepartamentoContainingIgnoreCaseAndNomeContainingIgnoreCase(departamento, nome, pageable);
        } else if (departamento != null) {
            // 🔍 Buscar por departamento (apenas produtos ativos)
            produtos = produtoRepository.findByAtivoTrueAndDepartamentoContainingIgnoreCase(departamento, pageable);
        } else if (nome != null) {
            // 🔍 Buscar por nome (apenas produtos ativos)
            produtos = produtoRepository.findByAtivoTrueAndNomeContainingIgnoreCase(nome, pageable);
        } else {
            // 🔍 Listar todos (apenas produtos ativos)
            produtos = produtoRepository.findByAtivoTrue(pageable);
        }

        return produtos.map(this::convertToResponse);
    }

    public ProdutoDTO.ProdutoResponse buscarPorId(Long id) {
        log.debug("Buscando produto por ID: {}", id);
        
        Produto produto = produtoRepository.findByIdWithTamanhos(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
        
        return convertToResponse(produto);
    }

    @Transactional
    public ProdutoDTO.ProdutoResponse criarProduto(ProdutoDTO.ProdutoRequest produtoRequest) {
        log.debug("Criando novo produto: {}", produtoRequest.getNome());
        
        String codigoFinal = processarCodigoBarras(produtoRequest);
        TipoCodigoBarras tipoFinal = produtoRequest.getTipoCodigoBarras() != null ? 
                                   produtoRequest.getTipoCodigoBarras() : 
                                   TipoCodigoBarras.PERSONALIZADO;
        
        if (codigoFinal != null && produtoRepository.existsByCodigo(codigoFinal)) {
            throw new RuntimeException("Já existe um produto com este código: " + codigoFinal);
        }
        
        Usuario usuarioLogado = securityUtil.getUsuarioLogado();
        
        String codigoResumido = codigoBarrasService.gerarProximoCodigoResumido();
        
        Long proximoCodigoInterno = obterProximoCodigoInternoSequencial();
        
        Produto produto = Produto.builder()
                .nome(produtoRequest.getNome())
                .descricao(produtoRequest.getDescricao())
                .preco(produtoRequest.getPreco())
                .departamento(produtoRequest.getDepartamento())
                .fornecedor(produtoRequest.getFornecedor())
                .codigo(codigoFinal)
                .codigoResumido(codigoResumido)
                .codigoInternoSequencial(proximoCodigoInterno) // 🆔 ADICIONADO
                .tipoCodigoBarras(tipoFinal)
                .prefixoCodigo(produtoRequest.getPrefixoCodigo() != null ? 
                    produtoRequest.getPrefixoCodigo() : "PROD") // 📝 Valor padrão melhor
                .estoque(produtoRequest.getEstoque() != null ? produtoRequest.getEstoque() : 0) // Usar estoque fornecido ou 0
                .estoqueMinimo(produtoRequest.getEstoqueMinimo())
                .custoUnitario(produtoRequest.getCustoUnitario() != null ? 
                    produtoRequest.getCustoUnitario() : BigDecimal.ZERO)
                .margem(produtoRequest.getMargem()) // 📊 ADICIONADO CAMPO MARGEM
                .pontosRecompensa(produtoRequest.getPontosRecompensa() != null ? 
                    produtoRequest.getPontosRecompensa() : 1) // 🎁 CAMPO PONTOS RECOMPENSA
                .usuarioCadastro(usuarioLogado) // 👤 ADICIONADO USUÁRIO LOGADO
                .ativo(true)
                .dataCadastro(LocalDateTime.now())
                .dataAtualizacao(LocalDateTime.now())
                .build();
        
        produto = produtoRepository.save(produto);
        log.info("✅ Produto criado com sucesso - ID: {}, Código: {}, Código Resumido: {}", 
                 produto.getId(), produto.getCodigo(), produto.getCodigoResumido());
        
        if (produtoRequest.getTamanhos() != null && !produtoRequest.getTamanhos().isEmpty()) {
            log.info("📏 Processando {} tamanhos para o produto ID: {}", 
                    produtoRequest.getTamanhos().size(), produto.getId());
            
            for (TamanhoProdutoDTO tamanhoDTO : produtoRequest.getTamanhos()) {
                try {
                    Integer estoqueInicial = tamanhoDTO.getQuantidade() != null ? 
                                           tamanhoDTO.getQuantidade() : 0;
                    
                    TamanhoProduto tamanho = TamanhoProduto.builder()
                            .produto(produto)
                            .tamanho(tamanhoDTO.getTamanho())
                            .estoque(estoqueInicial)
                            .preco(tamanhoDTO.getPreco())
                            .codigoBarras(tamanhoDTO.getCodigoBarras())
                            .vendidas(0)
                            .ativo(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    
                    tamanhoProdutoRepository.save(tamanho);
                    log.debug("✅ Tamanho criado: {} - Estoque: {}", 
                             tamanhoDTO.getTamanho(), estoqueInicial);
                } catch (Exception e) {
                    log.warn("⚠️ Erro ao criar tamanho '{}' para produto ID {}: {}", 
                            tamanhoDTO.getTamanho(), produto.getId(), e.getMessage());
                }
            }
            
            produto = produtoRepository.findByIdWithTamanhos(produto.getId()).orElse(produto);
        }
        
        return convertToResponse(produto);
    }

    /**
     * Processar código de barras baseado na requisição
     */
    private String processarCodigoBarras(ProdutoDTO.ProdutoRequest request) {
        if (Boolean.TRUE.equals(request.getGerarCodigoAutomatico()) && 
            request.getTipoCodigoBarras() != null && 
            request.getTipoCodigoBarras().permiteGeracaoAutomatica()) {
            
            return codigoBarrasService.gerarProximoCodigo(
                request.getTipoCodigoBarras(), 
                request.getPrefixoCodigo()
            );
        }
        
        if (request.getCodigoBarras() != null && !request.getCodigoBarras().trim().isEmpty()) {
            TipoCodigoBarras tipo = request.getTipoCodigoBarras() != null ? 
                                   request.getTipoCodigoBarras() : 
                                   codigoBarrasService.detectarTipoCodigo(request.getCodigoBarras());
            
            if (!codigoBarrasService.validarCodigoBarras(request.getCodigoBarras(), tipo)) {
                throw new RuntimeException("Código de barras inválido para o tipo especificado");
            }
            
            return request.getCodigoBarras();
        }
        
        return codigoBarrasService.gerarProximoCodigo(
            TipoCodigoBarras.PERSONALIZADO, 
            request.getPrefixoCodigo() != null ? request.getPrefixoCodigo() : "PROD"
        );
    }

    @Transactional
    public ProdutoDTO.ProdutoResponse atualizarProduto(Long id, ProdutoDTO.ProdutoRequest produtoRequest, 
                                                      Long usuarioId, String ipAddress, String userAgent) {
        log.debug("Atualizando produto ID: {}", id);
        
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
        
        if (recordLockService.isProdutoBloqueado(id) && 
            !recordLockService.usuarioTemLockProduto(id, usuarioId)) {
            
            Map<String, Object> lockInfo = recordLockService.obterInfoLockProduto(id);
            String usuarioBloqueio = lockInfo != null ? (String) lockInfo.get("usuario_nome") : "Desconhecido";
            throw new RuntimeException("Produto está sendo editado por: " + usuarioBloqueio);
        }
        
        if (!recordLockService.usuarioTemLockProduto(id, usuarioId)) {
            boolean lockAdquirido = recordLockService.adquirirLockProduto(id, usuarioId, ipAddress, userAgent);
            if (!lockAdquirido) {
                throw new RuntimeException("Não foi possível obter permissão para editar o produto");
            }
        }
        
        try {
            if (produtoRequest.getCodigoBarras() != null && 
                !produtoRequest.getCodigoBarras().equals(produto.getCodigo())) {
                
                if (produtoRepository.existsByCodigo(produtoRequest.getCodigoBarras())) {
                    throw new RuntimeException("Já existe um produto com este código: " + produtoRequest.getCodigoBarras());
                }
                
                TipoCodigoBarras tipo = produtoRequest.getTipoCodigoBarras() != null ? 
                                       produtoRequest.getTipoCodigoBarras() : produto.getTipoCodigoBarras();
                
                if (!codigoBarrasService.validarCodigoBarras(produtoRequest.getCodigoBarras(), tipo)) {
                    throw new RuntimeException("Código de barras inválido para o tipo especificado");
                }
                
                produto.setCodigo(produtoRequest.getCodigoBarras());
            }
            
            // 🔒 VALIDAÇÕES DE REGRAS DE NEGÓCIO PARA ALTERAÇÕES
            
            if (produtoRequest.getDepartamento() != null && 
                !produtoRequest.getDepartamento().equals(produto.getDepartamento())) {
                
                boolean temTamanhos = produto.getTamanhos() != null && !produto.getTamanhos().isEmpty();
                boolean saiuDepartamentoRoupas = produto.getDepartamento().equals("06") && 
                                                !produtoRequest.getDepartamento().equals("06");
                
                if (temTamanhos && saiuDepartamentoRoupas) {
                    throw new RuntimeException(
                        "Não é possível alterar o departamento de um produto que possui tamanhos cadastrados. " +
                        "Remova os tamanhos primeiro ou mantenha no departamento 06 (Roupas)."
                    );
                }
                
                log.info("🔄 Alterando departamento do produto {} de '{}' para '{}'", 
                        produto.getId(), produto.getDepartamento(), produtoRequest.getDepartamento());
            }
            
            if (produtoRequest.getTipoCodigoBarras() != null && 
                !produtoRequest.getTipoCodigoBarras().equals(produto.getTipoCodigoBarras())) {
                
                log.info("🔄 Alterando tipo de código do produto {} de '{}' para '{}'", 
                        produto.getId(), produto.getTipoCodigoBarras(), produtoRequest.getTipoCodigoBarras());
                
                if (!produtoRequest.getTipoCodigoBarras().validar(produto.getCodigo())) {
                    String novoCodigo = processarCodigoBarras(produtoRequest);
                    produto.setCodigo(novoCodigo);
                    log.info("📝 Código regenerado para novo tipo: {}", novoCodigo);
                }
            }
            
            log.debug("📝 Atualizando campos do produto ID: {}", produto.getId());
            produto.setNome(produtoRequest.getNome());
            produto.setDescricao(produtoRequest.getDescricao());
            produto.setPreco(produtoRequest.getPreco());
            produto.setDepartamento(produtoRequest.getDepartamento());
            produto.setFornecedor(produtoRequest.getFornecedor());
            produto.setEstoqueMinimo(produtoRequest.getEstoqueMinimo());
            
            if (produtoRequest.getCustoUnitario() != null) {
                produto.setCustoUnitario(produtoRequest.getCustoUnitario());
            }
            
            if (produtoRequest.getMargem() != null) {
                produto.setMargem(produtoRequest.getMargem());
            }
            
            if (produtoRequest.getPontosRecompensa() != null) {
                produto.setPontosRecompensa(produtoRequest.getPontosRecompensa());
            }
            
            if (produtoRequest.getTipoCodigoBarras() != null) {
                produto.setTipoCodigoBarras(produtoRequest.getTipoCodigoBarras());
            }
            
            if (produtoRequest.getPrefixoCodigo() != null) {
                produto.setPrefixoCodigo(produtoRequest.getPrefixoCodigo());
            }
            
            produto.setDataAtualizacao(LocalDateTime.now());
            
            produto = produtoRepository.save(produto);
            log.info("✅ Produto atualizado com sucesso - ID: {}", produto.getId());
            
            // 📏 PROCESSAR TAMANHOS SE FORAM FORNECIDOS (IMPLEMENTAÇÃO IGUAL À CRIAÇÃO)
            if (produtoRequest.getTamanhos() != null && !produtoRequest.getTamanhos().isEmpty()) {
                log.info("📏 Processando {} tamanhos para o produto ID: {}", 
                        produtoRequest.getTamanhos().size(), produto.getId());
                
                List<TamanhoProduto> tamanhosExistentes = tamanhoProdutoRepository.findByProdutoId(produto.getId());
                for (TamanhoProduto tamanhoExistente : tamanhosExistentes) {
                    tamanhoProdutoRepository.delete(tamanhoExistente);
                    log.debug("🗑️ Removido tamanho existente: {} para produto ID: {}", 
                             tamanhoExistente.getTamanho(), produto.getId());
                }
                
                for (TamanhoProdutoDTO tamanhoDTO : produtoRequest.getTamanhos()) {
                    try {
                        TamanhoProduto tamanho = TamanhoProduto.builder()
                                .produto(produto)
                                .tamanho(tamanhoDTO.getTamanho())
                                .estoque(0) // Sempre inicializar com zero - será gerenciado pela área de movimentação
                                .preco(tamanhoDTO.getPreco())
                                .codigoBarras(tamanhoDTO.getCodigoBarras())
                                .vendidas(0) // Reset vendas para novos tamanhos
                                .ativo(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        
                        tamanhoProdutoRepository.save(tamanho);
                        log.debug("✅ Tamanho atualizado: {} - Estoque: {}", 
                                 tamanhoDTO.getTamanho(), tamanho.getEstoque());
                    } catch (Exception e) {
                        log.warn("⚠️ Erro ao atualizar tamanho '{}' para produto ID {}: {}", 
                                tamanhoDTO.getTamanho(), produto.getId(), e.getMessage());
                    }
                }
                
                produto = produtoRepository.findByIdWithTamanhos(produto.getId()).orElse(produto);
                log.info("🔄 Produto recarregado com {} tamanhos atualizados", produto.getTamanhos().size());
            }
            
            return convertToResponse(produto);
            
        } finally {
            recordLockService.liberarLockProduto(id, usuarioId);
        }
    }

    @Transactional
    public void excluirProduto(Long id) {
        log.debug("Excluindo produto ID: {}", id);
        
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
        
        produto.setAtivo(false);
        produtoRepository.save(produto);
        
        log.info("Produto excluído (soft delete) - ID: {}", id);
    }
    
    /**
     * Reativar produto desabilitado
     */
    @Transactional
    public void reativarProduto(Long id) {
        log.debug("Reativando produto ID: {}", id);
        
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
        
        produto.setAtivo(true);
        produtoRepository.save(produto);
        
        log.info("Produto reativado - ID: {}", id);
    }

    /**
     * Atualizar qualidade do produto
     */
    @Transactional
    public void atualizarQualidadeProduto(Long id, ProdutoDTO.QualidadeRequest qualidadeRequest, 
                                         Long usuarioId, String ipAddress, String userAgent) {
        log.debug("Atualizando qualidade do produto ID: {} para status: {}", id, qualidadeRequest.getStatusQualidade());
        
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
        
        // Registrar status anterior para auditoria
        com.tcc.estoque.model.enums.StatusQualidade statusAnterior = produto.getStatusQualidade();
        
        // Atualizar status de qualidade
        produto.setStatusQualidade(qualidadeRequest.getStatusQualidade());
        produto.setDataAtualizacao(LocalDateTime.now());
        
        produtoRepository.save(produto);
        
        log.info("Qualidade do produto atualizada - ID: {}, Status anterior: {}, Novo status: {}, Observações: {}", 
                id, statusAnterior, qualidadeRequest.getStatusQualidade(), qualidadeRequest.getObservacoes());
    }
    
    /**
     * Listar produtos desabilitados (para administração)
     */
    public Page<ProdutoDTO.ProdutoResponse> listarProdutosDesabilitados(Pageable pageable) {
        log.debug("Listando produtos DESABILITADOS");
        
        Page<Produto> produtos = produtoRepository.findByAtivoFalse(pageable);
        return produtos.map(this::convertToResponse);
    }

    public List<ProdutoDTO.ProdutoResumo> buscarProdutosEstoqueBaixo(Integer limite) {
        log.debug("Buscando produtos com estoque baixo");
        
        List<Produto> produtos = produtoRepository.findProdutosComEstoqueBaixo();
        
        return produtos.stream()
                .limit(limite != null ? limite : 10)
                .map(this::convertToResumo)
                .collect(Collectors.toList());
    }

    public List<String> listarDepartamentos() {
        log.debug("Listando departamentos de produtos");
        return produtoRepository.findDistinctDepartamentos();
    }

    @Transactional
    public ProdutoDTO.ProdutoResponse atualizarEstoque(Long id, ProdutoDTO.EstoqueRequest estoqueRequest) {
        log.debug("Atualizando estoque do produto ID: {}", id);
        
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
        
        produto.setEstoque(estoqueRequest.getQuantidade());
        produto = produtoRepository.save(produto);
        
        log.info("Estoque atualizado - Produto ID: {}, Nova quantidade: {}", 
                produto.getId(), produto.getEstoque());
        
        return convertToResponse(produto);
    }

    public ProdutoDTO.ProdutoResponse buscarPorCodigoBarras(String codigo) {
        log.debug("Buscando produto por código: {}", codigo);
        
        Produto produto = produtoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com código: " + codigo));
        
        return convertToResponse(produto);
    }

    public Object obterEstatisticas() {
        log.debug("Obtendo estatísticas de produtos");
        
        long totalProdutos = produtoRepository.count();
        long produtosAtivos = produtoRepository.countByAtivoTrue();
        long produtosEstoqueBaixo = produtoRepository.countProdutosComEstoqueBaixo();
        
        return ProdutoDTO.EstatisticasResponse.builder()
                .totalProdutos(totalProdutos)
                .produtosAtivos(produtosAtivos)
                .produtosEstoqueBaixo(produtosEstoqueBaixo)
                .build();
    }

    private ProdutoDTO.ProdutoResponse convertToResponse(Produto produto) {
        Map<String, Object> lockInfo = recordLockService.obterInfoLockProduto(produto.getId());
        
        List<TamanhoProdutoDTO> tamanhos = produto.getTamanhos() != null ? 
            produto.getTamanhos().stream()
                .map(this::convertTamanhoToDTO)
                .collect(Collectors.toList()) : 
            List.of();
        
        return ProdutoDTO.ProdutoResponse.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .preco(produto.getPreco())
                .quantidadeEstoque(produto.getEstoqueTotal()) // Usa getEstoqueTotal() que considera produtos com e sem tamanhos
                .departamento(produto.getDepartamento())
                .fornecedor(produto.getFornecedor())
                .estoqueMinimo(produto.getEstoqueMinimo())
                .custoUnitario(produto.getCustoUnitario())  // ADICIONANDO CAMPO CUSTOUNITARIO
                .margem(produto.getMargem()) // 📊 ADICIONANDO CAMPO MARGEM
                .pontosRecompensa(produto.getPontosRecompensa()) // 🎁 ADICIONANDO CAMPO PONTOS RECOMPENSA
                .codigoBarras(produto.getCodigo())
                .codigoResumido(produto.getCodigoResumido())
                .codigoInternoSequencial(produto.getCodigoInternoSequencial())
                .tipoCodigoBarras(produto.getTipoCodigoBarras())
                .prefixoCodigo(produto.getPrefixoCodigo())
                .ativo(produto.getAtivo())
                .dataCadastro(produto.getDataCadastro())
                .dataAtualizacao(produto.getDataAtualizacao())
                .estoqueBaixo(produto.isEstoqueBaixo())
                .bloqueado(lockInfo != null)
                .bloqueadoPorUsuario(lockInfo != null ? (String) lockInfo.get("usuario_nome") : null)
                .bloqueioExpiraEm((lockInfo != null && lockInfo.get("data_expiracao") != null) ? 
                    ((java.sql.Timestamp) lockInfo.get("data_expiracao")).toLocalDateTime() : null)
                .codigoValido(produto.isCodigoValido())
                .padraoBrasileiro(produto.isPadraoBrasileiro())
                .codigoFormatado(produto.obterCodigoFormatado())
                .tamanhos(tamanhos)
                .build();
    }

    /**
     * Converte TamanhoProduto para TamanhoProdutoDTO
     */
    private TamanhoProdutoDTO convertTamanhoToDTO(TamanhoProduto tamanho) {
        return TamanhoProdutoDTO.builder()
                .id(tamanho.getId())
                .tamanho(tamanho.getTamanho())
                .preco(tamanho.getPreco())
                .codigoBarras(tamanho.getCodigoBarras())
                .ativo(tamanho.getAtivo())
                .produtoId(tamanho.getProduto() != null ? tamanho.getProduto().getId() : null)
                .estoque(tamanho.getEstoque()) // Campo reabilitado
                .vendidas(tamanho.getVendidas()) // Campo reabilitado
                .build();
    }

    private ProdutoDTO.ProdutoResumo convertToResumo(Produto produto) {
        return ProdutoDTO.ProdutoResumo.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .quantidadeEstoque(produto.getEstoque())
                .estoqueMinimo(produto.getEstoqueMinimo())
                .preco(produto.getPreco())
                .estoqueBaixo(produto.isEstoqueBaixo())
                .build();
    }

    // ========================================
    // ========================================

    @Transactional
    public ProdutoDTO.LockResponse adquirirLockProduto(Long produtoId, Long usuarioId, String ipAddress, String userAgent) {
        log.debug("Tentando adquirir lock para produto ID: {} pelo usuário: {}", produtoId, usuarioId);
        
        if (!produtoRepository.existsById(produtoId)) {
            return ProdutoDTO.LockResponse.builder()
                    .sucesso(false)
                    .mensagem("Produto não encontrado")
                    .podeEditar(false)
                    .build();
        }
        
        if (recordLockService.isProdutoBloqueado(produtoId) && 
            !recordLockService.usuarioTemLockProduto(produtoId, usuarioId)) {
            
            Map<String, Object> lockInfo = recordLockService.obterInfoLockProduto(produtoId);
            return ProdutoDTO.LockResponse.builder()
                    .sucesso(false)
                    .mensagem("Produto está sendo editado por outro usuário")
                    .bloqueadoPorUsuario(lockInfo != null ? (String) lockInfo.get("usuario_nome") : "Desconhecido")
                    .bloqueioExpiraEm(lockInfo != null ? 
                        ((java.sql.Timestamp) lockInfo.get("data_expiracao")).toLocalDateTime() : null)
                    .podeEditar(false)
                    .build();
        }
        
        boolean sucesso = recordLockService.adquirirLockProduto(produtoId, usuarioId, ipAddress, userAgent);
        
        return ProdutoDTO.LockResponse.builder()
                .sucesso(sucesso)
                .mensagem(sucesso ? "Lock adquirido com sucesso" : "Falha ao adquirir lock")
                .podeEditar(sucesso)
                .build();
    }

    @Transactional
    public ProdutoDTO.LockResponse liberarLockProduto(Long produtoId, Long usuarioId) {
        log.debug("Liberando lock do produto ID: {} pelo usuário: {}", produtoId, usuarioId);
        
        boolean sucesso = recordLockService.liberarLockProduto(produtoId, usuarioId);
        
        return ProdutoDTO.LockResponse.builder()
                .sucesso(sucesso)
                .mensagem(sucesso ? "Lock liberado com sucesso" : "Nenhum lock encontrado para liberar")
                .podeEditar(false)
                .build();
    }

    public ProdutoDTO.LockResponse verificarStatusLock(Long produtoId, Long usuarioId) {
        Map<String, Object> lockInfo = recordLockService.obterInfoLockProduto(produtoId);
        boolean usuarioTemLock = recordLockService.usuarioTemLockProduto(produtoId, usuarioId);
        
        return ProdutoDTO.LockResponse.builder()
                .sucesso(lockInfo != null)
                .mensagem(lockInfo != null ? "Produto está bloqueado" : "Produto disponível para edição")
                .bloqueadoPorUsuario(lockInfo != null ? (String) lockInfo.get("usuario_nome") : null)
                .bloqueioExpiraEm(lockInfo != null ? 
                    ((java.sql.Timestamp) lockInfo.get("data_expiracao")).toLocalDateTime() : null)
                .podeEditar(usuarioTemLock)
                .build();
    }

    // ========================================
    // ========================================

    public String gerarCodigoPersonalizado(TipoCodigoBarras tipo, String prefixo) {
        return codigoBarrasService.gerarProximoCodigo(tipo, prefixo);
    }

    public List<Map<String, Object>> listarTiposCodigoSuportados() {
        return codigoBarrasService.listarTiposCodigoBarras();
    }

    public ProdutoDTO.ProdutoResponse buscarPorCodigoResumido(String codigoResumido) {
        var produtoOpt = codigoBarrasService.buscarPorCodigoResumido(codigoResumido);
        
        if (produtoOpt.isEmpty()) {
            throw new RuntimeException("Produto não encontrado com código resumido: " + codigoResumido);
        }
        
        Map<String, Object> produtoMap = produtoOpt.get();
        Long id = ((Number) produtoMap.get("id")).longValue();
        
        return buscarPorId(id);
    }
    
    /**
     * Gera o próximo código interno sequencial
     * Busca o maior código existente e incrementa
     */
    private Long obterProximoCodigoInternoSequencial() {
        Long maxCodigo = produtoRepository.findMaxCodigoInternoSequencial();
        return (maxCodigo != null ? maxCodigo : 0L) + 1;
    }
}
