import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';

// ===== ENUMS =====
export enum StatusVenda {
  PENDENTE = 'PENDENTE',
  CONFIRMADA = 'CONFIRMADA',
  CANCELADA = 'CANCELADA'
}

export enum FormaPagamento {
  DINHEIRO = 'DINHEIRO',
  CARTAO_DEBITO = 'CARTAO_DEBITO',
  CARTAO_CREDITO = 'CARTAO_CREDITO',
  PIX = 'PIX'
}

export enum CategoriaCliente {
  BRONZE = 'BRONZE',
  PRATA = 'PRATA',
  OURO = 'OURO',
  DIAMANTE = 'DIAMANTE'
}

export enum TipoUsuario {
  OWNER = 'OWNER',
  ADMIN = 'ADMIN',
  VENDEDOR = 'VENDEDOR',
  ESTOQUISTA = 'ESTOQUISTA'
}

// ===== INTERFACES BÁSICAS =====
export interface PeriodoDTO {
  inicio: string;
  fim: string;
  diasPeriodo: number;
}

export interface VendedorRanking {
  nomeVendedor: string;
  quantidadeVendas: number;
  valorTotal: number;
}

export interface ProdutoVendidoRanking {
  nomeProduto: string;
  quantidadeVendida: number;
}

export interface ProdutoEstoqueBaixo {
  id: number;
  nome: string;
  codigo: string;
  estoqueAtual: number;
  estoqueMinimo: number;
  categoria: string;
}

export interface CategoriaValorEstoque {
  categoria: string;
  quantidadeProdutos: number;
  valorTotal: number;
}

export interface ProdutoMovimentacaoRanking {
  nomeProduto: string;
  quantidadeMovimentacoes: number;
}

export interface VendaPorDia {
  data: string;
  quantidadeVendas: number;
  valorTotal: number;
}

export interface ReceitaPorCategoria {
  categoria: string;
  receita: number;
  percentual: number;
}

// ===== INTERFACES DE RESPOSTA =====
export interface RelatorioVendasResponse {
  periodo: string;
  totalVendas: number;
  totalFaturamento: number;
  ticketMedio: number;
  topVendedores: VendedorRanking[];
  produtosMaisVendidos: ProdutoVendidoRanking[];
  dataGeracao: string;
}

export interface RelatorioEstoqueResponse {
  totalProdutos: number;
  produtosAtivos: number;
  produtosEstoqueBaixo: number;
  valorTotalEstoque: number;
  produtosComEstoqueBaixo: ProdutoEstoqueBaixo[]; // Apenas produtos com estoque baixo (alertas)
  todosProdutos: ProdutoEstoqueBaixo[]; // Todos os produtos (tabela completa)
  categoriasPorValor: CategoriaValorEstoque[];
  dataGeracao: string;
}

export interface RelatorioMovimentacaoResponse {
  periodo: string;
  totalMovimentacoes: number;
  totalEntradas: number;
  totalSaidas: number;
  totalAjustes: number;
  quantidadeEntradas: number;
  quantidadeSaidas: number;
  saldoMovimentacao: number;
  produtosMaisMovimentados: ProdutoMovimentacaoRanking[];
  dataGeracao: string;
}

export interface DashboardExecutivoResponse {
  vendasMes: number;
  faturamentoMes: number;
  vendasHoje: number;
  faturamentoHoje: number;
  vendasPendentes: number;
  produtosEstoqueBaixo: number;
  totalProdutos: number;
  movimentacoesHoje: number;
  ultimaAtualizacao: string;
}

export interface RelatorioFinanceiroResponse {
  periodo: string;
  receitaTotal: number;
  custoTotal: number;
  lucroTotal: number;
  margemLucro: number;
  vendasPorDia: VendaPorDia[];
  receitaPorCategoria: ReceitaPorCategoria[];
  dataGeracao: string;
}

// ===== INTERFACES EXPANDIDAS =====
export interface ResumoVendasDTO {
  totalVendas: number;
  valorTotal: number;
  ticketMedio: number;
  crescimento: number;
  vendasConfirmadas: number;
  vendasPendentes: number;
}

export interface VendaPorDiaDTO {
  data: string;
  quantidade: number;
  valor: number;
}

export interface TopProdutoDTO {
  produtoId: number;
  nome: string;
  categoria: string;
  quantidadeVendida: number;
  faturamento: number;
  precoUnitario: number;
}

export interface TopVendedorDTO {
  vendedorId: number;
  nome: string;
  email: string;
  tipo: TipoUsuario;
  totalVendas: number;
  faturamento: number;
  ticketMedio: number;
}

export interface VendaDTO {
  id: number;
  nomeCliente: string;
  dataVenda: string;
  valorTotal: number;
  status: StatusVenda;
  formaPagamento: FormaPagamento;
  vendedorNome: string;
  eventoNome?: string;
}

