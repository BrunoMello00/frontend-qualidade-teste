import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
// IMPORTAÇÃO DE CADASTRO DESABILITADA PARA USO FUTURO
// import { CadastroComponent } from './components/cadastro/cadastro.component';
import { RedefinirSenhaComponent } from './components/redefinir-senha/redefinir-senha.component';
import { NovaSenhaComponent } from './components/nova-senha/nova-senha.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { ProdutosComponent } from './components/produtos/produtos.component';
import { EstoqueComponent } from './components/estoque/estoque.component';
import { VendasComponent } from './components/vendas/vendas.component';
import { RelatoriosComponent } from './components/relatorios/relatorios.component';
import { PerfilComponent } from './components/perfil/perfil.component';
import { ConfiguracoesComponent } from './components/configuracoes/configuracoes.component';
import { ClientesComponent } from './components/clientes/clientes.component';
import { EventosComponent } from './components/eventos/eventos.component';
import { UserManagementComponent } from './components/user-management/user-management.component';
import { AcessoNegadoComponent } from './components/acesso-negado/acesso-negado.component';
import { AuthGuard } from './guards/auth.guard';
import { PermissionGuard } from './guards/permission.guard';

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  // ROTA DE CADASTRO DESABILITADA PARA USO FUTURO
  // { path: 'cadastro', component: CadastroComponent },
  { path: 'redefinir-senha', component: RedefinirSenhaComponent },
  { path: 'nova-senha', component: NovaSenhaComponent },
  { 
    path: 'dashboard', 
    component: DashboardComponent, 
    canActivate: [AuthGuard, PermissionGuard],
    data: { 
      requireDashboardAccess: true,
      roles: ['OWNER', 'ADMIN']
    }
  },
  { 
    path: 'produtos', 
    component: ProdutosComponent, 
    canActivate: [AuthGuard, PermissionGuard],
    data: { 
      requireProductAccess: true 
    }
  },
  { 
    path: 'estoque', 
    component: EstoqueComponent, 
    canActivate: [AuthGuard, PermissionGuard],
    data: { 
      requireStockAccess: true 
    }
  },
  { 
    path: 'vendas', 
    component: VendasComponent, 
    canActivate: [AuthGuard, PermissionGuard],
    data: { 
      requireSalesAccess: true 
    }
  },
  { 
    path: 'clientes', 
    component: ClientesComponent, 
    canActivate: [AuthGuard, PermissionGuard],
    data: { 
      requireClientAccess: true 
    }
  },
  { 
    path: 'eventos', 
    component: EventosComponent, 
    canActivate: [AuthGuard, PermissionGuard],
    data: { 
      requireEventAccess: true 
    }
  },
  { 
    path: 'usuarios', 
    component: UserManagementComponent, 
    canActivate: [AuthGuard, PermissionGuard],
    data: { 
      requireUserManagement: true,
      adminOnly: true 
    }
  },
  { 
    path: 'relatorios', 
    component: RelatoriosComponent, 
    canActivate: [AuthGuard, PermissionGuard],
    data: { 
      requireReportsAccess: true,
      roles: ['OWNER', 'ADMIN']
    }
  },
  { 
    path: 'perfil', 
    component: PerfilComponent, 
    canActivate: [AuthGuard] 
  },
  { 
    path: 'configuracoes', 
    component: ConfiguracoesComponent, 
    canActivate: [AuthGuard] 
  },
  { 
    path: 'acesso-negado', 
    component: AcessoNegadoComponent 
  },
  { path: '**', redirectTo: '/produtos' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    scrollPositionRestoration: 'top',
    anchorScrolling: 'enabled'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
