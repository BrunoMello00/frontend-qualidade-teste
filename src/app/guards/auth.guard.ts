import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanActivateChild {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    return this.checkAuth(state.url);
  }

  canActivateChild(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    return this.checkAuth(state.url);
  }

  private checkAuth(url: string): Observable<boolean> {
    // Verificar se está autenticado
    if (!this.authService.isAuthenticated()) {
      console.log('🔒 AuthGuard: Usuário não autenticado, redirecionando para login');
      this.router.navigate(['/login'], { queryParams: { returnUrl: url } });
      return of(false);
    }

    // Verificar se tem token válido
    const token = this.authService.getToken();
    if (!token) {
      console.log('🔒 AuthGuard: Token não encontrado, redirecionando para login');
      this.authService.logout();
      return of(false);
    }

    // Verificar sessão no servidor
    return this.authService.verificarSessao().pipe(
      map(response => {
        if (response.success) {
          return true;
        } else {
          console.log('❌ AuthGuard: Sessão inválida');
          this.authService.logout();
          return false;
        }
      }),
      catchError(error => {
        console.log('❌ AuthGuard: Erro ao verificar sessão', error);
        
        if (error.status === 401) {
          this.authService.logout();
        }
        
        return of(false);
      })
    );
  }
}