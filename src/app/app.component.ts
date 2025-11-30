import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from './services/auth.service';
import { EnvironmentService } from './services/environment.service';
import { EnvironmentUtils } from './utils/environment.utils';
import { Usuario } from './models/user.models';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'Sistema de Estoque e Vendas';
  
  isAuthenticated$: Observable<boolean>;
  currentUser$: Observable<Usuario | null>;

  constructor(
    public authService: AuthService, 
    private router: Router,
    private environmentService: EnvironmentService
  ) {
    this.isAuthenticated$ = this.authService.isAuthenticated$;
    this.currentUser$ = this.authService.currentUser$;
    
    EnvironmentUtils.initializeEnvironmentLogging();
  }

  ngOnInit(): void {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      window.scrollTo(0, 0);
    });

    this.isAuthenticated$.subscribe(isAuthenticated => {
      const currentPath = this.router.url;
      
      if (!isAuthenticated && !['/login', '/redefinir-senha', '/nova-senha'].includes(currentPath)) {
        this.router.navigate(['/login']);
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getDefaultRoute(): string {
    const currentUser = this.authService.getCurrentUser();
    const userType = currentUser?.tipoUsuario;

    switch (userType) {
      case 'ESTOQUISTA':
        return '/estoque';
      case 'VENDEDOR':
        return '/vendas';
      case 'COMPRAS':
        return '/produtos';
      case 'ADMIN':
      case 'OWNER':
        return '/dashboard';
      default:
        return '/produtos';
    }
  }
}
