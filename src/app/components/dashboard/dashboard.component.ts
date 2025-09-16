import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import { ProdutoService, Produto } from '../../services/produto.service';
import { DashboardService } from '../../services/dashboard.service';
import { VendaService } from '../../services/venda.service';
import { ClienteService } from '../../services/cliente.service';

Chart.register(...registerables);

interface DashboardData {
  vendasHoje: { quantidade: number; valor: number };
  vendasMes: { quantidade: number; valor: number };
  vendasAno: { quantidade: number; valor: number };
  produtosBaixoEstoque: number;
  clientesAtivos: number;
  ticketMedio: number;
}

interface VendaSemana {
  dia: string;
  vendas: number;
  valor: number;
}

interface TopProduto {
  id: number;
  nome: string;
  categoria: string;
  quantidadeVendida: number;
  valorTotal: number;
  percentualVendas: number;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  // Propriedades para os dados
  dashboardData: DashboardData | null = null;
  vendasSemana: VendaSemana[] = [];
  topProdutos: TopProduto[] = [];
  produtosBaixoEstoque: Produto[] = [];
  vendasRecentes: any[] = [];
  isLoading = true;

  // Propriedade para compatibilidade com o template
  stats: any = null;

  // Dados para gráficos
  chartVendasSemana: any = null;
  chartTopProdutos: any = null;

  private refreshIntervalId: any;
  periodo: 'hoje'|'semana'|'mes' = 'mes';

  constructor(
    private router: Router,
    private produtoService: ProdutoService,
    private dashboardService: DashboardService,
    private clienteService: ClienteService,
    private vendaService: VendaService
  ) {}

  ngOnInit(): void {
    this.carregarDashboard();

    // Atualizar automaticamente a cada 30s
    this.refreshIntervalId = setInterval(() => {
      this.carregarDashboard();
    }, 30000);
  }

  ngOnDestroy(): void {
    if (this.refreshIntervalId) {
      clearInterval(this.refreshIntervalId);
    }
  }

  carregarDashboard(): void {
    console.log('Iniciando carregamento do dashboard...');
    this.isLoading = true;

    // Obter estatísticas do dashboard (usa mock quando local)
    this.dashboardService.obterEstatisticas(this.periodo).subscribe({
      next: (data: any) => {
        this.stats = data;
        // Mapear para a interface local usada pelo template
        this.dashboardData = {
          vendasHoje: { quantidade: 0, valor: data.vendasHoje?.valor || 0 },
          vendasMes: { quantidade: 0, valor: data.vendasMes?.valor || 0 },
          vendasAno: { quantidade: 0, valor: data.vendasAno || 0 },
          produtosBaixoEstoque: data.produtosBaixoEstoque || 0,
          clientesAtivos: data.clientesAtivos || 0,
          ticketMedio: data.ticketMedio || 0
        };

        // animar cards — simples transição de números
        this.animateCardValue('vendasMes', 0, this.dashboardData.vendasMes.valor, 800);
        this.animateCardValue('vendasHoje', 0, this.dashboardData.vendasHoje.valor, 800);

        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao obter estatísticas do dashboard', err);
        this.isLoading = false;
      }
    });

    // Carregar vendas recentes
    this.dashboardService.obterVendasSemanais().subscribe({
      next: (vendas: any) => {
        this.vendasSemana = vendas.map((v: any) => ({
          dia: new Date(v.data).toLocaleDateString('pt-BR', { weekday: 'short' }),
          vendas: 1,
          valor: v.vendas
        }));
        this.setupChartVendasSemana();
      },
      error: (error: any) => {
        console.error('Erro ao carregar vendas semanais', error);
      }
    });

    // Carregar produtos mais vendidos
    this.dashboardService.obterTopProdutos(5).subscribe({
      next: (produtos: any) => {
        this.topProdutos = produtos.map((produto: any) => ({
          id: produto.produtoId,
          nome: produto.produtoNome,
          categoria: produto.categoria,
          quantidadeVendida: produto.quantidadeVendida,
          valorTotal: produto.valorTotal,
          percentualVendas: produto.percentualVendas
        }));
        this.setupChartTopProdutos();
      },
      error: (error: any) => {
        console.error('Erro ao carregar top produtos', error);
      }
    });

    // Carregar produtos com baixo estoque
    this.produtoService.buscarProdutosBaixoEstoque().subscribe({
      next: (produtos) => {
        // Pegar apenas os primeiros 5 produtos
        this.produtosBaixoEstoque = produtos.slice(0, 5).map(produto => ({
          id: produto.id || 0,
          nome: produto.nome,
          categoria: produto.categoria,
          preco: produto.preco,
          estoque: produto.quantidade || produto.estoque || 0,
          estoqueMinimo: produto.estoqueMinimo || produto.quantidadeMinima || 5
        } as Produto));
        console.log(`✅ ${this.produtosBaixoEstoque.length} produtos com baixo estoque carregados`);
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('❌ Erro ao carregar produtos baixo estoque:', error);
        this.isLoading = false;
      }
    });

    // Carregar vendas recentes do backend real
    this.vendaService.obterVendasRecentes(5).subscribe({
      next: (vendas: any[]) => {
        this.vendasRecentes = vendas.map(venda => ({
          id: venda.id || 0,
          cliente: (venda as any).clienteNome || 'Cliente Avulso',
          valor: (venda as any).valorTotal,
          data: (venda as any).dataVenda || new Date(),
          status: (venda as any).status || 'PENDENTE'
        }));
        console.log(`✅ ${this.vendasRecentes.length} vendas recentes carregadas do backend`);
      },
      error: (err: any) => {
        console.error('❌ Erro ao carregar vendas recentes:', err);
        this.vendasRecentes = [];
      }
    });
  }

