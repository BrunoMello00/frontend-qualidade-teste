import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import { EnvironmentService } from './environment.service';
import { MockDataService } from './mock-data.service';

export interface EmailRequest {
  to: string;
  subject: string;
  body: string;
  isHtml?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class EmailNotificationService extends BaseService {
  constructor(http: HttpClient, environmentService: EnvironmentService, private mock: MockDataService) {
    super(http, environmentService);
  }

  enviarEmail(emailRequest: EmailRequest): Observable<any> {
    if (this.environmentService && this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.enviarEmail(emailRequest);
    }
    return this.post<void>('email/enviar', emailRequest);
  }

  enviarEmailResetSenha(email: string): Observable<any> {
    if (this.environmentService && this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.enviarEmailResetSenha(email);
    }
    return this.post<void>('email/reset-senha', { email });
  }

  validarCodigoReset(codigo: string): Observable<{ valido: boolean }> {
    if (this.environmentService && this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.validarCodigoReset(codigo);
    }
    return this.post<{ valido: boolean }>('email/validar-codigo-reset', { codigo });
  }

  // Método adicional para compatibilidade
  enviarNotificacaoAlteracaoPerfil(email: string, nome: string, campos: string[]): Observable<any> {
    if (this.environmentService && this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.enviarNotificacaoAlteracaoPerfil(email, nome, campos);
    }
    return this.post<any>('email/notificacao-alteracao-perfil', { email, nome, campos });
  }

  enviarNotificacaoMudancaSenha(email: string, nomeUsuario: string, dataAlteracao: Date): Observable<any> {
    if (this.environmentService && this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.enviarNotificacaoMudancaSenha(email, nomeUsuario, dataAlteracao);
    }
    return this.post<any>('email/notificacao-mudanca-senha', { 
      email, 
      nomeUsuario, 
      dataAlteracao 
    });
  }
}
