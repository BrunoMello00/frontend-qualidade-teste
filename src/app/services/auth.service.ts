import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError, timer } from 'rxjs';
import { map, catchError, tap, switchMap, finalize } from 'rxjs/operators';
import { 
  Usuario, 
  LoginRequest, 
  LoginResponse, 
  AlterarSenhaRequest, 
  ResetSenhaRequest,
  TipoUsuario,
  StatusUsuario,
  ApiResponse,
  ValidacaoSenha
} from '../models/user.models';
import { EnvironmentService } from './environment.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly USER_KEY = 'current_user';
  private readonly PERMISSIONS_KEY = 'user_permissions';

  private currentUserSubject = new BehaviorSubject<Usuario | null>(null);
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private permissionsSubject = new BehaviorSubject<string[]>([]);
  private loadingSubject = new BehaviorSubject<boolean>(false);

  public currentUser$ = this.currentUserSubject.asObservable();
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public permissions$ = this.permissionsSubject.asObservable();
  public loading$ = this.loadingSubject.asObservable();

  private refreshTimer?: any;

  constructor(
    private http: HttpClient,
    private router: Router,
    private environmentService: EnvironmentService
  ) {
    this.initializeAuth();
    this.environmentService.logEnvironmentInfo();
  }


  private getApiUrl(): string {
    return `${this.environmentService.getApiUrl()}/auth`;
  }


  private initializeAuth(): void {
    const token = this.getToken();
    const user = this.getCurrentUserFromStorage();
    const permissions = this.getPermissionsFromStorage();

    if (token && user) {
      this.currentUserSubject.next(user);
      this.isAuthenticatedSubject.next(true);
      this.permissionsSubject.next(permissions);
      this.startRefreshTimer();
    }
  }

  private getCurrentUserFromStorage(): Usuario | null {
    try {
      const userJson = localStorage.getItem(this.USER_KEY);
      return userJson ? JSON.parse(userJson) : null;
    } catch {
      return null;
    }
  }

  private getPermissionsFromStorage(): string[] {
    try {
      const permissionsJson = localStorage.getItem(this.PERMISSIONS_KEY);
      return permissionsJson ? JSON.parse(permissionsJson) : [];
    } catch {
      return [];
    }
  }


  checkMfaRequired(email: string, senha: string): Observable<{ mfaRequired: boolean; userEmail?: string }> {
    this.loadingSubject.next(true);

    const credentials = { email, senha };
    return this.http.post<{ mfaRequired: boolean; userEmail?: string }>(`${this.getApiUrl()}/check-mfa`, credentials).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  verifyMfaCode(email: string, codigo: string): Observable<{ success: boolean; message: string }> {
    this.loadingSubject.next(true);

    const body = { email, codigo };
    return this.http.post<{ success: boolean; message: string }>(`${this.getApiUrl()}/verify-mfa`, body).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  resendMfaCode(email: string): Observable<{ message: string }> {
    this.loadingSubject.next(true);

    const body = { email };
    return this.http.post<{ message: string }>(`${this.getApiUrl()}/resend-mfa`, body).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }


  login(credentialsOrEmail: LoginRequest | string, senha?: string): Observable<LoginResponse> {
    this.loadingSubject.next(true);

    let credentials: LoginRequest;
    if (typeof credentialsOrEmail === 'string') {
      credentials = { email: credentialsOrEmail, senha: senha! };
    } else {
      credentials = credentialsOrEmail;
    }

    return this.http.post<any>(`${this.getApiUrl()}/login`, credentials).pipe(
      map(response => {
        const adaptedResponse: LoginResponse = {
          success: response.success || true,
          message: response.message || 'Login realizado com sucesso',
          token: response.token || response.accessToken,
          refreshToken: response.refreshToken,
          usuario: response.usuario ? {
            ...response.usuario,
            tipoUsuario: this.mapTipoUsuario(response.usuario.tipoUsuario || response.usuario.role),
            status: 'ATIVO' as StatusUsuario,
            primeiroAcesso: false,
            tentativasLogin: 0,
            dataCriacao: new Date(),
            dataAtualizacao: new Date(),
            metaMensal: 0,
            comissaoPercentual: 0,
            ativo: response.usuario.ativo !== false
          } : undefined,
          permissoes: this.generatePermissionsFromRole(response.usuario?.tipoUsuario || response.usuario?.role)
        };
        
        if (adaptedResponse.success && adaptedResponse.token && adaptedResponse.usuario) {
          this.setSession(adaptedResponse);
        }
        
        return adaptedResponse;
      }),
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  private mapTipoUsuario(role: string): TipoUsuario {
    switch (role?.toUpperCase()) {
      case 'ADMIN':
      case 'ADMINISTRADOR':
        return TipoUsuario.ADMIN;
      case 'OWNER':
      case 'PROPRIETARIO':
        return TipoUsuario.OWNER;
      case 'VENDEDOR':
      case 'USER':
        return TipoUsuario.VENDEDOR;
      case 'ESTOQUISTA':
        return TipoUsuario.ESTOQUISTA;
      case 'COMPRAS':
        return TipoUsuario.COMPRAS;
      default:
        return TipoUsuario.VENDEDOR;
    }
  }

  private generatePermissionsFromRole(role: string): string[] {
    const roleUpper = role?.toUpperCase();
    
    switch (roleUpper) {
      case 'ADMIN':
      case 'ADMINISTRADOR':
        return [
          'USUARIOS_GERENCIAR',
          'PRODUTOS_GERENCIAR',
          'VENDAS_GERENCIAR',
          'ESTOQUE_GERENCIAR',
          'CLIENTES_GERENCIAR',
          'EVENTOS_GERENCIAR',
          'RELATORIOS_VISUALIZAR',
          'RELATORIOS_GERENCIAIS'
        ];
      case 'OWNER':
      case 'PROPRIETARIO':
        return [
          'USUARIOS_GERENCIAR',
          'PRODUTOS_GERENCIAR',
          'VENDAS_GERENCIAR',
          'ESTOQUE_GERENCIAR',
          'CLIENTES_GERENCIAR',
          'EVENTOS_GERENCIAR',
          'RELATORIOS_VISUALIZAR',
          'RELATORIOS_GERENCIAIS',
          'CONFIGURACOES_SISTEMA'
        ];
      case 'VENDEDOR':
      case 'USER':
        return [
          'PRODUTOS_VISUALIZAR',
          'VENDAS_CRIAR',
          'VENDAS_GERENCIAR',
          'CLIENTES_VISUALIZAR',
          'CLIENTES_GERENCIAR',
          'EVENTOS_VISUALIZAR',
          'EVENTOS_GERENCIAR'
        ];
      case 'ESTOQUISTA':
        return [
          'PRODUTOS_VISUALIZAR',
          'PRODUTOS_GERENCIAR',
          'ESTOQUE_GERENCIAR',
          'ESTOQUE_VISUALIZAR',
          'RELATORIOS_ESTOQUE'
        ];
      case 'COMPRAS':
        return [
          'PRODUTOS_VISUALIZAR'
        ];
      default:
        return [
          'PRODUTOS_VISUALIZAR',
          'VENDAS_CRIAR',
          'CLIENTES_VISUALIZAR'
        ];
    }
  }

  logout(): void {
    this.loadingSubject.next(true);

    const token = this.getToken();
    if (token) {
      this.http.post(`${this.getApiUrl()}/logout`, {}, this.getHttpOptions()).pipe(
        finalize(() => this.completeLogout())
      ).subscribe({
        error: () => this.completeLogout() // Se falhar, faz logout local mesmo assim
      });
    } else {
      this.completeLogout();
    }
  }

  private completeLogout(): void {
    this.clearSession();
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.permissionsSubject.next([]);
    this.stopRefreshTimer();
    this.loadingSubject.next(false);
    this.router.navigate(['/login']);
  }

  private setSession(loginResponse: LoginResponse): void {
    if (loginResponse.token && loginResponse.usuario) {
      localStorage.setItem(this.TOKEN_KEY, loginResponse.token);
      localStorage.setItem(this.USER_KEY, JSON.stringify(loginResponse.usuario));
      
      if (loginResponse.refreshToken) {
        localStorage.setItem(this.REFRESH_TOKEN_KEY, loginResponse.refreshToken);
      }
      
      if (loginResponse.permissoes) {
        localStorage.setItem(this.PERMISSIONS_KEY, JSON.stringify(loginResponse.permissoes));
        this.permissionsSubject.next(loginResponse.permissoes);
      }

      this.currentUserSubject.next(loginResponse.usuario);
      this.isAuthenticatedSubject.next(true);
      this.startRefreshTimer();
    }
  }

  private clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    localStorage.removeItem(this.PERMISSIONS_KEY);
  }


  alterarSenha(request: AlterarSenhaRequest): Observable<ApiResponse> {
    this.loadingSubject.next(true);

    return this.http.put<ApiResponse>(`${this.getApiUrl()}/alterar-senha`, request, this.getHttpOptions()).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  resetSenha(request: ResetSenhaRequest): Observable<ApiResponse> {
    this.loadingSubject.next(true);

    return this.http.post<ApiResponse>(`${this.getApiUrl()}/reset-senha`, request).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  solicitarRedefinicaoSenha(request: { email: string }): Observable<{ mensagem: string }> {
    this.loadingSubject.next(true);

    return this.http.post<{ mensagem: string }>(`${this.getApiUrl()}/solicitar-redefinicao`, request).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  redefinirSenha(dados: { token: string; novaSenha: string; confirmarSenha: string }): Observable<{ mensagem: string }> {
    this.loadingSubject.next(true);

    return this.http.post<{ mensagem: string }>(`${this.getApiUrl()}/redefinir-senha`, dados).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  validarSenha(senha: string): ValidacaoSenha {
    const validacao: ValidacaoSenha = {
      tamanhoMinimo: senha.length >= 8,
      temNumero: /\d/.test(senha),
      temLetraMaiuscula: /[A-Z]/.test(senha),
      temLetraMinuscula: /[a-z]/.test(senha),
      temCaractereEspecial: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\?]/.test(senha),
      valida: false
    };

    validacao.valida = validacao.tamanhoMinimo && 
                      validacao.temNumero && 
                      validacao.temLetraMaiuscula && 
                      validacao.temLetraMinuscula && 
                      validacao.temCaractereEspecial;
    
    return validacao;
  }


  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  refreshToken(): Observable<LoginResponse> {
    const refreshToken = this.getRefreshToken();
    
    if (!refreshToken) {
      return throwError('Refresh token não encontrado');
    }

    const body = { refreshToken };
    return this.http.post<LoginResponse>(`${this.getApiUrl()}/refresh`, body).pipe(
      tap(response => {
        if (response.success && response.token) {
          this.setSession(response);
        }
      }),
      catchError(error => {
        this.completeLogout();
        return throwError(error);
      })
    );
  }

  private startRefreshTimer(): void {
    this.stopRefreshTimer();
    
    this.refreshTimer = timer(55 * 60 * 1000, 55 * 60 * 1000)
      .pipe(
        switchMap(() => this.refreshToken())
      )
      .subscribe({
        error: () => this.completeLogout()
      });
  }

  private stopRefreshTimer(): void {
    if (this.refreshTimer) {
      this.refreshTimer.unsubscribe();
      this.refreshTimer = null;
    }
  }


  hasPermission(permission: string): boolean {
    const permissions = this.permissionsSubject.value;
    return permissions.includes(permission);
  }

  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some(permission => this.hasPermission(permission));
  }

  hasAllPermissions(permissions: string[]): boolean {
    return permissions.every(permission => this.hasPermission(permission));
  }

  isOwner(): boolean {
    const user = this.currentUserSubject.value;
    return user?.tipoUsuario === TipoUsuario.OWNER;
  }

  isAdmin(): boolean {
    const user = this.currentUserSubject.value;
    return user?.tipoUsuario === TipoUsuario.ADMIN;
  }

  isVendedor(): boolean {
    const user = this.currentUserSubject.value;
    return user?.tipoUsuario === TipoUsuario.VENDEDOR;
  }

  isEstoquista(): boolean {
    const user = this.currentUserSubject.value;
    return user?.tipoUsuario === TipoUsuario.ESTOQUISTA;
  }

  isCompras(): boolean {
    const user = this.currentUserSubject.value;
    return user?.tipoUsuario === TipoUsuario.COMPRAS;
  }

  canManageUsers(): boolean {
    return this.hasPermission('USUARIOS_GERENCIAR') || this.isOwner() || this.isAdmin();
  }

  canDeleteUser(targetUserType: TipoUsuario): boolean {
    const currentUser = this.currentUserSubject.value;
    if (!currentUser) return false;
    
    if (currentUser.tipoUsuario === TipoUsuario.OWNER) {
      return targetUserType !== TipoUsuario.OWNER;
    }
    
    if (currentUser.tipoUsuario === TipoUsuario.ADMIN) {
      return targetUserType === TipoUsuario.VENDEDOR || targetUserType === TipoUsuario.ESTOQUISTA || targetUserType === TipoUsuario.COMPRAS;
    }
    
    return false;
  }

  canViewReports(): boolean {
    return this.hasAnyPermission(['RELATORIOS_VISUALIZAR', 'RELATORIOS_GERENCIAIS']) || this.isOwner() || this.isAdmin();
  }

  canManageProducts(): boolean {
    return this.isOwner() || this.isAdmin() || this.isEstoquista();
  }

  canViewProducts(): boolean {
    return this.isOwner() || this.isAdmin() || this.isEstoquista() || this.isCompras();
  }

  canManageSales(): boolean {
    return this.isOwner() || this.isAdmin() || this.isVendedor();
  }

  canManageStock(): boolean {
    return this.isOwner() || this.isAdmin() || this.isEstoquista();
  }

  canManageClients(): boolean {
    return this.isOwner() || this.isAdmin() || this.isVendedor();
  }

  canManageEvents(): boolean {
    return this.isOwner() || this.isAdmin() || this.isVendedor();
  }

  canViewDashboard(): boolean {
    return this.isOwner() || this.isAdmin();
  }

  // Métodos específicos para acesso a páginas - baseado nas regras definidas pelo usuário
  canAccessEstoquePage(): boolean {
    // ESTOQUISTA só pode ter acesso à página de estoque
    // ADMIN e OWNER podem ter acesso a tudo
    return this.isOwner() || this.isAdmin() || this.isEstoquista();
  }

  canAccessVendasPage(): boolean {
    // VENDEDOR só pode ter acesso às páginas de vendas, clientes e eventos
    // ADMIN e OWNER podem ter acesso a tudo
    return this.isOwner() || this.isAdmin() || this.isVendedor();
  }

  canAccessClientesPage(): boolean {
    // VENDEDOR só pode ter acesso às páginas de vendas, clientes e eventos
    // ADMIN e OWNER podem ter acesso a tudo
    return this.isOwner() || this.isAdmin() || this.isVendedor();
  }

  canAccessEventosPage(): boolean {
    // VENDEDOR só pode ter acesso às páginas de vendas, clientes e eventos
    // ADMIN e OWNER podem ter acesso a tudo
    return this.isOwner() || this.isAdmin() || this.isVendedor();
  }

  canAccessCatalogPage(): boolean {
    // COMPRAS só pode ter acesso à página de catálogo
    // ADMIN e OWNER podem ter acesso a tudo
    return this.isOwner() || this.isAdmin() || this.isCompras();
  }

  canAccessUsersPage(): boolean {
    // Apenas ADMIN e OWNER podem gerenciar usuários
    return this.isOwner() || this.isAdmin();
  }

  canAccessDashboardPage(): boolean {
    // Apenas ADMIN e OWNER podem acessar dashboard completo
    return this.isOwner() || this.isAdmin();
  }

  canAccessReportsPage(): boolean {
    // Apenas ADMIN e OWNER podem acessar relatórios
    return this.isOwner() || this.isAdmin();
  }


  getCurrentUser(): Usuario | null {
    return this.currentUserSubject.value;
  }

  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  getPermissions(): string[] {
    return this.permissionsSubject.value;
  }

  isLoading(): boolean {
    return this.loadingSubject.value;
  }


  public getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  public getHttpOptions(): { headers: HttpHeaders } {
    return {
      headers: this.getAuthHeaders()
    };
  }

  private handleError = (error: any): Observable<never> => {
    console.error('Erro no AuthService:', error);
    
    let errorMessage = 'Erro interno do servidor';
    
    if (typeof error === 'string') {
      errorMessage = error;
    } else if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    } else if (error.status === 401) {
      errorMessage = 'Credenciais inválidas';
      this.completeLogout();
    } else if (error.status === 403) {
      errorMessage = 'Acesso negado';
    } else if (error.status === 0) {
      errorMessage = 'Erro de conexão. Verifique sua internet.';
    }

    return throwError(errorMessage);
  }


  isPrimeiroAcesso(): boolean {
    const user = this.getCurrentUser();
    return user?.primeiroAcesso === true;
  }

  marcarPrimeiroAcessoCompleto(): Observable<ApiResponse> {
    return new Observable<ApiResponse>(observer => {
      setTimeout(() => {
        const user = this.getCurrentUser();
        if (user) {
          user.primeiroAcesso = false;
          localStorage.setItem(this.USER_KEY, JSON.stringify(user));
          this.currentUserSubject.next(user);

          const response: ApiResponse = {
            success: true,
            message: 'Primeiro acesso marcado como completo'
          };

          observer.next(response);
          observer.complete();
        } else {
          observer.error('Usuário não encontrado');
        }
      }, 500);
    }).pipe(
      catchError(this.handleError)
    );
  }


  verificarSessao(): Observable<ApiResponse<Usuario>> {
    const token = this.getToken();
    if (!token) {
      return throwError('Token não encontrado');
    }

    return this.http.get<ApiResponse<Usuario>>(`${this.getApiUrl()}/verificar-sessao`, this.getHttpOptions()).pipe(
      catchError(error => {
        console.log('Erro ao verificar sessão:', error);
        if (error.status === 401) {
          console.log('Sessão expirada, fazendo logout...');
          this.completeLogout();
        }
        return throwError(error);
      })
    );
  }


  getAuthHeadersOld(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }


  ngOnDestroy(): void {
    this.stopRefreshTimer();
  }
}
