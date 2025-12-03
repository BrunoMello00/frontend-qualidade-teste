import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

// ===================================
// ===================================

export enum TipoUsuario {
  OWNER = 'OWNER',
  ADMIN = 'ADMIN',
  VENDEDOR = 'VENDEDOR',
  ESTOQUISTA = 'ESTOQUISTA'
}

export enum StatusUsuario {
  ATIVO = 'ATIVO',
  INATIVO = 'INATIVO',
  BLOQUEADO = 'BLOQUEADO',
  PENDENTE = 'PENDENTE'
}

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  tipoUsuario: TipoUsuario;
  ativo: boolean;
  bloqueado: boolean;
  telefone?: string;
  ultimoLogin?: Date;
  dataCriacao: Date;
  dataAtualizacao?: Date;
  metaMensal?: number;
  comissaoPercentual?: number;
  observacoes?: string;
  criadoPor?: string;
  permissoes?: string[];
}

export interface NovoUsuario {
  nome: string;
  email: string;
  senha: string;
  confirmarSenha: string;
  tipoUsuario: TipoUsuario;
  telefone?: string;
  metaMensal?: number;
  comissaoPercentual?: number;
  observacoes?: string;
}

export interface AtualizarUsuario {
  nome?: string;
  email?: string;
  telefone?: string;
  tipoUsuario?: TipoUsuario;
  metaMensal?: number;
  comissaoPercentual?: number;
  observacoes?: string;
}

export interface AlterarSenhaRequest {
  senhaAtual: string;
  novaSenha: string;
  confirmarNovaSenha: string;
}

export interface FiltrosUsuario {
  termo?: string;
  tipoUsuario?: TipoUsuario;
  ativo?: boolean;
  page?: number;
  size?: number;
  orderBy?: string;
  orderDirection?: 'ASC' | 'DESC';
}

export interface UsuarioPaginado {
  content: Usuario[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface EstatisticasUsuario {
  totalUsuarios: number;
  usuariosAtivos: number;
  usuariosInativos: number;
  usuariosBloqueados: number;
  totalAdmins: number;
  totalVendedores: number;
  usuariosOnlineHoje: number;
  novosCadastrosUltimos30Dias: number;
}

export interface PermissaoUsuario {
  id: number;
  modulo: string;
  acao: string;
  descricao: string;
  ativo: boolean;
}

export interface PerfilUsuario {
  id: number;
  nome: string;
  email: string;
  tipoUsuario: TipoUsuario;
  telefone?: string;
  ultimoLogin?: Date;
  totalLoginsMes: number;
  permissoes: string[];
  configuracoes: any;
}

export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data?: T;
  errors?: string[];
}

// ===================================
// ===================================

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  
  private readonly baseUrl = `${environment.apiUrl}/usuarios`;
  private readonly perfilUrl = `${environment.apiUrl}/perfil`;
  
