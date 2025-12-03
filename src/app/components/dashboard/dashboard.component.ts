import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { 
  DashboardService, 
  DashboardStats,
  ProdutoBaixoEstoque, 
  VendaRecente,
  VendaSemana,
  TopProduto
} from '../../services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  stats: DashboardStats | null = null;
  
  isLoading = true;
  private destroy$ = new Subject<void>();

  produtosBaixoEstoque: ProdutoBaixoEstoque[] = [];
  vendasRecentes: VendaRecente[] = [];
  vendasSemana: VendaSemana[] = [];
  topProdutos: TopProduto[] = [];

  constructor(
    private router: Router,
    private dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.carregarDashboard();
    this.carregarVendasRecentes();
    this.carregarProdutosBaixoEstoque();
    this.carregarVendasSemana();
    this.carregarTopProdutos();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private carregarDashboard(): void {
    this.isLoading = true;

    this.dashboardService.getDashboardStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stats: DashboardStats) => {
          this.stats = {
            ...stats,
            vendasDia: stats.faturamentoDia,
            vendasMes: stats.faturamentoMes
          };
          this.isLoading = false;
        },
        error: (error: any) => {
          console.error('❌ Erro ao carregar dashboard:', error);
          this.isLoading = false;
        }
      });
  }

  recarregarDados(): void {
    this.carregarDashboard();
    this.carregarVendasRecentes();
    this.carregarProdutosBaixoEstoque();
    this.carregarVendasSemana();
    this.carregarTopProdutos();
  }

  navegarPara(rota: string): void {
    this.router.navigate([rota]);
  }

  navegarParaEstoque(): void {
    this.router.navigate(['/produtos']);
  }

  navegarParaEstoqueBaixo(): void {
    this.router.navigate(['/produtos'], { queryParams: { filtro: 'estoque-baixo' } });
  }

  formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  }

  formatarData(data: string | Date): string {
    const dataObj = typeof data === 'string' ? new Date(data) : data;
    return dataObj.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatarNumero(valor: number): string {
    return new Intl.NumberFormat('pt-BR').format(valor);
  }

  formatarDiaSemana(data: string): string {
    const dataObj = new Date(data);
    const diasSemana = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];
    return diasSemana[dataObj.getDay()];
  }

  getMaxVendaSemana(): number {
    if (this.vendasSemana.length === 0) return 100;
    return Math.max(...this.vendasSemana.map(v => v.valor));
  }

  private carregarVendasRecentes(): void {
    this.dashboardService.getVendasRecentes().subscribe({
      next: (vendas: VendaRecente[]) => {
        this.vendasRecentes = vendas.map(venda => ({
          ...venda,
          id: venda.vendaId,
          cliente: venda.nomeCliente,
          valor: venda.valorTotal,
          data: venda.dataVenda
        }));
      },
      error: (error: any) => {
        console.error('❌ Erro ao carregar vendas recentes:', error);
        this.vendasRecentes = [];
      }
    });
  }

  private carregarProdutosBaixoEstoque(): void {
    this.dashboardService.getProdutosBaixoEstoque().subscribe({
      next: (produtos: ProdutoBaixoEstoque[]) => {
        this.produtosBaixoEstoque = produtos.map(produto => ({
          ...produto,
          id: produto.produtoId,
          quantidade: produto.quantidadeAtual,
          estoque: produto.quantidadeAtual,
          quantidadeMinima: produto.estoqueMinimo,
          estoqueAtual: produto.quantidadeAtual
        }));
      },
      error: (error: any) => {
        console.error('❌ Erro ao carregar produtos baixo estoque:', error);
        this.produtosBaixoEstoque = [];
      }
    });
  }

  private carregarVendasSemana(): void {
    this.dashboardService.getVendasSemana().subscribe({
      next: (vendas: VendaSemana[]) => {
        this.vendasSemana = vendas;
      },
      error: (error: any) => {
        console.error('❌ Erro ao carregar vendas da semana:', error);
        this.vendasSemana = [];
      }
    });
  }

  private carregarTopProdutos(): void {
    this.dashboardService.getTopProdutos().subscribe({
      next: (produtos: TopProduto[]) => {
        this.topProdutos = produtos;
      },
      error: (error: any) => {
        console.error('❌ Erro ao carregar top produtos:', error);
        this.topProdutos = [];
      }
    });
  }
}