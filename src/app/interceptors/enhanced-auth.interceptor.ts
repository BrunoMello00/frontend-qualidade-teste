import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { EnvironmentService } from '../services/environment.service';

/**
 * Interceptor aprimorado com medidas de segurança
 */
@Injectable()
export class EnhancedAuthInterceptor implements HttpInterceptor {

  constructor(
    private authService: AuthService,
    private environmentService: EnvironmentService
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Clonar requisição para adicionar headers de segurança
    let authReq = req.clone();

    // Adicionar token de autenticação se disponível
    const token = this.authService.getToken();
    if (token) {
      authReq = authReq.clone({
        setHeaders: {
          'Authorization': `Bearer ${token}`
        }
      });
    }

    // Adicionar headers de segurança
    authReq = authReq.clone({
      setHeaders: {
        'X-Requested-With': 'XMLHttpRequest',
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
      }
    });

    // Validar URL para prevenir redirecionamentos maliciosos
    if (!this.isValidUrl(authReq.url)) {
      console.error('SECURITY: Tentativa de requisição para URL inválida:', authReq.url);
      return throwError(() => new Error('URL inválida'));
    }

    // Processar requisição com retry em caso de falha temporária
    return next.handle(authReq).pipe(
      retry(1), // Retry apenas uma vez para evitar spam
      catchError((error: HttpErrorResponse) => {
        return this.handleError(error);
      })
    );
  }

  private isValidUrl(url: string): boolean {
    // Obter URLs válidas do EnvironmentService
    const currentApiUrl = this.environmentService.getApiUrl();
    const environmentInfo = this.environmentService.getEnvironmentInfo();
    
    // Lista de domínios permitidos baseados no ambiente
    const allowedDomains = [
      'localhost',
      '127.0.0.1',
      'app-backend-estoque-vendas.bravesky-fa21ce16.eastus2.azurecontainerapps.io',
      'meuapp.azurewebsites.net',
      'api.meudominio.com'
    ];

    // Se a URL começar com a API URL atual, é válida
    if (url.startsWith(currentApiUrl)) {
      return true;
    }

    // Se for uma URL relativa (começar com /), é válida
    if (url.startsWith('/')) {
      return true;
    }

    try {
      const urlObj = new URL(url, window.location.origin);
      
      // Verificar protocolo
      if (!['http:', 'https:'].includes(urlObj.protocol)) {
        return false;
      }

      // Verificar domínio
      const hostname = urlObj.hostname;
      return allowedDomains.some(domain => 
        hostname === domain || hostname.endsWith('.' + domain)
      );
    } catch {
      return false;
    }
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Erro desconhecido';

    if (error.error instanceof ErrorEvent) {
      // Erro do lado cliente
      errorMessage = `Erro: ${error.error.message}`;
    } else {
      // Erro do lado servidor
      switch (error.status) {
        case 401:
          errorMessage = 'Não autorizado. Faça login novamente.';
          this.authService.logout();
          break;
        case 403:
          errorMessage = 'Acesso negado. Você não tem permissão.';
          break;
        case 429:
          errorMessage = 'Muitas tentativas. Tente novamente mais tarde.';
          break;
        case 500:
          errorMessage = 'Erro interno do servidor.';
          break;
        default:
          errorMessage = `Erro ${error.status}: ${error.message}`;
      }
    }

    // Log de segurança
    console.error('SECURITY_EVENT:', {
      timestamp: new Date().toISOString(),
      error: errorMessage,
      status: error.status,
      url: error.url
    });

    return throwError(() => new Error(errorMessage));
  }
}
