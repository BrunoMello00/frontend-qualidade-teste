import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';

@Injectable({
  providedIn: 'root'
})
export class EmailNotificationService extends BaseService {

  enviarNotificacaoMudancaSenha(email: string, nomeUsuario: string, dataAlteracao: Date): Observable<any> {
    return this.post<any>('email/notificacao-mudanca-senha', { 
      email, 
      nomeUsuario, 
      dataAlteracao 
    });
  }
}
