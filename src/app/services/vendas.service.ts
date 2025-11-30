import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, map, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';

// ===== ENUMS =====
export enum StatusVenda {
  PENDENTE = 'PENDENTE',
  CONFIRMADA = 'CONFIRMADA',
  ENTREGUE = 'ENTREGUE',
  CANCELADA = 'CANCELADA'
}

export enum FormaPagamento {
  DINHEIRO = 'DINHEIRO',
  CARTAO_CREDITO = 'CARTAO_CREDITO',
  CARTAO_DEBITO = 'CARTAO_DEBITO',
  PIX = 'PIX',
  TRANSFERENCIA = 'TRANSFERENCIA',
  BOLETO = 'BOLETO'
}

// ===== INTERFACES =====
export interface VendaRequest {
  nomeCliente: string;
  emailCliente?: string;
  telefoneCliente?: string;
  formaPagamento: FormaPagamento;
  desconto?: number;
  observacoes?: string;
  eventoId?: number;  // Campo para suporte a eventos promocionais
  dataVenda?: Date;   // Campo para data/hora da venda (opcional - usa now() se não fornecido)
  itens: ItemVendaRequest[];
}

export interface ItemVendaRequest {
  produtoId: number;
  quantidade: number;
  precoUnitario: number;
  nomeProduto?: string; // 🔧 Campo adicionado para resolver erro NULL
  descontoItem?: number; // 🔧 Campo para desconto por item
  tamanho?: string; // 🔧 Campo para tamanho do produto
}

export interface VendaResponse {
  id: number;
  nomeCliente: string;
  clienteNome?: string; // Alias para compatibilidade
  emailCliente?: string;
  clienteEmail?: string; // Alias para compatibilidade
  telefoneCliente?: string;
  dataVenda: Date;
  dataConfirmacao?: Date;
  dataCancelamento?: Date;
  status: StatusVenda;
  subtotal: number;
  desconto: number;
  valorTotal: number;
  total?: number; // Alias para compatibilidade
  formaPagamento?: FormaPagamento; // Campo de forma de pagamento
  observacoes?: string;
  motivoCancelamento?: string;
  quantidadeItens: number;
  eventoId?: number;  // Campo para suporte a eventos promocionais
  eventoNome?: string;  // Nome do evento para exibição
  evento?: string; // Alias para compatibilidade
  itens: ItemVendaResponse[];
  nomeVendedor: string;
  emailVendedor: string;
}

export interface ItemVendaResponse {
  id: number;
  produtoId: number;
  nomeProduto: string;
  codigoBarras?: string;
  quantidade: number;
  precoUnitario: number;
  subtotal: number;
  tamanho?: string; // Campo para tamanho do produto
}

export interface VendaListResponse {
  id: number;
  nomeCliente: string;
  dataVenda: Date;
  status: StatusVenda;
  valorTotal: number;
  quantidadeItens: number;
  nomeVendedor: string;
}

export interface EstatisticasVendasResponse {
  totalVendas: number;
  totalFaturamento: number;
  ticketMedio: number;
  vendasPendentes: number;
  vendasConfirmadas: number;
  vendasCanceladas: number;
}

export interface TopProdutoResponse {
  produtoId: number;
  nomeProduto: string;
  categoriaProduto?: string;
  quantidadeVendida: number;
  receitaTotal: number;
  precoUnitario: number;
  ranking: number;
  percentualVendas: number;
}

export interface FiltroVendas {
  status?: StatusVenda;
  dataInicio?: Date;
  dataFim?: Date;
  nomeCliente?: string;
  vendedorId?: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface CancelamentoRequest {
  motivoCancelamento: string;
}

export interface VendaResumo {
  id: number;
  nomeCliente: string;
  dataVenda: Date;
  status: StatusVenda;
  valorTotal: number;
  nomeVendedor: string;
}

// ===== SERVICE =====
@Injectable({
  providedIn: 'root'
})
export class VendasService {
  private readonly baseUrl = `${environment.apiUrl}/vendas`;
  
  private estatisticasSubject = new BehaviorSubject<EstatisticasVendasResponse | null>(null);
  public estatisticas$ = this.estatisticasSubject.asObservable();

  constructor(private http: HttpClient) {}

  // ===== MÉTODOS PRINCIPAIS DE CRUD =====

