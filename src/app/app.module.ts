import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { DatePipe, CommonModule } from '@angular/common';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './components/login/login.component';
import { CadastroComponent } from './components/cadastro/cadastro.component';
import { PasswordStrengthIndicatorComponent } from './components/password-strength-indicator/password-strength-indicator.component';
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
// import { ConfiguracoesPontuacaoComponent } from './components/configuracoes-pontuacao/configuracoes-pontuacao.component';
import { AcessoNegadoComponent } from './components/acesso-negado/acesso-negado.component';
import { EnvironmentDebugComponent } from './components/environment-debug/environment-debug.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    CadastroComponent,
    PasswordStrengthIndicatorComponent,
    RedefinirSenhaComponent,
    NovaSenhaComponent,
    DashboardComponent,
    ProdutosComponent,
    EstoqueComponent,
    VendasComponent,
    RelatoriosComponent,
    PerfilComponent,
    ConfiguracoesComponent,
    ClientesComponent,
    EventosComponent,
    UserManagementComponent,
    // ConfiguracoesPontuacaoComponent,
    AcessoNegadoComponent,
    EnvironmentDebugComponent
  ],
  imports: [
    BrowserModule,
    CommonModule,
    AppRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    HttpClientModule
  ],
  providers: [DatePipe],
  bootstrap: [AppComponent]
})
export class AppModule { }
