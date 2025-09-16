import { Injectable } from '@angular/core';
import { 
  CanActivate, 
  CanActivateChild, 
  CanLoad, 
  Route, 
  UrlSegment, 
  ActivatedRouteSnapshot, 
  RouterStateSnapshot, 
  Router 
} from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanActivateChild, CanLoad {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    return this.checkAuth(state.url, route.data);
  }

  canActivateChild(
    childRoute: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    return this.checkAuth(state.url, childRoute.data);
  }

  canLoad(
    route: Route,
    segments: UrlSegment[]
  ): Observable<boolean> | Promise<boolean> | boolean {
    const url = segments.map(segment => segment.path).join('/');
    return this.checkAuth(`/${url}`, route.data);
  }

  private checkAuth(url: string, routeData?: any): Observable<boolean> {
    return this.authService.isAuthenticated$.pipe(
      take(1),
      map(isAuthenticated => {
        if (!isAuthenticated) {
          // Usuário não autenticado - redirecionar para login
          this.router.navigate(['/login'], { 
            queryParams: { returnUrl: url } 
          });
          return false;
        }

        // Verificar permissões específicas da rota
        if (routeData?.permissions && routeData.permissions.length > 0) {
          const hasPermission = this.authService.hasAnyPermission(routeData.permissions);
          if (!hasPermission) {
            // Usuário não tem permissão - redirecionar para página de acesso negado
            this.router.navigate(['/acesso-negado']);
            return false;
          }
        }

        // Verificar roles específicos da rota
        if (routeData?.roles && routeData.roles.length > 0) {
          const user = this.authService.getCurrentUser();
          const hasRole = user && routeData.roles.includes(user.tipoUsuario);
          if (!hasRole) {
            // Usuário não tem o role necessário
            this.router.navigate(['/acesso-negado']);
            return false;
          }
        }

        // Verificar se é admin only
        if (routeData?.adminOnly === true) {
          if (!this.authService.isAdmin()) {
            this.router.navigate(['/acesso-negado']);
            return false;
          }
        }

        return true;
      })
    );
  }
}
