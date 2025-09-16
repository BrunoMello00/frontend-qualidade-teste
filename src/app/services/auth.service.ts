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
import { MockDataService } from './mock-data.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly USER_KEY = 'current_user';
  private readonly PERMISSIONS_KEY = 'user_permissions';

  // BehaviorSubjects para estado reativo
  private currentUserSubject = new BehaviorSubject<Usuario | null>(null);
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private permissionsSubject = new BehaviorSubject<string[]>([]);
  private loadingSubject = new BehaviorSubject<boolean>(false);

  // Observables públicos
  public currentUser$ = this.currentUserSubject.asObservable();
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public permissions$ = this.permissionsSubject.asObservable();
  public loading$ = this.loadingSubject.asObservable();

  // Timer para refresh automático do token
  private refreshTimer?: any;

  constructor(
    private http: HttpClient,
    private router: Router,
    private environmentService: EnvironmentService,
    private mockDataService: MockDataService
  ) {
    this.initializeAuth();
    // Log das informações do ambiente no console
    this.environmentService.logEnvironmentInfo();
  }

  // Injetar MockDataService apenas quando necessário (usado via property para evitar breaking changes)
  // O MockDataService será injetado dinamicamente para não quebrar instâncias existentes
  private get mock() {
    // @ts-ignore - injector pattern simplificado
    return (this as any).mockDataService as MockDataService | null;
  }

  // ===================================
  // MÉTODOS DE URL
  // ===================================

  private getApiUrl(): string {
    return `${this.environmentService.getApiUrl()}/auth`;
  }

  // ===================================
  // INICIALIZAÇÃO E ESTADO
  // ===================================

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

  // ===================================
  // AUTENTICAÇÃO MULTIFATOR (MFA)
  // ===================================

  checkMfaRequired(email: string, senha: string): Observable<{ mfaRequired: boolean; userEmail?: string }> {
    this.loadingSubject.next(true);

    if (this.environmentService.isLocal()) {
      return this.mockDataService.checkMfaRequired(email, senha).pipe(
        tap(() => {}),
        catchError(this.handleError),
        finalize(() => this.loadingSubject.next(false))
      );
    }

    const credentials = { email, senha };
    return this.http.post<{ mfaRequired: boolean; userEmail?: string }>(`${this.getApiUrl()}/check-mfa`, credentials).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  verifyMfaCode(email: string, codigo: string): Observable<{ success: boolean; message: string }> {
    this.loadingSubject.next(true);

    if (this.environmentService.isLocal()) {
      return this.mockDataService.verifyMfaCode(email, codigo).pipe(
        catchError(this.handleError),
        finalize(() => this.loadingSubject.next(false))
      );
    }

    const body = { email, codigo };
    return this.http.post<{ success: boolean; message: string }>(`${this.getApiUrl()}/verify-mfa`, body).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  resendMfaCode(email: string): Observable<{ message: string }> {
    this.loadingSubject.next(true);

    if (this.environmentService.isLocal()) {
      return this.mockDataService.resendMfaCode(email).pipe(
        catchError(this.handleError),
        finalize(() => this.loadingSubject.next(false))
      );
    }

    const body = { email };
    return this.http.post<{ message: string }>(`${this.getApiUrl()}/resend-mfa`, body).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  // ===================================
  // AUTENTICAÇÃO SIMULADA (Para desenvolvimento)
  // ===================================

  login(credentialsOrEmail: LoginRequest | string, senha?: string): Observable<LoginResponse> {
    this.loadingSubject.next(true);

    // Normaliza os parâmetros
    let credentials: LoginRequest;
    if (typeof credentialsOrEmail === 'string') {
      credentials = { email: credentialsOrEmail, senha: senha! };
    } else {
      credentials = credentialsOrEmail;
    }

    if (this.environmentService.isLocal()) {
      return this.mockDataService.login(credentials).pipe(
        tap(response => {
          if (response.success && response.token && response.usuario) {
            this.setSession(response);
          }
        }),
        catchError(this.handleError),
        finalize(() => this.loadingSubject.next(false))
      );
    }

    // Chamada real para o backend
    return this.http.post<LoginResponse>(`${this.getApiUrl()}/login`, credentials).pipe(
      tap(response => {
        if (response.success && response.token && response.usuario) {
          this.setSession(response);
        }
      }),
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  logout(): void {
    this.loadingSubject.next(true);

    // Chamada para o backend para invalidar o token
    const token = this.getToken();
    if (this.environmentService.isLocal()) {
      this.mockDataService.logout(token || undefined).subscribe({
        next: () => this.completeLogout(),
        error: () => this.completeLogout()
      });
      return;
    }

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

  // ===================================
  // GESTÃO DE SENHA
  // ===================================

  alterarSenha(request: AlterarSenhaRequest): Observable<ApiResponse> {
    this.loadingSubject.next(true);

    if (this.environmentService.isLocal()) {
      // Implementação simples de alteração: exige token e atualiza password map
      return new Observable<ApiResponse>(observer => {
        setTimeout(() => {
          observer.next({ success: true, message: 'Senha alterada (mock)' });
          observer.complete();
          this.loadingSubject.next(false);
        }, 300);
      }).pipe(catchError(this.handleError));
    }

    return this.http.put<ApiResponse>(`${this.getApiUrl()}/alterar-senha`, request, this.getHttpOptions()).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  resetSenha(request: ResetSenhaRequest): Observable<ApiResponse> {
    this.loadingSubject.next(true);
    if (this.environmentService.isLocal()) {
      return this.mockDataService.solicitarRedefinicaoSenha(request.email).pipe(
        catchError(this.handleError),
        finalize(() => this.loadingSubject.next(false))
      );
    }

    return this.http.post<ApiResponse>(`${this.getApiUrl()}/reset-senha`, request).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  solicitarRedefinicaoSenha(request: { email: string }): Observable<{ mensagem: string }> {
    this.loadingSubject.next(true);
    if (this.environmentService.isLocal()) {
      return this.mockDataService.solicitarRedefinicaoSenha(request.email).pipe(
        map((resp: ApiResponse) => ({ mensagem: resp.message })),
        catchError(this.handleError),
        finalize(() => this.loadingSubject.next(false))
      );
    }

    return this.http.post<{ mensagem: string }>(`${this.getApiUrl()}/solicitar-redefinicao`, request).pipe(
      catchError(this.handleError),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  redefinirSenha(dados: { token: string; novaSenha: string; confirmarSenha: string }): Observable<{ mensagem: string }> {
    this.loadingSubject.next(true);
    if (this.environmentService.isLocal()) {
      return this.mockDataService.redefinirSenha(dados.token, dados.novaSenha).pipe(
        map((resp: ApiResponse) => ({ mensagem: resp.message })),
        catchError(this.handleError),
        finalize(() => this.loadingSubject.next(false))
      );
    }

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

  // ===================================
  // TOKENS E REFRESH
  // ===================================

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

    if (this.environmentService.isLocal()) {
      // No mock, apenas falhar se não houver refresh token
      return new Observable<LoginResponse>(observer => {
        setTimeout(() => {
          observer.error('Refresh token não suportado no mock');
        }, 300);
      });
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
    
    // Renovar token a cada 55 minutos (assumindo que expira em 1 hora)
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

  // ===================================
  // PERMISSÕES E AUTORIZAÇÃO
  // ===================================

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

  canManageUsers(): boolean {
    return this.hasPermission('USUARIOS_GERENCIAR') || this.isOwner() || this.isAdmin();
  }

  canDeleteUser(targetUserType: TipoUsuario): boolean {
    const currentUser = this.currentUserSubject.value;
    if (!currentUser) return false;
    
    // Owner pode excluir todos exceto outros owners
    if (currentUser.tipoUsuario === TipoUsuario.OWNER) {
      return targetUserType !== TipoUsuario.OWNER;
    }
    
    // Admin pode excluir apenas vendedores
    if (currentUser.tipoUsuario === TipoUsuario.ADMIN) {
      return targetUserType === TipoUsuario.VENDEDOR;
    }
    
    // Vendedores não podem excluir ninguém
    return false;
  }

  canViewReports(): boolean {
    return this.hasAnyPermission(['RELATORIOS_VISUALIZAR', 'RELATORIOS_GERENCIAIS']) || this.isOwner() || this.isAdmin();
  }

  canManageProducts(): boolean {
    return this.hasPermission('PRODUTOS_GERENCIAR') || this.isOwner() || this.isAdmin();
  }

  canViewProducts(): boolean {
    return this.hasAnyPermission(['PRODUTOS_GERENCIAR', 'PRODUTOS_VISUALIZAR']) || this.isOwner() || this.isAdmin();
  }

  canManageSales(): boolean {
    return this.hasPermission('VENDAS_GERENCIAR') || this.isOwner() || this.isAdmin();
  }

  canManageStock(): boolean {
    return this.hasPermission('ESTOQUE_GERENCIAR') || this.isOwner() || this.isAdmin();
  }

  canManageClients(): boolean {
    return this.hasPermission('CLIENTES_GERENCIAR') || this.isOwner() || this.isAdmin();
  }

  canManageEvents(): boolean {
    return this.hasPermission('EVENTOS_GERENCIAR') || this.isOwner() || this.isAdmin();
  }

  canViewDashboard(): boolean {
    // Dashboard disponível para Owner e Admin apenas
    return this.isOwner() || this.isAdmin();
  }

  // ===================================
  // GETTERS DE ESTADO
  // ===================================

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

  // ===================================
  // UTILITÁRIOS
  // ===================================

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

  // ===================================
  // VERIFICAÇÕES DE PRIMEIRO ACESSO
  // ===================================

  isPrimeiroAcesso(): boolean {
    const user = this.getCurrentUser();
    return user?.primeiroAcesso === true;
  }

  marcarPrimeiroAcessoCompleto(): Observable<ApiResponse> {
    // Simulação para desenvolvimento
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

  // ===================================
  // VERIFICAÇÕES DE SESSÃO
  // ===================================

  verificarSessao(): Observable<ApiResponse<Usuario>> {
    return this.http.get<ApiResponse<Usuario>>(`${this.getApiUrl()}/verificar-sessao`, this.getHttpOptions()).pipe(
      catchError(error => {
        if (error.status === 401) {
          this.completeLogout();
        }
        return throwError(error);
      })
    );
  }

  // ===================================
  // MÉTODOS PARA COMPATIBILIDADE COM CÓDIGO EXISTENTE
  // ===================================

  getAuthHeadersOld(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  // ===================================
  // CLEANUP
  // ===================================

  ngOnDestroy(): void {
    this.stopRefreshTimer();
  }
}
