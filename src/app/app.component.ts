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
    
    // Inicializar logging de ambiente na startup
    EnvironmentUtils.initializeEnvironmentLogging();
  }

  ngOnInit(): void {
    // Scroll para o topo ao mudar de página
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      window.scrollTo(0, 0);
    });

    // Verificar se deve redirecionar baseado no estado de autenticação
    this.isAuthenticated$.subscribe(isAuthenticated => {
      const currentPath = this.router.url;
      
      // Só redireciona se não estiver autenticado e não estiver em rotas públicas
      if (!isAuthenticated && !['/login', '/redefinir-senha', '/nova-senha'].includes(currentPath)) {
        this.router.navigate(['/login']);
      }
    });
  }

  logout(): void {
    this.authService.logout();
    // Força redirecionamento imediato para login
    this.router.navigate(['/login']);
  }
}