export interface ResumoProdutosDTO {
  totalProdutos: number;
  produtosAtivos: number;
  totalCategorias: number;
  valorEstoque: number;
  valorMedioUnitario: number;
}

export interface EstoqueResumoDTO {
  produtosEstoqueBaixo: number;
  produtosSemEstoque: number;
  estoqueTotal: number;
  valorTotalEstoque: number;
  alertasCriticos: number;
}

export interface ProdutoResumoDTO {
  id: number;
  produtoNome: string;
  categoria: string;
  quantidadeVendida: number;
  faturamento: number;
  margem: number;
  estoqueAtual: number;
  precoUnitario: number;
}

export interface CategoriaProdutoDTO {
  nome: string;
  quantidadeProdutos: number;
  valorEstoque: number;
}

export interface AlertaEstoqueDTO {
  produtoId: number;
  nomeProduto: string;
  estoqueAtual: number;
  estoqueMinimo: number;
  status: string;
  categoria: string;
  valor: number;
}

export interface ResumoClientesDTO {
  totalClientes: number;
  clientesAtivos: number;
  clientesNovos: number;
  ticketMedioGeral: number;
  faturamentoTotal: number;
  totalPontosDistribuidos: number;
}

export interface TopClienteDTO {
  clienteId: number;
  nome: string;
  email: string;
  categoria: CategoriaCliente;
  totalCompras: number;
  quantidadeCompras: number;
  pontos: number;
  ultimaCompra: string;
}

export interface ClienteDTO {
  id: number;
  nome: string;
  email: string;
  categoria: CategoriaCliente;
  totalCompras: number;
  quantidadeCompras: number;
  pontos: number;
  dataCadastro: string;
  ultimaCompra: string;
}

export interface ReceitasDTO {
  receitaBruta: number;
  descontos: number;
  receitaLiquida: number;
  numeroVendas: number;
}

export interface CustosDTO {
  custoProdutos: number;
  percentualCusto: number;
}

export interface LucrosDTO {
  lucroLiquido: number;
  margemLucro: number;
  crescimentoPeriodoAnterior: number;
}

// ===== RESPONSES COMPLETAS =====
export interface RelatorioVendasResponseCompleto {
  periodo: PeriodoDTO;
  resumo: ResumoVendasDTO;
  vendas: VendaDTO[];
  vendasPorDia: VendaPorDiaDTO[];
  vendasPorStatus: { [key: string]: number };
  topProdutos: TopProdutoDTO[];
  topVendedores: TopVendedorDTO[];
  geradoEm: string;
}

export interface RelatorioProdutosResponse {
  resumo: ResumoProdutosDTO;
  estoque: EstoqueResumoDTO;
  categorias: CategoriaProdutoDTO[];
  produtosMaisVendidos: TopProdutoDTO[];
  alertasEstoque: AlertaEstoqueDTO[];
  produtos: ProdutoResumoDTO[]; // Lista de produtos para a tabela
  geradoEm: string;
}

export interface RelatorioClientesResponse {
  resumo: ResumoClientesDTO;
  distribuicaoCategoria: { [key: string]: number };
  topClientes: TopClienteDTO[];
  clientesNovos: ClienteDTO[];
  geradoEm: string;
}

export interface RelatorioFinanceiroResponseCompleto {
  periodo: PeriodoDTO;
  receitas: ReceitasDTO;
  custos: CustosDTO;
  lucros: LucrosDTO;
  geradoEm: string;
}

// ===== FILTROS =====
export interface FiltroRelatorio {
  // Período
  dataInicio?: string;
  dataFim?: string;
  inicio?: string;
  fim?: string;
  periodo?: string;
  
  // Filtros específicos
  vendedorId?: number;
  categoriaId?: number;
  categoria?: string;
  status?: StatusVenda;
  clienteId?: number;
  evento?: string;
  agruparPor?: string;
  
  // Filtros de estoque
  statusEstoque?: string;
  valorMinimo?: number;
  
  // Filtros de apresentação
  ordenarPor?: string;
  criterioPerformance?: string;
  limiteResultados?: number;
}

@Injectable({
  providedIn: 'root'
})
export class RelatorioService {
  private readonly apiUrl = `${environment.apiUrl}/relatorios`;
  
  private dashboardSubject = new BehaviorSubject<DashboardExecutivoResponse | null>(null);
  public dashboard$ = this.dashboardSubject.asObservable();

  constructor(private http: HttpClient) {}

  // ===== RELATÓRIOS BÁSICOS =====

