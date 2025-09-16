import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ProdutoService, Produto } from '../../services/produto.service';

interface MovimentacaoEstoque {
  id?: number;
  produtoId: number;
  produto?: Produto;
  tipo: 'ENTRADA' | 'SAIDA';
  quantidade: number;
  motivo: string;
  dataMovimentacao: Date;
  usuario?: string;
}

@Component({
  selector: 'app-estoque',
  templateUrl: './estoque.component.html',
  styleUrls: ['./estoque.component.css']
})
export class EstoqueComponent implements OnInit {
  produtos: Produto[] = [];
  movimentacoes: MovimentacaoEstoque[] = [];
  movimentacaoForm: FormGroup;
  
  isLoading = false;
  showMovimentacaoForm = false;
  errorMessage = '';
  successMessage = '';
  
  // Filtros
  searchTerm = '';
  selectedCategory = '';
  statusFilter = '';
  categories = ['Roupas', 'Acessórios', 'Calçados', 'Eletrônicos', 'Casa', 'Outros'];
  activeFilterBox = ''; // Para controlar qual box está ativo
  
  // Estatísticas
  stats = {
    totalProdutos: 0,
    produtosEstoqueBaixo: 0,
    produtosSemEstoque: 0,
    valorTotalEstoque: 0
  };

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private produtoService: ProdutoService
  ) {
    this.movimentacaoForm = this.createMovimentacaoForm();
  }

  ngOnInit(): void {
    this.carregarDados();
    
    // Verificar se há filtro nos query parameters
    this.route.queryParams.subscribe(params => {
      if (params['filtro']) {
        this.aplicarFiltroFromRoute(params['filtro']);
      }
    });
  }

  createMovimentacaoForm(): FormGroup {
    return this.fb.group({
      produtoId: ['', [Validators.required]],
      tipo: ['ENTRADA', [Validators.required]],
      // Aumentou validação mínima de 1 para 1 (evita quantidades zero ou negativas)
      quantidade: [1, [Validators.required, Validators.min(1)]],
      motivo: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  carregarDados(): void {
    this.isLoading = true;
    this.produtoService.listarProdutos().subscribe({
      next: (response: any) => {
        this.produtos = response.content || response;
        this.calcularEstatisticas();
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Erro ao carregar produtos:', error);
        this.errorMessage = 'Erro ao carregar dados dos produtos';
        this.isLoading = false;
      }
    });
  }

  calcularEstatisticas(): void {
    this.stats.totalProdutos = this.produtos.length;
    this.stats.produtosEstoqueBaixo = this.produtos.filter(p => 
      p.quantidade != null && p.quantidade > 0 && p.quantidade <= 10 // Considerando 10 como estoque mínimo padrão
    ).length;
    this.stats.produtosSemEstoque = this.produtos.filter(p => p.quantidade === 0).length;
    this.stats.valorTotalEstoque = this.produtos.reduce((total, p) => 
      total + ((p.quantidade || 0) * p.preco), 0
    );
  }

  onSubmitMovimentacao(): void {
    if (this.movimentacaoForm.valid) {
      this.isLoading = true;
      const movimentacao = this.movimentacaoForm.value;
      
      // Usar ProdutoService para atualizar estoque
      this.produtoService.atualizarEstoque(
        Number(movimentacao.produtoId), 
        movimentacao.quantidade
      ).subscribe({
        next: (resultado: any) => {
          this.successMessage = 'Movimentação registrada com sucesso!';
          this.carregarDados(); // Recarregar dados
          this.resetMovimentacaoForm();
          this.isLoading = false;
        },
        error: (error: any) => {
          this.errorMessage = 'Erro ao registrar movimentação';
          this.isLoading = false;
          console.error('Erro:', error);
        }
      });
    }
  }

  resetMovimentacaoForm(): void {
    this.movimentacaoForm.reset({
      tipo: 'ENTRADA',
      quantidade: 1
    });
    this.showMovimentacaoForm = false;
  }

  toggleMovimentacaoForm(): void {
    this.showMovimentacaoForm = !this.showMovimentacaoForm;
    if (!this.showMovimentacaoForm) {
      this.resetMovimentacaoForm();
    }
  }

  getFilteredProducts(): Produto[] {
    return this.produtos.filter(produto => {
      const matchesSearch = produto.nome.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchesCategory = !this.selectedCategory || produto.categoria === this.selectedCategory;
      
      let matchesStatus = true;
      const estoqueMinimo = 10; // Valor padrão para estoque mínimo
      if (this.statusFilter === 'sem-estoque') {
        matchesStatus = produto.quantidade === 0;
      } else if (this.statusFilter === 'estoque-baixo') {
        matchesStatus = (produto.quantidade ?? 0) > 0 && (produto.quantidade ?? 0) <= estoqueMinimo;
      } else if (this.statusFilter === 'em-estoque') {
        matchesStatus = (produto.quantidade ?? 0) > estoqueMinimo;
      }
      
      return matchesSearch && matchesCategory && matchesStatus;
    });
  }

  getStatusBadgeClass(produto: Produto): string {
    const estoqueMinimo = 10; // Valor padrão para estoque mínimo
    if (produto.quantidade === 0) return 'bg-danger';
    if ((produto.quantidade ?? 0) <= estoqueMinimo) return 'bg-warning text-dark';
    return 'bg-success';
  }

  getStatusText(produto: Produto): string {
    const estoqueMinimo = 10; // Valor padrão para estoque mínimo
    if (produto.quantidade === 0) return 'SEM ESTOQUE';
    if ((produto.quantidade ?? 0) <= estoqueMinimo) return 'ESTOQUE BAIXO';
    return 'EM ESTOQUE';
  }

  getProgressPercentage(produto: Produto): number {
    const estoqueMinimo = 10; // Valor padrão para estoque mínimo
    const max = Math.max(estoqueMinimo * 2, produto.quantidade ?? 0);
    return Math.min(((produto.quantidade ?? 0) / max) * 100, 100);
  }

  getProgressClass(produto: Produto): string {
    const estoqueMinimo = 10; // Valor padrão para estoque mínimo
    if (produto.quantidade === 0) return 'bg-danger';
    if ((produto.quantidade ?? 0) <= estoqueMinimo) return 'bg-warning';
    return 'bg-success';
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
    const control = this.movimentacaoForm.get(field);
    if (control?.errors && control.touched) {
      if (control.errors['required']) return `${field} é obrigatório`;
      if (control.errors['min']) {
        if (field === 'quantidade') {
          return 'Quantidade deve ser maior que zero';
        }
        return `${field} deve ser maior que ${control.errors['min'].min}`;
      }
      if (control.errors['minlength']) return `${field} deve ter pelo menos ${control.errors['minlength'].requiredLength} caracteres`;
    }
    return '';
  }

  // Métodos para os botões de Entrada e Saída
  abrirMovimentacaoRapida(produto: Produto, tipo: 'ENTRADA' | 'SAIDA'): void {
    // Preencher o formulário com os dados do produto selecionado
    this.movimentacaoForm.patchValue({
      produtoId: produto.id,
      tipo: tipo,
      quantidade: 1,
      motivo: tipo === 'ENTRADA' ? 'Entrada rápida' : 'Saída rápida'
    });
    
    // Mostrar o formulário
    this.showMovimentacaoForm = true;
    
    // Scroll suave para o formulário
    setTimeout(() => {
      const formElement = document.querySelector('.card-custom.shadow-custom');
      if (formElement) {
        formElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 100);
  }

  entradaRapida(produto: Produto): void {
    this.abrirMovimentacaoRapida(produto, 'ENTRADA');
  }

  saidaRapida(produto: Produto): void {
    this.abrirMovimentacaoRapida(produto, 'SAIDA');
  }

  // Métodos para filtros por boxes
  aplicarFiltroTodos(): void {
    if (this.activeFilterBox === 'todos') {
      // Se já está ativo, desativa o filtro
      this.activeFilterBox = '';
      this.statusFilter = '';
    } else {
      // Ativa o filtro para todos os produtos
      this.activeFilterBox = 'todos';
      this.statusFilter = '';
    }
  }

  aplicarFiltroEstoqueBaixo(): void {
    if (this.activeFilterBox === 'estoque-baixo') {
      // Se já está ativo, desativa o filtro
      this.activeFilterBox = '';
      this.statusFilter = '';
    } else {
      // Ativa o filtro para estoque baixo
      this.activeFilterBox = 'estoque-baixo';
      this.statusFilter = 'estoque-baixo';
    }
  }

  aplicarFiltroSemEstoque(): void {
    if (this.activeFilterBox === 'sem-estoque') {
      // Se já está ativo, desativa o filtro
      this.activeFilterBox = '';
      this.statusFilter = '';
    } else {
      // Ativa o filtro para sem estoque
      this.activeFilterBox = 'sem-estoque';
      this.statusFilter = 'sem-estoque';
    }
  }

  // Método auxiliar para verificar se um box está ativo
  isBoxActive(filterType: string): boolean {
    return this.activeFilterBox === filterType;
  }

  // Método para limpar todos os filtros
  limparTodosFiltros(): void {
    this.searchTerm = '';
    this.selectedCategory = '';
    this.statusFilter = '';
    this.activeFilterBox = '';
  }

  // Método para aplicar filtro vindo da navegação
  aplicarFiltroFromRoute(filtro: string): void {
    switch (filtro) {
      case 'estoque-baixo':
        this.aplicarFiltroEstoqueBaixo();
        break;
      case 'sem-estoque':
        this.aplicarFiltroSemEstoque();
        break;
      case 'todos':
        this.aplicarFiltroTodos();
        break;
    }
  }
}