  /**
   * Lista vendas com paginação e filtros avançados
   */
  listarVendas(
    filtros: FiltroVendas = {},
    page: number = 0,
    size: number = 20,
    sortBy: string = 'dataVenda',
    sortDir: 'ASC' | 'DESC' = 'DESC'
  ): Observable<PaginatedResponse<VendaResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (filtros.status) {
      params = params.set('status', filtros.status);
    }
    if (filtros.dataInicio) {
      params = params.set('dataInicio', this.formatarDataISO(filtros.dataInicio));
    }
    if (filtros.dataFim) {
      params = params.set('dataFim', this.formatarDataISO(filtros.dataFim));
    }
    if (filtros.nomeCliente) {
      params = params.set('nomeCliente', filtros.nomeCliente);
    }

    return this.http.get<PaginatedResponse<VendaResponse>>(this.baseUrl, { params })
      .pipe(
        map(response => ({
          ...response,
          content: response.content.map(venda => this.processarVendaResponse(venda))
        })),
        catchError(error => {
          console.error('Erro ao listar vendas:', error);
          return of({
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: size,
            number: page,
            first: true,
            last: true
          });
        })
      );
  }

  /**
   * Busca venda específica por ID
   */
  buscarVendaPorId(id: number): Observable<VendaResponse | null> {
    return this.http.get<VendaResponse>(`${this.baseUrl}/${id}`)
      .pipe(
        map(venda => this.processarVendaResponse(venda)),
        catchError(error => {
          console.error('Erro ao buscar venda por ID:', error);
          return of(null);
        })
      );
  }

  /**
   * Cria nova venda
   */
  criarVenda(vendaRequest: VendaRequest): Observable<VendaResponse> {
    // 🔧 Construir request limpo com apenas campos esperados pelo backend
    const requestData = {
      nomeCliente: vendaRequest.nomeCliente,
      emailCliente: vendaRequest.emailCliente,
      telefoneCliente: vendaRequest.telefoneCliente,
      formaPagamento: vendaRequest.formaPagamento,
      desconto: vendaRequest.desconto,
      observacoes: vendaRequest.observacoes,
      eventoId: vendaRequest.eventoId,
      // Se não foi fornecida uma data específica, usa a data/hora atual
      dataVenda: vendaRequest.dataVenda || new Date(),
      itens: vendaRequest.itens.map(item => ({
        produtoId: item.produtoId,
        quantidade: item.quantidade,
        precoUnitario: item.precoUnitario,
        nomeProduto: item.nomeProduto,
        descontoItem: item.descontoItem,
        tamanho: item.tamanho
      }))
    };

    console.log('📅 FRONTEND - Enviando venda com data:', requestData.dataVenda);
    console.log('🔧 FRONTEND - Request limpo:', requestData);
    
    return this.http.post<VendaResponse>(this.baseUrl, requestData)
      .pipe(
        map(venda => this.processarVendaResponse(venda)),
        catchError(error => {
          console.error('Erro ao criar venda:', error);
          throw error;
        })
      );
  }

  /**
   * Confirma uma venda pendente
   */
  confirmarVenda(id: number): Observable<VendaResponse> {
    return this.http.patch<VendaResponse>(`${this.baseUrl}/${id}/confirmar`, {})
      .pipe(
        map(venda => this.processarVendaResponse(venda)),
        catchError(error => {
          console.error('Erro ao confirmar venda:', error);
          throw error;
        })
      );
  }

  /**
   * Cancela uma venda com motivo
   */
  cancelarVenda(id: number, motivo?: string): Observable<VendaResponse> {
    let params = new HttpParams();
    if (motivo) {
      params = params.set('motivo', motivo);
    }

    return this.http.patch<VendaResponse>(`${this.baseUrl}/${id}/cancelar`, {}, { params })
      .pipe(
        map(venda => this.processarVendaResponse(venda)),
        catchError(error => {
          console.error('Erro ao cancelar venda:', error);
          throw error;
        })
      );
  }

  // ===== MÉTODOS DE CONSULTA AVANÇADA =====

  /**
   * Busca vendas por período específico
   */
  buscarVendasPorPeriodo(dataInicio: Date, dataFim: Date): Observable<VendaResponse[]> {
    const params = new HttpParams()
      .set('dataInicio', this.formatarDataISO(dataInicio))
      .set('dataFim', this.formatarDataISO(dataFim));

    return this.http.get<VendaResponse[]>(`${this.baseUrl}/por-periodo`, { params })
      .pipe(
        map(vendas => vendas.map(venda => this.processarVendaResponse(venda))),
        catchError(error => {
          console.error('Erro ao buscar vendas por período:', error);
          return of([]);
        })
      );
  }