  // Simples animação de números para os cards
  private animateCardValue(key: string, start: number, end: number, duration: number = 800) {
    const startTime = performance.now();
    const animate = (now: number) => {
      const elapsed = now - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const current = start + (end - start) * progress;
      // atribui valor formatado se for vendas
      if (key === 'vendasMes') {
        // Atualiza stats temporariamente para efeito
        if (this.stats) this.stats.vendasMes = { valor: current };
      }
      if (progress < 1) {
        requestAnimationFrame(animate);
      } else {
        if (this.stats) this.stats.vendasMes = { valor: end };
      }
    };
    requestAnimationFrame(animate);
  }

  setupChartVendasSemana(): void {
    const ctx = document.getElementById('chartVendasSemana') as HTMLCanvasElement;
    if (!ctx) return;

    if (this.chartVendasSemana) {
      this.chartVendasSemana.destroy();
    }

    this.chartVendasSemana = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.vendasSemana.map(v => v.dia),
        datasets: [
          {
            label: 'Vendas',
            data: this.vendasSemana.map(v => v.vendas),
            borderColor: '#007bff',
            backgroundColor: 'rgba(0, 123, 255, 0.1)',
            tension: 0.4
          },
          {
            label: 'Receita (R$ mil)',
            data: this.vendasSemana.map(v => v.valor / 1000), // Dividir por 1000 para melhor visualização
            borderColor: '#28a745',
            backgroundColor: 'rgba(40, 167, 69, 0.1)',
            tension: 0.4
          }
        ]
      },
      options: {
        responsive: true,
        plugins: {
          title: {
            display: true,
            text: 'Vendas da Semana'
          }
        }
      }
    });
  }

  setupChartTopProdutos(): void {
    const ctx = document.getElementById('chartTopProdutos') as HTMLCanvasElement;
    if (!ctx) return;

    if (this.chartTopProdutos) {
      this.chartTopProdutos.destroy();
    }

    this.chartTopProdutos = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.topProdutos.map(p => p.nome),
        datasets: [{
          label: 'Quantidade Vendida',
          data: this.topProdutos.map(p => p.quantidadeVendida),
          backgroundColor: [
            '#FF6384',
            '#36A2EB',
            '#FFCE56',
            '#4BC0C0',
            '#9966FF'
          ]
        }]
      },
      options: {
        responsive: true,
        plugins: {
          title: {
            display: true,
            text: 'Top 5 Produtos Mais Vendidos'
          }
        }
      }
    });
  }

  navegarPara(rota: string): void {
    this.router.navigate([rota]);
  }

  recarregarDados(): void {
    this.carregarDashboard();
  }

  navegarParaEstoque(): void {
    this.router.navigate(['/estoque']);
  }

  navegarParaEstoqueBaixo(): void {
    this.router.navigate(['/estoque'], { queryParams: { filtro: 'estoque-baixo' } });
  }

  formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  }
}