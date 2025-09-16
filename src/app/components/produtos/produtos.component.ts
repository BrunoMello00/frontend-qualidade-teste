import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProdutoService, Produto } from '../../services/produto.service';
import { TipoCodigoBarras, CodigoBarrasRequest } from '../../models/codigo-barras.model';

// Interfaces para compatibilidade
interface TipoTamanho {
  id: number;
  nome: string;
  descricao?: string;
  categoria?: string;
  tamanhos?: string[];
}

interface TamanhoProduto {
  id?: number;
  tamanho: string;
  preco?: number;
  quantidade?: number;
  estoque?: number;
  codigo?: string;
  vendidas?: number;
}

// Interface para estatísticas de venda por produto
interface ProdutoEstatisticas {
  produtoId: number;
  quantidadeVendida: number;
  ultimaVenda?: Date;
  diasSemVender: number;
}

@Component({
  selector: 'app-produtos',
  templateUrl: './produtos.component.html',
  styleUrls: ['./produtos.component.css']
})
export class ProdutosComponent implements OnInit {
  produtos: Produto[] = [];
  produtoEstatisticas: ProdutoEstatisticas[] = [];
  produtoForm: FormGroup;
  isLoading = false;
  isEditMode = false;
  editingProductId: number | null = null;
  showForm = false;
  errorMessage = '';
  successMessage = '';
  
  // Filtros
  searchTerm = '';
  selectedCategory = '';
  selectedTamanho = ''; // novo filtro por tamanho
  categories: string[] = []; // Agora será carregado dinamicamente do backend
  tamanhosDisponiveis: string[] = []; // tamanhos únicos para filtro
  
  // Sistema de tamanhos
  tiposTamanho: TipoTamanho[] = [];
  showTamanhos = false; // mostrar/ocultar seção de tamanhos no formulário
  tamanhosForm: TamanhoProduto[] = []; // tamanhos do produto sendo editado
  categoriaTamanhoSelecionada: string = ''; // categoria selecionada para tamanhos predefinidos
  tamanhoSelecionadoFiltro: string = ''; // tamanho selecionado para filtro na lista de produtos
  novoTamanho = '';
  novoTamanhoEstoque = 0;
  
  // Sistema de código de produto
  codigoPreview = '';
  
  // Sistema de código de barras
  temCodigoBarras = true; // SEMPRE ATIVO - obrigatório
  codigoBarrasGerado = ''; // Código de barras gerado internamente
  codigoBarrasGeradoAutomaticamente = true; // Flag para identificar se foi gerado automaticamente
  edicaoManualHabilitada = false; // Controla se permite edição manual
  tiposCodigoBarras: TipoCodigoBarras[] = []; // Tipos disponíveis do backend
  tipoCodigoBarrasSelecionado = 'EAN13'; // Tipo padrão
  codigoBarrasManual = ''; // Código inserido manualmente pelo usuário
  
  // Sistema de formatação de preço
  precoNumerico = 0;
  precoCompra = 0;
  precoVenda = 0;
  margemLucro = 0;
  markup = 0;
  
  // Filtros avançados
  orderBy = ''; // Valores: 'mais-vendidos', 'menos-vendidos', 'alfabetico-asc', 'alfabetico-desc', 'mais-tempo-sem-vender', 'menos-tempo-sem-vender'
  showAdvancedFilters = false;
  
  // Paginação
  currentPage = 1;
  itemsPerPage = 10;
  
  constructor(
    private fb: FormBuilder,
    private produtoService: ProdutoService
  ) {
    this.produtoForm = this.createForm();
  }

  ngOnInit(): void {
    this.carregarProdutos();
    this.carregarCategorias();
    this.carregarTiposTamanho();
    this.carregarTamanhosDisponiveis();
    this.carregarTiposCodigoBarras();
    
    // Configurar geração automática de código de barras e cálculos
    this.configurarCalculosAutomaticos();
  }

  configurarCalculosAutomaticos(): void {
    // Escutar mudanças nos campos nome e departamento para código de barras
    this.produtoForm.get('nome')?.valueChanges.subscribe(() => {
      this.gerarCodigoBarrasAutomatico();
    });
    
    this.produtoForm.get('departamento')?.valueChanges.subscribe(() => {
      this.gerarCodigoBarrasAutomatico();
    });

    // Escutar mudanças no tipo de código de barras
    this.produtoForm.get('tipoCodigoBarras')?.valueChanges.subscribe(() => {
      this.onTipoCodigoBarrasChange();
    });

    // Escutar mudanças nos preços para calcular margem e markup
    this.produtoForm.get('precoCompra')?.valueChanges.subscribe(() => {
      this.calcularMargemLucro();
    });

    this.produtoForm.get('precoVenda')?.valueChanges.subscribe(() => {
      this.calcularMargemLucro();
    });
  }

