import { Component, OnInit } from '@angular/core';
import { ProdutoService, ProdutoResponse } from '../../services/produto.service';
import { DevolucaoService } from '../../services/devolucao.service';
import { AuthService } from '../../services/auth.service';

interface ProdutoDefeituoso {
  produtoId: number;
  nomeProduto: string;
  codigoBarras?: string;
  quantidadeDefeituosa: number;
  precoOriginal: number;
  precoDefeituoso: number;
  percentualDesconto: number;
  valorEstimadoDesconto: number;
  dataUltimaAtualizacao: string;
}

interface ProdutoDefeitosoAPI {
  produtoId: number;
  nomeProduto: string;
  quantidadeDefeituosa: number;
  valorEstimadoDesconto: number;
}

@Component({
  selector: 'app-produtos-defeituosos',
  templateUrl: './produtos-defeituosos.component.html',
  styleUrls: ['./produtos-defeituosos.component.css']
})
export class ProdutosDefeituososComponent implements OnInit {

  produtosDefeituosos: ProdutoDefeituoso[] = [];
  produtoSelecionado: ProdutoResponse | null = null;
  
  // Estados de UI
  isLoading = false;
  showModalGerenciar = false;
  showModalPromocao = false;
  
  // Mensagens
  successMessage = '';
  errorMessage = '';
  
  // Filtros
  searchTerm = '';
  orderBy = 'nome';
  orderDirection = 'asc';
  
  // Formulário de gestão
  novoPrecoDefeituoso = 0;
  novoPercentualDesconto = 30;
  observacoes = '';
  
  // Estatísticas
  stats = {
    totalProdutosDefeituosos: 0,
    valorTotalEstoque: 0,
    valorTotalComDesconto: 0,
    economiaTotal: 0
  };

  constructor(
    private produtoService: ProdutoService,
    private devolucaoService: DevolucaoService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.carregarProdutosDefeituosos();
  }

  carregarProdutosDefeituosos(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.devolucaoService.listarProdutosComDefeito().subscribe({
      next: (produtosAPI: ProdutoDefeitosoAPI[]) => {
        // Converter dados da API para o formato do componente
        this.produtosDefeituosos = produtosAPI.map(produto => ({
          produtoId: produto.produtoId,
          nomeProduto: produto.nomeProduto,
          codigoBarras: undefined, // Será carregado quando necessário
          quantidadeDefeituosa: produto.quantidadeDefeituosa,
          precoOriginal: 0, // Será carregado quando necessário
          precoDefeituoso: 0, // Calculado com base no desconto
          percentualDesconto: 30, // Padrão de 30%
          valorEstimadoDesconto: produto.valorEstimadoDesconto,
          dataUltimaAtualizacao: new Date().toLocaleDateString('pt-BR')
        }));
        this.calcularEstatisticas();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erro ao carregar produtos defeituosos:', error);
        this.errorMessage = 'Erro ao carregar produtos defeituosos. Tente novamente.';
        this.isLoading = false;
      }
    });
  }

  calcularEstatisticas(): void {
    this.stats = {
      totalProdutosDefeituosos: this.produtosDefeituosos.length,
      valorTotalEstoque: this.produtosDefeituosos.reduce((sum, p) => sum + (p.precoOriginal * p.quantidadeDefeituosa), 0),
      valorTotalComDesconto: this.produtosDefeituosos.reduce((sum, p) => sum + (p.precoDefeituoso * p.quantidadeDefeituosa), 0),
      economiaTotal: 0
    };
    
    this.stats.economiaTotal = this.stats.valorTotalEstoque - this.stats.valorTotalComDesconto;
  }

  get produtosFiltrados(): ProdutoDefeituoso[] {
    let produtos = [...this.produtosDefeituosos];

    // Filtro por texto
    if (this.searchTerm.trim()) {
      const termo = this.searchTerm.toLowerCase();
      produtos = produtos.filter(p => 
        p.nomeProduto.toLowerCase().includes(termo) ||
        (p.codigoBarras && p.codigoBarras.toLowerCase().includes(termo))
      );
    }

    // Ordenação
    produtos.sort((a, b) => {
      let valueA: any, valueB: any;
      
      switch (this.orderBy) {
        case 'nome':
          valueA = a.nomeProduto.toLowerCase();
          valueB = b.nomeProduto.toLowerCase();
          break;
        case 'quantidade':
          valueA = a.quantidadeDefeituosa;
          valueB = b.quantidadeDefeituosa;
          break;
        case 'desconto':
          valueA = a.percentualDesconto;
          valueB = b.percentualDesconto;
          break;
        case 'valor':
          valueA = a.valorEstimadoDesconto;
          valueB = b.valorEstimadoDesconto;
          break;
        default:
          valueA = a.nomeProduto.toLowerCase();
          valueB = b.nomeProduto.toLowerCase();
      }

      if (valueA < valueB) return this.orderDirection === 'asc' ? -1 : 1;
      if (valueA > valueB) return this.orderDirection === 'asc' ? 1 : -1;
      return 0;
    });

    return produtos;
  }

