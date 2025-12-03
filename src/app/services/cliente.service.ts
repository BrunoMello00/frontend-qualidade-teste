import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Cliente {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  cpf: string;
  dataNascimento?: string;
  endereco?: {
    cep: string;
    rua: string;
    numero: string;
    complemento?: string;
    bairro: string;
    cidade: string;
    estado: string;
  };
  categoria: 'BRONZE' | 'PRATA' | 'OURO' | 'DIAMANTE';
  pontos: number;
  totalGasto: number;
  totalCompras: number; // Adicionado para compatibilidade com template
  ultimaCompra?: string;
  dataCadastro: string;
  ativo: boolean;
  observacoes?: string;
}

export interface ClienteRequest {
  nome: string;
  email: string;
  telefone: string;
  cpf: string;
  dataNascimento?: string;
  endereco?: {
    cep: string;
    rua: string;
    numero: string;
    complemento?: string;
    bairro: string;
    cidade: string;
    estado: string;
  };
  observacoes?: string;
}

export interface ClienteResumo {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  cpf: string;
  categoria: 'BRONZE' | 'PRATA' | 'OURO' | 'DIAMANTE'; // Corrigido para usar enum
  pontos: number;
  totalGasto: number;
  totalCompras: number; // Adicionado para compatibilidade
  ultimaCompra?: string;
  dataCadastro: string; // Adicionado para compatibilidade
  ativo: boolean;
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

export interface FiltroClientes {
  termo?: string;
  categoria?: string;
  ativo?: boolean;
  dataCadastroInicio?: string;
  dataCadastroFim?: string;
  ultimaCompraInicio?: string;
  ultimaCompraFim?: string;
  pontosMinimos?: number;
  pontosMaximos?: number;
  orderBy?: string;
  orderDirection?: string;
}

export interface EstatisticasCliente {
  totalClientes: number;
  clientesAtivos: number;
  clientesInativos: number;
  totalPontos: number;
  mediaGastoPorCliente: number;
  clientesMaisFrequentes: Array<{
    id: number;
    nome: string;
    totalCompras: number;
    valorTotal: number;
  }>;
  distribuicaoCategorias: Array<{
    categoria: string;
    quantidade: number;
    percentual: number;
  }>;
  novosClientesMes: number;
  aniversariantesHoje: number;
}

export interface HistoricoPontos {
  id: number;
  tipo: 'ADICAO' | 'REMOCAO' | 'RESGATE';
  pontos: number;
  descricao: string;
  data: string;
  vendaId?: number;
}

export interface EstadosResponse {
  estados: Array<{
    sigla: string;
    nome: string;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class ClienteService {
  private baseUrl = environment.apiUrl;
  private apiUrl = `${environment.apiUrl}/clientes`;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('auth_token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    });
  }

  private getHttpOptions() {
    return { headers: this.getHeaders() };
  }

  // ===================================
  // ===================================

  criarCliente(cliente: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>(`${this.baseUrl}/clientes`, cliente, this.getHttpOptions());
  }

  buscarPorId(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.baseUrl}/clientes/${id}`, this.getHttpOptions());
  }

  buscarPorCpf(cpf: string): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.baseUrl}/clientes/cpf/${cpf}`, this.getHttpOptions());
  }