  private usuariosSubject = new BehaviorSubject<Usuario[]>([]);
  private estatisticasSubject = new BehaviorSubject<EstatisticasUsuario | null>(null);
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);

  public usuarios$ = this.usuariosSubject.asObservable();
  public estatisticas$ = this.estatisticasSubject.asObservable();
  public loading$ = this.loadingSubject.asObservable();
  public error$ = this.errorSubject.asObservable();

  private cache = new Map<string, any>();
  private cacheTimeout = 5 * 60 * 1000; // 5 minutos

  constructor(private http: HttpClient) {
  }

  // ===================================
  // ===================================

  /**
   * Lista usuários com filtros e paginação
   */
  listarUsuarios(filtros: FiltrosUsuario = {}): Observable<UsuarioPaginado> {
    this.setLoading(true);
    this.clearError();

    let params = new HttpParams();
    
    if (filtros.termo) params = params.set('termo', filtros.termo);
    if (filtros.tipoUsuario) params = params.set('tipoUsuario', filtros.tipoUsuario);
    if (filtros.ativo !== undefined) params = params.set('ativo', filtros.ativo.toString());
    if (filtros.page !== undefined) params = params.set('page', filtros.page.toString());
    if (filtros.size !== undefined) params = params.set('size', filtros.size.toString());
    if (filtros.orderBy) params = params.set('orderBy', filtros.orderBy);
    if (filtros.orderDirection) params = params.set('orderDirection', filtros.orderDirection);

    return this.http.get<UsuarioPaginado>(this.baseUrl, { params }).pipe(
      tap(response => {
        this.usuariosSubject.next(response.content);
        this.setLoading(false);
      }),
      catchError(error => this.handleError('Erro ao carregar usuários', error))
    );
  }

  /**
   * Busca usuário por ID
   */
  obterUsuario(id: number): Observable<Usuario> {
    const cacheKey = `usuario_${id}`;
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return of(cached);
    }

    return this.http.get<Usuario>(`${this.baseUrl}/${id}`).pipe(
      tap(usuario => {
        this.setCache(cacheKey, usuario);
      }),
      catchError(error => this.handleError('Erro ao carregar usuário', error))
    );
  }

  /**
   * Cria novo usuário
   */
  criarUsuario(novoUsuario: NovoUsuario): Observable<Usuario> {
    this.setLoading(true);
    this.clearError();

    return this.http.post<Usuario>(this.baseUrl, novoUsuario).pipe(
      tap(usuario => {
        this.invalidateCache('usuarios_');
        this.atualizarEstatisticas();
        this.setLoading(false);
      }),
      catchError(error => this.handleError('Erro ao criar usuário', error))
    );
  }

  /**
   * Atualiza usuário existente
   */
  atualizarUsuario(id: number, dados: AtualizarUsuario): Observable<Usuario> {
    this.setLoading(true);
    this.clearError();

    return this.http.put<Usuario>(`${this.baseUrl}/${id}`, dados).pipe(
      tap(usuario => {
        this.invalidateCache(`usuario_${id}`);
        this.invalidateCache('usuarios_');
        this.setLoading(false);
      }),
      catchError(error => this.handleError('Erro ao atualizar usuário', error))
    );
  }

  /**
   * Remove usuário (soft delete)
   */
  removerUsuario(id: number): Observable<void> {
    this.setLoading(true);
    this.clearError();

    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.invalidateCache(`usuario_${id}`);
        this.invalidateCache('usuarios_');
        this.atualizarEstatisticas();
        this.setLoading(false);
      }),
      catchError(error => this.handleError('Erro ao remover usuário', error))
    );
  }

  // ===================================
  // ===================================

  /**
   * Ativa usuário
   */
  ativarUsuario(id: number): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.baseUrl}/${id}/reativar`, {}).pipe(
      tap(usuario => {
        this.invalidateCache(`usuario_${id}`);
        this.invalidateCache('usuarios_');
        this.atualizarEstatisticas();
      }),
      catchError(error => this.handleError('Erro ao ativar usuário', error))
    );
  }

  /**
   * Desativa usuário
   */
  desativarUsuario(id: number): Observable<Usuario> {
    return this.http.delete<Usuario>(`${this.baseUrl}/${id}`).pipe(
      tap(usuario => {
        this.invalidateCache(`usuario_${id}`);
        this.invalidateCache('usuarios_');
        this.atualizarEstatisticas();
      }),
      catchError(error => this.handleError('Erro ao desativar usuário', error))
    );
  }

  /**
   * Bloqueia usuário
   */
  bloquearUsuario(id: number): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.baseUrl}/${id}/bloquear`, {}).pipe(
      tap(usuario => {
        console.log('🚫 Usuário bloqueado:', usuario.nome);
        this.invalidateCache(`usuario_${id}`);
        this.invalidateCache('usuarios_');
        this.atualizarEstatisticas();
      }),
      catchError(error => this.handleError('Erro ao bloquear usuário', error))
    );
  }

  // ===================================
  // ===================================

  /**
   * Obtém perfil do usuário logado
   */
  obterPerfil(): Observable<PerfilUsuario> {
    return this.http.get<PerfilUsuario>(`${this.perfilUrl}`).pipe(
      catchError(error => this.handleError('Erro ao carregar perfil', error))
    );
  }

  /**
   * Atualiza perfil do usuário logado
   */
  atualizarPerfil(dados: Partial<PerfilUsuario>): Observable<PerfilUsuario> {
    return this.http.put<PerfilUsuario>(`${this.perfilUrl}`, dados).pipe(
      catchError(error => this.handleError('Erro ao atualizar perfil', error))
    );
  }

  /**
   * Altera senha do usuário
   */
  alterarSenha(request: AlterarSenhaRequest): Observable<ApiResponse> {
    return this.http.put<ApiResponse>(`${this.perfilUrl}/senha`, request).pipe(
      catchError(error => this.handleError('Erro ao alterar senha', error))
    );
  }

  // ===================================
  // ===================================

  /**
   * Obtém permissões do usuário
   */
  obterPermissoes(id: number): Observable<PermissaoUsuario[]> {
    return this.http.get<PermissaoUsuario[]>(`${this.baseUrl}/${id}/permissoes`).pipe(
      catchError(error => this.handleError('Erro ao carregar permissões', error))
    );
  }

  /**
   * Verifica se usuário tem permissão específica
   */
  verificarPermissao(id: number, permissao: string): Observable<boolean> {
    return this.http.get<{temPermissao: boolean}>(`${this.baseUrl}/${id}/permissoes/verificar`, {
      params: { permissao }
    }).pipe(
      map(response => response.temPermissao),
      catchError(error => this.handleError('Erro ao verificar permissão', error))
    );
  }

  // ===================================
  // ===================================

  /**
   * Obtém estatísticas gerais dos usuários
   */
  obterEstatisticas(): Observable<EstatisticasUsuario> {
    const cacheKey = 'estatisticas_usuarios';
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      console.log('📊 Estatísticas obtidas do cache');
      this.estatisticasSubject.next(cached);
      return of(cached);
    }

    return this.http.get<any>(`${this.baseUrl}/estatisticas`).pipe(
      map(backendStats => {
        console.log('📊 Dados brutos do backend:', backendStats);
        
        const stats: EstatisticasUsuario = {
          totalUsuarios: backendStats.totalUsuarios || 0,
          usuariosAtivos: backendStats.usuariosAtivos || 0,
          usuariosInativos: backendStats.usuariosInativos || 0,
          usuariosBloqueados: backendStats.usuariosBloqueados || 0,
          totalAdmins: backendStats.totalAdmins || 0,
          totalVendedores: backendStats.totalVendedores || 0,
          usuariosOnlineHoje: backendStats.usuariosOnlineHoje || 0,
          novosCadastrosUltimos30Dias: backendStats.novosCadastrosUltimos30Dias || 0
        };

        console.log('📊 Estatísticas convertidas:', stats);
        return stats;
      }),
      tap(stats => {
        console.log('📊 Estatísticas de usuários carregadas e convertidas');
        this.estatisticasSubject.next(stats);
        this.setCache(cacheKey, stats);
      }),
      catchError(error => this.handleError('Erro ao carregar estatísticas', error))
    );
  }

  /**
   * Busca usuários por termo
   */
  buscarUsuarios(termo: string): Observable<Usuario[]> {
    if (!termo || termo.trim().length === 0) {
      return of([]);
    }

    return this.http.get<Usuario[]>(`${this.baseUrl}/buscar`, {
      params: { termo: termo.trim() }
    }).pipe(
      tap(usuarios => console.log('🔍 Usuários encontrados:', usuarios.length)),
      catchError(error => this.handleError('Erro na busca de usuários', error))
    );
  }

  /**
   * Obtém usuários por tipo
   */
  obterUsuariosPorTipo(tipo: TipoUsuario): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.baseUrl}/tipo/${tipo}`).pipe(
      catchError(error => this.handleError(`Erro ao carregar usuários ${tipo}`, error))
    );
  }

  // ===================================
  // ===================================

  /**
   * Valida se email está disponível
   */
  validarEmail(email: string, id?: number): Observable<boolean> {
    let params = new HttpParams().set('email', email);
    if (id) params = params.set('excludeId', id.toString());

    return this.http.get<{disponivel: boolean}>(`${this.baseUrl}/validar-email`, { params }).pipe(
      map(response => response.disponivel),
      catchError(() => of(false))
    );
  }

  /**
   * Atualiza estatísticas (chamada privada)
   */
  private atualizarEstatisticas(): void {
    this.obterEstatisticas().subscribe();
  }

  /**
   * Recarrega lista de usuários
   */
  recarregarUsuarios(filtros: FiltrosUsuario = {}): void {
    this.invalidateCache('usuarios_');
    this.listarUsuarios(filtros).subscribe();
  }

  /**
   * Recarrega estatísticas de usuários
   */
  recarregarEstatisticas(): void {
    this.invalidateCache('estatisticas_usuarios');
    this.obterEstatisticas().subscribe();
  }

  // ===================================
  // ===================================

  private setCache(key: string, data: any): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    });
  }

  private getFromCache(key: string): any {
    const cached = this.cache.get(key);
    if (!cached) return null;

    const isExpired = Date.now() - cached.timestamp > this.cacheTimeout;
    if (isExpired) {
      this.cache.delete(key);
      return null;
    }

    return cached.data;
  }

  private invalidateCache(keyPrefix: string): void {
    const keysToDelete = Array.from(this.cache.keys()).filter(key => key.startsWith(keyPrefix));
    keysToDelete.forEach(key => this.cache.delete(key));
  }

  private clearCache(): void {
    this.cache.clear();
  }

  // ===================================
  // ===================================

  private handleError(message: string, error: any): Observable<never> {
    console.error(`❌ ${message}:`, error);
    this.setError(message);
    this.setLoading(false);
    return throwError(error);
  }

  private setLoading(loading: boolean): void {
    this.loadingSubject.next(loading);
  }

  private setError(error: string | null): void {
    this.errorSubject.next(error);
  }

  private clearError(): void {
    this.errorSubject.next(null);
  }

  // ===================================
  // ===================================

  /**
   * Formata tipo de usuário para exibição
   */
  formatarTipoUsuario(tipo: TipoUsuario): string {
    const labels = {
      [TipoUsuario.OWNER]: 'Proprietário',
      [TipoUsuario.ADMIN]: 'Administrador',
      [TipoUsuario.VENDEDOR]: 'Vendedor',
      [TipoUsuario.ESTOQUISTA]: 'Estoquista'
    };
    return labels[tipo] || tipo;
  }

  /**
   * Obtém classe CSS para badge de tipo de usuário
   */
  getClasseTipoUsuario(tipo: TipoUsuario): string {
    const classes = {
      [TipoUsuario.OWNER]: 'bg-success',
      [TipoUsuario.ADMIN]: 'bg-primary',
      [TipoUsuario.VENDEDOR]: 'bg-secondary',
      [TipoUsuario.ESTOQUISTA]: 'bg-info'
    };
    return classes[tipo] || 'bg-light';
  }

  /**
   * Obtém classe CSS para badge de status
   */
  getClasseStatus(ativo: boolean, bloqueado: boolean): string {
    if (bloqueado) return 'bg-danger';
    return ativo ? 'bg-success' : 'bg-warning';
  }

  /**
   * Formata status para exibição
   */
  formatarStatus(ativo: boolean, bloqueado: boolean): string {
    if (bloqueado) return 'Bloqueado';
    return ativo ? 'Ativo' : 'Inativo';
  }

  /**
   * Limpa todos os states e cache
   */
  limparDados(): void {
    this.usuariosSubject.next([]);
    this.estatisticasSubject.next(null);
    this.setLoading(false);
    this.clearError();
    this.clearCache();
    console.log('🧹 UsuarioService limpo');
  }
}