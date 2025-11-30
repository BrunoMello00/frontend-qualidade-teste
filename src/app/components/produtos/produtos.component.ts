import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProdutoService, Produto, TipoCodigoBarras, CodigoBarrasRequest } from '../../services/produto.service';
import { AuthService } from '../../services/auth.service';

interface TipoCodigoBarrasInfo {
  codigo: string;
  nome: string;
  descricao: string;
  padraoBrasileiro: boolean;
  comprimento: number;
}

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
  codigo?: string;
}

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
  
  searchTerm = '';
  selectedCategory = '';
  selectedTamanho = ''; // novo filtro por tamanho
  categories: string[] = []; // Agora será carregado dinamicamente do backend
  tamanhosDisponiveis: string[] = []; // tamanhos únicos para filtro
  
  tiposTamanho: TipoTamanho[] = [];
  showTamanhos = false; // mostrar/ocultar seção de tamanhos no formulário
  tamanhosForm: TamanhoProduto[] = []; // tamanhos do produto sendo editado
  categoriaTamanhoSelecionada: string = ''; // categoria selecionada para tamanhos predefinidos
  tamanhoSelecionadoFiltro: string = ''; // tamanho selecionado para filtro na lista de produtos
  novoTamanho = '';
  
  codigoPreview = '';
  
  temCodigoBarras = true; // SEMPRE ATIVO - obrigatório
  codigoBarrasGerado = ''; // Código de barras gerado internamente
  codigoBarrasGeradoAutomaticamente = true; // Flag para identificar se foi gerado automaticamente
  edicaoManualHabilitada = false; // Controla se permite edição manual
  tiposCodigoBarras: TipoCodigoBarrasInfo[] = []; // Tipos disponíveis do backend
  tipoCodigoBarrasSelecionado = 'EAN13'; // Tipo padrão
  codigoBarrasManual = ''; // Código inserido manualmente pelo usuário
  
  pausarObservadores = false; // Flag para pausar observadores durante preenchimento
  
  precoNumerico = 0;
  precoCompra = 0;
  precoVenda = 0;
  margemLucro = 0;
  markup = 0;
  
  orderBy = ''; // Valores: 'mais-vendidos', 'menos-vendidos', 'alfabetico-asc', 'alfabetico-desc', 'mais-tempo-sem-vender', 'menos-tempo-sem-vender'
  showAdvancedFilters = false;
  
  // 🆕 Controle de produtos desabilitados
  showDisabledProducts = false; // Toggle para mostrar produtos desabilitados
  produtosDesabilitados: Produto[] = [];
  currentPageDesabilitados = 1;
  
  currentPage = 1;
  itemsPerPage = 10;
  
  mostrarValidacao = false;
  camposFaltantes: string[] = [];
  
  // Mapeamento de códigos de departamento para nomes
  departamentos: { [key: string]: string } = {
    '01': 'Alimentação',
    '02': 'Bebidas', 
    '03': 'Limpeza',
    '04': 'Higiene',
    '05': 'Eletrônicos',
    '06': 'Roupas',
    '07': 'Casa & Decoração',
    '08': 'Esportes',
    '09': 'Livros & Papelaria',
    '10': 'Outros'
  };
  
  constructor(
    private fb: FormBuilder,
    private produtoService: ProdutoService,
    public authService: AuthService
  ) {
    this.produtoForm = this.createForm();
  }

  ngOnInit(): void {
    this.carregarProdutos();
    this.carregarCategorias();
    this.carregarTiposTamanho();
    this.carregarTamanhosDisponiveis();
    this.carregarTiposCodigoBarras();
    
    this.configurarCalculosAutomaticos();
    
    this.configurarMonitoramentoValidacao();
  }

  configurarMonitoramentoValidacao(): void {
    this.produtoForm.valueChanges.subscribe(() => {
      this.verificarCamposFaltantes();
    });
    
    this.verificarCamposFaltantes();
  }

  verificarCamposFaltantes(): void {
    this.camposFaltantes = [];
    
    const campos = {
      'nome': 'Nome do Produto',
      'precoCompra': 'Preço de Compra',
      'precoVenda': 'Preço de Venda',
      'estoqueMinimo': 'Estoque Mínimo',
      'departamento': 'Departamento',
      'tipoCodigoBarras': 'Tipo de Código de Barras'
    };

    for (const [campo, nomeCampo] of Object.entries(campos)) {
      const control = this.produtoForm.get(campo);
      if (control && control.invalid && (control.dirty || control.touched || this.mostrarValidacao)) {
        if (control.errors?.['required']) {
          this.camposFaltantes.push(`${nomeCampo} é obrigatório`);
        } else if (control.errors?.['min']) {
          this.camposFaltantes.push(`${nomeCampo} deve ser maior que ${control.errors['min'].min}`);
        } else if (control.errors?.['minlength']) {
          this.camposFaltantes.push(`${nomeCampo} deve ter pelo menos ${control.errors['minlength'].requiredLength} caracteres`);
        } else if (control.errors?.['maxlength']) {
          this.camposFaltantes.push(`${nomeCampo} não pode ter mais que ${control.errors['maxlength'].requiredLength} caracteres`);
        }
      }
    }

  }

  marcarCamposComoTocados(): void {
    this.mostrarValidacao = true;
    Object.keys(this.produtoForm.controls).forEach(campo => {
      this.produtoForm.get(campo)?.markAsTouched();
    });
    this.verificarCamposFaltantes();
  }

  get formularioValido(): boolean {
    return this.produtoForm.valid && this.getCamposFaltantes().length === 0;
  }

  getButtonTooltip(): string {
    if (this.isLoading) {
      return 'Salvando produto...';
    }
    
    const camposFaltantes = this.getCamposFaltantes();
    if (camposFaltantes.length > 0) {
      return `Complete os seguintes campos: ${camposFaltantes.join(', ')}`;
    }
    
    if (!this.produtoForm.valid) {
      return 'Preencha todos os campos obrigatórios para continuar';
    }
    
    return this.isEditMode ? 'Clique para atualizar o produto' : 'Clique para cadastrar o produto';
  }

  shouldShowFormStatus(): boolean {
    const temErrosBasicos = !this.produtoForm.valid && (this.mostrarValidacao || this.temAlgumCampoTocado());
    
    return temErrosBasicos;
  }

  shouldShowEstoqueError(): boolean {
    return false;
  }

  temAlgumCampoTocado(): boolean {
    return Object.keys(this.produtoForm.controls).some(campo => 
      this.produtoForm.get(campo)?.touched
    );
  }

  getFormStatusBadgeClass(): string {
    if (this.formularioValido) {
      return 'bg-success';
    } else {
      return 'bg-warning';
    }
  }

  getFormStatusText(): string {
    if (this.formularioValido) {
      return 'Pronto para enviar';
    } else {
      return 'Aguardando preenchimento';
    }
  }

  getFormStatusDetails(): string {
    if (this.formularioValido) {
      return 'Todos os campos estão corretos';
    }
    
    const errosBasicos = this.camposFaltantes.filter(erro => 
      !erro.includes('estoque inicial') && !erro.includes('soma dos estoques')
    ).length;
    
    let detalhes = '';
    
    if (errosBasicos > 0) {
      detalhes += `${errosBasicos} campo${errosBasicos !== 1 ? 's' : ''} obrigatório${errosBasicos !== 1 ? 's' : ''} pendente${errosBasicos !== 1 ? 's' : ''}`;
    }
    
    return detalhes || 'Verificando campos...';
  }

  get temErros(): boolean {
    return this.camposFaltantes.length > 0;
  }

  configurarCalculosAutomaticos(): void {
    this.produtoForm.get('nome')?.valueChanges.subscribe(() => {
      if (!this.pausarObservadores && !this.isEditMode && this.codigoBarrasGeradoAutomaticamente) {
        this.gerarCodigoBarrasAutomatico();
      }
    });
    
    this.produtoForm.get('departamento')?.valueChanges.subscribe(() => {
      if (!this.pausarObservadores && !this.isEditMode && this.codigoBarrasGeradoAutomaticamente) {
        this.gerarCodigoBarrasAutomatico();
      }
      if (!this.pausarObservadores) {
        this.verificarDepartamentoRoupas();
      }
    });

    this.produtoForm.get('tipoCodigoBarras')?.valueChanges.subscribe(() => {
      if (!this.pausarObservadores) {
        this.onTipoCodigoBarrasChange();
      }
    });

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
      departamento: ['', [Validators.required]],
      estoqueMinimo: ['', [Validators.required, Validators.min(1)]], // OBRIGATÓRIO: Mínimo 1
      fornecedor: [''],
      tipoCodigoBarras: [this.tipoCodigoBarrasSelecionado, [Validators.required]],
      codigoBarrasManual: [''],
      // 🆕 NOVOS CAMPOS ADICIONADOS
      prefixoCodigo: ['PROD', [Validators.maxLength(10)]], // Prefixo personalizável
      margem: ['', [Validators.min(0), Validators.max(999.99)]] // Margem de lucro (%)
    });
  }

  debugProduto(produto: any): boolean {
    console.log('🐛 Debug produto:', produto.nome);
    console.log('🐛 temTamanhos:', produto.temTamanhos);
    console.log('🐛 tamanhos array:', produto.tamanhos);
    console.log('🐛 tamanhos length:', produto.tamanhos?.length);
    console.log('🐛 Condição final:', produto.tamanhos && produto.tamanhos.length > 0);
    return produto.tamanhos && produto.tamanhos.length > 0;
  }

  private processarProdutos(produtos: Produto[]): Produto[] {
    return produtos.map(produto => {
      if (!produto.tamanhos) {
        produto.tamanhos = [];
      }
      
      produto.temTamanhos = produto.tamanhos && produto.tamanhos.length > 0;
      
      console.log(`🔧 Produto processado: ${produto.nome} - tamanhos: ${produto.tamanhos?.length || 0}, temTamanhos: ${produto.temTamanhos}`);
      
      return produto;
    });
  }

  carregarProdutos(): void {
    this.isLoading = true;
    console.log('🔄 Iniciando carregamento de produtos...');
    this.produtoService.getProdutosAtivos().subscribe({
      next: (produtos: Produto[]) => {
        console.log('🔍 Dados brutos do backend:', produtos);
        console.log('🔍 Total de produtos recebidos:', produtos.length);
        console.log('🔍 Primeiro produto completo:', produtos[0]);
        console.log('🔍 IDs dos produtos:', produtos.map(p => ({ id: p.id, nome: p.nome })));
        
        this.produtos = this.processarProdutos(produtos);
        
        console.log('🔍 Produtos com tamanhos:', this.produtos.filter(p => p.temTamanhos));
        console.log('✅ Produtos carregados com sucesso!', this.produtos.length, 'produtos');
        
        this.carregarEstatisticasProdutos();
        this.isLoading = false;
      },
      error: (error: any) => {
        this.errorMessage = 'Erro ao carregar produtos';
        this.isLoading = false;
        console.error('❌ Erro ao carregar produtos:', error);
      }
    });
  }

  carregarCategorias(): void {
    this.produtoService.buscarCategorias().subscribe({
      next: (categorias: string[]) => {
        this.categories = categorias;
      },
      error: (error) => {
        this.categories = ['Roupas', 'Acessórios', 'Calçados', 'Eletrônicos', 'Casa', 'Outros'];
        console.warn('⚠️ Erro ao carregar categorias, usando padrão:', error);
      }
    });
  }

  carregarTiposTamanho(): void {
    this.tiposTamanho = [
      { id: 1, nome: 'Roupas Gerais', categoria: 'roupas-gerais', tamanhos: ['PP', 'P', 'M', 'G', 'GG', 'XG'] },
      { id: 2, nome: 'Calças Numéricas', categoria: 'calcas-numericas', tamanhos: ['36', '38', '40', '42', '44', '46', '48'] },
      { id: 3, nome: 'Calçados', categoria: 'calcados', tamanhos: ['33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44'] }
    ];
  }

  carregarTamanhosDisponiveis(): void {
    const tamanhosUnicos = new Set<string>();
    
    this.produtos.forEach(produto => {
      if (produto.temTamanhos && produto.tamanhos) {
        produto.tamanhos.forEach(tamanho => {
          tamanhosUnicos.add(tamanho.tamanho);
        });
      }
    });
    
    this.tamanhosDisponiveis = Array.from(tamanhosUnicos).sort();
  }

  carregarTiposCodigoBarras(): void {
    this.produtoService.listarTiposCodigoSuportados().subscribe({
      next: (tipos: any[]) => {
        this.tiposCodigoBarras = tipos.map(tipo => ({ 
          codigo: tipo.codigo, // Use o campo codigo do enum
          nome: tipo.nome, // Use o campo nome do enum
          descricao: tipo.descricao,
          padraoBrasileiro: tipo.padraoBrasileiro,
          comprimento: tipo.tamanhoMaximo
        }));
      },
      error: (error: any) => {
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
    this.produtoEstatisticas = this.produtos.map(produto => {
      return {
        produtoId: produto.id!,
        quantidadeVendida: 0,  // Produtos novos não têm vendas
        ultimaVenda: undefined,  // Sem data de última venda
        diasSemVender: 0  // Zero dias sem vender (produto novo)
      };
    });
    
    // para buscar as estatísticas reais de vendas de cada produto
  }

  onSubmit(): void {
    this.marcarCamposComoTocados();
    
    if (this.produtoForm.valid) {
      this.isLoading = true;
      const produtoForm = this.produtoForm.value;

      if (this.isEditMode) {
        const produtoAtualizado = {
          nome: produtoForm.nome,
          descricao: produtoForm.descricao || '',
          preco: parseFloat(produtoForm.precoVenda) || 0,
          departamento: produtoForm.departamento || '',
          estoqueMinimo: parseInt(produtoForm.estoqueMinimo) || 1,
          fornecedor: produtoForm.fornecedor || '',
          custoUnitario: parseFloat(produtoForm.precoCompra) || 0, // Agora mapeado corretamente!
          codigoBarras: produtoForm.codigoBarrasManual || '',
          tipoCodigoBarras: produtoForm.tipoCodigoBarras || 'EAN13', // 🔧 CORRIGIDO: sem hífen
          // 🆕 NOVOS CAMPOS ADICIONADOS NA EDIÇÃO:
          prefixoCodigo: produtoForm.prefixoCodigo || 'PROD', // 📋 Prefixo do Código
          margem: parseFloat(produtoForm.margem) || undefined, // 📊 Margem de Lucro (%)
          tamanhos: this.showTamanhos && this.tamanhosForm.length > 0 ? 
            this.tamanhosForm.map(t => ({
              id: t.id,
              tamanho: t.tamanho,
              preco: t.preco || parseFloat(produtoForm.precoVenda) || 0,
              codigo: t.codigo
            })) : []
        };

        console.log('� DEBUG EDIÇÃO - Gestão de Tamanhos:');
        console.log('🔍 showTamanhos:', this.showTamanhos);
        console.log('🔍 tamanhosForm.length:', this.tamanhosForm.length);
        console.log('🔍 tamanhosForm completo:', this.tamanhosForm);
        console.log('🔍 tamanhos enviados:', produtoAtualizado.tamanhos);
        
        console.log('�📦 Dados mapeados para atualização:', produtoAtualizado);
        console.log('💰 Custo unitário enviado:', produtoAtualizado.custoUnitario);
        console.log('🔖 Tipo de código de barras enviado:', produtoAtualizado.tipoCodigoBarras);

        this.produtoService.atualizarProduto(this.editingProductId!, produtoAtualizado).subscribe({
          next: (response) => {
            console.log('🔖 Tipo de código retornado:', response?.tipoCodigoBarras);
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
        const codigoGerado = this.gerarCodigoCompleto();
        const produtoForm = this.produtoForm.value;
        
        console.log('🔍 DEBUG - Gestão de Tamanhos:');
        console.log('🔍 showTamanhos:', this.showTamanhos);
        console.log('🔍 tamanhosForm.length:', this.tamanhosForm.length);
        console.log('🔍 tamanhosForm completo:', this.tamanhosForm);
        
        const tamanhosParaEnvio = this.showTamanhos && this.tamanhosForm.length > 0 ? 
          this.tamanhosForm.map(t => ({
            tamanho: t.tamanho,
            preco: t.preco || this.precoVenda || 0
          })) : [];

        console.log('🔍 tamanhosParaEnvio:', tamanhosParaEnvio);

        const produtoComQuantidade = {
          nome: produtoForm.nome,
          descricao: produtoForm.descricao || '',
          preco: this.precoVenda, // Usar preço de venda como preço principal
          departamento: produtoForm.departamento,
          estoqueMinimo: parseInt(produtoForm.estoqueMinimo) || 5,
          // ✅ ADICIONADO: estoque inicial para produtos sem tamanhos
          estoque: tamanhosParaEnvio.length === 0 ? (parseInt(produtoForm.estoqueInicial) || 0) : 0,
          custoUnitario: parseFloat(produtoForm.precoCompra) || 0, // 💰 Preço de Compra
          fornecedor: produtoForm.fornecedor || '', // 🏭 Fornecedor
          tipoCodigoBarras: produtoForm.tipoCodigoBarras || 'EAN13', // 📊 Tipo de Código
          codigoBarras: produtoForm.codigoBarrasManual || '', // 🔢 Código de Barras
          // 🆕 NOVOS CAMPOS ADICIONADOS:
          prefixoCodigo: produtoForm.prefixoCodigo || 'PROD', // 📋 Prefixo do Código
          margem: parseFloat(produtoForm.margem) || undefined, // 📊 Margem de Lucro (%)
          tamanhos: tamanhosParaEnvio
        };
        
        console.log('📦 Dados mapeados para criação:', produtoComQuantidade);
        console.log('💰 Custo unitário enviado:', produtoComQuantidade.custoUnitario);
        console.log('🏭 Fornecedor enviado:', produtoComQuantidade.fornecedor);
        console.log('📊 Tipo código enviado:', produtoComQuantidade.tipoCodigoBarras);
        
        this.produtoService.criarProduto(produtoComQuantidade).subscribe({
          next: (produtoSalvo: any) => {
            console.log('✅ Produto criado com sucesso!', produtoSalvo);
            console.log('📌 ID do produto criado:', produtoSalvo.id);
            console.log('📌 Nome do produto:', produtoSalvo.nome);
            console.log('📌 Estoque do produto:', produtoSalvo.quantidadeEstoque || produtoSalvo.estoque);
            this.successMessage = 'Produto cadastrado com sucesso!';
            this.resetForm();
            // Aguardar um pouco antes de recarregar para garantir que o banco processou
            setTimeout(() => {
              console.log('🔄 Recarregando lista de produtos...');
              this.carregarProdutos();
            }, 500);
            this.isLoading = false;
          },
          error: (error: any) => {
            console.error('❌ Erro completo ao criar produto:', error);
            this.errorMessage = 'Erro ao salvar produto: ' + (error.error?.message || error.message);
            this.isLoading = false;
          }
        });
      }
    } else {
      this.errorMessage = 'Por favor, preencha todos os campos obrigatórios corretamente.';
    }
  }

  editarProduto(produto: Produto): void {
    console.log('🔧 Editando produto (dados da lista):', produto);
    console.log('🔍 Dados recebidos - custoUnitario:', produto.custoUnitario);
    console.log('🔍 Dados recebidos - departamento:', produto.departamento);
    
    // 🎯 LÓGICA DE COLAPSO: Se clicar no mesmo produto que já está sendo editado, fecha o formulário
    if (this.showForm && this.isEditMode && this.editingProductId === produto.id) {
      console.log('🔄 Fechando formulário - mesmo produto selecionado novamente');
      this.showForm = false;
      this.resetForm();
      return;
    }
    
    this.isLoading = true;
    this.produtoService.buscarPorId(produto.id!).subscribe({
      next: (produtoCompleto: any) => {
        console.log('📦 Dados completos do backend:', produtoCompleto);
        console.log('🔍 Todos os campos disponíveis:', Object.keys(produtoCompleto));
        console.log('🔍 Campo departamento:', produtoCompleto.departamento);
        console.log('🔍 Campo custoUnitario:', produtoCompleto.custoUnitario);
        console.log('🔍 Campo preco:', produtoCompleto.preco);
        
        const camposCom10 = Object.entries(produtoCompleto).filter(([key, value]) => value === 10);
        console.log('🔍 Campos com valor 10:', camposCom10);
        
        this.isEditMode = true;
        this.editingProductId = produto.id!;
        this.showForm = true;
        
        this.pausarObservadores = true;
        
        const originalFlag = this.codigoBarrasGeradoAutomaticamente;
        this.codigoBarrasGeradoAutomaticamente = false;
        
        this.produtoForm.patchValue({
          nome: produtoCompleto.nome || '',
          descricao: produtoCompleto.descricao || '',
          precoCompra: produtoCompleto.custoUnitario || 0, // Campo custoUnitario do backend
          precoVenda: produtoCompleto.preco || 0,
          estoqueMinimo: produtoCompleto.estoqueMinimo || 1,
          departamento: produtoCompleto.departamento || '',
          fornecedor: produtoCompleto.fornecedor || '', // Campo agora existe no backend
          tipoCodigoBarras: produtoCompleto.tipoCodigoBarras || 'EAN13', // Usar formato do backend
          codigoBarrasManual: produtoCompleto.codigoBarras || produtoCompleto.codigo || '',
          // 🆕 NOVOS CAMPOS ADICIONADOS NO PREENCHIMENTO:
          prefixoCodigo: produtoCompleto.prefixoCodigo || 'PROD', // 📋 Prefixo do Código
          margem: produtoCompleto.margem || undefined // 📊 Margem de Lucro (%)
        });
        
        this.codigoBarrasGeradoAutomaticamente = originalFlag;
        
        setTimeout(() => {
          this.pausarObservadores = false;
        }, 100);

        console.log('🔍 Código de barras preservado:', this.produtoForm.get('codigoBarrasManual')?.value);
        console.log('🔖 Tipo de código de barras original do backend:', produtoCompleto.tipoCodigoBarras);
        console.log('🔖 Tipo de código de barras no formulário:', this.produtoForm.get('tipoCodigoBarras')?.value);

        if (produtoCompleto.tamanhos && produtoCompleto.tamanhos.length > 0) {
          this.showTamanhos = true;
          this.tamanhosForm = produtoCompleto.tamanhos.map((t: any) => ({
            id: t.id,
            tamanho: t.tamanho,
            preco: t.preco,
            codigo: t.codigo
          }));
          
          // 🔧 DEFINIR CATEGORIA DE TAMANHO baseada nos tamanhos existentes
          this.definirCategoriaAutomatica(produtoCompleto.tamanhos);
        } else if (produtoCompleto.departamento === '06') {
          this.showTamanhos = true;
          // 🔧 DEFINIR CATEGORIA PADRÃO para roupas quando não há tamanhos
          this.categoriaTamanhoSelecionada = 'roupas-gerais';
          console.log('🔧 Categoria de tamanho definida automaticamente: roupas-gerais');
        }

        this.isLoading = false;
        
        setTimeout(() => {
          const formElement = document.getElementById('form-produto');
          if (formElement) {
            formElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }
        }, 100);
      },
      error: (error: any) => {
        console.error('❌ Erro ao buscar produto completo:', error);
        this.isLoading = false;
        this.errorMessage = 'Erro ao carregar dados do produto para edição';
      }
    });

    if (produto.tamanhos && produto.tamanhos.length > 0) {
      this.showTamanhos = true;
      this.tamanhosForm = produto.tamanhos.map(t => ({
        id: t.id,
        tamanho: t.tamanho,
        preco: t.preco,
        codigo: t.codigo
      }));
    }

    setTimeout(() => {
      const formElement = document.getElementById('form-produto');
      if (formElement) {
        formElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 100);
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
                      <option value="${cat}" ${cat === produto.departamento ? 'selected' : ''}>${cat}</option>
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
        const dadosAtualizacao = {
          nome,
          preco,
          categoria,
          descricao: descricao || produto.descricao,
          quantidade: produto.estoque || 0, // Mapear estoque para quantidade
        };

        const resultado = await this.produtoService.atualizarProduto(produto.id!, dadosAtualizacao as any).toPromise();

        const index = this.produtos.findIndex(p => p.id === produto.id);
        if (index !== -1) {
          this.produtos[index] = { ...this.produtos[index], nome, preco, departamento: categoria, descricao: descricao || produto.descricao };
        }

        this.successMessage = 'Produto atualizado com sucesso!';
        setTimeout(() => this.clearMessages(), 5000);

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

    const bsModal = new (window as any).bootstrap.Modal(modal);
    bsModal.show();

    modal.addEventListener('hidden.bs.modal', () => {
      modal.remove();
    });
  }

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
                        <strong>Departamento:</strong><br>
                        <span class="badge bg-secondary">${this.getNomeDepartamento(produto.departamento)}</span>
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
                        <strong>Estoque Total:</strong><br>
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

              ${produto.tamanhos && produto.tamanhos.length > 0 ? `
              <div class="col-12">
                <div class="card bg-light">
                  <div class="card-body">
                    <h6 class="card-title text-primary mb-3">
                      <i class="bi bi-rulers me-2"></i>
                      Estoque por Tamanho
                    </h6>
                    <div class="row g-2">
                      ${produto.tamanhos.map((tamanho: any) => `
                        <div class="col-md-3">
                          <div class="border rounded p-2 bg-white text-center">
                            <div class="fw-bold text-primary">${tamanho.tamanho}</div>
                            <div class="mt-1">
                              <span class="badge ${(tamanho.estoque ?? 0) > 5 ? 'bg-success' : (tamanho.estoque ?? 0) > 0 ? 'bg-warning text-dark' : 'bg-danger'} fs-6">
                                ${tamanho.estoque ?? 0} unid.
                              </span>
                            </div>
                            ${tamanho.preco && tamanho.preco !== produto.preco ? `
                              <div class="small text-muted mt-1">
                                R$ ${tamanho.preco.toFixed(2)}
                              </div>
                            ` : ''}
                          </div>
                        </div>
                      `).join('')}
                    </div>
                    <div class="mt-3">
                      <small class="text-muted">
                        <i class="bi bi-info-circle me-1"></i>
                        Estoques individuais por tamanho disponível
                      </small>
                    </div>
                  </div>
                </div>
              </div>
              ` : ''}

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
            <!-- BOTÃO REMOVIDO: Editar Produto - conforme solicitado -->
          </div>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    const btnImprimir = modal.querySelector('.imprimir-etiqueta') as HTMLButtonElement;
    const btnImprimirModal = modal.querySelector('.imprimir-etiqueta-modal') as HTMLButtonElement;

    btnImprimir?.addEventListener('click', () => {
      this.imprimirEtiqueta(produto);
    });

    btnImprimirModal?.addEventListener('click', () => {
      this.imprimirEtiqueta(produto);
    });


    const bsModal = new (window as any).bootstrap.Modal(modal);
    bsModal.show();

    modal.addEventListener('hidden.bs.modal', () => {
      modal.remove();
    });
  }

  /**
   * 🔄 MELHORADO: Desabilitação de produto com modal avançado e validações
   */
  desabilitarProduto(id: number, nome: string): void {
    const produto = this.produtos.find((p: Produto) => p.id === id);
    
    if (!produto) {
      this.errorMessage = 'Produto não encontrado na lista atual.';
      return;
    }

    this.mostrarModalDesabilitarProduto(produto);
  }

  /**
   * 🆕 Modal avançado para desabilitação de produto
   */
  private mostrarModalDesabilitarProduto(produto: Produto): void {
    const modalId = 'modal-desabilitar-produto-' + produto.id;
    const modalHtml = `
      <div class="modal fade" id="${modalId}" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content border-warning">
            
            <!-- Header -->
            <div class="modal-header bg-warning text-dark">
              <h5 class="modal-title">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                Desabilitar Produto
              </h5>
              <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>

            <!-- Body -->
            <div class="modal-body">
              <div class="text-center mb-3">
                <i class="bi bi-eye-slash text-warning" style="font-size: 3rem;"></i>
              </div>
              
              <h6 class="text-center mb-3">
                Tem certeza que deseja desabilitar este produto?
              </h6>

              <!-- Informações do produto -->
              <div class="card bg-light mb-3">
                <div class="card-body py-2">
                  <h6 class="card-title mb-2 text-primary">
                    <i class="bi bi-box me-2"></i>
                    ${produto.nome}
                  </h6>
                  <div class="row text-sm">
                    <div class="col-6">
                      <strong>Departamento:</strong><br>
                      <span class="badge bg-secondary">${produto.departamento || 'Sem departamento'}</span>
                    </div>
                    <div class="col-6">
                      <strong>Preço:</strong><br>
                      <span class="text-success">R$ ${produto.preco?.toFixed(2) || '0,00'}</span>
                    </div>
                    <!-- SEÇÃO REMOVIDA: Estoque Atual - será gerenciado na área de movimentação -->
                    <div class="col-6 mt-2">
                      <strong>Código:</strong><br>
                      <span class="badge bg-light text-dark">${produto.codigoBarras || 'Sem código'}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Alertas baseados no status do produto -->
              <!-- ALERTA REMOVIDO: Estoque será gerenciado na área de movimentação -->

              ${produto.estoqueBaixo ? `
                <div class="alert alert-warning d-flex align-items-center">
                  <i class="bi bi-exclamation-triangle-fill me-2"></i>
                  <small>
                    <strong>Estoque Baixo:</strong> Este produto está com estoque abaixo do mínimo.
                  </small>
                </div>
              ` : ''}

              <!-- Explicação da ação -->
              <div class="alert alert-warning d-flex align-items-start">
                <i class="bi bi-lightbulb-fill me-2 mt-1"></i>
                <div>
                  <strong>O que acontecerá:</strong>
                  <ul class="mb-0 mt-1">
                    <li>O produto será removido da listagem principal</li>
                    <li>Não aparecerá mais nas pesquisas</li>
                    <li>Dados históricos serão preservados</li>
                    <li>Pode ser reativado posteriormente</li>
                  </ul>
                </div>
              </div>
            </div>

            <!-- Footer -->
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                <i class="bi bi-x-lg me-1"></i>
                Cancelar
              </button>
              <button 
                type="button" 
                class="btn btn-warning" 
                id="btn-confirmar-desabilitacao-${produto.id}"
                onclick="window.confirmarDesabilitacaoProduto(${produto.id}, '${produto.nome}')">
                <i class="bi bi-eye-slash me-1"></i>
                Sim, Desabilitar
              </button>
            </div>

          </div>
        </div>
      </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    const modal = new (window as any).bootstrap.Modal(document.getElementById(modalId));
    modal.show();

    (window as any).confirmarDesabilitacaoProduto = (id: number, nome: string) => {
      this.executarDesabilitacaoProduto(id, nome, modal);
    };

    modal._element.addEventListener('hidden.bs.modal', () => {
      document.getElementById(modalId)?.remove();
      delete (window as any).confirmarDesabilitacaoProduto;
    });
  }

  /**
   * 🆕 Executa a desabilitação com feedback visual melhorado
   */
  private executarDesabilitacaoProduto(id: number, nome: string, modal: any): void {
    const botaoConfirmar = document.getElementById(`btn-confirmar-desabilitacao-${id}`);
    
    if (botaoConfirmar) {
      botaoConfirmar.innerHTML = `
        <span class="spinner-border spinner-border-sm me-2" role="status"></span>
        Desabilitando...
      `;
      botaoConfirmar.setAttribute('disabled', 'true');
    }

    this.produtoService.deletarProduto(id).subscribe({
      next: () => {
        modal.hide();
        
        this.successMessage = `Produto "${nome}" desabilitado com sucesso! 🎯`;
        
        this.carregarProdutos();
        
        setTimeout(() => {
          this.successMessage = '';
        }, 5000);
      },
      error: (error: any) => {
        console.error('Erro ao desabilitar produto:', error);
        
        if (botaoConfirmar) {
          botaoConfirmar.innerHTML = `
            <i class="bi bi-eye-slash me-1"></i>
            Sim, Desabilitar
          `;
          botaoConfirmar.removeAttribute('disabled');
        }
        
        this.errorMessage = `Erro ao desabilitar produto "${nome}". Tente novamente.`;
        
        setTimeout(() => {
          this.errorMessage = '';
        }, 7000);
      }
    });
  }
  
  /**
   * 🔄 MELHORADO: Reativação de produto com modal avançado
   */
  reativarProduto(id: number, nome: string): void {
    const produto = this.produtosDesabilitados.find((p: Produto) => p.id === id);
    
    if (!produto) {
      this.errorMessage = 'Produto não encontrado na lista de desabilitados.';
      return;
    }

    this.mostrarModalReativarProduto(produto);
  }

  /**
   * 🆕 Modal avançado para reativação de produto
   */
  private mostrarModalReativarProduto(produto: Produto): void {
    const modalId = 'modal-reativar-produto-' + produto.id;
    const modalHtml = `
      <div class="modal fade" id="${modalId}" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content border-success">
            
            <!-- Header -->
            <div class="modal-header bg-success text-white">
              <h5 class="modal-title">
                <i class="bi bi-arrow-clockwise me-2"></i>
                Reativar Produto
              </h5>
              <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>

            <!-- Body -->
            <div class="modal-body">
              <div class="text-center mb-3">
                <i class="bi bi-check-circle text-success" style="font-size: 3rem;"></i>
              </div>
              
              <h6 class="text-center mb-3">
                Tem certeza que deseja reativar este produto?
              </h6>

              <!-- Informações do produto -->
              <div class="card bg-light mb-3">
                <div class="card-body py-2">
                  <h6 class="card-title mb-2 text-primary">
                    <i class="bi bi-box me-2"></i>
                    ${produto.nome}
                  </h6>
                  <div class="row text-sm">
                    <div class="col-6">
                      <strong>Departamento:</strong><br>
                      <span class="badge bg-secondary">${produto.departamento || 'Sem departamento'}</span>
                    </div>
                    <div class="col-6">
                      <strong>Preço:</strong><br>
                      <span class="text-success">R$ ${produto.preco?.toFixed(2) || '0,00'}</span>
                    </div>
                    <!-- SEÇÃO REMOVIDA: Estoque Atual - será gerenciado na área de movimentação -->
                    <div class="col-6 mt-2">
                      <strong>Código:</strong><br>
                      <span class="badge bg-light text-dark">${produto.codigoBarras || 'Sem código'}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Explicação da ação -->
              <div class="alert alert-success d-flex align-items-start">
                <i class="bi bi-lightbulb-fill me-2 mt-1"></i>
                <div>
                  <strong>O que acontecerá:</strong>
                  <ul class="mb-0 mt-1">
                    <li>O produto voltará a aparecer na listagem principal</li>
                    <li>Poderá ser encontrado nas pesquisas</li>
                    <li>Estará disponível para vendas</li>
                    <li>Histórico e dados serão mantidos</li>
                  </ul>
                </div>
              </div>
            </div>

            <!-- Footer -->
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                <i class="bi bi-x-lg me-1"></i>
                Cancelar
              </button>
              <button 
                type="button" 
                class="btn btn-success" 
                id="btn-confirmar-reativacao-${produto.id}"
                onclick="window.confirmarReativacaoProduto(${produto.id}, '${produto.nome}')">
                <i class="bi bi-arrow-clockwise me-1"></i>
                Sim, Reativar
              </button>
            </div>

          </div>
        </div>
      </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    const modal = new (window as any).bootstrap.Modal(document.getElementById(modalId));
    modal.show();

    (window as any).confirmarReativacaoProduto = (id: number, nome: string) => {
      this.executarReativacaoProduto(id, nome, modal);
    };

    modal._element.addEventListener('hidden.bs.modal', () => {
      document.getElementById(modalId)?.remove();
      delete (window as any).confirmarReativacaoProduto;
    });
  }

  /**
   * 🆕 Executa a reativação com feedback visual melhorado
   */
  private executarReativacaoProduto(id: number, nome: string, modal: any): void {
    const botaoConfirmar = document.getElementById(`btn-confirmar-reativacao-${id}`);
    
    if (botaoConfirmar) {
      botaoConfirmar.innerHTML = `
        <span class="spinner-border spinner-border-sm me-2" role="status"></span>
        Reativando...
      `;
      botaoConfirmar.setAttribute('disabled', 'true');
    }

    this.produtoService.reativarProduto(id).subscribe({
      next: () => {
        modal.hide();
        
        this.successMessage = `Produto "${nome}" reativado com sucesso! ✅`;
        
        this.carregarProdutosDesabilitados(); // Recarregar lista de desabilitados
        this.carregarProdutos(); // Recarregar lista principal
        
        setTimeout(() => {
          this.successMessage = '';
        }, 5000);
      },
      error: (error: any) => {
        console.error('Erro ao reativar produto:', error);
        
        if (botaoConfirmar) {
          botaoConfirmar.innerHTML = `
            <i class="bi bi-arrow-clockwise me-1"></i>
            Sim, Reativar
          `;
          botaoConfirmar.removeAttribute('disabled');
        }
        
        this.errorMessage = `Erro ao reativar produto "${nome}". Tente novamente.`;
        
        setTimeout(() => {
          this.errorMessage = '';
        }, 7000);
      }
    });
  }
  
  /**
   * 🆕 Toggle para mostrar/ocultar produtos desabilitados
   */
  toggleProdutosDesabilitados(): void {
    this.showDisabledProducts = !this.showDisabledProducts;
    if (this.showDisabledProducts) {
      this.carregarProdutosDesabilitados();
    }
  }
  
  /**
   * 🆕 Carregar produtos desabilitados
   */
  carregarProdutosDesabilitados(): void {
    this.produtoService.listarProdutosDesabilitados(this.currentPageDesabilitados - 1, this.itemsPerPage).subscribe({
      next: (response: any) => {
        this.produtosDesabilitados = response.content || response;
      },
      error: (error: any) => {
        console.error('❌ Erro ao carregar produtos desabilitados:', error);
        this.errorMessage = 'Erro ao carregar produtos desabilitados';
      }
    });
  }

  resetForm(): void {
    this.produtoForm.reset();
    this.isEditMode = false;
    this.editingProductId = null;
    this.showForm = false;
    this.errorMessage = '';
    
    this.precoNumerico = 0;
    this.precoCompra = 0;
    this.precoVenda = 0;
    this.margemLucro = 0;
    this.markup = 0;
    this.codigoPreview = '';
    
    this.codigoBarrasGerado = '';
    this.codigoBarrasGeradoAutomaticamente = true;
    this.edicaoManualHabilitada = false;
    this.tipoCodigoBarrasSelecionado = 'EAN13'; // Resetar para padrão
    this.codigoBarrasManual = '';
    
    this.showTamanhos = false;
    this.tamanhosForm = [];
    this.categoriaTamanhoSelecionada = '';
    this.novoTamanho = '';
    
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
      const searchLower = this.searchTerm.toLowerCase();
      const matchesSearch = !this.searchTerm || 
                           produto.nome.toLowerCase().includes(searchLower) ||
                           (produto.descricao || '').toLowerCase().includes(searchLower);
      
      const matchesCategory = !this.selectedCategory || 
                             this.selectedCategory === '' || // "Todas as categorias"
                             this.getNomeDepartamento(produto.departamento) === this.selectedCategory;
      
      const matchesTamanho = !this.tamanhoSelecionadoFiltro || 
                            this.tamanhoSelecionadoFiltro === '' || // "Todos os tamanhos"
                            (produto.temTamanhos && produto.tamanhos && 
                             produto.tamanhos.some(t => t.tamanho === this.tamanhoSelecionadoFiltro));
      
      return matchesSearch && matchesCategory && matchesTamanho;
    });

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
          return (statsB?.diasSemVender || 0) - (statsA?.diasSemVender || 0);
        
        case 'menos-tempo-sem-vender':
          return (statsA?.diasSemVender || 0) - (statsB?.diasSemVender || 0);
        
        default:
          return 0;
      }
    });
  }

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

  // 🆕 Métodos para status ativo/inativo do produto
  getStatusAtivoBadgeClass(produto: Produto): string {
    return produto.ativo ? 'bg-success' : 'bg-secondary';
  }

  getStatusAtivoText(produto: Produto): string {
    return produto.ativo ? 'ATIVO' : 'INATIVO';
  }

  getStatusAtivoIcon(produto: Produto): string {
    return produto.ativo ? 'bi-check-circle-fill' : 'bi-x-circle-fill';
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
      
      if (control.errors['required']) return `${field} é obrigatório`;
      if (control.errors['minlength']) return `${field} deve ter pelo menos ${control.errors['minlength'].requiredLength} caracteres`;
      if (control.errors['maxlength']) return `${field} deve ter no máximo ${control.errors['maxlength'].requiredLength} caracteres`;
      if (control.errors['min']) return `${field} deve ser maior que ${control.errors['min'].min}`;
    }
    return '';
  }

  getTempoSemVenderClass(produtoId: number): string {
    const diasSemVender = this.getDiasSemVender(produtoId);
    
    if (diasSemVender === 0) return ''; // Nunca vendido
    if (diasSemVender <= 30) return 'tempo-recente'; // Até 1 mês
    if (diasSemVender <= 90) return 'tempo-medio'; // Até 3 meses
    if (diasSemVender <= 180) return 'tempo-alto'; // Até 6 meses
    return 'tempo-critico'; // Mais de 6 meses
  }

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
    
    if (mapeamentoCategorias[categoriaLower]) {
      return mapeamentoCategorias[categoriaLower];
    }

    for (const [chave, codigo] of Object.entries(mapeamentoCategorias)) {
      if (categoriaLower.includes(chave) || chave.includes(categoriaLower)) {
        return codigo;
      }
    }

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
    const prefixo = `${departamento}${categoria}`;
    
    const agora = new Date();
    const sequencial = (agora.getHours() * 100 + agora.getMinutes()) % 9999 + 1;
    
    return sequencial.toString().padStart(4, '0');
  }

  private calcularDigitoVerificador(codigo: string): string {
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

  formatarPreco(event: any): void {
    let input = event.target.value;
    
    let valorParaCalculo = input.replace(',', '.');
    const valorNumerico = parseFloat(valorParaCalculo) || 0;
    this.precoNumerico = valorNumerico;
    
    this.produtoForm.get('preco')?.setValue(valorNumerico, { emitEvent: false });
  }
  
  formatarPrecoAoSair(event: any): void {
    const valor = event.target.value;
    if (!valor) return;
    
    const valorParaCalculo = valor.replace(',', '.');
    const valorNumerico = parseFloat(valorParaCalculo) || 0;
    
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    event.target.value = valorFormatado;
    this.validarPreco();
  }

  formatarPrecoCompra(event: any): void {
    let valor = event.target.value;
    
    const apenasNumeros = valor.replace(/\D/g, '');
    
    if (apenasNumeros === '') {
      this.precoCompra = 0;
      this.produtoForm.get('precoCompra')?.setValue(0, { emitEvent: false });
      this.calcularMargemLucro();
      return;
    }
    
    const valorEmCentavos = parseInt(apenasNumeros);
    const valorNumerico = valorEmCentavos / 100;
    
    this.precoCompra = valorNumerico;
    
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    setTimeout(() => {
      event.target.value = valorFormatado;
    }, 0);
    
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

  formatarPrecoVenda(event: any): void {
    let valor = event.target.value;
    
    const apenasNumeros = valor.replace(/\D/g, '');
    
    if (apenasNumeros === '') {
      this.precoVenda = 0;
      this.produtoForm.get('precoVenda')?.setValue(0, { emitEvent: false });
      this.calcularMargemLucro();
      return;
    }
    
    const valorEmCentavos = parseInt(apenasNumeros);
    const valorNumerico = valorEmCentavos / 100;
    
    this.precoVenda = valorNumerico;
    
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    setTimeout(() => {
      event.target.value = valorFormatado;
    }, 0);
    
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

  formatarPrecoCompraAoSair(event: any): void {
    const valor = event.target.value;
    if (!valor) return;
    
    const valorParaCalculo = valor.replace(',', '.');
    const valorNumerico = parseFloat(valorParaCalculo) || 0;
    
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    event.target.value = valorFormatado;
    this.precoCompra = valorNumerico;
    this.calcularMargemLucro();
  }

  formatarPrecoVendaAoSair(event: any): void {
    const valor = event.target.value;
    if (!valor) return;
    
    const valorParaCalculo = valor.replace(',', '.');
    const valorNumerico = parseFloat(valorParaCalculo) || 0;
    
    const valorFormatado = valorNumerico.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    event.target.value = valorFormatado;
    this.precoVenda = valorNumerico;
    this.calcularMargemLucro();
  }

  formatarEstoque(event: any): void {
    let valor = event.target.value;
    
    valor = valor.replace(/\D/g, '');
    
    const valorNumerico = parseInt(valor) || 0;
    
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

  validarEstoque(event: any): void {
    let valor = parseInt(event.target.value) || 0;
    
    if (valor <= 0) {
      event.target.classList.add('is-invalid');
      event.target.style.border = '2px solid #dc3545'; // Borda vermelha
      this.produtoForm.get('estoque')?.setErrors({ invalidStock: true });
      
      this.errorMessage = '⚠️ Estoque não pode ser zero ou negativo!';
      setTimeout(() => this.errorMessage = '', 3000);
    } else {
      event.target.classList.remove('is-invalid');
      event.target.style.border = '';
      this.produtoForm.get('estoque')?.setErrors(null);
    }
    
    if (valor <= 0) {
      event.target.value = 1;
      this.produtoForm.get('estoque')?.setValue(1, { emitEvent: false });
    }
  }

  validarEstoqueMinimo(event: any): void {
    let valor = parseInt(event.target.value) || 0;
    
    if (valor <= 0) {
      event.target.classList.add('is-invalid');
      event.target.style.border = '2px solid #dc3545'; // Borda vermelha
      this.produtoForm.get('estoqueMinimo')?.setErrors({ invalidStock: true });
      
      this.errorMessage = '⚠️ Estoque mínimo não pode ser zero ou negativo!';
      setTimeout(() => this.errorMessage = '', 3000);
    } else {
      event.target.classList.remove('is-invalid');
      event.target.style.border = '';
      this.produtoForm.get('estoqueMinimo')?.setErrors(null);
    }
    
    if (valor <= 0) {
      event.target.value = 1;
      this.produtoForm.get('estoqueMinimo')?.setValue(1, { emitEvent: false });
    }
  }

  validarPrecoCompra(event: any): void {
    let valor = parseFloat(event.target.value.replace(/[^\d,]/g, '').replace(',', '.')) || 0;
    
    if (valor <= 0) {
      event.target.classList.add('is-invalid');
      event.target.style.border = '2px solid #dc3545'; // Borda vermelha
      this.produtoForm.get('precoCompra')?.setErrors({ invalidPrice: true });
      
      this.errorMessage = '⚠️ Preço de compra não pode ser zero ou negativo!';
      setTimeout(() => this.errorMessage = '', 3000);
    } else {
      event.target.classList.remove('is-invalid');
      event.target.style.border = '';
      this.produtoForm.get('precoCompra')?.setErrors(null);
    }
  }

  validarPrecoVenda(event: any): void {
    let valor = parseFloat(event.target.value.replace(/[^\d,]/g, '').replace(',', '.')) || 0;
    
    if (valor <= 0) {
      event.target.classList.add('is-invalid');
      event.target.style.border = '2px solid #dc3545'; // Borda vermelha
      this.produtoForm.get('precoVenda')?.setErrors({ invalidPrice: true });
      
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
    
    const valorLimpo = valorFormatado
      .replace(/\./g, '')
      .replace(',', '.');
    
    const numero = parseFloat(valorLimpo);
    return isNaN(numero) ? 0 : numero;
  }

  gerarCodigoBarrasAutomatico(): void {
    if (!this.codigoBarrasGeradoAutomaticamente) {
      return;
    }
    
    const tipoCodigoBarras = this.produtoForm.get('tipoCodigoBarras')?.value || this.tipoCodigoBarrasSelecionado;
    const departamento = this.produtoForm.get('departamento')?.value;
    const codigoExistente = this.produtoForm.get('codigoBarrasManual')?.value;
    
    if (this.isEditMode && codigoExistente && codigoExistente.trim() !== '') {
      return;
    }
    
    if (!tipoCodigoBarras) {
      return; // Não gera se não tem tipo selecionado
    }

    if (!departamento) {
      return; // Não gera se não tem departamento (necessário para prefixo)
    }

    let prefixo = '';
    if (tipoCodigoBarras === 'EAN13' || tipoCodigoBarras === 'EAN8') {
      prefixo = departamento;
    } else {
      prefixo = `D${departamento}`;
    }

    const request: CodigoBarrasRequest = {
      tipoCodigoBarras: tipoCodigoBarras as TipoCodigoBarras,
      prefixo: prefixo,
      gerarAutomaticamente: true
    };

    this.produtoService.gerarCodigoPersonalizado(request).subscribe({
      next: (response: any) => {
        this.codigoBarrasGerado = response.codigo;
        this.codigoBarrasGeradoAutomaticamente = true;
      },
      error: (error: any) => {
        console.warn('⚠️ Erro ao gerar código de barras, usando fallback:', error);
        const timestamp = Date.now().toString().slice(-8);
        const fallbackCodigo = prefixo + timestamp;
        this.codigoBarrasGerado = fallbackCodigo;
        this.codigoBarrasGeradoAutomaticamente = true;
      }
    });
  }


  onTipoCodigoBarrasChange(): void {
    const novoTipo = this.produtoForm.get('tipoCodigoBarras')?.value;
    this.tipoCodigoBarrasSelecionado = novoTipo;
    
    if (this.codigoBarrasGeradoAutomaticamente) {
      this.gerarCodigoBarrasAutomatico();
    }
  }

  alternarModoCodigoBarras(): void {
    this.edicaoManualHabilitada = !this.edicaoManualHabilitada;
    
    if (this.edicaoManualHabilitada) {
      this.codigoBarrasGeradoAutomaticamente = false;
      this.codigoBarrasManual = this.codigoBarrasGerado; // Copiar para edição
    } else {
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
    
    if (codigo) {
      const valido = this.produtoService.validarCodigoBarras(codigo);
      if (valido) {
        this.successMessage = `Código de barras ${codigo} é válido`;
        this.codigoBarrasGerado = codigo;
      } else {
        this.errorMessage = `Código de barras ${codigo} é inválido`;
      }
      setTimeout(() => this.clearMessages(), 3000);
    }
  }

  getCodigoBarrasDisplay(): string {
    if (this.edicaoManualHabilitada) {
      return this.codigoBarrasManual || '';
    }
    return this.codigoBarrasGerado || '';
  }

  getTipoCodigoBarrasSelecionadoInfo(): TipoCodigoBarrasInfo | undefined {
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


  toggleTamanhos(): void {
    // showTamanhos já é atualizado automaticamente pelo ngModel
    
    if (this.showTamanhos && this.tamanhosForm.length === 0) {
      setTimeout(() => {
        this.adicionarTamanhoForm();
      }, 100);
    }
    
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
    console.log('🔍 DEBUG: Adicionando tamanho form');
    console.log('🔍 Estado anterior tamanhosForm:', this.tamanhosForm);
    
    const novoId = this.tamanhosForm.length > 0 ? 
      Math.max(...this.tamanhosForm.map(t => t.id!)) + 1 : 1;
    
    this.tamanhosForm.push({
      id: novoId,
      tamanho: '',
      codigo: ''
    });

    console.log('🔍 Estado após adicionar:', this.tamanhosForm);

    setTimeout(() => this.verificarCamposFaltantes(), 200);
    
    // 🎯 MELHORIA DE USABILIDADE: Focar automaticamente no campo de tamanho
    setTimeout(() => {
      this.focarCampoTamanhoRecemAdicionado();
    }, 100);
  }

  /**
   * 🎯 MELHORIA DE USABILIDADE: Foca no campo de tamanho do último item adicionado
   */
  private focarCampoTamanhoRecemAdicionado(): void {
    const ultimoIndex = this.tamanhosForm.length - 1;
    setTimeout(() => {
      const campoTamanho = document.querySelector(`tr:nth-child(${ultimoIndex + 1}) td:nth-child(1) input`) as HTMLInputElement;
      if (campoTamanho) {
        campoTamanho.focus();
        campoTamanho.select();
        console.log('🎯 Foco definido automaticamente no campo de tamanho');
      }
    }, 150); // Timeout aumentado para garantir renderização
  }

  /**
   * 🎯 MELHORIA DE USABILIDADE: Adiciona tamanho predefinido e foca no campo quantidade
   */
  adicionarTamanhoPredefinido(tamanho: string): void {
    console.log('🔍 DEBUG: Adicionando tamanho predefinido:', tamanho);
    console.log('🔍 Estado atual tamanhosForm:', this.tamanhosForm);
    
    const tamanhoExiste = this.tamanhosForm.some(t => 
      t.tamanho.toLowerCase().trim() === tamanho.toLowerCase().trim()
    );
    
    if (tamanhoExiste) {
      this.errorMessage = `O tamanho "${tamanho}" já foi adicionado. Escolha um tamanho diferente.`;
      setTimeout(() => this.clearMessages(), 3000);
      return;
    }
    
    this.adicionarTamanhoForm();
    const ultimoTamanho = this.tamanhosForm[this.tamanhosForm.length - 1];
    ultimoTamanho.tamanho = tamanho;
  }

  /**
   * 🎯 MELHORIA DE USABILIDADE: Foca no campo quantidade de um tamanho específico
   */
  private focarCampoQuantidade(index: number): void {
    setTimeout(() => {
      const campoQuantidade = document.querySelector(`tr:nth-child(${index + 1}) td:nth-child(2) input`) as HTMLInputElement;
      if (campoQuantidade) {
        campoQuantidade.focus();
        campoQuantidade.select();
        console.log(`🎯 Foco definido automaticamente no campo quantidade (linha ${index + 1})`);
      }
    }, 150);
  }

  /**
   * 🎯 MELHORIA DE USABILIDADE: Navega entre campos com Enter no campo tamanho
   */
  onTamanhoKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.focarCampoQuantidade(index);
    }
  }

  /**
   * 🎯 MELHORIA DE USABILIDADE: Navega entre campos com Enter no campo quantidade
   */
  onQuantidadeKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      
      if (index === this.tamanhosForm.length - 1) {
        this.adicionarTamanhoForm();
      } else {
        setTimeout(() => {
          this.focarCampoTamanhoDoIndice(index + 1);
        }, 50);
      }
    }
  }

  /**
   * 🎯 MELHORIA DE USABILIDADE: Foca no campo tamanho de um índice específico
   */
  private focarCampoTamanhoDoIndice(index: number): void {
    setTimeout(() => {
      const campoTamanho = document.querySelector(`tr:nth-child(${index + 1}) td:nth-child(1) input`) as HTMLInputElement;
      if (campoTamanho) {
        campoTamanho.focus();
        campoTamanho.select();
        console.log(`🎯 Foco definido no campo tamanho (linha ${index + 1})`);
      }
    }, 150);
  }

  /**
   * 🎯 MELHORIA DE USABILIDADE: Quando categoria de tamanho muda, rola automaticamente para mostrar tamanhos disponíveis
   */
  onCategoriaTamanhoChange(): void {
    console.log('🔧 Categoria de tamanho alterada para:', this.categoriaTamanhoSelecionada);
    
    if (this.categoriaTamanhoSelecionada && this.categoriaTamanhoSelecionada !== '') {
      setTimeout(() => {
        const tamanhosDisponiveis = document.querySelector('.d-flex.flex-wrap.gap-2');
        if (tamanhosDisponiveis) {
          tamanhosDisponiveis.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }, 200);
    }
  }

  removerTamanhoForm(index: number): void {
    this.tamanhosForm.splice(index, 1);
    
    setTimeout(() => this.verificarCamposFaltantes(), 200);
  }


  formatarTamanho(tamanho: TamanhoProduto): void {
    if (!tamanho.tamanho) return;
    
    tamanho.tamanho = tamanho.tamanho.trim().toUpperCase();
  }

  validarTamanhoPersonalizado(tamanho: TamanhoProduto, event: any): void {
    const valor = event.target.value?.trim().toUpperCase();
    
    if (this.categoriaTamanhoSelecionada === 'roupas-gerais' && valor) {
      const tamanhosRoupasPadrao = ['PP', 'P', 'M', 'G', 'GG', 'GGG', 'XG', 'XGG', 'XGGG', '2G', '3G'];
      
      if (!tamanhosRoupasPadrao.includes(valor) && valor.length > 0) {
        this.confirmarTamanhoNaoPadrao(tamanho, valor);
      }
    }
  }

  private confirmarTamanhoNaoPadrao(tamanho: TamanhoProduto, valor: string): void {
    const confirmacao = confirm(
      `⚠️ Confirmação de Tamanho\n\n` +
      `O tamanho "${valor}" não é um tamanho padrão para roupas gerais.\n\n` +
      `Tamanhos padrão: PP, P, M, G, GG, GGG, XG, XGG, XGGG, 2G, 3G\n\n` +
      `Deseja realmente usar o tamanho "${valor}"?`
    );
    
    if (!confirmacao) {
      tamanho.tamanho = '';
    }
  }

  getTamanhosPorCategoria(categoria: string): string[] {
    const tipo = this.tiposTamanho.find(t => t.categoria === categoria);
    return tipo ? (tipo.tamanhos || []) : [];
  }

  adicionarTamanhoCustomizado(): void {
    console.log('🔍 DEBUG: Adicionando tamanho customizado:', this.novoTamanho);
    console.log('🔍 Estado atual tamanhosForm:', this.tamanhosForm);
    
    if (this.novoTamanho.trim()) {
      const tamanhoExiste = this.tamanhosForm.some(t => 
        t.tamanho.toLowerCase().trim() === this.novoTamanho.toLowerCase().trim()
      );
      
      if (tamanhoExiste) {
        this.errorMessage = `O tamanho "${this.novoTamanho.trim()}" já foi adicionado. Digite um tamanho diferente.`;
        setTimeout(() => this.clearMessages(), 3000);
        return;
      }
      
      this.adicionarTamanhoForm();
      const ultimoTamanho = this.tamanhosForm[this.tamanhosForm.length - 1];
      ultimoTamanho.tamanho = this.novoTamanho.trim();
      
      console.log('🔍 Tamanho adicionado:', ultimoTamanho);
      console.log('🔍 Estado final tamanhosForm:', this.tamanhosForm);
      
      if (!this.tamanhosDisponiveis.includes(this.novoTamanho.trim())) {
        this.tamanhosDisponiveis.push(this.novoTamanho.trim());
        this.tamanhosDisponiveis.sort();
      }
      
      this.novoTamanho = '';
    }
  }

  // - relacionados ao estoque inicial, não mais necessários

  onDepartamentoChange(): void {
    this.gerarPreviewCodigo();
    
    // 🎯 MELHORIA DE USABILIDADE: Auto-ativar gestão de tamanhos para departamento de roupas
    if (this.isDepartamentoRoupas()) {
      this.showTamanhos = true;
      // 🔧 Definir categoria padrão automaticamente para roupas
      if (!this.categoriaTamanhoSelecionada) {
        this.categoriaTamanhoSelecionada = 'roupas-gerais';
        console.log('🔧 Categoria de tamanho definida automaticamente: roupas-gerais (departamento 06)');
      }
      
      // 🎯 SCROLL AUTOMÁTICO para a seção de tamanhos após expansão
      setTimeout(() => {
        const tamanhoSection = document.querySelector('.tamanhos-card');
        if (tamanhoSection) {
          tamanhoSection.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }, 300);
    } else {
      this.verificarDepartamentoRoupas(); // Este método já desativa se não for roupas
      this.categoriaTamanhoSelecionada = '';
    }
  }

  isDepartamentoRoupas(): boolean {
    const departamento = this.produtoForm.get('departamento')?.value;
    return departamento === '06';
  }

  verificarDepartamentoRoupas(): void {
    if (!this.isDepartamentoRoupas()) {
      this.showTamanhos = false;
      this.tamanhosForm = [];
    }
  }

  definirCategoriaAutomatica(tamanhos: any[]): void {
    // 🔧 Definir categoria de tamanho automaticamente baseada nos tamanhos existentes
    if (!tamanhos || tamanhos.length === 0) {
      return;
    }

    const tamanhosExistentes = tamanhos.map(t => t.tamanho);
    console.log('🔍 Analisando tamanhos existentes:', tamanhosExistentes);

    for (const tipoTamanho of this.tiposTamanho) {
      if (!tipoTamanho.tamanhos || !tipoTamanho.categoria) {
        continue;
      }

      const matchCount = tamanhosExistentes.filter(t => 
        tipoTamanho.tamanhos!.includes(t)
      ).length;
      
      if (matchCount >= tamanhosExistentes.length / 2) {
        this.categoriaTamanhoSelecionada = tipoTamanho.categoria;
        console.log(`🔍 Match: ${matchCount}/${tamanhosExistentes.length} tamanhos`);
        return;
      }
    }

    this.categoriaTamanhoSelecionada = 'roupas-gerais';
    console.log('🔧 Fallback: categoria definida como roupas-gerais');
  }


  getCamposFaltantes(): string[] {
    const camposFaltantes: string[] = [];

    if (!this.produtoForm.get('nome')?.value?.trim()) {
      camposFaltantes.push('Nome do produto');
    }
    
    
    if (!this.produtoForm.get('precoCompra')?.value || this.produtoForm.get('precoCompra')?.value < 0) {
      camposFaltantes.push('Preço de compra válido (R$ 0,00 ou maior)');
    }
    
    if (!this.produtoForm.get('precoVenda')?.value || this.produtoForm.get('precoVenda')?.value < 0.01) {
      camposFaltantes.push('Preço de venda válido (mínimo R$ 0,01)');
    }
    
    if (!this.produtoForm.get('departamento')?.value) {
      camposFaltantes.push('Departamento');
    }
    
    if (!this.produtoForm.get('tipoCodigoBarras')?.value) {
      camposFaltantes.push('Tipo de código de barras');
    }

    return camposFaltantes;
  }

  gerarCodigoTamanho(codigoBase: string, tamanho: string): string {
    return `${codigoBase}-${tamanho}`;
  }

  getEstatisticasTamanho(produtoId: number): void {
    console.log('📊 Estatísticas por tamanho não disponíveis no backend atual');
  }

  visualizarCodigoBarras(produto: Produto): void {
    const codigoBarras = produto.codigoBarras || produto.codigo?.replace(/-/g, '') || 'SEM-CODIGO';
    
    // Gerar IDs únicos
    const timestamp = Date.now();
    const svgId = `barcode-svg-${timestamp}`;
    const fallbackId = `barcode-fallback-${timestamp}`;
    
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.innerHTML = `
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header bg-primary text-white">
            <h5 class="modal-title">
              <i class="bi bi-upc-scan me-2"></i>
              Código de Barras: ${produto.nome}
            </h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body text-center">
            <div class="border p-4 bg-light rounded mb-3">
              <div class="font-monospace h4 text-primary mb-3">${codigoBarras}</div>
              <div class="small text-muted mb-3">Código para scanner ou leitura manual</div>
              
              <!-- Container para código de barras SVG -->
              <div class="barcode-container bg-white p-3 border rounded shadow-sm mb-3">
                <svg id="${svgId}" class="barcode-svg" style="width: 100%; height: 100px;"></svg>
                <div class="fallback-barcode d-none">
                  <div class="text-center p-3 border rounded bg-white">
                    <div class="barcode-visual mb-2" id="${fallbackId}"></div>
                  </div>
                </div>
              </div>
              
              <div class="text-muted small">
                <i class="bi bi-info-circle me-1"></i>
                Tipo: ${produto.tipoCodigoBarras || 'EAN13'} | 
                Formato: ${codigoBarras.length} dígitos
              </div>
            </div>
            
            <div class="row text-start">
              <div class="col-md-6">
                <strong>📦 Produto:</strong><br>
                <span class="text-muted">${produto.nome}</span>
              </div>
              <div class="col-md-3">
                <strong>💰 Preço:</strong><br>
                <span class="text-success fw-bold">R$ ${produto.preco?.toFixed(2) || '0,00'}</span>
              </div>
              <div class="col-md-3">
                <strong>📊 Estoque:</strong><br>
                <span class="text-info">${produto.estoque || 0} un.</span>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">
              <i class="bi bi-x-circle me-1"></i>Fechar
            </button>
            <button type="button" class="btn btn-success imprimir-codigo">
              <i class="bi bi-printer me-1"></i>Imprimir Etiqueta
            </button>
            <button type="button" class="btn btn-primary copiar-codigo">
              <i class="bi bi-clipboard me-1"></i>Copiar Código
            </button>
          </div>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    // Gerar código de barras SVG após modal estar visível
    setTimeout(() => {
      this.renderizarCodigoBarrasSVG(codigoBarras, svgId, fallbackId);
    }, 300);

    // Event listeners
    const btnImprimir = modal.querySelector('.imprimir-codigo') as HTMLButtonElement;
    btnImprimir?.addEventListener('click', () => {
      this.imprimirEtiqueta(produto);
    });

    const btnCopiar = modal.querySelector('.copiar-codigo') as HTMLButtonElement;
    btnCopiar?.addEventListener('click', () => {
      navigator.clipboard.writeText(codigoBarras).then(() => {
        btnCopiar.innerHTML = '<i class="bi bi-check me-1"></i>Copiado!';
        setTimeout(() => {
          btnCopiar.innerHTML = '<i class="bi bi-clipboard me-1"></i>Copiar Código';
        }, 2000);
      });
    });

    const bsModal = new (window as any).bootstrap.Modal(modal);
    bsModal.show();

    modal.addEventListener('hidden.bs.modal', () => {
      modal.remove();
    });
  }

  // Método para gerar código de barras visual usando sistema CSS personalizado
  renderizarCodigoBarrasSVG(codigo: string, svgId: string, fallbackId?: string): void {
    console.log('🎯 Renderizando código de barras:', codigo, 'SVG ID:', svgId);
    
    // Detectar tipo de código e renderizar adequadamente
    const isCodigoPersonalizado = codigo.startsWith('PROD-') || codigo.includes('-');
    
    if (isCodigoPersonalizado) {
      console.log('🔧 Usando sistema personalizado para código:', codigo);
    } else {
      console.log('🔧 Usando sistema EAN13 para código numérico:', codigo);
    }
    
    this.mostrarFallbackCodigoBarras(svgId, fallbackId, codigo);
    
    /* Código JsBarcode desabilitado - usando sistema personalizado
    try {
      // Verificar se JsBarcode está disponível
      if (typeof (window as any).JsBarcode === 'undefined') {
        console.warn('⚠️ JsBarcode não disponível, usando fallback');
        this.mostrarFallbackCodigoBarras(svgId, fallbackId, codigo);
        return;
      }
      
      const svg = document.getElementById(svgId);
      if (!svg) {
        console.warn('⚠️ Elemento SVG não encontrado:', svgId);
        this.mostrarFallbackCodigoBarras(svgId, fallbackId, codigo);
        return;
      }
      
      console.log('✅ Gerando código de barras SVG...');
      
      // Limpar o código para JsBarcode (apenas números para EAN)
      const codigoLimpo = codigo.replace(/[^0-9]/g, '');
      const formato = this.detectarFormatoCodigoBarras(codigoLimpo);
      
      console.log('📊 Código limpo:', codigoLimpo, 'Formato:', formato);
      
      (window as any).JsBarcode(svg, codigoLimpo, {
        format: formato,
        width: 2,
        height: 80,
        displayValue: true,
        fontSize: 14,
        margin: 5,
        background: '#ffffff',
        lineColor: '#000000',
        textAlign: 'center',
        textPosition: 'bottom',
        font: 'monospace'
      });
      
      console.log('✅ Código de barras SVG gerado com sucesso!');
      
    } catch (error) {
      console.error('❌ Erro ao gerar código de barras SVG:', error);
      this.mostrarFallbackCodigoBarras(svgId, fallbackId, codigo);
    }
    */
  }

  private mostrarFallbackCodigoBarras(svgId: string, fallbackId: string | undefined, codigo: string): void {
    console.log('🔄 Ativando fallback para código:', codigo);
    
    const svg = document.getElementById(svgId);
    if (svg) {
      svg.style.display = 'none';
      const fallbackContainer = svg.parentElement?.querySelector('.fallback-barcode');
      if (fallbackContainer) {
        fallbackContainer.classList.remove('d-none');
        
        // Atualizar conteúdo do fallback com código de barras visual real
        const fallbackDiv = fallbackContainer.querySelector('.barcode-visual');
        if (fallbackDiv) {
          fallbackDiv.innerHTML = this.gerarCodigoBarrasVisualMelhorado(codigo);
          
          // Adicionar estilos CSS para as barras
          this.adicionarEstilosCodigoBarras();
        }
      }
    }
  }
  
  private adicionarEstilosCodigoBarras(): void {
    // Verificar se os estilos já foram adicionados
    if (document.getElementById('barcode-styles')) {
      return;
    }
    
    const style = document.createElement('style');
    style.id = 'barcode-styles';
    style.textContent = `
      .barcode-container-visual {
        display: flex;
        align-items: end;
        justify-content: center;
        height: 80px;
        background: white;
        padding: 10px;
        margin: 10px 0;
      }
      
      .barcode-bar {
        width: 1px;
        height: 100%;
        background-color: #000;
        margin: 0;
      }
      
      .barcode-space {
        width: 1px;
        height: 100%;
        background-color: transparent;
        margin: 0;
      }
      
      .barcode-number {
        text-align: center;
        font-family: 'Courier New', monospace;
        font-size: 14px;
        font-weight: bold;
        margin-top: 5px;
        letter-spacing: 2px;
        color: #000;
      }
    `;
    
    document.head.appendChild(style);
  }

  private detectarFormatoCodigoBarras(codigo: string): string {
    console.log('🔍 Detectando formato para código:', codigo, 'Tamanho:', codigo.length);
    
    if (codigo.length === 13 && /^\d+$/.test(codigo)) {
      console.log('✅ Formato detectado: EAN13');
      return 'EAN13';
    }
    if (codigo.length === 8 && /^\d+$/.test(codigo)) {
      console.log('✅ Formato detectado: EAN8');
      return 'EAN8';
    }
    if (codigo.length === 12 && /^\d+$/.test(codigo)) {
      console.log('✅ Formato detectado: UPC');
      return 'UPC';
    }
    
    console.log('✅ Formato detectado: CODE128 (fallback)');
    return 'CODE128';
  }

  private getFormatoCodigoBarras(codigo: string): string {
    const formato = this.detectarFormatoCodigoBarras(codigo);
    const tamanho = codigo.length;
    return `${formato} (${tamanho} dígitos)`;
  }

  private gerarCodigoBarrasVisual(codigo: string): string {
    // Gera representação visual com barras usando caracteres especiais
    const barras = [];
    for (let i = 0; i < codigo.length; i++) {
      const digito = parseInt(codigo[i]) || 0;
      // Padrão simples: dígitos pares = barra grossa, ímpares = barra fina
      barras.push(digito % 2 === 0 ? '█' : '▌');
    }
    return `▌${barras.join('')}▌`;
  }
  
  private gerarCodigoBarrasVisualMelhorado(codigo: string): string {
    const codigoLimpo = codigo.replace(/[^0-9]/g, '');
    
    // Gerar código de barras HTML com CSS
    const barcodeHtml = this.gerarCodigoBarrasHTML(codigoLimpo);
    
    return barcodeHtml;
  }
  
  private gerarCodigoBarrasHTML(codigo: string): string {
    // Padrões EAN13 reais para cada dígito (L-code, G-code, R-code)
    const patternsL = {
      '0': '0001101', '1': '0011001', '2': '0010011', '3': '0111101',
      '4': '0100011', '5': '0110001', '6': '0101111', '7': '0111011',
      '8': '0110111', '9': '0001011'
    };
    
    const patternsG = {
      '0': '0100111', '1': '0110011', '2': '0011011', '3': '0100001',
      '4': '0011101', '5': '0111001', '6': '0000101', '7': '0010001',
      '8': '0001001', '9': '0010111'
    };
    
    const patternsR = {
      '0': '1110010', '1': '1100110', '2': '1101100', '3': '1000010',
      '4': '1011100', '5': '1001110', '6': '1010000', '7': '1000100',
      '8': '1001000', '9': '1110100'
    };
    
    // Padrões de seleção para primeiro dígito
    const firstDigitPatterns = {
      '0': 'LLLLLL', '1': 'LLGLGG', '2': 'LLGGLG', '3': 'LLGGGL',
      '4': 'LGLLGG', '5': 'LGGLLG', '6': 'LGGGLL', '7': 'LGLGLG',
      '8': 'LGLGGL', '9': 'LGGLGL'
    };
    
    // Completar com zeros se necessário
    const paddedCode = codigo.padEnd(13, '0').substring(0, 13);
    
    let binaryPattern = '101'; // Start guard
    
    // Primeiro dígito define o padrão
    const firstDigit = paddedCode[0];
    const pattern = firstDigitPatterns[firstDigit as keyof typeof firstDigitPatterns] || 'LLLLLL';
    
    // Primeiros 6 dígitos
    for (let i = 1; i <= 6; i++) {
      const digit = paddedCode[i] || '0';
      if (pattern[i-1] === 'L') {
        binaryPattern += patternsL[digit as keyof typeof patternsL];
      } else {
        binaryPattern += patternsG[digit as keyof typeof patternsG];
      }
    }
    
    binaryPattern += '01010'; // Center guard
    
    // Últimos 6 dígitos
    for (let i = 7; i <= 12; i++) {
      const digit = paddedCode[i] || '0';
      binaryPattern += patternsR[digit as keyof typeof patternsR];
    }
    
    binaryPattern += '101'; // End guard
    
    // Converter padrão binário em HTML
    let barcodeHtml = '<div class="barcode-container-visual">';
    
    for (let i = 0; i < binaryPattern.length; i++) {
      if (binaryPattern[i] === '1') {
        barcodeHtml += '<div class="barcode-bar"></div>';
      } else {
        barcodeHtml += '<div class="barcode-space"></div>';
      }
    }
    
    barcodeHtml += '</div>';
    barcodeHtml += `<div class="barcode-number">${paddedCode}</div>`;
    
    return barcodeHtml;
  }

  imprimirEtiqueta(produto: Produto): void {
    const codigoBarras = produto.codigoBarras || produto.codigo || 'SEM-CODIGO';
    
    // Usar o mesmo sistema de renderização do visualizarCodigoBarras
    const codigoVisualHTML = this.gerarCodigoBarrasVisualMelhorado(codigoBarras);
    
    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(`
        <html>
          <head>
            <title>Etiqueta - ${produto.nome}</title>
            <style>
              body { 
                font-family: Arial, sans-serif; 
                margin: 0; 
                padding: 20px; 
                background: white;
              }
              .etiqueta { 
                border: 2px solid #000; 
                padding: 15px; 
                width: 400px; 
                text-align: center;
                background: white;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                margin: 0 auto;
              }
              .nome-produto { 
                font-weight: bold; 
                margin-bottom: 12px; 
                font-size: 16px;
                word-wrap: break-word;
                color: #333;
              }
              .codigo-barras-container {
                margin: 15px 0;
                background: white;
                padding: 10px;
                border: 1px solid #ddd;
                border-radius: 4px;
              }
              .codigo-numero {
                font-family: 'Courier New', monospace;
                font-size: 14px;
                font-weight: bold;
                color: #0066cc;
                margin-bottom: 10px;
              }
              .barcode-container-visual {
                display: flex !important;
                align-items: end;
                justify-content: center;
                height: 80px;
                background: white;
                padding: 10px;
                margin: 10px 0;
                border: 1px solid #eee;
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
              }
              .barcode-bar {
                width: 2px !important;
                min-width: 2px !important;
                height: 100% !important;
                background-color: #000 !important;
                margin: 0;
                flex-shrink: 0;
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
              }
              .barcode-space {
                width: 1px !important;
                min-width: 1px !important;
                height: 100% !important;
                background-color: white !important;
                margin: 0;
                flex-shrink: 0;
              }
              .barcode-number {
                text-align: center;
                font-family: 'Courier New', monospace;
                font-size: 12px;
                font-weight: bold;
                margin-top: 5px;
                letter-spacing: 1px;
                color: #666;
              }
              .preco { 
                font-size: 20px; 
                font-weight: bold; 
                color: #d63384;
                margin: 15px 0;
              }
              .info-adicional {
                font-size: 11px;
                color: #666;
                margin-top: 12px;
                text-align: left;
                border-top: 1px solid #eee;
                padding-top: 8px;
              }
              .info-linha {
                margin: 3px 0;
                display: flex;
                justify-content: space-between;
              }
              @media print {
                * {
                  -webkit-print-color-adjust: exact !important;
                  print-color-adjust: exact !important;
                }
                body { 
                  margin: 0; 
                  padding: 10px; 
                }
                .etiqueta { 
                  border: 2px solid #000 !important; 
                  box-shadow: none; 
                }
                .barcode-container-visual {
                  display: flex !important;
                  border: 1px solid #000 !important;
                }
                .barcode-bar {
                  background-color: #000 !important;
                  width: 2px !important;
                  min-width: 2px !important;
                }
                .barcode-space {
                  background-color: white !important;
                  width: 1px !important;
                  min-width: 1px !important;
                }
              }
            </style>
          </head>
          <body>
            <div class="etiqueta">
              <div class="nome-produto">${produto.nome}</div>
              <div class="codigo-barras-container">
                <div class="codigo-numero">${codigoBarras}</div>
                ${codigoVisualHTML}
              </div>
              <div class="preco">R$ ${produto.preco?.toFixed(2) || '0,00'}</div>
              <div class="info-adicional">
                <div class="info-linha">
                  <span>Código:</span>
                  <span>${produto.codigo || 'N/A'}</span>
                </div>
                <div class="info-linha">
                  <span>Estoque:</span>
                  <span>${produto.estoque || 0} un.</span>
                </div>
                <div class="info-linha">
                  <span>Departamento:</span>  
                  <span>${produto.departamento || 'N/A'}</span>
                </div>
                <div class="info-linha">
                  <span>Data:</span>
                  <span>${new Date().toLocaleDateString('pt-BR')}</span>
                </div>
              </div>
            </div>
            <script>
              setTimeout(() => {
                window.print();
                setTimeout(() => window.close(), 1000);
              }, 500);
            </script>
          </body>
        </html>
      `);
    }
  }

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
            ${produto.departamento || ''}<br>
            Gerado em: ${new Date().toLocaleDateString('pt-BR')}
          </div>
        </div>
      </body>
      </html>
    `;
  }

  imprimirCodigoBarras(produto: Produto): void {
    const codigoBarras = produto.codigoBarras || produto.codigo?.replace(/-/g, '') || 'SEM-CODIGO';
    
    // Gerar código de barras HTML com padrões EAN13 reais
    const codigoVisualHTML = this.gerarCodigoBarrasVisualMelhorado(codigoBarras);
    
    const printWindow = window.open('', '_blank', 'width=600,height=400');
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
                background: white;
              }
              .header {
                font-size: 18px;
                font-weight: bold;
                margin-bottom: 20px;
                color: #333;
              }
              .codigo-container {
                border: 2px solid #000;
                padding: 20px;
                margin: 20px auto;
                background: white;
                max-width: 500px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
              }
              .codigo-numero {
                font-size: 20px;
                font-weight: bold;
                font-family: 'Courier New', monospace;
                margin-bottom: 15px;
                color: #0066cc;
              }
              .barcode-container-visual {
                display: flex !important;
                align-items: end;
                justify-content: center;
                height: 80px;
                background: white;
                padding: 10px;
                margin: 15px 0;
                border: 1px solid #ddd;
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
              }
              .barcode-bar {
                width: 2px !important;
                min-width: 2px !important;
                height: 100% !important;
                background-color: #000 !important;
                margin: 0;
                flex-shrink: 0;
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
              }
              .barcode-space {
                width: 1px !important;
                min-width: 1px !important;
                height: 100% !important;
                background-color: white !important;
                margin: 0;
                flex-shrink: 0;
              }
              .barcode-number {
                text-align: center;
                font-family: 'Courier New', monospace;
                font-size: 14px;
                font-weight: bold;
                margin-top: 10px;
                letter-spacing: 2px;
                color: #000;
              }
              .info {
                font-size: 14px;
                margin-top: 15px;
                color: #666;
              }
              @media print {
                * {
                  -webkit-print-color-adjust: exact !important;
                  print-color-adjust: exact !important;
                }
                body { 
                  padding: 10px; 
                  margin: 0;
                }
                .codigo-container { 
                  box-shadow: none; 
                  border: 2px solid #000 !important;
                }
                .barcode-container-visual {
                  display: flex !important;
                  border: 1px solid #000 !important;
                }
                .barcode-bar {
                  background-color: #000 !important;
                  width: 2px !important;
                  min-width: 2px !important;
                }
                .barcode-space {
                  background-color: white !important;
                  width: 1px !important;
                  min-width: 1px !important;
                }
              }
            </style>
          </head>
          <body>
            <div class="header">Código de Barras: ${produto.nome}</div>
            <div class="codigo-container">
              <div class="codigo-numero">${codigoBarras}</div>
              ${codigoVisualHTML}
              <div class="info">
                Tipo: ${produto.tipoCodigoBarras || 'EAN13'} | 
                Departamento: ${produto.departamento || 'N/A'} |
                Preço: R$ ${produto.preco?.toFixed(2) || '0,00'}
              </div>
            </div>
            <script>
              setTimeout(() => {
                window.print();
                setTimeout(() => window.close(), 1000);
              }, 500);
            </script>
          </body>
        </html>
      `);
    }
  }

  // Função para obter o nome do departamento a partir do código
  getNomeDepartamento(codigo: string | undefined): string {
    if (!codigo) {
      return 'Sem departamento';
    }
    return this.departamentos[codigo] || codigo || 'Sem departamento';
  }
}