  /**
   * Gera relatório de vendas
   */
  gerarRelatorioVendas(inicio: string, fim: string, filtros?: FiltroRelatorio): Observable<RelatorioVendasResponse> {
    let params = new HttpParams()
      .set('inicio', inicio)
      .set('fim', fim);
    
    // Adicionar filtros opcionais
    if (filtros) {
      if (filtros.categoria) params = params.set('categoria', filtros.categoria);
      if (filtros.evento) params = params.set('evento', filtros.evento);
      if (filtros.agruparPor) params = params.set('agruparPor', filtros.agruparPor);
      if (filtros.vendedorId) params = params.set('vendedorId', filtros.vendedorId.toString());
      if (filtros.criterioPerformance) params = params.set('criterioPerformance', filtros.criterioPerformance);
      if (filtros.limiteResultados) params = params.set('limiteResultados', filtros.limiteResultados.toString());
    }
    
    return this.http.get<RelatorioVendasResponse>(`${this.apiUrl}/vendas`, { params });
  }

  /**
   * Gera relatório de vendas detalhadas (vendas individuais)
   */
  gerarRelatorioVendasDetalhadas(inicio: string, fim: string, filtros?: FiltroRelatorio): Observable<any> {
    let params = new HttpParams()
      .set('inicio', inicio)
      .set('fim', fim);
    
    // Adicionar filtros opcionais
    if (filtros) {
      if (filtros.categoria) params = params.set('categoria', filtros.categoria);
      if (filtros.evento) params = params.set('evento', filtros.evento);
      if (filtros.agruparPor) params = params.set('agruparPor', filtros.agruparPor);
      if (filtros.vendedorId) params = params.set('vendedorId', filtros.vendedorId.toString());
      if (filtros.ordenarPor) params = params.set('ordenarPor', filtros.ordenarPor);
      if (filtros.limiteResultados) params = params.set('limiteResultados', filtros.limiteResultados.toString());
    }
    
    return this.http.get<any>(`${this.apiUrl}/vendas/detalhadas`, { params });
  }

  /**
   * Obtém dados para gráfico de evolução de vendas
   */
  obterDadosGraficoVendas(inicio: string, fim: string, filtros?: FiltroRelatorio): Observable<any> {
    let params = new HttpParams()
      .set('inicio', inicio)
      .set('fim', fim);
    
    // Adicionar filtros opcionais
    if (filtros) {
      if (filtros.categoria) params = params.set('categoria', filtros.categoria);
      if (filtros.evento) params = params.set('evento', filtros.evento);
      if (filtros.agruparPor) params = params.set('agruparPor', filtros.agruparPor);
      if (filtros.vendedorId) params = params.set('vendedorId', filtros.vendedorId.toString());
    }
    
    return this.http.get<any>(`${this.apiUrl}/vendas/grafico`, { params });
  }

  /**
   * Gera relatório de estoque
   */
  gerarRelatorioEstoque(filtros?: FiltroRelatorio): Observable<RelatorioEstoqueResponse> {
    let params = new HttpParams();
    
    // Adicionar filtros opcionais
    if (filtros) {
      if (filtros.categoria) params = params.set('categoria', filtros.categoria);
      if (filtros.statusEstoque) params = params.set('statusEstoque', filtros.statusEstoque);
      if (filtros.valorMinimo) params = params.set('valorMinimo', filtros.valorMinimo.toString());
      if (filtros.ordenarPor) params = params.set('ordenarPor', filtros.ordenarPor);
      if (filtros.limiteResultados) params = params.set('limiteResultados', filtros.limiteResultados.toString());
    }
    
    const options = params.keys().length > 0 ? { params } : {};
    return this.http.get<RelatorioEstoqueResponse>(`${this.apiUrl}/estoque`, options);
  }

  /**
   * Gera relatório de movimentação
   */
  gerarRelatorioMovimentacao(inicio: string, fim: string, filtros?: FiltroRelatorio): Observable<RelatorioMovimentacaoResponse> {
    let params = new HttpParams()
      .set('inicio', inicio)
      .set('fim', fim);
    
    // Adicionar filtros opcionais
    if (filtros) {
      if (filtros.categoria) params = params.set('categoria', filtros.categoria);
      if (filtros.agruparPor) params = params.set('agruparPor', filtros.agruparPor);
      if (filtros.ordenarPor) params = params.set('ordenarPor', filtros.ordenarPor);
      if (filtros.limiteResultados) params = params.set('limiteResultados', filtros.limiteResultados.toString());
    };
    
    return this.http.get<RelatorioMovimentacaoResponse>(`${this.apiUrl}/movimentacao`, { params });
  }

  /**
   * Gera dashboard executivo
   */
  gerarDashboardExecutivo(): Observable<DashboardExecutivoResponse> {
    const dashboard$ = this.http.get<DashboardExecutivoResponse>(`${this.apiUrl}/dashboard/executivo`);
    
    dashboard$.subscribe(data => this.dashboardSubject.next(data));
    
    return dashboard$;
  }

  // ===== RELATÓRIOS EXPANDIDOS =====
  
