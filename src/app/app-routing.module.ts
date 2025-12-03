import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { CadastroComponent } from './components/cadastro/cadastro.component';
import { RedefinirSenhaComponent } from './components/redefinir-senha/redefinir-senha.component';
import { NovaSenhaComponent } from './components/nova-senha/nova-senha.component';
import { PerfilComponent } from './components/perfil/perfil.component';
import { ConfiguracoesComponent } from './components/configuracoes/configuracoes.component';
import { EventosComponent } from './components/eventos/eventos.component';
import { UserManagementComponent } from './components/user-management/user-management.component';
import { AcessoNegadoComponent } from './components/acesso-negado/acesso-negado.component';
import { ProdutosComponent } from './components/produtos/produtos.component';
import { EstoqueComponent } from './components/estoque/estoque.component';
import { SistemaPontuacaoComponent } from './components/sistema-pontuacao/sistema-pontuacao.component';
import { VendasComponent } from './components/vendas/vendas.component';
import { ClientesComponent } from './components/clientes/clientes.component';
import { ProdutosDefeituososComponent } from './components/produtos-defeituosos/produtos-defeituosos.component';

import { AuthGuard } from './guards/auth.guard';
import { SimpleAuthGuard } from './guards/simple-auth.guard';
import { RoleGuard } from './guards/role.guard';
import { PageAccessGuard } from './guards/page-access.guard';

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'cadastro', component: CadastroComponent },
  { path: 'redefinir-senha', component: RedefinirSenhaComponent },
  { path: 'nova-senha', component: NovaSenhaComponent },
  
  { 
    path: 'dashboard', 
    loadChildren: () => import('./components/dashboard/dashboard.module').then(m => m.DashboardModule),
    canActivate: [SimpleAuthGuard, PageAccessGuard]
  },
  { 
    path: 'clientes', 
    component: ClientesComponent, 
    canActivate: [SimpleAuthGuard, PageAccessGuard] 
  },
  { 
    path: 'relatorios', 
    loadChildren: () => import('./components/relatorios/relatorios.module').then(m => m.RelatoriosModule),
    canActivate: [SimpleAuthGuard, PageAccessGuard]
  },
  { 
    path: 'produtos', 
    component: ProdutosComponent, 
    canActivate: [SimpleAuthGuard, PageAccessGuard] 
  },
  { 
    path: 'vendas', 
    component: VendasComponent, 
    canActivate: [SimpleAuthGuard, PageAccessGuard] 
  },
  { 
    path: 'estoque', 
    component: EstoqueComponent, 
    canActivate: [SimpleAuthGuard, PageAccessGuard]
  },
  { 
    path: 'produtos-defeituosos', 
    component: ProdutosDefeituososComponent, 
    canActivate: [SimpleAuthGuard, PageAccessGuard]
  },
  { 
    path: 'sistema-pontuacao', 
    component: SistemaPontuacaoComponent, 
    canActivate: [SimpleAuthGuard]
  },
  
  { path: 'perfil', component: PerfilComponent, canActivate: [SimpleAuthGuard] },
  { path: 'configuracoes', component: ConfiguracoesComponent, canActivate: [SimpleAuthGuard] },
  { path: 'eventos', component: EventosComponent, canActivate: [SimpleAuthGuard, PageAccessGuard] },
  { 
    path: 'user-management', 
    component: UserManagementComponent, 
    canActivate: [SimpleAuthGuard, PageAccessGuard]
  },
  
  { path: 'acesso-negado', component: AcessoNegadoComponent },
  
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }