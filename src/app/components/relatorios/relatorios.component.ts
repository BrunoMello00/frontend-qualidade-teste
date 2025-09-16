import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { jsPDF } from 'jspdf';
import { VendaService, Venda as VendaBackend, EstatisticasVenda, TopProdutoResponse } from '../../services/venda.service';
import { ProdutoService, Produto as ProdutoBackend } from '../../services/produto.service';
import { Chart, registerables } from 'chart.js';
import { DashboardService } from '../../services/dashboard.service';
import { EventosService } from '../../services/eventos.service';


// Interfaces temporárias até implementar o backend real
interface VendaMes {
  id: number;
  mes: string;
  vendas: number;
  faturamento: number;
}

interface Produto {
  id: number;
  nome: string;
  categoria: string;
  preco: number;
  vendidosMes?: number;
  estoque?: number;
}

// Interface local da Venda com evento (similar ao vendas.component.ts)
interface Venda {
  id?: number;
  dataVenda: Date;
  clienteNome: string;
  clienteEmail?: string;
  clienteTelefone?: string;
  evento?: string;
  itens: ItemVenda[];
  subtotal: number;
  desconto: number;
  percentualDesconto?: number;
  total: number;
  formaPagamento: string;
  status: 'PENDENTE' | 'CONFIRMADA' | 'ENTREGUE' | 'CANCELADA';
  observacoes?: string;
}

interface ItemVenda {
  produtoId: number;
  produtoNome: string;
  categoria: string;
  quantidade: number;
  precoUnitario: number;
  subtotal: number;
}

// Declaração para extensão do jsPDF
declare module 'jspdf' {
  interface jsPDF {
    autoTable: (options: any) => jsPDF;
  }
}

// Importar autoTable e aplicar ao jsPDF
import autoTable from 'jspdf-autotable';

interface RelatorioVendas {
  periodo: string;
  totalVendas: number;
  faturamento: number;
  ticketMedio: number;
  crescimento: number;
}

interface RelatorioProdutos {
  produtoId: number;
  produtoNome: string;
  categoria: string;
  quantidadeVendida: number;
  faturamento: number;
  margem: number;
}

interface RelatorioEstoque {
  produtoId: number;
  produtoNome: string;
  categoria: string;
  estoqueAtual: number;
  estoqueMinimo: number;
  valorEstoque: number;
  status: string;
}

@Component({
  selector: 'app-relatorios',
  templateUrl: './relatorios.component.html',
  styleUrls: ['./relatorios.component.css']
})
export class RelatoriosComponent implements OnInit {
  
  // Estados
  isLoading = false;
  isLoadingPrint = false;
  activeTab = 'vendas';
  tipoRelatorio = 'vendas'; // Nova propriedade para o tipo de relatório
  
  // Formulários
  filtroForm!: FormGroup;
  
  // Dados dos relatórios
  vendas: Venda[] = [];
  vendasMes: VendaMes[] = [];
  produtos: Produto[] = [];
  vendasPorCategoria: any[] = [];
  estoqueTotal: any = {};
  relatorioVendas: RelatorioVendas[] = [];
  relatorioProdutos: RelatorioProdutos[] = [];
  relatorioEstoque: RelatorioEstoque[] = [];
  
  // Estatísticas gerais
  estatisticasGerais = {
    totalVendasPeriodo: 0,
    faturamentoPeriodo: 0,
    crescimentoVendas: 0,
    produtoMaisVendido: '',
    categoriaTopVendas: '',
    ticketMedio: 0,
    produtosBaixoEstoque: 0,
    valorTotalEstoque: 0
  };

  // Opções de filtro
  periodosDisponiveis = [
    { value: 'hoje', label: 'Hoje' },
    { value: 'ontem', label: 'Ontem' },
    { value: 'semana', label: 'Esta Semana' },
    { value: 'semana-passada', label: 'Semana Passada' },
    { value: 'mes', label: 'Este Mês' },
    { value: 'mes-passado', label: 'Mês Passado' },
    { value: 'trimestre', label: 'Este Trimestre' },
    { value: 'ano', label: 'Este Ano' },
    { value: 'personalizado', label: 'Período Personalizado' }
  ];

  categorias = [
    'Eletrônicos',
    'Informática',
    'Acessórios',
    'Móveis',
    'Casa e Jardim'
  ];

