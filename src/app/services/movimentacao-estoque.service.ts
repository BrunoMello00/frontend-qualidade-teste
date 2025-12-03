import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

import {
  TipoMovimentacao,
  MovimentacaoEstoqueRequest,
  EntradaEstoqueRequest,
  SaidaEstoqueRequest,
  AjusteEstoqueRequest,
  MovimentacaoEstoqueResponse,
  MovimentacaoResumoResponse,
  EstatisticasMovimentacaoResponse,
  HistoricoEstoqueResponse,
  FiltroMovimentacaoRequest,
  PageableMovimentacaoResponse,
  TIPOS_MOVIMENTACAO_OPCOES,
  MOTIVOS_PADRAO
} from '../models/movimentacao-estoque.models';

@Injectable({
  providedIn: 'root'
})
export class MovimentacaoEstoqueService {
  private readonly apiUrl = `${environment.apiUrl}/estoque`;  // CORRIGIDO: URL base do controller
  
  private movimentacoesSubject = new BehaviorSubject<MovimentacaoEstoqueResponse[]>([]);
  public movimentacoes$ = this.movimentacoesSubject.asObservable();

  constructor(private http: HttpClient) {}

  // ===== OPERAÇÕES CRUD =====

  /**
   * Lista movimentações com filtros e paginação
   */
  listarMovimentacoes(filtros?: FiltroMovimentacaoRequest): Observable<PageableMovimentacaoResponse> {
    let params = new HttpParams();
    
    if (filtros) {
      if (filtros.produtoId) params = params.set('produtoId', filtros.produtoId.toString());
      if (filtros.tipo) params = params.set('tipo', filtros.tipo);
      if (filtros.dataInicio) params = params.set('dataInicio', this.formatDate(filtros.dataInicio));
      if (filtros.dataFim) params = params.set('dataFim', this.formatDate(filtros.dataFim));
      if (filtros.usuarioId) params = params.set('usuarioId', filtros.usuarioId.toString());
      if (filtros.page !== undefined) params = params.set('page', filtros.page.toString());
      if (filtros.size !== undefined) params = params.set('size', filtros.size.toString());
    }

    return this.http.get<PageableMovimentacaoResponse>(`${this.apiUrl}/movimentacoes`, { params })
      .pipe(
        map(response => this.transformDates(response)),
        catchError(this.handleError)
      );
  }

  /**
   * Registra entrada de estoque
   */
  registrarEntrada(request: EntradaEstoqueRequest): Observable<MovimentacaoEstoqueResponse> {
    return this.http.post<MovimentacaoEstoqueResponse>(`${this.apiUrl}/entrada`, request)
      .pipe(
        map(response => this.transformDatesSingle(response)),
        catchError(this.handleError)
      );
  }

  /**
   * Registra saída de estoque
   */
  registrarSaida(request: SaidaEstoqueRequest): Observable<MovimentacaoEstoqueResponse> {
    return this.http.post<MovimentacaoEstoqueResponse>(`${this.apiUrl}/saida`, request)
      .pipe(
        map(response => this.transformDatesSingle(response)),
        catchError(this.handleError)
      );
  }

  /**
   * Registra ajuste de estoque
   */
  registrarAjuste(request: AjusteEstoqueRequest): Observable<MovimentacaoEstoqueResponse> {
    return this.http.post<MovimentacaoEstoqueResponse>(`${this.apiUrl}/ajuste`, request)
      .pipe(
        map(response => this.transformDatesSingle(response)),
        catchError(this.handleError)
      );
  }

  /**
   * Registra movimentação genérica
   */
  registrarMovimentacao(request: MovimentacaoEstoqueRequest): Observable<MovimentacaoEstoqueResponse> {
    return this.http.post<MovimentacaoEstoqueResponse>(this.apiUrl, request)
      .pipe(
        map(response => this.transformDatesSingle(response)),
        catchError(this.handleError)
      );
  }

