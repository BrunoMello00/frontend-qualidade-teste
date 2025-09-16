import { Injectable } from '@angular/core';
import { 
  CanActivate, 
  CanActivateChild, 
  ActivatedRouteSnapshot, 
  RouterStateSnapshot, 
  Router 
} from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { TipoUsuario } from '../models/user.models';

@Injectable({
  providedIn: 'root'
})
export class PermissionGuard implements CanActivate, CanActivateChild {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    return this.checkPermissions(route.data);
  }

  canActivateChild(
    childRoute: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    return this.checkPermissions(childRoute.data);
  }

  private checkPermissions(routeData: any): Observable<boolean> {
    return this.authService.isAuthenticated$.pipe(
      take(1),
      map(() => {
        // Se o usuário não estiver autenticado, o AuthGuard já cuidará disso
        if (!this.authService.isAuthenticated()) {
          return false;
        }

        const user = this.authService.getCurrentUser();
        if (!user) {
          this.router.navigate(['/login']);
          return false;
        }

        // Admin tem acesso a tudo
        if (user.tipoUsuario === TipoUsuario.ADMIN) {
          return true;
        }

        // Verificar permissões específicas
        if (routeData.permissions && routeData.permissions.length > 0) {
          const hasPermission = this.authService.hasAnyPermission(routeData.permissions);
          if (!hasPermission) {
            this.router.navigate(['/acesso-negado'], {
              queryParams: { 
                reason: 'permission_denied',
                required: routeData.permissions.join(',')
              }
            });
            return false;
          }
        }

        // Verificar se todas as permissões são necessárias
        if (routeData.allPermissionsRequired && routeData.permissions && routeData.permissions.length > 0) {
          const hasAllPermissions = this.authService.hasAllPermissions(routeData.permissions);
          if (!hasAllPermissions) {
            this.router.navigate(['/acesso-negado'], {
              queryParams: { 
                reason: 'insufficient_permissions',
                required: routeData.permissions.join(',')
              }
            });
            return false;
          }
        }

        // Verificar roles específicos
        if (routeData.roles && routeData.roles.length > 0) {
          const hasRole = routeData.roles.includes(user.tipoUsuario);
          if (!hasRole) {
            this.router.navigate(['/acesso-negado'], {
              queryParams: { 
                reason: 'role_denied',
                required: routeData.roles.join(','),
                current: user.tipoUsuario
              }
            });
            return false;
          }
        }

        // Verificar se é admin only
        if (routeData.adminOnly === true) {
          if (!this.authService.isAdmin()) {
            this.router.navigate(['/acesso-negado'], {
              queryParams: { 
                reason: 'admin_only'
              }
            });
            return false;
          }
        }

        // Verificar se é owner ou admin
        if (routeData.managerOrAdmin === true) {
          if (!this.authService.isOwner() && !this.authService.isAdmin()) {
            this.router.navigate(['/acesso-negado'], {
              queryParams: { 
                reason: 'manager_or_admin_only'
              }
            });
            return false;
          }
        }

        // Verificar se pode gerenciar usuários
        if (routeData.requireUserManagement === true) {
          if (!this.authService.canManageUsers()) {
            this.router.navigate(['/acesso-negado'], {
              queryParams: { 
                reason: 'user_management_denied'
              }
            });
            return false;
          }
        }

        // Verificar se pode ver dashboard
        if (routeData.requireDashboardAccess === true) {
          if (!this.authService.canViewDashboard()) {
            this.router.navigate(['/produtos'], {
              queryParams: { 
                reason: 'dashboard_access_denied'
              }
            });
            return false;
          }
        }

        // Verificar se pode ver produtos
        if (routeData.requireProductAccess === true) {
          if (!this.authService.canViewProducts()) {
            this.router.navigate(['/acesso-negado'], {
              queryParams: { 
                reason: 'product_access_denied'
              }
            });
            return false;
          }
        }

        // Verificar se pode gerenciar estoque
        if (routeData.requireStockAccess === true) {
          if (!this.authService.canManageStock()) {
            this.router.navigate(['/produtos'], {
              queryParams: { 
                reason: 'stock_access_denied'
              }
            });
            return false;
          }
        }

        // Verificar se pode gerenciar vendas
        if (routeData.requireSalesAccess === true) {
          if (!this.authService.canManageSales()) {
            this.router.navigate(['/produtos'], {
              queryParams: { 
                reason: 'sales_access_denied'
              }
            });
            return false;
          }
        }

        // Verificar se pode gerenciar clientes
        if (routeData.requireClientAccess === true) {
          if (!this.authService.canManageClients()) {
            this.router.navigate(['/produtos'], {
              queryParams: { 
                reason: 'client_access_denied'
              }
            });
            return false;
          }
        }

        // Verificar se pode gerenciar eventos
        if (routeData.requireEventAccess === true) {
          if (!this.authService.canManageEvents()) {
            this.router.navigate(['/produtos'], {
              queryParams: { 
                reason: 'event_access_denied'
              }
            });
            return false;
          }
        }

        // Verificar se pode gerenciar produtos
        if (routeData.requireProductManagement === true) {
          if (!this.authService.canManageProducts()) {
            this.router.navigate(['/acesso-negado'], {
              queryParams: { 
                reason: 'product_management_denied'
              }
            });
            return false;
          }
        }

        // Verificar acesso apenas para vendedores
        if (routeData.vendedorOnly === true) {
          if (!this.authService.isVendedor()) {
            this.router.navigate(['/acesso-negado'], {
              queryParams: { 
                reason: 'vendedor_only'
              }
            });
            return false;
          }
        }

        // Verificar se usuário está ativo
        if (routeData.requireActiveUser === true) {
          if (!user.ativo) {
            this.router.navigate(['/conta-inativa'], {
              queryParams: { 
                reason: 'user_inactive'
              }
            });
            return false;
          }
        }

        // Verificar se não é primeiro acesso
        if (routeData.blockFirstAccess === true) {
          if (user.primeiroAcesso) {
            this.router.navigate(['/primeiro-acesso'], {
              queryParams: { 
                reason: 'first_access_required'
              }
            });
            return false;
          }
        }

        return true;
      })
    );
  }
}