  // Lista de eventos para filtros
  eventosDisponiveis: string[] = [];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private vendaService: VendaService,
    private produtoService: ProdutoService,
    private dashboardService: DashboardService,
    private eventosService: EventosService
  ) {
    this.initializeForms();
    // registrar componentes do Chart.js (se ainda não registrado)
    try { Chart.register(...registerables); } catch(e) { /* ignore se já registrado */ }
  }

  ngOnInit(): void {
    this.loadRelatorios();
  }

  initializeForms(): void {
    this.filtroForm = this.fb.group({
      periodo: ['mes'],
      dataInicio: [''],
      dataFim: [''],
      categoria: [''],
      evento: [''], // Novo filtro por evento
      agruparPor: ['dia'],
      // Controles específicos de produtos
      criterioPerformance: ['vendas'], // vendas, receita, margem
      limiteResultados: [10],
      // Controles específicos de estoque
      statusEstoque: ['todos'], // todos, normal, baixo, zerado
      valorMinimo: [''],
      ordenarPor: ['alfabetico'] // alfabetico, estoque, valor
    });

    // Observar mudanças no período
    this.filtroForm.get('periodo')?.valueChanges.subscribe((value: string) => {
      if (value !== 'personalizado') {
        this.filtroForm.patchValue({
          dataInicio: '',
          dataFim: ''
        });
      }
    });
  }

  async loadRelatorios(): Promise<void> {
    try {
      this.isLoading = true;
      

      await Promise.all([
        this.loadVendasIndividuais(),
        this.loadDadosVendas(),
        this.loadDadosProdutos(),
        this.loadDadosEstoque(),
        this.loadEstatisticasGerais(),
        this.loadEventosDisponiveis()
      ]);
      
    } catch (error) {
      console.error('❌ Erro ao carregar relatórios:', error);
    } finally {
      this.isLoading = false;
    }
  }

  private async loadVendasIndividuais(): Promise<void> {
    try {
      console.log('📊 Carregando vendas individuais do backend...');
      
      // Obter filtros do formulário
      const filtros = this.filtroForm.value;
      let dataInicio: Date | undefined;
      let dataFim: Date | undefined;
      
      // Configurar período baseado no filtro
      if (filtros.periodo && filtros.periodo !== 'todos') {
        const agora = new Date();
        switch (filtros.periodo) {
          case 'hoje':
            dataInicio = new Date(agora.getFullYear(), agora.getMonth(), agora.getDate());
            break;
          case 'ontem':
            const ontem = new Date(agora);
            ontem.setDate(ontem.getDate() - 1);
            dataInicio = new Date(ontem.getFullYear(), ontem.getMonth(), ontem.getDate());
            dataFim = new Date(ontem.getFullYear(), ontem.getMonth(), ontem.getDate(), 23, 59, 59);
            break;
          case 'semana':
            const inicioSemana = new Date(agora);
            inicioSemana.setDate(agora.getDate() - agora.getDay());
            dataInicio = new Date(inicioSemana.getFullYear(), inicioSemana.getMonth(), inicioSemana.getDate());
            break;
          case 'semana-passada':
            const inicioSemanaPassada = new Date(agora);
            inicioSemanaPassada.setDate(agora.getDate() - agora.getDay() - 7);
            const fimSemanaPassada = new Date(inicioSemanaPassada);
            fimSemanaPassada.setDate(inicioSemanaPassada.getDate() + 6);
            dataInicio = new Date(inicioSemanaPassada.getFullYear(), inicioSemanaPassada.getMonth(), inicioSemanaPassada.getDate());
            dataFim = new Date(fimSemanaPassada.getFullYear(), fimSemanaPassada.getMonth(), fimSemanaPassada.getDate(), 23, 59, 59);
            break;
          case 'mes':
            dataInicio = new Date(agora.getFullYear(), agora.getMonth(), 1);
            break;
          case 'mes-passado':
            const inicioMesPassado = new Date(agora.getFullYear(), agora.getMonth() - 1, 1);
            const fimMesPassado = new Date(agora.getFullYear(), agora.getMonth(), 0);
            dataInicio = inicioMesPassado;
            dataFim = new Date(fimMesPassado.getFullYear(), fimMesPassado.getMonth(), fimMesPassado.getDate(), 23, 59, 59);
            break;
          case 'trimestre':
            const trimestreAtual = Math.floor(agora.getMonth() / 3);
            dataInicio = new Date(agora.getFullYear(), trimestreAtual * 3, 1);
            break;
          case 'ano':
            dataInicio = new Date(agora.getFullYear(), 0, 1);
            break;
        }
      }
      
      // Usar datas personalizadas se definidas
      if (filtros.dataInicio) {
        dataInicio = new Date(filtros.dataInicio);
      }
      if (filtros.dataFim) {
        dataFim = new Date(filtros.dataFim);
      }
      
      // Buscar vendas do backend com paginação extendida para relatórios
      const vendasResponse = await this.vendaService.listarVendas(0, 1000, dataInicio, dataFim).toPromise();
      
      if (vendasResponse && vendasResponse.content) {
        // Converter vendas do backend para formato do componente (uso de any para tolerância de tipos)
        const vendasAny: any = vendasResponse;
        this.vendas = (vendasAny.content || []).map((venda: any) => ({
          id: venda.id,
          dataVenda: venda.dataVenda ? new Date(venda.dataVenda) : new Date(),
          clienteNome: venda.clienteNome || (venda.cliente && venda.cliente.nome) || 'Cliente Avulso',
          clienteEmail: venda.clienteEmail || (venda.cliente && venda.cliente.email) || null,
          clienteTelefone: venda.clienteTelefone || (venda.cliente && venda.cliente.telefone) || null,
          evento: (typeof venda.evento === 'string') ? venda.evento : (venda.evento && venda.evento.nome) || '',
          itens: (venda.itens || []).map((item: any) => ({
            produtoId: item.produtoId,
            produtoNome: item.nomeProduto || item.produtoNome || (item.produto && item.produto.nome) || 'Produto',
            categoria: item.categoria || (item.produto && item.produto.categoria) || 'Sem categoria',
            quantidade: item.quantidade,
            precoUnitario: item.precoUnitario || item.preco || 0,
            subtotal: (item.precoUnitario || item.preco || 0) * (item.quantidade || 0)
          })),
          subtotal: (venda.subtotal !== undefined ? venda.subtotal : (venda.valorTotal !== undefined ? venda.valorTotal - (venda.desconto || 0) : 0)),
          desconto: venda.desconto || 0,
          percentualDesconto: venda.percentualDesconto || 0,
          total: venda.total || venda.valorTotal || 0,
          formaPagamento: venda.formaPagamento || 'PIX',
          status: venda.status || 'PENDENTE',
          observacoes: venda.observacoes || ''
        } as any));
        
        console.log(`✅ ${this.vendas.length} vendas carregadas do backend`);
      } else {
        this.vendas = [];
        console.log('⚠️ Nenhuma venda encontrada no período');
      }
    } catch (error) {
      console.error('❌ Erro ao carregar vendas individuais:', error);
      this.vendas = [];
    }
  }

  private async loadDadosVendas(): Promise<void> {
    try {
      console.log('📊 Carregando dados de vendas do backend...');
      
      // Obter estatísticas gerais de vendas
      const estatisticas = await this.vendaService.obterEstatisticasVendas().toPromise();
      
      if (estatisticas) {
        // Processar dados por mês (simulando agrupamento por período)
        const agora = new Date();
        const vendasMes: VendaMes[] = [];
        
        // Criar dados dos últimos 12 meses baseado nas estatísticas
        for (let i = 11; i >= 0; i--) {
          const data = new Date(agora.getFullYear(), agora.getMonth() - i, 1);
          const mesNome = data.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
          
          // Para o mês atual, usar dados reais, para outros meses simular distribuição
          if (i === 0) {
            vendasMes.push({
              id: 12 - i,
              mes: mesNome,
              vendas: estatisticas.vendasMes,
              faturamento: estatisticas.faturamentoMes
            });
          } else {
            // Simular dados históricos baseados nos dados atuais
            const fatorVariacao = 0.7 + (Math.random() * 0.6); // Variação entre 70% e 130%
            vendasMes.push({
              id: 12 - i,
              mes: mesNome,
              vendas: Math.round(estatisticas.vendasMes * fatorVariacao),
              faturamento: estatisticas.faturamentoMes * fatorVariacao
            });
          }
        }
        
        this.vendasMes = vendasMes;
  // Montar gráfico de vendas mensais
  setTimeout(() => this.setupChartVendasMes(), 50);
        
        // Criar relatório de vendas estruturado
        this.relatorioVendas = [
          {
            periodo: 'Hoje',
            totalVendas: estatisticas.vendasDia,
            faturamento: estatisticas.faturamentoDia,
            ticketMedio: estatisticas.vendasDia > 0 ? estatisticas.faturamentoDia / estatisticas.vendasDia : 0,
            crescimento: 0 // TODO: Calcular crescimento comparando com ontem
          },
          {
            periodo: 'Este Mês',
            totalVendas: estatisticas.vendasMes,
            faturamento: estatisticas.faturamentoMes,
            ticketMedio: estatisticas.ticketMedio,
            crescimento: 0 // TODO: Calcular crescimento comparando com mês anterior
          },
          {
            periodo: 'Este Ano',
            totalVendas: estatisticas.vendasAno,
            faturamento: estatisticas.faturamentoAno,
            ticketMedio: estatisticas.vendasAno > 0 ? estatisticas.faturamentoAno / estatisticas.vendasAno : 0,
            crescimento: 0 // TODO: Calcular crescimento comparando com ano anterior
          }
        ];
        
        console.log(`✅ Dados de vendas carregados: ${this.relatorioVendas.length} períodos`);
      } else {
        this.vendasMes = [];
        this.relatorioVendas = [];
        console.log('⚠️ Nenhuma estatística de vendas encontrada');
      }
    } catch (error) {
      console.error('❌ Erro ao carregar dados de vendas:', error);
      this.vendasMes = [];
      this.relatorioVendas = [];
    }
  }

  setupChartVendasMes(): void {
    try {
      const ctx = document.getElementById('chartVendasMes') as HTMLCanvasElement;
      if (!ctx) return;

      const labels = this.vendasMes.map(v => v.mes);
      const dataVendas = this.vendasMes.map(v => v.vendas);
      const dataFaturamento = this.vendasMes.map(v => v.faturamento);

      // Remover gráfico existente se houver
      // @ts-ignore - propriedade privada gerenciando instancia se existir
      if ((this as any)._chartVendasMes) { (this as any)._chartVendasMes.destroy(); }

      // Criar novo gráfico usando Chart importado
      (this as any)._chartVendasMes = new Chart(ctx, {
        type: 'line',
        data: {
          labels,
          datasets: [
            { label: 'Vendas', data: dataVendas, borderColor: '#007bff', backgroundColor: 'rgba(0,123,255,0.1)', tension: 0.4 },
            { label: 'Faturamento (R$)', data: dataFaturamento, borderColor: '#28a745', backgroundColor: 'rgba(40,167,69,0.1)', tension: 0.4, yAxisID: 'y1' }
          ]
        },
        options: {
          responsive: true,
          scales: {
            y: { position: 'left' },
            y1: { position: 'right', grid: { drawOnChartArea: false } }
          },
          plugins: { title: { display: true, text: 'Vendas Mensais' } }
        }
      });
    } catch (e) {
      console.error('Erro ao montar gráfico de vendas:', e);
    }
  }

  private async loadDadosProdutos(): Promise<void> {
    try {
      console.log('📊 Carregando dados de produtos do backend...');
      
      // Buscar produtos e top produtos em paralelo
      const [produtos, topProdutos] = await Promise.all([
        this.produtoService.listarProdutos().toPromise(),
        this.vendaService.obterTopProdutos().toPromise()
      ]);
      
      if (produtos) {
        // Converter produtos do backend para formato do componente
        this.produtos = produtos.map(produto => ({
          id: produto.id || 0,
          nome: produto.nome,
          categoria: produto.categoria || 'Sem categoria',
          preco: produto.preco,
          vendidosMes: 0, // Será preenchido com dados dos top produtos
          estoque: produto.quantidade || produto.estoque || 0
        }));
        
        console.log(`✅ ${this.produtos.length} produtos carregados`);
      }
      
      if (topProdutos) {
        // Criar relatório de produtos baseado nos top produtos
        this.relatorioProdutos = topProdutos.map((item: any) => ({
          produtoId: item.produtoId,
          produtoNome: item.produtoNome,
          categoria: item.categoria || 'Outros',
          quantidadeVendida: item.quantidadeVendida,
          faturamento: item.valorTotal,
          margem: item.percentualVendas || 0 // Usar percentual como margem temporariamente
        }));
        
        // Atualizar vendidosMes nos produtos baseado nos top produtos
        this.produtos.forEach(produto => {
          const topProduto = topProdutos.find(tp => tp.produtoId === produto.id);
          if (topProduto) {
            produto.vendidosMes = topProduto.quantidadeVendida;
          }
        });
        
        console.log(`✅ ${this.relatorioProdutos.length} produtos no relatório de performance`);
      } else {
        this.relatorioProdutos = [];
        console.log('⚠️ Nenhum dado de top produtos encontrado');
      }
    } catch (error) {
      console.error('❌ Erro ao carregar dados de produtos:', error);
      this.produtos = [];
      this.relatorioProdutos = [];
    }
  }

  private async loadDadosEstoque(): Promise<void> {
    try {
      console.log('📊 Carregando dados de estoque do backend...');
      
      // Buscar produtos para dados de estoque
      const produtos = await this.produtoService.listarProdutos().toPromise();
      
      if (produtos) {
        // Criar relatório de estoque
        this.relatorioEstoque = produtos.map(produto => {
          const estoqueAtual = produto.quantidade || produto.estoque || 0;
          const estoqueMinimo = produto.estoqueMinimo || produto.quantidadeMinima || 5;
          const valorEstoque = estoqueAtual * produto.preco;
          
          let status = 'NORMAL';
          if (estoqueAtual === 0) {
            status = 'ZERADO';
          } else if (estoqueAtual <= estoqueMinimo) {
            status = 'BAIXO';
          }
          
          return {
            produtoId: produto.id || 0,
            produtoNome: produto.nome,
            categoria: produto.categoria || 'Sem categoria',
            estoqueAtual: estoqueAtual,
            estoqueMinimo: estoqueMinimo,
            valorEstoque: valorEstoque,
            status: status
          };
        });
        
        console.log(`✅ ${this.relatorioEstoque.length} produtos no relatório de estoque`);
      } else {
        this.relatorioEstoque = [];
        console.log('⚠️ Nenhum produto encontrado para relatório de estoque');
      }
    } catch (error) {
      console.error('❌ Erro ao carregar dados de estoque:', error);
      this.relatorioEstoque = [];
    }
  }

  private async loadEstatisticasGerais(): Promise<void> {
    try {
      console.log('📊 Carregando estatísticas gerais do backend...');
      
      // Buscar dados de múltiplas fontes em paralelo
      const [estatisticasVendas, topProdutos, produtos] = await Promise.all([
        this.vendaService.obterEstatisticasVendas().toPromise(),
        // chamar com limite 1 para obter o produto mais vendido
        this.vendaService.obterTopProdutos(1).toPromise(),
        this.produtoService.listarProdutos().toPromise()
      ]);
      
      // Processar estatísticas de vendas
      if (estatisticasVendas) {
        this.estatisticasGerais.totalVendasPeriodo = estatisticasVendas.vendasMes;
        this.estatisticasGerais.faturamentoPeriodo = estatisticasVendas.faturamentoMes;
        this.estatisticasGerais.ticketMedio = estatisticasVendas.ticketMedio;
        this.estatisticasGerais.crescimentoVendas = 0; // TODO: Calcular crescimento real
      }
      
      // Processar produto mais vendido
      if (topProdutos && topProdutos.length > 0) {
        this.estatisticasGerais.produtoMaisVendido = topProdutos[0].produtoNome || '';

        // Determinar categoria top vendas
        const categoriaTop = topProdutos[0].categoria || '';
        this.estatisticasGerais.categoriaTopVendas = categoriaTop;
        
        // Criar dados de vendas por categoria
        const categorias = new Map<string, number>();
        topProdutos.forEach((produto: any) => {
          const categoria = produto.categoria || 'Outros';
          categorias.set(categoria, (categorias.get(categoria) || 0) + (produto.valorTotal || 0));
        });
        
        this.vendasPorCategoria = Array.from(categorias.entries()).map(([categoria, valor]) => ({
          categoria,
          valor,
          quantidade: topProdutos.filter(p => p.categoria === categoria).length
        }));
      } else {
        this.estatisticasGerais.produtoMaisVendido = 'N/A';
        this.estatisticasGerais.categoriaTopVendas = 'N/A';
        this.vendasPorCategoria = [];
      }
      
      // Processar dados de estoque
      if (produtos) {
        let valorTotalEstoque = 0;
        let produtosBaixoEstoque = 0;
        
        produtos.forEach(produto => {
          const estoque = produto.quantidade || produto.estoque || 0;
          const estoqueMinimo = produto.estoqueMinimo || produto.quantidadeMinima || 5;
          
          valorTotalEstoque += estoque * produto.preco;
          
          if (estoque <= estoqueMinimo) {
            produtosBaixoEstoque++;
          }
        });
        
        this.estatisticasGerais.valorTotalEstoque = valorTotalEstoque;
        this.estatisticasGerais.produtosBaixoEstoque = produtosBaixoEstoque;
        
        // Configurar estoqueTotal para compatibilidade
        this.estoqueTotal = { valorEstoque: valorTotalEstoque };
      } else {
        this.estatisticasGerais.valorTotalEstoque = 0;
        this.estatisticasGerais.produtosBaixoEstoque = 0;
        this.estoqueTotal = { valorEstoque: 0 };
      }
      
      console.log('✅ Estatísticas gerais carregadas:', this.estatisticasGerais);
    } catch (error) {
      console.error('❌ Erro ao carregar estatísticas gerais:', error);
      
      // Inicializar com valores padrão em caso de erro
      this.estoqueTotal = { valorEstoque: 0 };
      this.vendasPorCategoria = [];
      this.estatisticasGerais = {
        totalVendasPeriodo: 0,
        faturamentoPeriodo: 0,
        crescimentoVendas: 0,
        produtoMaisVendido: 'N/A',
        categoriaTopVendas: 'N/A',
        ticketMedio: 0,
        produtosBaixoEstoque: 0,
        valorTotalEstoque: 0
      };
    }
  }

  private async loadEventosDisponiveis(): Promise<void> {
    try {
      console.log('📊 Carregando eventos disponíveis...');
      this.eventosDisponiveis = await this.getEventosUnicos();
      console.log(`✅ ${this.eventosDisponiveis.length} eventos carregados para filtros`);
    } catch (error) {
      console.error('❌ Erro ao carregar eventos disponíveis:', error);
      this.eventosDisponiveis = [];
    }
  }

  onFiltroChange(): void {
    this.loadRelatorios();
  }

  limparFiltros(): void {
    // Resetar o formulário para os valores padrão
    this.filtroForm.patchValue({
      periodo: 'mes',
      dataInicio: '',
      dataFim: '',
      categoria: '',
      agruparPor: 'dia'
    });
    
    // Recarregar os relatórios com filtros limpos
    this.loadRelatorios();
    
    // Mostrar feedback visual
    this.showToast('Filtros removidos e dados recarregados!', 'success');
    
    console.log('Filtros limpos e dados recarregados');
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

  onTipoRelatorioChange(tipo: string): void {
    this.tipoRelatorio = tipo;
    this.activeTab = tipo;
    this.atualizarFiltrosEspecificos(tipo);
    this.loadRelatorios();
  }

  private atualizarFiltrosEspecificos(tipo: string): void {
    // Resetar formulário de filtros
    this.filtroForm.patchValue({
      categoria: '',
      evento: '',
      agruparPor: 'dia'
    });

    // Atualizar filtros específicos baseado no tipo
    if (tipo === 'vendas') {
      // Filtros específicos para vendas já estão configurados
    } else if (tipo === 'estoque') {
      // Para estoque, focar em categoria e status
      this.filtroForm.patchValue({
        agruparPor: 'categoria'
      });
    } else if (tipo === 'produtos') {
      // Para produtos, focar em categoria e performance
      this.filtroForm.patchValue({
        agruparPor: 'categoria'
      });
    }
  }

  exportarRelatorio(formato: string): void {
    try {
      console.log(`Exportando relatório em ${formato}`);
      
      if (formato === 'pdf') {
        this.exportarPDF();
      } else if (formato === 'excel') {
        this.exportarExcel();
      } else if (formato === 'csv') {
        this.exportarCSV();
      }
    } catch (error) {
      console.error('Erro ao exportar relatório:', error);
      alert('Erro ao exportar relatório. Tente novamente.');
    }
  }

  // Novos métodos para obter dados filtrados
  private getDadosFiltrados(): any[] {
    const filtros = this.filtroForm.value;
    
    if (this.activeTab === 'vendas') {
      return this.aplicarFiltrosVendas(this.relatorioVendas, filtros);
    } else if (this.activeTab === 'produtos') {
      return this.aplicarFiltrosProdutos(this.relatorioProdutos, filtros);
    } else if (this.activeTab === 'estoque') {
      return this.aplicarFiltrosEstoque(this.produtos, filtros);
    }
    
    return [];
  }

  private aplicarFiltrosVendas(dados: any[], filtros: any): any[] {
    let dadosFiltrados = [...dados];
    
    // Filtro por período
    if (filtros.periodo && filtros.periodo !== '') {
      const agora = new Date();
      let dataInicio: Date;
      
      switch (filtros.periodo) {
        case 'hoje':
          dataInicio = new Date(agora.getFullYear(), agora.getMonth(), agora.getDate());
          break;
        case 'semana':
          dataInicio = new Date(agora.getTime() - 7 * 24 * 60 * 60 * 1000);
          break;
        case 'mes':
          dataInicio = new Date(agora.getFullYear(), agora.getMonth(), 1);
          break;
        case 'trimestre':
          dataInicio = new Date(agora.getFullYear(), agora.getMonth() - 3, 1);
          break;
        case 'ano':
          dataInicio = new Date(agora.getFullYear(), 0, 1);
          break;
        default:
          dataInicio = new Date(0);
      }
      
      // Filtrar dados por período baseado no tipo de dados
      if (filtros.periodo !== 'todos') {
        dadosFiltrados = dadosFiltrados.filter(item => {
          // Para dados de relatório de vendas, verificar se está no período
          if (item.periodo) {
            // Se o item tem período específico (Hoje, Este Mês, Este Ano), incluir apenas relevantes
            const periodo = item.periodo.toLowerCase();
            const filtro = filtros.periodo.toLowerCase();
            
            if (filtro === 'hoje' && periodo.includes('hoje')) return true;
            if (filtro === 'mes' && periodo.includes('mês')) return true;
            if (filtro === 'ano' && periodo.includes('ano')) return true;
            
            // Para outros períodos, incluir todos por enquanto
            return true;
          }
          return true;
        });
      }
    }
    
    // Filtro por data específica
    if (filtros.dataInicio) {
      const dataInicio = new Date(filtros.dataInicio);
      dadosFiltrados = dadosFiltrados.filter(item => {
        const itemData = new Date(item.periodo);
        return itemData >= dataInicio;
      });
    }
    
    if (filtros.dataFim) {
      const dataFim = new Date(filtros.dataFim);
      dadosFiltrados = dadosFiltrados.filter(item => {
        const itemData = new Date(item.periodo);
        return itemData <= dataFim;
      });
    }
    
    return dadosFiltrados;
  }

  private aplicarFiltrosProdutos(dados: any[], filtros: any): any[] {
    let dadosFiltrados = [...dados];
    
    // Filtro por categoria
    if (filtros.categoria && filtros.categoria !== '') {
      dadosFiltrados = dadosFiltrados.filter(item => 
        item.categoria.toLowerCase().includes(filtros.categoria.toLowerCase())
      );
    }
    
    // Filtro por critério de performance
    if (filtros.criterioPerformance && filtros.criterioPerformance.length > 0) {
      // Ordenar por critério selecionado
      if (filtros.criterioPerformance.includes('vendas')) {
        dadosFiltrados.sort((a, b) => b.quantidadeVendida - a.quantidadeVendida);
      } else if (filtros.criterioPerformance.includes('receita')) {
        dadosFiltrados.sort((a, b) => b.faturamento - a.faturamento);
      }
    }
    
    // Limitar resultados
    if (filtros.limiteResultados && filtros.limiteResultados > 0) {
      dadosFiltrados = dadosFiltrados.slice(0, filtros.limiteResultados);
    }
    
    return dadosFiltrados;
  }

  private aplicarFiltrosEstoque(dados: any[], filtros: any): any[] {
    let dadosFiltrados = [...dados];
    
    // Filtro por categoria
    if (filtros.categoria && filtros.categoria !== '') {
      dadosFiltrados = dadosFiltrados.filter(item => 
        item.categoria.toLowerCase().includes(filtros.categoria.toLowerCase())
      );
    }
    
    // Filtro por status do estoque
    if (filtros.statusEstoque && filtros.statusEstoque !== '') {
      dadosFiltrados = dadosFiltrados.filter(item => {
        switch (filtros.statusEstoque) {
          case 'baixo':
            return item.quantidade <= item.quantidadeMinima;
          case 'normal':
            return item.quantidade > item.quantidadeMinima && item.quantidade > 0;
          case 'zerado':
            return item.quantidade === 0;
          default:
            return true;
        }
      });
    }
    
    // Filtro por preço mínimo
    if (filtros.precoMinimo && filtros.precoMinimo > 0) {
      dadosFiltrados = dadosFiltrados.filter(item => item.preco >= filtros.precoMinimo);
    }
    
    // Ordenação
    if (filtros.ordenarPor) {
      switch (filtros.ordenarPor) {
        case 'nome':
          dadosFiltrados.sort((a, b) => a.nome.localeCompare(b.nome));
          break;
        case 'preco':
          dadosFiltrados.sort((a, b) => b.preco - a.preco);
          break;
        case 'quantidade':
          dadosFiltrados.sort((a, b) => b.quantidade - a.quantidade);
          break;
      }
    }
    
    return dadosFiltrados;
  }

  private temFiltrosAtivos(): boolean {
    const filtros = this.filtroForm.value;
    
    // Verificar se algum filtro foi aplicado além dos valores padrão
    const filtrosAtivos = [
      filtros.periodo && filtros.periodo !== 'mes',
      filtros.dataInicio && filtros.dataInicio !== '',
      filtros.dataFim && filtros.dataFim !== '',
      filtros.categoria && filtros.categoria !== '',
      filtros.evento && filtros.evento !== '',
      filtros.statusEstoque && filtros.statusEstoque !== '',
      filtros.precoMinimo && filtros.precoMinimo > 0,
      filtros.criterioPerformance && filtros.criterioPerformance.length > 0,
      filtros.limiteResultados && filtros.limiteResultados !== 50
    ];
    
    return filtrosAtivos.some(filtro => filtro);
  }

  private exportarPDF(): void {
    try {
      // Criar nova instância do jsPDF
      const doc = new jsPDF();
      
      // Verificar se autoTable está disponível
      if (typeof autoTable !== 'function') {
        console.error('autoTable não está disponível');
        this.exportarPDFSimples();
        return;
      }
      
      // Configurar fontes e cores
      const primaryColor = [255, 107, 53]; // Cor laranja principal
      const grayColor = [108, 117, 125];
      
      // Cabeçalho do documento
      doc.setFontSize(20);
      doc.setTextColor(primaryColor[0], primaryColor[1], primaryColor[2]);
      doc.text('Sistema de Estoque e Vendas', 20, 25);
      
      doc.setFontSize(16);
      doc.setTextColor(grayColor[0], grayColor[1], grayColor[2]);
      const tituloAba = this.activeTab === 'vendas' ? 'Relatório de Vendas' :
                       this.activeTab === 'produtos' ? 'Relatório de Produtos' :
                       'Relatório de Estoque';
      doc.text(tituloAba, 20, 35);
      
      // Data de geração
      doc.setFontSize(10);
      const dataGeracao = `Gerado em: ${new Date().toLocaleDateString('pt-BR')} às ${new Date().toLocaleTimeString('pt-BR')}`;
      doc.text(dataGeracao, 20, 45);
      
      // Linha separadora
      doc.setDrawColor(primaryColor[0], primaryColor[1], primaryColor[2]);
      doc.setLineWidth(0.5);
      doc.line(20, 50, 190, 50);
      
      // Seção de estatísticas
      let yPos = 60;
      doc.setFontSize(12);
      doc.setTextColor(0, 0, 0);
      doc.text('Resumo Executivo', 20, yPos);
      
      yPos += 10;
      doc.setFontSize(10);
      
      // Estatísticas em colunas
      const stats = [
        [`Total de Vendas: ${this.estatisticasGerais.totalVendasPeriodo}`, `Faturamento: ${this.formatarMoeda(this.estatisticasGerais.faturamentoPeriodo)}`],
        [`Ticket Médio: ${this.formatarMoeda(this.estatisticasGerais.ticketMedio)}`, `Produtos em Falta: ${this.estatisticasGerais.produtosBaixoEstoque}`]
      ];
      
      stats.forEach((linha, index) => {
        doc.text(linha[0], 20, yPos + (index * 8));
        doc.text(linha[1], 110, yPos + (index * 8));
      });
      
      yPos += 25;
      
      // Gerar tabela baseada na aba ativa
      this.gerarTabelaPDF(doc, yPos);
      
      // Salvar o arquivo
      const nomeArquivo = `relatorio-${this.activeTab}-${new Date().toISOString().split('T')[0]}.pdf`;
      doc.save(nomeArquivo);
      
      this.showToast('Relatório PDF gerado com sucesso!', 'success');
      
    } catch (error) {
      console.error('Erro ao gerar PDF:', error);
      this.showToast('Erro ao gerar PDF. Tentando método alternativo...', 'error');
      // Fallback para método simples
      this.exportarPDFSimples();
    }
  }

  private exportarPDFSimples(): void {
    try {
      // Método alternativo sem tabelas complexas
      const doc = new jsPDF();
      
      // Cabeçalho
      doc.setFontSize(20);
      doc.setTextColor(13, 110, 253);
      doc.text('Sistema de Estoque e Vendas', 20, 25);
      
      doc.setFontSize(16);
      doc.setTextColor(108, 117, 125);
      const tituloAba = this.activeTab === 'vendas' ? 'Relatório de Vendas' :
                       this.activeTab === 'produtos' ? 'Relatório de Produtos' :
                       'Relatório de Estoque';
      doc.text(tituloAba, 20, 35);
      
      // Data
      doc.setFontSize(10);
      doc.setTextColor(0, 0, 0);
      const dataGeracao = `Gerado em: ${new Date().toLocaleDateString('pt-BR')} às ${new Date().toLocaleTimeString('pt-BR')}`;
      doc.text(dataGeracao, 20, 45);
      
      // Linha
      doc.setDrawColor(13, 110, 253);
      doc.line(20, 50, 190, 50);
      
      // Dados em formato texto simples
      let yPos = 60;
      doc.setFontSize(12);
      doc.text('Resumo dos Dados:', 20, yPos);
      
      yPos += 15;
      doc.setFontSize(10);
      
      if (this.activeTab === 'vendas') {
        this.relatorioVendas.forEach((item, index) => {
          if (yPos > 250) { // Nova página se necessário
            doc.addPage();
            yPos = 30;
          }
          doc.text(`${index + 1}. ${this.formatarData(item.periodo)} - ${item.totalVendas} vendas - ${this.formatarMoeda(item.faturamento)}`, 20, yPos);
          yPos += 8;
        });
      } else if (this.activeTab === 'produtos') {
        this.relatorioProdutos.forEach((item, index) => {
          if (yPos > 250) {
            doc.addPage();
            yPos = 30;
          }
          doc.text(`${index + 1}. ${item.produtoNome} - ${item.quantidadeVendida} vendidos - ${this.formatarMoeda(item.faturamento)}`, 20, yPos);
          yPos += 8;
        });
      } else {
        this.relatorioEstoque.forEach((item, index) => {
          if (yPos > 250) {
            doc.addPage();
            yPos = 30;
          }
          doc.text(`${index + 1}. ${item.produtoNome} - Estoque: ${item.estoqueAtual} - Status: ${this.getStatusText(item.status)}`, 20, yPos);
          yPos += 8;
        });
      }
      
      // Salvar
      const nomeArquivo = `relatorio-${this.activeTab}-${new Date().toISOString().split('T')[0]}.pdf`;
      doc.save(nomeArquivo);
      
      this.showToast('Relatório PDF (versão simples) gerado com sucesso!', 'success');
      
    } catch (error) {
      console.error('Erro no método alternativo:', error);
      this.showToast('Erro ao gerar PDF. Tente novamente.', 'error');
    }
  }

  private gerarTabelaPDF(doc: jsPDF, startY: number): void {
    // Obter dados filtrados ao invés de usar todos os dados
    const dadosFiltrados = this.getDadosFiltrados();
    
    if (this.activeTab === 'vendas') {
      // Tabela de vendas com dados filtrados
      const headers = [['Período', 'Total Vendas', 'Faturamento', 'Ticket Médio', 'Crescimento']];
      const data = dadosFiltrados.map(item => [
        this.formatarData(item.periodo),
        item.totalVendas.toString(),
        this.formatarMoeda(item.faturamento),
        this.formatarMoeda(item.ticketMedio),
        this.formatarPercentual(item.crescimento)
      ]);
      
      // Adicionar informação sobre filtros aplicados
      if (this.temFiltrosAtivos()) {
        doc.setFontSize(10);
        doc.setTextColor(255, 107, 53); // Cor laranja
        doc.text('📋 Dados filtrados conforme critérios selecionados', 20, startY - 5);
        startY += 5;
      }
      
      autoTable(doc, {
        head: headers,
        body: data,
        startY: startY,
        styles: {
          fontSize: 10,
          cellPadding: 3,
        },
        headStyles: {
          fillColor: [13, 110, 253],
          textColor: [255, 255, 255],
          fontStyle: 'bold'
        },
        alternateRowStyles: {
          fillColor: [248, 249, 250]
        },
        margin: { left: 20, right: 20 }
      });
      
    } else if (this.activeTab === 'produtos') {
      // Tabela de produtos com dados filtrados
      const headers = [['Produto', 'Categoria', 'Qtd Vendida', 'Faturamento', 'Margem']];
      const data = dadosFiltrados.map(item => [
        item.produtoNome,
        item.categoria,
        item.quantidadeVendida.toString(),
        this.formatarMoeda(item.faturamento),
        this.formatarPercentual(item.margem)
      ]);
      
      // Adicionar informação sobre filtros aplicados
      if (this.temFiltrosAtivos()) {
        doc.setFontSize(10);
        doc.setTextColor(255, 107, 53); // Cor laranja
        doc.text('📋 Dados filtrados conforme critérios selecionados', 20, startY - 5);
        startY += 5;
      }
      
      autoTable(doc, {
        head: headers,
        body: data,
        startY: startY,
        styles: {
          fontSize: 10,
          cellPadding: 3,
        },
        headStyles: {
          fillColor: [13, 110, 253],
          textColor: [255, 255, 255],
          fontStyle: 'bold'
        },
        alternateRowStyles: {
          fillColor: [248, 249, 250]
        },
        margin: { left: 20, right: 20 }
      });
      
    } else if (this.activeTab === 'estoque') {
      // Tabela de estoque com dados filtrados
      const headers = [['Produto', 'Categoria', 'Est. Atual', 'Est. Mínimo', 'Preço', 'Status']];
      const data = dadosFiltrados.map(item => [
        item.nome,
        item.categoria,
        item.quantidade.toString(),
        item.quantidadeMinima.toString(),
        this.formatarMoeda(item.preco),
        this.getStatusEstoque(item.quantidade, item.quantidadeMinima)
      ]);
      
      // Adicionar informação sobre filtros aplicados
      if (this.temFiltrosAtivos()) {
        doc.setFontSize(10);
        doc.setTextColor(255, 107, 53); // Cor laranja
        doc.text('📋 Dados filtrados conforme critérios selecionados', 20, startY - 5);
        startY += 5;
      }
      
      autoTable(doc, {
        head: headers,
        body: data,
        startY: startY,
        styles: {
          fontSize: 10,
          cellPadding: 3,
        },
        headStyles: {
          fillColor: [13, 110, 253],
          textColor: [255, 255, 255],
          fontStyle: 'bold'
        },
        alternateRowStyles: {
          fillColor: [248, 249, 250]
        },
        margin: { left: 20, right: 20 },
        didParseCell: (data: any) => {
          // Colorir status de estoque
          if (data.column.index === 5 && data.section === 'body') {
            const status = data.cell.text[0];
            if (status === 'Sem Estoque') {
              data.cell.styles.textColor = [220, 53, 69]; // text-danger
            } else if (status === 'Estoque Baixo') {
              data.cell.styles.textColor = [255, 193, 7]; // text-warning
            } else {
              data.cell.styles.textColor = [40, 167, 69]; // text-success
            }
          }
        }
      });
    }
  }

  private getStatusEstoque(quantidade: number, quantidadeMinima: number): string {
    if (quantidade === 0) {
      return 'Sem Estoque';
    } else if (quantidade <= quantidadeMinima) {
      return 'Estoque Baixo';
    } else {
      return 'Normal';
    }
  }

  private exportarExcel(): void {
    try {
      let dados: any[] = [];
      let filename = '';
      
      // Obter dados filtrados
      const dadosFiltrados = this.getDadosFiltrados();
      
      if (this.activeTab === 'vendas') {
        dados = dadosFiltrados.map(item => ({
          'Período': this.formatarData(item.periodo),
          'Total Vendas': item.totalVendas,
          'Faturamento': `R$ ${item.faturamento.toFixed(2).replace('.', ',')}`,
          'Ticket Médio': `R$ ${item.ticketMedio.toFixed(2).replace('.', ',')}`,
          'Crescimento (%)': `${item.crescimento.toFixed(1)}%`
        }));
        filename = 'relatorio-vendas';
      } else if (this.activeTab === 'produtos') {
        dados = dadosFiltrados.map(item => ({
          'Produto': item.produtoNome,
          'Categoria': item.categoria,
          'Quantidade Vendida': item.quantidadeVendida,
          'Faturamento': `R$ ${item.faturamento.toFixed(2).replace('.', ',')}`,
          'Margem (%)': `${item.margem.toFixed(1)}%`
        }));
        filename = 'relatorio-produtos';
      } else if (this.activeTab === 'estoque') {
        dados = dadosFiltrados.map(item => ({
          'Produto': item.nome,
          'Categoria': item.categoria,
          'Estoque Atual': item.quantidade,
          'Estoque Mínimo': item.quantidadeMinima,
          'Preço Unitário': `R$ ${item.preco.toFixed(2).replace('.', ',')}`,
          'Status': this.getStatusEstoque(item.quantidade, item.quantidadeMinima)
        }));
        filename = 'relatorio-estoque';
      }

      if (dados.length === 0) {
        const mensagem = this.temFiltrosAtivos() ? 
          'Nenhum dado encontrado com os filtros aplicados.' : 
          'Nenhum dado disponível para exportar.';
        this.showToast(mensagem, 'error');
        return;
      }

      const csvContent = this.convertToCSV(dados);
      
      // Adicionar BOM para suporte a caracteres especiais no Excel
      const BOM = '\uFEFF';
      const blob = new Blob([BOM + csvContent], { 
        type: 'text/csv;charset=utf-8;' 
      });
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${filename}-${new Date().toISOString().split('T')[0]}.csv`;
      link.style.display = 'none';
      
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      this.showToast('Relatório Excel/CSV exportado com sucesso!', 'success');
      
    } catch (error) {
      console.error('Erro ao exportar Excel:', error);
      this.showToast('Erro ao exportar Excel. Tente novamente.', 'error');
    }
  }

  private exportarCSV(): void {
    this.exportarExcel(); // Reutilizar lógica do Excel para CSV
    this.showToast('Relatório CSV exportado com sucesso!', 'success');
  }

  private convertToCSV(data: any[]): string {
    if (data.length === 0) return '';
    
    const headers = Object.keys(data[0]);
    const csvContent = [
      // Cabeçalhos
      headers.join(';'),
      // Dados
      ...data.map(row => 
        headers.map(header => {
          let value = row[header];
          
          // Converter para string e tratar valores nulos/undefined
          if (value === null || value === undefined) {
            value = '';
          } else {
            value = String(value);
          }
          
          // Escapar valores que contêm ponto e vírgula ou aspas
          if (value.includes(';') || value.includes('"') || value.includes('\n')) {
            value = `"${value.replace(/"/g, '""')}"`;
          }
          
          return value;
        }).join(';')
      )
    ].join('\n');
    
    return csvContent;
  }

  private gerarDadosRelatorio(): any {
    return {
      aba: this.activeTab,
      filtros: this.filtroForm.value,
      estatisticas: this.estatisticasGerais,
      dados: this.activeTab === 'vendas' ? this.relatorioVendas :
             this.activeTab === 'produtos' ? this.relatorioProdutos :
             this.relatorioEstoque,
      dataGeracao: new Date().toISOString()
    };
  }

  private showToast(message: string, type: 'success' | 'error' = 'success'): void {
    // Implementar toast notification
    console.log(`${type.toUpperCase()}: ${message}`);
    
    // Criar elemento toast
    const toast = document.createElement('div');
    toast.className = `alert alert-${type === 'success' ? 'success' : 'danger'} alert-dismissible fade show position-fixed`;
    toast.style.top = '20px';
    toast.style.right = '20px';
    toast.style.zIndex = '9999';
    toast.style.minWidth = '300px';
    
    toast.innerHTML = `
      <i class="bi bi-${type === 'success' ? 'check-circle' : 'exclamation-triangle'} me-2"></i>
      ${message}
      <button type="button" class="btn-close" onclick="this.parentElement.remove()"></button>
    `;
    
    document.body.appendChild(toast);
    
    // Remover automaticamente após 5 segundos
    setTimeout(() => {
      if (toast.parentElement) {
        toast.remove();
      }
    }, 5000);
  }

  // Método de impressão corrigido
  imprimirRelatorio(): void {
    try {
      this.isLoadingPrint = true;
      
      if (!this.estatisticasGerais) {
        this.isLoadingPrint = false;
        this.showToast('Carregue os dados primeiro', 'error');
        return;
      }
      
      const printWindow = window.open('', '_blank', 'width=800,height=600');
      
      if (!printWindow) {
        this.isLoadingPrint = false;
        this.showToast('Permita pop-ups para imprimir', 'error');
        return;
      }

      const htmlContent = this.gerarHTMLParaImpressao();
      
      printWindow.document.open();
      printWindow.document.write(htmlContent);
      printWindow.document.close();
      
      // Parar o loading imediatamente após escrever o conteúdo
      this.isLoadingPrint = false;
      
      // Aguardar que a janela carregue completamente antes de imprimir
      const waitForLoad = () => {
        if (printWindow && !printWindow.closed) {
          // Verificar se o documento está pronto
          if (printWindow.document.readyState === 'complete') {
            // Aguardar mais um pouco para garantir renderização
            setTimeout(() => {
              try {
                printWindow.focus();
                printWindow.print();
              } catch (printError) {
                console.error('Erro ao chamar print():', printError);
              }
            }, 200);
          } else {
            // Se ainda não carregou, tentar novamente em 100ms
            setTimeout(waitForLoad, 100);
          }
        }
      };
      
      // Iniciar verificação após um delay inicial
      setTimeout(waitForLoad, 500);
      
    } catch (error) {
      console.error('Erro ao gerar relatório:', error);
      this.isLoadingPrint = false;
      this.showToast('Erro ao gerar relatório', 'error');
    }
  }

  private gerarHTMLParaImpressao(): string {
    const tipoRelatorio = this.activeTab === 'vendas' ? 'de Vendas' : 
                          this.activeTab === 'produtos' ? 'de Produtos' : 'de Estoque';
    
    return '<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Relatório ' + tipoRelatorio + '</title>' +
      '<style>body{font-family:Arial,sans-serif;margin:20px;color:#333}' +
      '.header{text-align:center;margin-bottom:30px;border-bottom:2px solid #ff6b35;padding-bottom:20px}' +
      '.logo{font-size:28px;font-weight:bold;color:#ff6b35}' +
      '.subtitle{color:#666;margin:5px 0}' +
      '.stats{display:flex;justify-content:space-around;margin:30px 0;background:#f8f9fa;padding:20px;border-radius:8px}' +
      '.stat-item{text-align:center;flex:1}' +
      '.stat-value{font-size:24px;font-weight:bold;color:#ff6b35;margin-bottom:5px}' +
      '.stat-label{color:#666;font-size:14px}' +
      'table{width:100%;border-collapse:collapse;margin:20px 0;font-size:14px}' +
      'th,td{border:1px solid #ddd;padding:12px 8px;text-align:left}' +
      'th{background-color:#f8f9fa;font-weight:bold;color:#495057}' +
      'tbody tr:nth-child(even){background-color:#f8f9fa}' +
      '.footer{margin-top:40px;text-align:center;color:#666;font-size:12px;border-top:1px solid #dee2e6;padding-top:20px}' +
      '@media print{body{margin:0}}</style></head><body>' +
      '<div class="header"><div class="logo">Sistema de Estoque e Vendas</div>' +
      '<div class="subtitle">Relatório ' + tipoRelatorio + '</div>' +
      '<div class="subtitle">Gerado em: ' + new Date().toLocaleDateString('pt-BR') + ' às ' + new Date().toLocaleTimeString('pt-BR') + '</div></div>' +
      '<div class="stats">' +
      '<div class="stat-item"><div class="stat-value">' + (this.estatisticasGerais.totalVendasPeriodo || 0) + '</div><div class="stat-label">Total de Vendas</div></div>' +
      '<div class="stat-item"><div class="stat-value">' + this.formatarMoedaSeguro(this.estatisticasGerais.faturamentoPeriodo || 0) + '</div><div class="stat-label">Faturamento</div></div>' +
      '<div class="stat-item"><div class="stat-value">' + this.formatarMoedaSeguro(this.estatisticasGerais.ticketMedio || 0) + '</div><div class="stat-label">Ticket Médio</div></div>' +
      '</div>' + this.gerarTabelaSegura() +
      '<div class="footer"><p>Relatório gerado automaticamente pelo Sistema de Estoque e Vendas</p>' +
      '<p>Data de geração: ' + new Date().toLocaleString('pt-BR') + '</p></div></body></html>';
  }

  private gerarTabelaSegura(): string {
    try {
      // Obter dados filtrados para impressão
      const dadosFiltrados = this.getDadosFiltrados();
      
      // Adicionar indicação de filtros se aplicados
      let filtroInfo = '';
      if (this.temFiltrosAtivos()) {
        filtroInfo = '<div style="background:#fff3cd;border:1px solid #ffeaa7;padding:10px;margin:20px 0;border-radius:5px;text-align:center;color:#856404">' +
                    '<strong>📋 Dados filtrados conforme critérios selecionados</strong></div>';
      }
      
      if (this.activeTab === 'vendas') {
        if (dadosFiltrados.length === 0) {
          return filtroInfo + '<p style="text-align:center;color:#666;margin:40px 0">Nenhum dado de vendas disponível com os filtros aplicados.</p>';
        }
        
        let html = filtroInfo + '<table><thead><tr><th>Período</th><th>Total Vendas</th><th>Faturamento</th><th>Ticket Médio</th><th>Crescimento</th></tr></thead><tbody>';
        dadosFiltrados.forEach(item => {
          html += '<tr><td>' + this.formatarDataSegura(item.periodo) + '</td><td>' + (item.totalVendas || 0) + '</td><td>' + 
                  this.formatarMoedaSeguro(item.faturamento || 0) + '</td><td>' + this.formatarMoedaSeguro(item.ticketMedio || 0) + 
                  '</td><td>' + this.formatarPercentualSeguro(item.crescimento || 0) + '</td></tr>';
        });
        return html + '</tbody></table>';
        
      } else if (this.activeTab === 'produtos') {
        if (dadosFiltrados.length === 0) {
          return filtroInfo + '<p style="text-align:center;color:#666;margin:40px 0">Nenhum dado de produtos disponível com os filtros aplicados.</p>';
        }
        
        let html = filtroInfo + '<table><thead><tr><th>Produto</th><th>Categoria</th><th>Qtd. Vendida</th><th>Faturamento</th><th>Margem</th></tr></thead><tbody>';
        dadosFiltrados.forEach((item: any) => {
          html += '<tr><td>' + (item.produtoNome || 'N/A') + '</td><td>' + (item.categoria || 'N/A') + '</td><td>' + 
                  (item.quantidadeVendida || 0) + '</td><td>' + this.formatarMoedaSeguro(item.faturamento || 0) + 
                  '</td><td>' + this.formatarPercentualSeguro(item.margem || 0) + '</td></tr>';
        });
        return html + '</tbody></table>';
        
      } else {
        if (dadosFiltrados.length === 0) {
          return filtroInfo + '<p style="text-align:center;color:#666;margin:40px 0">Nenhum dado de estoque disponível com os filtros aplicados.</p>';
        }
        
        let html = filtroInfo + '<table><thead><tr><th>Produto</th><th>Categoria</th><th>Estoque Atual</th><th>Estoque Mínimo</th><th>Preço</th><th>Status</th></tr></thead><tbody>';
        dadosFiltrados.forEach((item: any) => {
          html += '<tr><td>' + (item.nome || 'N/A') + '</td><td>' + (item.categoria || 'N/A') + '</td><td>' + 
                  (item.quantidade || 0) + '</td><td>' + (item.quantidadeMinima || 0) + '</td><td>' + 
                  this.formatarMoedaSeguro(item.preco || 0) + '</td><td>' + this.getStatusEstoque(item.quantidade || 0, item.quantidadeMinima || 0) + '</td></tr>';
        });
        return html + '</tbody></table>';
      }
    } catch (error) {
      console.error('Erro ao gerar tabela:', error);
      return '<p style="text-align:center;color:#dc3545;margin:40px 0">Erro ao gerar dados da tabela.</p>';
    }
  }

  private formatarMoedaSeguro(valor: number): string {
    try {
      return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
    } catch {
      return 'R$ ' + valor.toFixed(2).replace('.', ',');
    }
  }

  private formatarPercentualSeguro(valor: number): string {
    try {
      return new Intl.NumberFormat('pt-BR', { style: 'percent', minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(valor / 100);
    } catch {
      return valor.toFixed(1) + '%';
    }
  }

  private formatarDataSegura(data: any): string {
    try {
      if (typeof data === 'string') return data;
      return new Date(data).toLocaleDateString('pt-BR');
    } catch {
      return String(data);
    }
  }

  private getStatusTextSeguro(status: string): string {
    const statusMap: { [key: string]: string } = {
      'NORMAL': 'Normal',
      'BAIXO': 'Estoque Baixo',
      'ZERADO': 'Sem Estoque',
      'ALTO': 'Estoque Alto'
    };
    return statusMap[status] || status;
  }

  formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  }

  formatarPercentual(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'percent',
      minimumFractionDigits: 1,
      maximumFractionDigits: 1
    }).format(valor / 100);
  }

  formatarData(data: Date | string): string {
    if (typeof data === 'string') return data;
    return new Date(data).toLocaleDateString('pt-BR');
  }

  getStatusText(status: string): string {
    const statusMap: { [key: string]: string } = {
      'NORMAL': 'Normal',
      'BAIXO': 'Estoque Baixo',
      'ZERADO': 'Sem Estoque',
      'ALTO': 'Estoque Alto',
      'OK': 'Em Estoque'
    };
    return statusMap[status] || status;
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'OK':
        return 'bg-success text-white';
      case 'BAIXO':
        return 'bg-warning text-dark';
      case 'ZERADO':
        return 'bg-danger text-white';
      default:
        return 'bg-secondary text-white';
    }
  }

  getCrescimentoClass(crescimento: number): string {
    if (crescimento > 0) {
      return 'text-success';
    } else if (crescimento < 0) {
      return 'text-danger';
    }
    return 'text-muted';
  }

  getCrescimentoIcon(crescimento: number): string {
    if (crescimento > 0) {
      return 'bi-trend-up';
    } else if (crescimento < 0) {
      return 'bi-trend-down';
    }
    return 'bi-dash';
  }

  isPeriodoPersonalizado(): boolean {
    return this.filtroForm.get('periodo')?.value === 'personalizado';
  }

  getTopProdutos(limit: number = 5): RelatorioProdutos[] {
    return this.relatorioProdutos
      .sort((a, b) => b.quantidadeVendida - a.quantidadeVendida)
      .slice(0, limit);
  }

  getProdutosBaixoEstoque(): RelatorioEstoque[] {
    return this.relatorioEstoque.filter(item => 
      item.status === 'BAIXO' || item.status === 'ZERADO'
    );
  }

  // Método para obter eventos únicos das vendas
  async getEventosUnicos(): Promise<string[]> {
    try {
      // Buscar eventos do backend
      const eventosBackend = await this.eventosService.listarEventos().toPromise();

      // eventosBackend pode ser um objeto paginado { content: [], ... } ou um array de eventos
      const eventosArray = Array.isArray(eventosBackend) ? eventosBackend : (eventosBackend && eventosBackend.content ? eventosBackend.content : []);

      if (eventosArray && eventosArray.length > 0) {
        // Extrair nomes únicos dos eventos ativos
        const eventosUnicos = eventosArray
          .filter((evento: any) => evento && evento.ativo !== false && evento.nome) // Filtrar eventos ativos com nome
          .map((evento: any) => evento.nome)
          .filter((nome: string, index: number, array: string[]) => array.indexOf(nome) === index) // Remover duplicatas
          .sort();

        console.log(`✅ ${eventosUnicos.length} eventos únicos carregados do backend`);
        return eventosUnicos;
      }
      
      // Fallback: buscar eventos das vendas carregadas se backend não disponível
      const eventosVendas = this.vendas
        .map(venda => venda.evento)
        .filter((evento): evento is string => evento !== undefined && evento.trim() !== '')
        .filter((evento, index, array) => array.indexOf(evento) === index)
        .sort();

      console.log(`⚠️ Usando ${eventosVendas.length} eventos das vendas locais como fallback`);
      return eventosVendas;
    } catch (error) {
      console.error('❌ Erro ao buscar eventos únicos:', error);
      
      // Fallback: buscar eventos das vendas carregadas
      const eventosVendas = this.vendas
        .map(venda => venda.evento)
        .filter((evento): evento is string => evento !== undefined && evento.trim() !== '')
        .filter((evento, index, array) => array.indexOf(evento) === index)
        .sort();

      console.log(`⚠️ Erro no backend, usando ${eventosVendas.length} eventos das vendas locais`);
      return eventosVendas;
    }
  }

  // Métodos de navegação para boxes clicáveis
  navegarParaSemEstoque(): void {
    this.router.navigate(['/estoque'], { 
      queryParams: { filtro: 'sem-estoque' } 
    });
  }

}
