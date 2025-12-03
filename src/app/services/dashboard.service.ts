import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface ProdutoBaixoEstoque {
  produtoId: number;
  nome: string;
  quantidadeAtual: number;
  estoqueMinimo: number;
  categoria: string;
  preco: number;
  status: string;
  id?: number;
  estoqueAtual?: number;
  quantidade?: number;
  estoque?: number;
  quantidadeMinima?: number;
}

export interface VendaRecente {
  vendaId: number;
  nomeCliente: string;
  valorTotal: number;
  dataVenda: string;
  status: string;
  quantidadeItens: number;
  id?: number;
  cliente?: string;
  valor?: number;
  data?: string;
}

export interface VendaSemana {
  data: string;
  valor: number;
  quantidade: number;
}

export interface TopProduto {
  produtoId: number;
  nome: string;
  quantidadeVendida: number;
  faturamento: number;
  categoria: string;
  precoUnitario: number;
}

export interface DashboardStats {
  totalVendas: number;
  faturamentoMes: number;
  faturamentoDia: number;
  totalProdutos: number;
  produtosEstoqueBaixo: number;
  ticketMedio: number;
  crescimentoMes: number;
  vendasPendentes: number;
  totalClientes: number;
  totalUsuarios: number;
  eventosAtivos: number;
  clientesNovosHoje: number;
  vendasDia?: number;
  vendasMes?: number;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/dashboard/dados`);
  }

  getVendasRecentes(): Observable<VendaRecente[]> {
    return this.http.get<any>(`${this.apiUrl}/dashboard/vendas-recentes`).pipe(
      map(response => response.vendas)
    );
  }

  getProdutosBaixoEstoque(): Observable<ProdutoBaixoEstoque[]> {
    return this.http.get<any>(`${this.apiUrl}/dashboard/estoque-baixo`).pipe(
      map(response => response.produtos)
    );
  }

  getVendasSemana(): Observable<VendaSemana[]> {
    return this.http.get<any>(`${this.apiUrl}/dashboard/vendas-semana`).pipe(
      map(response => response.vendas)
    );
  }

  getTopProdutos(): Observable<TopProduto[]> {
    return this.http.get<any>(`${this.apiUrl}/dashboard/top-produtos`).pipe(
      map(response => response.produtos)
    );
  }
}