  createForm(): FormGroup {
    return this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      descricao: ['', [Validators.maxLength(500)]],
      precoCompra: ['', [Validators.required, Validators.min(0)]], // Permite zero para doações
      precoVenda: ['', [Validators.required, Validators.min(0.01)]], // OBRIGATÓRIO: Mínimo R$ 0,01 - NÃO PODE SER ZERO
      estoque: ['', [Validators.required, Validators.min(1)]], // OBRIGATÓRIO: Mínimo 1 - NÃO PODE SER ZERO
      departamento: ['', [Validators.required]],
      estoqueMinimo: ['', [Validators.required, Validators.min(1)]], // OBRIGATÓRIO: Mínimo 1
      fornecedor: [''],
      tipoCodigoBarras: [this.tipoCodigoBarrasSelecionado, [Validators.required]],
      codigoBarrasManual: ['']
    });
  }

  carregarProdutos(): void {
    this.isLoading = true;
    // Usar método para carregar apenas produtos ativos
    this.produtoService.getProdutosAtivos().subscribe({
      next: (produtos: Produto[]) => {
        this.produtos = produtos;
        this.carregarEstatisticasProdutos();
        this.isLoading = false;
        console.log('✅ Produtos ativos carregados:', produtos.length, 'produtos');
      },
      error: (error: any) => {
        this.errorMessage = 'Erro ao carregar produtos';
        this.isLoading = false;
        console.error('❌ Erro ao carregar produtos:', error);
      }
    });
  }

  carregarCategorias(): void {
    // Carregar categorias do backend (via ProdutoService que já tem o método)
    this.produtoService.buscarCategorias().subscribe({
      next: (categorias: string[]) => {
        this.categories = categorias;
        console.log('✅ Categorias carregadas:', categorias);
      },
      error: (error) => {
        // Fallback para categorias padrão se houver erro
        this.categories = ['Roupas', 'Acessórios', 'Calçados', 'Eletrônicos', 'Casa', 'Outros'];
        console.warn('⚠️ Erro ao carregar categorias, usando padrão:', error);
      }
    });
  }

  carregarTiposTamanho(): void {
    this.produtoService.getTiposTamanho().subscribe({
      next: (tipos: any[]) => {
        this.tiposTamanho = tipos;
        console.log('✅ Tipos de tamanho carregados:', tipos.length);
      },
      error: (error: any) => {
        console.error('❌ Erro ao carregar tipos de tamanho:', error);
      }
    });
  }

  carregarTamanhosDisponiveis(): void {
    // Extrair todos os tamanhos únicos dos produtos que têm tamanhos
    const tamanhosUnicos = new Set<string>();
    
    this.produtos.forEach(produto => {
      if (produto.temTamanhos && produto.tamanhos) {
        produto.tamanhos.forEach(tamanho => {
          tamanhosUnicos.add(tamanho.tamanho);
        });
      }
    });
    
    this.tamanhosDisponiveis = Array.from(tamanhosUnicos).sort();
    console.log('✅ Tamanhos disponíveis carregados:', this.tamanhosDisponiveis);
  }

  carregarTiposCodigoBarras(): void {
    this.produtoService.listarTiposCodigoBarras().subscribe({
      next: (tipos) => {
        this.tiposCodigoBarras = tipos.map(tipo => ({ 
          codigo: tipo, 
          nome: tipo, 
          descricao: tipo,
          padraoBrasileiro: false,
          comprimento: 13
        }));
        console.log('✅ Tipos de código de barras carregados:', tipos.length);
      },
      error: (error) => {
        // Fallback para tipos padrão se houver erro
        this.tiposCodigoBarras = [
          { codigo: 'EAN13', nome: 'EAN-13', descricao: 'Padrão brasileiro 13 dígitos', padraoBrasileiro: true, comprimento: 13 },
          { codigo: 'EAN8', nome: 'EAN-8', descricao: 'Padrão brasileiro 8 dígitos', padraoBrasileiro: true, comprimento: 8 },
          { codigo: 'UPC_A', nome: 'UPC-A', descricao: 'Padrão americano 12 dígitos', padraoBrasileiro: false, comprimento: 12 },
          { codigo: 'CODE128', nome: 'CODE 128', descricao: 'Padrão alfanumérico', padraoBrasileiro: true, comprimento: 0 }
        ];
        console.warn('⚠️ Erro ao carregar tipos de código de barras, usando padrão:', error);
      }
    });
  }

  carregarEstatisticasProdutos(): void {
    // Carregar estatísticas de vendas dos produtos
    const hoje = new Date();
    
    this.produtoEstatisticas = this.produtos.map(produto => {
      // Simular dados de venda para cada produto
      const vendaAleatoria = Math.random();
      const quantidadeVendida = Math.floor(vendaAleatoria * 100);
      const diasSemVender = Math.floor(vendaAleatoria * 365); // 0 a 365 dias
      
      let ultimaVenda: Date | undefined;
      if (diasSemVender > 0) {
        ultimaVenda = new Date(hoje.getTime() - (diasSemVender * 24 * 60 * 60 * 1000));
      }

      return {
        produtoId: produto.id!,
        quantidadeVendida,
        ultimaVenda,
        diasSemVender
      };
    });
  }

  onSubmit(): void {
    if (this.produtoForm.valid) {
      this.isLoading = true;
      const produto = this.produtoForm.value;

      if (this.isEditMode) {
        // Modo edição - usar serviço normal
        this.produtoService.atualizarProduto(this.editingProductId!, produto).subscribe({
          next: () => {
            this.successMessage = 'Produto atualizado com sucesso!';
            this.resetForm();
            this.carregarProdutos();
            this.isLoading = false;
          },
          error: (error) => {
            this.errorMessage = 'Erro ao atualizar produto';
            this.isLoading = false;
            console.error('Erro:', error);
          }
        });
      } else {
        // Modo criação - gerar código e código de barras automaticamente
        const codigoGerado = this.gerarCodigoCompleto();
        const produtoForm = this.produtoForm.value;
        
        const produtoComQuantidade = {
          ...produtoForm,
          preco: this.precoVenda, // Usar preço de venda como preço principal
          precoCompra: this.precoCompra,
          precoVenda: this.precoVenda,
          margemLucro: this.margemLucro,
          markup: this.markup,
          quantidade: parseInt(produtoForm.estoque) || 1, // Usar estoque informado
          estoque: parseInt(produtoForm.estoque) || 1,
          codigo: codigoGerado, // Código gerado automaticamente
          codigoBarras: this.codigoBarrasGerado, // Código de barras gerado automaticamente
          tipoCodigoBarras: this.tipoCodigoBarrasSelecionado, // Tipo do código de barras
          // Sistema de tamanhos
          temTamanhos: this.showTamanhos && this.tamanhosForm.length > 0,
          tamanhos: this.showTamanhos && this.tamanhosForm.length > 0 ? 
                   this.tamanhosForm.map(t => ({
                     ...t,
                     codigo: this.gerarCodigoTamanho(codigoGerado, t.tamanho)
                   })) : undefined
        };
        
        this.produtoService.criarProdutoComVerificacao(produtoComQuantidade).subscribe({
          next: (produtoSalvo: any) => {
            if (produtoSalvo.ativo && this.produtos.find(p => p.id === produtoSalvo.id)) {
              this.successMessage = 'Produto reativado e atualizado com sucesso!';
            } else {
              this.successMessage = 'Produto cadastrado com sucesso!';
            }
            this.resetForm();
            this.carregarProdutos();
            this.isLoading = false;
          },
          error: (error: any) => {
            if (error.message?.includes('já existe e está ativo')) {
              this.errorMessage = 'Produto com este código já existe. Use a função de edição.';
            } else {
              this.errorMessage = 'Erro ao salvar produto';
            }
            this.isLoading = false;
            console.error('Erro:', error);
          }
        });
      }
    }
  }

  editarProduto(produto: Produto): void {
    this.isEditMode = true;
    this.editingProductId = produto.id!;
    this.showForm = true;
    this.produtoForm.patchValue(produto);
  }

  mostrarModalEditarProduto(produto: Produto): void {
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.innerHTML = `
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header bg-primary text-white">
            <h5 class="modal-title">
              <i class="bi bi-pencil-square me-2"></i>
              Editar Produto: ${produto.nome}
            </h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <form id="formEditarProduto">
              <div class="row g-3">
                <div class="col-12 col-md-6">
                  <label for="nome" class="form-label fw-semibold">Nome do Produto *</label>
                  <input type="text" class="form-control" id="nome" value="${produto.nome}" required>
                </div>
                
                <div class="col-12 col-md-3">
                  <label for="preco" class="form-label fw-semibold">Preço *</label>
                  <div class="input-group">
                    <span class="input-group-text">R$</span>
                    <input type="number" class="form-control" id="preco" value="${produto.preco}" 
                           step="0.01" min="0" required>
                  </div>
                </div>

                <div class="col-12 col-md-3">
                  <label for="categoria" class="form-label fw-semibold">Categoria *</label>
                  <select class="form-select" id="categoria" required>
                    <option value="">Selecione...</option>
                    ${this.categories.map(cat => `
                      <option value="${cat}" ${cat === produto.categoria ? 'selected' : ''}>${cat}</option>
                    `).join('')}
                  </select>
                </div>

                <div class="col-12">
                  <label for="descricao" class="form-label fw-semibold">Descrição</label>
                  <textarea class="form-control" id="descricao" rows="3" 
                            placeholder="Descrição do produto...">${produto.descricao || ''}</textarea>
                </div>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="button" class="btn btn-primary" id="btnSalvarProduto">
              <i class="bi bi-check-circle me-1"></i>Salvar Alterações
            </button>
          </div>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    // Configurar eventos
    const btnSalvar = modal.querySelector('#btnSalvarProduto') as HTMLButtonElement;
    const inputNome = modal.querySelector('#nome') as HTMLInputElement;
    const inputPreco = modal.querySelector('#preco') as HTMLInputElement;
    const selectCategoria = modal.querySelector('#categoria') as HTMLSelectElement;
    const textareaDescricao = modal.querySelector('#descricao') as HTMLTextAreaElement;

    btnSalvar.addEventListener('click', async () => {
      const nome = inputNome.value.trim();
      const preco = parseFloat(inputPreco.value);
      const categoria = selectCategoria.value;
      const descricao = textareaDescricao.value.trim();

      if (!nome || !preco || preco <= 0 || !categoria) {
        alert('Por favor, preencha todos os campos obrigatórios corretamente.');
        return;
      }

      btnSalvar.disabled = true;
      btnSalvar.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Salvando...';

      try {
        // Criar objeto com os campos que serão atualizados para o serviço
        const dadosAtualizacao = {
          nome,
          preco,
          categoria,
          descricao: descricao || produto.descricao,
          quantidade: produto.estoque || 0, // Mapear estoque para quantidade
        };

        const resultado = await this.produtoService.atualizarProduto(produto.id!, dadosAtualizacao as any).toPromise();

        // Atualizar produto na lista
        const index = this.produtos.findIndex(p => p.id === produto.id);
        if (index !== -1) {
          this.produtos[index] = { ...this.produtos[index], nome, preco, categoria, descricao: descricao || produto.descricao };
        }

        this.successMessage = 'Produto atualizado com sucesso!';
        setTimeout(() => this.clearMessages(), 5000);

        // Fechar modal
        modal.remove();
        document.querySelector('.modal-backdrop')?.remove();
        document.body.classList.remove('modal-open');

      } catch (error) {
        console.error('Erro ao atualizar produto:', error);
        this.errorMessage = 'Erro ao atualizar produto.';
        setTimeout(() => this.clearMessages(), 5000);
        
        btnSalvar.disabled = false;
        btnSalvar.innerHTML = '<i class="bi bi-check-circle me-1"></i>Salvar Alterações';
      }
    });

    // Mostrar modal
    const bsModal = new (window as any).bootstrap.Modal(modal);
    bsModal.show();

    // Limpar modal ao fechar
    modal.addEventListener('hidden.bs.modal', () => {
      modal.remove();
    });
  }

  // Método para mostrar informações detalhadas do produto
  mostrarModalVisualizarProduto(produto: Produto): void {
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.innerHTML = `
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header bg-primary text-white">
            <h5 class="modal-title">
              <i class="bi bi-eye me-2"></i>
              Informações do Produto: ${produto.nome}
            </h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <div class="row g-3">
              <div class="col-12">
                <div class="card bg-light">
                  <div class="card-body">
                    <h6 class="card-title text-primary mb-3">
                      <i class="bi bi-info-circle me-2"></i>
                      Informações Básicas
                    </h6>
                    <div class="row">
                      <div class="col-md-6">
                        <strong>Nome:</strong><br>
                        <span class="text-muted">${produto.nome}</span>
                      </div>
                      <div class="col-md-6">
                        <strong>Código:</strong><br>
                        <span class="text-muted font-monospace">${produto.codigo || 'N/A'}</span>
                      </div>
                    </div>
                    <div class="row mt-2">
                      <div class="col-md-6">
                        <strong>Preço:</strong><br>
                        <span class="text-success fw-bold">R$ ${produto.preco?.toFixed(2) || '0,00'}</span>
                      </div>
                      <div class="col-md-6">
                        <strong>Categoria:</strong><br>
                        <span class="badge bg-secondary">${produto.categoria || 'N/A'}</span>
                      </div>
                    </div>
                    ${produto.descricao ? `
                    <div class="row mt-2">
                      <div class="col-12">
                        <strong>Descrição:</strong><br>
                        <span class="text-muted">${produto.descricao}</span>
                      </div>
                    </div>
                    ` : ''}
                  </div>
                </div>
              </div>
              
              <div class="col-12">
                <div class="card bg-light">
                  <div class="card-body">
                    <h6 class="card-title text-primary mb-3">
                      <i class="bi bi-box me-2"></i>
                      Controle de Estoque
                    </h6>
                    <div class="row">
                      <div class="col-md-4">
                        <strong>Estoque Atual:</strong><br>
                        <span class="badge ${(produto.estoque ?? 0) > 10 ? 'bg-success' : (produto.estoque ?? 0) > 0 ? 'bg-warning' : 'bg-danger'} fs-6">
                          ${produto.estoque} unidades
                        </span>
                      </div>
                      <div class="col-md-4">
                        <strong>Estoque Mínimo:</strong><br>
                        <span class="text-muted">${produto.estoqueMinimo || 1} unidades</span>
                      </div>
                      <div class="col-md-4">
                        <strong>Status:</strong><br>
                        <span class="badge ${(produto.estoque ?? 0) > 0 ? 'bg-success' : 'bg-danger'}">
                          ${(produto.estoque ?? 0) > 0 ? 'Disponível' : 'Esgotado'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              ${produto.codigoBarras ? `
              <div class="col-12">
                <div class="card bg-light">
                  <div class="card-body">
                    <h6 class="card-title text-primary mb-3">
                      <i class="bi bi-upc-scan me-2"></i>
                      Código de Barras
                    </h6>
                    <div class="row">
                      <div class="col-md-8">
                        <div class="border p-3 text-center bg-white">
                          <div class="font-monospace h5 text-primary mb-2">${produto.codigoBarras}</div>
                          <small class="text-muted">Código para scanner</small>
                        </div>
                      </div>
                      <div class="col-md-4 d-flex align-items-center">
                        <button type="button" class="btn btn-outline-primary w-100 imprimir-etiqueta-modal">
                          <i class="bi bi-printer me-2"></i>
                          Imprimir Etiqueta
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              ` : ''}

              ${produto.fornecedor ? `
              <div class="col-12">
                <div class="card bg-light">
                  <div class="card-body">
                    <h6 class="card-title text-primary mb-3">
                      <i class="bi bi-building me-2"></i>
                      Fornecedor
                    </h6>
                    <span class="text-muted">${produto.fornecedor}</span>
                  </div>
                </div>
              </div>
              ` : ''}
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Fechar</button>
            <button type="button" class="btn btn-primary imprimir-etiqueta">
              <i class="bi bi-printer me-1"></i>Imprimir Etiqueta
            </button>
            <button type="button" class="btn btn-warning editar-produto">
              <i class="bi bi-pencil me-1"></i>Editar Produto
            </button>
          </div>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    // Adicionar eventos
    const btnImprimir = modal.querySelector('.imprimir-etiqueta') as HTMLButtonElement;
    const btnImprimirModal = modal.querySelector('.imprimir-etiqueta-modal') as HTMLButtonElement;
    const btnEditar = modal.querySelector('.editar-produto') as HTMLButtonElement;

    btnImprimir?.addEventListener('click', () => {
      this.imprimirEtiqueta(produto);
    });

    btnImprimirModal?.addEventListener('click', () => {
      this.imprimirEtiqueta(produto);
    });

    btnEditar?.addEventListener('click', () => {
      modal.remove();
      document.querySelector('.modal-backdrop')?.remove();
      document.body.classList.remove('modal-open');
      setTimeout(() => this.mostrarModalEditarProduto(produto), 100);
    });

    // Mostrar modal
    const bsModal = new (window as any).bootstrap.Modal(modal);
    bsModal.show();

    // Limpar modal ao fechar
    modal.addEventListener('hidden.bs.modal', () => {
      modal.remove();
    });
  }

  desabilitarProduto(id: number, nome: string): void {
    if (confirm(`Tem certeza que deseja desabilitar o produto "${nome}"?\n\nEle será removido da listagem mas mantido no banco de dados.`)) {
      this.isLoading = true;
      this.produtoService.desabilitarProduto(id).subscribe({
        next: (sucesso: boolean) => {
          if (sucesso) {
            this.successMessage = 'Produto desabilitado com sucesso!';
            this.carregarProdutos();
          } else {
            this.errorMessage = 'Erro ao desabilitar produto';
          }
          this.isLoading = false;
        },
        error: (error: any) => {
          this.errorMessage = 'Erro ao desabilitar produto';
          this.isLoading = false;
          console.error('Erro:', error);
        }
      });
    }
  }

  resetForm(): void {
    this.produtoForm.reset();
    this.isEditMode = false;
    this.editingProductId = null;
    this.showForm = false;
    this.errorMessage = '';
    
    // Limpar campos de formatação
    this.precoNumerico = 0;
    this.precoCompra = 0;
    this.precoVenda = 0;
    this.margemLucro = 0;
    this.markup = 0;
    this.codigoPreview = '';
    
    // Limpar sistema de código de barras
    this.codigoBarrasGerado = '';
    this.codigoBarrasGeradoAutomaticamente = true;
    this.edicaoManualHabilitada = false;
    this.tipoCodigoBarrasSelecionado = 'EAN13'; // Resetar para padrão
    this.codigoBarrasManual = '';
    
    // Limpar sistema de tamanhos
    this.showTamanhos = false;
    this.tamanhosForm = [];
    this.categoriaTamanhoSelecionada = '';
    this.novoTamanho = '';
    this.novoTamanhoEstoque = 0;
    
    // Resetar valor do tipo de código de barras no form
    this.produtoForm.patchValue({
      tipoCodigoBarras: this.tipoCodigoBarrasSelecionado
    });
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    if (!this.showForm) {
      this.resetForm();
    }
  }

  getFilteredProducts(): Produto[] {
    let filtered = this.produtos.filter(produto => {
      // Busca por nome e descrição
      const searchLower = this.searchTerm.toLowerCase();
      const matchesSearch = !this.searchTerm || 
                           produto.nome.toLowerCase().includes(searchLower) ||
                           (produto.descricao || '').toLowerCase().includes(searchLower);
      
      // Filtro por categoria única
      const matchesCategory = !this.selectedCategory || 
                             this.selectedCategory === '' || // "Todas as categorias"
                             produto.categoria && produto.categoria.toLowerCase().includes(this.selectedCategory.toLowerCase());
      
      // Filtro por tamanho
      const matchesTamanho = !this.tamanhoSelecionadoFiltro || 
                            this.tamanhoSelecionadoFiltro === '' || // "Todos os tamanhos"
                            (produto.temTamanhos && produto.tamanhos && 
                             produto.tamanhos.some(t => t.tamanho === this.tamanhoSelecionadoFiltro));
      
      return matchesSearch && matchesCategory && matchesTamanho;
    });

    // Aplicar ordenação
    if (this.orderBy) {
      filtered = this.applySorting(filtered);
    }

    return filtered;
  }

  applySorting(produtos: Produto[]): Produto[] {
    return produtos.sort((a, b) => {
      const statsA = this.produtoEstatisticas.find(s => s.produtoId === a.id);
      const statsB = this.produtoEstatisticas.find(s => s.produtoId === b.id);

      switch (this.orderBy) {
        case 'mais-vendidos':
          return (statsB?.quantidadeVendida || 0) - (statsA?.quantidadeVendida || 0);
        
        case 'menos-vendidos':
          return (statsA?.quantidadeVendida || 0) - (statsB?.quantidadeVendida || 0);
        
        case 'alfabetico-asc':
          return a.nome.localeCompare(b.nome);
        
        case 'alfabetico-desc':
          return b.nome.localeCompare(a.nome);
        
        case 'mais-tempo-sem-vender':
          // Ordena do que está há mais tempo sem vender para o menos tempo
          return (statsB?.diasSemVender || 0) - (statsA?.diasSemVender || 0);
        
        case 'menos-tempo-sem-vender':
          // Ordena do que está há menos tempo sem vender para o mais tempo
          return (statsA?.diasSemVender || 0) - (statsB?.diasSemVender || 0);
        
        default:
          return 0;
      }
    });
  }

  // Métodos para controlar filtros avançados
  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  limparFiltrosAvancados(): void {
    this.orderBy = '';
    this.searchTerm = '';
    this.selectedCategory = '';
    this.tamanhoSelecionadoFiltro = '';
    this.currentPage = 1;
  }

  // Métodos auxiliares para estatísticas
  getQuantidadeVendida(produtoId: number): number {
    const stats = this.produtoEstatisticas.find(s => s.produtoId === produtoId);
    return stats?.quantidadeVendida || 0;
  }

  getUltimaVenda(produtoId: number): Date | null {
    const stats = this.produtoEstatisticas.find(s => s.produtoId === produtoId);
    return stats?.ultimaVenda || null;
  }

  getDiasSemVender(produtoId: number): number {
    const stats = this.produtoEstatisticas.find(s => s.produtoId === produtoId);
    return stats?.diasSemVender || 0;
  }

  getPaginatedProducts(): Produto[] {
    const filtered = this.getFilteredProducts();
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return filtered.slice(start, start + this.itemsPerPage);
  }

  getTotalPages(): number {
    return Math.ceil(this.getFilteredProducts().length / this.itemsPerPage);
  }

  changePage(page: number): void {
    this.currentPage = page;
  }

  getStatusBadgeClass(produto: Produto): string {
    if (produto.estoque === 0) return 'bg-danger';
    if ((produto.estoque ?? 0) <= (produto.estoqueMinimo ?? 0)) return 'bg-warning';
    return 'bg-success';
  }

  getStatusText(produto: Produto): string {
    if (produto.estoque === 0) return 'SEM ESTOQUE';
    if ((produto.estoque ?? 0) <= (produto.estoqueMinimo ?? 0)) return 'ESTOQUE BAIXO';
    return 'EM ESTOQUE';
  }

  formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getErrorMessage(field: string): string {
    const control = this.produtoForm.get(field);
    if (control?.errors && control.touched) {
      // Mensagens específicas para cada campo
      if (field === 'precoCompra') {
        if (control.errors['required']) return 'Preço de compra é obrigatório';
        if (control.errors['min']) return 'Preço de compra deve ser maior que R$ 0,00';
        if (control.errors['negative']) return 'Preço de compra não pode ser negativo';
        return 'Preço de compra inválido';
      }
      
      if (field === 'precoVenda') {
        if (control.errors['required']) return 'Preço de venda é obrigatório';
        if (control.errors['min']) return 'Preço de venda deve ser maior que R$ 0,00';
        if (control.errors['invalidPrice']) return 'Preço de venda não pode ser zero ou negativo';
        return 'Preço de venda inválido';
      }
      
      if (field === 'estoque') {
        if (control.errors['required']) return 'Estoque é obrigatório';
        if (control.errors['min']) return 'Estoque deve ter pelo menos 1 unidade';
        if (control.errors['invalidStock']) return 'Estoque não pode ser zero ou negativo';
        return 'Estoque inválido';
      }
      
      if (field === 'estoqueMinimo') {
        if (control.errors['required']) return 'Estoque Mínimo é obrigatório';
        if (control.errors['min']) return 'Estoque Mínimo deve ser maior que 1';
        if (control.errors['invalidStock']) return 'Estoque Mínimo não pode ser zero ou negativo';
        return 'Estoque Mínimo inválido';
      }
      
      // Mensagens genéricas
      if (control.errors['required']) return `${field} é obrigatório`;
      if (control.errors['minlength']) return `${field} deve ter pelo menos ${control.errors['minlength'].requiredLength} caracteres`;
      if (control.errors['maxlength']) return `${field} deve ter no máximo ${control.errors['maxlength'].requiredLength} caracteres`;
      if (control.errors['min']) return `${field} deve ser maior que ${control.errors['min'].min}`;
    }
    return '';
  }

  // Método para calcular classe CSS baseada no tempo sem vender
  getTempoSemVenderClass(produtoId: number): string {
    const diasSemVender = this.getDiasSemVender(produtoId);
    
    if (diasSemVender === 0) return ''; // Nunca vendido
    if (diasSemVender <= 30) return 'tempo-recente'; // Até 1 mês
    if (diasSemVender <= 90) return 'tempo-medio'; // Até 3 meses
    if (diasSemVender <= 180) return 'tempo-alto'; // Até 6 meses
    return 'tempo-critico'; // Mais de 6 meses
  }

  // Método para obter texto descritivo do tempo sem vender
  getTempoSemVenderTexto(produtoId: number): string {
    const diasSemVender = this.getDiasSemVender(produtoId);
    
    if (diasSemVender === 0) return 'Nunca vendido';
    if (diasSemVender === 1) return '1 dia atrás';
    if (diasSemVender < 30) return `${diasSemVender} dias atrás`;
    if (diasSemVender < 365) {
      const meses = Math.floor(diasSemVender / 30);
      return `${meses} ${meses === 1 ? 'mês' : 'meses'} atrás`;
    }
    const anos = Math.floor(diasSemVender / 365);
    return `${anos} ${anos === 1 ? 'ano' : 'anos'} atrás`;
  }
  // Métodos para sistema de código de produto
  gerarPreviewCodigo(): void {
    const departamento = this.produtoForm.get('departamento')?.value;

    if (departamento) {
      const codigoDepartamento = departamento; // já vem formatado (ex: "01")
      const codigoCategoria = "99"; // Categoria padrão para "Diversos"
      const sequencial = this.obterProximoSequencial(departamento, codigoCategoria);
      const ano = new Date().getFullYear().toString().slice(-2); // "25" para 2025
      
      const codigoSemDV = `${codigoDepartamento}${codigoCategoria}${sequencial}${ano}`;
      const digitoVerificador = this.calcularDigitoVerificador(codigoSemDV);
      
      this.codigoPreview = `${codigoDepartamento}${codigoCategoria}-${sequencial}-${ano}-${digitoVerificador}`;
    } else {
      this.codigoPreview = '';
    }
  }

  private obterCodigoCategoria(categoria: string): string {
    // Mapear categorias para códigos de 2 dígitos
    const mapeamentoCategorias: { [key: string]: string } = {
      'alimentação': '01', 'alimentos': '01', 'comida': '01',
      'bebidas': '02', 'drinks': '02',
      'limpeza': '03', 'produtos de limpeza': '03',
      'higiene': '04', 'cuidados pessoais': '04',
      'eletrônicos': '05', 'eletronicos': '05', 'tecnologia': '05',
      'roupas': '06', 'vestuário': '06', 'moda': '06',
      'casa': '07', 'decoração': '07', 'casa & decoração': '07',
      'esportes': '08', 'fitness': '08',
      'livros': '09', 'mídia': '09', 'livros & mídia': '09',
      'outros': '99'
    };

    const categoriaLower = categoria.toLowerCase().trim();
    
    // Buscar correspondência exata
    if (mapeamentoCategorias[categoriaLower]) {
      return mapeamentoCategorias[categoriaLower];
    }

    // Buscar por palavras-chave
    for (const [chave, codigo] of Object.entries(mapeamentoCategorias)) {
      if (categoriaLower.includes(chave) || chave.includes(categoriaLower)) {
        return codigo;
      }
    }

    // Se não encontrar, gerar código baseado no hash da categoria
    const hash = this.simpleHash(categoriaLower);
    const codigo = (hash % 89) + 10; // Gera códigos entre 10-98 (evita 99 que é "outros")
    return codigo.toString().padStart(2, '0');
  }

  private simpleHash(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Converter para 32bit
    }
    return Math.abs(hash);
  }

  private obterProximoSequencial(departamento: string, categoria: string): string {
    // Simular busca no banco de dados do próximo sequencial
    // Na implementação real, isso seria uma consulta ao backend
    const prefixo = `${departamento}${categoria}`;
    
    // Por enquanto, vou simular retornando um número baseado no tempo
    const agora = new Date();
    const sequencial = (agora.getHours() * 100 + agora.getMinutes()) % 9999 + 1;
    
    return sequencial.toString().padStart(4, '0');
  }

  private calcularDigitoVerificador(codigo: string): string {
    // Algoritmo similar ao dígito verificador do CPF
    let soma = 0;
    const pesos = [2, 3, 4, 5, 6, 7, 8, 9];
    
    for (let i = 0; i < codigo.length && i < pesos.length; i++) {
      soma += parseInt(codigo[i]) * pesos[i % pesos.length];
    }
    
    const resto = soma % 11;
    const dv = resto < 2 ? 0 : 11 - resto;
    
    return dv.toString();
  }

  private gerarCodigoCompleto(): string {
    const departamento = this.produtoForm.get('departamento')?.value;

    if (!departamento) {
      throw new Error('Departamento é obrigatório para gerar o código');
    }

    const codigoDepartamento = departamento;
    const codigoCategoria = "99"; // Categoria padrão para "Diversos"
    const sequencial = this.obterProximoSequencial(departamento, codigoCategoria);
    const ano = new Date().getFullYear().toString().slice(-2);
    
    const codigoSemDV = `${codigoDepartamento}${codigoCategoria}${sequencial}${ano}`;
    const digitoVerificador = this.calcularDigitoVerificador(codigoSemDV);
    
    return `${codigoDepartamento}${codigoCategoria}-${sequencial}-${ano}-${digitoVerificador}`;
  }

  // Métodos para formatação de preço
  formatarPreco(event: any): void {
    let input = event.target.value;
    
    // Conversão para cálculos internos
    let valorParaCalculo = input.replace(',', '.');
    const valorNumerico = parseFloat(valorParaCalculo) || 0;
    this.precoNumerico = valorNumerico;
    
    // Atualiza o FormControl
    this.produtoForm.get('preco')?.setValue(valorNumerico, { emitEvent: false });
  }
  
  // Formatar quando sair do campo
  formatarPrecoAoSair(event: any): void {
    const valor = event.target.value;
    if (!valor) return;
    
    // Converte para número
    const valorParaCalculo = valor.replace(',', '.');
    const valorNumerico = parseFloat(valorParaCalculo) || 0;
    
    // Formata no padrão brasileiro para exibição final
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    event.target.value = valorFormatado;
    this.validarPreco();
  }

  // Método para formatar preço de compra com formatação automática
  formatarPrecoCompra(event: any): void {
    let valor = event.target.value;
    
    // Remove tudo que não for número
    const apenasNumeros = valor.replace(/\D/g, '');
    
    if (apenasNumeros === '') {
      this.precoCompra = 0;
      this.produtoForm.get('precoCompra')?.setValue(0, { emitEvent: false });
      this.calcularMargemLucro();
      return;
    }
    
    // Converte para centavos e depois para reais
    const valorEmCentavos = parseInt(apenasNumeros);
    const valorNumerico = valorEmCentavos / 100;
    
    this.precoCompra = valorNumerico;
    
    // Formata para exibição
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    // Atualiza o campo sem trigger de eventos para evitar loop
    setTimeout(() => {
      event.target.value = valorFormatado;
    }, 0);
    
    // Validação
    if (valorNumerico >= 0) {
      event.target.classList.remove('is-invalid');
      this.produtoForm.get('precoCompra')?.setErrors(null);
    } else {
      event.target.classList.add('is-invalid');
      this.produtoForm.get('precoCompra')?.setErrors({ invalidPrice: true });
    }
    
    this.produtoForm.get('precoCompra')?.setValue(valorNumerico, { emitEvent: false });
    this.calcularMargemLucro();
  }

  // Método para formatar preço de venda com formatação automática
  formatarPrecoVenda(event: any): void {
    let valor = event.target.value;
    
    // Remove tudo que não for número
    const apenasNumeros = valor.replace(/\D/g, '');
    
    if (apenasNumeros === '') {
      this.precoVenda = 0;
      this.produtoForm.get('precoVenda')?.setValue(0, { emitEvent: false });
      this.calcularMargemLucro();
      return;
    }
    
    // Converte para centavos e depois para reais
    const valorEmCentavos = parseInt(apenasNumeros);
    const valorNumerico = valorEmCentavos / 100;
    
    this.precoVenda = valorNumerico;
    
    // Formata para exibição
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    // Atualiza o campo sem trigger de eventos para evitar loop
    setTimeout(() => {
      event.target.value = valorFormatado;
    }, 0);
    
    // Validação: preço de venda deve ser maior que 0
    if (valorNumerico > 0) {
      event.target.classList.remove('is-invalid');
      this.produtoForm.get('precoVenda')?.setErrors(null);
    } else {
      event.target.classList.add('is-invalid');
      this.produtoForm.get('precoVenda')?.setErrors({ invalidPrice: true });
    }
    
    this.produtoForm.get('precoVenda')?.setValue(valorNumerico, { emitEvent: false });
    this.calcularMargemLucro();
  }

  // Formatar preço de compra ao sair do campo (onBlur)
  formatarPrecoCompraAoSair(event: any): void {
    const valor = event.target.value;
    if (!valor) return;
    
    // Converte para número
    const valorParaCalculo = valor.replace(',', '.');
    const valorNumerico = parseFloat(valorParaCalculo) || 0;
    
    // Formata no padrão brasileiro para exibição
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    event.target.value = valorFormatado;
    this.precoCompra = valorNumerico;
    this.calcularMargemLucro();
  }

  // Formatar preço de venda ao sair do campo (onBlur)
  formatarPrecoVendaAoSair(event: any): void {
    const valor = event.target.value;
    if (!valor) return;
    
    // Converte para número
    const valorParaCalculo = valor.replace(',', '.');
    const valorNumerico = parseFloat(valorParaCalculo) || 0;
    
    // Formata no padrão brasileiro para exibição
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    event.target.value = valorFormatado;
    this.precoVenda = valorNumerico;
    this.calcularMargemLucro();
  }

  // Método para formatar estoque com validação
  formatarEstoque(event: any): void {
    let valor = event.target.value;
    
    // Remove tudo que não for número
    valor = valor.replace(/\D/g, '');
    
    const valorNumerico = parseInt(valor) || 0;
    
    // Validar que não seja negativo ou zero
    if (valorNumerico <= 0) {
      event.target.classList.add('is-invalid');
      this.produtoForm.get('estoque')?.setErrors({ invalidStock: true });
      return;
    } else {
      event.target.classList.remove('is-invalid');
      this.produtoForm.get('estoque')?.setErrors(null);
    }
    
    event.target.value = valorNumerico.toString();
    this.produtoForm.get('estoque')?.setValue(valorNumerico, { emitEvent: false });
  }

  // Calcular margem de lucro e markup
  // Calcular margem de lucro e markup
  calcularMargemLucro(): void {
    if (this.precoCompra >= 0 && this.precoVenda > 0) {
      const lucro = this.precoVenda - this.precoCompra;
      this.margemLucro = (lucro / this.precoVenda) * 100;
      this.markup = this.precoCompra > 0 ? (lucro / this.precoCompra) * 100 : 0;
    } else {
      this.margemLucro = 0;
      this.markup = 0;
    }
  }

  // Validar estoque - não permite valores negativos ou zero
  validarEstoque(event: any): void {
    let valor = parseInt(event.target.value) || 0;
    
    if (valor <= 0) {
      event.target.classList.add('is-invalid');
      event.target.style.border = '2px solid #dc3545'; // Borda vermelha
      this.produtoForm.get('estoque')?.setErrors({ invalidStock: true });
      
      // Mostra mensagem de erro
      this.errorMessage = '⚠️ Estoque não pode ser zero ou negativo!';
      setTimeout(() => this.errorMessage = '', 3000);
    } else {
      event.target.classList.remove('is-invalid');
      event.target.style.border = '';
      this.produtoForm.get('estoque')?.setErrors(null);
    }
    
    // Força o valor mínimo
    if (valor <= 0) {
      event.target.value = 1;
      this.produtoForm.get('estoque')?.setValue(1, { emitEvent: false });
    }
  }

  // Validar estoque mínimo - não permite valores negativos ou zero
  validarEstoqueMinimo(event: any): void {
    let valor = parseInt(event.target.value) || 0;
    
    if (valor <= 0) {
      event.target.classList.add('is-invalid');
      event.target.style.border = '2px solid #dc3545'; // Borda vermelha
      this.produtoForm.get('estoqueMinimo')?.setErrors({ invalidStock: true });
      
      // Mostra mensagem de erro
      this.errorMessage = '⚠️ Estoque mínimo não pode ser zero ou negativo!';
      setTimeout(() => this.errorMessage = '', 3000);
    } else {
      event.target.classList.remove('is-invalid');
      event.target.style.border = '';
      this.produtoForm.get('estoqueMinimo')?.setErrors(null);
    }
    
    // Força o valor mínimo
    if (valor <= 0) {
      event.target.value = 1;
      this.produtoForm.get('estoqueMinimo')?.setValue(1, { emitEvent: false });
    }
  }

  // Validar preço de compra - não permite valores negativos ou zero
  validarPrecoCompra(event: any): void {
    let valor = parseFloat(event.target.value.replace(/[^\d,]/g, '').replace(',', '.')) || 0;
    
    if (valor <= 0) {
      event.target.classList.add('is-invalid');
      event.target.style.border = '2px solid #dc3545'; // Borda vermelha
      this.produtoForm.get('precoCompra')?.setErrors({ invalidPrice: true });
      
      // Mostra mensagem de erro
      this.errorMessage = '⚠️ Preço de compra não pode ser zero ou negativo!';
      setTimeout(() => this.errorMessage = '', 3000);
    } else {
      event.target.classList.remove('is-invalid');
      event.target.style.border = '';
      this.produtoForm.get('precoCompra')?.setErrors(null);
    }
  }

  // Validar preço de venda - não permite valores negativos ou zero
  validarPrecoVenda(event: any): void {
    let valor = parseFloat(event.target.value.replace(/[^\d,]/g, '').replace(',', '.')) || 0;
    
    if (valor <= 0) {
      event.target.classList.add('is-invalid');
      event.target.style.border = '2px solid #dc3545'; // Borda vermelha
      this.produtoForm.get('precoVenda')?.setErrors({ invalidPrice: true });
      
      // Mostra mensagem de erro
      this.errorMessage = '⚠️ Preço de venda não pode ser zero ou negativo!';
      setTimeout(() => this.errorMessage = '', 3000);
    } else {
      event.target.classList.remove('is-invalid');
      event.target.style.border = '';
      this.produtoForm.get('precoVenda')?.setErrors(null);
    }
  }

  validarPreco(): void {
    const precoControl = this.produtoForm.get('preco');
    if (precoControl && this.precoNumerico <= 0) {
      precoControl.setErrors({ min: true });
    }
  }

  private extrairValorNumerico(valorFormatado: string): number {
    if (!valorFormatado) return 0;
    
    // Remover pontos de milhar e substituir vírgula por ponto
    const valorLimpo = valorFormatado
      .replace(/\./g, '')
      .replace(',', '.');
    
    const numero = parseFloat(valorLimpo);
    return isNaN(numero) ? 0 : numero;
  }

  // Geração automática de código de barras usando código do produto
  gerarCodigoBarrasAutomatico(): void {
    const tipoCodigoBarras = this.produtoForm.get('tipoCodigoBarras')?.value || this.tipoCodigoBarrasSelecionado;
    const departamento = this.produtoForm.get('departamento')?.value;
    
    if (!tipoCodigoBarras) {
      console.log('⚠️ Tipo de código de barras não selecionado');
      return; // Não gera se não tem tipo selecionado
    }

    if (!departamento) {
      console.log('⚠️ Departamento não selecionado - necessário para geração automática');
      return; // Não gera se não tem departamento (necessário para prefixo)
    }

    // Gerar prefixo baseado no departamento selecionado
    // Para códigos EAN-13: usar código do departamento como prefixo
    let prefixo = '';
    if (tipoCodigoBarras === 'EAN13' || tipoCodigoBarras === 'EAN8') {
      // Para EAN, usar o código do departamento como prefixo (ex: "01", "02")
      prefixo = departamento;
    } else {
      // Para outros tipos, usar um prefixo mais genérico
      prefixo = `D${departamento}`;
    }

    // Chama o backend para gerar código de barras
    const request: CodigoBarrasRequest = {
      tipoCodigoBarras: tipoCodigoBarras,
      prefixo: prefixo,
      gerarAutomaticamente: true
    };

    this.produtoService.gerarCodigoBarras(request).subscribe({
      next: (response) => {
        this.codigoBarrasGerado = response.codigo;
        this.codigoBarrasGeradoAutomaticamente = true;
        console.log('✅ Código de barras gerado:', response.codigo, 'Tipo:', response.tipo, 'Prefixo:', response.prefixo);
      },
      error: (error) => {
        console.warn('⚠️ Erro ao gerar código de barras, usando fallback:', error);
        // Fallback: gerar código simples baseado no timestamp e departamento
        const timestamp = Date.now().toString().slice(-8);
        const fallbackCodigo = prefixo + timestamp;
        this.codigoBarrasGerado = fallbackCodigo;
        this.codigoBarrasGeradoAutomaticamente = true;
      }
    });
  }

  // ===== MÉTODOS PARA SISTEMA DE CÓDIGO DE BARRAS =====

  onTipoCodigoBarrasChange(): void {
    const novoTipo = this.produtoForm.get('tipoCodigoBarras')?.value;
    this.tipoCodigoBarrasSelecionado = novoTipo;
    
    // Regenerar código automaticamente se estava em modo automático
    if (this.codigoBarrasGeradoAutomaticamente) {
      this.gerarCodigoBarrasAutomatico();
    }
  }

  alternarModoCodigoBarras(): void {
    this.edicaoManualHabilitada = !this.edicaoManualHabilitada;
    
    if (this.edicaoManualHabilitada) {
      // Modo manual: limpar código gerado automaticamente
      this.codigoBarrasGeradoAutomaticamente = false;
      this.codigoBarrasManual = this.codigoBarrasGerado; // Copiar para edição
    } else {
      // Modo automático: gerar novo código
      this.codigoBarrasGeradoAutomaticamente = true;
      this.codigoBarrasManual = '';
      this.gerarCodigoBarrasAutomatico();
    }
  }

  onCodigoBarrasManualChange(event: any): void {
    const codigo = event.target.value;
    this.codigoBarrasManual = codigo;
    
    if (codigo && codigo.length > 0) {
      this.codigoBarrasGerado = codigo;
      this.codigoBarrasGeradoAutomaticamente = false;
    }
  }

  validarCodigoBarrasManual(): void {
    const codigo = this.codigoBarrasManual;
    const tipo = this.tipoCodigoBarrasSelecionado;
    
    if (codigo && tipo) {
      this.produtoService.validarCodigoBarras(codigo, tipo).subscribe({
        next: (response) => {
          if (response.valido) {
            this.successMessage = `Código de barras ${codigo} é válido para ${tipo}`;
            this.codigoBarrasGerado = codigo;
          } else {
            this.errorMessage = `Código de barras ${codigo} é inválido para ${tipo}`;
          }
          setTimeout(() => this.clearMessages(), 3000);
        },
        error: (error) => {
          this.errorMessage = 'Erro ao validar código de barras';
          console.error('Erro na validação:', error);
          setTimeout(() => this.clearMessages(), 3000);
        }
      });
    }
  }

  getCodigoBarrasDisplay(): string {
    if (this.edicaoManualHabilitada) {
      return this.codigoBarrasManual || '';
    }
    return this.codigoBarrasGerado || '';
  }

  // Métodos auxiliares para o template
  getTipoCodigoBarrasSelecionadoInfo(): TipoCodigoBarras | undefined {
    return this.tiposCodigoBarras.find(t => t.codigo === this.tipoCodigoBarrasSelecionado);
  }

  getTipoNome(): string {
    const tipo = this.getTipoCodigoBarrasSelecionadoInfo();
    return tipo?.nome || this.tipoCodigoBarrasSelecionado;
  }

  getTipoDescricao(): string {
    const tipo = this.getTipoCodigoBarrasSelecionadoInfo();
    return tipo?.descricao || 'Não disponível';
  }

  getTipoPadraoBrasileiro(): boolean {
    const tipo = this.getTipoCodigoBarrasSelecionadoInfo();
    return tipo?.padraoBrasileiro || false;
  }

  getTipoComprimento(): string {
    const tipo = this.getTipoCodigoBarrasSelecionadoInfo();
    const comprimento = tipo?.comprimento || 0;
    return comprimento > 0 ? comprimento + ' dígitos' : 'Variável';
  }

  // ===== MÉTODOS PARA SISTEMA DE TAMANHOS =====

  toggleTamanhos(): void {
    // showTamanhos já é atualizado automaticamente pelo ngModel
    
    if (this.showTamanhos && this.tamanhosForm.length === 0) {
      // Pequeno delay para permitir que a animação comece antes de adicionar conteúdo
      setTimeout(() => {
        this.adicionarTamanhoForm();
      }, 100);
    }
    
    // Scroll suave para a seção quando expandir
    if (this.showTamanhos) {
      setTimeout(() => {
        const elemento = document.querySelector('.tamanhos-section');
        if (elemento) {
          elemento.scrollIntoView({ 
            behavior: 'smooth', 
            block: 'nearest' 
          });
        }
      }, 300); // Aumentei o delay para dar tempo da animação
    }
  }

  adicionarTamanhoForm(): void {
    const novoId = this.tamanhosForm.length > 0 ? 
      Math.max(...this.tamanhosForm.map(t => t.id!)) + 1 : 1;
    
    this.tamanhosForm.push({
      id: novoId,
      tamanho: '',
      estoque: 0,
      codigo: '',
      vendidas: 0
    });
  }

  removerTamanhoForm(index: number): void {
    this.tamanhosForm.splice(index, 1);
  }

  getTamanhosPorCategoria(categoria: string): string[] {
    const tipo = this.tiposTamanho.find(t => t.categoria === categoria);
    return tipo ? (tipo.tamanhos || []) : [];
  }

  adicionarTamanhoCustomizado(): void {
    if (this.novoTamanho.trim()) {
      const tamanhoExiste = this.tamanhosForm.some(t => 
        t.tamanho.toLowerCase() === this.novoTamanho.toLowerCase()
      );
      
      if (!tamanhoExiste) {
        this.adicionarTamanhoForm();
        const ultimoTamanho = this.tamanhosForm[this.tamanhosForm.length - 1];
        ultimoTamanho.tamanho = this.novoTamanho.trim();
        ultimoTamanho.estoque = this.novoTamanhoEstoque;
        
        // Adicionar aos tamanhos disponíveis se não existe
        if (!this.tamanhosDisponiveis.includes(this.novoTamanho.trim())) {
          this.tamanhosDisponiveis.push(this.novoTamanho.trim());
          this.tamanhosDisponiveis.sort();
        }
        
        this.novoTamanho = '';
        this.novoTamanhoEstoque = 0;
      } else {
        alert('Este tamanho já foi adicionado!');
      }
    }
  }

  calcularEstoqueTotal(): number {
    return this.tamanhosForm.reduce((total, tamanho) => total + (tamanho.estoque || 0), 0);
  }

  gerarCodigoTamanho(codigoBase: string, tamanho: string): string {
    return `${codigoBase}-${tamanho}`;
  }

  // Método para obter estatísticas de um produto por tamanho
  getEstatisticasTamanho(produtoId: number): void {
    this.produtoService.getEstatisticasPorTamanho(produtoId).subscribe({
      next: (estatisticas: any) => {
        console.log('📊 Estatísticas por tamanho:', estatisticas);
        // Aqui você pode processar e exibir as estatísticas em um modal ou seção específica
      },
      error: (error: any) => {
        console.error('❌ Erro ao carregar estatísticas por tamanho:', error);
      }
    });
  }

  // Método para visualizar código de barras em modal
  visualizarCodigoBarras(produto: Produto): void {
    const codigoBarras = produto.codigoBarras || produto.codigo?.replace(/-/g, '') || 'SEM-CODIGO';
    
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.innerHTML = `
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header bg-primary text-white">
            <h5 class="modal-title">
              <i class="bi bi-upc-scan me-2"></i>
              Código de Barras: ${produto.nome}
            </h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body text-center">
            <div class="border p-4 bg-light">
              <div class="font-monospace h3 text-primary mb-3">${codigoBarras}</div>
              <div class="small text-muted mb-3">Código para scanner USB</div>
              <div style="font-family: 'Courier New', monospace; font-size: 24px; letter-spacing: 2px; background: white; padding: 10px; border: 2px solid #000;">
                ||||| ${codigoBarras} |||||
              </div>
            </div>
            <div class="mt-3">
              <strong>Produto:</strong> ${produto.nome}<br>
              <strong>Preço:</strong> R$ ${produto.preco?.toFixed(2) || '0,00'}
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Fechar</button>
            <button type="button" class="btn btn-primary imprimir-codigo">
              <i class="bi bi-printer me-1"></i>Imprimir Etiqueta
            </button>
          </div>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    // Adicionar evento de impressão
    const btnImprimir = modal.querySelector('.imprimir-codigo') as HTMLButtonElement;
    btnImprimir?.addEventListener('click', () => {
      this.imprimirEtiqueta(produto);
    });

    // Mostrar modal
    const bsModal = new (window as any).bootstrap.Modal(modal);
    bsModal.show();

    // Limpar modal ao fechar
    modal.addEventListener('hidden.bs.modal', () => {
      modal.remove();
    });
  }

  // Método para imprimir etiqueta de código de barras
  imprimirEtiqueta(produto: any): void {
    const conteudoEtiqueta = this.gerarConteudoEtiqueta(produto);
    
    const janelaImpressao = window.open('', '_blank', 'width=400,height=300');
    if (janelaImpressao) {
      janelaImpressao.document.write(conteudoEtiqueta);
      janelaImpressao.document.close();
      janelaImpressao.print();
    }
  }

  // Gera o conteúdo HTML da etiqueta
  private gerarConteudoEtiqueta(produto: any): string {
    const codigoBarras = produto.codigoBarras || produto.codigo?.replace(/-/g, '') || 'SEM-CODIGO';
    
    return `
      <!DOCTYPE html>
      <html>
      <head>
        <title>Etiqueta - ${produto.nome}</title>
        <style>
          body { 
            font-family: Arial, sans-serif; 
            margin: 0; 
            padding: 20px;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
          }
          .etiqueta {
            border: 2px solid #000;
            padding: 15px;
            text-align: center;
            background: white;
            max-width: 300px;
          }
          .nome-produto {
            font-weight: bold;
            font-size: 14px;
            margin-bottom: 10px;
          }
          .codigo-barras {
            font-family: monospace;
            font-size: 18px;
            letter-spacing: 2px;
            margin: 10px 0;
            padding: 5px;
            border: 1px solid #ccc;
            background: #f9f9f9;
          }
          .preco {
            font-size: 16px;
            font-weight: bold;
            color: #0066cc;
            margin-top: 10px;
          }
          .info-adicional {
            font-size: 10px;
            color: #666;
            margin-top: 10px;
          }
        </style>
      </head>
      <body>
        <div class="etiqueta">
          <div class="nome-produto">${produto.nome}</div>
          <div class="codigo-barras">${codigoBarras}</div>
          <div class="preco">R$ ${produto.preco?.toFixed(2) || produto.precoVenda?.toFixed(2) || '0,00'}</div>
          <div class="info-adicional">
            ${produto.departamento || produto.categoria || ''}<br>
            Gerado em: ${new Date().toLocaleDateString('pt-BR')}
          </div>
        </div>
      </body>
      </html>
    `;
  }

  // Método para imprimir código de barras rapidamente
  imprimirCodigoBarras(produto: Produto): void {
    const codigoBarras = produto.codigoBarras || produto.codigo?.replace(/-/g, '') || 'SEM-CODIGO';
    
    // Criar janela de impressão com código de barras
    const printWindow = window.open('', '_blank', 'width=400,height=200');
    if (printWindow) {
      printWindow.document.write(`
        <html>
          <head>
            <title>Código de Barras - ${produto.nome}</title>
            <style>
              body { 
                font-family: Arial, sans-serif; 
                text-align: center; 
                padding: 20px; 
                margin: 0;
              }
              .codigo { 
                font-size: 24px; 
                font-weight: bold; 
                font-family: monospace; 
                letter-spacing: 3px;
                border: 2px solid #000;
                padding: 10px;
                margin: 20px 0;
                background: white;
              }
              .produto { 
                font-size: 16px; 
                margin-bottom: 10px; 
              }
              @media print {
                body { padding: 10px; }
              }
            </style>
          </head>
          <body>
            <div class="produto">Produto: <strong>${produto.nome}</strong></div>
            <div class="codigo">||||| ${codigoBarras} |||||</div>
            <div>Departamento: ${produto.categoria}</div>
            <div>Preço: R$ ${produto.preco?.toFixed(2) || '0,00'}</div>
          </body>
        </html>
      `);
      printWindow.document.close();
      printWindow.focus();
      setTimeout(() => {
        printWindow.print();
        printWindow.close();
      }, 250);
    }
  }

}