  atualizarCliente(id: number, cliente: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.baseUrl}/clientes/${id}`, cliente, this.getHttpOptions());
  }

  deletarCliente(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/clientes/${id}`, this.getHttpOptions());
  }

  reativarCliente(id: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/clientes/${id}/reativar`, {}, this.getHttpOptions());
  }

  // ===================================
  // ===================================

  listarClientes(filtro: FiltroClientes = {}, page: number = 0, size: number = 20): Observable<PaginatedResponse<ClienteResumo>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filtro.termo) params = params.set('termo', filtro.termo);
    if (filtro.categoria) params = params.set('categoria', filtro.categoria);
    if (filtro.ativo !== undefined) params = params.set('ativo', filtro.ativo.toString());
    if (filtro.dataCadastroInicio) params = params.set('dataCadastroInicio', filtro.dataCadastroInicio);
    if (filtro.dataCadastroFim) params = params.set('dataCadastroFim', filtro.dataCadastroFim);
    if (filtro.ultimaCompraInicio) params = params.set('ultimaCompraInicio', filtro.ultimaCompraInicio);
    if (filtro.ultimaCompraFim) params = params.set('ultimaCompraFim', filtro.ultimaCompraFim);
    if (filtro.pontosMinimos) params = params.set('pontosMinimos', filtro.pontosMinimos.toString());
    if (filtro.pontosMaximos) params = params.set('pontosMaximos', filtro.pontosMaximos.toString());
    if (filtro.orderBy) params = params.set('orderBy', filtro.orderBy);
    if (filtro.orderDirection) params = params.set('orderDirection', filtro.orderDirection);

    return this.http.get<PaginatedResponse<ClienteResumo>>(`${this.baseUrl}/clientes`, { 
      ...this.getHttpOptions(), 
      params 
    });
  }

  buscarPorTermo(termo: string): Observable<ClienteResumo[]> {
    const params = new HttpParams().set('termo', termo);
    return this.http.get<ClienteResumo[]>(`${this.baseUrl}/clientes/buscar`, { 
      ...this.getHttpOptions(), 
      params 
    });
  }

  // ===== CLIENTE FAKE PARA "CONTINUAR SEM CADASTRO" =====
  
  /**
   * Busca ou cria o cliente fake usado para "Continuar sem cadastro"
   * Cliente fake tem CPF 000.000.000-00
   */
  buscarClienteFake(): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.apiUrl}/fake`);
  }

  // ===================================
  // ===================================

  adicionarPontos(clienteId: number, pontos: number, descricao: string): Observable<void> {
    const request = { pontos, descricao };
    return this.http.post<void>(`${this.baseUrl}/clientes/${clienteId}/pontos/adicionar`, request, this.getHttpOptions());
  }

  removerPontos(clienteId: number, pontos: number, descricao: string): Observable<void> {
    const request = { pontos, descricao };
    return this.http.post<void>(`${this.baseUrl}/clientes/${clienteId}/pontos/remover`, request, this.getHttpOptions());
  }

  buscarHistoricoPontos(clienteId: number): Observable<HistoricoPontos[]> {
    return this.http.get<HistoricoPontos[]>(`${this.baseUrl}/clientes/${clienteId}/historico-pontos`, this.getHttpOptions());
  }

  // ===================================
  // ===================================

  registrarCompra(clienteId: number, valorCompra: number, vendaId?: number): Observable<void> {
    let params = new HttpParams().set('valorCompra', valorCompra.toString());
    if (vendaId) params = params.set('vendaId', vendaId.toString());

    return this.http.post<void>(`${this.baseUrl}/clientes/${clienteId}/registrar-compra`, {}, { 
      ...this.getHttpOptions(), 
      params 
    });
  }

  // ===================================
  // ===================================

  obterEstatisticas(): Observable<EstatisticasCliente> {
    return this.http.get<EstatisticasCliente>(`${this.baseUrl}/clientes/estatisticas`, this.getHttpOptions());
  }

  buscarAniversariantes(): Observable<ClienteResumo[]> {
    return this.http.get<ClienteResumo[]>(`${this.baseUrl}/clientes/aniversariantes`, this.getHttpOptions());
  }

  obterResumoClientes(): Observable<EstatisticasCliente> {
    return this.http.get<EstatisticasCliente>(`${this.baseUrl}/clientes/dashboard/resumo`, this.getHttpOptions());
  }

  obterTopClientes(): Observable<Array<{id: number, nome: string, totalCompras: number, valorTotal: number}>> {
    return this.http.get<Array<{id: number, nome: string, totalCompras: number, valorTotal: number}>>(`${this.baseUrl}/clientes/top-clientes`, this.getHttpOptions());
  }

  obterDistribuicaoCategorias(): Observable<Array<{categoria: string, quantidade: number, percentual: number}>> {
    return this.http.get<Array<{categoria: string, quantidade: number, percentual: number}>>(`${this.baseUrl}/clientes/categorias/distribuicao`, this.getHttpOptions());
  }

  // ===================================
  // ===================================

  validarCpf(cpf: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/clientes/validar-cpf/${cpf}`, this.getHttpOptions());
  }

  // ===================================
  // ===================================

  healthCheck(): Observable<string> {
    return this.http.get<string>(`${this.baseUrl}/clientes/health`, this.getHttpOptions());
  }

  // ===================================
  // ===================================

  formatarCpf(cpf: string): string {
    return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  }

  formatarTelefone(telefone: string): string {
    const numero = telefone.replace(/\D/g, '');
    if (numero.length === 11) {
      return numero.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    } else if (numero.length === 10) {
      return numero.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
    }
    return telefone;
  }

  formatarCategoria(categoria: string): string {
    switch (categoria.toUpperCase()) {
      case 'BRONZE': return 'Bronze';
      case 'PRATA': return 'Prata';
      case 'OURO': return 'Ouro';
      case 'DIAMANTE': return 'Diamante';
      default: return categoria;
    }
  }

  getCorCategoria(categoria: string): string {
    switch (categoria.toUpperCase()) {
      case 'BRONZE': return '#CD7F32';
      case 'PRATA': return '#C0C0C0';
      case 'OURO': return '#FFD700';
      case 'DIAMANTE': return '#B9F2FF';
      default: return '#6c757d';
    }
  }

  calcularCategoria(totalGasto: number): string {
    if (totalGasto >= 10000) return 'DIAMANTE';
    if (totalGasto >= 5000) return 'OURO';
    if (totalGasto >= 1000) return 'PRATA';
    return 'BRONZE';
  }

  formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  }

  formatarData(data: string | Date): string {
    const dateObj = typeof data === 'string' ? new Date(data) : data;
    return new Intl.DateTimeFormat('pt-BR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).format(dateObj);
  }

  formatarDataHora(data: string | Date): string {
    const dateObj = typeof data === 'string' ? new Date(data) : data;
    return new Intl.DateTimeFormat('pt-BR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(dateObj);
  }
}