  /**
   * Busca movimentação por ID
   */
  buscarPorId(id: number): Observable<MovimentacaoEstoqueResponse> {
    return this.http.get<MovimentacaoEstoqueResponse>(`${this.apiUrl}/${id}`)
      .pipe(
        map(response => this.transformDatesSingle(response)),
        catchError(this.handleError)
      );
  }

  // ===== CONSULTAS ESPECÍFICAS =====

  /**
   * Busca movimentações por produto
   */
  buscarPorProduto(produtoId: number, page: number = 0, size: number = 20): Observable<PageableMovimentacaoResponse> {
    const params = new HttpParams()
      .set('produtoId', produtoId.toString())
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageableMovimentacaoResponse>(`${this.apiUrl}/produto`, { params })
      .pipe(
        map(response => this.transformDates(response)),
        catchError(this.handleError)
      );
  }

  /**
   * Busca últimas movimentações
   */
  buscarUltimasMovimentacoes(limit: number = 10): Observable<MovimentacaoResumoResponse[]> {
    const params = new HttpParams().set('limit', limit.toString());
    
    return this.http.get<MovimentacaoResumoResponse[]>(`${this.apiUrl}/ultimas`, { params })
      .pipe(
        map(response => response.map(item => this.transformDatesSingleResumo(item))),
        catchError(this.handleError)
      );
  }

  /**
   * Busca movimentações por período
   */
  buscarPorPeriodo(dataInicio: Date, dataFim: Date): Observable<MovimentacaoEstoqueResponse[]> {
    const params = new HttpParams()
      .set('dataInicio', this.formatDate(dataInicio))
      .set('dataFim', this.formatDate(dataFim));

    return this.http.get<MovimentacaoEstoqueResponse[]>(`${this.apiUrl}/periodo`, { params })
      .pipe(
        map(response => response.map(item => this.transformDatesSingle(item))),
        catchError(this.handleError)
      );
  }

  /**
   * Obtém estatísticas de movimentação
   */
  obterEstatisticas(dataInicio?: Date, dataFim?: Date): Observable<EstatisticasMovimentacaoResponse> {
    let params = new HttpParams();
    
    if (dataInicio) params = params.set('dataInicio', this.formatDate(dataInicio));
    if (dataFim) params = params.set('dataFim', this.formatDate(dataFim));

    return this.http.get<EstatisticasMovimentacaoResponse>(`${this.apiUrl}/estatisticas`, { params })
      .pipe(catchError(this.handleError));
  }

  /**
   * Obtém histórico completo de um produto
   */
  obterHistoricoProduto(produtoId: number): Observable<HistoricoEstoqueResponse> {
    return this.http.get<HistoricoEstoqueResponse>(`${this.apiUrl}/historico/${produtoId}`)
      .pipe(
        map(response => ({
          ...response,
          movimentacoes: response.movimentacoes.map(item => this.transformDatesSingleResumo(item))
        })),
        catchError(this.handleError)
      );
  }

  // ===== UTILITÁRIOS =====

  /**
   * Formatar tipo de movimentação para exibição
   */
  formatarTipo(tipo: TipoMovimentacao): string {
    const opcao = TIPOS_MOVIMENTACAO_OPCOES.find(op => op.value === tipo);
    return opcao ? opcao.label : tipo;
  }

  /**
   * Formatar quantidade com sinal para exibição
   */
  formatarQuantidade(quantidade: number, tipo: TipoMovimentacao): string {
    const sinal = this.isEntrada(tipo) ? '+' : '-';
    return `${sinal}${Math.abs(quantidade)}`;
  }

  /**
   * Calcular variação entre quantidade anterior e atual
   */
  calcularVariacao(quantidadeAnterior: number, quantidadeAtual: number): number {
    return quantidadeAtual - quantidadeAnterior;
  }

  /**
   * Verificar se é entrada
   */
  isEntrada(tipo: TipoMovimentacao): boolean {
    return tipo === TipoMovimentacao.ENTRADA || tipo === TipoMovimentacao.DEVOLUCAO;
  }

