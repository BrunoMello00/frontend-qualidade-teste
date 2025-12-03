package com.tcc.estoque.service;

import com.tcc.estoque.dto.TamanhoProdutoDTO;
import com.tcc.estoque.dto.TamanhoProdutoEstoqueDTO;
import com.tcc.estoque.dto.CriarTamanhoComEstoqueRequest;
import com.tcc.estoque.model.Produto;
import com.tcc.estoque.model.TamanhoProduto;
import com.tcc.estoque.repository.ProdutoRepository;
import com.tcc.estoque.repository.TamanhoProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço para operações CRUD de tamanhos de produtos
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TamanhoProdutoService {

    private final TamanhoProdutoRepository tamanhoProdutoRepository;
    private final ProdutoRepository produtoRepository;

    /**
     * Busca todos os tamanhos de um produto
     */
    public List<TamanhoProdutoDTO> buscarTamanhosPorProdutoId(Long produtoId) {
        log.debug("Buscando tamanhos para produto ID: {}", produtoId);
        
        List<TamanhoProduto> tamanhos = tamanhoProdutoRepository.findByProdutoIdAndAtivoTrue(produtoId);
        
        return tamanhos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 🆕 Busca tamanhos com estoque disponível (> 0) para um produto
     */
    public List<TamanhoProdutoDTO> buscarTamanhosComEstoque(Long produtoId) {
        log.debug("Buscando tamanhos com estoque para produto ID: {}", produtoId);
        
        List<TamanhoProduto> tamanhos = tamanhoProdutoRepository.findByProdutoIdAndAtivoTrue(produtoId);
        
        return tamanhos.stream()
                .filter(tamanho -> tamanho.getEstoque() != null && tamanho.getEstoque() > 0)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca tamanho por ID
     */
    public TamanhoProdutoDTO buscarPorId(Long id) {
        log.debug("Buscando tamanho por ID: {}", id);
        
        TamanhoProduto tamanho = tamanhoProdutoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tamanho não encontrado com ID: " + id));
        
        return convertToDTO(tamanho);
    }

    /**
     * Cria um novo tamanho para um produto
     */
    @Transactional
    public TamanhoProdutoDTO criarTamanho(Long produtoId, TamanhoProdutoDTO tamanhoDTO) {
        log.debug("Criando tamanho para produto ID: {}", produtoId);
        
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + produtoId));
        
        if (tamanhoProdutoRepository.existsByProdutoIdAndTamanho(produtoId, tamanhoDTO.getTamanho())) {
            throw new RuntimeException("Já existe um tamanho '" + tamanhoDTO.getTamanho() + 
                                     "' para este produto");
        }
        
        TamanhoProduto tamanho = TamanhoProduto.builder()
                .produto(produto)
                .tamanho(tamanhoDTO.getTamanho())
                .estoque(0) // SEMPRE INICIALIZAR COM ZERO - estoque gerenciado na área de movimentação
                .preco(tamanhoDTO.getPreco())
                .codigoBarras(tamanhoDTO.getCodigoBarras())
                .vendidas(0)
                .ativo(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        tamanho = tamanhoProdutoRepository.save(tamanho);
        log.info("✅ Tamanho criado com sucesso - ID: {}, Produto: {}, Tamanho: {} (estoque será gerenciado na área de movimentação)", 
                 tamanho.getId(), produtoId, tamanho.getTamanho());
        
        return convertToDTO(tamanho);
    }

    /**
     * Atualiza um tamanho existente
     */
    @Transactional
    public TamanhoProdutoDTO atualizarTamanho(Long id, TamanhoProdutoDTO tamanhoDTO) {
        log.debug("Atualizando tamanho ID: {}", id);
        
        TamanhoProduto tamanho = tamanhoProdutoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tamanho não encontrado com ID: " + id));
        
        if (!tamanho.getTamanho().equals(tamanhoDTO.getTamanho()) &&
            tamanhoProdutoRepository.existsByProdutoIdAndTamanho(
                tamanho.getProduto().getId(), tamanhoDTO.getTamanho())) {
            throw new RuntimeException("Já existe um tamanho '" + tamanhoDTO.getTamanho() + 
                                     "' para este produto");
        }
        
        tamanho.setTamanho(tamanhoDTO.getTamanho());
        tamanho.setPreco(tamanhoDTO.getPreco());
        tamanho.setCodigoBarras(tamanhoDTO.getCodigoBarras());
        
        if (tamanhoDTO.getAtivo() != null) {
            tamanho.setAtivo(tamanhoDTO.getAtivo());
        }
        
        tamanho.setUpdatedAt(LocalDateTime.now());
        
        tamanho = tamanhoProdutoRepository.save(tamanho);
        log.info("✅ Tamanho atualizado com sucesso - ID: {}", tamanho.getId());
        
        return convertToDTO(tamanho);
    }

    /**
     * Atualiza estoque de um tamanho
     */
    @Transactional
    public TamanhoProdutoDTO atualizarEstoque(Long id, Integer novoEstoque) {
        log.debug("Atualizando estoque do tamanho ID: {} para: {}", id, novoEstoque);
        
        TamanhoProduto tamanho = tamanhoProdutoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tamanho não encontrado com ID: " + id));
        
        if (novoEstoque < 0) {
            throw new RuntimeException("Estoque não pode ser negativo");
        }
        
        tamanho.setEstoque(novoEstoque);
        tamanho.setUpdatedAt(LocalDateTime.now());
        
        tamanho = tamanhoProdutoRepository.save(tamanho);
        log.info("✅ Estoque do tamanho atualizado - ID: {}, Novo estoque: {}", 
                 tamanho.getId(), tamanho.getEstoque());
        
        return convertToDTO(tamanho);
    }

    /**
     * Remove estoque de um tamanho (para vendas)
     */
    @Transactional
    public TamanhoProdutoDTO removerEstoque(Long id, Integer quantidade) {
        log.debug("Removendo {} unidades do estoque do tamanho ID: {}", quantidade, id);
        
        TamanhoProduto tamanho = tamanhoProdutoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tamanho não encontrado com ID: " + id));
        
        if (!tamanho.removerEstoque(quantidade)) {
            throw new RuntimeException("Estoque insuficiente. Disponível: " + tamanho.getEstoque() + 
                                     ", Solicitado: " + quantidade);
        }
        
        tamanho.adicionarVenda(quantidade);
        tamanho = tamanhoProdutoRepository.save(tamanho);
        
        log.info("✅ Estoque removido - Tamanho ID: {}, Quantidade: {}, Estoque restante: {}", 
                 tamanho.getId(), quantidade, tamanho.getEstoque());
        
        return convertToDTO(tamanho);
    }

    /**
     * Adiciona estoque a um tamanho
     */
    @Transactional
    public TamanhoProdutoDTO adicionarEstoque(Long id, Integer quantidade) {
        log.debug("Adicionando {} unidades ao estoque do tamanho ID: {}", quantidade, id);
        
        TamanhoProduto tamanho = tamanhoProdutoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tamanho não encontrado com ID: " + id));
        
        tamanho.adicionarEstoque(quantidade);
        tamanho = tamanhoProdutoRepository.save(tamanho);
        
        log.info("✅ Estoque adicionado - Tamanho ID: {}, Quantidade: {}, Novo estoque: {}", 
                 tamanho.getId(), quantidade, tamanho.getEstoque());
        
        return convertToDTO(tamanho);
    }

    /**
     * Exclui um tamanho (soft delete)
     */
    @Transactional
    public void excluirTamanho(Long id) {
        log.debug("Excluindo tamanho ID: {}", id);
        
        TamanhoProduto tamanho = tamanhoProdutoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tamanho não encontrado com ID: " + id));
        
        tamanho.setAtivo(false);
        tamanho.setUpdatedAt(LocalDateTime.now());
        tamanhoProdutoRepository.save(tamanho);
        
        log.info("✅ Tamanho excluído (soft delete) - ID: {}", id);
    }

    /**
     * Busca tamanhos com estoque baixo para um produto
     */
    public List<TamanhoProdutoDTO> buscarTamanhosEstoqueBaixo(Long produtoId, Integer estoqueMinimo) {
        log.debug("Buscando tamanhos com estoque baixo para produto ID: {}", produtoId);
        
        List<TamanhoProduto> tamanhos = tamanhoProdutoRepository
                .findByProdutoIdAndEstoqueLessThanEqualAndAtivoTrue(produtoId, estoqueMinimo);
        
        return tamanhos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converte TamanhoProduto para TamanhoProdutoDTO
     */
    private TamanhoProdutoDTO convertToDTO(TamanhoProduto tamanho) {
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

    // =================== MÉTODOS PARA MOVIMENTAÇÃO DE ESTOQUE ===================

    /**
     * Lista tamanhos com informações completas de estoque (para tela de movimentação)
     */
    public List<TamanhoProdutoEstoqueDTO> listarTamanhosComEstoque(Long produtoId) {
        log.debug("Listando tamanhos com estoque para produto ID: {}", produtoId);
        
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + produtoId));
        
        return tamanhoProdutoRepository.findByProdutoIdAndAtivoTrue(produtoId)
                .stream()
                .map(tamanho -> convertToEstoqueDTO(tamanho, produto))
                .collect(Collectors.toList());
    }

    /**
     * Adiciona estoque em tamanho existente com histórico
     */
    @Transactional
    public TamanhoProdutoEstoqueDTO adicionarEstoqueComHistorico(Long tamanhoId, Integer quantidade, String observacoes) {
        log.debug("Adicionando {} unidades ao tamanho ID: {}", quantidade, tamanhoId);
        
        TamanhoProduto tamanho = tamanhoProdutoRepository.findById(tamanhoId)
                .orElseThrow(() -> new RuntimeException("Tamanho não encontrado com ID: " + tamanhoId));
        
        tamanho.adicionarEstoque(quantidade);
        tamanho = tamanhoProdutoRepository.save(tamanho);
        
        log.info("✅ Estoque adicionado - Tamanho: {}, Quantidade: {}, Novo estoque: {}", 
                 tamanho.getTamanho(), quantidade, tamanho.getEstoque());
        
        return convertToEstoqueDTO(tamanho, tamanho.getProduto());
    }

    /**
     * Remove estoque de tamanho existente com histórico
     */
    @Transactional
    public TamanhoProdutoEstoqueDTO removerEstoqueComHistorico(Long tamanhoId, Integer quantidade, String observacoes) {
        log.debug("Removendo {} unidades do tamanho ID: {}", quantidade, tamanhoId);
        
        TamanhoProduto tamanho = tamanhoProdutoRepository.findById(tamanhoId)
                .orElseThrow(() -> new RuntimeException("Tamanho não encontrado com ID: " + tamanhoId));
        
        if (!tamanho.removerEstoque(quantidade)) {
            throw new RuntimeException("Estoque insuficiente. Disponível: " + tamanho.getEstoque() + 
                                     ", Solicitado: " + quantidade);
        }
        
        tamanho = tamanhoProdutoRepository.save(tamanho);
        
        log.info("✅ Estoque removido - Tamanho: {}, Quantidade: {}, Estoque restante: {}", 
                 tamanho.getTamanho(), quantidade, tamanho.getEstoque());
        
        return convertToEstoqueDTO(tamanho, tamanho.getProduto());
    }

    /**
     * Cria novo tamanho com estoque inicial (para tela de movimentação)
     */
    @Transactional
    public TamanhoProdutoEstoqueDTO criarTamanhoComEstoque(Long produtoId, CriarTamanhoComEstoqueRequest request) {
        log.debug("Criando novo tamanho com estoque para produto ID: {}", produtoId);
        
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + produtoId));

        boolean tamanhoExiste = tamanhoProdutoRepository
                .existsByProdutoIdAndTamanhoIgnoreCase(produto.getId(), request.getTamanho().trim());
        
        if (tamanhoExiste) {
            throw new RuntimeException("Já existe um tamanho '" + request.getTamanho() + "' para este produto");
        }

        TamanhoProduto novoTamanho = TamanhoProduto.builder()
                .produto(produto)
                .tamanho(request.getTamanho().trim())
                .preco(request.getPreco())
                .codigoBarras(request.getCodigoBarras())
                .estoque(request.getQuantidadeInicial())
                .vendidas(0)
                .ativo(true)
                .build();

        novoTamanho = tamanhoProdutoRepository.save(novoTamanho);
        
        log.info("✅ Novo tamanho criado com estoque - Produto: {}, Tamanho: {}, Estoque inicial: {}", 
                 produto.getNome(), novoTamanho.getTamanho(), novoTamanho.getEstoque());
        
        return convertToEstoqueDTO(novoTamanho, produto);
    }

    /**
     * Busca tamanhos com estoque baixo
     */
    public List<TamanhoProdutoEstoqueDTO> buscarEstoqueBaixo(Integer estoqueMinimo) {
        log.debug("Buscando tamanhos com estoque baixo (< {})", estoqueMinimo);
        
        return tamanhoProdutoRepository.findByEstoqueLessThanAndAtivoTrue(estoqueMinimo)
                .stream()
                .map(tamanho -> convertToEstoqueDTO(tamanho, tamanho.getProduto()))
                .collect(Collectors.toList());
    }

    /**
     * Converte TamanhoProduto para TamanhoProdutoEstoqueDTO (inclui informações de estoque)
     */
    private TamanhoProdutoEstoqueDTO convertToEstoqueDTO(TamanhoProduto tamanho, Produto produto) {
        return TamanhoProdutoEstoqueDTO.builder()
                .id(tamanho.getId())
                .produtoId(tamanho.getProduto().getId())
                .tamanho(tamanho.getTamanho())
                .preco(tamanho.getPreco())
                .codigoBarras(tamanho.getCodigoBarras())
                .ativo(tamanho.getAtivo())
                .estoque(tamanho.getEstoque())
                .vendidas(tamanho.getVendidas())
                .nomeProduto(produto.getNome())
                .categoriaProduto(produto.getDepartamento())
                .build();
    }
}