  abrirModalGerenciar(produto: ProdutoDefeituoso): void {
    this.produtoService.buscarPorId(produto.produtoId).subscribe({
      next: (produtoCompleto) => {
        this.produtoSelecionado = produtoCompleto;
        this.novoPrecoDefeituoso = produto.precoDefeituoso;
        this.novoPercentualDesconto = produto.percentualDesconto;
        this.observacoes = '';
        this.showModalGerenciar = true;
      },
      error: (error) => {
        console.error('Erro ao buscar produto:', error);
        this.errorMessage = 'Erro ao carregar detalhes do produto.';
      }
    });
  }

  calcularPrecoComDesconto(): void {
    if (this.produtoSelecionado) {
      const precoOriginal = this.produtoSelecionado.preco;
      const desconto = this.novoPercentualDesconto / 100;
      this.novoPrecoDefeituoso = precoOriginal * (1 - desconto);
    }
  }

  calcularPercentualDesconto(): void {
    if (this.produtoSelecionado && this.novoPrecoDefeituoso > 0) {
      const precoOriginal = this.produtoSelecionado.preco;
      this.novoPercentualDesconto = ((precoOriginal - this.novoPrecoDefeituoso) / precoOriginal) * 100;
    }
  }

  atualizarProdutoDefeituoso(): void {
    if (!this.produtoSelecionado) return;

    this.isLoading = true;
    this.errorMessage = '';

    const dadosAtualizacao = {
      precoDefeituoso: this.novoPrecoDefeituoso,
      percentualDesconto: this.novoPercentualDesconto,
      observacoes: this.observacoes
    };

    // Simulação da atualização - implementar endpoint no backend
    setTimeout(() => {
      this.successMessage = `Produto ${this.produtoSelecionado?.nome} atualizado com sucesso!`;
      this.fecharModalGerenciar();
      this.carregarProdutosDefeituosos();
      this.isLoading = false;
      
      setTimeout(() => {
        this.successMessage = '';
      }, 5000);
    }, 1000);
  }

  marcarComoNormal(produto: ProdutoDefeituoso): void {
    if (confirm(`Deseja marcar o produto "${produto.nomeProduto}" como normal? Isso removerá o status defeituoso.`)) {
      this.isLoading = true;
      
      // Simulação - implementar endpoint no backend
      setTimeout(() => {
        this.successMessage = `Produto ${produto.nomeProduto} foi marcado como normal.`;
        this.carregarProdutosDefeituosos();
        this.isLoading = false;
        
        setTimeout(() => {
          this.successMessage = '';
        }, 5000);
      }, 1000);
    }
  }

  criarPromocao(produto: ProdutoDefeituoso): void {
    // Funcionalidade futura - criar promoção específica para produtos defeituosos
    this.successMessage = `Promoção criada para ${produto.nomeProduto}! (Funcionalidade em desenvolvimento)`;
    setTimeout(() => {
      this.successMessage = '';
    }, 3000);
  }

  fecharModalGerenciar(): void {
    this.showModalGerenciar = false;
    this.produtoSelecionado = null;
    this.observacoes = '';
  }

  fecharModalPromocao(): void {
    this.showModalPromocao = false;
  }

  exportarRelatorio(): void {
    // Implementar exportação de relatório
    this.successMessage = 'Relatório exportado com sucesso! (Funcionalidade em desenvolvimento)';
    setTimeout(() => {
      this.successMessage = '';
    }, 3000);
  }

  formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  }

  formatarPercentual(valor: number): string {
    return `${valor.toFixed(1)}%`;
  }

  trackByProdutoId(index: number, produto: ProdutoDefeituoso): number {
    return produto.produtoId;
  }
}