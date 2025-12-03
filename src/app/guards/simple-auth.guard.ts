import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class SimpleAuthGuard implements CanActivate, CanActivateChild {

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
    // Verificação simples: só verifica se tem token e usuário
    const token = this.authService.getToken();
    const user = this.authService.getCurrentUser();
    
    if (token && user) {
      return of(true);
    } else {
      console.log('🔒 SimpleAuthGuard: Usuário não autenticado, redirecionando para login');
      this.router.navigate(['/login'], { queryParams: { returnUrl: url } });
      return of(false);
    }
  }
}