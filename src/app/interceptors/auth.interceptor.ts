import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();
    
    // Adicionar token se existir e não for uma requisição de login/auth
    if (token && !this.isAuthRequest(request)) {
      request = this.addTokenHeader(request, token);
      console.log('🔑 AuthInterceptor: Token adicionado à requisição', request.url);
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        console.log('❌ AuthInterceptor: Erro na requisição', error.status, request.url);
        
        if (error.status === 401 && !this.isAuthRequest(request)) {
          return this.handle401Error(request, next);
        }
        
        if (error.status === 403) {
          console.error('🚫 Acesso negado:', error);
          this.router.navigate(['/acesso-negado']);
        }

        return throwError(() => error);
      })
    );
  }

  private isAuthRequest(request: HttpRequest<any>): boolean {
    return request.url.includes('/auth/login') || 
           request.url.includes('/auth/refresh') || 
           request.url.includes('/auth/register') ||
           request.url.includes('/auth/reset-senha') ||
           request.url.includes('/auth/solicitar-redefinicao');
  }

  private addTokenHeader(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = this.authService.getRefreshToken();

      if (refreshToken) {
        return this.authService.refreshToken().pipe(
          switchMap((response: any) => {
            this.isRefreshing = false;
            
            if (response.success && response.token) {
              this.refreshTokenSubject.next(response.token);
              return next.handle(this.addTokenHeader(request, response.token));
            }
            
            // Se o refresh falhou, fazer logout
            this.authService.logout();
            return throwError(() => new Error('Token refresh failed'));
          }),
          catchError((refreshError) => {
            this.isRefreshing = false;
            this.authService.logout();
            return throwError(() => refreshError);
          })
        );
      } else {
        // Não há refresh token, fazer logout direto
        this.authService.logout();
        return throwError(() => new Error('No refresh token available'));
      }
    }

    // Se já está refreshing, aguardar o novo token
    return this.refreshTokenSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap((token) => next.handle(this.addTokenHeader(request, token)))
    );
  }
}