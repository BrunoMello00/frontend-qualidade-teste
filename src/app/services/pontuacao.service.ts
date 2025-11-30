import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// ========== INTERFACES ==========

export interface CategoriaConfig {
  id?: number;
  nome: string;
  descricao?: string;
  pontosMinimos: number;
  pontosMaximos?: number;
  pontosIniciais: number;
  cor: string;
  ativo?: boolean;
  ordem?: number;
  dataCriacao?: Date;
  dataAtualizacao?: Date;
}

export interface Recompensa {
  id?: number;
  nome: string;
  descricao?: string;
  pontosNecessarios: number;
  categoria?: string;
  valorDesconto?: number;
  percentualDesconto?: number;
  ativo?: boolean;
  quantidadeDisponivel?: number;
  quantidadeResgatada?: number;
  dataValidade?: Date;
  dataCriacao?: Date;
  disponivel?: boolean;
  temEstoque?: boolean;
}

export interface ClientePontuacao {
  id: number;
  nome: string;
  email: string;
  pontos: number;
  categoriaAtual: string;
  corCategoria: string;
  proximaCategoria?: string;
  pontosProximaCategoria?: number;
  totalCompras: number;
  quantidadeCompras: number;
  ultimaCompra?: Date;
  pontosParaProximaCategoria?: number;
  progressoCategoria?: number;
}

export interface EstatisticasPontuacao {
  totalClientes: number;
  totalPontosAtivos: number;
  mediaPontosCliente: number;
  totalResgates: number;
  totalRecompensasAtivas: number;
  distribuicaoCategoria?: Array<{
    categoria: string;
    cor: string;
    quantidade: number;
    percentual: number;
  }>;
}

export interface TransacaoPontos {
  clienteId: number;
  pontos: number;
  motivo: string;
  observacoes?: string;
  vendaId?: number;
  produtoId?: number;
}

export interface ResgatePontos {
  clienteId: number;
  recompensaId: number;
  observacoes?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PontuacaoService {
  private baseUrl = `${environment.apiUrl}/pontuacao`;

  constructor(private http: HttpClient) { }

  // ========== GESTÃO DE CATEGORIAS ==========

  criarCategoria(categoria: CategoriaConfig): Observable<CategoriaConfig> {
    return this.http.post<CategoriaConfig>(`${this.baseUrl}/categorias`, categoria);
  }

  atualizarCategoria(id: number, categoria: CategoriaConfig): Observable<CategoriaConfig> {
    return this.http.put<CategoriaConfig>(`${this.baseUrl}/categorias/${id}`, categoria);
  }

  removerCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/categorias/${id}`);
  }

  listarCategorias(): Observable<CategoriaConfig[]> {
    return this.http.get<CategoriaConfig[]>(`${this.baseUrl}/categorias`);
  }

  buscarCategoriaPorId(id: number): Observable<CategoriaConfig> {
    return this.http.get<CategoriaConfig>(`${this.baseUrl}/categorias/${id}`);
  }

  // ========== GESTÃO DE RECOMPENSAS ==========

  criarRecompensa(recompensa: Recompensa): Observable<Recompensa> {
    return this.http.post<Recompensa>(`${this.baseUrl}/recompensas`, recompensa);
  }

  atualizarRecompensa(id: number, recompensa: Recompensa): Observable<Recompensa> {
    return this.http.put<Recompensa>(`${this.baseUrl}/recompensas/${id}`, recompensa);
  }

  removerRecompensa(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/recompensas/${id}`);
  }

  listarRecompensas(): Observable<Recompensa[]> {
    return this.http.get<Recompensa[]>(`${this.baseUrl}/recompensas`);
  }

  listarRecompensasDisponiveis(clienteId: number): Observable<Recompensa[]> {
    return this.http.get<Recompensa[]>(`${this.baseUrl}/recompensas/cliente/${clienteId}`);
  }

  // ========== GESTÃO DE PONTOS ==========

  listarClientesComPontuacao(page: number = 0, size: number = 20): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/clientes?page=${page}&size=${size}`);
  }

  obterEstatisticas(): Observable<EstatisticasPontuacao> {
    return this.http.get<EstatisticasPontuacao>(`${this.baseUrl}/estatisticas`);
  }

  recalcularCategorias(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/recalcular-categorias`, {});
  }

  // ========== MÉTODOS AUXILIARES ==========

  formatarPontos(pontos: number): string {
    return `${pontos.toLocaleString('pt-BR')} pts`;
  }

  calcularProgressoCategoria(pontosAtuais: number, pontosProximaCategoria?: number): number {
    if (!pontosProximaCategoria) return 100;
    return Math.min(100, (pontosAtuais / pontosProximaCategoria) * 100);
  }

  formatarPercentual(valor: number): string {
    return `${valor.toFixed(1)}%`;
  }

  obterCorCategoria(categoria: string): string {
    const cores: { [key: string]: string } = {
      'BRONZE': '#CD7F32',
      'PRATA': '#C0C0C0',
      'OURO': '#FFD700',
      'DIAMANTE': '#B9F2FF'
    };
    return cores[categoria.toUpperCase()] || '#999999';
  }
}