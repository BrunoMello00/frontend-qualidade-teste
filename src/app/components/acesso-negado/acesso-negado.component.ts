import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-acesso-negado',
  templateUrl: './acesso-negado.component.html',
  styleUrls: ['./acesso-negado.component.css']
})
export class AcessoNegadoComponent implements OnInit {
  reason: string = '';
  message: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.reason = params['reason'] || 'access_denied';
      this.setMessage();
    });
  }

  private setMessage(): void {
    switch (this.reason) {
      case 'dashboard_access_denied':
        this.message = 'Você não tem permissão para acessar o Dashboard. Esta área é restrita a Proprietários e Administradores.';
        break;
      case 'reports_access_denied':
        this.message = 'Você não tem permissão para acessar os Relatórios. Esta área é restrita a Proprietários e Administradores.';
        break;
      case 'user_management_denied':
        this.message = 'Você não tem permissão para gerenciar usuários. Esta área é restrita a Administradores.';
        break;
      case 'admin_only':
        this.message = 'Esta área é restrita apenas a Administradores do sistema.';
        break;
      case 'manager_or_admin_only':
        this.message = 'Esta área é restrita a Proprietários e Administradores do sistema.';
        break;
      case 'role_denied':
        this.message = 'Seu perfil de usuário não tem permissão para acessar esta área.';
        break;
      default:
        this.message = 'Você não tem permissão para acessar esta área do sistema.';
    }
  }

  voltarParaPrincipal(): void {
    if (this.authService.canViewDashboard()) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/produtos']);
    }
  }

  irParaPerfil(): void {
    this.router.navigate(['/perfil']);
  }
}