  /**
   * Obtém estatísticas gerais de vendas
   */
  obterEstatisticasVendas(dataInicio?: Date, dataFim?: Date): Observable<EstatisticasVendasResponse> {
    let params = new HttpParams();
    if (dataInicio) {
      params = params.set('dataInicio', this.formatarDataISO(dataInicio));
    }
    if (dataFim) {
      params = params.set('dataFim', this.formatarDataISO(dataFim));
    }

    return this.http.get<EstatisticasVendasResponse>(`${this.baseUrl}/estatisticas`, { params })
      .pipe(
        map(stats => {
          this.estatisticasSubject.next(stats);
          return stats;
        }),
        catchError(error => {
          console.error('Erro ao obter estatísticas:', error);
          const estatisticasVazias: EstatisticasVendasResponse = {
            totalVendas: 0,
            totalFaturamento: 0,
            ticketMedio: 0,
            vendasPendentes: 0,
            vendasConfirmadas: 0,
            vendasCanceladas: 0
          };
          return of(estatisticasVazias);
        })
      );
  }

  /**
   * Busca top produtos mais vendidos
   */
  obterTopProdutos(
    dataInicio?: Date,
    dataFim?: Date,
    tipoRanking: 'quantidade' | 'receita' = 'quantidade',
    limite: number = 10
  ): Observable<TopProdutoResponse[]> {
    let params = new HttpParams()
      .set('tipoRanking', tipoRanking)
      .set('limite', limite.toString());

    if (dataInicio) {
      params = params.set('dataInicio', this.formatarDataISO(dataInicio));
    }
    if (dataFim) {
      params = params.set('dataFim', this.formatarDataISO(dataFim));
    }

    return this.http.get<TopProdutoResponse[]>(`${this.baseUrl}/top-produtos`, { params })
      .pipe(
        catchError(error => {
          console.error('Erro ao obter top produtos:', error);
          return of([]);
        })
      );
  }

  // ===== MÉTODOS UTILITÁRIOS =====

  /**
   * Processa resposta do backend convertendo datas e formatando dados
   */
  private processarVendaResponse(venda: any): VendaResponse {
    return {
      ...venda,
      dataVenda: new Date(venda.dataVenda),
      dataConfirmacao: venda.dataConfirmacao ? new Date(venda.dataConfirmacao) : undefined,
      dataCancelamento: venda.dataCancelamento ? new Date(venda.dataCancelamento) : undefined,
      status: venda.status as StatusVenda
    };
  }

  /**
   * Formata data para padrão ISO aceito pelo backend
   */
  private formatarDataISO(data: Date): string {
    return data.toISOString().split('T')[0];
  }

