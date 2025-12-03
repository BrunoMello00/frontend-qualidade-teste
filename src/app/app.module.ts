import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { DatePipe, CommonModule, DecimalPipe } from '@angular/common';

import { AuthInterceptor } from './interceptors/auth.interceptor';
import { SharedModule } from './shared/shared.module';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './components/login/login.component';
import { CadastroComponent } from './components/cadastro/cadastro.component';
import { PasswordStrengthIndicatorComponent } from './components/password-strength-indicator/password-strength-indicator.component';
import { RedefinirSenhaComponent } from './components/redefinir-senha/redefinir-senha.component';
import { NovaSenhaComponent } from './components/nova-senha/nova-senha.component';
import { PerfilComponent } from './components/perfil/perfil.component';
import { ConfiguracoesComponent } from './components/configuracoes/configuracoes.component';
import { EventosComponent } from './components/eventos/eventos.component';
import { UserManagementComponent } from './components/user-management/user-management.component';
import { AcessoNegadoComponent } from './components/acesso-negado/acesso-negado.component';

import { VendasComponent } from './components/vendas/vendas.component';
import { EstoqueComponent } from './components/estoque/estoque.component';
import { SistemaPontuacaoComponent } from './components/sistema-pontuacao/sistema-pontuacao.component';
import { ClientesComponent } from './components/clientes/clientes.component';
import { ProdutosComponent } from './components/produtos/produtos.component';
import { ProdutosDefeituososComponent } from './components/produtos-defeituosos/produtos-defeituosos.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    CadastroComponent,
    PasswordStrengthIndicatorComponent,
    RedefinirSenhaComponent,
    NovaSenhaComponent,
    PerfilComponent,
    ConfiguracoesComponent,
    EventosComponent,
    UserManagementComponent,
    AcessoNegadoComponent,
    VendasComponent,
    EstoqueComponent,
    SistemaPontuacaoComponent,
    ClientesComponent,
    ProdutosComponent,
    ProdutosDefeituososComponent
  ],
  imports: [
    BrowserModule,
    SharedModule,
    AppRoutingModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule
  ],
  providers: [
    DatePipe,
    DecimalPipe,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }