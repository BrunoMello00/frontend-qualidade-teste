import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseService } from './base.service';
import { environment } from '../../environments/environment';
import { MockDataService } from './mock-data.service';
import { EnvironmentService } from './environment.service';

export interface ItemVenda {
  produtoId: number;
  tamanho?: string;
  quantidade: number;
  precoUnitario: number;
  precoTotal?: number;
}

export interface Venda {
  id?: number;
  dataVenda?: string;
  clienteNome?: string;
  clienteCpf?: string;
  clienteEmail?: string;
  clienteTelefone?: string;
  evento?: string;
  itens: ItemVenda[];
  subtotal: number;
  desconto: number;
  percentualDesconto?: number;
  total: number;
  formaPagamento: string;
  parcelas?: number;
  status?: 'PENDENTE' | 'CONFIRMADA' | 'ENTREGUE' | 'CANCELADA';
  vendedorId?: any;
  vendedorNome?: string;
  observacoes?: string;
  pontosFidelidade?: number;
  dataEntrega?: string | null;
  numeroNF?: string;
}

export interface VendaResponse {
  content: Venda[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface EstatisticasVenda {
  vendasDia: number;
  vendasMes: number;
  vendasAno: number;
  totalVendas: number;
  faturamentoDia: number;
  faturamentoMes: number;
  faturamentoAno: number;
  ticketMedio: number;
}

export interface TopProdutoResponse {
  produtoId: number;
  produtoNome: string;
  categoria?: string;
  quantidadeVendida: number;
  valorTotal: number;
  percentualVendas?: number;
}

export interface CriarVendaRequest {
  clienteId?: number;
  clienteNome?: string;
  clienteCpf?: string;
  clienteEmail?: string;
  clienteTelefone?: string;
  evento?: string;
  itens: {
    produtoId: number;
    tamanho?: string;
    quantidade: number;
    precoUnitario: number;
    descontoItem?: number;
  }[];
  desconto?: number;
  formaPagamento: string;
  parcelas?: number;
  observacoes?: string;
  vendedorId?: any;
  dataEntrega?: string | null;
}

@Injectable({ providedIn: 'root' })
export class VendaService extends BaseService {
  constructor(http: HttpClient, environmentService: EnvironmentService, private mock: MockDataService) {
    super(http, environmentService);
  }

  listarVendas(page: number = 0, size: number = 10, dataInicio?: string | Date, dataFim?: string | Date, status?: string, vendedorId?: string): Observable<VendaResponse> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.listarVendas(page, size, dataInicio, dataFim, status, vendedorId);
    const params: any = { page, size };
    if (dataInicio) params.dataInicio = dataInicio;
    if (dataFim) params.dataFim = dataFim;
    if (status) params.status = status;
    if (vendedorId) params.vendedorId = vendedorId;
    return this.get<VendaResponse>('/vendas', params);
  }

  buscarPorId(id: number): Observable<Venda> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.buscarVendaPorId(Number(id));
    return this.get<Venda>(`/vendas/${id}`);
  }

  criarVenda(venda: CriarVendaRequest): Observable<Venda> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.criarVenda(venda);
    return this.post<Venda>('/vendas', venda);
  }

  confirmarVenda(id: number): Observable<any> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.confirmarVenda(Number(id));
    return this.post<any>(`/vendas/${id}/confirmar`, {});
  }

  cancelarVenda(id: number, motivo?: string): Observable<any> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.cancelarVenda(Number(id), motivo);
    return this.post<any>(`/vendas/${id}/cancelar`, { motivo });
  }

  obterEstatisticasVendas(vendedorId?: string, dataInicio?: string | Date, dataFim?: string | Date): Observable<EstatisticasVenda> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.obterEstatisticasVendas(vendedorId, dataInicio, dataFim);
    const params: any = {};
    if (vendedorId) params.vendedorId = vendedorId;
    if (dataInicio) params.dataInicio = dataInicio;
    if (dataFim) params.dataFim = dataFim;
    return this.get<EstatisticasVenda>('/vendas/estatisticas', params);
  }

  obterTopProdutos(limit: number = 10): Observable<TopProdutoResponse[]> {
    if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.getTopProdutos(limit) as Observable<TopProdutoResponse[]>;
    return this.get<TopProdutoResponse[]>('/vendas/top-produtos', { limit: String(limit) } as any);
  }

  obterVendasRecentes(limit: number = 10): Observable<Venda[]> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.obterVendasRecentes(limit);
    return this.get<Venda[]>(`/vendas/recentes`, { limit: String(limit) } as any);
  }

  obterProdutosMaisVendidos(limit: number = 5): Observable<TopProdutoResponse[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.obterTopProdutos(limit);
    return this.get<TopProdutoResponse[]>(`/vendas/produtos-mais-vendidos`, { limit: String(limit) } as any);
  }

  atualizarStatusVenda(id: number, status: string, observacoes?: string): Observable<any> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.atualizarStatusVenda(Number(id), status, observacoes);
    return this.patch<any>(`/vendas/${id}/status`, { status, observacoes });
  }
}