  /**
   * Relatório completo de vendas (se existir endpoint)
   */
  gerarRelatorioVendasCompleto(filtros: FiltroRelatorio): Observable<RelatorioVendasResponseCompleto> {
    let params = new HttpParams();
    
    if (filtros.inicio) params = params.set('inicio', filtros.inicio);
    if (filtros.fim) params = params.set('fim', filtros.fim);
    if (filtros.vendedorId) params = params.set('vendedorId', filtros.vendedorId.toString());
    if (filtros.status) params = params.set('status', filtros.status);
    
    return this.http.get<RelatorioVendasResponseCompleto>(`${this.apiUrl}/vendas/completo`, { params });
  }

  /**
   * Relatório de produtos
   */
  gerarRelatorioProdutos(): Observable<RelatorioProdutosResponse> {
    return this.http.get<RelatorioProdutosResponse>(`${this.apiUrl}/produtos`);
  }

  /**
   * Relatório de clientes
   */
  gerarRelatorioClientes(): Observable<RelatorioClientesResponse> {
    return this.http.get<RelatorioClientesResponse>(`${this.apiUrl}/clientes`);
  }

  /**
   * Relatório financeiro completo
   */
  gerarRelatorioFinanceiroCompleto(inicio: string, fim: string): Observable<RelatorioFinanceiroResponseCompleto> {
    const params = new HttpParams()
      .set('inicio', inicio)
      .set('fim', fim);
    
    return this.http.get<RelatorioFinanceiroResponseCompleto>(`${this.apiUrl}/financeiro/completo`, { params });
  }

  // ===== MÉTODOS UTILITÁRIOS =====

  /**
   * Formatar valor monetário
   */
  formatarMoeda(valor: number | null | undefined): string {
    if (valor === null || valor === undefined || isNaN(valor)) {
      valor = 0;
    }
    
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(valor);
  }

  /**
   * Formatar percentual
   */
  formatarPercentual(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'percent',
      minimumFractionDigits: 1,
      maximumFractionDigits: 2
    }).format((valor || 0) / 100);
  }

  /**
   * Formatar data
   */
  formatarData(data: string): string {
    if (!data) return 'N/A';
    
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      }).format(new Date(data));
    } catch {
      return data;
    }
  }

  /**
   * Formatar data/hora
   */
  formatarDataHora(data: string): string {
    if (!data) return 'N/A';
    
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }).format(new Date(data));
    } catch {
      return data;
    }
  }

  /**
   * Formatar número
   */
  formatarNumero(valor: number): string {
    return new Intl.NumberFormat('pt-BR').format(valor || 0);
  }

  /**
   * Obter cor do status
   */
  getCorStatus(status: StatusVenda): string {
    switch (status) {
      case StatusVenda.CONFIRMADA: return '#28a745';
      case StatusVenda.PENDENTE: return '#ffc107';
      case StatusVenda.CANCELADA: return '#dc3545';
      default: return '#6c757d';
    }
  }

  /**
   * Obter cor da categoria cliente
   */
  getCorCategoriaCliente(categoria: CategoriaCliente): string {
    switch (categoria) {
      case CategoriaCliente.BRONZE: return '#cd7f32';
      case CategoriaCliente.PRATA: return '#c0c0c0';
      case CategoriaCliente.OURO: return '#ffd700';
      case CategoriaCliente.DIAMANTE: return '#b9f2ff';
      default: return '#6c757d';
    }
  }

  /**
   * Obter período atual (últimos 30 dias)
   */
  getPeriodoAtual(): { inicio: string, fim: string } {
    const hoje = new Date();
    const inicio = new Date(hoje);
    inicio.setDate(hoje.getDate() - 30);
    
    return {
      inicio: inicio.toISOString().split('T')[0],
      fim: hoje.toISOString().split('T')[0]
    };
  }

  /**
   * Obter período mensal atual
   */
  getPeriodoMensal(): { inicio: string, fim: string } {
    const hoje = new Date();
    const inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
    
    return {
      inicio: inicio.toISOString().split('T')[0],
      fim: hoje.toISOString().split('T')[0]
    };
  }

  /**
   * Validar período
   */
  validarPeriodo(inicio: string, fim: string): boolean {
    const dataInicio = new Date(inicio);
    const dataFim = new Date(fim);
    
    return dataInicio <= dataFim && dataInicio <= new Date();
  }

  /**
   * Calcular crescimento percentual
   */
  calcularCrescimento(valorAtual: number, valorAnterior: number): number {
    if (valorAnterior === 0) return valorAtual > 0 ? 100 : 0;
    return ((valorAtual - valorAnterior) / valorAnterior) * 100;
  }

  /**
   * Limpar cache do dashboard
   */
  limparCache(): void {
    this.dashboardSubject.next(null);
  }
}