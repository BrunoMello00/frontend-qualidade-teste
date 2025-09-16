import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseService } from './base.service';
import { MockDataService } from './mock-data.service';
import { EnvironmentService } from './environment.service';
import { HttpClient } from '@angular/common/http';

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  tipoUsuario: 'OWNER' | 'ADMIN' | 'VENDEDOR';
  codigoVendedor?: string;
  status: 'ATIVO' | 'INATIVO' | 'BLOQUEADO' | 'PENDENTE';
  primeiroAcesso?: boolean;
  ultimoLogin?: Date;
  tentativasLogin?: number;
  bloqueadoAte?: Date;
  dataCriacao?: Date;
  dataAtualizacao?: Date;
  criadoPor?: string;
  atualizadoPor?: string;
  avatarUrl?: string;
  telefone?: string;
  observacoes?: string;
  metaMensal?: number;
  comissaoPercentual?: number;
  ativo?: boolean;
}

export interface UsuarioResponse {
  data: Usuario[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface CriarUsuarioRequest {
  nome: string;
  email: string;
  tipoUsuario: 'OWNER' | 'ADMIN' | 'VENDEDOR';
  telefone?: string;
  observacoes?: string;
  metaMensal?: number;
  comissaoPercentual?: number;
}

export interface EstatisticasVendedor {
  id: string;
  nome: string;
  codigoVendedor: string;
  email: string;
  totalVendas: number;
  valorTotalVendas: number;
  ticketMedio: number;
  ultimaVenda?: Date;
  metaMensal: number;
  comissaoPercentual: number;
  percentualMeta: number;
}

export interface Permissao {
  permissao: {
    id: string;
    nome: string;
    descricao: string;
    modulo: string;
    acao: string;
  };
  concedida: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioService extends BaseService {
  constructor(http: HttpClient, environmentService: EnvironmentService, private mock: MockDataService) {
    super(http, environmentService);
  }

  // CRUD Básico
  listarUsuarios(
    page: number = 0, 
    limit: number = 10,
    termo?: string,
    tipoUsuario?: string,
    status?: string,
    dataInicio?: Date,
    dataFim?: Date,
    orderBy?: string,
    orderDirection?: string
  ): Observable<UsuarioResponse> {
    const params: any = { page, limit };
    if (termo) params.termo = termo;
    if (tipoUsuario) params.tipoUsuario = tipoUsuario;
    if (status) params.status = status;
    if (dataInicio) params.dataInicio = dataInicio;
    if (dataFim) params.dataFim = dataFim;
    if (orderBy) params.orderBy = orderBy;
    if (orderDirection) params.orderDirection = orderDirection;
    
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.listarUsuarios(page, limit, termo, tipoUsuario, status) as unknown as Observable<UsuarioResponse>;
    return this.get<UsuarioResponse>('/usuarios', params);
  }

  buscarPorId(id: string): Observable<Usuario> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.buscarUsuarioPorId(id) as unknown as Observable<Usuario>;
    return this.get<Usuario>(`/usuarios/${id}`);
  }

  criarUsuario(usuario: CriarUsuarioRequest): Observable<Usuario> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.criarUsuario(usuario) as unknown as Observable<Usuario>;
    return this.post<Usuario>('/usuarios', usuario);
  }

  atualizarUsuario(id: string, usuario: CriarUsuarioRequest): Observable<Usuario> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.atualizarUsuario(id, usuario) as unknown as Observable<Usuario>;
    return this.put<Usuario>(`/usuarios/${id}`, usuario);
  }

  excluirUsuario(id: string): Observable<{ success: boolean; message: string }> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.excluirUsuario(id) as unknown as Observable<{ success: boolean; message: string }>;
    return this.delete<{ success: boolean; message: string }>(`/usuarios/${id}`);
  }

  // Operações de Status
  desativarUsuario(id: string): Observable<Usuario> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.desativarUsuario(id) as Observable<Usuario>;
    return this.patch<Usuario>(`/usuarios/${id}/desativar`);
  }

  reativarUsuario(id: string): Observable<Usuario> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.reativarUsuario(id) as Observable<Usuario>;
    return this.patch<Usuario>(`/usuarios/${id}/reativar`);
  }

  // Convite por Email
  enviarConvite(usuario: CriarUsuarioRequest): Observable<{ success: boolean; message: string }> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.enviarConvite ? this.mock.enviarConvite(usuario) : new Observable(observer => { observer.next({ success: true, message: 'Convite enviado (mock)'}); observer.complete(); }) as any;
    return this.post<{ success: boolean; message: string }>('/usuarios/convite', usuario);
  }

  // Estatísticas de Vendedores
  obterEstatisticasVendedores(
    ano?: number, 
    mes?: number, 
    apenasAtivos: boolean = true
  ): Observable<EstatisticasVendedor[]> {
    const params: any = { apenasAtivos };
    if (ano) params.ano = ano;
    if (mes) params.mes = mes;
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.obterEstatisticasVendedores(ano, mes, apenasAtivos) as unknown as Observable<EstatisticasVendedor[]>;
    return this.get<EstatisticasVendedor[]>('/usuarios/estatisticas-vendedores', params);
  }

  // Permissões
  obterPermissoes(id: string): Observable<Permissao[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.obterPermissoes(id) as Observable<Permissao[]>;
    return this.get<Permissao[]>(`/usuarios/${id}/permissoes`);
  }

  atualizarPermissao(id: string, permissaoId: string, concedida: boolean): Observable<{ success: boolean; message: string }> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.atualizarPermissao ? this.mock.atualizarPermissao(id, permissaoId, concedida) : of({ success: true, message: 'Permissão atualizada (mock)' });
    return this.post<{ success: boolean; message: string }>(`/usuarios/${id}/permissoes`, { permissaoId, concedida });
  }

  // Métodos de conveniência
  listarVendedores(): Observable<Usuario[]> {
    return this.listarUsuarios(0, 100, undefined, 'VENDEDOR', 'ATIVO').pipe(
      map((response: UsuarioResponse) => response.data)
    );
  }

  buscarVendedorPorCodigo(codigo: string): Observable<Usuario | null> {
    return this.listarVendedores().pipe(
      map((vendedores: Usuario[]) => 
        vendedores.find(v => v.codigoVendedor === codigo) || null
      )
    );
  }

  contarUsuarios(): Observable<number> {
    return this.listarUsuarios(0, 1).pipe(
      map((response: UsuarioResponse) => response.total)
    );
  }

  contarVendedores(): Observable<number> {
    return this.listarUsuarios(0, 1, undefined, 'VENDEDOR', 'ATIVO').pipe(
      map((response: UsuarioResponse) => response.total)
    );
  }
}