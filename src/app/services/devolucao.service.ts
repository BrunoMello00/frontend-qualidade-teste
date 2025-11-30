import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ItemDevolucaoRequest {
  itemVendaId: number;
  quantidade: number;
  statusQualidade: 'NORMAL' | 'DEFEITUOSO' | 'INDISPONIVEL';
  percentualDesconto?: number;
}

export interface DevolucaoRequest {
  vendaId: number;
  tipoDevolucao: 'DEVOLUCAO_SIMPLES' | 'TROCA_POR_DEFEITO' | 'TROCA_POR_TAMANHO' | 'PRODUTO_INCORRETO';
  motivo: string;
  observacoes?: string;
  itens: ItemDevolucaoRequest[];
}

export interface ItemDevolucaoResponse {
  id: number;
  itemVendaId: number;
  quantidade: number;
  statusQualidade: string;
  percentualDesconto?: number;
  valorOriginal: number;
  valorDevolvido: number;
  produto: {
    id: number;
    nome: string;
    codigoBarras?: string;
  };
}

export interface DevolucaoResponse {
  id: number;
  vendaId: number;
  tipoDevolucao: string;
  motivo: string;
  observacoes?: string;
  valorTotal: number;
  dataDevolucao: string;
  usuarioId: number;
  nomeUsuario: string;
  status: string;
  itens: ItemDevolucaoResponse[];
}

export interface EstatisticasDevolucaoResponse {
  totalDevolucoes: number;
  valorTotalDevolucoes: number;
  devolucoesPorTipo: { [tipo: string]: number };
  produtosComDefeito: number;
  ticketMedioDevolucao: number;
}

@Injectable({
  providedIn: 'root'
})
export class DevolucaoService {
  private readonly API_URL = `${environment.apiUrl}/api/devolucoes`;

  constructor(private http: HttpClient) {}

  private getHttpOptions() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      })
    };
  }

  /**
   * Processa uma devolução
   */
  processarDevolucao(devolucao: DevolucaoRequest): Observable<DevolucaoResponse> {
    return this.http.post<DevolucaoResponse>(this.API_URL, devolucao, this.getHttpOptions());
  }

  /**
   * Lista todas as devoluções com filtros opcionais
   */
  listarDevolucoes(
    page: number = 0,
    size: number = 10,
    vendaId?: number,
    tipoDevolucao?: string,
    dataInicio?: string,
    dataFim?: string
  ): Observable<{ content: DevolucaoResponse[], totalElements: number, totalPages: number }> {
    let params = `?page=${page}&size=${size}`;
    
    if (vendaId) params += `&vendaId=${vendaId}`;
    if (tipoDevolucao) params += `&tipoDevolucao=${tipoDevolucao}`;
    if (dataInicio) params += `&dataInicio=${dataInicio}`;
    if (dataFim) params += `&dataFim=${dataFim}`;

    return this.http.get<{ content: DevolucaoResponse[], totalElements: number, totalPages: number }>(
      `${this.API_URL}${params}`, 
      this.getHttpOptions()
    );
  }

  /**
   * Busca uma devolução específica por ID
   */
  buscarDevolucaoPorId(id: number): Observable<DevolucaoResponse> {
    return this.http.get<DevolucaoResponse>(`${this.API_URL}/${id}`, this.getHttpOptions());
  }

  /**
   * Lista devoluções de uma venda específica
   */
  listarDevolucoesPorVenda(vendaId: number): Observable<DevolucaoResponse[]> {
    return this.http.get<DevolucaoResponse[]>(
      `${this.API_URL}/por-venda/${vendaId}`, 
      this.getHttpOptions()
    );
  }

  /**
   * Obtém estatísticas de devolução
   */
  obterEstatisticas(
    dataInicio?: string,
    dataFim?: string
  ): Observable<EstatisticasDevolucaoResponse> {
    let params = '';
    if (dataInicio && dataFim) {
      params = `?dataInicio=${dataInicio}&dataFim=${dataFim}`;
    }

    return this.http.get<EstatisticasDevolucaoResponse>(
      `${this.API_URL}/estatisticas${params}`, 
      this.getHttpOptions()
    );
  }

  /**
   * Lista produtos com defeito para controle de qualidade
   */
  listarProdutosComDefeito(): Observable<{
    produtoId: number;
    nomeProduto: string;
    quantidadeDefeituosa: number;
    valorEstimadoDesconto: number;
  }[]> {
    return this.http.get<{
      produtoId: number;
      nomeProduto: string;
      quantidadeDefeituosa: number;
      valorEstimadoDesconto: number;
    }[]>(`${this.API_URL}/produtos-defeituosos`, this.getHttpOptions());
  }

  /**
   * Cancela uma devolução (se permitido)
   */
  cancelarDevolucao(id: number, motivo: string): Observable<void> {
    return this.http.post<void>(
      `${this.API_URL}/${id}/cancelar`, 
      { motivo }, 
      this.getHttpOptions()
    );
  }
}