  /**
   * Verificar se é saída
   */
  isSaida(tipo: TipoMovimentacao): boolean {
    return tipo === TipoMovimentacao.SAIDA || tipo === TipoMovimentacao.PERDA;
  }

  /**
   * Obter opções de tipos de movimentação
   */
  getTiposMovimentacao() {
    return TIPOS_MOVIMENTACAO_OPCOES;
  }

  /**
   * Obter motivos padrão por tipo
   */
  getMotivosPadrao(tipo: TipoMovimentacao): string[] {
    return MOTIVOS_PADRAO[tipo] || [];
  }

  /**
   * Obter cor para exibição baseada no tipo
   */
  getCorTipo(tipo: TipoMovimentacao): string {
    const opcao = TIPOS_MOVIMENTACAO_OPCOES.find(op => op.value === tipo);
    return opcao ? opcao.color : 'secondary';
  }

  /**
   * Obter ícone para exibição baseado no tipo
   */
  getIconeTipo(tipo: TipoMovimentacao): string {
    const opcao = TIPOS_MOVIMENTACAO_OPCOES.find(op => op.value === tipo);
    return opcao ? opcao.icon : 'bi-question-circle';
  }

  // ===== MÉTODOS PRIVADOS =====

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  private transformDates(response: PageableMovimentacaoResponse): PageableMovimentacaoResponse {
    return {
      ...response,
      content: response.content.map(item => this.transformDatesSingle(item))
    };
  }

  private transformDatesSingle(item: MovimentacaoEstoqueResponse): MovimentacaoEstoqueResponse {
    return {
      ...item,
      dataMovimentacao: new Date(item.dataMovimentacao)
    };
  }

  private transformDatesSingleResumo(item: MovimentacaoResumoResponse): MovimentacaoResumoResponse {
    return {
      ...item,
      dataMovimentacao: new Date(item.dataMovimentacao)
    };
  }

  private handleError = (error: any): Observable<any> => {
    console.error('Erro no MovimentacaoEstoqueService:', error);
    throw error;
  };

  // ===== MÉTODOS DE VALIDAÇÃO =====

  /**
   * Validar entrada de estoque
   */
  validarEntrada(request: EntradaEstoqueRequest): { valido: boolean; erros: string[] } {
    const erros: string[] = [];

    if (!request.produtoId || request.produtoId <= 0) {
      erros.push('Produto é obrigatório');
    }

    if (!request.quantidade || request.quantidade <= 0) {
      erros.push('Quantidade deve ser maior que zero');
    }

    if (!request.motivo || request.motivo.trim().length === 0) {
      erros.push('Motivo é obrigatório');
    }

    return { valido: erros.length === 0, erros };
  }

  /**
   * Validar saída de estoque
   */
  validarSaida(request: SaidaEstoqueRequest): { valido: boolean; erros: string[] } {
    const erros: string[] = [];

    if (!request.produtoId || request.produtoId <= 0) {
      erros.push('Produto é obrigatório');
    }

    if (!request.quantidade || request.quantidade <= 0) {
      erros.push('Quantidade deve ser maior que zero');
    }

    if (!request.motivo || request.motivo.trim().length === 0) {
      erros.push('Motivo é obrigatório');
    }

    return { valido: erros.length === 0, erros };
  }

  /**
   * Validar ajuste de estoque
   */
  validarAjuste(request: AjusteEstoqueRequest): { valido: boolean; erros: string[] } {
    const erros: string[] = [];

    if (!request.produtoId || request.produtoId <= 0) {
      erros.push('Produto é obrigatório');
    }

    if (request.quantidadeNova === undefined || request.quantidadeNova < 0) {
      erros.push('Quantidade nova deve ser maior ou igual a zero');
    }

    if (!request.motivo || request.motivo.trim().length === 0) {
      erros.push('Motivo é obrigatório');
    }

    return { valido: erros.length === 0, erros };
  }
}