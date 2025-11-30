import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    const requiredRoles = route.data['roles'] as string[];
    const requiredPermissions = route.data['permissions'] as string[];

    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return of(false);
    }

    const currentUser = this.authService.getCurrentUser();
    
    // Verificar roles se especificadas
    if (requiredRoles && requiredRoles.length > 0) {
      const userRole = currentUser?.tipoUsuario;
      if (!userRole || !requiredRoles.includes(userRole)) {
        console.log('🔒 RoleGuard: Acesso negado - Role insuficiente');
        this.router.navigate(['/acesso-negado']);
        return of(false);
      }
    }

    // Verificar permissões se especificadas
    if (requiredPermissions && requiredPermissions.length > 0) {
      const hasPermission = this.authService.hasAnyPermission(requiredPermissions);
      if (!hasPermission) {
        console.log('🔒 RoleGuard: Acesso negado - Permissão insuficiente');
        this.router.navigate(['/acesso-negado']);
        return of(false);
      }
    }

    return of(true);
  }
}