  /**
   * Formata valor monetário para exibição
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
   * Formata data para exibição
   */
  formatarData(data: Date): string {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(data));
  }

  /**
   * Formata apenas data sem horário
   */
  formatarDataSimples(data: Date): string {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(new Date(data));
  }

  /**
   * Converte enum para texto legível
   */
  getTextoStatus(status: StatusVenda): string {
    const textos = {
      [StatusVenda.PENDENTE]: 'Pendente',
      [StatusVenda.CONFIRMADA]: 'Confirmada',
      [StatusVenda.ENTREGUE]: 'Entregue',
      [StatusVenda.CANCELADA]: 'Cancelada'
    };
    return textos[status] || status;
  }

  /**
   * Converte enum para classe CSS do badge
   */
  getClasseStatus(status: StatusVenda): string {
    const classes = {
      [StatusVenda.PENDENTE]: 'bg-warning text-dark',
      [StatusVenda.CONFIRMADA]: 'bg-info text-white',
      [StatusVenda.ENTREGUE]: 'bg-success text-white',
      [StatusVenda.CANCELADA]: 'bg-danger text-white'
    };
    return classes[status] || 'bg-secondary text-white';
  }

  /**
   * Converte enum para texto legível da forma de pagamento
   */
  getTextoFormaPagamento(forma: FormaPagamento): string {
    const textos = {
      [FormaPagamento.DINHEIRO]: 'Dinheiro',
      [FormaPagamento.CARTAO_CREDITO]: 'Cartão de Crédito',
      [FormaPagamento.CARTAO_DEBITO]: 'Cartão de Débito',
      [FormaPagamento.PIX]: 'PIX',
      [FormaPagamento.TRANSFERENCIA]: 'Transferência',
      [FormaPagamento.BOLETO]: 'Boleto'
    };
    return textos[forma] || forma;
  }

  /**
   * Calcula percentual de crescimento entre dois valores
   */
  calcularCrescimento(valorAtual: number, valorAnterior: number): number {
    if (valorAnterior === 0) return valorAtual > 0 ? 100 : 0;
    return ((valorAtual - valorAnterior) / valorAnterior) * 100;
  }

  /**
   * Obtém cor para indicador de crescimento
   */
  getCorCrescimento(crescimento: number): string {
    if (crescimento > 0) return 'text-success';
    if (crescimento < 0) return 'text-danger';
    return 'text-muted';
  }

  /**
   * Obtém ícone para indicador de crescimento
   */
  getIconeCrescimento(crescimento: number): string {
    if (crescimento > 0) return 'bi-arrow-up';
    if (crescimento < 0) return 'bi-arrow-down';
    return 'bi-dash';
  }

  /**
   * Formata número com separadores de milhares
   */
  formatarNumero(numero: number): string {
    return new Intl.NumberFormat('pt-BR').format(numero || 0);
  }

  /**
   * Calcula ticket médio
   */
  calcularTicketMedio(totalFaturamento: number, totalVendas: number): number {
    return totalVendas > 0 ? totalFaturamento / totalVendas : 0;
  }

  // ===== MÉTODOS DE VALIDAÇÃO =====

  /**
   * Valida se uma venda pode ser confirmada
   */
  podeConfirmar(venda: VendaResponse): boolean {
    return venda.status === StatusVenda.PENDENTE;
  }

  /**
   * Valida se uma venda pode ser cancelada
   */
  podeCancelar(venda: VendaResponse): boolean {
    return venda.status === StatusVenda.PENDENTE || venda.status === StatusVenda.CONFIRMADA;
  }

  /**
   * Valida se uma venda pode ser editada
   */
  podeEditar(venda: VendaResponse): boolean {
    return venda.status === StatusVenda.PENDENTE;
  }

  // ===== MÉTODOS DE FILTRO E BUSCA =====

  /**
   * Filtra vendas por múltiplos critérios (frontend)
   */
  filtrarVendas(vendas: VendaResponse[], filtros: any): VendaResponse[] {
    return vendas.filter(venda => {
      if (filtros.termoBusca) {
        const termo = filtros.termoBusca.toLowerCase();
        if (!venda.nomeCliente.toLowerCase().includes(termo) &&
            !venda.id.toString().includes(termo)) {
          return false;
        }
      }

      if (filtros.status && venda.status !== filtros.status) {
        return false;
      }

      if (filtros.dataInicio || filtros.dataFim) {
        const dataVenda = new Date(venda.dataVenda);
        if (filtros.dataInicio && dataVenda < filtros.dataInicio) return false;
        if (filtros.dataFim && dataVenda > filtros.dataFim) return false;
      }

      return true;
    });
  }

  /**
   * Ordena vendas por diferentes critérios
   */
  ordenarVendas(vendas: VendaResponse[], criterio: string, direcao: 'asc' | 'desc' = 'desc'): VendaResponse[] {
    return [...vendas].sort((a, b) => {
      let valorA: any, valorB: any;

      switch (criterio) {
        case 'data':
          valorA = new Date(a.dataVenda);
          valorB = new Date(b.dataVenda);
          break;
        case 'valor':
          valorA = a.valorTotal;
          valorB = b.valorTotal;
          break;
        case 'cliente':
          valorA = a.nomeCliente.toLowerCase();
          valorB = b.nomeCliente.toLowerCase();
          break;
        case 'status':
          valorA = a.status;
          valorB = b.status;
          break;
        default:
          return 0;
      }

      if (valorA < valorB) return direcao === 'asc' ? -1 : 1;
      if (valorA > valorB) return direcao === 'asc' ? 1 : -1;
      return 0;
    });
  }

  // ===== MÉTODOS DE CACHE E OTIMIZAÇÃO =====

  /**
   * Atualiza cache de estatísticas
   */
  atualizarEstatisticas(): void {
    this.obterEstatisticasVendas().subscribe();
  }

  /**
   * Limpa cache de estatísticas
   */
  limparCache(): void {
    this.estatisticasSubject.next(null);
  }

  // ===== MÉTODOS PARA RELATÓRIOS =====

  /**
   * Gera resumo de vendas para relatório
   */
  gerarResumoVendas(vendas: VendaResponse[]): any {
    const totalVendas = vendas.length;
    const totalFaturamento = vendas.reduce((sum, v) => sum + v.valorTotal, 0);
    const ticketMedio = this.calcularTicketMedio(totalFaturamento, totalVendas);

    const vendasPorStatus = vendas.reduce((acc, venda) => {
      acc[venda.status] = (acc[venda.status] || 0) + 1;
      return acc;
    }, {} as Record<StatusVenda, number>);

    return {
      totalVendas,
      totalFaturamento,
      ticketMedio,
      vendasPorStatus,
      periodoInicio: vendas.length > 0 ? new Date(Math.min(...vendas.map(v => new Date(v.dataVenda).getTime()))) : null,
      periodoFim: vendas.length > 0 ? new Date(Math.max(...vendas.map(v => new Date(v.dataVenda).getTime()))) : null
    };
  }
}