import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ProdutoService } from '../../services/produto.service';
import { MovimentacaoEstoqueService } from '../../services/movimentacao-estoque.service';
import { TipoMovimentacao, EntradaEstoqueRequest, SaidaEstoqueRequest } from '../../models/movimentacao-estoque.models';
import { FormatService } from '../../services/format.service';
import { environment } from '../../../environments/environment';

interface Produto {
  id: number;
  nome: string;
  departamento: string;
  preco: number;
  quantidade: number;
  quantidadeMinima: number;
  ativo: boolean;
  tamanhos?: TamanhoProduto[];
}

interface TamanhoProduto {
  id?: number;
  tamanho: string;
  estoque: number;
  preco?: number;
  codigoBarras?: string;
}

interface MovimentacaoEstoque {
  produtoId: number;
  tipo: 'ENTRADA' | 'SAIDA';
  quantidade: number;
  motivo: string;
  tamanhoId?: number; // Para movimentações específicas por tamanho
}

interface EstoqueStats {
  totalProdutos: number;
  produtosEstoqueBaixo: number;
  produtosSemEstoque: number;
  valorTotalEstoque: number;
}

@Component({
  selector: 'app-estoque',
  templateUrl: './estoque.component.html',
  styleUrls: ['./estoque.component.css']
})
export class EstoqueComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  produtos: Produto[] = [];
  movimentacoes: any[] = [];
  stats: EstoqueStats = {
    totalProdutos: 0,
    produtosEstoqueBaixo: 0,
    produtosSemEstoque: 0,
    valorTotalEstoque: 0
  };

  isLoading = false;
  showMovimentacaoForm = false;
  errorMessage = '';
  successMessage = '';

  produtoSelecionado: Produto | null = null;
  showTamanhosSection = false;
  tamanhosMovimentacao: { tamanho: string; quantidade: number; isNew?: boolean }[] = [];
  novoTamanho = '';

  searchTerm = '';
  selectedCategory = '';
  statusFilter = '';
  activeFilterBox = '';
  categories: string[] = [];

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

  movimentacaoForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private produtoService: ProdutoService,
    private movimentacaoService: MovimentacaoEstoqueService,
    private formatService: FormatService,
    private cdr: ChangeDetectorRef,
    private http: HttpClient
  ) {
    this.movimentacaoForm = this.fb.group({
      produtoId: ['', Validators.required],
      tipo: ['ENTRADA', Validators.required],
      quantidade: [1, [Validators.required, Validators.min(1)]],
      motivo: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  ngOnInit(): void {
    this.carregarDados();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  carregarDados(): void {
    this.isLoading = true;
    
    this.produtoService.listarProdutos({}, 0, 1000) // Carregar até 1000 produtos
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          console.log('📦 Resposta do backend:', response.content);
          this.produtos = response.content.map(p => ({
            id: p.id,
            nome: p.nome,
            departamento: p.departamento || 'Sem departamento',
            preco: p.preco,
            quantidade: this.calcularEstoqueProduto(p), // Calcular estoque corretamente
            quantidadeMinima: p.estoqueMinimo,
            ativo: p.ativo,
            tamanhos: (p as any).tamanhos || []
          }));
          
          console.log('📊 Produtos processados:', this.produtos);
          
          this.calcularStats();
          this.extrairCategorias();
          this.carregarUltimasMovimentacoes();
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Erro ao carregar produtos:', error);
          this.errorMessage = 'Erro ao carregar dados. Tente novamente.';
          this.isLoading = false;
        }
      });
  }

  /**
   * Calcula o estoque total de um produto (soma dos tamanhos ou campo quantidadeEstoque)
   */
  calcularEstoqueProduto(produto: any): number {
    if (produto.tamanhos && produto.tamanhos.length > 0) {
      return produto.tamanhos.reduce((total: number, tamanho: any) => {
        return total + (tamanho.estoque || 0);
      }, 0);
    }
    
    return produto.quantidadeEstoque || 0;
  }

  /**
   * Abre formulário de movimentação para o produto selecionado
   */
  abrirMovimentacaoProduto(produto: any): void {
    this.movimentacaoForm.patchValue({
      produtoId: produto.id,
      tipo: 'ENTRADA', // Padrão para entrada
      quantidade: 1,
      motivo: `Movimentação rápida - ${produto.nome}`
    });
    
    this.onProdutoChange();
    
    this.showMovimentacaoForm = true;
    
    this.scrollToTop();
  }

  carregarUltimasMovimentacoes(): void {
    this.movimentacaoService.listarMovimentacoes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.movimentacoes = response.content || [];
        },
        error: (error) => {
          console.error('Erro ao carregar movimentações:', error);
        }
      });
  }

  calcularStats(): void {
    this.stats = {
      totalProdutos: this.produtos.length,
      produtosEstoqueBaixo: this.produtos.filter(p => p.quantidade > 0 && p.quantidade <= p.quantidadeMinima).length,
      produtosSemEstoque: this.produtos.filter(p => p.quantidade === 0).length,
      valorTotalEstoque: this.produtos.reduce((total, p) => total + (p.quantidade * p.preco), 0)
    };
  }

  extrairCategorias(): void {
    // Usar nomes dos departamentos ao invés de códigos
    const departamentosUnicos = [...new Set(this.produtos.map(p => p.departamento))];
    this.categories = departamentosUnicos.map(codigo => this.getNomeDepartamento(codigo)).filter(nome => nome !== 'Sem departamento');
  }

  getFilteredProducts(): Produto[] {
    let filtered = [...this.produtos];

    if (this.searchTerm) {
      filtered = filtered.filter(p => 
        p.nome.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }

    if (this.selectedCategory) {
      // Filtrar por nome do departamento
      filtered = filtered.filter(p => this.getNomeDepartamento(p.departamento) === this.selectedCategory);
    }

    if (this.statusFilter) {
      switch (this.statusFilter) {
        case 'em-estoque':
          filtered = filtered.filter(p => p.quantidade > p.quantidadeMinima);
          break;
        case 'estoque-baixo':
          filtered = filtered.filter(p => p.quantidade > 0 && p.quantidade <= p.quantidadeMinima);
          break;
        case 'sem-estoque':
          filtered = filtered.filter(p => p.quantidade === 0);
          break;
      }
    }

    if (this.activeFilterBox) {
      switch (this.activeFilterBox) {
        case 'estoque-baixo':
          filtered = filtered.filter(p => p.quantidade > 0 && p.quantidade <= p.quantidadeMinima);
          break;
        case 'sem-estoque':
          filtered = filtered.filter(p => p.quantidade === 0);
          break;
      }
    }

    return filtered;
  }

  aplicarFiltroTodos(): void {
    this.activeFilterBox = this.activeFilterBox === 'todos' ? '' : 'todos';
    this.statusFilter = '';
  }

  aplicarFiltroEstoqueBaixo(): void {
    this.activeFilterBox = this.activeFilterBox === 'estoque-baixo' ? '' : 'estoque-baixo';
    this.statusFilter = '';
  }

  aplicarFiltroSemEstoque(): void {
    this.activeFilterBox = this.activeFilterBox === 'sem-estoque' ? '' : 'sem-estoque';
    this.statusFilter = '';
  }

  limparTodosFiltros(): void {
    this.searchTerm = '';
    this.selectedCategory = '';
    this.statusFilter = '';
    this.activeFilterBox = '';
  }

  isBoxActive(boxType: string): boolean {
    return this.activeFilterBox === boxType;
  }

  formatarMoeda(valor: number | null | undefined): string {
    return this.formatService.formatarMoeda(valor);
  }

  getStatusBadgeClass(produto: Produto): string {
    if (produto.quantidade === 0) return 'bg-danger';
    if (produto.quantidade <= produto.quantidadeMinima) return 'bg-warning';
    return 'bg-success';
  }

  getStatusText(produto: Produto): string {
    if (produto.quantidade === 0) return 'Sem Estoque';
    if (produto.quantidade <= produto.quantidadeMinima) return 'Estoque Baixo';
    return 'Em Estoque';
  }

  getProgressClass(produto: Produto): string {
    if (produto.quantidade === 0) return 'bg-danger';
    if (produto.quantidade <= produto.quantidadeMinima) return 'bg-warning';
    return 'bg-success';
  }

  getProgressPercentage(produto: Produto): number {
    if (produto.quantidadeMinima === 0) return 100;
    const percentage = (produto.quantidade / (produto.quantidadeMinima * 2)) * 100;
    return Math.min(Math.max(percentage, 5), 100);
  }

  
  onProdutoChange(): void {
    const produtoId = this.movimentacaoForm.get('produtoId')?.value;
    if (produtoId) {
      this.produtoSelecionado = this.produtos.find(p => p.id == produtoId) || null;
      this.showTamanhosSection = !!(this.produtoSelecionado?.tamanhos?.length);
      
      console.log('🔍 Produto selecionado:', this.produtoSelecionado);
      console.log('📏 Tamanhos encontrados:', this.produtoSelecionado?.tamanhos);
      console.log('🎯 Show tamanhos section:', this.showTamanhosSection);
      
      this.inicializarTamanhosMovimentacao();
      
      this.cdr.detectChanges();
    } else {
      this.produtoSelecionado = null;
      this.showTamanhosSection = false;
      this.tamanhosMovimentacao = [];
      this.cdr.detectChanges();
    }
  }

  inicializarTamanhosMovimentacao(): void {
    this.tamanhosMovimentacao = [];
    
    if (this.produtoSelecionado?.tamanhos && this.produtoSelecionado.tamanhos.length > 0) {
      this.tamanhosMovimentacao = this.produtoSelecionado.tamanhos.map(t => ({
        id: t.id, // ✅ Preservar o ID do tamanho
        tamanho: t.tamanho,
        quantidade: 0,
        isNew: false
      }));
      
    }
  }

  adicionarNovoTamanho(): void {
    if (this.novoTamanho.trim() && this.produtoSelecionado) {
      const jaExiste = this.tamanhosMovimentacao.some(t => 
        t.tamanho.toLowerCase() === this.novoTamanho.trim().toLowerCase()
      );
      
      if (jaExiste) {
        this.errorMessage = 'Este tamanho já existe!';
        return;
      }

      this.tamanhosMovimentacao.push({
        tamanho: this.novoTamanho.trim(),
        quantidade: 0,
        isNew: true
      });

      console.log('➕ Novo tamanho adicionado:', this.novoTamanho.trim());
      console.log('📝 Lista atualizada:', this.tamanhosMovimentacao);

      this.novoTamanho = '';
      this.clearMessages();
      this.cdr.detectChanges();
    }
  }

  podeAdicionarTamanho(): boolean {
    return !!(this.novoTamanho && this.novoTamanho.trim().length > 0);
  }

  debugTamanhos(): void {
    console.log('=== DEBUG TAMANHOS ===');
    console.log('Produto selecionado:', this.produtoSelecionado);
    console.log('Show tamanhos section:', this.showTamanhosSection);
    console.log('Tamanhos do produto:', this.produtoSelecionado?.tamanhos);
    console.log('Tamanhos movimentação:', this.tamanhosMovimentacao);
    console.log('Novo tamanho:', this.novoTamanho);
    console.log('====================');
  }

  trackByTamanho(index: number, tamanho: any): string {
    return tamanho.tamanho + (tamanho.isNew ? '_new' : '_existing');
  }

  onQuantidadeChange(valor: number, index: number): void {
    this.tamanhosMovimentacao[index].quantidade = valor;
    this.cdr.detectChanges();
  }

  removerTamanhoMovimentacao(index: number): void {
    this.tamanhosMovimentacao.splice(index, 1);
  }

  getTamanhosComQuantidade(): any[] {
    return this.tamanhosMovimentacao.filter(t => t.quantidade > 0);
  }

  isMovimentacaoValida(): boolean {
    if (this.showTamanhosSection) {
      return this.getTamanhosComQuantidade().length > 0;
    }
    return this.movimentacaoForm.get('quantidade')?.value > 0;
  }

  toggleMovimentacaoForm(): void {
    this.showMovimentacaoForm = !this.showMovimentacaoForm;
    if (!this.showMovimentacaoForm) {
      this.resetMovimentacaoForm();
    } else {
      this.scrollToTop();
    }
  }

  resetMovimentacaoForm(): void {
    this.movimentacaoForm.reset({
      tipo: 'ENTRADA',
      quantidade: 1
    });
    this.showMovimentacaoForm = false;
    this.produtoSelecionado = null;
    this.showTamanhosSection = false;
    this.tamanhosMovimentacao = [];
    this.novoTamanho = '';
  }

  onSubmitMovimentacao(): void {
    if (this.movimentacaoForm.valid && this.isMovimentacaoValida()) {
      this.isLoading = true;
      const formData = this.movimentacaoForm.value;
      
      if (this.showTamanhosSection) {
        this.processarMovimentacaoPorTamanhos(formData);
      } else {
        this.processarMovimentacaoSimples(formData);
      }
    }
  }

  processarMovimentacaoPorTamanhos(formData: any): void {
    const tamanhosComQuantidade = this.getTamanhosComQuantidade();
    
    console.log('🚀 Iniciando processamento de movimentação por tamanhos:', tamanhosComQuantidade);
    
    this.executarMovimentacoesTamanhos(formData, tamanhosComQuantidade);
  }

  executarMovimentacoesTamanhos(formData: any, tamanhos: any[]): void {
    let processados = 0;
    let erros = 0;

    console.log('🔍 Executando movimentações para tamanhos:', tamanhos);

    tamanhos.forEach(tamanho => {
      console.log('📏 Processando tamanho:', tamanho);
      
      if (tamanho.isNew && !tamanho.id) {
        console.log('➕ Criando novo tamanho com estoque:', tamanho.tamanho, 'quantidade:', tamanho.quantidade);
        this.criarTamanhoComEstoque(tamanho, parseInt(formData.produtoId)).then((tamanhoCreated: any) => {
          processados++;
          this.verificarConclucaoMovimentacao(processados, erros, tamanhos.length);
        }).catch((error: any) => {
          console.error(`Erro ao criar tamanho ${tamanho.tamanho}:`, error);
          erros++;
          this.verificarConclucaoMovimentacao(processados, erros, tamanhos.length);
        });
      } else {
        if (!tamanho.id) {
          console.error(`❌ Tamanho ${tamanho.tamanho} sem ID válido:`, tamanho);
          erros++;
          this.verificarConclucaoMovimentacao(processados, erros, tamanhos.length);
          return;
        }
        
        console.log('📦 Adicionando estoque ao tamanho existente:', tamanho.tamanho, 'ID:', tamanho.id);
        this.adicionarEstoqueAoTamanho(tamanho, parseInt(formData.produtoId)).then(() => {
          processados++;
          this.verificarConclucaoMovimentacao(processados, erros, tamanhos.length);
        }).catch((error: any) => {
          console.error(`Erro ao adicionar estoque ao tamanho ${tamanho.tamanho}:`, error);
          erros++;
          this.verificarConclucaoMovimentacao(processados, erros, tamanhos.length);
        });
      }
    });
  }

  processarMovimentacaoSimples(formData: any): void {
    if (formData.tipo === 'ENTRADA') {
      const entradaRequest: EntradaEstoqueRequest = {
        produtoId: parseInt(formData.produtoId),
        quantidade: formData.quantidade,
        motivo: formData.motivo,
        observacoes: formData.observacoes || ''
      };

      this.movimentacaoService.registrarEntrada(entradaRequest)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.successMessage = 'Entrada registrada com sucesso!';
            this.resetMovimentacaoForm();
            this.carregarDados();
            this.isLoading = false;
          },
          error: (error) => {
            console.error('Erro ao registrar entrada:', error);
            this.errorMessage = 'Erro ao registrar entrada. Tente novamente.';
            this.isLoading = false;
          }
        });
    } else if (formData.tipo === 'SAIDA') {
      const saidaRequest: SaidaEstoqueRequest = {
        produtoId: parseInt(formData.produtoId),
        quantidade: formData.quantidade,
        motivo: formData.motivo,
        observacoes: formData.observacoes || ''
      };

      this.movimentacaoService.registrarSaida(saidaRequest)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.successMessage = 'Saída registrada com sucesso!';
            this.resetMovimentacaoForm();
            this.carregarDados();
            this.isLoading = false;
          },
          error: (error) => {
            console.error('Erro ao registrar saída:', error);
            this.errorMessage = 'Erro ao registrar saída. Tente novamente.';
            this.isLoading = false;
          }
        });
    }
  }

  verificarConclucaoMovimentacao(processados: number, erros: number, total: number): void {
    if (processados + erros === total) {
      this.isLoading = false;
      
      if (erros === 0) {
        this.successMessage = `Movimentação registrada com sucesso para ${processados} tamanho(s)!`;
      } else {
        this.errorMessage = `Movimentação concluída com ${erros} erro(s). ${processados} tamanho(s) processado(s) com sucesso.`;
      }
      
      this.resetMovimentacaoForm();
      this.carregarDados();
    }
  }

  getErrorMessage(field: string): string {
    const control = this.movimentacaoForm.get(field);
    if (control?.errors) {
      if (control.errors['required']) return 'Este campo é obrigatório';
      if (control.errors['min']) return 'Valor deve ser maior que zero';
      if (control.errors['minlength']) return 'Mínimo de 3 caracteres';
    }
    return '';
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  /**
   * Rola a página para o topo suavemente
   */
  scrollToTop(): void {
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }

  /**
   * Cria um novo tamanho com estoque inicial
   */
  async criarTamanhoComEstoque(tamanho: any, produtoId: number): Promise<any> {
    const request = {
      tamanho: tamanho.tamanho,
      estoque: tamanho.quantidade, // Usar a quantidade informada pelo usuário
      preco: 0, // Valor padrão
      codigoBarras: '' // Valor padrão
    };

    console.log('📤 Enviando request para criar tamanho:', request);

    const url = `${environment.apiUrl}/produtos/${produtoId}/tamanhos`;
    
    return this.http.post(url, request).toPromise();
  }

  /**
   * Adiciona estoque a um tamanho existente
   */
  async adicionarEstoqueAoTamanho(tamanho: any, produtoId: number): Promise<any> {
    const request = {
      quantidade: tamanho.quantidade
    };

    const url = `${environment.apiUrl}/produtos/${produtoId}/tamanhos/${tamanho.id}/estoque/adicionar`;
    
    return this.http.post(url, request).toPromise();
  }

  // Função para obter o nome do departamento a partir do código
  getNomeDepartamento(codigo: string | undefined): string {
    if (!codigo) {
      return 'Sem departamento';
    }
    return this.departamentos[codigo] || codigo || 'Sem departamento';
  }
}