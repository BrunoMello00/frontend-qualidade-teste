import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard aprimorado com verificações de segurança adicionais
 */
@Injectable({
  providedIn: 'root'
})
export class EnhancedAuthGuard implements CanActivate {

  // Lista de tentativas de acesso por IP (simulação)
  private accessAttempts = new Map<string, { count: number, lastAttempt: Date }>();
  private readonly MAX_ATTEMPTS = 5;
  private readonly LOCKOUT_DURATION = 15 * 60 * 1000; // 15 minutos

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    // Verificar autenticação básica
    if (!this.authService.isAuthenticated()) {
      this.logSecurityEvent('UNAUTHORIZED_ACCESS_ATTEMPT', state.url);
      this.router.navigate(['/login'], { 
        queryParams: { returnUrl: state.url }
      });
      return false;
    }

    // Verificar rate limiting (simulação)
    if (this.isRateLimited()) {
      this.logSecurityEvent('RATE_LIMIT_EXCEEDED', state.url);
      this.router.navigate(['/login']);
      return false;
    }

    // Verificar permissões baseadas em roles
    const requiredRoles = route.data['roles'] as Array<string>;
    if (requiredRoles && !this.hasRequiredRole(requiredRoles)) {
      this.logSecurityEvent('INSUFFICIENT_PRIVILEGES', state.url);
      this.router.navigate(['/dashboard']);
      return false;
    }

    // Verificar sessão ativa
    if (!this.isSessionValid()) {
      this.logSecurityEvent('INVALID_SESSION', state.url);
      this.authService.logout();
      return false;
    }

    // Verificar se a rota é permitida
    if (!this.isRouteAllowed(state.url)) {
      this.logSecurityEvent('FORBIDDEN_ROUTE_ACCESS', state.url);
      this.router.navigate(['/dashboard']);
      return false;
    }

    return true;
  }

  private isRateLimited(): boolean {
    const clientId = this.getClientIdentifier();
    const now = new Date();
    
    if (this.accessAttempts.has(clientId)) {
      const attempts = this.accessAttempts.get(clientId)!;
      
      // Verificar se ainda está no período de lockout
      if (now.getTime() - attempts.lastAttempt.getTime() < this.LOCKOUT_DURATION) {
        if (attempts.count >= this.MAX_ATTEMPTS) {
          return true;
        }
        attempts.count++;
        attempts.lastAttempt = now;
      } else {
        // Reset contador após lockout
        attempts.count = 1;
        attempts.lastAttempt = now;
      }
    } else {
      this.accessAttempts.set(clientId, { count: 1, lastAttempt: now });
    }

    return false;
  }

  private hasRequiredRole(requiredRoles: string[]): boolean {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) return false;

    // Simular verificação de roles
    const userRole = 'ADMIN'; // Em produção, pegar do token/usuário
    return requiredRoles.includes(userRole);
  }

  private isSessionValid(): boolean {
    const token = this.authService.getToken();
    if (!token) return false;

    try {
      // Verificar se o token não expirou
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expirationTime = payload.exp * 1000;
      const currentTime = Date.now();
      
      return currentTime < expirationTime;
    } catch (error) {
      return false;
    }
  }

  private isRouteAllowed(url: string): boolean {
    // Lista de rotas protegidas que requerem validação extra
    const restrictedRoutes = [
      '/admin',
      '/configuracoes',
      '/relatorios'
    ];

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) return false;

    // Verificar se a rota é restrita e se o usuário tem permissão
    const isRestricted = restrictedRoutes.some(route => url.startsWith(route));
    if (isRestricted) {
      // Simular verificação de permissão específica
      return currentUser.email === 'admin@admin.com';
    }

    return true;
  }

  private getClientIdentifier(): string {
    // Em um ambiente real, usar uma identificação mais robusta
    return 'client-' + (localStorage.getItem('clientId') || 'unknown');
  }

  private logSecurityEvent(event: string, url: string): void {
    const logData = {
      timestamp: new Date().toISOString(),
      event,
      url,
      userAgent: navigator.userAgent,
      clientId: this.getClientIdentifier()
    };

    console.warn('SECURITY_EVENT:', logData);

    // Em produção, enviar para sistema de logging
    // this.securityService.logEvent(logData);
  }
}
