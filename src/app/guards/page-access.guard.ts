import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class PageAccessGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return of(false);
    }

    const url = state.url;
    const currentUser = this.authService.getCurrentUser();
    const userType = currentUser?.tipoUsuario;

    console.log(`🔒 PageAccessGuard: Verificando acesso para ${userType} à página ${url}`);

    // Aplicar regras específicas baseadas no tipo de usuário
    let hasAccess = false;

    switch (url) {
      case '/estoque':
        // ESTOQUISTA só pode ter acesso à página de estoque
        hasAccess = this.authService.canAccessEstoquePage();
        break;
        
      case '/vendas':
        // VENDEDOR só pode ter acesso às páginas de vendas, clientes e eventos
        hasAccess = this.authService.canAccessVendasPage();
        break;
        
      case '/clientes':
        // VENDEDOR só pode ter acesso às páginas de vendas, clientes e eventos
        hasAccess = this.authService.canAccessClientesPage();
        break;
        
      case '/eventos':
        // VENDEDOR só pode ter acesso às páginas de vendas, clientes e eventos
        hasAccess = this.authService.canAccessEventosPage();
        break;
        
      case '/produtos':
        // COMPRAS só pode ter acesso à página de catálogo (produtos para visualização)
        hasAccess = this.authService.canAccessCatalogPage();
        break;
        
      case '/dashboard':
        // Apenas ADMIN e OWNER podem acessar dashboard
        hasAccess = this.authService.canAccessDashboardPage();
        break;
        
      case '/user-management':
        // Apenas ADMIN e OWNER podem gerenciar usuários
        hasAccess = this.authService.canAccessUsersPage();
        break;
        
      case '/relatorios':
        // Apenas ADMIN e OWNER podem acessar relatórios
        hasAccess = this.authService.canAccessReportsPage();
        break;
        
      default:
        // Para outras rotas, permitir acesso se autenticado
        hasAccess = true;
        break;
    }

    if (!hasAccess) {
      console.log(`🔒 PageAccessGuard: Acesso negado para ${userType} à página ${url}`);
      
      // Redirecionar para página permitida baseado no tipo de usuário
      this.redirectToAllowedPage(userType);
      return of(false);
    }

    console.log(`✅ PageAccessGuard: Acesso permitido para ${userType} à página ${url}`);
    return of(true);
  }

  private redirectToAllowedPage(userType: string | undefined): void {
    switch (userType) {
      case 'ESTOQUISTA':
        this.router.navigate(['/estoque']);
        break;
      case 'VENDEDOR':
        this.router.navigate(['/vendas']);
        break;
      case 'COMPRAS':
        this.router.navigate(['/produtos']);
        break;
      case 'ADMIN':
      case 'OWNER':
        this.router.navigate(['/dashboard']);
        break;
      default:
        this.router.navigate(['/acesso-negado']);
        break;
    }
  }
}